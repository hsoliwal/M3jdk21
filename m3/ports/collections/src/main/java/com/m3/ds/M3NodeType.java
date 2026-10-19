// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.structure.MIndexNodeType}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/structure/MIndexNodeType.java}
 * blob {@code d155e2176edfbee57c0e368603227d3c5a09970f} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 6624bcbb60b64afebef2111f12f0d73ccd2699f5e9a85a39585ffc01c9b8a5a9}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.structure.M3NodeType}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S14}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: constants are encoded as ordinal bits inside owner state masks.
 */
public enum M3NodeType {
    NULL,
    BOOLEAN,
    INT,
    LONG,
    FLOAT,
    DOUBLE,
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

    public byte code() {
        throw new M3NotYetReceived("M3NodeType#code/0");
    }

    public boolean isAtom() {
        throw new M3NotYetReceived("M3NodeType#isAtom/0");
    }

    public boolean isComposition() {
        throw new M3NotYetReceived("M3NodeType#isComposition/0");
    }

    public static M3NodeType fromCode(byte code) {
        throw new M3NotYetReceived("M3NodeType#fromCode/1");
    }
}
