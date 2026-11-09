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
package jdk.internal.m3.collections.shape;

import java.util.Objects;
import jdk.internal.m3.collections.CollectionAspectMask;

/**
 * Hard requirements. Fuzzy preferences cannot relax any of these fields.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/CollectionRequirements.java}
 * blob {@code 16ac1f491229cc92217a9af8e0e289c9aec87ca0} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public record CollectionRequirements(Kind kind, Order order, Equality equality,
                                     Concurrency concurrency, boolean allowNull,
                                     int maximumSize) {
    /** Distinct semantic shapes. */
    public enum Kind { MAP, SET, SEQUENCE, LINKED_SEQUENCE, DEQUE, WINDOW, PRIORITY_QUEUE, TOP_K, PREFIX_SET, RANKED_SELECTION, ADDRESSABLE_PRIORITY_QUEUE }
    /** UNSPECIFIED imposes no ordering promise; positional order is required for sequences. */
    public enum Order { UNSPECIFIED, INSERTION, POSITION, SORTED }
    /** Key comparison domain, never inferred from object interning. */
    public enum Equality { VALUE, IDENTITY }
    /** Built-ins are SINGLE_THREAD; unsupported concurrent requests are rejected. */
    public enum Concurrency { SINGLE_THREAD, CONCURRENT }

    /** Additive primitive-mask constructor; existing enum/boolean constructor remains canonical. */
    public CollectionRequirements(int aspects, int maximumSize) {
        this(
                semanticKind(aspects),
                CollectionAspectMask.shapeOrder(aspects),
                CollectionAspectMask.shapeEquality(aspects),
                CollectionAspectMask.shapeConcurrency(aspects),
                CollectionAspectMask.has(aspects, CollectionAspectMask.NULLABLE),
                maximumSize);
    }

    private static Kind semanticKind(int aspects) {
        CollectionAspectMask.requireSemanticOnly(aspects);
        return CollectionAspectMask.shapeKind(aspects);
    }

    /** Exact primitive representation of the current hard semantic fields. */
    public int aspects() {
        return CollectionAspectMask.shapeRequirements(this);
    }

    public CollectionRequirements {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(order, "order");
        Objects.requireNonNull(equality, "equality");
        Objects.requireNonNull(concurrency, "concurrency");
        if (maximumSize < 0 || maximumSize > (1 << 28)) {
            throw new IllegalArgumentException("maximumSize outside 0..2^28");
        }
    }
}
