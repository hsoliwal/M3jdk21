// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.util;

import java.net.URI;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.collections.MIndexSource}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/collections/MIndexSource.java}
 * blob {@code 0abf0a45e1c1e9610b108a2b4f917ec490e2154e} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 6a69f572f06e250c2fd9ecc47100785aad2509832529ad5eadbb9dc37bffc675}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.collections.M3Source}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S04}.
 * Map family {@code COLLECTIONS}; naming-law family {@code GENERAL_COLLECTIONS}.
 *
 * <p>DOOP mapping: components are value lanes; owners store them column-wise,
 * never as per-entry objects.
 */
public record M3Source(String id, URI uri, String revision, String sha256, String license) {
}
