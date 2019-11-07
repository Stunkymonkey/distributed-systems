package de.unistgt.ipvs.vs.ex1.server;

import de.unistgt.ipvs.vs.ex1.common.ICalculation;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Add fields and methods to this class as necessary to fulfill the assignment.
 */
public class CalculationSession implements Runnable {
	Socket cliSocket;

	enum Operation {
		Add, Subtract, Multiply
	}

	public CalculationSession(Socket socket) {
		this.cliSocket = socket;
	}

	@Override
	public void run() {
		// System.out.println("New session thread");
		try {
			ObjectOutputStream out = new ObjectOutputStream(this.cliSocket.getOutputStream());
			ObjectInputStream in = new ObjectInputStream(this.cliSocket.getInputStream());

			// Send RDY when ready
			oosOut.writeObject("<08:RDY>");

			ICalculation calc = new CalculationImpl();
			// TOOD
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}