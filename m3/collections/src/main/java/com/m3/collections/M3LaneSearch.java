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
package com.m3.collections;

/**
 * Bisection kernels over sorted primitive lanes: arrays, offsets and a key; no allocation, no boxing.
 *
 * <p>Catalogue: binary-search (lower / upper bound, the LeetCode 35 / 34 ideas). The midpoint is
 * {@code from + ((to - from) >>> 1)}, so the sum of two valid indexes never overflows. Landed by
 * {@code M3LeetCodifyReplaceRecipe}; every call site carries a generated differential test.</p>
 */
//FLLDCIM: LEETCODIFY — owner of MANUAL_BINARY_SEARCH LOWER_BOUND / UPPER_BOUND kernels (DOOP: lanes + offsets)
public final class M3LaneSearch {
    private M3LaneSearch() {}

    /** First index in [from, to) whose value is not below key, or to; lane[from, to) must be non-decreasing. */
    public static int lowerBound(int[] lane, int from, int to, int key) {
        int low = from;
        int high = to;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (lane[mid] < key) low = mid + 1;
            else high = mid;
        }
        return low;
    }

    /** First index in [from, to) whose value is not below key, or to; lane[from, to) must be non-decreasing. */
    public static int lowerBound(long[] lane, int from, int to, long key) {
        int low = from;
        int high = to;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (lane[mid] < key) low = mid + 1;
            else high = mid;
        }
        return low;
    }

    /** First index in [from, to) whose value is above key, or to; lane[from, to) must be non-decreasing. */
    public static int upperBound(int[] lane, int from, int to, int key) {
        int low = from;
        int high = to;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (lane[mid] <= key) low = mid + 1;
            else high = mid;
        }
        return low;
    }

    /** First index in [from, to) whose value is above key, or to; lane[from, to) must be non-decreasing. */
    public static int upperBound(long[] lane, int from, int to, long key) {
        int low = from;
        int high = to;
        while (low < high) {
            int mid = low + ((high - low) >>> 1);
            if (lane[mid] <= key) low = mid + 1;
            else high = mid;
        }
        return low;
    }
}
