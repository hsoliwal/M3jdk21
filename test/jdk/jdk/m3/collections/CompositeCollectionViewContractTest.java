/*
 * @test
 * @summary Array-backed M3 collection composite preserves Commons concatenation and live mutations
 * @modules jdk.m3.collections/jdk.internal.m3.collections
 * @run main CompositeCollectionViewContractTest
 */
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import jdk.internal.m3.collections.CompositeCollectionView;

public class CompositeCollectionViewContractTest {
    private static long assertions;
    private static void check(boolean ok, String what) {
        assertions++;
        if (!ok) throw new AssertionError(what);
    }
    private static void same(Object found, Object wanted, String what) {
        check(Objects.equals(found, wanted), what + ": " + found + " vs " + wanted);
    }
    private static Class<? extends Throwable> error(Runnable run) {
        try { run.run(); } catch (Throwable problem) { return problem.getClass(); }
        return null;
    }
    private static final class Reference<E> {
        final ArrayList<Collection<E>> backings = new ArrayList<>();
        void addComposited(Collection<E> c) { if (c != null) backings.add(c); }
        void removeComposited(Collection<E> c) { backings.remove(c); }
        int size() {
            long size = 0;
            for (var c : backings) size += c.size();
            return (int) Math.min(size, Integer.MAX_VALUE);
        }
        boolean isEmpty() { for (var c : backings) if (!c.isEmpty()) return false; return true; }
        boolean contains(Object e) { for (var c : backings) if (c.contains(e)) return true; return false; }
        List<E> flatten() {
            ArrayList<E> out = new ArrayList<>();
            for (var c : backings) out.addAll(c);
            return out;
        }
    }
    private static void assertEquivalent(CompositeCollectionView<Object> actual,
                                         Reference<Object> oracle, Random random) {
        same(actual.size(), oracle.size(), "live size");
        same(actual.isEmpty(), oracle.isEmpty(), "live empty");
        for (Object q : Arrays.asList(null, "word-0", "word-3", Integer.valueOf(0), new Object())) {
            same(actual.contains(q), oracle.contains(q), "contains");
        }
        List<Object> flattened = oracle.flatten();
        ArrayList<Object> seen = new ArrayList<>();
        for (Object value : actual) seen.add(value);
        same(seen.size(), flattened.size(), "traversed size");
        for (int i = 0; i < flattened.size(); i++) {
            check(seen.get(i) == flattened.get(i), "physical reference identity " + i);
        }
        Iterator<Object> iter = actual.iterator();
        for (Object next : flattened) {
            check(iter.hasNext(), "has next");
            check(iter.next() == next, "next identity");
        }
        check(!iter.hasNext(), "end of chain");
        same(error(iter::next), NoSuchElementException.class, "next after end");
    }
    private static void behavioral() {
        CompositeCollectionView<Object> owner = new CompositeCollectionView<>();
        Reference<Object> oracle = new Reference<>();
        Random random = new Random(0x414a116L);
        ArrayList<Object> left = new ArrayList<>(Arrays.asList(null, "x", Integer.valueOf(3)));
        ArrayList<Object> right = new ArrayList<>(Arrays.asList("x", null));
        owner.addComposited(null); oracle.addComposited(null);
        owner.addComposited(left); oracle.addComposited(left);
        owner.addComposited(right); oracle.addComposited(right);
        owner.addComposited(left); oracle.addComposited(left);
        assertEquivalent(owner, oracle, random);
        left.add("added later");
        right.remove(0);
        assertEquivalent(owner, oracle, random);
        owner.removeComposited(left); oracle.removeComposited(left);
        assertEquivalent(owner, oracle, random);
        same(error(() -> owner.add("x")), UnsupportedOperationException.class, "mutator add gate");
        same(error(() -> owner.remove("x")), UnsupportedOperationException.class, "mutator remove gate");
        Iterator<Object> it = owner.iterator();
        Object first = it.next();
        check(first == right.get(0), "first child identity");
        it.remove();
        // Both views reference the same backing collection; removal is visible immediately.
        assertEquivalent(owner, oracle, random);
        same(error(it::remove), IllegalStateException.class, "double remove refusal");
        same(CompositeCollectionView.scaffoldDescriptor(),
            "M3CompositeCollectionView|COMPOSITE_COLLECTION|OBJECT|SELECTED|Object[] backingLane", "descriptor");
    }
    private static void randomization() {
        for (int seed = 0; seed < 25; seed++) {
            var actual = new CompositeCollectionView<Object>();
            var expected = new Reference<Object>();
            Random rand = new Random(seed * 7723L + 0x98133L);
            ArrayList<ArrayList<Object>> pools = new ArrayList<>();
            for (int pool = 0; pool < 9; pool++) pools.add(new ArrayList<>());
            for (int step = 0; step < 150; step++) {
                int pool = rand.nextInt(pools.size());
                ArrayList<Object> child = pools.get(pool);
                int choice = rand.nextInt(5);
                if (choice == 0) {
                    actual.addComposited(child); expected.addComposited(child);
                } else if (choice == 1) {
                    actual.removeComposited(child); expected.removeComposited(child);
                } else if (choice == 2 && child.size() < 20) {
                    child.add(rand.nextInt(7) == 0 ? null : rand.nextBoolean() ? "word-" + rand.nextInt(6) : Integer.valueOf(rand.nextInt(9)));
                } else if (choice == 3 && !child.isEmpty()) {
                    child.remove(rand.nextInt(child.size()));
                } else if (choice == 4 && !child.isEmpty()) {
                    child.set(rand.nextInt(child.size()), Integer.valueOf(rand.nextInt(7)));
                }
                assertEquivalent(actual, expected, rand);
            }
        }
    }
    private static void reflectedStorage() throws Exception {
        Field owner = CompositeCollectionView.class.getDeclaredField("backingLane");
        same(owner.getType(), Object[].class, "one owner array of backing references");
        for (Field f : CompositeCollectionView.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
                check(!Collection.class.isAssignableFrom(f.getType()), "no retained Java Collection field");
            }
        }
    }
    public static void main(String[] args) throws Exception {
        behavioral();
        randomization();
        reflectedStorage();
        System.out.println("CompositeCollectionViewContractTest PASS assertions=" + assertions);
    }
}
