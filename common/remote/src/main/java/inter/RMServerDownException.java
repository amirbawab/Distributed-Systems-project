package inter;

import java.rmi.RemoteException;

public class RMServerDownException extends RemoteException {
    public RMServerDownException() {
        super("Error 500 Internal server error");
    }
    public RMServerDownException(String msg) {
        super(msg);
    }
}
