package linda.shm;

import linda.Callback;
import linda.Tuple;

/**
 * Représente un évènement futur, read ou take. Associe un template à un
 * callback.
 *
 * @author Nathan Chavas
 * @author Mohamed Moudjeb
 */
public class Event {

    /**
     * Stock un tuple et un callback.
     */
    private Tuple motif;
    private Callback callback;

    /**
     * Construit un Event avec un template et un callback.
     *
     * @param motif le template associé à l'évènement.
     * @param callback le callback appelé lors de l'évènement.
     * @see Tuple
     */
    public Event(Tuple motif, Callback callback) {
        this.motif = motif;
        this.callback = callback;
    }

    /**
     * Vérifie si le tuple correspond au template.
     *
     * @param tuple le tuple comparé au template.
     * @return vrai si le tuple match le template.
     * @see Tuple
     */
    public boolean isMatching(Tuple tuple) {
        return tuple.matches(this.motif);
    }

    /**
     * Appel du callback associé à l'évènement.
     *
     * @param t le tuple à envoyé pour le call.
     * @see Tuple
     */
    public void call(Tuple t) {
        this.callback.call(t);
    }
}