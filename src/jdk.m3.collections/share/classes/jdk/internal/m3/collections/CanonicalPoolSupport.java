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
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/canonical/CanonicalPoolSupport.java}
 * blob {@code 89735f97fb9115e66b48ed0bcbea8f71f44f73dc} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
final class CanonicalPoolSupport<T, K> {
    private final Function<? super T, ? extends K> keyExtractor;
    private final BiPredicate<? super T, ? super T> equivalence;
    private final ToLongFunction<? super T> weight;

    CanonicalPoolSupport(
            Function<? super T, ? extends K> keyExtractor,
            BiPredicate<? super T, ? super T> equivalence,
            ToLongFunction<? super T> weight) {
        this.keyExtractor = Objects.requireNonNull(keyExtractor, "keyExtractor");
        this.equivalence = Objects.requireNonNull(equivalence, "equivalence");
        this.weight = Objects.requireNonNull(weight, "weight");
    }

    K key(T candidate) {
        return Objects.requireNonNull(keyExtractor.apply(Objects.requireNonNull(candidate, "candidate")),
                "canonical key");
    }

    boolean equivalent(T left, T right) {
        return equivalence.test(left, right);
    }

    long weight(T value) {
        long result = weight.applyAsLong(value);
        if (result < 0) {
            throw new IllegalArgumentException("canonical value weight must be non-negative");
        }
        return result;
    }
}
