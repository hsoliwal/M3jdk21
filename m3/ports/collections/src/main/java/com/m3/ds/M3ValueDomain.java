// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.graph.MIndexValueDomain}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/graph/MIndexValueDomain.java}
 * blob {@code c973f9e436c91e443ce437a736833be5d1f81d44} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 7da3c65a0dc11f5661b91ca7689fea8046eb0aeae7f85843964825a5ec75a4dd}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.graph.M3ValueDomain}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S11}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: constants are encoded as ordinal bits inside owner state masks.
 */
public enum M3ValueDomain {
    TEXT,
    AST_ATOM,
    DAG,
    DAG_NODE,
    DAG_EDGE,
    INTERACTION;

    public int code() {
        throw new M3NotYetReceived("M3ValueDomain#code/0");
    }

    public static M3ValueDomain fromCode(int code) {
        throw new M3NotYetReceived("M3ValueDomain#fromCode/1");
    }
}
