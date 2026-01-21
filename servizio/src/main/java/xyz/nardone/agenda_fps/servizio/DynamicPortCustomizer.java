package xyz.nardone.agenda_fps.servizio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Assigns a free port within a range when the service boots.
 */
@Component
public class DynamicPortCustomizer implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {
    private static Logger logger = LoggerFactory.getLogger(DynamicPortCustomizer.class);

    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        try {
            int port = findFreePort(8080, 8090);
            factory.setPort(port);
            logger.info("Service is running on Port: " + port + " (http)");
        } catch (IOException | IllegalStateException e){
            logger.error(e.getMessage());
        }
    }

    private int findFreePort(int start, int end) throws IOException {
        for (int i = start; i <= end; i++) {
            if (isPortAvailable(i)){
                return i;
            }
        }
        throw new IllegalStateException("Porta libera non trovata.");
    }

    private boolean isPortAvailable(int port) throws IOException {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        }  catch (IOException e) {
            return false;
        }
    }
}
