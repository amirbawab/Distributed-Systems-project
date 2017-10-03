package client;

import inter.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.Vector;

public class CLI {

    // Store a reference to the resource manager
    private ResourceManager m_resourceManager= null;

    // Logger
    private static final Logger logger = LogManager.getLogger(CLI.class);

    /**
     * Construct a CLI
     * @param resourceManager
     */
    public CLI(ResourceManager resourceManager) {
        this.m_resourceManager = resourceManager;
    }

    /**
     * Parse input
     * @param command
     * @return vector of tokenize command
     */
    private Vector<String> parse(String command) {
        Vector<String> arguments = new Vector<>();
        StringTokenizer tokenizer = new StringTokenizer(command,",");
        String argument ="";
        while (tokenizer.hasMoreTokens()) {
            argument = tokenizer.nextToken();
            argument = argument.trim();
            arguments.add(argument);
        }
        return arguments;
    }

    /**
     * Star the command line interface
     */
    public boolean start() {

        // Prepare temp variables for the arguments
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        String command, location;
        int Id, Cid, flightNum, flightPrice, flightSeats, price, numRooms, numCars;

        // Start client application
        System.out.println("\n\n\tClient Interface");
        System.out.println("Type \"help\" for list of supported commands");
        boolean inputEnabled = true;
        while(inputEnabled) {
            System.out.print("\n>");
            try{
                command =stdin.readLine();
            } catch (IOException io){
                logger.error("Unable to read from standard in");
                return false;
            }

            // Remove heading and trailing white space
            command=command.trim();
            Vector<String> arguments = parse(command);

            //decide which of the commands this was
            switch(ResourceManager.Command.getFunctionByName(arguments.elementAt(0))){
                case HELP:
                    if(arguments.size()==1)   //command was "help"
                        listCommands();
                    else if (arguments.size()==2)  //command was "help <commandname>"
                        listSpecific(arguments.elementAt(1));
                    else  //wrong use of help command
                        System.out.println("Improper use of help command. Type help or help, <commandname>");
                    break;

                case ADD_FLIGHT:
                    if(arguments.size()!=5){
                        wrongNumber();
                        break;
                    }
                    System.out.println("Adding a new Flight using id: "+arguments.elementAt(1));
                    System.out.println("Flight number: "+arguments.elementAt(2));
                    System.out.println("Add Flight Seats: "+arguments.elementAt(3));
                    System.out.println("Set Flight Price: "+arguments.elementAt(4));

                    try{
                        Id = Integer.parseInt(arguments.elementAt(1));
                        flightNum = Integer.parseInt(arguments.elementAt(2));
                        flightSeats = Integer.parseInt(arguments.elementAt(3));
                        flightPrice = Integer.parseInt(arguments.elementAt(4));
                        if(m_resourceManager.addFlight(Id,flightNum,flightSeats,flightPrice))
                            System.out.println("Flight added");
                        else
                            System.out.println("Flight could not be added");
                    } catch(Exception e){
                        logger.error(e.getMessage());
                        return false;
                    }
                    break;

                case ADD_CARS:
                    if(arguments.size()!=5){
                        wrongNumber();
                        break;
                    }
                    System.out.println("Adding a new Car using id: "+arguments.elementAt(1));
                    System.out.println("Car Location: "+arguments.elementAt(2));
                    System.out.println("Add Number of Cars: "+arguments.elementAt(3));
                    System.out.println("Set Price: "+arguments.elementAt(4));
                    try{
                        Id = Integer.parseInt(arguments.elementAt(1));
                        location = arguments.elementAt(2);
                        numCars = Integer.parseInt(arguments.elementAt(3));
                        price = Integer.parseInt(arguments.elementAt(4));
                        if(m_resourceManager.addCars(Id,location,numCars,price))
                            System.out.println("Cars added");
                        else
                            System.out.println("Cars could not be added");
                    } catch(Exception e){
                        logger.error(e.getMessage());
                        return false;
                    }
                    break;

                case ADD_ROOMS:
                    if(arguments.size()!=5){
                        wrongNumber();
                        break;
                    }
                    System.out.println("Adding a new Room using id: "+arguments.elementAt(1));
                    System.out.println("Room Location: "+arguments.elementAt(2));
                    System.out.println("Add Number of Rooms: "+arguments.elementAt(3));
                    System.out.println("Set Price: "+arguments.elementAt(4));
                    try{
                        Id = Integer.parseInt(arguments.elementAt(1));
                        location = arguments.elementAt(2);
                        numRooms = Integer.parseInt(arguments.elementAt(3));
                        price = Integer.parseInt(arguments.elementAt(4));
                        if(m_resourceManager.addRooms(Id,location,numRooms,price))
                            System.out.println("Rooms added");
                        else
                            System.out.println("Rooms could not be added");
                    } catch(Exception e){
                        logger.error(e.getMessage());
                        return false;
                    }
                    break;

                case NEW_CUSTOMER:
                    if(arguments.size()!=2){
                        wrongNumber();
                        break;
                    }
                    System.out.println("Adding a new Customer using id:"+arguments.elementAt(1));
                    try{
                        Id = Integer.parseInt(arguments.elementAt(1));
                        int customer=m_resourceManager.newCustomer(Id);
                        System.out.println("new customer id:"+customer);
                    } catch(Exception e){
                        logger.error(e.getMessage());
                        return false;
                    }
                    break;

                case DELETE_FLIGHT:
                    if(arguments.size()!=3){
                        wrongNumber();
                        break;
                    }
                    System.out.println("Deleting a flight using id: "+arguments.elementAt(1));
                    System.out.println("Flight Number: "+arguments.elementAt(2));
                    try{
                        Id = Integer.parseInt(arguments.elementAt(1));
                        flightNum = Integer.parseInt(arguments.elementAt(2));
                        if(m_resourceManager.deleteFlight(Id,flightNum))
                            System.out.println("Flight Deleted");
                        else
                            System.out.println("Flight could not be deleted");
                    } catch(Exception e){
                        logger.error(e.getMessage());
                        return false;
                    }
                    break;

                case DELETE_CARS:
                    if(arguments.size()!=3){
                        wrongNumber();
                        break;
                    }
                    System.out.println("Deleting the cars from a particular location  using id: "+arguments.elementAt(1));
                    System.out.println("Car Location: "+arguments.elementAt(2));
                    try{
                        Id = Integer.parseInt(arguments.elementAt(1));
                        location = arguments.elementAt(2);

                        if(m_resourceManager.deleteCars(Id,location))
                            System.out.println("Cars Deleted");
                        else
                            System.out.println("Cars could not be deleted");
                    } catch(Exception e){
                        logger.error(e.getMessage());
                        return false;
                    }
                    break;

                case DELETE_ROOMS:
                    if(arguments.size()!=3){
                        wrongNumber();
                        break;
                    }
                    System.out.println("Deleting all rooms from a particular location  using id: "+arguments.elementAt(1));
                    System.out.println("Room Location: "+arguments.elementAt(2));
                    try{
                        Id = Integer.parseInt(arguments.elementAt(1));
                        location = arguments.elementAt(2);
                        if(m_resourceManager.deleteRooms(Id,location))
                            System.out.println("Rooms Deleted");
                        else
                            System.out.println("Rooms could not be deleted");
                    } catch(Exception e){
                        logger.error(e.getMessage());
                        return false;
                    }
                    break;

                case DELETE_CUSTOMER:
                    if(arguments.size()!=3){
                        wrongNumber();
                        break;
                    }
                    System.out.println("Deleting a customer from the database using id: "+arguments.elementAt(1));
                    System.out.println("Customer id: "+arguments.elementAt(2));
                    try{
                        Id = Integer.parseInt(arguments.elementAt(1));
                        int customer = Integer.parseInt(arguments.elementAt(2));
                        if(m_resourceManager.deleteCustomer(Id,customer))
                            System.out.println("Customer Deleted");
                        else
                            System.out.println("Customer could not be deleted");
                    } catch(Exception e){
                        logger.error(e.getMessage());
                        return false;
                    }
                    break;

                case QUERY_FLIGHT:
                    if(arguments.size()!=3){
                        wrongNumber();
                        break;
                    }
                    System.out.println("Querying a flight using id: "+arguments.elementAt(1));
                    System.out.println("Flight number: "+arguments.elementAt(2));
                    try{
                        Id = Integer.parseInt(arguments.elementAt(1));
                        flightNum = Integer.parseInt(arguments.elementAt(2));
                        int seats=m_resourceManager.queryFlight(Id,flightNum);
                        System.out.println("Number of seats available:"+seats);
                    } catch(Exception e){
                        logger.error(e.getMessage());
                        return false;
                    }
                    break;

                case QUERY_CARS:
                    if(arguments.size()!=3){
                        wrongNumber();
                        break;
                    }
                    System.out.println("Querying a car location using id: "+arguments.elementAt(1));
                    System.out.println("Car location: "+arguments.elementAt(2));
                    try{
                        Id = Integer.parseInt(arguments.elementAt(1));
                        location = arguments.elementAt(2);
                        numCars=m_resourceManager.queryCars(Id,location);
                        System.out.println("number of Cars at this location:"+numCars);
                    } catch(Exception e){
                        logger.error(e.getMessage());
                        return false;
                    }
                    break;

                case QUERY_ROOMS:
                    if(arguments.size()!=3){
                        wrongNumber();
                        break;
                    }
                    System.out.println("Querying a room location using id: "+arguments.elementAt(1));
                    System.out.println("Room location: "+arguments.elementAt(2));
                    try{
                        Id = Integer.parseInt(arguments.elementAt(1));
                        location = arguments.elementAt(2);
                        numRooms=m_resourceManager.queryRooms(Id,location);
                        System.out.println("number of Rooms at this location:"+numRooms);
                    } catch(Exception e){
                        logger.error(e.getMessage());
                        return false;
                    }
                    break;

                case QUERY_CUSTOMER_INFO:
                    if(arguments.size()!=3){
                        wrongNumber();
                        break;
                    }
                    System.out.println("Querying Customer information using id: "+arguments.elementAt(1));
                    System.out.println("Customer id: "+arguments.elementAt(2));
                    try{
                        Id = Integer.parseInt(arguments.elementAt(1));
                        int customer = Integer.parseInt(arguments.elementAt(2));
                        String bill=m_resourceManager.queryCustomerInfo(Id,customer);
                        System.out.println("Customer info:" + bill.replace("@","\n"));
                    } catch(Exception e){
                        logger.error(e.getMessage());
                        return false;
                    }
                    break;

                case QUERY_FLIGHT_PRICE:
                    if(arguments.size()!=3){
                        wrongNumber();
                        break;
                    }
                    System.out.println("Querying a flight Price using id: "+arguments.elementAt(1));
                    System.out.println("Flight number: "+arguments.elementAt(2));
                    try{
                        Id = Integer.parseInt(arguments.elementAt(1));
                        flightNum = Integer.parseInt(arguments.elementAt(2));
                        price=m_resourceManager.queryFlightPrice(Id,flightNum);
                        System.out.println("Price of a seat:"+price);
                    } catch(Exception e){
                        logger.error(e.getMessage());
                        return false;
                    }
                    break;

                case QUERY_CARS_PRICE:
                    if(arguments.size()!=3){
                        wrongNumber();
                        break;
                    }
                    System.out.println("Querying a car price using id: "+arguments.elementAt(1));
                    System.out.println("Car location: "+arguments.elementAt(2));
                    try{
                        Id = Integer.parseInt(arguments.elementAt(1));
                        location = arguments.elementAt(2);
                        price=m_resourceManager.queryCarsPrice(Id,location);
                        System.out.println("Price of a car at this location:"+price);
                    } catch(Exception e){
                        logger.error(e.getMessage());
                        return false;
                    }
                    break;

                case QUERY_ROOMS_PRICE:
                    if(arguments.size()!=3){
                        wrongNumber();
                        break;
                    }
                    System.out.println("Querying a room price using id: "+arguments.elementAt(1));
                    System.out.println("Room Location: "+arguments.elementAt(2));
                    try{
                        Id = Integer.parseInt(arguments.elementAt(1));
                        location = arguments.elementAt(2);
                        price=m_resourceManager.queryRoomsPrice(Id,location);
                        System.out.println("Price of Rooms at this location:"+price);
                    } catch(Exception e){
                        logger.error(e.getMessage());
                        return false;
                    }
                    break;

                case RESERVE_FLIGHT:
                    if(arguments.size()!=4){
                        wrongNumber();
                        break;
                    }
                    System.out.println("Reserving a seat on a flight using id: "+arguments.elementAt(1));
                    System.out.println("Customer id: "+arguments.elementAt(2));
                    System.out.println("Flight number: "+arguments.elementAt(3));
                    try{
                        Id = Integer.parseInt(arguments.elementAt(1));
                        int customer = Integer.parseInt(arguments.elementAt(2));
                        flightNum = Integer.parseInt(arguments.elementAt(3));
                        if(m_resourceManager.reserveFlight(Id,customer,flightNum))
                            System.out.println("Flight Reserved");
                        else
                            System.out.println("Flight could not be reserved.");
                    } catch(Exception e){
                        logger.error(e.getMessage());
                        return false;
                    }
                    break;

                case RESERVE_CAR:
                    if(arguments.size()!=4){
                        wrongNumber();
                        break;
                    }
                    System.out.println("Reserving a car at a location using id: "+arguments.elementAt(1));
                    System.out.println("Customer id: "+arguments.elementAt(2));
                    System.out.println("Location: "+arguments.elementAt(3));

                    try{
                        Id = Integer.parseInt(arguments.elementAt(1));
                        int customer = Integer.parseInt(arguments.elementAt(2));
                        location = arguments.elementAt(3);

                        if(m_resourceManager.reserveCar(Id,customer,location))
                            System.out.println("Car Reserved");
                        else
                            System.out.println("Car could not be reserved.");
                    } catch(Exception e){
                        logger.error(e.getMessage());
                        return false;
                    }
                    break;

                case RESERVE_ROOM:
                    if(arguments.size()!=4){
                        wrongNumber();
                        break;
                    }
                    System.out.println("Reserving a room at a location using id: "+arguments.elementAt(1));
                    System.out.println("Customer id: "+arguments.elementAt(2));
                    System.out.println("Location: "+arguments.elementAt(3));
                    try{
                        Id = Integer.parseInt(arguments.elementAt(1));
                        int customer = Integer.parseInt(arguments.elementAt(2));
                        location = arguments.elementAt(3);

                        if(m_resourceManager.reserveRoom(Id,customer,location))
                            System.out.println("Room Reserved");
                        else
                            System.out.println("Room could not be reserved.");
                    } catch(Exception e){
                        logger.error(e.getMessage());
                        return false;
                    }
                    break;

                case ITINERARY:
                    if(arguments.size()<7){
                        wrongNumber();
                        break;
                    }
                    System.out.println("Reserving an Itinerary using id:"+arguments.elementAt(1));
                    System.out.println("Customer id:"+arguments.elementAt(2));
                    for(int i=0;i<arguments.size()-6;i++)
                        System.out.println("Flight number"+arguments.elementAt(3+i));
                    System.out.println("Location for Car/Room booking:"+arguments.elementAt(arguments.size()-3));
                    System.out.println("Car to book?:"+arguments.elementAt(arguments.size()-2));
                    System.out.println("Room to book?:"+arguments.elementAt(arguments.size()-1));
                    try{
                        Id = Integer.parseInt(arguments.elementAt(1));
                        int customer = Integer.parseInt(arguments.elementAt(2));
                        Vector<String> flightNumbers = new Vector<>();
                        for(int i=0;i<arguments.size()-6;i++) {
                            flightNumbers.addElement(arguments.elementAt(3 + i));
                        }
                        location = arguments.elementAt(arguments.size()-3);
                        boolean Car = Boolean.parseBoolean(arguments.elementAt(arguments.size()-2));
                        boolean Room = Boolean.parseBoolean(arguments.elementAt(arguments.size()-1));

                        if(m_resourceManager.itinerary(Id,customer,flightNumbers,location,Car,Room))
                            System.out.println("Itinerary Reserved");
                        else
                            System.out.println("Itinerary could not be reserved.");
                    } catch(Exception e){
                        logger.error(e.getMessage());
                        return false;
                    }
                    break;

                case QUIT:
                    if(arguments.size()!=1){
                        wrongNumber();
                        break;
                    }
                    System.out.println("Quitting client.");
                    inputEnabled = false;
                    break;

                case NEW_CUSTOMER_ID:
                    if(arguments.size()!=3){
                        wrongNumber();
                        break;
                    }
                    System.out.println("Adding a new Customer using id:"+arguments.elementAt(1) + " and cid " +arguments.elementAt(2));
                    try{
                        Id = Integer.parseInt(arguments.elementAt(1));
                        Cid = Integer.parseInt(arguments.elementAt(2));
                        m_resourceManager.newCustomer(Id,Cid);
                        System.out.println("new customer id:"+Cid);
                    } catch(Exception e){
                        logger.error(e.getMessage());
                        return false;
                    }
                    break;

                default:
                    System.out.println("The interface does not support this command.");
                    break;
            }
        }
        return true;
    }

