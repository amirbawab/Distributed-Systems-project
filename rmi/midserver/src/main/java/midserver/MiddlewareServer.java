package midserver;

import inter.ResourceManager;
import inter.ServerDownException;
import lm.DeadlockException;
import lm.LockManager;
import lm.TransactionAbortedException;
import lm.TrxnObj;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tm.Transaction;
import tm.TransactionManager;

import javax.transaction.InvalidTransactionException;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Set;
import java.util.Vector;

class MiddlewareServer implements ResourceManager {

    // Sync lock
    private final static Object lockCar = new Object();
    private final static Object lockRoom = new Object();
    private final static Object lockFlight = new Object();

    // RM components
    private ResourceManager m_carRM;
    private ResourceManager m_flightRM;
    private ResourceManager m_roomRM;

    // Program exit codes
    private static final int CODE_ERROR=1;

    // Transaction manager
    private TransactionManager m_tm;

    // RM Registry
    private static Registry s_registry;

    // Logger
    private static final Logger logger = LogManager.getLogger(MiddlewareServer.class);

    // Middleware server
    private static MiddlewareServer m_ms;

    // Server info
    private static String rmRMIRegistryIP;
    private static int rmRMIRegistryPort;

    public static void main(String[] args) {

        // Figure out where server is running
        if (args.length != 3) {
            System.err.println ("Wrong usage");
            System.out.println("    Usage: java ResImpl.ResourceManagerImpl [server port] [registry ip] [registry port]");
            System.exit(MiddlewareServer.CODE_ERROR);
        }

        // Set server port
        int serverRMIRegistryPort = Integer.parseInt(args[0]);
        rmRMIRegistryIP = args[1];
        rmRMIRegistryPort = Integer.parseInt(args[2]);

        // Build a middleware server
        m_ms = bindRM(ResourceManager.MID_SERVER_REF, serverRMIRegistryPort);

        // Initialize the transaction manager
        m_ms.m_tm = new TransactionManager();

        // Connect to all RMs
        m_ms.connectToAllRm();

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
    }

    /**
     * Connect to RM
     */
    private void connectToAllRm() {
        m_ms.m_carRM = m_ms.connectToRM(ResourceManager.RM_CAR_REF, rmRMIRegistryIP, rmRMIRegistryPort);
        m_ms.m_flightRM = m_ms.connectToRM(ResourceManager.RM_FLIGHT_REF, rmRMIRegistryIP, rmRMIRegistryPort);
        m_ms.m_roomRM = m_ms.connectToRM(ResourceManager.RM_ROOM_REF, rmRMIRegistryIP, rmRMIRegistryPort);
    }

    /**
     * Middleware server constructor
     */
    public MiddlewareServer() {
        final int SLEEP_TIME = 1000;
        final int MAX_IDLE_TIME = 60000;

        // Abort old transactions
        new Thread(() -> {
            try {
                while (true) {
                    if(MiddlewareServer.this.m_tm != null) {
                        for(Transaction tx : MiddlewareServer.this.m_tm.getTransactions()) {
                            if(tx.getIdleTime() > MAX_IDLE_TIME) {
                                abort(tx.getXID());
                            }
                        }
                    }
                    Thread.sleep(SLEEP_TIME);
                }
            } catch (RemoteException | InterruptedException e) {
                logger.error("Exception occurred in abort thread. Long transactions will not be auto-aborted");
            }
        }).start();
    }

    private static MiddlewareServer bindRM(String key, int port) {
        MiddlewareServer obj = new MiddlewareServer();
        final int BIND_SLEEP = 5000;

        ResourceManager rm = null;
        while(true) {
            // Bind server object
            try {

                // Export one time only
                if(rm == null) {
                    // Create a new Server object
                    // Dynamically generate the stub (client proxy)
                    rm = (ResourceManager) UnicastRemoteObject.exportObject(obj, 0);
                }

                // Bind the remote object's stub in the registry
                s_registry = LocateRegistry.getRegistry(port);
                s_registry.rebind(key, rm);
                logger.info("Middleware server ready!");
                return obj;
            } catch (Exception e) {
                logger.error("Failed to bind object: " + e.toString());
                try {
                    logger.info("Trying again in " + BIND_SLEEP + " ms");
                    Thread.sleep(BIND_SLEEP);
                } catch (InterruptedException e1) {
                    logger.error("Failed to put thread to sleep. Message: " + e1.getMessage());
                }
            }
        }
    }

