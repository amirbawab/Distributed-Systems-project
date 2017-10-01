package application;

import inter.ResourceManager;

import java.util.Vector;

import java.net.ServerSocket;
import java.net.Socket;

class MiddlewareServer implements ResourceManager {
    private ResourceManager m_carRM;
    private ResourceManager m_flightRM;
    private ResourceManager m_roomRM;

    public static void main(String[] args) {

        // Figure out where server is running
        if (args.length != 3) {
            System.err.println ("Wrong usage");
            System.out.println("Usage: java ResImpl.ResourceManagerImpl [port]");
            System.exit(1);
        }


        // Set server port
        //int serverRMIRegistryPort = Integer.parseInt(args[0]);
        //String rmRMIRegistryIP = args[1];
        //int rmRMIRegistryPort = Integer.parseInt(args[2]);

        // socket for client
        Socket socketClient = new Socket("Client", 9090);
        PrintWriter outToClient = new PrintWriter(socketClient.getOutputStream(), true); // open an output stream to the client
        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socketClient.getInputStream())); // open an input stream from the server

        // Connect to the three RMs; hardcode machine names and port numbers or use gradle
        Socket socketRMFlight = new Socket("RM1", 9090);
        PrintWriter outToRMFlight = new PrintWriter(socketRMFlight.getOutputStream(), true); // open an output stream to the server
        BufferedReader inFromRMFlight = new BufferedReader(new InputStreamReader(socketRMFlight.getInputStream())); // open an input stream from the server

        Socket socketRMCar = new Socket("RM2", 9091);
        PrintWriter outToRMCar = new PrintWriter(socketRMCar.getOutputStream(), true); // open an output stream to the server
        BufferedReader inFromRMCar = new BufferedReader(new InputStreamReader(socketRMCar.getInputStream())); // open an input stream from the server

        Socket socketRMRoom = new Socket("RM3", 9092);
        PrintWriter outToRMRoom = new PrintWriter(socketRMRoom.getOutputStream(), true); // open an output stream to the server
        BufferedReader inFromRMRoom = new BufferedReader(new InputStreamReader(socketRMRoom.getInputStream())); // open an input stream from the server

        // read messages from client
        String message = null;
        while ((message = inFromClient.readLine())!=null)
        {
            // split message with comma, and call that function
            String[] params =  message.split(",");
            if (params[0] == "addFlight") {
                outToRMFlight.println(message);
                outToClient.println(inFromRMFlight.readline());
            }
            else if (params[0] == "addCars") {
                outToRMCar.println(message);
                outToClient.println(inFromRMCar.readline());
            }
            else if (params[0] == "addRooms") {
                outToRMRoom.println(message);
                outToClient.println(inFromRMRoom.readline());
            }
            else if (params[0] == "newCustomer") {
                if (params.length == 1) {
                    outToRMCar.println(message);
                    int cid = Integer.parseInt(inFromRMCar.readline());
                    outToRMRoom.println(params[0] + "," + cid);
                    outToRMFlight.println(params[0] + "," + cid);
                    outToClient.println(cid);
                }
                else { // params.length == 2
                    outToRMRoom.println(message);
                    boolean res1 = Boolean.parseBoolean(inFromRMRoom.readline());
                    outToRMCar.println(message);
                    boolean res2 = Boolean.parseBoolean(inFromRMCar.readline());
                    outToRMFlight.println(message);
                    boolean res3 = Boolean.parseBoolean(inFromRMFlight.readline());
                    if (res1 && res2 && res3)
                        outToClient.println("true");
                    else
                        outToClient.println("false");
                }
            }
            else if (params[0] == "deleteFlight") {
                outToRMFlight.println(message);
                outToClient.println(inFromRMFlight.readline());
            }
            else if (params[0] == "deleteCars") {
                outToRMCar.println(message);
                outToClient.println(inFromRMCar.readline());
            }
            else if (params[0] == "deleteRooms") {
                outToRMRoom.println(message);
                outToClient.println(inFromRMRoom.readline());
            }
            else if (params[0] == "deleteCustomer") {
                outToRMRoom.println(message);
                boolean res1 = Boolean.parseBoolean(inFromRMRoom.readline());
                outToRMCar.println(message);
                boolean res2 = Boolean.parseBoolean(inFromRMCar.readline());
                outToRMFlight.println(message);
                boolean res3 = Boolean.parseBoolean(inFromRMFlight.readline());
                if (res1 && res2 && res3)
                    outToClient.println("true");
                else
                    outToClient.println("false");
            }
            else if (params[0] == "queryFlight") {
                outToRMFlight.println(message);
                outToClient.println(inFromRMFlight.readline());
            }
            else if (params[0] == "queryCars") {
                outToRMCar.println(message);
                outToClient.println(inFromRMCar.readline());
            }
            else if (params[0] == "queryRooms") {
                outToRMRoom.println(message);
                outToClient.println(inFromRMRoom.readline());
            }
            else if (params[0] == "queryCustomerInfo") {
                StringBuilder sb = new StringBuilder();
                outToRMCar.println(message);
                sb.append("\nCar info:\n").append(inFromRMCar.readline());
                outToRMFlight.println(message);
                sb.append("\nFlight info:\n").append(inFromRMFlight.readline());
                outToRMRoom.println(message);
                sb.append("\nRoom info:\n").append(inFromRMRoom.readline());
                outToClient.println(sb.toString());
            }
            else if (params[0] == "queryFlightPrice") {
                outToRMFlight.println(message);
                outToClient.println(inFromRMFlight.readline());
            }
            else if (params[0] == "queryCarsPrice") {
                outToRMCar.println(message);
                outToClient.println(inFromRMCar.readline());
            }
            else if (params[0] == "queryRoomsPrice") {
                outToRMRoom.println(message);
                outToClient.println(inFromRMRoom.readline());
            }
            else if (params[0] == "reserveFlight") {
                outToRMFlight.println(message);
                outToClient.println(inFromRMFlight.readline());
            }
            else if (params[0] == "reserveCar") {
                outToRMCar.println(message);
                outToClient.println(inFromRMCar.readline());
            }
            else if (params[0] == "reserveRoom") {
                outToRMRoom.println(message);
                outToClient.println(inFromRMRoom.readline());
            }
            else if (params[0] == "itinerary") {
                outToRMRoom.println(message);
                boolean res1 = Boolean.parseBoolean(inFromRMRoom.readline());
                outToRMCar.println(message);
                boolean res2 = Boolean.parseBoolean(inFromRMCar.readline());
                outToRMFlight.println(message);
                boolean res3 = Boolean.parseBoolean(inFromRMFlight.readline());
                if (res1 && res2 && res3)
                    outToClient.println("true");
                else
                    outToClient.println("false");
            }
        }


        /*
        MiddlewareServer ms = bindRM(ResourceManager.MID_SERVER_REF, serverRMIRegistryPort);
        ms.m_carRM = connectToRM(ResourceManager.RM_CAR_REF, rmRMIRegistryIP, rmRMIRegistryPort);
        ms.m_flightRM = connectToRM(ResourceManager.RM_FLIGHT_REF, rmRMIRegistryIP, rmRMIRegistryPort);
        ms.m_roomRM = connectToRM(ResourceManager.RM_ROOM_REF, rmRMIRegistryIP, rmRMIRegistryPort);
        */

        /*
        serverSocket midServer = new serverSocket();
        try
        {
            midServer.runMidServerThread();
        }
        catch (IOException e)
        {

        }
        */

        // Create and install a security manager
        /*if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }*/

    }

    /*
    private static MiddlewareServer bindRM(String key, int port) {
        // Bind server object
        try {
            // create a new Server object
            MiddlewareServer obj = new MiddlewareServer();
            // dynamically generate the stub (client proxy)
            ResourceManager rm = (ResourceManager) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry(port);
            registry.rebind(key, rm);
            System.out.println("Middleware server ready!");
            return obj;
        } catch (Exception e) {
            System.err.println("Middleware server exception: " + e.toString());
            e.printStackTrace();
            throw new RuntimeException("Binding failure: Terminating program ...");
        }
    }
    */

    /**
     * Connect to RM
     * @param key
     */
    /*
    private static ResourceManager connectToRM(String key, String server, int port) {
        try
        {
            // get a reference to the rmiregistry
            Registry registry = LocateRegistry.getRegistry(server, port);
            // get the proxy and the remote reference by rmiregistry lookup
            ResourceManager rm = (ResourceManager) registry.lookup(key);
            if(rm!=null) {
                System.out.println("Connected to RM: " + key);
            } else {
                System.err.println("Could not connect to RM: " + key);
            }
            return rm;
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
        throw new RuntimeException("Connection failure: Terminating program ...");
    }
    */
/*
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
    public boolean itinerary(int id, int customer, Vector flightNumbers, String location, boolean Car, boolean Room) throws RemoteException {
        return m_flightRM.itinerary(id, customer, flightNumbers, location, Car, Room) &&
                m_carRM.itinerary(id, customer, flightNumbers, location, Car, Room) &&
                m_roomRM.itinerary(id, customer, flightNumbers, location, Car, Room);
    }
}
*/
