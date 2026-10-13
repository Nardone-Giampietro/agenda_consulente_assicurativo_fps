package xyz.nardone.agenda_wa.servizio.config;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;
import xyz.nardone.agenda_wa.servizio.service.LocalInitializationService;

@Component
public class LocalBootstrap implements SmartInitializingSingleton {
  private final LocalInitializationService initialization;

  public LocalBootstrap(LocalInitializationService initialization) {
    this.initialization = initialization;
  }

  @Override
  public void afterSingletonsInstantiated() {
    try {
      initialization.initialize();
    } catch (Exception e) {
      throw new IllegalStateException("Inizializzazione dell’agenda locale non riuscita.", e);
    }
  }
}
