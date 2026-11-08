// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.algorithm;

import jdk.m3.algorithm.M3ProgressMonitor;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CancellationException;

/** Null-safe monitor and cancellation checks: thread interruption first, then the monitor. */
//FLLDCIM: RELOCATE - donor blob 8507c2772316 at commit 78cf2ad4c7e5 (repository, path, URL and digests: m3/algorithms/recipe/catalogue-kernels-20261107/PROVENANCE.tsv); recipe m3/algorithms/recipe/catalogue-kernels-20261107/relocate.py v1
public final class M3ProgressChecks {
    private M3ProgressChecks() { }

    public static M3ProgressMonitor nonNull(M3ProgressMonitor monitor) {
        return monitor == null ? M3ProgressMonitor.none() : monitor;
    }

    public static void checkCanceled(M3ProgressMonitor monitor) {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Algorithm thread interrupted");
        }
        monitor.checkCanceled();
    }

}
