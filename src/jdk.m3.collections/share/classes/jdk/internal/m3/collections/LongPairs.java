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

import jdk.m3.collections.LongLongConsumer;
import jdk.m3.collections.LongLongPredicate;

/**
 * Parallel primitive long/long pair sequence contract.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/LongPairs.java}
 * blob {@code df04d7f003befd58431cc77e4dc7b45a1bc0c5a8} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface LongPairs {
    void add(long first, long second);
    long first(int index);
    long second(int index);
    void set(int index, long first, long second);
    void removeAt(int index);
    int filterInPlace(LongLongPredicate predicate);
    void forEach(LongLongConsumer consumer);
    int size();
    boolean isEmpty();
    void clear();
    long[] firstToArray();
    long[] secondToArray();
}
