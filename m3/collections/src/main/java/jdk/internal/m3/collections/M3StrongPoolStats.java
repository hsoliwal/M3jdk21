// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

/** Allocation-free snapshot of live strong-pool reuse and pressure. */
//FLLDCIM: RELOCATE - donor blob cbe91f8405d8 at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/recipe/immutable-pools-20261106/PROVENANCE.tsv); recipe m3/collections/recipe/immutable-pools-20261106/relocate.py v2
public record M3StrongPoolStats(
        long hits,
        long inserts,
        long overflows,
        int entries,
        int capacity) {

    public long requests() {
        return hits + inserts + overflows;
    }

    public double reuseRatio() {
        long total = hits + inserts;
        return total == 0L ? 0.0d : (double) hits / (double) total;
    }
}
