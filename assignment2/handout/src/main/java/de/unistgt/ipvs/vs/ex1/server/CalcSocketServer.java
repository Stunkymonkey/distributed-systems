package de.unistgt.ipvs.vs.ex1.server;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Extend the run-method of this class as necessary to complete the assignment.
 * You may also add some fields, methods, or further classes.
 */
public class CalcSocketServer extends Thread {
	private ServerSocket srvSocket;
	private int port;

	public CalcSocketServer(int port) {
		super("Calculation Server Accept Thread");
		this.srvSocket = null;
		this.port = port;
	}
	
	@Override
	public void interrupt() {
		try {
			if (srvSocket != null) srvSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		if (port <= 0) {
			System.err.println("Wrong number of arguments.\nUsage: SocketServer <listenPort>\n");
			System.exit(-1);
		}

		// Start listening server socket ..
		try {
			srvSocket = new ServerSocket(port);
			while(true) {
				// spawn a new session for each client that connects
				new Thread(new CalculationSession(srvSocket.accept())).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void waitUntilRunning(){
		while(this.srvSocket == null){
			try {
				Thread.sleep(1);
			} catch (InterruptedException ex) {
			}
		}
	}
}
