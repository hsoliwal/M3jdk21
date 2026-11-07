// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

/** Boundary representation used to derive a numeric first-pass signal. */
//FLLDCIM: RELOCATE - donor blob 05a70310817d at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/qualification/object-signal-20261107/PROVENANCE.tsv); recipe m3/collections/qualification/object-signal-20261107/relocate.py v1
public enum M3ObjectProjectionKind {
    STRING,
    INDEX,
    CANONICAL_BYTES,
    SIGNAL
}
