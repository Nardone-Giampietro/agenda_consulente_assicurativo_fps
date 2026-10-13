# Agenda WA

Applicazione web **locale** per la gestione dell’attività di un consulente assicurativo. Il backend è Java 21 / Spring Boot 4.0.5, il database è MongoDB Community e l’interfaccia viene servita dallo stesso processo Spring Boot.

Il progetto contiene il solo modulo `servizio`, con interfaccia web locale. Le anagrafiche riguardano i clienti; appuntamenti e callback sono collegati direttamente al cliente.

Sites non viene usato: il suo runtime server è basato su Cloudflare Workers / JavaScript, mentre questo progetto deve mantenere Java/Spring Boot e MongoDB in locale. Per un’anteprima temporanea è possibile esporre il servizio locale tramite Cloudflare Quick Tunnel.

## Avvio manuale

Requisiti: Java **21 o superiore**, MongoDB Community (`mongod`), shell MongoDB (`mongosh`) e Maven oppure il wrapper Maven incluso. Il primo download delle dipendenze Maven richiede Internet; una volta compilato il pacchetto, l’applicazione, i font e tutte le risorse funzionano localmente.

Il database `agenda_wa` e il replica set locale `rs0` sono già configurati su questo computer. I file persistono in `.local/mongodb`, esclusa da Git. Gli avvii successivi riutilizzano questa cartella.

### Avviare MongoDB

Dalla cartella del progetto, in un terminale dedicato:

```sh
mkdir -p .local/mongodb
mongod --dbpath .local/mongodb --bind_ip 127.0.0.1 --port 27018 --replSet rs0 \
  --logpath .local/mongodb/mongod.log --pidfilepath .local/mongodb/mongod.pid
```

Il processo resta in esecuzione nel terminale. Se il MongoDB del progetto è già attivo, passare direttamente all’avvio dell’applicazione.

Solo per un’installazione nuova con una cartella dati vuota, inizializzare il replica set da un secondo terminale:

```sh
mongosh --quiet --norc 'mongodb://127.0.0.1:27018/?directConnection=true' \
  --eval 'rs.initiate({_id:"rs0",members:[{_id:0,host:"127.0.0.1:27018"}]})'
```

Questo passaggio non va ripetuto sul database attuale, già configurato. Per verificare che il nodo sia pronto:

```sh
mongosh --quiet --norc 'mongodb://127.0.0.1:27018/?replicaSet=rs0&serverSelectionTimeoutMS=5000' \
  --eval 'db.hello().isWritablePrimary'
```

Il risultato deve essere `true`. Le transazioni richiedono un replica set anche con un solo nodo; un server MongoDB standalone non è sufficiente.

### Avviare l’applicazione

In un altro terminale, dalla cartella del progetto:

```sh
mvn package
java -jar servizio/target/agenda-wa.jar
```

In alternativa a Maven installato, compilare con `./servizio/mvnw -f servizio/pom.xml package`.

Aprire **http://127.0.0.1:8080**. Per usare un’altra porta web:

```sh
PORT=8090 java -jar servizio/target/agenda-wa.jar
```

La connessione predefinita è:

```text
mongodb://127.0.0.1:27018/agenda_wa?replicaSet=rs0&serverSelectionTimeoutMS=5000
```

Per collegarsi a un altro MongoDB locale già configurato:

```sh
MONGODB_URI='mongodb://127.0.0.1:27017/agenda_wa?replicaSet=rs0' \
  java -jar servizio/target/agenda-wa.jar
```

Per arrestare Spring Boot o MongoDB, premere **Ctrl+C** nel rispettivo terminale. I dati restano disponibili per l’avvio successivo. Applicazione e database del progetto ascoltano soltanto sull’indirizzo locale. L’applicazione richiede registrazione o accesso con email e password. Ogni account può leggere e modificare esclusivamente la propria agenda, anche quando il servizio è raggiunto tramite un tunnel locale.

Per condividere un’anteprima con `cloudflared` già installato, mantenere attivi MongoDB e l’applicazione e avviare in un altro terminale:

```sh
cloudflared tunnel --url http://127.0.0.1:8080
```

