/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

/**
 * Expected-head conflict; caller must inspect new state before deciding to retry.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/memory/MementoConflictException.java}
 * blob {@code eedd4c673e7e2000e073e46e99eb98819ee03b80} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MementoConflictException extends IllegalStateException {
    private static final long serialVersionUID = 1L;
    private final long expected;
    private final long actual;

    public MementoConflictException(long expected, long actual) {
        super("Context revision conflict: expected " + expected + ", actual " + actual);
        this.expected = expected;
        this.actual = actual;
    }

    /** Expected revision. */
    public long expected() { return expected; }
    /** Actual revision. */
    public long actual() { return actual; }
}
