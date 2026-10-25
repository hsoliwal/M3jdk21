/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Random;
import java.util.TreeMap;

/** Independent deterministic method-signal schema, graph and refusal oracle. */
public final class M3MethodSignalImageSelfTest {
    private static final String HEADER = "# M3JDK21-METHOD-SIGNALS/1\n"
            + "# M=exact source AST counts; C=attributed method-call candidates, not runtime observations\n"
            + "# method identity is the attributed owner/signature, not a lexical hash\n";
    private static long assertions;

    private M3MethodSignalImageSelfTest() { }

    public static void main(String[] arguments) throws Exception {
        runAll();
        if (arguments.length > 1) throw new IllegalArgumentException("at most one compiler sidecar");
        if (arguments.length == 1) {
            String file = Files.readString(Path.of(arguments[0]), StandardCharsets.UTF_8);
            M3MethodSignalImage image = M3MethodSignalImage.parse(file);
            check(image.rowCount() > 0, "real compiler sidecar contains attributed methods");
            check(image.sourceSha256().equals(digest(file.substring(0, file.lastIndexOf("# sha256\t")))),
                    "real compiler sidecar hash binds entire source image");
            long internal = 0;
            for (int row = 0; row < image.rowCount(); row++) {
                check(image.methodName(row).contains("#"), "attributed method identity");
                internal += image.relationships().outgoingSites(row);
            }
            check(internal > 0, "real javac direct and candidate edges were indexed");
        }
        System.out.println("PASS M3MethodSignalImageSelfTest assertions=" + assertions);
    }

    public static void runAll() {
        emptyImageAndCanonicalIdentity();
        deterministicTwoMethodGraph();
        schemaAndTrustRefusals();
        seededDifferential();
    }

    private static void emptyImageAndCanonicalIdentity() {
        String image = seal(HEADER);
        var empty = M3MethodSignalImage.parse(image);
        check(empty.rowCount() == 0, "empty compiler universe is valid");
        check(empty.columns().columnCount() == M3MethodSignalImage.Scalar.values().length,
                "all dimensions retained for empty image");
        check(empty.relationships().edgeCount() == 0, "empty graph");
        check(empty.sourceSha256().equals(digest(HEADER)), "complete source digest");
        check(empty.externalCandidateSites() == 0 && empty.unresolvedCandidateSites() == 0,
                "unknown edges are not fabricated");
        throwsAs(IndexOutOfBoundsException.class, () -> empty.methodName(0));
        throwsAs(IndexOutOfBoundsException.class, () -> empty.reachableMethods(0, 1, true));
    }

    private static void deterministicTwoMethodGraph() {
        String alpha = "p.A#alpha():void";
        String beta = "p.B#beta():void";
        int[] first = counts();
        first[0] = 2;
        first[4] = 1;
        first[5] = 1;
        first[7] = 2;
        first[12] = 3;
        int[] second = counts();
        second[0] = 1;
        second[6] = 1;
        second[13] = 1;
        String payload = HEADER + method(alpha, first)
                + call(alpha, "call:DIRECT:" + beta, 1)
                + call(alpha, "reference:DISPATCH_CANDIDATE:p.Other#test():void", 1)
                + method(beta, second);
        var image = M3MethodSignalImage.parse(seal(payload));
        check(image.rowCount() == 2, "method universe");
        check(image.methodName(0).equals(alpha) && image.methodName(1).equals(beta),
                "exact attributed names, not their hashes");
        check(image.sourceSha256().equals(digest(payload)), "producer artifact is authenticated");
        check(image.columns().columnCount() == 16, "fourteen source counts plus two graph counts");
        var columns = image.columns();
        check(columns.kind(0) == M3SignalColumns.Kind.INTEGER
                && columns.origin(0) == M3SignalColumns.Origin.STATIC,
                "compiler source coordinate kind");
        check(columns.snapshotColumn(0).integers()[0] == 2, "call syntax count");
        check(columns.snapshotColumn(7).integers()[0] == 2, "loop syntax count");
        check(columns.snapshotColumn(12).integers()[0] == 3, "field-reference syntax count");
        check(columns.snapshotColumn(13).integers()[1] == 1, "write syntax count");
        check(columns.snapshotColumn(14).longs()[1] == 1L, "known incoming count");
        check(columns.snapshotColumn(15).longs()[0] == 1L, "known outgoing count");
        int[] snapshot = columns.snapshotColumn(0).integers();
        snapshot[0] = -90;
        check(columns.snapshotColumn(0).integers()[0] == 2, "defensive primitive lane");
        check(image.relationships().edgeCount() == 1, "external candidates excluded from CSR");
        check(image.attributedSites(0, 1, false) == 1, "direct same-image method edge");
        check(image.attributedSites(1, 0, true) == 0, "no invented reciprocal edge");
        check(image.reachableMethods(0, 1, false) == 1, "bounded known adjacency");
        check(image.reachableMethods(0, 0, false) == 0, "zero hop");
        check(image.externalCandidateRecords() == 1 && image.externalCandidateSites() == 1,
                "resolved outside image is not a known internal callee");
        check(image.unresolvedCandidateSites() == 1,
                "unresolved compiler call is distinct from external known symbol");
        throwsAs(IllegalArgumentException.class, () -> image.reachableMethods(0, 129, true));
        throwsAs(IndexOutOfBoundsException.class, () -> image.attributedSites(-1, 0, true));
        throwsAs(IndexOutOfBoundsException.class, () -> image.attributedSites(0, 2, true));
    }

