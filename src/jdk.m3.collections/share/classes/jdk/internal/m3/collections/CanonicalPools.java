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
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * Factory methods for explicit immutable-value canonicalization policies.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/canonical/CanonicalPools.java}
 * blob {@code 977827c2cab2395c3273a74ffdd72036611aa840} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class CanonicalPools {
    private CanonicalPools() {
    }

    public static <T, K> IndexedCanonicalPool<T, K> strongIndexed(
            int maxEntries,
            long maxWeight,
            Function<? super T, ? extends K> keyExtractor,
            BiPredicate<? super T, ? super T> equivalence,
            ToLongFunction<? super T> weight) {
        return new IndexedCanonicalPool<>(maxEntries, maxWeight, keyExtractor, equivalence, weight);
    }

    /**
     * Strong canonicalization using the immutable value itself as the exact map key.
     * Its equals/hashCode implementation must remain stable after admission.
     */
    public static <T> IndexedCanonicalPool<T, T> strongByEquals(
            int maxEntries, long maxWeight, ToLongFunction<? super T> weight) {
        Objects.requireNonNull(weight, "weight");
        return strongIndexed(maxEntries, maxWeight, Function.identity(), Objects::equals, weight);
    }

    public static <T, K> WeakCanonicalPool<T, K> weak(
            int maxEntries,
            long maxWeight,
            Function<? super T, ? extends K> keyExtractor,
            BiPredicate<? super T, ? super T> equivalence,
            ToLongFunction<? super T> weight) {
        return new WeakCanonicalPool<>(maxEntries, maxWeight, keyExtractor, equivalence, weight);
    }

    /** Unit-weight helper for pools bounded only by entry count. */
    public static <T> ToLongFunction<T> unitWeight() {
        return ignored -> 1L;
    }
}
