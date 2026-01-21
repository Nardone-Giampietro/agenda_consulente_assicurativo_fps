package xyz.nardone.agenda_fps.applicazione.controllers;

import java.io.IOException;

import javafx.fxml.FXML;
import xyz.nardone.agenda_fps.applicazione.App;
import xyz.nardone.agenda_fps.applicazione.ConnectionRequest;
import xyz.nardone.agenda_fps.applicazione.PageSharedId;
import xyz.nardone.agenda_fps.applicazione.Response;
import xyz.nardone.agenda_fps.applicazione.utility.ConfirmationDialog;

/**
 * Controller for application settings and destructive actions.
 */
public class SettingsController {

    @FXML
    public void initialize() {
    }

    @FXML
    public void switchToEsiti() throws IOException {
        PageSharedId.getInstance().setId(null);
        App.setRoot("esitiList");
    }

    @FXML
    public void switchToPolizze() throws IOException {
        PageSharedId.getInstance().setId(null);
        App.setRoot("polizzeList");
    }

    @FXML
    public void deleteAllAppointments() throws IOException {
        if (ConfirmationDialog.show(
                "Conferma cancellazione",
                "Confermi la cancellazioni di tutti gli appuntamenti?\nQuesta operazione sarà irreversibile.",
                "Confermo",
                "Annulla")) {
            try {
                ConnectionRequest<Void> connection = new ConnectionRequest<>("DELETE", "/appointments/delete_all");
                Response response = connection.send(Response.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void deleteAllClients() throws IOException {
        if (ConfirmationDialog.show(
                "Conferma Cancellazione",
                "Confermi la cancellazioni di tutti i clienti?\nQuesto comporterà anche la cancellazione di tutti gli appuntamenti.\nQuesta operazione sarà irreversibile.",
                "Confermo",
                "Annulla")) {
            try {
                ConnectionRequest<Void> connection = new ConnectionRequest<>("DELETE", "/clients/delete_all");
                Response response = connection.send(Response.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
