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
package jdk.internal.m3.collections.shape;

import java.util.Objects;

/**
 * Immutable request, separating required semantics from fuzzy workload preferences.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/CollectionRequest.java}
 * blob {@code 64838deacacd107cf17ecb3e71a79df58ca456bf} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 2 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public record CollectionRequest(CollectionRequirements required, CollectionPreferences preferred) {
    public CollectionRequest {
        Objects.requireNonNull(required, "required");
        Objects.requireNonNull(preferred, "preferred");
        if (preferred.expectedSize() > required.maximumSize()) {
            throw new IllegalArgumentException("expectedSize exceeds maximumSize");
        }
    }
    /** Default value-equality single-threaded request; ranked selection rejects null candidates. */
    public static CollectionRequest of(CollectionRequirements.Kind kind, int expectedSize) {
        var order = kind == CollectionRequirements.Kind.MAP || kind == CollectionRequirements.Kind.SET
                || kind == CollectionRequirements.Kind.PRIORITY_QUEUE || kind == CollectionRequirements.Kind.TOP_K
                || kind == CollectionRequirements.Kind.PREFIX_SET
                || kind == CollectionRequirements.Kind.ADDRESSABLE_PRIORITY_QUEUE
                ? CollectionRequirements.Order.UNSPECIFIED : CollectionRequirements.Order.POSITION;
        if (kind == CollectionRequirements.Kind.RANKED_SELECTION) { order = CollectionRequirements.Order.SORTED; }
        return new CollectionRequest(new CollectionRequirements(kind, order,
                CollectionRequirements.Equality.VALUE, CollectionRequirements.Concurrency.SINGLE_THREAD,
                kind != CollectionRequirements.Kind.RANKED_SELECTION, 1 << 28), CollectionPreferences.balanced(expectedSize));
    }
    /** Explicit live bound; for WINDOW and TOP_K this is their exact logical capacity. */
    public static CollectionRequest bounded(CollectionRequirements.Kind kind, int capacity, int expectedSize) {
        CollectionRequest defaults = of(kind, expectedSize);
        CollectionRequirements r = defaults.required();
        return new CollectionRequest(new CollectionRequirements(kind, r.order(), r.equality(),
                r.concurrency(), r.allowNull(), capacity), defaults.preferred());
    }
}
