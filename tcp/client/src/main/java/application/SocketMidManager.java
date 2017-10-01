import java.net.ServerSocket;
import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SocketMidManager {

    // establish a socket with the mid level server and port; change the values to something configurable with gradle
	Socket socket = new Socket("localhost", 9090);

    // open an output stream to the middle layer server
	PrintWriter outToServer = new PrintWriter(socket.getOutputStream(), true); 

    // open an input stream from the middle layer server
	BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream())); 

    // for each method, send the function name and arguments as one string, each arg concatenated with a comma ","
    // then block and wait for response
    // then convert return value from a string, and return it
    public boolean addFlight(int Id,int flightNum,int flightSeats,int flightPrice) {
        String functionName = Thread.currentThread().getStackTrace()[1].getMethodName();
        outToServer.println(functionName + "," + Integer.toString(Id) +","+ Integer.toString(flightNum) +","+ Integer.toString(flightSeats) + "," + Integer.toString(flightPrice));
        return Boolean.parseBoolean(inFromServer.readLine());
    }
    public boolean addCars(int Id,String location,int numCars,int price) {
        String functionName = Thread.currentThread().getStackTrace()[1].getMethodName();
        outToServer.println(functionName + "," + Integer.toString(Id) +","+ location +","+ Integer.toString(numCars) + "," + Integer.toString(price));
        return Boolean.parseBoolean(inFromServer.readLine());
    }
    public boolean addRooms(int Id,String location,int numRooms,int price) {
        String functionName = Thread.currentThread().getStackTrace()[1].getMethodName();
        outToServer.println(functionName + "," + Integer.toString(Id) +","+ location +","+ Integer.toString(numRooms) + "," + Integer.toString(price));
        return Boolean.parseBoolean(inFromServer.readLine());
    }
    public int newCustomer(int Id) {
        String functionName = Thread.currentThread().getStackTrace()[1].getMethodName();
        outToServer.println(functionName + "," + Integer.toString(Id));
        return Integer.parseInt(inFromServer.readLine());
    }
    public boolean deleteFlight(int Id,int flightNum) {
        String functionName = Thread.currentThread().getStackTrace()[1].getMethodName();
        outToServer.println(functionName + "," + Integer.toString(Id) +","+ Integer.toString(flightNum));
        return Boolean.parseBoolean(inFromServer.readLine());
    }
    public boolean deleteCars(int Id,String location) {
        String functionName = Thread.currentThread().getStackTrace()[1].getMethodName();
        outToServer.println(functionName + "," + Integer.toString(Id) +","+ location);
        return Boolean.parseBoolean(inFromServer.readLine());
    }
    public boolean deleteRooms(int Id,String location) {
        String functionName = Thread.currentThread().getStackTrace()[1].getMethodName();
        outToServer.println(functionName + "," + Integer.toString(Id) +","+ location);
        return Boolean.parseBoolean(inFromServer.readLine());
    }
    public boolean deleteCustomer(int Id,int customer) {
        String functionName = Thread.currentThread().getStackTrace()[1].getMethodName();
        outToServer.println(functionName + "," + Integer.toString(Id) +","+ Integer.toString(customer));
        return Boolean.parseBoolean(inFromServer.readLine());
    }
    public int queryFlight(int Id,int flightNum) {
        String functionName = Thread.currentThread().getStackTrace()[1].getMethodName();
        outToServer.println(functionName + "," + Integer.toString(Id) +","+ Integer.toString(flightNum));
        return Integer.parseInt(inFromServer.readLine());
    }
    public int queryCars(int Id,String location) {
        String functionName = Thread.currentThread().getStackTrace()[1].getMethodName();
        outToServer.println(functionName + "," + Integer.toString(Id) +","+ location);
        return Integer.parseInt(inFromServer.readLine());
    }
    public int queryRooms(int Id,String location) {
        String functionName = Thread.currentThread().getStackTrace()[1].getMethodName();
        outToServer.println(functionName + "," + Integer.toString(Id) +","+ location);
        return Integer.parseInt(inFromServer.readLine());
    }
    public String queryCustomerInfo(int Id,int customer) {
        String functionName = Thread.currentThread().getStackTrace()[1].getMethodName();
        outToServer.println(functionName + "," + Integer.toString(Id) +","+ Integer.toString(customer));
        return inFromServer.readLine();
    }
    public int queryFlightPrice(int Id,int flightNum) {
        String functionName = Thread.currentThread().getStackTrace()[1].getMethodName();
        outToServer.println(functionName + "," + Integer.toString(Id) +","+ Integer.toString(flightNum));
        return Integer.parseInt(inFromServer.readLine());
    }
    public int queryCarsPrice(int Id,String location) {
        String functionName = Thread.currentThread().getStackTrace()[1].getMethodName();
        outToServer.println(functionName + "," + Integer.toString(Id) +","+ location);
        return Integer.parseInt(inFromServer.readLine());
    }
    public int queryRoomsPrice(int Id,String location) {
        String functionName = Thread.currentThread().getStackTrace()[1].getMethodName();
        outToServer.println(functionName + "," + Integer.toString(Id) +","+ location);
        return Integer.parseInt(inFromServer.readLine());
    }
    public boolean reserveFlight(int Id,int customer,int flightNum) {
        String functionName = Thread.currentThread().getStackTrace()[1].getMethodName();
        outToServer.println(functionName + "," + Integer.toString(Id) +","+ Integer.toString(customer) + "," + Integer.toString(flightNum));
        return Boolean.parseBoolean(inFromServer.readLine());
    }
    public boolean reserveCar(int Id,int customer,String location) {
        String functionName = Thread.currentThread().getStackTrace()[1].getMethodName();
        outToServer.println(functionName + "," + Integer.toString(Id) +","+ Integer.toString(customer) + "," + location);
        return Boolean.parseBoolean(inFromServer.readLine());
    }
    public boolean reserveRoom(int Id,int customer,String location) {
        String functionName = Thread.currentThread().getStackTrace()[1].getMethodName();
        outToServer.println(functionName + "," + Integer.toString(Id) +","+ Integer.toString(customer) + "," + location);
        return Boolean.parseBoolean(inFromServer.readLine());
    }
    public boolean itinerary(int Id,int customer,int flightNumbers,String location,int Car,int Room) {
        String functionName = Thread.currentThread().getStackTrace()[1].getMethodName();
        outToServer.println(functionName + "," + Integer.toString(Id) +","+ Integer.toString(customer) +","+ Integer.toString(flightNumbers) + "," + location + "," + Integer.toString(Car) + "," + Integer.toString(Room));
        return Boolean.parseBoolean(inFromServer.readLine());
    }
    public boolean newCustomer(int Id,int Cid) {
        String functionName = Thread.currentThread().getStackTrace()[1].getMethodName();
        outToServer.println(functionName + "," + Integer.toString(Id) +","+ Integer.toString(Cid));
        return Boolean.parseBoolean(inFromServer.readLine());
    }
}

