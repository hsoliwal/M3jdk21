// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Exact counters for one inventory run: per-unit metrics, per-corpus totals and the global
 * content-keyed tables used for cross-class duplicate counts. Nothing is sampled or estimated.
 */
final class M3InventoryState {
    /** Corpus ordinal: 0 = JDK modules, 1 = vendored universe jars. */
    static final String[] CORPORA = {"jdk21", "vendor"};

    /** Per-string state: classes per corpus, last class serial, length. */
    static final class Seen {
        final int[] classes = new int[2];
        int lastSerial = -1;
    }

    /** One distinct constant regex key. */
    static final class RegexKey {
        final String pattern;
        final int flags;
        final int[] sites = new int[2];
        final TreeSet<String> apis = new TreeSet<>();
        final TreeSet<String> units = new TreeSet<>();
        M3RegexRequirement.Analysis analysis;
        boolean shapeLiteral;

        RegexKey(String pattern, int flags) {
            this.pattern = pattern;
            this.flags = flags;
        }
    }

    final Map<String, Seen> ldc = new HashMap<>(1 << 20);
    final Map<String, Seen> concatRecipes = new HashMap<>(1 << 16);
    final Map<String, Seen> formats = new HashMap<>(1 << 14);
    final Map<String, RegexKey> regex = new TreeMap<>();
    final Map<String, M3FormatPlan.Kind> formatKinds = new HashMap<>();
    final List<Map<String, Long>> totals = List.of(new TreeMap<>(), new TreeMap<>());
    Map<String, Long> unit = new TreeMap<>();
    int corpus;
    String unitName = "";
    int classSerial;

    void beginUnit(int corpusOrdinal, String name) {
        corpus = corpusOrdinal;
        unitName = name;
        unit = new TreeMap<>();
    }

    void add(String category, String metric, long value) {
        String key = category + '\t' + metric;
        unit.merge(key, value, Long::sum);
        totals.get(corpus).merge(key, value, Long::sum);
    }

    void inc(String category, String metric) {
        add(category, metric, 1);
    }

    /** Records one occurrence of {@code text} in the current class; true when first in class. */
    boolean seen(Map<String, Seen> table, String text) {
        Seen s = table.computeIfAbsent(text, key -> new Seen());
        if (s.lastSerial == classSerial) {
            return false;
        }
        s.lastSerial = classSerial;
        s.classes[corpus]++;
        return true;
    }

    /** Length bucket label: 0, 1, 2-3, 4-7, ... 1024+. */
    static String bucket(int length) {
        if (length == 0) {
            return "len_0000";
        }
        int bit = 31 - Integer.numberOfLeadingZeros(length);
        if (bit >= 10) {
            return "len_1024+";
        }
        int low = 1 << bit;
        return String.format("len_%04d-%04d", low, (low << 1) - 1);
    }
}
