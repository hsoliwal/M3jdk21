/*
 * @test
 * @summary M3 fused pipeline preserves sequential stream results and invocation order
 * @modules java.base/jdk.internal.util
 * @run main/othervm M3PipelinePlanTest
 */
import jdk.internal.util.M3PipelinePlan;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class M3PipelinePlanTest {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        List<String> input = Arrays.asList(" A ", null, "a", "B", "b", " B ");
        AtomicInteger calls = new AtomicInteger();
        List<String> actual = M3PipelinePlan.from(input)
                .filter(s -> { calls.incrementAndGet(); return s != null; })
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .distinctToList();
        List<String> expected = input.stream().filter(s -> s != null)
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(String::toLowerCase).distinct().toList();
        check(actual.equals(expected), "distinct parity");
        check(calls.get() == input.size(), "predicate calls");
        List<String> sorted = M3PipelinePlan.from(input)
                .filter(s -> s != null).map(String::trim)
                .sortedToList(Comparator.naturalOrder());
        check(sorted.equals(input.stream().filter(s -> s != null)
                .map(String::trim).sorted().toList()), "sorted parity");
        List<String> empty = M3PipelinePlan.<String>from(List.of())
                .map(String::trim).toList();
        check(empty.isEmpty(), "empty input");
        List<Integer> mapped = M3PipelinePlan.from(Arrays.asList("1", null, "2"))
                .map(s -> s == null ? 0 : Integer.parseInt(s)).toList();
        check(mapped.equals(Arrays.asList(1, 0, 2)), "null mapping");
        ArrayList<String> log = new ArrayList<>();
        M3PipelinePlan.from(List.of("a", "b"))
                .map(s -> { log.add("map:" + s); return s; })
                .filter(s -> { log.add("filter:" + s); return true; })
                .toList();
        check(log.equals(List.of("map:a", "filter:a", "map:b", "filter:b")),
                "stage order");
    }
}
