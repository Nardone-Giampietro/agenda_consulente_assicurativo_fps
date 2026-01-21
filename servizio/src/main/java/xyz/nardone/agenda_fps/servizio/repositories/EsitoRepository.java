package xyz.nardone.agenda_fps.servizio.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;
import xyz.nardone.agenda_fps.servizio.entities.Esito;


/**
 * Repository for esito entities and custom queries.
 */
public interface EsitoRepository extends CrudRepository<Esito, Integer> {
    /**
     * Finds an esito by label.
     */
    Esito findEsitoByEsitoLike(String esito);

    /**
     * Updates an esito label via native SQL.
     */
    @Transactional
    @Modifying
    @Query(value = """
    UPDATE ESITI 
    SET 
        ESITO = :esito
    WHERE ID = :esitoId
    """, nativeQuery = true)
    int updateEsito(
            Integer esitoId,
            String esito
    );
}
