package xyz.nardone.agenda_fps.applicazione.controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nardone.agenda_fps.applicazione.App;
import xyz.nardone.agenda_fps.applicazione.ConnectionRequest;
import xyz.nardone.agenda_fps.applicazione.PageSharedId;
import xyz.nardone.agenda_fps.applicazione.Response;
import xyz.nardone.agenda_fps.applicazione.utility.Normalize;

import java.io.IOException;
import java.net.URLEncoder;

/**
 * Controller for adding a new polizza.
 */
public class AddPolizzaController {
    @FXML Text infoText;
    @FXML TextField polizza;

    private static final Logger logger = LogManager.getLogger(AddPolizzaController.class);

    @FXML
    private void addPolizza() {
        if (!polizza.getText().trim().isEmpty()) {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        String query = String.format("nomePolizza=%s",
                                URLEncoder.encode(Normalize.normalize(polizza.getText().trim()), "UTF-8"));
                        String urlString = "/polizze/add" + "?" + query;
                        ConnectionRequest<Void> req = new ConnectionRequest("PUT", urlString);
                        Response resp = req.send(Response.class);
                        Platform.runLater(() -> {
                            if (resp.status.equals("OK")) {
                                polizza.setDisable(false);
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
            polizza.setDisable(true);
            new Thread(task).start();
        }
    }

    @FXML
    private void switchToDashboard() throws IOException{
        PageSharedId.getInstance().setId(null);
        App.setRoot("dashboard");
    }

    @FXML
    public void switchToPolizze() throws IOException {
        PageSharedId.getInstance().setId(5);
        App.setRoot("polizzeList");
    }

}
