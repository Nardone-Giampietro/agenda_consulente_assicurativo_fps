# Architettura dell’agenda locale

## Componenti

Un unico pacchetto Spring Boot contiene API e risorse web. Il browser usa richieste sullo stesso indirizzo del server: non servono un server frontend separato, CORS o Node.js durante l’utilizzo.

La struttura segue il progetto di riferimento `LSMD_Project_MonGoFundMe`: DTO raggruppati per area, documenti separati, mapper, interfacce dei servizi, implementazioni e repository Spring Data. Il package radice è `xyz.nardone.agenda_wa.servizio`.

| Package | Responsabilità |
| --- | --- |
| `controllers` | Endpoint REST, validazione degli ingressi, stato HTTP e cookie; dipendenza dalle interfacce dei servizi |
| `DTO/activity`, `DTO/client`, `DTO/catalog`, `DTO/auth`, `DTO/overview`, `DTO/common` | Richieste validate e risposte esplicite, indipendenti dalla persistenza |
| `entity` | Documenti MongoDB immutabili, indici, date e importi |
| `mapper` | Conversioni tra richieste, entità e risposte; nessun accesso al database |
| `service/interfaces` | Contratti applicativi di autenticazione, anagrafiche, attività, cataloghi e panoramica |
| `service` | Classi `*ServiceImpl`: regole, controllo dei riferimenti, transazioni e coordinamento dei repository |
| `repository` | CRUD Spring Data con vincolo al proprietario; frammenti personalizzati per ricerche dinamiche, selezioni e aggregazioni |
| `repository/query` | Costruzione condivisa dei filtri e della paginazione MongoDB |
| `exception` | Gestione centralizzata degli errori con `ProblemDetail` e messaggi applicativi |
| `config` | Transazioni MongoDB, conversione date, orologio e valori dei cataloghi iniziali |
| `security` | JWT, CSRF, chiave, hash delle password, vincolo utente e limitazione dei tentativi |
| `util` | Normalizzazione dei nomi e degli ID facoltativi |
| `static/js` | API client, componenti, viste, dialoghi e navigazione |

```text
controllers → service/interfaces ← service/*ServiceImpl → repository → MongoDB
      ↓                                  ↓
     DTO                              mapper ↔ entity / DTO
```

Tutte le dipendenze vengono iniettate nei costruttori. Controller e servizi non usano `MongoTemplate`, query MongoDB o `Document` BSON. I cinque repository delle entità estendono `MongoRepository`; i metodi derivati e gli aggiornamenti dichiarati con `@Query`/`@Update` includono il proprietario. I frammenti `*RepositoryCustom` / `*RepositoryCustomImpl` usano `MongoTemplate` per i filtri variabili e i riepiloghi. `OwnedQuerySupport` applica sempre `ownerId` prima di eseguire query, conteggi o aggregazioni. `MetadataRepository` gestisce esclusivamente i marcatori interni in `metadati`.

Il proprietario viene ricavato dai servizi tramite `UserScope.id()`. Le operazioni sui cataloghi iniziali ricevono l’ID appena creato o riservato durante la registrazione, quando ancora non esiste una sessione. Le letture globali degli utenti sono limitate ad autenticazione e validazione della sessione; le risorse dell’agenda utilizzano sempre metodi con `ownerId`. I metodi generici ereditati da `MongoRepository` non vanno usati per leggere o eliminare dati applicativi senza tale vincolo.

I controller restituiscono DTO, mai documenti persistiti. Le risposte di scrittura (`AppointmentDTO`, `CallbackDTO`, `ClientDTO`, `CatalogItemDTO`) espongono i campi delle risorse correnti; `ActivityDTO` contiene invece i dettagli con etichette e `ClientSummaryDTO` l’elenco sintetico. Non viene aggiunto un contenitore JSON alle risposte. Date, esiti, cataloghi, stati HTTP e autenticazione mantengono il comportamento esistente.

Lo spostamento da `domain` a `entity` conserva gli identificatori `_class` esistenti con `@TypeAlias`. Si tratta del nome persistito dei documenti: non richiede migrazioni, riscritture dei dati o classi duplicate. Le entità mantengono tipi BSON e ID. Il nuovo schema degli appuntamenti conserva il riferimento al cliente e incorpora i nomi dei cataloghi, come descritto sotto. I test leggono documenti scritti con gli identificatori esistenti, ne modificano i dati e verificano che il formato rimanga compatibile.

## Documenti

| Collection     | Dati                                                                                   |
| -------------- | -------------------------------------------------------------------------------------- |
| `utenti` | ID, nome, cognome, email normalizzata univoca, hash della password e versione dei token |
| `clienti`      | ID, tipo fisso, nome/cognome in minuscolo per persone oppure nome legale in maiuscolo per aziende, telefono facoltativo e `appuntamenti: [{id, data, esito}]` |
| `appuntamenti` | Data, `clientId` obbligatorio, `cliente: {tipo, nome, cognome}` oppure `{tipo, nomeLegale}`, `esito` e `polizza` come stringhe senza ID, premio e note |
| `callback`     | Data facoltativa, data appuntamento, cliente obbligatorio, note |
| `cataloghi`    | Esiti e polizze con nome normalizzato univoco per utente e tipo                |
| `metadati`     | Marcatori personali dei cataloghi; prenotazione dell’email proprietaria dei dati iniziali                              |

