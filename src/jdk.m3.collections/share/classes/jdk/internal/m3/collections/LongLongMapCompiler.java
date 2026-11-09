/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import jdk.internal.m3.collections.packed.PackedHashPolicy;

import java.util.Objects;

/**
 * Deterministic representation compiler for primitive long-to-long maps.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/collections/LongLongMapCompiler.java}
 * blob {@code 948314a3970a966d9a91169ef9cf2aaedb2bb9fa} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (algorithms.collections v6 batch (no-sprawl): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class LongLongMapCompiler {
    private static final int DENSE_SORTED_SMALL = 256;
    private static final int PAGED_SORTED_LARGE = 4_096;

    private LongLongMapCompiler() {}

    public static LongLongMapPlan compile(LongLongMapSpec spec) {
        Objects.requireNonNull(spec, "spec");

        if (spec.forcedRepresentation() != null) {
            if (spec.forcedRepresentation().order() != spec.order()) {
                throw new IllegalArgumentException(
                        "forced representation does not satisfy requested order");
            }
            return new LongLongMapPlan(spec, spec.forcedRepresentation(), "MAP-FORCED");
        }

        return switch (spec.order()) {
            case SORTED -> compileSorted(spec);
            case INSERTION -> new LongLongMapPlan(
                    spec, LongLongMapRepresentation.INSERTION_ORDERED, "MAP-ORDER-INSERTION");
            case ACCESS -> new LongLongMapPlan(
                    spec, LongLongMapRepresentation.ACCESS_ORDERED, "MAP-ORDER-ACCESS");
            case UNORDERED -> compileUnordered(spec);
        };
    }

    private static LongLongMapPlan compileSorted(LongLongMapSpec spec) {
        if (spec.mutation() == CollectionMutation.READ_MOSTLY
                || spec.expectedSize() <= DENSE_SORTED_SMALL) {
            return new LongLongMapPlan(
                    spec, LongLongMapRepresentation.DENSE_SORTED, "MAP-SORT-DENSE");
        }
        if (spec.expectedSize() >= PAGED_SORTED_LARGE
                || spec.mutation() == CollectionMutation.WRITE_HEAVY
                || spec.mutation() == CollectionMutation.BATCHED
                || spec.mutation() == CollectionMutation.DELETION_HEAVY) {
            return new LongLongMapPlan(
                    spec, LongLongMapRepresentation.PAGED_SORTED, "MAP-SORT-PAGED");
        }
        return new LongLongMapPlan(
                spec, LongLongMapRepresentation.DENSE_SORTED, "MAP-SORT-BALANCED");
    }

    private static LongLongMapPlan compileUnordered(LongLongMapSpec spec) {
        if (spec.hashPolicy() != PackedHashPolicy.AUTO) {
            return new LongLongMapPlan(
                    spec, fromHashPolicy(spec.hashPolicy()), "MAP-HASH-EXPLICIT");
        }
        return switch (spec.mutation()) {
            case READ_MOSTLY -> new LongLongMapPlan(
                    spec, LongLongMapRepresentation.SWISS_HASH, "MAP-HASH-READ");
            case DELETION_HEAVY -> new LongLongMapPlan(
                    spec, LongLongMapRepresentation.ROBIN_HOOD_HASH, "MAP-HASH-DELETE");
            case BALANCED, WRITE_HEAVY, BATCHED -> new LongLongMapPlan(
                    spec, LongLongMapRepresentation.LINEAR_HASH, "MAP-HASH-BASELINE");
        };
    }

    private static LongLongMapRepresentation fromHashPolicy(PackedHashPolicy policy) {
        return switch (policy) {
            case LINEAR -> LongLongMapRepresentation.LINEAR_HASH;
            case ROBIN_HOOD -> LongLongMapRepresentation.ROBIN_HOOD_HASH;
            case SWISS -> LongLongMapRepresentation.SWISS_HASH;
            case AUTO -> throw new IllegalArgumentException("AUTO has no concrete representation");
        };
    }
}
