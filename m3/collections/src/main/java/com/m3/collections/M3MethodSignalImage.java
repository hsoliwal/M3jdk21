/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Version-checked consumer of the existing opt-in javac M3JDK21-METHOD-SIGNALS/1 artifact.
 * Method identities are attributed compiler signatures, not heuristic hashes. Known
 * static call candidates are held as primitive CSR; external/unresolved calls are retained
 * as separate counts, never promoted to a runtime execution or graph-closure certificate.
 */
public final class M3MethodSignalImage {
    private static final String HEADER = "# M3JDK21-METHOD-SIGNALS/1";
    private static final String COUNTS =
            "# M=exact source AST counts; C=attributed method-call candidates, not runtime observations";
    private static final String IDENTITIES =
            "# method identity is the attributed owner/signature, not a lexical hash";
    private static final String TRAILER = "# sha256\t";
    private static final int SCALARS = 14;
    private static final int MAX_BYTES = 64 * 1024 * 1024;
    private static final int MAX_METHODS = 500_000;
    private static final int MAX_EDGES = 2_000_000;

    /** Exact source-site counters, not runtime profiling or proven method effects. */
    public enum Scalar {
        INVOCATIONS, NEW_OBJECTS, NEW_ARRAYS, MEMBER_REFERENCES, DIRECT_CALLS,
        DISPATCH_CANDIDATES, UNRESOLVED_CALLS, LOOPS, BRANCHES, TRIES,
        CATCHES, LAMBDAS, FIELD_REFERENCES, FIELD_WRITES,
        KNOWN_INCOMING_SITES, KNOWN_OUTGOING_SITES
    }

    private final byte[] names;
    private final int[] nameOffsets;
    private final M3SignalColumns columns;
    private final M3SignalRelations relations;
    private final String sourceSha256;
    private final long sourceHint;
    private final int externalRecords;
    private final long externalSites;
    private final long unresolvedSites;

    private M3MethodSignalImage(byte[] names, int[] offsets, M3SignalColumns columns,
            M3SignalRelations relations, String sha, long hint,
            int externalRecords, long externalSites, long unresolvedSites) {
        this.names = names;
        this.nameOffsets = offsets;
        this.columns = columns;
        this.relations = relations;
        this.sourceSha256 = sha;
        this.sourceHint = hint;
        this.externalRecords = externalRecords;
        this.externalSites = externalSites;
        this.unresolvedSites = unresolvedSites;
    }

