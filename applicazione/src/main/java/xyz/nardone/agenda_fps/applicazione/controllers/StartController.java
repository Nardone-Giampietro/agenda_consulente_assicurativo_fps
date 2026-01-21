package xyz.nardone.agenda_fps.applicazione.controllers;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nardone.agenda_fps.applicazione.App;
import xyz.nardone.agenda_fps.applicazione.DynamicPortQuery;
import xyz.nardone.agenda_fps.applicazione.ServerPort;

import java.io.IOException;


/**
 * Controller for the startup screen and service discovery.
 */
public class StartController {
    @FXML private Text infoText;

    private static final Logger logger = LogManager.getLogger(StartController.class);

    @FXML
    public void initialize() {
        try {
            int port = DynamicPortQuery.findServerPort(8080, 8090);
            ServerPort.get().setPort(port);
            Platform.runLater(() -> {
                try {
                    App.setRoot("dashboard");
                } catch (IOException e) {
                    infoText.setText("Errore applicazione.");
                    logger.error("Errore nel passare a Dashboard.", e);
                }
            });
        } catch (RuntimeException e) {
            infoText.setText("Errore durante l'inizializzazione del Server Database.");
            logger.error(e.getMessage());
        }
    }
}
