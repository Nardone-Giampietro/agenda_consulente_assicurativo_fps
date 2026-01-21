package xyz.nardone.agenda_fps.applicazione.controllers;

import javafx.fxml.FXML;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nardone.agenda_fps.applicazione.*;

import javafx.scene.control.TextField;
import xyz.nardone.agenda_fps.applicazione.utility.Normalize;

import java.io.IOException;
import java.util.Objects;

/**
 * Controller for adding a new banker.
 */
public class AddBankerController {
    @FXML private TextField nome;
    @FXML private TextField cognome;
    @FXML private Text infoText;

    private static final Logger logger = LogManager.getLogger(AddBankerController.class);

    @FXML
    private void addBanker(){
        if(!Objects.equals(nome.getText(), "") && !Objects.equals(cognome.getText(), "")) {
            Banker banker = new Banker(null, Normalize.normalize(nome.getText()), Normalize.normalize(cognome.getText()));
            ConnectionRequest<Banker> req = new ConnectionRequest<>("PUT","/bankers/add", banker);
            try {
                Response resp = req.send(Response.class);
                infoText.setText(resp.message);
            } catch (IOException e) {
                infoText.setText("Errore nella connessione al server. Riprovare");
                logger.error(e.getMessage());
            }
        }
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
