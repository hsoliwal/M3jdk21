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
 * Strict receiving image of the existing javac M3MethodSignals methodOut protocol.
 *
 * <p>A compiler M row describes a method's fourteen exact source-AST counters;
 * a C row is an attributed call-expression candidate, not a dynamic invocation.
 * The owning M3SignalColumns and M3SignalRelations retain only primitive arrays.
 * Unknown/external callees remain separately counted, never silently closed.
 * SHA-256 validates image integrity, not the identity of a trusted producer.</p>
 */
public final class M3MethodSignalImage {
    private static final String HEADER = "# M3JDK21-METHOD-SIGNALS/1";
    private static final String COMMENT_COUNTS =
            "# M=exact source AST counts; C=attributed method-call candidates, not runtime observations";
    private static final String COMMENT_IDENTITY =
            "# method identity is the attributed owner/signature, not a lexical hash";
    private static final String TRAILER = "# sha256\t";
    private static final int COUNTERS = 14;
    private static final int MAX_IMAGE_CHARS = 64 * 1024 * 1024;
    private static final int MAX_METHODS = 1_000_000;
    private static final int MAX_EDGES = 2_000_000;

    /** Source counts followed by graph degrees; no value is a runtime frequency. */
    public enum Signal {
        INVOCATIONS, OBJECT_ALLOCATIONS, ARRAY_ALLOCATIONS, MEMBER_REFERENCES,
        DIRECT_CALLS, DISPATCH_CANDIDATES, UNRESOLVED_CALLS, LOOPS, BRANCHES,
        TRY_BLOCKS, CATCHES, LAMBDAS, FIELD_REFERENCES, FIELD_WRITES,
        KNOWN_INCOMING_SITES, KNOWN_OUTGOING_SITES, EXTERNAL_OUTGOING_SITES
    }

    private record Edge(String owner, String candidate, byte kind, int sites) { }

    private final M3SignalColumns columns;
    private final M3SignalRelations relationships;
    private final byte[] names;
    private final int[] nameOffsets;
    private final long[] externalByMethod;
    private final String sourceSha256;
    private final long sourceIdentityHint;
    private final int externalRecords;
    private final long externalSites;

    private M3MethodSignalImage(M3SignalColumns columns, M3SignalRelations relationships,
                                byte[] names, int[] nameOffsets, long[] externalByMethod,
                                String sourceSha256, long sourceIdentityHint,
                                int externalRecords, long externalSites) {
        this.columns = columns;
        this.relationships = relationships;
        this.names = names;
        this.nameOffsets = nameOffsets;
        this.externalByMethod = externalByMethod;
        this.sourceSha256 = sourceSha256;
        this.sourceIdentityHint = sourceIdentityHint;
        this.externalRecords = externalRecords;
        this.externalSites = externalSites;
    }

    /**
     * Verify the entire existing v1 sidecar and manufacture a detached primitive image.
     * No compiler, reflective object or original source text is retained after parsing.
     */
    public static M3MethodSignalImage parse(String text) {
        Objects.requireNonNull(text, "text");
        if (text.length() > MAX_IMAGE_CHARS || !text.endsWith("\n")
                || text.getBytes(StandardCharsets.UTF_8).length > MAX_IMAGE_CHARS) {
            throw new IllegalArgumentException("oversized or unterminated method image");
        }
        int trailerAt = text.lastIndexOf(TRAILER);
        if (trailerAt <= 0 || trailerAt + TRAILER.length() + 65 != text.length()
                || text.charAt(trailerAt - 1) != '\n') {
            throw new IllegalArgumentException("missing or malformed SHA-256 trailer");
        }
        String digest = text.substring(trailerAt + TRAILER.length(), text.length() - 1);
        if (!digest.matches("[0-9a-f]{64}")
                || !digest.equals(sha256(text.substring(0, trailerAt)))) {
            throw new IllegalArgumentException("method image SHA-256 mismatch");
        }
        String[] lines = text.substring(0, trailerAt).split("\n", -1);
        if (lines.length < 4 || !HEADER.equals(lines[0])
                || !COMMENT_COUNTS.equals(lines[1])
                || !COMMENT_IDENTITY.equals(lines[2])
                || !lines[lines.length - 1].isEmpty()) {
            throw new IllegalArgumentException("unsupported method image schema");
        }
        List<String> identities = new ArrayList<>();
        List<int[]> facts = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();
        String current = null;
        String priorCall = null;
        for (int line = 3; line < lines.length - 1; line++) {
            String record = lines[line];
            String[] fields = record.split("\t", -1);
            if (fields.length == COUNTERS + 2 && "M".equals(fields[0])) {
                String id = decodedCanonical(fields[1]);
                if (!validSignature(id) || (current != null && id.compareTo(current) <= 0)) {
                    throw new IllegalArgumentException("duplicate, invalid or unsorted method identity");
                }
                if (identities.size() >= MAX_METHODS) {
                    throw new IllegalArgumentException("method count budget exceeded");
                }
                int[] numbers = new int[COUNTERS];
                for (int i = 0; i < numbers.length; i++) {
                    numbers[i] = nonnegative(fields[i + 2]);
                }
                identities.add(id);
                facts.add(numbers);
                current = id;
                priorCall = null;
            } else if (fields.length == 4 && "C".equals(fields[0])) {
                String id = decodedCanonical(fields[1]);
                String key = decodedCanonical(fields[2]);
                if (current == null || !current.equals(id)
                        || (priorCall != null && key.compareTo(priorCall) <= 0)) {
                    throw new IllegalArgumentException("out-of-order or duplicate method call");
                }
                byte kind = callKind(key);
                String callee = key.substring(key.indexOf(':', key.indexOf(':') + 1) + 1);
                if (!validSignature(callee)) {
                    throw new IllegalArgumentException("invalid attributed callee method");
                }
                if (edges.size() >= MAX_EDGES) {
                    throw new IllegalArgumentException("method call edge budget exceeded");
                }
                edges.add(new Edge(id, callee, kind, positive(fields[3])));
                priorCall = key;
            } else {
                throw new IllegalArgumentException("unrecognized or malformed method record");
            }
        }
        return build(identities, facts, edges, digest);
    }

