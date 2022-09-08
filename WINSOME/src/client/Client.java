package out.client;

import out.common.ClientCallbackNotify;
import out.common.RMIInterface;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Classe che modella l'interfaccia a linea di comando, i controlli lato Client dei parametri delle richieste e l'invio
 * delle richieste al Server sulla connessione TCP.
 */
public class Client extends RemoteObject implements ClientCallbackNotify {

    private String username; // username dell'utente che si collegherà
    private ConcurrentLinkedQueue<String> followers; // lista di followers dell'utente, mantenuta localmente
    private RMIInterface serverRMI; // variabile che conterrà l'oggetto remoto
    private ClientCallbackNotify stub; // stub per il servizio Callback
    private Socket socket; // socket di connessione TCP
    private BufferedReader reader = null; // BufferedReader per leggere le risposte del Server
    private BufferedWriter writer = null; // BufferedWriter per scrivere le richieste verso il Server
    private String ClientSocketAddress; // indirizzo IP su cui il Client si collegherà
    private int TCPServerPort; // numero di porta su cui inviare le richieste

    /**
     * Costruttore della classe.
     * @param serverRMI
     * @param ClientSocketAddress
     * @param TCPServerPort
     * @throws RemoteException
     */
    public Client(RMIInterface serverRMI, String ClientSocketAddress, int TCPServerPort) throws RemoteException {
        this.serverRMI = serverRMI;
        this.stub = (ClientCallbackNotify) UnicastRemoteObject.exportObject(this, 0);
        this.followers = new ConcurrentLinkedQueue<>();
        this.ClientSocketAddress = ClientSocketAddress;
        this.TCPServerPort = TCPServerPort;
    }

