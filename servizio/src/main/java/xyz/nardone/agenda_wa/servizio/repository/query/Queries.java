package xyz.nardone.agenda_wa.servizio.repository.query;

import static xyz.nardone.agenda_wa.servizio.exception.ApplicationErrors.bad;
import static xyz.nardone.agenda_wa.servizio.util.CommonUtil.key;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import xyz.nardone.agenda_wa.servizio.DTO.activity.AppointmentSelectionDTO;

public final class Queries {
  private Queries() {}

  public record Filter(String sql, List<Object> values) {
    public Filter {
      values = List.copyOf(values);
    }

    public Object[] args() {
      return values.toArray();
    }

    public Filter and(String expression, Object... args) {
      var all = new ArrayList<>(values);
      all.addAll(Arrays.asList(args));
      return new Filter(sql + " AND " + expression, all);
    }
  }

  public static Filter owned(String owner) {
    return new Filter("owner_id=?", List.of(Objects.requireNonNull(owner)));
  }

  public static Filter clients(String owner, String search) {
    Filter f = owned(owner);
    if (search != null && !search.isBlank())
      for (String word : key(search).split(" ")) f = f.and("LOCATE(?,search_text)>0", word);
    return f;
  }

  public static Filter activities(String owner, LocalDate dal, LocalDate al, String esito) {
    if (dal != null && al != null && dal.isAfter(al))
      throw bad("La data iniziale deve precedere la data finale.");
    Filter f = owned(owner);
    if (esito != null && !esito.isBlank()) f = f.and("esito=?", esito);
    if (dal != null) f = f.and("data>=?", dal);
    if (al != null) f = f.and("data<=?", al);
    return f;
  }

  public static String words(String text) {
    return " "
        + String.join(
            " ",
            Arrays.stream(key(text == null ? "" : text).split("[\\s\\p{P}\\p{S}]+"))
                .filter(w -> !w.isEmpty())
                .toList())
        + " ";
  }

  public static Filter appointments(
      String owner, LocalDate dal, LocalDate al, String esito, String search) {
    Filter f = activities(owner, dal, al, esito);
    if (search != null && !search.isBlank()) {
      var tokens =
          Arrays.stream(words(search).strip().split(" "))
              .filter(w -> !w.isEmpty())
              .distinct()
              .toList();
      if (tokens.isEmpty()) return f.and("1=0");
      for (String word : tokens) f = f.and("LOCATE(?,search_words)>0", " " + word + " ");
    }
    return f;
  }

  public static void paginate(int page, int size) {
    if (page < 0 || size < 1 || size > 100)
      throw bad("Pagina o dimensione non valide (massimo 100 righe).");
  }

  public static Filter appointmentSelection(String owner, AppointmentSelectionDTO s, String esito) {
    if (s == null || s.periodo() == null) throw bad("Seleziona il periodo degli appuntamenti.");
    Filter f = activities(owner, null, null, esito);
    switch (s.periodo()) {
      case TUTTE -> {
        if (s.dal() != null || s.al() != null || s.data() != null)
          throw bad("Per tutte le date, lascia vuoti i campi data.");
      }
      case INTERVALLO -> {
        if (s.dal() == null || s.al() == null || s.data() != null)
          throw bad("Indica la data iniziale e la data finale dell’intervallo.");
        if (s.dal().isAfter(s.al())) throw bad("La data iniziale deve precedere la data finale.");
        f = f.and("data>=? AND data<=?", s.dal(), s.al());
      }
      case PRIMA, DOPO -> {
        if (s.data() == null || s.dal() != null || s.al() != null)
          throw bad("Indica una sola data per il periodo scelto.");
        f = f.and(s.periodo().name().equals("PRIMA") ? "data<?" : "data>?", s.data());
      }
    }
    return f;
  }
}
