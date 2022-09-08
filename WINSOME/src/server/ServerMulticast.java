package out.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.sql.Timestamp;

/**
 * Classe che modella il thread di calcolo ricompense, invio messaggio di notifica di avvenuto calcolo agli utenti e
 * calcolo cambio valuta in Bitcoin.
 */
public class ServerMulticast implements Runnable {

    private static final String MESSAGE = "Server updated rewards and wallets."; // messaggio di notifica
    private Server server; // oggetto Server
    private Timestamp lastTimestamp; // timstamp che indica il momento in cui l'ultimo calcolo ricompense è avvenuto
    private int timeItervalRewards; // intervallo di tempo tra un calcolo e il successivo
    private int UDPMultiCastPort; // numero di porta su cui inviare il messaggio di notifica
    private String MultiCastAddress; // indirizzo IP su cui inviare il messaggio di notifica

    /**
     * Costruttore oggetto ServerMulticast
     * @param server
     * @param timeItervalRewards
     * @param UDPMultiCastPort
     * @param MultiCastAddress
     */
    public ServerMulticast(Server server, int timeItervalRewards, int UDPMultiCastPort, String MultiCastAddress) {
        this.server = server;
        this.lastTimestamp = new Timestamp(0);
        this.timeItervalRewards = timeItervalRewards;
        this.UDPMultiCastPort = UDPMultiCastPort;
        this.MultiCastAddress = MultiCastAddress;
    }

    /**
     * Implementazione metodo run.
     */
    @Override
    public synchronized void run() {

        // predisposizione invio messaggio di notifica
        InetAddress ia = null;
        try {
            ia = InetAddress.getByName(MultiCastAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        byte[] data;
        data = MESSAGE.getBytes();
        DatagramPacket dp = new DatagramPacket(data, data.length, ia, UDPMultiCastPort);
        DatagramSocket ms = null;
        try {
            ms = new DatagramSocket(6800);
        } catch (SocketException e) {
            e.printStackTrace();
        }


        while (true) {
            // calcolo cambio valuta in Bitcoin
            this.server.setChange(getChange());

            // sleep tra un calcolo ricompense e il successivo
            try {
                Thread.sleep(timeItervalRewards);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // calcolo ricompense e aggiornamento portafogli
            this.lastTimestamp = this.server.updateRewards(lastTimestamp);
            System.out.println(MESSAGE);

            // invio datagramma contenente il messaggio di notifica
            try {
                ms.send(dp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Metodo di richiesta intero e frazione decimale casuali per il calcolo del cambio valuta in Bitcoin.
     * @return il numero intero casuale moltiplicato per la frazione decimale casuale
     */
    public double getChange()  {
        URL url = null;
        InputStream streamInteger = null;
        InputStream streamFraction = null;
        int rate = -1;

        // richiesta numero intero casuale generato dal servizio web
        try {
            url = new URL("https://www.random.org/integers/?num=1&min=1&max=" + 20394 + "&col=1&base=10&format=plain&rnd=new");
            streamInteger = null;
            streamInteger = url.openStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // lettura risposta
        InputStreamReader inputStreamReaderInteger = new InputStreamReader(streamInteger);
        BufferedReader bufferedReaderInteger = new BufferedReader(inputStreamReaderInteger);
        try {
            rate = Integer.parseInt(bufferedReaderInteger.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // richiesta frazione decimale casuale generata dal servizio web
        try {
            url = new URL("https://www.random.org/decimal-fractions/?num=1&dec=10&col=1&format=plain&rnd=new");
            streamFraction = url.openStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // lettura risposta
        double fraction = -1;
        InputStreamReader inputStreamReaderFraction = new InputStreamReader(streamFraction);
        BufferedReader bufferedReaderFraction = new BufferedReader(inputStreamReaderFraction);
        try {
            fraction = Double.parseDouble(bufferedReaderFraction.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rate * fraction;
    }
}
