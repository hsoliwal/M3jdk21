/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */
// Template of gen_atom_tests.py / gen_atom_perf.py: the generated copy replaces this header.

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.JTree;
import javax.swing.tree.TreePath;

import sun.swing.tree.M3ExpandedState;

/**
 * Patterns ADD_REMOVE_GET / ITERATION_ORDER / GATE / EQUALS_HASHCODE: {@code M3ExpandedState} against a plain
 * {@code java.util.Hashtable<TreePath, Boolean>} fed the same {@code put}, {@code remove} and {@code clear} calls,
 * and against the stock {@code JTree} scans it replaces: {@code getExpandedDescendants} (keys in {@code keys()}
 * order, {@code TRUE}, {@code parent.isDescendant(key)}, {@code key != parent}) and
 * {@code removeDescendantToggledPaths} (every key {@code isDescendant} of the path). Path components collide in their
 * hash on purpose so that buckets hold chains. After every step: the {@code keys()} order of both tables, the bucket
 * count against the real {@code Hashtable} table, every value, and the trie holds exactly the keys and their
 * ancestors.
 */
final class TableScript {

    static final int FANOUT = 4;
    static final int DEPTH = 4;
    static final int STEPS = 300;
    static final int HASH_BUCKETS = 5;
    static final int FALSE_ONE_IN = 4;
    static final int CLEAR_ONE_IN = 60;
    static final int QUERIES = 3;
    /** {@code new Hashtable()}: 11 buckets, then 2n + 1 per rehash. */
    static final int INITIAL_BUCKETS = 11;
    static final int GROWTHS = 6;
    static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    enum Mix {
        PUT(8, 1, 1, 1), REMOVE(4, 5, 1, 1), DESCENDANTS(4, 1, 5, 1), QUERY(4, 1, 1, 6), MIXED(4, 2, 2, 3);

        final int put;
        final int remove;
        final int descendants;
        final int query;

        Mix(int put, int remove, int descendants, int query) {
            this.put = put;
            this.remove = remove;
            this.descendants = descendants;
            this.query = query;
        }
    }

    /** A path component equal by id whose hash collides with others on purpose. */
    static final class Part {
        final int id;

        Part(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Part p && p.id == id;
        }

        @Override
        public int hashCode() {
            return id % HASH_BUCKETS;
        }

        @Override
        public String toString() {
            return "p" + id;
        }
    }

    static final VarHandle TABLE;
    static final VarHandle NODES = AtomSupport.field(M3ExpandedState.class, "nodes", HashMap.class);

