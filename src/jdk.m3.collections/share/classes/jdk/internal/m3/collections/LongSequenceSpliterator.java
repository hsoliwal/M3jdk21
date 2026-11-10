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

import jdk.m3.collections.LongSequence;

import java.util.Objects;
import java.util.Spliterator;
import java.util.function.LongConsumer;

/**
 * Owner-indirected spliterator for live indexed long sequences.
 *
 * <p>The descriptor retains the logical owner plus index range, never a replaceable backing
 * {@code long[]}. Capacity relocation may therefore replace physical storage without stranding
 * an already-created live view. Structural mutation during traversal remains the owner's contract.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/LongSequenceSpliterator.java}
 * blob {@code 435383c58c7f4085101a84b0156cdeffb056bbbb} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections receipt-aware batch (v6, RECEIPT_MAP): identifiers and package only; bytecode equal to the source modulo names).
 */
final class LongSequenceSpliterator implements Spliterator.OfLong {
    private final LongSequence owner;
    private int index;
    private final int fence;

    LongSequenceSpliterator(LongSequence owner, int index, int fence) {
        this.owner = Objects.requireNonNull(owner, "owner");
        Objects.checkFromToIndex(index, fence, owner.size());
        this.index = index;
        this.fence = fence;
    }

    @Override public OfLong trySplit() {
        int low = index;
        int middle = (low + fence) >>> 1;
        if (low >= middle) return null;
        index = middle;
        return new LongSequenceSpliterator(owner, low, middle);
    }

    @Override public boolean tryAdvance(LongConsumer action) {
        Objects.requireNonNull(action, "action");
        if (index >= fence) return false;
        action.accept(owner.get(index++));
        return true;
    }

    @Override public void forEachRemaining(LongConsumer action) {
        Objects.requireNonNull(action, "action");
        while (index < fence) action.accept(owner.get(index++));
    }

    @Override public long estimateSize() { return fence - index; }

    @Override public int characteristics() {
        return ORDERED | SIZED | SUBSIZED;
    }
}
