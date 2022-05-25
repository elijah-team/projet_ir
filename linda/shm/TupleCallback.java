package linda.shm;

import java.io.Serializable;
import java.util.concurrent.Semaphore;
import linda.Callback;
import linda.Tuple;

/**
 * Callback utilisé pour la gestion de l'écriture/lecture concurrent.
 *
 * @author Nathan Chavas
 * @author Mohamed Moudjeb
 */
public class TupleCallback implements Callback, Serializable {
    private Semaphore sem = new Semaphore(0);

    private Tuple t;

    public void call(Tuple t) {
        this.sem.release();
        this.t = t;
    }

    public void waitCallback() {
        try {
            this.sem.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Tuple getTuple() {
        return this.t;
    }
}
