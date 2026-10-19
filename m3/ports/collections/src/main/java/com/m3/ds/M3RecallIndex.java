// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;
import java.util.List;

/**
 * M3 SDK scaffold of {@code com.synexia.indexstring.recall.MIndexRecallIndex}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-indexstring/src/main/java/com/synexia/indexstring/recall/MIndexRecallIndex.java}
 * blob {@code 8205579a199110833257ec03435493efb782c45a} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 7429d83db5b84783dc3d2b588b9ae4408e501c33896ef38f4bcbbd8241bdd466}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.indexstring.recall.M3RecallIndex}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S14}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: contract only; implementations keep data in growable primitive
 * lanes and per-slot state bitmasks, never in stored entry or node objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code member page(Cue, Context, long, int, IProgressMonitor) (return type)}</li>
 *   <li>{@code member applicable(Cue, Memory, Context, IProgressMonitor) (parameter cue)}</li>
 *   <li>{@code member followUps(Memory, Context, int, IProgressMonitor) (return type)}</li>
 * </ul>
 */
public interface M3RecallIndex {
    String snapshotId();
}
