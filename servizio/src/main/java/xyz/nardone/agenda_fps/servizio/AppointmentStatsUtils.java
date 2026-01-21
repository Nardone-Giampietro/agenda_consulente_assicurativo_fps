package xyz.nardone.agenda_fps.servizio;

import java.util.List;
import java.util.Objects;
import xyz.nardone.agenda_fps.servizio.entities.Appointment;

/**
 * Utility methods for computing appointment statistics.
 */
public final class AppointmentStatsUtils {
    private AppointmentStatsUtils() {
    }

    /**
     * Counts appointments matching a specific outcome (esito).
     *
     * @param appointments the appointment list to scan
     * @param esito the outcome label to match
     * @return the number of matching appointments
     */
    public static int countByEsito(List<Appointment> appointments, String esito) {
        return (int) appointments.stream()
                .filter(app -> Objects.equals(esito, app.getEsito()))
                .count();
    }

    /**
     * Sums premio values, treating null as 0.
     *
     * @param appointments the appointment list to scan
     * @return the sum of premio values
     */
    public static int sumPremi(List<Appointment> appointments) {
        return appointments.stream()
                .mapToInt(app -> app.getPremio() == null ? 0 : app.getPremio())
                .sum();
    }
}
