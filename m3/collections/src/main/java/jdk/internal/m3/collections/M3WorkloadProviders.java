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
 * Eight concrete workload providers; profiles are versioned policy judgments.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/WorkloadProviders.java}
 * blob {@code 6006274acbbec4c0edc1564c47f59fda3f51e6b1} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 2 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class M3WorkloadProviders implements M3ShapesCollectionProvider {
    private final M3CollectionCandidate candidate;
    private final String storage;
    private M3WorkloadProviders(M3CollectionRequirements.Kind kind, String shape, String storage,
                              M3CollectionRequirements.Order order, M3CandidateProfile profile) {
        this.storage = storage;
        candidate = new M3CollectionCandidate("m3." + shape + "." + storage + ".v1", kind, order,
                M3CollectionRequirements.Concurrency.SINGLE_THREAD, true, true, true, 1 << 28, profile);
    }
    static List<M3ShapesCollectionProvider> all() {
        var providers = new ArrayList<M3ShapesCollectionProvider>(M3BuiltinProviders.all());
        for (var kind : List.of(M3CollectionRequirements.Kind.DEQUE, M3CollectionRequirements.Kind.WINDOW)) {
            String shape = kind == M3CollectionRequirements.Kind.DEQUE ? "deque" : "window";
            providers.add(new M3WorkloadProviders(kind, shape, "flat-ring", M3CollectionRequirements.Order.POSITION,
                    new M3CandidateProfile(M3CandidateProfile.SizeBand.GENERAL, 1000, 900, 1000, 900, 200)));
            providers.add(new M3WorkloadProviders(kind, shape, "segmented-ring", M3CollectionRequirements.Order.POSITION,
                    new M3CandidateProfile(M3CandidateProfile.SizeBand.LARGE, 800, 850, 800, 500, 1000)));
        }
        for (var kind : List.of(M3CollectionRequirements.Kind.PRIORITY_QUEUE, M3CollectionRequirements.Kind.TOP_K)) {
            String shape = kind == M3CollectionRequirements.Kind.PRIORITY_QUEUE ? "priority" : "top-k";
            providers.add(new M3WorkloadProviders(kind, shape, "scan", M3CollectionRequirements.Order.UNSPECIFIED,
                    new M3CandidateProfile(M3CandidateProfile.SizeBand.SMALL, 150, 450, 1000, 1000, 250)));
            providers.add(new M3WorkloadProviders(kind, shape, "heap", M3CollectionRequirements.Order.UNSPECIFIED,
                    new M3CandidateProfile(M3CandidateProfile.SizeBand.GENERAL, 1000, 850, 1000, 1000, 250)));
        }
        return List.copyOf(providers);
    }
    @Override public M3CollectionCandidate candidate() { return candidate; }
    private <E> M3RingStore<E> ring(M3ShapesCollectionRequest request) {
        return storage.equals("flat-ring") ? new M3FlatRingStore<>(request.required().maximumSize())
                : new M3SegmentedRingStore<>(request.required().maximumSize());
    }
    private <E> M3RankStore<E> ranks(M3ShapesCollectionRequest request, boolean minimum) {
        return storage.equals("scan") ? new M3ScanRankStore<>(request.required().maximumSize(), minimum)
                : new M3HeapRankStore<>(request.required().maximumSize(), minimum);
    }
    @Override public <E> M3ShapeDeque<E> deque(M3ShapesCollectionRequest request) {
        return new M3RingShapeDeque<>(request.required(), ring(request));
    }
    @Override public <E> M3ShapeWindow<E> window(M3ShapesCollectionRequest request) {
        return new M3RingShapeWindow<>(request.required(), ring(request));
    }
    @Override public <E> M3ShapePriorityQueue<E> priorityQueue(M3ShapesCollectionRequest request) {
        return new M3RankShapePriorityQueue<>(request.required(), ranks(request, true));
    }
    @Override public <E> M3ShapeTopK<E> topK(M3ShapesCollectionRequest request) {
        return new M3RankShapeTopK<>(request.required(), ranks(request, false));
    }
}
