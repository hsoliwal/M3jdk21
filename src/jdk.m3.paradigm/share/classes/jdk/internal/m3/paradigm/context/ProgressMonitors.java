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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Process-global fallback provider for context progress monitors.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextProgressMonitors.java}
 * blob {@code 75c0b7b258a0d280b9e237b6138f34d15194defb} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ProgressMonitors {

    public static final String PROPERTY_KEY = "progress-monitor";
    public static final String LEGACY_PROPERTY_KEY = "synexia.progress.monitor";

    private static final Supplier<? extends ContextProgressMonitor> NULL_SUPPLIER =
            NullProgressMonitor::instance;

    private static final AtomicReference<Supplier<? extends ContextProgressMonitor>> GLOBAL =
            new AtomicReference<>(NULL_SUPPLIER);

    private ProgressMonitors() {
    }

    public static ContextProgressMonitor global() {
        ContextProgressMonitor monitor = GLOBAL.get().get();
        return monitor == null ? NullProgressMonitor.instance() : monitor;
    }

    public static Registration install(
            Supplier<? extends ContextProgressMonitor> supplier) {
        Supplier<? extends ContextProgressMonitor> normalized =
                Objects.requireNonNull(supplier, "supplier");
        Supplier<? extends ContextProgressMonitor> previous =
                GLOBAL.getAndSet(normalized);
        return new Registration(previous);
    }

    public static Registration install(ContextProgressMonitor monitor) {
        ContextProgressMonitor normalized = Objects.requireNonNull(monitor, "monitor");
        return install(() -> normalized);
    }

    public static void reset() {
        GLOBAL.set(NULL_SUPPLIER);
    }

    /** Restores the previous provider when closed. */
    public static final class Registration implements AutoCloseable {

        private final Supplier<? extends ContextProgressMonitor> previous;
        private boolean closed;

        private Registration(Supplier<? extends ContextProgressMonitor> previous) {
            this.previous = previous;
        }

        @Override
        public synchronized void close() {
            if (!closed) {
                GLOBAL.set(previous);
                closed = true;
            }
        }
    }
}
