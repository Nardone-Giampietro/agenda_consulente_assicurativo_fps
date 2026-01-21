package xyz.nardone.agenda_fps.applicazione;

import java.io.Serializable;

/**
 * Callback DTO with banker and client IDs for API calls.
 */
public class CallbackIds implements Serializable {
    public Integer id;
    public String data;
    public String data_app;
    public String nota;
    public Integer id_banker;
    public Integer id_cliente;

    public CallbackIds(Integer id, String nota, String data, String data_app, Integer id_banker, Integer client_id) {
        this.id = id;
        this.data = data;
        this.data_app = data_app;
        this.nota = nota;
        this.id_banker = id_banker;
        this.id_cliente = client_id;
    }

}
