package linda.shm;

import linda.Callback;
import linda.Linda;
import linda.Tuple;

import java.io.*;
import java.util.*;

/**
 * Shared sharedSpace implementation of Linda.
 *
 * @author Nathan Chavas
 * @author Mohamed Moudjeb
 */
public class CentralizedLinda implements Linda {

    private String filepath = "./.linda_backup";
    private List<Tuple> sharedSpace;
    private final List<Event> readerEventList;
    private final List<Event> takerEventList;

    public CentralizedLinda() {
        this.sharedSpace = Collections.synchronizedList(new ArrayList<>());
        this.readerEventList = Collections.synchronizedList(new ArrayList<>());
        this.takerEventList = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * Méthode write permettant l'écriture d'un tuple dans l'espace partagé.
     * Prévient les clients en attente d'un motif spécifique.
     * @param tuple tuple ajouté à l'espace partagé.
     */
    public void write(Tuple tuple) {
        // Clonage du tuple pour éviter des mauvaises manipulations.
        tuple = tuple.deepclone();
        System.out.println("Notify readers");
        this.notifyReaders(tuple);
        System.out.println("Notify takers");
        boolean taken = this.notifyTaker(tuple);
        save(this.filepath);
        // Ajoute le tuple à l'espace partagé si personne ne l'a pris.
        if (!taken) this.sharedSpace.add(tuple);
    }

    /**
     * Alerte les lecteurs en attente de l'ajout d'un tuple dans l'espace
     * partagé.
     * @param tuple tuple ajouté à l'espace partagé.
     */
    private void notifyReaders(Tuple tuple) {
        ArrayList<Event> toRemove = new ArrayList<>();
        // Accès restreint à la liste des lecteurs.
        synchronized(this.readerEventList) {
            for (Event readEvent : readerEventList) {
                if (readEvent.isMatching(tuple)) {
                    synchronized (readEvent) {
                        System.out.println("Motif trouvé");
                        readEvent.call(tuple);
                    }
                    toRemove.add(readEvent);
                }
            }
            //On retire tous les évenements de lecture effectués
            readerEventList.removeAll(toRemove);
        }
    }

    /**
     * Alerte les consomateurs en attente de l'ajout d'un tuple dans l'espace
     * partagé.
     * @param tuple tuple ajouté à l'espace partagé.
     */
    private boolean notifyTaker(Tuple tuple) {
        boolean taken = false;
        Event takeEvent = null;
        synchronized(this.takerEventList) {
            Iterator<Event> someEvent = this.takerEventList.iterator();
            // tant qu'il y a un évènement en mode take et que le tuple n'a pas été pris
            while (someEvent.hasNext() && !taken) {
                takeEvent = someEvent.next();
                // si le tuple à écrire correspond au template associé à l'évènement
                if (takeEvent.isMatching(tuple)) {
                    // appel du callback de l'évènement
                    takeEvent.call(tuple);
                    // on signifie qu'on a consommé le tuple pour arrêter de chercher
                    taken = true;
                }
            }
            // enlève l'évènement du registre
            if (taken) takerEventList.remove(takeEvent);
        }
        return taken;
    }

    /**
     * Fonction read. Réalise un read sur un tuple s'il existe dans la mémoire
     * sinon se met en attente de l'écriture d'un tuple correspondant
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
     * Fonction take. réalise un take sur un tuple s'il existe dans la mémoire
     * sinon se met en attente jusqu'à avoir un tuple de disponible
     *
     * @param template le template du take que l'on veut faire
     * @return le tuple trouvé en mémoire correspondant au template
     * @see Tuple
     */
    public Tuple take(Tuple template) {
        TupleCallback cb = new TupleCallback();
        eventRegister(Linda.eventMode.TAKE, Linda.eventTiming.IMMEDIATE, template, cb);
        cb.waitCallback();
        System.out.println("Je prend : " + cb.getTuple());
        return cb.getTuple();
    }

    /**
     * Fonction tryRead. Fait un read non bloquant.
     *
     * @param template le template du read que l'on veut faire
     * @return le tuple trouvé en mémoire correspondant au template. Null si
     * aucun tuple trouvé
     */
    @Override
    public Tuple tryRead(Tuple template) {
        synchronized (this.sharedSpace){
            // pour chaque tuple de la mémoire
            for (Tuple tuple : this.sharedSpace) {
                // si le tuple courrant correspond au template
                if (tuple.matches(template)) {
                    // retourne le premier tuple trouvé
                    return tuple;
                }
            }
        }
        // si aucun tuple n'a pas été trouvé : renvoi null
        return null;
    }

    /**
     * Fonction tryTake. Fait un take non bloquant.
     *
     * @param template le template du tuple que l'on souhaite prendre
     * @return le tuple trouvé en mémoire correspondant au template. Null si
     * aucun tuple trouvé
     */
    @Override
    public Tuple tryTake(Tuple template) {
        synchronized (this.sharedSpace) {
            // pour chaque tuple en mémoire partagée
            for (Tuple tuple : this.sharedSpace) {
                // si le tuple courant correspond
                if (tuple.matches(template)) {
                    // enlève le premier tuple trouvé de la mémoire
                    this.sharedSpace.remove(tuple);
                    save(this.filepath);
                    // retourne le tuple
                    return tuple;
                }
            }
        }
        // si aucun tuple n'a pas été trouvé : renvoi null
        return null;
    }

    /**
     * Fonction takeAll. Recupère tous les tuples correspondants au template et
     * les enlève de la mémoire partagée.
     *
     * @param template le template du take que l'on veut faire
     * @return une collection de Tuple trouvés en mémoire correspondant au
     * template. Vide si aucun tuple n'a été trouvé.
     * @see Tuple
     */
    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        Tuple tuple;
        Collection<Tuple> list = new ArrayList();
        while ((tuple = tryTake(template)) != null) {
            list.add(tuple);
        }
        System.out.println("Je prend tout : " + list);
        save(this.filepath);
        return list;
    }

