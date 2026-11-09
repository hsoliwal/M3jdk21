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
import java.util.List;

/**
 * Four opt-in slot/index layouts; ratings are explicit policy heuristics, not benchmarks.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/IndexedProviders.java}
 * blob {@code 42c6724d33fefff7f559e23365487c0449157345} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 2 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class IndexedProviders implements CollectionProvider {
    private final CollectionCandidate candidate;
    private IndexedProviders(String id, CollectionRequirements.Kind kind, CollectionRequirements.Order order,
                             CandidateProfile profile) {
        candidate = new CollectionCandidate(id, kind, order, CollectionRequirements.Concurrency.SINGLE_THREAD,
                true, true, true, 1 << 28, profile);
    }
    static List<CollectionProvider> all() {
        var result = new ArrayList<CollectionProvider>(ReductionProviders.all());
        for (var kind : List.of(CollectionRequirements.Kind.MAP, CollectionRequirements.Kind.SET)) {
            String shape = kind == CollectionRequirements.Kind.MAP ? "map" : "set";
            result.add(new IndexedProviders("m3." + shape + ".ordered-slots.v1", kind,
                    CollectionRequirements.Order.INSERTION,
                    new CandidateProfile(CandidateProfile.SizeBand.GENERAL, 950, 950, 700, 450, 300)));
        }
        result.add(new IndexedProviders("m3.sequence.indexed-slots.v1", CollectionRequirements.Kind.SEQUENCE,
                CollectionRequirements.Order.POSITION,
                new CandidateProfile(CandidateProfile.SizeBand.GENERAL, 850, 950, 650, 500, 300)));
        result.add(new IndexedProviders("m3.priority.indexed-heap.v1", CollectionRequirements.Kind.PRIORITY_QUEUE,
                CollectionRequirements.Order.UNSPECIFIED,
                new CandidateProfile(CandidateProfile.SizeBand.GENERAL, 1000, 950, 600, 450, 200)));
        return List.copyOf(result);
    }
    @Override public CollectionCandidate candidate() { return candidate; }
    @Override public <K, V> ShapeMap<K, V> map(CollectionRequest request) {
        return new StoreShapeMap<>(new OrderedHashKeyStore<>(request.required(), true));
    }
    @Override public <E> ShapeSet<E> set(CollectionRequest request) {
        return new StoreShapeSet<>(new OrderedHashKeyStore<>(request.required(), false));
    }
    @Override public <E> ShapeSequence<E> sequence(CollectionRequest request) {
        return new IndexedShapeSequence<>(request.required());
    }
    @Override public <E> ShapePriorityQueue<E> priorityQueue(CollectionRequest request) {
        return new IndexedShapePriorityQueue<>(request.required());
    }
}
