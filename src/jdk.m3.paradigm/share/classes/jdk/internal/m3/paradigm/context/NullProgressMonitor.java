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

import java.util.List;

/**
 * Allocation-free null object used when progress is intentionally ignored.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextNullProgressMonitor.java}
 * blob {@code e6f96fb072382975e5154a855b5d21be95e50040} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class NullProgressMonitor
        implements ContextProgressMonitor {

    public static final NullProgressMonitor INSTANCE =
            new NullProgressMonitor();

    private static final ProgressSnapshot SNAPSHOT =
            new ProgressSnapshot(
                    "",
                    UNKNOWN_WORK,
                    0L,
                    "",
                    false,
                    false,
                    List.of());

    private NullProgressMonitor() { }

    public static NullProgressMonitor instance() {
        return INSTANCE;
    }

    @Override
    public ProgressSnapshot snapshot() {
        return SNAPSHOT;
    }

    @Override
    public ContextProgressMonitor child(
            String task,
            long totalWork) {
        return this;
    }
}
