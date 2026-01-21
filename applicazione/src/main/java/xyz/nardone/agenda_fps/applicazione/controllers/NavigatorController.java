package xyz.nardone.agenda_fps.applicazione.controllers;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nardone.agenda_fps.applicazione.App;
import xyz.nardone.agenda_fps.applicazione.ConnectionRequest;
import xyz.nardone.agenda_fps.applicazione.PageSharedId;

import java.io.IOException;

/**
 * Controller for the main navigation sidebar.
 */
public class NavigatorController {
    public Button settingsButton;
    @FXML
    private Label callbackCountLabel;
    @FXML
    private Button appointmentsListButton;
    @FXML
    private Button callbacksListButton;
    @FXML
    private Button clientsListButton;
    @FXML
    private Button dashboardButton;
    @FXML
    private Button bankersListButton;

    private final Logger logger = LogManager.getLogger(NavigatorController.class);

    @FXML
    public void initialize() {
        try {
            initializeButtonsSelection();
            updateCallbacksCount();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void initializeButtonsSelection() throws IOException {
        if (PageSharedId.getInstance().geId() != null) {
            switch (PageSharedId.getInstance().geId()) {
            case 1:
                appointmentsListButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("pressed"), true);
                break;
            case 2:
                callbacksListButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("pressed"), true);
                break;
            case 3:
                clientsListButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("pressed"), true);
                break;
            case 4:
                bankersListButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("pressed"), true);
                break;
            case 5:
                settingsButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("pressed"), true);
                break;
            }
        } else {
            resetButtonsSelection();
        }
    }

    private void updateCallbacksCount() throws IOException {
        ConnectionRequest<Void> connection = new ConnectionRequest<>("GET", "/callbacks/count");
        Integer res = connection.send(Integer.class);
        if (res != 0) {
            callbackCountLabel.setText(String.valueOf(res));

        } else {
            callbackCountLabel.setManaged(false);
            callbackCountLabel.setVisible(false);
        }
    }

    private void resetButtonsSelection() throws IOException {
        appointmentsListButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("pressed"), false);
        bankersListButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("pressed"), false);
        callbacksListButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("pressed"), false);
        clientsListButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("pressed"), false);
        settingsButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("pressed"), false);
    }

    @FXML
    public void switchToDashboard() throws IOException {
        PageSharedId.getInstance().setId(null);
        App.setRoot("dashboard");
    }


    @FXML
    public void switchToCallbacksList() throws IOException {
        PageSharedId.getInstance().setId(2);
        App.setRoot("callbacksList");
    }

    @FXML
    private void switchToAppointmentsList() throws IOException {
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
    public void switchToSettings() throws IOException {
        PageSharedId.getInstance().setId(5);
        App.setRoot("settings");
    }
}

