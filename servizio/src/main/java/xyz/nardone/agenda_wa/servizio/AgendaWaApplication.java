package xyz.nardone.agenda_wa.servizio;

import java.nio.file.Path;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import xyz.nardone.agenda_wa.servizio.config.LocalLaunchGuard;

@SpringBootApplication
public class AgendaWaApplication {
  public static void main(String[] args) throws Exception {
    String data =
        System.getenv()
            .getOrDefault(
                "AGENDA_DATA_DIR", System.getProperty("user.home") + "/.local/share/agenda-wa");
    int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    for (String arg : args) {
      if (arg.startsWith("--agenda.data-dir=")) data = arg.substring("--agenda.data-dir=".length());
      if (arg.startsWith("--server.port="))
        port = Integer.parseInt(arg.substring("--server.port=".length()));
    }
    if (!LocalLaunchGuard.acquire(Path.of(data), port)) return;
    try {
      SpringApplication.run(AgendaWaApplication.class, args);
    } catch (Exception e) {
      LocalLaunchGuard.release();
      throw e;
    }
  }
}
