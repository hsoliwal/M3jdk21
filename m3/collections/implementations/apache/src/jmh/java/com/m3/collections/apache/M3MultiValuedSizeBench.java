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

import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * Size queries and counted writes of the owned Apache multivalued maps (leaf D1). The same source runs against the
 * master classes (size() sums every group, isEmpty() and keySet().size() scan the directory) and the counter
 * classes (two transient ints): keys in {10, 1e3, 1e5}, values per key in {1, 4}. writeMix measures the write side:
 * 90% put and 10% removeMapping on a set-valued map whose value domain is twice the values per key, each followed
 * by size(); the domain bounds the map, so a trial needs no reset. Keys and values are boxed once in setUp.
 */
//FLLDCIM: ALG - LC904 distinct-count: O(1) size/isEmpty/keySet().size() against the O(#keys) scans they replace
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class M3MultiValuedSizeBench {
    private static final int OPS = 4_096;
    private static final int OPS_MASK = OPS - 1;
    private static final int PUT_PERCENT = 90;
    private static final int PERCENT = 100;
    private static final long SEED = 0xD1_5EEDL;

    @Param({"10", "1000", "100000"})
    public int keys;

    @Param({"1", "4"})
    public int valuesPerKey;

    private M3ListValuedMap<Integer, Integer> list;
    private M3SetValuedMap<Integer, Integer> set;
    private Integer[] opKeys;
    private Integer[] opValues;
    private boolean[] opPuts;
    private int cursor;

    @Setup
    public void setUp() {
        list = new M3ListValuedMap<>();
        set = new M3SetValuedMap<>();
        for (int key = 0; key < keys; key++) {
            for (int value = 0; value < valuesPerKey; value++) {
                list.put(key, value);
                set.put(key, value);
            }
        }
        final SplittableRandom random = new SplittableRandom(SEED);
        opKeys = new Integer[OPS];
        opValues = new Integer[OPS];
        opPuts = new boolean[OPS];
        for (int i = 0; i < OPS; i++) {
            opKeys[i] = random.nextInt(keys);
            opValues[i] = random.nextInt(2 * valuesPerKey);
            opPuts[i] = random.nextInt(PERCENT) < PUT_PERCENT;
        }
    }

    @Benchmark
    public int size() {
        return list.size();
    }

    @Benchmark
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Benchmark
    public int keySetSize() {
        return list.keySet().size();
    }

    @Benchmark
    public int writeMix() {
        final int i = cursor++ & OPS_MASK;
        if (opPuts[i]) {
            set.put(opKeys[i], opValues[i]);
        } else {
            set.removeMapping(opKeys[i], opValues[i]);
        }
        return set.size();
    }
}
