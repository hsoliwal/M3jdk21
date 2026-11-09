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
 * Weighted workload/preference axes used by deterministic fuzzy scoring.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/collections/CollectionTrait.java}
 * blob {@code 7d66272d3575577f5ca997235c3a0162a3578e39} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum CollectionTrait {
    LOOKUP,
    INSERT,
    DELETE,
    ITERATE,
    RANDOM_ACCESS,
    RANGE_QUERY,
    MEMORY_EFFICIENCY,
    LOCALITY,
    BATCH_INGEST,
    SPARSE_LOGICAL,
    STABLE_HANDLES,
    CONCURRENT_READ,
    CONCURRENT_WRITE
}
