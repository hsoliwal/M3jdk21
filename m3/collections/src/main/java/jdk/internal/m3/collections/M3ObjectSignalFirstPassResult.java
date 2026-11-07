// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

/** Measured pruning outcome of the numeric/bitwise first stage. */
//FLLDCIM: RELOCATE - donor blob 0b7ebc54f14b at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/qualification/object-signal-20261107/PROVENANCE.tsv); recipe m3/collections/qualification/object-signal-20261107/relocate.py v1
public record M3ObjectSignalFirstPassResult(
        M3ObjectSignalBitmap candidates,
        int totalObjects,
        int afterBitwiseSignals,
        int afterMetadata) {

    public int rejectedByBitwiseSignals() {
        return totalObjects - afterBitwiseSignals;
    }

    public int rejectedByMetadata() {
        return afterBitwiseSignals - afterMetadata;
    }

    public double survivorRatio() {
        return totalObjects == 0 ? 0.0d : (double) afterMetadata / totalObjects;
    }
}
