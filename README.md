# Agenda WA — online

Applicazione web per gestire **clienti, appuntamenti e callback**, con esiti e polizze personalizzabili. Backend Java/Spring Boot, database MongoDB e accesso con account personali.

## Avvio

Richiede Java 21 o superiore e MongoDB configurato come replica set `rs0` su `localhost:27018`.

```sh
./servizio/mvnw -f servizio/pom.xml package
java -jar servizio/target/agenda-wa.jar
```

Apri **http://localhost:8080** e registra il tuo account. Nel terminale, **Ctrl+C** arresta l’applicazione.

## Schermate

Esempi con dati dimostrativi. Clicca sulle immagini per ingrandirle.

| Panoramica | Clienti |
| :---: | :---: |
| [<img src="docs/web/readme-panoramica.jpg" width="380" alt="Panoramica con riepilogo e prossimi incontri">](docs/web/readme-panoramica.jpg) | [<img src="docs/web/readme-clienti.jpg" width="380" alt="Anagrafica e ricerca clienti">](docs/web/readme-clienti.jpg) |
| **Appuntamenti** | **Callback** |
| [<img src="docs/web/readme-appuntamenti.jpg" width="380" alt="Appuntamenti con filtri, premi ed esiti">](docs/web/readme-appuntamenti.jpg) | [<img src="docs/web/readme-callback.jpg" width="380" alt="Callback di oggi e contatti da pianificare">](docs/web/readme-callback.jpg) |

Il branch **`online`** contiene questa versione; **`locale`** contiene quella offline con H2 e pacchetti Debian. Per i dettagli tecnici: [architettura](docs/ARCHITETTURA.md).
