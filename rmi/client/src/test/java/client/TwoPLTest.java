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

        // Objects details
        final String testLocation = "test-location-1-";
        final int cid = 10000;
        final int testLevel = HIGH_TEST;

        logger.info("TITLE: 1 Client and all RM");

        // Start transactions
        logger.info("Starting a new transaction");
        int xid = rm.start();

        // Add new customer
        logger.info("Adding a new customer");
        rm.newCustomer(xid, cid);

        // Add flights
        logger.info("Adding " + testLevel + " flights");
        for(int i = 1; i <= testLevel; i++) {
            try {
                if(!rm.addFlight(xid, i, i, i)) {
                    fail(String.format("Failed to add flight id: %d, number: %d", i, i));
                }
            } catch (RemoteException e) {
                fail("Failed to add flight caused by remote exception");
            }
        }

        // Reserve flights
        logger.info("Reserving flights");
        for(int i=1; i <= testLevel; i++) {
            if(!rm.reserveFlight(xid, cid, i)) {
                fail(String.format("Fail to reserve in flight id: %d, number: %d", i, i));
            }
        }

        // Query flights
        logger.info("Verifying flight reservations");
        for(int i=1; i <= testLevel; i++) {
            if(rm.queryFlight(xid, i) != i-1) {
                fail(String.format("Flight id: %d, number: %d was not reserved!", i, i));
            }
        }

        // Abort transaction
        logger.info("Aborting changes to avoid modifying the DB");
        rm.abort(xid);
        logger.info("Test completed successfully!");
    }

    @Test
    public void oneClientAllRM_test() throws RemoteException, InterruptedException {

        // Objects details
        final String testLocation = "test-location-2-";
        final int cid = 20000;
        final int testLevel = HIGH_TEST;

        logger.info("TITLE: 1 Client and all RM");

        // Start transactions
        logger.info("Starting a new transaction");
        int xid = rm.start();

        // Add new customer
        logger.info("Adding a new customer");
        rm.newCustomer(xid, cid);

        // Add flights
        logger.info("Adding " + testLevel + " flights");
        for(int i = 1; i <= testLevel; i++) {
            try {
                if(!rm.addFlight(xid, i, i, i)) {
                    fail(String.format("Failed to add flight id: %d, number: %d", i, i));
                }
            } catch (RemoteException e) {
                fail("Failed to add flight caused by remote exception");
            }
        }

        // Add rooms
        logger.info("Adding " + testLevel + " rooms");
        for(int i = 1; i <= testLevel; i++) {
            try {
                if(!rm.addRooms(xid, testLocation+i, i, i)) {
                    fail(String.format("Failed to add room id: %d, location: %s", i, testLocation+i));
                }
            } catch (RemoteException e) {
                fail("Failed to add room caused by remote exception");
            }
        }

        // Add cars
        logger.info("Adding " + testLevel + " cars");
        for(int i = 1; i <= testLevel; i++) {
            try {
                if(!rm.addCars(xid, testLocation+i, i, i)) {
                    fail(String.format("Failed to add car id: %d, location: %s", i, testLocation+i));
                }
            } catch (RemoteException e) {
                fail("Failed to add car caused by remote exception");
            }
        }

        // Reserve flights
        logger.info("Reserving flights");
        for(int i=1; i <= testLevel; i++) {
            if(!rm.reserveFlight(xid, cid, i)) {
                fail(String.format("Fail to reserve in flight id: %d, number: %d", i, i));
            }
        }

        // Reserve rooms
        logger.info("Reserving rooms");
        for(int i=1; i <= testLevel; i++) {
            if(!rm.reserveRoom(xid, cid, testLocation+i)) {
                fail(String.format("Fail to reserve room id: %d, location: %s", i, testLocation+i));
            }
        }

        // Reserve cars
        logger.info("Reserving cars");
        for(int i=1; i <= testLevel; i++) {
            if(!rm.reserveCar(xid, cid, testLocation+i)) {
                fail(String.format("Fail to reserve car id: %d, location: %s", i, testLocation+i));
            }
        }

        // Query flights
        logger.info("Verifying flight reservations");
        for(int i=1; i <= testLevel; i++) {
            if(rm.queryFlight(xid, i) != i-1) {
                fail(String.format("Flight id: %d, number: %d was not reserved!", i, i));
            }
        }

        // Query rooms
        logger.info("Verifying rooms reservations");
        for(int i=1; i <= testLevel; i++) {
            if(rm.queryRooms(xid, testLocation+i) != i-1) {
                fail(String.format("Room id: %d, location: %s was not reserved!", i, testLocation+i));
            }
        }

        // Query rooms
        logger.info("Verifying cars reservations");
        for(int i=1; i <= testLevel; i++) {
            if(rm.queryCars(xid, testLocation+i) != i-1) {
                fail(String.format("Car id: %d, location: %s was not reserved!", i, testLocation+i));
            }
        }

        // Abort transaction
        logger.info("Aborting changes to avoid modifying the DB");
        rm.abort(xid);
        logger.info("Test completed successfully!");
    }

    @Test
    @Ignore
    public void manyClientsAllRM_test() throws RemoteException, InterruptedException {

        final int cid = 30000;

        // Objects details
        final String testLocation = "test-location-3-";

        logger.info("TITLE: Many Clients and all RM");

        // Thread number
        int totalThreads = LOW_THREAD;
        int testLevel = LOW_TEST;

        // Cleaning transaction
        int cleanXid = rm.start();

        // Transaction array
        List<Integer> xidArray = new ArrayList<>();

        // Start transactions
        logger.info("Starting " + totalThreads + " new transaction");
        for(int i=0; i < totalThreads; i++) {
            xidArray.add(rm.start());
        }

        // Delete customer
        logger.info("Cleaning any created customers");
        rm.deleteCustomer(cleanXid, cid);

        // Remove flights if any
        logger.info("Cleaning any created flights");
        for(int i=0; i < testLevel; i++) {
            rm.deleteFlight(cleanXid, i);
        }

        // Remove cars if any
        logger.info("Cleaning any created cars");
        for(int i=0; i < testLevel; i++) {
            rm.deleteCars(cleanXid, testLocation+i);
        }

        // Remove rooms if any
        logger.info("Cleaning any created rooms");
        for(int i=0; i < testLevel; i++) {
            rm.deleteRooms(cleanXid, testLocation+i);
        }

        // Commit clean data
        try {
            logger.info("Committing clean transaction");
            rm.commit(cleanXid);
        } catch (TransactionAbortedException e) {
            fail("Failed to commit clean transaction");
        }

        // Prepare thread list
        List<Thread> threadList = new ArrayList<>();

        // Start threads
        logger.info("Adding " + testLevel + " flights in " + totalThreads + " transactions");
        for(int t=0; t < totalThreads; t++) {
            // Create thread
            int finalT = t;
            Thread thread = new Thread(() -> {
                // Add flights
                for(int i = 0; i < testLevel; i++) {
                    try {
                        if(!rm.addFlight(xidArray.get(finalT), i, i, i)) {
                            fail(String.format("Failed to add flight id: %d, number: %d", i, i));
                        }
                    } catch (RemoteException e) {
                        fail(e.getMessage());
                    }
                }
            });
            thread.start();
            threadList.add(thread);
        }

        // Start threads
        logger.info("Adding " + testLevel + " cars in " + totalThreads + " transactions");
        for(int t=0; t < totalThreads; t++) {
            // Create thread
            int finalT = t;
            Thread thread = new Thread(() -> {
                // Add flights
                for(int i = 0; i < testLevel; i++) {
                    try {
                        if(!rm.addCars(xidArray.get(finalT), testLocation+i, i, i)) {
                            fail(String.format("Failed to add flight id: %d, location: %s", i, testLocation+i));
                        }
                    } catch (RemoteException e) {
                        fail(e.getMessage());
                    }
                }
            });
            thread.start();
            threadList.add(thread);
        }

        // Start threads
        logger.info("Adding " + testLevel + " rooms in " + totalThreads + " transactions");
        for(int t=0; t < totalThreads; t++) {
            // Create thread
            int finalT = t;
            Thread thread = new Thread(() -> {
                // Add flights
                for(int i = 0; i < testLevel; i++) {
                    try {
                        if(!rm.addRooms(xidArray.get(finalT), testLocation+i, i, i)) {
                            fail(String.format("Failed to add room id: %d, location: %s", i, testLocation+i));
                        }
                    } catch (RemoteException e) {
                        fail(e.getMessage());
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

        // TODO Compute the loss
        // TODO If transaction aborted then flag it so that it's not in use anymore

        // Abort transaction
        logger.info("Aborting changes to avoid modifying the DB");
        for(int i=0; i < totalThreads; i++) {
            rm.abort(xidArray.get(i));
        }
        logger.info("Test completed successfully!");
    }
}