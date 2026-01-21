import org.junit.jupiter.api.Test;

public class MultipleCountersQuestTest {

    final long ITERATIONS = 1_000_000_000;

    // OOP is a lie.
    static class Counters {
        volatile long counter1;
        volatile long counter2;
    }

    static long run(Runnable r1, Runnable r2) throws Exception {
        Thread t1 = new Thread(r1);
        Thread t2 = new Thread(r2);

        long t0 = System.currentTimeMillis();
        t1.start();
        t2.start();

        t1.join();
        t2.join();
        return System.currentTimeMillis() - t0;
    }

    @Test
    void runUnpadded() throws Exception {
        final var counters = new Counters();

        var result = run(
                () -> {
                    for (long i = 0; i < ITERATIONS; i++) counters.counter1 += 1;
                },
                () -> {
                    for (long i = 0; i < ITERATIONS; i++) counters.counter2 += 1;
                }
        );
        System.out.println("----------------------------------------------");
        System.out.printf("counter1 = %d; counter2 = %d %n", counters.counter1, counters.counter2);
        System.out.printf("time = %dms %n", result);
        System.out.println("----------------------------------------------");
    }


}
