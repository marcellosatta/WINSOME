package configFile.out;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

public class ConfigFileMain {

    private static final String CONFIG_FILE_SERVER_PATH = "./configFile/configServer/"; // Percorso della cartella che contiene il file di configurazione del server
    private static final String CONFIG_FILE_CLIENT_PATH = "./configFile/configClient/"; // Percorso della cartella che contiene il file di configurazione del client

    private static final String SERVER_CONFIG_FILE = "serverConfigFile.properties"; // file di configurazione server
    private static final String CLIENT_CONFIG_FILE = "clientConfigFile.properties"; // file di configurazione client


    public static void main(String[] args) {

        File configDirServer = new File(CONFIG_FILE_SERVER_PATH);

        if(!configDirServer.exists()) {
            configDirServer.mkdir();
        }

        try (OutputStream output = new FileOutputStream(CONFIG_FILE_SERVER_PATH + SERVER_CONFIG_FILE)) {

            Properties prop = new Properties();

            // scrivo le proprietà 
            prop.setProperty("timeIntervalRewards", "30000");
            prop.setProperty("authorRewardPercentage", "79");
            prop.setProperty("TCPServerPort", "1501");
            prop.setProperty("UDPMultiCastPort", "6789");
            prop.setProperty("RMIRegistryPort", "4002");
            prop.setProperty("RMIBindingName", "ServerRMI");
            prop.setProperty("ServerSocketAddress", "localhost");
            prop.setProperty("MultiCastAddress", "239.255.1.3");
            prop.setProperty("backLog", "50");

            // salvo su file del server
            prop.store(output, null);

            System.out.println(prop);

        } catch (IOException io) {
            io.printStackTrace();
        }

        File configDirClient = new File(CONFIG_FILE_CLIENT_PATH);

        if(!configDirClient.exists()) {
            configDirClient.mkdir();
        }
        try (OutputStream output = new FileOutputStream(CONFIG_FILE_CLIENT_PATH + CLIENT_CONFIG_FILE)) {

            Properties prop = new Properties();

            // scrivo le proprietà 
            prop.setProperty("TCPServerPort", "1501");
            prop.setProperty("RMIRegistryPort", "4002");
            prop.setProperty("RMIBindingName", "ServerRMI");
            prop.setProperty("ClientSocketAddress", "127.0.0.1");

            // salvo su file del client
            prop.store(output, null);

            System.out.println(prop);

        } catch (IOException io) {
            io.printStackTrace();
        }
    }
}
