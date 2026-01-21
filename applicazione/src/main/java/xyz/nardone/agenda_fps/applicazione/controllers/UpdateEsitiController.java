package xyz.nardone.agenda_fps.applicazione.controllers;

import javafx.application.Platform;
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
 * Controller for updating an esito.
 */
public class UpdateEsitiController {

    @FXML
    private TextField esitoField;
    @FXML
    private Text infoText;

    private static final Logger logger = LogManager.getLogger(UpdateEsitiController.class);
    private final Integer esitoId = EsitoSharedId.getInstance().geId();
    private Esito esito;

    @FXML
    public void initialize() {
        try {
            initializeFields();
        } catch (IOException e) {
            logger.error("Error initializing fields: ", e);
            infoText.setText("Errore comunicazione con il Server.");
        }
    }

    private void initializeFields() throws IOException {
        initializeEsito();
        initializeEsitoField();
    }

    private void initializeEsito() throws IOException {
        String query = String.format("esitoId=%s",
                URLEncoder.encode(esitoId.toString(), "UTF-8"));
        String urlString = "/esiti/get" + "?" + query;
        ConnectionRequest<Void> connection = new ConnectionRequest<>("POST", urlString);
        esito = connection.send(Esito.class);
    }

    private void initializeEsitoField() throws IOException {
        esitoField.setText(esito.getEsito());
    }

    @FXML
    public void updateEsito() {
        if (!esitoField.getText().trim().isEmpty()) {
            Task<Void> task = new Task<>() {
                @Override
                public Void call() {
                    try {
                        Esito updatedEsito = new Esito(esitoId, Normalize.normalize(esitoField.getText().trim()));
                        ConnectionRequest<Esito> req = new ConnectionRequest<>("PUT", "/esiti/update", updatedEsito);
                        Response res = req.send(Response.class);
                        Platform.runLater(() -> infoText.setText(res.message));
                    } catch (IOException e) {
                        Platform.runLater(() -> infoText.setText("Errore comunicazione con il Server."));
                        logger.error("Error updating esito: ", e);
                    }
                    return null;
                }
            };
            new Thread(task).start();
        }
    }

    @FXML
    public void switchToCallbacksList() throws IOException {
        PageSharedId.getInstance().setId(2);
        App.setRoot("callbacksList");
    }

    @FXML
    public void switchToDashboard() throws IOException {
        PageSharedId.getInstance().setId(null);
        App.setRoot("dashboard");
    }

    @FXML
    public void switchToAppointmentsList() throws IOException {
        PageSharedId.getInstance().setId(1);
        App.setRoot("appointmentsList");
    }

    @FXML
    public void switchToClients() throws IOException {
        PageSharedId.getInstance().setId(3);
        App.setRoot("clientsList");
    }

    @FXML
    public void switchToBankers() throws IOException {
        App.setRoot("bankersList");
    }

    @FXML
    public void switchToEsiti() throws IOException {
        PageSharedId.getInstance().setId(null);
        App.setRoot("esitiList");
    }
}
