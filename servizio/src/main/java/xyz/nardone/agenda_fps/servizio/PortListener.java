package xyz.nardone.agenda_fps.servizio;

import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Captures the HTTP port that Spring Boot selects at runtime.
 */
@Component
public class PortListener implements ApplicationListener<WebServerInitializedEvent> {

    private int port;

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        this.port = event.getWebServer().getPort();
    }

    public int getPort() {
        return port;
    }
}
