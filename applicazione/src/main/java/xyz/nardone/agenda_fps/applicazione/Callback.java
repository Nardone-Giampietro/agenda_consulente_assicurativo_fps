package xyz.nardone.agenda_fps.applicazione;

import java.io.Serializable;

/**
 * Callback DTO used by the JavaFX application.
 */
public class Callback implements Serializable {
    public Integer id;
    public String data;
    public String data_app;
    public String note;
    public Banker banker;
    public Client client;

    public Callback ( Integer id, String data, String data_app, Client client, Banker banker, String note ) {
        this.id = id;
        this.data = data;
        this.data_app = data_app;
        this.note = note;
        this.banker = banker;
        this.client = client;
    }
}
