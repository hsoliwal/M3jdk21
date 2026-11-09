// Copyright 2026 Hitesh Soliwal and contributors
// Full donor dependency receiver; source custody in JSON_CLOSURE_PROVENANCE.json.
/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jdk.m3.file;

import jdk.m3.collections.M3ProgressState;
import java.time.Instant;

/**
 * Progress contract for every task or long-running process.
 *
 * <p>The surface intentionally follows the familiar Eclipse progress-monitor vocabulary while
 * retaining Synexia's machine-readable snapshot model. Implementations must be thread-safe when
 * used by concurrent workers.
 *
 * <p>An adapter over the owner {@link jdk.m3.collections.M3ProgressMonitor}: the abstract methods,
 * {@code UNKNOWN}, {@code checkCanceled()} and the shadow streams are the owner's; this interface
 * adds the snapshot model, the covariant weighted {@link #split(long)} and {@link #noop()}.
 *
 * @since ContextOS 1.0
 */
//FLLDCIM: UNIFY - adapter over jdk.m3.collections.M3ProgressMonitor (long units on both sides; jdk.m3.file.M3Progress, M3ProgressChild and M3ProgressState retired); recipe m3/qualification/progress-monitor-unify-20261106/unify.py v1
public interface M3ProgressMonitor extends jdk.m3.collections.M3ProgressMonitor {

    /**
     * Returns the latest immutable progress state.
     *
     * @return progress snapshot
     */
    M3ProgressSnapshot snapshot();

    @Override
    default M3ProgressState state() {
        M3ProgressSnapshot snapshot = snapshot();
        long startedAtMs = Math.max(0L, snapshot.timestamp().toEpochMilli() - snapshot.elapsedMs());
        return new M3ProgressState(
                snapshot.taskName(),
                snapshot.subTaskName(),
                snapshot.worked(),
                snapshot.totalWork(),
                snapshot.canceled(),
                snapshot.done(),
                0L,
                startedAtMs);
    }

    /**
     * Creates a weighted child monitor whose completed work contributes at most
     * {@code parentWork} units to this monitor.
     *
     * <p>The child is a contribution, not a reservation. Callers remain responsible for keeping
     * the sum of child weights within the parent's intended budget. Child completion credits any
     * unreported remainder exactly once, and child cancellation propagates to the parent.
     *
     * @param parentWork maximum work units contributed to this monitor
     * @return weighted child monitor
     * @throws IllegalArgumentException when {@code parentWork} is negative
     */
    @Override
    default M3ProgressMonitor split(final long parentWork) {
        return new M3ChildProgressMonitor(this, parentWork);
    }

    /**
     * Returns a no-op monitor for compatibility boundaries that cannot yet expose concrete state.
     *
     * @return no-op monitor
     */
    static M3ProgressMonitor noop() {
        return NoOpHolder.INSTANCE;
    }

    /** Lazy no-op singleton holder. */
    final class NoOpHolder {
        private static final M3ProgressMonitor INSTANCE = new M3ProgressMonitor() {
            @Override
            public void beginTask(final String taskName, final long totalWork) {}

            @Override
            public void done() {}

            @Override
            public void worked(final long work) {}

            @Override
            public void setProgress(final long worked, final long totalWork) {}

            @Override
            public void setTaskName(final String taskName) {}

            @Override
            public void subTask(final String subTaskName) {}

            @Override
            public boolean isCanceled() {
                return false;
            }

            @Override
            public void setCanceled(final boolean canceled) {}

            @Override
            public M3ProgressMonitor split(final long parentWork) {
                if (parentWork < 0L) {
                    throw new IllegalArgumentException("parentWork must be non-negative");
                }
                return this;
            }

            @Override
            public M3ProgressSnapshot snapshot() {
                return new M3ProgressSnapshot("", "", 0L, UNKNOWN, -1.0d, 0.0d, 0L, -1L, false, false, Instant.EPOCH);
            }
        };

        private NoOpHolder() {}
    }
}
