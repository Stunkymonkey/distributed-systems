package de.uni_stuttgart.ipvs.ids.replication;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Random;

import de.uni_stuttgart.ipvs.ids.communication.ReadRequestMessage;
import de.uni_stuttgart.ipvs.ids.communication.ReleaseReadLock;
import de.uni_stuttgart.ipvs.ids.communication.ReleaseWriteLock;
import de.uni_stuttgart.ipvs.ids.communication.RequestReadVote;
import de.uni_stuttgart.ipvs.ids.communication.RequestWriteVote;
import de.uni_stuttgart.ipvs.ids.communication.ValueResponseMessage;
import de.uni_stuttgart.ipvs.ids.communication.Vote;
import de.uni_stuttgart.ipvs.ids.communication.WriteRequestMessage;

public class Replica<T> extends Thread {

	public enum LockType {
		UNLOCKED, READLOCK, WRITELOCK
	};

	private int id;

	private double availability;
	private VersionedValue<T> value;

	protected DatagramSocket socket = null;
	
	protected LockType lock;
	
	/**
	 * This address holds the addres of the client holding the lock. This
	 * variable should be set to NULL every time the lock is set to UNLOCKED.
	 */
	protected SocketAddress lockHolder;

	public Replica(int id, int listenPort, double availability, T initialValue) throws SocketException {
		super("Replica:" + listenPort);
		this.id = id;
		SocketAddress socketAddress = new InetSocketAddress("127.0.0.1", listenPort);
		this.socket = new DatagramSocket(socketAddress);
		this.availability = availability;
		this.value = new VersionedValue<T>(0, initialValue);
		this.lock = LockType.UNLOCKED;
	}
	

	/**
	 * Part a) Implement this run method to receive and process request
	 * messages. To simulate a replica that is sometimes unavailable, it should
	 * randomly discard requests as long as it is not locked.
	 * The probability for discarding a request is (1 - availability).
	 * 
	 * For each request received, it must also be checked whether the request is valid.
	 * For example:
	 * - Does the requesting client hold the correct lock?
	 * - Is the replica unlocked when a new lock is requested?
	 */
	public void run() {
		// TODO: Implement me!

		byte[] buffer = new byte[2048];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

		//Used for the failure simulation
		Random random = new Random();

		while(true)
		{
			try
			{
				//recive und unpack object
				socket.receive(packet);
				ObjectInputStream objStream = new ObjectInputStream(new ByteArrayInputStream(packet.getData()));
				Object msg = objStream.readObject();
				objStream.close();

				//Simulating crashed Nodes
				//Only happend if node is unlocked
				if(lock == LockType.UNLOCKED && random.nextDouble()>availability)
					continue;

				//Take action based on object type
				if(msg instanceof RequestReadVote)
				{
					if(lock != LockType.WRITELOCK)
					{
						lock = LockType.READLOCK;
						lockHolder = packet.getSocketAddress();

						sendVote(packet.getSocketAddress(), Vote.State.YES, value.version);
						continue;
					}
					else
					{
						sendVote(packet.getSocketAddress(), Vote.State.NO, value.version);
						continue;
					}
				}

				if(msg instanceof ReleaseReadLock)
				{
					if(lock == LockType.READLOCK && lockHolder.equals(packet.getSocketAddress()))
					{
						lock = LockType.UNLOCKED;
						lockHolder = null;
						sendVote(packet.getSocketAddress(), Vote.State.YES, value.version);
						continue;
					}
					else
					{
						sendVote(packet.getSocketAddress(), Vote.State.NO, value.version);
						continue;
					}
				}

				if(msg instanceof ReadRequestMessage)
				{
					if(lock == LockType.READLOCK && lockHolder.equals(packet.getSocketAddress()))
					{
						ValueResponseMessage responseMessage = new ValueResponseMessage(value.value);

						ByteArrayOutputStream bStream = new ByteArrayOutputStream();
						ObjectOutput oo = new ObjectOutputStream(bStream);
						oo.writeObject(responseMessage);
						oo.close();

						byte[] byteMsg = bStream.toByteArray();

						DatagramSocket socket = new DatagramSocket();
						socket.send(new DatagramPacket(byteMsg, byteMsg.length, packet.getSocketAddress()));
						continue;
					}
					else
					{
						sendVote(packet.getSocketAddress(), Vote.State.NO, value.version);
						continue;
					}
				}

				if(msg instanceof RequestWriteVote)
				{
					if(lock == LockType.UNLOCKED)
					{
						lock = LockType.WRITELOCK;
						lockHolder = packet.getSocketAddress();

						sendVote(packet.getSocketAddress(), Vote.State.YES, value.version);
						continue;
					}
					else
					{
						sendVote(packet.getSocketAddress(), Vote.State.NO, value.version);
						continue;
					}
				}

				if(msg instanceof RequestWriteVote)
				{
					if(lock == LockType.WRITELOCK && lockHolder.equals(packet.getSocketAddress()))
					{
						lock = LockType.UNLOCKED;
						lockHolder = null;

						sendVote(packet.getSocketAddress(), Vote.State.YES, value.version);
						continue;
					}
					else
					{
						sendVote(packet.getSocketAddress(), Vote.State.NO, value.version);
						continue;
					}
				}

				if(msg instanceof WriteRequestMessage)
				{

					if(lock == LockType.WRITELOCK && lockHolder.equals(packet.getSocketAddress()))
					{
						VersionedValue<T> newValue = (VersionedValue) msg;

						//Converting WriteRequestMessage to VersionedValue
						value = new VersionedValue<>(newValue);

						sendVote(packet.getSocketAddress(), Vote.State.YES, value.version);
						continue;
					}
					else
					{
						sendVote(packet.getSocketAddress(), Vote.State.NO, value.version);
						continue;
					}
				}
			}
			catch (IOException | ClassNotFoundException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * This is a helper method. You can implement it if you want to use it or just ignore it.
	 * Its purpose is to send a Vote (YES/NO depending on the state) to the given address.
	 */
	protected void sendVote(SocketAddress address,
			Vote.State state, int version) throws IOException {

		Vote vote = new Vote(state, version);

		ByteArrayOutputStream bStream = new ByteArrayOutputStream();
		ObjectOutput oo = new ObjectOutputStream(bStream);
		oo.writeObject(vote);
		oo.close();

		byte[] msg = bStream.toByteArray();

		DatagramSocket socket = new DatagramSocket();
		DatagramPacket packet = new DatagramPacket(msg, msg.length, address);
		socket.send(packet);
	}

	/**
	 * This is a helper method. You can implement it if you want to use it or just ignore it.
	 * Its purpose is to extract the object stored in a DatagramPacket.
	 */
	protected Object getObjectFromMessage(DatagramPacket packet)
			throws IOException, ClassNotFoundException {
		// TODO: Implement me!
		return null; // Pacify the compiler
	}

	public int getID() {
		return id;
	}

	public SocketAddress getSocketAddress() {
		return socket.getLocalSocketAddress();
	}

}
