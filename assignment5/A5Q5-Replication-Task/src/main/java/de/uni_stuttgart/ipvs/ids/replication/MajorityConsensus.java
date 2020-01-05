package de.uni_stuttgart.ipvs.ids.replication;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.uni_stuttgart.ipvs.ids.communication.MessageWithSource;
import de.uni_stuttgart.ipvs.ids.communication.NonBlockingReceiver;
import de.uni_stuttgart.ipvs.ids.communication.ReadRequestMessage;
import de.uni_stuttgart.ipvs.ids.communication.ReleaseReadLock;
import de.uni_stuttgart.ipvs.ids.communication.ReleaseWriteLock;
import de.uni_stuttgart.ipvs.ids.communication.RequestReadVote;
import de.uni_stuttgart.ipvs.ids.communication.RequestWriteVote;
import de.uni_stuttgart.ipvs.ids.communication.ValueResponseMessage;
import de.uni_stuttgart.ipvs.ids.communication.Vote;
import de.uni_stuttgart.ipvs.ids.communication.Vote.State;
import de.uni_stuttgart.ipvs.ids.communication.WriteRequestMessage;

public class MajorityConsensus<T> {

	protected Collection<SocketAddress> replicas;

	protected DatagramSocket socket;
	protected NonBlockingReceiver nbio;

	final static int TIMEOUT = 1000;

	public MajorityConsensus(Collection<SocketAddress> replicas, int port)
			throws SocketException {
		this.replicas = replicas;
		SocketAddress address = new InetSocketAddress("127.0.0.1", port);
		this.socket = new DatagramSocket(address);
		this.nbio = new NonBlockingReceiver(socket);
	}

	/**
	 * Part c) Implement this method.
	 */
	protected Collection<MessageWithSource<Vote>> requestReadVote() throws QuorumNotReachedException {
		RequestReadVote message = new RequestReadVote();
		try {
			byte[] byteMsg = this.serializeMessage(message);
			for (SocketAddress replica: this.replicas) {
				this.sendMessage(replica, byteMsg);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		int required = (int) (this.replicas.size() / 2) + 1; // TODO check quorum condition...
		Vector<DatagramPacket> rawMessages = this.nbio.receiveMessages(5000, this.replicas.size()); // TODO adjust timeout
		try {
			Collection<MessageWithSource<Vote>> messages = NonBlockingReceiver.unpack(rawMessages);
			return this.checkQuorum(messages);
		} catch (IOException|ClassNotFoundException e) {
			e.printStackTrace();
		}
		throw new QuorumNotReachedException(required, new HashSet<SocketAddress>());
	}

	/**
	 * Part c) Implement this method.
	 */
	protected void releaseReadLock(Collection<SocketAddress> lockedReplicas) {
		ReleaseReadLock message = new ReleaseReadLock();
		try {
			byte[] byteMsg = this.serializeMessage(message);
			for (SocketAddress replica: lockedReplicas) {
				this.sendMessage(replica, byteMsg);
			}
			// get replies out of the buffer
			this.nbio.receiveMessages(5000, this.replicas.size());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Part d) Implement this method.
	 */
	protected Collection<MessageWithSource<Vote>> requestWriteVote() throws QuorumNotReachedException {
		// TODO: Implement me!
		return null;
	}

	/**
	 * Part d) Implement this method.
	 */
	protected void releaseWriteLock(Collection<SocketAddress> lockedReplicas) {
		// TODO: Implement me!
	}

	/**
	 * Part c) Implement this method.
	 */
	protected T readReplica(SocketAddress replica) {
		ReadRequestMessage readRequest = new ReadRequestMessage();

		try {
			this.sendMessage(replica, this.serializeMessage(readRequest));
			return this.receiveValue();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Part d) Implement this method.
	 */
	protected void writeReplicas(Collection<SocketAddress> lockedReplicas, VersionedValue<T> newValue) {
		// TODO: Implement me!
	}

	private byte[] serializeMessage(Object serializable) throws IOException {
		try (ByteArrayOutputStream bStream = new ByteArrayOutputStream()) {
			try (ObjectOutput oo = new ObjectOutputStream(bStream)) {
				oo.writeObject(serializable);
			}
			return bStream.toByteArray();
		}
	}

	private void sendMessage(SocketAddress replica, byte[] byteMsg) {
		try {
			this.socket.send(new DatagramPacket(byteMsg, byteMsg.length, replica));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private <V> V deserializeMessage(DatagramPacket packet) throws IOException {
		try (ByteArrayInputStream bStream = new ByteArrayInputStream(packet.getData())) {
			try (ObjectInputStream iStream = new ObjectInputStream(bStream)) {
				return (V) iStream.readObject();
			} catch (ClassNotFoundException e) {
				throw new IOException(e);
			}
		}
	}

	private T receiveValue() throws IOException {
		byte[] buffer = new byte[2048];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		socket.receive(packet);
		try {
			ValueResponseMessage<T> message = deserializeMessage(packet);
			return message.getValue();
		} catch (ClassCastException e) {
			// TODO handle NO votes correctly
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Part c) Implement this method (and checkQuorum(), see below) to read the
	 * replicated value using the majority consensus protocol.
	 */
	public VersionedValue<T> get() throws QuorumNotReachedException {
		Collection<SocketAddress> replicasToRelease = this.replicas;
		try {
			Collection<MessageWithSource<Vote>> lockedReplicas = this.requestReadVote();
			replicasToRelease = lockedReplicas.stream().map(msg -> msg.getSource()).collect(Collectors.toList());
			MessageWithSource<Vote> replicaToRead = lockedReplicas.stream()
				.reduce(null, (resultA, resultB) -> {
					if (resultA == null) {
						return resultB;
					}
					if (resultB == null) {
						return resultA;
					}
					if (resultA.getMessage().getVersion() >= resultB.getMessage().getVersion()) {
						return resultA;
					}
					return resultB;
				});
			return new VersionedValue<T>(replicaToRead.getMessage().getVersion(), this.readReplica(replicaToRead.getSource()));
		} finally {
			this.releaseReadLock(replicasToRelease);
		}
	}

	/**
	 * Part d) Implement this method to set the
	 * replicated value using the majority consensus protocol.
	 */
	public void set(T value) throws QuorumNotReachedException {
		// TODO: Implement me!
	}

	/**
	 * Part c) Implement this method to check whether a sufficient number of
	 * replies were received. If a sufficient number was received, this method
	 * should return the {@link MessageWithSource}s of the locked {@link Replica}s.
	 * Otherwise, a QuorumNotReachedException must be thrown.
	 * @throws QuorumNotReachedException
	 */
	protected Collection<MessageWithSource<Vote>> checkQuorum(
			Collection<MessageWithSource<Vote>> replies) throws QuorumNotReachedException {
		Collection<MessageWithSource<Vote>> validReplies = replies.stream().filter((msg) -> {
			return msg.getMessage().getState() == State.YES;
		}).collect(Collectors.toList());
		int required = (int) (this.replicas.size() / 2) + 1; // TODO check quorum condition...
		if (validReplies.size() < required) {
			Collection<SocketAddress> achieved = validReplies.stream().map(msg -> msg.getSource()).collect(Collectors.toList());
			throw new QuorumNotReachedException(required, achieved);
		}
		return validReplies;
	}

}
