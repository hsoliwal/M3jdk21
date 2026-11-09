// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

/** Exact-stage outcome after numeric first-pass pruning. */
//FLLDCIM: RELOCATE - donor blob 01ddf6d2cb9b at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/qualification/object-signal-20261107/PROVENANCE.tsv); recipe m3/collections/qualification/object-signal-20261107/relocate.py v1
public record ObjectSignalExactResult(
        ObjectSignalFirstPassResult firstPass,
        ObjectSignalBitmap exactMatches,
        int exactChecks) {

    public int exactMatchCount() {
        return exactMatches.cardinality();
    }

    public int exactChecksAvoided() {
        return firstPass.totalObjects() - exactChecks;
    }
}
