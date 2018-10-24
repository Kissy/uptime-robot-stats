package fr.kissy.uptime.robot.stats.converter;

import com.beust.jcommander.IStringConverter;
import fr.kissy.uptime.robot.stats.model.BillablePeriod;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class BillablePeriodConverter implements IStringConverter<BillablePeriod> {
  @Override
  public BillablePeriod convert(String value) {
    String[] localTimes = value.split("-");
    if (localTimes.length != 2) {
      throw new IllegalArgumentException("Cannot parse billable period " + value);
    }
    return BillablePeriod.between(LocalTime.parse(localTimes[0], DateTimeFormatter.ISO_LOCAL_TIME), LocalTime.parse(localTimes[1], DateTimeFormatter.ISO_LOCAL_TIME));
  }
}