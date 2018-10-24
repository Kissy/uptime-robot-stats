package fr.kissy.uptime.robot.stats.model;

import java.time.Duration;
import java.time.LocalDateTime;

public class Downtime implements Comparable<Downtime> {
    private String monitor;
    private final long duration;
    private final LocalDateTime start;
    private final LocalDateTime end;

    public Downtime(String monitor, LocalDateTime start, LocalDateTime end) {
        this(monitor, start, Duration.between(start, end).getSeconds());
    }

    public Downtime(String monitor, LocalDateTime start, long duration) {
        this.monitor = monitor;
        this.duration = duration;
        this.start = start;
        this.end = this.start.plusSeconds(duration);
    }

    public String getMonitor() {
        return monitor;
    }

    public long getDuration() {
        return duration;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public boolean isOverTwoDays() {
        return start.getDayOfMonth() != end.getDayOfMonth();
    }

    @Override
    public String toString() {
        return "Downtime{" +
                "duration=" + duration +
                ", start=" + start +
                ", end=" + end +
                '}';
    }

    @Override
    public int compareTo(Downtime o) {
        return this.start.compareTo(o.start);
    }
}
