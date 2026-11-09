// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.ProgressMonitor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

//FLLDCIM: RELOCATE - donor blob 8ab74c69342a at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/families-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/families-20261106/relocate.py v1
public final class IntervalGreedyAlgorithms {
    private IntervalGreedyAlgorithms() {
    }

    public record Interval(int start, int end) {
        public Interval {
            if (end < start) {
                throw new IllegalArgumentException("interval end precedes start");
            }
        }
    }

    public static List<Interval> merge(
            List<Interval> intervals,
            ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "merge-intervals", intervals.size());
        try {
            if (intervals.isEmpty()) {
                return List.of();
            }
            List<Interval> sorted = intervals.stream()
                    .sorted(Comparator.comparingInt(Interval::start)
                            .thenComparingInt(Interval::end))
                    .toList();
            List<Interval> result = new ArrayList<>();
            int start = sorted.getFirst().start();
            int end = sorted.getFirst().end();
            for (int index = 1; index < sorted.size(); index++) {
                AlgorithmSupport.tick(monitor);
                Interval current = sorted.get(index);
                if (current.start() <= end) {
                    end = Math.max(end, current.end());
                } else {
                    result.add(new Interval(start, end));
                    start = current.start();
                    end = current.end();
                }
            }
            result.add(new Interval(start, end));
            return List.copyOf(result);
        } finally {
            monitor.done();
        }
    }


    public static List<Interval> insert(
            List<Interval> sortedNonOverlapping,
            Interval inserted,
            ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "insert-interval", sortedNonOverlapping.size());
        try {
            List<Interval> result = new ArrayList<>(sortedNonOverlapping.size() + 1);
            int index = 0;
            while (index < sortedNonOverlapping.size()
                    && sortedNonOverlapping.get(index).end() < inserted.start()) {
                AlgorithmSupport.tick(monitor);
                result.add(sortedNonOverlapping.get(index++));
            }
            int start = inserted.start();
            int end = inserted.end();
            while (index < sortedNonOverlapping.size()
                    && sortedNonOverlapping.get(index).start() <= end) {
                AlgorithmSupport.tick(monitor);
                Interval current = sortedNonOverlapping.get(index++);
                start = Math.min(start, current.start());
                end = Math.max(end, current.end());
            }
            result.add(new Interval(start, end));
            while (index < sortedNonOverlapping.size()) {
                AlgorithmSupport.tick(monitor);
                result.add(sortedNonOverlapping.get(index++));
            }
            return List.copyOf(result);
        } finally {
            monitor.done();
        }
    }

    public static int eraseOverlapCount(
            List<Interval> intervals,
            ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "erase-overlap-intervals", intervals.size());
        try {
            if (intervals.isEmpty()) {
                return 0;
            }
            List<Interval> sorted = intervals.stream()
                    .sorted(Comparator.comparingInt(Interval::end)
                            .thenComparingInt(Interval::start))
                    .toList();
            int kept = 0;
            int lastEnd = Integer.MIN_VALUE;
            for (Interval interval : sorted) {
                AlgorithmSupport.tick(monitor);
                if (interval.start() >= lastEnd) {
                    kept++;
                    lastEnd = interval.end();
                }
            }
            return intervals.size() - kept;
        } finally {
            monitor.done();
        }
    }

    public static boolean canReachEnd(int[] jumps, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "jump-game", jumps.length);
        try {
            long farthest = 0L;
            for (int index = 0; index < jumps.length; index++) {
                AlgorithmSupport.tick(monitor);
                if (index > farthest) {
                    return false;
                }
                farthest = Math.max(farthest, (long) index + jumps[index]);
                if (farthest >= jumps.length - 1) {
                    return true;
                }
            }
            return jumps.length <= 1;
        } finally {
            monitor.done();
        }
    }

    public static int minimumJumps(int[] jumps, ProgressMonitor monitor) {
        AlgorithmSupport.begin(monitor, "minimum-jumps", jumps.length);
        try {
            if (jumps.length <= 1) {
                return 0;
            }
            int jumpsUsed = 0;
            long currentEnd = 0L;
            long farthest = 0L;
            for (int index = 0; index < jumps.length - 1; index++) {
                AlgorithmSupport.tick(monitor);
                if (index > farthest) {
                    return -1;
                }
                farthest = Math.max(farthest, (long) index + jumps[index]);
                if (index == currentEnd) {
                    jumpsUsed++;
                    currentEnd = farthest;
                    if (currentEnd >= jumps.length - 1) {
                        return jumpsUsed;
                    }
                }
            }
            return -1;
        } finally {
            monitor.done();
        }
    }

    public static int gasStationStart(
            int[] gas,
            int[] cost,
            ProgressMonitor monitor) {
        if (gas.length != cost.length) {
            throw new IllegalArgumentException("gas/cost length mismatch");
        }
        AlgorithmSupport.begin(monitor, "gas-station", gas.length);
        try {
            long total = 0L;
            long tank = 0L;
            int start = 0;
            for (int index = 0; index < gas.length; index++) {
                AlgorithmSupport.tick(monitor);
                long delta = (long) gas[index] - cost[index];
                total += delta;
                tank += delta;
                if (tank < 0L) {
                    start = index + 1;
                    tank = 0L;
                }
            }
            return total < 0L || start == gas.length ? -1 : start;
        } finally {
            monitor.done();
        }
    }
}
