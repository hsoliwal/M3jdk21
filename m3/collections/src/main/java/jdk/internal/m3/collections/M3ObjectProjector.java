// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import jdk.internal.m3.collections.M3PoolProgressMonitor;

/** Explicit boundary from a rich object to its numeric first-pass representation. */
@FunctionalInterface
//FLLDCIM: RELOCATE - donor blob 4c13e3521c48 at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/qualification/object-signal-20261107/PROVENANCE.tsv); recipe m3/collections/qualification/object-signal-20261107/relocate.py v1
public interface M3ObjectProjector<T> {
    M3ObjectSignal project(T value, M3PoolProgressMonitor monitor);
}
