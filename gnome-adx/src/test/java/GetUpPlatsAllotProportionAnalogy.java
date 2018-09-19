
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * Created by WangHaibo on 2016/10/13.
 */
public class GetUpPlatsAllotProportionAnalogy {

    private static final int NUM_THREADS = 50;
    static final CountDownLatch latch = new CountDownLatch(NUM_THREADS);

    private static ThreadLocal<Random> randomThreadLocal = new ThreadLocal<Random>() {
        @Override
        protected Random initialValue() {
            return new Random(System.nanoTime());
        }
    };
    public static void main(String[] args) throws InterruptedException {

    for (int i = 0; i < NUM_THREADS; i++) {
      new TransferThread().start();
      latch.countDown();
//      System.out.println(i);
    }


//        List<Integer> list = new ArrayList<>();
//        list.add(5);
//        list.add(15);
//        list.add(52);
//        list.add(25);
//
//        list = getSelectLists(list);
//        System.out.println("--------");
//        System.out.println(list);
    }

    public static List<Integer> getSelectLists(List<Integer> upPlatLists) {

        Map<Integer, Integer> mapOfRandom = new HashMap<>();

        int proportion;
        for (int i=0; i<upPlatLists.size(); ++i) {
            proportion = randomThreadLocal.get().nextInt(100);
            mapOfRandom.put(upPlatLists.get(i), proportion);
            if (proportion < 30) {
                continue;
            } else {
                upPlatLists.remove(i--);
            }
        }

        System.out.println("mapOfRandom: " + mapOfRandom);
        System.out.println("upPlatsList: " + upPlatLists);

        return upPlatLists;
    }

    public static Set<Integer> getSelectList(Map<Integer, Integer> mapPlats) {

//    Map<Integer, Integer>  mapOfRandom = new HashMap<>();
//    int proportion;
//    List<Integer> platLists = new ArrayList<>();
//    platLists.addAll(mapPlats.keySet());
//    System.out.println("平台列表：" + platLists);
//    for (int i=0; i<platLists.size(); ++i) {
//      proportion = randomThreadLocal.get().nextInt(100);
//      mapOfRandom.put(platLists.get(i), proportion);
//      if (proportion < mapPlats.get(platLists.get(i))) {
//        ///
//      } else {
//        //platList.remove(platList.indexOf(platList.get(i)));
//        mapPlats.remove(platLists.get(i));
//        //i--;
//      }
//    }
//
//    System.out.println("mapOfRandom" + mapOfRandom);
//    System.out.println("SetPlats   " + mapPlats.keySet());
//    return mapPlats.keySet();


        Map<Integer, Integer>  mapOfRandom = new HashMap<>();
        int proportion;
        List<Integer> platLists = new ArrayList<>();
        platLists.addAll(mapPlats.keySet());
        System.out.println("平台列表：" + platLists);
        for (int i=0; i<platLists.size(); ++i) {
            proportion = randomThreadLocal.get().nextInt(100);
            mapOfRandom.put(platLists.get(i), proportion);
            if (proportion < mapPlats.get(platLists.get(i))) {
                ///
            } else {
                //platList.remove(platList.indexOf(platList.get(i)));
                mapPlats.remove(platLists.get(i));
                //i--;
            }
        }

        System.out.println("mapOfRandom" + mapOfRandom);
        System.out.println("SetPlats   " + mapPlats.keySet());
        return mapPlats.keySet();
    }
    static class TransferThread extends Thread {
        private static ThreadLocal<Random> randomThreadLocal = new ThreadLocal<Random>() {
            @Override
            protected Random initialValue() {
                return new Random(System.nanoTime());
            }
        };

        TransferThread() {

        }

        private static final int NUM_ITERATIONS = 1000000;

        @Override
        public void run() {
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            {
                //Map<Integer, Integer> mapLists = new HashMap<>();
                List<Integer> upPlatlists = new ArrayList<>();
                long start = System.nanoTime();
                for (int i = 0; i < NUM_ITERATIONS; i++) {
                    //int proportionRandom = new Random(System.nanoTime()).nextInt(100);
                    upPlatlists.add(12);
                    upPlatlists.add(23);
                    upPlatlists.add(10);
                    upPlatlists.add(19);
                    //getSelectList(mapLists);
                    getSelectLists(upPlatlists);
                }
                long end = System.nanoTime();
                System.out.println(Thread.currentThread().getName() + ",selectPlats," + (end - start) * 1.0 /1000 / NUM_ITERATIONS   + ",us");
            }
        }


        public static List<Integer> getSelectLists(List<Integer> upPlatLists) {

            //Map<Integer, Integer> mapOfRandom = new HashMap<>();

            int proportion;
            for (int i=0; i<upPlatLists.size(); ++i) {

                proportion = randomThreadLocal.get().nextInt(100);
                //mapOfRandom.put(upPlatLists.get(i), proportion);
                if (proportion < 30) {
                    continue;
                } else {
                    upPlatLists.remove(i--);
                }
            }

//      System.out.println("mapOfRandom: " + mapOfRandom);
//      System.out.println("upPlatsList: " + upPlatLists);

            return upPlatLists;
        }
    }
}

