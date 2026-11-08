// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.ProgressMonitor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.IntStream;

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

    //FLLDCIM: FOLD - member SchedulingIntervals.maxNonOverlapping from donor blob 16db2c345488 (donor blob 16db2c345488 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/scheduling-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/scheduling-fold/FOLD_MAP.tsv
    /**
     * Maximum number of non-overlapping intervals. Greedy: sort by end time,
     * greedily select.
     */
    public static int maxNonOverlapping(int[][] intervals) {
        if (intervals.length == 0) return 0;

        Arrays.sort(intervals, Comparator.comparingInt(a -> a[1]));

        int[] state = {1, intervals[0][1]}; // count, end
        IntStream.range(1, intervals.length).forEach(i -> {
            if (intervals[i][0] >= state[1]) {
                state[0]++;
                state[1] = intervals[i][1];
            }
        });
        return state[0];
    }

    //FLLDCIM: FOLD - member SchedulingIntervals.minRemoveForNonOverlapping from donor blob 16db2c345488 (donor blob 16db2c345488 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/scheduling-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/scheduling-fold/FOLD_MAP.tsv
    /** Minimum intervals to remove to make non-overlapping. */
    public static int eraseOverlapCount(int[][] intervals) {
        return intervals.length - maxNonOverlapping(intervals);
    }

    //FLLDCIM: FOLD - member SchedulingIntervals.mergeIntervals from donor blob 16db2c345488 (donor blob 16db2c345488 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/scheduling-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/scheduling-fold/FOLD_MAP.tsv
    /** Merge overlapping intervals. */
    public static int[][] merge(int[][] intervals) {
        if (intervals.length <= 1) return intervals;

        Arrays.sort(intervals, Comparator.comparingInt(a -> a[0]));

        List<int[]> result = new ArrayList<>();
        int[][] currentHolder = {intervals[0]};
        result.add(currentHolder[0]);

        Arrays.stream(intervals).forEach(interval -> {
            if (interval[0] <= currentHolder[0][1]) {
                currentHolder[0][1] = Math.max(currentHolder[0][1], interval[1]);
            } else {
                currentHolder[0] = interval;
                result.add(currentHolder[0]);
            }
        });
        return result.toArray(new int[0][]);
    }

    //FLLDCIM: FOLD - member SchedulingIntervals.insertInterval from donor blob 16db2c345488 (donor blob 16db2c345488 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/scheduling-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/scheduling-fold/FOLD_MAP.tsv
    /** Insert interval into sorted list and merge. */
    public static int[][] insert(int[][] intervals, int[] newInterval) {
        List<int[]> result = new ArrayList<>();
        int i = 0, n = intervals.length;

        // Add all before
        while (i < n && intervals[i][1] < newInterval[0]) {
            result.add(intervals[i++]);
        }

        // Merge overlapping
        while (i < n && intervals[i][0] <= newInterval[1]) {
            newInterval[0] = Math.min(newInterval[0], intervals[i][0]);
            newInterval[1] = Math.max(newInterval[1], intervals[i][1]);
            i++;
        }
        result.add(newInterval);

        // Add all after
        while (i < n) {
            result.add(intervals[i++]);
        }

        return result.toArray(new int[0][]);
    }

    //FLLDCIM: FOLD - member SchedulingIntervals.minMeetingRooms from donor blob 16db2c345488 (donor blob 16db2c345488 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/scheduling-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/scheduling-fold/FOLD_MAP.tsv
    /** Minimum meeting rooms needed. */
    public static int minMeetingRooms(int[][] intervals) {
        if (intervals.length == 0) return 0;

        int[] starts = Arrays.stream(intervals).mapToInt(i -> i[0]).sorted().toArray();
        int[] ends = Arrays.stream(intervals).mapToInt(i -> i[1]).sorted().toArray();

        int rooms = 0, maxRooms = 0;
        int s = 0, e = 0;

        while (s < intervals.length) {
            if (starts[s] < ends[e]) {
                rooms++;
                s++;
            } else {
                rooms--;
                e++;
            }
            maxRooms = Math.max(maxRooms, rooms);
        }
        return maxRooms;
    }

    //FLLDCIM: FOLD - member SchedulingIntervals.canAttendAll from donor blob 16db2c345488 (donor blob 16db2c345488 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/scheduling-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/scheduling-fold/FOLD_MAP.tsv
    /** Check if person can attend all meetings. */
    public static boolean canAttendAll(int[][] intervals) {
        Arrays.sort(intervals, Comparator.comparingInt(a -> a[0]));

        return IntStream.range(1, intervals.length).allMatch(i -> intervals[i][0] >= intervals[i - 1][1]);
    }
    //FLLDCIM: FOLD - member SchedulingJobs.sjf from donor blob b91168726b62 (donor blob b91168726b62 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/scheduling-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/scheduling-fold/FOLD_MAP.tsv
    /** Shortest Job First (SJF) scheduling. Returns order of job indices. */
    public static int[] shortestJobFirst(int[] jobDurations) {
        return IntStream.range(0, jobDurations.length)
                .boxed()
                .sorted(Comparator.comparingInt(i -> jobDurations[i]))
                .mapToInt(Integer::intValue)
                .toArray();
    }

    //FLLDCIM: FOLD - member SchedulingJobs.avgWaitingTime from donor blob b91168726b62 (donor blob b91168726b62 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/scheduling-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/scheduling-fold/FOLD_MAP.tsv
    /** Average waiting time for SJF. */
    public static double averageWaitingTime(int[] jobDurations) {
        int[] order = shortestJobFirst(jobDurations);
        int n = order.length;

        long[] state = {0, 0}; // totalWait, currentTime
        Arrays.stream(order).forEach(idx -> {
            state[0] += state[1];
            state[1] += jobDurations[idx];
        });

        return (double) state[0] / n;
    }

    //FLLDCIM: FOLD - member SchedulingJobs.jobSequencing from donor blob b91168726b62 (donor blob b91168726b62 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/scheduling-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/scheduling-fold/FOLD_MAP.tsv
    /**
     * Job sequencing with deadlines for maximum profit. Each job takes 1 unit, must
     * finish by deadline.
     */
    public static int jobSequencing(int[] deadlines, int[] profits) {
        int n = deadlines.length;
        Integer[] indices = IntStream.range(0, n).boxed().toArray(Integer[]::new);

        // Sort by profit descending
        Arrays.sort(indices, (a, b) -> Integer.compare(profits[b], profits[a]));

        int maxDeadline = Arrays.stream(deadlines).max().orElse(0);
        boolean[] slots = new boolean[maxDeadline + 1];
        int totalProfit = 0;

        for (int idx : indices) {
            // Find latest available slot before deadline
            for (int slot = deadlines[idx]; slot > 0; slot--) {
                if (!slots[slot]) {
                    slots[slot] = true;
                    totalProfit += profits[idx];
                    break;
                }
            }
        }
        return totalProfit;
    }

    //FLLDCIM: FOLD - member SchedulingJobs.weightedJobScheduling from donor blob b91168726b62 (donor blob b91168726b62 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/scheduling-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/scheduling-fold/FOLD_MAP.tsv
    /** Weighted job scheduling for maximum profit. Jobs have start, end, profit. */
    public static int weightedJobScheduling(int[][] jobs) {
        // jobs[i] = {start, end, profit}
        int n = jobs.length;

        // Sort by end time
        Integer[] indices = IntStream.range(0, n).boxed().toArray(Integer[]::new);
        Arrays.sort(indices, Comparator.comparingInt(i -> jobs[i][1]));

        int[] dp = new int[n];
        dp[0] = jobs[indices[0]][2];

        for (int i = 1; i < n; i++) {
            int include = jobs[indices[i]][2];

            // Find last non-conflicting job
            int left = 0, right = i - 1, lastNonConflict = -1;
            while (left <= right) {
                int mid = left + (right - left) / 2;
                if (jobs[indices[mid]][1] <= jobs[indices[i]][0]) {
                    lastNonConflict = mid;
                    left = mid + 1;
                } else {
                    right = mid - 1;
                }
            }

            if (lastNonConflict != -1) {
                include += dp[lastNonConflict];
            }

            dp[i] = Math.max(dp[i - 1], include);
        }

        return dp[n - 1];
    }
    //FLLDCIM: FOLD - member SchedulingCPU.roundRobin from donor blob e3a8c396c0c0 (donor blob e3a8c396c0c0 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/scheduling-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/scheduling-fold/FOLD_MAP.tsv
    /** Round Robin scheduling. Returns completion times for each process. */
    public static int[] roundRobin(int[] burstTimes, int quantum) {
        int n = burstTimes.length;
        int[] remaining = burstTimes.clone();
        int[] completion = new int[n];
        Queue<Integer> queue = new LinkedList<>();

        IntStream.range(0, n).forEach(queue::add);

        int time = 0;
        while (!queue.isEmpty()) {
            int proc = queue.poll();

            if (remaining[proc] <= quantum) {
                time += remaining[proc];
                remaining[proc] = 0;
                completion[proc] = time;
            } else {
                time += quantum;
                remaining[proc] -= quantum;
                queue.add(proc);
            }
        }

        return completion;
    }

    //FLLDCIM: FOLD - member SchedulingCPU.priorityScheduling from donor blob e3a8c396c0c0 (donor blob e3a8c396c0c0 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/scheduling-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/scheduling-fold/FOLD_MAP.tsv
    /**
     * Priority scheduling (non-preemptive). Lower priority number = higher
     * priority.
     */
    public static int[] priorityScheduling(int[] burstTimes, int[] priorities) {
        int n = burstTimes.length;
        int[] completion = new int[n];
        int[] timeRef = {0};

        IntStream.range(0, n)
                .boxed()
                .sorted(Comparator.comparingInt(i -> priorities[i]))
                .forEachOrdered(idx -> {
                    timeRef[0] += burstTimes[idx];
                    completion[idx] = timeRef[0];
                });

        return completion;
    }

    //FLLDCIM: FOLD - member SchedulingCPU.srtf from donor blob e3a8c396c0c0 (donor blob e3a8c396c0c0 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/scheduling-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/scheduling-fold/FOLD_MAP.tsv
    /** Shortest Remaining Time First (preemptive SJF). Returns completion times. */
    public static int[] shortestRemainingTimeFirst(int[] arrivalTimes, int[] burstTimes) {
        int n = arrivalTimes.length;
        int[] remaining = burstTimes.clone();
        int[] completion = new int[n];
        boolean[] done = new boolean[n];

        int time = 0, completed = 0;

        while (completed < n) {
            int shortest = -1;
            int minRemaining = Integer.MAX_VALUE;

            for (int i = 0; i < n; i++) {
                if (!done[i] && arrivalTimes[i] <= time && remaining[i] < minRemaining) {
                    minRemaining = remaining[i];
                    shortest = i;
                }
            }

            if (shortest == -1) {
                time++;
                continue;
            }

            remaining[shortest]--;
            time++;

            if (remaining[shortest] == 0) {
                done[shortest] = true;
                completion[shortest] = time;
                completed++;
            }
        }

        return completion;
    }

    //FLLDCIM: FOLD - member SchedulingCPU.turnaroundTime from donor blob e3a8c396c0c0 (donor blob e3a8c396c0c0 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/scheduling-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/scheduling-fold/FOLD_MAP.tsv
    /** Calculate turnaround time from completion and arrival times. */
    public static int[] turnaroundTime(int[] arrivalTimes, int[] completionTimes) {
        return IntStream.range(0, arrivalTimes.length)
                .map(i -> completionTimes[i] - arrivalTimes[i])
                .toArray();
    }

    //FLLDCIM: FOLD - member SchedulingCPU.waitingTime from donor blob e3a8c396c0c0 (donor blob e3a8c396c0c0 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/scheduling-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/scheduling-fold/FOLD_MAP.tsv
    /** Calculate waiting time from turnaround and burst times. */
    public static int[] waitingTime(int[] turnaroundTimes, int[] burstTimes) {
        return IntStream.range(0, turnaroundTimes.length)
                .map(i -> turnaroundTimes[i] - burstTimes[i])
                .toArray();
    }
    //FLLDCIM: FOLD - member SchedulingTasks.taskSchedulerWithCooldown from donor blob 49428ba5b8e4 (donor blob 49428ba5b8e4 at commit 45da5f035 (repository, path and digests: m3/algorithms/recipe/scheduling-fold/PROVENANCE.tsv)); recipe m3/tooling/jdk-naming/fold_members.py v1, map m3/algorithms/recipe/scheduling-fold/FOLD_MAP.tsv
    /** Task scheduler with cooldown. Same task must have at least n intervals apart. */
    public static int taskSchedulerWithCooldown(char[] tasks, int cooldown) {
        int[] freq = new int[26];
        new String(tasks).chars().forEach(t -> freq[t - 'A']++);

        int maxFreq = Arrays.stream(freq).max().orElse(0);
        int maxCount = (int) Arrays.stream(freq).filter(f -> f == maxFreq).count();

        // Formula: (maxFreq - 1) * (cooldown + 1) + maxCount
        int minLen = (maxFreq - 1) * (cooldown + 1) + maxCount;

        return tasks.length == 0 ? 0 : Math.max(minLen, tasks.length);
    }

}
