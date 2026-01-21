package xyz.nardone.agenda_fps.applicazione.controllers;

import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nardone.agenda_fps.applicazione.*;
import xyz.nardone.agenda_fps.applicazione.utility.Normalize;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for adding a new client.
 */
public class AddClientController {
    @FXML ComboBox<String> bankerComboBox;
    @FXML Text infoText;
    @FXML TextField nome;
    @FXML TextField cognome;
    @FXML Button addClientButton;

    private static final Logger logger = LogManager.getLogger(AddClientController.class);
    private ObservableList<String> bankerChoices;
    private final Map<String, String> bankerMap = new HashMap<>();

    @FXML
    public void initialize() {
        bankerChoices = FXCollections.observableArrayList();
        bankerMap.clear();
        bankerComboBox.setDisable(true);
        addClientButton.setDisable(true);
        try {
            ConnectionRequest<Void> req = new ConnectionRequest<>("GET","/bankers/all");
            List<Banker> bankersList = req.send(new TypeToken<List<Banker>>() {}.getType());
            for (Banker b : bankersList) {
                String bankerId = b.id.toString();
                String bankerFullName = b.cognome + " " + b.nome;
                bankerMap.put(bankerFullName, bankerId);
                bankerChoices.add(bankerFullName);
            }
            Collections.sort(bankerChoices);
            bankerComboBox.setItems(bankerChoices);
            bankerComboBox.setDisable(false);
            addClientButton.setDisable(false);
        } catch (IOException e){
            infoText.setText("Errore comunicazione con il Server.");
            logger.error(e.getMessage());
        }
    }

    @FXML
    private void addClient() {
        if (bankerComboBox.getSelectionModel().getSelectedItem() != null &&
            !nome.getText().trim().isEmpty() &&
            !cognome.getText().trim().isEmpty()){

            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    int bankerId = Integer.parseInt(bankerMap.get(bankerComboBox.getValue()));
                    ClientIds newClient = new ClientIds(Normalize.normalize(nome.getText()),Normalize.normalize(cognome.getText()), bankerId);
                    try {
                        ConnectionRequest<ClientIds> req = new ConnectionRequest<>("PUT","/clients/add", newClient);
                        Response resp = req.send(Response.class);
                        Platform.runLater(() -> {
                           if (resp.status.equals("OK")){
                               bankerComboBox.setDisable(false);
                               nome.setDisable(false);
                               cognome.setDisable(false);
                               addClientButton.setDisable(false);
                           }
                            infoText.setText(resp.message);
                        });
                    } catch (IOException e) {
                        infoText.setText("Errore comunicazione con il Server.");
                        logger.error(e.getMessage());
                    }
                    return null;
                }
            };
            new Thread(task).start();
            bankerComboBox.setDisable(true);
            nome.setDisable(true);
            cognome.setDisable(true);
            addClientButton.setDisable(true);
        }
    }

    @FXML
    public void switchToCallbacksList() throws IOException {
        App.setRoot("callbacksList");
    }

    @FXML
    private void switchToDashboard() throws IOException{
        App.setRoot("dashboard");
    }

    @FXML
    public void switchToAppointmentsList() throws IOException {
        PageSharedId.getInstance().setId(1);
        App.setRoot("appointmentsList");
    }

    @FXML
    public void switchToClients() throws IOException {
        App.setRoot("clientsList");
    }

    @FXML
    public void switchToBankers() throws IOException {
        App.setRoot("bankersList");
    }

}
