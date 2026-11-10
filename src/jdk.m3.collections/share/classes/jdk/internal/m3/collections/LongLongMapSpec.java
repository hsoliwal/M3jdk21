/* *****************************************************************************
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

import java.util.Objects;
import jdk.internal.m3.collections.packed.PackedHashPolicy;

/**
 * Explicit semantic/workload facts for a primitive long-to-long map compilation.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/collections/LongLongMapSpec.java}
 * blob {@code a87fdc6e297d21a6fb5cd34b86669cd637357946} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public record LongLongMapSpec(
        int expectedSize,
        LongLongMapRepresentation.Order order,
        CollectionMutation mutation,
        PackedHashPolicy hashPolicy,
        int pageSize,
        LongLongMapRepresentation forcedRepresentation) {

    public static final int DEFAULT_PAGE_SIZE = 64;

    public LongLongMapSpec {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("expectedSize");
        }
        Objects.requireNonNull(order, "order");
        Objects.requireNonNull(mutation, "mutation");
        Objects.requireNonNull(hashPolicy, "hashPolicy");
        if (pageSize < 4) {
            throw new IllegalArgumentException("pageSize");
        }
        if (order != LongLongMapRepresentation.Order.UNORDERED
                && hashPolicy != PackedHashPolicy.AUTO) {
            throw new IllegalArgumentException("hash policy requires unordered semantics");
        }
    }

    public static LongLongMapSpec unordered(int expectedSize) {
        return new LongLongMapSpec(
                expectedSize,
                LongLongMapRepresentation.Order.UNORDERED,
                CollectionMutation.BALANCED,
                PackedHashPolicy.AUTO,
                DEFAULT_PAGE_SIZE,
                null);
    }

    public static LongLongMapSpec sorted(int expectedSize) {
        return unordered(expectedSize).withOrder(LongLongMapRepresentation.Order.SORTED);
    }

    public static LongLongMapSpec insertionOrdered(int expectedSize) {
        return unordered(expectedSize).withOrder(LongLongMapRepresentation.Order.INSERTION);
    }

    public static LongLongMapSpec accessOrdered(int expectedSize) {
        return unordered(expectedSize).withOrder(LongLongMapRepresentation.Order.ACCESS);
    }

    public LongLongMapSpec withOrder(LongLongMapRepresentation.Order next) {
        return new LongLongMapSpec(
                expectedSize, next, mutation, hashPolicy, pageSize, forcedRepresentation);
    }

    public LongLongMapSpec withMutation(CollectionMutation next) {
        return new LongLongMapSpec(
                expectedSize, order, next, hashPolicy, pageSize, forcedRepresentation);
    }

    public LongLongMapSpec withHashPolicy(PackedHashPolicy next) {
        return new LongLongMapSpec(
                expectedSize, order, mutation, next, pageSize, forcedRepresentation);
    }

    public LongLongMapSpec withPageSize(int next) {
        return new LongLongMapSpec(
                expectedSize, order, mutation, hashPolicy, next, forcedRepresentation);
    }

    public LongLongMapSpec force(LongLongMapRepresentation representation) {
        return new LongLongMapSpec(
                expectedSize, order, mutation, hashPolicy, pageSize,
                Objects.requireNonNull(representation, "representation"));
    }
}
