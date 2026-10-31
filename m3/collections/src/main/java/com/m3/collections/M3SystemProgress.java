// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

/** Single process-wide progress root shared by common, job and lazy collection layers. */
public final class M3SystemProgress {
    private static final M3DefaultProgressMonitor ROOT = createRoot();

    private M3SystemProgress() {}

    public static M3ProgressMonitor get() { return ROOT; }

    public static M3ProgressMonitor split(long systemWork) {
        return ROOT.split(systemWork);
    }

    public static M3ProgressMonitor resolve(M3ProgressMonitor monitor, long systemWork) {
        return monitor == null ? split(systemWork) : monitor;
    }

    private static M3DefaultProgressMonitor createRoot() {
        M3DefaultProgressMonitor monitor = new M3DefaultProgressMonitor();
        monitor.beginTask("Synexia system", M3ProgressMonitor.UNKNOWN);
        return monitor;
    }
}
