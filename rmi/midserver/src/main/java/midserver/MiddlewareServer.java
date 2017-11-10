package midserver;

import inter.ResourceManager;
import lm.LockManager;
import lm.TransactionAbortedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tm.TransactionManager;

import javax.transaction.InvalidTransactionException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;

class MiddlewareServer implements ResourceManager {

    // Sync lock
    private final static Object lock = new Object();

    // RM components
    private ResourceManager m_carRM;
    private ResourceManager m_flightRM;
    private ResourceManager m_roomRM;

    // Program exit codes
    private static final int CODE_ERROR=1;

    // Lock manager
    private LockManager m_lockManager;

    // Transaction manager
    private TransactionManager m_tm;

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

        // Initialize the lock manager
        ms.m_lockManager = new LockManager();

        // Initialize the transaction manager
        ms.m_tm = new TransactionManager();

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
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
            throws RemoteException {
        synchronized (lock) {
            m_tm.getTransaction(id).addRM(m_flightRM);
            return m_flightRM.addFlight(m_tm.getTransaction(id).getXID(), flightNum, flightSeats, flightPrice);
        }
    }

    @Override
    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException {
        synchronized (lock) {
            m_tm.getTransaction(id).addRM(m_carRM);
            return m_carRM.addCars(m_tm.getTransaction(id).getXID(), location, numCars, price);
        }
    }

    @Override
    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException {
        synchronized (lock){
            m_tm.getTransaction(id).addRM(m_roomRM);
            return m_roomRM.addRooms(m_tm.getTransaction(id).getXID(), location, numRooms, price);
        }
    }

    @Override
    public int newCustomer(int id) throws RemoteException {
        synchronized (lock) {
            m_tm.getTransaction(id).addRM(m_roomRM);
            m_tm.getTransaction(id).addRM(m_carRM);
            m_tm.getTransaction(id).addRM(m_flightRM);
            int cid = m_carRM.newCustomer(m_tm.getTransaction(id).getXID());
            m_roomRM.newCustomer(id, cid);
            m_flightRM.newCustomer(id, cid);
            return cid;
        }
    }

    @Override
    public boolean newCustomer(int id, int cid) throws RemoteException {
        synchronized (lock) {
            id = m_tm.getTransaction(id).getXID();
            m_tm.getTransaction(id).addRM(m_flightRM);
            m_tm.getTransaction(id).addRM(m_carRM);
            m_tm.getTransaction(id).addRM(m_roomRM);
            return m_flightRM.newCustomer(id, cid) &&
                    m_carRM.newCustomer(id, cid) &&
                    m_roomRM.newCustomer(id, cid);
        }
    }

    @Override
    public boolean deleteFlight(int id, int flightNum) throws RemoteException {
        synchronized (lock) {
            m_tm.getTransaction(id).addRM(m_flightRM);
            return m_flightRM.deleteFlight(m_tm.getTransaction(id).getXID(), flightNum);
        }
    }

    @Override
    public boolean deleteCars(int id, String location) throws RemoteException {
        synchronized (lock) {
            m_tm.getTransaction(id).addRM(m_carRM);
            return m_carRM.deleteCars(m_tm.getTransaction(id).getXID(), location);
        }
    }

    @Override
    public boolean deleteRooms(int id, String location) throws RemoteException {
        synchronized (lock) {
            m_tm.getTransaction(id).addRM(m_roomRM);
            return m_roomRM.deleteRooms(m_tm.getTransaction(id).getXID(), location);
        }
    }

    @Override
    public boolean deleteCustomer(int id, int customer) throws RemoteException {
        synchronized (lock) {
            id = m_tm.getTransaction(id).getXID();
            m_tm.getTransaction(id).addRM(m_roomRM);
            m_tm.getTransaction(id).addRM(m_carRM);
            m_tm.getTransaction(id).addRM(m_flightRM);
            return m_roomRM.deleteCustomer(id, customer) &&
                    m_carRM.deleteCustomer(id, customer) &&
                    m_flightRM.deleteCustomer(id, customer);
        }
    }

    @Override
    public int queryFlight(int id, int flightNumber) throws RemoteException {
        synchronized (lock) {
            m_tm.getTransaction(id).addRM(m_flightRM);
            return m_flightRM.queryFlight(m_tm.getTransaction(id).getXID(), flightNumber);
        }
    }

    @Override
    public int queryCars(int id, String location) throws RemoteException {
        synchronized (lock) {
            m_tm.getTransaction(id).addRM(m_carRM);
            return m_carRM.queryCars(m_tm.getTransaction(id).getXID(), location);
        }
    }

