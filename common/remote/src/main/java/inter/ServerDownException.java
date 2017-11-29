package inter;

import java.rmi.RemoteException;

public class ServerDownException extends RemoteException {
    public ServerDownException() {
        super("Error 500 Internal server error");
    }
    public ServerDownException(String msg) {
        super(msg);
    }
}
