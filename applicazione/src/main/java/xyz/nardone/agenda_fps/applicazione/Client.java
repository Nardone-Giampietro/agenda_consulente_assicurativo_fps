
package xyz.nardone.agenda_fps.applicazione;

import java.io.Serializable;

/**
 * Client DTO used by the JavaFX application.
 */
public class Client implements Serializable {
    public String nome;
    public String cognome;
    public Integer id;
    public Banker banker;

    public Client(String nome, String cognome, Integer id, Banker banker) {
        this.nome = nome;
        this.cognome = cognome;
        this.id = id;
        this.banker = banker;
    }
}
