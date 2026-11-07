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

import java.util.List;
import java.util.function.ObjIntConsumer;

/**
 * Dense index/value sequence with implicit primitive indexes.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/IndexedValues.java}
 * blob {@code 1c00b614033a54c017c77597fcf17a8da7df2b47} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface M3IndexedValues<T> {
    void add(T value);
    int size();
    T value(int index);
    void forEach(ObjIntConsumer<? super T> consumer);
    List<T> valuesView();
}
