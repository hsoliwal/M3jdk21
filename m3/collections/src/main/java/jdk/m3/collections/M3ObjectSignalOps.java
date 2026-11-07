// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.util.Objects;

/** Java kernel of the bulk transposed feature-plane pass (the donor's optional JNI accelerator is a HELD row of this owner's recipe). */
//FLLDCIM: RELOCATE - donor blob a4663c8f8ca1 at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/qualification/object-signal-20261107/PROVENANCE.tsv); recipe m3/collections/qualification/object-signal-20261107/relocate.py v1
final class M3ObjectSignalOps {
    private M3ObjectSignalOps() { }

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

}
