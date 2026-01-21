package xyz.nardone.agenda_fps.servizio.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * Appointment entity persisted in the appointments table.
 */
@Entity
@Table(name = "appointments")
public class Appointment implements Serializable {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer Id;
    
    @Column(name="data")
    private String data;
    
    @Column(name="esito")
    private String esito;
    
    @Column(name="polizza")
    private String polizza;
    
    @Column(name="premio")
    private Integer premio;

    @Column(name="note")
    private String note;
    
    @ManyToOne
    @JoinColumn(name = "id_cliente", nullable = false, foreignKey = @ForeignKey(name = "fk_appointmet_client"))
    private Client client;
    
    @ManyToOne
    @JoinColumn(name = "id_banker", nullable = false, foreignKey = @ForeignKey(name = "fk_appointmet_banker"))
    @JsonIgnore
    private Banker banker;

    public Integer getId() {
        return Id;
    }

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

    public Client getClient() {
        return client;
    }

    public Banker getBanker() {
        return banker;
    }

    public String getNote() {
        return note;
    }

    public void setId(Integer Id) {
        this.Id = Id;
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

    public void setClient(Client client) {
        this.client = client;
    }

    public void setBanker(Banker banker) {
        this.banker = banker;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Appointment(Integer Id, String data, String esito, String polizza, Integer premio, Client client, Banker banker, String note) {
        this.Id = Id;
        this.data = data;
        this.esito = esito;
        this.polizza = polizza;
        this.premio = premio;
        this.client = client;
        this.banker = banker;
        this.note = note;
    }
    
    public Appointment(){
    }
}
