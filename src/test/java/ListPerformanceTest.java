import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ListPerformanceTest {

  private static final int SIZE = 5_000_000;
  private static final int WORD_LENGTH = 10;
  private static final int RUNS = 5;

  private static final String[] WORDS = createWords();

  @Test
  void test() {

    System.out.println("Elements:    " + SIZE);
    System.out.println("Word length: " + WORD_LENGTH);
    System.out.println();

    // JIT warmup
    for (int i = 0; i < 2; i++) {
      createArrayList();
      createPreallocatedArrayList();
      createLinkedList();
    }

    System.out.println("---- insertion ----");

    for (int run = 1; run <= RUNS; run++) {

      long arrayList = measure(ListPerformanceTest::createArrayList);
      long preallocated = measure(ListPerformanceTest::createPreallocatedArrayList);
      long linkedList = measure(ListPerformanceTest::createLinkedList);

      System.out.printf(
          """
          run %d:
            ArrayList             = %8.2f ms
            ArrayList(preallocated) = %8.2f ms
            LinkedList            = %8.2f ms
          %n""",
          run,
          arrayList / 1_000_000.0,
          preallocated / 1_000_000.0,
          linkedList / 1_000_000.0
      );
    }

    System.out.println("---- iteration ----");

    List<String> arrayList = createPreallocatedArrayList();
    List<String> linkedList = createLinkedList();

    for (int run = 1; run <= RUNS; run++) {

      long arrayTime = measureIteration(arrayList);
      long linkedTime = measureIteration(linkedList);

      System.out.printf(
          """
          run %d:
            ArrayList  = %8.2f ms
            LinkedList = %8.2f ms
          %n""",
          run,
          arrayTime / 1_000_000.0,
          linkedTime / 1_000_000.0
      );
    }
  }

  private static ArrayList<String> createArrayList() {
    ArrayList<String> result = new ArrayList<>();

    for (String word : WORDS) {
      result.add(word);
    }

    return result;
  }

  private static ArrayList<String> createPreallocatedArrayList() {
    ArrayList<String> result = new ArrayList<>(SIZE);

    for (String word : WORDS) {
      result.add(word);
    }

    return result;
  }

  private static LinkedList<String> createLinkedList() {
    LinkedList<String> result = new LinkedList<>();

    for (String word : WORDS) {
      result.add(word);
    }

    return result;
  }

  private static long measure(ListFactory factory) {
    long started = System.nanoTime();

    List<String> result = factory.create();

    long elapsed = System.nanoTime() - started;

    if (result.size() != SIZE) {
      throw new AssertionError();
    }

    return elapsed;
  }

  private static long measureIteration(List<String> list) {
    long started = System.nanoTime();

    long totalLength = 0;

    for (String word : list) {
      totalLength += word.length();
    }

    long elapsed = System.nanoTime() - started;

    if (totalLength != (long) SIZE * WORD_LENGTH) {
      throw new AssertionError();
    }

    return elapsed;
  }

  private static String[] createWords() {
    String[] result = new String[SIZE];

    for (int i = 0; i < SIZE; i++) {
      char[] chars = new char[WORD_LENGTH];
      Arrays.fill(chars, 'a');

      int value = i;

      for (int position = WORD_LENGTH - 1; position >= 0 && value > 0; position--) {
        chars[position] = (char) ('0' + value % 10);
        value /= 10;
      }

      result[i] = new String(chars);
    }

    return result;
  }

  @FunctionalInterface
  interface ListFactory {
    List<String> create();
  }
}