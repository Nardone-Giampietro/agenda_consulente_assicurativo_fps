package xyz.nardone.agenda_fps.applicazione.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Comparator;

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
 * Controller for listing and managing bankers.
 */
public class BankersListController {

    @FXML
    TableView<BankerList> bankersTable = new TableView<>();
    @FXML
    Text infoText;
    @FXML
    private ContextMenu contextMenu = new ContextMenu();
    @FXML
    private MenuItem menuItem = new MenuItem();


    private ObservableList<BankerList> ol;
    private static final Logger logger = LogManager.getLogger(ClientsListController.class);

    @FXML
    public void initialize() {
        try {
            initializeTable();
        } catch (IOException e) {
            infoText.setText("Errore comunicazione con il Server.");
            logger.error(e.getMessage());
        }
    }

    private void initializeTable() throws IOException {
        TableColumn idCol = new TableColumn("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("Id"));
        TableColumn nameCol = new TableColumn("Nome");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("nome"));
        TableColumn lastCol = new TableColumn("Cognome");
        lastCol.setCellValueFactory(new PropertyValueFactory<>("cognome"));
        bankersTable.getColumns().addAll(idCol, lastCol, nameCol);
        ol = FXCollections.observableArrayList();
        bankersTable.setItems(ol);

        ConnectionRequest<Void> connection = new ConnectionRequest("GET", "/bankers/all");
        JsonElement json = connection.send(JsonElement.class);
        JsonArray clients = json.getAsJsonArray();
        for (int i = 0; i < clients.size(); i++) {
            JsonObject a = clients.get(i).getAsJsonObject();
            BankerList newBanker = new BankerList(
                    a.get("id").getAsInt(),
                    a.get("nome").getAsString(),
                    a.get("cognome").getAsString()
            );
            ol.add(newBanker);
        }
        bankersTable.setFixedCellSize(-1); // Enables dynamic row height
        bankersTable.refresh();
        bankersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        FXCollections.sort(ol, Comparator.comparing(BankerList::getCognome));
    }

    @FXML
    private void remove() {
        Task task = new Task<Void>() {
            @Override
            public Void call() {
                if (bankersTable.getSelectionModel().getSelectedItem() != null) {
                    try {
                        Integer bankerId = bankersTable.getSelectionModel().getSelectedItem().getId();
                        String query = String.format("bankerId=%s",
                                URLEncoder.encode(bankerId.toString(), "UTF-8"));
                        String urlString = "/bankers/delete" + "?" + query;
                        ConnectionRequest<Void> connection = new ConnectionRequest<>("DELETE", urlString);
                        Response resp = connection.send(Response.class);
                        Platform.runLater(() -> {
                                    if (resp.status.equals("OK")) {
                                        ol.remove(bankersTable.getSelectionModel().getSelectedItem());
                                    }
                                    infoText.setText(resp.message);
                                }
                        );
                    } catch (IOException e) {
                        infoText.setText("Errore comunicazione con il Server.");
                        logger.error(e.getMessage());
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
    private void switchToUpdateBanker() throws IOException {
        if (bankersTable.getSelectionModel().getSelectedItem() != null) {
            PageSharedId.getInstance().setId(null);
            BankerSharedId.getInstance().setId(bankersTable.getSelectionModel().getSelectedItem().getId());
            App.setRoot("updateBanker");
        }
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
