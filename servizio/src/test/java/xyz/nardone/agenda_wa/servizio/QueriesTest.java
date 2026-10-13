package xyz.nardone.agenda_wa.servizio;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import xyz.nardone.agenda_wa.servizio.repository.query.Queries;

class QueriesTest {
  @Test
  void normalizesAccentsAndWhitespace() {
    assertEquals(
        "mario de andre",
        xyz.nardone.agenda_wa.servizio.util.CommonUtil.key("  Màrio   De André  "));
  }

  @Test
  void rejectsReversedRangeAndUnboundedPages() {
    assertThrows(
        RuntimeException.class,
        () -> Queries.activities(LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 2), null));
    assertThrows(
        RuntimeException.class,
        () -> Queries.paginate(new org.springframework.data.mongodb.core.query.Query(), -1, 20));
    assertThrows(
        RuntimeException.class,
        () -> Queries.paginate(new org.springframework.data.mongodb.core.query.Query(), 0, 101));
  }

  @Test
  void preservesOneSidedRanges() {
    LocalDate date = LocalDate.of(2026, 10, 3);
    assertTrue(
        ((org.bson.Document) Queries.activities(date, null, null).getQueryObject().get("data"))
            .containsKey("$gte"));
    assertTrue(
        ((org.bson.Document) Queries.activities(null, date, null).getQueryObject().get("data"))
            .containsKey("$lte"));
  }
}
