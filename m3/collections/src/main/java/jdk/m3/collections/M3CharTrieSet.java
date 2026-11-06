/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Char-sequence trie with nodes encoded as parallel primitive arrays.
 *
 * <p>Node 0 is the root. Each node stores char label, first-child index,
 * next-sibling index and one terminal byte; no node object or retained String is
 * allocated per prefix component.
 *
 * <p>Lineage: absorbed from Synexia {@code synexia-primitives/src/main/java/com/synexia/primitives/CharTrieSet.java}
 * blob {@code a2f9bcd5e71965b49178b24271ee307d1dd007b6} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_SYNEXIA_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3CharTrieSet {
    private char[] label;
    private int[] firstChild;
    private int[] nextSibling;
    private byte[] terminal;
    private int nodeCount = 1;
    private int size;

    public M3CharTrieSet() { this(16); }

    public M3CharTrieSet(int initialNodes) {
        if (initialNodes < 1) throw new IllegalArgumentException("initialNodes < 1");
        label = new char[initialNodes];
        firstChild = new int[initialNodes];
        nextSibling = new int[initialNodes];
        terminal = new byte[initialNodes];
        Arrays.fill(firstChild, -1);
        Arrays.fill(nextSibling, -1);
    }

    public int size() { return size; }
    public int nodeCount() { return nodeCount; }
    public boolean isEmpty() { return size == 0; }
    public long payloadBytes() {
        return (long) label.length * Character.BYTES
                + (long) firstChild.length * Integer.BYTES * 2L
                + terminal.length;
    }

    public boolean add(CharSequence value) {
        Objects.requireNonNull(value, "value");
        int node = 0;
        for (int i = 0; i < value.length(); i++) {
            node = childOrCreate(node, value.charAt(i));
        }
        if (terminal[node] != 0) return false;
        terminal[node] = 1;
        size++;
        return true;
    }

    public boolean contains(CharSequence value) {
        int node = findNode(value);
        return node >= 0 && terminal[node] != 0;
    }

    public boolean containsPrefix(CharSequence prefix) {
        return findNode(prefix) >= 0;
    }

    public void clear() {
        Arrays.fill(label, (char) 0);
        Arrays.fill(firstChild, -1);
        Arrays.fill(nextSibling, -1);
        Arrays.fill(terminal, (byte) 0);
        nodeCount = 1;
        size = 0;
    }

    private int findNode(CharSequence value) {
        Objects.requireNonNull(value, "value");
        int node = 0;
        for (int i = 0; i < value.length(); i++) {
            node = child(node, value.charAt(i));
            if (node < 0) return -1;
        }
        return node;
    }

    private int child(int parent, char target) {
        for (int node = firstChild[parent]; node >= 0; node = nextSibling[node]) {
            if (label[node] == target) return node;
        }
        return -1;
    }

    private int childOrCreate(int parent, char target) {
        int existing = child(parent, target);
        if (existing >= 0) return existing;
        ensure(nodeCount + 1);
        int node = nodeCount++;
        label[node] = target;
        firstChild[node] = -1;
        nextSibling[node] = firstChild[parent];
        firstChild[parent] = node;
        return node;
    }

    private void ensure(int required) {
        if (required <= label.length) return;
        int oldLength = label.length;
        int capacity = oldLength + (oldLength >>> 1) + 1;
        if (capacity < required) capacity = required;
        label = Arrays.copyOf(label, capacity);
        firstChild = Arrays.copyOf(firstChild, capacity);
        nextSibling = Arrays.copyOf(nextSibling, capacity);
        terminal = Arrays.copyOf(terminal, capacity);
        Arrays.fill(firstChild, oldLength, capacity, -1);
        Arrays.fill(nextSibling, oldLength, capacity, -1);
    }
}
