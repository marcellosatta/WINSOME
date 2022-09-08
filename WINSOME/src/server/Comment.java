package out.server;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.sql.Timestamp;

/**
 * Classe che modella un commento relativo ad un post.
 */
public class Comment {

    private String author; // autore del commento
    private String comment; // contenuto del commento
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss.SSS")
    private Timestamp timestamp; // timestamp che descrive il momento in cui il commento viene creato

    /**
     * Costruttore necessario per costruire gli oggetti di tipo Comment tramite objectMapper.
     */
    public Comment() {}

    /**
     * Costruttore utilizzato per creare un oggetto Comment alla richiesta di aggiunta da parte dell'utente.
     * @param author
     * @param comment
     * @param timestamp
     */
    public Comment(String author, String comment, Timestamp timestamp) {
        this.author = author;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    /**
     * Metodo get author.
     * @return variabile privata author
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Metodo set author.
     * @param author
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * Metodo get comment.
     * @return variabile privata comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * Metodo set comment.
     * @param comment
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Metodo get timestamp.
     * @return variabile privata timestamo
     */
    public Timestamp getTimestamp() {
        return timestamp;
    }

    /**
     * Metodo set timestamp.
     * @param timestamp
     */
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
