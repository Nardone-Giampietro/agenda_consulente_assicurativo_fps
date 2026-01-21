package xyz.nardone.agenda_fps.applicazione;

/**
 * Singleton holder for the currently selected client ID.
 */
public class ClientSharedId {
    private static final ClientSharedId instance = new ClientSharedId();

    private Integer id;

    private ClientSharedId() {}

    public static ClientSharedId getInstance() {
        return instance;
    }

    public Integer geId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

}
