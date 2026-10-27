/*******************************************************************************
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
package com.m3.collections.apache;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.BenchmarkParams;

/**
 * M3PassiveExpiringMap against the Commons Collections 4.5.0 PassiveExpiringMap (the unmodified jar classes with their
 * clock redirected to {@link M3PassiveExpiringTestClock}, see {@link M3PassiveExpiringOracle}); both maps read the same
 * fake clock and, for the per-entry case, run the same policy class.
 *
 * <p>{@code slidingSize} and {@code slidingIsEmpty} keep {@code n} live entries with a time-to-live of {@code n}
 * milliseconds: each call moves the clock by one millisecond, so an expired entry is present at every call, puts one
 * new key and calls {@code size()} or {@code isEmpty()}, which must remove the expired entries first.
 * {@code sizeNoExpiry} calls {@code size()} on a map where nothing expires. {@code get} reads existing keys in random
 * order; {@code putRefresh} replaces existing keys while the clock moves by one millisecond per call, so every put
 * gives its entry a new expiration time; {@code putNewAndRemove} puts one new key and removes the oldest one, so the
 * map keeps {@code n} entries. Nothing expires in the last four.
 *
 * <p>Build and run commands are in the module README ({@code -f 1 -wi 5 -i 5 -w 1s -r 1s -jvmArgs -Xmx2g -prof gc}).
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class M3PassiveExpiringMapBench {
    private static final String POLICY = M3PassiveExpiringBenchPolicy.class.getName();
    private static final long FROZEN_TTL = 1_000_000_000_000L;
    private static final String VALUE = "value";

    /** Live entries. */
    @Param({"1000", "100000"})
    public int n;

    /** {@code constantTtl} (ring index in M3) or {@code perEntry} (heap index in M3). */
    @Param({"constantTtl", "perEntry"})
    public String policy;

    /** {@code m3} or {@code commons}. */
    @Param({"m3", "commons"})
    public String impl;

    private Map<Integer, String> map;
    private Integer[] pool;
    private Integer[] probes;
    private int mask;
    private int cursor;
    private int fresh;
    private int oldest;

    /**
     * Builds the map once per trial: every benchmark keeps it in a steady state (the sliding window and the
     * new-and-remove churn keep {@code n} entries), and the GC profiler's bytes per operation then exclude the build.
     */
    @Setup(Level.Trial)
    public void setup(BenchmarkParams params) {
        boolean sliding = params.getBenchmark().contains("sliding");
        long ttl = sliding ? n : FROZEN_TTL;
        pool = new Integer[2 * n];
        for (int i = 0; i < pool.length; i++) {
            pool[i] = i;
        }
        M3PassiveExpiringTestClock.set(0L, 0L);
        map = create(ttl);
        for (int i = 0; i < n; i++) {
            map.put(pool[i], VALUE);
            if (sliding) {
                M3PassiveExpiringTestClock.advance(1);
            }
        }
        if (sliding) {
            M3PassiveExpiringTestClock.advance(-1);
        }
        fresh = n;
        oldest = 0;
        probes = new Integer[Integer.highestOneBit(Math.max(1, n - 1)) << 2];
        mask = probes.length - 1;
        Random r = new Random(20261024L + n);
        for (int i = 0; i < probes.length; i++) {
            probes[i] = pool[r.nextInt(n)];
        }
        cursor = 0;
    }

    private Map<Integer, String> create(long ttl) {
        boolean perEntry = "perEntry".equals(policy);
        if ("m3".equals(impl)) {
            M3PassiveExpiringBenchPolicy p = perEntry ? new M3PassiveExpiringBenchPolicy(ttl) : null;
            return new M3PassiveExpiringMap<>(p, ttl, Map.of(), M3PassiveExpiringTestClock::currentTimeMillis);
        }
        M3PassiveExpiringOracle oracle = OracleHolder.ORACLE;
        return perEntry ? oracle.createWithPolicy(oracle.isolated(POLICY, new Class<?>[] {long.class}, ttl))
                : oracle.create(ttl);
    }

    private static final class OracleHolder {
        static final M3PassiveExpiringOracle ORACLE = new M3PassiveExpiringOracle(POLICY);
    }

    private Integer nextFresh() {
        int i = fresh;
        fresh = i + 1 == pool.length ? 0 : i + 1;
        return pool[i];
    }

    private Integer nextOldest() {
        int i = oldest;
        oldest = i + 1 == pool.length ? 0 : i + 1;
        return pool[i];
    }

    @Benchmark
    public int slidingSize() {
        M3PassiveExpiringTestClock.advance(1);
        map.put(nextFresh(), VALUE);
        return map.size();
    }

    @Benchmark
    public boolean slidingIsEmpty() {
        M3PassiveExpiringTestClock.advance(1);
        map.put(nextFresh(), VALUE);
        return map.isEmpty();
    }

    @Benchmark
    public int sizeNoExpiry() {
        return map.size();
    }

    @Benchmark
    public String get() {
        return map.get(probes[cursor++ & mask]);
    }

    @Benchmark
    public String putRefresh() {
        M3PassiveExpiringTestClock.advance(1);
        return map.put(probes[cursor++ & mask], VALUE);
    }

    @Benchmark
    public String putNewAndRemove() {
        map.put(nextFresh(), VALUE);
        return map.remove(nextOldest());
    }
}
