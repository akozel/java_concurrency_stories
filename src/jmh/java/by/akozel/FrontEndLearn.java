package by.akozel;

import java.nio.file.Path;
import java.util.Random;

public class FrontEndLearn {

  static final int SIZE = 30_000_000;
  static final int WARMUP_RUNS = 5;
  static final int RUNS = 10;

  /*
   * Prevents the JVM from treating benchmark results as unused.
   */
  static volatile double BLACKHOLE;

  public enum Status {
    PROCESSED,
    CANCELED
  }

  public record Order(double price, Status status) {
  }

  /*
   * Inlining of this method is explicitly disabled.
   *
   * This is important because otherwise C2 could inline the addition
   * and make the first two tests behave differently from what we want
   * to demonstrate.
   */
  static double add(double sum, double price) {
    return sum + price;
  }

  /*
   * TEST 1
   *
   * Branch before computation.
   *
   * The computation is performed only for PROCESSED orders.
   *
   * With random 50/50 input:
   *
   *   ~15M calls to add()
   *   +
   *   an unpredictable branch
   */
  static double branchBeforeComputation(Order[] orders) {
    double sum = 0.0;

    for (var order : orders) {
      if (Status.PROCESSED == order.status) {
        sum = add(sum, order.price); // compute sum only when it is necessary (do less work)
      }
    }

    return sum;
  }

  /*
   * TEST 2
   *
   * Computation before branch.
   *
   * The computation is performed on EVERY iteration:
   *
   *   30M calls to add()
   *
   * even though approximately half of the results are discarded.
   *
   * However, when the condition is evaluated, both possible values
   * are already available:
   *
   *   old sum
   *   tmp
   *
   * This gives the JIT an opportunity to replace the unpredictable
   * control-flow branch with a conditional selection.
   */
  static double computationBeforeBranch(Order[] orders) {
    double sum = 0.0;

    for (var order : orders) {
      double tmp = add(sum, order.price); // compute ALWAYS!!!

      if (Status.PROCESSED == order.status) {
        sum = tmp;
      }
    }

    return sum;
  }

  /*
   * TEST 3
   *
   * The addition is written directly in the hot loop.
   *
   * There is no separate method call.
   *
   * This represents the manually inlined version of TEST 1.
   */
  static double inlineAddition(Order[] orders) {
    double sum = 0.0;

    for (var order : orders) {
      if (Status.PROCESSED == order.status) {
        sum += order.price;
      }
    }

    return sum;
  }

  public static void main(String[] args) throws Exception {

    /*
     * The first JVM only relaunches this program with the required
     * HotSpot compiler directives.
     */
    if (!Boolean.getBoolean("benchmark.child")) {
      relaunchWithJvmFlags();
      return;
    }

    runBenchmark();
  }

  static void runBenchmark() {
    var random = new Random(42);
    var orders = new Order[SIZE];

    /*
     * The exact same array is used by all tests.
     *
     * Status distribution is random ~50/50.
     */
    for (int i = 0; i < SIZE; i++) {
      var status = random.nextBoolean()
          ? Status.PROCESSED
          : Status.CANCELED;

      orders[i] = new Order(1.0, status);
    }

    /*
     * JIT warmup.
     */
    for (int i = 0; i < WARMUP_RUNS; i++) {
      BLACKHOLE = branchBeforeComputation(orders);
      BLACKHOLE = computationBeforeBranch(orders);
      BLACKHOLE = inlineAddition(orders);
    }

    /*
     * Run tests sequentially.
     */
    double branchBeforeTime =
        benchmarkBranchBeforeComputation(orders);

    double computationBeforeTime =
        benchmarkComputationBeforeBranch(orders);

    double inlineAdditionTime =
        benchmarkInlineAddition(orders);

    /*
     * Print only final results.
     */
    System.out.printf(
        "Branch before computation : %8.3f ms%n",
        branchBeforeTime
    );

    System.out.printf(
        "Computation before branch : %8.3f ms%n",
        computationBeforeTime
    );

    System.out.printf(
        "Inline addition           : %8.3f ms%n",
        inlineAdditionTime
    );
  }

  static double benchmarkBranchBeforeComputation(Order[] orders) {
    double total = 0.0;

    for (int run = 0; run < RUNS; run++) {
      long start = System.nanoTime();

      double result = branchBeforeComputation(orders);

      long elapsed = System.nanoTime() - start;

      BLACKHOLE = result;

      total += elapsed / 1_000_000.0;
    }

    return total / RUNS;
  }

  static double benchmarkComputationBeforeBranch(Order[] orders) {
    double total = 0.0;

    for (int run = 0; run < RUNS; run++) {
      long start = System.nanoTime();

      double result = computationBeforeBranch(orders);

      long elapsed = System.nanoTime() - start;

      BLACKHOLE = result;

      total += elapsed / 1_000_000.0;
    }

    return total / RUNS;
  }

  static double benchmarkInlineAddition(Order[] orders) {
    double total = 0.0;

    for (int run = 0; run < RUNS; run++) {
      long start = System.nanoTime();

      double result = inlineAddition(orders);

      long elapsed = System.nanoTime() - start;

      BLACKHOLE = result;

      total += elapsed / 1_000_000.0;
    }

    return total / RUNS;
  }

  /*
   * Relaunch the same program in a child JVM with the compiler
   * directives required for this experiment.
   */
  static void relaunchWithJvmFlags() throws Exception {
    String javaExecutable = javaExecutable();
    String classpath = System.getProperty("java.class.path");

    var process = new ProcessBuilder(
        javaExecutable,

        /*
         * Prevent an infinite self-relaunch loop.
         */
        "-Dbenchmark.child=true",

        /*
         * Keep heap configuration stable.
         */
        "-Xms2g",
        "-Xmx2g",

        /*
         * Suppress CompileCommand messages.
         */
        "-XX:CompileCommand=quiet",

        /*
         * Critical for the experiment:
         *
         * C2 must not inline add().
         */
        "-XX:CompileCommand=dontinline,by.akozel.FrontEndLearn::add",

        /*
         * Keep benchmark methods as separate compilation units.
         *
         * These directives do NOT disable optimizations inside them.
         */
        "-XX:CompileCommand=dontinline,by.akozel.FrontEndLearn::branchBeforeComputation",
        "-XX:CompileCommand=dontinline,by.akozel.FrontEndLearn::computationBeforeBranch",
        "-XX:CompileCommand=dontinline,by.akozel.FrontEndLearn::inlineAddition",

        "-cp",
        classpath,

        FrontEndLearn.class.getName()
    )
        .inheritIO()
        .start();

    int exitCode = process.waitFor();

    if (exitCode != 0) {
      throw new IllegalStateException(
          "Child JVM exited with code " + exitCode
      );
    }
  }

  static String javaExecutable() {
    boolean windows = System.getProperty("os.name")
        .toLowerCase()
        .contains("win");

    return Path.of(
        System.getProperty("java.home"),
        "bin",
        windows ? "java.exe" : "java"
    ).toString();
  }
}