    /** Refuses unsupported, malformed, inconsistent or unsealed method facts before publication. */
    public static M3MethodSignalImage parse(String source) {
        Objects.requireNonNull(source, "source");
        if (source.length() > MAX_BYTES || !source.startsWith(HEADER + "\n")
                || !source.endsWith("\n")) {
            throw new IllegalArgumentException("unsupported or oversized method image");
        }
        byte[] utf8 = source.getBytes(StandardCharsets.UTF_8);
        if (utf8.length > MAX_BYTES
                || !new String(utf8, StandardCharsets.UTF_8).equals(source)) {
            throw new IllegalArgumentException("method image exceeds byte budget or malformed UTF-8");
        }
        int position = source.lastIndexOf(TRAILER);
        if (position < HEADER.length() || source.charAt(position - 1) != '\n'
                || source.length() != position + TRAILER.length() + 65) {
            throw new IllegalArgumentException("invalid method image trailer");
        }
        String sha = source.substring(position + TRAILER.length(), source.length() - 1);
        if (!sha.matches("[0-9a-f]{64}") || !sha.equals(sha256(source.substring(0, position)))) {
            throw new IllegalArgumentException("method signal SHA-256 mismatch");
        }
        String[] lines = source.substring(0, position).split("\n", -1);
        if (lines.length < 4 || !HEADER.equals(lines[0])
                || !COUNTS.equals(lines[1]) || !IDENTITIES.equals(lines[2])) {
            throw new IllegalArgumentException("unsupported method producer headers");
        }

        List<String> identities = new ArrayList<>();
        List<int[]> values = new ArrayList<>();
        IntLane sources = new IntLane();
        IntLane siteCounts = new IntLane();
        ByteLane kinds = new ByteLane();
        List<String> targets = new ArrayList<>();
        String lastMethod = null, lastCall = null;
        int row = -1;
        long direct = 0L, candidate = 0L;
        for (int line = 3; line < lines.length; line++) {
            String item = lines[line];
            if (item.isEmpty() && line == lines.length - 1) continue;
            String[] fields = item.split("\t", -1);
            if (fields[0].equals("M")) {
                if (row >= 0 && (direct != values.get(row)[4]
                        || candidate != values.get(row)[5])) {
                    throw new IllegalArgumentException("method edge counts contradict scalar facts");
                }
                if (fields.length != SCALARS + 2) {
                    throw new IllegalArgumentException("method scalar geometry");
                }
                String identity = unescape(fields[1]);
                if (identity.isBlank() || identity.indexOf('#') < 1
                        || (lastMethod != null && lastMethod.compareTo(identity) >= 0)) {
                    throw new IllegalArgumentException("unordered or duplicate method ID");
                }
                if (identities.size() >= MAX_METHODS) {
                    throw new IllegalArgumentException("method count budget exceeded");
                }
                int[] counters = new int[SCALARS];
                for (int i = 0; i < SCALARS; i++) counters[i] = nonnegative(fields[i + 2]);
                if ((long) counters[0] + counters[1] + counters[3]
                        != (long) counters[4] + counters[5] + counters[6]) {
                    throw new IllegalArgumentException("compiler call-site accounting inconsistent");
                }
                identities.add(identity);
                values.add(counters);
                row++;
                lastMethod = identity;
                lastCall = null;
                direct = candidate = 0L;
            } else if (fields[0].equals("C")) {
                if (fields.length != 4 || row < 0
                        || !identities.get(row).equals(unescape(fields[1]))) {
                    throw new IllegalArgumentException("call-edge owner/geometry mismatch");
                }
                String call = unescape(fields[2]);
                if (lastCall != null && lastCall.compareTo(call) >= 0) {
                    throw new IllegalArgumentException("duplicate or unsorted call-edge shape");
                }
                EdgeTarget target = target(call);
                int count = positive(fields[3]);
                if (sources.size() >= MAX_EDGES) {
                    throw new IllegalArgumentException("call-edge budget exceeded");
                }
                sources.add(row);
                siteCounts.add(count);
                kinds.add(target.kind());
                targets.add(target.identity());
                if (target.kind() == M3SignalRelations.DIRECT) {
                    direct = Math.addExact(direct, count);
                } else {
                    candidate = Math.addExact(candidate, count);
                }
                lastCall = call;
            } else {
                throw new IllegalArgumentException("unrecognized method signal row");
            }
        }
        if (row >= 0 && (direct != values.get(row)[4]
                || candidate != values.get(row)[5])) {
            throw new IllegalArgumentException("last method edges contradict scalar facts");
        }

        IntLane knownFrom = new IntLane(), knownTo = new IntLane(), knownSites = new IntLane();
        ByteLane knownKinds = new ByteLane();
        int unknownRecords = 0;
        long unknownSites = 0L;
        for (int i = 0; i < sources.size(); i++) {
            int target = Collections.binarySearch(identities, targets.get(i));
            if (target < 0) {
                unknownRecords++;
                unknownSites = Math.addExact(unknownSites, siteCounts.get(i));
            } else {
                knownFrom.add(sources.get(i));
                knownTo.add(target);
                knownSites.add(siteCounts.get(i));
                knownKinds.add(kinds.get(i));
            }
        }
        M3SignalRelations graph = M3SignalRelations.of(identities.size(), knownFrom.toArray(),
                knownTo.toArray(), knownKinds.toArray(), knownSites.toArray());
        long hint = ByteBuffer.wrap(HexFormat.of().parseHex(sha), 0, 8).getLong();
        long[] validity = M3SignalColumns.allValid(identities.size());
        M3SignalColumns.Column[] columns = new M3SignalColumns.Column[Scalar.values().length];
        for (int column = 0; column < SCALARS; column++) {
            int[] facts = new int[identities.size()];
            for (int i = 0; i < facts.length; i++) facts[i] = values.get(i)[column];
            columns[column] = new M3SignalColumns.Column(M3SignalColumns.Kind.INTEGER,
                    M3SignalColumns.Origin.STATIC, hint ^ column, false,
                    validity, facts, null, null);
        }
        long[] in = new long[identities.size()], out = new long[identities.size()];
        long unresolved = 0L;
        for (int i = 0; i < identities.size(); i++) {
            in[i] = graph.incomingSites(i);
            out[i] = graph.outgoingSites(i);
            unresolved = Math.addExact(unresolved, values.get(i)[6]);
        }
        columns[SCALARS] = new M3SignalColumns.Column(M3SignalColumns.Kind.LONG,
                M3SignalColumns.Origin.STATIC, hint ^ SCALARS, false,
                validity, null, in, null);
        columns[SCALARS + 1] = new M3SignalColumns.Column(M3SignalColumns.Kind.LONG,
                M3SignalColumns.Origin.STATIC, hint ^ (SCALARS + 1), false,
                validity, null, out, null);

        int[] offsets = new int[identities.size() + 1];
        ByteArrayOutputStream packed = new ByteArrayOutputStream();
        for (int i = 0; i < identities.size(); i++) {
            offsets[i] = packed.size();
            packed.writeBytes(identities.get(i).getBytes(StandardCharsets.UTF_8));
        }
        offsets[identities.size()] = packed.size();
        return new M3MethodSignalImage(packed.toByteArray(), offsets,
                new M3SignalColumns(identities.size(), columns), graph, sha, hint,
                unknownRecords, unknownSites, unresolved);
    }

