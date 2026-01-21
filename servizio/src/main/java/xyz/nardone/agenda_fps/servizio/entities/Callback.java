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
 * Callback entity persisted in the callbacks table.
 */
@Entity
@Table(name = "callbacks")
public class Callback implements Serializable {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;

    @Column(name="data")
    private String data;

    @Column(name="data_app")
    private String data_app;

    @Column(name="note")
    private String note;

    @ManyToOne
    @JoinColumn(name = "id_cliente", nullable = false, foreignKey = @ForeignKey(name = "fk_appointmet_client"))
    private Client client;

    @ManyToOne
    @JoinColumn(name = "id_banker", nullable = false, foreignKey = @ForeignKey(name = "fk_appointmet_banker"))
    private Banker banker;

    public Integer getId() {
        return id;
    }

    public String getData() {
        return data;
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

    public String getData_app() {
        return data_app;
    }

    public void setId(Integer Id) {
        this.id = Id;
    }

    public void setData(String data) {
        this.data = data;
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

    public void setData_app(String data_app) {
        this.data_app = data_app;
    }

    public Callback(Integer id, String data, String data_app,Client client, Banker banker, String note) {
        this.id = id;
        this.data = data;
        this.client = client;
        this.banker = banker;
        this.note = note;
        this.data_app = data_app;
    }

    public Callback(){
    }
}