    /**
     * Connect to RM
     * @param key
     */
    private ResourceManager connectToRM(String key, String server, int port) {
        final int CONNECT_SLEEP = 5000;
        while (true) {
            try {

                // Connect to registry
                Registry registry = LocateRegistry.getRegistry(server, port);

                // Lookup resource manager
                ResourceManager rm = (ResourceManager) registry.lookup(key);

                // Verify connection is working
                rm.healthCheck();

                // Sync transactions
                rm.syncTransactions(m_tm.getTransactionsId());
                logger.info("Connected to RM: " + key);
                return rm;
            } catch (Exception e) {
                logger.error("Exception while connecting to RM " + key + ". Message"+ e.toString());
                try {
                    logger.info("Trying again in " + CONNECT_SLEEP + " ms");
                    Thread.sleep(CONNECT_SLEEP);
                } catch (InterruptedException e1) {
                    logger.error("Failed to put thread to sleep. Message: " + e1.getMessage());
                }
            }
        }
    }

    /**
     * Execute code on RM crash
     */
    public void onRMCrash() {
        logger.info("One or more RM crashed. RM health check will be performed");
        connectToAllRm();
    }

    /****************************
     *      CLIENT ACTIONS
     ***************************/

    @Override
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
            throws RemoteException {
        synchronized (lockFlight) {
            try {
                m_tm.getTransaction(id).updateLastActive();
                m_tm.getTransaction(id).addRM(ResourceManager.RM_FLIGHT_REF);
                return m_flightRM.addFlight(m_tm.getTransaction(id).getXID(), flightNum, flightSeats, flightPrice);
            } catch (DeadlockException e) {
                logger.error(e.getMessage());
                abort(e.GetXId());
                throw new InvalidTransactionException("Request aborted: Deadlock detected ...");
            } catch (RemoteException e) {
                onRMCrash();
                abort(id);
                throw new ServerDownException();
            }
        }
    }

    @Override
    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException {
        synchronized (lockCar) {
            try {
                m_tm.getTransaction(id).updateLastActive();
                m_tm.getTransaction(id).addRM(ResourceManager.RM_CAR_REF);
                return m_carRM.addCars(m_tm.getTransaction(id).getXID(), location, numCars, price);
            } catch (DeadlockException e) {
                logger.error(e.getMessage());
                abort(e.GetXId());
                throw new InvalidTransactionException("Request aborted: Deadlock detected ...");
            } catch (RemoteException e) {
                onRMCrash();
                abort(id);
                throw new ServerDownException();
            }
        }
    }

    @Override
    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException {
        synchronized (lockRoom){
            try {
                m_tm.getTransaction(id).updateLastActive();
                m_tm.getTransaction(id).addRM(ResourceManager.RM_ROOM_REF);
                return m_roomRM.addRooms(m_tm.getTransaction(id).getXID(), location, numRooms, price);
            } catch (DeadlockException e) {
                logger.error(e.getMessage());
                abort(e.GetXId());
                throw new InvalidTransactionException("Request aborted: Deadlock detected ...");
            } catch (RemoteException e) {
                onRMCrash();
                abort(id);
                throw new ServerDownException();
            }
        }
    }

    @Override
    public int newCustomer(int id) throws RemoteException {
        synchronized (lockRoom) {
            synchronized (lockCar) {
                synchronized (lockFlight) {
                    try {
                        m_tm.getTransaction(id).updateLastActive();
                        m_tm.getTransaction(id).addRM(ResourceManager.RM_ROOM_REF);
                        m_tm.getTransaction(id).addRM(ResourceManager.RM_CAR_REF);
                        m_tm.getTransaction(id).addRM(ResourceManager.RM_FLIGHT_REF);
                        int cid = m_carRM.newCustomer(m_tm.getTransaction(id).getXID());
                        m_roomRM.newCustomer(id, cid);
                        m_flightRM.newCustomer(id, cid);
                        return cid;
                    } catch (DeadlockException e) {
                        logger.error(e.getMessage());
                        abort(e.GetXId());
                        throw new InvalidTransactionException("Request aborted: Deadlock detected ...");
                    } catch (RemoteException e) {
                        onRMCrash();
                        abort(id);
                        throw new ServerDownException();
                    }
                }
            }
        }
    }

