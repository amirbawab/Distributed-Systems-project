package inter;

import lm.TransactionAbortedException;

import javax.transaction.InvalidTransactionException;
import java.rmi.RemoteException;

public interface ResourceManagerActions {
    int start() throws RemoteException;
    boolean commit(int transactionId) throws RemoteException, TransactionAbortedException, InvalidTransactionException;
    void abort(int transactionId) throws RemoteException, InvalidTransactionException;
    boolean shutdown() throws RemoteException;
}
