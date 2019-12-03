package de.unistgt.ipvs.vs.ex4.distributed_debugging_algorithm;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

//you are not allowed to change this class structure. However, you can add local functions!
public class Monitor implements Runnable {

    /**
     * The state consists on vector timestamp and local variables of each
     * process. In this class, a state is represented by messages (events)
     * indices of each process. The message contains a local variable and vector
     * timestamp, see Message class. E.g. if state.processesMessagesCurrentIndex
     * contains {1, 2}, it means that the state contains the second message
     * (event) from process1 and the third message (event) from process2
     */
    private class State {
        // Message indices of each process
        private int[] processesMessagesCurrentIndex;

        public State(int numberOfProcesses) {
            processesMessagesCurrentIndex = new int[numberOfProcesses];
        }

        public State(int[] processesMessagesCurrentIndex) {
            this.processesMessagesCurrentIndex = processesMessagesCurrentIndex;
        }

        {
            processesMessagesCurrentIndex = new int[numberOfProcesses];
        }

        public int[] getProcessesMessagesCurrentIndex() {
            return processesMessagesCurrentIndex;
        }

        public int getProcessMessageCurrentIndex(int processId) {
            return this.processesMessagesCurrentIndex[processId];
        }

        @Override
        public boolean equals(Object other) {
            State otherState = (State) other;

            // Iterate over processesMessagesCurrentIndex array
            for (int i = 0; i < numberOfProcesses; i++)
                if (this.processesMessagesCurrentIndex[i] != otherState.processesMessagesCurrentIndex[i])
                    return false;

            return true;
        }

        @Override
        public String toString() {
            // debugger display
            return "[" + String.join(", ", Arrays.stream(processesMessagesCurrentIndex).mapToObj(String::valueOf).toArray(String[]::new)) + "]";
        }
    }

    private int numberOfProcesses;
    private final int numberOfPredicates = 4;

    // Count of still running processes. The monitor starts to check predicates
    // (build lattice) whenever runningProcesses equals zero.
    private AtomicInteger runningProcesses;
    /*
     * Q1, Q2, ..., Qn It represents the processes' queue. See distributed
     * debugging algorithm from global state lecture!
     */
    private List<List<Message>> processesMessages;

    // list of states
    private LinkedList<State> states;

    // The predicates checking results
    private boolean[] possiblyTruePredicatesIndex;
    private boolean[] definitelyTruePredicatesIndex;

    public Monitor(int numberOfProcesses) {
        this.numberOfProcesses = numberOfProcesses;

        runningProcesses = new AtomicInteger();
        runningProcesses.set(numberOfProcesses);

        processesMessages = new ArrayList<>(numberOfProcesses);
        for (int i = 0; i < numberOfProcesses; i++) {
            List<Message> tempList = new ArrayList<>();
            processesMessages.add(i, tempList);
        }

        states = new LinkedList<>();

        possiblyTruePredicatesIndex = new boolean[numberOfPredicates];// there
        // are
        // three
        // predicates
        for (int i = 0; i < numberOfPredicates; i++)
            possiblyTruePredicatesIndex[i] = false;

        definitelyTruePredicatesIndex = new boolean[numberOfPredicates];
        for (int i = 0; i < numberOfPredicates; i++)
            definitelyTruePredicatesIndex[i] = false;
    }

    /**
     * receive messages (events) from processes
     *
     * @param processId
     * @param message
     */
    public void receiveMessage(int processId, Message message) {
        synchronized (processesMessages) {
            processesMessages.get(processId).add(message);
        }
    }

    /**
     * Whenever a process terminates, it notifies the Monitor. Monitor only
     * starts to build lattice and check predicates when all processes terminate
     *
     * @param processId
     */
    public void processTerminated(int processId) {
        runningProcesses.decrementAndGet();
    }

    public boolean[] getPossiblyTruePredicatesIndex() {
        return possiblyTruePredicatesIndex;
    }

    public boolean[] getDefinitelyTruePredicatesIndex() {
        return definitelyTruePredicatesIndex;
    }

    @Override
    public void run() {
        // wait till all processes terminate
        while (runningProcesses.get() != 0)
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        // create initial state (S00)
        State initialState = new State(numberOfProcesses);

        // check predicates for part (b)
        for (int predicateNo = 0; predicateNo < 3; predicateNo++) {
            System.out.printf("Predicate%d-----------------------------------\n", predicateNo);
            states.add(initialState); // add the initial state to states list
            buildLattice(predicateNo, 0, 1);
            states.clear();

        }

        if (numberOfProcesses > 2) {
            int predicateNo = 3;
            System.out.printf("Predicate%d-----------------------------------\n", predicateNo);
            states.add(initialState); // add the initial state to states list
            buildLattice(predicateNo, 0, 2);
            states.clear();
        }

    }