    /**
     * Fonction readAll. Recupère tous les tuples correspondants au template et
     * les laisse dans la mémoire partagée.
     *
     * @param template le template du read que l'on veut faire
     * @return une collection de tuple trouvés en mémoire correspondant au
     * template. Vide si aucun tuple n'a été trouvé
     * @see Tuple
     */
    @Override
    public Collection<Tuple> readAll(Tuple template) {
        Collection<Tuple> list = new ArrayList();
        for (Tuple tuple : this.sharedSpace) {
            if (tuple.matches(template)) {
                list.add(tuple);
            }
        }
        System.out.println("I read all : " + list.toString());
        return list;
    }

    /**
     * Procédure eventRegister. Enregistre les évenements dans la liste des
     * registres s'ils sont en attente. Sinon execute le mode demandé si
     * possible.
     *
     * @param mode le mode de l'évènement (read ou take)
     * @param timing le timing de l'évènement (immédiat ou futur)
     * @param template le template du tuple à chercher
     * @param callback le callback a appeler lors de l'évènement
     * @see Tuple
     */
    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {
        // si c'est un évenement immédiat
        if (timing.equals(eventTiming.IMMEDIATE)) {
            // si c'est un read
            if (mode.equals(eventMode.READ)) {
                // essai d'un read
                Tuple tuple = this.tryRead(template);
                if (tuple == null) {
                    // si aucun tuple à lire n'a été trouvé en mémoire : ajout de l'évenement en attente au registre
                    this.readerEventList.add(new Event(template, callback));
                } else {
                    // sinon appel le callback associé à l'évènement
                    callback.call(tuple);
                }
            } else {
                // si c'est un take
                // essai d'un take sur la mémoire partagée
                Tuple tuple = this.tryTake(template);
                if (tuple == null) {
                    // si aucun tuple n'a été trouvé : enregistrement de l'evenement dans le registre des take
                    this.takerEventList.add(new Event(template, callback));
                } else {
                    // sinon appel le callback associé à l'évènement
                    callback.call(tuple);
                }
            }
        } else {
            // si c'est un évenement futur
            if (mode.equals(eventMode.READ)) {
                // si c'est un read : enregistrement dans le registre des read en attente
                this.readerEventList.add(new Event(template, callback));
            } else {
                // si c'est un take : enregistrement dans le registre des take en attente
                this.takerEventList.add(new Event(template, callback));
            }
        }
        System.out.println("I registred : " + mode.name() + " " + template.toString());
    }

    public void save(String filepath) {
        try {
            FileOutputStream file_output = new FileOutputStream(filepath);
            ObjectOutputStream object_output = new ObjectOutputStream(file_output);
            object_output.writeObject(this.sharedSpace);
            object_output.close();
            file_output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load(String filepath) {
        try {
            FileInputStream file_input = new FileInputStream(filepath);
            ObjectInputStream object_input = new ObjectInputStream(file_input);
            this.sharedSpace = (List<Tuple>) object_input.readObject();
            object_input.close();
            file_input.close();
        } catch (IOException|ClassNotFoundException e) {
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
}
