import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class clientSocket{

public static void main(String args[]) throws IOException
	{
	String serverName=args[0];

	Socket socket= new Socket(serverName, 9090); // establish a socket with a server using the given port#

	PrintWriter outToServer = new PrintWriter(socket.getOutputStream(),true); // open an output stream to the server...
	BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream())); // open an input stream from the server...

	BufferedReader bufferedReader = new java.io.BufferedReader(new InputStreamReader(System.in)); //to read user's input

	while(true) // works forever
		{
		String readerInput=bufferedReader.readLine(); // read user's input

	
		outToServer.println(readerInput); // send the user's input via the output stream to the server
		String res=inFromServer.readLine(); // receive the server's result via the input stream from the server
		System.out.println("result: "+res); // print the server result to the user
		}

	}


}
