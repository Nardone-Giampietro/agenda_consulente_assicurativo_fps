# Due versioni di Agenda WA

Il repository ha soltanto due branch:

- `online`: Java/Spring Boot, MongoDB e account personali; versione web precedente e branch predefinito.
- `locale`: Java/Spring Boot, H2 incorporato, un utilizzatore, password fissa e interfaccia nel browser; funzionamento offline e pacchetti `.deb`.

Passare da una versione all’altra con `git switch online` oppure `git switch locale`. La cartella del progetto è unica; i dati dei due database restano distinti.

I pacchetti Debian sono nella cartella `dist/` del branch `locale`. In quel branch sono condivisi anche dati iniziali e hash della password, nelle risorse Java. Il portafoglio Excel e le immagini sono condivisi in entrambi. Le istruzioni locali per Codex, le cache e le chiavi JWT non sono tracciate.

Per l’uso offline e le prove dei pacchetti consultare il README e la documentazione del branch `locale`.
