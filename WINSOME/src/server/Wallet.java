package out.server;

import java.sql.Timestamp;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classe che modella il portafoglio virtuale di un utente, contiene al suo interno una collezione di oggetti
 * Transaction che rappresentano le transazioni create dal calcolo ricompense, il numero di traansazioni
 * ricevute e il bilancio totale.
 */
public class Wallet {

    private float balance; // bilancio totale del portafoglio
    private int nTransaction; // numero di transazioni ricevute
    private ConcurrentHashMap<Integer, Transaction> transactions; // collezione di oggetti Transaction

    /**
     * Costruttore utilizzato sia dall'objectMapper per creare l'oggetto letto dal file Json, sia per creare il
     * portafoglio alla registrazione dell'utente.
     */
    public Wallet() {
        this.balance = 0;
        this.nTransaction = 0;
        this.transactions = new ConcurrentHashMap<>();
    }

    /**
     * Metodo get balance.
     * @return variabile privata balance
     */
    public float getBalance() {
        return balance;
    }

    /**
     * Metodo set balance.
     * @param balance
     */
    public void setBalance(float balance) {
        this.balance = balance;
    }

    /**
     * Metodo di aggiornamento del bilancio, aggiunge alla variabile privata balance l'importo relativo alla
     * transazione ricevuta.
     * @param newGain
     */
    public void updateBalance(double newGain) {
        this.balance += newGain;
    }

    /**
     * Metodo get transactions.
     * @return variabile privata transactions
     */
    public ConcurrentHashMap<Integer, Transaction> getTransactions() {
        return transactions;
    }

    /**
     * Metodo set transactions.
     * @param transactions
     */
    public void setTransactions(ConcurrentHashMap<Integer, Transaction> transactions) {
        this.transactions = transactions;
    }

    /**
     * Metodo che aggiunge un nuovo oggetto Transaction alla collezione.
     * @param timestamp
     * @param increment
     */
    public void addTransaction(Timestamp timestamp, double increment) {
        Transaction transaction = new Transaction(timestamp, increment);
        this.transactions.putIfAbsent(nTransaction++, transaction);
    }
}
