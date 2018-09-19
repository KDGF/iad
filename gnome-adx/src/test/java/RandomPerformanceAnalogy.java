import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * Created by qlzhang on 9/17/2016.
 */
public class RandomPerformanceAnalogy {

  private static final int NUM_THREADS = 50;
  static final CountDownLatch latch = new CountDownLatch(NUM_THREADS);


  public static void main(String[] args) throws InterruptedException {

    for (int i = 0; i < NUM_THREADS; i++) {
      new TransferThread().start();
      latch.countDown();
//      System.out.println(i);
    }
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
        long start = System.nanoTime();
        for (int i = 0; i < NUM_ITERATIONS; i++) {
          int proportionRandom = (int) (Math.random() * 100);
        }
        long end = System.nanoTime();
        System.out.println(Thread.currentThread().getName() + ",Math.randomThreadLocal()," + (end - start) * 1.0 / 1000000 + ",ms");
      }

      {
        long start = System.nanoTime();
        for (int i = 0; i < NUM_ITERATIONS; i++) {
          int proportionRandom = randomThreadLocal.get().nextInt(100);
        }
        long end = System.nanoTime();
        System.out.println(Thread.currentThread().getName() + ",randomThreadLocal.nextInt()," + (end - start) * 1.0 / 1000000 + ",ms");
      }

      {
        long start = System.nanoTime();
        for (int i = 0; i < NUM_ITERATIONS; i++) {
          int proportionRandom = new Random(System.nanoTime()).nextInt(100);
        }
        long end = System.nanoTime();
        System.out.println(Thread.currentThread().getName() + ",new_Random(System.nanoTime()).nextInt()," + (end - start) * 1.0 / 1000000 + ",ms");
      }

    }
  }
}

