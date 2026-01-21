package xyz.nardone.agenda_fps.applicazione;

/**
 * Singleton holder for the currently selected esito ID.
 */
public class EsitoSharedId {
    private static final EsitoSharedId instance = new EsitoSharedId();

    private Integer id;

    private EsitoSharedId() {}

    public static EsitoSharedId getInstance() {
        return instance;
    }

    public Integer geId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

}
