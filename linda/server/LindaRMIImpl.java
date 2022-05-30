package linda.server;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collection;

import linda.Callback;
import linda.Linda;
import linda.Linda.eventMode;
import linda.Linda.eventTiming;
import linda.Tuple;
import linda.shm.CentralizedLinda;

/**
 * Serveur linda. Gère un {@link CentralizedLinda} et les requêtes des clients.
 *
 * @author Nathan Chavas
 * @author Mohamed Moudjeb
 */
public class LindaRMIImpl extends UnicastRemoteObject implements LindaRMI {

    /**
     * Le {@link CentralizedLinda} associé au serveur.
     */
    public Linda linda;

    /**
     * Hôte de l'URI RMI sur laquelle le serveur Linda est déclaré
     */
    private String host;

    /**
     * Port de l'URI RMI sur laquelle le serveur Linda est déclaré
     */
    private int port;

    /**
     * Serveur Linda secondaire ("de backup") pour le serveur principal
     * Ou serveur principal quand le serveur secondaire devient principal
     */
    private LindaRMI backup;

    /**
     * Vrai si le serveur courant est un serveur secondaire ("de backup")
     * Faux sinon, c'est un serveur principal
     */
    private boolean isBackup;

    /**
     * Initialize le linda server avec un {@link CentralizedLinda}.
     *
     * @throws RemoteException
     */
    public LindaRMIImpl() throws RemoteException {
        this.linda = new CentralizedLinda();
    }

    @Override
    public void declareServer(String host, int port) throws RemoteException {
        // "Sauvegarde" l'adresse et le port de l'URI
        this.host = host;
        this.port = port;
        try {
            // Enregistre le serveur courant en tant que serveur principal
            Naming.bind("rmi://"+this.host+":"+this.port+"/LindaRMI", this);
            this.isBackup = false;
        } catch (AlreadyBoundException abe) { // Si un serveur est déjà déclaré
            try {
                // Enregistre le serveur courant en tant que serveur secondaire
                this.backup = (LindaRMI) Naming.lookup("rmi://"+this.host+":"+this.port+"/LindaRMI");
                this.isBackup = true;
                // S'enregistre comme backup auprès du serveur principal
                this.backup.registerBackup(this);
                // Lance un thread qui s'occupera de vérifier l'état du serveur principal
                new Thread(this::pollPrimary).start();
            } catch (MalformedURLException | NotBoundException e) {
                System.err.println(e);
            }
        } catch (MalformedURLException e) {
            System.err.println(e);
        }
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
     *         vide si aucun.
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
     *         vide si aucun
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
     * @param mode     le mode de l'évènement (take ou read).
     * @param timing   le timind de l'évènement (immédiat ou futur).
     * @param template le template du tuple associé à l'évènement.
     */
    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, CallbackRMI callback)
            throws RemoteException {
        Callback localcallback = t -> {
            try {
                callback.call(t);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        };
        this.linda.eventRegister(mode, timing, template, localcallback);
        if (!this.isBackup && this.backup != null) {
            try {
                this.backup.eventRegister(mode, timing, template, callback);
            } catch (RemoteException e) {
                this.unregisterBackup();
            }
        }
    }

    public void save() {
        ((CentralizedLinda) this.linda).save();
    }

    public void load(String fileName) {
        if (fileName.toUpperCase() != "DEFAULT")
            ((CentralizedLinda) this.linda).setBackupPath(fileName);
        ((CentralizedLinda) this.linda).load();
    }

    @Override
    public void registerBackup(LindaRMIImpl backupServer) {
        try {
            // Enregistre backupServer comme serveur de backup
            this.backup = backupServer;
            // Lui envoie tous les tuples courants
            for (Tuple tuple : ((CentralizedLinda) this.linda).getAllTuples()) {
                this.backup.write(tuple);
            }
        } catch (RemoteException e) {
            System.err.println(e);
        }
    }

    /**
     * Supprime le serveur de backup
     * Appelé par les méthodes de la classe quand celui-ci ne répond pas
     */
    private void unregisterBackup() {
        this.backup = null;
    }

    /**
     * Permet au serveur courant de devenir le serveur principal
     * Appelé par pollPrimary() quand le serveur de backup détecte que le serveur principal ne répond plus
     */
    private void becomePrimary() {
        try {
            // Recrée un registre au besoin
            try {
                LocateRegistry.createRegistry(4000);
            } catch (java.rmi.server.ExportException e) {
                System.out.println("A registry is already running, proceeding...");
            }
            // S'enregistre en tant que serveur principal, supprime le serveur de backup
            Naming.rebind("rmi://"+this.host+":"+this.port+"/LindaRMI", this);
            this.unregisterBackup();
            this.isBackup = false;
            this.load("DEFAULT");
        } catch (RemoteException | MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void pollPrimary() {
        // Appelle keepAlive() puis attend 2s, et recommence
        while (this.isBackup) {
            try {
                System.out.println("Ping...");
                this.backup.keepAlive();
                System.out.println("...Pong!");
                Thread.sleep(2000);
            } catch (RemoteException re) {
                this.becomePrimary(); // Si une erreur réseau survient, devient le serveur principal
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }
    }

    @Override
    public void keepAlive() {

    }

    /**
     * Appel le debug sur le {@link CentralizedLinda}.
     * 
     * @param prefix le préfix du débug.
     * @see CentralizedLinda
     */
    @Override
    public void debug(String prefix) {
        this.linda.debug(prefix);
    }
}