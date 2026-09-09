import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// ['123', 'qwe', 'abc', 'qwedft', 'world', 'hello']
// performance!!!
// reverse each word
// in separate threads

public class ConcurrentReverseStringTest {

    static final int N = Integer.getInteger("N", 200_000);
    static final int M = Integer.getInteger("M", 20);
    static final int THREADS = Integer.getInteger("threads", Runtime.getRuntime().availableProcessors());
    static final int WARMUP = Integer.getInteger("warmup", 30);
    static final int ITERATIONS = Integer.getInteger("iterations", 10);
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

    /**
     * Coarse-grained variant: the input is split into {@code threads} large chunks,
     * so only ~{@code threads} tasks are submitted instead of N tiny ones.
     * Each task writes into its own non-overlapping range of {@code out}, so there is
     * no synchronization on the hot path. The checksum is computed sequentially
     * afterwards to avoid sharing a mutable counter between threads.
     */
    public static String[] reverseWithExecutorChunked(String[] input, ExecutorService executor, int threads) throws Exception {
        int len = input.length;
        String[] out = new String[len];

        if (len == 0) {
            sink = 1;
            return out;
        }

        int effectiveThreads = Math.max(1, Math.min(threads, len));
        int chunk = (len + effectiveThreads - 1) / effectiveThreads;
        ArrayList<Future<?>> futures = new ArrayList<>(effectiveThreads);

        for (int t = 0; t < effectiveThreads; t++) {
            final int start = t * chunk;
            final int end = Math.min(start + chunk, len);
            if (start >= end) break;
            futures.add(executor.submit(() -> {
                for (int i = start; i < end; i++) {
                    out[i] = reverse(input[i]);
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get();
        }

        long checksum = 1;
        for (String r : out) {
            checksum = checksum * 31 + r.hashCode();
        }

        sink = checksum;
        return out;
    }


}
