package linda.server;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Collection;
import linda.Callback;
import linda.Linda;
import linda.Tuple;

/**
 * Client part of a client/server implementation of Linda. It implements the
 * Linda interface and propagates everything to the server it is connected to.
 *
 * @author Nathan Chavas
 * @author Mohamed Moudjeb
 */
public class LindaClient implements Linda {

    /**
     * Le temps fixe d'attente avant de tenter de se reconnecter à un serveur
     * lorsque le serveur auquel on est connecté ne répond plus
     */
    private static final int BACKUP_WAIT = 1000;

    /**
     * URI du serveur auquel on est connecté
     * Exemple : rmi://localhost:4000/LindaServer
     */
    private String serverURI;

    /**
     * Le {@link LindaRMI}.
     */
    private LindaRMI linda;

    /**
     * Initializes the Linda implementation.
     *
     * @param serverURI the URI of the server, e.g.
     * "//localhost:4000/LindaServer".
     */
    public LindaClient(String serverURI) {
        System.out.println("Client called with URI: " + serverURI);
        this.serverURI = serverURI; // "sauvegarder" l'URI du serveur
        this.joinServer();
    }

    /**
     * Se connecter à un LindaServer à l'aide d'une URI
     */
    private void joinServer() {
        //  Connexion au serveur de noms (obtention d'un handle)
        try {
            this.linda = (LindaRMI) Naming.lookup(this.serverURI);
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    /**
     * Se reconnecter à un LindaServer à l'aide d'une URI
     * (attend 1s et appelle joinServer())
     */
    private void rejoinServer() {
        try {
            System.out.println("Main server dead, switching...");
            Thread.sleep(BACKUP_WAIT);
            this.joinServer();
        } catch (InterruptedException e) {
            System.err.println(e);
        }
    }

    /**
     * Appel la méthode write du {@link LindaRMI}.
     *
     * @param t le tuple à écrire.
     * @see Tuple
     */
    @Override
    public void write(Tuple t) {
        try {
            this.linda.write(t);
        } catch (RemoteException ex) {
            System.err.println("Erreur d'execution : " + ex);
            this.rejoinServer();
            this.write(t);
        }
    }

    /**
     * Appel la méthode take du {@link LindaRMI}.
     *
     * @param template le template recherché.
     * @return le tuple trouvé.
     * @see Tuple
     */
    @Override
    public Tuple take(Tuple template) {
        Tuple tuple = null;
        try {
            tuple = this.linda.take(template);
        } catch (RemoteException ex) {
            System.err.println("Erreur d'execution : " + ex);
            this.rejoinServer();
            return this.take(template);

        }
        return tuple;
    }

    /**
     * Appel le read du {@link LindaRMI}.
     *
     * @param template le template recherché.
     * @return le tuple trouvé.
     * @see Tuple
     */
    @Override
    public Tuple read(Tuple template) {
        Tuple tuple = null;
        try {
            tuple = this.linda.read(template);
        } catch (RemoteException ex) {
            System.err.println("Erreur d'execution : " + ex);
            this.rejoinServer();
            return this.read(template);
        }
        return tuple;
    }

    /**
     * Appel le tryTake du {@link LindaRMI}.
     *
     * @param template le template recherché.
     * @return le tuple trouvé.
     * @see Tuple
     */
    @Override
    public Tuple tryTake(Tuple template) {
        Tuple tuple = null;
        try {
            tuple = this.linda.tryTake(template);
        } catch (RemoteException ex) {
            System.err.println("Erreur d'execution : " + ex);
            this.rejoinServer();
            return this.tryTake(template);
        }
        return tuple;
    }

    /**
     * Appel du tryRead du {@link LindaRMI}.
     *
     * @param template le template recherché.
     * @return le tuple trouvé.
     * @see Tuple
     */
    @Override
    public Tuple tryRead(Tuple template) {
        Tuple tuple = null;
        try {
            tuple = this.linda.tryRead(template);
        } catch (RemoteException ex) {
            System.err.println("Erreur d'execution : " + ex);
            this.rejoinServer();
            return this.tryRead(template);
        }
        return tuple;
    }

    /**
     * Appel du takeAll du {@link LindaRMI}.
     *
     * @param template le template recherché.
     * @return la collection de tuple trouvée.
     * @see Tuple
     */
    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        Collection<Tuple> tupleCollection = null;
        try {
            tupleCollection = this.linda.takeAll(template);
        } catch (RemoteException ex) {
            System.err.println("Erreur d'execution : " + ex);
            this.rejoinServer();
            return this.takeAll(template);
        }
        return tupleCollection;
    }

    /**
     * Appel du readAll du {@link LindaRMI}.
     *
     * @param template le template recherché.
     * @return la collection de tuple trouvée.
     * @see Tuple
     */
    @Override
    public Collection<Tuple> readAll(Tuple template) {
        Collection<Tuple> tupleCollection = null;
        try {
            tupleCollection = this.linda.readAll(template);
        } catch (RemoteException ex) {
            System.err.println("Erreur d'execution : " + ex);
            this.rejoinServer();
            return this.readAll(template);
        }
        return tupleCollection;
    }

    /**
     * Enregistre un évènement au près du serveur.
     * Appel le callback lorsque l'èvenement à eu lieu.
     *
     * @param mode le mode de l'évènement.
     * @param timing le timing de l'évènement.
     * @param template le template recherché.
     * @param callback le callback à appeler.
     * @see Tuple
     */
    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {
        new Thread(() -> {
            try {
                CallbackRMI remoteCallback = new CallbackRMIImpl(callback);
                this.linda.eventRegister(mode, timing, template, remoteCallback);
            } catch (RemoteException e) {
                System.err.println("Erreur d'execution : " + e);
                this.rejoinServer();
                this.eventRegister(mode, timing, template, callback);
            }
        }).start();
    }

    /**
     * Appel du debug du {@link LindaRMI}.
     *
     * @param prefix le préfix lors du debug.
     */
    @Override
    public void debug(String prefix) {
        try {
            this.linda.debug(prefix);
        } catch (RemoteException ex) {
            System.err.println("Erreur d'execution : " + ex);
            this.rejoinServer();
            this.debug(prefix);
        }
    }
}