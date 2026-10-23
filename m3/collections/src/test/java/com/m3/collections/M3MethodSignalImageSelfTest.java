/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

/** Independent Java 21 v1 compiler sidecar contract/CSR oracle, with adversarial source data. */
public final class M3MethodSignalImageSelfTest {
    private static final String HEADER = "# M3JDK21-METHOD-SIGNALS/1\n"
            + "# M=exact source AST counts; C=attributed method-call candidates, not runtime observations\n"
            + "# method identity is the attributed owner/signature, not a lexical hash\n";
    private static long checks;

    private M3MethodSignalImageSelfTest() { }

    public static void main(String[] args) throws Exception {
        fixedSchemaContract();
        rejectInvalidSourceArtifacts();
        randomizedMethodGraphs();
        if (args.length == 1) actualCompilerSidecar(java.nio.file.Path.of(args[0]));
        System.out.println("PASS M3MethodSignalImageSelfTest checks=" + checks);
    }

    private static void fixedSchemaContract() throws Exception {
        String body = HEADER
                + method("demo.A#run():void", 2, 4)
                + "C\tdemo.A#run():void\tcall:DIRECT:demo.B#step(int;):void\t3\n"
                + "C\tdemo.A#run():void\tcall:DISPATCH_CANDIDATE:demo.C#go():void\t2\n"
                + "C\tdemo.A#run():void\tnew:DIRECT:external.Helper#<init>():void\t5\n"
                + method("demo.B#step(int;):void", 0, 0)
                + "C\tdemo.B#step(int;):void\tcall:DIRECT:demo.C#go():void\t7\n"
                + method("demo.C#go():void", 1, 0);
        M3MethodSignalImage image = M3MethodSignalImage.parse(seal(body));
        check(image.methodCount() == 3, "method cardinality");
        check(image.columns().rowCount() == 3 && image.columns().columnCount() == 17,
                "exact 14 plus 3 primitive columns");
        check(image.methodSignature(0).equals("demo.A#run():void"), "exact method signature");
        check(image.methodSignature(2).equals("demo.C#go():void"), "lexicographic source method ordinal");
        check(image.columns().snapshotColumn(0).integers()[0] == 2, "static scalar exact integer");
        check(image.columns().snapshotColumn(1).integers()[0] == 4, "second source scalar exact");
        check(image.columns().snapshotColumn(16).longs()[0] == 5L,
                "external site column survives scalar/graph separation");
        check(image.columns().snapshotColumn(16).origin() == M3SignalColumns.Origin.STATIC,
                "external candidates are static source metadata, not measured runtime");
        check(image.externalCandidateRecords() == 1 && image.externalCandidateSites() == 5L,
                "unseen target is explicit external evidence");
        check(image.externalSitesAt(0) == 5L && image.externalSitesAt(1) == 0L,
                "per-method external site index");
        check(image.relationships().edgeCount() == 3, "internal relation record count");
        check(image.relationships().outgoingEdges(0) == 2, "caller CSR offsets");
        check(image.relationships().outgoingSites(0) == 5L, "caller static-site multiplicity");
        check(image.relationships().incomingSites(2) == 9L, "combined incoming sites");
        check(image.relationships().reachableTypes(0, 1, false) == 1,
                "direct method reachability within one hop");
        check(image.relationships().reachableTypes(0, 2, false) == 2,
                "second-hop static candidate topology");
        check(image.relationships().reachableTypes(0, 1, true) == 2,
                "candidate dispatch does not become a direct edge");
        check(image.sourceSha256().equals(seal(body).split("# sha256\\t")[1].trim()),
                "strong source evidence preserved");
        check(!image.sourceSha256().isBlank(), "nonempty source digest");
        M3MethodSignalImage empty = M3MethodSignalImage.parse(seal(HEADER));
        check(empty.methodCount() == 0 && empty.relationships().edgeCount() == 0,
                "empty exact compiler image is admitted");
    }

    private static void rejectInvalidSourceArtifacts() throws Exception {
        String first = HEADER + method("demo.A#run():void", 1, 2);
        String good = seal(first);
        refuse(() -> M3MethodSignalImage.parse(good.replace("\t1\t2\t", "\t4\t2\t")),
                "tampered digest");
        refuse(() -> M3MethodSignalImage.parse(good.substring(0, good.length() - 2)),
                "truncated trailer");
        refuse(() -> M3MethodSignalImage.parse(good + "# extra\n"),
                "trailing injected record");
        refuse(() -> M3MethodSignalImage.parse(seal(first + method("demo.A#run():void", 1, 2))),
                "duplicate method");
        refuse(() -> M3MethodSignalImage.parse(seal(HEADER + method("demo.Z#run():void", 1, 2)
                + method("demo.A#run():void", 1, 2))), "unsorted methods");
        refuse(() -> M3MethodSignalImage.parse(seal(HEADER +
                "C\tdemo.A#run():void\tcall:DIRECT:demo.B#run():void\t2\n")),
                "call without declaring method");
        refuse(() -> M3MethodSignalImage.parse(seal(first +
                "C\tdemo.A#run():void\tnew:DISPATCH_CANDIDATE:demo.B#<init>():void\t1\n")),
                "virtual constructor classification");
        refuse(() -> M3MethodSignalImage.parse(seal(first +
                "C\tdemo.A#run():void\tcall:DIRECT:demo.B#run():void\t0\n")),
                "zero callsite count");
        refuse(() -> M3MethodSignalImage.parse(seal(first.replace("\t1\t2\t", "\t-1\t2\t"))),
                "negative AST signal");
        refuse(() -> M3MethodSignalImage.parse(seal(first +
                "C\tdemo.A#run():void\tcall:DIRECT:demo.B#run():void\t1\n"
                + "C\tdemo.A#run():void\tcall:DIRECT:demo.B#run():void\t3\n")),
                "duplicate expression key");
        refuse(() -> M3MethodSignalImage.parse(seal(first + "# arbitrary comment after method\n")),
                "unrecognized inline comment");
        refuse(() -> M3MethodSignalImage.parse(seal(first.replace("demo.A#run():void", "demo.\\q#run():void"))),
                "malformed escaped type ID");
    }

