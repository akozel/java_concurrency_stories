package by.akozel;

import org.openjdk.jmh.annotations.*;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2)
@State(Scope.Benchmark)
public class OopAndCacheMissBench {

    @Param({"100000", "50000000", "100000000"})
    public int size;

    private static final String[] NAMES = {
            "Alice", "Bob", "Charlie", "Diana", "Eve",
            "Frank", "Grace", "Heidi", "Ivan", "Judy"
    };

    // AoS
    public static class ArrayOfStructs {
        String firstName;
        String lastName;
        int age;
        int salary;
    }

    // SoA
    public static class StructOfArrays {
        String[] firstName;
        String[] lastName;
        int[] age;
        int[] salary;
    }

    private ArrayOfStructs[] aos;
    private StructOfArrays soa;

    @Setup(Level.Trial)
    public void setup() {
        aos = createArrayOfStructs(size);
        soa = createStructOfArrays(size);
    }

    // Following AoS you just ignore CPU L1 cache optimizations. OOP - is a cache miss.
    // More fields you have -> less efficient app you get.
    // L1 memory is Fast but very Expensive. Basically you pay money for it :(
    @Benchmark
    public long aosBenchmark() {
        long sum = 0;
        for (ArrayOfStructs ao : aos) {
            if (ao.age > 18 && ao.age < 65) {
                sum += ao.salary;
            }
        }
        return sum / aos.length;
    }

    // Using SoA you are friendly to silicon optimizations, fast and cost-effective.
    // It can be used natively on distributed computing systems (like DDD applications, Event Sourcing, CQRS)
    // Also, Apache Arrow is perspective format that works in a similar way, and it is become popular :)
    @Benchmark
    public long soaBenchmark() {
        long sum = 0;
        int[] salary = soa.salary;
        int[] age = soa.age;
        for (int i = 0; i < salary.length; i++) {
            if (age[i] > 18 && age[i] < 65) {
                sum += salary[i];
            }
        }
        return sum / salary.length;
    }

    private static ArrayOfStructs[] createArrayOfStructs(int size) {
        var rnd = ThreadLocalRandom.current();
        ArrayOfStructs[] aos = new ArrayOfStructs[size];

        for (int i = 0; i < size; i++) {
            ArrayOfStructs rec = new ArrayOfStructs();
            rec.firstName = NAMES[rnd.nextInt(NAMES.length)];
            rec.lastName = NAMES[rnd.nextInt(NAMES.length)];
            rec.age = rnd.nextInt(100);
            rec.salary = rnd.nextInt(30_000, 200_000);
            aos[i] = rec;
        }
        return aos;
    }

    private static StructOfArrays createStructOfArrays(int size) {
        var rnd = ThreadLocalRandom.current();

        StructOfArrays soa = new StructOfArrays();
        soa.firstName = new String[size];
        soa.lastName = new String[size];
        soa.age = new int[size];
        soa.salary = new int[size];

        for (int i = 0; i < size; i++) {
            soa.firstName[i] = NAMES[rnd.nextInt(NAMES.length)];
            soa.lastName[i] = NAMES[rnd.nextInt(NAMES.length)];
            soa.age[i] = rnd.nextInt(100);
            soa.salary[i] = rnd.nextInt(30_000, 200_000);
        }
        return soa;
    }
}
