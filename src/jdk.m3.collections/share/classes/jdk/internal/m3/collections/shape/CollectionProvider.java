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

import java.util.Comparator;

/**
 * Explicit trusted extension point. Implementations must honor each admitted request.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/CollectionProvider.java}
 * blob {@code 871fcffca2793c00ed24b8cb44516592381de621} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 2 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public interface CollectionProvider {
    /** Stable, immutable descriptor. Factories snapshot it at registration. */
    CollectionCandidate candidate();

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
    default <E> ShapeAddressablePriorityQueue<E> addressablePriorityQueue(CollectionRequest request) {
        throw new UnsupportedOperationException("provider has no addressable priority queue implementation");
    }
    /** Construct a fresh empty map when the descriptor kind is MAP. */
    default <K, V> ShapeMap<K, V> map(CollectionRequest request) {
        throw new UnsupportedOperationException("provider has no map implementation");
    }
    /** Construct a fresh empty set when the descriptor kind is SET. */
    default <E> ShapeSet<E> set(CollectionRequest request) {
        throw new UnsupportedOperationException("provider has no set implementation");
    }
    /** Construct a fresh empty positional sequence. */
    default <E> ShapeSequence<E> sequence(CollectionRequest request) {
        throw new UnsupportedOperationException("provider has no sequence implementation");
    }
    /** Construct a fresh empty stable-handle sequence. */
    default <E> ShapeLinkedSequence<E> linkedSequence(CollectionRequest request) {
        throw new UnsupportedOperationException("provider has no linked implementation");
    }
    /** Construct a fresh empty ShapeDeque when its descriptor kind is admitted. */
    default <E> ShapeDeque<E> deque(CollectionRequest request) {
        throw new UnsupportedOperationException("provider has no deque implementation");
    }
    /** Construct a fresh empty ShapeWindow when its descriptor kind is admitted. */
    default <E> ShapeWindow<E> window(CollectionRequest request) {
        throw new UnsupportedOperationException("provider has no window implementation");
    }
    /** Construct a fresh empty ShapePriorityQueue when its descriptor kind is admitted. */
    default <E> ShapePriorityQueue<E> priorityQueue(CollectionRequest request) {
        throw new UnsupportedOperationException("provider has no priorityQueue implementation");
    }
    /** Construct a fresh empty ShapeTopK when its descriptor kind is admitted. */
    default <E> ShapeTopK<E> topK(CollectionRequest request) {
        throw new UnsupportedOperationException("provider has no topK implementation");
    }
    /** Construct a fresh prefix antichain for an admitted path domain. */
    default <E> ShapePrefixSet<E> prefixSet(CollectionRequest request) {
        throw new UnsupportedOperationException("provider has no prefix set implementation");
    }
    /** Construct stable best-K admission using the caller's best-first comparator. */
    default <E> ShapeRankedSelection<E> rankedSelection(CollectionRequest request, Comparator<? super E> comparator) {
        throw new UnsupportedOperationException("provider has no ranked selection implementation");
    }
}
