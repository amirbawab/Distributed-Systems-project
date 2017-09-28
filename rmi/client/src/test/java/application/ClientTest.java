package application;
import inter.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import static org.junit.Assert.*;

public class ClientTest {

    // Logger
    private static final Logger logger = LogManager.getLogger(Client.class);

    // Declare a static RM
    private static ResourceManager rm = null;

    // Test level
    private final int LOW_TEST = 100;
    private final int MED_TEST = 1000;
    private final int HIGH_TEST = 10000;

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
            logger.error(e.toString());
            throw new RuntimeException("Failed to connect to the middleware server");
        }
    }

    @Test
    public void addFlights() throws RemoteException {

        // Remove flights if any
        for(int i=1; i < LOW_TEST; i++) {
            rm.deleteFlight(i, i);
        }

        // Add flights
        for(int i=1; i < LOW_TEST; i++) {
            if(!rm.addFlight(i, i, i, i)) {
                fail(String.format("Failed to add flight id: %d, number: %d", i, i));
            }
        }

        // Query flights
        for(int i=1; i < LOW_TEST; i++) {
            if(rm.queryFlight(i, i) != i || rm.queryFlightPrice(i, i) != i) {
                fail(String.format("Flight id: %d, number: %d was not added!", i, i));
            }
        }

    }
}