Per accedere esclusivamente tramite HTTPS, avviare Spring Boot con `AGENDA_SECURE_COOKIES=true` prima del tunnel, così il browser invia il cookie di accesso soltanto su connessioni sicure. Con questa opzione usare l’indirizzo HTTPS del tunnel anche per il login. Condividere l’indirizzo HTTPS `trycloudflare.com` mostrato dal comando. Premere **Ctrl+C** nel terminale del tunnel per interrompere la condivisione. Il tunnel espone il servizio web; MongoDB resta locale.

### Indice per la ricerca degli appuntamenti

All’avvio, dopo la conversione dei documenti esistenti, il backend crea in modo idempotente l’indice composto `appuntamento_testo`: `ownerId` ascendente e campi testuali `cliente.nome`, `cliente.cognome`, `cliente.nomeLegale`, `note`. La lingua `none` conserva parole come «Di» e non applica stemming: `rinnovo` trova `rinnovo`, mentre `rinn` e `rinnovi` non lo trovano. `rossi rinnovo` richiede entrambe le parole, anche in campi diversi; virgolette e trattini dell’input sono separatori, senza sintassi speciale di negazione o frasi. La ricerca clienti conserva il comportamento precedente per frammenti.

Per creare lo stesso indice manualmente sul database locale configurato:

```sh
mongosh --quiet --norc 'mongodb://127.0.0.1:27018/agenda_wa?replicaSet=rs0&serverSelectionTimeoutMS=5000' \
  --eval 'db.appuntamenti.createIndex({ownerId:1,"cliente.nome":"text","cliente.cognome":"text","cliente.nomeLegale":"text",note:"text"},{name:"appuntamento_testo",default_language:"none"})'
```

