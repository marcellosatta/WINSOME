package out.server;

import out.common.ClientCallbackNotify;
import out.common.RMIInterface;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import static java.lang.Math.log;
import static java.lang.StrictMath.exp;

/**
 * Classe che modella il Server. Al suo interno sono racchiuse tutte le strutture dati necessarie
 * al corretto funzionamento e tutti i metodi di modifica di queste strutture.
 */
public class Server extends RemoteServer implements RMIInterface {

    private File backupDir; // directory che contiene il file di backup
    private File backupFile; // file di backup
    private final String BACKUP_PATH = "./backup/"; // percorso della cartella che contiene il file di backup
    private final String USERS_BACKUP = "users_backup.json"; // nome del file di backup
    private ConcurrentHashMap<String, User> users; // collezione di utenti registrati
    private ConcurrentHashMap<String, ClientCallbackNotify> registeredForCallback; // collezione degli utenti registrati
                                                                                   // al servizio di callback
    private double change; // variabile contenente il cambio valuta in Bitcoin
    private ObjectMapper objectMapper; // Oggetto utilizzato per serializzare e deserializzare il file json
    private int authorRewardPercentage; // percentuale di ricompensa per l'autore del singolo post


    /**
     * Costruttore dell'oggetto Server
     * @param authorRewardPercentage
     */
    public Server(int authorRewardPercentage) {
        this.users = new ConcurrentHashMap<String, User>();
        this.registeredForCallback = new ConcurrentHashMap<>();
        this.authorRewardPercentage = authorRewardPercentage;

        // operazione di caricamento dati dal file Json
        loadBackup();
    }

    /**
     * Metodo di ricerca chiave per l'inserimento post
     * @return numero intero che modella l'id del post da inserire
     */
    public synchronized int findKey() {
        int i;
        ConcurrentHashMap<Integer, Post> feedCopy = new ConcurrentHashMap<>();
        for (Map.Entry userEntry : this.users.entrySet()) {
            String key = (String) userEntry.getKey();
            feedCopy.putAll(getUserBlog(key));
        }

        for (i = 0; i < feedCopy.size(); i++) {
            if(!feedCopy.containsKey(i))
                break;
        }
        if(i != feedCopy.size())
            return i;
        else
            return feedCopy.size();
    }

    /**
     * Metodo di regitrazione, implementato a partire dall'interfaccia RMIInterface, che è lo scheletro dell'oggetto
     * remoto.
     * @param username
     * @param password
     * @param tags
     * @return numero intero che rappresenta un codice valutato dal chiamante
     */
    @Override
    public int register(String username, String password, ConcurrentLinkedQueue<String> tags) {

        if (username == null || password == null) {
            return 1; // username o password null
        } else if (username == "" || password == "")
            return 2; // username o password vuote
        else {
            if (tags.size() < 1 || tags.size() > 5)
                return 3; // numero di tags errato
            else {
                for (String tag: tags) {
                    if (tag == null)
                        return 4; // tag nullo
                    else if (tag == "")
                        return 5; // tag vuoto
                }
            }
        }

        User newUser = new User(username, password, tags);

        // inserimento nella collezione
        if (this.users.putIfAbsent(username, newUser) != null)
            return 6; // l'utente è già registrato

        backup();

        return 0; // registrazione andata a buon fine
    }

    /**
     * Metodo di backup sil file Json.
     */
    public synchronized void backup() {
        try {
            objectMapper.writeValue(backupFile, users.values());
        }
        catch (IOException e) {
            System.out.println("Error updating backup file.");
        }
    }

    /**
     * Metodo di registrazione al servizio di callback.
     * @param username
     * @param stub
     * @throws RemoteException
     */
    @Override
    public void registeraForCallback(String username, ClientCallbackNotify stub) throws RemoteException {
        this.registeredForCallback.putIfAbsent(username, stub);
    }

    /**
     * Metodo che cancella la registrazione al servizio di callback.
     * @param username
     * @param stub
     * @throws RemoteException
     */
    @Override
    public void unregisteraForCallback(String username, ClientCallbackNotify stub) throws RemoteException {
        this.registeredForCallback.remove(username);
    }

