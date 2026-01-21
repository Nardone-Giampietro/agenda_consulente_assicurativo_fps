
package xyz.nardone.agenda_fps.applicazione.controllers;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nardone.agenda_fps.applicazione.*;
import xyz.nardone.agenda_fps.applicazione.utility.Piechart;


/**
 * Controller for rendering banker statistics and the pie chart.
 */
public class BankerStatisticsController {
    @FXML public Label cognome;
    @FXML public Label nome;
    @FXML public GridPane statisticsTable;
    @FXML PieChart pieChart;
    @FXML Label name;
    @FXML Label dates;
    @FXML private Label labelNome;
    @FXML private Label labelCognome;
    @FXML private Label labelAppuntamenti;
    @FXML private Label labelSottoscrizioni;
    @FXML private Label labelPrimiAppuntamenti;
    @FXML private Label labelPremi;
    private static final Logger logger = LogManager.getLogger(BankerStatisticsController.class);
    private BankerStatistics bankerStatistics;
    private Integer bankerId;
    private String startDate;
    private String endDate;
    private Piechart piechart;

    @FXML
    public void initialize() {
        initializeBankersIdAndDates();
        piechart = new Piechart(pieChart,bankerId, startDate, endDate);
        try{
            piechart.inizialize();
            updateStatisticsTable();
            setHeader();
        } catch(IOException e){
            logger.error(e.getMessage());
        }
    }

    private void initializeBankersIdAndDates() {
        bankerId = BankerSharedIdAndDates.getInstance().geId();
        startDate = BankerSharedIdAndDates.getInstance().getStartDate();
        endDate = BankerSharedIdAndDates.getInstance().getEndDate();
    }

    private void updateStatisticsTable() {
        bankerStatistics = piechart.getBankerStatistics();
        // Update Labels (Table)
        if (!bankerId.equals(-1)){
            labelNome.setText(bankerStatistics.bankerName);
            labelCognome.setText(bankerStatistics.bankerCognome);
        } else {
            statisticsTable.getChildren().remove(nome);
            statisticsTable.getChildren().remove(cognome);
            statisticsTable.getChildren().remove(labelNome);
            statisticsTable.getChildren().remove(labelCognome);
        }
        labelAppuntamenti.setText(String.valueOf(bankerStatistics.appuntamenti));
        labelSottoscrizioni.setText(String.valueOf(bankerStatistics.sottoscrizioni));
        labelPrimiAppuntamenti.setText(String.valueOf(bankerStatistics.primiAppuntamenti));
        labelPremi.setText(String.valueOf(bankerStatistics.premi) + " €");
    }


    private void setHeader(){
        if (!bankerId.equals(-1)){
            name.setText(bankerStatistics.bankerCognome + " " + bankerStatistics.bankerName);
        }
        StringBuffer date = new StringBuffer();
        date.append("");
        date.append("Statistiche ");
        if (BankerSharedIdAndDates.getInstance().getStartDate() != null && BankerSharedIdAndDates.getInstance().getEndDate() != null) {
            date.append("a partire da " + BankerSharedIdAndDates.getInstance().getStartDate() + " fino a " + BankerSharedIdAndDates.getInstance().getEndDate());
        } else if (BankerSharedIdAndDates.getInstance().getStartDate() != null) {
            date.append("a partire da " + BankerSharedIdAndDates.getInstance().getStartDate());
        } else if (BankerSharedIdAndDates.getInstance().getEndDate() != null) {
            date.append("fino a " + BankerSharedIdAndDates.getInstance().getEndDate());
        } else {
            date.append(" totali");
        }
        dates.setText(date.toString());
    }

    @FXML
    public void switchToCallbacksList() throws IOException {
        PageSharedId.getInstance().setId(2);
        App.setRoot("callbacksList");
    }

    @FXML
    private void switchToBankerSelection() throws IOException {
        PageSharedId.getInstance().setId(null);
        App.setRoot("bankerSelection");
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
