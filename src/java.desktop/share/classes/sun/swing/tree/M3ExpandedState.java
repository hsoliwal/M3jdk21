/*
 * Copyright (c) 2026, Hitesh Soliwal (Synexia) and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.swing.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;

import javax.swing.JTree;
import javax.swing.tree.TreePath;

import sun.security.action.GetBooleanAction;

/**
 * The expansion table of {@code JTree} ({@code expandedState}) with an index of its paths by
 * parent ({@code -Dm3.swing.viewport=true}, off by default). It is the stock {@code Hashtable}, so
 * {@code get}, {@code keys()} and their order are unchanged; {@code put}, {@code remove} and
 * {@code clear} also keep a trie of the toggled paths and the ancestors that lead to them. Stock
 * {@code JTree} scans the whole table with {@code TreePath.isDescendant} for every expanded node's
 * descendants ({@code getExpandedDescendants}, on every expansion through {@code BasicTreeUI}) and
 * for every removed path ({@code removeDescendantToggledPaths}): O(table) per call. The trie answers
 * both from the subtree in O(descendants).
 * <p>
 * Exactness: {@link #removeDescendants} removes the same keys as the stock scan, and the order of
 * removals does not change the table's structure (a removal unlinks one entry and never rehashes).
 * {@link #expandedKeysBelow} gives the keys the stock loop tests, in the order of {@code keys()}:
 * {@code keys()} walks the buckets from the last to the first, the bucket count is mirrored from
 * {@code Hashtable}'s growth rule, and when two of the keys share a bucket the stock loop runs. A
 * JTree class that overrides {@code getDescendantToggledPaths} keeps a plain {@code Hashtable}
 * ({@link #accepts}). A mutation the index did not see (none in {@code JTree}, which only calls
 * {@code get}, {@code put}, {@code remove}, {@code clear} and {@code keys}) makes the key count
 * differ and hands every call back to the stock scan.
 * <p>
 * Precondition, as for the {@code Hashtable} itself: the last component of a path keeps its
 * {@code hashCode} and {@code equals} while the tree shows it. The index also holds the ancestors
 * of the keys, so a component whose hash changes while only its descendants are keys can make
 * {@link #expandedKeysBelow} miss a key that the stock loop, which uses only {@code equals},
 * finds. Design and proof: m3/docs/swing-viewport/DESIGN.md section 11.
 */
//FLLDCIM: M3 swing-viewport leaf 5 - JTree expansion table indexed by parent, same table and order
@SuppressWarnings("serial") // transient in JTree, never serialized
public final class M3ExpandedState extends Hashtable<TreePath, Boolean> {

    /** Whether {@code JTree} creates this table; read once from {@code m3.swing.viewport}. */
    public static final boolean ENABLED = GetBooleanAction.privilegedGetProperty("m3.swing.viewport");

    /** Whether a JTree class keeps the stock {@code getDescendantToggledPaths}. */
    private static final ClassValue<Boolean> STOCK_TOGGLED_PATHS = new ClassValue<>() {
        @Override
        @SuppressWarnings("removal")
        protected Boolean computeValue(Class<?> type) {
            return java.security.AccessController.doPrivileged(
                    (java.security.PrivilegedAction<Boolean>) () -> stockToggledPaths(type));
        }
    };

    private static boolean stockToggledPaths(Class<?> type) {
        for (Class<?> c = type; c != null && c != JTree.class; c = c.getSuperclass()) {
            try {
                c.getDeclaredMethod("getDescendantToggledPaths", TreePath.class);
                return false;
            } catch (NoSuchMethodException e) {
                // not overridden in c
            } catch (LinkageError e) {
                return false;
            }
        }
        return true;
    }

    /** A path of the trie: a key of the table ({@code toggled}, {@code path} is then the key instance) or an ancestor of one. */
    private static final class Node {
        TreePath path;
        final Node parent;
        final ArrayList<Node> children = new ArrayList<>(2);
        boolean toggled;
        int hash;
        int slot;

        Node(TreePath path, Node parent) {
            this.path = path;
            this.parent = parent;
        }
    }

    /** Hashtable's defaults ({@code new Hashtable()}: 11 buckets, load factor 0.75) and size limit. */
    private static final float LOAD_FACTOR = 0.75f;
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    private static final TreePath[] NONE = new TreePath[0];

    private final HashMap<TreePath, Node> nodes = new HashMap<>();
    private int toggled;
    private int capacity = 11;
    private int threshold = (int) Math.min(11 * LOAD_FACTOR, MAX_ARRAY_SIZE + 1);

    /** Creates an empty table. */
    public M3ExpandedState() {
    }

    /**
     * Whether a {@code JTree} of class {@code jtreeClass} gets this table: the property is set and the
     * class keeps the stock {@code getDescendantToggledPaths}, which the index replaces.
     */
    public static boolean accepts(Class<?> jtreeClass) {
        return ENABLED && STOCK_TOGGLED_PATHS.get(jtreeClass);
    }

    /** The number of buckets of the table, mirrored from {@code Hashtable} (for tests). */
    public synchronized int bucketCount() {
        return capacity;
    }

    @Override
    public synchronized Boolean put(TreePath key, Boolean value) {
        int before = size();
        Boolean old = super.put(key, value);
        if (old == null) {
            if (before >= threshold) {
                grow();
            }
            link(key);
        }
        return old;
    }

