package xyz.nardone.agenda_fps.applicazione;

import java.io.Serializable;

/**
 * Banker statistics DTO used by the JavaFX application.
 */
public class BankerStatistics implements Serializable {
    public Integer appuntamenti;
    public Integer sottoscrizioni;
    public String bankerName;
    public String bankerCognome;
    public Integer premi;
    public Integer primiAppuntamenti;

    public BankerStatistics(Integer appuntamenti, Integer sottoscrizioni, String bankerName, String bankerCognome, Integer premi, Integer primiAppuntamenti) {
        this.appuntamenti = appuntamenti;
        this.sottoscrizioni = sottoscrizioni;
        this.bankerName = bankerName;
        this.bankerCognome = bankerCognome;
        this.premi = premi;
        this.primiAppuntamenti = primiAppuntamenti;
    }
}
