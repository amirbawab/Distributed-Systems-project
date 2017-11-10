// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
//
package rm;

import inter.ResourceManager;
import lm.TransactionAbortedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tm.Transaction;

import javax.transaction.InvalidTransactionException;
import java.util.*;
import java.rmi.RemoteException;

public class ResourceManagerImpl implements ResourceManager {

    // Logger
    private static final Logger logger = LogManager.getLogger(ResourceManagerImpl.class);

    // Constants
    private static final int GLOBAL_TABLE=-1;

    // Local table for each transaction
    private Map<Integer, RMHashtable> m_tables;

    /**
     * Construct a new resource manager
     */
    ResourceManagerImpl() {
        m_tables = new HashMap<>();
        m_tables.put(GLOBAL_TABLE, new RMHashtable());
    }

    /**
     * Reads a data item
     * @param id
     * @param key
     * @return item read
     */
    private RMItem readData( int id, String key ) {
        synchronized(getTable(id)) {
            return getTable(id).get(key);
        }
    }

    /**
     * Write a data item
     * @param id
     * @param key
     * @param value
     */
    private void writeData( int id, String key, RMItem value ) {
        synchronized(getTable(id)) {
            getTable(id).put(key, value);
        }
    }
    
    /**
     * Remove the item out of storage
     * @param id
     * @param key
     * @return removed item
     */
    private RMItem removeData(int id, String key) {
        synchronized(getTable(id)) {
            return getTable(id).remove(key);
        }
    }
    
    /**
     * Deletes the entire item
     * @param id
     * @param key
     * @return true if deleted
     */
    private boolean deleteItem(int id, String key) {
        logger.info ("RM::deleteItem(" + id + ", " + key + ") called" );
        ReservableItem curObj = (ReservableItem) readData( id, key );
        // Check if there is such an item in the storage
        if ( curObj == null ) {
            logger.warn("RM::deleteItem(" + id + ", " + key + ") failed--item doesn't exist" );
            return false;
        } else {

            // Verify that the item is safe to be deleted
            if (curObj.getReserved()==0) {
                removeData(id, curObj.getKey());
                logger.info("RM::deleteItem(" + id + ", " + key + ") item deleted" );
                return true;
            } else {
                logger.info("RM::deleteItem(" + id + ", " + key + ") item can't be deleted because some " +
                        "customers reserved it");
                return false;
            }
        }
    }

    /**
     * Query the number of available seats/rooms/cars
     * @param id
     * @param key
     * @return number of available elements
     */
    private int queryNum(int id, String key) {
        logger.info("RM::queryNum(" + id + ", " + key + ") called" );
        ReservableItem curObj = (ReservableItem) readData( id, key);
        int value = 0;  
        if ( curObj != null ) {
            value = curObj.getCount();
        }
        logger.info("RM::queryNum(" + id + ", " + key + ") returns count=" + value);
        return value;
    }    
    
    /**
     * Query the price of an item
     * @param id
     * @param key
     * @return price of an item
     */
    private int queryPrice(int id, String key) {
        logger.info("RM::queryPrice(" + id + ", " + key + ") called" );
        ReservableItem curObj = (ReservableItem) readData( id, key);
        int value = 0; 
        if ( curObj != null ) {
            value = curObj.getPrice();
        }
        logger.info("RM::queryPrice(" + id + ", " + key + ") returns cost=$" + value );
        return value;        
    }
    
    /**
     * Reserve an item
     * @param id
     * @param customerID
     * @param key
     * @param location
     * @return true if reserved
     */
    private boolean reserveItem(int id, int customerID, String key, String location) {
        logger.info("RM::reserveItem( " + id + ", customer=" + customerID + ", " +key+ ", "+location+" ) called" );
        // Read customer object if it exists (and read lock it)
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );        
        if ( cust == null ) {
            logger.warn("RM::reserveCar( " + id + ", " + customerID + ", " + key + ", "+location+")  failed--customer doesn't exist" );
            return false;
        } 
        
