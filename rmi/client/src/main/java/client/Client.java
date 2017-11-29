package client;

import inter.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

    // Logger
    private static final Logger logger = LogManager.getLogger(Client.class);

    // Constants
    private static final int CODE_SUCCESS=0;
    private static final int CODE_ERROR=1;

    public static void main(String args[]) {

        // Verify the number of arguments passed
        if (args.length != 2) {
            System.out.println ("Usage: java Client [middleware server ip] [middleware server port]");
            System.exit(CODE_ERROR);
        }

        // Read arguments
        String server = args[0];
        int port = Integer.parseInt(args[1]);

        // Start the command line interface
        CLI cli = new CLI(server, port);

        // Start CLI
        if(!cli.start()) {
            logger.error("Error occurred while interacting with the command line interface. Terminating program ...");
            System.exit(CODE_ERROR);
        }

        // All good!
        System.exit(CODE_SUCCESS);
    }
}
