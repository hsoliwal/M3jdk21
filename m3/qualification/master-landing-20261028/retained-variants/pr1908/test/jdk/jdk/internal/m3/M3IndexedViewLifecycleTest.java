// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary Lazy indexed views preserve first-use, failure, range and split contracts
 * @modules java.base/jdk.internal.m3
 * @run main/othervm M3IndexedViewLifecycleTest
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import jdk.internal.m3.M3IndexedArray;

public final class M3IndexedViewLifecycleTest {
    private static int checks;
    private static final Comparator<Integer> ORDER = Comparator.nullsFirst(Integer::compare);
    private static void check(boolean ok, String why) {
        checks++;
        if (!ok) throw new AssertionError(why);
    }
    private static void expect(Class<? extends Throwable> type, Runnable task) {
        try { task.run(); throw new AssertionError("missing " + type.getName()); }
        catch (Throwable error) { check(type.isInstance(error), "exception: " + error); }
    }
    private static List<Integer> create(M3IndexedArray<Integer> owner, int kind,
                                         AtomicInteger calls) {
        Predicate<Integer> odd = x -> { calls.incrementAndGet(); return x == null || (x & 1) != 0; };
        return switch (kind) {
            case 0 -> owner.filtered(odd);
            case 1 -> owner.sorted(ORDER);
            case 2 -> owner.filteredSorted(odd, ORDER);
            case 3 -> owner.maskedStates(M3IndexedArray.LEASED, 0);
            case 4 -> owner.maskedSortedStates(M3IndexedArray.LEASED, 0, ORDER);
            case 5 -> owner.filteredMaskedStates(odd, M3IndexedArray.LEASED, 0);
            case 6 -> owner.filteredMaskedSortedStates(odd, M3IndexedArray.LEASED, 0, ORDER);
            case 7 -> owner.maskedStates(M3IndexedArray.RETIRED, 0);
            default -> throw new AssertionError(kind);
        };
    }
    private static List<Integer> want(int kind) {
        return switch (kind) {
            case 0 -> Arrays.asList(3, null, 1);
            case 1 -> Arrays.asList(null, 1, 2, 3);
            case 2 -> Arrays.asList(null, 1, 3);
            case 3, 5 -> List.of(3, 1);
            case 4, 6 -> List.of(1, 3);
            case 7 -> List.of();
            default -> throw new AssertionError(kind);
        };
    }
    private static void firstReaders() {
        for (int kind = 0; kind < 8; kind++) for (int reader = 0; reader < 8; reader++) {
            M3IndexedArray<Integer> owner = new M3IndexedArray<>();
            for (Integer value : Arrays.asList(3, null, 1, 2)) owner.append(value);
            owner.retain(owner.handleAt(0)); owner.retain(owner.handleAt(2));
            AtomicInteger calls = new AtomicInteger();
            List<Integer> view = create(owner, kind, calls), expected = want(kind);
            Spliterator<Integer> unbound = view.spliterator();
            check(unbound.hasCharacteristics(Spliterator.SIZED), "metadata");
            check(calls.get() == 0, "construction and metadata are lazy");
            List<Integer> actual = new ArrayList<>();
            switch (reader) {
                case 0 -> { check(view.size() == expected.size(), "first size"); actual.addAll(view); }
                case 1 -> { if (expected.isEmpty()) expect(IndexOutOfBoundsException.class, () -> view.get(0));
                            else check(view.get(0) == expected.get(0), "first get identity"); actual.addAll(view); }
                case 2 -> actual.addAll(view.subList(0, expected.size()));
                case 3 -> { ListIterator<Integer> it = view.listIterator(expected.size());
                            while (it.hasPrevious()) actual.add(0, it.previous()); }
                case 4 -> { check(unbound.estimateSize() == expected.size(), "first estimate");
                            unbound.forEachRemaining(actual::add); }
                case 5 -> { Spliterator<Integer> prefix = unbound.trySplit();
                            if (prefix != null) prefix.forEachRemaining(actual::add);
                            unbound.forEachRemaining(actual::add); }
                case 6 -> { while (unbound.tryAdvance(actual::add)) { } }
                case 7 -> actual.addAll(view.parallelStream().toList());
                default -> throw new AssertionError(reader);
            }
            check(actual.equals(expected), "first reader " + reader + " kind " + kind);
            int observed = calls.get(); view.size();
            check(calls.get() == observed, "no repeated selection");
            List<Integer> range = view.subList(0, view.size());
            var iterator = view.iterator(); Spliterator<Integer> split = range.spliterator();
            check(owner.replace(owner.handleAt(3), 4), "replace unleased value");
            expect(ConcurrentModificationException.class, range::size);
            expect(ConcurrentModificationException.class, iterator::hasNext);
            expect(ConcurrentModificationException.class, split::estimateSize);
            check(view.size() == expected.size(), "refresh after mutation");
        }
    }
    private static void failureRecovery() {
        M3IndexedArray<Integer> owner = new M3IndexedArray<>();
        owner.append(3); owner.append(1); owner.append(2);
        RuntimeException sentinel = new RuntimeException("first selection");
        AtomicBoolean fail = new AtomicBoolean(true);
        List<Integer> view = owner.filtered(x -> { if (fail.get()) throw sentinel; return x < 3; });
        var split = view.spliterator();
        expect(NullPointerException.class, () -> split.tryAdvance(null));
        try { split.estimateSize(); throw new AssertionError("missing failure"); }
        catch (RuntimeException error) { check(error == sentinel, "predicate exception identity"); }
        fail.set(false);
        check(split.estimateSize() == 2, "same unbound cursor retries");
        check(view.equals(List.of(1, 2)), "predicate recovery");
        fail.set(true);
        List<Integer> sorted = owner.sorted((a, b) -> { if (fail.get()) throw sentinel; return Integer.compare(a, b); });
        try { sorted.subList(0, 1); throw new AssertionError("missing sort failure"); }
        catch (RuntimeException error) { check(error == sentinel, "comparator exception identity"); }
        fail.set(false); check(sorted.equals(List.of(1, 2, 3)), "sort retry");
        AtomicBoolean mutate = new AtomicBoolean(true);
        List<Integer> reentrant = owner.filtered(x -> { if (mutate.getAndSet(false)) owner.append(4); return true; });
        expect(ConcurrentModificationException.class, reentrant::size);
        check(reentrant.equals(List.of(3, 1, 2, 4)), "reentrant first-use retry");
        M3IndexedArray<Integer> empty = new M3IndexedArray<>();
        List<Integer> later = empty.filtered(x -> true);
        check(later.isEmpty(), "empty initial directory");
        empty.append(null); check(later.size() == 1 && later.get(0) == null, "empty to null value");
        empty.remove(empty.handleAt(0)); check(later.isEmpty(), "back to empty");
    }
    private static void concurrentFirstUse() throws Exception {
        M3IndexedArray<Integer> owner = new M3IndexedArray<>();
        for (int i = 0; i < 64; i++) owner.append(i);
        AtomicInteger calls = new AtomicInteger();
        List<Integer> view = owner.filtered(x -> { calls.incrementAndGet(); return (x & 1) == 0; });
        try (var workers = Executors.newFixedThreadPool(2)) {
            var one = workers.submit(view::size); var two = workers.submit(() -> view.get(31));
            check(one.get() == 32 && two.get() == 62, "concurrent publication");
        }
        check(calls.get() == 64, "one successful selection");
    }
    public static void main(String[] args) throws Exception {
        firstReaders(); failureRecovery(); concurrentFirstUse();
        System.out.println("M3IndexedViewLifecycleTest PASS checks=" + checks);
    }
}
