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
            this.notifyReaders(t);
            this.notifyTaker(t);
        }
    }

    private void notifyReaders(Tuple template) {
        synchronized(readerList) {
            for(Tuple tuple : readerList) {
                synchronized(tuple) {
                    if(tuple.matches(template))
                        tuple.notifyAll();
                }
            }
        }
    }

    private void notifyTaker(Tuple template) {
        synchronized(takerList) {
            Iterator<Tuple> i = this.takerList.iterator();
            Tuple t;

            while(i.hasNext()){
                t = i.next();
                synchronized(t) {
                    if(t.matches(template)){
                        t.notifyAll();
                        return;
                    }
                }
            }
        }
    }

    public Tuple read(Tuple template) {
        try {
            Tuple readed;
            synchronized(readerList) {
                readerList.add(template);
            }
            synchronized (sharedSpace) {
                while((readed = tryRead(template)) == null) {
                    template.wait();
                }
            }
            synchronized(readerList) {
                readerList.remove(template);
            }
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
            synchronized(takerList) {
                takerList.add(template);
            }
            synchronized (sharedSpace) {
                while((taken = tryTake(template)) == null) {
                    template.wait();
                }
            }
            synchronized(takerList) {
                takerList.remove(template);
            }
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
                    sharedSpace.remove(tupSpace);
                }
            }
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
