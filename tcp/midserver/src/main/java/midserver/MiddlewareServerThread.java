package midserver;

import inter.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class MiddlewareServerThread extends Thread {

    // Middleware sever socket with the client
    private Socket m_socket;

    // Open an output stream to the middle layer server
    private PrintWriter m_outToClient;

    // Open an input stream from the middle layer server
    private BufferedReader m_inFromClient;

    // Middleware server as client
    private Socket m_socketRMFlight;
    private Socket m_socketRMCar;
    private Socket m_socketRMRoom;

    // Constants
    private static String TRUE = "true";
    private static String FALSE = "false";

    // Logger
    private static final Logger logger = LogManager.getLogger(MiddlewareServerThread.class);

    /**
     * Construct a new middleware
     * @param socket
     */
    public MiddlewareServerThread (Socket socket) throws IOException {
        this.m_socket= socket;
        this.m_outToClient = new PrintWriter(m_socket.getOutputStream(), true);
        this.m_inFromClient = new BufferedReader(new InputStreamReader(m_socket.getInputStream()));
    }

    /**
     * Connect flight socket
     * @param server
     * @param port
     */
    public void connectFlightSocket(String server, int port) throws IOException {
        this.m_socketRMFlight = new Socket(server, port);
    }

    /**
     * Connect car socket
     * @param server
     * @param port
     */
    public void connectCarSocket(String server, int port) throws IOException {
        this.m_socketRMCar = new Socket(server, port);
    }

    /**
     * Connect room socket
     * @param server
     * @param port
     */
    public void connectRoomSocket(String server, int port) throws IOException {
        this.m_socketRMRoom = new Socket(server, port);
    }

    @Override
    public void run() {
        try {

            // Flight streams
            PrintWriter pwFlight = new PrintWriter(m_socketRMFlight.getOutputStream(), true);
            BufferedReader brFlight = new BufferedReader(new InputStreamReader(m_socketRMFlight.getInputStream()));

            // Car streams
            PrintWriter pwCar = new PrintWriter(m_socketRMCar.getOutputStream(), true);
            BufferedReader brCar = new BufferedReader(new InputStreamReader(m_socketRMCar.getInputStream()));

            // Room streams
            PrintWriter pwRoom = new PrintWriter(m_socketRMRoom.getOutputStream(), true);
            BufferedReader brRoom = new BufferedReader(new InputStreamReader(m_socketRMRoom.getInputStream()));

            // Read messages from client
            String message;
            while ((message = m_inFromClient.readLine())!=null) {
                logger.debug("Received command: " + message);

                // Split message with comma, and call that function
                String[] params = message.split(",");

                // If params is empty, continue to next command
                if (params.length == 0) {
                    logger.warn("Cannot parse an empty param. Continuing to read next command ...");
                    continue;
                }

                switch (ResourceManager.Command.getFunctionByName(params[0])) {
                    case ADD_FLIGHT:
                    case DELETE_FLIGHT:
                    case QUERY_FLIGHT:
                    case QUERY_FLIGHT_PRICE:
                    case RESERVE_FLIGHT:
                        pwFlight.println(message);
                        m_outToClient.println(brFlight.readLine());
                        break;

                    case ADD_CARS:
                    case DELETE_CARS:
                    case QUERY_CARS:
                    case QUERY_CARS_PRICE:
                    case RESERVE_CAR:
                        pwCar.println(message);
                        m_outToClient.println(brCar.readLine());
                        break;

                    case ADD_ROOMS:
                    case RESERVE_ROOM:
                    case DELETE_ROOMS:
                    case QUERY_ROOMS:
                    case QUERY_ROOMS_PRICE:
                        pwRoom.println(message);
                        m_outToClient.println(brRoom.readLine());
                        break;

                    case NEW_CUSTOMER:
                        // TODO Refactor code
                        pwCar.println(message);
                        int cid = Integer.parseInt(brCar.readLine());
                        String newCommand = String.format("%s,%d,%d",
                                ResourceManager.Command.NEW_CUSTOMER_ID.getName(),
                                Integer.parseInt(params[1]), cid);
                        pwRoom.println(newCommand);
                        pwFlight.println(newCommand);

                        // Read to clear buffer
                        brFlight.readLine();
                        brCar.readLine();
                        brRoom.readLine();
                        m_outToClient.println(cid);
                        break;

                    case NEW_CUSTOMER_ID:
                    case DELETE_CUSTOMER:
                    case ITINERARY:
                        pwCar.println(message);
                        pwRoom.println(message);
                        pwFlight.println(message);
                        if(brCar.readLine().equals(FALSE) || brFlight.readLine().equals(FALSE)
                                || brRoom.readLine().equals(FALSE)) {
                            m_outToClient.println(FALSE);
                        } else {
                            m_outToClient.println(TRUE);
                        }
                        break;

                    case QUERY_CUSTOMER_INFO:
                        pwCar.println(message);
                        pwFlight.println(message);
                        pwRoom.println(message);
                        StringBuilder info = new StringBuilder();
                        info.append(brCar.readLine());
                        info.append(brFlight.readLine());
                        info.append(brRoom.readLine());
                        m_outToClient.println(info.toString());
                        break;
                }
            }

            // Close sockets
            m_socket.close();
            m_socketRMRoom.close();
            m_socketRMCar.close();
            m_socketRMFlight.close();
        } catch (IOException e) {
            logger.error("Error communicating with one or more RM servers");
        }
    }
}
