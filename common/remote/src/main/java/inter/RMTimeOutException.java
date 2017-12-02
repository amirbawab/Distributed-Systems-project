package inter;

public class RMTimeOutException extends Exception {
    private String m_name;
    public RMTimeOutException(String name){
        super("RM " + name + " is down for a long time");
    }

    /**
     * Get name
     * @return name
     */
    public String getName() {
        return m_name;
    }
}