    private static M3MethodSignalImage build(List<String> ids, List<int[]> counts,
                                              List<Edge> edges, String digest) {
        int rows = ids.size();
        IntLane sources = new IntLane();
        IntLane targets = new IntLane();
        IntLane sites = new IntLane();
        ByteLane kinds = new ByteLane();
        long[] external = new long[rows];
        int unknown = 0;
        long unknownSites = 0;
        for (Edge edge : edges) {
            int source = Collections.binarySearch(ids, edge.owner());
            if (source < 0) throw new IllegalStateException("source identity inventory drift");
            int target = Collections.binarySearch(ids, edge.candidate());
            if (target < 0) {
                unknown = Math.addExact(unknown, 1);
                unknownSites = Math.addExact(unknownSites, edge.sites());
                external[source] = Math.addExact(external[source], edge.sites());
            } else {
                sources.add(source);
                targets.add(target);
                sites.add(edge.sites());
                kinds.add(edge.kind());
            }
        }
        M3SignalRelations graph = M3SignalRelations.of(rows, sources.toArray(),
                targets.toArray(), kinds.toArray(), sites.toArray());
        M3SignalColumns.Column[] typed = new M3SignalColumns.Column[Signal.values().length];
        long prefix = ByteBuffer.wrap(HexFormat.of().parseHex(digest), 0, 8).getLong();
        long[] validity = M3SignalColumns.allValid(rows);
        for (int column = 0; column < COUNTERS; column++) {
            int[] values = new int[rows];
            for (int row = 0; row < rows; row++) values[row] = counts.get(row)[column];
            typed[column] = new M3SignalColumns.Column(M3SignalColumns.Kind.INTEGER,
                    M3SignalColumns.Origin.STATIC, prefix ^ column, false,
                    validity, values, null, null);
        }
        long[] incoming = new long[rows], outgoing = new long[rows];
        for (int row = 0; row < rows; row++) {
            incoming[row] = graph.incomingSites(row);
            outgoing[row] = graph.outgoingSites(row);
        }
        long[][] graphCounts = {incoming, outgoing, external};
        for (int i = 0; i < graphCounts.length; i++) {
            typed[COUNTERS + i] = new M3SignalColumns.Column(M3SignalColumns.Kind.LONG,
                    M3SignalColumns.Origin.STATIC, prefix ^ (COUNTERS + i), false,
                    validity, null, graphCounts[i], null);
        }
        int[] offsets = new int[rows + 1];
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (int row = 0; row < rows; row++) {
            offsets[row] = output.size();
            output.writeBytes(ids.get(row).getBytes(StandardCharsets.UTF_8));
        }
        offsets[rows] = output.size();
        return new M3MethodSignalImage(new M3SignalColumns(rows, typed), graph,
                output.toByteArray(), offsets, external, digest, prefix, unknown, unknownSites);
    }

    /** Number of distinct source-declared method signatures in the verified image. */
    public int methodCount() { return nameOffsets.length - 1; }

