package xyz.nardone.agenda_wa.servizio.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import xyz.nardone.agenda_wa.servizio.entity.Callback;

public interface CallbackRepository
    extends MongoRepository<Callback, String>, CallbackRepositoryCustom {
  Optional<Callback> findByIdAndOwnerId(String id, String ownerId);

  List<Callback> findByOwnerId(String ownerId);

  long countByOwnerId(String ownerId);

  long deleteByIdAndOwnerId(String id, String ownerId);

  long deleteByOwnerId(String ownerId);

  long deleteByOwnerIdAndClientId(String ownerId, String clientId);

  long countByOwnerIdAndData(String ownerId, LocalDate date);

  List<Callback> findByOwnerIdAndDataOrderByIdAsc(String ownerId, LocalDate date, Pageable limit);
}
