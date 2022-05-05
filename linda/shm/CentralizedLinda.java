package linda.shm;

import linda.Callback;
import linda.Linda;
import linda.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Shared memory implementation of Linda. */
public class CentralizedLinda implements Linda {

    private ArrayList<Tuple> sharedSpace;

    public CentralizedLinda() {
        sharedSpace = new ArrayList<>();
    }

    @Override
    public void write(Tuple t) {

    }

    @Override
    public Tuple take(Tuple template) {
        return null;
    }

    @Override
    public Tuple read(Tuple template) {
        return null;
    }

    @Override
    public Tuple tryTake(Tuple template) {
        Tuple tupFound = null;
        for(Tuple tupSpace : sharedSpace){
            if(tupSpace.matches(template)){
                tupFound = tupSpace;
                sharedSpace.remove(tupSpace);
                return (tupFound);
            }
        }
        return null;
    }

    @Override
    public Tuple tryRead(Tuple template) {
        for (Tuple tupSpace : sharedSpace) {
            if (tupSpace.matches(template)) {
                return (tupSpace);
            }
            return null;
        }
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        Collection<Tuple> listTup = new ArrayList<>();
        for(Tuple tupSpace : sharedSpace){
            if(tupSpace.matches(template)){
                listTup.add(tupSpace);
                sharedSpace.remove(tupSpace);
            }
        }
        return (listTup);
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) {
        Collection<Tuple> listTup = new ArrayList<>();
        for(Tuple tupSpace : this.sharedSpace){
            if(tupSpace.matches(template)){
                listTup.add(tupSpace);
            }
        }
        return (listTup);
    }

    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {

    }

    @Override
    public void debug(String prefix) {

    }
}
