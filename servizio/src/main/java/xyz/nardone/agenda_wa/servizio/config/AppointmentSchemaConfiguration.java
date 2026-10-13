package xyz.nardone.agenda_wa.servizio.config;

import xyz.nardone.agenda_wa.servizio.repository.ClientHistorySchemaRepository;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import xyz.nardone.agenda_wa.servizio.repository.AppointmentSchemaRepository;
import xyz.nardone.agenda_wa.servizio.repository.ClientSchemaRepository;

@Configuration
public class AppointmentSchemaConfiguration {
  @Bean
  SmartInitializingSingleton appointmentSchema(
      AppointmentSchemaRepository schema, ClientSchemaRepository clients,
      ClientHistorySchemaRepository history) {
    return () -> {
      clients.migrate();
      schema.migrate();
      history.migrate();
      clients.removeLegacyIndexes();
      schema.removeLegacyIndexes();
      schema.ensureTextIndex();
    };
  }
}
