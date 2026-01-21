package xyz.nardone.agenda_fps.applicazione;

/**
 * Singleton holder for the currently selected banker ID.
 */
public class BankerSharedId {
    private static final BankerSharedId instance = new BankerSharedId();

    private Integer id = 0;

    private BankerSharedId() {}

    public static BankerSharedId getInstance() {
        return instance;
    }

    public Integer geId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
    
}
