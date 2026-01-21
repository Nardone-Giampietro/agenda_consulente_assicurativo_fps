package xyz.nardone.agenda_fps.applicazione.controllers;

import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nardone.agenda_fps.applicazione.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

/**
 * Controller for listing and managing polizze.
 */
public class PolizzeListController {

    @FXML private Text infoText;
    @FXML private TableView<Polizza> polizzeTable = new TableView<>();
    @FXML private ContextMenu contextMenu = new ContextMenu();
    @FXML private MenuItem menuItem = new MenuItem();
    @FXML private MenuItem menuItem1 = new MenuItem();

    private ObservableList<Polizza> ol;
    private static final Logger logger = LogManager.getLogger(PolizzeListController.class);

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
        TableColumn polizzaCol = new TableColumn("Polizza");
        polizzaCol.setCellValueFactory(new PropertyValueFactory<>("polizza"));
        polizzeTable.getColumns().addAll(idCol, polizzaCol);
        ol = FXCollections.observableArrayList();
        polizzeTable.setItems(ol);
        polizzeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    @FXML
    private void updateEsitiList() throws IOException {
        ConnectionRequest<Void> connection = new ConnectionRequest<>("GET", "/polizze/all");
        ol.clear();
        List<Polizza> polizze = connection.send(new TypeToken<List<Polizza>>() {}.getType());
        ol.addAll(polizze);
    }

    @FXML
    private void switchToDashboard() throws IOException {
        PageSharedId.getInstance().setId(null);
        App.setRoot("dashboard");
    }

    @FXML
    public void switchToUpdate() throws IOException {
        PageSharedId.getInstance().setId(null);
        PolizzaSharedId.getInstance().setId(polizzeTable.getSelectionModel().getSelectedItem().getId());
        App.setRoot("updatePolizza");
    }

    @FXML
    public void remove() {
        Task<Void> task = new Task<>() {
            @Override
            public Void call() {
            if (polizzeTable.getSelectionModel().getSelectedItem() != null) {
                try {
                    Integer id = polizzeTable.getSelectionModel().getSelectedItem().getId();
                    String query = String.format("id=%s",
                            URLEncoder.encode(id.toString(), "UTF-8"));
                    String queryString = "/polizze/delete" + "?" + query;
                    ConnectionRequest<Void> connection = new ConnectionRequest<>("DELETE", queryString);
                    Response resp = connection.send(Response.class);
                    Platform.runLater(() -> {
                        try {
                            if ("OK".equals(resp.status)) {
                                var selectedItem = polizzeTable.getSelectionModel().getSelectedItem();
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
    public void switchToAddPolizza() throws IOException {
        PageSharedId.getInstance().setId(null);
        App.setRoot("addPolizza");
    }
}
