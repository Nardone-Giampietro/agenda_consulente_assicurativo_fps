package xyz.nardone.agenda_wa.servizio.repository;

import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import xyz.nardone.agenda_wa.servizio.config.LocalPaths;

/** Versioned Java schema: no external migration or maintenance scripts. */
@Repository
public class SqlSchemaRepository {
  private final JdbcTemplate jdbc;

  public SqlSchemaRepository(JdbcTemplate jdbc, LocalPaths paths) {
    this.jdbc = jdbc;
  }

  @PostConstruct
  void initialize() {
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS schema_version (id INT PRIMARY KEY CHECK(id=1), version INT NOT"
            + " NULL)");
    var versions =
        jdbc.queryForList("SELECT version FROM schema_version WHERE id=1", Integer.class);
    if (!versions.isEmpty() && versions.getFirst() != 1)
      throw new IllegalStateException(
          "Versione del database non supportata da questa applicazione.");
    List.of(
            "CREATE TABLE IF NOT EXISTS operation_lock (id INT PRIMARY KEY CHECK(id=1))",
            "CREATE TABLE IF NOT EXISTS utenti (id VARCHAR(80) PRIMARY KEY, nome VARCHAR(100) NOT"
                + " NULL, cognome VARCHAR(100) NOT NULL, email VARCHAR(254) NOT NULL, token_version"
                + " BIGINT NOT NULL DEFAULT 0)",
            "CREATE TABLE IF NOT EXISTS metadati (id VARCHAR(160) PRIMARY KEY, owner_id VARCHAR(80)"
                + " NOT NULL)",
            "CREATE TABLE IF NOT EXISTS clienti (id VARCHAR(80) PRIMARY KEY, owner_id VARCHAR(80)"
                + " NOT NULL REFERENCES utenti(id), tipo VARCHAR(20) NOT NULL CHECK(tipo IN"
                + " ('PERSONA_FISICA','AZIENDA')), nome VARCHAR(120), cognome VARCHAR(120),"
                + " nome_legale VARCHAR(200), telefono VARCHAR(40) NOT NULL, search_text"
                + " VARCHAR(1000) NOT NULL, sort_key VARBINARY(8192) NOT NULL, revision BIGINT NOT"
                + " NULL DEFAULT 0, UNIQUE(id,owner_id), CHECK((tipo='AZIENDA' AND nome_legale IS"
                + " NOT NULL AND nome IS NULL AND cognome IS NULL) OR (tipo='PERSONA_FISICA' AND"
                + " nome IS NOT NULL AND cognome IS NOT NULL AND nome_legale IS NULL)))",
            "CREATE TABLE IF NOT EXISTS cataloghi (id VARCHAR(80) PRIMARY KEY, owner_id VARCHAR(80)"
                + " NOT NULL REFERENCES utenti(id), tipo VARCHAR(16) NOT NULL CHECK(tipo IN"
                + " ('esiti','polizze')), nome VARCHAR(150) NOT NULL, chiave VARCHAR(1000) NOT"
                + " NULL, revision BIGINT NOT NULL DEFAULT 0, UNIQUE(owner_id,tipo,chiave))",
            "CREATE TABLE IF NOT EXISTS appuntamenti (id VARCHAR(80) PRIMARY KEY, owner_id"
                + " VARCHAR(80) NOT NULL, data DATE NOT NULL, client_id VARCHAR(80) NOT NULL,"
                + " client_tipo VARCHAR(20) NOT NULL, client_nome VARCHAR(120), client_cognome"
                + " VARCHAR(120), client_nome_legale VARCHAR(200), esito VARCHAR(150) NOT NULL,"
                + " polizza VARCHAR(150), premio DECIMAL(12,2) NOT NULL CHECK(premio>=0), note"
                + " VARCHAR(5000) NOT NULL, search_words CLOB NOT NULL, FOREIGN"
                + " KEY(client_id,owner_id) REFERENCES clienti(id,owner_id) ON DELETE CASCADE)",
            "CREATE TABLE IF NOT EXISTS callback (id VARCHAR(80) PRIMARY KEY, owner_id VARCHAR(80)"
                + " NOT NULL, data DATE, data_appuntamento DATE NOT NULL, client_id VARCHAR(80) NOT"
                + " NULL, note VARCHAR(5000) NOT NULL, FOREIGN KEY(client_id,owner_id) REFERENCES"
                + " clienti(id,owner_id) ON DELETE CASCADE, CHECK(data IS NULL OR"
                + " data>=data_appuntamento))",
            "CREATE INDEX IF NOT EXISTS cliente_ordine ON clienti(owner_id,sort_key,id)",
            "CREATE INDEX IF NOT EXISTS appuntamento_data ON appuntamenti(owner_id,data,id)",
            "CREATE INDEX IF NOT EXISTS appuntamento_cliente ON"
                + " appuntamenti(owner_id,client_id,data,id)",
            "CREATE INDEX IF NOT EXISTS appuntamento_esito ON appuntamenti(owner_id,esito,data)",
            "CREATE INDEX IF NOT EXISTS callback_data ON callback(owner_id,data,id)",
            "INSERT INTO operation_lock SELECT 1 WHERE NOT EXISTS(SELECT 1 FROM operation_lock"
                + " WHERE id=1)",
            "INSERT INTO schema_version SELECT 1,1 WHERE NOT EXISTS(SELECT 1 FROM schema_version"
                + " WHERE id=1)")
        .forEach(jdbc::execute);
  }
}
