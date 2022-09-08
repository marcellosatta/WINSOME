package out.server;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.sql.Timestamp;

/**
 * Classe che modella le transazioni create tramite il calcolo delle ricompense.
 */
public class Transaction {

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private Timestamp timestamp; // timestamp che descrive il momento in cui la transazione viene creata e aggiunta
                                 // al portafoglio
    private double increment; // ammontare della transazione in WINCOIN

    /**
     * Costruttore necessario per costruire gli oggetti di tipo Transaction tramite objectMapper.
     */
    public Transaction() { }

    /**
     * Costruttore utilizzato per creare una nuovo oggetto Transaction durante il calcolo delle ricompense.
     * @param timestamp
     * @param increment
     */
    public Transaction(Timestamp timestamp, double increment) {
        this.timestamp = timestamp;
        this.increment = increment;
    }

    /**
     * Metodo get timestamp.
     * @return variabile privata timestamp
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

    /**
     * Metodo get increment.
     * @return variabile privata increment
     */
    public double getIncrement() {
        return increment;
    }

    /**
     * Metodo set increment.
     * @param increment
     */
    public void setIncrement(double increment) {
        this.increment = increment;
    }
}
