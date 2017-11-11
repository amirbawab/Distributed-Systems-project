package client;

import inter.ResourceManager;
import lm.TransactionAbortedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

public class TwoPLTest {

    // Logger
    private static final Logger logger = LogManager.getLogger(TwoPLTest.class);

    // Declare a static RM
    private static ResourceManager rm = null;

    // Test level
    private final int LOW_TEST = 10;
    private final int MED_TEST = 100;
    private final int HIGH_TEST = 1000;

    // Thread level
    private final int LOW_THREAD = 2;
    private final int MED_THREAD = 4;
    private final int HIGH_THREAD = 6;

    // Objects details
    private final int cid = 1;
    private final String testLocation = "test-location-";

    @BeforeClass
    public static void connectToServer() {

        // Read arguments
        String server = System.getProperty("server_ip");
        int port = Integer.parseInt(System.getProperty("server_port"));

        try  {
            Registry registry = LocateRegistry.getRegistry(server, port);
            rm = (ResourceManager) registry.lookup(ResourceManager.MID_SERVER_REF);
            if(rm!=null) {
                logger.info("Object lookup hit");
            } else {
                logger.error("Object lookup miss!");
                throw new RuntimeException("Could not find the object in the registry");
            }
        } catch (Exception e) {
            logger.error("Cloud not connect, is registry up on " + server + ":" + port + " ?");
            throw new RuntimeException("Failed to connect to the middleware server");
        }
    }

    @Test
    public void oneClientOneRM_test() throws RemoteException, InterruptedException {

        logger.info("TEST: 1 Client and 1 RM (Flight RM)");

        // Thread number
        int totalThreads = LOW_THREAD;
        int testLevel = LOW_TEST;

        // Start transactions
        logger.info("Starting a new transaction");
        int xid = rm.start();

        // Delete customer
        logger.info("Cleaning any created customers");
        rm.deleteCustomer(xid, cid);

        // Remove flights if any
        logger.info("Cleaning any created flights");
        for(int i=0; i < testLevel * totalThreads; i++) {
            rm.deleteFlight(xid, i);
        }

        // Prepare thread list
        List<Thread> threadList = new ArrayList<>();

        // Start threads
        logger.info("Adding " + testLevel + " flights in each of " + totalThreads + " threads");
        for(int t=1; t <= totalThreads; t++) {
            // Create thread
            int finalT = t;
            Thread thread = new Thread(() -> {
                // Add flights
                for(int i = testLevel*(finalT -1); i < testLevel* finalT; i++) {
                    try {
                        if(!rm.addFlight(xid, i, i, i)) {
                            fail(String.format("Failed to add flight id: %d, number: %d", i, i));
                        }
                    } catch (RemoteException e) {
                        fail("Failed to add flight caused by remote exception");
                    }
                }
            });
            thread.start();
            threadList.add(thread);
        }

        // Wait until all threads terminate
        for(Thread thread : threadList) {
            thread.join();
        }

        // Query flights
        logger.info("Verifying that all flights were added ...");
        for(int i=0; i < testLevel * totalThreads; i++) {
            if(rm.queryFlight(xid, i) != i || rm.queryFlightPrice(xid, i) != i) {
                fail(String.format("Flight id: %d, number: %d was not added!", i, i));
            }
        }
        logger.info("Verification completed");

        // Commit transaction
        try {
            logger.info("Committing changes");
            rm.commit(xid);
        } catch (TransactionAbortedException e) {
            fail("Failed to commit transaction");
        }
        logger.info("Test completed successfully!");
    }
}