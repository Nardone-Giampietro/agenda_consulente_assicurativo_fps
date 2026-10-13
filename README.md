# Agenda WA — locale

Agenda per gestire **clienti, appuntamenti e callback**, con esiti e polizze personalizzabili. Funziona offline, su un solo computer, con interfaccia nel browser e database H2.

## Avvio

Scarica il pacchetto dalla cartella [dist](dist/): **amd64** per Intel/AMD, **arm64** per ARM. Java è incluso. Dopo l’installazione avvia **Agenda WA** dal menu Linux e accedi con la password concordata.

In **Impostazioni** puoi esportare e ripristinare i dati; le copie automatiche vengono salvate sul computer.

## Sviluppo

Richiede Java 21 o superiore. Dalla cartella del progetto:

```sh
./servizio/mvnw -f servizio/pom.xml package
java -jar servizio/target/agenda-wa.jar
```

Apri **http://localhost:8080**. Nel terminale, **Ctrl+C** arresta l’applicazione.

## Schermate

Esempi con dati dimostrativi. Clicca sulle immagini per ingrandirle.

| Panoramica | Clienti |
| :---: | :---: |
| [<img src="docs/web/readme-panoramica.jpg" width="380" alt="Panoramica con riepilogo e prossimi incontri">](docs/web/readme-panoramica.jpg) | [<img src="docs/web/readme-clienti.jpg" width="380" alt="Anagrafica e ricerca clienti">](docs/web/readme-clienti.jpg) |
| **Appuntamenti** | **Callback** |
| [<img src="docs/web/readme-appuntamenti.jpg" width="380" alt="Appuntamenti con filtri, premi ed esiti">](docs/web/readme-appuntamenti.jpg) | [<img src="docs/web/readme-callback.jpg" width="380" alt="Callback di oggi e contatti da pianificare">](docs/web/readme-callback.jpg) |

[Backup e ripristino: screenshot](docs/web/readme-backup.jpg).

Il branch **`locale`** contiene questa versione; **`online`** contiene quella con MongoDB e account personali. Per i dettagli tecnici: [architettura](docs/ARCHITETTURA.md).
