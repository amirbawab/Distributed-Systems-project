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

    // Sync lock
    private final static Object lock = new Object();

    // RM Streams
    private PrintWriter m_pwFlight ;
    private BufferedReader m_brFlight ;
    private PrintWriter m_pwCar ;
    private BufferedReader m_brCar ;
    private PrintWriter m_pwRoom ;
    private BufferedReader m_brRoom ;

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

    /**
     * Send value to client
     * @param value
     */
    private void sendToClient(String value) throws IOException {
        logger.debug("Sending to Client: " + value);
        this.m_outToClient.println(value);
    }

    /**
     * Send command to Car RM
     * @param command
     */
    private String sendToCarRM(String command) throws IOException {
        logger.debug("Sending to Car RM: " +command );
        m_pwCar.println(command);
        String ret = this.m_brCar.readLine();
        logger.debug("Car RM returned: " + ret);
        return ret;
    }

    /**
     * Send command to Room RM
     * @param command
     */
    private String sendToRoomRM(String command) throws IOException {
        logger.debug("Sending to Room RM: " +command );
        m_pwRoom.println(command);
        String ret = this.m_brRoom.readLine();
        logger.debug("Room RM returned: " + ret);
        return ret;
    }
    /**
     * Send command to Car RM
     * @param command
     */
    private String sendToFlightRM(String command) throws IOException {
        logger.debug("Sending to Flight RM: " +command );
        m_pwFlight.println(command);
        String ret = this.m_brFlight.readLine();
        logger.debug("Flight RM returned: " + ret);
        return ret;
    }

    @Override
    public void run() {
        try {

            // Flight streams
            m_pwFlight = new PrintWriter(m_socketRMFlight.getOutputStream(), true);
            m_brFlight = new BufferedReader(new InputStreamReader(m_socketRMFlight.getInputStream()));

            // Car streams
            m_pwCar = new PrintWriter(m_socketRMCar.getOutputStream(), true);
            m_brCar = new BufferedReader(new InputStreamReader(m_socketRMCar.getInputStream()));

            // Room streams
            m_pwRoom = new PrintWriter(m_socketRMRoom.getOutputStream(), true);
            m_brRoom = new BufferedReader(new InputStreamReader(m_socketRMRoom.getInputStream()));

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

                // Synchronize command calls
                synchronized (lock) {

                    // Check command
                    switch (ResourceManager.Command.getFunctionByName(params[0])) {
                        case ADD_FLIGHT:
                        case DELETE_FLIGHT:
                        case QUERY_FLIGHT:
                        case QUERY_FLIGHT_PRICE:
                        case RESERVE_FLIGHT:
                            sendToClient(sendToFlightRM(message));
                            break;

                        case ADD_CARS:
                        case DELETE_CARS:
                        case QUERY_CARS:
                        case QUERY_CARS_PRICE:
                        case RESERVE_CAR:
                            sendToClient(sendToCarRM(message));
                            break;

                        case ADD_ROOMS:
                        case RESERVE_ROOM:
                        case DELETE_ROOMS:
                        case QUERY_ROOMS:
                        case QUERY_ROOMS_PRICE:
                            sendToClient(sendToRoomRM(message));
                            break;

                        case NEW_CUSTOMER:
                            // TODO Refactor code
                            String cid = sendToCarRM(message);
                            String newCommand = String.format("%s,%s,%s",
                                    ResourceManager.Command.NEW_CUSTOMER_ID.getName(),
                                    params[1], cid);
                            sendToFlightRM(newCommand);
                            sendToRoomRM(newCommand);
                            sendToClient(cid);
                            break;

                        case NEW_CUSTOMER_ID:
                        case DELETE_CUSTOMER:
                            if(sendToCarRM(message).equals(FALSE)
                                    || sendToFlightRM(message).equals(FALSE)
                                    || sendToRoomRM(message).equals(FALSE)) {
                                sendToClient(FALSE);
                            } else {
                                sendToClient(TRUE);
                            }
                            break;

                        case QUERY_CUSTOMER_INFO:

                            // TODO in later release, for now the @ is replaced by a new line in the CLI
                            StringBuilder info = new StringBuilder();
                            info.append("@Car info:@").append(sendToCarRM(message))
                                    .append("@Flight info:@").append(sendToFlightRM(message))
                                    .append("@Room info:@").append(sendToRoomRM(message));
                            sendToClient(info.toString());
                            break;

                        case ITINERARY:
                            String[] fNumSplit = params[3].split(":");

                            // Check if can access
                            boolean passCheck = true;
                            for(Object fNum : fNumSplit) {
                                String queryFlight = ResourceManager.Command.QUERY_FLIGHT.getName() + "," + params[1]
                                        + "," + fNum.toString();
                                if(Integer.parseInt(sendToFlightRM(queryFlight)) == 0) {
                                    passCheck = false;
                                    break;
                                }
                            }

                            // Check if can reserve car
                            if(params[5].equals(TRUE)) {
                                String queryCar = ResourceManager.Command.QUERY_CARS.getName()+","
                                        +params[1]+","+params[4];
                                if(Integer.parseInt(sendToCarRM(queryCar)) == 0) {
                                    passCheck = false;
                                }
                            }

                            // Check if can reserve room
                            if(params[6].equals(TRUE)) {
                                String queryRoom = ResourceManager.Command.QUERY_ROOMS.getName()+","
                                        +params[1]+","+params[4];
                                if(Integer.parseInt(sendToRoomRM(queryRoom)) == 0) {
                                    passCheck = false;
                                }
                            }

                            if(!passCheck) {
                                sendToClient(FALSE);
                            } else {

                                String success = TRUE;
                                for(Object fNum : fNumSplit) {
                                    String reserveFlightCommand = ResourceManager.Command.RESERVE_FLIGHT.getName() + ","
                                            + params[1] + "," + params[2] + "," + fNum;
                                    String tmpRet = sendToFlightRM(reserveFlightCommand);
                                    if(tmpRet.equals(FALSE)) {
                                        success = FALSE;
                                    }
                                }

                                // If should reserve a car
                                if(params[5].equals(TRUE)) {
                                    String tmpRet = sendToCarRM(ResourceManager.Command.RESERVE_CAR.getName() + ","
                                            + params[1] + "," + params[2] + "," + params[4]);
                                    if(tmpRet.equals(FALSE)) {
                                        success = FALSE;
                                    }
                                }

                                // If should reserve a room
                                if(params[6].equals(TRUE)) {
                                    String tmpRet = sendToRoomRM(ResourceManager.Command.RESERVE_ROOM.getName() + ","
                                            + params[1] +"," + params[2] + "," + params[4]);
                                    if(tmpRet.equals(FALSE)) {
                                        success = FALSE;
                                    }
                                }

                                // Check if can reserve but not successful
                                if(success.equals(FALSE)) {
                                    logger.warn("Query showed that the user can reserve but the reservation was " +
                                            "not successful. Further investigation is required");
                                }

                                // Return value to client
                                sendToClient(success);
                            }
                            break;
                    }
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
