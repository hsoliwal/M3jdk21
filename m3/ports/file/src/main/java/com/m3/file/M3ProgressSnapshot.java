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

import java.time.Instant;

/**
 * Immutable machine-readable snapshot of a long-running task.
 *
 * @param taskName current task name
 * @param subTaskName current sub-task name
 * @param worked completed work units
 * @param totalWork total work units, or {@link M3ProgressMonitor#UNKNOWN}
 * @param percentage completion percentage, or {@code -1} when unknown
 * @param ratePerSecond current processing rate
 * @param elapsedMs elapsed time in milliseconds
 * @param etaMs estimated remaining time in milliseconds, or {@code -1} when unknown
 * @param canceled whether cancellation was requested
 * @param done whether the task has completed
 * @param timestamp snapshot timestamp
 * @since ContextOS 1.0
 */
public record M3ProgressSnapshot(
        String taskName,
        String subTaskName,
        long worked,
        long totalWork,
        double percentage,
        double ratePerSecond,
        long elapsedMs,
        long etaMs,
        boolean canceled,
        boolean done,
        Instant timestamp) {}