    private static void randomizedMethodGraphs() throws Exception {
        Random random = new Random(0x4d334d4554484f44L);
        for (int trial = 0; trial < 650; trial++) {
            int nodes = 1 + random.nextInt(48);
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < nodes; i++) ids.add(String.format("demo.M%03d#f():void", i));
            List<TreeMap<String, Integer>> outgoing = new ArrayList<>();
            long[] expectedIn = new long[nodes], expectedOut = new long[nodes];
            long[] external = new long[nodes];
            int externalRecords = 0;
            int totalKnownEdges = 0;
            for (int i = 0; i < nodes; i++) {
                TreeMap<String, Integer> arcs = new TreeMap<>();
                int rows = random.nextInt(10);
                for (int k = 0; k < rows; k++) {
                    int target = random.nextInt(nodes + 4);
                    boolean direct = random.nextBoolean();
                    String signature = target < nodes ? ids.get(target) : "external.Z" + target + "#z():void";
                    String shape = direct ? "call:DIRECT:" : "call:DISPATCH_CANDIDATE:";
                    arcs.merge(shape + signature, 1 + random.nextInt(5), Integer::sum);
                }
                outgoing.add(arcs);
                for (var arc : arcs.entrySet()) {
                    int count = arc.getValue();
                    int marker = arc.getKey().indexOf(':', arc.getKey().indexOf(':') + 1) + 1;
                    int target = Collections.binarySearch(ids, arc.getKey().substring(marker));
                    if (target >= 0) {
                        expectedIn[target] += count;
                        expectedOut[i] += count;
                        totalKnownEdges++;
                    } else {
                        external[i] += count;
                        externalRecords++;
                    }
                }
            }
            StringBuilder body = new StringBuilder(HEADER);
            for (int i = 0; i < nodes; i++) {
                body.append(method(ids.get(i), i, i + 1));
                for (var arc : outgoing.get(i).entrySet()) {
                    body.append("C\t").append(ids.get(i)).append('\t').append(arc.getKey())
                            .append('\t').append(arc.getValue()).append('\n');
                }
            }
            M3MethodSignalImage image = M3MethodSignalImage.parse(seal(body.toString()));
            check(image.methodCount() == nodes, "retained exact method rows");
            check(image.relationships().edgeCount() == totalKnownEdges, "known edge count");
            check(image.externalCandidateRecords() == externalRecords, "external record count");
            for (int i = 0; i < nodes; i++) {
                check(ids.get(i).equals(image.methodSignature(i)), "method identity round trip");
                check(image.columns().snapshotColumn(0).integers()[i] == i, "source count lane");
                check(image.relationships().incomingSites(i) == expectedIn[i], "known in-site total");
                check(image.relationships().outgoingSites(i) == expectedOut[i], "known out-site total");
                check(image.externalSitesAt(i) == external[i], "external sites not dropped");
                check(image.columns().snapshotColumn(16).longs()[i] == external[i],
                        "external site column exactness");
            }
        }
    }

    private static void actualCompilerSidecar(java.nio.file.Path path) throws Exception {
        String raw = java.nio.file.Files.readString(path, StandardCharsets.UTF_8);
        M3MethodSignalImage image = M3MethodSignalImage.parse(raw);
        check(image.methodCount() >= 2, "original javac methodOut v1 produced methods");
        check(image.columns().columnCount() == 17, "real compiler columns");
        check(image.sourceSha256().matches("[0-9a-f]{64}"), "actual compiler digest");
        boolean foundCall = false;
        for (int i = 0; i < image.methodCount(); i++) {
            foundCall |= image.relationships().outgoingEdges(i) > 0 || image.externalSitesAt(i) > 0;
        }
        check(foundCall, "original javac emitted attributed method call candidate");
    }

    private static String method(String identity, int a, int b) {
        StringBuilder s = new StringBuilder("M\t").append(identity).append('\t').append(a)
                .append('\t').append(b);
        for (int i = 2; i < 14; i++) s.append("\t0");
        return s.append('\n').toString();
    }

    private static String seal(String body) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(body.getBytes(StandardCharsets.UTF_8));
            return body + "# sha256\t" + HexFormat.of().formatHex(digest) + '\n';
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static void check(boolean trueValue, String meaning) {
        checks++;
        if (!trueValue) throw new AssertionError(meaning + " check=" + checks);
    }

    private static void refuse(Runnable action, String meaning) {
        checks++;
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("expected compiler-image refusal: " + meaning);
    }
}
