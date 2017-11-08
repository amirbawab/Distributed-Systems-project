package lm;

/*
    The transaction is aborted. 
*/

public class TransactionAbortedException extends Exception
{
    private int xid = 0;
    
    public  TransactionAbortedException(int xid, String msg)
    {
        super("The transaction " + xid + " is aborted:" + msg);
        this.xid = xid;
    }
    
    int GetXId()
    {
        return xid;
    }
}
