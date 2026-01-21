package xyz.nardone.agenda_fps.applicazione;

import java.io.Serializable;

/**
 * Polizza DTO used by the JavaFX application.
 */
public class Polizza implements Serializable {
    private Integer id;
    private String polizza;

    public Integer getId() {
        return id;
    }

    public String getPolizza() {
        return polizza;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setPolizza(String polizza) {
        this.polizza = polizza;
    }

    public Polizza(Integer id, String polizza) {
        this.id = id;
        this.polizza = polizza;
    }

    public Polizza() {}
}
