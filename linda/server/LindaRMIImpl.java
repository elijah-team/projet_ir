package linda.server;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;
import java.util.List;
import linda.Backup;
import linda.Linda;
import linda.Linda.eventMode;
import linda.Linda.eventTiming;
import linda.Tuple;
import linda.shm.CentralizedLinda;
import linda.shm.TupleCallback;

/**
 * Serveur linda. Gère un {@link CentralizedLinda} et les requêtes des clients.
 *
 * @author Nathan Chavas
 * @author Mohamed Moudjeb
 */
public class LindaRMIImpl extends UnicastRemoteObject implements LindaRMI, Backup {

    /**
     * Le {@link CentralizedLinda} associé au serveur.
     */
    public Linda linda;

    /**
     * Initialize le linda server avec un {@link CentralizedLinda}.
     *
     * @throws RemoteException
     */
    public LindaRMIImpl() throws RemoteException {
        this.linda = new CentralizedLinda();
    }

    /**
     * Effectue un write sur le {@link CentralizedLinda}.
     *
     * @param t le tuple à écrire
     * @throws RemoteException
     * @see CentralizedLinda
     */
    @Override
    public void write(Tuple t) throws RemoteException {
        this.linda.write(t);
    }

    /**
     * Effectue un take sur le {@link CentralizedLinda}. Bloquant si aucun tuple
     * ne correspond.
     *
     * @param template le template du tuple à prendre.
     * @return le tuple pris.
     * @throws RemoteException
     * @see CentralizedLinda
     */
    @Override
    public Tuple take(Tuple template) throws RemoteException {
        return this.linda.take(template);
    }

    /**
     * Effectue un read sur le {@link CentralizedLinda}. Bloquant si aucun tuple
     * ne correspond.
     *
     * @param template le template du tuple à lire.
     * @return le tuple lut.
     * @throws RemoteException
     * @see CentralizedLinda
     */
    @Override
    public Tuple read(Tuple template) throws RemoteException {
        return this.linda.read(template);
    }

    /**
     * Effectue le tryTake sur le {@link CentralizedLinda}.
     *
     * @param template le template du tuple à prendre.
     * @return le tuple pris ou null si aucun tuple ne correspond.
     * @throws RemoteException
     * @see CentralizedLinda
     */
    @Override
    public Tuple tryTake(Tuple template) throws RemoteException {
        return this.linda.tryTake(template);
    }

    /**
     * Effectue le tryRead sur le {@link CentralizedLinda}.
     *
     * @param template le template du tuple à lire.
     * @return le tuple lut ou null si aucun tuple ne correspond.
     * @throws RemoteException
     * @see CentralizedLinda
     */
    @Override
    public Tuple tryRead(Tuple template) throws RemoteException {
        return this.linda.tryRead(template);
    }

    /**
     * Effectue un takeAll sur le {@link CentralizedLinda}.
     *
     * @param template le template des tuples à prendre.
     * @return une collection de tuple correspondant au template, collection
     * vide si aucun.
     * @throws RemoteException
     * @see CentralizedLinda
     */
    @Override
    public Collection<Tuple> takeAll(Tuple template) throws RemoteException {
        return this.linda.takeAll(template);
    }

    /**
     * Effectue un readAll sur le {@link CentralizedLinda}.
     *
     * @param template le template des tuples à lire.
     * @return une collection de tuple correspondant au template, collection
     * vide si aucun
     * @throws RemoteException
     * @see CentralizedLinda
     */
    @Override
    public Collection<Tuple> readAll(Tuple template) throws RemoteException {
        return this.linda.readAll(template);
    }

    /**
     * Retourne le tuple d'un évènement. Pas de notion de callback ici.
     * Attention, bloquant tant que l'évènement n'a pas eu lieu.
     *
     * @param mode le mode de l'évènement (take ou read).
     * @param timing le timind de l'évènement (immédiat ou futur).
     * @param template le template du tuple associé à l'évènement.
     * @return le tuple trouvé lors de l'évènement.
     * @throws RemoteException
     */
    @Override
    public Tuple waitEvent(eventMode mode, eventTiming timing, Tuple template) throws RemoteException {
        TupleCallback cb = new TupleCallback();
        System.out.println(template);
        this.linda.eventRegister(Linda.eventMode.READ, Linda.eventTiming.IMMEDIATE, template, cb);
        cb.waitCallback();
        System.out.println("Je récupère le tuple d'évenement : " + cb.getTuple());
        return cb.getTuple();
    }

    public void save(String fileName) {
        ((CentralizedLinda)this.linda).save(fileName);
    }

    public void load(String fileName) {
        ((CentralizedLinda)this.linda).load(fileName);
    }

    /**
     * Appel le debug sur le {@link CentralizedLinda}.
     * @param prefix le préfix du débug.
     * @see CentralizedLinda
     */
    @Override
    public void debug(String prefix) {
        this.linda.debug(prefix);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws RemoteException {
        Registry dns = null;
        try {
             dns = LocateRegistry.createRegistry(4000);
        } catch (RemoteException ex) {
            System.err.println("Serveur déjà en écoute sur ce port...");
        }
        LindaRMI server = new LindaRMIImpl();
        ((LindaRMIImpl) server).load("save_backup");
        dns.rebind("linda", server);
        System.out.println("Le serveur est up");
    }
}