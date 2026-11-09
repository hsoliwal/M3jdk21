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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Four native reduction providers added only through the opt-in reductions registry.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/ReductionProviders.java}
 * blob {@code 7ed0794b8fb30ad8f96c64968ac1a7d6178f9015} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 2 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class ReductionProviders implements CollectionProvider {
    private final CollectionCandidate candidate;
    private final boolean scan;
    private ReductionProviders(String id, CollectionRequirements.Kind kind, CollectionRequirements.Order order,
                               boolean scan, boolean nullable, boolean identity, CandidateProfile profile) {
        this.scan = scan;
        candidate = new CollectionCandidate(id, kind, order, CollectionRequirements.Concurrency.SINGLE_THREAD,
                true, identity, nullable, 1 << 28, profile);
    }
    static List<CollectionProvider> all() {
        var result = new ArrayList<CollectionProvider>(WorkloadProviders.all());
        result.add(new ReductionProviders("m3.prefix.scan.v1", CollectionRequirements.Kind.PREFIX_SET,
                CollectionRequirements.Order.INSERTION, true, true, false,
                new CandidateProfile(CandidateProfile.SizeBand.SMALL, 150, 600, 1000, 1000, 250)));
        result.add(new ReductionProviders("m3.prefix.sorted.v1", CollectionRequirements.Kind.PREFIX_SET,
                CollectionRequirements.Order.SORTED, false, true, false,
                new CandidateProfile(CandidateProfile.SizeBand.GENERAL, 900, 450, 1000, 1000, 250)));
        result.add(new ReductionProviders("m3.ranked.scan.v1", CollectionRequirements.Kind.RANKED_SELECTION,
                CollectionRequirements.Order.SORTED, true, false, true,
                new CandidateProfile(CandidateProfile.SizeBand.SMALL, 1000, 300, 1000, 1000, 250)));
        result.add(new ReductionProviders("m3.ranked.heap.v1", CollectionRequirements.Kind.RANKED_SELECTION,
                CollectionRequirements.Order.SORTED, false, false, true,
                new CandidateProfile(CandidateProfile.SizeBand.GENERAL, 1000, 900, 450, 650, 250)));
        return List.copyOf(result);
    }
    @Override public CollectionCandidate candidate() { return candidate; }
    @Override public <E> ShapePrefixSet<E> prefixSet(CollectionRequest request) {
        return scan ? new ScanPrefixSet<>(request.required()) : new SortedPrefixSet<>(request.required());
    }
    @Override public <E> ShapeRankedSelection<E> rankedSelection(CollectionRequest request, Comparator<? super E> comparator) {
        return scan ? new ScanRankedSelection<>(request.required(), comparator) : new HeapRankedSelection<>(request.required(), comparator);
    }
}
