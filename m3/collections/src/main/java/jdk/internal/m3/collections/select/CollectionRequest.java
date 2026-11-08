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
package jdk.internal.m3.collections.select;

import java.util.Objects;
import jdk.internal.m3.collections.CollectionAspectMask;

/**
 * Immutable requirements and workload hints. Expected size is not a size limit.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/selection/CollectionRequest.java}
 * blob {@code 968b14a1ddd13d68458f8a370629e7da4d7eed97} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public record CollectionRequest<T>(CollectionFamily<T> family, int expectedSize,
        Requirements requirements, Workload workload, Preferences preferences) {
    public CollectionRequest {
        Objects.requireNonNull(family, "family");
        Objects.requireNonNull(requirements, "requirements");
        Objects.requireNonNull(workload, "workload");
        Objects.requireNonNull(preferences, "preferences");
        if (expectedSize < 0) {
            throw new IllegalArgumentException("expectedSize must be nonnegative");
        }
    }

    public static <T> CollectionRequest<T> forFamily(CollectionFamily<T> family) {
        return new CollectionRequest<>(family, 16, Requirements.ANY,
                Workload.BALANCED, Preferences.BALANCED);
    }

    public CollectionRequest<T> withExpectedSize(int size) {
        return new CollectionRequest<>(family, size, requirements, workload, preferences);
    }

    public CollectionRequest<T> requiring(Requirements value) {
        return new CollectionRequest<>(family, expectedSize, value, workload, preferences);
    }

    public CollectionRequest<T> withWorkload(Workload value) {
        return new CollectionRequest<>(family, expectedSize, requirements, value, preferences);
    }

    public CollectionRequest<T> withPreferences(Preferences value) {
        return new CollectionRequest<>(family, expectedSize, requirements, workload, value);
    }

    public enum Order { ANY, INSERTION, ACCESS, SORTED, MIN_FIRST, MAX_FIRST }

    /** False means not required. Block size zero means provider default, not zero-size blocks. */
    public record Requirements(Order order, boolean concurrent, boolean stableHandles,
            boolean lazyStorage, boolean noPayloadCopyOnGrowth, int blockSize) {
        public static final Requirements ANY = new Requirements(Order.ANY,
                false, false, false, false, 0);

        /** Primitive representation preserving false-means-not-required semantics. */
        public int aspects() {
            return CollectionAspectMask.selectionRequirements(this);
        }

        public Requirements {
            Objects.requireNonNull(order, "order");
            if (blockSize < 0) {
                throw new IllegalArgumentException("blockSize must be nonnegative");
            }
        }
    }

    /** Relative operation weights; emptyPermille is the probability of an unused instance. */
    public record Workload(int lookups, int inserts, int removes, int scans,
            int indexedAccess, int emptyPermille) {
        public static final Workload BALANCED = new Workload(40, 25, 15, 20, 0, 0);

        public Workload {
            weight(lookups); weight(inserts); weight(removes); weight(scans);
            weight(indexedAccess); weight(emptyPermille);
            if (lookups + inserts + removes + scans + indexedAccess == 0) {
                throw new IllegalArgumentException("at least one operation weight is required");
            }
        }

        int total() { return lookups + inserts + removes + scans + indexedAccess; }
    }

    /** Payload target is a soft primitive-byte preference, never a retained-heap bound. */
    public record Preferences(int memory, int operations, int allocation, long payloadTargetBytes) {
        public static final Preferences BALANCED = new Preferences(50, 40, 10, 65_536);

        public Preferences {
            weight(memory); weight(operations); weight(allocation);
            if (memory + operations + allocation == 0 || payloadTargetBytes <= 0) {
                throw new IllegalArgumentException("positive total weight and payload target required");
            }
        }
    }

    static void weight(int value) {
        if (value < 0 || value > 1000) {
            throw new IllegalArgumentException("weight/membership must be in 0..1000");
        }
    }
}
