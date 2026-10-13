# Agenda WA — edizione locale

Agenda per un solo utilizzatore e un solo computer: clienti, appuntamenti, callback, esiti e polizze. Java 21 / Spring Boot 4.0.5 serve l’interfaccia nel browser e usa **H2 2.4.240 incorporato su disco**. Nessuna funzione richiede Internet, font esterni, MongoDB, Docker o un Java installato sul computer destinatario.

Il primo ambiente previsto è Linux su Chromebook già abilitato, con le normali librerie Debian dell’ambiente grafico. I pacchetti sono costruiti su Debian 12: `agenda-wa_1.0.0_amd64.deb` per Intel/AMD e `agenda-wa_1.0.0_arm64.deb` per ARM. Il runtime Java è incluso. La verifica automatica installa ed esegue entrambi i pacchetti in container Debian senza rete e senza Java di sistema. Resta necessaria una prova su un Chromebook fisico per verificare l’integrazione con il menu e il browser ChromeOS.

## Uso e dati

Avviare **Agenda WA** dal menu delle applicazioni Linux; il processo apre il browser su `http://localhost:8080`. Un secondo avvio riapre il browser senza avviare un’altra istanza. Il server ascolta esclusivamente su `127.0.0.1`. La porta può essere scelta con `PORT`; se occupata da un altro programma l’avvio termina e registra l’errore.

L’accesso richiede la sola password fissa concordata, senza registrazione, cambio o recupero. Nella build è incluso soltanto il suo hash PBKDF2 con sale; l’hash è incluso nelle risorse del progetto, mentre la password non viene salvata in chiaro. JWT in cookie HttpOnly/SameSite=Strict, CSRF e revoca al logout proteggono tutte le API. Non esistono server H2 TCP o console web del database.

I dati risiedono in `~/.local/share/agenda-wa/`, indipendentemente dalla cartella di avvio:

- `agenda.mv.db`: database persistente;
- `security/jwt.key`: chiave locale generata al primo avvio;
- `backups/`: copie automatiche e precedenti al ripristino;
- `logs/agenda.log`: log con rotazione;
- `app.lock`: protezione dalla doppia istanza, rilasciata all’arresto.

Cartella dati e backup hanno permessi riservati all’utente Linux. Le copie JSON contengono dati personali in chiaro: conservarle come il database. La password controlla l’accesso dall’interfaccia, senza cifrare il disco. Per scegliere un’altra cartella usare `AGENDA_DATA_DIR` con un percorso assoluto; il database non si sposta automaticamente.

Chiudere la scheda del browser lascia attivo il processo locale. Per arrestarlo manualmente identificare il PID con `pgrep -af '/opt/agenda-wa/bin/Agenda WA'`, controllarne comando e proprietario, quindi inviare `kill -TERM PID`. Nel terminale Java è sufficiente Ctrl+C. Non cancellare file H2 mentre il processo è attivo.

## Backup e aggiornamenti

In **Impostazioni → Backup dell’agenda**, **Esporta backup** salva clienti, cataloghi, appuntamenti e callback in un JSON versionato con impronta SHA-256. **Ripristina backup** richiede la conferma `RIPRISTINA`: valida formato, campi, impronta e riferimenti, conserva una copia preventiva e sostituisce i dati in una transazione. Dopo il ripristino occorre accedere nuovamente. Un file non valido lascia intatta l’agenda.

Le copie automatiche iniziano dopo un minuto e vengono controllate ogni 15 minuti mentre il processo è attivo; una copia viene scritta soltanto se i dati sono cambiati. Si conservano le ultime 14 copie automatiche e 5 copie precedenti ai ripristini. L’interfaccia mostra data e numero delle copie automatiche. Le copie sullo stesso disco non coprono la perdita del computer: l’esportazione permette di conservarne una altrove. Non viene applicato il precedente limite di 20 MB; il ripristino legge il caricamento come stream, e le snapshot vengono elaborate nella memoria del processo.

Prima di aggiornare esportare un backup, arrestare l’applicazione e installare il nuovo `.deb`. I dati nella home restano disponibili dopo aggiornamento o reinstallazione; la disinstallazione non li elimina. Lo schema è versionato: una versione non supportata blocca l’avvio senza conversioni implicite. Un futuro cambio del motore H2 richiede esportazione/importazione e verifica dedicata. Non copiare il file fisico del database tra versioni incompatibili.

## Dati iniziali della build

Su richiesta dell’utente questa consegna contiene **280 clienti e 22 nomi di polizze** dell’agenda esistente, più i sei esiti iniziali. Non contiene appuntamenti, callback, storico, account, password o token provenienti da MongoDB. Il MongoDB originale resta intatto. L’inserimento iniziale avviene una sola volta; clienti e cataloghi cancellati non vengono ripristinati al riavvio.

Il seed iniziale e l’hash PBKDF2 sono in `servizio/src/main/resources/`; i pacchetti `.deb` e le loro somme SHA-256 sono in [dist](dist/). Sono condivisi insieme ai sorgenti, al portafoglio Excel e alle immagini del progetto, su richiesta dell’utente. Le istruzioni locali per Codex non sono tracciate.

## Compilazione del backend

