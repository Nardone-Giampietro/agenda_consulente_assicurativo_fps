package xyz.nardone.agenda_fps.applicazione.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nardone.agenda_fps.applicazione.*;

/**
 * Controller for listing and filtering appointments.
 */
public class AppointmentsListController{
    @FXML TableView<Appointment> appointmentsTable = new TableView<>();
    @FXML Text infoText;
    @FXML private ContextMenu contextMenu = new ContextMenu();
    @FXML private MenuItem menuItem = new MenuItem();
    @FXML public MenuItem menuItemVisualize = new MenuItem();
    @FXML private ComboBox<String> BankersComboBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Label appuntamenti;
    @FXML private Label sottoscrizioni;
    @FXML private Label premi;
    private final Map<String, String> bankerMap = new HashMap<>();
    private ObservableList<Appointment> ol;
    private ObservableList<String> bankersChoices;
    private static final Logger logger = LogManager.getLogger(AppointmentsListController.class);

    @FXML
    public void initialize() {
        try{
            initializeTable();
            initializeBankersComboBox();
            updateList();
            updateStatistics();
        } catch (IOException e){
            infoText.setText("Errore comunicazione con il Server.");
            logger.error(e.getMessage());
        }         
    }

    private void updateList() throws IOException{
        String queryString = buildQueryString();
        ConnectionRequest<Void> connection = new ConnectionRequest<>("POST", "/appointments/filter" + queryString);
        JsonElement json = connection.send(JsonElement.class);
        JsonArray appointments = json.getAsJsonArray();
        ol.clear();
        for(int i = 0; i < appointments.size(); i++) {
            JsonObject a = appointments.get(i).getAsJsonObject();
            Appointment newAppointment = new Appointment(
                a.get("id").getAsInt(),
                a.get("data").getAsString(),
                a.get("esito").getAsString(),
                a.get("polizza").getAsString(),
                a.get("premio").getAsInt(),
                a.get("client").getAsJsonObject().get("nome").getAsString(),
                a.get("client").getAsJsonObject().get("cognome").getAsString(),
                a.get("client").getAsJsonObject().get("banker").getAsJsonObject().get("nome").getAsString(),
                a.get("client").getAsJsonObject().get("banker").getAsJsonObject().get("cognome").getAsString(),
                a.get("note").getAsString()
            );
            ol.add(newAppointment);
        }
        FXCollections.sort(ol, Comparator.comparing(Appointment::getData).reversed());
    }

    private void updateStatistics() throws IOException{
        String queryString = buildQueryString();
        ConnectionRequest<Void> connection = new ConnectionRequest<>("POST", "/appointments/statistics" + queryString);
        AppointmentsStatistics statistics = connection.send(AppointmentsStatistics.class);
        premi.setText(statistics.premi.toString());
        sottoscrizioni.setText(statistics.sottoscrizioni.toString());
        appuntamenti.setText(statistics.appuntamenti.toString());
    }

    private String buildQueryString() {
        StringBuilder queryParams = new StringBuilder();
        if (BankersComboBox.getValue() != null && !BankersComboBox.getValue().equals("Tutti") && bankerMap.containsKey(BankersComboBox.getValue())) {
            queryParams.append("bankerId=").append(bankerMap.get(BankersComboBox.getValue())).append("&");}
        if (startDatePicker.getValue() != null) {queryParams.append("startDate=").append(URLEncoder.encode(startDatePicker.getValue().toString(), StandardCharsets.UTF_8)).append("&");}
        if (endDatePicker.getValue() != null) {queryParams.append("endDate=").append(URLEncoder.encode(endDatePicker.getValue().toString(), StandardCharsets.UTF_8)).append("&");}
        return !queryParams.isEmpty() ? "?" + queryParams.substring(0, queryParams.length() - 1) : "";
    }

    private void initializeTable() {
        TableColumn idCol = new TableColumn("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("Id"));
        TableColumn nameCol = new TableColumn("Nome");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("clientNome"));
        TableColumn lastCol = new TableColumn("Cognome");
        lastCol.setCellValueFactory(new PropertyValueFactory<>("clientCognome"));
        TableColumn bankerNameCol = new TableColumn("Nome FB");
        bankerNameCol.setCellValueFactory(new PropertyValueFactory<>("bankerNome"));
        TableColumn bankerLastCol = new TableColumn("Cognome FB");
        bankerLastCol.setCellValueFactory(new PropertyValueFactory<>("bankerCognome"));
        TableColumn dateCol = new TableColumn("Data");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("data"));
        TableColumn esitoCol = new TableColumn("polizza");
        esitoCol.setCellValueFactory(new PropertyValueFactory<>("esito"));
        TableColumn premioCol = new TableColumn("Premio");
        premioCol.setCellValueFactory(new PropertyValueFactory<>("premio"));
        TableColumn polizzaCol = new TableColumn("Polizza");
        polizzaCol.setCellValueFactory(new PropertyValueFactory<>("polizza"));
        TableColumn noteCol = new TableColumn("Note");
        noteCol.setCellValueFactory(new PropertyValueFactory<>("note"));
        appointmentsTable.getColumns().addAll(idCol, dateCol, lastCol, nameCol,
                bankerLastCol, bankerNameCol, esitoCol, premioCol, polizzaCol, noteCol);
        ol = FXCollections.observableArrayList();
        appointmentsTable.setItems(ol);

