package xyz.nardone.agenda_fps.applicazione;

import java.io.Serializable;

/**
 * Client list item used for table views.
 */
public class ClientList implements Serializable{
    private Integer id;
    private String nome;
    private String cognome;
    private String bankerNome;
    private String bankerCognome;

    public Integer getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCognome() {
        return cognome;
    }

    public String getBankerNome() {
        return bankerNome;
    }

    public String getBankerCognome() {
        return bankerCognome;
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

    public void setBankerNome(String bankerNome) {
        this.bankerNome = bankerNome;
    }

    public void setBankerCognome(String bankerCognome) {
        this.bankerCognome = bankerCognome;
    }

    public ClientList(Integer id, String nome, String cognome, String bankerNome, String bankerCognome) {
        this.id = id;
        this.nome = nome;
        this.cognome = cognome;
        this.bankerNome = bankerNome;
        this.bankerCognome = bankerCognome;
    }

    
    
}
