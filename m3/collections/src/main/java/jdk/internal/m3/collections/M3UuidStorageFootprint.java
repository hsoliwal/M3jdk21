// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

/** Pure payload accounting for UUID text vs binary representations. */
//FLLDCIM: RELOCATE - donor blob 182523d90b0c at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/recipe/immutable-pools-20261106/PROVENANCE.tsv); recipe m3/collections/recipe/immutable-pools-20261106/relocate.py v2
public record M3UuidStorageFootprint(
        long occurrences,
        long binaryBytes,
        long canonicalAsciiBytes,
        long savedBytes,
        double textToBinaryRatio) {

    public static M3UuidStorageFootprint forOccurrences(long occurrences) {
        if (occurrences < 0L) throw new IllegalArgumentException("negative occurrences");
        long binary = Math.multiplyExact(occurrences, M3Uuid128.BINARY_BYTES);
        long text = Math.multiplyExact(occurrences, M3Uuid128.TEXT_CHARS);
        return new M3UuidStorageFootprint(
            occurrences,
            binary,
            text,
            text - binary,
            binary == 0L ? 0.0d : (double) text / (double) binary);
    }
}