L’indice usa le generalità già denormalizzate negli appuntamenti e si aggiorna automaticamente al salvataggio delle note o alla rinomina del cliente. Elenco e riepilogo usano lo stesso `$text`, sempre con `ownerId`, date ed esito; per più parole, un controllo aggiuntivo dei confini impedisce corrispondenze parziali nel controllo delle frasi di MongoDB; l’ordinamento resta per data decrescente e ID crescente. La costruzione iniziale indicizza anche i documenti esistenti. MongoDB consente un solo indice testuale per collezione: su un database diverso con un indice preesistente incompatibile, l’avvio segnala il conflitto senza eliminarlo automaticamente. Riferimenti: [operatore `$text`](https://www.mongodb.com/docs/manual/reference/operator/query/text/) e [lingua dell’indice](https://www.mongodb.com/docs/manual/core/indexes/index-types/index-text/specify-text-index-language/).

## Account e accesso

Alla prima apertura, scegliere **Registrati** e inserire nome, cognome, email e password di almeno **12 caratteri** (massimo 128). Le email non distinguono maiuscole e minuscole. La registrazione apre subito l’agenda; agli accessi successivi bastano email e password. La barra superiore mostra cognome e nome dell’account, con il pulsante **Esci** sempre visibile anche su mobile.

I dati dimostrativi già presenti sono associati a **nashj760@gmail.com**, come indicato dall’utente. Accedere con le credenziali scelte durante la registrazione per ritrovarli. Non è stata impostata una password predefinita. Gli altri account iniziano senza clienti o attività, con una copia personale dei sei esiti e delle 22 polizze iniziali ricavate dalla colonna PRODOTTO di `Finale Portafoglio Jenica.xlsx`. Anche cataloghi, riepiloghi, filtri, anteprime e cancellazioni collettive riguardano soltanto l’utente corrente.

Il 5 ottobre 2026, su conferma dell’utente, i precedenti 32 clienti, 72 appuntamenti e 28 callback di questa agenda sono stati sostituiti con **280 clienti** estratti da `Finale Portafoglio Jenica.xlsx` e nuove attività dimostrative. `Foglio1!A11:A566` contiene 549 occorrenze e 280 nominativi unici: **264 persone fisiche e 16 soggetti di tipo Azienda**, incluso `COND. SOSPELLO 145`. Le ditte e i loro titolari presenti separatamente nel file restano clienti distinti. La separazione dei nomi e dei cognomi composti è stata confermata dall’utente.

Sono presenti **560 appuntamenti**, due per cliente, distribuiti dal 1° gennaio al 31 dicembre 2026, e **140 callback**, di cui **28 da pianificare**. I callback hanno una data dell’appuntamento esistente per lo stesso cliente; le date di ricontatto, quando presenti, non la precedono. Esiti, polizze e premi sono simulati usando i cataloghi personali esistenti, conservati insieme all’account e ai metadati. Le note `[DEMO]` identificano le attività dimostrative: questi dati sono destinati alle prove dell’utente e vanno conservati durante la pulizia dei database di verifica. Una copia dei soli clienti e delle attività precedenti è conservata in `.local/backups/portafoglio-jenica-prima-sostituzione-2026-10-05-3afb85e1-1619-4ff5-a56c-92b27ef46e54.ejson`, esclusa da Git e senza credenziali.

Il server emette JWT firmati **HS256**, validi **2 ore**, in un cookie `HttpOnly`, `SameSite=Strict`, limitato a `/api`. Il JWT non è conservato in localStorage né restituito a JavaScript; le modifiche richiedono anche un token CSRF. Il browser ripristina la sessione solo dopo la verifica del server e torna al login alla scadenza, svuotando pagine, moduli e dati in memoria. **Esci** invalida anche gli altri JWT già emessi per lo stesso account: occorre accedere nuovamente su eventuali altre finestre o dispositivi.

Le password sono memorizzate come hash PBKDF2-HMAC-SHA256 con sale casuale e 600.000 iterazioni. Dopo dieci tentativi errati per la stessa email e indirizzo, l’accesso viene temporaneamente limitato per 15 minuti. La chiave di firma viene generata casualmente al primo avvio e conservata in **`.local/security/jwt.key`**, esclusa da Git e leggibile solo dall’utente del sistema su macOS/Linux. Conservarla insieme ai dati; cancellarla o sostituirla invalida le sessioni esistenti. `AGENDA_JWT_KEY_FILE` permette di indicare un percorso diverso. La cartella `.local` è relativa alla directory dalla quale viene eseguito il comando Java: avviare dalla cartella del progetto come negli esempi.

## Funzioni

La barra superiore mostra il nome dell’utente registrato. Il pulsante **Aggiorna dati**, con icona ed etichetta visibile, ricarica i dati della pagina corrente mantenendo i filtri e la pagina selezionata; durante il caricamento mostra «Aggiornamento…» ed è disabilitato. Al termine conferma «Dati aggiornati.».

- **Panoramica:** appuntamenti, premi totali, prossimi incontri, callback di oggi e distribuzione generale degli esiti.
- **Clienti:** persone fisiche e aziende in un unico elenco alfabetico, con telefono facoltativo (massimo 40 caratteri, conservato come testo). La conversione iniziale ha aggiunto il telefono vuoto ai clienti esistenti, senza inventare numeri. Il 6 ottobre 2026 i telefoni sono stati ricavati da `Finale Portafoglio Jenica.xlsx`, colonne `Foglio1!E11:G566`, e associati alle 280 anagrafiche tramite il contraente della colonna A: **26 clienti con telefono e 254 con campo vuoto**. I 27 numeri distinti sono conservati come testo; le ripetizioni nelle righe e nelle colonne sono eliminate per cliente, mentre i due numeri distinti di un cliente sono conservati nello stesso campo separati da ` / `. Tre celle contenenti soltanto `0` sono considerate prive di numero. L’importazione aggiorna esclusivamente `telefono` per l’account proprietario dei dati, conservando ID, generalità, storico, appuntamenti, callback, cataloghi e account. La copia precedente dei documenti cliente e il riepilogo dell’importazione sono in `.local/backups/telefoni-portafoglio-2026-10-06-2de0fe9e-2b81-4bd8-b827-18925c4eefe2`, esclusa da Git e senza credenziali. Su desktop il telefono ha una colonna dedicata; fino a 780 px compare sotto il nome. Un clic sulla riga o Invio/Spazio sul nome apre lo storico del cliente: soltanto data ed esito, dal più recente, con 20 appuntamenti per pagina. Selezionando una voce vengono caricati tutti i dettagli, con **Torna allo storico** per conservare la pagina di provenienza. In creazione è proposta **Persona fisica**, con nome e cognome obbligatori; scegliendo **Azienda** è richiesto il solo **Nome legale**. Il tipo resta fisso dopo il salvataggio, mentre le generalità sono modificabili. Ricerca per nome, cognome o nome legale; ordinamento sul solo cognome delle persone e sul nome legale delle aziende. Nome e cognome vengono salvati in minuscolo e mostrati nell’interfaccia con l’iniziale maiuscola di ogni componente (es. «Marco Giovanni», «De Pietro»); il nome legale è salvato e mostrato interamente in maiuscolo (es. «COSTRUZIONI»). Non sono presenti i campi `chiave` o `ordine` nei clienti.
- **Appuntamenti:** inserimento, modifica, cancellazione, cliente obbligatorio, esito, polizza facoltativa, premio con due decimali e note; ordinamento dal più recente; filtri per esito, date e testo su cliente e note dell’appuntamento; la ricerca usa `$text` di MongoDB su parole intere, senza distinguere maiuscole e accenti, e richiede tutte le parole, anche distribuite tra i campi. La punteggiatura separa le parole; un testo composto soltanto da simboli non trova risultati. Si applica con **Filtra** o Invio, mentre **Azzera** svuota anche il testo; riepilogo aggiornato sull’intero insieme filtrato. Un clic o tap sulla riga, nell’elenco o nella panoramica, apre i dettagli in sola lettura. Il pulsante **Modifica** dentro la finestra attiva i campi: **Salva** aggiorna i dati e torna ai dettagli, **Annulla** scarta le modifiche e ripristina i dettagli originali. Il pulsante di modifica nell’elenco apre direttamente il modulo. I dettagli sono accessibili anche da tastiera tramite il pulsante sulla data o sul nome del cliente. La consultazione aperta dall’elenco, dalla panoramica o dall’anteprima di cancellazione mostra il telefono corrente del cliente (oppure **Non inserito**) e il pulsante **Appuntamenti del cliente**. Da qui si apre lo stesso storico, con **Torna all’appuntamento** per rientrare nell’incontro iniziale; i dettagli aperti dallo storico non ripetono il telefono. Modifica, annullamento e salvataggio conservano il ritorno allo storico.
- **Callback:** cliente obbligatorio; creazione insieme all’appuntamento oppure dalla sezione dedicata; modifica e cancellazione; data facoltativa per quelli da pianificare. Nell’elenco vengono prima quelli fissati per oggi, evidenziati in lilla con il badge **Oggi**; seguono tutti gli altri nell’ordine precedente (da pianificare, poi data crescente). La priorità precede la paginazione e rispetta i filtri per data; il giorno corrente è quello dell’agenda in Europe/Rome.
- **Impostazioni:** gestione di esiti e polizze in elenchi inizialmente chiusi. Un clic sul titolo apre o richiude il relativo elenco con un’animazione fluida; i due pannelli sono indipendenti. I pulsanti per aggiungere valori restano visibili e lo stato di apertura viene conservato dopo una modifica o **Aggiorna dati**. L’apertura funziona anche con Invio/Spazio e rispetta la preferenza di movimento ridotto. Sono disponibili inoltre la cancellazione degli appuntamenti filtrati per date e la cancellazione di tutti i clienti con conferma esplicita.

Ogni documento `clienti` conserva `telefono` e un array `appuntamenti` con **soltanto `{id, data, esito}`** per ciascun appuntamento del cliente (partial document embedding), ordinato per data decrescente e ID crescente nelle parità. Note, polizza e premio restano nella collezione `appuntamenti` e vengono letti quando si apre il dettaglio. Le API `GET /api/clienti/{id}` e `GET /api/clienti/{id}/appuntamenti?page=0&size=20` richiedono l’utente proprietario; lo storico usa `$size`/`$slice` sull’array incorporato, senza leggere i documenti completi. L’elenco clienti esclude lo storico dalla risposta e dalla proiezione finale. Creazione, modifica, cambio cliente, cancellazione singola o filtrata e rinomina dell’esito aggiornano il documento cliente nella stessa transazione. La modifica dell’anagrafica conserva lo storico.

All’avvio, dopo le conversioni degli appuntamenti, `ClientHistorySchemaRepository` verifica i riferimenti e ricostruisce lo storico degli appuntamenti già presenti, aggiungendo il telefono vuoto soltanto dove manca. La conversione è transazionale e idempotente; conserva telefoni già inseriti, dati dimostrativi, attività, account e cataloghi, e si interrompe se un riferimento è assente o di un altro proprietario. Il documento MongoDB mantiene il limite di 16 MiB: lo storico incorporato è adatto al volume attuale dell’agenda; una crescita molto elevata per singolo cliente richiederebbe una diversa organizzazione dello storico.

Gli appuntamenti salvano `cliente: {tipo, nome, cognome}` per le persone fisiche oppure `cliente: {tipo, nomeLegale}` per le aziende, `esito` e `polizza` direttamente nel documento MongoDB. Non persistono `esitoId` o `polizzaId`; `clientId` resta per il collegamento all’anagrafica e le cancellazioni a cascata. Le rinomine del cliente o dei cataloghi aggiornano i valori denormalizzati nella stessa transazione. Letture delle generalità e riepiloghi usano i dati dell’appuntamento; una sola lettura del catalogo per pagina ricostruisce gli ID richiesti dai moduli delle API esistenti.

Al primo avvio della versione aggiornata, prima di accettare richieste, l’applicazione aggiorna i clienti e le copie negli appuntamenti al formato corrente: nome/cognome in minuscolo e nome legale in maiuscolo. I clienti precedenti senza tipo sono persone fisiche; i campi derivati `chiave` e `ordine` vengono rimossi dai clienti. Converte inoltre gli appuntamenti del formato precedente. Le conversioni sono transazionali e rimuovono i soli indici sostituiti (nomi dei clienti e vecchi ID dei cataloghi). Ogni riferimento viene verificato con lo stesso proprietario e il tipo corretto; riferimenti mancanti o estranei interrompono l’avvio e annullano la conversione. La conversione è idempotente: gli avvii successivi conservano i documenti già aggiornati. Non elimina appuntamenti, clienti, callback, cataloghi o account.

Gli elenchi sono paginati. Negli elenchi, i filtri con una sola data definiscono un intervallo aperto: da quella data in avanti o fino a quella data. Nessuna data significa l’intero periodo.

Per eliminare più appuntamenti, aprire **Impostazioni → Gestione dei dati → Elimina appuntamenti**. Scegliere **Tutte le date**, **Intervallo di date** (estremi inclusi), **Prima di una data** o **Dopo una data** (data scelta esclusa). **Verifica appuntamenti** mostra il numero totale di risultati, un’anteprima dei primi cinque e il riepilogo dei filtri. Modificare un filtro invalida l’anteprima: occorre verificarla di nuovo. La cancellazione richiede il pulsante **Elimina appuntamenti** e la conferma digitando **ELIMINA**; elimina tutti i risultati, anche quelli oltre l’anteprima, conservando clienti e callback. Se il conteggio cambia prima della conferma, l’operazione viene rifiutata e occorre verificare nuovamente. **Elimina tutti i clienti** resta disponibile come azione distinta.

### Integrità dei dati

- Eliminare un **cliente** elimina anche i suoi appuntamenti e callback.
- Eliminare un **appuntamento**, anche con il reset collettivo, conserva i callback.
- Modificare il nome del cliente aggiorna le etichette delle attività senza cambiarne il collegamento.
- Creare un appuntamento con callback è un’unica transazione: entrambi vengono salvati oppure nessuno.
- Ogni cliente ha un ID distinto: sono consentiti clienti omonimi. Il cliente indicato su un’attività deve appartenere all’utente corrente.
- Un esito o una polizza usati da appuntamenti possono essere rinominati; per eliminarli occorre prima rimuovere i riferimenti dagli appuntamenti.
- I cataloghi personali vengono inizializzati durante la registrazione, senza ripristinare valori rimossi a ogni avvio. I nomi iniziali delle polizze in `DefaultCatalogs.POLICIES` sono i 22 valori distinti della colonna PRODOTTO (`Foglio1!L11:L572`) di `Finale Portafoglio Jenica.xlsx`: si conservano i codici e le diciture del file, eliminando soltanto duplicati e spazi superflui. Il file Excel viene usato come fonte dei nomi, senza importarne le anagrafiche. I cataloghi degli account già esistenti non vengono sovrascritti all’avvio.

## Verifiche

Test unitari, senza un database:

```sh
mvn test
```

I test di integrazione vengono saltati quando la variabile dedicata non è impostata. Per eseguirli con MongoDB locale:

```sh
AGENDA_TEST_MONGO_URI='mongodb://127.0.0.1:27018/agenda_wa_test_backend?replicaSet=rs0&serverSelectionTimeoutMS=5000' \
  mvn test
```

Il nome del database deve iniziare con `agenda_wa_test_`: i test rifiutano nomi di produzione. Al termine della classe di integrazione il database di test viene eliminato interamente, comprese collezioni, cataloghi e indici, anche se un’asserzione fallisce. Sono verificati precisione dei premi, date e filtri, rollback, clienti indipendenti e omonimi, attività collegate ai clienti, rinomina dei clienti e modifica del cliente dell’appuntamento senza cambiare i callback esistenti, cancellazione dei clienti, reset, cataloghi, errori HTTP e richieste autenticate con origine esterna, senza abilitare CORS.

L’interfaccia è stata verificata nel browser su un database di prova separato, con creazione e modifica di clienti/appuntamenti/callback, filtri, cataloghi, conferme e disposizione compatta. I dati usati nelle verifiche automatiche vengono rimossi al termine.

La consultazione degli appuntamenti è stata verificata da panoramica ed elenco, con dettagli in sola lettura, passaggio alla modifica, annullamento senza salvataggio e salvataggio persistente con ritorno ai dettagli aggiornati. Sono stati controllati accesso da tastiera e disposizione delle finestre da 320 a 1440 px. Al termine delle verifiche arrestare i servizi applicativi avviati o usati e controllare che le loro porte siano libere; MongoDB può restare attivo, secondo la regola permanente in [AGENTS.md](AGENTS.md).

Il modulo **Nuovo appuntamento** mantiene i campi entro il dialogo anche con nomi di clienti, esiti e polizze lunghi: due colonne sopra 780 px, una colonna fino a 780 px, scorrimento verticale nelle finestre basse. Le verifiche coprono undici dimensioni da 320 a 1920 px, Callback attivo/disattivo, modifica, moduli condivisi di clienti e cataloghi, campi obbligatori, errori di salvataggio e tastiera. Creazione con e senza Callback, modifica e persistenza dopo il ricaricamento sono state provate anche con il backend reale e un database isolato. Riferimenti: [desktop](docs/web/nuovo-appuntamento.jpg), [mobile](docs/web/nuovo-appuntamento-mobile.jpg) e [Callback mobile con scorrimento](docs/web/nuovo-appuntamento-callback-mobile.jpg).

Telefono e storico sono verificati da test backend su migrazione idempotente, proiezione esatta dei tre campi, ordinamento, paginazione, rinomina, spostamento, cancellazione, rollback e isolamento tra due utenti. Le prove browser includono caricamento dei dettagli soltanto alla selezione, ritorno allo storico, modifica, telefono, nomi lunghi, elenchi vuoti, errori e risposte tardive dopo chiusura o scadenza della sessione. Riferimenti: [storico desktop](docs/web/cliente-storico.jpg), [mobile](docs/web/cliente-storico-mobile.jpg) e [dettaglio dallo storico](docs/web/cliente-storico-dettagli.jpg).

I test di regressione del layout usano HTML, CSS e moduli reali con risposte API fittizie, senza avviare Spring Boot o modificare MongoDB. Richiedono Node.js e Playwright; le dipendenze di verifica possono essere installate fuori dal progetto:

```sh
npm install --prefix /tmp/agenda-wa-browser-tests --no-save playwright
/tmp/agenda-wa-browser-tests/node_modules/.bin/playwright install chromium
NODE_PATH=/tmp/agenda-wa-browser-tests/node_modules \
  node --test servizio/src/test/browser/editor-layout.test.cjs
```

Se si usa Chrome già installato, impostare `CHROMIUM_EXECUTABLE_PATH` al percorso del suo eseguibile al posto del download di Chromium. I test chiudono automaticamente il browser e il server locale temporaneo.

Il test del ciclo di creazione, modifica e cancellazione dei callback verifica anche `/api/callback`, la creazione tramite `creaCallback`/`dataCallback` e i campi `callbackOggi`/`callback` dei riepiloghi.

I test della cancellazione filtrata verificano estremi inclusi nell’intervallo, estremi esclusi per «prima» e «dopo», combinazione con l’esito, date e filtri non validi, conservazione di anagrafiche e callback, rifiuto di un conteggio cambiato e cancellazione di tutti i risultati oltre le cinque righe dell’anteprima. Il flusso è stato provato nel browser su una copia temporanea dell’agenda, anche a 320 e 390 px, con annullamento, selezione vuota, errori e cancellazione effettiva. La suite completa esegue **45 test**, inclusi quattordici test dedicati ad autenticazione e isolamento: registrazione e login, validazione delle credenziali, password protette, email univoche, accesso anonimo negato, filtri e riepiloghi personali, ID e riferimenti di altri utenti rifiutati, cancellazioni e cataloghi indipendenti, CSRF, JWT alterati o scaduti, logout, limitazione dei tentativi errati e assegnazione dei dati esistenti. Sono inoltre verificati la lettura e il salvataggio dei documenti MongoDB con i nomi persistiti esistenti e gli insiemi dei campi JSON delle API dopo la separazione dei DTO. Il test della ricerca degli appuntamenti copre parole distribuite tra generalità e note, accenti, esclusione dei frammenti, punteggiatura senza operatori impliciti, date/esito, paginazione, riepiloghi coerenti, rinomina e isolamento tra due utenti. Un test MongoDB verifica inoltre l’unicità e l’idempotenza dell’indice, il prefisso per proprietario, la lingua `none`, l’uso effettivo dell’indice tramite `explain`, la conservazione delle parole brevi e l’assenza di stemming. Anche i file delle chiavi usate nei test vengono rimossi. I nuovi controlli verificano dati aziendali denormalizzati, rinomina del nome legale, ordinamento misto e paginazione, validazione delle generalità, tipo fisso in entrambe le direzioni e conversione idempotente delle persone e delle copie precedenti.

### Dati dimostrativi dell’agenda

Il 3 ottobre 2026, su richiesta dell’utente, sono stati inseriti nel database locale `agenda_wa` **32 clienti, 72 appuntamenti e 28 callback**. Il 5 ottobre 2026 i 32 clienti sono stati classificati come persone fisiche e il tipo è stato aggiunto alle copie nei 72 appuntamenti, conservando gli altri dati. Sono dati fittizi salvati tramite le API dell’applicazione; restano disponibili dopo il riavvio e non vengono rigenerati automaticamente. Non sono stati aggiunti script né collezioni dedicate.

- Gli appuntamenti vanno dal **5 giugno al 2 novembre 2026**, con tutti e sei gli esiti iniziali. Il 5 ottobre 2026 il catalogo è stato sostituito con le 22 polizze ricavate dall’Excel; soltanto i 22 appuntamenti già associati a una polizza hanno ricevuto un nuovo nome casuale, mentre i 50 senza polizza sono rimasti invariati. Dieci sottoscrizioni hanno premi diversi, anche con centesimi, per un totale di **7.879,89 €**; gli altri incontri hanno premio zero.
- Il cliente «Luca Rossi» ha più incontri; un secondo cliente omonimo ha un ID distinto. Sono presenti anche nomi composti, accenti e apostrofi per provare la ricerca.
- I callback includono date passate e future, **7 callback per il 3 ottobre 2026** e **7 senza data**, da pianificare. Dieci sono stati creati insieme all’appuntamento e diciotto dalla sezione Callback. L’indicatore «oggi» cambia con la data corrente.
- I clienti **Claudio Monti, Marta Pellegrini e Dario Ferri** non hanno attività, per provare modifica e cancellazione di anagrafiche libere.
- In Impostazioni, **Esito dimostrativo libero** non è usato da appuntamenti, per provare rinomina e cancellazione di valori liberi. **Polizza dimostrativa libera** è stata rimossa insieme al vecchio catalogo delle polizze nella sostituzione richiesta il 5 ottobre 2026.

Questi dati sono riservati all’account con email **nashj760@gmail.com**. Le note delle attività iniziano con `[DEMO]`. Gli elenchi superano la dimensione di una pagina per consentire di provare la paginazione. Ricerca, filtri per esito e date, collegamenti e riepiloghi sono stati verificati sulle API. Questi dati dimostrativi sono destinati alle prove dell’utente e vanno conservati durante la pulizia dei database temporanei di test. Modifiche, cancellazioni e reset dall’interfaccia agiscono normalmente anche su questi dati.

### Pulizia dopo le modifiche al database

Al termine di ogni task sul database, rimuovere i database e le collezioni di test creati durante le verifiche, comprese quelle nel browser, e le collezioni dell’agenda confermate obsolete. Se un processo di test viene interrotto prima della pulizia automatica, completare la pulizia manualmente. Conservare i dati applicativi attivi e i database di sistema MongoDB. La regola permanente per il lavoro sul progetto è in [AGENTS.md](AGENTS.md).

Le collezioni necessarie in `agenda_wa` sono `utenti`, `clienti`, `appuntamenti`, `callback`, `cataloghi` e `metadati`. I documenti dell’agenda hanno `ownerId`, assegnato dal server. I marcatori personali in `metadati` preservano i cataloghi; la prenotazione dell’email proprietaria dei dati dimostrativi viene rimossa dopo la sua registrazione. Non vengono eseguite migrazioni o cancellazioni automatiche all’avvio.

## Grafica e struttura

La grafica segue `AXA_Guida_Visiva.png`: blu `#00008F`, superfici chiare, accenti lilla e menta, icone semplici e pulsanti arrotondati. [AXA Italia](https://www.axa.it/) usa Source Sans Pro e Publico: Source Sans Pro è incluso localmente con la sua licenza OFL; per i titoli si usa Publico se installato, altrimenti Georgia. Il font proprietario Publico non è distribuito nel progetto.

Le regole dello stile corrente sono nella skill locale [agenda-wa-ui](.agents/skills/agenda-wa-ui/SKILL.md). [AGENTS.md](AGENTS.md) ne richiede l’uso per le aggiunte e le modifiche grafiche, con aggiornamento della skill nello stesso task dopo la verifica dell’interfaccia. La guida deve descrivere lo stile risultante, così gli interventi successivi restano coerenti.

Accesso e registrazione sono in `servizio/src/main/resources/static/index.html` e `js/auth.js`; il client API autenticato è in `js/api.js`. I riferimenti visivi aggiornati sono in [docs/web/accesso.jpg](docs/web/accesso.jpg) e [docs/web/registrazione-mobile.jpg](docs/web/registrazione-mobile.jpg).

Il frontend è HTML/CSS/JavaScript a moduli, senza una catena di compilazione o dipendenze da CDN. Il backend segue la struttura di `LSMD_Project_MonGoFundMe`: `controllers`, DTO separati per area in `DTO`, documenti in `entity`, `mapper`, contratti in `service/interfaces`, implementazioni `*ServiceImpl` in `service`, `repository`, `exception`, `config` e `security`. I servizi coordinano regole e transazioni; i repository contengono gli accessi MongoDB. La ristrutturazione conserva API, dati persistiti e frontend. Dettagli delle API e del modello documentale in [docs/ARCHITETTURA.md](docs/ARCHITETTURA.md). Le immagini in `docs/images` conservano le schermate della precedente versione desktop come riferimento storico; la documentazione JavaFX generata è stata rimossa.
