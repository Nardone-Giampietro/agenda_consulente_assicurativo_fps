package xyz.nardone.agenda_fps.applicazione;

import java.io.Serializable;

/**
 * Callback list item used for table views.
 */
public class CallbackList implements Serializable {
    private Integer id;
    private String data;
    private String data_app;
    private String note;
    private String bankerNome;
    private String bankerCognome;
    private String clientNome;
    private String clientCognome;

    public Integer getId() {
        return id;
    }

    public String getData() {
        return data;
    }

    public String getData_app() {
        return data_app;
    }

    public String getNote() {
        return note;
    }

    public String getBankerNome() {
        return bankerNome;
    }

    public String getBankerCognome() {
        return bankerCognome;
    }

    public String getClientNome() {
        return clientNome;
    }

    public String getClientCognome() {
        return clientCognome;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setData_app(String data_app) {
        this.data_app = data_app;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setBankerNome(String bankerNome) {
        this.bankerNome = bankerNome;
    }

    public void setBankerCognome(String bankerCognome) {
        this.bankerCognome = bankerCognome;
    }

    public void setClientNome(String clientNome) {
        this.clientNome = clientNome;
    }

    public void setClientCognome(String clientCognome) {
        this.clientCognome = clientCognome;
    }

    public CallbackList(Integer id, String data, String data_app, String bankerNome, String bankerCognome, String clientNome, String clientCognome, String note) {
        this.id = id;
        this.data = data;
        this.data_app = data_app;
        this.note = note;
        this.bankerNome = bankerNome;
        this.bankerCognome = bankerCognome;
        this.clientNome = clientNome;
        this.clientCognome = clientCognome;
    }

    public CallbackList() {}
}
