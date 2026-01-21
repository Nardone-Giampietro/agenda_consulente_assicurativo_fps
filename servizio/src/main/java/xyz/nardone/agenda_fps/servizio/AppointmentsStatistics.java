package xyz.nardone.agenda_fps.servizio;

import java.io.Serializable;

/**
 * Aggregated statistics for a set of appointments.
 */
public class AppointmentsStatistics implements Serializable{
    private Integer Premi;
    private Integer Sottoscrizioni;
    private Integer Appuntamenti;

    public Integer getPremi() {
        return Premi;
    }

    public Integer getSottoscrizioni() {
        return Sottoscrizioni;
    }

    public Integer getAppuntamenti() {
        return Appuntamenti;
    }

    public void setPremi(Integer premi) {
        this.Premi = premi;
    }

    public void setSottoscrizioni(Integer chiusure) {
        this.Sottoscrizioni = chiusure;
    }

    public void setAppuntamenti(Integer appuntamenti) {
        this.Appuntamenti = appuntamenti;
    }

    public AppointmentsStatistics(Integer premi, Integer sottoscrizioni, Integer appuntamenti) {
        Premi = premi;
        Sottoscrizioni = sottoscrizioni;
        Appuntamenti = appuntamenti;
    }

    public AppointmentsStatistics(){
    }
    
}
