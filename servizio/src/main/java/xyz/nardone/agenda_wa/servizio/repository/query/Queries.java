package xyz.nardone.agenda_wa.servizio.repository.query;

import static xyz.nardone.agenda_wa.servizio.exception.ApplicationErrors.bad;
import static xyz.nardone.agenda_wa.servizio.util.CommonUtil.id;
import static xyz.nardone.agenda_wa.servizio.util.CommonUtil.key;

import java.time.LocalDate;
import java.util.regex.Pattern;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import xyz.nardone.agenda_wa.servizio.DTO.activity.AppointmentPeriod;
import xyz.nardone.agenda_wa.servizio.DTO.activity.AppointmentSelectionDTO;

public final class Queries {
  private Queries() {}

  public static Query clients(String search) {
    Query q = new Query();
    textSearch(q, search, "nome", "cognome", "nomeLegale");
    return q;
  }

  private static void textSearch(Query q, String search, String... fields) {
    if (search != null && !search.isBlank()) {
      // Every word must match at least one field, including fragments and accented letters.
      var words =
          java.util.Arrays.stream(key(search).split(" "))
              .map(
                  word -> {
                    String regex = textSearchPattern(word);
                    return new Criteria()
                        .orOperator(
                            java.util.Arrays.stream(fields)
                                .map(field -> Criteria.where(field).regex(regex, "i"))
                                .toArray(Criteria[]::new));
                  })
              .toArray(Criteria[]::new);
      q.addCriteria(new Criteria().andOperator(words));
    }
  }

  private static String textSearchPattern(String word) {
    StringBuilder regex = new StringBuilder();
    for (int letter : word.codePoints().toArray()) {
      String plain = new String(Character.toChars(letter));
      if (letter >= 'a' && letter <= 'z') {
        StringBuilder variants = new StringBuilder(plain);
        for (int candidate = 0x00c0; candidate <= 0x024f; candidate++) {
          String accented = new String(Character.toChars(candidate));
          if (key(accented).equals(plain)) variants.append(accented);
        }
        regex.append("[").append(variants).append("]\\p{M}*");
      } else regex.append(Pattern.quote(plain));
    }
    return regex.toString();
  }

  public static Query activities(LocalDate dal, LocalDate al, String esito) {
    if (dal != null && al != null && dal.isAfter(al))
      throw bad("La data iniziale deve precedere la data finale.");
    Query q = new Query();
    if (id(esito) != null) q.addCriteria(Criteria.where("esito").is(esito));
    if (dal != null || al != null) {
      Criteria date = Criteria.where("data");
      if (dal != null) date.gte(dal);
      if (al != null) date.lte(al);
      q.addCriteria(date);
    }
    return q;
  }

  public static Query appointments(LocalDate dal, LocalDate al, String esito, String search) {
    Query q = activities(dal, al, esito);
    if (search != null && !search.isBlank()) {
      // Quote each token separately: all words are required, even across indexed fields.
      // Punctuation separates words and cannot introduce MongoDB's phrase/negation syntax.
      var words =
          java.util.Arrays.stream(key(search).split("[\\s\\p{P}\\p{S}]+"))
              .filter(word -> !word.isEmpty())
              .distinct()
              .toList();
      if (words.isEmpty()) q.addCriteria(Criteria.where("_id").in(java.util.List.of()));
      else {
        TextCriteria text =
            TextCriteria.forLanguage("none").caseSensitive(false).diacriticSensitive(false);
        words.forEach(text::matchingPhrase);
        q.addCriteria(text);
        if (words.size() > 1) {
          // MongoDB's phrase checks can accept a fragment when another indexed term matches.
          // Keep index-backed candidate selection and require whole words in every field check.
          String[] fields = {"cliente.nome", "cliente.cognome", "cliente.nomeLegale", "note"};
          var wholeWords =
              words.stream()
                  .map(
                      word -> {
                        String regex =
                            "(?<![\\p{L}\\p{N}\\p{M}])"
                                + textSearchPattern(word)
                                + "(?![\\p{L}\\p{N}\\p{M}])";
                        return new Criteria()
                            .orOperator(
                                java.util.Arrays.stream(fields)
                                    .map(field -> Criteria.where(field).regex(regex, "i"))
                                    .toArray(Criteria[]::new));
                      })
                  .toArray(Criteria[]::new);
          q.addCriteria(new Criteria().andOperator(wholeWords));
        }
      }
    }
    return q;
  }

  public static Query paginate(Query q, int page, int size) {
    if (page < 0 || size < 1 || size > 100)
      throw bad("Pagina o dimensione non valide (massimo 100 righe).");
    return q.skip((long) page * size).limit(size);
  }

  public static Query appointmentSelection(AppointmentSelectionDTO selection, String esito) {
    if (selection == null || selection.periodo() == null)
      throw bad("Seleziona il periodo degli appuntamenti.");
    LocalDate dal = selection.dal(), al = selection.al(), data = selection.data();
    Query q = activities(null, null, esito);
    switch (selection.periodo()) {
      case TUTTE -> {
        if (dal != null || al != null || data != null)
          throw bad("Per tutte le date, lascia vuoti i campi data.");
      }
      case INTERVALLO -> {
        if (dal == null || al == null || data != null)
          throw bad("Indica la data iniziale e la data finale dell’intervallo.");
        if (dal.isAfter(al)) throw bad("La data iniziale deve precedere la data finale.");
        q.addCriteria(Criteria.where("data").gte(dal).lte(al));
      }
      case PRIMA, DOPO -> {
        if (data == null || dal != null || al != null)
          throw bad("Indica una sola data per il periodo scelto.");
        q.addCriteria(
            selection.periodo() == AppointmentPeriod.PRIMA
                ? Criteria.where("data").lt(data)
                : Criteria.where("data").gt(data));
      }
    }
    return q;
  }
}
