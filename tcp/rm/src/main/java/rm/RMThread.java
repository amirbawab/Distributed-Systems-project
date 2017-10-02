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
    private PrintWriter m_outToMidServer;

    // Open an input stream from the middle layer server
    private BufferedReader m_inFromMidServer;

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
        this.m_outToMidServer = new PrintWriter(m_socket.getOutputStream(), true);
        this.m_inFromMidServer = new BufferedReader(new InputStreamReader(m_socket.getInputStream()));
    }

    /**
     * Send value to middleware server
     * @param value
     */
    private void sendToMidServer(String value) throws IOException {
        logger.debug("Sending to Middleware server: " + value);
        this.m_outToMidServer.println(value);
    }

    /**
     * @see #sendToMidServer(String)
     */
    private void sendToMidServer(boolean value) throws IOException {
        sendToMidServer(""+value);
    }

    /**
     * @see #sendToMidServer(String)
     */
    private void sendToMidServer(int value) throws IOException {
        sendToMidServer(""+value);
    }

    public void run() {
        try {

            // Read messages from client (mid level server)
            String message;
            while ((message = m_inFromMidServer.readLine())!=null) {
                logger.debug("Received command: " + message);

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
                        sendToMidServer(m_rm.addFlight(Integer.parseInt(params[1]), Integer.parseInt(params[2]),
                                Integer.parseInt(params[3]),Integer.parseInt(params[4])));
                        break;
                    case ADD_CARS:
                        sendToMidServer(m_rm.addCars(Integer.parseInt(params[1]), params[2],
                                Integer.parseInt(params[3]), Integer.parseInt(params[4])));
                        break;
                    case ADD_ROOMS:
                        sendToMidServer(m_rm.addRooms(Integer.parseInt(params[1]), params[2], Integer.parseInt(params[3]),
                                Integer.parseInt(params[4])));
                        break;
                    case NEW_CUSTOMER:
                        sendToMidServer(m_rm.newCustomer(Integer.parseInt(params[1])));
                        break;
                    case DELETE_FLIGHT:
                        sendToMidServer(m_rm.deleteFlight(Integer.parseInt(params[1]), Integer.parseInt(params[2])));
                        break;
                    case DELETE_CARS:
                        sendToMidServer(m_rm.deleteCars(Integer.parseInt(params[1]),params[2]));
                        break;
                    case DELETE_ROOMS:
                        sendToMidServer(m_rm.deleteRooms(Integer.parseInt(params[1]),params[2]));
                        break;
                    case DELETE_CUSTOMER:
                        sendToMidServer(m_rm.deleteCustomer(Integer.parseInt(params[1]),Integer.parseInt(params[2])));
                        break;
                    case QUERY_FLIGHT:
                        sendToMidServer(m_rm.queryFlight(Integer.parseInt(params[1]),Integer.parseInt(params[2])));
                        break;
                    case QUERY_CARS:
                        sendToMidServer(m_rm.queryCars(Integer.parseInt(params[1]),params[2]));
                        break;
                    case QUERY_ROOMS:
                        sendToMidServer(m_rm.queryRooms(Integer.parseInt(params[1]),params[2]));
                        break;
                    case QUERY_CUSTOMER_INFO:
                        sendToMidServer(m_rm.queryCustomerInfo(Integer.parseInt(params[1]),Integer.parseInt(params[2])));
                        break;
                    case QUERY_FLIGHT_PRICE:
                        sendToMidServer(m_rm.queryFlightPrice(Integer.parseInt(params[1]),Integer.parseInt(params[2])));
                        break;
                    case QUERY_CARS_PRICE:
                        sendToMidServer(m_rm.queryCarsPrice(Integer.parseInt(params[1]),params[2]));
                        break;
                    case QUERY_ROOMS_PRICE:
                        sendToMidServer(m_rm.queryRoomsPrice(Integer.parseInt(params[1]),params[2]));
                        break;
                    case RESERVE_FLIGHT:
                        sendToMidServer(m_rm.reserveFlight(Integer.parseInt(params[1]),Integer.parseInt(params[2]),Integer.parseInt(params[3])));
                        break;
                    case RESERVE_CAR:
                        sendToMidServer(m_rm.reserveCar(Integer.parseInt(params[1]),Integer.parseInt(params[2]),params[3]));
                        break;
                    case RESERVE_ROOM:
                        sendToMidServer(m_rm.reserveRoom(Integer.parseInt(params[1]),Integer.parseInt(params[2]),params[3]));
                        break;
                    case ITINERARY:
                        // Handled by the middleware server
                        break;
                    case NEW_CUSTOMER_ID:
                        sendToMidServer(m_rm.newCustomer(Integer.parseInt(params[1]),Integer.parseInt(params[2])));
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
