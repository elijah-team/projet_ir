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
        synchronized(this.sharedSpace) {
            this.sharedSpace.add(t);
            this.notifyReaders(t);
            this.notifyTaker(t);
        }
    }

    private void notifyReaders(Tuple template) {
        synchronized(this.readerList) {
            for(Tuple r : this.readerList) {
                synchronized(r) {
                    if(r.matches(template))
                        r.notifyAll();
                }
            }
        }
    }

    private void notifyTaker(Tuple template) {
        synchronized(this.takerList) {
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

            while((readed = this.tryRead(template)) == null) {
                synchronized(this.readerList) {
                    this.takerList.add(template);
                }
                template.wait();
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

            while((taken = this.tryTake(template)) == null) {
                synchronized(this.takerList) {
                    this.takerList.add(template);
                }
                template.wait();
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
        return null;
    }

    @Override
    public Tuple tryRead(Tuple template) {
        return null;
    }

    @Override
    public Collection<Tuple> takeAll(Tuple template) {
        return null;
    }

    @Override
    public Collection<Tuple> readAll(Tuple template) {
        return null;
    }

    @Override
    public void eventRegister(eventMode mode, eventTiming timing, Tuple template, Callback callback) {

    }

    @Override
    public void debug(String prefix) {

    }
}
