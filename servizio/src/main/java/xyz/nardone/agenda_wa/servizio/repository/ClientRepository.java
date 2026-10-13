package xyz.nardone.agenda_wa.servizio.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import xyz.nardone.agenda_wa.servizio.entity.Client;

public interface ClientRepository extends MongoRepository<Client, String>, ClientRepositoryCustom {
  @Query(value = "{'_id': ?0, 'ownerId': ?1}", fields = "{'appuntamenti': 0}")
  Optional<Client> findByIdAndOwnerId(String id, String ownerId);

  List<Client> findByOwnerId(String ownerId);

  long countByOwnerId(String ownerId);

  long deleteByIdAndOwnerId(String id, String ownerId);

  long deleteByOwnerId(String ownerId);

  List<Client> findByOwnerIdAndIdIn(String ownerId, Collection<String> ids);

  @Query("{'ownerId': ?0, '_id': ?1}")
  @Update("{'$inc': {'revision': 1}}")
  long lock(String ownerId, String id);
}
