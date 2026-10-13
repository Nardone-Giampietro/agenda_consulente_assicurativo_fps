package xyz.nardone.agenda_wa.servizio.repository;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import jakarta.annotation.PostConstruct;
import java.util.Optional;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class MetadataRepository {
  private static final String COLLECTION = "metadati";
  private final MongoTemplate mongo;

  public MetadataRepository(MongoTemplate mongo) {
    this.mongo = mongo;
  }

  @PostConstruct
  void ensureCollection() {
    if (!mongo.collectionExists(COLLECTION)) mongo.createCollection(COLLECTION);
  }

  public Optional<String> reservedOwner(String email) {
    Document initial = mongo.findById("initial-owner", Document.class, COLLECTION);
    return initial != null && email.equals(initial.getString("email"))
        ? Optional.of(initial.getString("userId"))
        : Optional.empty();
  }

  public void removeReservation() {
    mongo.remove(Query.query(where("_id").is("initial-owner")), COLLECTION);
  }

  public boolean catalogsInitialized(String ownerId) {
    return mongo.findById("cataloghi-v2:" + ownerId, Document.class, COLLECTION) != null;
  }

  public void markCatalogsInitialized(String ownerId) {
    mongo.insert(
        new Document("_id", "cataloghi-v2:" + ownerId).append("ownerId", ownerId), COLLECTION);
  }
}
