// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.util.concurrent.CancellationException;

/** Progress/cancellation contract for non-constant-time pool operations. */
//FLLDCIM: RELOCATE - donor blob 6328187b7bf3 at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/recipe/immutable-pools-20261106/PROVENANCE.tsv); recipe m3/collections/recipe/immutable-pools-20261106/relocate.py v2
public interface M3PoolProgressMonitor {
    long UNKNOWN = -1L;
    default void begin(String task, long totalWork) { }
    default void worked(long work) { }
    default boolean cancelled() { return false; }
    default void done() { }

    default void checkCancelled() {
        if (cancelled()) throw new CancellationException("immutable-pool operation cancelled");
    }

    static M3PoolProgressMonitor none() {
        return None.INSTANCE;
    }

    enum None implements M3PoolProgressMonitor {
        INSTANCE
    }
}
