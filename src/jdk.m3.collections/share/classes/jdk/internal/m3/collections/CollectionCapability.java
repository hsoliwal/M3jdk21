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

/**
 * Hard collection semantics/capabilities. Soft preferences belong in CollectionTrait.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/collections/CollectionCapability.java}
 * blob {@code c7e5055f5a7b6bfd5c9ef5d068a81b812fbd45f0} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum CollectionCapability {
    EXACT,
    APPROXIMATE,
    MUTABLE,
    IMMUTABLE,
    SORTED,
    NAVIGABLE,
    INSERTION_ORDER,
    ACCESS_ORDER,
    FIFO,
    DEQUE,
    PRIORITY,
    STABLE_HANDLES,
    SPARSE_LOGICAL,
    BOUNDED_DOMAIN,
    RANDOM_ACCESS,
    RANGE_QUERY,
    RANGE_UPDATE,
    MULTIVALUE,
    DUPLICATES,
    PREFIX_SEARCH,
    CONCURRENT,
    SPSC,
    MPMC,
    BLOCKING,
    PERSISTENT
}
