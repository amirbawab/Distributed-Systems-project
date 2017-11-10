package tm;

import inter.ResourceManagerActions;
import lm.TransactionAbortedException;

import javax.transaction.InvalidTransactionException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionManager implements ResourceManagerActions {

    // Keep track of transactions
    private Map<Integer, Transaction> m_transactionMap;

    // Unique transaction id
    private static int m_uniqId = 1;

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
}