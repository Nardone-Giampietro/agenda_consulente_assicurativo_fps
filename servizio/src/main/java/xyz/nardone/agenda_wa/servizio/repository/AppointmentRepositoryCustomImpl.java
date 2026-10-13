package xyz.nardone.agenda_wa.servizio.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Query;
import xyz.nardone.agenda_wa.servizio.DTO.activity.AppointmentSelectionDTO;
import xyz.nardone.agenda_wa.servizio.entity.Appointment;
import xyz.nardone.agenda_wa.servizio.repository.query.Queries;

public class AppointmentRepositoryCustomImpl extends OwnedQuerySupport<Appointment>
    implements AppointmentRepositoryCustom {
  private static final Sort ORDER = Sort.by(Sort.Order.desc("data"), Sort.Order.asc("id"));

  public AppointmentRepositoryCustomImpl(MongoTemplate mongo) {
    super(mongo, Appointment.class);
  }

  @Override
  public RepositoryPage<Appointment> search(
      String ownerId,
      LocalDate dal,
      LocalDate al,
      String esito,
      String search,
      int page,
      int size) {
    return page(ownerId, Queries.appointments(dal, al, esito, search), page, size, ORDER);
  }

  @Override
  public RepositoryPage<Appointment> selection(
      String ownerId, AppointmentSelectionDTO selection, String esito) {
    Query query = owned(ownerId, Queries.appointmentSelection(selection, esito));
    long total = mongo.count(query, entity);
    return new RepositoryPage<>(mongo.find(query.limit(5).with(ORDER), entity), total);
  }

  @Override
  public long countSelection(String ownerId, AppointmentSelectionDTO selection, String esito) {
    return mongo.count(owned(ownerId, Queries.appointmentSelection(selection, esito)), entity);
  }

  @Override
  public List<String> selectionClientIds(String ownerId, AppointmentSelectionDTO selection, String esito) {
    return mongo.findDistinct(owned(ownerId, Queries.appointmentSelection(selection, esito)),
        "clientId", entity, String.class);
  }

  @Override
  public long deleteSelection(String ownerId, AppointmentSelectionDTO selection, String esito) {
    return mongo
        .remove(owned(ownerId, Queries.appointmentSelection(selection, esito)), entity)
        .getDeletedCount();
  }

  @Override
  public List<OutcomeTotal> totals(
      String ownerId, LocalDate dal, LocalDate al, String esito, String search) {
    Query filter = owned(ownerId, Queries.appointments(dal, al, esito, search));
    Aggregation aggregation =
        Aggregation.newAggregation(
            (AggregationOperation)
                ctx -> new Document("$match", ctx.getMappedObject(filter.getQueryObject())),
            Aggregation.group("esito").count().as("numero").sum("premio").as("premi"));
    return mongo.aggregate(aggregation, entity, Document.class).getMappedResults().stream()
        .map(
            row ->
                new OutcomeTotal(
                    row.getString("_id"),
                    ((Number) row.get("numero")).longValue(),
                    amount(row.get("premi"))))
        .toList();
  }

  private BigDecimal amount(Object value) {
    if (value instanceof Decimal128 decimal) return decimal.bigDecimalValue();
    return value instanceof Number number ? new BigDecimal(number.toString()) : BigDecimal.ZERO;
  }
}
