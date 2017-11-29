package inter;

import java.rmi.RemoteException;

public class TMException extends RemoteException {
    public TMException() {super("Transaction manager is down");}
}
