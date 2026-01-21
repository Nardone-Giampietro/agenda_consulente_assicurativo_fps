package xyz.nardone.agenda_fps.applicazione.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nardone.agenda_fps.applicazione.*;


/**
 * Controller for creating new appointments from the UI.
 */
public class AddAppointmentController {
    @FXML
    ChoiceBox<String> appointmentEsitoChoiceBox;
    @FXML
    ChoiceBox<String> polizzaChoiceBox;
    @FXML
    ComboBox<String> bankerComboBox;
    @FXML
    ComboBox<String> clientComboBox;
    @FXML
    TextArea noteTextArea;
    @FXML
    DatePicker datePicker;
    @FXML
    TextField premioField;
    @FXML
    Text infoText;
    @FXML
    ToggleButton callbackToggleButton;
    @FXML
    DatePicker callbackDatePicker;
    @FXML
    HBox callbackDateBox;
    @FXML
    Button addAppointmentButton;

    private static final Logger logger = LogManager.getLogger(AddAppointmentController.class);
    private ObservableList<String> appointmentChoices;
    private ObservableList<String> polizzaChoices;
    private ObservableList<String> bankerChoices;
    private ObservableList<String> clientChoices;
    private final Map<String, String> bankerMap = new HashMap<>();
    private Map<String, String> clientMap;

    /**
     * Loads initial data and configures input controls.
     */
    @FXML
    public void initialize() {
        appointmentChoices = FXCollections.observableArrayList();
        bankerChoices = FXCollections.observableArrayList();
        clientComboBox.setDisable(true);
        try {
            initializebankerComboBox();
            initializePolizzaChoiceBox();
            initializeEsitoChoiceBox();
            initializePremioField();
            initializeToggleButton();
        } catch (IOException e) {
            infoText.setText("Errore comunicazione con il Server.");
            logger.error(e.getMessage());
        }
    }

    private void initializeToggleButton() {
        callbackToggleButton.setOnAction(event -> {
            if (callbackToggleButton.isSelected()) {
                callbackToggleButton.setText("Sì");
                callbackToggleButton.getStyleClass().removeAll("toggle-button-no");
                callbackToggleButton.getStyleClass().add("toggle-button-yes");
                callbackDateBox.setVisible(true);
                callbackDateBox.setManaged(true);
            } else {
                callbackToggleButton.setText("No");
                callbackToggleButton.getStyleClass().removeAll("toggle-button-yes");
                callbackToggleButton.getStyleClass().add("toggle-button-no");
                callbackDateBox.setVisible(false);
                callbackDateBox.setManaged(false);

            }
        });
    }

    private void initializebankerComboBox() throws IOException {
        ConnectionRequest<Void> req = new ConnectionRequest<>("GET", "/bankers/all");
        JsonElement json = req.send(JsonElement.class);
        JsonArray bankers = json.getAsJsonArray();
        for (int i = 0; i < bankers.size(); i++) {
            JsonObject b = bankers.get(i).getAsJsonObject();
            String bankerId = b.get("id").getAsString();
            String bankerFullName = b.get("cognome").getAsString() + " "
                    + b.get("nome").getAsString();
            bankerMap.put(bankerFullName, bankerId);
            bankerChoices.add(bankerFullName);
        }
        Collections.sort(bankerChoices);
        bankerComboBox.setItems(bankerChoices);
    }

    private void initializePolizzaChoiceBox() throws IOException {
        polizzaChoices = FXCollections.observableArrayList();
        ConnectionRequest<Void> connection = new ConnectionRequest<>("GET", "/polizze/all");
        JsonElement json = connection.send(JsonElement.class);
        JsonArray polizze = json.getAsJsonArray();
        for (int i = 0; i < polizze.size(); i++) {
            JsonObject p = polizze.get(i).getAsJsonObject();
            String polizzaName = p.get("polizza").getAsString();
            polizzaChoices.add(polizzaName);
        }
        Collections.sort(polizzaChoices);
        polizzaChoices.addFirst("Nessuna");
        polizzaChoiceBox.setItems(polizzaChoices);
    }

    private void initializeEsitoChoiceBox() throws IOException {
        ConnectionRequest<Void> req = new ConnectionRequest<>("GET", "/esiti/all");
        JsonElement json = req.send(JsonElement.class);
        JsonArray esiti = json.getAsJsonArray();
        for (int i = 0; i < esiti.size(); i++) {
            JsonObject es = esiti.get(i).getAsJsonObject();
            String esito = es.get("esito").getAsString();
            appointmentChoices.add(esito);
        }
        appointmentEsitoChoiceBox.setItems(appointmentChoices);
    }

