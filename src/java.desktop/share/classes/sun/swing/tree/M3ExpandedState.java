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
 * {@link #hasExpandedDescendant} only decides that the stock scan would find nothing; when it would
 * find something, the stock scan runs, so the enumeration order stays the table's. Any mutation the
 * index did not see (a view or iterator removal) is detected by the count check and hands every
 * call back to the stock scan. Design and proof: m3/docs/swing-viewport/DESIGN.md section 11.
 */
//FLLDCIM: M3 swing-viewport leaf 5 - JTree expansion table indexed by parent, same table and order
@SuppressWarnings("serial") // transient in JTree, never serialized
public final class M3ExpandedState extends Hashtable<TreePath, Boolean> {

    /** Whether {@code JTree} creates this table; read once from {@code m3.swing.viewport}. */
    public static final boolean ENABLED = GetBooleanAction.privilegedGetProperty("m3.swing.viewport");

    /** Whether a JTree class keeps the stock {@code getDescendantToggledPaths}. */
    private static final ClassValue<Boolean> STOCK_TOGGLED_PATHS = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            for (Class<?> c = type; c != null && c != JTree.class; c = c.getSuperclass()) {
                try {
                    c.getDeclaredMethod("getDescendantToggledPaths", TreePath.class);
                    return Boolean.FALSE;
                } catch (NoSuchMethodException e) {
                    // not overridden here
                }
            }
            return Boolean.TRUE;
        }
    };

    /** A path of the trie: a key of the table ({@code toggled}, {@code path} is then the key instance) or an ancestor of one. */
    private static final class Node {
        TreePath path;
        final Node parent;
        final ArrayList<Node> children = new ArrayList<>(2);
        boolean toggled;
        int slot;

        Node(TreePath path, Node parent) {
            this.path = path;
            this.parent = parent;
        }
    }

    private final HashMap<TreePath, Node> nodes = new HashMap<>();
    private int toggled;

    /** Creates an empty table. */
    public M3ExpandedState() {
    }

    /** Whether {@code jtreeClass} keeps the stock {@code getDescendantToggledPaths}. */
    public static boolean stockToggledPaths(Class<?> jtreeClass) {
        return STOCK_TOGGLED_PATHS.get(jtreeClass);
    }

    @Override
    public synchronized Boolean put(TreePath key, Boolean value) {
        Boolean old = super.put(key, value);
        if (old == null) {
            link(key);
        }
        return old;
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
     * Whether the stock {@code getExpandedDescendants} scan could find a path: an expanded key in
     * the subtree of {@code parent} other than {@code parent} itself (an equal key is counted when it
     * is a different instance, as the stock {@code path != parent} test does). Answers true when the
     * index cannot decide.
     */
    public synchronized boolean hasExpandedDescendant(TreePath parent) {
        Node n = parent == null ? null : nodes.get(parent);
        if (!indexed()) {
            return true;
        }
        if (n == null) {
            return false;
        }
        if (n.toggled && n.path != parent && Boolean.TRUE.equals(get(n.path))) {
            return true;
        }
        ArrayList<Node> todo = new ArrayList<>(n.children);
        while (!todo.isEmpty()) {
            Node c = todo.remove(todo.size() - 1);
            if (c.toggled && Boolean.TRUE.equals(get(c.path))) {
                return true;
            }
            todo.addAll(c.children);
        }
        return false;
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
