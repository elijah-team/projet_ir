package linda.server;

import linda.Tuple;

import java.rmi.RemoteException;

public interface CallbackRMI extends java.rmi.Remote {

    public void call(Tuple t) throws RemoteException;

}