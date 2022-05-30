package linda.shm;

import linda.Backup;
import linda.Callback;
import linda.Linda;
import linda.Tuple;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Implémentation de type mémoire partagée de Linda.
 *
 * @author Nathan Chavas
 * @author Mohamed Moudjeb
 */
public class CentralizedLinda implements Linda, Backup {

    private String filePath = "./.linda_backup";
    private List<Tuple> sharedSpace;
    private final List<Event> readerEventList;
    private final List<Event> takerEventList;

    public CentralizedLinda() {
        this.sharedSpace = Collections.synchronizedList(new ArrayList<Tuple>());
        this.readerEventList = Collections.synchronizedList(new ArrayList<Event>());
        this.takerEventList = Collections.synchronizedList(new ArrayList<Event>());
    }

    /**
     * Méthode write permettant l'écriture d'un tuple dans l'espace partagé.
     * Prévient les clients en attente d'un motif spécifique.
     * 
     * @param tuple Tuple ajouté à l'espace partagé.
     */
    public void write(Tuple tuple) {
        // Clonage du tuple pour éviter des mauvaises manipulations.
        tuple = tuple.deepclone();
        System.out.println("Notify readers");
        this.notifyReaders(tuple);
        System.out.println("Notify takers");
        boolean taken = this.notifyTaker(tuple);
        // Ajoute le tuple à l'espace partagé si personne ne l'a pris.
        if (!taken)
            this.sharedSpace.add(tuple);
        save();
    }

    /**
     * Alerte les lecteurs en attente de l'ajout d'un tuple dans l'espace
     * partagé.
     * 
     * @param tuple Tuple ajouté à l'espace partagé.
     */
    private void notifyReaders(Tuple tuple) {
        ArrayList<Event> toRemove = new ArrayList<>();
        // Accès restreint à la liste des lecteurs.
        synchronized (this.readerEventList) {
            // On parcours les lecteurs en attente
            for (Event readEvent : readerEventList) {
                if (readEvent.isMatching(tuple)) {
                    System.out.println("Motif trouvé");
                    //readEvent.call(tuple);
                    // Lecture effectuée
                    toRemove.add(readEvent);
                }
            }
            // On retire tous les évenements de lecture effectués
            for (Event e : toRemove) {
                readerEventList.remove(e);
                e.call(tuple);
            }
        }
    }

    /**
     * Alerte les consomateurs en attente de l'ajout d'un tuple dans l'espace
     * partagé.
     * 
     * @param tuple Tuple ajouté à l'espace partagé.
     */
    private boolean notifyTaker(Tuple tuple) {
        boolean taken = false;
        Event takeEvent = null;
        synchronized (this.takerEventList) {
            Iterator<Event> someEvent = this.takerEventList.iterator();
            // Tant qu'il y a un évènement en mode take et que le tuple n'a pas été pris
            while (someEvent.hasNext() && !taken) {
                takeEvent = someEvent.next();
                // Si le tuple à écrire correspond au motif associé à l'évènement
                if (takeEvent.isMatching(tuple)) {
                    // Appel du callback de l'évènement
                    takeEvent.call(tuple);
                    // On signifie qu'on a consommé le tuple pour arrêter de chercher
                    taken = true;
                }
            }
            // Enlève l'évènement du registre
            if (taken)
                takerEventList.remove(takeEvent);
        }
        return taken;
    }

    /**
     * Lis et renvoi un tuple de l'espace partagé s'il en existe un correspondant au
     * motif.
     * Se met en attente passive jusqu'à avoir un tuple de disponible.
     *
     * @param template le template du tuple que l'on veut read
     * @return le tuple trouvé.
     * @see Tuple
     */
    @Override
    public Tuple read(Tuple template) {
        TupleCallback cb = new TupleCallback();
        System.out.println(template);
        eventRegister(Linda.eventMode.READ, Linda.eventTiming.IMMEDIATE, template, cb);
        cb.waitCallback();
        System.out.println("Je lis : " + cb.getTuple());
        return cb.getTuple();
    }

    /**
     * Retire et renvoi un tuple de l'espace partagé s'il en existe un correspondant
     * au motif.
     * Se met en attente passive jusqu'à avoir un tuple de disponible.
     *
     * @param template Le motif recherché.
     * @return Le tuple trouvé en mémoire correspondant au motif.
     * @see Tuple
     */
    public Tuple take(Tuple template) {
        TupleCallback cb = new TupleCallback();
        eventRegister(Linda.eventMode.TAKE, Linda.eventTiming.IMMEDIATE, template, cb);
        cb.waitCallback();
        System.out.println("Je prends : " + cb.getTuple());
        return cb.getTuple();
    }

    /**
     * Read non bloquant.
     *
     * @param template Le motif du tuple que l'on recherche.
     * @return Le tuple trouvé en mémoire correspondant au template. Null si
     *         aucun tuple correspondant.
     */
    @Override
    public Tuple tryRead(Tuple template) {
        synchronized (this.sharedSpace) {
            // Parcours de la mémoire partagée
            for (Tuple tuple : this.sharedSpace) {
                // Si un tuple correspond au motif
                if (tuple.matches(template)) {
                    // Renvoi le tuple
                    return tuple;
                }
            }
        }
        // Si aucun tuple n'a pas été trouvé
        return null;
    }