    @Override
    public boolean newCustomer(int id, int cid) throws RemoteException {
        synchronized (lockRoom) {
            synchronized (lockCar) {
                synchronized (lockFlight) {
                    try {
                        id = m_tm.getTransaction(id).getXID();
                        m_tm.getTransaction(id).updateLastActive();
                        m_tm.getTransaction(id).addRM(ResourceManager.RM_FLIGHT_REF);
                        m_tm.getTransaction(id).addRM(ResourceManager.RM_CAR_REF);
                        m_tm.getTransaction(id).addRM(ResourceManager.RM_ROOM_REF);
                        boolean flight = m_flightRM.newCustomer(id, cid);
                        boolean car = m_carRM.newCustomer(id, cid);
                        boolean room = m_roomRM.newCustomer(id, cid);
                        return flight && car && room;
                    } catch (DeadlockException e) {
                        logger.error(e.getMessage());
                        abort(e.GetXId());
                        throw new InvalidTransactionException("Request aborted: Deadlock detected ...");
                    } catch (RemoteException e) {
                        onRMCrash();
                        abort(id);
                        throw new ServerDownException();
                    }
                }
            }
        }
    }

    @Override
    public boolean deleteFlight(int id, int flightNum) throws RemoteException {
        synchronized (lockFlight) {
            try {
                m_tm.getTransaction(id).updateLastActive();
                m_tm.getTransaction(id).addRM(ResourceManager.RM_FLIGHT_REF);
                return m_flightRM.deleteFlight(m_tm.getTransaction(id).getXID(), flightNum);
            } catch (DeadlockException e) {
                logger.error(e.getMessage());
                abort(e.GetXId());
                throw new InvalidTransactionException("Request aborted: Deadlock detected ...");
            } catch (RemoteException e) {
                onRMCrash();
                abort(id);
                throw new ServerDownException();
            }
        }
    }

    @Override
    public boolean deleteCars(int id, String location) throws RemoteException {
        synchronized (lockCar) {
            try {
                m_tm.getTransaction(id).updateLastActive();
                m_tm.getTransaction(id).addRM(ResourceManager.RM_CAR_REF);
                return m_carRM.deleteCars(m_tm.getTransaction(id).getXID(), location);
            } catch (DeadlockException e) {
                logger.error(e.getMessage());
                abort(e.GetXId());
                throw new InvalidTransactionException("Request aborted: Deadlock detected ...");
            } catch (RemoteException e) {
                onRMCrash();
                abort(id);
                throw new ServerDownException();
            }
        }
    }

    @Override
    public boolean deleteRooms(int id, String location) throws RemoteException {
        synchronized (lockRoom) {
            try {
                m_tm.getTransaction(id).updateLastActive();
                m_tm.getTransaction(id).addRM(ResourceManager.RM_ROOM_REF);
                return m_roomRM.deleteRooms(m_tm.getTransaction(id).getXID(), location);
            } catch (DeadlockException e) {
                logger.error(e.getMessage());
                abort(e.GetXId());
                throw new InvalidTransactionException("Request aborted: Deadlock detected ...");
            } catch (RemoteException e) {
                onRMCrash();
                abort(id);
                throw new ServerDownException();
            }
        }
    }

    @Override
    public boolean deleteCustomer(int id, int customer) throws RemoteException {
        synchronized (lockRoom) {
            synchronized (lockCar) {
                synchronized (lockFlight) {
                    try {
                        id = m_tm.getTransaction(id).getXID();
                        m_tm.getTransaction(id).updateLastActive();
                        m_tm.getTransaction(id).addRM(ResourceManager.RM_ROOM_REF);
                        m_tm.getTransaction(id).addRM(ResourceManager.RM_CAR_REF);
                        m_tm.getTransaction(id).addRM(ResourceManager.RM_FLIGHT_REF);
                        boolean room = m_roomRM.deleteCustomer(id, customer);
                        boolean car = m_carRM.deleteCustomer(id, customer);
                        boolean flight = m_flightRM.deleteCustomer(id, customer);
                        return room && car && flight;
                    } catch (DeadlockException e) {
                        logger.error(e.getMessage());
                        abort(e.GetXId());
                        throw new InvalidTransactionException("Request aborted: Deadlock detected ...");
                    } catch (RemoteException e) {
                        onRMCrash();
                        abort(id);
                        throw new ServerDownException();
                    }
                }
            }
        }
    }

