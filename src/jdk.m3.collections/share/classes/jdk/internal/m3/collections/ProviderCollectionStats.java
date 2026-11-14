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
package jdk.internal.m3.collections;

/**
 * Immutable collection-level counters for provider-backed residency.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/ProviderCollectionStats.java}
 * blob {@code c069952c21113fbd13ec351add67e0a770eab61c} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public record ProviderCollectionStats(
        long touches,
        long cacheHits,
        long loads,
        long loadFailures,
        long retries,
        long policyEvictions,
        long explicitEvictions,
        long invalidations,
        long materializedPuts,
        long admissionFailures) {

    public ProviderCollectionStats {
        if (touches < 0L || cacheHits < 0L || loads < 0L || loadFailures < 0L
                || retries < 0L || policyEvictions < 0L || explicitEvictions < 0L
                || invalidations < 0L || materializedPuts < 0L || admissionFailures < 0L) {
            throw new IllegalArgumentException("negative provider statistic");
        }
        if (cacheHits > touches) {
            throw new IllegalArgumentException("cacheHits > touches");
        }
    }

    public long misses() {
        return touches - cacheHits;
    }

    public double hitRate() {
        return touches == 0L ? 0.0d : cacheHits / (double) touches;
    }
}
