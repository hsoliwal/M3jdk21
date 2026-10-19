// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;
import java.nio.file.Path;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.resource.MIndexResourceFilter}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/resource/src/main/java/com/synexia/mindex/resource/MIndexResourceFilter.java}
 * blob {@code 00268ed7db78321a4a5d4896fd38b3ddbf56b0d7} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code a43a2c50223fb23dc9e435433cc19bc52cb77eb53a64e15e077c02c6ff5869ee}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.resource.M3ResourceFilter}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S12}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: contract only; implementations keep data in growable primitive
 * lanes and per-slot state bitmasks, never in stored entry or node objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code constant ALL (M3ResourceFilter)}</li>
 *   <li>{@code member include(Path, MIndexResourceKind) (parameter kind)}</li>
 * </ul>
 */
public interface M3ResourceFilter {
    static M3ResourceFilter all() {
        throw new M3NotYetReceived("M3ResourceFilter#all/0");
    }

    static M3ResourceFilter namedTokens(String expression) {
        throw new M3NotYetReceived("M3ResourceFilter#namedTokens/1");
    }
}
