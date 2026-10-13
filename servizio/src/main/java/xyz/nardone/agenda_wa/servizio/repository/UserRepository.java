package xyz.nardone.agenda_wa.servizio.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import xyz.nardone.agenda_wa.servizio.entity.User;

public interface UserRepository extends MongoRepository<User, String> {
  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  @Query("{'_id': ?0}")
  @Update("{'$inc': {'tokenVersion': 1}}")
  long revokeTokens(String id);
}
