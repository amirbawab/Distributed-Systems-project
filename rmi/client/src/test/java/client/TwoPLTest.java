package client;

import inter.ResourceManager;
import lm.DeadlockException;
import lm.TransactionAbortedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.transaction.InvalidTransactionException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.fail;

public class TwoPLTest {

    // Logger
    private static final Logger logger = LogManager.getLogger(TwoPLTest.class);

    // Declare a static RM
    private static ResourceManager rm = null;

    // Transaction stats
    private int totalTransactions = 0;
    private List<Integer> completedTransactions;

    // Test level
    private final int LOW_TEST = 10;
    private final int MED_TEST = 100;
    private final int HIGH_TEST = 1000;

    // Thread level
    private final int LOW_THREAD = 2;
    private final int MED_THREAD = 5;
    private final int HIGH_THREAD = 10;

    /**
     * Log test analysis report
     * @param aborted
     * @param startTime
     */
    private void _logAnalysis(List<Boolean> aborted, long startTime) {
        int countAborted = 0;
        for(Boolean isAborted : aborted) {
            countAborted += isAborted ? 1 : 0;
        }
        int countCompleted = 0;
        for(int transactions : completedTransactions) {
            countCompleted += transactions;
        }
        logger.info("TEST REPORT:");
        logger.info(">> " + countAborted + " aborted threads out of " + aborted.size());
        logger.info(">> " + countCompleted + " transaction completed out of " + totalTransactions);
        logger.info(">> Total execution time: " + (System.currentTimeMillis() - startTime) + " ms");
    }

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
    @Ignore
    public void oneClientOneRM_test() throws RemoteException, InterruptedException {

        // Objects details
        final String testLocation = "test-location-1-";
        final int cid = 10000;
        final int testLevel = HIGH_TEST;
        final long startTime = System.currentTimeMillis();
        totalTransactions = 0;
        completedTransactions = new ArrayList<>();
        completedTransactions.add(0);

        logger.info("TITLE: 1 Client and all RM");

        // Start transactions
        logger.info("Starting a new transaction");
        int xid = rm.start();

        // Add new customer
        logger.info("Adding a new customer");
        rm.newCustomer(xid, cid);

        // Add flights
        logger.info("Adding " + testLevel + " flights");
        totalTransactions += testLevel;
        for(int i = 1; i <= testLevel; i++) {
            try {
                if(!rm.addFlight(xid, i, i, i)) {
                    fail(String.format("Failed to add flight id: %d, number: %d", i, i));
                }
                completedTransactions.set(0, completedTransactions.get(0)+1);
            } catch (RemoteException e) {
                fail("Failed to add flight caused by remote exception");
            }
        }

        // Reserve flights
        logger.info("Reserving flights");
        totalTransactions += testLevel;
        for(int i=1; i <= testLevel; i++) {
            if(!rm.reserveFlight(xid, cid, i)) {
                fail(String.format("Fail to reserve in flight id: %d, number: %d", i, i));
            }
            completedTransactions.set(0, completedTransactions.get(0)+1);
        }

        // Query flights
        totalTransactions += testLevel;
        logger.info("Verifying flight reservations");
        for(int i=1; i <= testLevel; i++) {
            if(rm.queryFlight(xid, i) != i-1) {
                fail(String.format("Flight id: %d, number: %d was not reserved!", i, i));
            }
            completedTransactions.set(0, completedTransactions.get(0)+1);
        }

        // Abort transaction
        logger.info("Aborting changes to avoid modifying the DB");
        rm.abort(xid);
        _logAnalysis(Arrays.asList(false), startTime);
        logger.info("Test completed successfully!");
    }

