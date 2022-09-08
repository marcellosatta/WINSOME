package out.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classe che modella un post la cui richiesta di creazione viene effettuata da un utente.
 */
public class Post {

    private int id; // id univoco assegnato al post
    private int nIterations; // numero di iterazioni di calcolo delle ricompense a cui il post è stato sottoposto
    private String title; // titolo del post
    private String content; // contenuto del post
    private int positiveVotes; // numero totale di voti positivi ricevuti dal post
    private int lastPositiveVotes; // numero totale di voti postivi ricevuti dal post fino all'ultimo calcolo ricompense
    private ConcurrentHashMap<String, Integer> votes; // collezione di tipo hashmap contenente come chiave
                                                      // la stringa che identifica l'username dell'utente
                                                      // e come valore il voto assegnato dal medesimo utente (1 o -1)
    private int negativeVotes; // numero totale di voti negativi ricevuti dal post
    private int lastNegativeVotes; // numero totale di voti negativi ricevuti dal post fino all'ultimo
                                   // calcolo ricompense
    private ConcurrentHashMap<Integer, Comment> comments; // collezione che contiene i commenti relativi al post
    private int nComment; // numero di commenti relativi al post

    /**
     * Costruttore necessario per costruire gli oggetti di tipo Post tramite objectMapper.
     */
    public Post() {}

    /**
     * Costruttore utilizzato in fase di aggiunta di un nuovo post.
     * @param id
     * @param author
     * @param title
     * @param content
     */
    public Post(int id, String author, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.positiveVotes = 0;
        this.lastPositiveVotes = 0;
        this.lastNegativeVotes = 0;
        this.negativeVotes = 0;
        this.votes = new ConcurrentHashMap<>();
        this.comments = new ConcurrentHashMap<>();
        this.nIterations = 0;
        this.nComment = 0;
    }

    /**
     * Metodo get id.
     * @return variabile privata id
     */
    public int getId() {
        return id;
    }

    /**
     * Metodo set id.
     * @param id
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Metodo get title.
     * @return variabile privata title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Metodo set title.
     * @param title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Metodo get content.
     * @return variabile privata content
     */
    public String getContent() {
        return content;
    }

    /**
     * Metodo set content.
     * @param content
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Metodo get positiveVotes.
     * @return variabile private positiveVotes
     */
    public int getPositiveVotes() {
        return this.positiveVotes;
    }

    /**
     * Metodo get lastPositiveVotes.
     * @return variabile privata lastPositiveVotes
     */
    public int getLastPositiveVotes() {
        return lastPositiveVotes;
    }

    /**
     * Metodo set lastPositiveVotes.
     * @param lastPositiveVotes
     */
    public void setLastPositiveVotes(int lastPositiveVotes) {
        this.lastPositiveVotes = lastPositiveVotes;
    }

    /**
     * Metodo set positiveVotes.
     * @param positiveVotes
     */
    public void setPositiveVotes(int positiveVotes) {
        this.positiveVotes = positiveVotes;
    }

    /**
     * Metodo di aggiunta voto.
     * @param username
     * @param vote
     * @return valore intero che rappresenta un codice trattato dal chiamante
     */
    public int addVote(String username, int vote) {
        if(this.votes.putIfAbsent(username, vote) != null) {
            return 1;
        }
        if(vote == 1)
            this.positiveVotes++;
        else if (vote == -1)
            this.negativeVotes++;
        else
            return 2;

        if(checkConsistencyVote())
            return 0;
        return 2;
    }

    /**
     * Metodo get negativeVotes.
     * @return variabile privata negativeVotes
     */
    public int getNegativeVotes() {
        return negativeVotes;
    }

    /**
     * Metodo get lastNegativeVotes.
     * @return variabile privata lastNegativeVotes
     */
    public int getLastNegativeVotes() {
        return lastNegativeVotes;
    }

    /**
     * Metodo set lastNegativeVotes.
     * @param lastNegativeVotes
     */
    public void setLastNegativeVotes(int lastNegativeVotes) {
        this.lastNegativeVotes = lastNegativeVotes;
    }

    /**
     * Metodo che controlla la consistenza della dimensione della collezione di voti rispetto alle quantità di voti
     * rappresentate come numero interi.
     * @return true se la consistenza è rispettata, false altrimenti.
     */
    public synchronized boolean checkConsistencyVote() {
        int posVotes = 0;
        int negVotes = 0;
        for (Map.Entry mapElement : this.votes.entrySet()) {
            String key = (String) mapElement.getKey();
            if(this.votes.get(key) == 1)
                posVotes++;
            else if(this.votes.get(key) == -1)
                negVotes++;
            else
                return false;
        }
        if(posVotes + negVotes == this.votes.size())
            return true;
        return false;
    }

    /**
     * Metodo set negativeVotes.
     * @param negativeVotes
     */
    public void setNegativeVotes(int negativeVotes) {
        this.negativeVotes = negativeVotes;
    }

    /**
     * Metodo get comments.
     * @return variabile privata comments
     */
    public ConcurrentHashMap<Integer, Comment> getComments() {
        return comments;
    }

    /**
     * Metodo che aggiunge un commento alla collezione
     * @param comment
     * @return true se l'aggiunta del commento avviene con successo, false altrimenti
     */
    public boolean addComment(Comment comment) {
        if(this.comments.putIfAbsent(nComment++, comment) != null)
            return false;
        return true;
    }

    /**
     * Metodo get votes.
     * @return variabile privata votes
     */
    public ConcurrentHashMap<String, Integer> getVotes() {
        return votes;
    }

    /**
     * Metodo get nIterations.
     * @return variabile privata nIterations
     */
    public int getnIterations() {
        return nIterations;
    }

    /**
     * Metodo set nIterations.
     * @param nIterations
     */
    public void setnIterations(int nIterations) {
        this.nIterations = nIterations;
    }

    /**
     * Metodo set comments.
     * @param comments
     */
    public void setComments(ConcurrentHashMap<Integer, Comment> comments) {
        this.comments = comments;
    }

    /**
     * Metodo set votes.
     * @param votes
     */
    public void setVotes(ConcurrentHashMap<String, Integer> votes) {
        this.votes = votes;
    }

    /**
     * Metoso get comment.
     * @param idComment
     * @return il commento di chiave idComment
     */
    public Comment getComment(int idComment) {
        return getComments().get(idComment);
    }
}
