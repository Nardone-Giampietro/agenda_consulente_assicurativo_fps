package xyz.nardone.agenda_fps.applicazione.controllers;

import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nardone.agenda_fps.applicazione.*;
import xyz.nardone.agenda_fps.applicazione.utility.Normalize;

import java.io.IOException;
import java.net.URLEncoder;

/**
 * Controller for adding a new esito.
 */
public class AddEsitoController {
    @FXML Text infoText;
    @FXML TextField Esito;

    private static final Logger logger = LogManager.getLogger(AddEsitoController.class);

    @FXML
    private void addEsito() {
        if (!Esito.getText().trim().isEmpty()) {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        String query = String.format("nomeEsito=%s",
                                URLEncoder.encode(Normalize.normalize(Esito.getText().trim()), "UTF-8"));
                        String urlString = "/esiti/add" + "?" + query;
                        ConnectionRequest<Void> req = new ConnectionRequest("PUT", urlString);
                        Response resp = req.send(Response.class);
                        Platform.runLater(() -> {
                            if (resp.status.equals("OK")) {
                                Esito.setDisable(false);
                            }
                            infoText.setText(resp.message);
                        });
                    } catch (IOException e) {
                        infoText.setText("Errore comunicazione con il Server.");
                        logger.error(e.getMessage());
                    }
                    return null;
                }
            };
            Esito.setDisable(true);
            new Thread(task).start();
        }
    }

    @FXML
    private void switchToDashboard() throws IOException{
        PageSharedId.getInstance().setId(null);
        App.setRoot("dashboard");
    }

    @FXML
    public void switchToEsiti() throws IOException {
        PageSharedId.getInstance().setId(5);
        App.setRoot("esitiList");
    }

}
