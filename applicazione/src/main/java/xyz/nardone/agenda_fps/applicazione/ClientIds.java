
package xyz.nardone.agenda_fps.applicazione;

import java.io.Serializable;

/**
 * Client DTO with banker ID for API calls.
 */
public class ClientIds  implements Serializable {
    public String nome;
    public String cognome;
    public Integer id_banker;

    public ClientIds(String nome, String cognome, Integer id_banker) {
        this.nome = nome;
        this.cognome = cognome;
        this.id_banker = id_banker;
    }
}