    public int methodCount() { return nameOffsets.length - 1; }
    public String methodSignature(int row) {
        int at = Objects.checkIndex(row, methodCount());
        return new String(names, nameOffsets[at], nameOffsets[at + 1] - nameOffsets[at],
                StandardCharsets.UTF_8);
    }
    public M3SignalColumns columns() { return columns; }
    public M3SignalRelations relationships() { return relations; }
    public String sourceSha256() { return sourceSha256; }
    /** Index hint, never a substitute for the full SHA-256 source identity. */
    public long sourceIdentityHint() { return sourceHint; }
    public int untrackedTargetRecords() { return externalRecords; }
    public long untrackedTargetSites() { return externalSites; }
    public long unresolvedCallSites() { return unresolvedSites; }
    /** Containment in emitted methods only; never runtime-dispatch graph closure. */
    public boolean recordedTargetsContained() {
        return externalRecords == 0 && unresolvedSites == 0;
    }

    private record EdgeTarget(String identity, byte kind) { }
    private static EdgeTarget target(String value) {
        int first = value.indexOf(':'), second = value.indexOf(':', first + 1);
        if (first <= 0 || second <= first + 1 || second == value.length() - 1) {
            throw new IllegalArgumentException("malformed method edge");
        }
        String shape = value.substring(0, first);
        if (!shape.equals("call") && !shape.equals("new") && !shape.equals("reference")) {
            throw new IllegalArgumentException("unknown source call expression");
        }
        byte kind = switch (value.substring(first + 1, second)) {
            case "DIRECT" -> M3SignalRelations.DIRECT;
            case "DISPATCH_CANDIDATE" -> M3SignalRelations.DISPATCH_CANDIDATE;
            default -> throw new IllegalArgumentException("unknown attributed edge kind");
        };
        String identity = value.substring(second + 1);
        if (identity.indexOf('#') < 1 || identity.indexOf('(') < 0) {
            throw new IllegalArgumentException("missing attributed method signature");
        }
        return new EdgeTarget(identity, kind);
    }
    private static int nonnegative(String text) {
        try {
            int number = Integer.parseInt(text);
            if (number >= 0) return number;
        } catch (NumberFormatException bad) {
            throw new IllegalArgumentException("bad attributed scalar", bad);
        }
        throw new IllegalArgumentException("negative attributed scalar");
    }
    private static int positive(String text) {
        int number = nonnegative(text);
        if (number == 0) throw new IllegalArgumentException("zero recorded call sites");
        return number;
    }
    private static String unescape(String raw) {
        StringBuilder result = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c != '\\') { result.append(c); continue; }
            if (++i == raw.length()) throw new IllegalArgumentException("dangling source escape");
            switch (raw.charAt(i)) {
                case '\\' -> result.append('\\');
                case 't' -> result.append('\t');
                case 'n' -> result.append('\n');
                case 'r' -> result.append('\r');
                default -> throw new IllegalArgumentException("invalid source escape");
            }
        }
        return result.toString();
    }
    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    /** Invocation-scoped primitive staging only; never retained as per-edge Java objects. */
    private static final class IntLane {
        private int[] values = new int[16];
        private int size;
        void add(int value) {
            if (size == values.length) values = Arrays.copyOf(values, size << 1);
            values[size++] = value;
        }
        int size() { return size; }
        int get(int at) { return values[Objects.checkIndex(at, size)]; }
        int[] toArray() { return Arrays.copyOf(values, size); }
    }
    private static final class ByteLane {
        private byte[] values = new byte[16];
        private int size;
        void add(byte value) {
            if (size == values.length) values = Arrays.copyOf(values, size << 1);
            values[size++] = value;
        }
        byte get(int at) { return values[Objects.checkIndex(at, size)]; }
        byte[] toArray() { return Arrays.copyOf(values, size); }
    }
}
