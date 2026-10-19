// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.structure.MIndexSourceFormat}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/structure/MIndexSourceFormat.java}
 * blob {@code f2a64212a7e2a06e300979d50b8602d9a6dd2831} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 2f24063fb7098919a9548296a65c5f24f97997bdedb66d36df2941a99309d865}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.structure.M3SourceFormat}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S14}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: constants are encoded as ordinal bits inside owner state masks.
 */
public enum M3SourceFormat {
    JSON,
    YAML,
    TOML,
    JSON_LD,
    BSON,
    CSV,
    TSV,
    LOG,
    PROPERTIES,
    XML,
    AST,
    EVENT_STREAM,
    BINARY,
    UNKNOWN,
    HTML,
    MARKDOWN,
    INI;
}