    /**
     * Metodo con cui un utente può ottenere la lista di followers da mantenere localmente.
     * @param username
     * @return lista dei followers dell'utente dell'utente avente chiave username
     * @throws RemoteException
     */
    @Override
    public ConcurrentLinkedQueue<String> callBackFriends(String username) throws RemoteException {
        return this.users.get(username).getFollowed();
    }
    /**
     * Metodo get user a partire dalla chiave username.
     * @param username
     * @return oggetto User a partire dalla chiave username
     */
    public User getUser(String username) {
        return this.users.get(username);
    }

    /**
     * Metodo get blog di un particolare user identificato dalla chiave username.
     * @param username
     * @return il blog dell'utente avente chiave username
     */
    public ConcurrentHashMap<Integer, Post> getUserBlog(String username) {
        return this.users.get(username).getBlog();
    }

    /**
     * Metodo get post a partire dall'id di quest'ultimo e dall'username dell'autore
     * @param idPost
     * @param username
     * @return il post avente chiave id creato dall'utente username
     */
    public Post getUserPost(int idPost, String username) {
        return this.users.get(username).getBlog().get(idPost);
    }

    /**
     * Metodo di login utente
     * @param username
     * @param password
     * @return numero intero che rappresenta un codice verificato dal chiamante
     * @throws Exception
     */
    public int login(String username, String password) throws Exception {

        if (!this.users.containsKey(username)) {
            return 1; // utente non registrato
        }

        User user = getUser(username);
        user.lock();
        if (user.isLogged()) {
            user.unlock();
            return 2; // utente già loggato
        }

        if (!password.equals(user.getPassword())) {
            user.unlock();
            return 3; // password errata
        }

        user.login();
        user.unlock();
        return 0; // operazione andata a buon fine
    }

    /**
     * Metodo di logout utente
     * @param username
     * @return true se l'operazione va a buon fine, false altrimenti
     */
    public boolean logout(String username) {
        User user = getUser(username);

        user.lock();
        if (user.isLogged()) {
            user.logOut();
            user.unlock();

            backup();

            return true;
        } else {
            user.unlock();
            return false;
        }
    }

    /**
     * Metodo di ricerca utenti aventi almeno un tag in comune con l'utente che ha richiesto l'operazione
     * @param username
     * @return ArrayList di String contenti i nome degli utenti che hanno almeno un tag in comune con l'utente che ha
     * fatto richiesta dell'operazione
     */
    public synchronized ArrayList<String> findUserList(String username) {
        ArrayList<String> userList = new ArrayList<>();
        User user = getUser(username);

        ConcurrentLinkedQueue<String> userTags = user.getTags();
        for (Map.Entry userEntry : this.users.entrySet()) {
            String otherUsername = (String) userEntry.getKey();
            User otherUser = getUser(otherUsername);
            if(!otherUsername.equals(username)) {
                ConcurrentLinkedQueue<String> otherUserTags = otherUser.getTags();
                for (String tag: userTags) {
                   if (otherUserTags.contains(tag)) {
                     if (!userList.contains(otherUsername))
                            userList.add(otherUsername);
                   }
                }
            }
        }
        return userList;
    }

    /**
     * Metodo di richiesta lista utenti seguiti da chi ha richiesto l'operazione.
     * @param username
     * @return lista di utenti seguiti dall'utente che ha richiesto l'operazione
     */
    public ConcurrentLinkedQueue<String> listFollowing(String username) {
        User user = getUser(username);
        ConcurrentLinkedQueue<String> following = user.getFollowing();
        return following;


    }

    /**
     * Metodo che afferma o nega la presenza dell'oggetto User avente chiave username all'interno della collezione users
     * @param username
     * @return true se l'utente è presente, false altrimenti
     */
    public synchronized boolean isPresent(String username) {
        return this.users.containsKey(username);
    }

    /**
     * Metodo con cui un utente può seguirne un altro.
     * @param username
     * @param usernameToFollow
     * @return true se l'operazione va a buon fine, false altrimenti
     * @throws RemoteException
     */
    public boolean follow(String username, String usernameToFollow) throws RemoteException {
        User user = getUser(username);
        User userToFollow = getUser(usernameToFollow);
        if (userToFollow.addFollowed(username) && user.addFollowing(usernameToFollow)) {
            if(userToFollow.isLogged()) {
                registeredForCallback.get(usernameToFollow).notifyNewFriend(username);
            }
            backup();

            return true;
        } else {
            return false;
        }
    }

