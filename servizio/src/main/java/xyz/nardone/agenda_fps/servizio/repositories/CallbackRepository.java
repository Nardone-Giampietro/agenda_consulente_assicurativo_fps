package xyz.nardone.agenda_fps.servizio.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;
import xyz.nardone.agenda_fps.servizio.entities.Callback;


/**
 * Repository for callback entities and custom queries.
 */
public interface CallbackRepository extends CrudRepository<Callback, Integer> {

    /**
     * Returns the total number of callbacks.
     */
    @Query(value = "SELECT COUNT(*) FROM CALLBACKS", nativeQuery=true)
    int countAllCallbacks();

    /**
     * Updates a callback via native SQL.
     */
    @Transactional
    @Modifying
    @Query(value = """
    UPDATE CALLBACKS 
    SET ID_BANKER = :bankerId, 
        ID_CLIENTE = :clientId, 
        DATA = :data, 
        DATA_APP = :dataApp,
        NOTE = :note 
    WHERE ID = :callbackId
    """, nativeQuery = true)
    int updateCallback(
            Integer callbackId,
            Integer bankerId,
            Integer clientId,
            String data,
            String dataApp,
            String note
    );

    /**
     * Counts callbacks scheduled on a specific date.
     */
    @Query(value = "SELECT COUNT(*) FROM CALLBACKS c WHERE c.DATA =:data", nativeQuery = true)
    int countAllByDataEquals(String data);

    /**
     * Deletes callbacks for a specific client.
     */
    @Transactional
    @Modifying
    @Query(value = "DELETE FROM callbacks WHERE id_cliente = :clientId", nativeQuery = true)
    void deleteCallbacksByClientId(Integer clientId);

    /**
     * Deletes callbacks for a specific banker.
     */
    @Transactional
    @Modifying
    @Query(value = "DELETE FROM callbacks WHERE ID_BANKER = :bankerId", nativeQuery = true)
    void deleteCallbacksByBankerId(Integer bankerId);
}
