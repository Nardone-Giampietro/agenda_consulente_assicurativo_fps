package xyz.nardone.agenda_fps.applicazione.controllers;
import com.google.gson.reflect.TypeToken;
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
import java.util.List;

/**
 * Controller for listing and managing esiti.
 */
public class EsitiListController {

    @FXML private Text infoText;
    @FXML private TableView<Esito> esitiTable = new TableView<>();
    @FXML private ContextMenu contextMenu = new ContextMenu();
    @FXML private MenuItem menuItem = new MenuItem();
    @FXML private MenuItem menuItem1 = new MenuItem();

    private ObservableList<Esito> ol;
    private static final Logger logger = LogManager.getLogger(EsitiListController.class);

    @FXML
    public void initialize() {
        try {
            initializeTable();
            updateEsitiList();
        } catch (IOException e) {
            infoText.setText("Errore comunicazione con il Server.");
            logger.error(e.getMessage());
        }
    }

    private void initializeTable() {
        TableColumn idCol = new TableColumn("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn esitoCol = new TableColumn("polizza");
        esitoCol.setCellValueFactory(new PropertyValueFactory<>("esito"));
        esitiTable.getColumns().addAll(idCol, esitoCol);
        ol = FXCollections.observableArrayList();
        esitiTable.setItems(ol);
        esitiTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    @FXML
    private void updateEsitiList() throws IOException {
        ConnectionRequest<Void> connection = new ConnectionRequest<>("GET", "/esiti/all");
        ol.clear();
        List<Esito> esiti = connection.send(new TypeToken<List<Esito>>() {}.getType());
        ol.addAll(esiti);
    }

    @FXML
    private void switchToDashboard() throws IOException {
        PageSharedId.getInstance().setId(null);
        App.setRoot("dashboard");
    }

    @FXML
    public void switchToUpdate() throws IOException {
        PageSharedId.getInstance().setId(null);
        EsitoSharedId.getInstance().setId(esitiTable.getSelectionModel().getSelectedItem().getId());
        App.setRoot("updateEsito");
    }

    @FXML
    public void remove() {
        Task<Void> task = new Task<>() {
            @Override
            public Void call() {
            if (esitiTable.getSelectionModel().getSelectedItem() != null) {
                try {
                    Integer id = esitiTable.getSelectionModel().getSelectedItem().getId();
                    String query = String.format("id=%s",
                            URLEncoder.encode(id.toString(), "UTF-8"));
                    String queryString = "/esiti/delete" + "?" + query;
                    ConnectionRequest<Void> connection = new ConnectionRequest<>("DELETE", queryString);
                    Response resp = connection.send(Response.class);
                    Platform.runLater(() -> {
                        try {
                            if ("OK".equals(resp.status)) {
                                var selectedItem = esitiTable.getSelectionModel().getSelectedItem();
                                if (selectedItem != null) {
                                    ol.remove(selectedItem);
                                }
                            }
                            infoText.setText(resp.message);
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
    public void switchToSettings() throws IOException {
        PageSharedId.getInstance().setId(5);
        App.setRoot("settings");
    }

    @FXML
    public void switchToAddEsito() throws IOException {
        PageSharedId.getInstance().setId(null);
        App.setRoot("addEsito");
    }
}
