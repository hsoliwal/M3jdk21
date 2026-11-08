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
import java.util.function.Consumer;

/**
 * Bounded best-K by stable comparator: earlier equal candidates win; candidates must be non-null.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/ShapeRankedSelection.java}
 * blob {@code e250ebc73b63474e9a8e48635dced8120a31fbb4} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public interface M3ShapeRankedSelection<E> extends M3ShapeContainer {
    /** Exact retained limit, including zero. */
    int capacity();
    /** Admit or replace a strictly worse cutoff; comparator failure leaves membership unchanged. */
    boolean offer(E value);
    /** Worst retained value; throws NoSuchElementException when empty. */
    E worst();
    /** Detached immutable best-first snapshot, with input arrival order breaking ties. */
    List<E> snapshotBestFirst();
    /** Visit every retained occurrence without comparisons; mutation during traversal is rejected. */
    void forEachRetained(Consumer<? super E> action);
}