    @Test
    @Ignore
    public void oneClientTwoRM_test() throws RemoteException, InterruptedException {

        // Objects details
        final String testLocation = "test-location-2-";
        final int cid = 20000;
        final int testLevel = HIGH_TEST;
        final long startTime = System.currentTimeMillis();
        totalTransactions = 0;
        completedTransactions = new ArrayList<>();
        completedTransactions.add(0);

        logger.info("TITLE: 1 Client and all RM");

        // Start transactions
        logger.info("Starting a new transaction");
        int xid = rm.start();

        // Add new customer
        logger.info("Adding a new customer");
        rm.newCustomer(xid, cid);

        // Add flights
        logger.info("Adding " + testLevel/2 + " flights");
        totalTransactions += testLevel/2;
        for(int i = 1; i <= testLevel/2; i++) {
            try {
                if(!rm.addFlight(xid, i, i, i)) {
                    fail(String.format("Failed to add flight id: %d, number: %d", i, i));
                }
                completedTransactions.set(0, completedTransactions.get(0)+1);
            } catch (RemoteException e) {
                fail("Failed to add flight caused by remote exception");
            }
        }

        // Add rooms
        logger.info("Adding " + testLevel/2 + " rooms");
        totalTransactions += testLevel/2;
        for(int i = 1; i <= testLevel/2; i++) {
            try {
                if(!rm.addRooms(xid, testLocation+i, i, i)) {
                    fail(String.format("Failed to add room id: %d, location: %s", i, testLocation+i));
                }
                completedTransactions.set(0, completedTransactions.get(0)+1);
            } catch (RemoteException e) {
                fail("Failed to add room caused by remote exception");
            }
        }

        // Reserve flights
        logger.info("Reserving flights");
        totalTransactions += testLevel/2;
        for(int i=1; i <= testLevel/2; i++) {
            if(!rm.reserveFlight(xid, cid, i)) {
                fail(String.format("Fail to reserve in flight id: %d, number: %d", i, i));
            }
            completedTransactions.set(0, completedTransactions.get(0)+1);
        }

        // Reserve rooms
        logger.info("Reserving rooms");
        totalTransactions += testLevel/2;
        for(int i=1; i <= testLevel/2; i++) {
            if(!rm.reserveRoom(xid, cid, testLocation+i)) {
                fail(String.format("Fail to reserve room id: %d, location: %s", i, testLocation+i));
            }
            completedTransactions.set(0, completedTransactions.get(0)+1);
        }

        // Query flights
        logger.info("Verifying flight reservations");
        totalTransactions += testLevel/2;
        for(int i=1; i <= testLevel/2; i++) {
            if(rm.queryFlight(xid, i) != i-1) {
                fail(String.format("Flight id: %d, number: %d was not reserved!", i, i));
            }
            completedTransactions.set(0, completedTransactions.get(0)+1);
        }

        // Query rooms
        logger.info("Verifying rooms reservations");
        totalTransactions += testLevel/2;
        for(int i=1; i <= testLevel/2; i++) {
            if(rm.queryRooms(xid, testLocation+i) != i-1) {
                fail(String.format("Room id: %d, location: %s was not reserved!", i, testLocation+i));
            }
            completedTransactions.set(0, completedTransactions.get(0)+1);
        }

        // Abort transaction
        logger.info("Aborting changes to avoid modifying the DB");
        rm.abort(xid);
        _logAnalysis(Arrays.asList(false), startTime);
        logger.info("Test completed successfully!");
    }

