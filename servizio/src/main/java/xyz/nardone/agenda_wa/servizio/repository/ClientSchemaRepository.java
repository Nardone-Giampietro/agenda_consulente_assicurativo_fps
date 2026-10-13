package xyz.nardone.agenda_wa.servizio.repository;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import xyz.nardone.agenda_wa.servizio.entity.ClientType;
import xyz.nardone.agenda_wa.servizio.util.ClientNames;

/** Normalize generalities and remove the obsolete derived fields, preserving other data. */
@Repository
public class ClientSchemaRepository {
  private final MongoTemplate mongo;

  public ClientSchemaRepository(MongoTemplate mongo) {
    this.mongo = mongo;
  }

  @Transactional
  public long migrate() {
    long count = 0;
    for (Document client : mongo.find(new Query(), Document.class, "clienti")) {
      String owner = client.getString("ownerId");
      if (owner == null || owner.isBlank())
        throw new IllegalStateException("Cliente senza proprietario: conversione interrotta.");
      String type = client.getString("tipo");
      ClientType kind = type == null ? ClientType.PERSONA_FISICA : ClientType.valueOf(type);
      Update update = new Update().set("tipo", kind.name()).unset("chiave").unset("ordine");
      if (kind == ClientType.AZIENDA)
        update.set("nomeLegale", ClientNames.company(client.getString("nomeLegale")))
            .unset("nome").unset("cognome");
      else update.set("nome", ClientNames.person(client.getString("nome")))
          .set("cognome", ClientNames.person(client.getString("cognome"))).unset("nomeLegale");
      count += mongo.updateFirst(Query.query(where("_id").is(client.get("_id"))
          .and("ownerId").is(owner)), update, "clienti").getModifiedCount();
    }
    return count;
  }

  public void removeLegacyIndexes() {
    var indexes = mongo.indexOps("clienti");
    for (var index : indexes.getIndexInfo())
      if (index.getName().equals("cliente_nomi") || index.getName().equals("cliente_ordine"))
        indexes.dropIndex(index.getName());
  }
}
