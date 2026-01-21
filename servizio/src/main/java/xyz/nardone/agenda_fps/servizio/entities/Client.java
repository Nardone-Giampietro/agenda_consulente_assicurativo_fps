package xyz.nardone.agenda_fps.servizio.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.List;

/**
 * Client entity persisted in the clients table.
 */
@Entity
@Table(name="clients")
public class Client implements Serializable{
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name="nome")
    private String nome;
    
    @Column(name="cognome")
    private String cognome;
    
    @ManyToOne
    @JoinColumn(name = "id_banker", nullable = false, foreignKey = @ForeignKey(name = "fk_client_banker"))
    private Banker banker;
    
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Appointment> appointments;

    public void setAppointments(List<Appointment> appointments) {
        this.appointments = appointments;
    }
    
    public Integer getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCognome() {
        return cognome;
    }

    public Banker getBanker() {
        return banker;
    }

    public List<Appointment> getAppointments() {
        return appointments;
    }

    
    
    public void setId(Integer id) {
        this.id = id;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public void setBanker(Banker banker) {
        this.banker = banker;
    }

    public Client(Integer id, String nome, String cognome, Banker banker, List<Appointment> appointments) {
        this.id = id;
        this.nome = nome;
        this.cognome = cognome;
        this.banker = banker;
        this.appointments = appointments;
    }
    
    public Client(){
    }
}
