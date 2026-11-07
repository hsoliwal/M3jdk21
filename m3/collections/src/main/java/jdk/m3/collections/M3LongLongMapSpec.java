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
package jdk.m3.collections;

import java.util.Objects;

/**
 * Explicit semantic/workload facts for a primitive long-to-long map compilation.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/collections/LongLongMapSpec.java}
 * blob {@code a87fdc6e297d21a6fb5cd34b86669cd637357946} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public record M3LongLongMapSpec(
        int expectedSize,
        M3LongLongMapRepresentation.Order order,
        M3CollectionMutation mutation,
        M3PackedHashPolicy hashPolicy,
        int pageSize,
        M3LongLongMapRepresentation forcedRepresentation) {

    public static final int DEFAULT_PAGE_SIZE = 64;

    public M3LongLongMapSpec {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("expectedSize");
        }
        Objects.requireNonNull(order, "order");
        Objects.requireNonNull(mutation, "mutation");
        Objects.requireNonNull(hashPolicy, "hashPolicy");
        if (pageSize < 4) {
            throw new IllegalArgumentException("pageSize");
        }
        if (order != M3LongLongMapRepresentation.Order.UNORDERED
                && hashPolicy != M3PackedHashPolicy.AUTO) {
            throw new IllegalArgumentException("hash policy requires unordered semantics");
        }
    }

    public static M3LongLongMapSpec unordered(int expectedSize) {
        return new M3LongLongMapSpec(
                expectedSize,
                M3LongLongMapRepresentation.Order.UNORDERED,
                M3CollectionMutation.BALANCED,
                M3PackedHashPolicy.AUTO,
                DEFAULT_PAGE_SIZE,
                null);
    }

    public static M3LongLongMapSpec sorted(int expectedSize) {
        return unordered(expectedSize).withOrder(M3LongLongMapRepresentation.Order.SORTED);
    }

    public static M3LongLongMapSpec insertionOrdered(int expectedSize) {
        return unordered(expectedSize).withOrder(M3LongLongMapRepresentation.Order.INSERTION);
    }

    public static M3LongLongMapSpec accessOrdered(int expectedSize) {
        return unordered(expectedSize).withOrder(M3LongLongMapRepresentation.Order.ACCESS);
    }

    public M3LongLongMapSpec withOrder(M3LongLongMapRepresentation.Order next) {
        return new M3LongLongMapSpec(
                expectedSize, next, mutation, hashPolicy, pageSize, forcedRepresentation);
    }

    public M3LongLongMapSpec withMutation(M3CollectionMutation next) {
        return new M3LongLongMapSpec(
                expectedSize, order, next, hashPolicy, pageSize, forcedRepresentation);
    }

    public M3LongLongMapSpec withHashPolicy(M3PackedHashPolicy next) {
        return new M3LongLongMapSpec(
                expectedSize, order, mutation, next, pageSize, forcedRepresentation);
    }

    public M3LongLongMapSpec withPageSize(int next) {
        return new M3LongLongMapSpec(
                expectedSize, order, mutation, hashPolicy, next, forcedRepresentation);
    }

    public M3LongLongMapSpec force(M3LongLongMapRepresentation representation) {
        return new M3LongLongMapSpec(
                expectedSize, order, mutation, hashPolicy, pageSize,
                Objects.requireNonNull(representation, "representation"));
    }
}
