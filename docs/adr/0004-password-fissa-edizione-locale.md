# Password fissa nell’edizione locale

Su richiesta esplicita dell’utente, l’edizione per un solo computer usa una password fissa incorporata nel pacchetto, senza registrazione, cambio password o recupero. La password fornita viene verificata tramite un hash PBKDF2, incluso nelle risorse versionate e nella build, su successiva richiesta dell’utente di condividere i file applicativi; JWT in cookie HttpOnly e protezione CSRF continuano a proteggere le API locali. La scelta sostituisce nell’edizione locale la precedente configurazione di account personali e password scelte nell’interfaccia.
