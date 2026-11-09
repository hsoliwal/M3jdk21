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
 * Immutable observation of one canonical pool.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/canonical/CanonicalPoolStats.java}
 * blob {@code c682d2317f2f87a2f31a6ca6366fa0715cf1c45e} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record CanonicalPoolStats(
        long poolId,
        long generation,
        int entries,
        long retainedWeight,
        long hits,
        long admissions,
        long collisions,
        long capacityRejects,
        long collected) {
    public CanonicalPoolStats {
        if (poolId <= 0 || generation <= 0 || entries < 0 || retainedWeight < 0
                || hits < 0 || admissions < 0 || collisions < 0 || capacityRejects < 0
                || collected < 0) {
            throw new IllegalArgumentException("negative/invalid canonical-pool statistic");
        }
    }
}
