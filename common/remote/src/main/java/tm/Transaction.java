package tm;

import inter.ResourceManager;
import lm.TrxnObj;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Transaction {
    private int m_xid;
    private List<TrxnObj> m_transactions;
    private Set<ResourceManager> m_rms;

    public Transaction(int xid) {
        m_xid = xid;
        m_transactions = new ArrayList<>();
        m_rms = new HashSet<>();
    }

    /**
     * Get transactions
     * @return list of transaction objects
     */
    public List<TrxnObj> getTransactionsObjects() {
        return m_transactions;
    }

    /**
     * Get involved Resource managers
     * @return set of resource managers
     */
    public Set<ResourceManager> getRMs() {
        return m_rms;
    }

    /**
     * Add transaction
     * @param transaction
     */
    public void addTransaction(TrxnObj transaction) {
        m_transactions.add(transaction);
    }

    /**
     * Add an involved RM
     * @param rm
     */
    public void addRM(ResourceManager rm) {
        m_rms.add(rm);
    }

    /**
     * Get transaction unique id
     * @return transaction id
     */
    public int getXID() {
        return m_xid;
    }
}