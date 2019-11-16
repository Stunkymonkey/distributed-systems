package de.unistgt.ipvs.vs.ex2.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import de.unistgt.ipvs.vs.ex2.common.ICalculation;
import de.unistgt.ipvs.vs.ex2.common.ICalculationFactory;

/**
 * Change this class (implementation/signature/...) as necessary to complete the
 * assignment. You may also add some fields or methods.
 */

public class CalculationImplFactory extends UnicastRemoteObject implements ICalculationFactory {
    
    private static final long serialVersionUID = 8409100566761383094L;
    
    /**
     * 0-args constructor for inheritance purposes. Auto-generated 0-args ctor would not handle RemoteException
     * @throws RemoteException
     */
    protected CalculationImplFactory() throws RemoteException {
        super();
    }
    
    @Override
    public ICalculation getSession() throws RemoteException {
        // we can return a CalculationImpl directly, because it inherits from Remote.
        // it is therefore marshalled across RMI boundaries by reference.
        return new CalculationImpl();
    }
}
