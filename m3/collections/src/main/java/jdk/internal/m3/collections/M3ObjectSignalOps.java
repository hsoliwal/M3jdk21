// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.util.Objects;

/** Optional JNI accelerator for the bulk transposed feature-plane pass. */
//FLLDCIM: RELOCATE - donor blob a4663c8f8ca1 at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/qualification/object-signal-20261107/PROVENANCE.tsv); recipe m3/collections/qualification/object-signal-20261107/relocate.py v1
final class M3ObjectSignalOps {
    private static final int JNI_WORD_THRESHOLD = 256;
    private static final boolean AVAILABLE;

    static {
        boolean loaded = false;
        if (!Boolean.getBoolean("m3.objectsignal.disableNative")) {
            try {
                System.loadLibrary("m3signalorder");
                loaded = true;
            } catch (UnsatisfiedLinkError ignored) {
                loaded = false;
            }
        }
        AVAILABLE = loaded;
    }

    private M3ObjectSignalOps() { }

    static boolean available() {
        return AVAILABLE;
    }

    static void applyFeatureMasks(
            long[] featurePlanes,
            int wordCount,
            long requiredBits,
            long forbiddenBits,
            long[] selected) {
        Objects.requireNonNull(featurePlanes, "featurePlanes");
        Objects.requireNonNull(selected, "selected");
        if (wordCount < 0
                || featurePlanes.length != Math.multiplyExact(Long.SIZE, wordCount)
                || selected.length != wordCount) {
            throw new IllegalArgumentException("feature plane shape mismatch");
        }
        if (AVAILABLE && wordCount >= JNI_WORD_THRESHOLD) {
            applyFeatureMasksNative(
                featurePlanes, wordCount, requiredBits, forbiddenBits, selected);
            return;
        }
        applyFeatureMasksJava(
            featurePlanes, wordCount, requiredBits, forbiddenBits, selected);
    }

    static void applyFeatureMasksJava(
            long[] featurePlanes,
            int wordCount,
            long requiredBits,
            long forbiddenBits,
            long[] selected) {
        long required = requiredBits;
        while (required != 0L) {
            int feature = Long.numberOfTrailingZeros(required);
            int base = feature * wordCount;
            for (int word = 0; word < wordCount; word++) {
                selected[word] &= featurePlanes[base + word];
            }
            required &= required - 1L;
        }

        long forbidden = forbiddenBits;
        while (forbidden != 0L) {
            int feature = Long.numberOfTrailingZeros(forbidden);
            int base = feature * wordCount;
            for (int word = 0; word < wordCount; word++) {
                selected[word] &= ~featurePlanes[base + word];
            }
            forbidden &= forbidden - 1L;
        }
    }

    private static native void applyFeatureMasksNative(
        long[] featurePlanes,
        int wordCount,
        long requiredBits,
        long forbiddenBits,
        long[] selected);
}
