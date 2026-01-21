package xyz.nardone.agenda_fps.applicazione;

/**
 * Singleton holder for the currently selected polizza ID.
 */
public class PolizzaSharedId {
    private static final PolizzaSharedId instance = new PolizzaSharedId();

    private Integer id;

    private PolizzaSharedId() {}

    public static PolizzaSharedId getInstance() {
        return instance;
    }

    public Integer geId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

}
