/*
 * @test
 * @summary Array/mask storage, generation-safe handles, leases and backed views
 * @modules java.base/jdk.internal.m3
 * @run main/othervm M3IndexedArrayTest
 */

import jdk.internal.m3.M3IndexedArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public class M3IndexedArrayTest {
    record Item(int rank, int sequence) { }

    public static void main(String[] args) {
        verifySlotHandles();
        verifyViews();
        verifySortIsStable();
        verifyCallbackReentrancyFails();
        verifyRandomizedReferenceBehavior();
        System.out.println("M3IndexedArrayTest PASS");
    }

    private static void verifySlotHandles() {
        M3IndexedArray<Integer> values = new M3IndexedArray<>();
        long first = values.append(10);
        long second = values.append(null);
        require(values.size() == 2, "append size");
        require(values.at(0) == 10 && values.at(1) == null, "array null and index");
        require(values.handleAt(1) == second, "handle from order");
        require(values.stateMask(first) == M3IndexedArray.LIVE, "initial live bit");
        values.retain(first);
        values.retain(first);
        require(values.ownerCount(first) == 2, "explicit leases");
        require((values.stateMask(first) & M3IndexedArray.SHARED) != 0, "shared bit");
        require(!values.remove(first), "active lease blocks reuse");
        require(!values.replace(first, 20), "active lease blocks replace");
        values.release(first);
        require(values.ownerCount(first) == 1, "lease decrement");
        require((values.stateMask(first) & M3IndexedArray.SHARED) == 0, "shared bit clear");
        values.release(first);
        expect(IllegalStateException.class, () -> values.release(first));
        require(values.remove(first), "remove after release");
        expect(IllegalArgumentException.class, () -> values.get(first));
        long third = values.append(99);
        require((int) third == (int) first, "free slot reused");
        require(third != first, "generation changed");
        require(values.get(third) == 99 && values.at(0) == null, "order retained");
        expect(IllegalArgumentException.class, () -> values.retain(first));
        expect(IndexOutOfBoundsException.class, () -> values.at(2));
        for (int i = 0; i < 5000; i++) {
            values.append(i);
        }
        require(values.size() == 5002, "growth");
    }

    private static void verifyViews() {
        M3IndexedArray<Integer> store = new M3IndexedArray<>();
        List<Integer> selected = store.filtered(n -> n != null && n % 2 == 0);
        List<Integer> sorted = store.sorted(Comparator.nullsLast(Integer::compareTo));
        List<Integer> filteredSorted = store.filteredSorted(
                n -> n != null && n % 2 == 0, Integer::compareTo);
        require(selected.isEmpty() && sorted.isEmpty(), "empty live views");
        long first = store.append(4);
        store.append(1);
        store.append(2);
        store.append(null);
        store.append(4);
        require(selected.equals(List.of(4, 2, 4)), "filtered view");
        require(filteredSorted.equals(List.of(2, 4, 4)), "filtered sorted view");
        require(sorted.equals(Arrays.asList(1, 2, 4, 4, null)), "sorted with null");
        Iterator<Integer> it = selected.iterator();
        require(it.next() == 4, "iterator first element");
        store.append(8);
        expect(ConcurrentModificationException.class, it::hasNext);
        require(selected.equals(List.of(4, 2, 4, 8)), "live view after append");
        require(store.replace(first, 6), "replace unleased");
        require(selected.equals(List.of(6, 2, 4, 8)), "live view after replace");
        require(filteredSorted.equals(List.of(2, 4, 6, 8)), "re-sorting after replace");
        expect(UnsupportedOperationException.class, () -> selected.add(100));
        expect(UnsupportedOperationException.class, () -> sorted.remove(0));
        require(store.remove(first), "remove");
        require(selected.equals(List.of(2, 4, 8)), "live view after removal");
    }

    private static void verifySortIsStable() {
        M3IndexedArray<Item> store = new M3IndexedArray<>();
        for (int i = 0; i < 2000; i++) {
            store.append(new Item(i % 11, i));
        }
        List<Item> view = store.sorted(Comparator.comparingInt(Item::rank));
        int lastRank = -1;
        int[] lastSequenceForRank = new int[11];
        Arrays.fill(lastSequenceForRank, -1);
        for (Item item : view) {
            require(item.rank() >= lastRank, "monotonic sorted rank");
            require(item.sequence() > lastSequenceForRank[item.rank()], "stable equal keys");
            lastRank = item.rank();
            lastSequenceForRank[item.rank()] = item.sequence();
        }
    }

    private static void verifyCallbackReentrancyFails() {
        M3IndexedArray<Integer> store = new M3IndexedArray<>();
        store.append(3);
        List<Integer> invalid = store.filtered(n -> {
            store.append(99);
            return true;
        });
        expect(ConcurrentModificationException.class, invalid::size);
        M3IndexedArray<Integer> other = new M3IndexedArray<>();
        other.append(2);
        other.append(1);
        List<Integer> invalidComparator = other.sorted((left, right) -> {
            other.append(0);
            return left.compareTo(right);
        });
        expect(ConcurrentModificationException.class, invalidComparator::size);
    }

    private static void verifyRandomizedReferenceBehavior() {
        final Random random = new Random(0x12345678L);
        final M3IndexedArray<Integer> store = new M3IndexedArray<>();
        final ArrayList<Integer> reference = new ArrayList<>();
        final ArrayList<Long> handles = new ArrayList<>();
        final Predicate<Integer> even = value -> value != null && (value & 1) == 0;
        final List<Integer> filtered = store.filtered(even);
        final List<Integer> sorted = store.filteredSorted(even, Comparator.naturalOrder());
        for (int iteration = 0; iteration < 18000; iteration++) {
            final int operation = reference.isEmpty() ? 0 : random.nextInt(3);
            if (operation == 0) {
                final int value = random.nextInt(201) - 100;
                handles.add(store.append(value));
                reference.add(value);
            } else if (operation == 1) {
                final int index = random.nextInt(reference.size());
                require(store.remove(handles.remove(index)), "fuzz remove");
                reference.remove(index);
            } else {
                final int index = random.nextInt(reference.size());
                final int value = random.nextInt(201) - 100;
                require(store.replace(handles.get(index), value), "fuzz replace");
                reference.set(index, value);
            }
            require(store.size() == reference.size(), "fuzz logical size");
            if (iteration % 73 == 0) {
                for (int index = 0; index < reference.size(); index++) {
                    require(reference.get(index).equals(store.at(index)), "fuzz logical order");
                    require(store.handleAt(index) == handles.get(index), "fuzz slot stability");
                }
                final List<Integer> expected = reference.stream().filter(even).toList();
                final List<Integer> expectedSorted = expected.stream().sorted().toList();
                require(filtered.equals(expected), "fuzz filter correctness");
                require(sorted.equals(expectedSorted), "fuzz sort correctness");
            }
        }
    }

    private static void expect(Class<? extends Throwable> type, Runnable runnable) {
        try {
            runnable.run();
            throw new AssertionError("Expected " + type.getSimpleName());
        } catch (Throwable exception) {
            if (!type.isInstance(exception)) {
                throw new AssertionError("Expected " + type.getSimpleName()
                        + " but got " + exception, exception);
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
