package xyz.nardone.agenda_fps.applicazione;

import java.io.Serializable;

/**
 * Appointment DTO with banker and client IDs for API calls.
 */
public class AppointmentIds implements Serializable {
    public Integer id;
    public String data;
    public String esito;
    public String polizza;
    public String note;
    public Integer premio;
    public Integer id_banker;
    public Integer id_cliente;

    public AppointmentIds(Integer id, String data, String esito, String polizza, Integer premio, Integer id_banker, Integer id_cliente, String note) {
        this.id = id;
        this.data = data;
        this.esito = esito;
        this.polizza = polizza;
        this.premio = premio;
        this.id_banker = id_banker;
        this.id_cliente = id_cliente;
        this.note = note;
    }    
}
