// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.graph.MIndexRole}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/graph/MIndexRole.java}
 * blob {@code c6ad22041e0f70153b476a4674957191f9581a5f} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code b30de6f940dc5567782470ca44e7b32514f93c7cb44558a91d7f34f2015572d0}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.graph.M3Role}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S09}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: constants are encoded as ordinal bits inside owner state masks.
 */
public enum M3Role {
    TEXT,
    STATEMENT,
    FACT,
    REASON,
    CONTEXT,
    LOGIC,
    AST,
    DAG,
    ACTION;

    public int code() {
        throw new M3NotYetReceived("M3Role#code/0");
    }

    public int mask() {
        throw new M3NotYetReceived("M3Role#mask/0");
    }

    public static M3Role fromCode(int code) {
        throw new M3NotYetReceived("M3Role#fromCode/1");
    }
}
