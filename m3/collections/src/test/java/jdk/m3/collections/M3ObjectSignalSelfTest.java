// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import jdk.m3.collections.M3PoolProgressMonitor;
import java.util.ArrayList;
import java.util.List;

/** Dependency-free differential verification of bitwise first-pass projections. */
//FLLDCIM: RELOCATE - donor blob 41d1312ab113 at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/qualification/object-signal-20261107/PROVENANCE.tsv); recipe m3/collections/qualification/object-signal-20261107/relocate.py v1
public final class M3ObjectSignalSelfTest {
    private static int checks;

    private M3ObjectSignalSelfTest() { }

    public static void main(String[] args) {
        checks = 0;
        stringEqualityParity();
        stringContainsParity();
        customBitSignals();
        bulkBackendParity();
        System.out.println("PASS: " + checks + " object-signal checks");
    }

    private static void stringEqualityParity() {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < 4_096; i++) {
            values.add(i % 257 == 0 ? "target-dogmatic-value" : "value-" + i);
        }
        M3ObjectProjector<String> projector = M3ObjectProjectors.byString(value -> value);
        M3ObjectSignalTable table = M3ObjectSignalTable.build(values, projector, M3PoolProgressMonitor.none());
        M3ObjectSignal target = projector.project("target-dogmatic-value", M3PoolProgressMonitor.none());
        M3ObjectSignalFirstPassResult pass = table.firstPass(M3ObjectSignalQuery.exact(target), M3PoolProgressMonitor.none());
        M3ObjectSignalExactResult exact = M3ObjectSignalMatcher.verify(
            values,
            pass,
            "target-dogmatic-value"::equals,
            M3PoolProgressMonitor.none());

        for (int i = 0; i < values.size(); i++) {
            check(
                exact.exactMatches().contains(i) == values.get(i).equals("target-dogmatic-value"),
                "String.equals parity " + i);
        }
        check(exact.exactChecks() < values.size(), "numeric equality first pass prunes exact checks");
    }

    private static void stringContainsParity() {
        List<String> values = List.of(
            "dog", "dogma", "dogmatic", "hotdog", "catalog", "cat", "do", "d-o-g",
            "prefix-dog-suffix", "something completely different", "");
        M3ObjectProjector<String> projector = M3ObjectProjectors.byString(value -> value);
        M3ObjectSignalTable table = M3ObjectSignalTable.build(values, projector, M3PoolProgressMonitor.none());
        M3ObjectSignal needle = projector.project("dog", M3PoolProgressMonitor.none());

        M3ObjectSignalFirstPassResult pass = table.firstPass(
            M3ObjectSignalQuery.maybeContains(needle), M3PoolProgressMonitor.none());
        M3ObjectSignalExactResult exact = M3ObjectSignalMatcher.verify(
            values, pass, value -> value.contains("dog"), M3PoolProgressMonitor.none());

        for (int i = 0; i < values.size(); i++) {
            boolean direct = values.get(i).contains("dog");
            check(exact.exactMatches().contains(i) == direct, "String.contains parity " + i);
            if (direct) check(pass.candidates().contains(i), "first pass has no contains false negative " + i);
        }
        check(pass.afterBitwiseSignals() < values.size(), "contains feature planes reject candidates");
    }

    private static void customBitSignals() {
        record Row(long flags) { }
        List<Row> values = List.of(
            new Row(0b001), new Row(0b101), new Row(0b111), new Row(0b100), new Row(0b1001));
        int domain = M3ObjectSignalDomains.CUSTOM_BASE + 7;
        M3ObjectSignalTable table = M3ObjectSignalTable.build(
            values,
            M3ObjectProjectors.bySignal(domain, Row::flags),
            M3PoolProgressMonitor.none());

        M3ObjectSignalFirstPassResult pass = table.firstPass(
            M3ObjectSignalQuery.mask(domain, 0b101, 0b010),
            M3PoolProgressMonitor.none());
        for (int i = 0; i < values.size(); i++) {
            long flags = values.get(i).flags();
            boolean expected = (flags & 0b101) == 0b101 && (flags & 0b010) == 0;
            check(pass.candidates().contains(i) == expected, "custom mask parity " + i);
        }
    }

    private static void bulkBackendParity() {
        record Row(long flags) { }
        int size = 20_000;
        List<Row> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            long flags = 0L;
            if ((i & 1) == 0) flags |= 1L << 3;
            if (i % 3 == 0) flags |= 1L << 11;
            if (i % 5 == 0) flags |= 1L << 19;
            if (i % 7 == 0) flags |= 1L << 27;
            values.add(new Row(flags));
        }
        int domain = M3ObjectSignalDomains.CUSTOM_BASE + 99;
        M3ObjectSignalTable table = M3ObjectSignalTable.build(
            values, M3ObjectProjectors.bySignal(domain, Row::flags), M3PoolProgressMonitor.none());
        long required = (1L << 3) | (1L << 11);
        long forbidden = 1L << 27;
        M3ObjectSignalFirstPassResult pass = table.firstPass(
            M3ObjectSignalQuery.mask(domain, required, forbidden), M3PoolProgressMonitor.none());

        for (int i = 0; i < size; i++) {
            long flags = values.get(i).flags();
            boolean expected = (flags & required) == required && (flags & forbidden) == 0L;
            check(pass.candidates().contains(i) == expected, "bulk backend parity " + i);
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
