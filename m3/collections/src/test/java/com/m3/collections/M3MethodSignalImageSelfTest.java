/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

/** Strict Java21 negative-evidence and seeded graph differential for the existing method plugin. */
public final class M3MethodSignalImageSelfTest {
    private static long checks;
    private M3MethodSignalImageSelfTest() { }

    public static void main(String[] args) throws Exception {
        trustedSample();
        malformedImagesRefuse();
        randomizedMethodGraphs();
        if (args.length == 1) {
            var compiled = M3MethodSignalImage.parse(
                    Files.readString(Path.of(args[0]), StandardCharsets.UTF_8));
            yes(compiled.methodCount() >= 2, "actual javac methods present");
            yes(compiled.columns().columnCount() == 16, "14 method AST counts and 2 graph columns");
            yes(compiled.sourceSha256().matches("[a-f0-9]{64}"), "compiler output fully sealed");
        }
        System.out.println("PASS M3MethodSignalImageSelfTest checks=" + checks);
    }

    private static void trustedSample() {
        var image = M3MethodSignalImage.parse(seal(sample()));
        yes(image.methodCount() == 3, "three source method IDs");
        yes(image.methodSignature(0).equals("demo.A#a():void"), "attributed source owner");
        yes(image.columns().snapshotColumn(0).integers()[0] == 2, "exact invocation count");
        yes(image.columns().columnCount() == M3MethodSignalImage.Scalar.values().length,
                "typed primitive source dimension count");
        yes(image.untrackedTargetRecords() == 1 && image.untrackedTargetSites() == 1,
                "out-of-compilation targets are explicit");
        yes(image.unresolvedCallSites() == 1, "unattributed call expression remains unknown");
        yes(!image.recordedTargetsContained(), "not a graph completeness certificate");
        M3SignalRelations graph = image.relationships();
        yes(graph.rowCount() == 3 && graph.edgeCount() == 2, "known-method CSR");
        yes(graph.target(0, 0) == 1 && graph.kind(0, 0) == M3SignalRelations.DIRECT,
                "direct static source edge");
        yes(graph.target(0, 1) == 2
                && graph.kind(0, 1) == M3SignalRelations.DISPATCH_CANDIDATE,
                "virtual candidate source edge");
        yes(graph.incomingSites(1) == 1L && graph.outgoingSites(0) == 2L,
                "no duplication of source edge counts");
        yes(image.columns().snapshotColumn(15).longs()[0] == 2,
                "known-method graph site count as exact long");
        yes(image.sourceSha256().matches("[a-f0-9]{64}"), "full source SHA-256");
        yes(M3MethodSignalImage.parse(seal(headers())).methodCount() == 0,
                "legitimate empty compilation image");
    }

    private static void malformedImagesRefuse() {
        String valid = seal(sample());
        refuses(() -> M3MethodSignalImage.parse(valid.replace("demo.A#a():void",
                "demo.Z#a():void")), "SHA-256 payload tampering");
        refuses(() -> M3MethodSignalImage.parse(valid + "# extra"), "trailing payload");
        refuses(() -> M3MethodSignalImage.parse(seal(sample().replace(
                "M\tdemo.B#b():void", "M\tdemo.A#a():void"))), "method identity collision");
        refuses(() -> M3MethodSignalImage.parse(seal(sample().replace(
                "C\tdemo.A#a():void\tcall:DIRECT:demo.B#b():void\t1",
                "C\tdemo.A#a():void\tcall:DIRECT:demo.B#b():void\t0"))), "zero recorded call count");
        refuses(() -> M3MethodSignalImage.parse(seal(sample().replace(
                "C\tdemo.A#a():void\tcall:DIRECT:demo.B#b():void\t1\n", ""))),
                "missing edge despite accepted direct scalar");
        refuses(() -> M3MethodSignalImage.parse(seal(sample().replace(
                "M\tdemo.A#a():void\t2", "M\tdemo.A#a():void\t1"))),
                "inconsistent call site scalar");
        refuses(() -> M3MethodSignalImage.parse(seal(sample().replace(
                "C\tdemo.A#a():void\tcall:DISPATCH_CANDIDATE:demo.C#c():void\t1",
                "C\tdemo.A#a():void\tcall:UNKNOWN:demo.C#c():void\t1"))),
                "unrecognized dispatch category");
        refuses(() -> M3MethodSignalImage.parse(seal(sample().replace(
                "M\tdemo.C#c():void\t0", "M\tdemo.C#c():void\t-1"))),
                "negative AST count");
        refuses(() -> M3MethodSignalImage.parse(seal(sample().replace(
                "# method identity is the attributed owner/signature, not a lexical hash",
                "# invented producer format"))), "unknown protocol header");
        refuses(() -> M3MethodSignalImage.parse(valid + "\uD800"), "unpaired surrogate");
    }

