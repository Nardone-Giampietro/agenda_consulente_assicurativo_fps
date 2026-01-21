package xyz.nardone.agenda_fps.applicazione;

/**
 * Singleton holder for the active navigation page ID.
 */
public class PageSharedId {
    private static final PageSharedId instance = new PageSharedId();

    private Integer id;

    private PageSharedId() {}

    public static PageSharedId getInstance() {
        return instance;
    }

    public Integer geId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

}
