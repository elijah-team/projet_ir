package linda.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import linda.Linda.eventMode;
import linda.Linda.eventTiming;
import linda.Tuple;

/**
 * Interface pour {@link LindaRMIImpl}
 *
 * @author Nathan Chavas
 * @author Mohamed Moudjeb
 */
public interface LindaRMI extends Remote {

    public void write(Tuple t) throws RemoteException;

    public Tuple take(Tuple template) throws RemoteException;

    public Tuple read(Tuple template) throws RemoteException;

    public Tuple tryTake(Tuple template) throws RemoteException;

    public Tuple tryRead(Tuple template) throws RemoteException;

    public Collection<Tuple> takeAll(Tuple template) throws RemoteException;

    public Collection<Tuple> readAll(Tuple template) throws RemoteException;

    public Tuple waitEvent(eventMode mode, eventTiming timing, Tuple template) throws RemoteException;

    public void save(String filepath) throws RemoteException;

    public void debug(String prefix) throws RemoteException;
}