    public void buildLattice(int predicateNo, int process_i_id, int process_j_id) {
        // TODO check if works
        /*
         * - implement this function to build the lattice of consistent states.
         * - The goal of building the lattice is to check a predicate if it is
         * possibly or/and definitely True. Thus your function should stop
         * whenever the predicate evaluates to both possibly and definitely
         * True. NOTE1: this function should call findReachableStates and
         * checkPredicate functions. NOTE2: predicateNo, process_i_id and
         * process_j_id are described in checkPredicate function.
         */
        State root = states.getFirst();
        Queue<State> queue = new LinkedList<State>();
        queue.add(root);
        while (!queue.isEmpty()) {
            // current state
            State current = queue.remove();
            // get reachable states from current state
            LinkedList<State> reachable = findReachableStates(current);
            second:
            for (State r : reachable) {
                // check if state is already contained
                for (State s : states) {
                    if (Arrays.equals(r.getProcessesMessagesCurrentIndex(), s.getProcessesMessagesCurrentIndex())) {
                        // skip if already contained
                        continue second;
                    }
                }
                // else add state
                states.add(r);
                queue.add(r);
            }
        }

        System.out.print("Lattice : ");
        for (State s : states) {
			System.out.print("(" + s.getProcessMessageCurrentIndex(0) + "," + s.getProcessMessageCurrentIndex(1));
        	if (numberOfProcesses > 2) {
				System.out.print("," + s.getProcessMessageCurrentIndex(2));
			}
        	System.out.println(")");
        }

        // check predicates
        definitelyTruePredicatesIndex[predicateNo] = checkPredicate(predicateNo, process_i_id, process_j_id);
		System.out.println("Possibly True : " + possiblyTruePredicatesIndex[predicateNo]);
		System.out.println("Definitely True : " + definitelyTruePredicatesIndex[predicateNo]);
    }

    /**
     * find all reachable states starting from a given state
     *
     * @param state
     * @return list of all reachable states
     */
    private LinkedList<State> findReachableStates(State state) {
        /*
         * Given a state, implement a code that find all reachable states. The
         * function should return a list of all reachable states
         */
        LinkedList<State> reachable = new LinkedList<State>();
        // current vector clocks
        ArrayList<VectorClock> vClocks = new ArrayList<VectorClock>();
        for (int i = 0; i < numberOfProcesses; i++) {
            VectorClock curr_vc = processesMessages.get(i).get(state.getProcessMessageCurrentIndex(i)).getVectorClock();
            vClocks.add(i, curr_vc);
        }
        // for each process
        outer:
        for (int i = 0; i < numberOfProcesses; i++) {
            int next = state.getProcessMessageCurrentIndex(i) + 1;
            // check if process has no more lines
            if (processesMessages.get(i).size() <= next) {
                continue;
            }
            VectorClock next_vc = processesMessages.get(i).get(next).getVectorClock();
            for (int j = 0; j < numberOfProcesses; j++) {
                if (i == j) {
                    continue;
                } else if (!next_vc.checkConsistency(j, vClocks.get(j))) {
                    // skip if not consistent
                    continue outer;
                }
            }
            // adds reachable state
            int[] new_indexes = state.getProcessesMessagesCurrentIndex().clone();
            new_indexes[i] += 1;
            reachable.add(new State(new_indexes));
        }
        return reachable;
    }

    /**
     * - check a predicate and return true if the predicate is **definitely**
     * True. - To simplify the code, we check the predicates only on local
     * variables of two processes. Therefore, process_i_Id and process_j_id
     * refer to the processes that have the local variables in the predicate.
     * The predicate0, predicate1 and predicate2 contain the local variables
     * from process1 and process2. whilst the predicate3 contains the local
     * variables from process1 and process3.
     *
     * @param predicateNo: which predicate to validate
     * @return true if predicate is definitely true else return false
     */
    private boolean checkPredicate(int predicateNo, int process_i_id, int process_j_id) {
        /*
         * - check if a predicate is possibly and/or definitely true. - iterate
         * over all reachable states to check the predicates. NOTE: you can use
         * the following code switch (predicateNo) { case 0: predicate =
         * Predicate.predicate0(process_i_Message, process_j_Message); break;
         * case 1: ... }
         */
		// Check if possibly true
		for (State s : states) {
			// get messages
			int index_i = s.getProcessMessageCurrentIndex(process_i_id);
			int index_j = s.getProcessMessageCurrentIndex(process_j_id);
			Message msg_i = processesMessages.get(process_i_id).get(index_i);
			Message msg_j = processesMessages.get(process_j_id).get(index_j);
			// selects predicate
			if (getPredicateNo(predicateNo, msg_i, msg_j)) {
				possiblyTruePredicatesIndex[predicateNo] = true;
				break;
			}
		}

		// TODO
		// Check if definitely true
		boolean predicate = false;
		// states in one level
		LinkedList<State> states_l = new LinkedList<State>();
		states_l.add(states.getFirst());

		/*while (!states_l.isEmpty()) {
			getPredicateNo(predicateNo, msg_i, msg_j);
		}*/

        return predicate;
    }

    private boolean getPredicateNo(int predicateNo, Message msg_i, Message msg_j) {
		switch (predicateNo) {
			case 0:
				return Predicate.predicate0(msg_i, msg_j);
			case 1:
				return Predicate.predicate1(msg_i, msg_j);
			case 2:
				return Predicate.predicate2(msg_i, msg_j);
			case 3:
				return Predicate.predicate3(msg_i, msg_j);
			default:
				System.out.println("predicate not implemented!");
		}
		return false;
	}
}
