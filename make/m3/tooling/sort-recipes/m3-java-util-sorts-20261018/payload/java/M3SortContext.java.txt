/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package java.util;

import java.util.concurrent.ProgressMonitor;
import java.util.function.IntBinaryOperator;

/** Borrowed progress and comparator state for one sorting invocation. */
final class M3SortContext {
    private final IntBinaryOperator comparator;
    private final ProgressMonitor monitor;
    private int pending;

    M3SortContext(IntBinaryOperator comparator, ProgressMonitor monitor) {
        this.comparator = Objects.requireNonNull(comparator, "comparator");
        this.monitor = monitor == ProgressMonitor.NONE ? null : monitor;
        if (this.monitor != null) this.monitor.checkCancelled();
    }

    int compare(int left, int right) {
        step();
        return comparator.applyAsInt(left, right);
    }

    void step() {
        if (monitor != null && ++pending == 1024) flush();
    }

    void finish() {
        if (monitor != null) flush();
    }

    private void flush() {
        int completed = pending;
        pending = 0;
        if (completed != 0) monitor.worked(completed);
        monitor.checkCancelled();
    }
}