Tutti i documenti dell’agenda, inclusi i cataloghi, contengono `ownerId`. `UserScope` lo ricava esclusivamente dal JWT verificato nel contesto Spring Security: i DTO delle richieste non accettano il proprietario. Letture, ricerche, conteggi, aggregazioni, aggiornamenti dei riferimenti e cancellazioni aggiungono questo vincolo; i riferimenti di un altro utente vengono rifiutati prima della scrittura, nella stessa transazione. I documenti senza proprietario restano inaccessibili.

Gli appuntamenti mantengono `clientId` per riferimenti e cancellazioni a cascata e incorporano le generalità in `cliente`, senza duplicare `ownerId` o ID nell’oggetto. `esito` è il nome obbligatorio, `polizza` il nome facoltativo; `esitoId` e `polizzaId` non sono persistiti. Gli alias `_class`, le date ISO e i premi Decimal128 restano invariati. Le letture degli appuntamenti non interrogano le anagrafiche; le etichette e i riepiloghi derivano direttamente dai documenti. Una lettura del catalogo personale per pagina ricostruisce gli ID delle risposte API tramite `(tipo, nome)`, per mantenere compatibili i moduli esistenti. I callback continuano a caricare i clienti in blocco per pagina.

Le richieste e i filtri API accettano gli ID dei cataloghi e li risolvono nel servizio, verificandone proprietario e tipo. Le query su appuntamenti e le aggregazioni usano `esito` e `polizza`. Un ID di esito inesistente o di un altro utente viene rifiutato con `400`. Rinomine di clienti, esiti e polizze propagano i valori incorporati con aggiornamenti multipli vincolati a `ownerId`, nella stessa transazione della modifica dell’anagrafica o del catalogo. La cancellazione di un catalogo usato resta vietata.

`AppointmentSchemaConfiguration` esegue le conversioni prima dell’avvio del server tramite `ClientSchemaRepository` e `AppointmentSchemaRepository`. La transazione risolve i vecchi riferimenti con proprietario e tipo corretti e modifica soltanto i campi interessati; un riferimento non valido annulla l’intera conversione e interrompe l’avvio. Gli altri campi e gli alias sono conservati. Dopo il commit vengono rimossi soltanto gli indici `appuntamento_esito_data` e `appuntamento_polizza`, sostituiti dagli indici sui nomi. Le generalità dei clienti e delle copie negli appuntamenti vengono normalizzate: nome/cognome in minuscolo, nome legale in maiuscolo, preservando gli accenti. I campi obsoleti `chiave` e `ordine` dei clienti vengono rimossi, insieme agli indici `cliente_nomi` e `cliente_ordine`. Gli altri campi sono conservati; le conversioni successive non modificano documenti già aggiornati.

Le date sono `LocalDate` in Java e stringhe ISO `YYYY-MM-DD` in MongoDB. Il confronto lessicografico preserva l’ordine e gli intervalli, senza conversioni di fuso orario che cambino il giorno. La data corrente è calcolata con un `Clock` nel fuso `Europe/Rome`.

I premi sono `BigDecimal` in Java e `Decimal128` in MongoDB. Il server valida valori non negativi con massimo due cifre decimali. Il riepilogo somma e raggruppa i dati nel database, anche se il browser mostra soltanto una pagina.

Gli indici composti iniziano con `ownerId` e coprono nomi, riferimento cliente, nomi di esito/polizza e date. Il catalogo è univoco per `(ownerId, tipo, chiave)`; le email degli utenti hanno un indice univoco. Sono consentiti clienti omonimi. Gli elenchi hanno ordinamento deterministico e pagine da 20 righe (massimo API: 100).

## Storico parzialmente incorporato nel cliente

`Client.AppointmentSummary` contiene solo ID, data ed esito. Il documento cliente conserva l’array `appuntamenti` ordinato per data decrescente, con ID crescente nelle parità. `ClientAppointmentDTO` espone gli stessi tre campi senza rivelare dati interni. `ClientRepositoryCustomImpl.history()` applica proprietario e ID prima della proiezione `$size`/`$slice`, con massimo 100 elementi per pagina; un cliente assente o estraneo produce `404`. Nessun documento completo dell’appuntamento è letto per consultare lo storico. Il suo dettaglio viene richiesto separatamente a `/appuntamenti/{id}`, con verifica indipendente del proprietario.

`rebuildAppointmentHistory()` legge dagli appuntamenti soltanto `_id`, `data` ed `esito`, vincolando proprietario e cliente, e aggiorna l’array nello stesso documento. Il servizio la esegue nella transazione di creazione, modifica, spostamento e cancellazione dell’appuntamento; spostare aggiorna entrambi i clienti. La cancellazione filtrata individua e blocca i clienti interessati prima del conteggio e della rimozione e ricostruisce i loro storici; la rinomina dell’esito fa lo stesso per i soli clienti coinvolti. Le scritture dell’anagrafica aggiornano le generalità e il telefono senza sostituire il documento, preservando lo storico e il contatore di revisione. Non esiste un endpoint che permetta al client di impostare lo storico o il proprietario.

