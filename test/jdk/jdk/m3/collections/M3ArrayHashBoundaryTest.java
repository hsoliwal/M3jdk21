/*
 * @test
 * @summary Exercise the real stable-scalar int[] index and collision-heavy keyed array offsets
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main M3ArrayHashBoundaryTest
 */
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import jdk.internal.m3.collections.SetUniqueListView;
import jdk.internal.m3.collections.IndexedCollectionView;

public class M3ArrayHashBoundaryTest {
    private static long checks;
    private static void test(boolean ok, String what) {
        checks++;
        if (!ok) throw new AssertionError(what);
    }
    private static String collision(int x) {
        StringBuilder s = new StringBuilder();
        for (int bit = 0; bit < 8; bit++) s.append((x & (1 << bit)) != 0 ? "Aa" : "BB");
        return s.toString();
    }
    private static void checkList(SetUniqueListView<Object> actual, List<Object> expected) {
        test(actual.size() == expected.size(), "unique size");
        test(Arrays.equals(actual.toArray(), expected.toArray()), "full list order");
        for (Object x : expected) test(actual.asSet().contains(x), "membership " + x);
    }
    private static void uniqueLargeIndex() throws ReflectiveOperationException {
        var actual = new SetUniqueListView<Object>();
        var expected = new ArrayList<Object>();
        for (int i = 0; i < 256; i++) {
            String value = collision(i);
            test(actual.add(value), "insert colliding string");
            expected.add(value);
        }
        for (int i = 0; i < 770; i++) {
            Object value = "unique-" + i;
            test(actual.add(value), "insert distinct string");
            expected.add(value);
        }
        test(actual.add(null), "insert null");
        expected.add(null);
        test(actual.add(Integer.valueOf(1234)), "insert Integer");
        expected.add(Integer.valueOf(1234));
        test(actual.add(Long.valueOf(1234L)), "insert distinct Long same hash");
        expected.add(Long.valueOf(1234L));
        checkList(actual, expected);
        Field lane = SetUniqueListView.class.getDeclaredField("hashIndexLane");
        lane.setAccessible(true);
        int[] indices = (int[]) lane.get(actual);
        test(indices.length >= 2 * actual.size(), "index must be materialized above threshold");
        test(!actual.add(collision(33)), "same-hash duplicate rejected");
        test(!actual.add(Integer.valueOf(1234)), "integer duplicate rejected");
        actual.add(100, "inserted");
        expected.add(100, "inserted");
        checkList(actual, expected);
        Object old = actual.set(700, collision(91));
        test(old == expected.set(700, collision(91)), "set previous reference");
        expected.remove(91);
        checkList(actual, expected);
        test(actual.remove(collision(180)), "remove colliding value");
        test(expected.remove(collision(180)), "reference remove");
        checkList(actual, expected);
    }
    private static final class BadKey {
        final int id;
        BadKey(int id) { this.id = id; }
        @Override public int hashCode() { return 17; }
        @Override public boolean equals(Object other) {
            return other instanceof BadKey key && id == key.id;
        }
    }
    private static final class Item {
        BadKey key;
        final int value;
        Item(int key, int value) { this.key = new BadKey(key); this.value = value; }
    }
    private static void indexedCollision() {
        var indexed = new IndexedCollectionView<BadKey, Item>(item -> item.key);
        var reference = new ArrayList<Item>();
        for (int i = 0; i < 500; i++) {
            Item row = new Item(i % 37, i);
            test(indexed.add(row), "add indexed collision");
            reference.add(row);
        }
        for (int id = 0; id < 40; id++) {
            BadKey key = new BadKey(id);
            Item first = null;
            var matches = new ArrayList<Item>();
            for (Item item : reference) if (Objects.equals(item.key, key)) {
                if (first == null) first = item;
                matches.add(item);
            }
            test(indexed.get(key) == first, "keyed lookup with collisions");
            Collection<Item> found = indexed.values(key);
            if (matches.isEmpty()) test(found == null, "absent key");
            else {
                test(found != null && found.size() == matches.size(), "matching values size");
                int n = 0;
                for (Item item : found) test(item == matches.get(n++), "matching identity");
            }
        }
        Item first = reference.get(0);
        test(indexed.remove(first), "remove earliest collision");
        reference.remove(first);
        test(indexed.get(new BadKey(0)) == reference.get(36), "first survivor with same key");
        Item changing = reference.get(50);
        changing.key = new BadKey(913);
        indexed.reindex();
        test(indexed.get(new BadKey(913)) == changing, "reindex after mutable key");
        indexed.clear();
        test(indexed.size() == 0 && indexed.get(new BadKey(913)) == null, "clear all arrays");
    }
    public static void main(String[] args) throws Exception {
        uniqueLargeIndex();
        indexedCollision();
        System.out.println("M3ArrayHashBoundaryTest PASS checks=" + checks);
    }
}
