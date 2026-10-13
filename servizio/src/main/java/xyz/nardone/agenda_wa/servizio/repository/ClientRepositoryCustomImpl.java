package xyz.nardone.agenda_wa.servizio.repository;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.ArrayList;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import xyz.nardone.agenda_wa.servizio.exception.ApplicationErrors;
import java.util.List;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Collation;
import xyz.nardone.agenda_wa.servizio.entity.Client;
import xyz.nardone.agenda_wa.servizio.repository.query.Queries;

public class ClientRepositoryCustomImpl extends OwnedQuerySupport<Client>
    implements ClientRepositoryCustom {
  public ClientRepositoryCustomImpl(MongoTemplate mongo) {
    super(mongo, Client.class);
  }

  @Override
  public RepositoryPage<Client> search(String ownerId, String search, int page, int size) {
    var filter = owned(ownerId, Queries.clients(search));
    Queries.paginate(new org.springframework.data.mongodb.core.query.Query(), page, size);
    long total = mongo.count(filter, Client.class);
    List<AggregationOperation> stages = new ArrayList<>();
    stages.add(context -> new Document("$match", filter.getQueryObject()));
    // The sort value exists only while querying; it is never stored in the client document.
    stages.add(context -> new Document("$addFields", new Document("_clientSort",
        new Document("$cond", List.of(new Document("$eq", List.of("$tipo", "AZIENDA")),
            "$nomeLegale", "$cognome")))));
    stages.add(Aggregation.sort(Sort.by("_clientSort", "_id")));
    if (page > 0) stages.add(Aggregation.skip((long) page * size));
    stages.add(Aggregation.limit(size));
    stages.add(context -> new Document("$unset", List.of("_clientSort", "appuntamenti")));
    var aggregation = Aggregation.newAggregation(stages).withOptions(
        Aggregation.newAggregationOptions().collation(Collation.of("it")
            .strength(Collation.ComparisonLevel.secondary())).build());
    return new RepositoryPage<>(mongo.aggregate(aggregation, "clienti", Client.class)
        .getMappedResults(), total);
  }
  @Override
  public long updateDetails(String ownerId, Client client) {
    Update update = new Update().set("tipo", client.tipo()).set("nome", client.nome())
        .set("cognome", client.cognome()).set("nomeLegale", client.nomeLegale())
        .set("telefono", client.telefono());
    return mongo.updateFirst(owned(ownerId, Query.query(where("_id").is(client.id()))),
        update, Client.class).getMatchedCount();
  }

  /** Only the three embedded fields are read and persisted; full appointments stay separate. */
  @Override
  public long rebuildAppointmentHistory(String ownerId, String clientId) {
    Query query = Query.query(where("ownerId").is(ownerId).and("clientId").is(clientId))
        .with(Sort.by(Sort.Order.desc("data"), Sort.Order.asc("_id")));
    query.fields().include("_id").include("data").include("esito");
    var summaries = mongo.find(query, Document.class, "appuntamenti").stream()
        .map(a -> new Document("id", a.getString("_id"))
            .append("data", a.get("data")).append("esito", a.getString("esito"))).toList();
    return mongo.updateFirst(owned(ownerId, Query.query(where("_id").is(clientId))),
        new Update().set("appuntamenti", summaries), "clienti").getModifiedCount();
  }

  @Override
  public RepositoryPage<Client.AppointmentSummary> history(
      String ownerId, String clientId, int page, int size) {
    Queries.paginate(new Query(), page, size);
    Query filter = owned(ownerId, Query.query(where("_id").is(clientId)));
    var aggregation = Aggregation.newAggregation(
        context -> new Document("$match", filter.getQueryObject()),
        context -> new Document("$project", new Document("total",
            new Document("$size", new Document("$ifNull", List.of("$appuntamenti", List.of()))))
            .append("items", new Document("$slice", List.of(
                new Document("$ifNull", List.of("$appuntamenti", List.of())), (long) page * size, size)))));
    Document result = mongo.aggregate(aggregation, "clienti", Document.class).getUniqueMappedResult();
    if (result == null) throw ApplicationErrors.missing();
    var items = result.getList("items", Document.class).stream()
        .map(d -> mongo.getConverter().read(Client.AppointmentSummary.class, d)).toList();
    return new RepositoryPage<>(items, ((Number) result.get("total")).longValue());
  }
}
