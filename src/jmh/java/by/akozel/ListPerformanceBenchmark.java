package by.akozel;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(3)
public class ListPerformanceBenchmark {

  // --------------------------------------------------
  // INSERT STATE
  // --------------------------------------------------

  @State(Scope.Benchmark)
  public static class InsertionState {

    @Param({"1000000", "5000000"})
    int size;

    @Param("20")
    int wordLength;

    String[] words;

    @Setup
    public void setup() {
      words = createWords(size, wordLength);
    }
  }

  // --------------------------------------------------
  // ITERATION STATES
  // --------------------------------------------------

  @State(Scope.Benchmark)
  public static class ArrayListState {

    @Param({"1000000", "5000000"})
    int size;

    @Param("20")
    int wordLength;

    List<String> list;

    @Setup
    public void setup() {
      String[] words = createWords(size, wordLength);

      list = new ArrayList<>(size);

      for (String word : words) {
        list.add(word);
      }
    }
  }

  @State(Scope.Benchmark)
  public static class LinkedListState {

    @Param({"1000000", "5000000"})
    int size;

    @Param("20")
    int wordLength;

    List<String> list;

    @Setup
    public void setup() {
      String[] words = createWords(size, wordLength);

      list = new LinkedList<>();

      for (String word : words) {
        list.add(word);
      }
    }
  }

  // --------------------------------------------------
  // INSERT
  //
  // One complete list construction = one measurement.
  // --------------------------------------------------

  @Benchmark
  @BenchmarkMode(Mode.SingleShotTime)
  @Warmup(iterations = 5)
  @Measurement(iterations = 10)
  public List<String> arrayList(InsertionState state) {
    List<String> list = new ArrayList<>();

    for (String word : state.words) {
      list.add(word);
    }

    return list;
  }

  @Benchmark
  @BenchmarkMode(Mode.SingleShotTime)
  @Warmup(iterations = 5)
  @Measurement(iterations = 10)
  public List<String> arrayListPreallocated(InsertionState state) {
    List<String> list = new ArrayList<>(state.size);

    for (String word : state.words) {
      list.add(word);
    }

    return list;
  }

  @Benchmark
  @BenchmarkMode(Mode.SingleShotTime)
  @Warmup(iterations = 5)
  @Measurement(iterations = 10)
  public List<String> linkedList(InsertionState state) {
    List<String> list = new LinkedList<>();

    for (String word : state.words) {
      list.add(word);
    }

    return list;
  }

  // --------------------------------------------------
  // ITERATION
  // --------------------------------------------------

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @Warmup(
      iterations = 3,
      time = 1,
      timeUnit = TimeUnit.SECONDS
  )
  @Measurement(
      iterations = 5,
      time = 1,
      timeUnit = TimeUnit.SECONDS
  )
  public long iterateArrayList(ArrayListState state) {
    long result = 0;

    for (String word : state.list) {
      result += word.length();
    }

    return result;
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @Warmup(
      iterations = 3,
      time = 1,
      timeUnit = TimeUnit.SECONDS
  )
  @Measurement(
      iterations = 5,
      time = 1,
      timeUnit = TimeUnit.SECONDS
  )
  public long iterateLinkedList(LinkedListState state) {
    long result = 0;

    for (String word : state.list) {
      result += word.length();
    }

    return result;
  }

  // --------------------------------------------------
  // DATA GENERATION
  // --------------------------------------------------

  private static String[] createWords(
      int size,
      int wordLength
  ) {
    String[] words = new String[size];

    for (int i = 0; i < size; i++) {
      words[i] = createWord(i, wordLength);
    }

    return words;
  }

  private static String createWord(
      int index,
      int length
  ) {
    char[] chars = new char[length];

    // Fill with letters first.
    for (int i = 0; i < length; i++) {
      chars[i] = 'x';
    }

    // Encode the element index into the end of the String.
    // This makes every String value different while
    // preserving exactly the requested length.
    int value = index;

    for (int i = length - 1; i >= 0 && value > 0; i--) {
      chars[i] = (char) ('0' + value % 10);
      value /= 10;
    }

    return new String(chars);
  }
}