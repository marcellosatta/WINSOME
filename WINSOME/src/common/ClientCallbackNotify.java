package out.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfaccia su cui si basa l'implementazione del clien per garantire il servizio di notifica callbak.
 */
public interface ClientCallbackNotify extends Remote {

    /**
     * Metodo di notifica quando un nuovo follower seque l'utente.
     * @param username
     * @throws RemoteException
     */
    void notifyNewFriend(String username) throws RemoteException;

    /**
     * Metodo di notifica quando un nuovo follower smette di seguire l'utente.
     * @param username
     * @throws RemoteException
     */
    void notifyOldFriend(String username) throws RemoteException;
}
