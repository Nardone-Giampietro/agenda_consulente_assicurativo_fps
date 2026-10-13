package xyz.nardone.agenda_wa.servizio.repository;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.Objects;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import xyz.nardone.agenda_wa.servizio.repository.query.Queries;

/** Shared query execution; the owner predicate is applied before any database access. */
abstract class OwnedQuerySupport<T> {
  protected final MongoTemplate mongo;
  protected final Class<T> entity;

  protected OwnedQuerySupport(MongoTemplate mongo, Class<T> entity) {
    this.mongo = mongo;
    this.entity = entity;
  }

  protected Query owned(String ownerId, Query query) {
    return query.addCriteria(where("ownerId").is(Objects.requireNonNull(ownerId)));
  }

  protected RepositoryPage<T> page(String ownerId, Query filter, int page, int size, Sort sort) {
    Query query = owned(ownerId, filter);
    long total = mongo.count(query, entity);
    return new RepositoryPage<>(
        mongo.find(Queries.paginate(query, page, size).with(sort), entity), total);
  }
}
