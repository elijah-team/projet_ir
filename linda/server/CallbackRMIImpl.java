package linda.server;

import linda.Callback;
import linda.Tuple;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class CallbackRMIImpl extends UnicastRemoteObject implements CallbackRMI {

    private final Callback callback;

    public CallbackRMIImpl(Callback callback) throws RemoteException {
        this.callback = callback;
    }

    @Override
    public void call(Tuple t) throws RemoteException {
        this.callback.call(t);
    }

}