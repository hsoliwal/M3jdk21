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
final class M3ReductionProviders implements M3ShapesCollectionProvider {
    private final M3CollectionCandidate candidate;
    private final boolean scan;
    private M3ReductionProviders(String id, M3CollectionRequirements.Kind kind, M3CollectionRequirements.Order order,
                               boolean scan, boolean nullable, boolean identity, M3CandidateProfile profile) {
        this.scan = scan;
        candidate = new M3CollectionCandidate(id, kind, order, M3CollectionRequirements.Concurrency.SINGLE_THREAD,
                true, identity, nullable, 1 << 28, profile);
    }
    static List<M3ShapesCollectionProvider> all() {
        var result = new ArrayList<M3ShapesCollectionProvider>(M3WorkloadProviders.all());
        result.add(new M3ReductionProviders("m3.prefix.scan.v1", M3CollectionRequirements.Kind.PREFIX_SET,
                M3CollectionRequirements.Order.INSERTION, true, true, false,
                new M3CandidateProfile(M3CandidateProfile.SizeBand.SMALL, 150, 600, 1000, 1000, 250)));
        result.add(new M3ReductionProviders("m3.prefix.sorted.v1", M3CollectionRequirements.Kind.PREFIX_SET,
                M3CollectionRequirements.Order.SORTED, false, true, false,
                new M3CandidateProfile(M3CandidateProfile.SizeBand.GENERAL, 900, 450, 1000, 1000, 250)));
        result.add(new M3ReductionProviders("m3.ranked.scan.v1", M3CollectionRequirements.Kind.RANKED_SELECTION,
                M3CollectionRequirements.Order.SORTED, true, false, true,
                new M3CandidateProfile(M3CandidateProfile.SizeBand.SMALL, 1000, 300, 1000, 1000, 250)));
        result.add(new M3ReductionProviders("m3.ranked.heap.v1", M3CollectionRequirements.Kind.RANKED_SELECTION,
                M3CollectionRequirements.Order.SORTED, false, false, true,
                new M3CandidateProfile(M3CandidateProfile.SizeBand.GENERAL, 1000, 900, 450, 650, 250)));
        return List.copyOf(result);
    }
    @Override public M3CollectionCandidate candidate() { return candidate; }
    @Override public <E> M3ShapePrefixSet<E> prefixSet(M3ShapesCollectionRequest request) {
        return scan ? new M3ScanPrefixSet<>(request.required()) : new M3SortedPrefixSet<>(request.required());
    }
    @Override public <E> M3ShapeRankedSelection<E> rankedSelection(M3ShapesCollectionRequest request, Comparator<? super E> comparator) {
        return scan ? new M3ScanRankedSelection<>(request.required(), comparator) : new M3HeapRankedSelection<>(request.required(), comparator);
    }
}
