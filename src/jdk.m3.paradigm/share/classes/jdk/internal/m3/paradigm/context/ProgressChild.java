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
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.paradigm.context;

import java.util.Objects;

/**
 * Existing parent-delegating progress adapter, independent of context execution storage.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextProgressChild.java}
 * blob {@code ee274c57e17fa1dc0aa7d78ba0ccf4d92490b24e} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
final class ProgressChild {
    private ProgressChild() {}

    static ContextProgressMonitor child(
            ContextProgressMonitor parent,
            String task,
            long totalWork) {
        Objects.requireNonNull(parent, "parent");
        return new ContextProgressMonitor() {
            private boolean begun;

            @Override
            public void begin(
                    String ignored,
                    long ignoredTotal) {
                if (!begun) {
                    begun = true;
                    parent.begin(
                            Objects.requireNonNullElse(task, ""),
                            totalWork);
                }
            }

            @Override
            public void worked(long work) {
                parent.worked(work);
            }

            @Override
            public void message(String message) {
                parent.message(message);
            }

            @Override
            public boolean isCancelled() {
                return parent.isCancelled();
            }

            @Override
            public void cancel() {
                parent.cancel();
            }

            @Override
            public void setCancelled(boolean cancelled) {
                parent.setCancelled(cancelled);
            }

            @Override
            public void done() {
                parent.done();
            }

            @Override
            public ProgressSnapshot snapshot() {
                return parent.snapshot();
            }
        };
    }
}
