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
package com.m3.file;

import com.m3.file.M3ProgressCalculations;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Objects;
import java.util.function.LongSupplier;

/** Weighted single-use child that contributes bounded work to an existing parent monitor. */
final class M3ChildProgressMonitor implements M3ProgressMonitor {
    private final M3ProgressMonitor parent;
    private final long parentWork;
    private final LongSupplier parentGeneration;
    private final long expectedParentGeneration;
    private String taskName = "";
    private String subTaskName = "";
    private long worked;
    private long totalWork = UNKNOWN;
    private long forwarded;
    private long startedAtMs = System.currentTimeMillis();
    private boolean canceled;
    private boolean done;
    private boolean started;

    M3ChildProgressMonitor(final M3ProgressMonitor parent, final long parentWork) {
        this(parent, parentWork, null, 0L);
    }

    M3ChildProgressMonitor(
            final M3ProgressMonitor parent,
            final long parentWork,
            final LongSupplier parentGeneration,
            final long expectedParentGeneration) {
        this.parent = Objects.requireNonNull(parent, "parent");
        if (parentWork < 0L) {
            throw new IllegalArgumentException("parentWork must be non-negative");
        }
        this.parentWork = parentWork;
        this.parentGeneration = parentGeneration;
        this.expectedParentGeneration = expectedParentGeneration;
    }

    @Override
    public void beginTask(final String taskName, final long totalWork) {
        synchronized (this) {
            if (started || worked != 0L || done) {
                throw new IllegalStateException("child progress is single-use");
            }
            started = true;
            this.taskName = Objects.requireNonNullElse(taskName, "");
            subTaskName = "";
            worked = 0L;
            this.totalWork = totalWork;
            startedAtMs = System.currentTimeMillis();
        }
        parent.subTask(this.taskName);
    }

    @Override
    public void done() {
        final long delta;
        synchronized (this) {
            if (done) {
                return;
            }
            done = true;
            if (totalWork > 0L) {
                worked = totalWork;
            }
            delta = advance(parentWork);
        }
        creditParent(delta);
    }

    @Override
    public void worked(final long work) {
        if (work < 0L) {
            throw new IllegalArgumentException("work must be non-negative");
        }
        if (work == 0L) {
            return;
        }
        final long delta;
        synchronized (this) {
            if (done) {
                return;
            }
            worked = boundedAdd(worked, work, totalWork);
            delta = advance(targetParentWork());
        }
        creditParent(delta);
    }

    @Override
    public void setProgress(final long worked, final long totalWork) {
        final long delta;
        synchronized (this) {
            if (done) {
                return;
            }
            this.totalWork = totalWork;
            this.worked = totalWork > 0L
                    ? Math.min(Math.max(0L, worked), totalWork)
                    : Math.max(0L, worked);
            delta = advance(targetParentWork());
        }
        creditParent(delta);
    }

    @Override
    public void setTaskName(final String taskName) {
        final String value = Objects.requireNonNullElse(taskName, "");
        synchronized (this) {
            this.taskName = value;
        }
        parent.subTask(value);
    }

    @Override
    public void subTask(final String subTaskName) {
        final String value = Objects.requireNonNullElse(subTaskName, "");
        synchronized (this) {
            this.subTaskName = value;
        }
        parent.subTask(value);
    }

    @Override
    public boolean isCanceled() {
        synchronized (this) {
            if (canceled) {
                return true;
            }
        }
        return parent.isCanceled();
    }

    @Override
    public void setCanceled(final boolean canceled) {
        synchronized (this) {
            this.canceled = canceled;
        }
        if (canceled) {
            parent.setCanceled(true);
        }
    }

    @Override
    public M3ProgressSnapshot snapshot() {
        final long currentWorked;
        final long currentTotal;
        final String currentTask;
        final String currentSubTask;
        final long elapsedMs;
        final boolean currentDone;
        synchronized (this) {
            currentWorked = worked;
            currentTotal = totalWork;
            currentTask = taskName;
            currentSubTask = subTaskName;
            elapsedMs = Math.max(0L, System.currentTimeMillis() - startedAtMs);
            currentDone = done;
        }
        final double percentage =
                M3ProgressCalculations.calculatePercentage(currentWorked, currentTotal);
        final double rate = M3ProgressCalculations.calculateRate(currentWorked, elapsedMs);
        final long etaMs = M3ProgressCalculations.calculateEta(currentWorked, currentTotal, rate);
        return new M3ProgressSnapshot(
                currentTask,
                currentSubTask,
                currentWorked,
                currentTotal,
                percentage,
                rate,
                elapsedMs,
                etaMs,
                isCanceled(),
                currentDone,
                Instant.now());
    }

    private void creditParent(final long delta) {
        if (delta <= 0L || staleParent()) {
            return;
        }
        final M3ProgressSnapshot parentSnapshot = parent.snapshot();
        if (!parentSnapshot.done()) {
            parent.worked(delta);
        }
    }

    private boolean staleParent() {
        return parentGeneration != null
                && parentGeneration.getAsLong() != expectedParentGeneration;
    }

    private long targetParentWork() {
        if (done) {
            return parentWork;
        }
        if (totalWork <= 0L || worked <= 0L || parentWork == 0L) {
            return 0L;
        }
        if (worked >= totalWork) {
            return parentWork;
        }
        return scale(worked, parentWork, totalWork);
    }

    private long advance(final long target) {
        final long bounded = Math.max(forwarded, Math.min(parentWork, target));
        final long delta = bounded - forwarded;
        forwarded = bounded;
        return delta;
    }

    private static long scale(final long value, final long weight, final long total) {
        try {
            return Math.multiplyExact(value, weight) / total;
        } catch (ArithmeticException overflow) {
            return BigInteger.valueOf(value)
                    .multiply(BigInteger.valueOf(weight))
                    .divide(BigInteger.valueOf(total))
                    .longValueExact();
        }
    }

    private static long boundedAdd(
            final long current, final long increment, final long total) {
        final long added;
        try {
            added = Math.addExact(current, increment);
        } catch (ArithmeticException overflow) {
            return total > 0L ? total : Long.MAX_VALUE;
        }
        return total > 0L ? Math.min(total, added) : added;
    }
}
