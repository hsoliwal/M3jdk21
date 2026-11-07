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

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable structured request for collection selection.
 *
 * <p>Capabilities are hard constraints. Trait weights are soft preferences in [0,100].
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/collections/CollectionRequest.java}
 * blob {@code 4dd127a6fa544b1e9448e9efbe99d3db83186019} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3CollectionRequest {
    private final M3CollectionShape shape;
    private final M3CollectionDataKind keyKind;
    private final M3CollectionDataKind valueKind;
    private final int expectedSize;
    private final long logicalDomainSize;
    private final boolean approximateAllowed;
    private final EnumSet<M3CollectionCapability> requiredCapabilities;
    private final EnumMap<M3CollectionTrait, Integer> weights;
    private final int alternatives;

    private M3CollectionRequest(Builder builder) {
        shape = builder.shape;
        keyKind = builder.keyKind;
        valueKind = builder.valueKind;
        expectedSize = builder.expectedSize;
        logicalDomainSize = builder.logicalDomainSize;
        approximateAllowed = builder.approximateAllowed;
        requiredCapabilities = builder.requiredCapabilities.clone();
        weights = new EnumMap<>(builder.weights);
        alternatives = builder.alternatives;
    }

    public static Builder forShape(M3CollectionShape shape) {
        return new Builder(shape);
    }

    public static Builder set() {
        return forShape(M3CollectionShape.SET);
    }

    public static Builder map() {
        return forShape(M3CollectionShape.MAP);
    }

    public static Builder sequence() {
        return forShape(M3CollectionShape.SEQUENCE);
    }

    public static Builder vector() {
        return forShape(M3CollectionShape.VECTOR);
    }

    public static Builder queue() {
        return forShape(M3CollectionShape.QUEUE);
    }

    public static Builder priority() {
        return forShape(M3CollectionShape.PRIORITY);
    }

    public static Builder range() {
        return forShape(M3CollectionShape.RANGE);
    }

    public static Builder graph() {
        return forShape(M3CollectionShape.GRAPH);
    }

    public static Builder text() {
        return forShape(M3CollectionShape.TEXT);
    }

    public static Builder fabric() {
        return forShape(M3CollectionShape.FABRIC);
    }

    public M3CollectionShape shape() {
        return shape;
    }

    public M3CollectionDataKind keyKind() {
        return keyKind;
    }

    public M3CollectionDataKind valueKind() {
        return valueKind;
    }

    public int expectedSize() {
        return expectedSize;
    }

    /** -1 means unknown/unbounded. */
    public long logicalDomainSize() {
        return logicalDomainSize;
    }

    public boolean approximateAllowed() {
        return approximateAllowed;
    }

    public Set<M3CollectionCapability> requiredCapabilities() {
        return Set.copyOf(requiredCapabilities);
    }

    public int weight(M3CollectionTrait trait) {
        return weights.getOrDefault(Objects.requireNonNull(trait, "trait"), 0);
    }

    public Map<M3CollectionTrait, Integer> weights() {
        return Map.copyOf(weights);
    }

    public int alternatives() {
        return alternatives;
    }

    public boolean requires(M3CollectionCapability capability) {
        return requiredCapabilities.contains(capability);
    }

    public static final class Builder {
        private final M3CollectionShape shape;
        private M3CollectionDataKind keyKind = M3CollectionDataKind.ANY;
        private M3CollectionDataKind valueKind = M3CollectionDataKind.ANY;
        private int expectedSize;
        private long logicalDomainSize = -1L;
        private boolean approximateAllowed;
        private final EnumSet<M3CollectionCapability> requiredCapabilities =
                EnumSet.noneOf(M3CollectionCapability.class);
        private final EnumMap<M3CollectionTrait, Integer> weights =
                new EnumMap<>(M3CollectionTrait.class);
        private int alternatives = 5;

        private Builder(M3CollectionShape shape) {
            this.shape = Objects.requireNonNull(shape, "shape");
            weights.put(M3CollectionTrait.LOOKUP, 50);
            weights.put(M3CollectionTrait.INSERT, 50);
            weights.put(M3CollectionTrait.DELETE, 25);
            weights.put(M3CollectionTrait.ITERATE, 20);
            weights.put(M3CollectionTrait.MEMORY_EFFICIENCY, 50);
            weights.put(M3CollectionTrait.LOCALITY, 50);
        }

        public Builder keyKind(M3CollectionDataKind kind) {
            keyKind = Objects.requireNonNull(kind, "kind");
            return this;
        }

        public Builder valueKind(M3CollectionDataKind kind) {
            valueKind = Objects.requireNonNull(kind, "kind");
            return this;
        }

        public Builder expectedSize(int size) {
            if (size < 0) {
                throw new IllegalArgumentException("expectedSize");
            }
            expectedSize = size;
            return this;
        }

        /** Number of addressable logical positions, or -1 for unknown. */
        public Builder logicalDomainSize(long size) {
            if (size == 0 || size < -1L) {
                throw new IllegalArgumentException("logicalDomainSize");
            }
            logicalDomainSize = size;
            return this;
        }

        public Builder require(M3CollectionCapability... capabilities) {
            Objects.requireNonNull(capabilities, "capabilities");
            for (M3CollectionCapability capability : capabilities) {
                requiredCapabilities.add(
                        Objects.requireNonNull(capability, "capability"));
            }
            return this;
        }

        public Builder approximateAllowed(boolean allowed) {
            approximateAllowed = allowed;
            return this;
        }

        public Builder weight(M3CollectionTrait trait, int weight) {
            Objects.requireNonNull(trait, "trait");
            if (weight < 0 || weight > 100) {
                throw new IllegalArgumentException("weight");
            }
            weights.put(trait, weight);
            return this;
        }

        public Builder alternatives(int count) {
            if (count < 1 || count > 64) {
                throw new IllegalArgumentException("alternatives");
            }
            alternatives = count;
            return this;
        }

        public Builder readMostly() {
            return weight(M3CollectionTrait.LOOKUP, 100)
                    .weight(M3CollectionTrait.INSERT, 15)
                    .weight(M3CollectionTrait.DELETE, 10)
                    .weight(M3CollectionTrait.ITERATE, 60);
        }

        public Builder writeHeavy() {
            return weight(M3CollectionTrait.LOOKUP, 45)
                    .weight(M3CollectionTrait.INSERT, 100)
                    .weight(M3CollectionTrait.DELETE, 45);
        }

        public Builder deletionHeavy() {
            return weight(M3CollectionTrait.LOOKUP, 60)
                    .weight(M3CollectionTrait.INSERT, 55)
                    .weight(M3CollectionTrait.DELETE, 100);
        }

        public Builder batched() {
            return weight(M3CollectionTrait.BATCH_INGEST, 100)
                    .weight(M3CollectionTrait.INSERT, 90);
        }

        public Builder memoryFirst() {
            return weight(M3CollectionTrait.MEMORY_EFFICIENCY, 100)
                    .weight(M3CollectionTrait.LOCALITY, 75);
        }

        public Builder speedFirst() {
            return weight(M3CollectionTrait.LOOKUP, 100)
                    .weight(M3CollectionTrait.LOCALITY, 100)
                    .weight(M3CollectionTrait.MEMORY_EFFICIENCY, 25);
        }

        public Builder sparseLogical() {
            requiredCapabilities.add(M3CollectionCapability.SPARSE_LOGICAL);
            weights.put(M3CollectionTrait.SPARSE_LOGICAL, 100);
            return this;
        }

        public Builder stableHandles() {
            requiredCapabilities.add(M3CollectionCapability.STABLE_HANDLES);
            weights.put(M3CollectionTrait.STABLE_HANDLES, 100);
            return this;
        }

        public Builder sorted() {
            requiredCapabilities.add(M3CollectionCapability.SORTED);
            requiredCapabilities.add(M3CollectionCapability.NAVIGABLE);
            return this;
        }

        public Builder insertionOrdered() {
            requiredCapabilities.add(M3CollectionCapability.INSERTION_ORDER);
            return this;
        }

        public Builder accessOrdered() {
            requiredCapabilities.add(M3CollectionCapability.ACCESS_ORDER);
            return this;
        }

        public Builder priorityOrdered() {
            requiredCapabilities.add(M3CollectionCapability.PRIORITY);
            return this;
        }

        public Builder concurrent() {
            requiredCapabilities.add(M3CollectionCapability.CONCURRENT);
            weights.put(M3CollectionTrait.CONCURRENT_READ, 100);
            weights.put(M3CollectionTrait.CONCURRENT_WRITE, 100);
            return this;
        }

        public Builder spsc() {
            requiredCapabilities.add(M3CollectionCapability.SPSC);
            return concurrent();
        }

        public Builder mpmc() {
            requiredCapabilities.add(M3CollectionCapability.MPMC);
            return concurrent();
        }

        public Builder blocking() {
            requiredCapabilities.add(M3CollectionCapability.BLOCKING);
            return concurrent();
        }

        public Builder rangeQueries() {
            requiredCapabilities.add(M3CollectionCapability.RANGE_QUERY);
            weights.put(M3CollectionTrait.RANGE_QUERY, 100);
            return this;
        }

        public Builder persistent() {
            requiredCapabilities.add(M3CollectionCapability.PERSISTENT);
            return this;
        }

        public M3CollectionRequest build() {
            if (logicalDomainSize > 0
                    && (shape == M3CollectionShape.SET || shape == M3CollectionShape.MAP)
                    && expectedSize > logicalDomainSize) {
                throw new IllegalArgumentException(
                        "expected entries exceed declared key domain");
            }
            return new M3CollectionRequest(this);
        }
    }
}
