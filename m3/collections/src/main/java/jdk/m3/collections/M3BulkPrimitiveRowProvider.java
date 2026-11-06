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
package jdk.m3.collections;

import java.util.Arrays;

/**
 * Optional vector/batch extension of {@link M3PrimitiveRowProvider}.
 *
 * <p>The collection supplies all scratch storage. Implementations may override this method to
 * issue one database/vectorized fetch. The default reuses the supplied single-row scratch and
 * delegates to {@link #load(long, long[])} without allocating a row array per element.
 *
 * <p>Lineage: absorbed from Synexia {@code synexia-primitives/src/main/java/com/synexia/primitives/BulkPrimitiveRowProvider.java}
 * blob {@code 500b3793565196b3b0de4a225a3eba96e04fd733} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_SYNEXIA_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public interface M3BulkPrimitiveRowProvider extends M3PrimitiveRowProvider {

    /**
     * Materialize requested row ids into row-major target storage.
     *
     * @param rowIds requested logical ids
     * @param from inclusive request offset
     * @param to exclusive request offset
     * @param laneCount exact primitive lane count per row
     * @param rowScratch reusable single-row scratch owned by the collection
     * @param rowMajorTarget output beginning at offset zero for {@code rowIds[from]}
     */
    default void loadRows(
            long[] rowIds,
            int from,
            int to,
            int laneCount,
            long[] rowScratch,
            long[] rowMajorTarget) throws Exception {
        if (from < 0 || from > to || to > rowIds.length) {
            throw new IndexOutOfBoundsException(from + ".." + to);
        }
        if (laneCount < 0 || rowScratch.length < laneCount) {
            throw new IllegalArgumentException("row scratch too small");
        }
        int count = to - from;
        int required = Math.multiplyExact(count, laneCount);
        if (rowMajorTarget.length < required) {
            throw new IllegalArgumentException("row-major target too small");
        }

        int targetOffset = 0;
        for (int index = from; index < to; index++) {
            Arrays.fill(rowScratch, 0, laneCount, 0L);
            load(rowIds[index], rowScratch);
            System.arraycopy(rowScratch, 0, rowMajorTarget, targetOffset, laneCount);
            targetOffset += laneCount;
        }
    }
}
