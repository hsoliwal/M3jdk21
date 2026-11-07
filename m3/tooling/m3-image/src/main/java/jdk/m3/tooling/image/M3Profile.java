/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Normalized class-load and sampled-method observations. */
public final class M3Profile {
    /** One normalized profile row. */
    public record Observation(String kind, String symbol, long count, long firstOrdinal) {
        /** Validates the observation. */
        public Observation {
            if (kind == null || kind.isBlank() || symbol == null || symbol.isBlank()) {
                throw new IllegalArgumentException("Profile kind and symbol must not be blank");
            }
            if (count < 1 || firstOrdinal < 0) {
                throw new IllegalArgumentException("Invalid profile counters");
            }
        }
    }

    private final Map<String, Long> loadedClassRanks;
    private final Map<String, Long> classSamples;
    private final Map<String, Long> methodSamples;

    /** Creates a normalized profile. */
    public M3Profile(Map<String, Long> loadedClassRanks,
                     Map<String, Long> classSamples,
                     Map<String, Long> methodSamples) {
        this.loadedClassRanks = immutableSorted(loadedClassRanks, true);
        this.classSamples = immutableSorted(classSamples, false);
        this.methodSamples = immutableSorted(methodSamples, false);
    }

    /** Returns an empty profile. */
    public static M3Profile empty() {
        return new M3Profile(Map.of(), Map.of(), Map.of());
    }

    /** Returns class load order, where lower is earlier. */
    public Map<String, Long> loadedClassRanks() {
        return loadedClassRanks;
    }

    /** Returns sampled class counts. */
    public Map<String, Long> classSamples() {
        return classSamples;
    }

    /** Returns sampled method counts. */
    public Map<String, Long> methodSamples() {
        return methodSamples;
    }

    /** Returns the aggregate sample count for a class. */
    public long hotCount(String binaryName) {
        return classSamples.getOrDefault(binaryName, 0L);
    }

    /** Returns the startup rank, or {@code -1}. */
    public long startupRank(String binaryName) {
        return loadedClassRanks.getOrDefault(binaryName, -1L);
    }

    /** Returns deterministic rows suitable for profile.tsv. */
    public List<Observation> observations() {
        List<Observation> rows = new ArrayList<>();
        loadedClassRanks.forEach((symbol, rank) -> rows.add(new Observation("CLASS_LOAD", symbol, 1, rank)));
        classSamples.forEach((symbol, count) -> rows.add(new Observation("CLASS_SAMPLE", symbol, count, 0)));
        methodSamples.forEach((symbol, count) -> rows.add(new Observation("METHOD_SAMPLE", symbol, count, 0)));
        rows.sort(java.util.Comparator.comparing(Observation::kind).thenComparing(Observation::symbol));
        return Collections.unmodifiableList(rows);
    }

    /** Merges profiles monotonically. Earliest load rank and summed samples win. */
    public M3Profile merge(M3Profile other) {
        Map<String, Long> ranks = new LinkedHashMap<>(loadedClassRanks);
        other.loadedClassRanks.forEach((key, value) -> ranks.merge(key, value, Math::min));
        Map<String, Long> classes = new LinkedHashMap<>(classSamples);
        other.classSamples.forEach((key, value) -> classes.merge(key, value, Math::addExact));
        Map<String, Long> methods = new LinkedHashMap<>(methodSamples);
        other.methodSamples.forEach((key, value) -> methods.merge(key, value, Math::addExact));
        return new M3Profile(ranks, classes, methods);
    }

    private static Map<String, Long> immutableSorted(Map<String, Long> input, boolean allowZero) {
        TreeMap<String, Long> sorted = new TreeMap<>();
        input.forEach((key, value) -> {
            if (key == null || key.isBlank() || value == null || value < 0 || (!allowZero && value == 0)) {
                throw new IllegalArgumentException("Invalid profile map entry");
            }
            sorted.put(key, value);
        });
        return Collections.unmodifiableMap(sorted);
    }
}
