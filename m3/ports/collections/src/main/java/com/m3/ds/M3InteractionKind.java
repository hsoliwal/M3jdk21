// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.graph.MIndexInteractionKind}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/graph/MIndexInteractionKind.java}
 * blob {@code 42143c2a9d264d620e16f597f97cedaa92af04f1} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code c46a983872716762eebb9981655c12812f8ea62399c6b12a259904cd6f99fe25}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.graph.M3InteractionKind}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S08}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: constants are encoded as ordinal bits inside owner state masks.
 */
public enum M3InteractionKind {
    UNKNOWN,
    IDENTITY,
    LOGIC,
    ARGUMENT,
    EVIDENCE,
    REASON,
    DISCUSSION,
    BRAINSTORMING,
    COMPOSITION,
    DEPENDENCY,
    DATA_FLOW,
    SEQUENCE,
    SUPPORT,
    CONTRADICTION;

    public int code() {
        throw new M3NotYetReceived("M3InteractionKind#code/0");
    }

    public static M3InteractionKind fromCode(int code) {
        throw new M3NotYetReceived("M3InteractionKind#fromCode/1");
    }
}
