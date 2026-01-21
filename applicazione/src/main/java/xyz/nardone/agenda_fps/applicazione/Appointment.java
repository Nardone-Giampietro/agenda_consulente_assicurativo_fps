package xyz.nardone.agenda_fps.applicazione;

/**
 * Client-facing appointment DTO used by the JavaFX app.
 */
public class Appointment {
    private Integer Id;
    private String data;
    private String esito;
    private String polizza;
    private Integer premio;
    private String clientNome;
    private String clientCognome;
    private String bankerNome;
    private String bankerCognome;
    private String note;

    public String getClientNome() {
        return clientNome;
    }

    public String getClientCognome() {
        return clientCognome;
    }

    public String getBankerNome() {
        return bankerNome;
    }

    public String getBankerCognome() {
        return bankerCognome;
    }

    public Integer getId() {
        return Id;
    }

    public String getData() {
        return data;
    }

    public String getEsito() {
        return esito;
    }

    public String getPolizza() {
        return polizza;
    }

    public Integer getPremio() {
        return premio;
    }

    public String getNote() {
        return note;
    }

    public void setClientNome(String clientNome) {
        this.clientNome = clientNome;
    }

    public void setClientCognome(String clientCognome) {
        this.clientCognome = clientCognome;
    }

    public void setBankerNome(String bankerNome) {
        this.bankerNome = bankerNome;
    }

    public void setBankerCognome(String bankerCognome) {
        this.bankerCognome = bankerCognome;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setId(Integer Id) {
        this.Id = Id;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setEsito(String esito) {
        this.esito = esito;
    }

    public void setPolizza(String polizza) {
        this.polizza = polizza;
    }

    public void setPremio(Integer premio) {
        this.premio = premio;
    }

    public Appointment(Integer Id, String data, String esito, String polizza, Integer premio, String clientNome, String clientCognome, String bankerNome, String bankerCognome, String note) {
        this.Id = Id;
        this.data = data;
        this.esito = esito;
        this.polizza = polizza;
        this.premio = premio;
        this.clientNome = clientNome;
        this.clientCognome = clientCognome;
        this.bankerNome = bankerNome;
        this.bankerCognome = bankerCognome;
        this.note = note;
    }
    
    public Appointment(){
    }
}
