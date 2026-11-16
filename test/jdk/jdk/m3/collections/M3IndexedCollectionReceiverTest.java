/*
 * @test
 * @summary Verify array-backed keyed collection owner and secondary int[] index with duplicate and unique keys
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main M3IndexedCollectionReceiverTest
 */
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import jdk.internal.m3.collections.IndexedCollectionView;

public class M3IndexedCollectionReceiverTest {
    private static long assertions;
    private static void check(boolean yes, String what) {
        assertions++;
        if (!yes) throw new AssertionError(what);
    }
    private static void same(Object got, Object want, String what) {
        check(Objects.equals(got, want), what + ": " + got + " vs " + want);
    }
    private static final class Row {
        String key;
        final int ordinal;
        Row(String key, int ordinal) { this.key = key; this.ordinal = ordinal; }
    }
    private static final class IndexedOracle {
        final ArrayList<Row> rows = new ArrayList<>();
        final ArrayList<String> cachedKeys = new ArrayList<>();
        void add(Row row) { rows.add(row); cachedKeys.add(row.key); }
        boolean remove(Row row) {
            int pos = rows.indexOf(row);
            if (pos < 0) return false;
            rows.remove(pos);
            cachedKeys.remove(pos);
            return true;
        }
        void reindex() {
            for (int i = 0; i < rows.size(); i++) cachedKeys.set(i, rows.get(i).key);
        }
        Row get(String key) {
            for (int i = 0; i < rows.size(); i++) {
                if (Objects.equals(key, cachedKeys.get(i))) return rows.get(i);
            }
            return null;
        }
        List<Row> values(String key) {
            ArrayList<Row> matching = new ArrayList<>();
            for (int i = 0; i < rows.size(); i++) {
                if (Objects.equals(key, cachedKeys.get(i))) matching.add(rows.get(i));
            }
            return matching.isEmpty() ? null : matching;
        }
    }
    private static void verify(IndexedCollectionView<String, Row> actual, IndexedOracle expected) {
        same(actual.size(), expected.rows.size(), "index size");
        for (String key : new String[] {null, "k0", "k1", "k2", "k3", "k4", "absent"}) {
            check(actual.get(key) == expected.get(key), "get first by key " + key);
            Collection<Row> v = actual.values(key);
            List<Row> w = expected.values(key);
            if (w == null) {
                check(v == null, "values absent: " + key);
            } else {
                check(v != null, "values present: " + key);
                same(v.size(), w.size(), "value count: " + key);
                int i = 0;
                for (Row r : v) check(r == w.get(i++), "values identity " + key + " " + i);
            }
        }
        Row query = new Row("k2", -1);
        check(actual.contains(query) == (expected.get("k2") != null), "key-derived contains");
    }
    private static void randomized() {
        for (int seed = 0; seed < 30; seed++) {
            Random rng = new Random(seed * 1777L + 991L);
            IndexedCollectionView<String, Row> actual = new IndexedCollectionView<>(row -> row.key);
            IndexedOracle oracle = new IndexedOracle();
            for (int step = 0; step < 450; step++) {
                int op = rng.nextInt(5);
                if (op < 2 || oracle.rows.isEmpty()) {
                    String key = rng.nextInt(8) == 0 ? null : "k" + rng.nextInt(5);
                    Row item = new Row(key, step);
                    check(actual.add(item), "add returns true");
                    oracle.add(item);
                } else if (op == 2) {
                    int i = rng.nextInt(oracle.rows.size());
                    Row item = oracle.rows.get(i);
                    check(actual.remove(item) == oracle.remove(item), "remove existing row");
                } else if (op == 3) {
                    Row item = oracle.rows.get(rng.nextInt(oracle.rows.size()));
                    item.key = rng.nextBoolean() ? null : "k" + rng.nextInt(5);
                    // A changed mutable key is not in the index until reindex().
                    if (rng.nextInt(3) == 0) {
                        actual.reindex();
                        oracle.reindex();
                    }
                } else {
                    actual.reindex();
                    oracle.reindex();
                }
                verify(actual, oracle);
                if (step % 149 == 0) {
                    actual.clear();
                    oracle.rows.clear();
                    oracle.cachedKeys.clear();
                    verify(actual, oracle);
                }
            }
        }
    }
    private static void uniquenessAndRefusal() {
        IndexedCollectionView<String, Row> unique = new IndexedCollectionView<>(r -> r.key, true);
        Row a = new Row("A", 1), b = new Row("B", 2);
        check(unique.add(a), "unique first");
        check(unique.add(b), "unique second");
        boolean duplicate = false;
        try { unique.add(new Row("A", 3)); }
        catch (IllegalArgumentException ex) { duplicate = true; }
        check(duplicate, "reject duplicate keys");
        same(unique.size(), 2, "no partial duplicate insert");
        b.key = "A";
        boolean invalid = false;
        try { unique.reindex(); } catch (IllegalArgumentException ex) { invalid = true; }
        check(invalid, "reject external mutation creating duplicate key");
        check(unique.get("B") == b, "failed reindex cannot corrupt previous index");
        b.key = "B";
        unique.reindex();
        check(unique.get("B") == b, "reindex refresh");
        check(unique.remove(a), "remove existing");
        check(unique.get("A") == null, "remove clears key");
    }
    private static void storage() throws ReflectiveOperationException {
        for (Field field : IndexedCollectionView.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            check(!java.util.Collection.class.isAssignableFrom(field.getType()), "no Collection field");
            check(!java.util.Map.class.isAssignableFrom(field.getType()), "no Map field");
        }
        same(IndexedCollectionView.class.getDeclaredField("elementLane").getType(), Object[].class, "element lane");
        same(IndexedCollectionView.class.getDeclaredField("indexKeyLane").getType(), Object[].class, "key lane");
        same(IndexedCollectionView.class.getDeclaredField("indexOffsetLane").getType(), int[].class, "offset lane");
    }
    public static void main(String[] args) throws ReflectiveOperationException {
        randomized();
        uniquenessAndRefusal();
        storage();
        System.out.println("M3IndexedCollectionReceiverTest PASS checks=" + assertions);
    }
}
