package xyz.nardone.agenda_fps.applicazione;

import java.io.Serializable;

/**
 * Esito DTO used by the JavaFX application.
 */
public class Esito implements Serializable{
    private Integer id;
    private String esito;

    public Integer getId() {
        return id;
    }

    public String getEsito() {
        return esito;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setEsito(String esito) {
        this.esito = esito;
    }

    public Esito(Integer id, String esito) {
        this.id = id;
        this.esito = esito;
    }
}
