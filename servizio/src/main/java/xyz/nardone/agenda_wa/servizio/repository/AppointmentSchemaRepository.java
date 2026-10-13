package xyz.nardone.agenda_wa.servizio.repository;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import java.util.Objects;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import xyz.nardone.agenda_wa.servizio.util.ClientNames;

/** Converts the previous appointment schema without rewriting unrelated data or aliases. */
@Repository
public class AppointmentSchemaRepository {
  private final MongoTemplate mongo;

  public AppointmentSchemaRepository(MongoTemplate mongo) {
    this.mongo = mongo;
  }

  @Transactional
  public long migrate() {
    Query legacy = new Query();
    long count = 0;
    for (Document appointment : mongo.find(legacy, Document.class, "appuntamenti")) {
      String owner = appointment.getString("ownerId");
      if (owner == null || owner.isBlank())
        throw new IllegalStateException("Appuntamento senza proprietario: conversione interrotta.");
      Update update = new Update().unset("esitoId").unset("polizzaId");
      if (!appointment.containsKey("cliente")) {
        Document client = reference("clienti", owner, appointment.getString("clientId"), null);
        String type = client.getString("tipo");
        Document details = new Document("tipo", type == null ? "PERSONA_FISICA" : type);
        if ("AZIENDA".equals(type))
          details.append("nomeLegale", ClientNames.company(client.getString("nomeLegale")));
        else
          details
              .append("nome", ClientNames.person(client.getString("nome")))
              .append("cognome", ClientNames.person(client.getString("cognome")));
        update.set("cliente", details);
      } else {
        Document details = appointment.get("cliente", Document.class);
        String type = details.getString("tipo");
        update.set("cliente.tipo", type == null ? "PERSONA_FISICA" : type);
        if ("AZIENDA".equals(type))
          update
              .set("cliente.nomeLegale", ClientNames.company(details.getString("nomeLegale")))
              .unset("cliente.nome")
              .unset("cliente.cognome");
        else
          update
              .set("cliente.nome", ClientNames.person(details.getString("nome")))
              .set("cliente.cognome", ClientNames.person(details.getString("cognome")))
              .unset("cliente.nomeLegale");
      }
      if (appointment.containsKey("esitoId"))
        update.set(
            "esito",
            reference("cataloghi", owner, appointment.getString("esitoId"), "esiti")
                .getString("nome"));
      if (appointment.containsKey("polizzaId")) {
        String policy = appointment.getString("polizzaId");
        update.set(
            "polizza",
            policy == null
                ? null
                : reference("cataloghi", owner, policy, "polizze").getString("nome"));
      }
      count +=
          mongo
              .updateFirst(
                  Query.query(where("_id").is(appointment.get("_id")).and("ownerId").is(owner)),
                  update,
                  "appuntamenti")
              .getModifiedCount();
    }
    return count;
  }

  private Document reference(String collection, String owner, String id, String type) {
    Query query = Query.query(where("_id").is(Objects.requireNonNull(id)).and("ownerId").is(owner));
    if (type != null) query.addCriteria(where("tipo").is(type));
    Document document = mongo.findOne(query, Document.class, collection);
    if (document == null)
      throw new IllegalStateException(
          "Riferimento assente o di altro proprietario: conversione interrotta.");
    return document;
  }

  public void removeLegacyIndexes() {
    var indexes = mongo.indexOps("appuntamenti");
    for (var index : indexes.getIndexInfo())
      if (index.getName().equals("appuntamento_esito_data")
          || index.getName().equals("appuntamento_polizza")) indexes.dropIndex(index.getName());
  }

  public void ensureTextIndex() {
    mongo
        .getCollection("appuntamenti")
        .createIndex(
            Indexes.compoundIndex(
                Indexes.ascending("ownerId"),
                Indexes.text("cliente.nome"),
                Indexes.text("cliente.cognome"),
                Indexes.text("cliente.nomeLegale"),
                Indexes.text("note")),
            new IndexOptions().name("appuntamento_testo").defaultLanguage("none"));
  }
}
