package xyz.nardone.agenda_wa.servizio.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class LocalConfiguration {
  @Bean
  Clock clock() {
    return Clock.system(ZoneId.of("Europe/Rome"));
  }
}
