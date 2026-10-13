package xyz.nardone.agenda_wa.servizio.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import xyz.nardone.agenda_wa.servizio.entity.Appointment;

public interface AppointmentRepository
    extends MongoRepository<Appointment, String>, AppointmentRepositoryCustom {
  Optional<Appointment> findByIdAndOwnerId(String id, String ownerId);

  List<Appointment> findByOwnerId(String ownerId);

  @Query(value = "{'ownerId': ?0, 'esito': ?1}", fields = "{'clientId': 1}")
  List<Appointment> clientsWithOutcome(String ownerId, String esito);

  long countByOwnerId(String ownerId);

  long deleteByIdAndOwnerId(String id, String ownerId);

  long deleteByOwnerId(String ownerId);

  long deleteByOwnerIdAndClientId(String ownerId, String clientId);

  boolean existsByOwnerIdAndEsito(String ownerId, String esito);

  boolean existsByOwnerIdAndPolizza(String ownerId, String polizza);

  @Query("{'ownerId': ?0, 'clientId': ?1}")
  @Update("{'$set': {'cliente': ?2}}")
  long updateClientDetails(String ownerId, String clientId, Appointment.ClientDetails details);

  @Query("{'ownerId': ?0, 'esito': ?1}")
  @Update("{'$set': {'esito': ?2}}")
  long renameOutcome(String ownerId, String previous, String name);

  @Query("{'ownerId': ?0, 'polizza': ?1}")
  @Update("{'$set': {'polizza': ?2}}")
  long renamePolicy(String ownerId, String previous, String name);

  List<Appointment> findByOwnerIdAndDataGreaterThanEqualOrderByDataAscIdAsc(
      String ownerId, LocalDate date, Pageable limit);
}
