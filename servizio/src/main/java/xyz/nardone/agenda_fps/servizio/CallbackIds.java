package xyz.nardone.agenda_fps.servizio;

import java.io.Serializable;

/**
 * DTO for callback data using explicit banker and client IDs.
 */
public class CallbackIds implements Serializable {
    private Integer id;
    private String nota;
    private String data;
    private String data_app;
    private Integer id_banker;
    private Integer id_cliente;

    public Integer getId() {
        return id;
    }

    public String getNota() {
        return nota;
    }

    public String getData() {
        return data;
    }

    public String getData_app() {
        return data_app;
    }

    public Integer getId_banker() {
        return id_banker;
    }

    public Integer getId_cliente() {
        return id_cliente;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setData_app(String data_app) {
        this.data_app = data_app;
    }

    public void setId_banker(Integer id_banker) {
        this.id_banker = id_banker;
    }

    public void setId_cliente(Integer id_cliente) {
        this.id_cliente = id_cliente;
    }

    public CallbackIds(Integer id, String nota, String data, String data_app, Integer id_banker, Integer id_cliente) {
        this.id = id;
        this.nota = nota;
        this.data = data;
        this.data_app = data_app;
        this.id_banker = id_banker;
        this.id_cliente = id_cliente;
    }

    public CallbackIds() {}
}