    @Test
    @Ignore
    public void oneClientThreeRM_test() throws RemoteException, InterruptedException {

        // Objects details
        final String testLocation = "test-location-2-";
        final int cid = 20000;
        final int testLevel = HIGH_TEST;
        final long startTime = System.currentTimeMillis();
        totalTransactions = 0;
        completedTransactions = new ArrayList<>();
        completedTransactions.add(0);

        logger.info("TITLE: 1 Client and all RM");

        // Start transactions
        logger.info("Starting a new transaction");
        int xid = rm.start();

        // Add new customer
        logger.info("Adding a new customer");
        rm.newCustomer(xid, cid);

        // Add flights
        logger.info("Adding " + testLevel/3 + " flights");
        totalTransactions += testLevel/3;
        for(int i = 1; i <= testLevel/3; i++) {
            try {
                if(!rm.addFlight(xid, i, i, i)) {
                    fail(String.format("Failed to add flight id: %d, number: %d", i, i));
                }
                completedTransactions.set(0, completedTransactions.get(0)+1);
            } catch (RemoteException e) {
                fail("Failed to add flight caused by remote exception");
            }
        }

        // Add rooms
        logger.info("Adding " + testLevel/3 + " rooms");
        totalTransactions += testLevel/3;
        for(int i = 1; i <= testLevel/3; i++) {
            try {
                if(!rm.addRooms(xid, testLocation+i, i, i)) {
                    fail(String.format("Failed to add room id: %d, location: %s", i, testLocation+i));
                }
                completedTransactions.set(0, completedTransactions.get(0)+1);
            } catch (RemoteException e) {
                fail("Failed to add room caused by remote exception");
            }
        }

        // Add cars
        logger.info("Adding " + testLevel/3 + " cars");
        totalTransactions += testLevel/3;
        for(int i = 1; i <= testLevel/3; i++) {
            try {
                if(!rm.addCars(xid, testLocation+i, i, i)) {
                    fail(String.format("Failed to add car id: %d, location: %s", i, testLocation+i));
                }
                completedTransactions.set(0, completedTransactions.get(0)+1);
            } catch (RemoteException e) {
                fail("Failed to add car caused by remote exception");
            }
        }

        // Reserve flights
        logger.info("Reserving flights");
        totalTransactions += testLevel/3;
        for(int i=1; i <= testLevel/3; i++) {
            if(!rm.reserveFlight(xid, cid, i)) {
                fail(String.format("Fail to reserve in flight id: %d, number: %d", i, i));
            }
            completedTransactions.set(0, completedTransactions.get(0)+1);
        }

        // Reserve rooms
        logger.info("Reserving rooms");
        totalTransactions += testLevel/3;
        for(int i=1; i <= testLevel/3; i++) {
            if(!rm.reserveRoom(xid, cid, testLocation+i)) {
                fail(String.format("Fail to reserve room id: %d, location: %s", i, testLocation+i));
            }
            completedTransactions.set(0, completedTransactions.get(0)+1);
        }

        // Reserve cars
        logger.info("Reserving cars");
        totalTransactions += testLevel/3;
        for(int i=1; i <= testLevel/3; i++) {
            if(!rm.reserveCar(xid, cid, testLocation+i)) {
                fail(String.format("Fail to reserve car id: %d, location: %s", i, testLocation+i));
            }
            completedTransactions.set(0, completedTransactions.get(0)+1);
        }

        // Query flights
        logger.info("Verifying flight reservations");
        totalTransactions += testLevel/3;
        for(int i=1; i <= testLevel/3; i++) {
            if(rm.queryFlight(xid, i) != i-1) {
                fail(String.format("Flight id: %d, number: %d was not reserved!", i, i));
            }
            completedTransactions.set(0, completedTransactions.get(0)+1);
        }

        // Query rooms
        logger.info("Verifying rooms reservations");
        totalTransactions += testLevel/3;
        for(int i=1; i <= testLevel/3; i++) {
            if(rm.queryRooms(xid, testLocation+i) != i-1) {
                fail(String.format("Room id: %d, location: %s was not reserved!", i, testLocation+i));
            }
            completedTransactions.set(0, completedTransactions.get(0)+1);
        }

        // Query rooms
        logger.info("Verifying cars reservations");
        totalTransactions += testLevel/3;
        for(int i=1; i <= testLevel/3; i++) {
            if(rm.queryCars(xid, testLocation+i) != i-1) {
                fail(String.format("Car id: %d, location: %s was not reserved!", i, testLocation+i));
            }
            completedTransactions.set(0, completedTransactions.get(0)+1);
        }

        // Abort transaction
        logger.info("Aborting changes to avoid modifying the DB");
        rm.abort(xid);
        _logAnalysis(Arrays.asList(false), startTime);
        logger.info("Test completed successfully!");
    }