    @Override
    public int queryFlight(int id, int flightNumber) throws RemoteException {
        synchronized (lockFlight) {
            try {
                m_tm.getTransaction(id).updateLastActive();
                m_tm.getTransaction(id).addRM(ResourceManager.RM_FLIGHT_REF);
                return m_flightRM.queryFlight(m_tm.getTransaction(id).getXID(), flightNumber);
            } catch (DeadlockException e) {
                logger.error(e.getMessage());
                abort(e.GetXId());
                throw new InvalidTransactionException("Request aborted: Deadlock detected ...");
            } catch (RemoteException e) {
                onRMCrash();
                abort(id);
                throw new ServerDownException();
            }
        }
    }

    @Override
    public int queryCars(int id, String location) throws RemoteException {
        synchronized (lockCar) {
            try {
                m_tm.getTransaction(id).addRM(ResourceManager.RM_CAR_REF);
                return m_carRM.queryCars(m_tm.getTransaction(id).getXID(), location);
            } catch (DeadlockException e) {
                logger.error(e.getMessage());
                abort(e.GetXId());
                throw new InvalidTransactionException("Request aborted: Deadlock detected ...");
            } catch (RemoteException e) {
                onRMCrash();
                abort(id);
                throw new ServerDownException();
            }
        }
    }

    @Override
    public int queryRooms(int id, String location) throws RemoteException {
        synchronized (lockRoom) {
            try {
                m_tm.getTransaction(id).updateLastActive();
                m_tm.getTransaction(id).addRM(ResourceManager.RM_ROOM_REF);
                return m_roomRM.queryRooms(m_tm.getTransaction(id).getXID(), location);
            } catch (DeadlockException e) {
                logger.error(e.getMessage());
                abort(e.GetXId());
                throw new InvalidTransactionException("Request aborted: Deadlock detected ...");
            } catch (RemoteException e) {
                onRMCrash();
                abort(id);
                throw new ServerDownException();
            }
        }
    }

    @Override
    public String queryCustomerInfo(int id, int customer) throws RemoteException {
        synchronized (lockRoom) {
            synchronized (lockCar) {
                synchronized (lockFlight) {
                    try {
                        m_tm.getTransaction(id).updateLastActive();
                        m_tm.getTransaction(id).addRM(ResourceManager.RM_CAR_REF);
                        m_tm.getTransaction(id).addRM(ResourceManager.RM_FLIGHT_REF);
                        m_tm.getTransaction(id).addRM(ResourceManager.RM_ROOM_REF);
                        StringBuilder sb = new StringBuilder();
                        id = m_tm.getTransaction(id).getXID();
                        sb.append("\nCar info:\n").append(m_carRM.queryCustomerInfo(id, customer))
                                .append("\nFlight info:\n").append(m_flightRM.queryCustomerInfo(id, customer))
                                .append("\nRoom info:\n").append(m_roomRM.queryCustomerInfo(id, customer));
                        return sb.toString();
                    } catch (DeadlockException e) {
                        logger.error(e.getMessage());
                        abort(e.GetXId());
                        throw new InvalidTransactionException("Request aborted: Deadlock detected ...");
                    } catch (RemoteException e) {
                        onRMCrash();
                        abort(id);
                        throw new ServerDownException();
                    }
                }
            }
        }
    }

    @Override
    public int queryFlightPrice(int id, int flightNumber) throws RemoteException {
        synchronized (lockFlight) {
            try {
                m_tm.getTransaction(id).updateLastActive();
                m_tm.getTransaction(id).addRM(ResourceManager.RM_FLIGHT_REF);
                return m_flightRM.queryFlightPrice(m_tm.getTransaction(id).getXID(), flightNumber);
            } catch (DeadlockException e) {
                logger.error(e.getMessage());
                abort(e.GetXId());
                throw new InvalidTransactionException("Request aborted: Deadlock detected ...");
            } catch (RemoteException e) {
                onRMCrash();
                abort(id);
                throw new ServerDownException();
            }
        }
    }

    @Override
    public int queryCarsPrice(int id, String location) throws RemoteException {
        synchronized (lockCar) {
            try {
                m_tm.getTransaction(id).updateLastActive();
                m_tm.getTransaction(id).addRM(ResourceManager.RM_CAR_REF);
                return m_carRM.queryCarsPrice(m_tm.getTransaction(id).getXID(), location);
            } catch (DeadlockException e) {
                logger.error(e.getMessage());
                abort(e.GetXId());
                throw new InvalidTransactionException("Request aborted: Deadlock detected ...");
            } catch (RemoteException e) {
                onRMCrash();
                abort(id);
                throw new ServerDownException();
            }
        }
    }

