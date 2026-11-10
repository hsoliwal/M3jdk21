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

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Parallel pair sequence contract without retained pair objects.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PairSequence.java}
 * blob {@code 12c2199783b772aa2d2e8acb5fb624f1f1a8ddac} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface PairSequence<A, B> {
    void add(A first, B second);
    int size();
    boolean isEmpty();
    A left(int index);
    B right(int index);
    void forEach(BiConsumer<? super A, ? super B> consumer);
    List<A> leftView();
    List<B> rightView();
}