Sul computer dello sviluppatore servono Java 21 o superiore e il wrapper Maven incluso. Internet serve solo per ottenere dipendenze e strumenti di costruzione. Dalla radice del progetto:

```sh
./servizio/mvnw -f servizio/pom.xml clean package
AGENDA_OPEN_BROWSER=false AGENDA_DATA_DIR="$PWD/.local/agenda-wa-locale" \
  java -jar servizio/target/agenda-wa.jar
```

Il repository include già `local-password.properties` e `initial-data.json` nelle risorse: il clone può essere compilato direttamente. Il primo file contiene solo l’hash PBKDF2-HMAC-SHA256 concordato (sale 16 byte, 600.000 iterazioni, digest 32 byte); il secondo contiene clienti e polizze iniziali. Nessuna configurazione deve essere copiata dalla cartella `.local` per compilare.

`.local` conserva soltanto i database usati durante lo sviluppo, cache e strumenti di costruzione temporanei. Non contiene una seconda copia del progetto. Il programma installato salva i propri dati in `~/.local/share/agenda-wa`, secondo la convenzione Linux.

## Costruzione dei pacchetti Debian

Docker serve soltanto sul computer dello sviluppatore. [packaging/Dockerfile](packaging/Dockerfile) usa `jpackage`, il launcher nativo e i file di installazione standard; include anche la directory necessaria a `xdg-desktop-menu` su Debian minimale. Non sono aggiunti script personalizzati di avvio, arresto o migrazione.

Preparare un contesto contenente solo JAR e icona, senza il resto del progetto:

```sh
mkdir -p .local/package-input dist
cp servizio/target/agenda-wa.jar .local/package-input/agenda-wa.jar
cp packaging/agenda-wa.png .local/package-input/agenda-wa.png
```

Runtime fissato: Eclipse Temurin **21.0.12.1+1**, scaricato durante la build e verificato con SHA-256. Per AMD64:

```sh
ARCH=amd64
JDK_URL='https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jdk_x64_linux_hotspot_21.0.12.1_1.tar.gz'
JDK_SHA256='ce79869e1307ed8ee1e2baa86a412b1eb5b75d10a01006d788a6f968bcfaee94'
```

Per ARM64:

```sh
ARCH=arm64
JDK_URL='https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jdk_aarch64_linux_hotspot_21.0.12.1_1.tar.gz'
JDK_SHA256='23e37e026f12f3e706f18938ff611db3032d075b09d0879a25d06718c773e223'
```

Poi, per l’architettura scelta:

```sh
docker build --platform "linux/$ARCH" -f packaging/Dockerfile \
  --build-arg "JDK_URL=$JDK_URL" --build-arg "JDK_SHA256=$JDK_SHA256" \
  -t "agenda-wa-local-build:$ARCH" .local/package-input
docker create --platform "linux/$ARCH" --name agenda-wa-package "agenda-wa-local-build:$ARCH"
docker cp "agenda-wa-package:/out/agenda-wa_1.0.0_$ARCH.deb" dist/
docker rm agenda-wa-package
```

Il `.deb` dipende dalle normali librerie Linux elencate da `dpkg-deb --field FILE.deb Depends`, fra cui `xdg-utils`, libc e X11; non dipende da un pacchetto Java. Sono predisposte nell’immagine di verifica prima di togliere la rete. La compilazione e le immagini di verifica non fanno parte della consegna al cliente.

## Verifiche

`clean package` esegue 14 test su veri file H2 in cartelle temporanee `agenda_wa_test_*`, con server HTTP reale, account fittizi, JWT/CSRF, isolamento dei riferimenti, transazioni concorrenti, ricerche, totali, backup, Unicode e ripristino oltre 20 MB. Il contesto e la cartella dati, comprese le chiavi, vengono eliminati anche dopo un errore.

Le prove browser usano Playwright e dati fittizi in un server HTTP temporaneo, senza database. Con Playwright installato fuori dal codice applicativo e Chrome disponibile:

```sh
NODE_PATH="$PWD/.local/browser-check/node_modules" \
  CHROMIUM_EXECUTABLE_PATH='/percorso/al/browser/chrome' \
  node --test servizio/src/test/browser/*.test.cjs
```

La [prova del pacchetto](servizio/src/test/package/local-package.test.py) deve essere eseguita esclusivamente nel container isolato creato da [Verification.Dockerfile](packaging/Verification.Dockerfile), con `--network none`, dati sotto `/home/agenda/agenda_wa_test_package` e credenziali via stdin. Verifica importazione, CRUD, backup/ripristino, revoca e persistenza al riavvio; il chiamante arresta e rimuove il container al termine. Non usare questi test sull’agenda attiva.

## Due branch

- **`online`**: versione web precedente, con MongoDB e account personali.
- **`locale`**: versione offline, con H2 e pacchetti Debian.

Per cambiare versione:

```sh
git switch online
git switch locale
```

I due branch usano la stessa cartella del progetto. Il checkout cambia il codice; conserva separatamente i database delle due edizioni. `online` è il branch predefinito su GitHub.

Consultare [architettura](docs/ARCHITETTURA.md), [distribuzione](docs/DISTRIBUZIONE-LOCALE.md) e [verifiche](docs/VERIFICHE-LOCALE.md).
