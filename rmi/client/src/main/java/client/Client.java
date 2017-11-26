package client;

import inter.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.Semaphore;

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

        // Create lock
        Semaphore lock = new Semaphore(0);

        // Create cli instance
        CLI cli = new CLI(lock);

        // Start health check thread
        new Thread(() -> {
            while(true) {
                if(lock.getQueueLength() > 0) {
                    // Connect to middleware server
                    cli.setResourceManager(connect(server, port));

                    // Release lock
                    lock.release();
                }
            }
        }).start();

        try {
            lock.acquire();
        } catch (InterruptedException e) {
            logger.error("Failed to acquire lock");
        }

        // Start the command line interface
        if(!cli.start()) {
            logger.error("Error occurred while interacting with the command line interface. Terminating program ...");
            System.exit(CODE_ERROR);
        }

        // All good!
        System.exit(CODE_SUCCESS);
    }

    /**
     * Connect to Middleware server
     * @param server
     * @param port
     */
    private static ResourceManager connect(String server, int port){
        final int CONNECT_SLEEP = 5000;
        while (true) {
            try  {

                // Connect to registry
                Registry registry = LocateRegistry.getRegistry(server, port);

                // Lookup RM object
                ResourceManager rm = (ResourceManager) registry.lookup(ResourceManager.MID_SERVER_REF);

                // Check if connection was successful
                rm.healthCheck();
                logger.info("Connected successfully to Middleware Server");
                return rm;
            } catch (Exception e) {
                logger.error("Exception while connecting to server. Message: "+
                        (e.getCause() != null ? e.getCause().toString() : e.toString()));
                try {
                    logger.info("Trying again in " + CONNECT_SLEEP + " ms");
                    Thread.sleep(CONNECT_SLEEP);
                } catch (InterruptedException e1) {
                    logger.error("Failed to put thread to sleep. Message: " + e1.getMessage());
                }
            }
        }
    }
}
