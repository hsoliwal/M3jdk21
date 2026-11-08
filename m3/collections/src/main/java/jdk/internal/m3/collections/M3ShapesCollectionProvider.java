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

import java.util.Comparator;

/**
 * Explicit trusted extension point. Implementations must honor each admitted request.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/CollectionProvider.java}
 * blob {@code 871fcffca2793c00ed24b8cb44516592381de621} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 2 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public interface M3ShapesCollectionProvider {
    /** Stable, immutable descriptor. Factories snapshot it at registration. */
    M3CollectionCandidate candidate();

    /**
     * Primitive capability word for hard admission.
     *
     * <p>Existing providers inherit their semantic candidate bits. Providers with additional
     * physical capabilities (segmented/pageable/pressure-adaptive/SQLite/etc.) override this
     * method and OR only capabilities they actually implement.</p>
     */
    default int aspects() {
        return candidate().aspects();
    }
    /** Construct a fresh generation-checked priority queue for an admitted addressable shape. */
    default <E> M3ShapeAddressablePriorityQueue<E> addressablePriorityQueue(M3ShapesCollectionRequest request) {
        throw new UnsupportedOperationException("provider has no addressable priority queue implementation");
    }
    /** Construct a fresh empty map when the descriptor kind is MAP. */
    default <K, V> M3ShapeMap<K, V> map(M3ShapesCollectionRequest request) {
        throw new UnsupportedOperationException("provider has no map implementation");
    }
    /** Construct a fresh empty set when the descriptor kind is SET. */
    default <E> M3ShapeSet<E> set(M3ShapesCollectionRequest request) {
        throw new UnsupportedOperationException("provider has no set implementation");
    }
    /** Construct a fresh empty positional sequence. */
    default <E> M3ShapeSequence<E> sequence(M3ShapesCollectionRequest request) {
        throw new UnsupportedOperationException("provider has no sequence implementation");
    }
    /** Construct a fresh empty stable-handle sequence. */
    default <E> M3ShapeLinkedSequence<E> linkedSequence(M3ShapesCollectionRequest request) {
        throw new UnsupportedOperationException("provider has no linked implementation");
    }
    /** Construct a fresh empty M3ShapeDeque when its descriptor kind is admitted. */
    default <E> M3ShapeDeque<E> deque(M3ShapesCollectionRequest request) {
        throw new UnsupportedOperationException("provider has no deque implementation");
    }
    /** Construct a fresh empty M3ShapeWindow when its descriptor kind is admitted. */
    default <E> M3ShapeWindow<E> window(M3ShapesCollectionRequest request) {
        throw new UnsupportedOperationException("provider has no window implementation");
    }
    /** Construct a fresh empty M3ShapePriorityQueue when its descriptor kind is admitted. */
    default <E> M3ShapePriorityQueue<E> priorityQueue(M3ShapesCollectionRequest request) {
        throw new UnsupportedOperationException("provider has no priorityQueue implementation");
    }
    /** Construct a fresh empty M3ShapeTopK when its descriptor kind is admitted. */
    default <E> M3ShapeTopK<E> topK(M3ShapesCollectionRequest request) {
        throw new UnsupportedOperationException("provider has no topK implementation");
    }
    /** Construct a fresh prefix antichain for an admitted path domain. */
    default <E> M3ShapePrefixSet<E> prefixSet(M3ShapesCollectionRequest request) {
        throw new UnsupportedOperationException("provider has no prefix set implementation");
    }
    /** Construct stable best-K admission using the caller's best-first comparator. */
    default <E> M3ShapeRankedSelection<E> rankedSelection(M3ShapesCollectionRequest request, Comparator<? super E> comparator) {
        throw new UnsupportedOperationException("provider has no ranked selection implementation");
    }
}
