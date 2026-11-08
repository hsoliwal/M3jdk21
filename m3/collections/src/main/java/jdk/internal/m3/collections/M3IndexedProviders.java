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
import java.util.List;

/**
 * Four opt-in slot/index layouts; ratings are explicit policy heuristics, not benchmarks.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/IndexedProviders.java}
 * blob {@code 42c6724d33fefff7f559e23365487c0449157345} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 2 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class M3IndexedProviders implements M3ShapesCollectionProvider {
    private final M3CollectionCandidate candidate;
    private M3IndexedProviders(String id, M3CollectionRequirements.Kind kind, M3CollectionRequirements.Order order,
                             M3CandidateProfile profile) {
        candidate = new M3CollectionCandidate(id, kind, order, M3CollectionRequirements.Concurrency.SINGLE_THREAD,
                true, true, true, 1 << 28, profile);
    }
    static List<M3ShapesCollectionProvider> all() {
        var result = new ArrayList<M3ShapesCollectionProvider>(M3ReductionProviders.all());
        for (var kind : List.of(M3CollectionRequirements.Kind.MAP, M3CollectionRequirements.Kind.SET)) {
            String shape = kind == M3CollectionRequirements.Kind.MAP ? "map" : "set";
            result.add(new M3IndexedProviders("m3." + shape + ".ordered-slots.v1", kind,
                    M3CollectionRequirements.Order.INSERTION,
                    new M3CandidateProfile(M3CandidateProfile.SizeBand.GENERAL, 950, 950, 700, 450, 300)));
        }
        result.add(new M3IndexedProviders("m3.sequence.indexed-slots.v1", M3CollectionRequirements.Kind.SEQUENCE,
                M3CollectionRequirements.Order.POSITION,
                new M3CandidateProfile(M3CandidateProfile.SizeBand.GENERAL, 850, 950, 650, 500, 300)));
        result.add(new M3IndexedProviders("m3.priority.indexed-heap.v1", M3CollectionRequirements.Kind.PRIORITY_QUEUE,
                M3CollectionRequirements.Order.UNSPECIFIED,
                new M3CandidateProfile(M3CandidateProfile.SizeBand.GENERAL, 1000, 950, 600, 450, 200)));
        return List.copyOf(result);
    }
    @Override public M3CollectionCandidate candidate() { return candidate; }
    @Override public <K, V> M3ShapeMap<K, V> map(M3ShapesCollectionRequest request) {
        return new M3StoreShapeMap<>(new M3OrderedHashKeyStore<>(request.required(), true));
    }
    @Override public <E> M3ShapeSet<E> set(M3ShapesCollectionRequest request) {
        return new M3StoreShapeSet<>(new M3OrderedHashKeyStore<>(request.required(), false));
    }
    @Override public <E> M3ShapeSequence<E> sequence(M3ShapesCollectionRequest request) {
        return new M3IndexedShapeSequence<>(request.required());
    }
    @Override public <E> M3ShapePriorityQueue<E> priorityQueue(M3ShapesCollectionRequest request) {
        return new M3IndexedShapePriorityQueue<>(request.required());
    }
}
