package xyz.nardone.agenda_wa.servizio.util;

import java.text.Normalizer;
import java.util.Locale;

public final class ClientNames {
  private ClientNames() {}

  private static String clean(String value) {
    return Normalizer.normalize(value.strip().replaceAll("\\s+", " "), Normalizer.Form.NFC);
  }

  public static String person(String value) {
    return bounded(clean(value).toLowerCase(Locale.ROOT), 100);
  }

  public static String company(String value) {
    return bounded(clean(value).toUpperCase(Locale.ROOT), 150);
  }

  private static String bounded(String value, int maximum) {
    if (value.length() > maximum)
      throw xyz.nardone.agenda_wa.servizio.exception.ApplicationErrors.bad(
          "Il nome normalizzato supera il limite di " + maximum + " caratteri.");
    return value;
  }
}
