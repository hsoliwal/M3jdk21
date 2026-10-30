// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// SPDX-License-Identifier: Apache-2.0
package com.m3.llm.projection;

/** Sparse coordinate: unsigned UTF-8 fields, then descending nonnegative version. */
public record CellKey(String namespace, String row, String family, String qualifier, long version)
        implements Comparable<CellKey> {
    public CellKey {
        namespace = EntropyValues.text(namespace, 512, false);
        row = EntropyValues.text(row, 512, false);
        family = EntropyValues.text(family, 512, false);
        qualifier = EntropyValues.text(qualifier, 512, false);
        if (version < 0) {
            throw new IllegalArgumentException("NEGATIVE_VERSION");
        }
    }

    @Override
    public int compareTo(CellKey other) {
        int result = EntropyValues.compare(namespace, other.namespace);
        if (result == 0) result = EntropyValues.compare(row, other.row);
        if (result == 0) result = EntropyValues.compare(family, other.family);
        if (result == 0) result = EntropyValues.compare(qualifier, other.qualifier);
        return result == 0 ? Long.compare(other.version, version) : result;
    }

    String rowToken() {
        return EntropyValues.framed(namespace, row);
    }

    /** Stable provenance identity; it is not a claim of semantic equivalence. */
    public String identity() {
        return EntropyValues.framed(namespace, row, family, qualifier, Long.toString(version));
    }
}
