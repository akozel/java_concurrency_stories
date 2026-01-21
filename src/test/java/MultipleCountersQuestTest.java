import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

public class MultipleCountersQuestTest {

    final long ITERATIONS = 1_000_000_000;

    // OOP is a lie.
    static class Counters {
        volatile long counter1;
        volatile long counter2;
    }

    static long run(Runnable r1, Runnable r2) throws Exception {
        var start = new CountDownLatch(1);

        Thread t1 = new Thread(() -> { await(start); r1.run(); });
        Thread t2 = new Thread(() -> { await(start); r2.run(); });

        t1.start();
        t2.start();

        long t0 = System.currentTimeMillis();
        start.countDown();

        t1.join();
        t2.join();
        return System.currentTimeMillis() - t0;
    }

    static void await(CountDownLatch l) {
        try { l.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    @Test
    void run() throws Exception {
        final var counters = new Counters();

        var result = run(
                () -> {
                    for (int i = 0; i < ITERATIONS; i++) counters.counter1 += 1;
                },
                () -> {
                    for (int i = 0; i < ITERATIONS; i++) counters.counter2 += 1;
                }
        );
        System.out.println("----------------------------------------------");
        System.out.printf("counter1 = %d; counter2 = %d %n", counters.counter1, counters.counter2);
        System.out.printf("time = %dms %n", result);
        System.out.println("----------------------------------------------");
    }


}
