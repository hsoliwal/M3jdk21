/*
 * @test
 * @summary M3JDK21 array/mask pipeline matches sequential semantics and handles deep stage chains
 * @modules java.base/jdk.internal.util
 * @run main/othervm/timeout=120 -Xss256k M3PipelinePlanIterativeTest
 */
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import jdk.internal.util.M3PipelinePlan;

public class M3PipelinePlanIterativeTest {
    private static int checks;
    private static void check(boolean yes, String name) {
        checks++;
        if (!yes) throw new AssertionError(name);
    }
    private static void equal(Object expected, Object actual, String name) {
        check(Objects.equals(expected, actual), name + ": expected=" + expected + ", got=" + actual);
    }

    public static void main(String[] args) {
        List<String> original = new ArrayList<>(Arrays.asList(" A ", null, "b", "b", " B ", ""));
        List<String> expected = original.stream().filter(Objects::nonNull)
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(String::toLowerCase).distinct().toList();
        var root = M3PipelinePlan.from(original);
        var candidate = root.filter(Objects::nonNull).map(String::trim)
                .filter(s -> !s.isEmpty()).map(String::toLowerCase);
        equal(expected, candidate.distinctToList(), "filtered distinct");
        equal(new ArrayList<>(original), root.toList(), "parent remains independent");
        equal(original.stream().filter(Objects::nonNull).map(String::trim)
                .sorted().toList(), root.filter(Objects::nonNull)
                .map(String::trim).sortedToList(Comparator.naturalOrder()), "sorted parity");
        equal(List.of(), M3PipelinePlan.from(List.<String>of())
                .map(String::trim).toList(), "empty source");

        // Each 64th boundary must retain the filter/map distinction.
        AtomicInteger filters = new AtomicInteger();
        M3PipelinePlan<Integer> maskPlan = M3PipelinePlan.from(List.of(1, 2, 3));
        for (int i = 0; i < 193; i++) {
            if ((i & 1) == 0) maskPlan = maskPlan.filter(n -> {
                filters.incrementAndGet();
                return n >= 0;
            });
            else maskPlan = maskPlan.map(n -> n + 1);
        }
        equal(List.of(97, 98, 99), maskPlan.toList(), "mask boundaries");
        equal(3 * 97, filters.get(), "every filter called once per accepted row");

        List<String> trace = new ArrayList<>();
        var shortCircuit = M3PipelinePlan.from(Arrays.asList(null, "ok", "no"))
                .map(s -> { trace.add("a:" + s); return s; })
                .filter(s -> { trace.add("b:" + s); return "ok".equals(s); })
                .map(s -> { trace.add("c:" + s); return s.toUpperCase(Locale.ROOT); });
        equal(List.of("OK"), shortCircuit.toList(), "short circuit values");
        equal(List.of("a:null", "b:null", "a:ok", "b:ok", "c:ok", "a:no", "b:no"),
                trace, "short circuit callback order");

        RuntimeException marker = new RuntimeException("marker");
        AtomicInteger after = new AtomicInteger();
        try {
            M3PipelinePlan.from(List.of("x"))
                    .map(s -> { throw marker; })
                    .map(s -> { after.incrementAndGet(); return s; }).toList();
            throw new AssertionError("missing callback exception");
        } catch (RuntimeException ex) {
            check(ex == marker, "exception identity");
            equal(0, after.get(), "later callback not invoked");
        }

        // Typed maps can return null; no internal sentinel is used as a value.
        List<Integer> ints = M3PipelinePlan.from(Arrays.asList("1", null, "2"))
                .map(s -> s == null ? null : Integer.parseInt(s)).toList();
        equal(Arrays.asList(1, null, 2), ints, "null mapper result");
        equal(Arrays.asList(1, null, 2), M3PipelinePlan.from(ints).toList(), "null source");

        // The old nested-sink design overflows a 256KB stack at this depth.
        M3PipelinePlan<Integer> deep = M3PipelinePlan.from(List.of(7));
        for (int i = 0; i < 4096; i++) deep = deep.map(n -> n + 1);
        equal(List.of(4103), deep.toList(), "4096 nested operations stack safe");
        original.add("c");
        equal(original.stream().filter(Objects::nonNull).map(String::trim)
                .filter(s -> !s.isEmpty()).map(String::toLowerCase).toList(),
                candidate.toList(), "source remains a live iterable");
        System.out.println("M3PipelinePlanIterativeTest PASSED checks=" + checks);
    }
}
