# Indicazioni per questo progetto

- L’applicazione è Agenda WA, con backend Java/Spring Boot e MongoDB locale. Usare «Callback» nell’interfaccia. Le anagrafiche sono esclusivamente quelle dei clienti; ogni appuntamento e callback si riferisce a un cliente. La risorsa dei callback usa `/api/callback`, la collezione `callback` e i campi `creaCallback`, `dataCallback` e `callbackOggi`.
- Non aggiungere script personalizzati di avvio, arresto, migrazione o manutenzione. Documentare i comandi manuali nel README; conservare il wrapper Maven per la compilazione.
- Al termine di ogni task, arrestare i servizi applicativi avviati o usati per lo sviluppo e le verifiche e controllare che le relative porte siano libere. MongoDB può restare attivo. Identificare i processi prima dell’arresto, senza coinvolgere servizi di altri progetti; completare questa pulizia anche se una verifica fallisce e riportarne l’esito.
- Dopo ogni task che modifica il database, rimuovere tutti i database e le collezioni di test creati per quel task e le collezioni dell’agenda confermate obsolete. La pulizia finale è parte del lavoro richiesto dall’utente, anche quando una verifica fallisce.
- Usare database di verifica separati con prefisso `agenda_wa_test_`. I test di integrazione eliminano il proprio database al termine; controllare anche gli eventuali database usati per le prove nel browser.
- Prima della pulizia, verificare la connessione effettiva, le collezioni usate dal codice corrente e la destinazione esatta. Conservare i dati applicativi attivi e i database di sistema MongoDB (`admin`, `config`, `local`); non cancellare database di altri progetti.
- Conservare i dati dimostrativi inseriti nell’agenda su esplicita richiesta dell’utente: sono dati applicativi destinati alle sue prove, non residui delle verifiche automatiche. Non eliminarli durante la pulizia dei database di test.
- Al termine, verificare che non restino dati di test o collezioni obsolete e riportare sinteticamente la pulizia eseguita. Non introdurre cancellazioni indiscriminate all’avvio dell’applicazione.

## Autenticazione e proprietà dei dati

- Tutte le API dell’agenda richiedono un JWT verificato da Spring Security. Solo risorse statiche e gli endpoint pubblici di registrazione, login e CSRF sono anonimi.
- Clienti, appuntamenti, callback e cataloghi appartengono a un solo utente tramite `ownerId`. Ricavarlo da `UserScope`, mai dai dati inviati dal client. Applicare il vincolo anche a ricerche, conteggi, aggregazioni, riferimenti, aggiornamenti e cancellazioni multiple; un ID conosciuto non concede accesso ai dati altrui.
- Esiti e polizze sono personali. I cataloghi iniziali vengono creati durante la registrazione e non ripristinati all’avvio.
- Conservare JWT in cookie HttpOnly e proteggere le modifiche con CSRF, senza localStorage o chiavi di firma nel codice. Non registrare password, hash o token nei log. Non creare account o password predefiniti per l’utente.
- Nei test usare account fittizi soltanto nei database isolati; rimuovere anche le chiavi temporanee. Verificare isolamento con almeno due utenti, accesso anonimo negato, ID e riferimenti altrui rifiutati e cancellazioni limitate al proprietario.
- I dati dimostrativi esistenti sono riservati a `nashj760@gmail.com`, su indicazione dell’utente; vanno conservati. La sua registrazione con password scelta nell’interfaccia li rende accessibili al relativo account.

## Struttura del backend

- Seguire la struttura di `LSMD_Project_MonGoFundMe`: `controllers`, `DTO` per area, `entity`, `mapper`, `service/interfaces`, implementazioni `*ServiceImpl` in `service`, `repository`, `exception`, `config` e `security`.
- Iniettare le interfacce dei servizi nei controller. Restituire DTO espliciti, mantenendo i contratti HTTP e JSON esistenti; non esporre direttamente i documenti MongoDB.
- I servizi gestiscono regole e transazioni; i repository gestiscono query e accessi al database. Riservare `MongoTemplate` e BSON ai repository e alla configurazione di persistenza. I mapper effettuano conversioni senza query.
- Ricavare `ownerId` da `UserScope` e usare metodi dei repository vincolati al proprietario, anche nei frammenti personalizzati. Evitare letture e cancellazioni generiche sulle entità applicative.
- Conservare gli alias MongoDB delle entità e il formato persistito quando si cambiano nomi o package Java. Verificare la compatibilità delle API e dei documenti esistenti.

## Stile dell’interfaccia

- Prima di aggiungere un elemento grafico o modificare l’aspetto della UI, leggere e applicare la skill locale [agenda-wa-ui](.agents/skills/agenda-wa-ui/SKILL.md), anche quando non è richiamata esplicitamente dall’utente.
- Riutilizzare colori, font, spaziature, classi e helper del progetto secondo la skill. Le nuove aggiunte devono seguire lo stile corrente; le richieste esplicite dell’utente possono farlo evolvere.
- Ogni task che aggiunge elementi grafici o apporta modifiche stilistiche deve aggiornare lo stesso `SKILL.md` nel corso del task con i valori e i pattern effettivamente implementati e verificati. Aggiornare i riferimenti visivi interessati e rimuovere le istruzioni superate; cambiare soltanto la data o aggiungere un promemoria non è sufficiente.
- Considerare completata una modifica grafica solo quando il risultato renderizzato e la guida sono coerenti, anche nel layout mobile. Nel resoconto indicare la verifica e l’aggiornamento della skill. Le sole modifiche alla logica backend non richiedono l’aggiornamento della guida grafica.
