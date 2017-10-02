package midserver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

class MiddlewareServer {

    // Constants
    private static final int CODE_ERROR = 1;

    // Logger
    private static final Logger logger = LogManager.getLogger(MiddlewareServer.class);

    /**
     * Start server
     * @param args
     */
    public static void main(String[] args) {

        // Figure out where server is running
        if (args.length != 7) {
            System.err.println("Wrong usage");
            System.out.println("Arguments: [port] [flight port] [flight ip] [car port] [car ip] [room port] [room ip]");
            System.exit(CODE_ERROR);
        }

        // Read arguments
        int serverPort = Integer.parseInt(args[0]);
        int flightPort = Integer.parseInt(args[1]);
        String flightIP = args[2];
        int carPort = Integer.parseInt(args[3]);
        String carIP = args[4];
        int roomPort = Integer.parseInt(args[5]);
        String roomIP = args[6];

        // Start listening ...
        boolean acceptClients = true;
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(serverPort);
        } catch (IOException e) {
            logger.error("Server failed to listen on port: " + serverPort);
            System.exit(CODE_ERROR);
        }

        // Start accepting clients
        while (acceptClients) {
            try {

                // Accept a new client
                Socket socket = serverSocket.accept();
                logger.info("Connected to a client!");

                // Create an configure thread
                MiddlewareServerThread thread = new MiddlewareServerThread(socket);
                try {
                    thread.connectFlightSocket(flightIP, flightPort);
                    thread.connectCarSocket(carIP, carPort);
                    thread.connectRoomSocket(roomIP, roomPort);

                } catch (IOException e) {
                    logger.error("Failed to connect to one or more RM. Terminating program ...");
                    System.exit(CODE_ERROR);
                }
                thread.start();
                logger.info("Server ready to serve a new client ...");
            } catch (IOException e) {
                logger.error("Error occurred, cannot accept clients. Terminating program ...");
                acceptClients = false;
            }
        }

        // Exit with error code when the server stops listening
        System.exit(CODE_ERROR);
    }
}

