package rm;

import inter.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class RMThread extends Thread {

    // Socket connecting RM to middleware server
    private Socket m_socket;

    // Open an output stream to the middle layer server
    private PrintWriter m_outToClient;

    // Open an input stream from the middle layer server
    private BufferedReader m_inFromClient;

    // Logger
    private static final Logger logger = LogManager.getLogger(RMThread.class);

    // Resource manager
    private ResourceManagerImpl m_rm;

    /**
     * Construct an RM thread
     * @param socket
     */
    RMThread (Socket socket, ResourceManagerImpl rm) throws IOException {
        this.m_socket=socket;
        this.m_rm = rm;
        this.m_outToClient = new PrintWriter(m_socket.getOutputStream(), true);
        this.m_inFromClient = new BufferedReader(new InputStreamReader(m_socket.getInputStream()));
    }

    public void run() {
        try {

            // Read messages from client (mid level server)
            String message;
            while ((message = m_inFromClient.readLine())!=null) {

                // split message with comma, and call that function
                String[] params =  message.split(",");

                // If params is empty, continue to next command
                if (params.length == 0) {
                    logger.warn("Cannot parse an empty param. Continuing to read next command ...");
                    continue;
                }

                // Parse command
                switch (ResourceManager.Command.getFunctionByName(params[0])) {
                    case ADD_FLIGHT:
                        m_outToClient.println(m_rm.addFlight(Integer.parseInt(params[1]),Integer.parseInt(params[2]),
                                Integer.parseInt(params[3]),Integer.parseInt(params[4])));
                        break;
                    case ADD_CARS:
                        m_outToClient.println(m_rm.addCars(Integer.parseInt(params[1]), params[2],
                                Integer.parseInt(params[3]), Integer.parseInt(params[4])));
                        break;
                    case ADD_ROOMS:
                        m_outToClient.println(m_rm.addRooms(Integer.parseInt(params[1]), params[2], Integer.parseInt(params[3]),
                                Integer.parseInt(params[4])));
                        break;
                    case NEW_CUSTOMER:
                        m_outToClient.println(m_rm.newCustomer(Integer.parseInt(params[1])));
                        break;
                    case DELETE_FLIGHT:
                        m_outToClient.println(m_rm.deleteFlight(Integer.parseInt(params[1]), Integer.parseInt(params[2])));
                        break;
                    case DELETE_CARS:
                        m_outToClient.println(m_rm.deleteCars(Integer.parseInt(params[1]),params[2]));
                        break;
                    case DELETE_ROOMS:
                        m_outToClient.println(m_rm.deleteRooms(Integer.parseInt(params[1]),params[2]));
                        break;
                    case DELETE_CUSTOMER:
                        m_outToClient.println(m_rm.deleteCustomer(Integer.parseInt(params[1]),Integer.parseInt(params[2])));
                        break;
                    case QUERY_FLIGHT:
                        m_outToClient.println(m_rm.queryFlight(Integer.parseInt(params[1]),Integer.parseInt(params[2])));
                        break;
                    case QUERY_CARS:
                        m_outToClient.println(m_rm.queryCars(Integer.parseInt(params[1]),params[2]));
                        break;
                    case QUERY_ROOMS:
                        m_outToClient.println(m_rm.queryRooms(Integer.parseInt(params[1]),params[2]));
                        break;
                    case QUERY_CUSTOMER_INFO:
                        m_outToClient.println(m_rm.queryCustomerInfo(Integer.parseInt(params[1]),Integer.parseInt(params[2])));
                        break;
                    case QUERY_FLIGHT_PRICE:
                        m_outToClient.println(m_rm.queryFlightPrice(Integer.parseInt(params[1]),Integer.parseInt(params[2])));
                        break;
                    case QUERY_CARS_PRICE:
                        m_outToClient.println(m_rm.queryCarsPrice(Integer.parseInt(params[1]),params[2]));
                        break;
                    case QUERY_ROOMS_PRICE:
                        m_outToClient.println(m_rm.queryRoomsPrice(Integer.parseInt(params[1]),params[2]));
                        break;
                    case RESERVE_FLIGHT:
                        m_outToClient.println(m_rm.reserveFlight(Integer.parseInt(params[1]),Integer.parseInt(params[2]),Integer.parseInt(params[3])));
                        break;
                    case RESERVE_CAR:
                        m_outToClient.println(m_rm.reserveCar(Integer.parseInt(params[1]),Integer.parseInt(params[2]),params[3]));
                        break;
                    case RESERVE_ROOM:
                        m_outToClient.println(m_rm.reserveRoom(Integer.parseInt(params[1]),Integer.parseInt(params[2]),params[3]));
                        break;
                    case ITINERARY:
                        // FIXME
                        // m_outToClient.println(m_rm.itinerary(Integer.parseInt(params[1]),Integer.parseInt(params[2]),Integer.parseInt(params[3]),Integer.parseInt(params[4])));
                        break;
                    case NEW_CUSTOMER_ID:
                        m_outToClient.println(m_rm.newCustomer(Integer.parseInt(params[1]),Integer.parseInt(params[2])));
                        break;
                }
            }
        } catch (IOException e) {
            logger.error("Error communicating with the middleware server");
        } finally {
            try {
                m_socket.close();
            } catch (IOException e) {
                logger.error("Error closing socket");
            }
        }
    }
}
