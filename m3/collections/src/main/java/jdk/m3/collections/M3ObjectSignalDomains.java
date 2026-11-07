// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

/** Reserved semantic domains for built-in projections. Custom domains should start at CUSTOM_BASE. */
//FLLDCIM: RELOCATE - donor blob eca8021ef308 at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/qualification/object-signal-20261107/PROVENANCE.tsv); recipe m3/collections/qualification/object-signal-20261107/relocate.py v1
public final class M3ObjectSignalDomains {
    public static final int STRING = 1;
    public static final int INDEX_STRING_BASE = 0x0001_0000;
    public static final int IMMUTABLE_ID_BASE = 0x0002_0000;
    public static final int CANONICAL_BYTES = 0x0003_0000;
    public static final int CUSTOM_BASE = 0x0010_0000;

    private M3ObjectSignalDomains() { }
}
