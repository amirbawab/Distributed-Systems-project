import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.io.BufferedReader;
import java.io.InputStreamReader;


public class RMThread extends Thread {
    Socket socket;
    RMThread (Socket socket)
    {
        this.socket=socket;
    }
    public void run()
    {
        try {

            // establish a socket with the mid level servername and port; change the values to something configurable with gradle
            //Socket socket = new Socket("localhost", 9090);

            // open an output stream to the middle layer server
            PrintWriter outToClient = new PrintWriter(socket.getOutputStream(), true); 

            // open an input stream from the middle layer server
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream())); 

            //System.out.println("Server ready");

            // read messages from client (mid level server)
            String message = null;
            while ((message = inFromClient.readLine())!=null)
            {
                // split message with comma, and call that function
                String[] params =  message.split(",");
                if (params[0] == "addFlight") {
                    addFlight(Integer.parseInt(param[1]),Integer.parseInt(param[2]),Integer.parseInt(param[3]),Integer.parseInt(param[4]));
                }
                else if (params[0] == "addCars") {
                    addCars(Integer.parseInt(param[1]),param[2],Integer.parseInt(param[3]),Integer.parseInt(param[4]));
                }
                else if (params[0] == "addRooms") {
                    addRooms(Integer.parseInt(param[1]),param[2],Integer.parseInt(param[3]),Integer.parseInt(param[4]));
                }
                else if (params[0] == "newCustomer") {
                    newCustomer(Integer.parseInt(param[1]));
                }
                else if (params[0] == "deleteFlight") {
                    deleteFlight(Integer.parseInt(param[1]),Integer.parseInt(param[2]));
                }
                else if (params[0] == "deleteCars") {
                    deleteCars(Integer.parseInt(param[1]),param[2]);
                }
                else if (params[0] == "deleteRooms") {
                    deleteRooms(Integer.parseInt(param[1]),param[2]);
                }
                else if (params[0] == "deleteCustomer") {
                    deleteCustomer(Integer.parseInt(param[1]),Integer.parseInt(param[2]));
                }
                else if (params[0] == "queryFlight") {
                    queryFlight(Integer.parseInt(param[1]),Integer.parseInt(param[2]));
                }
                else if (params[0] == "queryCars") {
                    queryCars(Integer.parseInt(param[1]),param[2]);
                }
                else if (params[0] == "queryRooms") {
                    queryRooms(Integer.parseInt(param[1]),param[2]);
                }
                else if (params[0] == "queryCustomerInfo") {
                    queryCustomerInfo(Integer.parseInt(param[1]),Integer.parseInt(param[2]));
                }
                else if (params[0] == "queryFlightPrice") {
                    queryFlightPrice(Integer.parseInt(param[1]),Integer.parseInt(param[2]));
                }
                else if (params[0] == "queryCarsPrice") {
                    queryCarsPrice(Integer.parseInt(param[1]),param[2]);
                }
                else if (params[0] == "queryRoomsPrice") {
                    queryRoomsPrice(Integer.parseInt(param[1]),param[2]);
                }
                else if (params[0] == "reserveFlight") {
                    reserveFlight(Integer.parseInt(param[1]),Integer.parseInt(param[2]),Integer.parseInt(param[3]));
                }
                else if (params[0] == "reserveCar") {
                    reserveCar(Integer.parseInt(param[1]),Integer.parseInt(param[2]),param[3]);
                }
                else if (params[0] == "reserveRoom") {
                    reserveRoom(Integer.parseInt(param[1]),Integer.parseInt(param[2]),param[3]);
                }
                else if (params[0] == "itinerary") {
                    //itinerary(Integer.parseInt(param[1]),Integer.parseInt(param[2]),Integer.parseInt(param[3]),Integer.parseInt(param[4]));
                    //method header is different here - need vector of flights, etc.
                    outToClient.println("false");
                }
                else if (params[0] == "newCustomer") {
                    newCustomer(Integer.parseInt(param[1]),Integer.parseInt(param[2]));
                }

            }
    /* method headers
        public boolean addFlight(int Id,int flightNum,int flightSeats,int flightPrice) 
        public boolean addCars(int Id,String location,int numCars,int price) 
        public boolean addRooms(int Id,String location,int numRooms,int price) 
        public int newCustomer(int Id) 
        public boolean deleteFlight(int Id,int flightNum) 
        public boolean deleteCars(int Id,String location) 
        public boolean deleteRooms(int Id,String location) 
        public boolean deleteCustomer(int Id,int customer) 
        public int queryFlight(int Id,int flightNum) 
        public int queryCars(int Id,String location) 
        public int queryRooms(int Id,String location) 
        public String queryCustomerInfo(int Id,int customer) 
        public int queryFlightPrice(int Id,int flightNum) 
        public int queryCarsPrice(int Id,String location) 
        public int queryRoomsPrice(int Id,String location) 
        public boolean reserveFlight(int Id,int customer,int flightNum) 
        public boolean reserveCar(int Id,int customer,String location) 
        public boolean reserveRoom(int Id,int customer,String location) 
        public boolean itinerary(int Id,int customer,int flightNumbers,String location,int Car,int Room) 
        public boolean newCustomer(int Id,int Cid) 
    */

            socket.close();
        }
        catch (IOException e)
        {

        }
    }
}
