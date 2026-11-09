// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import java.util.Arrays;
import java.util.Objects;

/**
 * Optional array-only accelerator for verified, sorted source-image names.
 * The canonical sorted names remain authoritative. All unresolved probes
 * fall back to the original exact binary search, including adversarial hashes.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/src/main/java/com/synexia/m3/pmem/PmemSourcePathIndex.java blob f26a4f709e52; recipe m3/optane/recipe/pmem-source-image-20261106/relocate.py v3
final class PmemSourcePathIndex {
    private static final int MIN_FILES = 64;
    private static final int MAX_PROBES = 16;
    private static final int[] NO_INDEX = new int[0];

    private PmemSourcePathIndex() {}

    /** Nonzero slots are one-based ordinals; zero is empty, never a path. */
    static int[] build(String[] sortedPaths) {
        Objects.requireNonNull(sortedPaths, "sortedPaths");
        int count = sortedPaths.length;
        if (count < MIN_FILES || count > (Integer.MAX_VALUE >>> 1)) return NO_INDEX;
        int wanted = count << 1;
        int capacity = 1;
        while (capacity < wanted) capacity <<= 1;
        int[] slots = new int[capacity];
        int mask = capacity - 1;
        for (int ordinal = 0; ordinal < count; ordinal++) {
            int slot = mix(sortedPaths[ordinal].hashCode()) & mask;
            int probes = 0;
            while (probes < MAX_PROBES && slots[slot] != 0) {
                slot = (slot + 1) & mask;
                probes++;
            }
            if (probes == MAX_PROBES || slots[slot] != 0) return NO_INDEX;
            slots[slot] = ordinal + 1;
        }
        return slots;
    }

    /** Same result and exceptions as Arrays.binarySearch normalized to -1. */
    static int find(String[] sortedPaths, int[] slots, String path) {
        Objects.requireNonNull(path, "relativePath");
        if (slots.length != 0) {
            int mask = slots.length - 1;
            int slot = mix(path.hashCode()) & mask;
            for (int probe = 0; probe < MAX_PROBES; probe++) {
                int ordinal = slots[slot];
                if (ordinal == 0) return -1;
                if (sortedPaths[ordinal - 1].equals(path)) return ordinal - 1;
                slot = (slot + 1) & mask;
            }
        }
        int binary = Arrays.binarySearch(sortedPaths, path);
        return binary < 0 ? -1 : binary;
    }

    private static int mix(int hash) {
        hash ^= hash >>> 16;
        hash *= 0x7feb352d;
        hash ^= hash >>> 15;
        hash *= 0x846ca68b;
        return hash ^ (hash >>> 16);
    }
}
