package linda.server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class LindaBackupServer {
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
