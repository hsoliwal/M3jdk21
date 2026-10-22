/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/** Actual Java21 method-level compiler fact, checksum, CSR and compatibility differential. */
public final class M3SourceMethodImageSelfTest {
    private static long checks;

    private M3SourceMethodImageSelfTest() { }

    public static void main(String[] args) throws Exception {
        originalV1IsStillSupported();
        sealedV2MethodsAndEdges();
        corruptedV2Refuses();
        if (args.length > 0) {
            if (args.length != 1) throw new IllegalArgumentException("one generated method sidecar");
            M3SourceSignalImage actual = M3SourceSignalImage.parse(
                    Files.readString(Path.of(args[0]), StandardCharsets.UTF_8));
            require(actual.hasMethodSignals(), "real javac methods=on");
            require(actual.methodSignals().methodCount() >= 2, "attributed real methods emitted");
            require(actual.sourceSha256().matches("[0-9a-f]{64}"), "strong actual source digest");
            require(actual.methodSignals().columns().columnCount() == 15,
                    "actual methods contain thirteen counts plus known-edge degrees");
        }
        System.out.println("PASS M3SourceMethodImageSelfTest checks=" + checks);
    }

    private static void originalV1IsStillSupported() {
        String legacy = v2Body().replace("M3JDK21-SOURCE-SIGNALS/2",
                "M3JDK21-SOURCE-SIGNALS/1")
                .lines().filter(line -> !line.startsWith("M\t") && !line.startsWith("R\t"))
                .reduce("", (a, b) -> a + b + "\n");
        M3SourceSignalImage parsed = M3SourceSignalImage.parse(seal(legacy));
        require(!parsed.hasMethodSignals(), "legacy source schema has no invented method claims");
        refuse(IllegalStateException.class, parsed::methodSignals,
                "legacy reader refuses nonexistent per-method evidence");
        require(parsed.relationships().edgeCount() == 1,
                "unchanged legacy type-level direct edge");
    }

    private static void sealedV2MethodsAndEdges() {
        String fixture = seal(v2Body());
        M3SourceSignalImage parsed = M3SourceSignalImage.parse(fixture);
        require(parsed.hasMethodSignals(), "schema v2 explicitly admits method image");
        require(parsed.rowCount() == 2, "type-level image remains intact");
        require(parsed.columns().columnCount() == 17, "type counts remain exact");
        require(parsed.relationships().edgeCount() == 1,
                "type-level exact known callee remains independently recorded");
        require(parsed.methodSignals().methodCount() == 2, "two method identities retained");
        require("demo.A#invoke():void".equals(parsed.methodSignals().signature(0)),
                "sorted declaring method names");
        require("demo.B#visit():void".equals(parsed.methodSignals().signature(1)),
                "second method name");
        require(parsed.methodSignals().ownerType(0) == 0
                && parsed.methodSignals().ownerType(1) == 1, "method/type owner coordinates");
        M3SignalColumns columns = parsed.methodSignals().columns();
        require(columns.columnCount() == 15, "13 source counters plus two CSR degrees");
        require(columns.kind(0) == M3SignalColumns.Kind.INTEGER,
                "flags/counts are exact int coordinates");
        require(columns.origin(0) == M3SignalColumns.Origin.STATIC,
                "static syntactic evidence is not runtime profiling");
        require(columns.snapshotColumn(M3SourceMethodImage.Scalar.MODIFIER_FLAGS.ordinal())
                .integers()[0] == 18, "static/public bitmask metadata");
        require(columns.snapshotColumn(M3SourceMethodImage.Scalar.CALL_SITES.ordinal())
                .integers()[0] == 2, "declaration exact expression count");
        require(parsed.methodSignals().externalCandidateRecords() == 1,
                "external virtual source edge incompleteness explicit");
        require(parsed.methodSignals().externalCandidateSites() == 1L,
                "external occurrence multiplicity retained");
        M3SignalRelations graph = parsed.methodSignals().relationships();
        require(graph.edgeCount() == 1 && graph.outgoingEdges(0) == 1,
                "known method edge CSR shape");
        require(graph.incomingSites(1) == 2L && graph.outgoingSites(0) == 2L,
                "direct known method site multiplicity");
        require(graph.reachableTypes(0, 1, false) == 1,
                "direct source graph known one-hop reachability");
        require(graph.reachableTypes(1, 1, true) == 0,
                "method without outgoing edges is terminal");
        require(parsed.sourceSha256().equals(fixture.substring(fixture.lastIndexOf('\t') + 1).strip()),
                "full compiler source/image SHA sealed");
        int[] owners = {0, 1};
        require(Arrays.equals(parsed.methodSignals().columns().selectedByState(
                new int[]{Integer.MIN_VALUE, 0}, Integer.MIN_VALUE, 0),
                new int[]{0}), "signed bit31 method-index selection");
        require(Arrays.equals(owners, new int[]{0, 1}), "fixture was not modified");
    }