    private static void schemaAndTrustRefusals() {
        String alpha = "p.A#a():void";
        String beta = "p.B#b():void";
        int[] one = counts();
        one[0] = 1;
        one[4] = 1;
        String method = method(alpha, one);
        String edge = call(alpha, "call:DIRECT:" + beta, 1);
        String valid = seal(HEADER + method + edge);
        throwsAs(IllegalArgumentException.class, () -> M3MethodSignalImage.parse(
                valid.replace("DIRECT", "DISPATCH_CANDIDATE")));
        throwsAs(IllegalArgumentException.class, () -> M3MethodSignalImage.parse(valid + "\n"));
        throwsAs(IllegalArgumentException.class, () -> M3MethodSignalImage.parse(""));
        throwsAs(NullPointerException.class, () -> M3MethodSignalImage.parse(null));
        throwsPayload(HEADER + method + edge + edge); // sorted/unique C records
        throwsPayload(HEADER + method + method); // sorted/unique M records
        throwsPayload(HEADER + method(beta, counts()) + method); // M order
        throwsPayload(HEADER + call(alpha, "call:DIRECT:" + beta, 1)); // missing M
        throwsPayload(HEADER + method + call(beta, "call:DIRECT:" + alpha, 1)); // C owner
        throwsPayload(HEADER + method); // declared DIRECT count with no C evidence
        throwsPayload(HEADER + method + call(alpha, "call:DIRECT:" + beta, 2)); // mismatch
        throwsPayload(HEADER + method + call(alpha, "call:MAYBE:" + beta, 1)); // unsupported class
        throwsPayload(HEADER + method + call(alpha, "invoke:DIRECT:" + beta, 1)); // shape
        throwsPayload(HEADER + method + call(alpha, "call:DIRECT:anonymous", 1)); // no signature
        throwsPayload(HEADER + method + call(alpha, "call:DIRECT:" + beta, 0)); // zero
        throwsPayload(HEADER + method + call(alpha, "call:DIRECT:" + beta, -1)); // negative
        throwsPayload(HEADER + method + "C\t" + alpha + "\tcall:DIRECT:" + beta + "\t01\n");
        throwsPayload(HEADER + method + "C\t" + alpha + "\tcall:DIRECT:" + beta + "\t+1\n");
        throwsPayload(HEADER + method + "Q\tunknown\n");
        throwsPayload(HEADER + method + "\n");
        throwsPayload(HEADER + "# comment after records\n");
        throwsPayload(HEADER + method.replace("\t1\t", "\t-1\t") + edge);
        throwsPayload(HEADER + method.replace("\t" + alpha + "\t", "\t" + alpha + "\\q\t") + edge);
        throwsPayload(HEADER + method.replace("\t" + alpha + "\t", "\t" + alpha + "\\\\x\t") + edge);
    }

