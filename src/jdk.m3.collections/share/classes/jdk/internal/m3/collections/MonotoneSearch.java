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

import java.util.Objects;
import java.util.function.IntPredicate;

/**
 * Partition search over a stable false-prefix/true-suffix predicate; no array required.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/MonotoneSearch.java}
 * blob {@code 049b81a019606208491d52ee0de488c3c7e2c472} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MonotoneSearch {
    private MonotoneSearch() { }

    /**
     * First true index in [fromInclusive,toExclusive), or toExclusive if all false.
     * Requires 0 &lt;= fromInclusive &lt;= toExclusive. Empty ranges do not call predicate.
     * Monotonicity is a binding obligation, not an O(n) validation scan performed here.
     */
    public static int firstTrue(int fromInclusive, int toExclusive, IntPredicate predicate) {
        Objects.requireNonNull(predicate, "predicate");
        if (fromInclusive < 0 || toExclusive < fromInclusive) {
            throw new IllegalArgumentException("range");
        }
        int lo = fromInclusive;
        int hi = toExclusive;
        while (lo < hi) {
            int mid = lo + ((hi - lo) >>> 1);
            if (predicate.test(mid)) hi = mid;
            else lo = mid + 1;
        }
        return lo;
    }
}
