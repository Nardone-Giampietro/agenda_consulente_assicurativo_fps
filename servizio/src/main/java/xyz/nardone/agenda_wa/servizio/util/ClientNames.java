package xyz.nardone.agenda_wa.servizio.util;

import java.text.Normalizer;
import java.util.Locale;

public final class ClientNames {
  private ClientNames() {}

  private static String clean(String value) {
    return Normalizer.normalize(value.strip().replaceAll("\\s+", " "), Normalizer.Form.NFC);
  }

  public static String person(String value) {
    return clean(value).toLowerCase(Locale.ROOT);
  }

  public static String company(String value) {
    return clean(value).toUpperCase(Locale.ROOT);
  }
}