    private static void seededDifferential() {
        Random random = new Random(0x5A17C0DEL);
        for (int trial = 0; trial < 350; trial++) {
            int n = random.nextInt(40) + 1;
            String[] names = new String[n];
            long[][] direct = new long[n][n], dispatch = new long[n][n];
            long[] external = new long[n];
            int[] externalRecords = new int[n];
            StringBuilder content = new StringBuilder(HEADER);
            for (int row = 0; row < n; row++) names[row] = name(row);
            for (int row = 0; row < n; row++) {
                TreeMap<String, Integer> calls = new TreeMap<>();
                int count = random.nextInt(100);
                long directCount = 0, dispatchCount = 0;
                for (int edge = 0; edge < count; edge++) {
                    int target = random.nextInt(n + 3);
                    boolean staticCall = random.nextBoolean();
                    String kind = staticCall ? "DIRECT" : "DISPATCH_CANDIDATE";
                    String shape = switch (random.nextInt(3)) {
                        case 0 -> "call";
                        case 1 -> "new";
                        default -> "reference";
                    };
                    String callee = target < n ? names[target] : "external.E#missing():void";
                    String key = shape + ':' + kind + ':' + callee;
                    calls.merge(key, 1, Integer::sum);
                    if (staticCall) directCount++; else dispatchCount++;
                    if (target < n) {
                        if (staticCall) direct[row][target]++;
                        else dispatch[row][target]++;
                    } else external[row]++;
                }
                int[] facts = counts();
                facts[0] = count;
                facts[4] = Math.toIntExact(directCount);
                facts[5] = Math.toIntExact(dispatchCount);
                content.append(method(names[row], facts));
                for (var call : calls.entrySet()) {
                    content.append(call(names[row], call.getKey(), call.getValue()));
                    if (call.getKey().endsWith("external.E#missing():void")) externalRecords[row]++;
                }
            }
            M3MethodSignalImage image = M3MethodSignalImage.parse(seal(content.toString()));
            check(image.rowCount() == n, "seeded method cardinality");
            long expectedExternal = Arrays.stream(external).sum();
            check(image.externalCandidateSites() == expectedExternal, "seeded unknown targets");
            check(image.externalCandidateRecords() == Arrays.stream(externalRecords).sum(),
                    "seeded external call shapes");
            for (int source = 0; source < n; source++) {
                check(image.methodName(source).equals(names[source]), "seeded identity");
                long outgoing = 0, incoming = 0;
                for (int target = 0; target < n; target++) {
                    check(image.attributedSites(source, target, false) == direct[source][target],
                            "seeded direct-only call multiplicity");
                    check(image.attributedSites(source, target, true)
                                    == direct[source][target] + dispatch[source][target],
                            "seeded all-candidate call multiplicity");
                    outgoing += direct[source][target] + dispatch[source][target];
                    incoming += direct[target][source] + dispatch[target][source];
                }
                check(image.relationships().outgoingSites(source) == outgoing,
                        "seeded outgoing source count");
                check(image.relationships().incomingSites(source) == incoming,
                        "seeded incoming source count");
            }
            for (int source = 0; source < n; source += Math.max(1, n / 8)) {
                for (int hops = 0; hops <= 3; hops++) {
                    for (boolean includeDispatch : new boolean[]{false, true}) {
                        check(image.reachableMethods(source, hops, includeDispatch)
                                == reachable(direct, dispatch, source, hops, includeDispatch),
                                "independent bounded BFS differential");
                    }
                }
            }
            check(image.sourceSha256().equals(digest(content.toString())),
                    "seeded complete content hash");
        }
    }

    private static int reachable(long[][] direct, long[][] dispatch,
            int source, int hops, boolean includeDispatch) {
        int size = direct.length;
        boolean[] seen = new boolean[size];
        int[] wave = new int[size];
        int head = 0, tail = 1, end = 1;
        wave[0] = source;
        seen[source] = true;
        for (int level = 0; level < hops && head < tail; level++) {
            while (head < end) {
                int row = wave[head++];
                for (int next = 0; next < size; next++) {
                    if ((direct[row][next] > 0 || includeDispatch && dispatch[row][next] > 0)
                            && !seen[next]) {
                        seen[next] = true;
                        wave[tail++] = next;
                    }
                }
            }
            end = tail;
        }
        return tail - 1;
    }

    private static String name(int row) {
        return "p.T#m" + String.format(Locale.ROOT, "%04d", row) + "():void";
    }
    private static int[] counts() { return new int[14]; }
    private static String method(String name, int[] fields) {
        StringBuilder line = new StringBuilder("M\t").append(escape(name));
        for (int field : fields) line.append('\t').append(field);
        return line.append('\n').toString();
    }
    private static String call(String owner, String label, int occurrences) {
        return "C\t" + escape(owner) + "\t" + escape(label) + "\t" + occurrences + '\n';
    }
    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\t", "\\t")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
    private static String seal(String payload) { return payload + "# sha256\t" + digest(payload) + '\n'; }
    private static String digest(String payload) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static void throwsPayload(String payload) {
        throwsAs(IllegalArgumentException.class, () -> M3MethodSignalImage.parse(seal(payload)));
    }
    private static void throwsAs(Class<? extends Throwable> kind, Runnable operation) {
        try { operation.run(); }
        catch (Throwable observed) {
            check(kind.isInstance(observed), "wrong exception: " + observed);
            return;
        }
        throw new AssertionError("expected " + kind.getSimpleName());
    }
    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
