package xyz.nardone.agenda_fps.servizio.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;
import xyz.nardone.agenda_fps.servizio.entities.Polizza;

/**
 * Repository for polizza entities and custom queries.
 */
public interface PolizzaRepository extends CrudRepository<Polizza, Integer> {

    /**
     * Finds a polizza by label.
     */
    Polizza findPolizzaByPolizzaLike(String polizza);

    /**
     * Updates a polizza label via native SQL.
     */
    @Transactional
    @Modifying
    @Query(value = """
    UPDATE POLIZZE 
    SET 
        POLIZZA = :polizza
    WHERE ID = :polizzaId
    """, nativeQuery = true)
    int updatePolizza(
            Integer polizzaId,
            String polizza
    );
}
