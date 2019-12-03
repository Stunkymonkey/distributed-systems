package de.unistgt.ipvs.vs.ex4.distributed_debugging_algorithm;

import java.util.Arrays;

//you are not allowed to change this class structure
public class VectorClock {

	protected int[] vectorClock;
	private int processId;

	public VectorClock(int processId, int numberOfProcesses) {
		vectorClock = new int[numberOfProcesses];
		this.processId = processId;
	}

	VectorClock(VectorClock other) {
		vectorClock = other.vectorClock.clone();
		processId = other.processId;
	}

	public void increment() {
		/*
		 * Complete a code to increment the local clock component
		 */
		vectorClock[processId] += 1;
	}

	public int[] get() {
		// Complete a code to return the vectorClock value
		return vectorClock;
	}

	public void update(VectorClock other) {
		/*
		 * Implement Supremum operation
		 */
		increment();
		for (int i = 0; i < vectorClock.length; i++) {
			vectorClock[i] = Math.max(vectorClock[i], other.vectorClock[i]);
		}
	}

	public boolean checkConsistency(int otherProcessId, VectorClock other) {
		/*
		 * Implement a code to check if a state is consist regarding two vector clocks (i.e. this and other).
		 * See slide 41 from global state lecture.
		 */
		// check that this clock does not count an event that the other clock hasn't seen yet in it's local process
		return vectorClock[otherProcessId] <= other.vectorClock[otherProcessId]
		// and check that the other clock has not seen an event for this process that hasn't happened yet
			&& vectorClock[processId] >= other.vectorClock[processId];
	}

	@Override
	public String toString() {
		// debugger display
		String[] parts = Arrays.stream(vectorClock).mapToObj(String::valueOf).toArray(String[]::new);
		parts[processId] = "(" + parts[processId] + ")";
		return "[" + String.join(", ", parts) + "]";
	}
}
