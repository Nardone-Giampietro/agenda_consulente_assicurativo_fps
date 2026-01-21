package xyz.nardone.agenda_fps.applicazione.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Comparator;


/**
 * Controller for listing and managing callbacks.
 */
public class CallbacksListController {
    @FXML TableView<CallbackList> callbacksTable = new TableView<>();
    @FXML Text infoText;
    @FXML private ContextMenu contextMenu = new ContextMenu();
    @FXML private MenuItem menuItem = new MenuItem();
    @FXML public MenuItem menuItemUpdate = new MenuItem();


    private ObservableList<CallbackList> obsList;
    private static final Logger logger = LogManager.getLogger(CallbacksListController.class);

    @FXML
    public void initialize() {
        try{
            initializeTable();
            updateList();
        } catch (IOException e){
            infoText.setText("Errore comunicazione con il Server.");
            logger.error(e.getMessage());
        }         
    }

    private void updateList() throws IOException{
        ConnectionRequest<Void> connection = new ConnectionRequest<>("GET", "/callbacks/all");
        JsonElement json = connection.send(JsonElement.class);
        JsonArray callbacks = json.getAsJsonArray();
        obsList.clear();
        for(int i = 0; i < callbacks.size(); i++) {
            JsonObject c = callbacks.get(i).getAsJsonObject();
            CallbackList newCallback = new CallbackList(
                c.get("id").getAsInt(),
                c.get("data").isJsonNull() ? null : c.get("data").getAsString(),
                c.get("data_app").getAsString(),
                c.get("client").getAsJsonObject().get("banker").getAsJsonObject().get("nome").getAsString(),
                c.get("client").getAsJsonObject().get("banker").getAsJsonObject().get("cognome").getAsString(),
                c.get("client").getAsJsonObject().get("nome").getAsString(),
                c.get("client").getAsJsonObject().get("cognome").getAsString(),
                c.get("note").getAsString()
            );
            obsList.add(newCallback);
        }
        FXCollections.sort(obsList,
                Comparator.comparing(CallbackList::getData, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .reversed()
        );

    }

    private void initializeTable() {
        TableColumn idCol = new TableColumn("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn nameCol = new TableColumn("Nome");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("clientNome"));
        TableColumn lastCol = new TableColumn("Cognome");
        lastCol.setCellValueFactory(new PropertyValueFactory<>("clientCognome"));
        TableColumn bankerNameCol = new TableColumn("Nome FB");
        bankerNameCol.setCellValueFactory(new PropertyValueFactory<>("bankerNome"));
        TableColumn bankerLastCol = new TableColumn("Cognome FB");
        bankerLastCol.setCellValueFactory(new PropertyValueFactory<>("bankerCognome"));
        TableColumn dateCol = new TableColumn("Data Callback");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("data"));
        TableColumn dateAppCol = new TableColumn("Data Appunt.");
        dateAppCol.setCellValueFactory(new PropertyValueFactory<>("data_app"));
        TableColumn noteCol = new TableColumn("Note");
        noteCol.setCellValueFactory(new PropertyValueFactory<>("note"));
        callbacksTable.getColumns().addAll(idCol, dateCol, dateAppCol,lastCol, nameCol,
                bankerLastCol, bankerNameCol, noteCol);
        obsList = FXCollections.observableArrayList();
        callbacksTable.setItems(obsList);

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

        callbacksTable.setFixedCellSize(-1); // Enables dynamic row height
        callbacksTable.refresh();
        callbacksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    @FXML
    private void remove(){
        Task task = new Task<Void>() {
            @Override
            public Void call(){
                if ( callbacksTable.getSelectionModel().getSelectedItem() != null){
                    try{
                        Integer callbackId = callbacksTable.getSelectionModel().getSelectedItem().getId();
                        String query = String.format("callbackId=%s",
                                URLEncoder.encode(callbackId.toString(), "UTF-8"));
                        String urlString = "/callbacks/delete" + "?" + query;
                        ConnectionRequest<Void> connection = new ConnectionRequest<>("DELETE", urlString);
                        Response resp = connection.send(Response.class);
                        Platform.runLater(()-> {
                            try {
                                if ("OK".equals(resp.status)) {
                                    var selectedItem = callbacksTable.getSelectionModel().getSelectedItem();
                                    if (selectedItem != null) {
                                        obsList.remove(selectedItem);
                                    }
                                }
                                infoText.setText(resp.message);
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
        PageSharedId.getInstance().setId(4);
        App.setRoot("bankersList");
    }

    @FXML
    public void switchToAddAppointment() throws IOException {
        PageSharedId.getInstance().setId(null);
        App.setRoot("addAppointment");
    }

    @FXML
    public void switchToCallbackUpdate() throws IOException {
        if (callbacksTable.getSelectionModel().getSelectedItem() != null) {
            CallbackSharedId.getInstance().setId(callbacksTable.getSelectionModel().getSelectedItem().getId());
            PageSharedId.getInstance().setId(null);
            App.setRoot("updateCallback");
        }
    }
}
