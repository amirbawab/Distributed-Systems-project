package midserver;

import inter.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;

class MiddlewareServer implements ResourceManager {

    // RM components
    private ResourceManager m_carRM;
    private ResourceManager m_flightRM;
    private ResourceManager m_roomRM;

    // Program exit codes
    private static final int CODE_ERROR=1;

    // Logger
    private static final Logger logger = LogManager.getLogger(MiddlewareServer.class);

    public static void main(String[] args) {

        // Figure out where server is running
        if (args.length != 3) {
            System.err.println ("Wrong usage");
            System.out.println("    Usage: java ResImpl.ResourceManagerImpl [server port] [registry ip] [registry port]");
            System.exit(MiddlewareServer.CODE_ERROR);
        }

        // Set server port
        int serverRMIRegistryPort = Integer.parseInt(args[0]);
        String rmRMIRegistryIP = args[1];
        int rmRMIRegistryPort = Integer.parseInt(args[2]);

        // Connect
        MiddlewareServer ms = bindRM(ResourceManager.MID_SERVER_REF, serverRMIRegistryPort);
        ms.m_carRM = connectToRM(ResourceManager.RM_CAR_REF, rmRMIRegistryIP, rmRMIRegistryPort);
        ms.m_flightRM = connectToRM(ResourceManager.RM_FLIGHT_REF, rmRMIRegistryIP, rmRMIRegistryPort);
        ms.m_roomRM = connectToRM(ResourceManager.RM_ROOM_REF, rmRMIRegistryIP, rmRMIRegistryPort);

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
    }

    private static MiddlewareServer bindRM(String key, int port) {
        // Bind server object
        try {
            // Create a new Server object
            MiddlewareServer obj = new MiddlewareServer();
            // Dynamically generate the stub (client proxy)
            ResourceManager rm = (ResourceManager) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry(port);
            registry.rebind(key, rm);
            logger.info("Middleware server ready!");
            return obj;
        } catch (Exception e) {
            logger.error("Middleware server exception: " + e.toString());
        }
        throw new RuntimeException("Binding failure: Terminating program ...");
    }

    /**
     * Connect to RM
     * @param key
     */
    private static ResourceManager connectToRM(String key, String server, int port) {
        try {
            // get a reference to the rmiregistry
            Registry registry = LocateRegistry.getRegistry(server, port);
            // get the proxy and the remote reference by rmiregistry lookup
            ResourceManager rm = (ResourceManager) registry.lookup(key);
            if(rm!=null) {
                logger.info("Connected to RM: " + key);
            } else {
                logger.error("Could not connect to RM: " + key);
            }
            return rm;
        } catch (Exception e) {
            logger.error("Exception while connecting to RM: "+ e.toString());
        }
        throw new RuntimeException("Connection failure: Terminating program ...");
    }

    @Override
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException {
        return m_flightRM.addFlight(id, flightNum, flightSeats, flightPrice);
    }

    @Override
    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException {
        return m_carRM.addCars(id, location, numCars, price);
    }

    @Override
    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException {
        return m_roomRM.addRooms(id, location, numRooms, price);
    }

    @Override
    public int newCustomer(int id) throws RemoteException {
        int cid = m_carRM.newCustomer(id);
        m_roomRM.newCustomer(id, cid);
        m_flightRM.newCustomer(id, cid);
        return cid;
    }

    @Override
    public boolean newCustomer(int id, int cid) throws RemoteException {
        return m_flightRM.newCustomer(id, cid) &&
                m_carRM.newCustomer(id, cid) &&
                m_roomRM.newCustomer(id, cid);
    }

    @Override
    public boolean deleteFlight(int id, int flightNum) throws RemoteException {
        return m_flightRM.deleteFlight(id, flightNum);
    }

    @Override
    public boolean deleteCars(int id, String location) throws RemoteException {
        return m_carRM.deleteCars(id, location);
    }

    @Override
    public boolean deleteRooms(int id, String location) throws RemoteException {
        return m_roomRM.deleteRooms(id, location);
    }

    @Override
    public boolean deleteCustomer(int id, int customer) throws RemoteException {
        return m_roomRM.deleteCustomer(id, customer) &&
                m_carRM.deleteCustomer(id, customer) &&
                m_flightRM.deleteCustomer(id, customer);
    }

    @Override
    public int queryFlight(int id, int flightNumber) throws RemoteException {
        return m_flightRM.queryFlight(id, flightNumber);
    }

    @Override
    public int queryCars(int id, String location) throws RemoteException {
        return m_carRM.queryCars(id, location);
    }

    @Override
    public int queryRooms(int id, String location) throws RemoteException {
        return m_roomRM.queryRooms(id, location);
    }

    @Override
    public String queryCustomerInfo(int id, int customer) throws RemoteException {
        StringBuilder sb = new StringBuilder();
        sb.append("\nCar info:\n").append(m_carRM.queryCustomerInfo(id, customer))
                .append("\nFlight info:\n").append(m_flightRM.queryCustomerInfo(id, customer))
                .append("\nRoom info:\n").append(m_roomRM.queryCustomerInfo(id, customer));
        return sb.toString();
    }

    @Override
    public int queryFlightPrice(int id, int flightNumber) throws RemoteException {
        return m_flightRM.queryFlightPrice(id, flightNumber);
    }

    @Override
    public int queryCarsPrice(int id, String location) throws RemoteException {
        return m_carRM.queryCarsPrice(id, location);
    }

    @Override
    public int queryRoomsPrice(int id, String location) throws RemoteException {
        return m_roomRM.queryRoomsPrice(id, location);
    }

    @Override
    public boolean reserveFlight(int id, int customer, int flightNumber) throws RemoteException {
        return m_flightRM.reserveFlight(id, customer, flightNumber);
    }

    @Override
    public boolean reserveCar(int id, int customer, String location) throws RemoteException {
        return m_carRM.reserveCar(id, customer, location);
    }

    @Override
    public boolean reserveRoom(int id, int customer, String locationd) throws RemoteException {
        return m_roomRM.reserveRoom(id, customer, locationd);
    }

    @Override
    public boolean itinerary(int id, int customer, Vector flightNumbers, String location, boolean car, boolean room)
            throws RemoteException {
        boolean success = true;
        // Reserve flights
        for(Object fNum : flightNumbers) {
            success &= reserveFlight(id, customer, Integer.parseInt(fNum.toString()));
        }

        // If should reserve a car
        if(car) {
            success &= reserveCar(id, customer, location);
        }

        // If should reserve a room
        if(room) {
            success &= reserveRoom(id, customer, location);
        }
        return success;
    }
}