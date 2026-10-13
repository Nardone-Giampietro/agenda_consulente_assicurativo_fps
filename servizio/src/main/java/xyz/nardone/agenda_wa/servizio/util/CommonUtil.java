package xyz.nardone.agenda_wa.servizio.util;

import java.text.Normalizer;
import java.util.Locale;

public final class CommonUtil {
  private CommonUtil() {}

  public static String key(String value) {
    return Normalizer.normalize(value.strip(), Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("\\s+", " ");
  }

  public static String id(String id) {
    return id == null || id.isBlank() ? null : id;
  }
}
