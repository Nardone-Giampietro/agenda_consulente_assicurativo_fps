package xyz.nardone.agenda_fps.servizio.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.List;

/**
 * Banker entity persisted in the bankers table.
 */
@Entity
@Table(name="bankers")
public class Banker implements Serializable {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name="nome")
    private String nome;
    
    @Column(name="cognome")
    private String cognome;

    @OneToMany(mappedBy = "banker", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Client> clients;
    
    @OneToMany(mappedBy = "banker", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Appointment> appointments;

    public List<Client> getClients() {
        return clients;
    }

    public List<Appointment> getAppointments() {
        return appointments;
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

    public void setAppointments(List<Appointment> appointments) {
        this.appointments = appointments;
    }
    
    public void setClients(List<Client> clients) {
        this.clients = clients;
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

    public Banker(Integer id, String nome, String cognome, List<Client> clients, List<Appointment> appointments) {
        this.id = id;
        this.nome = nome;
        this.cognome = cognome;
        this.clients = clients;
        this.appointments = appointments;
    }
    
    public Banker(){
    }
}
