package xyz.nardone.agenda_fps.servizio;

import java.io.Serializable;

/**
 * Aggregated statistics for a banker, optionally within a date range.
 */
public class BankerStatistics implements Serializable {
    private Integer appuntamenti;
    private Integer sottoscrizioni;
    private String bankerName;
    private String bankerCognome;
    private Integer premi;
    private Integer primiAppuntamenti;

    public Integer getAppuntamenti() {
        return appuntamenti;
    }

    public Integer getSottoscrizioni() {
        return sottoscrizioni;
    }

    public String getBankerName() {
        return bankerName;
    }

    public String getBankerCognome() {
        return bankerCognome;
    }

    public Integer getPremi() {
        return premi;
    }

    public Integer getPrimiAppuntamenti() {
        return primiAppuntamenti;
    }

    public void setAppuntamenti(Integer appuntamenti) {
        this.appuntamenti = appuntamenti;
    }

    public void setSottoscrizioni(Integer sottoscrizioni) {
        this.sottoscrizioni = sottoscrizioni;
    }

    public void setBankerName(String bankerName) {
        this.bankerName = bankerName;
    }

    public void setBankerCognome(String bankerCognome) {
        this.bankerCognome = bankerCognome;
    }

    public void setPremi(Integer premi) {
        this.premi = premi;
    }

    public void setPrimiAppuntamenti(Integer primiAppuntamenti) {
        this.primiAppuntamenti = primiAppuntamenti;
    }

    public BankerStatistics(Integer appuntamenti, Integer sottoscrizioni, String bankerName, String bankerCognome, Integer premi, Integer primiAppuntamenti) {
        this.appuntamenti = appuntamenti;
        this.sottoscrizioni = sottoscrizioni;
        this.bankerName = bankerName;
        this.bankerCognome = bankerCognome;
        this.premi = premi;
        this.primiAppuntamenti = primiAppuntamenti;
    }

    public BankerStatistics() {}
}
