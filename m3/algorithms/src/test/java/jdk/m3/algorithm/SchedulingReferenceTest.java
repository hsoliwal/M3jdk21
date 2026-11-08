// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

//FLLDCIM: TOP - M3JDK-authored reference oracle of recipe/scheduling-fold (the donors carry no tests): every folded
// scheduling kernel against an exhaustive or simulated formulation on small random inputs. The donor interval kernels
// sort their input in place, so every call receives a copy.
final class SchedulingReferenceTest {
    private static final int TRIALS = 300;

    private static int[][] copy(int[][] intervals) {
        return Arrays.stream(intervals).map(int[]::clone).toArray(int[][]::new);
    }

    private static int[][] randomIntervals(Random random, int maxCount, int bound) {
        int[][] intervals = new int[random.nextInt(maxCount + 1)][];
        for (int i = 0; i < intervals.length; i++) {
            int start = random.nextInt(bound);
            intervals[i] = new int[] {start, start + 1 + random.nextInt(bound)};
        }
        return intervals;
    }

    private static boolean disjoint(int[] a, int[] b) {
        return a[1] <= b[0] || b[1] <= a[0];
    }

    private static int[][] mergeByPoints(int[][] intervals) {
        int[][] sorted = copy(intervals);
        Arrays.sort(sorted, (a, b) -> Integer.compare(a[0], b[0]));
        List<int[]> merged = new ArrayList<>();
        for (int[] interval : sorted) {
            if (!merged.isEmpty() && interval[0] <= merged.get(merged.size() - 1)[1]) {
                merged.get(merged.size() - 1)[1] = Math.max(merged.get(merged.size() - 1)[1], interval[1]);
            } else {
                merged.add(interval.clone());
            }
        }
        return merged.toArray(new int[0][]);
    }

    @Test
    void intervalKernelsAgreeWithExhaustiveFormulations() {
        Random random = new Random(0x1A7E);
        for (int trial = 0; trial < TRIALS; trial++) {
            int[][] intervals = randomIntervals(random, 7, 6);
            int n = intervals.length;
            int best = 0;
            boolean allDisjoint = true;
            int rooms = 0;
            for (int mask = 0; mask < (1 << n); mask++) {
                boolean feasible = true;
                for (int i = 0; i < n && feasible; i++) {
                    for (int j = i + 1; j < n && feasible; j++) {
                        if ((mask >> i & 1) == 1 && (mask >> j & 1) == 1 && !disjoint(intervals[i], intervals[j])) {
                            feasible = false;
                        }
                    }
                }
                if (feasible) {
                    best = Math.max(best, Integer.bitCount(mask));
                }
            }
            for (int i = 0; i < n; i++) {
                int open = 0;
                for (int j = 0; j < n; j++) {
                    if (j != i && !disjoint(intervals[i], intervals[j])) {
                        allDisjoint = false;
                    }
                    if (intervals[j][0] <= intervals[i][0] && intervals[i][0] < intervals[j][1]) {
                        open++;
                    }
                }
                rooms = Math.max(rooms, open);
            }
            assertEquals(best, IntervalGreedyAlgorithms.maxNonOverlapping(copy(intervals)), "maxNonOverlapping");
            assertEquals(n - best, IntervalGreedyAlgorithms.eraseOverlapCount(copy(intervals)), "eraseOverlapCount");
            assertEquals(rooms, IntervalGreedyAlgorithms.minMeetingRooms(copy(intervals)), "minMeetingRooms");
            assertEquals(allDisjoint, IntervalGreedyAlgorithms.canAttendAll(copy(intervals)), "canAttendAll");
            int[][] merged = mergeByPoints(intervals);
            assertArrayEquals(merged, IntervalGreedyAlgorithms.merge(copy(intervals)), "merge");
            int start = random.nextInt(8);
            int[] added = {start, start + random.nextInt(5)};
            int[][] withAdded = Arrays.copyOf(merged, merged.length + 1);
            withAdded[merged.length] = added.clone();
            assertArrayEquals(mergeByPoints(withAdded), IntervalGreedyAlgorithms.insert(copy(merged), added.clone()), "insert");
        }
    }

