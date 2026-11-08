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

import jdk.internal.m3.collections.M3SelectionCollectionRequest.Order;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Provider promises, checked before any fuzzy score is calculated.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/selection/CollectionCapabilities.java}
 * blob {@code ab3c24670751b64f1ebde12318740b87292a9d62} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public record M3CollectionCapabilities(Order order, boolean concurrent, boolean stableHandles,
        boolean lazyStorage, boolean noPayloadCopyOnGrowth, int maximumExpectedSize,
        int minimumBlockSize, int maximumBlockSize, int defaultBlockSize) {
    public M3CollectionCapabilities {
        Objects.requireNonNull(order, "order");
        if (maximumExpectedSize < 0 || minimumBlockSize < 0
                || maximumBlockSize < minimumBlockSize
                || defaultBlockSize < minimumBlockSize || defaultBlockSize > maximumBlockSize
                || (minimumBlockSize == 0 && maximumBlockSize != 0)) {
            throw new IllegalArgumentException("invalid size/block capability range");
        }
    }

    /** Primitive representation of the existing order/concurrency/storage promises. */
    public int aspects() {
        return M3CollectionAspectMask.selectionCapabilities(this);
    }

    public int blockSize(M3SelectionCollectionRequest<?> request) {
        int specified = request.requirements().blockSize();
        return specified == 0 ? defaultBlockSize : specified;
    }

    public List<String> rejections(M3SelectionCollectionRequest<?> request) {
        var wanted = request.requirements();
        List<String> reasons = new ArrayList<>();
        int missing = M3CollectionAspectMask.missing(aspects(), wanted.aspects());
        if ((missing & M3CollectionAspectMask.ORDER_MASK) != 0) {
            reasons.add("order requires " + wanted.order() + "; provides " + order);
        }
        if ((missing & M3CollectionAspectMask.CONCURRENCY_CONCURRENT) != 0) {
            reasons.add("concurrency is not provided");
        }
        if ((missing & M3CollectionAspectMask.STABLE_HANDLES) != 0) {
            reasons.add("stable handles are not provided");
        }
        if ((missing & M3CollectionAspectMask.LAZY_STORAGE) != 0) {
            reasons.add("lazy backing storage is not provided");
        }
        if ((missing & M3CollectionAspectMask.NO_PAYLOAD_COPY_ON_GROWTH) != 0) {
            reasons.add("growth can copy the entire primitive payload");
        }
        if (request.expectedSize() > maximumExpectedSize) {
            reasons.add("expected size exceeds supported " + maximumExpectedSize);
        }
        int block = blockSize(request);
        if (block < minimumBlockSize || block > maximumBlockSize) {
            reasons.add("block size requires " + block + "; supported "
                    + minimumBlockSize + ".." + maximumBlockSize);
        }
        return List.copyOf(reasons);
    }
}
