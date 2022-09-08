package out.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Interfaccia che, una volta implementata dal server, fungerà da oggetto remoto per l'operazione di registrazione,
 * registrazione e cancellazione callback e recupero lista followers.
 */
public interface RMIInterface extends Remote  {

    /**
     * Metodo di registrazione sulla piattaforma.
     * @param username
     * @param password
     * @param tags
     * @return numero intero rappresentante un codice verificato dal chiamante
     * @throws RemoteException
     */
    int register(String username, String password, ConcurrentLinkedQueue<String> tags) throws RemoteException;

    /**
     * Registrazione al servizio di callback.
     * @param username
     * @param stub
     * @throws RemoteException
     */
    void registeraForCallback(String username, ClientCallbackNotify stub) throws RemoteException;

    /**
     * Cancellazione della registrazione dal servizio di callback.
     * @param username
     * @param stub
     * @throws RemoteException
     */
    void unregisteraForCallback(String username, ClientCallbackNotify stub) throws RemoteException;

    /**
     * Metodo di recupero lista followers.
     * @param username
     * @return lista di utenti seguaci dell'utente che chiama il metodo sull'oggetto remoto
     * @throws RemoteException
     */
    ConcurrentLinkedQueue<String> callBackFriends(String username) throws RemoteException;

}
