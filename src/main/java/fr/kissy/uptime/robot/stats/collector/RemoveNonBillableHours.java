package fr.kissy.uptime.robot.stats.collector;

import fr.kissy.uptime.robot.stats.model.BillablePeriod;
import fr.kissy.uptime.robot.stats.model.Downtime;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class RemoveNonBillableHours implements Collector<Downtime, List<Downtime>, List<Downtime>> {
    private List<BillablePeriod> billablePeriods;

    public RemoveNonBillableHours(List<BillablePeriod> billablePeriods) {
        this.billablePeriods = billablePeriods;
    }

    @Override
    public Supplier<List<Downtime>> supplier() {
        return ArrayList::new;
    }

    @Override
    public BiConsumer<List<Downtime>, Downtime> accumulator() {
        return (downtimes, downtime) -> {
            for (BillablePeriod billablePeriod : billablePeriods) {
                addBillableDowntime(downtimes, downtime, billablePeriod, downtime.getStart());
                if (downtime.isOverTwoDays()) {
                    addBillableDowntime(downtimes, downtime, billablePeriod, downtime.getEnd());
                }
            }
        };
    }

    @Override
    public BinaryOperator<List<Downtime>> combiner() {
        return (first, second) -> {
            throw new NotImplementedException();
        };
    }

    @Override
    public Function<List<Downtime>, List<Downtime>> finisher() {
        return Function.identity();
    }

    @Override
    public Set<Characteristics> characteristics() {
        return new HashSet<>(Collections.singletonList(Characteristics.IDENTITY_FINISH));
    }

    private void addBillableDowntime(List<Downtime> downtimes, Downtime downtime, BillablePeriod billablePeriod, LocalDateTime day) {
        // TODO handle differently
//        if (day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY) {
//            return;
//        }

        LocalDateTime start = day.with(billablePeriod.getFrom());
        LocalDateTime end = day.with(billablePeriod.getTo());
        if (downtime.getStart().isBefore(end) && downtime.getEnd().isAfter(start)) {
            LocalDateTime newDowntimeStart = downtime.getStart().isBefore(start) ? start : downtime.getStart();
            LocalDateTime newDowntimeEnd = downtime.getEnd().isAfter(end) ? end : downtime.getEnd();
            downtimes.add(new Downtime(downtime.getMonitor(), newDowntimeStart, newDowntimeEnd));
        }
    }
}
