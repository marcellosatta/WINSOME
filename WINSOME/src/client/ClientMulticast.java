package out.client;

import java.io.IOException;
import java.net.*;

/**
 * Classe che si occupa di attendere il messaggio di avvenuto calcolo delle ricompense e aggiornamento portafogli.
 */
public class ClientMulticast implements Runnable {

    private String multicastAddress; // indirizzo Multicast su cui il ClientMulticast si collegherà
    private int multicastPort; // numero di porta su cui attendere il messaggio

    /**
     * Costruttore della classe.
     * @param address
     * @param port
     */
    public ClientMulticast(String address, int port) {
        this.multicastAddress = address;
        this.multicastPort = port;
    }

    /**
     * Metodo di esecuzione del task.
     */
    @Override
    public void run() {

        MulticastSocket multicastSocket = null;
        InetSocketAddress group = null;
        NetworkInterface networkInterface = null;

        // Creazione del socket e inserimento nel gruppo Multicast
        try {
            multicastSocket = new MulticastSocket(multicastPort);
            group = new InetSocketAddress(InetAddress.getByName(this.multicastAddress), multicastPort);
            networkInterface = NetworkInterface.getByName("wlan1");
            multicastSocket.joinGroup(group, networkInterface);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // while di attesa del messaggio, il thread si blocca sulla receive e prosegue quando arriverà il messaggio
        while(!Thread.interrupted()) {
            byte[] buffer = new byte[8192];

            try {
                DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
                multicastSocket.receive(datagramPacket);

                if(Thread.interrupted())
                    break;

                String message = new String(datagramPacket.getData());

                message = message.substring(0, datagramPacket.getLength());
                // stampa del messaggio
                System.out.println(message);

            } catch (IOException e) {
                System.out.println(e);
            }
        }

        // uscita dal gruppo Multicast e chiusura del socket
        try {
            multicastSocket.leaveGroup(group, networkInterface);
            multicastSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

