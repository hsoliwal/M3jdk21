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
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;

/**
 * Insertion-time filtering, independent of the target collection's storage shape.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/LongArrayAdmission.java}
 * blob {@code 8e9472a04052a694ccb315fb9775feed05505c88} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class LongArrayAdmission {
    private LongArrayAdmission() { }

    /**
     * Forwards accepted values in input order. No boxing, scratch array or entry object.
     * An exception leaves the already-forwarded prefix in the target. Inputs must not
     * be modified by callbacks. The count is forwarded values, not distinct insertions.
     */
    public static int acceptIf(long[] source, int from, int to,
            LongPredicate predicate, LongConsumer target) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(target, "target");
        Objects.checkFromToIndex(from, to, source.length);
        int accepted = 0;
        for (int i = from; i < to; i++) {
            long value = source[i];
            if (predicate.test(value)) {
                target.accept(value);
                accepted++;
            }
        }
        return accepted;
    }
}