        // Check if the item is available
        ReservableItem item = (ReservableItem)readData(id, key);
        if ( item == null ) {
            logger.warn("RM::reserveItem( " + id + ", " + customerID + ", " + key+", " +location+") failed--item doesn't exist" );
            return false;
        } else if (item.getCount()==0) {
            logger.warn("RM::reserveItem( " + id + ", " + customerID + ", " + key+", " + location+") failed--No more items" );
            return false;
        } else {            
            cust.reserve( key, location, item.getPrice());        
            writeData( id, cust.getKey(), cust );
            
            // decrease the number of available items in the storage
            item.setCount(item.getCount() - 1);
            item.setReserved(item.getReserved()+1);
            
            logger.info("RM::reserveItem( " + id + ", " + customerID + ", " + key + ", " +location+") succeeded" );
            return true;
        }        
    }

    /**
     * Create a new flight, or add seats to existing flight
     * NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
     * @param id
     * @param flightNum
     * @param flightSeats
     * @param flightPrice
     * @return true if added successfully
     * @throws RemoteException
     */
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException {
        logger.info("RM::addFlight(" + id + ", " + flightNum + ", $" + flightPrice + ", " + flightSeats + ") called" );
        Flight curObj = (Flight) readData( id, Flight.getKey(flightNum) );
        if ( curObj == null ) {
            // Doesn't exist...add it
            Flight newObj = new Flight( flightNum, flightSeats, flightPrice );
            writeData( id, newObj.getKey(), newObj );
            logger.info("RM::addFlight(" + id + ") created new flight " + flightNum + ", seats=" +
                    flightSeats + ", price=$" + flightPrice );
        } else {
            // Add seats to existing flight and update the price...
            curObj.setCount( curObj.getCount() + flightSeats );
            if ( flightPrice > 0 ) {
                curObj.setPrice( flightPrice );
            }
            writeData( id, curObj.getKey(), curObj );
            logger.info("RM::addFlight(" + id + ") modified existing flight " + flightNum + ", seats=" + curObj.getCount() + ", price=$" + flightPrice );
        }
        return(true);
    }

    /**
     * Delete a flgith
     * @param id
     * @param flightNum
     * @return true if deleted successfully
     * @throws RemoteException
     */
    public boolean deleteFlight(int id, int flightNum) throws RemoteException {
        return deleteItem(id, Flight.getKey(flightNum));
    }

    /**
     * Create a new room location or add rooms to an existing location
     * NOTE: if price <= 0 and the room location already exists, it maintains its current price
     * @param id
     * @param location
     * @param count
     * @param price
     * @return true if added successfully
     * @throws RemoteException
     */
    public boolean addRooms(int id, String location, int count, int price) throws RemoteException {
        logger.info("RM::addRooms(" + id + ", " + location + ", " + count + ", $" + price + ") called" );
        Hotel curObj = (Hotel) readData( id, Hotel.getKey(location) );
        if ( curObj == null ) {
            // doesn't exist...add it
            Hotel newObj = new Hotel( location, count, price );
            writeData( id, newObj.getKey(), newObj );
            logger.info("RM::addRooms(" + id + ") created new room location " + location + ", count=" + count + ", price=$" + price );
        } else {
            // add count to existing object and update price...
            curObj.setCount( curObj.getCount() + count );
            if ( price > 0 ) {
                curObj.setPrice( price );
            }
            writeData( id, curObj.getKey(), curObj );
            logger.info("RM::addRooms(" + id + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price );
        }
        return true;
    }

    /**
     * Delete rooms from a location
     * @param id
     * @param location
     * @return true if deleted successfully
     * @throws RemoteException
     */
    public boolean deleteRooms(int id, String location) throws RemoteException {
        return deleteItem(id, Hotel.getKey(location));
    }

    /**
     * Create a new car location or add cars to an existing location
     * NOTE: if price <= 0 and the location already exists, it maintains its current price
     * @param id
     * @param location
     * @param count
     * @param price
     * @return true if car created
     * @throws RemoteException
     */
    public boolean addCars(int id, String location, int count, int price) throws RemoteException {
        logger.info("RM::addCars(" + id + ", " + location + ", " + count + ", $" + price + ") called" );
        Car curObj = (Car) readData( id, Car.getKey(location) );
        if ( curObj == null ) {
            // car location doesn't exist...add it
            Car newObj = new Car( location, count, price );
            writeData( id, newObj.getKey(), newObj );
            logger.info("RM::addCars(" + id + ") created new location " + location + ", count=" + count + ", price=$" + price );
        } else {
            // add count to existing car location and update price...
            curObj.setCount( curObj.getCount() + count );
            if ( price > 0 ) {
                curObj.setPrice( price );
            }
            writeData( id, curObj.getKey(), curObj );
            logger.info("RM::addCars(" + id + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price );
        }
        return(true);
    }

    /**
     * Delete cars from a location
     * @param id
     * @param location
     * @return true id deleted succesfully
     * @throws RemoteException
     */
    public boolean deleteCars(int id, String location) throws RemoteException {
        return deleteItem(id, Car.getKey(location));
    }

    /**
     * Query the number of empty seats on this flight
     * @param id
     * @param flightNum
     * @return the number empty seats on this flight
     * @throws RemoteException
     */
    public int queryFlight(int id, int flightNum) throws RemoteException {
        return queryNum(id, Flight.getKey(flightNum));
    }

    /**
     * Query price of this flight
     * @param id
     * @param flightNum
     * @return the price of the given flight
     * @throws RemoteException
     */
    public int queryFlightPrice(int id, int flightNum ) throws RemoteException {
        return queryPrice(id, Flight.getKey(flightNum));
    }

    /**
     * Query the number of rooms available at a location
     * @param id
     * @param location
     * @return the number of available rooms
     * @throws RemoteException
     */
    public int queryRooms(int id, String location) throws RemoteException {
        return queryNum(id, Hotel.getKey(location));
    }

    /**
     * Query room price at a given location
     * @param id
     * @param location
     * @return the price of room at a given location
     * @throws RemoteException
     */
    public int queryRoomsPrice(int id, String location) throws RemoteException {
        return queryPrice(id, Hotel.getKey(location));
    }

    //

    /**
     * Query the number of cars available at a location
     * @param id
     * @param location
     * @return the number of cars available at a location
     * @throws RemoteException
     */
    public int queryCars(int id, String location) throws RemoteException {
        return queryNum(id, Car.getKey(location));
    }

    /**
     * Returns price of cars at this location
     * @param id
     * @param location
     * @return the price of a car a given location
     * @throws RemoteException
     */
    public int queryCarsPrice(int id, String location) throws RemoteException {
        return queryPrice(id, Car.getKey(location));
    }

    /**
     * Returns data structure containing customer reservation info. Returns null if the
     * customer doesn't exist. Returns empty RMHashtable if customer exists but has no
     * reservations.
     * @param id
     * @param customerID
     * @return customer reservation info
     * @throws RemoteException
     */
    public RMHashtable getCustomerReservations(int id, int customerID) throws RemoteException {
        logger.info("RM::getCustomerReservations(" + id + ", " + customerID + ") called" );
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        if ( cust == null ) {
            logger.warn("RM::getCustomerReservations failed(" + id + ", " + customerID + ") failed--customer doesn't exist" );
            return null;
        } else {
            return cust.getReservations();
        }
    }

    /**
     * Query a customer bill
     * @param id
     * @param customerID
     * @return bill
     * @throws RemoteException
     */
    public String queryCustomerInfo(int id, int customerID) throws RemoteException {
        logger.info("RM::queryCustomerInfo(" + id + ", " + customerID + ") called" );
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        if ( cust == null ) {
            logger.warn("RM::queryCustomerInfo(" + id + ", " + customerID + ") failed--customer doesn't exist" );
            return "";   // NOTE: don't change this--WC counts on this value indicating a customer does not exist...
        } else {
                String s = cust.printBill();
                logger.info("RM::queryCustomerInfo(" + id + ", " + customerID + "), bill follows..." );
                return s;
        }
    }

    /**
     * Add a new customer
     * @param id
     * @return a unique customer identifier
     * @throws RemoteException
     */
    public int newCustomer(int id) throws RemoteException {
        logger.info("INFO: RM::newCustomer(" + id + ") called" );
        // Generate a globally unique ID for the new customer
        int cid = Integer.parseInt( String.valueOf(id) +
                                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                                String.valueOf( Math.round( Math.random() * 100 + 1 )));
        Customer cust = new Customer( cid );
        writeData( id, cust.getKey(), cust );
        logger.info("RM::newCustomer(" + cid + ") returns ID=" + cid );
        return cid;
    }

    /**
     * I opted to pass in customerID instead. This makes testing easier
     * @param id
     * @param customerID
     * @return true if successful
     * @throws RemoteException
     */
    public boolean newCustomer(int id, int customerID ) throws RemoteException {
        logger.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") called" );
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        if ( cust == null ) {
            cust = new Customer(customerID);
            writeData( id, cust.getKey(), cust );
            logger.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") created a new customer" );
            return true;
        } else {
            logger.info("INFO: RM::newCustomer(" + id + ", " + customerID + ") failed--customer already exists");
            return false;
        }
    }

    /**
     * Deletes customer from the database.
     * @param id
     * @param customerID
     * @return true if customer deleted
     * @throws RemoteException
     */
    public boolean deleteCustomer(int id, int customerID) throws RemoteException {
        logger.info("RM::deleteCustomer(" + id + ", " + customerID + ") called" );
        Customer cust = (Customer) readData( id, Customer.getKey(customerID) );
        if ( cust == null ) {
            logger.warn("RM::deleteCustomer(" + id + ", " + customerID + ") failed--customer doesn't exist" );
            return false;
        } else {            
            // Increase the reserved numbers of all reservable items which the customer reserved. 
            RMHashtable reservationHT = cust.getReservations();
            for (Enumeration e = reservationHT.keys(); e.hasMoreElements();) {        
                String reservedkey = (String) (e.nextElement());
                ReservedItem reserveditem = cust.getReservedItem(reservedkey);
                logger.info("RM::deleteCustomer(" + id + ", " + customerID + ") has reserved " + reserveditem.getKey() + " " +  reserveditem.getCount() +  " times"  );
                ReservableItem item  = (ReservableItem) readData(id, reserveditem.getKey());
                logger.info("RM::deleteCustomer(" + id + ", " + customerID + ") has reserved " + reserveditem.getKey() + "which is reserved" +  item.getReserved() +  " times and is still available " + item.getCount() + " times"  );
                item.setReserved(item.getReserved()-reserveditem.getCount());
                item.setCount(item.getCount()+reserveditem.getCount());
            }
            
            // Remove the customer from the storage
            removeData(id, cust.getKey());
            logger.info("RM::deleteCustomer(" + id + ", " + customerID + ") succeeded" );
            return true;
        }
    }

    /**
     * Adds car reservation to this customer.
     * @param id
     * @param customerID
     * @param location
     * @return true if car reserved
     * @throws RemoteException
     */
    public boolean reserveCar(int id, int customerID, String location) throws RemoteException {
        return reserveItem(id, customerID, Car.getKey(location), location);
    }

    /**
     * Adds room reservation to this customer.
     * @param id
     * @param customerID
     * @param location
     * @return true if room reserved
     * @throws RemoteException
     */
    public boolean reserveRoom(int id, int customerID, String location) throws RemoteException {
        return reserveItem(id, customerID, Hotel.getKey(location), location);
    }

    /**
     * Adds flight reservation to this customer.
     * @param id
     * @param customerID
     * @param flightNum
     * @return true if flight reserved
     * @throws RemoteException
     */
    public boolean reserveFlight(int id, int customerID, int flightNum) throws RemoteException {
        return reserveItem(id, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
    }
    
    /**
     * Reserve an itinerary
     * @param id
     * @param customer
     * @param flightNumbers
     * @param location
     * @param car
     * @param room
     * @return true if all reservations are successful
     * @throws RemoteException
     */
    public boolean itinerary(int id,int customer, Vector flightNumbers,String location, boolean car, boolean room)
            throws RemoteException  {
        // NOTE:
        // Implementation is handled by the middleware server
        return false;
    }

    @Override
    public int start() throws RemoteException {
        return 0;
    }

    @Override
    public boolean commit(int transactionId) throws RemoteException, TransactionAbortedException, InvalidTransactionException {
        return false;
    }

    @Override
    public void abort(int transactionId) throws RemoteException, InvalidTransactionException {

    }

    @Override
    public boolean shutdown() throws RemoteException {
        return false;
    }

    /**
     * Get local table
     * If table for the specified transaction id does not
     * exist, then create it then return it
     * @param transactionId
     * @return copy of the local table
     */
    public RMHashtable getTable(int transactionId) {
        if(m_tables.containsKey(transactionId)) {
            return m_tables.get(transactionId);
        }
        RMHashtable localTable = new RMHashtable(m_tables.get(GLOBAL_TABLE));
        m_tables.put(transactionId, localTable);
        return localTable;
    }

//    Returns the number of reservations for this flight.
//    public int queryFlightReservations(int id, int flightNum)
//        throws RemoteException
//    {
//        logger.info("RM::queryFlightReservations(" + id + ", #" + flightNum + ") called" );
//        RMInteger numReservations = (RMInteger) readData( id, Flight.getNumReservationsKey(flightNum) );
//        if ( numReservations == null ) {
//            numReservations = new RMInteger(0);
//        } // if
//        logger.info("RM::queryFlightReservations(" + id + ", #" + flightNum + ") returns " + numReservations );
//        return numReservations.getValue();
//    }

//     Frees flight reservation record. Flight reservation records help us make sure we
//     don't delete a flight if one or more customers are holding reservations
//    public boolean freeFlightReservation(int id, int flightNum)
//        throws RemoteException
//    {
//        logger.info("RM::freeFlightReservations(" + id + ", " + flightNum + ") called" );
//        RMInteger numReservations = (RMInteger) readData( id, Flight.getNumReservationsKey(flightNum) );
//        if ( numReservations != null ) {
//            numReservations = new RMInteger( Math.max( 0, numReservations.getValue()-1) );
//        } // if
//        writeData(id, Flight.getNumReservationsKey(flightNum), numReservations );
//        logger.info("RM::freeFlightReservations(" + id + ", " + flightNum + ") succeeded, this flight now has "
//                + numReservations + " reservations" );
//        return true;
//    }
}