    /**
     * Metodo con cui un utente può smettere di seguirne un altro.
     * @param username
     * @param usernameToUnfollow
     * @return true se l'operazione va a buon fine, false altrimenti
     * @throws RemoteException
     */
    public boolean unfollow(String username, String usernameToUnfollow) throws RemoteException {
        User user = getUser(username);
        User userToUnfollow = getUser(usernameToUnfollow);
        if (userToUnfollow.removeFollowed(username) && user.removeFollowing(usernameToUnfollow)) {
            if(userToUnfollow.isLogged()) {
                registeredForCallback.get(usernameToUnfollow).notifyOldFriend(username);
            }
            backup();

            return true;
        } else {
            return false;
        }
    }

    /**
     * Metodo con cui un utente richiede i post presenti all'interno del suo feed.
     * @param username
     * @return ArrayList di String contenenti la descrizione dei post del feed
     */
    public synchronized ArrayList<String> getFeed(String username) {
        ArrayList<String> feed = new ArrayList<>();
        User user = getUser(username);
        for (String usernameFollowing: user.getFollowing()) {
            ConcurrentHashMap<Integer, Post> blog = getUserBlog(usernameFollowing);
            for (Map.Entry postEntry : blog.entrySet()) {
                Integer idPost = (Integer) postEntry.getKey();
                Post post = getUserPost(idPost, usernameFollowing);
                feed.add("ID: " + post.getId() + "\nAuthor: " + usernameFollowing + "\nTitle: "
                        + post.getTitle() + "\n");
            }
        }
        return feed;
    }

    /**
     * Metodo con cui un utente richiede i post presenti all'interno del suo blog.
     * @param username
     * @return ArrayList di String contenenti la descrizione dei post del blog
     */
    public ArrayList<String> getBlog(String username) {
        ConcurrentHashMap<Integer, Post> blog = getUserBlog(username);
        ArrayList<String> outputBlog = new ArrayList<>();

        for (Map.Entry postEntry : blog.entrySet()) {
                Integer idPost = (Integer) postEntry.getKey();
                Post post = blog.get(idPost);
                outputBlog.add("ID: " + post.getId() + "\nAuthor: " + username + "\nTitle: " + post.getTitle() + "\n");
        }
        return outputBlog;
    }

    /**
     * Metodo con cui un utente può ottenere l'oggetto Post specificato dalla chiave idPost.
     * @param idPost
     * @return oggetto Post avente chiave idPost
     */
    public Post getPost(int idPost) {
        String author = getAuthorFromPostId(idPost);
        Post orDefault = this.users.get(author).getBlog().getOrDefault(idPost, null);
        return orDefault;
    }

    /**
     * Metodo di creazione nuovo post.
     * @param username
     * @param title
     * @param content
     * @return numero intero che rappresenta un codice verificato dal chiamante
     */
    public int createPost(String username, String title, String content) {

        if (title.equals("") || content.equals(""))
            return 1; // titolo o contenuto vuoto

        int idPost = findKey();

        Post post = new Post(idPost, username, title, content);

        ConcurrentHashMap<Integer, Post> blog = getUserBlog(username);

        if(blog.putIfAbsent(idPost, post) != null) {
            return 2; // impossibile mappare la chiave all'oggetto Post
        }
        backup();
        return 0; // operazione andata a buon fine
    }

