package xyz.nardone.agenda_fps.applicazione.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;

import java.net.URLEncoder;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nardone.agenda_fps.applicazione.*;

/**
 * Controller for listing and managing clients.
 */
public class ClientsListController {

    @FXML
    TableView<ClientList> clientsTable = new TableView<>();
    @FXML
    ComboBox BankersComboBox;
    @FXML
    Text infoText;
    @FXML
    private ContextMenu contextMenu = new ContextMenu();
    @FXML
    private MenuItem menuItem = new MenuItem();
    @FXML
    private MenuItem menuItem1 = new MenuItem();

    private final Map<String, String> bankerMap = new HashMap<>();
    private ObservableList<ClientList> ol;
    private ObservableList<String> bankersChoices;
    private static final Logger logger = LogManager.getLogger(ClientsListController.class);

    @FXML
    public void initialize() {
        try {
            initializeTable();
            initializeBankersComboBox();
            updateClientsList();
        } catch (IOException e) {
            infoText.setText("Errore comunicazione con il Server.");
            logger.error(e.getMessage());
        }
    }

    private void initializeTable() {
        TableColumn idCol = new TableColumn("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("Id"));
        TableColumn nameCol = new TableColumn("Nome");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("nome"));
        TableColumn lastCol = new TableColumn("Cognome");
        lastCol.setCellValueFactory(new PropertyValueFactory<>("cognome"));
        TableColumn bankerNameCol = new TableColumn("Nome FB");
        bankerNameCol.setCellValueFactory(new PropertyValueFactory<>("bankerNome"));
        TableColumn bankerLastCol = new TableColumn("Cognome FB");
        bankerLastCol.setCellValueFactory(new PropertyValueFactory<>("bankerCognome"));
        clientsTable.getColumns().addAll(idCol, lastCol, nameCol, bankerLastCol, bankerNameCol);
        ol = FXCollections.observableArrayList();
        clientsTable.setItems(ol);
        clientsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void updateClientsList() throws IOException {
        String queryParams = buildQueryString();
        ConnectionRequest<Void> connection = new ConnectionRequest<>("POST", "/clients/banker_clients" + queryParams);
        ol.clear();
        JsonElement json = connection.send(JsonElement.class);
        JsonArray clients = json.getAsJsonArray();
        for (int i = 0; i < clients.size(); i++) {
            JsonObject a = clients.get(i).getAsJsonObject();
            ClientList newClient = new ClientList(
                    a.get("id").getAsInt(),
                    a.get("nome").getAsString(),
                    a.get("cognome").getAsString(),
                    a.get("banker").getAsJsonObject().get("nome").getAsString(),
                    a.get("banker").getAsJsonObject().get("cognome").getAsString()
            );
            ol.add(newClient);
        }
        FXCollections.sort(ol, Comparator.comparing(ClientList::getCognome));
    }

    private void initializeBankersComboBox() throws IOException {
        bankersChoices = FXCollections.observableArrayList();
        ConnectionRequest<Void> req = new ConnectionRequest<>("GET", "/bankers/all");
        JsonElement json = req.send(JsonElement.class);
        JsonArray bankers = json.getAsJsonArray();
        for (int i = 0; i < bankers.size(); i++) {
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

    private String buildQueryString() {
        StringBuilder queryParams = new StringBuilder();
        if (BankersComboBox.getValue() != null && !BankersComboBox.getValue().equals("Tutti") && bankerMap.containsKey(BankersComboBox.getValue())) {
            queryParams.append("id_banker=").append(bankerMap.get(BankersComboBox.getValue())).append("&");
        }
        return !queryParams.isEmpty() ? "?" + queryParams.substring(0, queryParams.length() - 1) : "";
    }

    @FXML
    private void remove() {
        Task<Void> task = new Task<>() {
            @Override
            public Void call() {
                if (clientsTable.getSelectionModel().getSelectedItem() != null) {
                    try {
                        Integer clienteId = clientsTable.getSelectionModel().getSelectedItem().getId();
                        String query = String.format("clienteId=%s",
                                URLEncoder.encode(clienteId.toString(), "UTF-8"));
                        String queryString = "/clients/delete" + "?" + query;
                        ConnectionRequest<Void> connection = new ConnectionRequest<>("DELETE", queryString);
                        Response resp = connection.send(Response.class);
                        Platform.runLater(() -> {
                            try {
                                if ("OK".equals(resp.status)) {
                                    var selectedItem = clientsTable.getSelectionModel().getSelectedItem();
                                    if (selectedItem != null) {
                                        ol.remove(selectedItem);
                                    }
                                    updateClientsList();
                                }
                                infoText.setText(resp.message);
                            } catch (IOException e) {
                                infoText.setText("Errore nella rimozione client.");
                                logger.error("Errore durante la rimozione del cliente: " + e.getMessage(), e);
                            } catch (Exception e) {
                                infoText.setText("Si è verificato un errore imprevisto.");
                                logger.error("Errore imprevisto: " + e.getMessage(), e);
                            }
                        });
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
    private void applyFilter() {
        Task task = new Task<Void>() {
            @Override
            public Void call() {
                Platform.runLater(() -> {
                    try {
                        updateClientsList();
                    } catch (IOException e) {
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
    private void resetFilter() {
        Task task = new Task<Void>() {
            @Override
            protected Void call() {
                Platform.runLater(() -> {
                    BankersComboBox.setItems(bankersChoices);
                    BankersComboBox.getSelectionModel().selectFirst();
                    try {
                        updateClientsList();
                    } catch (IOException e) {
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
    private void switchToUpdate() throws IOException {
        if (clientsTable.getSelectionModel().getSelectedItem() != null) {
            PageSharedId.getInstance().setId(null);
            ClientSharedId.getInstance().setId(clientsTable.getSelectionModel().getSelectedItem().getId());
            App.setRoot("updateClient");
        }
    }

    @FXML
    public void switchToCallbacksList() throws IOException {
        PageSharedId.getInstance().setId(2);
        App.setRoot("callbacksList");
    }

    @FXML
    private void switchToDashboard() throws IOException {
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
