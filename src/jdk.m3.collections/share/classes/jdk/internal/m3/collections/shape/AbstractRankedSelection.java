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
package jdk.internal.m3.collections.shape;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Callback mutation exclusion shared by scan and existing-heap adapters.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/AbstractRankedSelection.java}
 * blob {@code ad78ead03094d51af335eba6b199aae7c0737fe8} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
abstract class AbstractRankedSelection<E> extends ShapeGuard implements ShapeRankedSelection<E> {
    final Comparator<? super E> comparator;
    AbstractRankedSelection(CollectionRequirements required, Comparator<? super E> comparator) {
        super(required); this.comparator = Objects.requireNonNull(comparator, "comparator");
    }
    abstract boolean admit(E value);
    abstract List<E> snapshot();
    abstract void walk(Consumer<? super E> action);
    @Override public final int capacity() { return required.maximumSize(); }
    @Override public final boolean offer(E value) {
        writable(); Objects.requireNonNull(value, "candidate"); enter();
        try { return capacity() != 0 && admit(value); } finally { leave(); }
    }
    @Override public final List<E> snapshotBestFirst() {
        enter(); try { return snapshot(); } finally { leave(); }
    }
    @Override public final void forEachRetained(Consumer<? super E> action) {
        Objects.requireNonNull(action, "action"); enter();
        try { walk(action); } finally { leave(); }
    }
}
