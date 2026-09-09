import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.junit.jupiter.api.Test;

public class FalseSharingDemoTest {

  private static final int THREADS =
      Runtime.getRuntime().availableProcessors();

  private static final int ITERATIONS = 10_000_000;
  private static final int RUNS = 5;

  static final class CompactAtomicLong extends AtomicLong {
  }

  static final class PaddedAtomicLong extends AtomicLong {
    long p01, p02, p03, p04, p05;
    long p06, p07, p08, p09, p10;
    long p11, p12, p13, p14, p15;
  }

  @Test
  public void test() throws Exception {
    System.out.println("Threads:               " + THREADS);
    System.out.println("Iterations per thread: " + ITERATIONS);
    System.out.println("Total increments:      " + (long) THREADS * ITERATIONS);
    System.out.println();

    try (ExecutorService executor =
        Executors.newFixedThreadPool(THREADS)) {

      // JIT warmup
      for (int i = 0; i < 2; i++) {
        run(createCompactCounters(), executor);
        run(createPaddedCounters(), executor);
        runSingleThreaded();
        runLongAdder(executor);
      }

      System.out.println("---- benchmark ----");

      for (int i = 1; i <= RUNS; i++) {

        long compact = run(
            createCompactCounters(),
            executor
        );

        long padded = run(
            createPaddedCounters(),
            executor
        );

        long single = runSingleThreaded();

        long longAdder = runLongAdder(executor);

        System.out.printf(
            """
            run %d:
              compact   = %8.2f ms
              padded    = %8.2f ms
              single    = %8.2f ms
              LongAdder = %8.2f ms

              padding speedup:   %.2fx
              LongAdder speedup: %.2fx
            %n""",
            i,
            compact / 1_000_000.0,
            padded / 1_000_000.0,
            single / 1_000_000.0,
            longAdder / 1_000_000.0,
            (double) compact / padded,
            (double) compact / longAdder
        );
      }
    }
  }

  private static AtomicLong[] createCompactCounters() {
    AtomicLong[] counters = new CompactAtomicLong[THREADS];

    Arrays.setAll(
        counters,
        ignored -> new CompactAtomicLong()
    );

    return counters;
  }

  private static AtomicLong[] createPaddedCounters() {
    AtomicLong[] counters = new PaddedAtomicLong[THREADS];

    Arrays.setAll(
        counters,
        ignored -> new PaddedAtomicLong()
    );

    return counters;
  }

  private static long run(
      AtomicLong[] counters,
      ExecutorService executor
  ) throws Exception {

    CountDownLatch start = new CountDownLatch(1);
    List<Future<?>> tasks = new ArrayList<>(THREADS);

    for (int i = 0; i < THREADS; i++) {
      int index = i;

      tasks.add(executor.submit(() -> {
        start.await();

        AtomicLong counter = counters[index];

        for (int j = 0; j < ITERATIONS; j++) {
          counter.incrementAndGet();
        }

        return null;
      }));
    }

    long started = System.nanoTime();

    start.countDown();

    for (Future<?> task : tasks) {
      task.get();
    }

    long elapsed = System.nanoTime() - started;

    verify(counters);

    return elapsed;
  }

  private static long runSingleThreaded() {
    AtomicLong counter = new AtomicLong();

    long totalIterations = (long) THREADS * ITERATIONS;

    long started = System.nanoTime();

    for (long i = 0; i < totalIterations; i++) {
      counter.incrementAndGet();
    }

    long elapsed = System.nanoTime() - started;

    if (counter.get() != totalIterations) {
      throw new AssertionError(
          "Expected: " + totalIterations + ", actual: " + counter.get()
      );
    }

    return elapsed;
  }

  private static long runLongAdder(
      ExecutorService executor
  ) throws Exception {

    LongAdder adder = new LongAdder();

    CountDownLatch start = new CountDownLatch(1);
    List<Future<?>> tasks = new ArrayList<>(THREADS);

    for (int i = 0; i < THREADS; i++) {
      tasks.add(executor.submit(() -> {
        start.await();

        for (int j = 0; j < ITERATIONS; j++) {
          adder.increment();
        }

        return null;
      }));
    }

    long started = System.nanoTime();

    start.countDown();

    for (Future<?> task : tasks) {
      task.get();
    }

    long elapsed = System.nanoTime() - started;

    long expected = (long) THREADS * ITERATIONS;
    long actual = adder.sum();

    if (actual != expected) {
      throw new AssertionError(
          "Expected: " + expected + ", actual: " + actual
      );
    }

    return elapsed;
  }

  private static void verify(AtomicLong[] counters) {
    long actual = Arrays.stream(counters)
        .mapToLong(AtomicLong::get)
        .sum();

    long expected = (long) THREADS * ITERATIONS;

    if (actual != expected) {
      throw new AssertionError(
          "Expected: " + expected + ", actual: " + actual
      );
    }
  }
}