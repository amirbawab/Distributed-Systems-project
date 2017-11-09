package tm;

import inter.ResourceManager;
import lm.TrxnObj;

import java.util.ArrayList;
import java.util.List;

class Transaction {
    private int m_xid;
    private List<TrxnObj> m_transactions;
    private List<ResourceManager> m_rms;

    public Transaction(int xid) {
        m_xid = xid;
        m_transactions = new ArrayList<>();
        m_rms = new ArrayList<>();
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
     * @return list of resource managers
     */
    public List<ResourceManager> getRMs() {
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