package xyz.nardone.agenda_fps.servizio.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import xyz.nardone.agenda_fps.servizio.entities.Banker;
import org.springframework.data.repository.CrudRepository;

/**
 * Repository for banker entities and custom queries.
 */
public interface BankerRepository extends CrudRepository<Banker, Integer>{

    /**
     * Returns the total number of bankers.
     */
    @Query(value = "SELECT COUNT(*) FROM bankers", nativeQuery=true)
    int countAllBankers();

    /**
     * Returns the count of bankers matching a name and surname.
     */
    @Query(value = "SELECT COUNT(*) FROM bankers b WHERE  b.nome = :bankerNome AND b.cognome = :bankerCognome", nativeQuery=true)
    int countBankersByName(String bankerNome, String bankerCognome);

    /**
     * Updates banker data via native SQL.
     */
    @Transactional
    @Modifying
    @Query(value = "UPDATE bankers SET nome=:bankerName, cognome=:bankerCognome WHERE id=:bankerId", nativeQuery = true)
    int updateBanker(String bankerName, String bankerCognome, int bankerId);
}
