/*
 * @test
 * @summary M3JDK21 pipeline packed bit-mask and index traversal equals sequential Stream
 * @modules java.base/jdk.internal.util
 * @run main/othervm M3PipelinePlanDifferentialTest
 */
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import jdk.internal.util.M3PipelinePlan;

public class M3PipelinePlanDifferentialTest {
    public static void main(String[] args) {
        Random random = new Random(0x4d334a444b32314cL);
        int checks = 0;
        for (int seed = 0; seed < 500; seed++) {
            int size = random.nextInt(30);
            List<Integer> input = new ArrayList<>();
            for (int i = 0; i < size; i++) input.add(random.nextInt(8) == 0 ? null : random.nextInt(101) - 50);
            M3PipelinePlan<Integer> plan = M3PipelinePlan.from(input);
            Stream<Integer> oracle = input.stream();
            int operations = random.nextInt(151);
            for (int stage = 0; stage < operations; stage++) {
                if (random.nextBoolean()) {
                    int shift = random.nextInt(7) - 3;
                    Function<Integer,Integer> function = n -> n == null ? shift : n + shift;
                    plan = plan.map(function);
                    oracle = oracle.map(function);
                } else {
                    int mod = random.nextInt(8) + 2;
                    Predicate<Integer> predicate = n -> n == null || Math.floorMod(n, mod) != 0;
                    plan = plan.filter(predicate);
                    oracle = oracle.filter(predicate);
                }
            }
            List<Integer> expected = oracle.toList();
            List<Integer> actual = plan.toList();
            if (!actual.equals(expected)) {
                throw new AssertionError("seed=" + seed + " stages=" + operations
                        + " expected=" + expected + " actual=" + actual);
            }
            checks++;
            List<Integer> sorted = new ArrayList<>(expected);
            sorted.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
            if (!plan.sortedToList(Comparator.nullsFirst(Comparator.naturalOrder())).equals(sorted)) {
                throw new AssertionError("sorted seed=" + seed);
            }
            checks++;
            List<Integer> distinct = expected.stream().distinct().toList();
            if (!plan.distinctToList().equals(distinct)) throw new AssertionError("distinct seed=" + seed);
            checks++;
        }
        System.out.println("M3PipelinePlanDifferentialTest PASSED checks=" + checks);
    }
}
