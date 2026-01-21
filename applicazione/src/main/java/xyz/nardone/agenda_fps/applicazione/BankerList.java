package xyz.nardone.agenda_fps.applicazione;

import java.io.Serializable;

/**
 * Banker list item used for table views.
 */
public class BankerList implements Serializable{
    private Integer id;
    private String nome;
    private String cognome;

    public Integer getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCognome() {
        return cognome;
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

    public BankerList(Integer id, String nome, String cognome) {
        this.id = id;
        this.nome = nome;
        this.cognome = cognome;
    }

    public BankerList() {
    }

}
