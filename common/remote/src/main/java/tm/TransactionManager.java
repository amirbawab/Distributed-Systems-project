package tm;

import inter.ResourceManagerActions;
import lm.TransactionAbortedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.transaction.InvalidTransactionException;
import java.io.*;
import java.rmi.RemoteException;
import java.util.*;

public class TransactionManager implements ResourceManagerActions, Serializable {

    // Logger
    private static final Logger logger = LogManager.getLogger(TransactionManager.class);

    // Keep track of transactions
    private Map<Integer, Transaction> m_transactionMap;

    // Unique transaction id
    private int m_uniqId = 1;

    /**
     * Construct a transaction manager
     */
    public TransactionManager() {
        m_transactionMap = new HashMap<>();
    }

    /**
     * Get transactions
     * @return transactions
     */
    public List<Transaction> getTransactions() {
        return new ArrayList<>(m_transactionMap.values());
    }

    /**
     * Get transaction by id
     * @param id
     * @return transactino
     * @exception InvalidTransactionException
     */
    public Transaction getTransaction(int id) throws InvalidTransactionException {
        if(!m_transactionMap.containsKey(id)) {
            throw new InvalidTransactionException("Transaction id " + id + " is not available");
        }
        return m_transactionMap.get(id);
    }

    public void removeTransaction(int id) throws InvalidTransactionException {
        if(!m_transactionMap.containsKey(id)) {
            throw new InvalidTransactionException("Transaction id " + id + " is not available");
        }
        m_transactionMap.remove(id);
        writeTM();
    }

    /**
     * Start transaction
     * @return
     * @throws RemoteException
     */
    @Override
    public int start() throws RemoteException {
       Transaction transaction = new Transaction(m_uniqId++);
       m_transactionMap.put(transaction.getXID(), transaction);
       writeTM();
       return transaction.getXID();
    }

    /**
     * Commit transaction
     * @param transactionId
     * @return true if transaction commited successfully
     * @throws RemoteException
     * @throws TransactionAbortedException
     * @throws InvalidTransactionException
     */
    @Override
    public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        return false;
    }

    /**
     * Abort transaction
     * @param transactionId
     * @throws RemoteException
     * @throws InvalidTransactionException
     */
    @Override
    public void abort(int transactionId) throws RemoteException, InvalidTransactionException {

    }

    /**
     * Shutdown transaction
     * @return true if shutdown successful
     * @throws RemoteException
     */
    @Override
    public boolean shutdown() throws RemoteException {
        return false;
    }

    /**
     * Nothing to do in TM
     * @param tid
     * @return
     * @throws RemoteException
     */
    @Override
    public boolean voteRequest(int tid) throws RemoteException {
        return false;
    }

    /**
     * Create a copy of the transaction key set
     * @return key set
     */
    public Set<Integer> getTransactionsId() {
        return new HashSet<>(m_transactionMap.keySet());
    }

    /**
     * Get the TM file
     * @return TM file
     */
    public File getTMFile() {
        return new File("TM_table");
    }

    /**
     * Write transaction manager to file
     */
    private synchronized void writeTM() {
        File tmFile = getTMFile();
        try(FileOutputStream fos = new FileOutputStream(tmFile); ObjectOutputStream obj = new ObjectOutputStream(fos)) {
            obj.writeObject(this);
            logger.info("File " + tmFile.getAbsolutePath() + " updated!");
        } catch (IOException e) {
            logger.error("Error writing file " + tmFile.getAbsolutePath() + ". Message: " + e.getMessage());
        }
    }

    /**
     * Update last activity for this transcation
     * @param id
     * @throws InvalidTransactionException
     */
    public void updateLastActive(int id) throws InvalidTransactionException {
        getTransaction(id).updateLastActive();
    }

    /**
     * Add RM to transaction
     * @param id
     * @param rm
     * @throws InvalidTransactionException
     */
    public void addRM(int id, String rm) throws InvalidTransactionException {
        getTransaction(id).addRM(rm);
    }
}