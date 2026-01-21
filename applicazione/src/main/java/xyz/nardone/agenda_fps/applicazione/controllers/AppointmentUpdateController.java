package xyz.nardone.agenda_fps.applicazione.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.util.*;

/**
 * Controller for updating an existing appointment.
 */
public class AppointmentUpdateController {

    @FXML
    private ComboBox<String> bankerComboBox;
    @FXML
    private ComboBox<String> clientComboBox;
    @FXML
    private ChoiceBox<String> appointmentEsitoChoiceBox;
    @FXML
    private ChoiceBox<String> polizzaChoiceBox;
    @FXML
    private DatePicker datePicker;
    @FXML
    private TextField premioField;
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

    private static final Logger logger = LogManager.getLogger(AppointmentUpdateController.class);

    private ObservableList<String> appointmentChoices = FXCollections.observableArrayList();
    private ObservableList<String> polizzaChoices = FXCollections.observableArrayList();
    private ObservableList<String> bankerChoices = FXCollections.observableArrayList();
    private ObservableList<String> clientChoices = FXCollections.observableArrayList();

    private final Map<String, String> bankerMap = new HashMap<>();
    private final Map<String, String> clientMap = new HashMap<>();
    private AppointmentIds appointmentIds;

    @FXML
    public void initialize() {
        try {
            initializeAppointmentIds();
        } catch (IOException e) {
            logger.error("Error initializing appointment IDs: ", e);
            infoText.setText("Errore comunicazione con il Server.");
            return;
        }

        try {
            initializeFields();
        } catch (IOException e) {
            logger.error("Error initializing fields: ", e);
            infoText.setText("Errore comunicazione con il Server.");
        }
    }

    private void initializeFields() throws IOException {
        initializeBankerComboBox();
        initializeEsitoChoiceBox();
        initializePolizzaChoiceBox();
        initializePremioField();
        updateClientComboBox(appointmentIds.id_cliente);
        datePicker.setValue(LocalDate.parse(appointmentIds.data));
        noteTextArea.setText(appointmentIds.note);
    }

    private void initializeAppointmentIds() throws IOException {
        String query = String.format("appointmentId=%s",
                URLEncoder.encode(AppointmentSharedId.getInstance().geId().toString(), "UTF-8"));
        ConnectionRequest<Void> connection = new ConnectionRequest<>("POST", "/appointments/get?" + query);
        appointmentIds = connection.send(AppointmentIds.class);
    }

    private void initializeBankerComboBox() throws IOException {
        String clientBankerFullName = "";
        ConnectionRequest<Void> req = new ConnectionRequest<>("GET", "/bankers/all");
        JsonElement json = req.send(JsonElement.class);
        JsonArray bankers = json.getAsJsonArray();
        List<String> bankerList = new ArrayList<>();
        for (JsonElement elem : bankers) {
            JsonObject b = elem.getAsJsonObject();
            String bankerId = b.get("id").getAsString();
            String bankerFullName = b.get("cognome").getAsString() + " " + b.get("nome").getAsString();

            if (!bankerMap.containsKey(bankerFullName)) {
                bankerMap.put(bankerFullName, bankerId);
                bankerList.add(bankerFullName);
            }

            if (Integer.parseInt(bankerId) == appointmentIds.id_banker) {
                clientBankerFullName = bankerFullName;
            }
        }

        Collections.sort(bankerList);
        bankerChoices.setAll(bankerList);
        bankerComboBox.setItems(bankerChoices);
        bankerComboBox.getSelectionModel().select(clientBankerFullName);
    }

    @FXML
    private void updateClientComboBox(Integer cId) throws IOException {
        clientMap.clear();
        clientChoices.clear();
        String clientBanker = bankerComboBox.getValue();

        if (clientBanker == null) return;

        String id_banker = bankerMap.get(clientBanker);
        if (id_banker == null) return;

        String query = String.format("id_banker=%s", URLEncoder.encode(id_banker, "UTF-8"));
        ConnectionRequest<Void> req = new ConnectionRequest<>("POST", "/clients/banker_clients?" + query);
        List<Client> clients = req.send(new TypeToken<List<Client>>() {
        }.getType());

        List<String> clientList = new ArrayList<>();
        StringBuilder selectedClient = new StringBuilder();

        for (Client c : clients) {
            String clientFullName = c.nome + " " + c.cognome;
            String clientId = c.id.toString();
            clientMap.put(clientFullName, clientId);
            clientList.add(clientFullName);

            if (cId != null && Integer.parseInt(clientId) == cId) {
                selectedClient.append(clientFullName);
            }
        }

        Platform.runLater(() -> {
            Collections.sort(clientList);
            clientChoices.setAll(clientList);
            clientComboBox.setItems(clientChoices);
            clientComboBox.setDisable(clientChoices.isEmpty());
            clientComboBox.getSelectionModel().select(selectedClient.toString());
        });
    }

    private void initializeEsitoChoiceBox() throws IOException {
        ConnectionRequest<Void> req = new ConnectionRequest<>("GET", "/esiti/all");
        JsonElement json = req.send(JsonElement.class);
        JsonArray esiti = json.getAsJsonArray();

        for (JsonElement elem : esiti) {
            String esito = elem.getAsJsonObject().get("esito").getAsString();
            appointmentChoices.add(esito);
        }
        appointmentEsitoChoiceBox.setItems(appointmentChoices);
        appointmentEsitoChoiceBox.getSelectionModel().select(appointmentIds.esito);
    }

    private void initializePolizzaChoiceBox() throws IOException {
        ConnectionRequest<Void> connection = new ConnectionRequest<>("GET", "/polizze/all");
        JsonElement json = connection.send(JsonElement.class);
        JsonArray polizze = json.getAsJsonArray();
        for (JsonElement elem : polizze) {
            String polizzaName = elem.getAsJsonObject().get("polizza").getAsString();
            polizzaChoices.add(polizzaName);
        }
        Collections.sort(polizzaChoices);
        polizzaChoices.add(0, "Nessuna");

        polizzaChoiceBox.setItems(polizzaChoices);
        polizzaChoiceBox.getSelectionModel().select(appointmentIds.polizza.isEmpty() ? "Nessuna" : appointmentIds.polizza);
    }

    private void initializePremioField() {
        premioField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                premioField.setText(oldValue);
            }
        });
        premioField.setText(String.valueOf(appointmentIds.premio));
    }

    @FXML
    public void updateAppointment() {
        Task<Void> task = new Task<>() {
            @Override
            public Void call() {
                try {
                    AppointmentIds appointment = new AppointmentIds(
                            appointmentIds.id, datePicker.getValue().toString(),
                            appointmentEsitoChoiceBox.getValue(), polizzaChoiceBox.getValue(),
                            premioField.getText().isEmpty() ? 0 : Integer.parseInt(premioField.getText().trim()),
                            Integer.parseInt(bankerMap.get(bankerComboBox.getValue())),
                            Integer.parseInt(clientMap.get(clientComboBox.getValue())),
                            noteTextArea.getText().trim()
                    );

                    ConnectionRequest<AppointmentIds> req = new ConnectionRequest<>("PUT", "/appointments/update", appointment);
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
    private void bankerSelectedAction() throws IOException {
        updateClientComboBox(null);
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
        PageSharedId.getInstance().setId(4);
        App.setRoot("bankersList");
    }
}