    /**
     * Metodo di cancellazione post.
     * @param idPost
     * @param username
     * @return true se l'operazione va a buon fine, false altrimenti
     * @throws IOException
     */
    public boolean deletePost(int idPost, String username) throws IOException {
        ConcurrentHashMap<Integer, Post> blog = getUserBlog(username);

        Post post = blog.remove(idPost);

        if(post != null) {
            backup();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Metodo che restituisce l'autore di un post a partire dalla chiave idPost.
     * @param idPost
     * @return String contenente l'username dell'autore del post, null se il post non esiste
     */
    public synchronized String getAuthorFromPostId(int idPost) {
        for (Map.Entry userEntry : this.users.entrySet()) {
            String username = (String) userEntry.getKey();

            ConcurrentHashMap<Integer, Post> blog = getUserBlog(username);

            if(blog.containsKey(idPost)) {
                return username;
            }
        }
        return null;
    }

    /**
     * Metodo di valutazione di un post.
     * @param idPost
     * @param rating
     * @param username
     * @return numero intero rappresentate un codice verificato dal chiamante
     */
    public synchronized int ratePost(int idPost, int rating, String username) {
        int ret;
        User user = getUser(username);
        String author = getAuthorFromPostId(idPost);
        ConcurrentHashMap<Integer, Post> blog = getUserBlog(author);

        if (blog.containsKey(idPost)) {
            if (user.getFollowing().contains(author) || author.equals(username)) {
                if ((ret = blog.get(idPost).addVote(username, rating)) == 0) {
                    backup();
                    return 0; // operazione andata a buon fine
                }
                else if (ret == 1) {
                    return 1; // l'utente ha già votato il post
                }
                else {
                    return 2; // voto incorretto o problema di consistenza
                }
            } else {
                return 3; // l'utente non può votare un post creato da un utente che non segue
            }
        } else {
            return 4; // il post non esiste
        }
    }

    /**
     * Metodo di aggiunta commento.
     * @param idPost
     * @param username
     * @param comment
     * @return numero intero rappresentate un codice verificato dal chiamante
     */
    public synchronized int addComment(int idPost, String username, String comment) {
        User user = getUser(username);
        String author;
        if ((author = getAuthorFromPostId(idPost)) != null) {
            Post post = getUserPost(idPost, author);
            if (user.getFollowing().contains(author) || username.equals(author)) {
                Comment c = new Comment(username, comment, new Timestamp(System.currentTimeMillis()));
                if(post.addComment(c)) {
                    backup();
                    return 0; // operazione andata a buon fine
                }
                else return 3; // errore, chiave che mappa il commento già presente
            } else {
                return 1; // l'utente non può commentare un post creato da un utente che non segue
            }
        } else {
            return 2; // il post non esiste
        }
    }

    /**
     * Metodo get wallet a partire dalla chiave username.
     * @param username
     * @return oggetto Wallet appartenente all'utente mappato su chiave username
     */
    public Wallet getWallet(String username) {
        return this.users.get(username).getWallet();
    }


    /**
     * Metodo di calcolo ricompense.
     * @param lastTimestamp
     * @return il timestamp in cui il calcolo ricompense è iniziato
     */
    public synchronized Timestamp updateRewards(Timestamp lastTimestamp) {
        Timestamp startUpdateTS = new Timestamp(System.currentTimeMillis());

        // copio tutti i post di tutti gli utenti su un'unica collezione
        HashMap<Integer, Post> feedCopy = new HashMap<>();
        for (Map.Entry userEntry : this.users.entrySet()) {
            String username = (String) userEntry.getKey();
            feedCopy.putAll(getUserBlog(username));
        }

        // scooro post per post e calcolo le ricompense di ognuno
        for (Map.Entry postEntry : feedCopy.entrySet()) {
            Integer idPost = (Integer) postEntry.getKey();

            User user = this.users.get(getAuthorFromPostId(idPost));
            Post post = getPost(idPost);

            // calcolo prima parte della formula, quella relativa ai likes
            int newPositiveVotes = post.getPositiveVotes() - post.getLastPositiveVotes();
            int newNegativeVotes = post.getNegativeVotes() - post.getLastNegativeVotes();

            int lp = (newPositiveVotes) - (newNegativeVotes);

            if (post.getVotes().get(user.getUsername()) != null)
                if (post.getVotes().get(user.getUsername()) == 1)
                    lp--;
                else
                    lp++;

            if (lp > 0)
                lp++;
            else
                lp = 1;

            double firstPart = log(lp);

            // calcolo seconda parte della formula, relativa ai commenti
            ArrayList<String> newUsersCommenting = new ArrayList<>();
            ArrayList<Integer> newComments = new ArrayList<>();

            for (Map.Entry commentEntry : post.getComments().entrySet()) {
                int idComment = (Integer) commentEntry.getKey();
                Comment comment = post.getComment(idComment);
                String commentAuthor = comment.getAuthor();
                try {
                    if (comment.getTimestamp().getTime() > lastTimestamp.getTime() && commentAuthor != user.getUsername()) {
                        newUsersCommenting.add(commentAuthor);
                    }
                } catch (Exception e) {
                    System.err.println(e);
                }
            }


             for (Map.Entry userEntry : this.users.entrySet()) {
                String username = (String) userEntry.getKey();

                if (newUsersCommenting.contains(username)) {
                    newComments.add(Collections.frequency(newUsersCommenting, username));
                }
            }
            double res = 0;
            for (int i = 0; i < newComments.size(); i++) {
                double div = 1 + exp(-(newComments.get(i) - 1));
                double fraction = 2 / div;
                res += fraction;
            }

            res += 1;

            res = log(res);
            post.setnIterations(post.getnIterations() + 1);
            double result = (firstPart + res) / post.getnIterations();

            getPost(idPost).setLastPositiveVotes(post.getPositiveVotes());
            getPost(idPost).setLastNegativeVotes(post.getNegativeVotes());

            // aggiornamento wallet e transazioni
            if(result != 0) {
                user.getWallet().addTransaction(startUpdateTS, result * authorRewardPercentage / 100.0);
                user.getWallet().updateBalance(result * authorRewardPercentage / 100.0);

                ArrayList<String> curators = new ArrayList<>();
                for (int i = 0; i < newUsersCommenting.size(); i++) {
                    if (!curators.contains(newUsersCommenting.get(i)))
                        curators.add(newUsersCommenting.get(i));
                }

                for (int i = 0; i < curators.size(); i++) {
                    User curator = getUser(curators.get(i));
                    if(post.getVotes().containsKey(curator.getUsername()) && post.getVotes().get(curator.getUsername()) == -1)
                        curators.remove(i);
                }

                int totCurators = newPositiveVotes + curators.size();

                double resCurator = (result * (100.0 - authorRewardPercentage) / 100.0) / totCurators;

                for (Map.Entry userEntry : post.getVotes().entrySet()) {
                    String username = (String) userEntry.getKey();
                    User curator = getUser(username);
                    if (post.getVotes().get(username) == 1) {
                        curator.getWallet().addTransaction(startUpdateTS, resCurator);
                        curator.getWallet().updateBalance(resCurator);
                    }
                }

                for (int i = 0; i < curators.size(); i++) {
                    User curator = getUser(curators.get(i));

                    curator.getWallet().addTransaction(startUpdateTS, resCurator);
                    curator.getWallet().updateBalance(resCurator);
                }
            }
        }

        backup();

        return startUpdateTS;
    }

    /**
     * Metodo set change.
     * @param change
     */
    public void setChange(double change) {
        this.change = change;
    }

    /**
     * Metodo get change.
     * @return
     */
    public double getChange() {
        return change;
    }

    /**
     * Metdo di caricamento dati dal file Json.
     */
    private void loadBackup() {
        backupDir = new File(BACKUP_PATH);
        backupFile = new File(BACKUP_PATH + USERS_BACKUP);
        objectMapper = new ObjectMapper();

        // Se la directory di backup non esiste, la creo
        if (!backupDir.exists()) {
            backupDir.mkdir();
        } else {
            // Se il file di backup non esiste lo creo
            if (!backupFile.exists()) {
                try {
                    backupFile.createNewFile();
                    objectMapper.writeValue(backupFile, users.values());
                } catch (IOException e) {
                    System.err.println("Error creating users backup files");
                    System.exit(-1);
                }
            } else {
                // Provo a caricare i dati contenuti nel file di backup json
                try {
                    User[] temp = objectMapper.readValue(backupFile, User[].class);

                    boolean valid = true; // Flag che indica che la lettura sia avvenuta correttamente

                    for (User user : temp) {
                        // Se nel file sono presenti due username uguali, il file non e' valido e segnalo l'errore
                        if (users.putIfAbsent(user.getUsername(), user) != null) {
                            System.err.println("Users bakup files is not valid. It contains duplicate nicknames");
                            valid = false;
                            break;
                        }
                    }

                    if (valid) {
                        System.out.println("Backup done successfully.");
                    }
                } catch (IOException e) {
                    System.err.println("Error loading backup.");
                    System.err.println(e.getMessage());

                    // Creo una HashMap vuota
                    users = new ConcurrentHashMap<String, User>();
                }
            }

            for (Map.Entry currentUser : this.users.entrySet()) {
                String key = (String) currentUser.getKey();

                this.users.get(key).setLogged(false);
            }
        }
    }
}

