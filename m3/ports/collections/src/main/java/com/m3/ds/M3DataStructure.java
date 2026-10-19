// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.structure.MIndexDataStructure}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/data-structure/src/main/java/com/synexia/mindex/structure/MIndexDataStructure.java}
 * blob {@code f0d882437ec23646926d97bbb27de25b45d29f04} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code 484029b4dff2cb788748fdf0b9985bc2be951a3fbd146aba161355a46df3db9f}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.structure.M3DataStructure}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S05}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: contract only; implementations keep data in growable primitive
 * lanes and per-slot state bitmasks, never in stored entry or node objects.
 *
 * <p>Not yet received:
 * <ul>
 *   <li>{@code source supertypes not received: extends MIndexEntity}</li>
 *   <li>{@code member keyAt(int) (return type)}</li>
 *   <li>{@code member name() (return type)}</li>
 *   <li>{@code member kind() (return type)}</li>
 * </ul>
 */
public interface M3DataStructure {
    int size();

    long valueAt(int index);

    default boolean isEmpty() {
        throw new M3NotYetReceived("M3DataStructure#isEmpty/0");
    }

    default long capabilityMask() {
        throw new M3NotYetReceived("M3DataStructure#capabilityMask/0");
    }

    default long structuralHash64() {
        throw new M3NotYetReceived("M3DataStructure#structuralHash64/0");
    }
}
