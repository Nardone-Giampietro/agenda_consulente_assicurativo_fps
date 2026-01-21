package xyz.nardone.agenda_fps.applicazione;

/**
 * Singleton holder for the currently selected callback ID.
 */
public class CallbackSharedId {
    private static final CallbackSharedId instance = new CallbackSharedId();

    private Integer id;

    private CallbackSharedId() {}

    public static CallbackSharedId getInstance() {
        return instance;
    }

    public Integer geId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

}
