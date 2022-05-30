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
        try {
            System.out.println("LindaServer URI : " + serverURI);
            this.linda = (LindaRMI) Naming.lookup(serverURI);
            System.out.println("Connexion établie !");
        } catch (Exception e) {
            System.err.println("Erreur d'execution : " + e);
        }
    }

    /**
     * Procédure write. Appel la méthode write du {@link LindaRMI}.
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
        }
    }

    /**
     * Procédure take. Appel la méthode take du {@link LindaRMI}.
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
        }
        return tuple;
    }

    /**
     * Procédure read. Appel le read du {@link LindaRMI}.
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
        }
        return tuple;
    }

    /**
     * Procédure trytake. Appel le tryTake du {@link LindaRMI}.
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
        }
        return tuple;
    }

    /**
     * Procédure tryRead. Appel du tryRead du {@link LindaRMI}.
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
        }
        return tuple;
    }

    /**
     * Procédure takeAll. Appel du takeAll du {@link LindaRMI}.
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
        }
        return tupleCollection;
    }

    /**
     * Procédure readAll. Appel du readAll du {@link LindaRMI}.
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
        }
        return tupleCollection;
    }

    /**
     * Procédure eventRegister. Enregistre un évènement au près du serveur.
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
        try {
            CallbackRMI remoteCallback = new CallbackRMIImpl(callback);
            this.linda.eventRegister(mode, timing, template, remoteCallback);
        } catch (RemoteException e) {
            System.err.println("Erreur d'execution : " + e);
        }
    }

    /**
     * Procédure debug. Appel du debug du {@link LindaRMI}.
     *
     * @param prefix le préfix lors du debug.
     */
    @Override
    public void debug(String prefix) {
        try {
            this.linda.debug(prefix);
        } catch (RemoteException ex) {
            System.err.println("Erreur d'execution : " + ex);
        }
    }
}