    static {
        try {
            TABLE = AtomSupport.field(Hashtable.class, "table", Class.forName("java.util.Hashtable$Entry").arrayType());
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    final Random rnd;
    final M3ExpandedState m3 = new M3ExpandedState();
    final Hashtable<TreePath, Boolean> plain = new Hashtable<>();
    /** Every path of a FANOUT-ary tree of DEPTH levels below the root, as component id lists. */
    final List<int[]> universe = new ArrayList<>();
    int compares;

    TableScript(String atom) {
        rnd = AtomSupport.random(atom);
        grow(new int[] {0}, 0);
    }

    private void grow(int[] ids, int depth) {
        universe.add(ids);
        if (depth < DEPTH) {
            for (int k = 0; k < FANOUT; k++) {
                int[] child = java.util.Arrays.copyOf(ids, ids.length + 1);
                child[ids.length] = ids[ids.length - 1] * FANOUT + k + 1;
                grow(child, depth + 1);
            }
        }
    }

    /** A fresh TreePath instance (equal to, not identical with, any key) for a universe entry. */
    static TreePath path(int[] ids) {
        Object[] parts = new Object[ids.length];
        for (int i = 0; i < ids.length; i++) {
            parts[i] = new Part(ids[i]);
        }
        return new TreePath(parts);
    }

    TreePath randomPath() {
        return path(universe.get(rnd.nextInt(universe.size())));
    }

    /** A key instance of the table, or a fresh equal path, or a path that is not a key. */
    TreePath randomParent() {
        List<TreePath> keys = Collections.list(m3.keys());
        if (!keys.isEmpty() && rnd.nextBoolean()) {
            return keys.get(rnd.nextInt(keys.size()));
        }
        return randomPath();
    }

    static int buckets(Hashtable<?, ?> table) {
        return ((Object[]) TABLE.get(table)).length;
    }

    // ---- oracles ---------------------------------------------------------------------------------------------

    /** The stock {@code getExpandedDescendants} loop without {@code isVisible}: the keys it tests, in order. */
    static List<TreePath> stockExpandedBelow(Hashtable<TreePath, Boolean> table, TreePath parent) {
        List<TreePath> out = new ArrayList<>();
        for (Enumeration<TreePath> e = table.keys(); e.hasMoreElements();) {
            TreePath p = e.nextElement();
            Boolean value = table.get(p);
            if (p != parent && value != null && value && parent.isDescendant(p)) {
                out.add(p);
            }
        }
        return out;
    }

    /** The stock {@code removeDescendantToggledPaths} for one path. */
    static void stockRemoveDescendants(Hashtable<TreePath, Boolean> table, TreePath path) {
        List<TreePath> descendants = new ArrayList<>();
        for (Enumeration<TreePath> e = table.keys(); e.hasMoreElements();) {
            TreePath p = e.nextElement();
            if (path.isDescendant(p)) {
                descendants.add(p);
            }
        }
        descendants.forEach(table::remove);
    }

    // ---- the comparison ----------------------------------------------------------------------------------------

    void compare(String what) {
        compares++;
        assertEquals(Collections.list(plain.keys()), Collections.list(m3.keys()), what + ": keys() order");
        assertEquals(plain.size(), m3.size(), what + ": size");
        assertEquals(buckets(plain), m3.bucketCount(), what + ": bucket count of the plain Hashtable");
        assertEquals(buckets(m3), m3.bucketCount(), what + ": bucket count of the table itself");
        for (int[] ids : universe) {
            TreePath p = path(ids);
            assertEquals(plain.get(p), m3.get(p), what + ": get " + p);
        }
        Set<TreePath> prefixes = new HashSet<>();
        for (TreePath k : plain.keySet()) {
            for (TreePath a = k; a != null; a = a.getParentPath()) {
                prefixes.add(a);
            }
        }
        assertEquals(prefixes, ((Map<?, ?>) NODES.get(m3)).keySet(), what + ": the trie holds the keys and their ancestors");
        for (int q = 0; q < QUERIES; q++) {
            query(what);
        }
    }

    void query(String what) {
        TreePath parent = randomParent();
        TreePath[] below = m3.expandedKeysBelow(parent);
        assertArrayEquals(stockExpandedBelow(plain, parent).toArray(new TreePath[0]), below,
                what + ": expandedKeysBelow " + parent);
    }

    // ---- script -------------------------------------------------------------------------------------------------

    void put() {
        TreePath p = randomPath();
        Boolean v = rnd.nextInt(FALSE_ONE_IN) == 0 ? Boolean.FALSE : Boolean.TRUE;
        assertEquals(plain.put(p, v), m3.put(p, v), "put returns the previous value");
    }

    void remove() {
        TreePath p = randomParent();
        assertEquals(plain.remove(p), m3.remove(p), "remove returns the previous value");
    }

    void removeDescendants() {
        TreePath p = randomParent();
        stockRemoveDescendants(plain, p);
        assertTrue(m3.removeDescendants(p), "the index decides while it saw every mutation");
    }

    void run(Mix mix, int steps) {
        compare("start");
        int total = mix.put + mix.remove + mix.descendants + mix.query;
        for (int step = 0; step < steps; step++) {
            if (rnd.nextInt(CLEAR_ONE_IN) == 0) {
                plain.clear();
                m3.clear();
                compare("clear");
                continue;
            }
            int op = rnd.nextInt(total);
            if (op < mix.put) {
                put();
            } else if (op < mix.put + mix.remove) {
                remove();
            } else if (op < mix.put + mix.remove + mix.descendants) {
                removeDescendants();
            } else {
                query("query");
            }
            compare("step " + step);
        }
    }

    // ---- scenarios of single atoms ------------------------------------------------------------------------------

    /** Growth: the bucket count follows the real table through GROWTHS rehashes. */
    void growth() {
        int expected = INITIAL_BUCKETS;
        assertEquals(expected, m3.bucketCount());
        int rehashes = 0;
        for (int[] ids : universe) {
            TreePath p = path(ids);
            plain.put(p, Boolean.TRUE);
            m3.put(p, Boolean.TRUE);
            if (m3.bucketCount() != expected) {
                expected = M3ExpandedState.nextCapacity(expected);
                rehashes++;
            }
            assertEquals(expected, m3.bucketCount(), "after " + m3.size() + " keys");
            assertEquals(buckets(m3), m3.bucketCount());
        }
        assertTrue(rehashes >= GROWTHS, "the universe forces at least " + GROWTHS + " rehashes");
        compare("grown");
    }

    /** A mutation behind the index's back (an iterator removal, computeIfAbsent) hands the decisions back. */
    void unseenMutation() {
        run(Mix.PUT, STEPS / 2);
        Iterator<TreePath> it = m3.keySet().iterator();
        if (!it.hasNext()) {
            m3.put(randomPath(), Boolean.TRUE);
            it = m3.keySet().iterator();
        }
        it.next();
        it.remove();
        assertNull(m3.expandedKeysBelow(randomPath()), "the index missed a removal: the caller runs the stock loop");
        assertFalse(m3.removeDescendants(randomPath()), "and the stock scan");
        m3.clear();
        assertEquals(0, m3.expandedKeysBelow(randomPath()).length, "clear makes the index whole again");
        TreePath added = randomPath();
        m3.computeIfAbsent(added, k -> Boolean.TRUE);
        assertNull(m3.expandedKeysBelow(added.getParentPath() == null ? added : added.getParentPath()),
                "computeIfAbsent adds an entry the index did not see");
    }

    static void nextCapacityContract() {
        assertEquals(2 * INITIAL_BUCKETS + 1, M3ExpandedState.nextCapacity(INITIAL_BUCKETS), "2n + 1");
        int c = INITIAL_BUCKETS;
        for (int g = 0; g < GROWTHS; g++) {
            int next = M3ExpandedState.nextCapacity(c);
            assertEquals(2 * c + 1, next);
            c = next;
        }
        int half = (MAX_ARRAY_SIZE - 1) / 2;
        assertEquals(MAX_ARRAY_SIZE, M3ExpandedState.nextCapacity(half), "2n + 1 reaching the limit exactly");
        assertEquals(MAX_ARRAY_SIZE, M3ExpandedState.nextCapacity(half + 1), "capped at the array limit");
        assertEquals(MAX_ARRAY_SIZE, M3ExpandedState.nextCapacity(MAX_ARRAY_SIZE), "at the limit nothing changes");
    }

    /** A JTree subclass that keeps the stock scan, one that overrides it, and one below the override. */
    static class PlainTree extends JTree {
    }

    static class OverridingTree extends JTree {
        @Override
        protected Enumeration<TreePath> getDescendantToggledPaths(TreePath parent) {
            return super.getDescendantToggledPaths(parent);
        }
    }

    static class BelowOverride extends OverridingTree {
    }

    static void gate(String atom) {
        assertEquals(Boolean.getBoolean("m3.swing.viewport"), M3ExpandedState.ENABLED, "ENABLED reads m3.swing.viewport");
        if ("stockToggledPaths".equals(atom)) {
            MethodHandle h = AtomSupport.statik(M3ExpandedState.class, "stockToggledPaths", boolean.class, Class.class);
            assertTrue((boolean) AtomSupport.call(h, JTree.class), "JTree itself keeps the stock method");
            assertTrue((boolean) AtomSupport.call(h, PlainTree.class));
            assertFalse((boolean) AtomSupport.call(h, OverridingTree.class), "an override replaces what the index mirrors");
            assertFalse((boolean) AtomSupport.call(h, BelowOverride.class), "inherited override");
            return;
        }
        for (int round = 0; round < 2; round++) {
            assertEquals(M3ExpandedState.ENABLED, M3ExpandedState.accepts(JTree.class), "round " + round);
            assertEquals(M3ExpandedState.ENABLED, M3ExpandedState.accepts(PlainTree.class));
            assertFalse(M3ExpandedState.accepts(OverridingTree.class), "an overriding JTree keeps a plain Hashtable");
            assertFalse(M3ExpandedState.accepts(BelowOverride.class));
        }
    }

    // ---- per-atom checks ----------------------------------------------------------------------------------------

    static void check(String atom) {
        TableScript s = new TableScript(atom);
        switch (atom) {
            case "M3ExpandedState" -> {
                M3ExpandedState fresh = new M3ExpandedState();
                assertEquals(0, fresh.size());
                assertEquals(INITIAL_BUCKETS, fresh.bucketCount(), "Hashtable's default bucket count");
                assertEquals(0, fresh.expandedKeysBelow(path(new int[] {0})).length);
                assertTrue(fresh.removeDescendants(path(new int[] {0})));
                s.run(Mix.MIXED, STEPS);
            }
            case "computeValue", "accepts", "stockToggledPaths" -> gate(atom);
            case "Node", "link", "attach" -> s.run(Mix.PUT, STEPS);
            case "bucketCount", "grow" -> s.growth();
            case "put" -> {
                s.run(Mix.PUT, STEPS);
                assertThrows(NullPointerException.class, () -> s.m3.put(s.randomPath(), null), "Hashtable refuses null values");
                s.compare("after a refused put");
            }
            case "nextCapacity" -> {
                nextCapacityContract();
                s.growth();
            }
            case "remove", "unlink" -> {
                s.run(Mix.REMOVE, STEPS);
                assertNull(s.m3.remove("not a path"), "a non-path key is not in the table");
                s.compare("after removing a non-path");
            }
            case "clear" -> {
                s.run(Mix.PUT, STEPS / 2);
                s.plain.clear();
                s.m3.clear();
                s.compare("cleared");
                assertEquals(0, s.m3.size());
                s.run(Mix.MIXED, STEPS / 2);
            }
            case "indexed" -> s.unseenMutation();
            case "expandedKeysBelow" -> {
                s.run(Mix.QUERY, STEPS);
                assertEquals(0, s.m3.expandedKeysBelow(null).length, "nothing is below no path");
            }
            case "removeDescendants" -> s.run(Mix.DESCENDANTS, STEPS);
            default -> fail("no check for M3ExpandedState#" + atom);
        }
    }
}