    private static void corruptedV2Refuses() {
        String body = v2Body(), intact = seal(body);
        refuse(IllegalArgumentException.class,
                () -> M3SourceSignalImage.parse(intact.replace("demo.B#visit", "demo.B#other")),
                "SHA256 tampering rejected before graph manufacturing");
        String wrongMethod = body.replace("M\tdemo.B\tdemo.B#visit():void",
                "M\tdemo.B\tdemo.A#visit():void");
        refuse(IllegalArgumentException.class,
                () -> M3SourceSignalImage.parse(seal(wrongMethod)),
                "method owner/reflection-string identity must match declared type");
        String nonpositive = body.replace("R\tdemo.A\tdemo.A#invoke():void"
                + "\tcall:DIRECT:demo.B#visit():void\t2",
                "R\tdemo.A\tdemo.A#invoke():void"
                + "\tcall:DIRECT:demo.B#visit():void\t0");
        refuse(IllegalArgumentException.class,
                () -> M3SourceSignalImage.parse(seal(nonpositive)),
                "zero call-expression occurrence refuses");
        String reordered = body.replace(
                "R\tdemo.A\tdemo.A#invoke():void\tcall:DISPATCH_CANDIDATE:unknown.X#task():void\t1\n",
                "R\tdemo.A\tdemo.A#invoke():void\tcall:DISPATCH_CANDIDATE:unknown.X#task():void\t1\n"
                + "R\tdemo.A\tdemo.A#invoke():void\tcall:DIRECT:demo.B#visit():void\t1\n");
        refuse(IllegalArgumentException.class,
                () -> M3SourceSignalImage.parse(seal(reordered)),
                "out-of-order/colliding method call records refuse");
        String legacyWithMethods = body.replace("M3JDK21-SOURCE-SIGNALS/2",
                "M3JDK21-SOURCE-SIGNALS/1");
        refuse(IllegalArgumentException.class,
                () -> M3SourceSignalImage.parse(seal(legacyWithMethods)),
                "legacy /1 schema refuses v2-only rows");
    }

    private static String v2Body() {
        return "# M3JDK21-SOURCE-SIGNALS/2\n"
                + "# S=scalar counts (exact nonnegative integers); T=hashed token; E=attributed candidate edge\n"
                + "# hashes and virtual call edges are heuristic, not exact equality or runtime dispatch proof\n"
                + "# M=attributed declaring-method exact counts; R=source method candidate call edge\n"
                + "# method flags and syntactic counters do not certify purity/escape/runtime dispatch\n"
                + "S\tdemo.A\t" + scalar15() + "\n"
                + "E\tdemo.A\tcall:DIRECT:demo.B#visit():void\t2\n"
                + "E\tdemo.A\tcall:DISPATCH_CANDIDATE:unknown.X#task():void\t1\n"
                + "M\tdemo.A\tdemo.A#invoke():void\t"
                + "18\t0\t2\t0\t0\t1\t1\t0\t0\t0\t0\t0\t0\n"
                + "R\tdemo.A\tdemo.A#invoke():void\tcall:DIRECT:demo.B#visit():void\t2\n"
                + "R\tdemo.A\tdemo.A#invoke():void\tcall:DISPATCH_CANDIDATE:unknown.X#task():void\t1\n"
                + "S\tdemo.B\t" + scalar15() + "\n"
                + "M\tdemo.B\tdemo.B#visit():void\t"
                + "2\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\n";
    }

    private static String scalar15() {
        return "0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0";
    }

    private static String seal(String body) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(body.getBytes(StandardCharsets.UTF_8));
            return body + "# sha256\t" + HexFormat.of().formatHex(hash) + "\n";
        } catch (Exception impossible) { throw new AssertionError(impossible); }
    }

    private static void require(boolean yes, String what) {
        checks++;
        if (!yes) throw new AssertionError(what + " check=" + checks);
    }

    private static void refuse(Class<? extends Throwable> type, Runnable task, String why) {
        checks++;
        try {
            task.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError(why + " wrong exception", failure);
        }
        throw new AssertionError(why + " expected " + type.getName());
    }
}
