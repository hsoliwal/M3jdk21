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
 * Eight concrete workload providers; profiles are versioned policy judgments.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/WorkloadProviders.java}
 * blob {@code 6006274acbbec4c0edc1564c47f59fda3f51e6b1} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 2 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class WorkloadProviders implements CollectionProvider {
    private final CollectionCandidate candidate;
    private final String storage;
    private WorkloadProviders(CollectionRequirements.Kind kind, String shape, String storage,
                              CollectionRequirements.Order order, CandidateProfile profile) {
        this.storage = storage;
        candidate = new CollectionCandidate("m3." + shape + "." + storage + ".v1", kind, order,
                CollectionRequirements.Concurrency.SINGLE_THREAD, true, true, true, 1 << 28, profile);
    }
    static List<CollectionProvider> all() {
        var providers = new ArrayList<CollectionProvider>(BuiltinProviders.all());
        for (var kind : List.of(CollectionRequirements.Kind.DEQUE, CollectionRequirements.Kind.WINDOW)) {
            String shape = kind == CollectionRequirements.Kind.DEQUE ? "deque" : "window";
            providers.add(new WorkloadProviders(kind, shape, "flat-ring", CollectionRequirements.Order.POSITION,
                    new CandidateProfile(CandidateProfile.SizeBand.GENERAL, 1000, 900, 1000, 900, 200)));
            providers.add(new WorkloadProviders(kind, shape, "segmented-ring", CollectionRequirements.Order.POSITION,
                    new CandidateProfile(CandidateProfile.SizeBand.LARGE, 800, 850, 800, 500, 1000)));
        }
        for (var kind : List.of(CollectionRequirements.Kind.PRIORITY_QUEUE, CollectionRequirements.Kind.TOP_K)) {
            String shape = kind == CollectionRequirements.Kind.PRIORITY_QUEUE ? "priority" : "top-k";
            providers.add(new WorkloadProviders(kind, shape, "scan", CollectionRequirements.Order.UNSPECIFIED,
                    new CandidateProfile(CandidateProfile.SizeBand.SMALL, 150, 450, 1000, 1000, 250)));
            providers.add(new WorkloadProviders(kind, shape, "heap", CollectionRequirements.Order.UNSPECIFIED,
                    new CandidateProfile(CandidateProfile.SizeBand.GENERAL, 1000, 850, 1000, 1000, 250)));
        }
        return List.copyOf(providers);
    }
    @Override public CollectionCandidate candidate() { return candidate; }
    private <E> RingStore<E> ring(CollectionRequest request) {
        return storage.equals("flat-ring") ? new FlatRingStore<>(request.required().maximumSize())
                : new SegmentedRingStore<>(request.required().maximumSize());
    }
    private <E> RankStore<E> ranks(CollectionRequest request, boolean minimum) {
        return storage.equals("scan") ? new ScanRankStore<>(request.required().maximumSize(), minimum)
                : new HeapRankStore<>(request.required().maximumSize(), minimum);
    }
    @Override public <E> ShapeDeque<E> deque(CollectionRequest request) {
        return new RingShapeDeque<>(request.required(), ring(request));
    }
    @Override public <E> ShapeWindow<E> window(CollectionRequest request) {
        return new RingShapeWindow<>(request.required(), ring(request));
    }
    @Override public <E> ShapePriorityQueue<E> priorityQueue(CollectionRequest request) {
        return new RankShapePriorityQueue<>(request.required(), ranks(request, true));
    }
    @Override public <E> ShapeTopK<E> topK(CollectionRequest request) {
        return new RankShapeTopK<>(request.required(), ranks(request, false));
    }
}
