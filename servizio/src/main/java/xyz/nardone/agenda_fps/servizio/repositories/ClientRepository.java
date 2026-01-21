package xyz.nardone.agenda_fps.servizio.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import xyz.nardone.agenda_fps.servizio.entities.Client;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

/**
 * Repository for client entities and custom queries.
 */
public interface ClientRepository extends CrudRepository<Client, Integer>{
    /**
     * Finds clients for a banker or all if bankerId is null.
     */
    @Query(value = "SELECT * FROM clients c WHERE (:bankerId IS NULL OR c.id_banker = :bankerId)", nativeQuery = true)
    List<Client> findClientsByBankerId(Integer bankerId);

    /**
     * Returns the number of clients with the same name and banker.
     */
    @Query(value = "SELECT COUNT(*) FROM clients c WHERE c.NOME = :clientNome AND c.COGNOME = :clientCognome AND c.ID_BANKER = :bankerId", nativeQuery = true)
    int countClientsByNameandBankerId(String clientNome, String clientCognome, Integer bankerId);

    /**
     * Updates client data via native SQL.
     */
    @Transactional
    @Modifying
    @Query(value = "UPDATE clients SET nome=:clientName, cognome=:clientCognome, id_banker=:bankerId WHERE id=:clientId", nativeQuery = true)
    int updateClient(String clientName, String clientCognome, int clientId, int bankerId);
}
