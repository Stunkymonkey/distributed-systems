package de.uni_stuttgart.ipvs.ids.communication;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.Vector;

/**
 * Part b) Extend the method receiveMessages to return all DatagramPackets that
 * were received during the given timeout.
 *
 * Also implement unpack() to conveniently convert a Collection of
 * DatagramPackets containing ValueResponseMessages to a collection of
 * VersionedValueWithSource objects.
 *
 */
public class NonBlockingReceiver {

	protected DatagramSocket socket;

	public NonBlockingReceiver(DatagramSocket socket) {
		this.socket = socket;
	}

	public Vector<DatagramPacket> receiveMessages(int timeoutMillis, int expectedMessages)
			 {

		Vector<DatagramPacket> ret = new Vector<>();

		//Get the start time
		//The remaining time for the current timeout will be calculated based on this
		long start = System.currentTimeMillis();
		long end = start + timeoutMillis;

		//Repeat while timeout is not reached
		for (long currentTimeout = timeoutMillis; currentTimeout > 0 && ret.size() < expectedMessages; currentTimeout = end - System.currentTimeMillis())
		{
			try
			{
				byte[] buffer = new byte[2048];
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				socket.setSoTimeout((int) currentTimeout);
				socket.receive(packet);

				ret.add(packet);
			}
			catch (SocketTimeoutException e)
			{
				//Timeout reached
				break;
			}
			catch (IOException e)
			{
				// something went wrong with receiving a packet
				e.printStackTrace();
				continue;
			}
		}
		//Set socket back to none timeout mode
		try {
			socket.setSoTimeout(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static <T> Collection<MessageWithSource<T>> unpack(
			Collection<DatagramPacket> packetCollection) throws IOException,
			ClassNotFoundException {
		Vector<MessageWithSource<T>> ret = new Vector<>();

		for(DatagramPacket packet : packetCollection)
		{
			ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(packet.getData()));
			MessageWithSource<T> mws = new MessageWithSource<T>(packet.getSocketAddress(), (T) iStream.readObject());
			iStream.close();
			ret.add(mws);
		}

		return ret;
	}

}
