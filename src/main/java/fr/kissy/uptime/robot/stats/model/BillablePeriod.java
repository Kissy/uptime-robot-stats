package fr.kissy.uptime.robot.stats.model;

import java.time.Duration;
import java.time.LocalTime;
import java.time.Period;

public class BillablePeriod {
    private final LocalTime from;
    private final LocalTime to;

    private BillablePeriod(LocalTime from, LocalTime to) {
        this.from = from;
        this.to = to;
    }

    public static BillablePeriod between(LocalTime from, LocalTime to) {
        return new BillablePeriod(from, to);
    }

    public long getSeconds() {
        return Duration.between(from, to).getSeconds();
    }

    public LocalTime getFrom() {
        return from;
    }

    public LocalTime getTo() {
        return to;
    }
}
