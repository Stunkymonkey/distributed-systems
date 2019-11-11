package de.unistgt.ipvs.vs.ex1.client;

import de.unistgt.ipvs.vs.ex1.common.MessageReader;

import java.io.*;
import java.net.Socket;

/**
 * Implement the connectTo-, disconnect-, and calculate-method of this class
 * as necessary to complete the assignment. You may also add some fields or methods.
 */
public class CalcSocketClient {
	private Socket cliSocket;
	private int    rcvdOKs;		// --> Number of valid message contents
	private int    rcvdErs;		// --> Number of invalid message contents
	private int    calcRes;		// --> Calculation result (cf.  'RES')

	private OutputStream out;
	private MessageReader in;
	
	public CalcSocketClient() {
		this.cliSocket = null;
		this.rcvdOKs   = 0;
		this.rcvdErs   = 0;
		this.calcRes   = 0;
	}
	
	public int getRcvdOKs() {
		return rcvdOKs;
	}

	public int getRcvdErs() {
		return rcvdErs;
	}

	public int getCalcRes() {
		return calcRes;
	}

	public boolean connectTo(String srvIP, int srvPort) {
               
		//Solution here
		try {
			cliSocket = new Socket(srvIP, srvPort);
			out = cliSocket.getOutputStream();
			in = new MessageReader(cliSocket.getInputStream());

			// check if RDY was sent, else exit
			String rdyMsg = in.next();
			if(!rdyMsg.equals("<08:RDY>")) {
				System.err.println("The server did not send the RDY message.");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean disconnect() {
	    //Solution here
		try {
			cliSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean calculate(String request) {
		if (cliSocket == null) {
			System.err.println("Client not connected!");
			return false;
		}
		try {
			// send the request to the server
			out.write(request.getBytes());
			// get the responses and handle them
			outer: while (true) {
				String response = in.next();
				String[] contents = in.contents(response.toUpperCase());
				for (int i = 0; i < contents.length; i++) {
					String responsePart = contents[i];
					switch (responsePart) {
						case "OK":
							rcvdOKs++;
							break;
						case "ERR":
							rcvdErs++;
							break;
						case "RES":
							// automatically advance loop counter to skip result for the loop
							calcRes = Integer.parseInt(contents[++i]);
							break;
						case "FIN":
							//message has been handled, return control flow to caller
							break outer;
					}
				}
			}
		} catch (IOException e) {
			System.err.println("Failed to calculate request with an exception");
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
