// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.ProgressMonitor;

/**
 * Strict algorithm contract whose execution always receives a progress monitor.
 *
 * <p>The functional contract of an M3 algorithm atom: the body receives the monitor (the families pass
 * {@code M3ProgressMonitor.none()} for callers without one) and observes its cancellation through
 * {@code M3ProgressChecks.checkCanceled}. Chains are lambdas; child slices are
 * {@code jdk.m3.collections.ProgressMonitor.split}.
 */
@FunctionalInterface
//FLLDCIM: RELOCATE - donor blob 0b0d30906fe8 at commit a7b7bb639802 (repository, path, URL and digests: m3/algorithms/recipe/progress-algorithm-20261108/PROVENANCE.tsv); recipe m3/algorithms/recipe/progress-algorithm-20261108/relocate.py v1
public interface ProgressAlgorithm<I, O> {

    O execute(I input, ProgressMonitor monitor);

}
