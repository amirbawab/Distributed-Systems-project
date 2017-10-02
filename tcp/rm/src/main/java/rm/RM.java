// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
//
package rm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class RM {

    // Logger
    private static final Logger logger = LogManager.getLogger(ResourceManagerImpl.class);

    // Constants
    private static final int CODE_ERROR=1;

    public static void main(String args[]) {

        // Figure out where server is running
        if (args.length != 1) {
            System.err.println("Wrong usage");
            System.out.println("Arguments: [port]");
            System.exit(CODE_ERROR);
        }

        // Read arguments
        int port = Integer.parseInt(args[0]);

        // Start listening ...
        boolean acceptClients = true;
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            logger.error("Server failed to listen on port: " + port);
            System.exit(CODE_ERROR);
        }

        // Create resource manager object
        ResourceManagerImpl obj = new ResourceManagerImpl();

        // Start accepting clients
        logger.info("Server on " + serverSocket.getInetAddress().getHostAddress() + ":" + port + " is ready to serve a client ...");
        while (acceptClients) {
            try {

                // Accept a new client
                Socket socket = serverSocket.accept();
                logger.info("Connected to a client!");

                // Create an configure thread
                RMThread thread = new RMThread(socket, obj);
                thread.start();
                logger.info("Server ready to serve a new client ...");
            } catch (IOException e) {
                logger.error("Error occurred, cannot accept clients. Terminating program ...");
                acceptClients = false;
            }
        }
    }
}
