/*
 * @test
 * @summary M3 indexed storage preserves insertion order, sorted index and invalidation
 * @modules java.base/jdk.internal.util
 * @run main/othervm M3IndexedStorageTest
 */
import jdk.internal.util.M3IndexedStorage;
import java.util.Arrays;
import java.util.Comparator;

public class M3IndexedStorageTest {
    private static void check(boolean ok) {
        if (!ok) throw new AssertionError();
    }
    public static void main(String[] args) {
        var storage = new M3IndexedStorage<String>(0);
        Comparator<String> comparator = Comparator.naturalOrder();
        storage.add("b");
        storage.add("a");
        storage.add("a");
        check(Arrays.equals(storage.sortedPositions(comparator), new int[] {1, 2, 0}));
        check(storage.get(0).equals("b"));
        int[] copy = storage.sortedPositions(comparator);
        copy[0] = 99;
        check(storage.sortedPositions(comparator)[0] == 1);
        storage.set(0, "0");
        check(Arrays.equals(storage.sortedPositions(comparator), new int[] {0, 1, 2}));
        storage.removeAt(1);
        check(Arrays.equals(storage.sortedPositions(comparator), new int[] {0, 1}));
        storage.discardDerivedIndexes();
        check(Arrays.equals(storage.sortedPositions(comparator), new int[] {0, 1}));
        storage.clear();
        check(storage.size() == 0);
        check(storage.sortedPositions(comparator).length == 0);
    }
}
