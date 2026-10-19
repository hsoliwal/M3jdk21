// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ds;

import com.m3.util.M3NotYetReceived;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.collections.MIndexSpace}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/collections/src/main/java/com/synexia/mindex/collections/MIndexSpace.java}
 * blob {@code 47454831a61d7c236d4c845d728dda6d3b1b15ce} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code bcad657f14e6eb9484854fcfe6a1b552573d5e40257eaa38d92d17be86ea1332}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.collections.M3Space}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S05}.
 * Map family {@code DATA_STRUCTURE}; naming-law family {@code SPECIALIZED_DATA_STRUCTURES}.
 *
 * <p>DOOP mapping: contract only; implementations keep data in growable primitive
 * lanes and per-slot state bitmasks, never in stored entry or node objects.
 */
public interface M3Space<E> {
    int id(E value);

    int findId(E value);

    E value(int id);

    int size();

    default boolean javaEqualityCompatible() {
        throw new M3NotYetReceived("M3Space#javaEqualityCompatible/0");
    }

    default Object identityAuthority() {
        throw new M3NotYetReceived("M3Space#identityAuthority/0");
    }

    default boolean contains(E value) {
        throw new M3NotYetReceived("M3Space#contains/1");
    }

    default boolean containsId(int id) {
        throw new M3NotYetReceived("M3Space#containsId/1");
    }

    default void requireId(int id) {
        throw new M3NotYetReceived("M3Space#requireId/1");
    }
}
