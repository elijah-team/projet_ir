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

    /**
     * Déclare le serveur courant comme serveur Linda sur l'URI composée d'un hôte et d'un port
     * Si un serveur est déjà déclaré à cette URI, s'enregistre en tant que serveur de backup
     * @param host l'hôte de l'URI
     * @param port le port de l'URI
     * @throws RemoteException en cas d'erreur réseau
     */
    void declareServer(String host, int port) throws RemoteException;

    void write(Tuple t) throws RemoteException;

    Tuple take(Tuple template) throws RemoteException;

    Tuple read(Tuple template) throws RemoteException;

    Tuple tryTake(Tuple template) throws RemoteException;

    Tuple tryRead(Tuple template) throws RemoteException;

    Collection<Tuple> takeAll(Tuple template) throws RemoteException;

    Collection<Tuple> readAll(Tuple template) throws RemoteException;

    void eventRegister(eventMode mode, eventTiming timing, Tuple template, CallbackRMI callback) throws RemoteException;

    void save() throws RemoteException;

    void load(String filePath) throws RemoteException;

    /**
     * Enregistre un serveur Linda comme serveur de backup pour le serveur courant
     * Et lui envoie tous les tuples déjà présents sur le serveur courant afin qu'il "rattrape" son état
     * @param backupServer le serveur de backup à enregistrer comme backup
     * @throws RemoteException en cas d'erreur réseau
     */
    void registerBackup(LindaRMIImpl backupServer) throws RemoteException;

    /**
     * Appelle la méthode keepAlive() du serveur principal pour voir si il répond puis attend un certain temps
     * Si une erreur réseau se produit lors de l'appel à keepAlive(), reprend la main en tant que serveur principal
     * Uniquement utilisé par le serveur de backup
     * @throws RemoteException en cas d'erreur réseau
     */
    void pollPrimary() throws RemoteException;

    /**
     * Appelé sur le serveur principal par le serveur de backup dans pollPrimary()
     * Ne fait rien, permet juste de vérifier si une erreur réseau se produit lors de l'appel à cette méthode
     * @throws RemoteException en cas d'erreur réseau, permet au serveur de backup de reprendre la main
     */
    void keepAlive() throws RemoteException;

    void debug(String prefix) throws RemoteException;
}