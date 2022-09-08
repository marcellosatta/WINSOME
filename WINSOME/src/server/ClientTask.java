package out.server;


import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Classe che descrive il task che si occupa di gestire le richieste del client.
 */
public class ClientTask implements Runnable {

    private Server server; // oggetto Server
    private Socket socket; // socket di comunicazione TCP
    private BufferedReader reader; // BufferedReader su cui leggere le richieste del client
    private BufferedWriter writer; // BUfferedWriter su cui scrivere le risposte verso il client
    private String username; // username che identifica l'utente a cui il ClientTask si rivolge
    private int UDPMultiCastPort; // numero di porta Multicast
    private String MultiCastAddress; // indirizzo IP Multicast

    /**
     * Costruttore ClientTask
     * @param server
     * @param socket
     * @param UDPMultiCastPort
     * @param MultiCastAddress
     * @throws IOException
     */
    public ClientTask(Server server, Socket socket, int UDPMultiCastPort, String MultiCastAddress) throws IOException {
        this.server = server;
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        this.UDPMultiCastPort = UDPMultiCastPort;
        this.MultiCastAddress = MultiCastAddress;
    }

    /**
     * Override del metodo di esecuzione del task
     */
    @Override
    public void run() {
        StringTokenizer tokenizer; // utilizzato per fare il parsing della stringa letta in input (inputLine)
        int numTokens; // numero di token della stringa inputLine
        String operation; // operazione richiesta dall'utente (primo token estratto dalla inputLine)
        String message; // contiene i messaggi da inviare/ricevere

        boolean quit = false;
        do {
            try {
                // get bloccante che legge dal BufferedReader
                message =  get();

                if (message != null) {
                    tokenizer = new StringTokenizer(message);
                    numTokens = tokenizer.countTokens();
                    if (numTokens == 0)
                        send("Write an operation, \"help\" to list the available ones.");
                    else {
                        operation = tokenizer.nextToken();

                        // swtich che indirizza alle operazioni implementate
                        switch (operation) {
                            case "quit":
                                if (numTokens != 1)
                                    send("Wrong operation, try: \"quit\".");
                                else {
                                        this.server.logout(username);
                                        send("User logged out.");
                                        socket.close();
                                        socket = null;
                                        quit = true;
                                }
                                break;
                            case "help":
                                if (numTokens == 1) {
                                    message = "Here are the possible operations:\n\n" +
                                            " - post \"title\" \"content\" : create a new post;\n" +
                                            " - rewin idPost : share on your blog the post with the specified id;\n" +
                                            " - delete idPost : delete your post with the specified id;\n" +
                                            " - show_feed : shows all the posts shared by the users you follow;\n" +
                                            " - blog : shows all the post you've created;\n" +
                                            " - show_post idPost : shows a specific post;" +
                                            " - rate isPost rating : rate a post (rating should be 1 or -1);\n" +
                                            " - comment idPost \"comment\" : comment a post;\n" +
                                            " - follow username : follow the user wihch has the specified username;\n" +
                                            " - unfollow username : unfollow the user which has the specified username;\n" +
                                            " - list_following : lists all the users you follow;\n" +
                                            " - list_followers : lists all the users that follow you;\n" +
                                            " - list_users : shows all the users with similar interests;\n" +
                                            " - wallet : shows your wallet;\n" +
                                            " - wallet_btc : shows your wallet in Bitcoin;\n" +
                                            " - logout username : logout from WINSOME;\n" +
                                            " - quit : close the application;\n";
                                    send(message);
                                } else {
                                    message = "Wrong operation, try: \"help\".";
                                    send(message);
                                }
                                break;
                            case "login":
                            case "login_after_crash":
                                if (numTokens == 3) {
                                    String username = tokenizer.nextToken();
                                    String password = tokenizer.nextToken();
                                    try {
                                        if(operation.equals("login_after_crash")) {
                                            server.logout(username);
                                        }
                                        int res = -1;
                                        if((res = server.login(username, password)) == 0) {
                                            this.username = username;
                                            message = "User logged in." + " " + this.MultiCastAddress + " " + this.UDPMultiCastPort;
                                        }
                                        else if( res == 1)
                                            message = "Unregistered user.";
                                        else if( res == 2)
                                            message = "User already logged in, Try \"login_after_crash USERNAME PASSWORD\" if the client crashed.";
                                        else
                                            message = "Wrong password.";

                                    } catch (Exception e) {
                                        message = e.getMessage();
                                    }

                                    send(message);

                                } else {
                                    message = "Wrong operation: write \"login USERNAME PASSWORD\" or \"login_after_crash USERNAME PASSWORD\".";
                                    send(message);
                                }
                                break;
                            case "logout":
                                if (numTokens == 2) {
                                    String username = tokenizer.nextToken();

                                    if (!username.equals(this.username)) {
                                        message = "Wrong username.";
                                        send(message);
                                    }
                                    else if(server.logout(username)) {
                                        message = "User logged out.";
                                        send(message);
                                        quit = false;
                                    }
                                    else {
                                        message = "User already offline.";
                                        send(message);
                                    }

                                } else {
                                    send("Wrong number of token. Try: \"logout USERNAME\".");
                                }
                                break;
                            case "list_users":
                                if(numTokens == 1) {
                                    ArrayList<String> usersList = this.server.findUserList(this.username);
                                    int l = usersList.size();
                                    message = "";
                                    for (int i = 0; i < l; i++) {
                                        message += usersList.get(i) + "\n";
                                    }
                                    send(message);
                                }
                                else {
                                    message = "Wrong number of token. Try: \"list_users\".";
                                    send(message);
                                }
                                break;
                            case "list_following":
                                if(numTokens == 1) {
                                    ConcurrentLinkedQueue<String> listFollowing = this.server.listFollowing(this.username);
                                    message = "";
                                    for (String following: listFollowing) {
                                        message += following + "\n";
                                    }
                                    send(message);
                                }
                                else {
                                    message = "Wrong number of token. Try: \"list_following\".";
                                    send(message);
                                }

                                break;
                            case "follow":
                                if(numTokens == 2) {
                                    String friend = tokenizer.nextToken();
                                    if(this.server.isPresent(friend)) {
                                        if(friend.equals(username)) {
                                            message = this.username + "You already follow yourself. :)";
                                            send(message);
                                        }
                                        else if (this.server.follow(this.username, friend)) {
                                            message = this.username + " started to follow " + friend + ".";
                                            send(message);
                                        }
                                        else {
                                            message = this.username + " already followed " + friend + ".";
                                            send(message);
                                        }
                                    }
                                    else {
                                        message = "Unregisterd user.";
                                        send(message);
                                    }
                                }
                                else {
                                    message = "Wrong number of token. Try: \"follow USERNAME\".";
                                    send(message);
                                }
                                break;
                            case "unfollow":
                                if(numTokens == 2) {
                                    String friend = tokenizer.nextToken();
                                    if(this.server.isPresent(friend)) {
                                        if(friend.equals(username)) {
                                            message = this.username + "You can't stop to follow yourself. :)";
                                            send(message);
                                        }
                                        else if (this.server.unfollow(this.username, friend)) {
                                            message = this.username + " stopped following " + friend;
                                            send(message);
                                        }
                                        else {
                                            message = this.username + " wasn't following " + friend;
                                            send(message);
                                        }
                                    }
                                    else {
                                        message = "Unregisterd user.";
                                        send(message);
                                    }
                                }
                                else {
                                    message = "Wrong number of token. Try: \"unfollow USERNAME\".";
                                    send(message);
                                }
                                break;
                            case "blog":
                                if(numTokens == 1) {
                                    message = "";
                                    ArrayList<String> blog = this.server.getBlog(this.username);;
                                    int l = blog.size();

                                    for (int i = 0; i < l; i++) {
                                        message += blog.get(i) + "\n";
                                    }
                                    send(message);
                                }
                                else {
                                    message = "Wrong number of token. Try: \"blog\".";
                                    send(message);
                                }
                                break;
                            case "show_feed":
                                if(numTokens == 1) {
                                    message = "";
                                    ArrayList<String> feed = this.server.getFeed(this.username);;
                                    int l = feed.size();

                                    for (int i = 0; i < l; i++) {
                                        message += feed.get(i) + "\n";
                                    }
                                    send(message);

                                }
                                else {
                                    message = "Wrong number of token. Try: \"show_feed\".";
                                    send(message);
                                }
                                break;
                            case "show_post":
                                if(numTokens == 2) {
                                    int idPost = Integer.parseInt(tokenizer.nextToken());
                                    Post post = this.server.getPost(idPost);
                                    String author = this.server.getAuthorFromPostId(idPost);
                                    if(post != null) {
                                        if((server.listFollowing(username)).contains(author) || username.equals(author)) {
                                            message = "Author: " + author + "Title: " + post.getTitle() + "\nContent: " + post.getContent() + "\nPositive votes: " + post.getPositiveVotes()
                                                    + "\nNegative votes: " + post.getNegativeVotes() + "\nComments: \n";
                                            for (Map.Entry mapElement : post.getComments().entrySet()) {
                                                int key = (Integer) mapElement.getKey();
                                                message += post.getComments().get(key).getAuthor() + ": " + post.getComments().get(key).getComment() + "\n";
                                            }
                                            send(message);
                                        }
                                        else {
                                            message = "We can't show you a post which is not on your feed.";
                                            send(message);
                                        }
                                    }
                                    else {
                                        message = "Post doesn't exist.";
                                        send(message);
                                    }
                                }
                                else {
                                    message = "Wrong number of token. Try: \"show_post IDPOST\".";
                                    send(message);
                                }
                                break;
                            case "post":
                                if(numTokens >= 3) {
                                    String title;
                                    String content = "";
                                    boolean err = false;
                                    title = message;
                                    if(title.indexOf("\"") != -1)
                                        title = title.substring(title.indexOf("\"") + 1);
                                    else
                                        err = true;
                                    if(title.indexOf("\"") != -1) {
                                        content = title.substring(title.indexOf("\"") + 1);
                                        title = title.substring(0, title.indexOf("\""));
                                    }
                                    else
                                        err = true;

                                    if(content.indexOf("\"") != -1) {
                                        content = content.substring(content.indexOf("\"") + 1);
                                    }
                                    else
                                        err = true;
                                    if(content.indexOf("\"") != -1) {
                                            content = content.substring(0, content.indexOf("\""));
                                    }
                                    else
                                        err = true;
                                    if(err) {
                                        message = "Enter both title and content in quotation marks.";
                                        send(message);
                                    }
                                    else {
                                        if(title.length() <= 20 && content.length() <= 500) {
                                            int result = this.server.createPost(this.username, title, content);
                                            if (result == 0) {
                                                message = "Post created successfully.";
                                                send(message);
                                            } else if (result == 1) {
                                                message = "Empty title or content.";
                                                send(message);
                                            } else {
                                                message = "Something went wrong. Try again.";
                                                send(message);
                                            }
                                        }
                                        else {
                                            message = "Title or content length is too much, Only 20 characters are allowed " +
                                                    "for the title and 500 for content.";
                                            send(message);
                                        }
                                    }
                                }
                                else {
                                    message = "Wrong number of token. Try: \"post \"TITLE\" \"CONTENT\".";
                                    send(message);
                                }
                                break;
                            case "delete":
                                if(numTokens == 2) {
                                    int idPost = Integer.parseInt(tokenizer.nextToken());
                                    String author = this.server.getAuthorFromPostId(idPost);
                                    if(author.equals(this.username)) {
                                        if (this.server.deletePost(idPost, username)) {
                                            message = "Post deleted successfully.";
                                            send(message);
                                        } else {
                                            message = "Post doesn't exist.";
                                            send(message);
                                        }
                                    }
                                    else {
                                        message = "You can't delete a post created by another user.";
                                        send(message);
                                    }
                                }
                                else {
                                    message = "Wrong number of token. Try: \"delete IDPOST\".";
                                    send(message);
                                }
                                break;
                            case "rate":
                                if(numTokens == 4) {
                                    int idPost = Integer.parseInt(tokenizer.nextToken());
                                    int rating = Integer.parseInt(tokenizer.nextToken());
                                    String username = tokenizer.nextToken();
                                    int res;
                                    if(rating == 1 || rating == -1) {
                                        if ((res = this.server.ratePost(idPost, rating, username)) == 0) {
                                            message = "Post rate successfully";
                                            send(message);
                                        }
                                        else if (res == 1) {
                                            message = "User already rated this post.";
                                            send(message);
                                        }
                                        else if (res == 2) {
                                            message = "Wrong rating. Try: \"rate IDPOST 1\" or \"rate IDPOST -1\".";
                                            send(message);
                                        }
                                        else if (res == 3) {
                                            message = "You can't rate a post created by a user that you don't follow.";
                                            send(message);
                                        }
                                        else if (res == 4) {
                                            message = "Post doesn't exist.";
                                            send(message);
                                        }
                                    }
                                    else {
                                        message = "Wrong rating. Try: \"rate IDPOST 1\" or \"rate IDPOST -1\".";
                                        send(message);
                                    }
                                }
                                else {
                                    message = "Wrong number of token. Try: \"rate IDPOST 1\" or \"rate IDPOST -1\".";
                                    send(message);
                                }
                                break;
                            case "rewin":
                                if(numTokens == 2) {
                                    int idPost = Integer.parseInt(tokenizer.nextToken());
                                    Post post = this.server.getPost(idPost);
                                    String author = this.server.getAuthorFromPostId(idPost);
                                    if(post != null) {
                                        if((server.listFollowing(username)).contains(author)) {
                                            int result = this.server.createPost(this.username, "Rewin (from: " + author + "): " + post.getTitle(), post.getContent());
                                            if (result == 0) {
                                                message = "Post shared successfully";
                                                send(message);
                                            } else if (result == 1) {
                                                message = "Empty title or content.";
                                                send(message);
                                            } else {
                                                message = "Something went wrong. Try again.";
                                                send(message);
                                            }
                                        }
                                        else {
                                            message = "You can't share a post created by a user that you don't follow.";
                                            send(message);
                                        }
                                    }
                                    else {
                                        message = "Post doesn't exist.";
                                        send(message);
                                    }
                                }
                                else {
                                    message = "Wrong number of token. Try: \"rewin IDPOST\"";
                                    send(message);
                                }
                                break;
                            case "comment":
                                if(numTokens >= 3) {
                                    int idPost = Integer.parseInt(tokenizer.nextToken());
                                    String comment = "";
                                    boolean err = false;
                                    comment = message;
                                    if(comment.indexOf("\"") != -1)
                                        comment = comment.substring(comment.indexOf("\"") + 1);
                                    else
                                        err = true;
                                    if(comment.indexOf("\"") != -1) {
                                        comment = comment.substring(0, comment.indexOf("\""));
                                    }
                                    else
                                        err = true;
                                    if(err) {
                                        message = "Enter the comment in quotation marks.";
                                        send(message);
                                    }
                                    else {
                                        int res = this.server.addComment(idPost, this.username, comment);
                                        if (res == 0) {
                                            message = "Comment created successfully.";
                                            send(message);
                                        } else if (res == 1) {
                                            message = "You can't comment a post created by a user that you don't follow.";
                                            send(message);
                                        } else if(res == 2){
                                            message = "Post doesn't exist.";
                                            send(message);
                                        }
                                        else {
                                            message = "Something went wrong. Try again.";
                                            send(message);
                                        }
                                    }
                                }
                                else {
                                    message = "Wrong number of token. Try: \"comment IDPOST \"COMMENT\"\".";
                                    send(message);
                                }
                                break;
                            case "wallet":
                                if(numTokens == 1) {
                                    Wallet wallet = this.server.getWallet(this.username);
                                    message = String.format("Balance: %.2f WINCOIN\n", wallet.getBalance()) + "\nTransactions: \n";
                                    for (Map.Entry currentPost : wallet.getTransactions().entrySet()) {
                                        int key = (Integer) currentPost.getKey();
                                        message += "Timestamp: " + wallet.getTransactions().get(key).getTimestamp()
                                                + String.format(" - Increment: + %.2f WINCOIN\n", wallet.getTransactions().get(key).getIncrement());
                                    }
                                    send(message);
                                }
                                else {
                                    message = "Wrong number of token. Try: \"wallet\".";
                                    send(message);
                                }
                                break;
                            case "wallet_btc":
                                if(numTokens == 1) {
                                    Wallet wallet = this.server.getWallet(this.username);
                                    double change = this.server.getChange();
                                    message = String.format("Balance: %.2f BITCOIN\n", wallet.getBalance() * change) + "\nTransactions: \n";
                                    for (Map.Entry currentPost : wallet.getTransactions().entrySet()) {
                                        int key = (Integer) currentPost.getKey();
                                        message += "Timestamp: " + wallet.getTransactions().get(key).getTimestamp()
                                                + String.format(" - Increment: + %.2f BITCOIN\n", wallet.getTransactions().get(key).getIncrement() * change);
                                    }
                                    send(message);
                                }
                                else {
                                    message = "Wrong number of token. Try: \"wallet_btc\".";
                                    send(message);
                                }
                                break;
                            }
                        }
                    }
                } catch(IOException e){
                e.printStackTrace();
            }
        } while(quit != true);
    }

    /**
     * Metodo che invia un messaggio sulla connessione TCP al client.
     * @param message
     */
    public void send(String message) {
        try {
            message = message.replace('\n', '*');
            this.writer.write(message + "\n");
            this.writer.flush();
        } catch (IOException e) {
            System.err.println("Send error!\n");
            e.printStackTrace();
        }
    }

    /**
     * Metodo che riceve un messaggio dalla connessione TCP con il client.
     * @return la String contenente suddetto messaggio
     */
    public String get() {
        String message = null;
        try {
            message = this.reader.readLine();

        } catch (IOException e) {
            System.err.println("Send error!\n");
            e.printStackTrace();
        }
        return message;
    }
}
