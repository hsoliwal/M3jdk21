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
package jdk.internal.m3.collections.packed;

import java.util.Objects;

/**
 * Zero-object mechanics for redistributing sorted primitive pages around insertion and deletion.
 *
 * <p>The insert plan uses a signed move: negative means move a prefix to the left sibling,
 * positive means move a suffix to the right sibling, and zero means no safe redistribution.
 * The delete plan returns a positive count to move from a fuller sibling into one sparse page.
 * Both paths preserve sorted order and use the same primitive movement atoms.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedSortedPageRebalanceAtoms.java}
 * blob {@code 552978ba1c0dc11871f0ee90bc77042014c857f8} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
final class PackedSortedPageRebalanceAtoms {
    private PackedSortedPageRebalanceAtoms() {}

    static int planMove(
            int insertionInPage,
            int pageCapacity,
            int leftCount,
            int rightCount) {
        if (pageCapacity < 4) {
            throw new IllegalArgumentException("pageCapacity must be >= 4");
        }
        if (insertionInPage < 0 || insertionInPage > pageCapacity) {
            throw new IndexOutOfBoundsException("insertionInPage");
        }
        validateNeighborCount(leftCount, pageCapacity);
        validateNeighborCount(rightCount, pageCapacity);

        int leftFree = leftCount < 0 ? 0 : pageCapacity - leftCount;
        int rightFree = rightCount < 0 ? 0 : pageCapacity - rightCount;
        boolean canLeft = leftFree >= 2;
        boolean canRight = rightFree >= 2;
        if (!canLeft && !canRight) {
            return 0;
        }

        if (canLeft && canRight) {
            int midpoint = pageCapacity >>> 1;
            if (insertionInPage < midpoint) {
                return -balancedMove(leftFree);
            }
            if (insertionInPage > midpoint) {
                return balancedMove(rightFree);
            }
            return leftFree > rightFree
                    ? -balancedMove(leftFree)
                    : balancedMove(rightFree);
        }
        return canLeft ? -balancedMove(leftFree) : balancedMove(rightFree);
    }

    /**
     * Plans Abseil-style delete-side rebalancing after merge is known not to fit.
     *
     * <p>A sparse page below half capacity may borrow from a sibling above half capacity. The
     * result balances the pair without draining the donor below the minimum. Zero means keep the
     * current page geometry.</p>
     */
    static int planDeleteMove(
            int sparseCount,
            int siblingCount,
            int pageCapacity) {
        if (pageCapacity < 4) {
            throw new IllegalArgumentException("pageCapacity must be >= 4");
        }
        validatePageCount(sparseCount, pageCapacity);
        validatePageCount(siblingCount, pageCapacity);

        int minimum = Math.max(1, pageCapacity >>> 1);
        if (sparseCount >= minimum
                || siblingCount <= minimum
                || sparseCount + siblingCount <= pageCapacity) {
            return 0;
        }

        int balanced = (siblingCount - sparseCount) >>> 1;
        int donorSlack = siblingCount - minimum;
        return Math.max(0, Math.min(balanced, donorSlack));
    }

    static void movePrefixToLeft(
            long[] lane,
            int leftOffset,
            int leftCount,
            int pageOffset,
            int pageCount,
            int move) {
        Objects.requireNonNull(lane, "lane");
        if (move <= 0 || move >= pageCount) {
            throw new IllegalArgumentException("move");
        }
        Objects.checkFromIndexSize(leftOffset, leftCount + move, lane.length);
        Objects.checkFromIndexSize(pageOffset, pageCount, lane.length);
        if ((long) leftOffset + leftCount + move > pageOffset) {
            throw new IllegalArgumentException("left sibling capacity exceeded");
        }
        System.arraycopy(lane, pageOffset, lane, leftOffset + leftCount, move);
        System.arraycopy(lane, pageOffset + move, lane, pageOffset, pageCount - move);
    }

    static void moveSuffixToRight(
            long[] lane,
            int pageOffset,
            int pageCount,
            int rightOffset,
            int rightCount,
            int move) {
        Objects.requireNonNull(lane, "lane");
        if (move <= 0 || move >= pageCount) {
            throw new IllegalArgumentException("move");
        }
        Objects.checkFromIndexSize(pageOffset, pageCount, lane.length);
        Objects.checkFromIndexSize(rightOffset, rightCount + move, lane.length);
        if ((long) pageOffset + pageCount > rightOffset) {
            throw new IllegalArgumentException("pages overlap");
        }
        System.arraycopy(lane, rightOffset, lane, rightOffset + move, rightCount);
        System.arraycopy(lane, pageOffset + pageCount - move, lane, rightOffset, move);
    }

    private static int balancedMove(int freeSlots) {
        return Math.max(1, freeSlots >>> 1);
    }

    private static void validateNeighborCount(int count, int pageCapacity) {
        if (count < -1 || count > pageCapacity) {
            throw new IllegalArgumentException("neighbor page count");
        }
    }

    private static void validatePageCount(int count, int pageCapacity) {
        if (count < 0 || count > pageCapacity) {
            throw new IllegalArgumentException("page count");
        }
    }
}
