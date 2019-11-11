package de.unistgt.ipvs.vs.ex1.server;

import de.unistgt.ipvs.vs.ex1.common.ICalculation;
import de.unistgt.ipvs.vs.ex1.common.MessageReader;
import de.unistgt.ipvs.vs.ex1.common.MessageWriter;

import java.io.IOException;
import java.net.Socket;
import java.rmi.RemoteException;

/**
 * Add fields and methods to this class as necessary to fulfill the assignment.
 */
public class CalculationSession implements Runnable {
	Socket cliSocket;
	private Operation currentOp;
	enum Operation {
		Add, Subtract, Multiply
	}

	public CalculationSession(Socket socket) {
		this.cliSocket = socket;
	}

	@Override
	public void run() {
		try (final MessageReader in = new MessageReader(this.cliSocket.getInputStream());
			final MessageWriter out = new MessageWriter(this.cliSocket.getOutputStream())) {
			// Send RDY when ready
			out.forceSend("RDY");
			final ICalculation calc = new CalculationImpl();
			while (in.hasNext()) {
				// in.next is a blocking call waiting for a complete message if necessary
				String message = in.next();
				if (message.isEmpty()) {
					// empty message can only happen if the input is exhausted
					// therefore skip the whole message handling
					break;
				}
				// acknowledge the message
				out.forceSend("OK");
				for (String part : in.contents(message.toUpperCase())) {
					processPart(part, calc, out);
				}
				// notify the client of the finished processing for the current message
				out.forceSend("FIN");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void processPart(final String part, final ICalculation calc, final MessageWriter out) throws IOException {
		switch (part) {
			case "ADD":
				currentOp = Operation.Add;
				out.enqueue("OK " + part);
				break;
			case "SUB":
				currentOp = Operation.Subtract;
				out.enqueue("OK " + part);
				break;
			case "MUL":
				currentOp = Operation.Multiply;
				out.enqueue("OK " + part);
				break;
			case "RES":
				out.enqueue("OK RES " + calc.getResult());
				break;
			case "OK":
			case "ERR":
			case "FIN":
				// contents are valid message parts, but do not require action
				out.enqueue("OK " + part);
				break;
			default:
				// try to parse an integer or fail with an error otherwise
				try {
					int value = Integer.parseInt(part);
					out.enqueue("OK " + part);
					if (currentOp != null) {
						doCalculation(calc, value);
					}
				} catch (NumberFormatException failure) {
					// that's an error, FAM
					out.enqueue("ERR " + part);
				}
		}
	}

	// throws RemoteException because the interface is reused for the RMI exercise!
	private void doCalculation(ICalculation calc, int value) throws RemoteException{
		switch (currentOp) {
			case Add:
				calc.add(value);
				break;
			case Subtract:
				calc.subtract(value);
				break;
			case Multiply:
				calc.multiply(value);
				break;
		}
	}
}
