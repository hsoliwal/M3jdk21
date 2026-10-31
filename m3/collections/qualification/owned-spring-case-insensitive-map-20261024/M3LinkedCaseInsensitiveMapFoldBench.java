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
package com.m3.collections.spring.bench;

import com.m3.collections.spring.M3LinkedCaseInsensitiveMap;
import java.util.Locale;
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
 * Lookups of the owned case-insensitive map with HTTP-header-like keys (the Spring HttpHeaders use), run against the
 * 20261018 body and the D2 fold fast-path body: get and containsKey of a present key in another case, containsKey of
 * an absent key, and a same-key put in another case. The tr locale takes the converting path in both bodies.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class M3LinkedCaseInsensitiveMapFoldBench {
    private static final String[] NAMES = {"Accept", "Accept-Encoding", "Authorization", "Cache-Control", "Connection",
        "Content-Length", "Content-Type", "Cookie", "Host", "If-None-Match", "Origin", "Referer", "User-Agent",
        "X-Forwarded-For", "X-Request-Id", "Vary"};
    private static final int MASK = 15;

    @Param({"en", "tr"})
    public String language;

    private M3LinkedCaseInsensitiveMap<String> map;
    private String[] upper;
    private String[] absent;
    private int cursor;

    @Setup
    public void setUp() {
        map = new M3LinkedCaseInsensitiveMap<>(NAMES.length, Locale.forLanguageTag(language));
        upper = new String[NAMES.length];
        absent = new String[NAMES.length];
        for (int i = 0; i < NAMES.length; i++) {
            map.put(NAMES[i], "v" + i);
            upper[i] = NAMES[i].toUpperCase(Locale.ROOT);
            absent[i] = "X-Missing-" + NAMES[i];
        }
    }

    @Benchmark
    public String getOtherCase() {
        return map.get(upper[cursor++ & MASK]);
    }

    @Benchmark
    public boolean containsKeyOtherCase() {
        return map.containsKey(upper[cursor++ & MASK]);
    }

    @Benchmark
    public boolean containsKeyAbsent() {
        return map.containsKey(absent[cursor++ & MASK]);
    }

    @Benchmark
    public String putSameKeyOtherCase() {
        int i = cursor++ & MASK;
        return map.put(upper[i], "w");
    }
}
