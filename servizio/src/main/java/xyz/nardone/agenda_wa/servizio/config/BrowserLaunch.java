package xyz.nardone.agenda_wa.servizio.config;

import java.io.IOException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class BrowserLaunch implements ApplicationListener<ApplicationReadyEvent> {
  private final boolean enabled;

  public BrowserLaunch(@Value("${agenda.open-browser:true}") boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    if (!enabled) return;
    int port =
        ((WebServerApplicationContext) event.getApplicationContext()).getWebServer().getPort();
    try {
      open(port);
    } catch (IOException e) {
      LoggerFactory.getLogger(getClass())
          .info("Apri l’agenda nel browser: http://localhost:{}", port);
    }
  }

  public static void open(int port) throws IOException {
    String command =
        System.getProperty("os.name").toLowerCase().contains("mac") ? "/usr/bin/open" : "xdg-open";
    new ProcessBuilder(command, "http://localhost:" + port).start();
  }
}
