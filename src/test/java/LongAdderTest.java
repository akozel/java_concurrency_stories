import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class LongAdderTest {

    final long ITERATIONS = 10_000_000L;
    final int THREADS = 10;

    static long run(Thread[] threads) throws Exception {
        long t0 = System.currentTimeMillis();
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        return System.currentTimeMillis() - t0;
    }

    @Test
    public void atomic_long_counter_test() throws Exception {
        var counter = new AtomicLong(0);

        Thread[] threads = new Thread[THREADS];
        for (int t = 0; t < THREADS; t++) {
            threads[t] = new Thread(() -> {
                for (long i = 0; i < ITERATIONS; i++) {
                    counter.incrementAndGet();
                }
            });
        }

        long timeMs = run(threads);

        System.out.println("----------------------------------------------");
        System.out.printf("AtomicLong value = %d (expected = %d)%n", counter.get(), THREADS * ITERATIONS);
        System.out.printf("time = %dms%n", timeMs);
        System.out.println("----------------------------------------------");
    }

    @Test
    public void long_adder_test() throws Exception {
        var adder = new LongAdder();

        Thread[] threads = new Thread[THREADS];
        for (int t = 0; t < THREADS; t++) {
            threads[t] = new Thread(() -> {
                for (long i = 0; i < ITERATIONS; i++) {
                    adder.increment();
                }
            });
        }

        long timeMs = run(threads);

        System.out.println("----------------------------------------------");
        System.out.printf("LongAdder sum = %d (expected = %d)%n", adder.sum(), THREADS * ITERATIONS);
        System.out.printf("time = %dms%n", timeMs);
        System.out.println("----------------------------------------------");
    }
}