    /**
     * Take non bloquant.
     *
     * @param template Le motif du tuple que l'on souhaite prendre.
     * @return Le tuple trouvé en mémoire correspondant au template. Null si
     *         aucun tuple correspondant.
     */
    @Override
    public Tuple tryTake(Tuple template) {
        synchronized (this.sharedSpace) {
            // Parcours de la mémoire partagée
            for (Tuple tuple : this.sharedSpace) {
                // Si un tuple correspond au motif
                if (tuple.matches(template)) {
                    // enlève le premier tuple trouvé de la mémoire
                    this.sharedSpace.remove(tuple);
                    save();
                    // retourne le tuple
                    return tuple;
                }
            }
        }
        // Si aucun tuple n'a été trouvé
        return null;
    }

    /**
     * Recupère tous les tuples correspondants au template et
     * les retire de la mémoire partagée.
     *
     * @param template Le motif recherché.
     * @return La collection de tuple trouvés en mémoire correspondant au motif.
     *         Vide si aucun tuple ne correspond.
     * @see Tuple
     */
    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        Tuple tuple;
        Collection<Tuple> list = new ArrayList<Tuple>();
        while ((tuple = tryTake(template)) != null) {
            list.add(tuple);
        }
        System.out.println("Je prend tout : " + list);
        save();
        return list;
    }

    /**
     * Recupère tous les tuples correspondants au template et
     * les laisse dans la mémoire partagée.
     *
     * @param template Le motif recherché.
     * @return La collection de tuple trouvés en mémoire correspondant au motif.
     *         Vide si aucun tuple ne correspond.
     * @see Tuple
     */
    @Override
    public Collection<Tuple> readAll(Tuple template) {
        Collection<Tuple> list = new ArrayList<Tuple>();
        for (Tuple tuple : this.sharedSpace) {
            if (tuple.matches(template)) {
                list.add(tuple);
            }
        }
        System.out.println("Je lis tout: " + list.toString());
        return list;
    }

    /**
     * Procédure eventRegister permettant d'enregistrer les évenements dans la liste
     * des
     * registres s'ils sont en attente. Execute le mode demandé si possible.
     *
     * @param mode     Le mode de l'évènement (read ou take).
     * @param timing   Le timing de l'évènement (immédiat ou futur).
     * @param template Le template du tuple à chercher.
     * @param callback Le callback a appeler lors de l'évènement.
     * @see Tuple
     */
    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {
        System.out.println("Evenement : " + mode.name() + " " + template.toString());
        // Si c'est un évenement immédiat...
        if (timing.equals(eventTiming.IMMEDIATE)) {
            // Si c'est une lecture...
            if (mode.equals(eventMode.READ)) {
                // Tentative de lecture
                Tuple tuple = this.tryRead(template);
                if (tuple == null) {
                    // Si aucun tuple à lire n'a été trouvé dans l'espace partagé : ajout de
                    // l'évenement dans le registre correspondant...
                    this.readerEventList.add(new Event(template, callback));
                } else {
                    // Sinon on appelle le callback associé à l'évènement
                    callback.call(tuple);
                }
            } else {
                // Si c'est un retrait...
                // Tentative de retrait
                Tuple tuple = this.tryTake(template);
                if (tuple == null) {
                    // Si aucun tuple à retirer n'a été trouvé dans l'espace partagé : ajout de
                    // l'évenement dans le registre correspondant...
                    this.takerEventList.add(new Event(template, callback));
                } else {
                    // Sinon on appelle le callback associé à l'évènement
                    callback.call(tuple);
                }
            }
        } else {
            // Si c'est un évenement futur...
            if (mode.equals(eventMode.READ)) {
                // Si c'est une lecture : ajout de l'évenement dans le registre des lecteurs en
                // attente...
                this.readerEventList.add(new Event(template, callback));
            } else {
                // Si c'est un take : ajout de l'évenement dans le registre des consommateurs en
                // attente
                this.takerEventList.add(new Event(template, callback));
            }
        }
    }

    /**
     * Sauvegarde les tuples de l'espace partagé au chemin spécifié.
     */
    public void save() {
        try {
            FileOutputStream file_output = new FileOutputStream(filePath);
            ObjectOutputStream object_output = new ObjectOutputStream(file_output);
            object_output.writeObject(this.sharedSpace);
            System.out.println("Mémoire partagée sauvegardée");
            object_output.close();
            file_output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Charge le fichier de tuple spécifié en paramètre dans l'espace partagé.
     */
    public void load() {
        if (!Files.exists(Paths.get(filePath)))
            throw new IOError(new RuntimeException("Le fichier spécifié est introuvable"));
        try {
            FileInputStream file_input = new FileInputStream(filePath);
            ObjectInputStream object_input = new ObjectInputStream(file_input);
            List<Tuple> readCase = (List<Tuple>) object_input.readObject();
            this.sharedSpace.addAll(readCase);
            object_input.close();
            file_input.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * To debug, prints any information it wants (e.g. the tuples in tuplespace
     * or the registered callbacks), prefixed by
     * <code>prefix</code.
     */
    @Override
    public void debug(String prefix) {
        System.out.println("Debug " + prefix + " : " + sharedSpace.toString());
    }

    @Override
    public void setBackupPath(String filePath) {
       this.filePath = filePath;
    }
}
