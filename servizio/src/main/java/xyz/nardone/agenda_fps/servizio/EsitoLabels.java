package xyz.nardone.agenda_fps.servizio;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Centralized labels for standard appointment outcomes (esiti).
 */
public final class EsitoLabels {
    public static final String SOTTOSCRIZIONE = "Sottoscrizione";
    public static final String CALLBACK = "Callback";
    public static final String RIFIUTO = "Rifiuto";
    public static final String ASSISTENZA = "Assistenza";
    public static final String PRIMO_APPUNTAMENTO = "Primo Appuntamento";

    public static final List<String> RESERVED = Collections.unmodifiableList(Arrays.asList(
            SOTTOSCRIZIONE,
            CALLBACK,
            RIFIUTO,
            ASSISTENZA,
            PRIMO_APPUNTAMENTO
    ));

    private EsitoLabels() {
    }
}