    private static void randomizedMethodGraphs() {
        Random random = new Random(0x4d334d4554484f44L);
        for (int trial = 0; trial < 1_200; trial++) {
            int nodes = 1 + random.nextInt(36);
            int links = random.nextInt(125);
            var calls = new ArrayList<TreeMap<String, Integer>>();
            int[] direct = new int[nodes], dispatch = new int[nodes];
            int[] unresolved = new int[nodes];
            int[] total = new int[nodes];
            for (int row = 0; row < nodes; row++) calls.add(new TreeMap<>());
            long knownSites = 0, externalSites = 0;
            for (int edge = 0; edge < links; edge++) {
                int from = random.nextInt(nodes), to = random.nextInt(nodes + 2);
                boolean staticTarget = random.nextBoolean();
                int sites = 1 + random.nextInt(5);
                String method = to < nodes ? signature(to) : "external.M#run():void";
                String call = "call:" + (staticTarget ? "DIRECT:" : "DISPATCH_CANDIDATE:") + method;
                calls.get(from).merge(call, sites, Integer::sum);
                total[from] += sites;
                if (staticTarget) direct[from] += sites; else dispatch[from] += sites;
                if (to < nodes) knownSites += sites; else externalSites += sites;
            }
            StringBuilder image = new StringBuilder(headers());
            for (int row = 0; row < nodes; row++) {
                unresolved[row] = random.nextInt(3);
                total[row] += unresolved[row];
                image.append("M\t").append(signature(row)).append('\t')
                        .append(total[row]).append("\t0\t0\t0\t")
                        .append(direct[row]).append('\t').append(dispatch[row]).append('\t')
                        .append(unresolved[row]).append("\t0\t0\t0\t0\t0\t0\t0\n");
                for (var edge : calls.get(row).entrySet()) {
                    image.append("C\t").append(signature(row)).append('\t')
                            .append(edge.getKey()).append('\t').append(edge.getValue())
                            .append('\n');
                }
            }
            var parsed = M3MethodSignalImage.parse(seal(image.toString()));
            yes(parsed.methodCount() == nodes, "method table identity count");
            long emitted = 0;
            for (int row = 0; row < nodes; row++) {
                yes(parsed.methodSignature(row).equals(signature(row)), "source-ordered signatures");
                yes(parsed.columns().snapshotColumn(0).integers()[row] == total[row],
                        "exact static invocation counts");
                yes(parsed.columns().snapshotColumn(4).integers()[row] == direct[row],
                        "direct source call count");
                yes(parsed.columns().snapshotColumn(5).integers()[row] == dispatch[row],
                        "virtual source candidate count");
                emitted += parsed.relationships().outgoingSites(row);
            }
            yes(emitted == knownSites && parsed.untrackedTargetSites() == externalSites,
                    "known vs external call-site conservation");
        }
    }

    private static String signature(int row) {
        return "test.M" + String.format("%03d", row) + "#run():void";
    }
    private static String headers() {
        return "# M3JDK21-METHOD-SIGNALS/1\n"
                + "# M=exact source AST counts; C=attributed method-call candidates, not runtime observations\n"
                + "# method identity is the attributed owner/signature, not a lexical hash\n";
    }
    private static String sample() {
        return headers()
                + "M\tdemo.A#a():void\t2\t0\t0\t0\t1\t1\t0\t0\t1\t0\t0\t0\t1\t0\n"
                + "C\tdemo.A#a():void\tcall:DIRECT:demo.B#b():void\t1\n"
                + "C\tdemo.A#a():void\tcall:DISPATCH_CANDIDATE:demo.C#c():void\t1\n"
                + "M\tdemo.B#b():void\t2\t0\t0\t0\t1\t0\t1\t0\t0\t0\t0\t0\t0\t0\n"
                + "C\tdemo.B#b():void\tcall:DIRECT:outside.X#x():void\t1\n"
                + "M\tdemo.C#c():void\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\n";
    }
    private static String seal(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8));
            return content + "# sha256\t" + HexFormat.of().formatHex(digest) + "\n";
        } catch (Exception impossible) { throw new AssertionError(impossible); }
    }
    private static void yes(boolean value, String description) {
        checks++;
        if (!value) throw new AssertionError(description + " case=" + checks);
    }
    private static void refuses(Runnable operation, String description) {
        checks++;
        try { operation.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("required method evidence refusal: " + description);
    }
}
