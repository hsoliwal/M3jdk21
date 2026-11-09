/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: GPL-2.0-only
 */
/*
 * @test
 * @summary Bound weak-owner index churn and preserve sparse/75-percent resize boundaries
 * @modules java.base/java.lang
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED M3ObjectOwnerIndexChurnTest near-full
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -Xint M3ObjectOwnerIndexChurnTest near-full
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED M3ObjectOwnerIndexChurnTest sparse
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -Xint M3ObjectOwnerIndexChurnTest sparse
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED M3ObjectOwnerIndexChurnTest threshold-75
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -Xint M3ObjectOwnerIndexChurnTest threshold-75
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED M3ObjectOwnerIndexChurnTest threshold-above-75
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -Xint M3ObjectOwnerIndexChurnTest threshold-above-75
 */
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Structural probe only: explicit WeakReference.clear() models a completed
 * collection after the probe releases each facade. This does not prove real
 * GC liveness, class unloading, or throughput.
 */
public final class M3ObjectOwnerIndexChurnTest {
    private static final int CAPACITY = 1024;
    private static final int ADMISSIONS = 32;
    private static final Class<?> RUNTIME;
    private static final Object LOCK;

    static {
        try {
            RUNTIME = Class.forName("java.lang.M3ObjectRuntime");
            LOCK = read("LOCK");
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private record Lanes(Object[] owners, int[] hashes, int[] next,
                         int[] buckets, int used) { }

    private static final class Entry {
        private M3Object held;
        final WeakReference<?> weak;
        final int hash;

        Entry(M3Object held, WeakReference<?> weak, int hash) {
            this.held = held;
            this.weak = weak;
            this.hash = hash;
        }

        void releaseAndClear() {
            held = null;
            weak.clear();
        }

        void keepAlive() {
            java.lang.ref.Reference.reachabilityFence(held);
        }
    }

    private record Fixture(Object[] sources, M3Object[] facades,
                           WeakReference<?>[] weak, int[] hashes) { }

    private static Object read(String name) throws ReflectiveOperationException {
        Field field = RUNTIME.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    private static Lanes lanes() throws ReflectiveOperationException {
        return new Lanes((Object[]) read("owners"), (int[]) read("ownerHashes"),
                (int[]) read("ownerNext"), (int[]) read("ownerBuckets"),
                (int) read("ownerUsed"));
    }

    private static void check(boolean condition, String detail) {
        if (!condition) throw new AssertionError(detail);
    }

    private static int bucket(int hash, int mask) {
        return (hash ^ (hash >>> 16)) & mask;
    }

    private static WeakReference<?> referenceTo(M3Object facade)
            throws ReflectiveOperationException {
        for (Object slot : lanes().owners()) {
            if (slot instanceof WeakReference<?> weak && weak.get() == facade) {
                return weak;
            }
        }
        throw new AssertionError("new facade missing from owner index");
    }

    private static Fixture fill() throws ReflectiveOperationException {
        check(read("owners") == null, "probe requires a fresh, unused runtime registry");
        Object[] sources = new Object[CAPACITY];
        M3Object[] facades = new M3Object[CAPACITY];
        WeakReference<?>[] weak = new WeakReference<?>[CAPACITY];
        int[] hashes = new int[CAPACITY];
        for (int i = 0; i < CAPACITY; i++) {
            sources[i] = new Object();
            facades[i] = M3Object.of(sources[i]);
            hashes[i] = System.identityHashCode(sources[i]);
        }
        for (int i = 0; i < CAPACITY; i++) weak[i] = referenceTo(facades[i]);
        check(lanes().owners().length == CAPACITY && lanes().used() == CAPACITY,
                "fixture must exactly fill the primitive owner lanes");
        return new Fixture(sources, facades, weak, hashes);
    }

    private static Entry admitOutsideClearedBuckets(List<Integer> clearedHashes)
            throws ReflectiveOperationException {
        int mask = lanes().buckets().length - 1;
        for (int attempt = 0; attempt < 10_000; attempt++) {
            Object source = new Object();
            int hash = System.identityHashCode(source);
            int selected = bucket(hash, mask);
            boolean collision = false;
            for (int old : clearedHashes) {
                if (bucket(old, mask) == selected) {
                    collision = true;
                    break;
                }
            }
            if (collision) continue;
            M3Object facade = M3Object.of(source);
            return new Entry(facade, referenceTo(facade), hash);
        }
        throw new AssertionError("could not select a bucket without cleared entries");
    }

    private static int replacedLanes(Lanes before, Lanes after) {
        int replaced = (before.owners() != after.owners() ? 1 : 0)
                + (before.hashes() != after.hashes() ? 1 : 0)
                + (before.next() != after.next() ? 1 : 0)
                + (before.buckets() != after.buckets() ? 1 : 0);
        check(replaced == 0 || replaced == 4, "partial lane publication");
        int size = after.owners().length;
        check(after.hashes().length == size && after.next().length == size
                && after.buckets().length == size * 2, "inconsistent lane sizes");
        return replaced;
    }

    private static void verifyRetained(Fixture fixture, int retained) {
        for (int i = 0; i < retained; i++) {
            check(M3Object.of(fixture.sources()[i]) == fixture.facades()[i],
                    "retained facade identity lost at " + i);
        }
    }

    private static void nearFull() throws ReflectiveOperationException {
        synchronized (LOCK) {
            Fixture fixture = fill();
            Entry current = new Entry(fixture.facades()[CAPACITY - 1],
                    fixture.weak()[CAPACITY - 1], fixture.hashes()[CAPACITY - 1]);
            fixture.facades()[CAPACITY - 1] = null;
            fixture.sources()[CAPACITY - 1] = null;
            List<Integer> clearedHashes = new ArrayList<>();
            int rebuilds = 0;
            int replacements = 0;
            for (int i = 0; i < ADMISSIONS; i++) {
                clearedHashes.add(current.hash);
                current.releaseAndClear();
                Lanes before = lanes();
                current = admitOutsideClearedBuckets(clearedHashes);
                Lanes after = lanes();
                int replaced = replacedLanes(before, after);
                if (replaced != 0) rebuilds++;
                replacements += replaced;
                verifyRetained(fixture, CAPACITY - 1);
                current.keepAlive();
            }
            System.out.println("near-full retained=" + (CAPACITY - 1)
                    + " admissions=" + ADMISSIONS + " rebuilds=" + rebuilds
                    + " laneReplacements=" + replacements
                    + " finalCapacity=" + lanes().owners().length);
            check(rebuilds <= 1, "nearly-full weak churn must leave headroom; rebuilt "
                    + rebuilds + " times for " + ADMISSIONS + " admissions");
        }
    }

    private static void occupancy(int retained, int expectedCapacity, String scenario)
            throws ReflectiveOperationException {
        synchronized (LOCK) {
            Fixture fixture = fill();
            List<Integer> clearedHashes = new ArrayList<>();
            for (int i = retained; i < CAPACITY; i++) {
                fixture.facades()[i] = null;
                fixture.sources()[i] = null;
                fixture.weak()[i].clear();
                clearedHashes.add(fixture.hashes()[i]);
            }
            Lanes before = lanes();
            Entry current = admitOutsideClearedBuckets(clearedHashes);
            Lanes after = lanes();
            int replaced = replacedLanes(before, after);
            verifyRetained(fixture, retained);
            current.keepAlive();
            check(replaced == 4 && after.owners().length == expectedCapacity,
                    scenario + " capacity: expected=" + expectedCapacity
                            + ", actual=" + after.owners().length);
            System.out.println(scenario + " retained=" + retained
                    + " admissions=1 laneReplacements=" + replaced
                    + " finalCapacity=" + after.owners().length);
        }
    }

    public static void main(String[] args) throws ReflectiveOperationException {
        check(args.length == 1, "select near-full, sparse, or a threshold scenario");
        if (args[0].equals("near-full")) nearFull();
        else if (args[0].equals("sparse")) occupancy(64, 256, args[0]);
        else if (args[0].equals("threshold-75"))
            occupancy(CAPACITY - CAPACITY / 4, CAPACITY, args[0]);
        else if (args[0].equals("threshold-above-75"))
            occupancy(CAPACITY - CAPACITY / 4 + 1, CAPACITY * 2, args[0]);
        else throw new IllegalArgumentException(args[0]);
        System.out.println("PASS M3ObjectOwnerIndexChurnTest structural " + args[0]);
    }
}
