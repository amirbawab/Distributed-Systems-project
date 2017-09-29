import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.io.BufferedReader;
import java.io.InputStreamReader;


public class serverSocketThread extends Thread
{
Socket socket;
serverSocketThread (Socket socket)
{
this.socket=socket;
}

public void run()
{

	try
		{
		BufferedReader inFromClient= new BufferedReader(new InputStreamReader(socket.getInputStream()));
		PrintWriter outToClient = new PrintWriter(socket.getOutputStream(), true);
			String message = null;
			while ((message = inFromClient.readLine())!=null)
			{

		System.out.println("message:"+message);
		String result="Working!";
		/**
		**/
		// System.out.println("message:"+message);
		
		
		simpleMath sm=new simpleMath();
		String[] params =  message.split(",");
		int x= Integer.parseInt(params[1]);
		int y= Integer.parseInt(params[2]);
		int res=0;
		System.out.println(params[0] +"--"+params[1]+"--"+params[2]);
		if (params[0].equals("mul"))
			 res=sm.mul(x,y);
		else if (params[0].equals("add"))
			 res=sm.add(x,y);

		 outToClient.println("hello client from server THREAD, your result is: " + res );

			}
	socket.close();
		}
	catch (IOException e)
	{
	
	}

}



}








/*		
		simpleMath sm=new simpleMath();
		int number=Integer.parseInt(message);
		int result=sm.addThree(number);
		


simpleMath sm=new simpleMath();
String[] params =  message.split(",");
int number= Integer.parseInt(params[1]);
if (params[0]=="mul")
		int result=sm.mulThree(number);
else if (params[0]=="add")
		int result=sm.addThree(number);

*/


