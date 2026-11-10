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

import java.util.ArrayList;
import java.util.List;

/**
 * Opt-in addressable variants; ratings are policy heuristics, not benchmark measurements.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/AddressableProviders.java}
 * blob {@code c06aba07e96f701f93f6231647ef45da393acdf9} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 2 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class AddressableProviders implements CollectionProvider {
    private final boolean heap;
    private final CollectionCandidate candidate;
    private AddressableProviders(boolean heap) {
        this.heap = heap;
        candidate = new CollectionCandidate("m3.addressable-priority."
                + (heap ? "indexed-heap.v1" : "scan-slots.v1"),
                CollectionRequirements.Kind.ADDRESSABLE_PRIORITY_QUEUE, CollectionRequirements.Order.UNSPECIFIED,
                CollectionRequirements.Concurrency.SINGLE_THREAD, true, true, true, 1 << 28,
                heap ? new CandidateProfile(CandidateProfile.SizeBand.GENERAL, 1000, 800, 700, 400, 300)
                     : new CandidateProfile(CandidateProfile.SizeBand.SMALL, 200, 1000, 900, 400, 500));
    }
    static List<CollectionProvider> all() {
        var result = new ArrayList<CollectionProvider>(IndexedProviders.all());
        result.add(new AddressableProviders(false)); result.add(new AddressableProviders(true));
        return List.copyOf(result);
    }
    @Override public CollectionCandidate candidate() { return candidate; }
    @Override public <E> ShapeAddressablePriorityQueue<E> addressablePriorityQueue(CollectionRequest request) {
        return heap ? new HeapAddressablePriorityQueue<>(request.required()) : new ScanAddressablePriorityQueue<>(request.required());
    }
}
