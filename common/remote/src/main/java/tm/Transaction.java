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
    private Set<String> m_rms;
    private long m_lastActive;

    public Transaction(int xid) {
        m_xid = xid;
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
     * Get involved Resource managers
     * @return set of resource managers
     */
    public Set<String> getRMs() {
        return m_rms;
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