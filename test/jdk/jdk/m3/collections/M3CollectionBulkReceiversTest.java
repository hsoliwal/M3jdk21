/*
 * @test
 * @summary Verify original array-backed M3 collection receivers against independent donor contract models
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main M3CollectionBulkReceiversTest
 */
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import jdk.internal.m3.collections.DefaultedMapView;
import jdk.internal.m3.collections.SetUniqueListView;

public class M3CollectionBulkReceiversTest {
    private static long checks;
    private static void check(boolean yes, String label) {
        checks++;
        if (!yes) throw new AssertionError(label);
    }
    private static void equal(Object got, Object want, String label) {
        check(Objects.equals(got, want), label + ": " + got + " vs " + want);
    }
    private static Class<? extends Throwable> error(Runnable action) {
        try { action.run(); } catch (Throwable t) { return t.getClass(); }
        return null;
    }
    private static final class UniqueOracle {
        private final ArrayList<Object> values = new ArrayList<>();
        int indexOf(Object candidate) { return values.indexOf(candidate); }
        boolean add(Object element) {
            if (indexOf(element) >= 0) return false;
            return values.add(element);
        }
        void add(int index, Object element) {
            Objects.checkIndex(index, values.size() + 1);
            if (indexOf(element) < 0) values.add(index, element);
        }
        Object set(int index, Object element) {
            int prevIndex = indexOf(element);
            Object prior = values.set(index, element);
            if (prevIndex >= 0 && prevIndex != index) values.remove(prevIndex);
            return prior;
        }
        boolean remove(Object element) { return values.remove(element); }
    }
    private static void sameUnique(SetUniqueListView<Object> actual, UniqueOracle expected) {
        equal(actual.size(), expected.values.size(), "unique size");
        Set<Object> projection = actual.asSet();
        equal(projection.size(), expected.values.size(), "live set size");
        Object[] array = actual.toArray();
        equal(array.length, expected.values.size(), "array length");
        for (int i = 0; i < expected.values.size(); i++) {
            check(actual.get(i) == expected.values.get(i), "unique reference slot " + i);
            check(array[i] == expected.values.get(i), "snapshot slot " + i);
            check(projection.contains(expected.values.get(i)), "projection membership " + i);
        }
        check(!projection.contains(new Object()), "unknown member");
        equal(error(() -> projection.add("new")), UnsupportedOperationException.class, "unmodifiable set");
    }
    private static void uniqueCases() {
        SetUniqueListView<Object> list = new SetUniqueListView<>();
        UniqueOracle oracle = new UniqueOracle();
        Set<Object> live = list.asSet();
        sameUnique(list, oracle);
        check(list.add(null), "first null");
        check(oracle.add(null), "oracle first null");
        check(!list.add(null), "duplicate null");
        check(!oracle.add(null), "oracle duplicate null");
        sameUnique(list, oracle);
        Object first = "alpha";
        equal(list.add(first), oracle.add(first), "first insert");
        equal(list.add("alpha"), oracle.add("alpha"), "duplicate equals insert");
        check(live.contains(first), "live projection update");
        Object second = "beta";
        list.add(0, second); oracle.add(0, second);
        sameUnique(list, oracle);
        equal(list.set(2, second), oracle.set(2, second), "earlier duplicate displaced");
        sameUnique(list, oracle);
        equal(list.remove(null), oracle.remove(null), "remove null");
        sameUnique(list, oracle);
        final SetUniqueListView<Object> initial = list;
        equal(error(() -> initial.get(-1)), IndexOutOfBoundsException.class, "negative get");
        equal(error(() -> initial.add(-1, 3)), IndexOutOfBoundsException.class, "negative add");
        Iterator<Object> it = list.asSet().iterator();
        check(it.hasNext(), "iterator before mutation");
        list.add("inserted");
        equal(error(it::next), java.util.ConcurrentModificationException.class, "fail-fast projection");
        for (int seed = 0; seed < 24; seed++) {
            Random random = new Random(9017L + seed);
            list = new SetUniqueListView<>();
            oracle = new UniqueOracle();
            Object[] keys = {null, "a", "b", "c", Integer.valueOf(1), Integer.valueOf(2), Long.valueOf(1)};
            for (int step = 0; step < 350; step++) {
                Object key = keys[random.nextInt(keys.length)];
                int op = random.nextInt(4);
                if (op == 0) {
                    equal(list.add(key), oracle.add(key), "add change");
                } else if (op == 1) {
                    int index = random.nextInt(oracle.values.size() + 1);
                    list.add(index, key); oracle.add(index, key);
                } else if (op == 2 && !oracle.values.isEmpty()) {
                    int index = random.nextInt(oracle.values.size());
                    check(list.set(index, key) == oracle.set(index, key), "set old identity");
                } else if (op == 3) {
                    equal(list.remove(key), oracle.remove(key), "remove change");
                }
                sameUnique(list, oracle);
            }
        }
    }
    private static final class Key {
        final int id;
        Key(int id) { this.id = id; }
        @Override public int hashCode() { return id % 7; }
        @Override public boolean equals(Object x) { return x instanceof Key k && k.id == id; }
        @Override public String toString() { return "K" + id; }
    }
    private static void mapCases(boolean lazy) {
        for (int seed = 0; seed < 25; seed++) {
            Random r = new Random(seed + (lazy ? 727 : 827));
            DefaultedMapView<Object, Object> actual =
                    new DefaultedMapView<>(key -> "missing:" + key, lazy);
            Map<Object, Object> expected = new HashMap<>();
            Object[] keys = new Object[51];
            keys[0] = null;
            for (int i = 1; i < keys.length; i++) keys[i] = new Key(i);
            for (int step = 0; step < 600; step++) {
                Object key = keys[r.nextInt(keys.length)];
                int op = r.nextInt(7);
                if (op <= 1) {
                    Object v = r.nextInt(7) == 0 ? null : "value:" + r.nextInt(33);
                    equal(actual.put(key, v), expected.put(key, v), "map put previous");
                } else if (op == 2) {
                    equal(actual.remove(key), expected.remove(key), "map remove previous");
                } else if (op == 3) {
                    Object wanted = expected.containsKey(key) ? expected.get(key) : "missing:" + key;
                    if (lazy && !expected.containsKey(key)) expected.put(key, wanted);
                    equal(actual.get(key), wanted, "map default/lazy get");
                } else if (op == 4) {
                    actual.clear(); expected.clear();
                } else if (op == 5) {
                    Object previous = actual.put(key, null);
                    equal(previous, expected.put(key, null), "put explicit null");
                    equal(actual.get(key), null, "present null, no factory");
                } else {
                    equal(actual.containsKey(key), expected.containsKey(key), "contains key");
                }
                equal(actual.size(), expected.size(), "map size");
                equal(actual.isEmpty(), expected.isEmpty(), "map empty");
                for (Object inspect : keys) {
                    equal(actual.containsKey(inspect), expected.containsKey(inspect), "all keys after mutation");
                }
            }
        }
        DefaultedMapView<String, String> defaultValue = new DefaultedMapView<>("sentinel");
        equal(defaultValue.get("missing"), "sentinel", "constant default");
        equal(defaultValue.size(), 0, "constant default is not stored");
        defaultValue.put("key", null);
        equal(defaultValue.get("key"), null, "explicit null must be present");
        DefaultedMapView<String, String> empty = new DefaultedMapView<>();
        equal(empty.get("never"), null, "empty default");
        equal(empty.size(), 0, "empty default no mutation");
        DefaultedMapView<String, String> failing = new DefaultedMapView<>(
                key -> { throw new IllegalStateException("factory"); }, true);
        equal(error(() -> failing.get("exception")), IllegalStateException.class, "callback failure");
        equal(failing.size(), 0, "callback exception atomicity");
    }
    private static void storage() throws ReflectiveOperationException {
        for (Class<?> type : List.of(SetUniqueListView.class, DefaultedMapView.class)) {
            for (Field f : type.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                check(!java.util.Collection.class.isAssignableFrom(f.getType()), "no retained Collection: " + f);
                check(!java.util.Map.class.isAssignableFrom(f.getType()), "no retained Map: " + f);
            }
        }
        equal(SetUniqueListView.class.getDeclaredField("valueLane").getType(), Object[].class, "unique array");
        equal(DefaultedMapView.class.getDeclaredField("keyLane").getType(), Object[].class, "key lane");
        equal(DefaultedMapView.class.getDeclaredField("valueLane").getType(), Object[].class, "value lane");
        equal(DefaultedMapView.class.getDeclaredField("hashIndexLane").getType(), int[].class, "index lane");
    }
    public static void main(String[] args) throws ReflectiveOperationException {
        uniqueCases();
        mapCases(false);
        mapCases(true);
        storage();
        System.out.println("M3CollectionBulkReceiversTest PASS checks=" + checks);
    }
}