    /**
     * Metodo di esecuzione del Client
     * @throws IOException
     */
    public void start() throws IOException {
        System.out.println("< -----Welcome to WINSOME!-----");
        Scanner input = new Scanner(System.in); // oggetto per leggere in input da tastiera
        String inputLine; // stringa letta in input
        StringTokenizer tokenizer; // utilizzato per fare il parsing della stringa letta in input (inputLine)
        int numTokens; // numero di token della stringa inputLine
        String operation; // operazione richiesta dall'utente (primo token estratto dalla inputLine)
        String message; // contiene i messaggi da inviare/ricevere
        boolean quit = false; // flag di chiusura dell'applicazione Client
        Thread clientMulticastThread = null; // thread ClientMulticast
        ClientMulticast clientMulticast = null; // oggetto ClientMulticast che modella il task da dare in pasto al
                                                // thread appena definito

        do {
            System.out.print("> ");

            inputLine = input.nextLine();
            tokenizer = new StringTokenizer(inputLine);
            numTokens = tokenizer.countTokens();

            if(numTokens == 0)
                System.out.print("< No operation was written, \"help\" to list the available ones.");
            else {
                operation = tokenizer.nextToken();

                // switch che rimanda alle operazioni implementate
                switch (operation) {
                    case "quit" :
                        if (numTokens != 1)
                            System.out.println("< Invalid operation!\n");
                        else {
                            if(socket != null) {
                                send(inputLine);
                                try {
                                    this.followers.clear();
                                    this.socket.close();

                                    this.followers = null;
                                    this.socket = null;
                                    this.username = null;

                                    clientMulticastThread.interrupt();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            System.out.println("See you soon! :)");
                            quit = true;
                        }
                        break;
                    case "help" :
                        if(socket == null) {
                            if(numTokens != 1) {
                                System.out.println("< Wrong number of token. Try: \"help\".");
                            }
                            else {
                                System.out.print("< Register or login to access WINSOME or quit if you want to leave.\n" +
                                        "Here are the possible operations:\n\n" +
                                        " - register username password tags [from one to five] : register a new user;\n" +
                                        " - login username password : login with the specified username and password;\n" +
                                        " - login_after_crash username password : login with the specified username" +
                                            " and password in case of crash of the client;\n" +
                                        " - quit : close the application.\n");
                            }
                        }
                        else {
                            send(inputLine);
                            message = get();

                            if(message != null) {
                                System.out.println("< " + message);
                            }
                        }
                        break;
                    case "register" :
                        if(socket != null) {
                            System.out.println("< User already logged in.");
                        }
                        else {
                            if(numTokens > 8 || numTokens < 4) {
                                System.out.println("< Wrong number of token. Try: \"register USERNAME PASSWORD TAGS [from one to five]\".");
                            }
                            else {
                                int registerCode = 0;
                                String username = tokenizer.nextToken();
                                String password = tokenizer.nextToken();
                                ConcurrentLinkedQueue<String> tags = new ConcurrentLinkedQueue<>();

                                while (tokenizer.hasMoreTokens())
                                    tags.add(tokenizer.nextToken().toLowerCase());

                                try {
                                    registerCode = this.serverRMI.register(username, password, tags);
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }

                                if(registerCode == 1) {
                                    System.out.println("< Null username or password.");
                                }
                                else if(registerCode == 2) {
                                    System.out.println("< Empty username or password.");
                                }
                                else if(registerCode == 3) {
                                    System.out.println("< Wrong number of tags, choose between one to five tags.");
                                }
                                else if(registerCode == 4){
                                    System.out.println("< One ore more tags are null.");
                                }
                                else if(registerCode == 5){
                                    System.out.println("< One or more tags are empty.");
                                }
                                else if(registerCode == 6){
                                    System.out.println("< This username is already registered, try another one.");
                                }
                                else {
                                    System.out.println("< User registered successfully.");
                                }
                            }
                        }
                        break;
                    case "login" :
                    case "login_after_crash":
                        if(socket != null) {
                            System.out.println("< User already logged in.");
                        }
                        else {
                            if(numTokens != 3) {
                                System.out.println("< Wrong number of token. Try: \"login USERNAME PASSWORD\".");
                            }
                            else {
                                try {
                                    this.socket = new Socket();
                                    this.socket.connect(new InetSocketAddress(InetAddress.getByName(ClientSocketAddress), TCPServerPort));
                                    this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                                    this.writer = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                send(inputLine);
                                message = get();

                                if(message != null) {
                                    String correctLoginString = "";
                                    String multicastAddress = null;
                                    StringTokenizer tokenizerLogin = new StringTokenizer(message);
                                    int nLoginTokens = tokenizerLogin.countTokens();
                                    if(nLoginTokens == 5) {
                                        for (int i = 0; i < 2; i++) {
                                            correctLoginString += tokenizerLogin.nextToken() + " ";
                                        }
                                        correctLoginString += tokenizerLogin.nextToken();
                                        multicastAddress = tokenizerLogin.nextToken();
                                        String port = tokenizerLogin.nextToken();

                                        if (correctLoginString.equals("User logged in.")) {

                                            System.out.println("< " + correctLoginString);

                                            try {
                                                username = tokenizer.nextToken();

                                                if(operation.equals("login_after_crash"))
                                                    this.serverRMI.unregisteraForCallback(username, this.stub);

                                                this.serverRMI.registeraForCallback(username, this.stub);
                                                this.followers = this.serverRMI.callBackFriends(username);

                                                clientMulticast = new ClientMulticast(multicastAddress, Integer.parseInt(port));
                                                clientMulticastThread = new Thread(clientMulticast);
                                                clientMulticastThread.start();

                                            } catch (RemoteException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                    else {
                                        System.out.println("< " + message);
                                        reader.close();
                                        writer.close();
                                        socket.close();
                                        socket = null;
                                    }
                                }
                            }
                        }
                        break;
                    case "logout" :
                        if(numTokens != 2) {
                            System.out.println("< Wrong number of token. Try: \"logout USERNAME\".");
                            }
                        else {
                            if(socket == null)
                                System.out.println("< You are not logged in.");
                            else {
                                send(inputLine);
                                message = get();
                                if(message.equals("User logged out.")) {
                                    clientMulticastThread.interrupt();
                                    this.serverRMI.unregisteraForCallback(username, this.stub);
                                    this.socket.close();
                                    this.socket = null;
                                    this.username = null;
                                    System.out.println("< " + message);

                                }
                                else
                                    System.out.println("< " + message);
                            }
                        }
                        break;
                    case "follow" :
                    case "unfollow" :
                        if(socket == null)
                            System.out.println("< You are not logged in.");
                        else {
                            String usernameFollowing = tokenizer.nextToken();
                            if(usernameFollowing.equals(username)) {
                                if (operation.equals("follow"))
                                    System.out.println("< You already follow yourself. :)");
                                else
                                    System.out.println("< You can't stop to follow yourself. :)");
                            }
                            else {
                                send(inputLine);
                                message = get();
                                if (message != null)
                                    System.out.println("< " + message);
                            }
                        }
                        break;
                    case "list_users" :
                    case "list_following" :
                    case "blog" :
                    case "show_feed" :
                    case "show_post" :
                    case "post" :
                    case "delete" :
                    case "rewin" :
                    case "comment" :
                    case "wallet" :
                    case "wallet_btc" :
                        if(socket == null)
                            System.out.println("< You are not logged in.");
                        else {
                            send(inputLine);
                            message = get();

                            if(message != null)
                                System.out.println("< " + message);
                        }
                        break;
                    case "rate" :
                        if(socket == null)
                            System.out.println("< You are not logged in.");
                        else {
                            tokenizer.nextToken();
                            int vote = Integer.parseInt(tokenizer.nextToken());
                            if(vote == 1 || vote == -1) {
                                send(inputLine + " " + username);
                                message = get();
                                if(message != null)
                                    System.out.println("< " + message);
                            }
                            else {
                                System.out.println("< Wrong rating. Try: \"rate IDPOST 1\" or \"rate IDPOST -1\".");
                            }
                        }
                        break;
                    case "list_followers" :
                        if(socket == null)
                            System.out.println("< You are not logged in.");
                        else {
                            if(numTokens != 1)
                                System.out.println("< Wrong rating. Try: \"list_followers\".");
                            else {
                                listFollowers();
                            }
                        }
                        break;
                    default:
                        System.out.println("< Operation doesn't exist, \"help\" to list the available ones.");
                        break;
                }
            }
        } while(quit != true);
    }

    /**
     * Implementazione del metodo definito nell'interfaccia ClientCallbackNotify che notifica di un nuovo follower.
     * @param username
     * @throws RemoteException
     */
    @Override
    public void notifyNewFriend(String username) throws RemoteException{
        this.followers.add(username);
        System.out.println("< New friend: " + username);
    }

    /**
     * Implementazione del metodo definito nell'interfaccia ClientCallbackNotify che notifica di un vecchio follower.
     * @param username
     * @throws RemoteException
     */
    @Override
    public void notifyOldFriend(String username) throws RemoteException{
        this.followers.remove(username);
        System.out.println("< Old friend: " + username);
    }

    /**
     * Metodo che invia un messaggio sulla connessione TCP al Server.
     * @param message
     */
    public void send(String message) {
        try {
            this.writer.write(message  + "\n");
            this.writer.flush();
        } catch (IOException e) {
            System.err.print("Send error!\n");
            e.printStackTrace();
        }
    }

    /**
     * Metodo che riceve un messaggio dalla connessione TCP con il Server.
     * @return
     */
    public String get() {
        String message = null;
        try {
            message = this.reader.readLine();
        } catch (IOException e) {
            System.err.println("Get error!\n");
            e.printStackTrace();
        }
        message = message.replace("*", "\n");
        return message;
    }

    /**
     * Metodo che stampa la lista dei followers, mantenuta locale.
     */
    public void listFollowers() {
        System.out.println("< List of followers:");
        for(String follower: this.followers) {
            System.out.println(follower);
        }
    }
}

