package xyz.nardone.agenda_fps.applicazione;

import java.io.Serializable;

/**
 * Banker DTO used by the JavaFX application.
 */
public class Banker implements Serializable{
    public Integer id = null;
    public String nome;
    public String cognome;

    public Banker(Integer id, String nome, String cognome) {
        this.id = id;
        this.nome = nome;
        this.cognome = cognome;
    }
}
