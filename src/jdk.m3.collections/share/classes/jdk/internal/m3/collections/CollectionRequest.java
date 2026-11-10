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
public final class CollectionRequest {
    private final CollectionShape shape;
    private final CollectionDataKind keyKind;
    private final CollectionDataKind valueKind;
    private final int expectedSize;
    private final long logicalDomainSize;
    private final boolean approximateAllowed;
    private final EnumSet<CollectionCapability> requiredCapabilities;
    private final EnumMap<CollectionTrait, Integer> weights;
    private final int alternatives;

    private CollectionRequest(Builder builder) {
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

    public static Builder forShape(CollectionShape shape) {
        return new Builder(shape);
    }

    public static Builder set() {
        return forShape(CollectionShape.SET);
    }

    public static Builder map() {
        return forShape(CollectionShape.MAP);
    }

    public static Builder sequence() {
        return forShape(CollectionShape.SEQUENCE);
    }

    public static Builder vector() {
        return forShape(CollectionShape.VECTOR);
    }

    public static Builder queue() {
        return forShape(CollectionShape.QUEUE);
    }

    public static Builder priority() {
        return forShape(CollectionShape.PRIORITY);
    }

    public static Builder range() {
        return forShape(CollectionShape.RANGE);
    }

    public static Builder graph() {
        return forShape(CollectionShape.GRAPH);
    }

    public static Builder text() {
        return forShape(CollectionShape.TEXT);
    }

    public static Builder fabric() {
        return forShape(CollectionShape.FABRIC);
    }

    public CollectionShape shape() {
        return shape;
    }

    public CollectionDataKind keyKind() {
        return keyKind;
    }

    public CollectionDataKind valueKind() {
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

    public Set<CollectionCapability> requiredCapabilities() {
        return Set.copyOf(requiredCapabilities);
    }

    public int weight(CollectionTrait trait) {
        return weights.getOrDefault(Objects.requireNonNull(trait, "trait"), 0);
    }

    public Map<CollectionTrait, Integer> weights() {
        return Map.copyOf(weights);
    }

    public int alternatives() {
        return alternatives;
    }

    public boolean requires(CollectionCapability capability) {
        return requiredCapabilities.contains(capability);
    }

    public static final class Builder {
        private final CollectionShape shape;
        private CollectionDataKind keyKind = CollectionDataKind.ANY;
        private CollectionDataKind valueKind = CollectionDataKind.ANY;
        private int expectedSize;
        private long logicalDomainSize = -1L;
        private boolean approximateAllowed;
        private final EnumSet<CollectionCapability> requiredCapabilities =
                EnumSet.noneOf(CollectionCapability.class);
        private final EnumMap<CollectionTrait, Integer> weights =
                new EnumMap<>(CollectionTrait.class);
        private int alternatives = 5;

        private Builder(CollectionShape shape) {
            this.shape = Objects.requireNonNull(shape, "shape");
            weights.put(CollectionTrait.LOOKUP, 50);
            weights.put(CollectionTrait.INSERT, 50);
            weights.put(CollectionTrait.DELETE, 25);
            weights.put(CollectionTrait.ITERATE, 20);
            weights.put(CollectionTrait.MEMORY_EFFICIENCY, 50);
            weights.put(CollectionTrait.LOCALITY, 50);
        }

        public Builder keyKind(CollectionDataKind kind) {
            keyKind = Objects.requireNonNull(kind, "kind");
            return this;
        }

        public Builder valueKind(CollectionDataKind kind) {
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

        public Builder require(CollectionCapability... capabilities) {
            Objects.requireNonNull(capabilities, "capabilities");
            for (CollectionCapability capability : capabilities) {
                requiredCapabilities.add(
                        Objects.requireNonNull(capability, "capability"));
            }
            return this;
        }

        public Builder approximateAllowed(boolean allowed) {
            approximateAllowed = allowed;
            return this;
        }

        public Builder weight(CollectionTrait trait, int weight) {
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
            return weight(CollectionTrait.LOOKUP, 100)
                    .weight(CollectionTrait.INSERT, 15)
                    .weight(CollectionTrait.DELETE, 10)
                    .weight(CollectionTrait.ITERATE, 60);
        }

        public Builder writeHeavy() {
            return weight(CollectionTrait.LOOKUP, 45)
                    .weight(CollectionTrait.INSERT, 100)
                    .weight(CollectionTrait.DELETE, 45);
        }

        public Builder deletionHeavy() {
            return weight(CollectionTrait.LOOKUP, 60)
                    .weight(CollectionTrait.INSERT, 55)
                    .weight(CollectionTrait.DELETE, 100);
        }

        public Builder batched() {
            return weight(CollectionTrait.BATCH_INGEST, 100)
                    .weight(CollectionTrait.INSERT, 90);
        }

        public Builder memoryFirst() {
            return weight(CollectionTrait.MEMORY_EFFICIENCY, 100)
                    .weight(CollectionTrait.LOCALITY, 75);
        }

        public Builder speedFirst() {
            return weight(CollectionTrait.LOOKUP, 100)
                    .weight(CollectionTrait.LOCALITY, 100)
                    .weight(CollectionTrait.MEMORY_EFFICIENCY, 25);
        }

        public Builder sparseLogical() {
            requiredCapabilities.add(CollectionCapability.SPARSE_LOGICAL);
            weights.put(CollectionTrait.SPARSE_LOGICAL, 100);
            return this;
        }

        public Builder stableHandles() {
            requiredCapabilities.add(CollectionCapability.STABLE_HANDLES);
            weights.put(CollectionTrait.STABLE_HANDLES, 100);
            return this;
        }

        public Builder sorted() {
            requiredCapabilities.add(CollectionCapability.SORTED);
            requiredCapabilities.add(CollectionCapability.NAVIGABLE);
            return this;
        }

        public Builder insertionOrdered() {
            requiredCapabilities.add(CollectionCapability.INSERTION_ORDER);
            return this;
        }

        public Builder accessOrdered() {
            requiredCapabilities.add(CollectionCapability.ACCESS_ORDER);
            return this;
        }

        public Builder priorityOrdered() {
            requiredCapabilities.add(CollectionCapability.PRIORITY);
            return this;
        }

        public Builder concurrent() {
            requiredCapabilities.add(CollectionCapability.CONCURRENT);
            weights.put(CollectionTrait.CONCURRENT_READ, 100);
            weights.put(CollectionTrait.CONCURRENT_WRITE, 100);
            return this;
        }

        public Builder spsc() {
            requiredCapabilities.add(CollectionCapability.SPSC);
            return concurrent();
        }

        public Builder mpmc() {
            requiredCapabilities.add(CollectionCapability.MPMC);
            return concurrent();
        }

        public Builder blocking() {
            requiredCapabilities.add(CollectionCapability.BLOCKING);
            return concurrent();
        }

        public Builder rangeQueries() {
            requiredCapabilities.add(CollectionCapability.RANGE_QUERY);
            weights.put(CollectionTrait.RANGE_QUERY, 100);
            return this;
        }

        public Builder persistent() {
            requiredCapabilities.add(CollectionCapability.PERSISTENT);
            return this;
        }

        public CollectionRequest build() {
            if (logicalDomainSize > 0
                    && (shape == CollectionShape.SET || shape == CollectionShape.MAP)
                    && expectedSize > logicalDomainSize) {
                throw new IllegalArgumentException(
                        "expected entries exceed declared key domain");
            }
            return new CollectionRequest(this);
        }
    }
}
