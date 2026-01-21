package xyz.nardone.agenda_fps.servizio.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * Polizza entity persisted in the polizze table.
 */
@Entity
@Table(name = "polizze")
public class Polizza implements Serializable {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;

    @Column(name="polizza")
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
