package xyz.nardone.agenda_wa.servizio.repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Query;
import xyz.nardone.agenda_wa.servizio.entity.Callback;
import xyz.nardone.agenda_wa.servizio.repository.query.Queries;

public class CallbackRepositoryCustomImpl extends OwnedQuerySupport<Callback>
    implements CallbackRepositoryCustom {
  public CallbackRepositoryCustomImpl(MongoTemplate mongo) {
    super(mongo, Callback.class);
  }

  @Override
  public RepositoryPage<Callback> search(
      String ownerId, LocalDate dal, LocalDate al, LocalDate today, int page, int size) {
    Queries.paginate(new Query(), page, size);
    var filter = owned(ownerId, Queries.activities(dal, al, null));
    long total = mongo.count(filter, entity);
    List<AggregationOperation> stages = new ArrayList<>();
    stages.add(context -> new Document("$match", context.getMappedObject(filter.getQueryObject())));
    // Prioritize today across the whole result, before pagination; retain the existing order.
    stages.add(context -> new Document("$addFields", new Document("_callbackPriority",
        new Document("$cond", List.of(
            new Document("$eq", List.of("$data", today.toString())), 0, 1)))));
    stages.add(Aggregation.sort(Sort.by("_callbackPriority", "data", "_id")));
    if (page > 0) stages.add(Aggregation.skip((long) page * size));
    stages.add(Aggregation.limit(size));
    stages.add(context -> new Document("$unset", "_callbackPriority"));
    return new RepositoryPage<>(mongo.aggregate(Aggregation.newAggregation(stages), entity, entity)
        .getMappedResults(), total);
  }
}
