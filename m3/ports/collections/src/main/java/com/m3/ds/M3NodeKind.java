// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.structure.MIndexNodeKind}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/structure/MIndexNodeKind.java}
 * blob {@code 7ed25e23b85d7878de6dab8906e2414157628c00} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code a09acbce1f59fdf1ddc4cdde6389d999b2479c4661fccea7cd1dfb5a06a7df5d}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.structure.M3NodeKind}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S14}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: constants are encoded as ordinal bits inside owner state masks.
 */
public enum M3NodeKind {
    NULL,
    BOOLEAN,
    INT32,
    INT64,
    FLOAT32,
    FLOAT64,
    DECIMAL,
    TEXT,
    BINARY,
    TEMPORAL,
    TUPLE,
    SEQUENCE,
    MAP,
    TABLE,
    EDGE,
    REF,
    EVENT;

    public boolean scalar() {
        throw new M3NotYetReceived("M3NodeKind#scalar/0");
    }

    public boolean composition() {
        throw new M3NotYetReceived("M3NodeKind#composition/0");
    }

    public boolean hasChildren() {
        throw new M3NotYetReceived("M3NodeKind#hasChildren/0");
    }
}
