/*
 * SPDX-FileCopyrightText: 2026 Synexia contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.tooling.donors;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Sorted committed Git tree metadata held exclusively in parallel string lanes and
 * primitive index arrays. No per-file Entry/Node/Map.Entry records are retained.
 * Sorting uses Java String.compareTo, preserving the former TreeMap path ordering.
 */
final class DonorTreeLanes {
    private String[] paths = new String[256];
    private String[] modes = new String[256];
    private String[] types = new String[256];
    private String[] objects = new String[256];
    private int[] order;
    private int size;

    void add(String path, String mode, String type, String object) {
        if (order != null) {
            throw new IllegalStateException("frozen source lanes");
        }
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(object, "object");
        if (size == paths.length) {
            if (size > Integer.MAX_VALUE / 2) {
                throw new IllegalArgumentException("Git tree lane budget exceeded");
            }
            int next = size * 2;
            // Allocate all four lanes before publishing any new capacity.
            // OutOfMemoryError must leave the old aligned lanes usable.
            String[] expandedPaths = Arrays.copyOf(paths, next);
            String[] expandedModes = Arrays.copyOf(modes, next);
            String[] expandedTypes = Arrays.copyOf(types, next);
            String[] expandedObjects = Arrays.copyOf(objects, next);
            paths = expandedPaths;
            modes = expandedModes;
            types = expandedTypes;
            objects = expandedObjects;
        }
        paths[size] = path;
        modes[size] = mode;
        types[size] = type;
        objects[size] = object;
        ++size;
    }

    /** Stable, iterative, allocation-bounded primitive ordinal merge sort. */
    void freeze() throws IOException {
        if (order != null) {
            throw new IllegalStateException("already frozen source lanes");
        }
        int[] sorted = new int[size];
        boolean alreadyOrdered = true;
        for (int i = 0; i < size; i++) {
            sorted[i] = i;
            if (i > 0 && paths[i - 1].compareTo(paths[i]) >= 0) {
                alreadyOrdered = false;
            }
        }
        // Git's committed tree is ordinarily sorted already. Preserve the exact
        // Java UTF-16 String comparator: non-ASCII Git byte-order differences,
        // reverse paths and duplicate names still require the normal sort.
        if (alreadyOrdered) {
            order = sorted;
            return;
        }
        int[] spare = new int[size];
        for (long width = 1; width < size; width <<= 1) {
            for (long start = 0; start < size; start += width << 1) {
                int lo = (int) start;
                int mid = (int) Math.min((long) size, start + width);
                int hi = (int) Math.min((long) size, start + (width << 1));
                int left = lo;
                int right = mid;
                for (int p = lo; p < hi; p++) {
                    if (right == hi || (left < mid
                            && paths[sorted[left]].compareTo(paths[sorted[right]]) <= 0)) {
                        spare[p] = sorted[left++];
                    } else {
                        spare[p] = sorted[right++];
                    }
                }
            }
            int[] swap = sorted;
            sorted = spare;
            spare = swap;
        }
        for (int i = 1; i < size; i++) {
            if (paths[sorted[i - 1]].equals(paths[sorted[i]])) {
                throw new IOException("duplicate Git tree path " + paths[sorted[i]]);
            }
        }
        order = sorted;
    }

    int size() {
        requireFrozen();
        return size;
    }

    String path(int at) {
        return paths[index(at)];
    }

    String mode(int at) {
        return modes[index(at)];
    }

    String type(int at) {
        return types[index(at)];
    }

    String objectId(int at) {
        return objects[index(at)];
    }

    boolean same(int left, DonorTreeLanes other, int right) {
        return mode(left).equals(other.mode(right))
                && type(left).equals(other.type(right))
                && objectId(left).equals(other.objectId(right));
    }

    private int index(int at) {
        requireFrozen();
        return order[at];
    }

    private void requireFrozen() {
        if (order == null) {
            throw new IllegalStateException("unfrozen source lanes");
        }
    }
}
