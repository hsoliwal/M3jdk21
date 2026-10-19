// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.util;

/**
 * M3 SDK scaffold of {@code com.synexia.mindex.collection.MIndexCollectionSpec}: declarations received, atoms not yet received.
 * Every member body throws {@link M3NotYetReceived}.
 *
 * <p>Lineage: Synexia {@code synexia-mindex/runtime/src/main/java/com/synexia/mindex/collection/MIndexCollectionSpec.java}
 * blob {@code 91f97153d09051514169ef3b075caa264b25ed4f} at develop {@code 11c94feb18155a53cc213708c353791132edc4a7},
 * source SHA-256 {@code dba5592a5a3c1a65f6799ce12deb2d35a86db3611a666d937c86bd2fbd7670ec}.
 * Copy contract {@code com.synexia.rewrite.M3CopyMIndexDeclarationRecipe} (same-package
 * {@code com.synexia.mindex.collection.M3CollectionSpec}); projection
 * {@code com.synexia.rewrite.M3MIndexScaffoldSkeletonRecipe} batch {@code S02}.
 * Map family {@code COLLECTIONS}; naming-law family {@code GENERAL_COLLECTIONS}.
 *
 * <p>DOOP mapping: components are value lanes; owners store them column-wise,
 * never as per-entry objects.
 */
public record M3CollectionSpec(int expectedSize, int maximumIdentity, boolean ordered, boolean readMostly, boolean membershipHeavy, boolean preferDirectAddress) {
    public static M3CollectionSpec general(int expectedSize) {
        throw new M3NotYetReceived("M3CollectionSpec#general/1");
    }

    public static M3CollectionSpec ordered(int expectedSize) {
        throw new M3NotYetReceived("M3CollectionSpec#ordered/1");
    }

    public static M3CollectionSpec dense(int expectedSize, int maximumIdentity) {
        throw new M3NotYetReceived("M3CollectionSpec#dense/2");
    }

    public M3CollectionSpec withMaximumIdentity(int value) {
        throw new M3NotYetReceived("M3CollectionSpec#withMaximumIdentity/1");
    }

    public M3CollectionSpec withOrdered(boolean value) {
        throw new M3NotYetReceived("M3CollectionSpec#withOrdered/1");
    }

    public M3CollectionSpec withReadMostly(boolean value) {
        throw new M3NotYetReceived("M3CollectionSpec#withReadMostly/1");
    }

    public M3CollectionSpec withMembershipHeavy(boolean value) {
        throw new M3NotYetReceived("M3CollectionSpec#withMembershipHeavy/1");
    }

    public M3CollectionSpec withPreferDirectAddress(boolean value) {
        throw new M3NotYetReceived("M3CollectionSpec#withPreferDirectAddress/1");
    }
}
