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

/** Strict Java21 differential for sealed javac sidecar intake and primitive relationship CSR. */
public final class M3SourceSignalImageSelfTest {
    private static long checks;
    private M3SourceSignalImageSelfTest() {}
    public static void main(String[] args) throws Exception {
        sealedCompilerCase();
        rejectInvalidProducerArtifacts();
        relationshipDifferential();
        if (args.length == 1) {
            M3SourceSignalImage compiler = M3SourceSignalImage.parse(
                    Files.readString(Path.of(args[0]), StandardCharsets.UTF_8));
            check(compiler.rowCount() >= 2, "actual attributed javac source types");
            check(compiler.columns().columnCount() == 17,
                    "real compiler counts plus internal graph degree lanes");
            check(compiler.relationships().edgeCount() >= 1,
                    "actual attributed source relation candidate");
            check(compiler.sourceSha256().matches("[0-9a-f]{64}"),
                    "compiler identity is SHA-256 sealed");
        }
        System.out.println("PASS M3SourceSignalImageSelfTest checks=" + checks);
    }
    private static void sealedCompilerCase() throws Exception {
        String content = sample();
        M3SourceSignalImage image = M3SourceSignalImage.parse(content);
        check(image.rowCount() == 3, "three sorted source types");
        check(image.typeName(0).equals("demo.A") && image.typeName(2).equals("demo.C"),
                "name table coordinates");
        check(image.sourceSha256().equals(content.substring(content.lastIndexOf('\t') + 1).strip()),
                "strong image digest");
        check(image.columns().columnCount() == M3SourceSignalImage.Scalar.values().length,
                "15 scalar + 2 internal graph counts");
        check(image.columns().kind(0) == M3SignalColumns.Kind.INTEGER,
                "compiler counters are exact integers");
        check(image.columns().origin(0) == M3SignalColumns.Origin.STATIC,
                "compiler facts are static, not a runtime sample");
        check(image.columns().snapshotColumn(0).integers()[0] == 2,
                "exact field count retained");
        check(image.hashedFeatures(0) == 2, "heuristic token count");
        check(image.featureHash(0, 0) == 1L, "unsigned hashed token");
        check(image.featureFamily(0, 1) == M3SourceSignalImage.Feature.CLASS,
                "explicit feature family");
        M3SignalRelations graph = image.relationships();
        check(graph.rowCount() == 3 && graph.edgeCount() == 3,
                "three attributed internal edges");
        check(graph.outgoingEdges(0) == 2 && graph.outgoingSites(0) == 5L,
                "source A direct/candidate call-site multiplicity");
        check(graph.incomingSites(2) == 8L,
                "incoming sites include static + virtual candidate edges");
        check(graph.reachableTypes(0, 1, false) == 1, "direct reachability one hop");
        check(graph.reachableTypes(0, 2, false) == 2, "two-hop static closure over known types");
        check(graph.reachableTypes(0, 1, true) == 2, "candidate closure is separately optional");
        check(image.externalCandidateRecords() == 1 && image.externalCandidateSites() == 4,
                "unseen plugin dependencies remain explicitly external");
        int[] selected = image.columns().selectedByState(
                new int[]{Integer.MIN_VALUE, 0, Integer.MIN_VALUE}, Integer.MIN_VALUE, 1);
        check(Arrays.equals(selected, new int[]{0, 2}), "physical state bit31 source selection");
        check(image.columns().sortSelected(selected).length == 2, "typed stable order");
    }
    private static void rejectInvalidProducerArtifacts() throws Exception {
        String accepted = sample();
        refuse(() -> M3SourceSignalImage.parse(accepted.replace("\t2\t", "\t3\t")),
                "SHA mismatch must refuse");
        refuse(() -> M3SourceSignalImage.parse(accepted.substring(0, accepted.length() - 2)),
                "trailer truncation refuses");
        refuse(() -> M3SourceSignalImage.parse(accepted + "# extra\n"),
                "extra trailing records refuse");
        String records = records();
        String duplicated = records + "S\tdemo.C\t" + zeros() + "\n";
        refuse(() -> M3SourceSignalImage.parse(seal(duplicated)), "duplicate type refusal");
        String badOrdering = records.replace("T\tdemo.A\tCLASS\tff\n",
                "T\tdemo.A\tPACKAGE\t0\n");
        refuse(() -> M3SourceSignalImage.parse(seal(badOrdering)), "unsorted family refusal");
        String badCount = records.replace("E\tdemo.B\tnew:DIRECT:demo.C#<init>():void\t6",
                "E\tdemo.B\tnew:DIRECT:demo.C#<init>():void\t0");
        refuse(() -> M3SourceSignalImage.parse(seal(badCount)), "zero edge refusal");
    }
    private static void relationshipDifferential() {
        Random rnd = new Random(0x4d33475241504821L);
        for (int trial = 0; trial < 1200; trial++) {
            int nodes = 1 + rnd.nextInt(44), edges = rnd.nextInt(260);
            int[] from = new int[edges], to = new int[edges], sites = new int[edges];
            byte[] kind = new byte[edges];
            List<List<Integer>> direct = new ArrayList<>(), all = new ArrayList<>();
            for (int i = 0; i < nodes; i++) {
                direct.add(new ArrayList<>());
                all.add(new ArrayList<>());
            }
            long[] incoming = new long[nodes], outgoing = new long[nodes];
            for (int i = 0; i < edges; i++) {
                from[i] = rnd.nextInt(nodes);
                to[i] = rnd.nextInt(nodes);
                sites[i] = 1 + rnd.nextInt(10);
                kind[i] = rnd.nextBoolean() ? M3SignalRelations.DIRECT
                        : M3SignalRelations.DISPATCH_CANDIDATE;
                all.get(from[i]).add(to[i]);
                if (kind[i] == M3SignalRelations.DIRECT) direct.get(from[i]).add(to[i]);
                incoming[to[i]] += sites[i];
                outgoing[from[i]] += sites[i];
            }
            M3SignalRelations graph = M3SignalRelations.of(nodes, from, to, kind, sites);
            check(graph.edgeCount() == edges, "edge multiset kept");
            for (int node = 0; node < nodes; node++) {
                check(graph.incomingSites(node) == incoming[node], "incoming sites");
                check(graph.outgoingSites(node) == outgoing[node], "outgoing sites");
                for (int hops = 0; hops <= 3; hops++) {
                    check(graph.reachableTypes(node, hops, false)
                            == bfs(direct, node, hops), "direct graph BFS");
                    check(graph.reachableTypes(node, hops, true)
                            == bfs(all, node, hops), "candidate graph BFS");
                }
            }
        }
        refuse(() -> M3SignalRelations.of(1, new int[]{0}, new int[]{0},
                new byte[]{4}, new int[]{1}), "unknown kind");
        refuse(() -> M3SignalRelations.of(1, new int[]{0}, new int[]{0},
                new byte[]{M3SignalRelations.DIRECT}, new int[]{0}), "zero occurrence");
    }
    private static int bfs(List<List<Integer>> graph, int root, int hops) {
        boolean[] seen = new boolean[graph.size()];
        int[] queue = new int[graph.size()];
        seen[root] = true;
        queue[0] = root;
        int read = 0, size = 1, limit = 1;
        for (int depth = 0; depth < hops && read < size; depth++) {
            while (read < limit) {
                for (int next : graph.get(queue[read++])) {
                    if (!seen[next]) { seen[next] = true; queue[size++] = next; }
                }
            }
            limit = size;
        }
        return size - 1;
    }
    private static String sample() throws Exception { return seal(records()); }
    private static String records() {
        return "# M3JDK21-SOURCE-SIGNALS/1\n"
                + "# S=scalar counts (exact nonnegative integers); T=hashed token; E=attributed candidate edge\n"
                + "# hashes and virtual call edges are heuristic, not exact equality or runtime dispatch proof\n"
                + "S\tdemo.A\t2\t1\t0\t2\t2\t0\t0\t1\t1\t0\t1\t1\t0\t0\t0\n"
                + "T\tdemo.A\tPACKAGE\t1\n"
                + "T\tdemo.A\tCLASS\tff\n"
                + "E\tdemo.A\tcall:DIRECT:demo.B#b():void\t3\n"
                + "E\tdemo.A\tcall:DISPATCH_CANDIDATE:demo.C#c():void\t2\n"
                + "E\tdemo.A\treference:DIRECT:unknown.Z#helper():void\t4\n"
                + "S\tdemo.B\t1\t2\t1\t2\t1\t1\t0\t1\t0\t0\t0\t0\t0\t0\t0\n"
                + "E\tdemo.B\tnew:DIRECT:demo.C#<init>():void\t6\n"
                + "S\tdemo.C\t0\t1\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\n";
    }
    private static String zeros() { return "0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0"; }
    private static String seal(String body) {
        try {
            byte[] sha = MessageDigest.getInstance("SHA-256")
                    .digest(body.getBytes(StandardCharsets.UTF_8));
            return body + "# sha256\t" + HexFormat.of().formatHex(sha) + "\n";
        } catch (Exception fail) { throw new AssertionError(fail); }
    }
    private static void check(boolean yes, String why) {
        checks++;
        if (!yes) throw new AssertionError(why + " check=" + checks);
    }
    private static void refuse(Runnable code, String why) {
        checks++;
        try { code.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("expected source-integrity refusal: " + why);
    }
}
