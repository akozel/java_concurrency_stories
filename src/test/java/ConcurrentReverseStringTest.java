import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ConcurrentReverseStringTest {

    static final int N = Integer.getInteger("N", 5_000);
    static final int M = Integer.getInteger("M", 1_500);
    static final int THREADS = Integer.getInteger("threads", Runtime.getRuntime().availableProcessors());
    static final int WARMUP = Integer.getInteger("warmup", 30);
    static final int ITERATIONS = Integer.getInteger("iterations", 15);
    static final long SEED = Long.getLong("seed", 42L);

    @Test
    void singleThread() {
        String[] input = generate(N, M, SEED);

        for (int i = 0; i < WARMUP; i++) {
            reverseSingleThread(input);
        }

        long bestNs = Long.MAX_VALUE;
        String[] last = null;

        for (int i = 0; i < ITERATIONS; i++) {
            long t0 = System.nanoTime();
            last = reverseSingleThread(input);
            long t1 = System.nanoTime();
            bestNs = Math.min(bestNs, t1 - t0);
        }

        System.out.printf(
                "singleThread: N=%d M=%d best=%.2fms%n",
                N, M, bestNs / 1_000_000.0
        );

        assert last != null;
        assert last.length == input.length;
    }

    @Test
    void executorFixedThreadPool() throws Exception {
        String[] input = generate(N, M, SEED);

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            // Прогрев
            for (int i = 0; i < WARMUP; i++) {
                reverseWithExecutor(input, executor);
            }

            long bestNs = Long.MAX_VALUE;
            String[] last = null;

            for (int i = 0; i < ITERATIONS; i++) {
                long t0 = System.nanoTime();
                last = reverseWithExecutor(input, executor);
                long t1 = System.nanoTime();
                bestNs = Math.min(bestNs, t1 - t0);
            }

            System.out.printf(
                    "executor(%d threads): N=%d M=%d best=%.2f ms%n",
                    THREADS, N, M, bestNs / 1_000_000.0
            );

        } finally {
            executor.shutdown();
        }
    }


    public static String[] generate(int n, int m, long seed) {
        if (n < 0 || m < 0) throw new IllegalArgumentException("n and m must be >= 0");

        var rnd = new SplittableRandom(seed);
        var arr = new String[n];

        for (int i = 0; i < n; i++) {
            char[] chars = new char[m];
            for (int j = 0; j < m; j++) {
                chars[j] = (char) ('a' + rnd.nextInt(26));
            }
            arr[i] = new String(chars);
        }
        return arr;
    }

    public static volatile long sink;

    public static String reverse(String s) {
        return new StringBuilder(s).reverse().toString();
    }

    public static String[] reverseSingleThread(String[] input) {
        String[] out = new String[input.length];
        long checksum = 1;

        for (int i = 0; i < input.length; i++) {
            String r = reverse(input[i]);
            out[i] = r;
            checksum = checksum * 31 + r.hashCode();
        }

        sink = checksum;
        return out;
    }

    public static String[] reverseWithExecutor(String[] input, ExecutorService executor) throws Exception {
        String[] out = new String[input.length];
        ArrayList<Future<String>> futures = new ArrayList<>(input.length);

        for (String s : input) {
            futures.add(executor.submit(() -> reverse(s)));
        }

        long checksum = 1;
        for (int i = 0; i < futures.size(); i++) {
            String r = futures.get(i).get();
            out[i] = r;
            checksum = checksum * 31 + r.hashCode();
        }

        sink = checksum;
        return out;
    }


}