`ClientHistorySchemaRepository.migrate()` viene eseguito dopo le conversioni preesistenti, prima di servire richieste. Verifica i riferimenti degli appuntamenti per proprietario, ricostruisce gli array e aggiunge il telefono vuoto ai documenti privi del campo. È idempotente, transazionale e conserva i telefoni presenti e tutti i documenti originari; non elimina dati all’avvio. L’array resta soggetto al limite MongoDB di 16 MiB per documento.

## Operazioni atomiche

Il `MongoTransactionManager` abilita le transazioni sul replica set locale. Creazione di appuntamento/callback e cancellazioni a cascata dei clienti sono operazioni atomiche.

Ogni appuntamento e callback si riferisce a un cliente obbligatorio dell’utente corrente. Modificare il nome del cliente aggiorna le etichette delle sue attività. Cambiare il cliente di un appuntamento non riassegna i callback già creati; questi si modificano dalla propria sezione.

Le scritture che dipendono da anagrafiche o cataloghi aggiornano anche un contatore `revision` sul documento di riferimento. Questo crea un conflitto di scrittura quando un’altra transazione tenta di rimuovere o modificare contemporaneamente quel riferimento. I conflitti vengono comunicati al client perché ricarichi e riprovi, senza annunciare falsi successi.

## API

Prefisso `/api`. Tutte le risorse applicative richiedono autenticazione; il vincolo al proprietario vale anche per le cancellazioni collettive e i riferimenti. Le richieste di modifica richiedono inoltre la protezione CSRF descritta sotto. Le risorse espongono `POST` per creare, `PUT /{id}` per aggiornare e `DELETE /{id}` per eliminare. Le creazioni rispondono `201`, le cancellazioni singole `204`, gli errori usano `ProblemDetail` con stato HTTP coerente. La cancellazione collettiva degli appuntamenti risponde `200` con il numero effettivamente eliminato.

| GET                       | Risultato / Parametri                                                                               |
| ------------------------- | --------------------------------------------------------------------------------------------------- |
| `/navigazione`            | Data corrente e numero dei callback odierni, senza ricalcolare il riepilogo generale                |
| `/panoramica`             | Data corrente, contatori, riepilogo generale, primi cinque prossimi appuntamenti e callback odierni |
| `/clienti`                | Pagina; `q`, `page`, `size`                                                                         |
| `/clienti/{id}`           | Generalità e telefono del cliente proprietario, senza storico |
| `/clienti/{id}/appuntamenti` | Pagina dello storico incorporato: soltanto `id`, `data`, `esito`; `page`, `size` |
| `/appuntamenti`           | Pagina; `dal`, `al`, `esitoId`, `page`, `size`                                        |
| `/appuntamenti/riepilogo` | Riepilogo dell’intero insieme filtrato con gli stessi filtri                                        |
| `/appuntamenti/selezione` | Conteggio totale e primi cinque appuntamenti da eliminare; `periodo`, `dal`, `al`, `data`, `esitoId` |
| `/appuntamenti/{id}`      | Appuntamento con etichette e riferimenti                                                            |
| `/callback`               | Pagina; `dal`, `al`, `page`, `size`                                                   |
| `/callback/{id}`          | Callback con etichette e riferimenti                                                                |
| `/cataloghi/esiti`        | Esiti                                                                                               |
| `/cataloghi/polizze`      | Polizze                                                                                             |

`DELETE /clienti` cancella anche appuntamenti e callback. `DELETE /appuntamenti` richiede gli stessi filtri di `/appuntamenti/selezione` e il parametro `attesi`, cioè il conteggio ottenuto dall’anteprima; conserva clienti e callback e restituisce `{"eliminati": N}`.

Il parametro obbligatorio `periodo` accetta `TUTTE` (senza date), `INTERVALLO` (`dal` e `al`, entrambi obbligatori e inclusi), `PRIMA` (`data`, esclusa) e `DOPO` (`data`, esclusa). `esitoId` è facoltativo nelle API per compatibilità e, se indicato, deve identificare un esito dell’utente corrente. La cancellazione nelle Impostazioni invia solo periodo e date e non mostra un filtro per esito; l’esito resta una colonna informativa dell’anteprima. Date invertite, assenti o incompatibili con il periodo scelto producono `400`, anche sulla richiesta di cancellazione. `repository.query.Queries.appointmentSelection()` riceve il nome dell’esito risolto dal servizio e costruisce la stessa query MongoDB per anteprima e cancellazione. Il servizio riconta i risultati nella transazione: se differiscono da `attesi`, risponde `409` senza eliminare documenti. La rimozione agisce nel database sull’intero insieme filtrato e non applica il limite di cinque righe dell’anteprima. La transazione mantiene coerenti conteggio e cancellazione; modifiche concorrenti ai documenti coinvolti producono un conflitto invece di un successo parziale.