    @Test
    @Ignore
    public void manyClientsAllRM_conflict_test() throws RemoteException, InterruptedException {

        logger.info("TITLE: Many Clients and all RM, with conflict");

        // Objects details
        final int cid = 30000;
        final String testLocation = "test-location-3-";
        final int totalThreads = 10;
        final int testLevel = 1000/totalThreads;
        final long startTime = System.currentTimeMillis();
        final int LOAD_SLEEP = 10;
        totalTransactions = 0;
        completedTransactions = new ArrayList<>();
        for(int i=0; i < totalThreads; i++) completedTransactions.add(0);

        // Transaction array
        List<Integer> xidArray = new ArrayList<>();
        List<Boolean> abortedArray = new ArrayList<>();

        // Start transactions
        logger.info("Starting " + totalThreads + " new transaction");
        for(int i=0; i < totalThreads; i++) {
            xidArray.add(rm.start());
            abortedArray.add(false);
        }

        // Prepare thread list
        List<Thread> threadList = new ArrayList<>();

        // Start threads
        totalTransactions += totalThreads * testLevel;
        logger.info("Adding " + testLevel + " flights in " + totalThreads + " transactions");
        for(int t=0; t < totalThreads && !abortedArray.get(t); t++) {
            // Create thread
            int finalT = t;
            Thread thread = new Thread(() -> {
                // Add flights
                try {
                    for(int i = 0; i < testLevel; i++) {
                        if (!rm.addFlight(xidArray.get(finalT), i, i, i)) {
                            fail(String.format("Failed to add flight id: %d, number: %d", i, i));
                        }
                        Thread.sleep(LOAD_SLEEP);
                        synchronized (completedTransactions) {
                            completedTransactions.set(finalT, completedTransactions.get(finalT)+1);
                        }
                    }
                } catch (RemoteException e) {
                    if(e.getCause() instanceof InvalidTransactionException) {
                        logger.info("Transaction " + xidArray.get(finalT) + " aborted due to deadlock");
                        abortedArray.set(finalT, true);
                    } else {
                        fail(e.getMessage());
                    }
                } catch (InterruptedException e) {
                    fail(e.getMessage());
                }
            });
            thread.start();
            threadList.add(thread);
        }

        // Start threads
        logger.info("Adding " + testLevel + " cars in " + totalThreads + " transactions");
        totalTransactions += totalThreads * testLevel;
        for(int t=0; t < totalThreads && !abortedArray.get(t); t++) {
            // Create thread
            int finalT = t;
            Thread thread = new Thread(() -> {
                try {
                    for(int i = 0; i < testLevel; i++) {
                        if(!rm.addCars(xidArray.get(finalT), testLocation+i, i, i)) {
                            fail(String.format("Failed to add flight id: %d, location: %s", i, testLocation+i));
                        }
                        Thread.sleep(LOAD_SLEEP);
                        synchronized (completedTransactions) {
                            completedTransactions.set(finalT, completedTransactions.get(finalT)+1);
                        }
                    }
                } catch (RemoteException e) {
                    if(e.getCause() instanceof InvalidTransactionException) {
                        logger.info("Transaction " + xidArray.get(finalT) + " aborted due to deadlock");
                        abortedArray.set(finalT, true);
                    } else {
                        fail(e.getMessage());
                    }
                } catch (InterruptedException e) {
                    fail(e.getMessage());
                }
            });
            thread.start();
            threadList.add(thread);
        }

        // Start threads
        logger.info("Adding " + testLevel + " rooms in " + totalThreads + " transactions");
        totalTransactions += totalThreads * testLevel;
        for(int t=0; t < totalThreads && !abortedArray.get(t); t++) {
            // Create thread
            int finalT = t;
            Thread thread = new Thread(() -> {
                try {
                    for(int i = 0; i < testLevel; i++) {
                        if(!rm.addRooms(xidArray.get(finalT), testLocation+i, i, i)) {
                            fail(String.format("Failed to add room id: %d, location: %s", i, testLocation+i));
                        }
                        Thread.sleep(LOAD_SLEEP);
                        synchronized (completedTransactions) {
                            completedTransactions.set(finalT, completedTransactions.get(finalT)+1);
                        }
                    }
                } catch (RemoteException e) {
                    if(e.getCause() instanceof InvalidTransactionException) {
                        logger.info("Transaction " + xidArray.get(finalT) + " aborted due to deadlock");
                        abortedArray.set(finalT, true);
                    } else {
                        fail(e.getMessage());
                    }
                } catch (InterruptedException e) {
                    fail(e.getMessage());
                }
            });
            thread.start();
            threadList.add(thread);
        }

        // Wait until all threads terminate
        for(Thread thread : threadList) {
            thread.join();
        }

        // Abort transaction
        logger.info("Aborting changes to avoid modifying the DB");
        for(int i=0; i < totalThreads && !abortedArray.get(i); i++) {
            rm.abort(xidArray.get(i));
        }
        _logAnalysis(abortedArray, startTime);
        logger.info("Test completed successfully!");
    }

