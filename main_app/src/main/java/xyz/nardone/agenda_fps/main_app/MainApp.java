package xyz.nardone.agenda_fps.main_app;
import xyz.nardone.agenda_fps.applicazione.App;
import xyz.nardone.agenda_fps.servizio.AgendaFpsApplication;
import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class MainApp {

    private static ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        // Start Spring Boot and wait for it to initialize
        Thread springThread = new Thread(() -> {
            springContext = SpringApplication.run(AgendaFpsApplication.class, args);
        });
        springThread.setDaemon(true); // Ensure Spring Boot shuts down with the JavaFX app
        springThread.start();

        // Wait for Spring Boot to start
        while (springContext == null || !springContext.isRunning()) {
            try {
                Thread.sleep(100); // Check every 100ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Spring Boot startup interrupted", e);
            }
        }

        // Launch JavaFX application
        Application.launch(App.class, args);

        // Close Spring Boot when JavaFX exits
        if (springContext != null) {
            springContext.close();
        }
    }
}
