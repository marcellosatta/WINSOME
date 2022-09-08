package out.server;

import out.common.RMIInterface;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;
import java.util.concurrent.*;

/**
 * Classe ServerMain che da avvio al Server.
 */
public class ServerMain {

    private static final String CONFIG_FILE_PATH = "./configFile/configServer/"; // path della cartella che contiene il
                                                                                 // file di configurazione server
    private static final String SERVER_CONFIG_FILE = "serverConfigFile.properties"; // file di configurazione server

    public static void main(String[] args) {

        // variabili da leggere dal file di configurazione:

        int timeIntervalRewards = -1; // intervallo di tempo tra un calcolo ricompense e l'altro (millisecondi)
        int authorRewardPercentage = -1; // percentuale di ricompensa autore
        int TCPServerPort = -1; // numero di porta di ascolto del server
        int UDPMultiCastPort = -1; // numero di porta su cui il ServerMulticast invierà il messaggio di avvenuto
                                   // calcolo ricompense
        int RMIRegistryPort = -1; // numero di porta su cui il registry rimane in ascolto
        String RMIBindingName = null; // nome con cui il riferimento all'oggetto remoto è registrato sul Registry
        String ServerSocketAddress = null; // indirizzo IP del server
        String MultiCastAddress = null; // indirizzo IP multicast
        int backLog = -1; // indica la lunghezza massima della coda delle richieste di connessione

        // lettura file di configurazione
        try (InputStream input = new FileInputStream(CONFIG_FILE_PATH + SERVER_CONFIG_FILE)) {

            Properties prop = new Properties();
            // load a properties file
            prop.load(input);

            timeIntervalRewards = Integer.parseInt(prop.getProperty("timeIntervalRewards"));
            authorRewardPercentage = Integer.parseInt(prop.getProperty("authorRewardPercentage"));
            TCPServerPort = Integer.parseInt(prop.getProperty("TCPServerPort"));
            UDPMultiCastPort = Integer.parseInt(prop.getProperty("UDPMultiCastPort"));
            RMIRegistryPort = Integer.parseInt(prop.getProperty("RMIRegistryPort"));
            RMIBindingName = prop.getProperty("RMIBindingName");
            ServerSocketAddress = prop.getProperty("ServerSocketAddress");
            MultiCastAddress = prop.getProperty("MultiCastAddress");
            backLog = Integer.parseInt(prop.getProperty("backLog"));

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // creazione oggetto Server
        Server server = new Server(authorRewardPercentage);

        // Avvio thread parallelo di calcolo delle ricompense (ServerMulticast)
        Thread t = new Thread(new ServerMulticast(server, timeIntervalRewards, UDPMultiCastPort, MultiCastAddress));
        t.start();

        // preparazione servizio RMI
        RMIInterface stub;
        try {
            stub = (RMIInterface) UnicastRemoteObject.exportObject(server, RMIRegistryPort);
            LocateRegistry.createRegistry(RMIRegistryPort);
            Registry register = LocateRegistry.getRegistry(RMIRegistryPort);
            register.rebind(RMIBindingName, stub);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        // creazione threadpool
        ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        System.out.println("WINSOME Server ready.");

        // ciclo di attesa nuove richieste di connessione
        try  {
            ServerSocket serverSocket = new ServerSocket(TCPServerPort, backLog, InetAddress.getByName(ServerSocketAddress));
            while(!Thread.interrupted()) {
                Socket client = serverSocket.accept();
                try {
                    ClientTask clientTask = new ClientTask(server, client,  UDPMultiCastPort, MultiCastAddress);
                    pool.execute(clientTask);
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // shutdown threadpool
        pool.shutdown();
        try {
            while(!pool.isTerminated()) {
                pool.awaitTermination(5000, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}
