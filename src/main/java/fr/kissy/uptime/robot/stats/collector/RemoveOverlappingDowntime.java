package fr.kissy.uptime.robot.stats.collector;

import fr.kissy.uptime.robot.stats.model.Downtime;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class RemoveOverlappingDowntime implements Collector<Downtime, List<Downtime>, List<Downtime>> {

    @Override
    public Supplier<List<Downtime>> supplier() {
        return ArrayList::new;
    }

    @Override
    public BiConsumer<List<Downtime>, Downtime> accumulator() {
        return (downtimes, downtime) -> {
            if (downtimes.isEmpty()) {
                downtimes.add(downtime);
            } else {
                Downtime last = downtimes.get(downtimes.size() - 1);
                if (downtime.getStart().isEqual(last.getEnd()) || downtime.getStart().isAfter(last.getEnd())) { // No overlapping
                    downtimes.add(downtime);
                } else if (downtime.getEnd().isAfter(last.getEnd())) { // Overlapping but counting
                    Duration duration = Duration.between(last.getEnd(), downtime.getEnd());
                    Downtime newDowntime = new Downtime(downtime.getMonitor(), last.getEnd(), duration.getSeconds());
                    downtimes.add(newDowntime);
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
}