    @Test
    @Ignore
    public void manyClientsAllRM_no_conflict_test() throws RemoteException, InterruptedException {

        logger.info("TITLE: Many Clients and all RM, no conflict");

        // Objects details
        final int cid = 40000;
        final String testLocation = "test-location-3-";
        final int totalThreads = MED_THREAD;
        final int testLevel = LOW_TEST;
        final long startTime = System.currentTimeMillis();
        totalTransactions = 0;
        completedTransactions = new ArrayList<>();
        for(int i=0; i < totalThreads; i++) completedTransactions.add(0);

        // Transaction array
        List<Integer> xidArray = new ArrayList<>();
        List<Boolean> abortedArray = new ArrayList<>();

        // Start transactions
        logger.info("Starting " + totalThreads + " new transaction");
        for(int i=0; i < totalThreads; i++) {
            xidArray.add(rm.start());
            abortedArray.add(false);
        }

        // Prepare thread list
        List<Thread> threadList = new ArrayList<>();

        // Start threads
        totalTransactions += totalThreads * testLevel;
        logger.info("Adding " + testLevel + " flights in " + totalThreads + " transactions");
        for(int t=0; t < totalThreads && !abortedArray.get(t); t++) {
            // Create thread
            int finalT = t;
            Thread thread = new Thread(() -> {
                // Add flights
                try {
                    for(int i = testLevel*(finalT -1); i < testLevel* finalT; i++) {
                        if (!rm.addFlight(xidArray.get(finalT), i, i, i)) {
                            fail(String.format("Failed to add flight id: %d, number: %d", i, i));
                        }
                        synchronized (completedTransactions) {
                            completedTransactions.set(finalT, completedTransactions.get(finalT)+1);
                        }
                    }
                } catch (RemoteException e) {
                    if(e.getCause() instanceof InvalidTransactionException) {
                        logger.info("Transaction " + xidArray.get(finalT) + " aborted due to deadlock");
                        abortedArray.set(finalT, true);
                    } else {
                        fail(e.getMessage());
                    }
                }
            });
            thread.start();
            threadList.add(thread);
        }

        // Start threads
        logger.info("Adding " + testLevel + " cars in " + totalThreads + " transactions");
        totalTransactions += totalThreads * testLevel;
        for(int t=0; t < totalThreads && !abortedArray.get(t); t++) {
            // Create thread
            int finalT = t;
            Thread thread = new Thread(() -> {
                try {
                    for(int i = testLevel*(finalT -1); i < testLevel* finalT; i++) {
                        if(!rm.addCars(xidArray.get(finalT), testLocation+i, i, i)) {
                            fail(String.format("Failed to add flight id: %d, location: %s", i, testLocation+i));
                        }
                        synchronized (completedTransactions) {
                            completedTransactions.set(finalT, completedTransactions.get(finalT)+1);
                        }
                    }
                } catch (RemoteException e) {
                    if(e.getCause() instanceof InvalidTransactionException) {
                        logger.info("Transaction " + xidArray.get(finalT) + " aborted due to deadlock");
                        abortedArray.set(finalT, true);
                    } else {
                        fail(e.getMessage());
                    }
                }
            });
            thread.start();
            threadList.add(thread);
        }

        // Start threads
        logger.info("Adding " + testLevel + " rooms in " + totalThreads + " transactions");
        totalTransactions += totalThreads * testLevel;
        for(int t=0; t < totalThreads && !abortedArray.get(t); t++) {
            // Create thread
            int finalT = t;
            Thread thread = new Thread(() -> {
                try {
                    for(int i = testLevel*(finalT -1); i < testLevel* finalT; i++) {
                        if(!rm.addRooms(xidArray.get(finalT), testLocation+i, i, i)) {
                            fail(String.format("Failed to add room id: %d, location: %s", i, testLocation+i));
                        }
                        synchronized (completedTransactions) {
                            completedTransactions.set(finalT, completedTransactions.get(finalT)+1);
                        }
                    }
                } catch (RemoteException e) {
                    if(e.getCause() instanceof InvalidTransactionException) {
                        logger.info("Transaction " + xidArray.get(finalT) + " aborted due to deadlock");
                        abortedArray.set(finalT, true);
                    } else {
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

        // Abort transaction
        logger.info("Aborting changes to avoid modifying the DB");
        for(int i=0; i < totalThreads && !abortedArray.get(i); i++) {
            rm.abort(xidArray.get(i));
        }
        _logAnalysis(abortedArray, startTime);
        logger.info("Test completed successfully!");
    }

    @Test
//    @Ignore
    public void manyClientsAllRM_conflict_read_test() throws RemoteException, InterruptedException {

        logger.info("TITLE: Many Clients and all RM, with conflict");

        // Objects details
        final int cid = 30000;
        final String testLocation = "test-location-3-";
        final int totalThreads = 10;
        final int testLevel = 1000/totalThreads;
        final long startTime = System.currentTimeMillis();
        final int LOAD_SLEEP = 10;
        totalTransactions = 0;
        completedTransactions = new ArrayList<>();
        for(int i=0; i < totalThreads; i++) completedTransactions.add(0);

        // Transaction array
        List<Integer> xidArray = new ArrayList<>();
        List<Boolean> abortedArray = new ArrayList<>();

        // Start transactions
        logger.info("Starting " + totalThreads + " new transaction");
        for(int i=0; i < totalThreads; i++) {
            xidArray.add(rm.start());
            abortedArray.add(false);
        }

        // Prepare thread list
        List<Thread> threadList = new ArrayList<>();

        // Start threads
        totalTransactions += totalThreads * testLevel;
        logger.info("Querying " + testLevel + " flights in " + totalThreads + " transactions");
        for(int t=0; t < totalThreads && !abortedArray.get(t); t++) {
            // Create thread
            int finalT = t;
            Thread thread = new Thread(() -> {
                // Add flights
                try {
                    for(int i = 0; i < testLevel; i++) {
                        if (rm.queryFlight(xidArray.get(finalT), i) != 0) {
                            fail(String.format("Failed to query flight id: %d, number: %d", i, i));
                        }
                        Thread.sleep(LOAD_SLEEP);
                        synchronized (completedTransactions) {
                            completedTransactions.set(finalT, completedTransactions.get(finalT)+1);
                        }
                    }
                } catch (RemoteException e) {
                    if(e.getCause() instanceof InvalidTransactionException) {
                        logger.info("Transaction " + xidArray.get(finalT) + " aborted due to deadlock");
                        abortedArray.set(finalT, true);
                    } else {
                        fail(e.getMessage());
                    }
                } catch (InterruptedException e) {
                    fail(e.getMessage());
                }
            });
            thread.start();
            threadList.add(thread);
        }

        // Start threads
        totalTransactions += totalThreads * testLevel;
        logger.info("Querying " + testLevel + " rooms in " + totalThreads + " transactions");
        for(int t=0; t < totalThreads && !abortedArray.get(t); t++) {
            // Create thread
            int finalT = t;
            Thread thread = new Thread(() -> {
                // Add flights
                try {
                    for(int i = 0; i < testLevel; i++) {
                        if (rm.queryRooms(xidArray.get(finalT), testLocation+i) != 0) {
                            fail(String.format("Failed to query room id: %d, location: %s", i, testLocation+i));
                        }
                        Thread.sleep(LOAD_SLEEP);
                        synchronized (completedTransactions) {
                            completedTransactions.set(finalT, completedTransactions.get(finalT)+1);
                        }
                    }
                } catch (RemoteException e) {
                    if(e.getCause() instanceof InvalidTransactionException) {
                        logger.info("Transaction " + xidArray.get(finalT) + " aborted due to deadlock");
                        abortedArray.set(finalT, true);
                    } else {
                        fail(e.getMessage());
                    }
                } catch (InterruptedException e) {
                    fail(e.getMessage());
                }
            });
            thread.start();
            threadList.add(thread);
        }

        // Start threads
        totalTransactions += totalThreads * testLevel;
        logger.info("Querying " + testLevel + " cars in " + totalThreads + " transactions");
        for(int t=0; t < totalThreads && !abortedArray.get(t); t++) {
            // Create thread
            int finalT = t;
            Thread thread = new Thread(() -> {
                // Add flights
                try {
                    for(int i = 0; i < testLevel; i++) {
                        if (rm.queryCars(xidArray.get(finalT), testLocation+i) != 0) {
                            fail(String.format("Failed to query cars id: %d, location: %s", i, testLocation+i));
                        }
                        Thread.sleep(LOAD_SLEEP);
                        synchronized (completedTransactions) {
                            completedTransactions.set(finalT, completedTransactions.get(finalT)+1);
                        }
                    }
                } catch (RemoteException e) {
                    if(e.getCause() instanceof InvalidTransactionException) {
                        logger.info("Transaction " + xidArray.get(finalT) + " aborted due to deadlock");
                        abortedArray.set(finalT, true);
                    } else {
                        fail(e.getMessage());
                    }
                } catch (InterruptedException e) {
                    fail(e.getMessage());
                }
            });
            thread.start();
            threadList.add(thread);
        }

        // Wait until all threads terminate
        for(Thread thread : threadList) {
            thread.join();
        }

        // Abort transaction
        logger.info("Aborting changes to avoid modifying the DB");
        for(int i=0; i < totalThreads && !abortedArray.get(i); i++) {
            rm.abort(xidArray.get(i));
        }
        _logAnalysis(abortedArray, startTime);
        logger.info("Test completed successfully!");
    }

    @Test
    @Ignore
    public void manyClientsAllRM_no_conflict_read_test() throws RemoteException, InterruptedException {

        logger.info("TITLE: Many Clients and all RM, no conflict");

        // Objects details
        final int cid = 40000;
        final String testLocation = "test-location-3-";
        final int totalThreads = 10;
        final int testLevel = 1000/totalThreads;
        final long startTime = System.currentTimeMillis();
        totalTransactions = 0;
        completedTransactions = new ArrayList<>();
        for(int i=0; i < totalThreads; i++) completedTransactions.add(0);

        // Transaction array
        List<Integer> xidArray = new ArrayList<>();
        List<Boolean> abortedArray = new ArrayList<>();

        // Start transactions
        logger.info("Starting " + totalThreads + " new transaction");
        for(int i=0; i < totalThreads; i++) {
            xidArray.add(rm.start());
            abortedArray.add(false);
        }

        // Prepare thread list
        List<Thread> threadList = new ArrayList<>();


        // Start threads
        totalTransactions += totalThreads * testLevel;
        logger.info("Querying " + testLevel + " flights in " + totalThreads + " transactions");
        for(int t=0; t < totalThreads && !abortedArray.get(t); t++) {
            // Create thread
            int finalT = t;
            Thread thread = new Thread(() -> {
                // Add flights
                try {
                    for(int i = testLevel*(finalT -1); i < testLevel* finalT; i++) {
                        if (rm.queryFlight(xidArray.get(finalT), i) != 0) {
                            fail(String.format("Failed to query flight id: %d, number: %d", i, i));
                        }
                        synchronized (completedTransactions) {
                            completedTransactions.set(finalT, completedTransactions.get(finalT)+1);
                        }
                    }
                } catch (RemoteException e) {
                    if(e.getCause() instanceof InvalidTransactionException) {
                        logger.info("Transaction " + xidArray.get(finalT) + " aborted due to deadlock");
                        abortedArray.set(finalT, true);
                    } else {
                        fail(e.getMessage());
                    }
                }
            });
            thread.start();
            threadList.add(thread);
        }

        // Start threads
        totalTransactions += totalThreads * testLevel;
        logger.info("Querying " + testLevel + " rooms in " + totalThreads + " transactions");
        for(int t=0; t < totalThreads && !abortedArray.get(t); t++) {
            // Create thread
            int finalT = t;
            Thread thread = new Thread(() -> {
                // Add flights
                try {
                    for(int i = testLevel*(finalT -1); i < testLevel* finalT; i++) {
                        if (rm.queryRooms(xidArray.get(finalT), testLocation+i) != 0) {
                            fail(String.format("Failed to query room id: %d, location: %s", i, testLocation+i));
                        }
                        synchronized (completedTransactions) {
                            completedTransactions.set(finalT, completedTransactions.get(finalT)+1);
                        }
                    }
                } catch (RemoteException e) {
                    if(e.getCause() instanceof InvalidTransactionException) {
                        logger.info("Transaction " + xidArray.get(finalT) + " aborted due to deadlock");
                        abortedArray.set(finalT, true);
                    } else {
                        fail(e.getMessage());
                    }
                }
            });
            thread.start();
            threadList.add(thread);
        }

        // Start threads
        totalTransactions += totalThreads * testLevel;
        logger.info("Querying " + testLevel + " cars in " + totalThreads + " transactions");
        for(int t=0; t < totalThreads && !abortedArray.get(t); t++) {
            // Create thread
            int finalT = t;
            Thread thread = new Thread(() -> {
                // Add flights
                try {
                    for(int i = testLevel*(finalT -1); i < testLevel* finalT; i++) {
                        if (rm.queryCars(xidArray.get(finalT), testLocation+i) != 0) {
                            fail(String.format("Failed to query cars id: %d, location: %s", i, testLocation+i));
                        }
                        synchronized (completedTransactions) {
                            completedTransactions.set(finalT, completedTransactions.get(finalT)+1);
                        }
                    }
                } catch (RemoteException e) {
                    if(e.getCause() instanceof InvalidTransactionException) {
                        logger.info("Transaction " + xidArray.get(finalT) + " aborted due to deadlock");
                        abortedArray.set(finalT, true);
                    } else {
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

        // Abort transaction
        logger.info("Aborting changes to avoid modifying the DB");
        for(int i=0; i < totalThreads && !abortedArray.get(i); i++) {
            rm.abort(xidArray.get(i));
        }
        _logAnalysis(abortedArray, startTime);
        logger.info("Test completed successfully!");
    }
}