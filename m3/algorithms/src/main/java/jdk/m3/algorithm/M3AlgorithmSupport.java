// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.M3ProgressMonitor;
import java.util.Objects;

//FLLDCIM: RELOCATE - donor blob ee97c7b89da3 at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/families-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/families-20261106/relocate.py v1
final class M3AlgorithmSupport {
    private M3AlgorithmSupport() {
    }

    static M3ProgressMonitor begin(
            M3ProgressMonitor monitor,
            String task,
            long totalWork) {
        Objects.requireNonNull(monitor, "monitor").checkCanceled();
        int total = totalWork < 0L
                ? M3ProgressMonitor.UNKNOWN
                : (int) Math.min(Integer.MAX_VALUE, totalWork);
        monitor.beginTask(task, total);
        return monitor;
    }

    static void tick(M3ProgressMonitor monitor) {
        monitor.checkCanceled();
        monitor.worked(1);
    }

    static int[] emptyPair() {
        return new int[] {-1, -1};
    }
}
