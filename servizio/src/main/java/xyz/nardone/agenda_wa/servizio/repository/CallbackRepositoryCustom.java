package xyz.nardone.agenda_wa.servizio.repository;

import java.time.LocalDate;
import xyz.nardone.agenda_wa.servizio.entity.Callback;

public interface CallbackRepositoryCustom {
  RepositoryPage<Callback> search(
      String ownerId, LocalDate dal, LocalDate al, LocalDate today, int page, int size);
}
