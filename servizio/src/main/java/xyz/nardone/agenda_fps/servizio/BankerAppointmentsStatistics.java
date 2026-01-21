package xyz.nardone.agenda_fps.servizio;

/**
 * Appointment statistics enriched with banker identity fields.
 */
public class BankerAppointmentsStatistics extends AppointmentsStatistics {
    private String bankerNome;
    private String bankerCognome;

    public String getBankerNome() {
        return bankerNome;
    }

    public String getBankerCognome() {
        return bankerCognome;
    }

    public void setBankerNome(String bankerNome) {
        this.bankerNome = bankerNome;
    }

    public void setBankerCognome(String bankerCognome) {
        this.bankerCognome = bankerCognome;
    }

    public BankerAppointmentsStatistics(String bankerNome, String bankerCognome, Integer Premi, Integer Sottoscrizioni, Integer Appuntamenti) {
        super(Premi, Sottoscrizioni, Appuntamenti);
        this.bankerNome = bankerNome;
        this.bankerCognome = bankerCognome;
    }
}
