package fr.kissy.uptime.robot.stats;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import fr.kissy.uptime.robot.stats.converter.LocalDateTimeConverter;
import fr.kissy.uptime.robot.stats.converter.BillablePeriodConverter;
import fr.kissy.uptime.robot.stats.model.BillablePeriod;
import fr.kissy.uptime.robot.stats.model.Downtime;
import fr.kissy.uptime.robot.stats.collector.RemoveNonBillableHours;
import fr.kissy.uptime.robot.stats.collector.RemoveOverlappingDowntime;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static sun.tools.jar.CommandLine.parse;

public class Main {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Parameter(names={"--file", "-f"}, required = true)
    private String file;
    @Parameter(names={"--monitor", "-m"}, description = "List of monitors names, default to all monitors", variableArity = true)
    private List<String> monitors = new ArrayList<>();
    @Parameter(names={"--start-date", "-st"}, description = "Ignore downtime before given date excluded, default to first day of current month", converter = LocalDateTimeConverter.class)
    private LocalDateTime startDate = LocalDateTime.now().withDayOfMonth(1).with(LocalTime.MIDNIGHT);
    @Parameter(names={"--end-date", "-ed"}, description = "Ignore downtime after given date included, default to first day of next month", converter = LocalDateTimeConverter.class)
    private LocalDateTime endDate = LocalDateTime.now().withDayOfMonth(1).with(LocalTime.MIDNIGHT).plusMonths(1);
    @Parameter(names={"--billable-period", "-bp"}, description = "Ignore downtime outside of given periods, default to 08:30-19:00", converter = BillablePeriodConverter.class)
    private List<BillablePeriod> billablePeriods = Collections.singletonList(BillablePeriod.between(LocalTime.of(8, 30, 0), LocalTime.of(19, 0, 0)));
    @Parameter(names = {"--generate-report", "-gr"})
    private boolean generateReport = false;
    @Parameter(names = {"--help", "-h"}, help = true)
    private boolean help;

    public void run() {
        List<Downtime> allDowntimes = getRawDowntimes();

        List<Downtime> filteredDowntime = allDowntimes.stream()
                .filter(e -> monitors.isEmpty() || monitors.contains(e.getMonitor()))
                .sorted()
                .filter(d -> d.getEnd().isAfter(startDate) && d.getStart().isBefore(endDate))
                .collect(new RemoveNonBillableHours(billablePeriods))
                .stream()
                .collect(new RemoveOverlappingDowntime());

        long totalDowntime = filteredDowntime.stream().mapToLong(Downtime::getDuration).sum();
        long billableSeconds = billablePeriods.stream().mapToLong(BillablePeriod::getSeconds).sum();
        long totalTime = billableSeconds * countWeekDays(startDate, endDate);
        double percentageUptime = ((totalTime - totalDowntime) / (double) totalTime) * 100;

        if (generateReport) {
            generateReport(filteredDowntime, totalDowntime, totalTime, percentageUptime);
        } else {
            System.out.println("Total time :       " + totalTime + "s");
            System.out.println("Total downtime :   " + totalDowntime + "s");
            System.out.println("Upime :            " + percentageUptime + "s");
        }
    }

    private void generateReport(List<Downtime> filteredDowntime, long totalDowntime, long totalTime, double percentageUptime) {
        String dataJs = "var container = document.getElementById('visualization');\n";
        String groups = filteredDowntime.stream()
                .map(Downtime::getMonitor)
                .sorted()
                .distinct()
                .map(d -> "{id: '" + d + "', content: '" + d + "'}")
                .collect(Collectors.joining(",\n"));
        dataJs += "var groups = new vis.DataSet([\n" + groups + "]);\n";
        String events = filteredDowntime
                .stream()
                .map(d -> "{" +
                        "group: '" + d.getMonitor() + "', " +
                        "start: new Date('" + d.getStart().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "'), " +
                        "end: new Date('" + d.getEnd().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "')," +
                        "title: '" + d.getStart().format(DateTimeFormatter.ISO_LOCAL_TIME) + " to " + d.getEnd().format(DateTimeFormatter.ISO_LOCAL_TIME)
                        + " (" + Math.ceil(d.getDuration() / 60d) + "m)'" +
                        "}")
                .collect(Collectors.joining(",\n"));
        dataJs += "var items = new vis.DataSet([\n" + events + "]);\n";
        dataJs += "new vis.Timeline(container, items, groups, {" +
                "showCurrentTime: false, " +
                "start: new Date('" + startDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "'), " +
                "end: new Date('" + endDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "')," +
                "min: new Date('" + startDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "'), " +
                "max: new Date('" + endDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "')" +
                "});\n";

        String indexHtml = "<!DOCTYPE HTML>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <title>Uptime report</title>\n" +
                "    <script src=\"//visjs.org/dist/vis.js\"></script>\n" +
                "    <link href=\"//visjs.org/dist/vis-timeline-graph2d.min.css\" rel=\"stylesheet\" type=\"text/css\" />\n" +
                "</head>\n" +
                "<body>\n" +
                "\n" +
                "<h3>Uptime " + Math.round(percentageUptime * 100) / 100d + " (" + totalDowntime + "s/" + totalTime + "s)</h3>\n" +
                "\n" +
                "<div id=\"visualization\"></div>\n" +
                "<script type=\"text/javascript\" src=\"data.js\"></script>\n" +
                "</body>\n" +
                "</html>\n";

        try {
            Files.write(Paths.get("data.js"), dataJs.getBytes(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
            Files.write(Paths.get("index.html"), indexHtml.getBytes(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new RuntimeException("Impossible to write report file");
        }
    }

    public static void main(String[] args) {
        Main main = new Main();
        JCommander jCommander = JCommander.newBuilder()
                .addObject(main)
                .build();
        jCommander.parse(args);
        if (main.help) {
            jCommander.usage();
        } else {
            main.run();
        }
    }

    private List<Downtime> getRawDowntimes() {
        List<Downtime> downtimes = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(Paths.get(file))) {
            Iterable<CSVRecord> records = CSVFormat.RFC4180.withHeader("Event", "Monitor", "Date-Time", "Reason", "Duration", "Duration (in mins.)").parse(reader);
            for (CSVRecord record : records) {
                String event = record.get("Event");
                if (!event.equals("Down")) {
                    continue;
                }

                String monitor = record.get("Monitor");
                String dateTimeString = record.get("Date-Time");
                long duration = Long.parseLong(record.get("Duration (in mins.)")) * 60;
                LocalDateTime dateTime = LocalDateTime.parse(dateTimeString, DATE_TIME_FORMATTER);
                downtimes.add(new Downtime(monitor, dateTime, duration));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read input file", e);
        }
        return downtimes;
    }

    private static long countWeekDays(LocalDateTime start, LocalDateTime stop) {
        /*final DayOfWeek startW = start.getDayOfWeek();
        final DayOfWeek stopW = stop.getDayOfWeek();

        final long days = ChronoUnit.DAYS.between(start, stop);
        final long daysWithoutWeekends = days - 2 * ((days + startW.getValue()) / 7);

        return daysWithoutWeekends + (startW == DayOfWeek.SUNDAY ? 1 : 0) + (stopW == DayOfWeek.SUNDAY ? 1 : 0);*/
        return Duration.between(start, stop).toDays();
    }
}
