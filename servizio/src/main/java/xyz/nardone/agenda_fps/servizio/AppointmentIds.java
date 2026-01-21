package xyz.nardone.agenda_fps.servizio;

import java.io.Serializable;

/**
 * DTO for appointment data using explicit banker and client IDs.
 */
public class AppointmentIds implements Serializable {
    private Integer id;
    private String data;
    private String esito;
    private String polizza;
    private String note;
    private Integer premio;
    private Integer id_banker;
    private Integer id_cliente;

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

    public Integer getId_banker() {
        return id_banker;
    }

    public Integer getId_cliente() {
        return id_cliente;
    }

    public String getNote(){
        return note;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public void setId_banker(Integer id_banker) {
        this.id_banker = id_banker;
    }

    public void setId_cliente(Integer id_cliente) {
        this.id_cliente = id_cliente;
    }

    public void setNote(String note) {
        this.note = note;
    }


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
    
    public AppointmentIds(){
    }
    
    
}