    @Override
    public int queryRoomsPrice(int id, String location) throws RemoteException {
        synchronized (lockRoom) {
            try {
                m_tm.getTransaction(id).updateLastActive();
                m_tm.getTransaction(id).addRM(ResourceManager.RM_ROOM_REF);
                return m_roomRM.queryRoomsPrice(m_tm.getTransaction(id).getXID(), location);
            } catch (DeadlockException e) {
                logger.error(e.getMessage());
                abort(e.GetXId());
                throw new InvalidTransactionException("Request aborted: Deadlock detected ...");
            } catch (RemoteException e) {
                onRMCrash();
                abort(id);
                throw new ServerDownException();
            }
        }
    }

    @Override
    public boolean reserveFlight(int id, int customer, int flightNumber) throws RemoteException {
        synchronized (lockFlight) {
            try {
                m_tm.getTransaction(id).updateLastActive();
                m_tm.getTransaction(id).addRM(ResourceManager.RM_FLIGHT_REF);
                return m_flightRM.reserveFlight(m_tm.getTransaction(id).getXID(), customer, flightNumber);
            } catch (DeadlockException e) {
                logger.error(e.getMessage());
                abort(e.GetXId());
                throw new InvalidTransactionException("Request aborted: Deadlock detected ...");
            } catch (RemoteException e) {
                onRMCrash();
                abort(id);
                throw new ServerDownException();
            }
        }
    }

    @Override
    public boolean reserveCar(int id, int customer, String location) throws RemoteException {
        synchronized (lockCar) {
            try {
                m_tm.getTransaction(id).updateLastActive();
                m_tm.getTransaction(id).addRM(ResourceManager.RM_CAR_REF);
                return m_carRM.reserveCar(m_tm.getTransaction(id).getXID(), customer, location);
            } catch (DeadlockException e) {
                logger.error(e.getMessage());
                abort(e.GetXId());
                throw new InvalidTransactionException("Request aborted: Deadlock detected ...");
            } catch (RemoteException e) {
                onRMCrash();
                abort(id);
                throw new ServerDownException();
            }
        }
    }

    @Override
    public boolean reserveRoom(int id, int customer, String locationd) throws RemoteException {
        synchronized (lockRoom) {
            try {
                m_tm.getTransaction(id).updateLastActive();
                m_tm.getTransaction(id).addRM(ResourceManager.RM_ROOM_REF);
                return m_roomRM.reserveRoom(m_tm.getTransaction(id).getXID(), customer, locationd);
            } catch (DeadlockException e) {
                logger.error(e.getMessage());
                abort(e.GetXId());
                throw new InvalidTransactionException("Request aborted: Deadlock detected ...");
            } catch (RemoteException e) {
                onRMCrash();
                abort(id);
                throw new ServerDownException();
            }
        }
    }

    @Override
    public boolean itinerary(int id, int customer, Vector flightNumbers, String location, boolean car, boolean room)
            throws RemoteException {
        synchronized (lockRoom) {
            synchronized (lockCar) {
                synchronized (lockFlight) {
                    try {
                        id = m_tm.getTransaction(id).getXID();
                        m_tm.getTransaction(id).updateLastActive();
                        m_tm.getTransaction(id).addRM(ResourceManager.RM_FLIGHT_REF);
                        m_tm.getTransaction(id).addRM(ResourceManager.RM_CAR_REF);
                        m_tm.getTransaction(id).addRM(ResourceManager.RM_ROOM_REF);

                        // Check if can reserve flight
                        for (Object fNum : flightNumbers) {
                            if (queryFlight(id, Integer.parseInt(fNum.toString())) == 0) {
                                return false;
                            }
                        }

                        // Check if can reserve a car
                        if (car && queryCars(id, location) == 0) {
                            return false;
                        }

                        // Check if can reserve room
                        if (room && queryRooms(id, location) == 0) {
                            return false;
                        }

                        // Start reserving
                        boolean success = true;

                        // Reserve flights
                        for (Object fNum : flightNumbers) {
                            success &= reserveFlight(id, customer, Integer.parseInt(fNum.toString()));
                        }

                        // If should reserve a car
                        if (car) {
                            success &= reserveCar(id, customer, location);
                        }

                        // If should reserve a room
                        if (room) {
                            success &= reserveRoom(id, customer, location);
                        }

                        // If can reserve but was not successful
                        if (!success) {
                            logger.warn("Query showed that the user can reserve but the reservation was not successful. " +
                                    "Further investigation is required");
                        }
                        return success;
                    } catch (DeadlockException e) {
                        logger.error(e.getMessage());
                        abort(e.GetXId());
                        throw new InvalidTransactionException("Request aborted: Deadlock detected ...");
                    } catch (RemoteException e) {
                        onRMCrash();
                        abort(id);
                        throw new ServerDownException();
                    }
                }
            }
        }
    }