    @Test
    void jobKernelsAgreeWithExhaustiveFormulations() {
        Random random = new Random(0x30B5);
        for (int trial = 0; trial < TRIALS; trial++) {
            int n = 1 + random.nextInt(6);
            int[] durations = new int[n];
            int[] deadlines = new int[n];
            int[] profits = new int[n];
            int[][] jobs = new int[n][];
            for (int i = 0; i < n; i++) {
                durations[i] = 1 + random.nextInt(9);
                deadlines[i] = 1 + random.nextInt(4);
                profits[i] = 1 + random.nextInt(20);
                int start = random.nextInt(8);
                jobs[i] = new int[] {start, start + 1 + random.nextInt(5), 1 + random.nextInt(20)};
            }
            Integer[] order = new Integer[n];
            for (int i = 0; i < n; i++) {
                order[i] = i;
            }
            Arrays.sort(order, (a, b) -> Integer.compare(durations[a], durations[b]));
            int[] expectedOrder = Arrays.stream(order).mapToInt(Integer::intValue).toArray();
            long waiting = 0;
            long clock = 0;
            for (int index : expectedOrder) {
                waiting += clock;
                clock += durations[index];
            }
            assertArrayEquals(expectedOrder, IntervalGreedyAlgorithms.shortestJobFirst(durations), "shortestJobFirst");
            assertEquals((double) waiting / n, IntervalGreedyAlgorithms.averageWaitingTime(durations), 1e-9, "averageWaitingTime");

            int bestSequence = 0;
            int bestWeighted = 0;
            for (int mask = 0; mask < (1 << n); mask++) {
                List<Integer> chosen = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    if ((mask >> i & 1) == 1) {
                        chosen.add(i);
                    }
                }
                List<Integer> byDeadline = new ArrayList<>(chosen);
                byDeadline.sort((a, b) -> Integer.compare(deadlines[a], deadlines[b]));
                boolean feasible = true;
                int profit = 0;
                for (int k = 0; k < byDeadline.size(); k++) {
                    if (deadlines[byDeadline.get(k)] < k + 1) {
                        feasible = false;
                    }
                    profit += profits[byDeadline.get(k)];
                }
                if (feasible) {
                    bestSequence = Math.max(bestSequence, profit);
                }
                boolean compatible = true;
                int weighted = 0;
                for (int a : chosen) {
                    weighted += jobs[a][2];
                    for (int b : chosen) {
                        if (a < b && !(jobs[a][1] <= jobs[b][0] || jobs[b][1] <= jobs[a][0])) {
                            compatible = false;
                        }
                    }
                }
                if (compatible) {
                    bestWeighted = Math.max(bestWeighted, weighted);
                }
            }
            assertEquals(bestSequence, IntervalGreedyAlgorithms.jobSequencing(deadlines.clone(), profits.clone()), "jobSequencing");
            assertEquals(bestWeighted, IntervalGreedyAlgorithms.weightedJobScheduling(copy(jobs)), "weightedJobScheduling");
        }
    }

    @Test
    void cpuKernelsAgreeWithSimulation() {
        Random random = new Random(0xC9D);
        for (int trial = 0; trial < TRIALS; trial++) {
            int n = 1 + random.nextInt(6);
            int[] bursts = new int[n];
            int[] priorities = new int[n];
            int[] arrivals = new int[n];
            for (int i = 0; i < n; i++) {
                bursts[i] = 1 + random.nextInt(7);
                priorities[i] = random.nextInt(4);
                arrivals[i] = random.nextInt(6);
            }
            int quantum = 1 + random.nextInt(4);
            int[] remaining = bursts.clone();
            int[] completion = new int[n];
            int[] ring = new int[n * 64];
            int head = 0;
            int tail = 0;
            for (int i = 0; i < n; i++) {
                ring[tail++] = i;
            }
            int time = 0;
            while (head < tail) {
                int process = ring[head++];
                int slice = Math.min(quantum, remaining[process]);
                time += slice;
                remaining[process] -= slice;
                if (remaining[process] == 0) {
                    completion[process] = time;
                } else {
                    ring[tail++] = process;
                }
            }
            assertArrayEquals(completion, IntervalGreedyAlgorithms.roundRobin(bursts.clone(), quantum), "roundRobin");

            Integer[] order = new Integer[n];
            for (int i = 0; i < n; i++) {
                order[i] = i;
            }
            Arrays.sort(order, (a, b) -> Integer.compare(priorities[a], priorities[b]));
            int[] byPriority = new int[n];
            int clock = 0;
            for (int index : order) {
                clock += bursts[index];
                byPriority[index] = clock;
            }
            assertArrayEquals(byPriority, IntervalGreedyAlgorithms.priorityScheduling(bursts.clone(), priorities.clone()), "priorityScheduling");

            int[] left = bursts.clone();
            int[] preemptive = new int[n];
            int done = 0;
            int tick = 0;
            while (done < n) {
                int pick = -1;
                for (int i = 0; i < n; i++) {
                    if (left[i] > 0 && arrivals[i] <= tick && (pick < 0 || left[i] < left[pick])) {
                        pick = i;
                    }
                }
                tick++;
                if (pick >= 0 && --left[pick] == 0) {
                    preemptive[pick] = tick;
                    done++;
                }
            }
            assertArrayEquals(preemptive, IntervalGreedyAlgorithms.shortestRemainingTimeFirst(arrivals.clone(), bursts.clone()), "shortestRemainingTimeFirst");
            int[] turnaround = IntervalGreedyAlgorithms.turnaroundTime(arrivals, preemptive);
            int[] waiting = IntervalGreedyAlgorithms.waitingTime(turnaround, bursts);
            for (int i = 0; i < n; i++) {
                assertEquals(preemptive[i] - arrivals[i], turnaround[i], "turnaroundTime");
                assertEquals(preemptive[i] - arrivals[i] - bursts[i], waiting[i], "waitingTime");
            }
        }
    }

    @Test
    void cooldownDependencyAndTopologicalKernelsAgreeWithSearch() {
        Random random = new Random(0x70B0);
        for (int trial = 0; trial < TRIALS; trial++) {
            int count = random.nextInt(6);
            char[] tasks = new char[count];
            for (int i = 0; i < count; i++) {
                tasks[i] = (char) ('A' + random.nextInt(3));
            }
            int cooldown = random.nextInt(3);
            int[] frequency = new int[3];
            for (char task : tasks) {
                frequency[task - 'A']++;
            }
            assertEquals(shortestSchedule(frequency, cooldown), IntervalGreedyAlgorithms.taskSchedulerWithCooldown(tasks.clone(), cooldown),
                    "taskSchedulerWithCooldown " + new String(tasks) + " " + cooldown);

            int n = 1 + random.nextInt(6);
            int[] times = new int[n];
            List<List<Integer>> successors = new ArrayList<>();
            List<int[]> edges = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                times[i] = 1 + random.nextInt(5);
                successors.add(new ArrayList<>());
            }
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (random.nextInt(3) == 0) {
                        successors.get(i).add(j);
                        edges.add(new int[] {i, j});
                    }
                }
            }
            int[] longest = new int[n];
            for (int v = 0; v < n; v++) {
                longest[v] = times[v];
                for (int u = 0; u < v; u++) {
                    if (successors.get(u).contains(v)) {
                        longest[v] = Math.max(longest[v], longest[u] + times[v]);
                    }
                }
            }
            assertEquals(Arrays.stream(longest).max().orElse(0), GraphAlgorithms.minimumTimeWithDependencies(times, successors),
                    "minimumTimeWithDependencies");

            int[][] dag = edges.toArray(new int[0][]);
            int[] order = GraphAlgorithms.topologicalOrder(n, dag);
            assertNotNull(order, "topologicalOrder of a DAG");
            int[] position = new int[n];
            Arrays.fill(position, -1);
            for (int i = 0; i < n; i++) {
                assertTrue(order[i] >= 0 && order[i] < n && position[order[i]] < 0, "permutation");
                position[order[i]] = i;
            }
            for (int[] edge : dag) {
                assertTrue(position[edge[0]] < position[edge[1]], "edge order");
            }
            if (n >= 2) {
                int[][] cyclic = Arrays.copyOf(dag, dag.length + 2);
                cyclic[dag.length] = new int[] {n - 1, 0};
                cyclic[dag.length + 1] = new int[] {0, n - 1};
                assertNull(GraphAlgorithms.topologicalOrder(n, cyclic), "cycle refuses");
            }
        }
    }

    private static int shortestSchedule(int[] frequency, int cooldown) {
        int total = Arrays.stream(frequency).sum();
        if (total == 0) {
            return 0;
        }
        int[] last = new int[frequency.length];
        Arrays.fill(last, Integer.MIN_VALUE / 2);
        return search(frequency.clone(), last, cooldown, 0, total);
    }

    private static int search(int[] remaining, int[] last, int cooldown, int time, int left) {
        if (left == 0) {
            return time;
        }
        int best = Integer.MAX_VALUE;
        boolean placed = false;
        for (int task = 0; task < remaining.length; task++) {
            if (remaining[task] > 0 && time - last[task] > cooldown) {
                int previous = last[task];
                remaining[task]--;
                last[task] = time;
                best = Math.min(best, search(remaining, last, cooldown, time + 1, left - 1));
                last[task] = previous;
                remaining[task]++;
                placed = true;
            }
        }
        if (!placed) {
            best = Math.min(best, search(remaining, last, cooldown, time + 1, left));
        }
        return best;
    }
}