    private void initializePremioField() {
        premioField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) { // Allows only digits (0-9)
                premioField.setText(oldValue);
            }
        });
        premioField.setText("0");
    }

    /**
     * Submits a new appointment (and optional callback) to the service.
     */
    @FXML
    private void addAppointment() {
        if (appointmentEsitoChoiceBox.getValue() != null &&
                polizzaChoiceBox.getValue() != null &&
                bankerComboBox.getValue() != null &&
                clientComboBox.getValue() != null &&
                datePicker.getValue() != null) {
            Task<Void> task1 = new Task<>() {
                @Override
                public Void call() {
                    AppointmentIds appointment = new AppointmentIds(
                            null,
                            datePicker.getValue().toString(),
                            appointmentEsitoChoiceBox.getValue(),
                            polizzaChoiceBox.getValue(),
                            premioField.getText().trim().isEmpty() ? 0 : Integer.parseInt(premioField.getText().trim()),
                            Integer.valueOf(bankerMap.get(bankerComboBox.getValue())),
                            Integer.valueOf(clientMap.get(clientComboBox.getValue())),
                            noteTextArea.getText().trim()
                    );
                    try {
                        ConnectionRequest<AppointmentIds> req = new ConnectionRequest<>("PUT", "/appointments/add", appointment);
                        Response res = req.send(Response.class);
                        Platform.runLater(() -> {
                            addAppointmentButton.setDisable(false);
                            addAppointmentButton.setText("Aggiungi Nuovo Appuntamento");
                            addAppointmentButton.setOnAction(event -> {
                                try {
                                    switchToAddAppointment();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                            infoText.setText(res.message);
                        });
                    } catch (IOException e) {
                        infoText.setText("Errore comunicazione con il Server.");
                        logger.error(e.getMessage());
                    }
                    return null;
                }
            };
            Task<Void> task2 = new Task<>() {
                @Override
                public Void call() {
                    CallbackIds callback = new CallbackIds(
                            null,
                            noteTextArea.getText().trim(),
                            callbackDatePicker.getValue() == null ? null : callbackDatePicker.getValue().toString(),
                            datePicker.getValue().toString(),
                            Integer.valueOf(bankerMap.get(bankerComboBox.getValue())),
                            Integer.valueOf(clientMap.get(clientComboBox.getValue()))
                    );
                    try {
                        ConnectionRequest<CallbackIds> req = new ConnectionRequest<>("PUT", "/callbacks/add", callback);
                        Response res = req.send(Response.class);
                    } catch (IOException e) {
                        logger.error(e.getMessage());
                    }
                    return null;
                }
            };
            appointmentEsitoChoiceBox.setDisable(true);
            polizzaChoiceBox.setDisable(true);
            bankerComboBox.setDisable(true);
            clientComboBox.setDisable(true);
            datePicker.setDisable(true);
            premioField.setDisable(true);
            noteTextArea.setDisable(true);
            callbackToggleButton.setDisable(true);
            callbackDatePicker.setDisable(true);
            new Thread(task1).start();
            if (callbackToggleButton.isSelected()) {
                new Thread(task2).start();
            }
        }
    }

    /**
     * Loads clients once a banker is selected.
     */
    @FXML
    private void bankerSelectedAction() {
        Task task = new Task<Void>() {
            @Override
            public Void call() {
                clientMap = new HashMap<>();
                String id_banker = bankerMap.get(bankerComboBox.getValue());
                try {
                    String query = String.format("id_banker=%s",
                            URLEncoder.encode(id_banker, "UTF-8"));
                    String queryString = "/clients/banker_clients" + "?" + query;
                    ConnectionRequest<Void> req = new ConnectionRequest<>("POST", queryString);
                    List<Client> clients = req.send(new TypeToken<List<Client>>() {
                    }.getType());
                    for (Client c : clients) {
                        String clientFullName = c.nome + " " + c.cognome;
                        String clientId = c.id.toString();
                        clientMap.put(clientFullName, clientId);
                        clientChoices.add(clientFullName);
                    }
                    Platform.runLater(() -> {
                        Collections.sort(clientChoices);
                        clientComboBox.setItems(clientChoices);
                        if (!clientChoices.isEmpty()) {
                            clientComboBox.setDisable(false);
                        }
                    });
                } catch (IOException e) {
                    infoText.setText("Errore comunicazione con il Server.");
                    logger.error(e.getMessage());
                }
                return null;
            }
        };
        clientComboBox.setDisable(true);
        clientChoices = FXCollections.observableArrayList();
        new Thread(task).start();
    }

    /**
     * Navigates to the callbacks list view.
     */
    @FXML
    public void switchToCallbacksList() throws IOException {
        PageSharedId.getInstance().setId(2);
        App.setRoot("callbacksList");
    }

    /**
     * Navigates to the dashboard view.
     */
    @FXML
    private void switchToDashboard() throws IOException{
        PageSharedId.getInstance().setId(null);
        App.setRoot("dashboard");
    }

    /**
     * Navigates to the appointments list view.
     */
    @FXML
    public void switchToAppointmentsList() throws IOException {
        PageSharedId.getInstance().setId(1);
        App.setRoot("appointmentsList");
    }

    /**
     * Reloads the add appointment view.
     */
    @FXML
    public void switchToAddAppointment() throws IOException {
        PageSharedId.getInstance().setId(null);
        App.setRoot("addAppointment");
    }

    /**
     * Navigates to the clients list view.
     */
    @FXML
    public void switchToClients() throws IOException {
        PageSharedId.getInstance().setId(3);
        App.setRoot("clientsList");
    }

    /**
     * Navigates to the bankers list view.
     */
    @FXML
    public void switchToBankers() throws IOException {
        PageSharedId.getInstance().setId(4);
        App.setRoot("bankersList");
    }
}
