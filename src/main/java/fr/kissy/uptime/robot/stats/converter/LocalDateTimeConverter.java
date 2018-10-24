package fr.kissy.uptime.robot.stats.converter;

import com.beust.jcommander.IStringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeConverter implements IStringConverter<LocalDateTime> {
  @Override
  public LocalDateTime convert(String value) {
    return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
  }
}