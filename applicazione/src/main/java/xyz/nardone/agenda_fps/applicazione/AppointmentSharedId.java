package xyz.nardone.agenda_fps.applicazione;

/**
 * Singleton holder for the currently selected appointment ID.
 */
public class AppointmentSharedId {
    private static final AppointmentSharedId instance = new AppointmentSharedId();

    private Integer id;

    private AppointmentSharedId() {}

    public static AppointmentSharedId getInstance() {
        return instance;
    }

    public Integer geId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

}
