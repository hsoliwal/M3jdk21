/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Dynamic indexed disjoint components with no per-element Entry/Node objects.
 * One primitive parent-or-negative-component-size lane owns topology; the parallel
 * int state-mask lane belongs to individual positions, never to a pooled object.
 * Path compression changes physical offsets without changing logical membership.
 * Union by size bounds path depth; exact ties deterministically retain the lower
 * root index. No deletion, concurrency, durable handles or JDK Set equivalence is
 * promised. Position zero is an ordinary member, not a tombstone.
 */
public final class M3DisjointIndex {
    private static final int[] EMPTY = new int[0];
    private static final int MAX_CAPACITY = Integer.MAX_VALUE - 8;

    public enum Backend { JAVA, NATIVE_REQUIRED }

    private int[] parentOrSize;
    private int[] stateMasks;
    private int size;
    private int components;
    private long revision;

    public M3DisjointIndex() { this(0); }

    public M3DisjointIndex(int initialCapacity) {
        if (initialCapacity < 0 || initialCapacity > MAX_CAPACITY)
            throw new IllegalArgumentException("invalid initial capacity");
        parentOrSize = initialCapacity == 0 ? EMPTY : new int[initialCapacity];
        stateMasks = initialCapacity == 0 ? EMPTY : new int[initialCapacity];
    }

    public int size() { return size; }
    public int capacity() { return parentOrSize.length; }
    public int componentCount() { return components; }
    public long revision() { return revision; }

    /** Capacity changes do not change logical membership or revision. */
    public void ensureCapacity(int minimum) {
        if (minimum < 0 || minimum > MAX_CAPACITY)
            throw new IllegalArgumentException("invalid minimum capacity");
        if (minimum <= capacity()) return;
        long grown = capacity() == 0 ? 8L : (long) capacity() + (capacity() >>> 1) + 1L;
        int target = (int) Math.min(MAX_CAPACITY, Math.max((long) minimum, grown));
        // Allocate *both* new lanes before publishing either one. OOME leaves
        // the owner with its previous aligned, intact directory.
        int[] parents = Arrays.copyOf(parentOrSize, target);
        int[] states = Arrays.copyOf(stateMasks, target);
        parentOrSize = parents;
        stateMasks = states;
    }

    /** Adds one standalone indexed component; returns the new zero-based ID. */
    public int add(int initialStateMask) {
        if (size == MAX_CAPACITY) throw new OutOfMemoryError("component index capacity exhausted");
        ensureCapacity(size + 1);
        int id = size++;
        parentOrSize[id] = -1;
        stateMasks[id] = initialStateMask;
        components++;
        revision++;
        return id;
    }

    /** Finds the canonical component root; path compression is semantically invisible. */
    public int find(int index) {
        int cursor = Objects.checkIndex(index, size);
        while (parentOrSize[cursor] >= 0) cursor = parentOrSize[cursor];
        int root = cursor;
        cursor = index;
        while (parentOrSize[cursor] >= 0 && parentOrSize[cursor] != root) {
            int next = parentOrSize[cursor];
            parentOrSize[cursor] = root;
            cursor = next;
        }
        return root;
    }

    public int componentSize(int index) { return -parentOrSize[find(index)]; }

    public boolean connected(int left, int right) {
        Objects.checkIndex(left, size);
        Objects.checkIndex(right, size);
        return find(left) == find(right);
    }

    /** Validates both indices before any change. Only genuine merges advance revision. */
    public boolean union(int left, int right) {
        Objects.checkIndex(left, size);
        Objects.checkIndex(right, size);
        int rootA = find(left), rootB = find(right);
        if (rootA == rootB) return false;
        int sizeA = -parentOrSize[rootA], sizeB = -parentOrSize[rootB];
        if (sizeA < sizeB || (sizeA == sizeB && rootB < rootA)) {
            int exchange = rootA; rootA = rootB; rootB = exchange;
        }
        parentOrSize[rootA] += parentOrSize[rootB];
        parentOrSize[rootB] = rootA;
        components--;
        revision++;
        return true;
    }

    /** Caller-defined full 32-bit flags; clear wins on overlapping set/clear. */
    public int updateState(int index, int setMask, int clearMask) {
        int id = Objects.checkIndex(index, size);
        int old = stateMasks[id];
        int next = (old | setMask) & ~clearMask;
        if (old != next) { stateMasks[id] = next; revision++; }
        return old;
    }

    public int stateAt(int index) { return stateMasks[Objects.checkIndex(index, size)]; }

    /** Detached, ascending root IDs filtered only by each root's own state flags.
     * A union does not propagate caller state from members to a chosen root.
     */
    public int[] selectedRoots(int required, int forbidden) {
        int kept = 0;
        for (int i = 0; i < size; i++) if (accept(i, required, forbidden)) kept++;
        int[] result = new int[kept];
        int put = 0;
        for (int i = 0; i < size; i++) if (accept(i, required, forbidden)) result[put++] = i;
        return result;
    }

    private boolean accept(int i, int required, int forbidden) {
        int bits = stateMasks[i];
        return parentOrSize[i] < 0 && (bits & required) == required
                && (bits & forbidden) == 0;
    }

    /** Read-only, detached batch connectivity. JNI is explicit and never invoked by JAVA.
     * The native path uses bounded primitive Region operations and retains no JVM pointer;
     * it is a parity backend, not a claimed throughput improvement.
     */
    public boolean[] connectedBatch(int[] left, int[] right, Backend backend) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.requireNonNull(backend, "backend");
        if (left.length != right.length) throw new IllegalArgumentException("unaligned query lanes");
        for (int i = 0; i < left.length; i++) {
            Objects.checkIndex(left[i], size);
            Objects.checkIndex(right[i], size);
        }
        if (backend == Backend.NATIVE_REQUIRED) {
            if (!NativeLibrary.AVAILABLE)
                throw new UnsatisfiedLinkError("m3_disjoint_index JNI unavailable");
            return connectedNative(parentOrSize, size, left, right);
        }
        boolean[] results = new boolean[left.length];
        for (int i = 0; i < left.length; i++) {
            results[i] = rootReadonly(left[i]) == rootReadonly(right[i]);
        }
        return results;
    }

    private int rootReadonly(int id) {
        while (parentOrSize[id] >= 0) id = parentOrSize[id];
        return id;
    }

    public static boolean nativeAvailable() { return NativeLibrary.AVAILABLE; }

    private static final class NativeLibrary {
        static final boolean AVAILABLE;
        static {
            boolean loaded;
            try { System.loadLibrary("m3_disjoint_index"); loaded = true; }
            catch (UnsatisfiedLinkError | SecurityException absent) { loaded = false; }
            AVAILABLE = loaded;
        }
    }

    private static native boolean[] connectedNative(int[] parentOrSize, int logicalSize,
            int[] left, int[] right);
}
