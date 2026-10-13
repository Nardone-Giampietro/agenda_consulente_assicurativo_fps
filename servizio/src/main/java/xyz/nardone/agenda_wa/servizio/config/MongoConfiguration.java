package xyz.nardone.agenda_wa.servizio.config;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

@Configuration
public class MongoConfiguration {
  @Bean
  MongoTransactionManager transactionManager(MongoDatabaseFactory factory) {
    return new MongoTransactionManager(factory);
  }

  @Bean
  Clock clock() {
    return Clock.system(ZoneId.of("Europe/Rome"));
  }

  @Bean
  MongoCustomConversions mongoCustomConversions() {
    return MongoCustomConversions.create(
        adapter -> {
          adapter.registerConverter(new DateWriter());
          adapter.registerConverter(new DateReader());
        });
  }

  @WritingConverter
  static class DateWriter implements Converter<LocalDate, String> {
    public String convert(LocalDate date) {
      return date.toString();
    }
  }

  @ReadingConverter
  static class DateReader implements Converter<String, LocalDate> {
    public LocalDate convert(String date) {
      return LocalDate.parse(date);
    }
  }
}
