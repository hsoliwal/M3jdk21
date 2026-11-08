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
package jdk.internal.m3.collections.shape;

import java.util.function.Consumer;

/**
 * Aggregates cleanup failures while preserving the primary operation failure and all cleanup attempts.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/SelectionCleanup.java}
 * blob {@code 9e230b2634478d4fee9400ade390f1e2d05a67ce} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
final class SelectionCleanup<E> {
    private final Consumer<? super E> action;
    private Throwable failure;
    SelectionCleanup(Consumer<? super E> action) { this.action = action; }
    void fail(Throwable error) {
        if (failure == null) { failure = error; }
        else if (failure != error) { failure.addSuppressed(error); }
    }
    void release(E value) {
        try { action.accept(value); } catch (RuntimeException | Error error) { fail(error); }
    }
    void throwIfFailed() {
        if (failure instanceof RuntimeException error) { throw error; }
        if (failure instanceof Error error) { throw error; }
    }
}
