/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * IntPostingList.indexOf/contains binary-search ascending views: against the original first-match
 * scan (the oracle below), equal results for ascending lanes with duplicates, unordered lanes,
 * slices, absent and extreme values, repeated calls and racing threads.
 */
//FLLDCIM: LEETCODIFY — differential proof for the ascending-view lower-bound search
class IntPostingListSearchTest {
    private static int scan(int[] lane, int value) {
        for (int i = 0; i < lane.length; i++) {
            if (lane[i] == value) {
                return i;
            }
        }
        return -1;
    }

    private static int[] lane(Random random, int shape, int size) {
        int[] lane = new int[size];
        int span = 1 + random.nextInt(shape == 3 ? 3 : 1000);
        for (int i = 0; i < size; i++) {
            lane[i] = shape == 4 ? random.nextInt() : random.nextInt(span) - span / 2;
        }
        if (shape <= 1 || shape == 3) {
            Arrays.sort(lane);
        }
        if (shape == 1 && size > 1) {
            lane[random.nextInt(size)] = Integer.MIN_VALUE;
        }
        return lane;
    }

    private static void compare(IntPostingList list, int[] lane, Random random) {
        int[] probes = {Integer.MIN_VALUE, Integer.MAX_VALUE, 0, -1, 1};
        for (int round = 0; round < 3; round++) {
            for (int probe : probes) {
                assertEquals(scan(lane, probe), list.indexOf(probe), "probe " + probe);
            }
            for (int k = 0; k < 2 * lane.length + 4; k++) {
                int probe = lane.length > 0 && random.nextBoolean()
                        ? lane[random.nextInt(lane.length)] : random.nextInt(2001) - 1000;
                assertEquals(scan(lane, probe), list.indexOf(probe), "probe " + probe);
                assertEquals(scan(lane, probe) >= 0, list.contains(probe));
            }
        }
    }

    @Test
    void indexOfMatchesFirstMatchScan() {
        Random random = new Random(0x5EA2C4L);
        for (int trial = 0; trial < 4000; trial++) {
            int shape = trial % 5;
            int[] lane = lane(random, shape, random.nextInt(trial % 7 == 0 ? 2000 : 40));
            compare(new IntPostingList(lane), lane, random);
            if (lane.length > 2) {
                int from = random.nextInt(lane.length);
                int to = from + random.nextInt(lane.length - from + 1);
                compare(new IntPostingList(lane).slice(from, to), Arrays.copyOfRange(lane, from, to)
                        , random);
            }
        }
    }

    @Test
    void racingThreadsSeeTheSameResults() throws Exception {
        int[] lane = new int[100_000];
        for (int i = 0; i < lane.length; i++) {
            lane[i] = i / 3;
        }
        IntPostingList list = new IntPostingList(lane);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            Future<?>[] runs = new Future<?>[4];
            for (int t = 0; t < runs.length; t++) {
                int seed = t;
                runs[t] = pool.submit(() -> {
                    Random random = new Random(seed);
                    for (int k = 0; k < 20_000; k++) {
                        int probe = random.nextInt(40_000) - 3_000;
                        assertEquals(probe < 0 || probe >= 33_334 ? -1 : probe * 3, list.indexOf(
                                probe));
                    }
                });
            }
            for (Future<?> run : runs) {
                run.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
