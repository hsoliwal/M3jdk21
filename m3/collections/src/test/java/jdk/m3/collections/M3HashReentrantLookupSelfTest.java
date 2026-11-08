/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Re-entrant key/value callbacks on the hash slot-map owners (qualification/hash-reentrant-guard-20261107), with the
 * java.util twin of every owner as the reference column. Keys and values are Probe objects whose hashCode and equals
 * count every call; at call N of an operation a one-shot action clears the owner and inserts keys 50 and 60, which
 * reuses the released payload slots; operations on a new key 9 also run with a racing action that inserts key 9
 * itself. Every operation shape runs over keys 1..7 with spread hashes (hashCode = id) and with colliding hashes (one
 * hash for every key, a single probe run / bucket chain), for every N.
 * Verdicts: FAILFAST (ConcurrentModificationException, the owner holding exactly the action's content), BEFORE (the
 * outcome of running the action first, then the operation), AFTER (the outcome of the operation first, then the
 * action: the operation linearized before the callback's change), PLAIN (the action did not fire; same as without
 * it), WRONG (none of these), DAMAGED (the owner's invariants or its map/set consistency broke).
 * Modes: {@code all} (default) asserts the M3 contract (see {@link #accepted}); {@code report} prints every case of
 * every owner, M3 and java.util, and never fails. The linked owners need the m3_arrays JNI library
 * (m3.arrays.native.path, profile m3-collections-native); without it they are skipped and named in the summary.
 */
public final class M3HashReentrantLookupSelfTest {
    private static final String CME = "!ConcurrentModificationException";
    private static int calls;
    private static int armedAt = -1;
    private static Runnable action;
    private static boolean fired;

    private M3HashReentrantLookupSelfTest() { }

    private static void callback() {
        calls++;
        if (calls == armedAt && action != null) {
            Runnable selected = action;
            action = null;
            fired = true;
            selected.run();
        }
    }

    /** Key or value whose hashCode and equals are counted user callbacks. */
    private static final class Probe {
        final char kind;
        final int id;
        final boolean collide;

        Probe(char kind, int id, boolean collide) { this.kind = kind; this.id = id; this.collide = collide; }

        @Override public int hashCode() {
            callback();
            return collide ? 42 : id;
        }

        @Override public boolean equals(Object other) {
            callback();
            return other instanceof Probe probe && probe.kind == kind && probe.id == id;
        }

        @Override public String toString() { return kind == 'k' ? String.valueOf(id) : "v" + id; }
    }

    /** An owner kind: name, java.util twin flag, map or set factory. */
    private record Owner(String name, boolean m3, Supplier<Map<Probe, Probe>> maps, Supplier<Set<Probe>> sets) { }

    private static final List<Owner> OWNERS = List.of(
            new Owner("M3HashMap", true, M3HashMap::new, null),
            new Owner("HashMap", false, HashMap::new, null),
            new Owner("M3LinkedHashMap", true, M3LinkedHashMap::new, null),
            new Owner("LinkedHashMap", false, LinkedHashMap::new, null),
            new Owner("M3LinkedHashMap(access)", true, () -> new M3LinkedHashMap<>(true), null),
            new Owner("LinkedHashMap(access)", false, () -> new LinkedHashMap<>(16, 0.75f, true), null),
            new Owner("M3WeakHashMap", true, M3WeakHashMap::new, null),
            new Owner("WeakHashMap", false, WeakHashMap::new, null),
            new Owner("M3HashSet", true, null, M3HashSet::new),
            new Owner("HashSet", false, null, HashSet::new),
            new Owner("M3LinkedHashSet", true, null, M3LinkedHashSet::new),
            new Owner("LinkedHashSet", false, null, LinkedHashSet::new));

    /** One owner instance with keys 1..7 (values v1..v7); the probes stay strongly reachable. */
    private static final class Fixture {
        final boolean collide;
        final Map<Probe, Probe> map;
        final Set<Probe> set;
        final List<Probe> held = new ArrayList<>();

        Fixture(Owner owner, boolean collide) {
            this.collide = collide;
            map = owner.maps() == null ? null : owner.maps().get();
            set = owner.sets() == null ? null : owner.sets().get();
            for (int id = 1; id <= 7; id++) insert(id);
        }

        Probe key(int id) { Probe key = new Probe('k', id, collide); held.add(key); return key; }
        Probe value(int id) { Probe value = new Probe('v', id, collide); held.add(value); return value; }

        void insert(int id) {
            if (map != null) map.put(key(id), value(id)); else set.add(key(id));
        }

        /** The callback's action: release every slot and reuse two of them for keys 50 and 60. */
        void reuse() {
            if (map != null) map.clear(); else set.clear();
            insert(50);
            insert(60);
        }

        /** The racing action: insert the operation's own new key 9 (an equal, distinct probe) with value v99. */
        void race() {
            if (map != null) map.put(key(9), value(99)); else set.add(key(9));
        }

        void act(boolean racing) {
            if (racing) race(); else reuse();
        }

        String content() {
            List<String> items = new ArrayList<>();
            int cap = 64;
            Iterator<?> cursor = map != null ? map.entrySet().iterator() : set.iterator();
            while (cursor.hasNext() && cap-- > 0) {
                Object item = cursor.next();
                Object key = item instanceof Map.Entry<?, ?> entry ? entry.getKey() : item;
                String id = key instanceof Probe probe ? String.valueOf(probe.id) : "null";
                items.add(item instanceof Map.Entry<?, ?> entry ? id + "=" + entry.getValue() : id);
            }
            items.sort((left, right) -> Integer.compare(order(left), order(right)));
            return items.toString();
        }

        private static int order(String item) {
            String id = item.split("=")[0];
            return id.equals("null") ? Integer.MIN_VALUE : Integer.parseInt(id);
        }

        /** Owner invariants (M3) and map/set consistency (all owners): size, uniqueness, lookups. */
        String verify() {
            try {
                if (map instanceof M3HashMap<?, ?> owner) owner.verifyInvariants();
                if (map instanceof M3LinkedHashMap<?, ?> owner) owner.verifyInvariants();
                if (set instanceof M3HashSet<?> owner) owner.verifyInvariants();
                if (set instanceof M3LinkedHashSet<?> owner) owner.verifyInvariants();
                Collection<?> items = map != null ? map.entrySet() : set;
                List<Object[]> snapshot = new ArrayList<>();
                for (Iterator<?> cursor = items.iterator(); cursor.hasNext() && snapshot.size() <= 64; ) {
                    Object item = cursor.next();
                    snapshot.add(item instanceof Map.Entry<?, ?> entry
                            ? new Object[] {entry.getKey(), entry.getValue()} : new Object[] {item, null});
                }
                Set<Integer> ids = new HashSet<>();
                for (Object[] item : snapshot) {
                    if (!(item[0] instanceof Probe key)) return "damaged:null key";
                    if (!ids.add(key.id)) return "damaged:duplicate key " + key.id;
                    Probe equal = new Probe('k', key.id, collide);
                    if (map != null) {
                        // access-order owners move the entry on get: lookups run after the snapshot
                        if (!map.containsKey(equal) || map.get(equal) != item[1]) return "damaged:lookup " + key.id;
                    } else if (!set.contains(equal)) {
                        return "damaged:lookup " + key.id;
                    }
                }
                int size = map != null ? map.size() : set.size();
                return size == snapshot.size() ? "valid" : "damaged:size " + size + " != " + snapshot.size();
            } catch (AssertionError | RuntimeException failure) {
                return "damaged:" + failure.getClass().getSimpleName() + ":" + failure.getMessage();
            }
        }
    }

    /** An operation: its name, whether it is read-only, map or set, and the body. */
    private record Op(String name, boolean reads, boolean onSet, Function<Fixture, Object> body) { }

    private static Op map(String name, boolean reads, Function<Fixture, Object> body) {
        return new Op(name, reads, false, body);
    }

    private static Op set(String name, boolean reads, Function<Fixture, Object> body) {
        return new Op(name, reads, true, body);
    }

    private static final List<Op> OPS = List.of(
            map("get", true, f -> f.map.get(f.key(2))),
            map("get(6)", true, f -> f.map.get(f.key(6))),
            map("containsKey", true, f -> f.map.containsKey(f.key(2))),
            map("getOrDefault", true, f -> f.map.getOrDefault(f.key(2), f.value(0))),
            map("keySet.contains", true, f -> f.map.keySet().contains(f.key(2))),
            map("remove", false, f -> f.map.remove(f.key(2))),
            map("remove(6)", false, f -> f.map.remove(f.key(6))),
            map("keySet.remove", false, f -> f.map.keySet().remove(f.key(2))),
            map("put(existing)", false, f -> f.map.put(f.key(2), f.value(20))),
            map("put(new)", false, f -> f.map.put(f.key(9), f.value(90))),
            map("putIfAbsent(existing)", false, f -> f.map.putIfAbsent(f.key(2), f.value(20))),
            map("putIfAbsent(new)", false, f -> f.map.putIfAbsent(f.key(9), f.value(90))),
            map("replace", false, f -> f.map.replace(f.key(2), f.value(20))),
            map("computeIfAbsent(new)", false, f -> f.map.computeIfAbsent(f.key(9), key -> f.value(90))),
            map("computeIfPresent", false, f -> f.map.computeIfPresent(f.key(2), (key, old) -> f.value(20))),
            map("compute(remove)", false, f -> f.map.compute(f.key(2), (key, old) -> null)),
            map("compute(write)", false, f -> f.map.compute(f.key(2), (key, old) -> f.value(old == null ? 30 : 20))),
            map("merge(remove)", false, f -> f.map.merge(f.key(2), f.value(20), (old, given) -> null)),
            map("merge(write)", false, f -> f.map.merge(f.key(2), f.value(20), (old, given) -> f.value(21))),
            map("compute(new)", false, f -> f.map.compute(f.key(9), (key, old) -> f.value(old == null ? 90 : 91))),
            map("merge(new)", false, f -> f.map.merge(f.key(9), f.value(90), (old, given) -> f.value(91))),
            map("putFirst(new)", false, f -> f.map instanceof SequencedMap<Probe, Probe> sequenced
                    ? sequenced.putFirst(f.key(9), f.value(90)) : "n/a"),
            map("remove(k,v)", false, f -> f.map.remove(f.key(2), f.value(2))),
            map("replace(k,old,new)", false, f -> f.map.replace(f.key(2), f.value(2), f.value(20))),
            map("entrySet.remove", false, f -> f.map.entrySet().remove(Map.entry(f.key(2), f.value(2)))),
            map("entrySet.contains", true, f -> f.map.entrySet().contains(Map.entry(f.key(2), f.value(2)))),
            map("containsValue", true, f -> f.map.containsValue(f.value(2))),
            map("values.remove", false, f -> f.map.values().remove(f.value(2))),
            set("set.contains", true, f -> f.set.contains(f.key(2))),
            set("set.add(existing)", false, f -> f.set.add(f.key(2))),
            set("set.add(new)", false, f -> f.set.add(f.key(9))),
            set("set.remove", false, f -> f.set.remove(f.key(2))),
            set("set.remove(6)", false, f -> f.set.remove(f.key(6))));

    private record Outcome(String result, String content, String verify, boolean fired, int calls) {
        boolean sameAs(Outcome other) { return result.equals(other.result) && content.equals(other.content); }
    }

    /** One case: armedAt counts the operation's own callbacks (0 = never fire); mode 1 = action first, 2 = after. */
    private static Outcome attempt(Owner owner, boolean collide, boolean racing, Op op, int armed, int mode) {
        action = null;
        armedAt = -1;
        fired = false;
        Fixture fixture = new Fixture(owner, collide);
        if (mode == 1) fixture.act(racing);
        int before = calls;
        if (armed > 0) {
            armedAt = before + armed;
            action = () -> fixture.act(racing);
        }
        String result;
        try {
            result = String.valueOf(op.body().apply(fixture));
        } catch (RuntimeException failure) {
            result = "!" + failure.getClass().getSimpleName();
        }
        int used = calls - before;
        boolean wasFired = fired;
        action = null;
        armedAt = -1;
        if (mode == 2) fixture.act(racing);
        return new Outcome(result, fixture.content(), fixture.verify(), wasFired, used);
    }

    private static String verdict(Outcome outcome, Outcome plain, Outcome first, Outcome after, String reused) {
        if (!outcome.verify().equals("valid")) return "DAMAGED";
        if (!outcome.fired()) return outcome.sameAs(plain) ? "PLAIN" : "WRONG";
        if (outcome.result().equals(CME) && outcome.content().equals(reused)) return "FAILFAST";
        if (outcome.sameAs(first)) return "BEFORE";
        if (outcome.sameAs(after)) return "AFTER";
        return "WRONG";
    }

    /**
     * The M3 contract: never DAMAGED; a mutating operation never WRONG (no wrong mapping removed or written); a
     * read-only operation WRONG only where its java.util twin is WRONG for the same shape too; FAILFAST only where
     * the twin is not cleanly linearizable for the same shape (not stricter than the JDK).
     */
    static boolean accepted(String verdict, boolean reads, boolean twinWrong, boolean twinClean) {
        if (verdict.equals("DAMAGED")) return false;
        if (verdict.equals("WRONG")) return reads && twinWrong;
        if (verdict.equals("FAILFAST")) return !twinClean;
        return true;
    }

    /** M3LinkedHashMap and M3LinkedHashSet allocate through the m3_arrays JNI library (native test profile). */
    private static boolean jniAvailable() {
        String configured = System.getProperty("m3.arrays.native.path");
        return jdk.m3.arrays.ArrayNative.isLoaded() || configured != null && !configured.isBlank();
    }

    public static void main(String[] args) {
        boolean report = args.length > 0 && args[0].equals("report");
        boolean jni = jniAvailable();
        List<String> failures = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        int cases = 0;
        for (int pair = 0; pair < OWNERS.size(); pair += 2) {
            Owner m3 = OWNERS.get(pair);
            Owner twin = OWNERS.get(pair + 1);
            if (!jni && m3.name().startsWith("M3Linked")) {
                skipped.add(m3.name());
                continue;
            }
            for (Op op : OPS) {
                if (op.onSet() != (m3.sets() != null)) continue;
                for (int variant = 0; variant < (op.name().contains("(new)") ? 4 : 2); variant++) {
                    boolean collide = (variant & 1) == 1;
                    boolean racing = variant >= 2;
                    String mode = (collide ? "collide" : "spread") + (racing ? "/race9" : "/reuse");
                    boolean twinWrong = false;
                    boolean twinClean = true;
                    List<String> m3Rows = new ArrayList<>();
                    for (Owner owner : List.of(twin, m3)) {
                        Outcome plain = attempt(owner, collide, racing, op, 0, 0);
                        Outcome first = attempt(owner, collide, racing, op, 0, 1);
                        Outcome after = attempt(owner, collide, racing, op, 0, 2);
                        Fixture alone = new Fixture(owner, collide);
                        alone.act(racing);
                        String reused = alone.content();
                        for (int call = 1; call <= plain.calls(); call++) {
                            Outcome outcome = attempt(owner, collide, racing, op, call, 0);
                            String verdict = verdict(outcome, plain, first, after, reused);
                            String row = owner.name() + "\t" + mode + "\t" + op.name() + "\t" + call + "\t" + verdict
                                    + "\t" + outcome.result() + "\t" + outcome.content() + "\t" + outcome.verify()
                                    + "\tbefore=" + first.result() + " " + first.content()
                                    + "\tafter=" + after.result() + "\tplain=" + plain.result();
                            if (owner == twin && verdict.equals("WRONG")) twinWrong = true;
                            if (owner == twin && !List.of("PLAIN", "BEFORE", "AFTER").contains(verdict)) {
                                twinClean = false;
                            }
                            if (owner == m3) m3Rows.add(verdict + "\t" + row);
                            if (report) System.out.println(row);
                        }
                    }
                    for (String row : m3Rows) {
                        cases++;
                        String verdict = row.substring(0, row.indexOf('\t'));
                        if (!accepted(verdict, op.reads(), twinWrong, twinClean)) {
                            failures.add(row.substring(row.indexOf('\t') + 1));
                        }
                    }
                }
            }
        }
        System.out.println("M3HashReentrantLookupSelfTest cases=" + cases + " failures=" + failures.size()
                + (skipped.isEmpty() ? "" : " skipped(no m3_arrays JNI)=" + skipped));
        if (!report && !failures.isEmpty()) {
            throw new AssertionError(failures.size() + " re-entrant hash callback cases failed, first: " + failures.get(0));
        }
    }
}
