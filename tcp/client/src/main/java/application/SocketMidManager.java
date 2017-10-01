package application;

import inter.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Vector;

public class SocketMidManager implements ResourceManager {

    // Logger
    private static final Logger logger = LogManager.getLogger(Client.class);

    // Client socket
    private Socket m_socket;

    // Open an output stream to the middle layer server
    private PrintWriter m_outToServer;

    // Open an input stream from the middle layer server
    private BufferedReader m_inFromServer;

    /**
     * Construct a client to middleware server socket manager
     * @param server
     * @param port
     * @throws IOException
     */
	public SocketMidManager(String server, int port) throws IOException {
        m_socket = new Socket(server, port);
        m_outToServer = new PrintWriter(m_socket.getOutputStream(), true);
        m_inFromServer = new BufferedReader(new InputStreamReader(m_socket.getInputStream()));
    }

    /**
     * Send command to the middleware server
     * @param commandName
     * @param args
     * @return message returned from the server
     */
    private String sendCommand(String commandName, Object ... args) {

        // Join command and arguments
        StringBuilder command = new StringBuilder();
        command.append(commandName);
        for(Object arg : args) {
            command.append(",");
            command.append(arg.toString());
        }

        // Sent command
        m_outToServer.println(command.toString());

        // Return the message sent back from the middleware server
        try {
            return m_inFromServer.readLine();
        } catch (IOException e) {
            logger.error("Error reading return value from the middleware server: " + e.getMessage());
        }
        return "";
    }

    /**
     * Read boolean string
     * @param returnStr
     * @return boolean string or false by default
     */
    private boolean boolOrDefault(String returnStr) {
        try {
            return Boolean.parseBoolean(returnStr);
        } catch (RuntimeException e) {
            logger.error("Error parsing return value to boolean, will return false");
            return false;
        }
    }

    /**
     * Read int string
     * @param returnStr
     * @return int string or -1 by default
     */
    private int intOrDefault(String returnStr) {
        try {
            return Integer.parseInt(returnStr);
        } catch (RuntimeException e) {
            logger.error("Error parsing return value to int, will return -1");
            return -1;
        }
    }

    /*************************
     * COMMAND LINE FUNCTIONS
     *************************/

    @Override
    public boolean addFlight(int id,int flightNum,int flightSeats,int flightPrice) {
        return boolOrDefault(sendCommand("addFlight", id, flightNum, flightSeats, flightPrice));
    }

    @Override
    public boolean addCars(int id,String location,int numCars,int price) {
        return boolOrDefault(sendCommand("addCars", id, location, numCars, price));
    }

    @Override
    public boolean addRooms(int id,String location,int numRooms,int price) {
        return boolOrDefault(sendCommand("addRooms", id, location, numRooms, price));
    }

    @Override
    public int newCustomer(int id) {
        return intOrDefault(sendCommand("newCustomer", id));
    }

    @Override
    public boolean deleteFlight(int id,int flightNum) {
        return boolOrDefault(sendCommand("deleteFlight", id, flightNum));
    }

    @Override
    public boolean deleteCars(int id,String location) {
        return boolOrDefault(sendCommand("deleteCars", id, location));
    }

    @Override
    public boolean deleteRooms(int id,String location) {
        return boolOrDefault(sendCommand("deleteRooms", id, location));
    }

    @Override
    public boolean deleteCustomer(int id,int customer) {
        return boolOrDefault(sendCommand("deleteCustomer", id, customer));
    }

    @Override
    public int queryFlight(int id,int flightNum) {
        return intOrDefault(sendCommand("deleteCustomer", id, flightNum));
    }

    @Override
    public int queryCars(int id,String location) {
        return intOrDefault(sendCommand("queryCars", id, location));
    }

    @Override
    public int queryRooms(int id,String location) {
        return intOrDefault(sendCommand("queryRooms", id, location));
    }

    @Override
    public String queryCustomerInfo(int id,int customer) {
        return sendCommand("queryCustomerInfo", id, customer);
    }

    @Override
    public int queryFlightPrice(int id,int flightNum) {
        return intOrDefault(sendCommand("queryFlightPrice", id, flightNum));
    }

    @Override
    public int queryCarsPrice(int id,String location) {
        return intOrDefault(sendCommand("queryCarsPrice", id, location));
    }

    @Override
    public int queryRoomsPrice(int id,String location) {
        return intOrDefault(sendCommand("queryRoomsPrice", id, location));
    }

    @Override
    public boolean reserveFlight(int id,int customer,int flightNum) {
        return boolOrDefault(sendCommand("reserveFlight", id, customer, flightNum));
    }

    @Override
    public boolean reserveCar(int id,int customer,String location) {
        return boolOrDefault(sendCommand("reserveCar", id, customer, location));
    }

    @Override
    public boolean reserveRoom(int id,int customer,String location) {
        return boolOrDefault(sendCommand("reserveRoom", id, customer, location));
    }

    @Override
    public boolean itinerary(int id, int customer, Vector flightNumbers, String location, boolean car, boolean room) {
        return boolOrDefault(sendCommand("itinerary", id, customer, flightNumbers, location, car, room));
    }

    @Override
    public boolean newCustomer(int id,int cid) {
        return boolOrDefault(sendCommand("newCustomer", id, cid));
    }
}