    /** The original compiler source's full SHA-256 identity, not a signature of its producer. */
    public String sourceSha256() { return sourceSha256; }

    /** A low-storage index hint only; never use as exact identity without sourceSha256(). */
    public long sourceIdentityHint() { return sourceIdentityHint; }

    /** Existing immutable primitive fact owner. */
    public M3SignalColumns columns() { return columns; }

    /** Existing immutable direct/candidate CSR relationship owner. */
    public M3SignalRelations relationships() { return relationships; }

    /** Decode a method identity only on explicit public API demand. */
    public String methodSignature(int row) {
        int checked = Objects.checkIndex(row, methodCount());
        return new String(names, nameOffsets[checked],
                nameOffsets[checked + 1] - nameOffsets[checked], StandardCharsets.UTF_8);
    }

    /** Unknown/external method-call record count, including direct and candidate shapes. */
    public int externalCandidateRecords() { return externalRecords; }

    /** Count of compiler call expressions to method signatures absent from this image. */
    public long externalCandidateSites() { return externalSites; }

    /** Per-recorded-method count of external or unknown call expressions. */
    public long externalSitesAt(int row) {
        return externalByMethod[Objects.checkIndex(row, methodCount())];
    }

    private static byte callKind(String edge) {
        int a = edge.indexOf(':'), b = edge.indexOf(':', a + 1);
        if (a <= 0 || b <= a + 1 || b + 1 >= edge.length()) {
            throw new IllegalArgumentException("invalid method-call shape");
        }
        String shape = edge.substring(0, a);
        if (!shape.equals("call") && !shape.equals("new") && !shape.equals("reference")) {
            throw new IllegalArgumentException("unknown source call-expression shape");
        }
        byte kind = switch (edge.substring(a + 1, b)) {
            case "DIRECT" -> M3SignalRelations.DIRECT;
            case "DISPATCH_CANDIDATE" -> M3SignalRelations.DISPATCH_CANDIDATE;
            default -> throw new IllegalArgumentException("unknown dispatch classification");
        };
        if (shape.equals("new") && kind != M3SignalRelations.DIRECT) {
            throw new IllegalArgumentException("a constructor cannot be a virtual candidate");
        }
        return kind;
    }

    private static boolean validSignature(String signature) {
        int owner = signature.indexOf('#');
        int params = signature.indexOf('(', owner + 1);
        int ending = signature.lastIndexOf("):");
        return owner > 0 && params > owner + 1 && ending > params
                && ending + 2 < signature.length()
                && signature.indexOf('\n') < 0 && signature.indexOf('\r') < 0
                && signature.indexOf('\t') < 0;
    }

    private static int positive(String source) {
        int number = nonnegative(source);
        if (number == 0) throw new IllegalArgumentException("zero compiler call occurrence");
        return number;
    }

    private static int nonnegative(String source) {
        try {
            int value = Integer.parseInt(source);
            if (value >= 0) return value;
        } catch (NumberFormatException broken) {
            throw new IllegalArgumentException("invalid compiler counter", broken);
        }
        throw new IllegalArgumentException("negative compiler counter");
    }

    private static String decodedCanonical(String escaped) {
        StringBuilder out = new StringBuilder(escaped.length());
        for (int i = 0; i < escaped.length(); i++) {
            char c = escaped.charAt(i);
            if (c != '\\') { out.append(c); continue; }
            if (++i >= escaped.length()) {
                throw new IllegalArgumentException("unterminated compiler text escape");
            }
            switch (escaped.charAt(i)) {
                case '\\' -> out.append('\\');
                case 't' -> out.append('\t');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                default -> throw new IllegalArgumentException("unknown compiler text escape");
            }
        }
        String decoded = out.toString();
        if (!escape(decoded).equals(escaped)) {
            throw new IllegalArgumentException("noncanonical compiler text escape");
        }
        return decoded;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\t", "\\t")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    /** Transient dynamically resized primitive collection of row/edge coordinates. */
    private static final class IntLane {
        private int[] buffer = new int[16];
        private int size;
        void add(int value) {
            if (size == buffer.length) buffer = Arrays.copyOf(buffer, Math.multiplyExact(size, 2));
            buffer[size++] = value;
        }
        int[] toArray() { return Arrays.copyOf(buffer, size); }
    }

    private static final class ByteLane {
        private byte[] buffer = new byte[16];
        private int size;
        void add(byte value) {
            if (size == buffer.length) buffer = Arrays.copyOf(buffer, Math.multiplyExact(size, 2));
            buffer[size++] = value;
        }
        byte[] toArray() { return Arrays.copyOf(buffer, size); }
    }
}