    /**
     * Print all commands
     */
    private void listCommands() {
        System.out.println("\nWelcome to the client interface provided to test your project.");
        System.out.println("Commands accepted by the interface are:");
        System.out.println(ResourceManager.Command.listCommands());
        System.out.println("\ntype help, <commandname> for detailed info(NOTE the use of comma).");
    }

    /**
     * Print command usage
     * @param commandStr
     */
    private void listSpecific(String commandStr) {
        System.out.print("Help on: ");
        ResourceManager.Command command = ResourceManager.Command.getFunctionByName(commandStr);
        switch(command) {
            case HELP:
                System.out.println("Help");
                System.out.println("\nTyping help on the prompt gives a list of all the commands available.");
                System.out.println("Typing " + command.getName() + ", <commandname> gives details on how to use the particular command.");
                break;

            case ADD_FLIGHT:
                System.out.println("Adding a new Flight.");
                System.out.println("Purpose:");
                System.out.println("\tAdd information about a new flight.");
                System.out.println("\nUsage:");
                System.out.println("\t" + command.getName() +",<id>,<flightnumber>,<flightSeats>,<flightprice>");
                break;

            case ADD_CARS:
                System.out.println("Adding a new Car.");
                System.out.println("Purpose:");
                System.out.println("\tAdd information about a new car location.");
                System.out.println("\nUsage:");
                System.out.println("\t" + command.getName() + ",<id>,<location>,<numberofcars>,<pricepercar>");
                break;

            case ADD_ROOMS:
                System.out.println("Adding a new Room.");
                System.out.println("Purpose:");
                System.out.println("\tAdd information about a new room location.");
                System.out.println("\nUsage:");
                System.out.println("\t" + command.getName() + ",<id>,<location>,<numberofrooms>,<priceperroom>");
                break;

            case NEW_CUSTOMER:
                System.out.println("Adding a new Customer.");
                System.out.println("Purpose:");
                System.out.println("\tGet the system to provide a new customer id. (same as adding a new customer)");
                System.out.println("\nUsage:");
                System.out.println("\t" + command.getName() + ",<id>");
                break;


            case DELETE_FLIGHT:
                System.out.println("Deleting a flight");
                System.out.println("Purpose:");
                System.out.println("\tDelete a flight's information.");
                System.out.println("\nUsage:");
                System.out.println("\t" + command.getName() + ",<id>,<flightnumber>");
                break;

            case DELETE_CARS:
                System.out.println("Deleting a Car");
                System.out.println("Purpose:");
                System.out.println("\tDelete all cars from a location.");
                System.out.println("\nUsage:");
                System.out.println("\t" +command.getName()+ ",<id>,<location>,<numCars>");
                break;

            case DELETE_ROOMS:
                System.out.println("Deleting a Room");
                System.out.println("\nPurpose:");
                System.out.println("\tDelete all rooms from a location.");
                System.out.println("Usage:");
                System.out.println("\t" + command.getName() + ",<id>,<location>,<numRooms>");
                break;

            case DELETE_CUSTOMER:
                System.out.println("Deleting a Customer");
                System.out.println("Purpose:");
                System.out.println("\tRemove a customer from the database.");
                System.out.println("\nUsage:");
                System.out.println("\t" + command.getName() + ",<id>,<customerid>");
                break;

            case QUERY_FLIGHT:
                System.out.println("Querying flight.");
                System.out.println("Purpose:");
                System.out.println("\tObtain Seat information about a certain flight.");
                System.out.println("\nUsage:");
                System.out.println("\t" + command.getName() + ",<id>,<flightnumber>");
                break;

            case QUERY_CARS:
                System.out.println("Querying a Car location.");
                System.out.println("Purpose:");
                System.out.println("\tObtain number of cars at a certain car location.");
                System.out.println("\nUsage:");
                System.out.println("\t" + command.getName() + ",<id>,<location>");
                break;

            case QUERY_ROOMS:
                System.out.println("Querying a Room Location.");
                System.out.println("Purpose:");
                System.out.println("\tObtain number of rooms at a certain room location.");
                System.out.println("\nUsage:");
                System.out.println("\t" + command.getName() + ",<id>,<location>");
                break;

            case QUERY_CUSTOMER_INFO:
                System.out.println("Querying Customer Information.");
                System.out.println("Purpose:");
                System.out.println("\tObtain information about a customer.");
                System.out.println("\nUsage:");
                System.out.println("\t" + command.getName() + ",<id>,<customerid>");
                break;

            case QUERY_FLIGHT_PRICE:
                System.out.println("Querying flight.");
                System.out.println("Purpose:");
                System.out.println("\tObtain price information about a certain flight.");
                System.out.println("\nUsage:");
                System.out.println("\tqueryflightprice,<id>,<flightnumber>");
                break;

            case QUERY_CARS_PRICE:
                System.out.println("Querying a Car location.");
                System.out.println("Purpose:");
                System.out.println("\tObtain price information about a certain car location.");
                System.out.println("\nUsage:");
                System.out.println("\t" + command.getName() + ",<id>,<location>");
                break;

            case QUERY_ROOMS_PRICE:
                System.out.println("Querying a Room Location.");
                System.out.println("Purpose:");
                System.out.println("\tObtain price information about a certain room location.");
                System.out.println("\nUsage:");
                System.out.println("\t" + command.getName() + ",<id>,<location>");
                break;

            case RESERVE_FLIGHT:
                System.out.println("Reserving a flight.");
                System.out.println("Purpose:");
                System.out.println("\tReserve a flight for a customer.");
                System.out.println("\nUsage:");
                System.out.println("\t" + command.getName() + ",<id>,<customerid>,<flightnumber>");
                break;

            case RESERVE_CAR:
                System.out.println("Reserving a Car.");
                System.out.println("Purpose:");
                System.out.println("\tReserve a given number of cars for a customer at a particular location.");
                System.out.println("\nUsage:");
                System.out.println("\t" + command.getName() + ",<id>,<customerid>,<location>");
                break;

            case RESERVE_ROOM:
                System.out.println("Reserving a Room.");
                System.out.println("Purpose:");
                System.out.println("\tReserve a given number of rooms for a customer at a particular location.");
                System.out.println("\nUsage:");
                System.out.println("\t" + command.getName() + ",<id>,<customerid>,<location>");
                break;

            case ITINERARY:
                System.out.println("Reserving an Itinerary.");
                System.out.println("Purpose:");
                System.out.println("\tBook one or more flights.Also book zero or more cars/rooms at a location.");
                System.out.println("\nUsage:");
                System.out.println("\t" + command.getName() + ",<id>,<customerid>,<flightnumber1>,...,<flightnumberN>,<LocationToBookCarsOrRooms>,<wantCar>,<wantRoom>");
                break;


            case QUIT:
                System.out.println("Quitting client.");
                System.out.println("Purpose:");
                System.out.println("\tExit the client application.");
                System.out.println("\nUsage:");
                System.out.println("\t" + command.getName());
                break;

            case NEW_CUSTOMER_ID:
                System.out.println("Create new customer providing an id");
                System.out.println("Purpose:");
                System.out.println("\tCreates a new customer with the id provided");
                System.out.println("\nUsage:");
                System.out.println("\t" + command.getName() + ", <id>, <customerid>");
                break;

            default:
                System.out.println(command);
                System.out.println("The interface does not support this command.");
                break;
        }
    }

    /**
     * Print error message
     */
    private void wrongNumber() {
        System.out.println("The number of arguments provided in this command are wrong.");
        System.out.println("Type help, <commandname> to check usage of this command.");
    }
}