I corpi di creazione e modifica di un cliente richiedono `tipo`: `PERSONA_FISICA` con `nome` e `cognome` (massimo 100 caratteri ciascuno), oppure `AZIENDA` con `nomeLegale` (massimo 150). I campi dell’altro tipo devono essere assenti o vuoti. Esempi: `{"tipo":"PERSONA_FISICA","nome":"Giulia","cognome":"Rossi"}` e `{"tipo":"AZIENDA","nomeLegale":"Alfa S.r.l."}`. Un tipo assente, sconosciuto, generalità incomplete o un cambio di tipo su `PUT` producono `400`. I DTO dei clienti includono `tipo`, `nomeLegale` e `telefono` senza `chiave` o `ordine`. Il telefono è testo facoltativo con massimo 40 caratteri, spazi esterni rimossi e valore vuoto se non fornito. Non fa parte delle generalità duplicate negli appuntamenti: la consultazione iniziale legge quello corrente con `GET /clienti/{id}`. `ActivityDTO` aggiunge `tipoCliente` per permettere al frontend di distinguere la presentazione delle generalità; `client` contiene il nome nel formato persistito. I contratti delle richieste delle attività e le risposte di scrittura restano invariati.

L’entità `Client` conserva l’alias MongoDB precedente e normalizza le generalità: nome e cognome in minuscolo per `PERSONA_FISICA`, nome legale in maiuscolo per `AZIENDA`. `ClientNames` conserva gli accenti, elimina gli spazi esterni, compatta gli spazi interni e normalizza Unicode NFC. Non si persistono `chiave` o `ordine`. La ricerca applica i termini direttamente a `nome`, `cognome` e `nomeLegale`, ignorando maiuscole e accenti e trattando i caratteri speciali come testo letterale; ogni termine deve corrispondere ad almeno un campo.

Il repository produce un unico elenco con un’aggregazione vincolata a `ownerId`, ordinando sul solo `cognome` per le persone e su `nomeLegale` per le aziende, con collazione italiana che non distingue maiuscole e minuscole. Il valore di ordinamento esiste solo durante la query e non viene salvato; l’ID risolve le parità, senza usare `nome`. Filtro e conteggio condividono il vincolo al proprietario e la paginazione segue l’ordinamento completo. Gli indici dei clienti sono `cliente_cognome` e `cliente_nome_legale`, entrambi con proprietario e ID.

Gli appuntamenti incorporano il tipo con nome/cognome in minuscolo oppure nome legale in maiuscolo. Le letture non consultano l’anagrafica; le rinomine aggiornano le copie nella stessa transazione e i callback mostrano il nome corrente tramite i riferimenti caricati per pagina. Il servizio rifiuta il cambio del tipo. La presentazione delle persone avviene esclusivamente nel frontend tramite `personName()` e `activityClientName()`, anche per nomi composti e cognomi con spazi, apostrofi o trattini; le aziende restano interamente maiuscole.

Prima di accettare richieste, `ClientSchemaRepository` normalizza i clienti ed elimina soltanto i due campi obsoleti; `AppointmentSchemaRepository` normalizza le generalità delle copie esistenti conservandone il contenuto storico e convertendo i vecchi riferimenti. Ogni aggiornamento conserva gli altri campi e gli alias e vincola la scrittura al proprietario del documento. Le conversioni sono idempotenti e gli indici precedenti vengono rimossi soltanto dopo le conversioni.

Esempio di creazione appuntamento:

```json
{
  "data": "2026-10-03",
  "clientId": "id-del-cliente",
  "esitoId": "id-dell-esito",
  "polizzaId": null,
  "premio": 120.5,
  "note": "Documenti da verificare",
  "creaCallback": true,
  "dataCallback": "2026-10-10"
}
```

Il cliente è sempre obbligatorio. Per creare o modificare un cliente si inviano `tipo` e le generalità pertinenti descritte sopra. Il callback può avere data nulla. In modifica dell’appuntamento non si crea un secondo callback implicito: i callback esistenti si modificano dalla loro sezione.

## Dati di verifica e pulizia

Le verifiche di integrazione usano esclusivamente un database separato con prefisso `agenda_wa_test_`. La suite comprende 36 test: quelli funzionali e di sicurezza, più la compatibilità dei documenti persistiti e dei campi JSON restituiti dalle API. Il metodo `@AfterAll` elimina l’intero database al termine della classe, con un controllo aggiuntivo sul nome della destinazione. Il contesto Spring viene poi chiuso per evitare di riutilizzare cataloghi o connessioni di un database appena eliminato.

Dopo ogni task che modifica il database, eliminare anche gli eventuali database usati per le prove nel browser e le collezioni dell’agenda confermate obsolete. Se una verifica si interrompe prima della pulizia automatica, completarla manualmente. La pulizia non viene eseguita indiscriminatamente all’avvio dell’applicazione. Conservare sempre i dati applicativi attivi e i database di sistema `admin`, `config` e `local`; le istruzioni permanenti del progetto sono in [AGENTS.md](../AGENTS.md).

### Verifica del modello centrato sui clienti

Il 4 ottobre 2026, la suite completa ha eseguito **32 test senza errori o test saltati**. Le verifiche comprendono l’assenza della risorsa rimossa, i campi JSON correnti, la persistenza delle attività collegate soltanto al cliente, i filtri per date/esito, le transazioni, la compatibilità dei documenti esistenti e l’isolamento tra utenti.

