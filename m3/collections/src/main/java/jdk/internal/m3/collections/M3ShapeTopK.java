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

import java.util.function.ObjLongConsumer;

/**
 * Bounded smallest-K signed-long scores; duplicate scores occupy separate positions.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/ShapeTopK.java}
 * blob {@code 4774bca9a0ca3aeca8a69f33e216ee0523d936c7} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public interface M3ShapeTopK<E> extends M3ShapeContainer {
    /** Exact logical limit. */
    int capacity();
    /** Admit below capacity or replace a strictly worse cutoff; zero capacity rejects. */
    boolean offer(E value, long score);
    /** Greatest retained score; throws NoSuchElementException when empty. */
    long cutoff();
    /** One payload at the cutoff; tied payload choice is unspecified. Empty access throws. */
    E worst();
    /** Remove one payload at the cutoff; throws NoSuchElementException when empty. */
    E removeWorst();
    /** Visit retained payloads and scores in unspecified order; mutation is rejected. */
    void forEach(ObjLongConsumer<? super E> action);
}
