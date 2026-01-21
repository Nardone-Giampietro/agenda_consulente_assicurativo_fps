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
 * Controller for updating a polizza.
 */
public class UpdatePolizzeController {

    @FXML
    private TextField polizzaField;
    @FXML
    private Text infoText;

    private static final Logger logger = LogManager.getLogger(UpdatePolizzeController.class);
    private final Integer polizzaId = PolizzaSharedId.getInstance().geId();
    private Polizza polizza;

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
        initializePolizza();
        initializePolizzaField();
    }

    private void initializePolizza() throws IOException {
        String query = String.format("polizzaId=%s",
                URLEncoder.encode(polizzaId.toString(), "UTF-8"));
        String urlString = "/polizze/get" + "?" + query;
        ConnectionRequest<Void> connection = new ConnectionRequest<>("POST", urlString);
        polizza = connection.send(Polizza.class);
    }

    private void initializePolizzaField() throws IOException {
        polizzaField.setText(polizza.getPolizza());
    }

    @FXML
    public void updatePolizza() {
        if (!polizzaField.getText().trim().isEmpty()) {
            Task<Void> task = new Task<>() {
                @Override
                public Void call() {
                    try {
                        Polizza updatedPolizza = new Polizza(polizzaId, Normalize.normalize(polizzaField.getText().trim()));
                        ConnectionRequest<Polizza> req = new ConnectionRequest<>("PUT", "/polizze/update", updatedPolizza);
                        Response res = req.send(Response.class);
                        Platform.runLater(() -> infoText.setText(res.message));
                    } catch (IOException e) {
                        Platform.runLater(() -> infoText.setText("Errore comunicazione con il Server."));
                        logger.error("Error updating polizza: ", e);
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
    public void switchToPolizze() throws IOException {
        PageSharedId.getInstance().setId(null);
        App.setRoot("polizzeList");
    }
}
