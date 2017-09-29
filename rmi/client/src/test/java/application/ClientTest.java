package application;
import inter.ResourceManager;
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

import static org.junit.Assert.*;

public class ClientTest {

    // Logger
    private static final Logger logger = LogManager.getLogger(Client.class);

    // Declare a static RM
    private static ResourceManager rm = null;

    // Test level
    private final int LOW_TEST = 10;
    private final int MED_TEST = 100;
    private final int HIGH_TEST = 1000;

    // Customer info
    int id = 1;
    int cid = 1;

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
    public void flights_test_1() throws RemoteException, InterruptedException {

        // Thread number
        int totalThreads = 5;
        int testLevel = LOW_TEST;

        // Delete customer
        rm.deleteCustomer(id, cid);

        // Remove flights if any
        for(int i=0; i < testLevel * totalThreads; i++) {
            rm.deleteFlight(i, i);
        }

        // Prepare thread list
        List<Thread> threadList = new ArrayList<>();

        // Start threads
        for(int t=1; t <= totalThreads; t++) {
            // Create thread
            int finalT = t;
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    // Add flights
                    for(int i = testLevel*(finalT -1); i < testLevel* finalT; i++) {
                        try {
                            if(!rm.addFlight(i, i, i, i)) {
                                fail(String.format("Failed to add flight id: %d, number: %d", i, i));
                            }
                        } catch (RemoteException e) {
                            fail("Failed to add flight caused by remote exception");
                        }
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
        for(int i=0; i < testLevel * totalThreads; i++) {
            if(rm.queryFlight(i, i) != i || rm.queryFlightPrice(i, i) != i) {
                fail(String.format("Flight id: %d, number: %d was not added!", i, i));
            }
        }
    }

    @Test
    public void flights_test_2() throws RemoteException, InterruptedException {

        // Thread number
        int totalThreads = 2;
        int testLevel = LOW_TEST;

        // Delete customer
        rm.deleteCustomer(id, cid);

        // Remove flights if any
        for(int i=0; i < testLevel * totalThreads; i++) {
            rm.deleteFlight(i, i);
        }

        // Add flights
        for(int i = 0; i < testLevel * totalThreads; i++) {
            if(!rm.addFlight(i, i, i, i)) {
                fail(String.format("Failed to add flight id: %d, number: %d", i, i));
            }
        }

        // Create customer
        rm.newCustomer(id, cid);

        // Prepare thread list
        List<Thread> threadList = new ArrayList<>();

        // Start threads
        for(int t=1; t <= totalThreads; t++) {
            int finalT = t;
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    for(int i=testLevel*(finalT -1); i < testLevel*finalT; i++) {
                        try {

                            // Reserve i seats
                            for(int k=0; k < i; k++) {
                                if(!rm.reserveFlight(i, cid, i)) {
                                    fail(String.format("Could not reserve seats in flight %d", i));
                                }
                            }
                        } catch (RemoteException e) {
                            fail("Failed to reserve flight caused by remote exception");
                        }
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
        for(int i=0; i < testLevel * totalThreads; i++) {
            if(rm.queryFlight(i, i) != 0) {
                fail(String.format("Flight id: %d, number: %d was not fully reserved!", i, i));
            }
        }
    }

    @Test
    public void cars_test_1() throws RemoteException, InterruptedException {

        // Thread number
        int totalThreads = 5;
        int testLevel = LOW_TEST;
        String testLocation = "test-location-";

        // Delete customer
        rm.deleteCustomer(id, cid);

        // Remove cars if any
        for(int i=0; i < testLevel * totalThreads; i++) {
            rm.deleteCars(i, testLocation+i);
        }

        // Prepare thread list
        List<Thread> threadList = new ArrayList<>();

        // Start threads
        for(int t=1; t <= totalThreads; t++) {
            // Create thread
            int finalT = t;
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    // Add cars
                    for(int i = testLevel*(finalT -1); i < testLevel* finalT; i++) {
                        try {
                            if(!rm.addCars(i, testLocation+i, i, i)) {
                                fail(String.format("Failed to add car id: %d", i));
                            }
                        } catch (RemoteException e) {
                            fail("Failed to add car caused by remote exception");
                        }
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

        // Query cars
        for(int i=0; i < testLevel * totalThreads; i++) {
            if(rm.queryCars(i, testLocation+i) != i || rm.queryCarsPrice(i, testLocation+i) != i) {
                fail(String.format("Car id: %d was not added!", i));
            }
        }
    }
}