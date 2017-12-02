package midserver;

import inter.RMTimeOutException;
import inter.ResourceManager;
import inter.RMServerDownException;
import inter.TMException;
import lm.DeadlockException;
import lm.TransactionAbortedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tm.Transaction;
import tm.TransactionManager;

import javax.transaction.InvalidTransactionException;
import java.io.*;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

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

    // Function call constants
    private final int RF_COMMIT = 1;
    private final int RF_ABORT = 2;

    // Crash component constants
    private final String COMP_MS = "ms";
    private final String COMP_FLIGHT = "flight";
    private final String COMP_ROOM = "room";
    private final String COMP_CAR = "car";
    private final String COMP_TM = "tm";

    // Decision
    private final String DECISION_COMMIT = "commit";
    private final String DECISION_ABORT = "abort";

    // Recover function call
    private HashMap<Integer, Integer> m_recoverFunction;

    // RM function call
    private HashMap<String, Set<Integer>> m_RMFunction;

    // Crash case
    private boolean[] m_crashCase = new boolean[ResourceManager.CC_TOTAL];

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
        m_ms.m_recoverFunction = new HashMap<>();
        m_ms.m_RMFunction = new HashMap<>();

        // Try to load TM
        m_ms.loadTM();

        // Connect to all RMs
        while (true) {
            try {
                m_ms.connectToAllRm();
                break;
            } catch (RMTimeOutException e) {
                logger.info("RM " + e.getName() + " timed out. Will keep trying because the MS just started ...");
            }
        }

        // Try to load RF & RMF
        m_ms.loadRF();
        m_ms.loadRMF();

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
    }

    /**
     * Connect to RM
     */
    private void connectToAllRm() throws RMTimeOutException {
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
        Thread abortThread =new Thread(() -> {
            try {
                while (true) {
                    if(MiddlewareServer.this.m_tm != null) {
                        for(Transaction tx : MiddlewareServer.this.m_tm.getTransactions()) {
                            if(tx.getIdleTime() > MAX_IDLE_TIME && !m_recoverFunction.containsKey(tx.getXID())) {
                                abort(tx.getXID());
                            }
                        }
                    }
                    Thread.sleep(SLEEP_TIME);
                }
            } catch (RemoteException | InterruptedException e) {
                logger.error("Exception occurred in abort thread. Long transactions will not be auto-aborted");
            }
        });
        abortThread.setName("Abort-thread");
        abortThread.start();
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
    private ResourceManager connectToRM(String key, String server, int port) throws RMTimeOutException {
        final int CONNECT_SLEEP = 5000;
        final int MAX_TRIALS = 5;
        int count = 0;
        while (count++ < MAX_TRIALS) {
            try {

                // Connect to registry
                Registry registry = LocateRegistry.getRegistry(server, port);

                // Lookup resource manager
                ResourceManager rm = (ResourceManager) registry.lookup(key);

                // Verify connection is working
                rm.healthCheck();

                // Apply all missed commit/abort
                RMFapply(key);

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
        throw new RMTimeOutException(key);
    }

    /**
     * Load TM from file
     */
    private synchronized void loadTM() {
        File tmFile = TransactionManager.getTMFile();
        if(tmFile.exists()) {
            try (FileInputStream fis = new FileInputStream(tmFile); ObjectInputStream ois = new ObjectInputStream(fis)){
                m_tm = (TransactionManager) ois.readObject();
                logger.info("TM file " + tmFile.getAbsolutePath() + " loaded");
            } catch (ClassNotFoundException | IOException e) {
                logger.error("Error loading file " + tmFile.getAbsolutePath() + ". Message: " + e.getMessage());
            }
        } else {
            logger.info("TM did not find a TM file to load. Starting empty");
        }
    }

    /**
     * Commit RF
     * @param tid
     */
    public void commitRF(int tid) {
        m_recoverFunction.put(tid, RF_COMMIT);
        writeRF();
    }

    /**
     * Abort RF
     * @param tid
     */
    public void abortRF(int tid) {
        m_recoverFunction.put(tid, RF_ABORT);
        writeRF();
    }

    /**
     * Deleted RF
     * @param tid
     */
    public void deleteRF(int tid) {
        m_recoverFunction.remove(tid);
        writeRF();
    }

    /**
     * Get the Recovery file
     * @return Recovery file
     */
    public File getRFFile() {
        return new File("RF_table");
    }

    /**
     * Get the RM file
     * @return RM file
     */
    public File getRMFFile() {
        return new File("RMF_table");
    }

    /**
     * Write transaction manager to file
     */
    private synchronized void writeRMF() {
        File rmfFile = getRMFFile();
        try(FileOutputStream fos = new FileOutputStream(rmfFile); ObjectOutputStream obj = new ObjectOutputStream(fos)) {
            obj.writeObject(m_RMFunction);
            logger.info("File " + rmfFile.getAbsolutePath() + " updated!");
        } catch (IOException e) {
            logger.error("Error writing file " + rmfFile.getAbsolutePath() + ". Message: " + e.getMessage());
        }
    }

    /**
     * Write transaction manager to file
     */
    private synchronized void writeRF() {
        File rfFile = getRFFile();
        try(FileOutputStream fos = new FileOutputStream(rfFile); ObjectOutputStream obj = new ObjectOutputStream(fos)) {
            obj.writeObject(m_recoverFunction);
            logger.info("File " + rfFile.getAbsolutePath() + " updated!");
        } catch (IOException e) {
            logger.error("Error writing file " + rfFile.getAbsolutePath() + ". Message: " + e.getMessage());
        }
    }

    /**
     * Load recovery function from file
     */
    private synchronized void loadRF() {
        File rfFile = getRFFile();
        if(rfFile.exists()) {
            try (FileInputStream fis = new FileInputStream(rfFile); ObjectInputStream ois = new ObjectInputStream(fis)){
                m_recoverFunction = (HashMap) ois.readObject();
                logger.info("RF file " + rfFile.getAbsolutePath() + " loaded");
            } catch (ClassNotFoundException | IOException e) {
                logger.error("Error loading file " + rfFile.getAbsolutePath() + ". Message: " + e.getMessage());
            }
        } else {
            logger.info("RF did not find a RF file to load. Starting empty");
        }

        // Reapply function on transactions
        for(int tid : m_recoverFunction.keySet()) {
            try {
                if(m_recoverFunction.get(tid) == RF_COMMIT) {
                    commit(tid);
                } else if(m_recoverFunction.get(tid) == RF_ABORT) {
                    abort(tid);
                }
            } catch (Exception e) {
                logger.error("Error recovering commit/abort function call for transaction " + tid +
                        ". Message: " + e.getMessage());
            }
        }
    }

    /**
     * Load RM function from file
     */
    private synchronized void loadRMF() {
        File rmfFile = getRMFFile();
        if(rmfFile.exists()) {
            try (FileInputStream fis = new FileInputStream(rmfFile); ObjectInputStream ois = new ObjectInputStream(fis)){
                m_RMFunction = (HashMap) ois.readObject();
                logger.info("RMF file " + rmfFile.getAbsolutePath() + " loaded");
            } catch (ClassNotFoundException | IOException e) {
                logger.error("Error loading file " + rmfFile.getAbsolutePath() + ". Message: " + e.getMessage());
            }
        } else {
            logger.info("RMF did not find a RMF file to load. Starting empty");
        }

    }

    private void RMFapply(String rmName) {

        // Reapply function on transactions
        List<String> keyList = Arrays.asList(rmName + "_" + DECISION_ABORT, rmName + "_" + DECISION_COMMIT);
        for(String key: keyList) {
            String[] keyArray = key.split("_");
            String rm = keyArray[0];
            String decision = keyArray[1];
            if(!m_RMFunction.containsKey(key)) {
                continue;
            }
            Set<Integer> valueSet = new HashSet<>(m_RMFunction.get(key));
            ResourceManager rmObj = null;
            if(rm.equals(RM_CAR_REF)) {
                rmObj = m_carRM;
            } else if(rm.equals(RM_FLIGHT_REF)) {
                rmObj = m_flightRM;
            } else if(rm.equals(RM_ROOM_REF)) {
                rmObj = m_roomRM;
            } else {
                throw new RuntimeException("Unknown resource manager");
            }

            for(Integer tid : valueSet) {
                try {
                    if (decision.equals(DECISION_COMMIT)) {
                        rmObj.commit(tid);
                    } else if(decision.equals(DECISION_ABORT)) {
                        rmObj.abort(tid);
                    } else {
                        throw new RuntimeException("Unknown decision");
                    }
                    unbufferDecision(rm, tid, decision);
                    break;
                }catch (RemoteException e) {
                    // Do nothing because RM is down already
                }
            }
        }
    }

    /**
     * Execute code on RM crash
     */
    public void onRMCrash(String name) throws RMTimeOutException {
        logger.error("RM " + name + " crashed. RM health check will be performed");
        connectToAllRm();
    }

    /**
     * Get decision key
     * @param rm
     * @param decision
     * @return
     */
    private String getBufferedKey(String rm, String decision) {
        switch (decision) {
            case DECISION_ABORT:
            case DECISION_COMMIT:
                return rm + "_" + decision;
            default:
                throw new RuntimeException("Unknown decision");
        }
    }

    /**
     * Buffer decision
     * @param rm
     * @param tid
     */
    private void bufferDecision(String rm, int tid, String decision) {
        String key = getBufferedKey(rm, decision);
        if(!m_RMFunction.containsKey(key)) {
            m_RMFunction.put(key, new HashSet<>());
        }
        logger.info("Buffering commit decision on transaction " + tid + " for RM " + rm);
        m_RMFunction.get(key).add(tid);
        writeRMF();
    }

    /**
     * Unbuffer decision
     * @param rm
     * @param tid
     */
    private void unbufferDecision(String rm, int tid, String decision) {
        String key = getBufferedKey(rm, decision);
        if(m_RMFunction.containsKey(key) && m_RMFunction.get(key).contains(tid)) {
            logger.info("Unbuffering commit decision on transaction " + tid + " for RM " + rm);
            m_RMFunction.get(key).remove(tid);
            writeRMF();
        }
    }

    /****************************
     *      CLIENT ACTIONS
     ***************************/

    @Override
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
            throws RemoteException {
        synchronized (lockFlight) {
            try {
                m_tm.updateLastActive(id);
                m_tm.addRM(id, ResourceManager.RM_FLIGHT_REF);
            } catch (NullPointerException e) {
                throw new TMException();
            } catch (InvalidTransactionException e){
                throw e;
            }

            while (true) {
                try {
                    return m_flightRM.addFlight(id, flightNum, flightSeats, flightPrice);
                } catch (DeadlockException e) {
                    logger.error(e.getMessage());
                    abort(e.GetXId());
                    throw e;
                } catch (RemoteException e) {
                    try {
                        onRMCrash(RM_FLIGHT_REF);
                    } catch (RMTimeOutException e1) {
                        abort(id);
                        throw new RMServerDownException();
                    }
                }
            }
        }
    }

    @Override
    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException {
        synchronized (lockCar) {
            try {
                m_tm.updateLastActive(id);
                m_tm.addRM(id, ResourceManager.RM_CAR_REF);
            } catch (NullPointerException e) {
                throw new TMException();
            } catch (InvalidTransactionException e) {
                throw e;
            }

            while (true) {
                try {
                    return m_carRM.addCars(id, location, numCars, price);
                } catch (DeadlockException e) {
                    logger.error(e.getMessage());
                    abort(e.GetXId());
                    throw e;
                } catch (RemoteException e) {
                    try {
                        onRMCrash(RM_CAR_REF);
                    } catch (RMTimeOutException e1) {
                        abort(id);
                        throw new RMServerDownException();
                    }
                }
            }
        }
    }

    @Override
    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException {
        synchronized (lockRoom){
            try {
                m_tm.updateLastActive(id);
                m_tm.addRM(id, ResourceManager.RM_ROOM_REF);
            } catch (NullPointerException e) {
                throw new TMException();
            } catch (InvalidTransactionException e) {
                throw e;
            }

            while (true) {
                try {
                    return m_roomRM.addRooms(id, location, numRooms, price);
                } catch (DeadlockException e) {
                    logger.error(e.getMessage());
                    abort(e.GetXId());
                    throw e;
                } catch (RemoteException e) {
                    try {
                        onRMCrash(RM_ROOM_REF);
                    } catch (RMTimeOutException e1) {
                        abort(id);
                        throw new RMServerDownException();
                    }
                }
            }
        }
    }

    @Override
    public int newCustomer(int id) throws RemoteException {
        synchronized (lockRoom) {
            synchronized (lockCar) {
                synchronized (lockFlight) {
                    try {
                        try {
                            m_tm.updateLastActive(id);
                            m_tm.addRM(id, ResourceManager.RM_ROOM_REF);
                            m_tm.addRM(id, ResourceManager.RM_CAR_REF);
                            m_tm.addRM(id, ResourceManager.RM_FLIGHT_REF);
                        } catch (NullPointerException e) {
                            throw new TMException();
                        } catch (InvalidTransactionException e) {
                            throw e;
                        }

                        int cid = 0;
                        while (true) {
                            try {
                                cid = m_carRM.newCustomer(id);
                                break;
                            } catch (RemoteException e) {
                                try {
                                    onRMCrash(RM_CAR_REF);
                                } catch (RMTimeOutException e1) {
                                    abort(id);
                                    throw new RMServerDownException();
                                }
                            }
                        }

                        while (true) {
                            try {
                                m_roomRM.newCustomer(id, cid);
                                break;
                            } catch (RemoteException e) {
                                try {
                                    onRMCrash(RM_ROOM_REF);
                                } catch (RMTimeOutException e1) {
                                    abort(id);
                                    throw new RMServerDownException();
                                }
                            }
                        }

                        while (true) {
                            try {
                                m_flightRM.newCustomer(id, cid);
                                break;
                            } catch (RemoteException e) {
                                try {
                                    onRMCrash(RM_FLIGHT_REF);
                                } catch (RMTimeOutException e1) {
                                    abort(id);
                                    throw new RMServerDownException();
                                }
                            }
                        }

                        return cid;
                    } catch (DeadlockException e) {
                        logger.error(e.getMessage());
                        abort(e.GetXId());
                        throw e;
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
                        try {
                            m_tm.updateLastActive(id);
                            m_tm.addRM(id, ResourceManager.RM_ROOM_REF);
                            m_tm.addRM(id, ResourceManager.RM_CAR_REF);
                            m_tm.addRM(id, ResourceManager.RM_FLIGHT_REF);
                        } catch (NullPointerException e) {
                            throw new TMException();
                        } catch (InvalidTransactionException e) {
                            throw e;
                        }

                        boolean flight = false;
                        boolean car = false;
                        boolean room = false;
                        while (true) {
                            try {
                                car = m_carRM.newCustomer(id, cid);
                                break;
                            } catch (RemoteException e) {
                                try {
                                    onRMCrash(RM_CAR_REF);
                                } catch (RMTimeOutException e1) {
                                    abort(id);
                                    throw new RMServerDownException();
                                }
                            }
                        }

                        while (true) {
                            try {
                                room = m_roomRM.newCustomer(id, cid);
                                break;
                            } catch (RemoteException e) {
                                try {
                                    onRMCrash(RM_ROOM_REF);
                                } catch (RMTimeOutException e1) {
                                    abort(id);
                                    throw new RMServerDownException();
                                }
                            }
                        }

                        while (true) {
                            try {
                                flight = m_flightRM.newCustomer(id, cid);
                                break;
                            } catch (RemoteException e) {
                                try {
                                    onRMCrash(RM_FLIGHT_REF);
                                } catch (RMTimeOutException e1) {
                                    abort(id);
                                    throw new RMServerDownException();
                                }
                            }
                        }
                        return flight && car && room;
                    } catch (DeadlockException e) {
                        logger.error(e.getMessage());
                        abort(e.GetXId());
                        throw e;
                    }
                }
            }
        }
    }

    @Override
    public boolean deleteFlight(int id, int flightNum) throws RemoteException {
        synchronized (lockFlight) {
            try {
                m_tm.updateLastActive(id);
                m_tm.addRM(id, ResourceManager.RM_FLIGHT_REF);
            } catch (NullPointerException e) {
                throw new TMException();
            } catch (InvalidTransactionException e) {
                throw e;
            }

            while (true) {
                try {
                    return m_flightRM.deleteFlight(id, flightNum);
                } catch (DeadlockException e) {
                    logger.error(e.getMessage());
                    abort(e.GetXId());
                    throw e;
                } catch (RemoteException e) {
                    try {
                        onRMCrash(RM_FLIGHT_REF);
                    } catch (RMTimeOutException e1) {
                        abort(id);
                        throw new RMServerDownException();
                    }
                }
            }
        }
    }

    @Override
    public boolean deleteCars(int id, String location) throws RemoteException {
        synchronized (lockCar) {
            try {
                m_tm.updateLastActive(id);
                m_tm.addRM(id, ResourceManager.RM_CAR_REF);
            } catch (NullPointerException e) {
                throw new TMException();
            } catch (InvalidTransactionException e) {
                throw e;
            }

            while (true) {
                try {
                    return m_carRM.deleteCars(id, location);
                } catch (DeadlockException e) {
                    logger.error(e.getMessage());
                    abort(e.GetXId());
                    throw e;
                } catch (RemoteException e) {
                    try {
                        onRMCrash(RM_CAR_REF);
                    } catch (RMTimeOutException e1) {
                        abort(id);
                        throw new RMServerDownException();
                    }
                }
            }
        }
    }

    @Override
    public boolean deleteRooms(int id, String location) throws RemoteException {
        synchronized (lockRoom) {
            try {
                m_tm.updateLastActive(id);
                m_tm.addRM(id, ResourceManager.RM_ROOM_REF);
            } catch (NullPointerException e) {
                throw new TMException();
            } catch (InvalidTransactionException e) {
                throw e;
            }

            while (true) {
                try {
                    return m_roomRM.deleteRooms(id, location);
                } catch (DeadlockException e) {
                    logger.error(e.getMessage());
                    abort(e.GetXId());
                    throw e;
                } catch (RemoteException e) {
                    try {
                        onRMCrash(RM_ROOM_REF);
                    } catch (RMTimeOutException e1) {
                        abort(id);
                        throw new RMServerDownException();
                    }
                }
            }
        }
    }

    @Override
    public boolean deleteCustomer(int id, int customer) throws RemoteException {
        synchronized (lockRoom) {
            synchronized (lockCar) {
                synchronized (lockFlight) {
                    try {
                        try {
                            m_tm.updateLastActive(id);
                            m_tm.addRM(id, ResourceManager.RM_ROOM_REF);
                            m_tm.addRM(id, ResourceManager.RM_CAR_REF);
                            m_tm.addRM(id, ResourceManager.RM_FLIGHT_REF);
                        } catch (NullPointerException e) {
                            throw new TMException();
                        } catch (InvalidTransactionException e) {
                            throw e;
                        }

                        boolean room = false;
                        boolean car = false;
                        boolean flight = false;
                        while (true) {
                            try {
                                room = m_roomRM.deleteCustomer(id, customer);
                                break;
                            } catch (RemoteException e) {
                                try {
                                    onRMCrash(RM_ROOM_REF);
                                } catch (RMTimeOutException e1) {
                                    abort(id);
                                    throw new RMServerDownException();
                                }
                            }
                        }

                        while (true) {
                            try {
                                car = m_carRM.deleteCustomer(id, customer);
                                break;
                            } catch (RemoteException e) {
                                try {
                                    onRMCrash(RM_CAR_REF);
                                } catch (RMTimeOutException e1) {
                                    abort(id);
                                    throw new RMServerDownException();
                                }
                            }
                        }

                        while (true) {
                            try {
                                flight = m_flightRM.deleteCustomer(id, customer);
                                break;
                            } catch (RemoteException e) {
                                try {
                                    onRMCrash(RM_FLIGHT_REF);
                                } catch (RMTimeOutException e1) {
                                    abort(id);
                                    throw new RMServerDownException();
                                }
                            }
                        }

                        return room && car && flight;
                    } catch (DeadlockException e) {
                        logger.error(e.getMessage());
                        abort(e.GetXId());
                        throw e;
                    }
                }
            }
        }
    }

    @Override
    public int queryFlight(int id, int flightNumber) throws RemoteException {
        synchronized (lockFlight) {
            try {
                m_tm.updateLastActive(id);
                m_tm.addRM(id, ResourceManager.RM_FLIGHT_REF);
            } catch (NullPointerException e) {
                throw new TMException();
            } catch (InvalidTransactionException e) {
                throw e;
            }

            while (true) {
                try {
                    return m_flightRM.queryFlight(id, flightNumber);
                } catch (DeadlockException e) {
                    logger.error(e.getMessage());
                    abort(e.GetXId());
                    throw e;
                } catch (RemoteException e) {
                    try {
                        onRMCrash(RM_FLIGHT_REF);
                    } catch (RMTimeOutException e1) {
                        abort(id);
                        throw new RMServerDownException();
                    }
                }
            }
        }
    }

    @Override
    public int queryCars(int id, String location) throws RemoteException {
        synchronized (lockCar) {
            try {
                m_tm.addRM(id, ResourceManager.RM_CAR_REF);
            } catch (NullPointerException e) {
                throw new TMException();
            } catch (InvalidTransactionException e) {
                throw e;
            }

            while (true) {
                try {
                    return m_carRM.queryCars(id, location);
                } catch (DeadlockException e) {
                    logger.error(e.getMessage());
                    abort(e.GetXId());
                    throw e;
                } catch (RemoteException e) {
                    try {
                        onRMCrash(RM_CAR_REF);
                    } catch (RMTimeOutException e1) {
                        abort(id);
                        throw new RMServerDownException();
                    }
                }
            }
        }
    }

    @Override
    public int queryRooms(int id, String location) throws RemoteException {
        synchronized (lockRoom) {
            try {
                m_tm.updateLastActive(id);
                m_tm.addRM(id, ResourceManager.RM_ROOM_REF);
            } catch (NullPointerException e) {
                throw new TMException();
            } catch (InvalidTransactionException e) {
                throw e;
            }

            while (true) {
                try {
                    return m_roomRM.queryRooms(id, location);
                } catch (DeadlockException e) {
                    logger.error(e.getMessage());
                    abort(e.GetXId());
                    throw e;
                } catch (RemoteException e) {
                    try {
                        onRMCrash(RM_ROOM_REF);
                    } catch (RMTimeOutException e1) {
                        abort(id);
                        throw new RMServerDownException();
                    }
                }
            }
        }
    }

    @Override
    public String queryCustomerInfo(int id, int customer) throws RemoteException {
        synchronized (lockRoom) {
            synchronized (lockCar) {
                synchronized (lockFlight) {
                    try {
                        try {
                            m_tm.updateLastActive(id);
                            m_tm.addRM(id, ResourceManager.RM_CAR_REF);
                            m_tm.addRM(id, ResourceManager.RM_FLIGHT_REF);
                            m_tm.addRM(id, ResourceManager.RM_ROOM_REF);
                        } catch (NullPointerException e) {
                            throw new TMException();
                        } catch (InvalidTransactionException e) {
                            throw e;
                        }

                        StringBuilder sb = new StringBuilder();
                        sb.append("\nCar info:\n");
                        while (true) {
                            try {
                                sb.append(m_carRM.queryCustomerInfo(id, customer));
                                break;
                            } catch (RemoteException e) {
                                try {
                                    onRMCrash(RM_CAR_REF);
                                } catch (RMTimeOutException e1) {
                                    abort(id);
                                    throw new RMServerDownException();
                                }
                            }
                        }

                        sb.append("\nFlight info:\n");
                        while (true) {
                            try {
                                sb.append(m_flightRM.queryCustomerInfo(id, customer));
                                break;
                            } catch (RemoteException e) {
                                try {
                                    onRMCrash(RM_FLIGHT_REF);
                                } catch (RMTimeOutException e1) {
                                    abort(id);
                                    throw new RMServerDownException();
                                }
                            }
                        }

                        sb.append("\nRoom info:\n");
                        while (true) {
                            try {
                                sb.append(m_roomRM.queryCustomerInfo(id, customer));
                                break;
                            } catch (RemoteException e) {
                                try {
                                    onRMCrash(RM_ROOM_REF);
                                } catch (RMTimeOutException e1) {
                                    abort(id);
                                    throw new RMServerDownException();
                                }
                            }
                        }
                        return sb.toString();
                    } catch (DeadlockException e) {
                        logger.error(e.getMessage());
                        abort(e.GetXId());
                        throw e;
                    }
                }
            }
        }
    }

    @Override
    public int queryFlightPrice(int id, int flightNumber) throws RemoteException {
        synchronized (lockFlight) {
            try {
                m_tm.updateLastActive(id);
                m_tm.addRM(id, ResourceManager.RM_FLIGHT_REF);
            } catch (NullPointerException e) {
                throw new TMException();
            } catch (InvalidTransactionException e) {
                throw e;
            }

            while (true) {
                try {
                    return m_flightRM.queryFlightPrice(id, flightNumber);
                } catch (DeadlockException e) {
                    logger.error(e.getMessage());
                    abort(e.GetXId());
                    throw e;
                } catch (RemoteException e) {
                    try {
                        onRMCrash(RM_FLIGHT_REF);
                    } catch (RMTimeOutException e1) {
                        abort(id);
                        throw new RMServerDownException();
                    }
                }
            }
        }
    }

    @Override
    public int queryCarsPrice(int id, String location) throws RemoteException {
        synchronized (lockCar) {
            try {
                m_tm.updateLastActive(id);
                m_tm.addRM(id, ResourceManager.RM_CAR_REF);
            } catch (NullPointerException e) {
                throw new TMException();
            } catch (InvalidTransactionException e) {
                throw e;
            }

            while (true) {
                try {
                    return m_carRM.queryCarsPrice(id, location);
                } catch (DeadlockException e) {
                    logger.error(e.getMessage());
                    abort(e.GetXId());
                    throw e;
                } catch (RemoteException e) {
                    try {
                        onRMCrash(RM_CAR_REF);
                    } catch (RMTimeOutException e1) {
                        abort(id);
                        throw new RMServerDownException();
                    }
                }
            }
        }
    }

    @Override
    public int queryRoomsPrice(int id, String location) throws RemoteException {
        synchronized (lockRoom) {
            try {
                m_tm.updateLastActive(id);
                m_tm.addRM(id, ResourceManager.RM_ROOM_REF);
            } catch (NullPointerException e) {
                throw new TMException();
            } catch (InvalidTransactionException e) {
                throw e;
            }

            while (true) {
                try {
                    return m_roomRM.queryRoomsPrice(id, location);
                } catch (DeadlockException e) {
                    logger.error(e.getMessage());
                    abort(e.GetXId());
                    throw e;
                } catch (RemoteException e) {
                    try {
                        onRMCrash(RM_ROOM_REF);
                    } catch (RMTimeOutException e1) {
                        abort(id);
                        throw new RMServerDownException();
                    }
                }
            }
        }
    }

    @Override
    public boolean reserveFlight(int id, int customer, int flightNumber) throws RemoteException {
        synchronized (lockFlight) {
            try {
                m_tm.updateLastActive(id);
                m_tm.addRM(id, ResourceManager.RM_FLIGHT_REF);
            } catch (NullPointerException e) {
                throw new TMException();
            } catch (InvalidTransactionException e) {
                throw e;
            }

            while (true) {
                try {
                    return m_flightRM.reserveFlight(id, customer, flightNumber);
                } catch (DeadlockException e) {
                    logger.error(e.getMessage());
                    abort(e.GetXId());
                    throw e;
                } catch (RemoteException e) {
                    try {
                        onRMCrash(RM_FLIGHT_REF);
                    } catch (RMTimeOutException e1) {
                        abort(id);
                        throw new RMServerDownException();
                    }
                }
            }
        }
    }

    @Override
    public boolean reserveCar(int id, int customer, String location) throws RemoteException {
        synchronized (lockCar) {
            try {
                m_tm.updateLastActive(id);
                m_tm.addRM(id, ResourceManager.RM_CAR_REF);
            } catch (NullPointerException e) {
                throw new TMException();
            } catch (InvalidTransactionException e) {
                throw e;
            }

            while (true) {
                try {
                    return m_carRM.reserveCar(id, customer, location);
                } catch (DeadlockException e) {
                    logger.error(e.getMessage());
                    abort(e.GetXId());
                    throw e;
                } catch (RemoteException e) {
                    try {
                        onRMCrash(RM_CAR_REF);
                    } catch (RMTimeOutException e1) {
                        abort(id);
                        throw new RMServerDownException();
                    }
                }
            }
        }
    }

    @Override
    public boolean reserveRoom(int id, int customer, String locationd) throws RemoteException {
        synchronized (lockRoom) {
            try {
                m_tm.updateLastActive(id);
                m_tm.addRM(id, ResourceManager.RM_ROOM_REF);
            } catch (NullPointerException e) {
                throw new TMException();
            } catch (InvalidTransactionException e) {
                throw e;
            }

            while (true) {
                try {
                    return m_roomRM.reserveRoom(id, customer, locationd);
                } catch (DeadlockException e) {
                    logger.error(e.getMessage());
                    abort(e.GetXId());
                    throw e;
                } catch (RemoteException e) {
                    try {
                        onRMCrash(RM_ROOM_REF);
                    } catch (RMTimeOutException e1) {
                        abort(id);
                        throw new RMServerDownException();
                    }
                }
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
                        try {
                            m_tm.updateLastActive(id);
                            m_tm.addRM(id, ResourceManager.RM_FLIGHT_REF);
                            m_tm.addRM(id, ResourceManager.RM_CAR_REF);
                            m_tm.addRM(id, ResourceManager.RM_ROOM_REF);
                        } catch (NullPointerException e) {
                            throw new TMException();
                        } catch (InvalidTransactionException e) {
                            throw e;
                        }

                        // Check if can reserve flight
                        for (Object fNum : flightNumbers) {
                            while (true) {
                                try {
                                    if (queryFlight(id, Integer.parseInt(fNum.toString())) == 0) {
                                        return false;
                                    }
                                    break;
                                } catch (RemoteException e) {
                                    try {
                                        onRMCrash(RM_FLIGHT_REF);
                                    } catch (RMTimeOutException e1) {
                                        abort(id);
                                        throw new RMServerDownException();
                                    }
                                }
                            }
                        }

                        while (true) {
                            try {
                                if (car && queryCars(id, location) == 0) {
                                    return false;
                                }
                                break;
                            } catch (RemoteException e) {
                                try {
                                    onRMCrash(RM_CAR_REF);
                                } catch (RMTimeOutException e1) {
                                    abort(id);
                                    throw new RMServerDownException();
                                }
                            }
                        }

                        while (true) {
                            try {
                                if (room && queryRooms(id, location) == 0) {
                                    return false;
                                }
                                break;
                            } catch (RemoteException e) {
                                try {
                                    onRMCrash(RM_ROOM_REF);
                                } catch (RMTimeOutException e1) {
                                    abort(id);
                                    throw new RMServerDownException();
                                }
                            }
                        }


                        // Start reserving
                        boolean success = true;

                        // Reserve flights
                        for (Object fNum : flightNumbers) {
                            while (true) {
                                try {
                                    success &= reserveFlight(id, customer, Integer.parseInt(fNum.toString()));
                                    break;
                                } catch (RemoteException e) {
                                    try {
                                        onRMCrash(RM_FLIGHT_REF);
                                    } catch (RMTimeOutException e1) {
                                        abort(id);
                                        throw new RMServerDownException();
                                    }
                                }
                            }
                        }

                        // If should reserve a car
                        if (car) {
                            while (true) {
                                try {
                                    success &= reserveCar(id, customer, location);
                                    break;
                                } catch (RemoteException e) {
                                    try {
                                        onRMCrash(RM_CAR_REF);
                                    } catch (RMTimeOutException e1) {
                                        abort(id);
                                        throw new RMServerDownException();
                                    }
                                }
                            }
                        }

                        // If should reserve a room
                        if (room) {
                            while (true) {
                                try {
                                    success &= reserveRoom(id, customer, location);
                                    break;
                                } catch (RemoteException e) {
                                    try {
                                        onRMCrash(RM_ROOM_REF);
                                    } catch (RMTimeOutException e1) {
                                        abort(id);
                                        throw new RMServerDownException();
                                    }
                                }
                            }
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
                        throw e;
                    }
                }
            }
        }
    }

    @Override
    public int start() throws RemoteException {
        try {
            int transactionId = m_tm.start();
            logger.info("Started a new transaction with id: " + transactionId);
            return transactionId;
        } catch (InvalidTransactionException e) {
            throw e;
        } catch (NullPointerException e) {
            throw new TMException();
        }
    }

    @Override
    public boolean commit(int transactionId) throws RemoteException, InvalidTransactionException {
        try {
            // Update function
            commitRF(transactionId);
            logger.info("Received a commit request on transaction " + transactionId);
            m_tm.updateLastActive(transactionId);

            // Crash case: CC_1
            if(m_crashCase[CC_1]) {
                crash(COMP_MS);
            }

            // Crash case: CC_2
            if(m_crashCase[CC_2]) {
                new Thread(()->{
                    try {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            logger.error("Failed to sleep CC_2");
                        }
                        crash(COMP_MS);
                    } catch (RemoteException e) {/*Will not throw exception*/}
                }).start();
            }

            // 2PC
            logger.info("Applying 2 phase commit on all involved RMs");
            boolean allVR = voteRequest(transactionId);

            // Crash case: CC_4 || CC_5
            if(m_crashCase[CC_4] || m_crashCase[CC_5]) {
                crash(COMP_MS);
            }

            logger.info("Commit phase 2: Sending decision");

            // If at least one VR replied NO, then abort
            if(!allVR) {
                abort(transactionId);
                return false;
            }

            // Apply commits
            for (String rmStr : m_tm.getRMs(transactionId)) {
                while (true) {
                    ResourceManager rm = null;
                    String name = "UNKNOWN";
                    try {
                        if (rmStr.equals(ResourceManager.RM_CAR_REF)) {
                            name = RM_CAR_REF;
                            rm = m_carRM;
                        } else if (rmStr.equals(ResourceManager.RM_FLIGHT_REF)) {
                            name = RM_FLIGHT_REF;
                            rm = m_flightRM;
                        } else if (rmStr.equals(ResourceManager.RM_ROOM_REF)) {
                            name = RM_ROOM_REF;
                            rm = m_roomRM;
                        } else {
                            throw new RuntimeException("Unknown resource manager");
                        }
                        logger.info("Commit on RM " + name);
                        rm.commit(transactionId);

                        // Crash case: CC_6
                        if(m_crashCase[CC_6]) {
                            crash(COMP_MS);
                        }
                        break;
                    } catch (RemoteException e) {
                        bufferDecision(name, transactionId, DECISION_COMMIT);
                    }
                }
            }
            m_tm.removeTransaction(transactionId);
            deleteRF(transactionId);

            // Crash case: CC_7
            if(m_crashCase[CC_7]) {
                crash(COMP_MS);
            }
            return true;
        } catch (InvalidTransactionException e) {
            throw e;
        } catch (NullPointerException e) {
            throw new TMException();
        }
    }

    @Override
    public void abort(int transactionId) throws RemoteException, InvalidTransactionException {
        try {
            abortRF(transactionId);
            logger.info("Aborting transaction " + transactionId);
            m_tm.updateLastActive(transactionId);
            for (String rmStr : m_tm.getRMs(transactionId)) {
                try {
                    if (rmStr.equals(ResourceManager.RM_ROOM_REF)) {
                        m_roomRM.abort(transactionId);
                    } else if (rmStr.equals(ResourceManager.RM_FLIGHT_REF)) {
                        m_flightRM.abort(transactionId);
                    } else if (rmStr.equals(ResourceManager.RM_CAR_REF)) {
                        m_carRM.abort(transactionId);
                    } else {
                        throw new RuntimeException("Unknown resource manager");
                    }
                } catch (RemoteException e) {
                    bufferDecision(rmStr, transactionId, DECISION_ABORT);
                }
            }
            m_tm.removeTransaction(transactionId);
            deleteRF(transactionId);
        } catch (NullPointerException e) {
            throw new TMException();
        } catch (InvalidTransactionException e) {
            logger.warn("Transaction " + transactionId + " was not found. Will delete from RF");
            deleteRF(transactionId);
            throw e;
        }
    }

    @Override
    public boolean shutdown() throws RemoteException {
        try {
            if (m_tm.getTransactions().isEmpty()) {
                for (String key : s_registry.list()) {
                    try {
                        s_registry.unbind(key);
                        UnicastRemoteObject.unexportObject(this, true);
                    } catch (NotBoundException e) {
                        logger.error("Error unbinding remote object with key: " + key);
                        return false;
                    }
                }

                try {
                    m_flightRM.shutdown();
                    m_carRM.shutdown();
                    m_roomRM.shutdown();
                } catch (RemoteException e) {
                    // Shutdown return true anyway
                }
                logger.info("All RMs are shutdown. Shutting down middleware server ...");
                return true;
            } else {
                logger.info("Will not shutdown because there are still transactions");
            }
            return false;
        } catch (InvalidTransactionException e) {
            throw e;
        } catch (NullPointerException e) {
            throw new TMException();
        }
    }

    @Override
    public boolean voteRequest(int tid) throws RemoteException {
        logger.info("Commit phase 1: Sending vote request");
        try {
            for (String rmStr : m_tm.getRMs(tid)) {
                while (true) {
                    String name = "UNKNOWN";
                    ResourceManager rm = null;
                    try {
                        if (rmStr.equals(ResourceManager.RM_CAR_REF)) {
                            name = RM_CAR_REF;
                            rm = m_carRM;
                        } else if (rmStr.equals(ResourceManager.RM_FLIGHT_REF)) {
                            name = RM_FLIGHT_REF;
                            rm = m_flightRM;
                        } else if (rmStr.equals(ResourceManager.RM_ROOM_REF)) {
                            name = RM_ROOM_REF;
                            rm = m_roomRM;
                        } else {
                            throw new RuntimeException("Unknown resource manager");
                        }
                        boolean vr = rm.voteRequest(tid);
                        logger.info("RM " + name + " replied with a " + (vr ? "YES" : "NO"));
                        if (!vr) {
                            return false;
                        }

                        // Crash case: CC_10 || CC_11
                        if(m_crashCase[CC_10] || m_crashCase[CC_11]) {
                            crash(name);
                        }

                        // Crash case: CC_3
                        if(m_crashCase[CC_3]) {
                            crash(COMP_MS);
                        }

                        break;
                    } catch (RemoteException e) {
                        try {
                            onRMCrash(name);
                        } catch (RMTimeOutException e1) {
                            logger.error("Could not collect vote for RM " + name + ". Will consider a NO vote");
                            return false;
                        }
                    }
                }
            }
            return true;
        } catch (InvalidTransactionException e) {
            logger.warn("Vote request for an non-existing transaction " + tid);
            throw e;
        } catch (NullPointerException e) {
            throw new TMException();
        }
    }

    @Override
    public boolean crash(String comp) throws RemoteException {
        switch (comp) {
            case COMP_TM:
                logger.info("Received TM crash request");
                if(m_tm == null) {
                    logger.warn("TM is crashed already. Won't crash again");
                    return false;
                }

                // Write TM just in case
                m_tm.writeTM();

                // Unbind TM
                m_tm = null;
                logger.info("TM is now crashed");

                final int TM_SLEEP = 10000;
                new Thread(() -> {
                    try {
                        Thread.sleep(TM_SLEEP);
                        loadTM();
                        logger.info("TM is now up and running");
                    } catch (InterruptedException e) {
                        logger.error("Failed to sleep");
                    }
                }).start();
                return true;

            case COMP_MS:
                logger.info("MS will crash now");
                System.exit(1);
                break;

            case COMP_FLIGHT:
                try {
                    logger.info("Trying to crash flight RM");
                    m_flightRM.crash(null);
                } catch (Exception e) {}
                return true;

            case COMP_CAR:
                try {
                    logger.info("Trying to crash car RM");
                    m_carRM.crash(null);
                } catch (Exception e) {}
                return true;

            case COMP_ROOM:
                try {
                    logger.info("Trying to crash room RM");
                    m_roomRM.crash(null);
                } catch (Exception e) {}
                return true;
        }
        logger.warn("Unknown component to crash");
        return false;
    }

    @Override
    public void healthCheck() throws RemoteException {
        /*Do nothing*/
    }

    @Override
    public void syncTransactions(Set<Integer> transactionsId) throws RemoteException {
        /*Do nothing*/
    }

    @Override
    public boolean crashCase(int id) throws RemoteException {
        switch (id) {
            case CC_8:
            case CC_13:
                logger.info("Crash case " + id + " cannot recover from the system, this has to be done manually");
                return false;

            case CC_0:
            case CC_1:
            case CC_2:
            case CC_3:
            case CC_4:
            case CC_5:
            case CC_6:
            case CC_7:
            case CC_9:
            case CC_10:
            case CC_11:
            case CC_12:
                logger.info("Will perform crash case " + id);
                try {
                    setCrashCase(id);
                } catch (Exception e) {
                    logger.error("Failed to set crash case flag");
                    try {
                        // Reset flags on crash
                        setCrashCase(ResourceManager.CC_0);
                    } catch (Exception e2){}
                    return false;
                }
                return true;
            default:
                return false;
        }
    }

    /**
     * Select crash case
     * @param id
     */
    @Override
    public void setCrashCase(int id) throws RemoteException {

        // Set for current object
        for(int i=0; i < m_crashCase.length; i++) {
            if(i == id) {
                m_crashCase[i] = true;
            } else {
                m_crashCase[i] = false;
            }
        }

        // Set for RMs
        m_roomRM.setCrashCase(id);
        m_carRM.setCrashCase(id);
        m_flightRM.setCrashCase(id);
    }

    /**
     * Get crash case index
     * @return
     */
    @Override
    public int getCrashCase() {
        for(int i=0; i<m_crashCase.length; i++) {
            if(m_crashCase[i]) {
                return i;
            }
        }
        return -1;
    }
}