Il browser ha usato `agenda_wa_test_remove_role_browser`, copia dei dati dimostrativi senza credenziali reali e con chiave temporanea. Sono stati verificati creazione e ricerca del cliente, creazione dell’appuntamento con callback, consultazione in sola lettura, modifica, annullamento, salvataggio e pianificazione del callback; anche filtro per date/esito, anteprima di cancellazione e annullamento della conferma. La navigazione contiene quattro voci e la panoramica due riepiloghi. Sono stati controllati desktop a 1280 px, mobile a 320/390/693 px e i breakpoint 781/1000/1150 px, senza allargamento della pagina o errori in console. La [guida grafica](../.agents/skills/agenda-wa-ui/SKILL.md) e i [riferimenti visivi](web/panoramica.jpg) descrivono lo stato corrente.

Nel database attivo `agenda_wa` restano **32 clienti, 72 appuntamenti, 28 callback, 15 voci di catalogo, un utente e un marcatore**. Il confronto EJSON completo prima/dopo conferma la conservazione di tutti i campi delle risorse mantenute, delle credenziali e dei metadati, eccetto i soli collegamenti rimossi dalle attività. Gli indici rimanenti corrispondono al modello corrente. I database dei test e la copia per il browser sono stati eliminati; non restano collezioni obsolete o riferimenti rimossi nel database attivo. I servizi applicativi usati sono stati arrestati e le porte 8080 e 18080 risultano libere; MongoDB è rimasto attivo. Le chiavi temporanee e le copie di verifica dei documenti sono state rimosse.

### Verifica della denormalizzazione e della cancellazione per date

Il 5 ottobre 2026 sono passati **36 test**, senza errori o test saltati: formato BSON senza ID dei cataloghi, lettura delle etichette anche senza documenti sorgente, rinomine e filtri, migrazione idempotente e rollback su riferimenti estranei, API e sicurezza con due utenti. I 72 appuntamenti di `agenda_wa` sono stati convertiti; il confronto tramite SHA-256 conferma esattamente i campi attesi e la conservazione integrale delle altre cinque collezioni. Non restano campi `esitoId`/`polizzaId` o indici sui vecchi riferimenti.

Nel browser, con database isolato `agenda_wa_test_denormalizzazione_browser`, sono stati verificati tutti i periodi, estremi inclusi/esclusi, invalidazione dell’anteprima, stato vuoto e conferma con riepilogo delle sole date. L’intervallo include appuntamenti con due esiti diversi. Controllati desktop 1280 px e mobile 390/320 px, senza overflow della pagina o errori in console. La guida UI e i tre riferimenti visivi della cancellazione sono aggiornati. I database della suite e del browser e le chiavi temporanee sono stati eliminati; non restano database di test o collezioni obsolete. I servizi usati sono arrestati e le porte 8080, 18080 e quelle temporanee dei test sono libere; MongoDB resta attivo.

### Catalogo polizze ricavato dal portafoglio

Il 5 ottobre 2026, su richiesta dell’utente, sono state sostituite le 8 polizze dell’account proprietario dell’agenda con i 22 nomi distinti della colonna PRODOTTO di `Finale Portafoglio Jenica.xlsx`, `Foglio1!L11:L572`. Sono conservati codici e diciture del file; duplicati e spazi superflui vengono eliminati. Nessuna anagrafica del foglio viene importata. `DefaultCatalogs.POLICIES` usa gli stessi nomi per le nuove registrazioni, senza sovrascrivere i cataloghi degli account esistenti all’avvio.

L’utente ha confermato l’assegnazione casuale soltanto ai 22 appuntamenti già associati a una polizza. Catalogo e nomi denormalizzati sono stati sostituiti in un’unica transazione vincolata al proprietario. Le impronte SHA-256 calcolate prima del commit confermano che i 50 appuntamenti senza polizza, tutti gli altri campi dei 22 aggiornati, clienti, callback, esiti, account e metadati sono invariati. La verifica successiva al commit conferma il catalogo esatto, 22 assegnazioni valide e nessun vecchio ID di polizza. Nel database attivo restano 32 clienti, 72 appuntamenti, 28 callback, 29 voci di catalogo (7 esiti e 22 polizze), un utente e un marcatore. La suite di 36 test è passata dopo l’aggiornamento dei valori iniziali. Non restano database di test, chiavi temporanee o collezioni obsolete; i servizi applicativi sono arrestati e le porte 8080 e 18080 sono libere.

## Autenticazione e accesso web

Il server ascolta su `127.0.0.1`; MongoDB resta locale. Solo HTML, CSS, moduli JavaScript, font, favicon e gli endpoint pubblici di autenticazione sono accessibili senza login. Le API applicative anonime rispondono `401`; modifiche senza CSRF rispondono `403`. Non è configurato CORS.

| Endpoint | Funzione |
| --- | --- |
| `GET /api/auth/csrf` | Token CSRF e nome dell’header, con relativo cookie HttpOnly |
| `POST /api/auth/register` | `nome`, `cognome`, `email`, `password`; crea account e cataloghi in transazione, risponde `201` |
| `POST /api/auth/login` | `email`, `password`; risponde `200`, errore generico `401` o limitazione `429` |
| `GET /api/auth/me` | Profilo dell’utente verificato e `scadeAlle`; richiede JWT |
| `POST /api/auth/logout` | Incrementa la versione dei token dell’utente e cancella il cookie, risponde `204` |

