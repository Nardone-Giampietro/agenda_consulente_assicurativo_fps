package xyz.nardone.agenda_fps.applicazione.controllers;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nardone.agenda_fps.applicazione.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDate;

/**
 * Controller for updating a callback.
 */
public class UpdateCallbackController {

    @FXML
    private ComboBox<String> bankerComboBox;
    @FXML
    private ComboBox<String> clientComboBox;
    @FXML
    private DatePicker datePicker;
    @FXML
    private DatePicker callbackDatePicker;
    @FXML
    private TextArea noteTextArea;
    @FXML
    private Button dashboardButton;
    @FXML
    private Button annullaButton;
    @FXML
    private Button actionButton;
    @FXML
    private Text infoText;

    private static final Logger logger = LogManager.getLogger(UpdateCallbackController.class);
    private final Integer callbackId = CallbackSharedId.getInstance().geId();
    private Callback callback;

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
        initializeCallback();
        initializeBankerComboBox();
        initializeClientComboBox();
        initializeDatePicker();
        callbackDatePicker.setValue(callback.data == null ? null : LocalDate.parse(callback.data));
        noteTextArea.setText(callback.note);
    }

    private void initializeCallback() throws IOException {
        String query = String.format("callbackId=%s",
                URLEncoder.encode(callbackId.toString(), "UTF-8"));
        String urlString = "/callbacks/get" + "?" + query;
        ConnectionRequest<Void> req = new ConnectionRequest<>("POST", urlString);
        Gson gson = new Gson();
        callback = req.send(Callback.class);
    }

    private void initializeBankerComboBox() throws IOException {
        bankerComboBox.setValue(callback.banker.cognome + " " + callback.banker.nome);
        bankerComboBox.setDisable(true);
    }

    private void initializeClientComboBox() throws IOException {
        clientComboBox.setValue(callback.client.cognome + " " + callback.client.nome);
        clientComboBox.setDisable(true);
    }

    private void initializeDatePicker() {
        datePicker.setValue(LocalDate.parse(callback.data_app));
        datePicker.setDisable(true);
    }

    @FXML
    public void updateCallback() {
        Task<Void> task = new Task<>() {
            @Override
            public Void call() {
                try {
                    CallbackIds updated = new CallbackIds(
                            callback.id,
                            noteTextArea.getText().trim(),
                            callbackDatePicker.getValue() == null ? null : callbackDatePicker.getValue().toString(),
                            datePicker.getValue().toString(),
                            callback.banker.id,
                            callback.client.id
                    );
                    ConnectionRequest<CallbackIds> req = new ConnectionRequest<>("PUT", "/callbacks/update", updated);
                    Response res = req.send(Response.class);
                    Platform.runLater(() -> infoText.setText(res.message));
                } catch (IOException e) {
                    Platform.runLater(() -> infoText.setText("Errore comunicazione con il Server."));
                    logger.error("Error updating appointment: ", e);
                }
                return null;
            }
        };
        new Thread(task).start();
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
}
