// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import jdk.m3.collections.DefaultProgressMonitor;
import jdk.m3.collections.ProgressMonitor;

/** Single process-wide progress root shared by common, job and lazy collection layers. */
public final class SystemProgress {
    private static final DefaultProgressMonitor ROOT = createRoot();

    private SystemProgress() {}

    public static ProgressMonitor get() { return ROOT; }

    public static ProgressMonitor split(long systemWork) {
        return ROOT.split(systemWork);
    }

    public static ProgressMonitor resolve(ProgressMonitor monitor, long systemWork) {
        return monitor == null ? split(systemWork) : monitor;
    }

    private static DefaultProgressMonitor createRoot() {
        DefaultProgressMonitor monitor = new DefaultProgressMonitor();
        monitor.beginTask("Synexia system", ProgressMonitor.UNKNOWN);
        return monitor;
    }
}
