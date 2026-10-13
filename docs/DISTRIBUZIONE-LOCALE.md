# Distribuzione locale su Chromebook

Decisioni concordate: Linux già abilitato, browser come interfaccia, singolo computer e utilizzatore, funzionamento senza Internet, nessuna sincronizzazione e consegna tramite `.deb`. Docker serve per costruire e verificare il pacchetto sul computer dello sviluppatore. Non è richiesto al destinatario.

Q5–Q8 sono risolte: H2 su disco, backup automatici ed esportazione/ripristino, password fissa fornita dall’utente e incorporata come hash, importazione dei soli clienti e nomi delle polizze correnti. Nessuna registrazione, recupero o attività dimostrativa nella nuova agenda. Decisioni: [edizione personale](adr/0002-edizione-locale-personale.md), [H2 e backup](adr/0003-h2-incorporato-e-backup.md), [password fissa](adr/0004-password-fissa-edizione-locale.md).

## Pacchetti e confini della verifica

Pacchetti 1.0.0 per amd64 e arm64, costruiti su Debian 12 con Temurin 21.0.12.1+1 incluso e hash del download verificato. Il launcher nativo apre il browser tramite `xdg-open` e il server ascolta sul loopback. Il pacchetto comprende il collegamento al menu Office e la directory richiesta da xdg-desktop-menu anche su Debian minimale.

Verifica prevista e riproducibile: installazione del `.deb` con `dpkg` dentro un container Debian senza rete, nessun Java di sistema, utente Linux ordinario, seed privato, login reale, creazione di attività, export/restore, revoca e riavvio con conservazione dei dati. Le normali librerie Debian grafiche vengono predisposte prima della prova offline. La dipendenza esatta è dichiarata dal pacchetto; non si afferma compatibilità con qualsiasi installazione Debian priva di quelle librerie.

Il bind `127.0.0.1` è coerente con il forwarding ufficiale ChromeOS: chunneld accetta localhost sul sistema host e chunnel si collega a localhost nel container. Non occorre esporre il backend su tutte le interfacce per questa via. Fonti primarie: [vm_tools README](https://chromium.googlesource.com/chromiumos/platform2/+/HEAD/vm_tools/README.md) e [Garcon ConnectChunnel](https://chromium.googlesource.com/chromiumos/platform2/+/HEAD/vm_tools/garcon/service_impl.cc).

I test container non riproducono il browser del sistema host ChromeOS, il menu Chromebook o la sospensione del dispositivo. Prima di qualificare la distribuzione come verificata su Chromebook occorre una prova fisica: installazione senza rete, apertura dal menu, raggiungibilità localhost da Chrome, riavvio/sospensione e aggiornamento con dati conservati. Font e risorse web sono già inclusi e verificati offline nel browser.

La build privata iniziale contiene 280 clienti e 22 polizze, con 0 appuntamenti e 0 callback. I pacchetti contengono questi dati e l’hash privato e non vengono pubblicati nel repository. Per destinatari diversi costruire un pacchetto senza quel seed. Il MongoDB originale è mantenuto integro come punto di ripresa dell’edizione precedente.

## Versionamento e operazioni

`main` e `codex/versione-online`, tag `versione-online-2026-10-13`, conservano la versione precedente. `codex/versione-locale` contiene la nuova edizione. `codex/archivio-originale` conserva soltanto sul computer dello sviluppatore il commit iniziale completo con il materiale riservato. Il checkout non cambia o migra i dati: H2 e MongoDB hanno destinazioni separate.

Per uso, build, aggiornamento e comandi manuali consultare il [README](../README.md). Non sono introdotti script personalizzati di avvio, arresto o migrazione. Le copie locali automatiche non sostituiscono un backup conservato fuori dal computer; il file fisico H2 non deve essere copiato mentre l’applicazione è attiva o riutilizzato alla cieca dopo un cambio di versione del motore.
