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

import java.util.Objects;

/**
 * Primitive array payload model and heuristic memberships, not measured latency or total heap.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/selection/CollectionEstimate.java}
 * blob {@code 6a5c30bc13c6301e9db3cc3c040c2a330fcbef94} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public record M3CollectionEstimate(long initialPayloadBytes, long expectedPayloadBytes,
        int lookupFit, int insertFit, int removeFit, int scanFit, int indexedAccessFit,
        int allocationFit, String assumptions) {
    public M3CollectionEstimate {
        Objects.requireNonNull(assumptions, "assumptions");
        if (initialPayloadBytes < 0 || expectedPayloadBytes < initialPayloadBytes) {
            throw new IllegalArgumentException("payload estimates must be nonnegative and monotonic");
        }
        M3SelectionCollectionRequest.weight(lookupFit); M3SelectionCollectionRequest.weight(insertFit);
        M3SelectionCollectionRequest.weight(removeFit); M3SelectionCollectionRequest.weight(scanFit);
        M3SelectionCollectionRequest.weight(indexedAccessFit); M3SelectionCollectionRequest.weight(allocationFit);
    }

    public long weightedPayloadBytes(int emptyPermille) {
        M3SelectionCollectionRequest.weight(emptyPermille);
        int populated = 1000 - emptyPermille;
        // Divide first so even a custom Long.MAX_VALUE estimate cannot overflow.
        return (initialPayloadBytes / 1000) * emptyPermille
                + (expectedPayloadBytes / 1000) * populated
                + ((initialPayloadBytes % 1000) * emptyPermille
                    + (expectedPayloadBytes % 1000) * populated) / 1000;
    }

    public int operationFit(M3SelectionCollectionRequest.Workload mix) {
        return (lookupFit * mix.lookups() + insertFit * mix.inserts()
                + removeFit * mix.removes() + scanFit * mix.scans()
                + indexedAccessFit * mix.indexedAccess()) / mix.total();
    }
}
