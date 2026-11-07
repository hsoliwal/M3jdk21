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

import java.util.Objects;

/**
 * Facts required to compile one primitive-long set representation.
 *
 * <p>{@code boundedDomainMaxInclusive >= 0} declares that every legal value lies in
 * {@code [0,max]}. It is a semantic contract, not a performance hint.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/collections/LongSetSpec.java}
 * blob {@code 966bf57744654ba70c563863b0e296d06b2783c1} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public record M3LongSetSpec(
        int expectedSize,
        boolean sorted,
        M3CollectionMutation mutation,
        M3PackedHashPolicy hashPolicy,
        int batchSize,
        int pageSize,
        int boundedDomainMaxInclusive,
        M3LongSetRepresentation forcedRepresentation) {

    public static final int DEFAULT_BATCH_SIZE = 4_096;
    public static final int DEFAULT_PAGE_SIZE = 64;

    public M3LongSetSpec {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("expectedSize");
        }
        Objects.requireNonNull(mutation, "mutation");
        Objects.requireNonNull(hashPolicy, "hashPolicy");
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize");
        }
        if (pageSize < 4) {
            throw new IllegalArgumentException("pageSize");
        }
        if (boundedDomainMaxInclusive < -1) {
            throw new IllegalArgumentException("boundedDomainMaxInclusive");
        }
        if (sorted && hashPolicy != M3PackedHashPolicy.AUTO) {
            throw new IllegalArgumentException("hash policy is meaningless for a sorted set");
        }
    }

    public static M3LongSetSpec unordered(int expectedSize) {
        return new M3LongSetSpec(
                expectedSize,
                false,
                M3CollectionMutation.BALANCED,
                M3PackedHashPolicy.AUTO,
                DEFAULT_BATCH_SIZE,
                DEFAULT_PAGE_SIZE,
                -1,
                null);
    }

    public static M3LongSetSpec sorted(int expectedSize) {
        return new M3LongSetSpec(
                expectedSize,
                true,
                M3CollectionMutation.BALANCED,
                M3PackedHashPolicy.AUTO,
                DEFAULT_BATCH_SIZE,
                DEFAULT_PAGE_SIZE,
                -1,
                null);
    }

    public M3LongSetSpec withMutation(M3CollectionMutation next) {
        return new M3LongSetSpec(
                expectedSize, sorted, next, hashPolicy, batchSize, pageSize,
                boundedDomainMaxInclusive, forcedRepresentation);
    }

    public M3LongSetSpec withHashPolicy(M3PackedHashPolicy next) {
        return new M3LongSetSpec(
                expectedSize, sorted, mutation, next, batchSize, pageSize,
                boundedDomainMaxInclusive, forcedRepresentation);
    }

    public M3LongSetSpec withBatchSize(int next) {
        return new M3LongSetSpec(
                expectedSize, sorted, mutation, hashPolicy, next, pageSize,
                boundedDomainMaxInclusive, forcedRepresentation);
    }

    public M3LongSetSpec withPageSize(int next) {
        return new M3LongSetSpec(
                expectedSize, sorted, mutation, hashPolicy, batchSize, next,
                boundedDomainMaxInclusive, forcedRepresentation);
    }

    public M3LongSetSpec withBoundedDomain(int maxInclusive) {
        return new M3LongSetSpec(
                expectedSize, sorted, mutation, hashPolicy, batchSize, pageSize,
                maxInclusive, forcedRepresentation);
    }

    public M3LongSetSpec force(M3LongSetRepresentation representation) {
        return new M3LongSetSpec(
                expectedSize, sorted, mutation, hashPolicy, batchSize, pageSize,
                boundedDomainMaxInclusive, Objects.requireNonNull(representation, "representation"));
    }

    public boolean boundedDomain() {
        return boundedDomainMaxInclusive >= 0;
    }
}
