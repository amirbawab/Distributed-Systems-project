package tm;

import inter.ResourceManager;
import lm.TrxnObj;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Transaction implements Serializable {
    private int m_xid;
    @Deprecated private List<TrxnObj> m_transactions;
    private Set<String> m_rms;
    private long m_lastActive;

    public Transaction(int xid) {
        m_xid = xid;
        m_transactions = new ArrayList<>();
        m_rms = new HashSet<>();
        updateLastActive();
    }

    /**
     * Update the last active time
     */
    public void updateLastActive() {
        m_lastActive = System.currentTimeMillis();
    }

    /**
     * Get how much time the transaction has been non-active
     * @return time in milliseconds
     */
    public long getIdleTime() {
        return System.currentTimeMillis() - m_lastActive;
    }

    /**
     * Get transactions
     * @return list of transaction objects
     */
    @Deprecated public List<TrxnObj> getTransactionsObjects() {
        return m_transactions;
    }

    /**
     * Get involved Resource managers
     * @return set of resource managers
     */
    public Set<String> getRMs() {
        return m_rms;
    }

    /**
     * Add transaction
     * @param transaction
     */
    @Deprecated public void addTransaction(TrxnObj transaction) {
        m_transactions.add(transaction);
    }

    /**
     * Add an involved RM
     * @param rm
     */
    public void addRM(String rm) {
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