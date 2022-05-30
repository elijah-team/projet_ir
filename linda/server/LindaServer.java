package linda.server;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Timer;
import java.util.TimerTask;

public class LindaServer {

    /**
     * Hôte de l'URI RMI
     */
    private static final String SERVER_HOST = "localhost";
    /**
     * Port de l'URI RMI
     */
    private static final int SERVER_PORT = 4000;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws RemoteException {
        // Gestion des arguments
        if (args.length > 1) {
            System.err.println("Utilisation: java LindaRMIImpl ['DEFAULT'|filepath]");
            System.exit(1);
        }

        final String filePath;
        if (args.length == 1) {
            filePath = args[0];
        } else {
            filePath = null;
        }

        Registry dns = null;
        try {
            dns = LocateRegistry.createRegistry(4000);
        } catch (RemoteException ex) {
            System.err.println("Serveur déjà en écoute sur ce port...");
        }
        LindaRMIImpl server = new LindaRMIImpl();
        server.declareServer(SERVER_HOST, SERVER_PORT);
        // Chargement des tuples sauvegardés (si le fichier existe)
        if (filePath != null && Files.exists(Paths.get(filePath))) {
            System.out.println("Récupération des tuples depuis " + filePath + "...");
            server.load(filePath);
            System.out.println("Tuples récupérés.");
        }

        assert dns != null;
        //System.setProperty("java.rmi.server.hostname","10.135.225.194");
        dns.rebind("LindaServer", server);
        System.out.println("Le serveur est up");
        Timer timer = new Timer();
        int begin = 1000; // timer starts after 1 second.
        int timeinterval = 10 * 1000; // timer executes every 10 seconds.
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                server.save();
            }
        }, begin, timeinterval);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("Sauvegarde de sécurité");
                server.save();
            } catch (Exception e) {
                System.err.println("Erreur lors de la sauvegarde : " + e);
            }
        }));
    }
}
