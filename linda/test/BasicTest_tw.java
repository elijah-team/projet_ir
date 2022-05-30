package linda.test;

import linda.*;

public class BasicTest_tw {

    public static void main(String[] a) {

        final Linda linda = new linda.shm.CentralizedLinda();
        // final Linda linda = new linda.server.LindaClient("//localhost:4000/aaa");

        new Thread() {
            public void run() {
                try {
                    Thread.sleep(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Tuple motif = new Tuple(Integer.class, Integer.class);
                Tuple res = linda.take(motif);
                System.out.println("(1) Resultat:" + res);
                linda.debug("(1)");
            }
        }.start();

        new Thread() {
            public void run() {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Tuple t1 = new Tuple(3, 4);
                System.out.println("(2) write: " + t1);
                linda.write(t1);

                linda.debug("(2)");

            }
        }.start();

    }
}