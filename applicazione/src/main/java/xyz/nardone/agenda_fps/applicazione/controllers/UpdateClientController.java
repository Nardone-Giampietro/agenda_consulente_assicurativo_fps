package xyz.nardone.agenda_fps.applicazione.controllers;

import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nardone.agenda_fps.applicazione.*;
import xyz.nardone.agenda_fps.applicazione.utility.Normalize;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Controller for updating client details.
 */
public class UpdateClientController {
    @FXML private TextField nome;
    @FXML private TextField cognome;
    @FXML private ComboBox<String> bankerComboBox;
    @FXML private Text infoText;
    @FXML private Button updateClientButton;


    private static final Logger logger = LogManager.getLogger(UpdateClientController.class);
    private ObservableList<String> bankerChoices;
    private final Map<String, String> bankerMap = new HashMap<>();
    private Client client;
    private Integer clientId = ClientSharedId.getInstance().geId();

    @FXML
    public void initialize() {
        nome.setDisable(true);
        cognome.setDisable(true);
        bankerComboBox.setDisable(true);
        try{
            bankerChoices = FXCollections.observableArrayList();
            String query = String.format("clientId=%s", URLEncoder.encode(clientId.toString(), "UTF-8"));
            ConnectionRequest<Client> reqClient = new ConnectionRequest<>("POST", "/clients/get" + "?" + query);
            client = reqClient.send(Client.class);
            ConnectionRequest<List<Banker>> reqBanker = new ConnectionRequest<>("GET", "/bankers/all");
            List<Banker> bankers = reqBanker.send(new TypeToken<List<Banker>>() {}.getType());
            for (Banker banker : bankers) {
                String bankerId = banker.id.toString();
                String bankerFullName = banker.cognome + " " + banker.nome;
                bankerMap.put(bankerFullName, bankerId);
                bankerChoices.add(bankerFullName);
            }
            bankerComboBox.setItems(bankerChoices);
                Platform.runLater(()-> {
                            nome.setText(client.nome);
                            nome.setDisable(false);
                            cognome.setText(client.cognome);
                            cognome.setDisable(false);
                            bankerComboBox.setValue(client.banker.cognome + " " + client.banker.nome);
                            bankerComboBox.setDisable(false);
                        }
                );
        } catch(IOException e){
            infoText.setText("Errore comunicazione con il Server.");
            logger.error(e.getMessage());
        }
    }

    @FXML
    private void updateClient() throws IOException {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
            if(!Objects.equals(nome.getText().trim(), "") &&
                    !Objects.equals(cognome.getText().trim(), "") &&
                    bankerComboBox.getValue() != null) {
                try {
                    String bankerId = bankerMap.get(bankerComboBox.getValue());
                    String query = String.format("bankerId=%s", URLEncoder.encode(bankerId, "UTF-8"));
                    ConnectionRequest<Banker> reqBanker = new ConnectionRequest<>("POST", "/bankers/get" + "?" + query);
                    Banker banker = reqBanker.send(Banker.class);
                    Client updatedClient = new Client(
                            Normalize.normalize(nome.getText()),
                            Normalize.normalize(cognome.getText()),
                            clientId,
                            banker);
                    ConnectionRequest<Client> req = new ConnectionRequest<>("PUT", "/clients/update", updatedClient);
                    Response response = req.send(Response.class);
                    Platform.runLater(()->{
                        if (response.status.equals("OK")){
                            nome.setDisable(false);
                            cognome.setDisable(false);
                            bankerComboBox.setDisable(false);
                            updateClientButton.setDisable(false);
                        }
                        infoText.setText(response.message);
                    });
                } catch (IOException e) {
                    infoText.setText("Errore nella connessione al server. Riprovare");
                    logger.error(e.getMessage());
                }
            }
            return null;
            }
        };
        new Thread(task).start();
        nome.setDisable(true);
        cognome.setDisable(true);
        bankerComboBox.setDisable(true);
        updateClientButton.setDisable(true);

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
}
