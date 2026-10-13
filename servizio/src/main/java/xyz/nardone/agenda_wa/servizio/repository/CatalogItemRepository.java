package xyz.nardone.agenda_wa.servizio.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import xyz.nardone.agenda_wa.servizio.entity.CatalogItem;

public interface CatalogItemRepository extends MongoRepository<CatalogItem, String> {
  Optional<CatalogItem> findByIdAndOwnerId(String id, String ownerId);

  List<CatalogItem> findByOwnerId(String ownerId);

  long countByOwnerId(String ownerId);

  long deleteByIdAndOwnerId(String id, String ownerId);

  long deleteByOwnerId(String ownerId);

  @Query("{'ownerId': ?0, '_id': ?1}")
  @Update("{'$inc': {'revision': 1}}")
  long lock(String ownerId, String id);

  List<CatalogItem> findByOwnerIdAndTipoOrderByNomeAsc(String ownerId, String tipo);

  Optional<CatalogItem> findByIdAndOwnerIdAndTipo(String id, String ownerId, String tipo);
}