    /** The bucket count {@code Hashtable.rehash} gives a table of {@code capacity} buckets: 2n + 1, at most the array limit. */
    public static int nextCapacity(int capacity) {
        int next = (capacity << 1) + 1;
        if (next - MAX_ARRAY_SIZE > 0) {
            return capacity == MAX_ARRAY_SIZE ? capacity : MAX_ARRAY_SIZE;
        }
        return next;
    }

    /**
     * Mirrors {@code Hashtable.rehash}, which a put of a new key runs when the count reached the
     * threshold. At the array limit {@code rehash} changes nothing; the threshold computed here is
     * then the one it already has.
     */
    private void grow() {
        capacity = nextCapacity(capacity);
        threshold = (int) Math.min(capacity * LOAD_FACTOR, MAX_ARRAY_SIZE + 1);
    }

    @Override
    public synchronized Boolean remove(Object key) {
        Boolean old = super.remove(key);
        if (old != null) {
            unlink((TreePath) key);
        }
        return old;
    }

    @Override
    public synchronized void clear() {
        super.clear();
        nodes.clear();
        toggled = 0;
    }

    /** Whether the index saw every key (no view or iterator removal behind its back). */
    private boolean indexed() {
        return toggled == size();
    }

    /**
     * The keys that the stock {@code getExpandedDescendants} loop tests with {@code isVisible} for
     * {@code parent}, in the order of {@code keys()}: the keys equal to or below {@code parent}
     * whose value is {@code TRUE}, except {@code parent} itself by identity (the stock
     * {@code path != parent}). {@code keys()} walks the buckets from the last to the first, so the
     * keys are sorted by bucket; null when two of them share a bucket (their order in the bucket's
     * chain is not mirrored) or the index missed a mutation, and the caller runs the stock loop.
     */
    public synchronized TreePath[] expandedKeysBelow(TreePath parent) {
        if (!indexed()) {
            return null;
        }
        Node n = parent == null ? null : nodes.get(parent);
        if (n == null) {
            return NONE;
        }
        ArrayList<Node> found = new ArrayList<>();
        if (n.toggled && n.path != parent && Boolean.TRUE.equals(get(n.path))) {
            found.add(n);
        }
        ArrayList<Node> todo = new ArrayList<>(n.children);
        while (!todo.isEmpty()) {
            Node c = todo.remove(todo.size() - 1);
            if (c.toggled && Boolean.TRUE.equals(get(c.path))) {
                found.add(c);
            }
            todo.addAll(c.children);
        }
        int k = found.size();
        if (k == 0) {
            return NONE;
        }
        long[] order = new long[k];
        for (int i = 0; i < k; i++) {
            order[i] = (long) ((found.get(i).hash & 0x7FFFFFFF) % capacity) << 32 | i;
        }
        Arrays.sort(order);
        TreePath[] keys = new TreePath[k];
        for (int i = 0; i < k; i++) {
            long o = order[k - 1 - i];
            if (i > 0 && o >>> 32 == order[k - i] >>> 32) {
                return null;
            }
            keys[i] = found.get((int) o).path;
        }
        return keys;
    }

    /**
     * Removes {@code path} and every key below it, as the stock {@code removeDescendantToggledPaths}
     * does for one path; false (nothing removed) when the index cannot decide, so the caller runs
     * the stock scan.
     */
    public synchronized boolean removeDescendants(TreePath path) {
        if (!indexed()) {
            return false;
        }
        Node n = path == null ? null : nodes.get(path);
        if (n == null) {
            return true;
        }
        ArrayList<TreePath> keys = new ArrayList<>();
        ArrayList<Node> todo = new ArrayList<>();
        todo.add(n);
        while (!todo.isEmpty()) {
            Node c = todo.remove(todo.size() - 1);
            if (c.toggled) {
                keys.add(c.path);
            }
            todo.addAll(c.children);
        }
        for (TreePath k : keys) {
            remove(k);
        }
        return true;
    }

    private void link(TreePath key) {
        Node n = nodes.get(key);
        if (n == null) {
            n = new Node(key, attach(key.getParentPath()));
            nodes.put(key, n);
            if (n.parent != null) {
                n.slot = n.parent.children.size();
                n.parent.children.add(n);
            }
        }
        n.path = key;
        n.hash = key.hashCode();
        n.toggled = true;
        toggled++;
    }

    /** The node of {@code path}, created (untoggled) with its ancestors when missing. */
    private Node attach(TreePath path) {
        if (path == null) {
            return null;
        }
        Node n = nodes.get(path);
        if (n == null) {
            n = new Node(path, attach(path.getParentPath()));
            nodes.put(path, n);
            if (n.parent != null) {
                n.slot = n.parent.children.size();
                n.parent.children.add(n);
            }
        }
        return n;
    }

    private void unlink(TreePath key) {
        Node n = nodes.get(key);
        if (n == null || !n.toggled) {
            return;
        }
        n.toggled = false;
        toggled--;
        while (n != null && !n.toggled && n.children.isEmpty()) {
            nodes.remove(n.path);
            Node p = n.parent;
            if (p != null) {
                Node last = p.children.remove(p.children.size() - 1);
                if (last != n) {
                    p.children.set(n.slot, last);
                    last.slot = n.slot;
                }
            }
            n = p;
        }
    }
}