Login, registrazione e `/me` restituiscono solo `id`, `nome`, `cognome`, `email` e `scadeAlle`. Password, hash e JWT non sono presenti nel corpo. Il JWT viene emesso in `agenda_session`, cookie HttpOnly / SameSite Strict / path `/api`, durata due ore. Il server verifica firma HS256, issuer `agenda-wa`, audience `agenda-wa-web`, scadenza, subject, esistenza dell’utente e versione del token. Non sono ammessi algoritmi scelti dal client. Il logout revoca tutti i JWT precedenti dello stesso account; non serve una collezione di blacklist o sessioni.

Il Resource Server supporta anche `Authorization: Bearer <JWT>`. Con JWT nei cookie, il normale esonero CSRF del Resource Server sarebbe inappropriato: è installato esplicitamente il **CsrfFilter di Spring Security**, per tutte le richieste di modifica. Anche i client che usano l’header Bearer ottengono prima il cookie/token da `/api/auth/csrf` e trasmettono il token in `X-XSRF-TOKEN`. Login, registrazione e CSRF ignorano eventuali vecchi cookie JWT non validi, consentendo un nuovo accesso dopo una scadenza o revoca. Le risposte usano le intestazioni di sicurezza e di controllo cache di Spring Security.

Le password hanno hash PBKDF2-HMAC-SHA256 con sale casuale di 16 byte e 600.000 iterazioni; lunghezza ammessa 12–128 caratteri. Il login con email sconosciuta verifica comunque un hash fittizio per evitare una risposta immediata. Le email sono normalizzate e protette da indice univoco. I tentativi errati sono contati in memoria per coppia indirizzo/email, con finestra di 15 minuti e limite di dieci; la mappa è limitata e non crea residui MongoDB.

La chiave HS256 è generata casualmente (64 byte) e salvata nel percorso configurabile `AGENDA_JWT_KEY_FILE`, predefinito `.local/security/jwt.key`; su macOS/Linux il file è privato. Non contiene una chiave predefinita nel codice. `AGENDA_SECURE_COOKIES=true` abilita l’attributo Secure per l’uso tramite HTTPS. In locale HTTP si mantiene il valore predefinito false.

`auth.js` mostra il login prima di caricare qualunque dato applicativo. Ripristina la sessione tramite `/me`, mostra il nome reale e svuota contenuti, dialoghi, cataloghi e memoria al logout o alla scadenza. `scadeAlle` consente di tornare al login senza attendere la prossima richiesta; al ritorno di una scheda in primo piano il server verifica nuovamente la sessione. `api.js` aggiunge CSRF alle modifiche e interrompe le richieste della sessione precedente al cambio di account. Il JWT non viene letto da JavaScript o salvato in localStorage. L’evento `pageshow` ricarica una pagina ripristinata dalla cache di navigazione per verificare nuovamente l’accesso.

I dati dimostrativi esistenti hanno un proprietario riservato all’email `nashj760@gmail.com`, indicata dall’utente. La registrazione con questa email riutilizza quell’ID e conserva i cataloghi e lo storico; gli altri account ricevono un ID nuovo e cataloghi personali. La prenotazione `initial-owner` in `metadati` viene rimossa nella stessa transazione della registrazione. Non esistono credenziali preimpostate o script di migrazione.

## Riferimenti

