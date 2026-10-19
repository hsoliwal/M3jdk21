/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary M3 swing-viewport leaf 5: JUnit suite of sun.swing.tree.M3ExpandedState: the table and
 *          its order against a plain Hashtable, the mirrored bucket count, expandedKeysBelow and
 *          removeDescendants against the stock loops, the fallbacks, the class gate, the recorded
 *          hash precondition, serialization, plus the JTree differential.
 * @modules java.desktop/sun.swing.tree java.desktop/javax.swing:open java.base/java.util:open
 * @build M3JTreeExpansionDifferential
 * @run junit/othervm -Djava.awt.headless=true -Dm3.swing.viewport=true -XX:-OmitStackTraceInFastThrow M3ExpandedStateTest
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.junit.jupiter.api.Test;

import sun.swing.tree.M3ExpandedState;

public class M3ExpandedStateTest {

    /** A path component equal to every component with the same name, with a settable hash. */
    static final class Name implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        final String s;
        int hash;

        Name(String s) {
            this.s = s;
            hash = s.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Name n && n.s.equals(s);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return s;
        }
    }

    /** Only referenced from {@link BrokenTree}; the test loader below refuses to load it. */
    static final class Missing {
    }

    /** A JTree class whose declared methods cannot be resolved when {@link Missing} is absent. */
    @SuppressWarnings("serial")
    public static class BrokenTree extends JTree {
        public void use(Missing m) {
        }
    }

    @SuppressWarnings("serial")
    static class StockChild extends M3JTreeExpansionDifferential.StockTree {
        StockChild() {
            super(new DefaultTreeModel(new DefaultMutableTreeNode("r")));
        }
    }

    /** The stock getExpandedDescendants loop, before its isVisible test. */
    static List<TreePath> stockBelow(Hashtable<TreePath, Boolean> t, TreePath parent) {
        List<TreePath> out = new ArrayList<>();
        for (Enumeration<TreePath> e = t.keys(); e.hasMoreElements();) {
            TreePath p = e.nextElement();
            Boolean v = t.get(p);
            if (p != parent && v != null && v && parent.isDescendant(p)) {
                out.add(p);
            }
        }
        return out;
    }

    /** The stock removeDescendantToggledPaths for one path. */
    static void stockRemove(Hashtable<TreePath, Boolean> t, TreePath path) {
        List<TreePath> d = new ArrayList<>();
        for (Enumeration<TreePath> e = t.keys(); e.hasMoreElements();) {
            TreePath p = e.nextElement();
            if (path.isDescendant(p)) {
                d.add(p);
            }
        }
        for (TreePath p : d) {
            t.remove(p);
        }
    }

    static int buckets(Hashtable<?, ?> t) throws Exception {
        Field f = Hashtable.class.getDeclaredField("table");
        f.setAccessible(true);
        return ((Object[]) f.get(t)).length;
    }

    static TreePath randomPath(Random rnd, Map<String, Name> pool) {
        int depth = 1 + rnd.nextInt(6);
        Object[] c = new Object[depth];
        String prefix = "";
        for (int i = 0; i < depth; i++) {
            prefix = prefix + rnd.nextInt(3);
            c[i] = rnd.nextInt(5) == 0 ? new Name(prefix) : pool.computeIfAbsent(prefix, Name::new);
        }
        return new TreePath(c);
    }

    static void sameKeys(Hashtable<TreePath, Boolean> a, Hashtable<TreePath, Boolean> b) {
        List<TreePath> ka = Collections.list(a.keys());
        List<TreePath> kb = Collections.list(b.keys());
        assertEquals(ka.size(), kb.size());
        for (int i = 0; i < ka.size(); i++) {
            assertSame(ka.get(i), kb.get(i), "key " + i);
            assertEquals(a.get(ka.get(i)), b.get(kb.get(i)));
        }
    }

    /** Random puts, removals, subtree removals and clears: same keys, values, order and bucket count. */
    @Test
    void tableOrderAndQueriesMatchHashtable() throws Exception {
        Random rnd = new Random(5);
        int answered = 0;
        int ties = 0;
        for (int round = 0; round < 30; round++) {
            Map<String, Name> pool = new HashMap<>();
            M3ExpandedState m3 = new M3ExpandedState();
            Hashtable<TreePath, Boolean> stock = new Hashtable<>();
            List<TreePath> used = new ArrayList<>();
            for (int op = 0; op < 500; op++) {
                int r = rnd.nextInt(100);
                TreePath p = !used.isEmpty() && rnd.nextBoolean() ? used.get(rnd.nextInt(used.size()))
                        : randomPath(rnd, pool);
                if (r < 50) {
                    Boolean v = rnd.nextInt(4) > 0;
                    assertEquals(stock.put(p, v), m3.put(p, v));
                    used.add(p);
                } else if (r < 62) {
                    assertEquals(stock.remove(p), m3.remove(p));
                } else if (r < 70) {
                    stockRemove(stock, p);
                    assertTrue(m3.removeDescendants(p));
                } else if (r < 71) {
                    stock.clear();
                    m3.clear();
                } else {
                    TreePath[] got = m3.expandedKeysBelow(p);
                    answered++;
                    List<TreePath> want = stockBelow(stock, p);
                    assertEquals(want.size(), got.length, "round " + round + " op " + op);
                    for (int i = 0; i < got.length; i++) {
                        assertSame(want.get(i), got[i], "round " + round + " op " + op + " key " + i);
                        if (i > 0 && bucket(got[i], m3) == bucket(got[i - 1], m3)) {
                            ties++;
                        }
                    }
                }
                sameKeys(stock, m3);
                assertEquals(buckets(m3), m3.bucketCount());
                assertEquals(buckets(stock), buckets(m3));
            }
        }
        assertTrue(answered > 1000 && ties > 100, answered + " answered, " + ties + " neighbours in one bucket");
    }

    static int bucket(TreePath p, M3ExpandedState t) {
        return (p.hashCode() & 0x7FFFFFFF) % t.bucketCount();
    }

    /** A null key or value never reaches rehash: same exception as Hashtable, same table after it. */
    @Test
    void nullKeyOrValueAtTheThreshold() {
        M3ExpandedState m3 = new M3ExpandedState();
        Hashtable<TreePath, Boolean> stock = new Hashtable<>();
        for (int i = 0; i < 8; i++) {
            TreePath p = new TreePath(new Object[] {"r", "k" + i});
            stock.put(p, Boolean.TRUE);
            m3.put(p, Boolean.TRUE);
        }
        TreePath fresh = new TreePath(new Object[] {"r", "fresh"});
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> m3.put(fresh, null));
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> stock.put(fresh, null));
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> m3.put(null, Boolean.TRUE));
        sameKeys(stock, m3);
        for (int i = 0; i < 30; i++) {
            TreePath p = new TreePath(new Object[] {"r", "m" + i});
            stock.put(p, Boolean.TRUE);
            m3.put(p, Boolean.TRUE);
        }
        sameKeys(stock, m3);
        List<TreePath> want = stockBelow(stock, new TreePath("r"));
        TreePath[] got = m3.expandedKeysBelow(new TreePath("r"));
        assertEquals(want.size(), got.length);
        for (int i = 0; i < got.length; i++) {
            assertSame(want.get(i), got[i]);
        }
    }

    /** Hashtable's growth rule, including the array limit, and the mirror over 10^5 puts. */
    @Test
    void bucketCountFollowsHashtable() throws Exception {
        assertEquals(23, M3ExpandedState.nextCapacity(11));
        int max = Integer.MAX_VALUE - 8;
        assertEquals(max, M3ExpandedState.nextCapacity(max / 2 + 10));
        assertEquals(max, M3ExpandedState.nextCapacity(max));
        M3ExpandedState m3 = new M3ExpandedState();
        assertEquals(11, m3.bucketCount());
        for (int i = 0; i < 100_000; i++) {
            m3.put(new TreePath(new Object[] {"r", Integer.valueOf(i)}), Boolean.TRUE);
            if (i % 997 == 0 || i < 100) {
                assertEquals(buckets(m3), m3.bucketCount(), "after " + i);
            }
        }
        assertEquals(buckets(m3), m3.bucketCount());
        m3.clear();
        assertEquals(buckets(m3), m3.bucketCount(), "clear keeps the buckets");
    }

    /** The parent is skipped by identity, as the stock {@code path != parent}; FALSE keys never count. */
    @Test
    void parentIdentityAndValues() {
        M3ExpandedState m3 = new M3ExpandedState();
        TreePath k1 = new TreePath(new Object[] {"r", "a"});
        TreePath k2 = new TreePath(new Object[] {"r", "a"});
        TreePath child = k1.pathByAddingChild("b");
        assertEquals(0, m3.expandedKeysBelow(null).length);
        assertEquals(0, m3.expandedKeysBelow(k1).length, "unknown parent");
        m3.put(k1, Boolean.TRUE);
        assertEquals(0, m3.expandedKeysBelow(k1).length);
        assertSame(k1, m3.expandedKeysBelow(k2)[0]);
        m3.put(child, Boolean.FALSE);
        assertEquals(0, m3.expandedKeysBelow(k1).length, "FALSE key");
        m3.put(k2, Boolean.FALSE);
        assertEquals(0, m3.expandedKeysBelow(k2).length, "put keeps the first key instance, now FALSE");
        assertTrue(m3.removeDescendants(k2));
        assertTrue(m3.isEmpty());
        assertTrue(m3.removeDescendants(null));
        assertTrue(m3.removeDescendants(k1), "unknown path");
    }

    /**
     * Keys in one bucket keep their chain order: newest first, and after each rehash the order in
     * which rehash moved them. Hashes 3 + 11k all land in bucket 3 of 11; growth to 23, 47 and 95
     * buckets splits and reverses the chains.
     */
    @Test
    void sharedBucketKeepsTheChainOrder() {
        M3ExpandedState m3 = new M3ExpandedState();
        Hashtable<TreePath, Boolean> stock = new Hashtable<>();
        TreePath r = new TreePath(new Name("root"));
        for (Hashtable<TreePath, Boolean> t : List.of(stock, m3)) {
            t.put(r, Boolean.TRUE);
        }
        TreePath fresh = new TreePath(new Name("root"));
        for (int i = 0; i < 60; i++) {
            Name c = new Name("c" + i);
            c.hash = 3 + 11 * (i % 7) + 23 * (i % 3);
            TreePath p = r.pathByAddingChild(c);
            for (Hashtable<TreePath, Boolean> t : List.of(stock, m3)) {
                t.put(p, Boolean.TRUE);
            }
            if (i % 9 == 4) {
                int j = i / 2;
                Name g = new Name("c" + j);
                g.hash = 3 + 11 * (j % 7) + 23 * (j % 3);
                TreePath gone = r.pathByAddingChild(g);
                assertEquals(stock.remove(gone), m3.remove(gone));
            }
            sameKeys(stock, m3);
            List<TreePath> want = stockBelow(stock, fresh);
            TreePath[] got = m3.expandedKeysBelow(fresh);
            assertEquals(want.size(), got.length);
            for (int k = 0; k < got.length; k++) {
                assertSame(want.get(k), got[k], "after " + i + " key " + k);
            }
        }
        assertEquals(95, m3.bucketCount());
    }

    /** A removal the index did not see (an entry-set iterator) hands every call back until clear. */
    @Test
    void unseenMutationFallsBack() {
        M3ExpandedState m3 = new M3ExpandedState();
        TreePath r = new TreePath("r");
        TreePath a = r.pathByAddingChild("a");
        m3.put(r, Boolean.TRUE);
        m3.put(a, Boolean.TRUE);
        assertNotNull(m3.expandedKeysBelow(new TreePath("r")));
        Iterator<Map.Entry<TreePath, Boolean>> it = m3.entrySet().iterator();
        it.next();
        it.remove();
        assertNull(m3.expandedKeysBelow(r));
        assertFalse(m3.removeDescendants(r));
        m3.put(a, Boolean.TRUE);
        m3.put(r, Boolean.TRUE);
        m3.remove(a);
        assertNull(m3.expandedKeysBelow(r), "still unseen");
        m3.clear();
        m3.put(r, Boolean.TRUE);
        assertEquals(0, m3.expandedKeysBelow(r).length);
        assertTrue(m3.removeDescendants(r));
    }

    /**
     * The recorded precondition (DESIGN.md section 11): a component whose hash changes after the index
     * holds it as an ancestor makes the index miss keys below it that the stock loop finds.
     */
    @Test
    void changedHashIsTheRecordedPrecondition() {
        M3ExpandedState m3 = new M3ExpandedState();
        Hashtable<TreePath, Boolean> stock = new Hashtable<>();
        Name a = new Name("a");
        TreePath child = new TreePath(new Object[] {a, "b"});
        for (Hashtable<TreePath, Boolean> t : List.of(stock, m3)) {
            t.put(child, Boolean.TRUE);
        }
        a.hash = 12345;
        TreePath parent = new TreePath(a);
        for (Hashtable<TreePath, Boolean> t : List.of(stock, m3)) {
            t.put(parent, Boolean.TRUE);
        }
        TreePath other = new TreePath(new Name("a"));
        ((Name) other.getLastPathComponent()).hash = 12345;
        assertEquals(2, stockBelow(stock, other).size());
        TreePath[] got = m3.expandedKeysBelow(other);
        assertEquals(1, got.length, "the index lost the key below the changed component");
        assertTrue(m3.remove(parent));
        assertTrue(m3.remove(child));
        assertTrue(m3.isEmpty());
        for (boolean reattach : new boolean[] {false, true}) {
            M3ExpandedState t = new M3ExpandedState();
            Name x = new Name("x");
            TreePath below = new TreePath(new Object[] {x, "b"});
            t.put(below, Boolean.TRUE);
            x.hash = 777;
            TreePath top = new TreePath(x);
            t.put(top, Boolean.TRUE);
            t.remove(below);
            if (!reattach) {
                for (int i = 0; i < 10; i++) {
                    t.put(new TreePath(new Object[] {"z", "z" + i}), Boolean.TRUE);
                }
                assertEquals(23, t.bucketCount(), "a rehash with a key the index lost");
                t.removeDescendants(new TreePath("z"));
            }
            if (reattach) {
                t.put(new TreePath(new Object[] {x, "c"}), Boolean.TRUE);
            }
            assertTrue(t.remove(top), "the table still removes the key");
            assertNull(t.expandedKeysBelow(new TreePath(x)), "the index lost track and hands back to the stock loop");
            assertFalse(t.removeDescendants(top));
            t.clear();
            t.put(top, Boolean.TRUE);
            assertEquals(0, t.expandedKeysBelow(top).length, "clear starts over");
        }
    }

    /** Only JTree classes that keep the stock getDescendantToggledPaths get the table. */
    @Test
    void classGate() throws Exception {
        assertTrue(M3ExpandedState.ENABLED);
        assertTrue(M3ExpandedState.accepts(JTree.class));
        assertTrue(M3ExpandedState.accepts(new JTree() { }.getClass()));
        assertTrue(M3ExpandedState.accepts(String.class), "not a JTree: nothing overridden");
        assertFalse(M3ExpandedState.accepts(M3JTreeExpansionDifferential.StockTree.class));
        assertFalse(M3ExpandedState.accepts(StockChild.class));
        String name = BrokenTree.class.getName();
        byte[] bytes;
        try (InputStream in = M3ExpandedStateTest.class.getResourceAsStream("M3ExpandedStateTest$BrokenTree.class")) {
            bytes = in.readAllBytes();
        }
        ClassLoader loader = new ClassLoader(M3ExpandedStateTest.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String n, boolean resolve) throws ClassNotFoundException {
                if (n.equals(name)) {
                    synchronized (getClassLoadingLock(n)) {
                        Class<?> c = findLoadedClass(n);
                        return c != null ? c : defineClass(n, bytes, 0, bytes.length);
                    }
                }
                if (n.endsWith("$Missing")) {
                    throw new ClassNotFoundException(n);
                }
                return super.loadClass(n, resolve);
            }
        };
        assertFalse(M3ExpandedState.accepts(loader.loadClass(name)), "unresolvable methods: stock table");
        assertTrue(table(new JTree()) instanceof M3ExpandedState);
        assertSame(Hashtable.class, table(new StockChild()).getClass());
    }

    @SuppressWarnings("unchecked")
    static Hashtable<TreePath, Boolean> table(JTree t) throws Exception {
        Field f = JTree.class.getDeclaredField("expandedState");
        f.setAccessible(true);
        return (Hashtable<TreePath, Boolean>) f.get(t);
    }

    /** readObject gives the M3 table too, with the same keys in the same order as the stock side (name-hashed nodes). */
    @Test
    void serializationRoundTrip() throws Exception {
        List<JTree> back = new ArrayList<>();
        for (boolean m3 : new boolean[] {false, true}) {
            DefaultMutableTreeNode root = new M3JTreeExpansionDifferential.Named("r");
            for (int i = 0; i < 6; i++) {
                DefaultMutableTreeNode c = new M3JTreeExpansionDifferential.Named("c" + i);
                root.add(c);
                for (int j = 0; j < 3; j++) {
                    c.add(new M3JTreeExpansionDifferential.Named("c" + i + "." + j));
                }
            }
            DefaultTreeModel model = new DefaultTreeModel(root);
            JTree t = m3 ? new JTree(model) : new M3JTreeExpansionDifferential.StockTree(model);
            for (int i = 0; i < t.getRowCount(); i += 2) {
                t.expandRow(i);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ObjectOutputStream o = new ObjectOutputStream(out)) {
                o.writeObject(t);
            }
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()))) {
                back.add((JTree) in.readObject());
            }
        }
        assertSame(Hashtable.class, table(back.get(0)).getClass());
        assertTrue(table(back.get(1)) instanceof M3ExpandedState);
        assertEquals(Collections.list(table(back.get(0)).keys()).toString(),
                Collections.list(table(back.get(1)).keys()).toString());
        assertEquals(back.get(0).getRowCount(), back.get(1).getRowCount());
        assertTrue(back.get(1).getRowCount() > 6);
    }

    /** The JTree-level differential. */
    @Test
    void jtreeDifferential() throws Exception {
        String summary = M3JTreeExpansionDifferential.runSeeds(100, 20, 200);
        assertTrue(summary.contains(" failed 0 "), summary);
    }
}
