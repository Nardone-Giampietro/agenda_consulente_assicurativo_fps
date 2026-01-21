package xyz.nardone.agenda_fps.servizio.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * Esito entity persisted in the esiti table.
 */
@Entity
@Table(name = "esiti")
public class Esito implements Serializable {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;

    @Column(name="esito")
    private String esito;

    public Integer getId() {
        return this.id;
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

    public Esito() {}
}
