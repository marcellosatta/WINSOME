package out.server;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Classe che modella un utente registrato sulla piattaforma WINSOME.
 */
public class User {

    private String username; // username dell'utente
    private String password; // password dell'utente
    private ConcurrentLinkedQueue<String> tags; // collezione di tags registrati dall'utente
    private ConcurrentLinkedQueue<String> followed; // collezione di utenti che seguono l'utente
    private ConcurrentLinkedQueue<String> following; // collezione di utenti seguiti dall'utente
    private ConcurrentHashMap<Integer, Post> blog; // collezione di post creati dall'utente

    @JsonIgnore
    private boolean logged; // booleano che indica se l'utente è online o meno

    @JsonIgnore
    private ReentrantLock userLock; // lock utilizzata per le operazioni di login e logout
    private Wallet wallet; // portafoglio virtuale dell'utente

    /**
     * Costruttore utilizzato dall'objectMapper per creare un ogetto User a partire dal file Json.
     */
    public User() {
        this.logged = false;
        this.userLock = new ReentrantLock();
    }

    /**
     * Costruttore utilizzato per creare l'oggetto User al momento della registrazione utente.
     * @param username
     * @param password
     * @param tags
     */
    public User(String username, String password, ConcurrentLinkedQueue<String> tags) {
        this.username = username;
        this.password = password;
        this.tags = tags;
        this.followed = new ConcurrentLinkedQueue<String>();
        this.following = new ConcurrentLinkedQueue<String>();
        this.logged = false;
        this.userLock = new ReentrantLock();
        this.wallet = new Wallet();
        this.blog = new ConcurrentHashMap<>();
    }

    /**
     * Metodo get username.
     * @return variabile privata username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Metodo get blog.
     * @return variabile privata blog
     */
    public ConcurrentHashMap<Integer, Post> getBlog() {
        return blog;
    }

    /**
     * Metodo set blog.
     * @param blog
     */
    public void setBlog(ConcurrentHashMap<Integer, Post> blog) {
        this.blog = blog;
    }

    /**
     * Metodo set username.
     * @param username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Metodo get password.
     * @return variabile privata password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Metodo set password.
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Metodo get tags.
     * @return variabile privata tags
     */
    public ConcurrentLinkedQueue<String> getTags() {
        return tags;
    }

    /**
     * Metodo set tags.
     * @param tags
     */
    public void setTags(ConcurrentLinkedQueue<String> tags) {
        this.tags = tags;
    }

    /**
     * Metodo get followed.
     * @return variabile privata followed
     */
    public ConcurrentLinkedQueue<String> getFollowed() {
        return followed;
    }

    /**
     * Metodo set followed.
     * @param followed
     */
    public void setFollowed(ConcurrentLinkedQueue<String> followed) {
        this.followed = followed;
    }

    /**
     * Metodo che aggiunge un nuovo utente alla lista di seguaci dell'utente.
     * @param friend
     * @return
     */
    public synchronized boolean addFollowed(String friend) {
        if(this.followed.contains(friend)) {
            return false;
        }
        this.followed.add(friend);
        return true;
    }

    /**
     * Metodo che rimuove un utente presente nella collezione dei seguaci dell'utente.
     * @param friend
     * @return true se la remove va a buon fine, false altrimenti
     */
    public synchronized boolean removeFollowed(String friend) {
        if(this.followed.contains(friend)) {
            this.followed.remove(friend);
            return true;
        }
        return false;
    }

    /**
     * Metodo get following.
     * @return variabile privata following
     */
    public ConcurrentLinkedQueue<String> getFollowing() {
        return following;
    }

    /**
     * Metodo set following.
     * @param following
     */
    public void setFollowing(ConcurrentLinkedQueue<String> following) {
        this.following = following;
    }

    /**
     * Metodo che aggiunge un nuovo utente alla lista di utenti seguiti.
     * @param friend
     * @return true se l'operazione va a buon fine, false altrimenti
     */
    public synchronized boolean addFollowing(String friend) {
        if(this.following.contains(friend)) {
            return false;
        }
        this.following.add(friend);
        return true;
    }

    /**
     * Metodo che rimuove un utente presente nella collezione di utenti seguiti.
     * @param friend
     * @return true se l'operazione va a buon fine, false altrimenti
     */
    public synchronized boolean removeFollowing(String friend) {
        if(this.following.contains(friend)) {
            this.following.remove(friend);
            return true;
        }
        return false;
    }

    /**
     * Metodo che controlla se l'utente è online, ovvero loggato sulla piattaforma, o meno.
     * @return true se l'utente è online, false altrimenti
     */
    public boolean isLogged() {
        if(this.logged == true)
            return true;
        else
            return false;
    }

    /**
     * Metodo che pone a true la variabile privata logged.
     */
    public void login() {
        this.logged = true;
    }

    /**
     * Metodo che pone a false la variabile privata logged.
     */
    public void logOut() {
        this.logged = false;
    }

    /**
     * Metodo di acquisizione userLock.
     */
    public void lock() {
        this.userLock.lock();
    }

    /**
     * Metodo di rilascio userLock.
     */
    public void unlock() {
        this.userLock.unlock();
    }

    /**
     * Metodo get wallet.
     * @return variabile privata wallet
     */
    public Wallet getWallet() {
        return wallet;
    }

    /**
     * Metodo set logged
     * @param logged
     */
    public void setLogged(boolean logged) {
        this.logged = logged;
    }

    /**
     * Metodo set wallet
     * @param wallet
     */
    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }
}
