package xyz.nardone.agenda_wa.servizio.config;

import java.util.List;

/** Values seeded only by transactional registration, never by application startup. */
public final class DefaultCatalogs {
  private DefaultCatalogs() {}

  public static final List<String> OUTCOMES =
      List.of(
          "Sottoscrizione", "Rifiuto", "Callback", "Checkup", "Assistenza", "Primo Appuntamento");
  /** Distinct product names from Finale Portafoglio Jenica.xlsx, Foglio1!L11:L572. */
  public static final List<String> POLICIES =
      List.of(
          "0757 Easy Plan",
          "0768 Mia Pensione",
          "6000 Tutta la vita",
          "8E42 Inf. Conducente Trasp. Ritiro Patente",
          "8E70 Inf. Malattia Individuale",
          "Auto",
          "Auto Protezione",
          "Auto Storiche",
          "Guida Autocarri",
          "ME28 Protezione salute nuovo",
          "ME35 Protezione salute ed. 2018",
          "Moto",
          "NPA Nuova Protezione Auto",
          "PrimaTutelaFamiglia",
          "Protezione InMoto",
          "Protezione VAN",
          "Protezione al Volante",
          "TutelAuto",
          "Van",
          "XE08 Protezione familiare 4",
          "XE44 Nuova Protezione Casa",
          "XE81 AZIENDA PROTETTA");
}
