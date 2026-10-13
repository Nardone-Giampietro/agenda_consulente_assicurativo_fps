package xyz.nardone.agenda_wa.servizio.repository;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Backfill the optional phone and exact partial embedding, retaining all application data. */
@Repository
public class ClientHistorySchemaRepository {
  private final MongoTemplate mongo;
  private final ClientRepository clients;

  public ClientHistorySchemaRepository(MongoTemplate mongo, ClientRepository clients) {
    this.mongo = mongo;
    this.clients = clients;
  }

  @Transactional
  public long migrate() {
    Query appointmentQuery = new Query();
    appointmentQuery.fields().include("ownerId").include("clientId").include("data").include("esito");
    for (Document appointment : mongo.find(appointmentQuery, Document.class, "appuntamenti")) {
      String owner = appointment.getString("ownerId"), clientId = appointment.getString("clientId");
      if (owner == null || owner.isBlank() || clientId == null
          || appointment.get("data") == null || appointment.getString("esito") == null
          || !mongo.exists(Query.query(where("ownerId").is(owner).and("_id").is(clientId)), "clienti"))
        throw new IllegalStateException("Riferimento cliente assente o estraneo: storico non convertito.");
    }
    Query clientQuery = new Query();
    clientQuery.fields().include("ownerId");
    long count = 0;
    for (Document client : mongo.find(clientQuery, Document.class, "clienti")) {
      String owner = client.getString("ownerId"), id = client.getString("_id");
      if (owner == null || owner.isBlank())
        throw new IllegalStateException("Cliente senza proprietario: storico non convertito.");
      count += clients.rebuildAppointmentHistory(owner, id);
      count += mongo.updateFirst(Query.query(where("_id").is(id).and("ownerId").is(owner)
          .and("telefono").is(null)), new Update().set("telefono", ""), "clienti").getModifiedCount();
    }
    return count;
  }
}
