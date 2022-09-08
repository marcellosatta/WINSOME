package out.client;

import out.common.RMIInterface;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;
import static java.lang.System.exit;

/**
 * Classe contenente il main che da avvio al Client.
 */
public class ClientMain {

    private static final String CONFIG_FILE_PATH = "./configFile/configClient/"; // percorso della cartella che contiene i file di configurazione del Client
    private static final String CLIENT_CONFIG_FILE = "clientConfigFile.properties"; // nome del file di configurazione del Client

    public static void main(String[] args) throws IOException, NotBoundException, InterruptedException {

        int TCPServerPort = -1; // numero di porta su cui il Server è in attesa di nuove connessioni
        int RMIRegistryPort = -1; // numero di porta su cui ascolta il Registry
        String RMIBindingName = null; // nome associato all'oggetto remoto implementato dal Server
        String ClientSocketAddress = null; // indirizzo IP su cui il Client si connetterà

        // lettura parametri dal file di configurazione
        try (InputStream input = new FileInputStream(CONFIG_FILE_PATH + CLIENT_CONFIG_FILE)) {

            Properties prop = new Properties();
            prop.load(input);

            TCPServerPort = Integer.parseInt(prop.getProperty("TCPServerPort"));
            RMIRegistryPort = Integer.parseInt(prop.getProperty("RMIRegistryPort"));
            RMIBindingName = prop.getProperty("RMIBindingName");
            ClientSocketAddress = prop.getProperty("ClientSocketAddress");

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // richiesta al riferimento dell'oggetto remoto creato dal Server
        Registry registry;
        registry = LocateRegistry.getRegistry(RMIRegistryPort);
        RMIInterface remote = (RMIInterface) registry.lookup(RMIBindingName);

        // avvio del Client
        Client c = new Client(remote, ClientSocketAddress, TCPServerPort);
        c.start();

        exit(0);
    }
}
