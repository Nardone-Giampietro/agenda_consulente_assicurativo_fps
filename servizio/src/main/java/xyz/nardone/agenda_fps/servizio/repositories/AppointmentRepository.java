package xyz.nardone.agenda_fps.servizio.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import xyz.nardone.agenda_fps.servizio.entities.Appointment;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Repository for appointment entities and custom queries.
 */
public interface AppointmentRepository extends CrudRepository<Appointment, Integer> {

    /**
     * Updates an appointment row via native SQL.
     */
    @Transactional
    @Modifying
    @Query(value = """
    UPDATE APPOINTMENTS 
    SET ID_BANKER = :bankerId, 
        ID_CLIENTE = :clientId, 
        DATA = :data, 
        ESITO = :esito, 
        POLIZZA = :polizza, 
        PREMIO = :premio, 
        NOTE = :note 
    WHERE ID = :appointmentId
    """, nativeQuery = true)
    int updateAppointment(
            Integer appointmentId,
            Integer bankerId,
            Integer clientId,
            String data,
            String esito,
            String polizza,
            Integer premio,
            String note
    );

    /**
     * Finds appointments with optional banker, date range, and esito filters.
     */
    @Query(value = """
    SELECT * FROM APPOINTMENTS a 
    WHERE (:idBanker IS NULL OR a.id_banker = :idBanker) 
      AND (:startDate IS NULL OR a.data >= :startDate) 
      AND (:endDate IS NULL OR a.data <= :endDate) 
      AND (:esito IS NULL OR a.esito = :esito)
    """, nativeQuery = true)
    List<Appointment> findAppointmentByBankerIdAndDataBetweenAndEsito(
            Integer idBanker,
            String startDate,
            String endDate,
            String esito
    );

}
