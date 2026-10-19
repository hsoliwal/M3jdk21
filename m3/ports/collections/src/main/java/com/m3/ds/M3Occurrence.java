// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.structure.MIndexOccurrence}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/structure/MIndexOccurrence.java}
 * blob {@code d17fb5a311ff253b32ee992ed7ac5183162e70a9} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code da540d8f4ac20ed6cc8856199113ed35cd7effa1688b6f581a7ffb9ab042b111}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.structure.M3Occurrence}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S14}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: components are value lanes; owners store them column-wise,
 * never as per-entry objects.
 */
public record M3Occurrence(M3NodeRef node, long byteStart, int byteLength, int ordinal, int flags) {
    public static final int QUOTED = 1;

    public static final int COMMENT_ADJACENT = 1 << 1;

    public static final int EXPLICIT_TYPE = 1 << 2;

    public static final int REPRESENTATION_SIGNIFICANT = 1 << 3;

    public long byteEndExclusive() {
        throw new M3NotYetReceived("M3Occurrence#byteEndExclusive/0");
    }
}
