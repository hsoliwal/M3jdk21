// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.recall.MIndexRecallFilter}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/recall/MIndexRecallFilter.java}
 * blob {@code 5706fe5cf2c63b9eb649d4f637b249555a0895d4} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 07a845690ac1b226354a6e986c335834fca132500a02d6459a0e9f086e42db9e}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.recall.M3RecallFilter}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S14}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: contract only; implementations keep data in growable primitive
 * lanes and per-slot state bitmasks, never in stored entry or node objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code constant JAVA (M3RecallFilter)}</li>
 *   <li>{@code member filter(long[], long[], long[], long, long, boolean, byte[], IProgressMonitor) (parameter monitor)}</li>
 * </ul>
 */
public interface M3RecallFilter {
    static void validate(long[] required, long[] forbidden, long[] shapes, byte[] output) {
        throw new M3NotYetReceived("M3RecallFilter#validate/4");
    }
}