    @Override
    public int queryRooms(int id, String location) throws RemoteException {
        synchronized (lock) {
            m_tm.getTransaction(id).addRM(m_roomRM);
            return m_roomRM.queryRooms(m_tm.getTransaction(id).getXID(), location);
        }
    }

    @Override
    public String queryCustomerInfo(int id, int customer) throws RemoteException {
        synchronized (lock) {
            m_tm.getTransaction(id).addRM(m_carRM);
            m_tm.getTransaction(id).addRM(m_flightRM);
            m_tm.getTransaction(id).addRM(m_roomRM);
            StringBuilder sb = new StringBuilder();
            id = m_tm.getTransaction(id).getXID();
            sb.append("\nCar info:\n").append(m_carRM.queryCustomerInfo(id, customer))
                    .append("\nFlight info:\n").append(m_flightRM.queryCustomerInfo(id, customer))
                    .append("\nRoom info:\n").append(m_roomRM.queryCustomerInfo(id, customer));
            return sb.toString();
        }
    }

    @Override
    public int queryFlightPrice(int id, int flightNumber) throws RemoteException {
        synchronized (lock) {
            m_tm.getTransaction(id).addRM(m_flightRM);
            return m_flightRM.queryFlightPrice(m_tm.getTransaction(id).getXID(), flightNumber);
        }
    }

    @Override
    public int queryCarsPrice(int id, String location) throws RemoteException {
        synchronized (lock) {
            m_tm.getTransaction(id).addRM(m_carRM);
            return m_carRM.queryCarsPrice(m_tm.getTransaction(id).getXID(), location);
        }
    }

    @Override
    public int queryRoomsPrice(int id, String location) throws RemoteException {
        synchronized (lock) {
            m_tm.getTransaction(id).addRM(m_roomRM);
            return m_roomRM.queryRoomsPrice(m_tm.getTransaction(id).getXID(), location);
        }
    }

    @Override
    public boolean reserveFlight(int id, int customer, int flightNumber) throws RemoteException {
        synchronized (lock) {
            m_tm.getTransaction(id).addRM(m_flightRM);
            return m_flightRM.reserveFlight(m_tm.getTransaction(id).getXID(), customer, flightNumber);
        }
    }

    @Override
    public boolean reserveCar(int id, int customer, String location) throws RemoteException {
        synchronized (lock) {
            m_tm.getTransaction(id).addRM(m_carRM);
            return m_carRM.reserveCar(m_tm.getTransaction(id).getXID(), customer, location);
        }
    }

    @Override
    public boolean reserveRoom(int id, int customer, String locationd) throws RemoteException {
        synchronized (lock) {
            m_tm.getTransaction(id).addRM(m_roomRM);
            return m_roomRM.reserveRoom(m_tm.getTransaction(id).getXID(), customer, locationd);
        }
    }

    @Override
    public boolean itinerary(int id, int customer, Vector flightNumbers, String location, boolean car, boolean room)
            throws RemoteException {
        synchronized (lock) {
            id = m_tm.getTransaction(id).getXID();
            m_tm.getTransaction(id).addRM(m_flightRM);
            m_tm.getTransaction(id).addRM(m_carRM);
            m_tm.getTransaction(id).addRM(m_roomRM);

            // Check if can reserve flight
            for(Object fNum : flightNumbers) {
                if(queryFlight(id, Integer.parseInt(fNum.toString())) == 0) {
                    return false;
                }
            }

            // Check if can reserve a car
            if(car && queryCars(id, location) == 0) {
                return false;
            }

            // Check if can reserve room
            if(room && queryRooms(id, location) == 0) {
                return false;
            }

            // Start reserving
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

            // If can reserve but was not successful
            if(!success) {
                logger.warn("Query showed that the user can reserve but the reservation was not successful. " +
                        "Further investigation is required");
            }
            return success;
        }
    }

    @Override
    public int start() throws RemoteException {
        return m_tm.start();
    }

    @Override
    public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        for(ResourceManager rm : m_tm.getTransaction(transactionId).getRMs()) {
            rm.commit(transactionId);
        }
        return true;
    }

    @Override
    public void abort(int transactionId) throws RemoteException, InvalidTransactionException {
        for(ResourceManager rm : m_tm.getTransaction(transactionId).getRMs()) {
            rm.abort(transactionId);
        }
    }

    @Override
    public boolean shutdown() throws RemoteException {
        return false;
    }
}