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
package jdk.m3.collections;

import java.util.Objects;

/**
 * Non-owning, full-domain signed long[] binding. No reserved value sentinel or wrapper.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/LongBoundaryTemplate.java}
 * blob {@code 8f652c457eff5e8f8972f437cf3af64814ee8d45} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3LongBoundaryTemplate extends M3MonotonicBoundaryTemplate {
    private final long[] values;

    public M3LongBoundaryTemplate(long[] values) {
        super(Objects.requireNonNull(values, "values").length);
        this.values = values;
    }

    @Override protected int compareAt(int left, int right) {
        return Long.compare(values[left], values[right]);
    }
}