- [Spring Security: JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Spring Security: CSRF](https://docs.spring.io/spring-security/reference/7.0/servlet/exploits/csrf.html)
- [Spring Data MongoDB: transazioni](https://docs.spring.io/spring-data/mongodb/reference/mongodb/client-session-transactions.html)
- [Spring Boot 4.0.5](https://spring.io/blog/2026/03/26/spring-boot-4-0-5-available-now/)
- [AXA Italia, fonte del tema e della tipografia](https://www.axa.it/)
- [Source Sans, progetto Adobe e licenza](https://github.com/adobe-fonts/source-sans)

## Verifica persone fisiche e aziende — 5 ottobre 2026

La suite completa passa con 42 test (26 MongoDB, 13 autenticazione/isolamento, 3 query). Verificati: aziende e persone con generalità denormalizzate nel formato richiesto, rinomina propagata alle attività, paginazione di un unico elenco alfabetico, ordinamento sul solo cognome con parità risolte dall’ID, ricerca senza campi derivati e senza distinzione di accenti o maiuscole, generalità obbligatorie e incompatibili, tipo fisso, isolamento tra due utenti e conversioni idempotenti che conservano gli altri campi. I documenti nuovi non contengono `chiave`, `ordine` o il valore temporaneo della query.

Nel browser, su `agenda_wa_test_clienti_nomi_browser`, sono stati creati «Marco Giovanni / De Pietro» e «COSTRUZIONI», con appuntamenti e callback. Controllate iniziali maiuscole per i nomi composti, azienda interamente maiuscola, rilettura nel modulo di modifica, selettori, dettagli, callback e anteprima di cancellazione. Il controllo diretto del database conferma `marco giovanni`, `de pietro` e `COSTRUZIONI` nei documenti e nelle copie degli appuntamenti, senza i due campi obsoleti. Layout verificato a 1280, 390 e 320 px senza allargamento della pagina; nessun errore o avviso in console. I riferimenti visivi dei clienti e dei moduli e la skill `agenda-wa-ui` sono aggiornati; validatore e link della skill passano.

Nel database attivo sono stati normalizzati i 32 clienti e le generalità incorporate nei 72 appuntamenti. I clienti non contengono più `chiave` o `ordine`. Le impronte SHA-256 coincidono con quelle attese applicando soltanto queste trasformazioni: ID, proprietari, generalità storiche salvo maiuscole/minuscole, polizze dei soli 22 appuntamenti associati, premi, date, callback, cataloghi, account e metadati sono conservati. Restano 32 clienti, 72 appuntamenti, 28 callback e 29 cataloghi. Gli indici precedenti dei clienti sono sostituiti da `cliente_cognome` e `cliente_nome_legale`.

I database di integrazione sono stati eliminati dai test; il database isolato del browser e tutte le chiavi temporanee sono stati rimossi. Non restano database di test o collezioni obsolete. I servizi applicativi usati sono arrestati e le porte di sviluppo e integrazione sono libere; MongoDB resta attivo.

## Sostituzione del portafoglio Jenica — 5 ottobre 2026

Su conferma dell’utente, nell’agenda di `nashj760@gmail.com` sono stati sostituiti i 32 clienti, 72 appuntamenti e 28 callback precedenti. L’estrazione da `Finale Portafoglio Jenica.xlsx`, `Foglio1!A11:A566`, contiene 549 occorrenze e 280 nominativi unici. Il confronto indipendente con il file conferma la copertura completa e la conservazione di tutte le componenti dei nomi. Sono stati inseriti 264 clienti persone fisiche e 16 clienti di tipo Azienda, incluso il condominio, con la separazione dei cognomi composti confermata dall’utente; ditte individuali e titolari restano distinti quando compaiono separatamente. Il glossario in `CONTEXT.md` registra queste definizioni e la natura delle attività dimostrative.

Il database attivo contiene ora 280 clienti, 560 appuntamenti (due per cliente, dal 1° gennaio al 31 dicembre 2026) e 140 callback, di cui 28 senza data di ricontatto e uno con data 5 ottobre 2026. Le attività hanno note `[DEMO]`; i 95 appuntamenti con esito Sottoscrizione hanno polizze del catalogo personale e premi simulati in Decimal128. Tutti i riferimenti appartengono al proprietario confermato, le generalità incorporate coincidono con il nuovo cliente e i callback rimandano alla data di un appuntamento dello stesso cliente, senza date di ricontatto precedenti.

La sostituzione è stata verificata prima in `agenda_wa_test_jenica_import_55a43d2272a34169921c825cfa74f4b6`, con due proprietari fittizi e conservazione dei record del secondo proprietario, poi applicata al database attivo in un’unica transazione. I tentativi preliminari interrotti per controlli di compatibilità MongoDB sono stati annullati senza modificare i dati precedenti. Il confronto EJSON e delle impronte SHA-256 prima e dopo il commit conferma il piano esatto, l’assenza di tutti gli ID sostituiti e la conservazione integrale di account, cataloghi, metadati e dati di altri proprietari. Una copia locale dei soli record sostituiti, senza credenziali, è disponibile nel percorso `.local/backups/portafoglio-jenica-prima-sostituzione-2026-10-05-3afb85e1-1619-4ff5-a56c-92b27ef46e54.ejson` indicato nel README.

Nel browser, con la sessione già autenticata dell’utente, sono stati verificati i conteggi dei tre elenchi, le generalità composte, il condominio visualizzato come Azienda, i callback da pianificare e il riepilogo degli appuntamenti. Una richiesta anonima a `/api/clienti` restituisce 401. Non sono stati modificati codice applicativo, credenziali o stile dell’interfaccia. Il database isolato è stato eliminato anche attraverso la pulizia finale della prova; non restano database di test o collezioni obsolete. Il servizio Spring Boot identificato del progetto è stato arrestato; le porte 8080 e 18080 sono libere e MongoDB resta attivo su 27018. I nuovi dati dimostrativi sono conservati per le prove dell’utente.

## Priorità dei callback di oggi — 5 ottobre 2026

La ricerca dei callback antepone le attività con data odierna all’ordine esistente (data crescente, prima le date assenti, poi ID crescente nelle parità). `ActivityServiceImpl` ricava oggi dal `Clock` dell’agenda in Europe/Rome; il repository applica prima il vincolo del proprietario e i filtri, poi una priorità temporanea dell’aggregazione, l’ordinamento e la paginazione. Il valore temporaneo viene escluso dal risultato e non viene persistito. Contratti HTTP e JSON restano invariati.

Il frontend usa `state.today`, già fornito dalla navigazione, per aggiungere `.callback-today` alle sole righe odierne e il badge Oggi accanto alla data. Le celle usano il lilla condiviso, con accento interno blu di 3 px nella prima cella; data e badge restano insieme. La guida `agenda-wa-ui` e i riferimenti `docs/web/callback.jpg` e `docs/web/callback-mobile.jpg` descrivono il risultato. Verificati desktop a 1280 px CSS e mobile a 390/320 px, senza allargamento della pagina, il primo callback evidenziato, i record successivi nell’ordine precedente, il filtro che esclude oggi e Azzera che ripristina l’elenco.

La compilazione e tutti i 43 test passano: 27 MongoDB, 13 autenticazione/isolamento e 3 query. Il nuovo test copre la priorità prima della paginazione con 24 callback senza data, le parità, i filtri, l’assenza di attività odierne, l’esclusione dei dati di un secondo utente e l’assenza del campo temporaneo nei documenti. Il confronto delle impronte EJSON di tutte le collezioni attive conferma che la verifica grafica non ha modificato clienti, attività, cataloghi, account o metadati. I database isolati `agenda_wa_test_callback_today` e `agenda_wa_test_callback_today_security` e le chiavi temporanee sono stati eliminati; non restano collezioni obsolete. I servizi usati sono arrestati e le porte 8080, 18080 e quelle dei test sono libere. MongoDB resta attivo.


## Ricerca degli appuntamenti per cliente e note

`GET /api/appuntamenti` e `GET /api/appuntamenti/riepilogo` accettano il parametro facoltativo `q`, combinato con date ed esito. Controller e servizi passano il testo ai repository; `Queries.appointments` costruisce un solo `TextCriteria` con lingua `none`, maiuscole e accenti ignorati. Spazi, punteggiatura e simboli separano i termini; ogni termine viene passato a `matchingPhrase` separatamente. Per esempio `rossi rinnovo` genera `$search: '"rossi" "rinnovo"'`: tutte le parole sono obbligatorie, anche distribuite tra campi diversi, senza richiedere ordine o adiacenza. Per ricerche con più termini, una verifica aggiuntiva dei confini delle parole tramite regex evita che il controllo delle frasi di MongoDB accetti un frammento quando un altro termine indicizzato corrisponde, come in «di rinn». Questi predicati verificano ogni parola in almeno uno dei quattro campi e ignorano maiuscole e accenti; `$text` mantiene la selezione dei candidati tramite indice. Non sono ammessi frammenti; virgolette e trattini non introducono frasi o negazioni nell’input. Un input non vuoto composto soltanto da separatori produce zero risultati; un input vuoto non applica il filtro. La ricerca clienti resta basata sulle regex precedenti.

`AppointmentSchemaRepository.ensureTextIndex`, chiamato dalla configurazione dopo le migrazioni esistenti, crea in modo idempotente `appuntamento_testo` con prefisso `ownerId: 1` e campi testuali `cliente.nome`, `cliente.cognome`, `cliente.nomeLegale`, `note`. `default_language: "none"` evita stemming e stop word, preservando anche cognomi come «Di Pietro». Non servono nuovi campi persistiti o join: la rinomina del cliente aggiorna le copie già esistenti e MongoDB mantiene l’indice, come per le note modificate. Un eventuale indice testuale incompatibile viene segnalato senza cancellazioni automatiche. Il README contiene il comando manuale equivalente.

`OwnedQuerySupport` aggiunge sempre `ownerId`. Elenco, conteggio e aggregazione del riepilogo condividono la stessa query; nell’aggregazione il `$match` con `$text` è il primo stadio. L’ordine resta per data decrescente e ID crescente, indipendente dal punteggio testuale; il text index seleziona i candidati e non accelera questo ordinamento. I contratti HTTP e JSON non cambiano. Riferimenti: [operatore MongoDB](https://www.mongodb.com/docs/manual/reference/operator/query/text/), [lingua `none`](https://www.mongodb.com/docs/manual/core/indexes/index-types/index-text/specify-text-index-language/) e [vincoli degli indici testuali](https://www.mongodb.com/docs/manual/core/indexes/index-types/index-text/text-index-restrictions/).

Il campo «Cerca per cliente o note» conserva posizione, stile e comportamento: Filtra e Invio applicano il testo e riportano alla prima pagina; Azzera svuota tutti i filtri, mentre Aggiorna dati mantiene quelli applicati. La guida UI descrive ora la ricerca per parole intere.

La compilazione e tutti i **45 test** passano: 28 MongoDB, 14 autenticazione/isolamento e 3 query. Sono verificati indice unico e idempotente, prefisso `ownerId`, lingua `none`, versione 3 e uso effettivo tramite `explain`, parole brevi, assenza di stemming, parole distribuite tra campi, ordine dei termini, esclusione dei frammenti anche insieme a parole valide, accenti, punteggiatura, aziende, date/esito, paginazione, riepiloghi, rinomina, modifica delle note, richieste anonime e due proprietari.

Nel browser isolato sono stati verificati Filtra, Invio e Azzera, maiuscole e accenti, il caso «di rinn» senza risultati e «di rinnovo» con un risultato, con riepiloghi coerenti. Nessun overflow della pagina a 1280, 390 e 320 px CSS, nessun errore in console; i riferimenti desktop/mobile mostrano ora parole intere. Non sono stati modificati i componenti grafici.

L’indice `appuntamento_testo` è stato creato anche sul database attivo `agenda_wa`; `explain` ne conferma l’utilizzo sui 560 appuntamenti dimostrativi. Le impronte EJSON complete delle sei collezioni sono identiche prima e dopo: clienti, attività, cataloghi, account e metadati restano conservati. I database `agenda_wa_test_text_index`, `agenda_wa_test_text_index_security` e `agenda_wa_test_text_index_browser` e le chiavi temporanee sono rimossi, anche dopo il primo giro di test fallito. Non restano database di test o collezioni obsolete. Il servizio identificato sulla porta 18080 è arrestato; 8080, 18080 e le porte temporanee della suite sono libere. MongoDB resta attivo.
