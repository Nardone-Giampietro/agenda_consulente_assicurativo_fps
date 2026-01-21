package xyz.nardone.agenda_fps.servizio;

import java.io.Serializable;

/**
 * DTO for client creation tied to a banker ID.
 */
public class ClientIds  implements Serializable {
    private String nome;
    private String cognome;
    private Integer id_banker;

    public String getNome() {
        return nome;
    }

    public String getCognome() {
        return cognome;
    }

    public Integer getId_banker() {
        return id_banker;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public void setId_banker(Integer id_banker) {
        this.id_banker = id_banker;
    }

    public ClientIds(String nome, String cognome, Integer id_banker) {
        this.nome = nome;
        this.cognome = cognome;
        this.id_banker = id_banker;
    }

    public ClientIds() {
    }
    
    
}
