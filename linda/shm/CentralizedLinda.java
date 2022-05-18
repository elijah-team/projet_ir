package linda.shm;

import linda.Callback;
import linda.Linda;
import linda.Tuple;

import java.util.*;

/** Shared memory implementation of Linda. */
public class CentralizedLinda implements Linda {

    private final List<Tuple> sharedSpace;
    private final List<Tuple> readerList;
    private final List<Tuple> takerList;

    public CentralizedLinda() {
        sharedSpace = Collections.synchronizedList(new ArrayList<>());
        readerList = Collections.synchronizedList(new ArrayList<>());
        takerList = Collections.synchronizedList(new ArrayList<>());
    }

    public void write(Tuple t) {
        synchronized(sharedSpace) {
            this.sharedSpace.add(t);
            System.out.println(sharedSpace);
            System.out.println("Notify readers");
            this.notifyReaders(t);
            System.out.println("Notify takers");
            this.notifyTaker(t);
        }
    }

    private void notifyReaders(Tuple template) {
        synchronized(readerList) {
            for(Tuple tuple : readerList) {
                if(template.matches(tuple)) {
                    synchronized (tuple) {
                        System.out.println("Motif trouvé");
                        tuple.notifyAll();
                    }
                }
            }
        }
    }

    private void notifyTaker(Tuple template) {
        synchronized(takerList) {
            for(Tuple tuple : takerList){
                //System.out.println("tuple : " + tuple);
                //System.out.println(template);
                if(template.matches(tuple)){
                    synchronized (tuple) {
                        System.out.println("Motif trouvé");
                        tuple.notifyAll();
                    }
                    return;
                }
            }
        }
    }

    public Tuple read(Tuple template) {
        try {
            Tuple readed;
            readerList.add(template);
            synchronized(template){
                while((readed = tryRead(template)) == null) {
                    template.wait();
                }
            }
            readerList.remove(template);
            return readed;
        }
        catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Tuple take(Tuple template) {
        try {
            Tuple taken;
            takerList.add(template);
            synchronized (template) {
                while((taken = tryTake(template)) == null) {
                    System.out.println("Attente");
                    template.wait();
                }
            }
            takerList.remove(template);
            return taken;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Tuple tryTake(Tuple template) {
        Tuple tupFound = null;
        synchronized (sharedSpace){
            for(Tuple tupSpace : sharedSpace){
                if(tupSpace.matches(template)){
                    tupFound = tupSpace;
                    sharedSpace.remove(tupSpace);
                    return (tupFound);
                }
            }
        }
        return null;
    }

    @Override
    public Tuple tryRead(Tuple template) {
        synchronized (sharedSpace){
            for (Tuple tupSpace : sharedSpace) {
                if (tupSpace.matches(template)) {
                    return (tupSpace);
                }
            }
        }
        return null;
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        Collection<Tuple> listTup = new ArrayList<>();
        synchronized (sharedSpace){
            for(Tuple tupSpace : sharedSpace){
                if(tupSpace.matches(template)){
                    listTup.add(tupSpace);
                }
            }
            for(Tuple toRemove : listTup)
                sharedSpace.remove(toRemove);
        }
        return (listTup);
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) {
        Collection<Tuple> listTup = new ArrayList<>();
        synchronized (sharedSpace){
            for(Tuple tupSpace : this.sharedSpace){
                if(tupSpace.matches(template)){
                    listTup.add(tupSpace);
                }
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
