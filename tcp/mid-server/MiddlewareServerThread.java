import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.io.BufferedReader;
import java.io.InputStreamReader;


public class MiddlewareServerThread extends Thread {
    Socket socket;
    MiddlewareServerThread (Socket socket)
    {
        this.socket=socket;
    }
    public void run()
    {
        try {

            // socket for client
            //Socket socketClient = new Socket("Client", 9090);
            PrintWriter outToClient = new PrintWriter(socket.getOutputStream(), true); // open an output stream to the client
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream())); // open an input stream from the server

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

            socket.close();
        }
        catch (IOException e)
        {

        }
    }
}
