package xyz.nardone.agenda_fps.applicazione;

import java.io.Serializable;

/**
 * Appointment statistics DTO used by the JavaFX application.
 */
public class AppointmentsStatistics implements Serializable{
    public Integer premi;
    public Integer sottoscrizioni;
    public Integer appuntamenti;

    public AppointmentsStatistics(Integer premi, Integer sottoscrizioni, Integer appuntamenti) {
        this.premi = premi;
        this.sottoscrizioni = sottoscrizioni;
        this.appuntamenti = appuntamenti;
    }
}
