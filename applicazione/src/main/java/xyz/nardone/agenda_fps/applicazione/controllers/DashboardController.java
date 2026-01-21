package xyz.nardone.agenda_fps.applicazione.controllers;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nardone.agenda_fps.applicazione.App;
import xyz.nardone.agenda_fps.applicazione.AppointmentsStatistics;
import xyz.nardone.agenda_fps.applicazione.ConnectionRequest;
import xyz.nardone.agenda_fps.applicazione.PageSharedId;
import xyz.nardone.agenda_fps.applicazione.utility.Piechart;

/**
 * Controller for the main dashboard view.
 */
public class DashboardController {
    @FXML
    private Label familyBankers;
    @FXML
    private Label appointments;
    @FXML
    private Label premi;
    @FXML
    private Label sottoscrizioni;
    @FXML
    private PieChart appointmentsPieChart;
    @FXML
    private Label callbacks;

    private final Logger logger = LogManager.getLogger(DashboardController.class);
    private  Piechart piechart;

    @FXML
    public void initialize() {
        appointments.setText("0");
        premi.setText("0");
        sottoscrizioni.setText("0");
        piechart = new Piechart(appointmentsPieChart ,-1, null, null);
        try {
            piechart.inizialize();
            initializeAppointmentsStatistics();
            initializeFamilyBankers();
            initializeTodayCallbacksCount();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void initializeTodayCallbacksCount() throws IOException {
        LocalDate date = LocalDate.now();
        String data = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String query = String.format("data=%s",
                URLEncoder.encode(data, "UTF-8"));
        ConnectionRequest<Void> connection = new ConnectionRequest<>("POST", "/callbacks/today" + "?" + query);
        Integer res = connection.send(Integer.class);
        callbacks.setText(String.valueOf(res));
    }

    private void initializeAppointmentsStatistics() throws IOException {
        ConnectionRequest<Void> req = new ConnectionRequest<>("POST", "/appointments/statistics");
        AppointmentsStatistics statistics = req.send(AppointmentsStatistics.class);
        Platform.runLater(() -> {
            appointments.setText(statistics.appuntamenti.toString());
            premi.setText(statistics.premi.toString());
            sottoscrizioni.setText(statistics.sottoscrizioni.toString());
        });
    }

    private void initializeFamilyBankers() throws IOException {
        ConnectionRequest<Void> connection = new ConnectionRequest<>("GET", "/bankers/count");
        Integer res = connection.send(Integer.class);
        familyBankers.setText(String.valueOf(res));
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
    private void switchToAddAppointments() throws IOException {
        PageSharedId.getInstance().setId(null);
        App.setRoot("addAppointment");
    }

    @FXML
    private void switchToAddClient() throws IOException {
        PageSharedId.getInstance().setId(null);
        App.setRoot("addClient");
    }

    @FXML
    public void switchToAddBanker() throws IOException {
        PageSharedId.getInstance().setId(null);
        App.setRoot("addBanker");
    }

    @FXML
    public void switchToBankerSelection() throws IOException {
        PageSharedId.getInstance().setId(null);
        App.setRoot("bankerSelection");
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
