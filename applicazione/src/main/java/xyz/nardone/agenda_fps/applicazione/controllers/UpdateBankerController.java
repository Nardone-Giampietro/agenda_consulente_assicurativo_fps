package xyz.nardone.agenda_fps.applicazione.controllers;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nardone.agenda_fps.applicazione.*;
import xyz.nardone.agenda_fps.applicazione.utility.Normalize;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Objects;

/**
 * Controller for updating banker details.
 */
public class UpdateBankerController {
    @FXML private TextField nome;
    @FXML private TextField cognome;
    @FXML private Text infoText;
    @FXML private Button updateBankerButton;


    private static final Logger logger = LogManager.getLogger(UpdateBankerController.class);
    private int port = ServerPort.get().getPort();
    private Integer bankerId = BankerSharedId.getInstance().geId();
    private String urlBase = "http://localhost:" + port;

    @FXML
    public void initialize() {
        nome.setDisable(true);
        cognome.setDisable(true);
        try{
            String query = String.format("bankerId=%s", URLEncoder.encode(bankerId.toString(), "UTF-8"));
            String urlString = urlBase + "/bankers/get" + "?" + query;
            URL url = new URL(urlString);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));){

                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                con.disconnect();

                Gson gson = new Gson();
                Banker banker = gson.fromJson(response.toString(), Banker.class);
                Platform.runLater(()-> {
                    nome.setText(banker.nome);
                    nome.setDisable(false);
                    cognome.setText(banker.cognome);
                    cognome.setDisable(false);
                        }
                );
            }
        } catch(IOException e){
            infoText.setText("Errore comunicazione con il Server.");
            logger.error(e.getMessage());
        }
    }

    @FXML
    private void updateBanker() throws IOException {
        Task<Void> task = new Task() {
            @Override
            protected Void call() throws Exception {
                if(!Objects.equals(nome.getText().trim(), "") && !Objects.equals(cognome.getText().trim(), "")) {
                    Banker banker = new Banker(bankerId, Normalize.normalize(nome.getText()), Normalize.normalize(cognome.getText()));
                    ConnectionRequest<Banker> req = new ConnectionRequest<>("PUT","/bankers/update", banker);
                    try {
                        Response resp = req.send(Response.class);
                        Platform.runLater(()->{
                            if (resp.status.equals("OK")){
                                nome.setDisable(false);
                                cognome.setDisable(false);
                                updateBankerButton.setDisable(false);
                            }
                            infoText.setText(resp.message);
                        });
                    } catch (IOException e) {
                        infoText.setText("Errore nella connessione al server. Riprovare");
                        logger.error(e.getMessage());
                    }
                }
                return null;
            }
        };
        nome.setDisable(true);
        cognome.setDisable(true);
        updateBankerButton.setDisable(true);
        new Thread(task).start();
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
