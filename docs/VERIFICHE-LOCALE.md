# Verifiche edizione locale 1.0.0 — 13 ottobre 2026

## Risultati

| Verifica | Risultato |
| --- | --- |
| Backend, H2 su file e HTTP reale | 14 test, 0 errori, 0 esclusi |
| Interfaccia esistente | 38 test Playwright superati |
| Accesso locale e backup | 4 test Playwright superati, larghezze 1280 / 390 / 320 px |
| Pacchetto amd64, Debian 12 senza rete | Installazione, avvio, login, importazione e operazioni superati |
| Pacchetto arm64, Debian 12 senza rete | Installazione, avvio, login, importazione e operazioni superati |
| Entrambi i pacchetti | Backup/ripristino, revoca JWT, riavvio, istanza unica e reinstallazione superati |
| Disinstallazione | Dati nella home conservati; container di prova poi eliminati |

I pacchetti includono Temurin 21.0.12.1+1; il backend usa H2 2.4.240. La prova Debian è eseguita con `--network none`, nessun Java di sistema e processo sotto l’utente ordinario `agenda`. Le librerie Debian dichiarate nel `.deb` sono predisposte prima della prova; nessuna dipendenza viene scaricata durante installazione o uso offline.

I test backend verificano accesso anonimo negato, CSRF, cookie, logout, riferimenti e cancellazioni vincolati al proprietario, concorrenza DELETE/PUT, ricerche con accenti e parole intere, totali, storico derivato, priorità callback, vincoli relazionali e ripristino transazionale. Le regressioni sulla normalizzazione Unicode e sui backup oltre 20 MB sono coperte; un backup con 4.200 appuntamenti viene esportato, salvato automaticamente e ripristinato.

Le schermate locali in `docs/web/accesso-locale*.jpg` e `backup-*.jpg` sono state ispezionate su desktop e mobile e usano dati fittizi. La guida [agenda-wa-ui](../.agents/skills/agenda-wa-ui/SKILL.md) è aggiornata ai controlli effettivi.

## Dati e pulizia

Il database H2 applicativo privato contiene 280 clienti, 22 polizze, 0 appuntamenti e 0 callback. Ogni campo importato di clienti e polizze corrisponde al seed privato; i sei esiti iniziali vengono creati dall’edizione locale. Il confronto di conteggi e impronte SHA-256 conferma che tutte e sei le collezioni del MongoDB originale sono identiche prima e dopo il lavoro. Nessuna collezione di quella versione è stata eliminata.

Le cartelle H2 temporanee dei test, le relative chiavi e i container Debian sono eliminati. I processi applicativi di verifica vengono identificati e arrestati; le porte 8080 e 18080 risultano libere sul computer dello sviluppatore, e la porta interna 18080 è verificata libera prima di rimuovere ogni container. MongoDB originale può restare attivo sulla porta 27018. Non restano database Mongo con prefisso `agenda_wa_test_`.

Password fornita, hash privato, seed, portafoglio, database e pacchetti personalizzati restano esclusi da Git. Le somme SHA-256 dei `.deb` sono nella cartella privata di consegna.

## Limite della verifica

Non è disponibile un Chromebook fisico. Le prove Debian non attestano il comportamento del menu ChromeOS, del browser host, della sospensione e degli aggiornamenti ChromeOS. Il forwarding verso il loopback del container è documentato da ChromiumOS; resta da verificare direttamente l’intera esperienza sul dispositivo destinatario, come indicato nella [distribuzione](DISTRIBUZIONE-LOCALE.md).
