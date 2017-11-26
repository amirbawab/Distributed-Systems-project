package inter;

import lm.TransactionAbortedException;

import javax.transaction.InvalidTransactionException;
import java.io.Serializable;
import java.rmi.RemoteException;

public interface ResourceManagerActions extends Serializable {
    int start() throws RemoteException;
    boolean commit(int transactionId) throws RemoteException, TransactionAbortedException, InvalidTransactionException;
    void abort(int transactionId) throws RemoteException, InvalidTransactionException;
    boolean shutdown() throws RemoteException;
    boolean voteRequest(int tid) throws RemoteException;
}