        noteCol.setPrefWidth(200);
        noteCol.setMaxWidth(250);
        noteCol.setMinWidth(150);

        noteCol.setCellFactory(tc -> {
            TableCell<Appointment, String> cell = new TableCell<>() {
                private final Text text = new Text();

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setGraphic(null);
                    } else {
                        text.setText(item);
                        text.setWrappingWidth(200); // Wraps text within column width
                        text.getStyleClass().add("text");
                        setGraphic(text);
                    }
                }
            };
            return cell;
        });

        appointmentsTable.setFixedCellSize(-1); // Enables dynamic row height
        appointmentsTable.refresh();
        appointmentsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void initializeBankersComboBox() throws IOException{
        bankersChoices = FXCollections.observableArrayList();
        ConnectionRequest<Void> req = new ConnectionRequest<>("GET", "/bankers/all");
        JsonElement json = req.send(JsonElement.class);
        JsonArray bankers = json.getAsJsonArray();
        for(int i = 0; i < bankers.size(); i++){
            JsonObject b = bankers.get(i).getAsJsonObject();
            String bankerId = b.get("id").getAsString();
            String bankerFullName = b.get("cognome").getAsString() + " " + b.get("nome").getAsString();
            bankerMap.put(bankerFullName, bankerId);
            bankersChoices.add(bankerFullName);
        }
        Collections.sort(bankersChoices);
        bankerMap.put("Tutti", null);
        bankersChoices.addFirst("Tutti");
        BankersComboBox.setItems(bankersChoices);
        BankersComboBox.getSelectionModel().selectFirst();

        BankersComboBox.setEditable(true);
        // Add filtering functionality
        TextField editor = BankersComboBox.getEditor();
        BankersComboBox.setOnKeyReleased(event -> {
            String typedText = editor.getText().toLowerCase();
            if (typedText.isEmpty()) {
                BankersComboBox.setItems(bankersChoices); // Reset to full list when empty
            } else {
                ObservableList<String> filteredList = bankersChoices.filtered(item -> item.toLowerCase().contains(typedText));
                BankersComboBox.setItems(filteredList);
                BankersComboBox.show(); // Keeps dropdown open while typing
            }
        });
    }


    @FXML
    private void resetFilter(){
        Task task = new Task<Void>() {
            @Override
            protected Void call(){
                Platform.runLater(() -> {
                    startDatePicker.setValue(null);
                    endDatePicker.setValue(null);
                    BankersComboBox.setItems(bankersChoices);
                    BankersComboBox.getSelectionModel().selectFirst();
                    try{
                        updateList();
                        updateStatistics();
                    } catch (IOException e){
                        infoText.setText("Errore comunicazione con il Server.");
                        logger.error(e.getMessage());
                    }
                });
                return null;
            }
        };
        new Thread(task).start();
    }

    @FXML
    private void applyFilter(){
        Task task = new Task<Void>() {
            @Override
            public Void call(){
                Platform.runLater(()-> {
                    try {
                        updateList();
                        updateStatistics();
                    } catch (IOException e){
                        infoText.setText("Errore comunicazione con il Server.");
                        logger.error(e.getMessage());
                    }
                });
                return null;
            }
        };
        new Thread(task).start();
    }

    @FXML
    private void remove(){
        Task task = new Task<Void>() {
            @Override
            public Void call(){
                if ( appointmentsTable.getSelectionModel().getSelectedItem() != null){
                    try{
                        Integer appointmentId = appointmentsTable.getSelectionModel().getSelectedItem().getId();
                        String query = String.format("appointmentId=%s",
                                URLEncoder.encode(appointmentId.toString(), "UTF-8"));
                        String urlString = "/appointments/delete" + "?" + query;
                        ConnectionRequest<Void> connection = new ConnectionRequest<>("DELETE", urlString);
                        Response resp = connection.send(Response.class);
                        Platform.runLater(()-> {
                            try {
                                if ("OK".equals(resp.status)) {
                                    var selectedItem = appointmentsTable.getSelectionModel().getSelectedItem();
                                    if (selectedItem != null) {
                                        ol.remove(selectedItem);
                                    }
                                    updateStatistics();
                                }
                                infoText.setText(resp.message);
                            } catch (IOException e) {
                                infoText.setText("Errore aggiornamento statistiche.");
                                logger.error("Errore durante l'aggiornamento delle statistiche: " + e.getMessage(), e);
                            } catch (Exception e) {
                                infoText.setText("Si è verificato un errore imprevisto.");
                                logger.error("Errore imprevisto: " + e.getMessage(), e);
                            }
                        }
                        );
                    } catch (IOException e) {
                        infoText.setText("Errore comunicazione con il Server.");
                        logger.error("Errore comunicazione con il server: " + e.getMessage(), e);
                    }
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
    private void switchToDashboard() throws IOException{
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
    public void switchToAppointmentVisualizeAndUpdate() throws IOException {
        PageSharedId.getInstance().setId(null);
        if (appointmentsTable.getSelectionModel().getSelectedItem() != null) {
            AppointmentSharedId.getInstance().setId(appointmentsTable.getSelectionModel().getSelectedItem().getId());
            App.setRoot("updateAppointment");
        }
    }

    @FXML
    public void switchToAddAppointment() throws IOException {
        PageSharedId.getInstance().setId(null);
        App.setRoot("addAppointment");
    }
}
