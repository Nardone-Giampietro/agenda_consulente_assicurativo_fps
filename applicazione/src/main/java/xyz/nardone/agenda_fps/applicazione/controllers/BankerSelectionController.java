package xyz.nardone.agenda_fps.applicazione.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nardone.agenda_fps.applicazione.App;
import xyz.nardone.agenda_fps.applicazione.BankerSharedIdAndDates;
import xyz.nardone.agenda_fps.applicazione.ConnectionRequest;
import xyz.nardone.agenda_fps.applicazione.PageSharedId;

/**
 * Controller for selecting a banker and optional date range.
 */
public class BankerSelectionController{
    
    @FXML ComboBox<String> bankerComboBox;
    @FXML DatePicker datePickerStart;
    @FXML DatePicker datePickerEnd;
    @FXML Button nextButton;
    @FXML Text infoText;

    
    private static final Logger logger = LogManager.getLogger(BankerSelectionController.class);
    private ObservableList<String> bankerChoices;
    private Map<String, String> bankerMap;

    @FXML
    public void initialize() {
        nextButton.setDisable(true);
        bankerChoices = FXCollections.observableArrayList();
        bankerMap = new HashMap<>();
        try{
            initializeBankerComboBox();
        } catch (IOException e){
            infoText.setText("Errore comunicazione con il Server.");
            logger.error(e.getMessage());
        } 
    }

    private void initializeBankerComboBox() throws IOException{
        ConnectionRequest<Void> connection = new ConnectionRequest<>("GET", "/bankers/all");
        JsonElement json = connection.send(JsonElement.class);
        JsonArray bankers = json.getAsJsonArray();
        for(int i = 0; i < bankers.size(); i++){
            JsonObject b = bankers.get(i).getAsJsonObject();
            String bankerId = b.get("id").getAsString();
            String bankerFullName = b.get("cognome").getAsString() + " "
                    + b.get("nome").getAsString();
            bankerMap.put(bankerFullName, bankerId);
            bankerChoices.add(bankerFullName);
        }
        Collections.sort(bankerChoices);
        bankerChoices.addFirst("Tutti");
        bankerComboBox.setItems(bankerChoices);
    }
    
    @FXML
    private void bankerSelectedAction(){
        Integer id_banker = -1;
        if (!bankerComboBox.getValue().equals("Tutti")){
            id_banker = Integer.valueOf(bankerMap.get(bankerComboBox.getValue()));
        }
        BankerSharedIdAndDates.getInstance().setId(id_banker);
        nextButton.setDisable(false);
    }

    @FXML
    public void switchToCallbacksList() throws IOException {
        App.setRoot("callbacksList");
    }
    
    @FXML
    private void nextAction() throws IOException{
        BankerSharedIdAndDates.getInstance().setEndDate(datePickerEnd.getValue() == null ? null : datePickerEnd.getValue().toString());
        BankerSharedIdAndDates.getInstance().setStartDate(datePickerStart.getValue() == null ? null : datePickerStart.getValue().toString());
        App.setRoot ("bankerStatistics");
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
