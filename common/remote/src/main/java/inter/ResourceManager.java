package inter;


import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.*;
/** 
 * Simplified version from CSE 593 Univ. of Washington
 *
 * Distributed  System in Java.
 * 
 * failure reporting is done using two pieces, exceptions and boolean 
 * return values.  Exceptions are used for systemy things. Return
 * values are used for operations that would affect the consistency
 * 
 * If there is a boolean return value and you're not sure how it 
 * would be used in your implementation, ignore it.  I used boolean
 * return values in the interface generously to allow flexibility in 
 * implementation.  But don't forget to return true when the operation
 * has succeeded.
 */

public interface ResourceManager extends Remote, ResourceManagerActions {

    // RM object reference
    String MID_SERVER_REF = "mid-server";
    String RM_CAR_REF = "car";
    String RM_ROOM_REF = "room";
    String RM_FLIGHT_REF = "flight";

    // Function names
    public enum Command {
        HELP("help"),
        QUIT("quit"),
        NOT_FOUND(""),
        ADD_FLIGHT("addFlight"),
        ADD_CARS("addCars"),
        ADD_ROOMS("addRooms"),
        NEW_CUSTOMER("newCustomer"),
        NEW_CUSTOMER_ID("newCustomerId"),
        DELETE_FLIGHT("deleteFlight"),
        DELETE_CARS("deleteCars"),
        DELETE_ROOMS("deleteRooms"),
        DELETE_CUSTOMER("deleteCustomer"),
        QUERY_FLIGHT("queryFlight"),
        QUERY_CARS("queryCars"),
        QUERY_ROOMS("queryRooms"),
        QUERY_CUSTOMER_INFO("queryCustomerInfo"),
        QUERY_FLIGHT_PRICE("queryFlightPrice"),
        QUERY_CARS_PRICE("queryCarsPrice"),
        QUERY_ROOMS_PRICE("queryRoomsPrice"),
        RESERVE_FLIGHT("reserveFlight"),
        RESERVE_CAR("reserveCar"),
        RESERVE_ROOM("reserveRoom"),
        ITINERARY("itinerary"),
        ;
        private String m_functionName;
        private int m_id = 0;
        private int uid = 1;
        Command(String functionName) {
            this.m_functionName = functionName;
            this.m_id = uid++;
        }

        /**
         * Get function name
         * @return function name
         */
        public String getName() {
            return this.m_functionName;
        }

        /**
         * Get function unique id
         * @return function unique id
         */
        public int getId() {
            return this.m_id;
        }

        public static String listCommands() {
            StringBuilder stringBuilder = new StringBuilder();
            for(Command command : values()) {
                stringBuilder.append(command.getName()+"\n");
            }
            return stringBuilder.toString();
        }

        /**
         * Get Function by name
         * @param functionName
         * @return Function
         */
        public static Command getFunctionByName(String functionName) {
            for(ResourceManager.Command function : Command.values()) {
                if(function.getName().equalsIgnoreCase(functionName)) {
                    return function;
                }
            }
            return NOT_FOUND;
        }
    }

    /* Add seats to a flight.  In general this will be used to create a new
     * flight, but it should be possible to add seats to an existing flight.
     * Adding to an existing flight should overwrite the current price of the
     * available seats.
     *
     * @return success.
     */
    boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice)
	throws RemoteException; 
    
    /* Add cars to a location.  
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     */
    boolean addCars(int id, String location, int numCars, int price)
	throws RemoteException; 
   
    /* Add rooms to a location.  
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     */
    boolean addRooms(int id, String location, int numRooms, int price)
	throws RemoteException; 			    

			    
    /* new customer just returns a unique customer identifier */
    int newCustomer(int id)
	throws RemoteException; 
    
    /* new customer with providing id */
    boolean newCustomer(int id, int cid)
    throws RemoteException;

    /**
     *   Delete the entire flight.
     *   deleteflight implies whole deletion of the flight.  
     *   all seats, all reservations.  If there is a reservation on the flight, 
     *   then the flight cannot be deleted
     *
     * @return success.
     */   
    boolean deleteFlight(int id, int flightNum)
	throws RemoteException; 
    
    /* Delete all Cars from a location.
     * It may not succeed if there are reservations for this location
     *
     * @return success
     */		    
    boolean deleteCars(int id, String location)
	throws RemoteException; 

    /* Delete all Rooms from a location.
     * It may not succeed if there are reservations for this location.
     *
     * @return success
     */
    boolean deleteRooms(int id, String location)
	throws RemoteException; 
    
    /* deleteCustomer removes the customer and associated reservations */
    boolean deleteCustomer(int id,int customer)
	throws RemoteException; 

    /* queryFlight returns the number of empty seats. */
    int queryFlight(int id, int flightNumber)
	throws RemoteException; 

    /* return the number of cars available at a location */
    int queryCars(int id, String location)
	throws RemoteException; 

    /* return the number of rooms available at a location */
    int queryRooms(int id, String location)
	throws RemoteException; 

    /* return a bill */
    String queryCustomerInfo(int id,int customer)
	throws RemoteException; 
    
    /* queryFlightPrice returns the price of a seat on this flight. */
    int queryFlightPrice(int id, int flightNumber)
	throws RemoteException; 

    /* return the price of a car at a location */
    int queryCarsPrice(int id, String location)
	throws RemoteException; 

    /* return the price of a room at a location */
    int queryRoomsPrice(int id, String location)
	throws RemoteException; 

    /* Reserve a seat on this flight*/
    boolean reserveFlight(int id, int customer, int flightNumber)
	throws RemoteException; 

    /* reserve a car at this location */
    boolean reserveCar(int id, int customer, String location)
	throws RemoteException; 

    /* reserve a room certain at this location */
    boolean reserveRoom(int id, int customer, String locationd)
	throws RemoteException; 


    /* reserve an itinerary */
    boolean itinerary(int id,int customer,Vector flightNumbers,String location, boolean Car, boolean Room)
	throws RemoteException; 
    			
}