    @Override
    public int start() throws RemoteException {
        int transactionId = m_tm.start();
        logger.info("Started a new transaction with id: " + transactionId);
        return transactionId;
    }

    @Override
    public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        logger.info("Received a commit request on transaction " + transactionId);
        m_tm.getTransaction(transactionId).updateLastActive();

        // 2PC
        logger.info("Applying 2 phase commit on all involved RMs");
        if(!voteRequest(transactionId)) {
            abort(transactionId);
            return false;
        }

        // Apply commits
        for(String rmStr : m_tm.getTransaction(transactionId).getRMs()) {
            ResourceManager rm = null;
            String name = "UNKNOWN";
            if(rmStr.equals(ResourceManager.RM_CAR_REF)) {
                name = "Car";
                rm = m_carRM;
            } else if(rmStr.equals(ResourceManager.RM_FLIGHT_REF)) {
                name = "Flight";
                rm = m_flightRM;
            } else if(rmStr.equals(ResourceManager.RM_ROOM_REF)) {
                name = "Room";
                rm = m_roomRM;
            }
            logger.info("Commit on RM " + name);
            rm.commit(transactionId);
        }
        m_tm.removeTransaction(transactionId);
        return true;
    }

    @Override
    public void abort(int transactionId) throws RemoteException, InvalidTransactionException {
        logger.info("Aborting transaction " + transactionId);
        m_tm.getTransaction(transactionId).updateLastActive();
        for(String rmStr : m_tm.getTransaction(transactionId).getRMs()) {
            if(rmStr.equals(ResourceManager.RM_ROOM_REF)) {
                m_roomRM.abort(transactionId);
            } else if(rmStr.equals(ResourceManager.RM_FLIGHT_REF)) {
                m_flightRM.abort(transactionId);
            } else if(rmStr.equals(ResourceManager.RM_CAR_REF)) {
                m_carRM.abort(transactionId);
            } else {
                throw new RuntimeException("Unknow resource manager");
            }
        }
        m_tm.removeTransaction(transactionId);
    }

    @Override
    public boolean shutdown() throws RemoteException {
        if(m_tm.getTransactions().isEmpty()){
            for(String key : s_registry.list()) {
                try {
                    s_registry.unbind(key);
                    UnicastRemoteObject.unexportObject(this, true);
                } catch (NotBoundException e) {
                    logger.error("Error unbinding remote object with key: " + key);
                    return false;
                }
            }
            boolean flight = m_flightRM.shutdown();
            boolean car = m_carRM.shutdown();
            boolean room = m_roomRM.shutdown();
            if(flight && car && room) {
                logger.info("All RMs are shutdown. Shutting down middleware server ...");
            } else {
                logger.error("Some RMs failed to shutdown");
            }
            return flight && car && room;
        } else {
            logger.info("Will not shutdown because there are still transactions");
        }
        return false;
    }

    @Override
    public boolean voteRequest(int tid) throws RemoteException {
        for(String rmStr : m_tm.getTransaction(tid).getRMs()) {
            String name = "UNKNOWN";
            ResourceManager rm = null;
            if(rmStr.equals(ResourceManager.RM_CAR_REF)) {
                name = "Car";
                rm = m_carRM;
            } else if(rmStr.equals(ResourceManager.RM_FLIGHT_REF)) {
                name = "Flight";
                rm = m_flightRM;
            } else if(rmStr.equals(ResourceManager.RM_ROOM_REF)) {
                name = "Room";
                rm = m_roomRM;
            } else {
                throw new RuntimeException("Unknown resource manager");
            }
            boolean vr = rm.voteRequest(tid);
            logger.info("RM " + name + " replied with a " + (vr ? "YES" : "NO"));
            if(!vr) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void healthCheck() throws RemoteException {
        /*Do nothing*/
    }

    @Override
    public void syncTransactions(Set<Integer> transactionsId) throws RemoteException {
        /*Do nothing*/
    }
}