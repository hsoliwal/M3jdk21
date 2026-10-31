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
 * Verified receiving image for M3JDK21's existing javac M3SourceSignals plugin (/1 protocol).
 * The compiler already manufactures the static facts; this class parses its sealed sidecar once
 * into typed primitive columns, unsigned lexical-feature lanes and a CSR call-candidate graph.
 * No AST parsing, runtime reflection or per-row Entry objects are retained by the image.
 * A compiled call edge never certifies that the target was invoked or dynamically dispatched.
 */
public final class M3SourceSignalImage {
    private static final String HEADER = "# M3JDK21-SOURCE-SIGNALS/1\n";
    private static final String TRAILER = "# sha256\t";
    private static final int SCALARS = 15;
    private static final int MAX_BYTES = 64 * 1024 * 1024;
    private static final int MAX_ROWS = 500_000;
    private static final int MAX_FEATURES = 8_000_000;
    private static final Feature[] FEATURES = Feature.values();

    /** Scalars retain the plugin's exact field order, not inferred runtime measurements. */
    public enum Scalar {
        FIELDS, METHODS, CONSTRUCTORS, DISTINCT_TYPE_DEPENDENCIES, CALL_SITES,
        ALLOCATION_SITES, MEMBER_REFERENCES, DIRECT_CALLS, DISPATCH_CANDIDATES,
        UNRESOLVED_CALLS, LOOPS, BRANCHES, TRY_BLOCKS, CATCHES, LAMBDAS,
        INTERNAL_INCOMING_SITES, INTERNAL_OUTGOING_SITES
    }

    /** FNV tokens are approximate lexical features; hash agreement is not exact text identity. */
    public enum Feature {
        PACKAGE, CLASS, FIELDS, METHODS, DEPENDENCIES, CALLS, INHERITANCE, ANNOTATIONS
    }

    private final M3SignalColumns signals;
    private final M3SignalRelations relationships;
    private final int[] namesOffsets;
    private final byte[] nameBytes;
    private final int[] tokenOffsets;
    private final long[] featureHashes;
    private final byte[] featureKinds;
    private final String sourceSha256;
    private final long sourceIdentityHint;
    private final int externalCandidateRecords;
    private final long externalCandidateSites;

    private M3SourceSignalImage(M3SignalColumns signals, M3SignalRelations relationships,
                                int[] namesOffsets, byte[] nameBytes, int[] tokenOffsets,
                                long[] featureHashes, byte[] featureKinds, String sourceSha256,
                                long sourceIdentityHint, int externalCandidateRecords,
                                long externalCandidateSites) {
        this.signals = signals;
        this.relationships = relationships;
        this.namesOffsets = namesOffsets;
        this.nameBytes = nameBytes;
        this.tokenOffsets = tokenOffsets;
        this.featureHashes = featureHashes;
        this.featureKinds = featureKinds;
        this.sourceSha256 = sourceSha256;
        this.sourceIdentityHint = sourceIdentityHint;
        this.externalCandidateRecords = externalCandidateRecords;
        this.externalCandidateSites = externalCandidateSites;
    }

    /** Strictly validate the existing source compiler's SHA-256-sealed v1 text format. */
    public static M3SourceSignalImage parse(String text) {
        Objects.requireNonNull(text, "text");
        if (text.length() > MAX_BYTES) {
            throw new IllegalArgumentException("oversized compiler image");
        }
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_BYTES || !text.startsWith(HEADER) || !text.endsWith("\n")) {
            throw new IllegalArgumentException("unsupported or oversized compiler image");
        }
        int trailer = text.lastIndexOf(TRAILER);
        if (trailer <= HEADER.length() || text.charAt(trailer - 1) != '\n'
                || text.length() != trailer + TRAILER.length() + 65) {
            throw new IllegalArgumentException("missing or malformed source evidence trailer");
        }
        String digest = text.substring(trailer + TRAILER.length(), text.length() - 1);
        if (!digest.matches("[0-9a-f]{64}")
                || !digest.equals(sha256(text.substring(0, trailer)))) {
            throw new IllegalArgumentException("source evidence SHA-256 mismatch");
        }

        String[] lines = text.substring(0, trailer).split("\n", -1);
        List<String> names = new ArrayList<>();
        List<int[]> countRows = new ArrayList<>();
        LongLane hashes = new LongLane();
        ByteLane families = new ByteLane();
        IntLane tokenOffsets = new IntLane();
        IntLane source = new IntLane();
        IntLane counts = new IntLane();
        ByteLane kinds = new ByteLane();
        List<String> edgeTargets = new ArrayList<>();
        int currentRow = -1, phase = 0, previousFamily = -1;
        long previousHash = 0L;
        String lastCall = null;
        boolean headerComplete = false;
        for (String line : lines) {
            if (line.isEmpty()) continue;
            if (line.startsWith("# ")) {
                if (headerComplete) throw new IllegalArgumentException("comment after image records");
                continue;
            }
            headerComplete = true;
            String[] fields = line.split("\t", -1);
            switch (fields[0]) {
                case "S" -> {
                    if (fields.length != SCALARS + 2) throw new IllegalArgumentException("scalar shape");
                    String owner = unescape(fields[1]);
                    if (owner.isBlank() || (!names.isEmpty()
                            && owner.compareTo(names.getLast()) <= 0)) {
                        throw new IllegalArgumentException("unordered/duplicate source type");
                    }
                    if (names.size() >= MAX_ROWS) throw new IllegalArgumentException("too many types");
                    names.add(owner);
                    int[] values = new int[SCALARS];
                    for (int i = 0; i < SCALARS; i++) {
                        values[i] = nonnegative(fields[i + 2]);
                    }
                    countRows.add(values);
                    tokenOffsets.add(hashes.size());
                    currentRow++;
                    phase = 1;
                    previousFamily = -1;
                    lastCall = null;
                }
                case "T" -> {
                    if (fields.length != 4 || currentRow < 0 || phase > 2
                            || !names.getLast().equals(unescape(fields[1]))) {
                        throw new IllegalArgumentException("token owner/ordering");
                    }
                    int family = familyOrdinal(fields[2]);
                    long hashed;
                    try {
                        hashed = Long.parseUnsignedLong(fields[3], 16);
                    } catch (NumberFormatException bad) {
                        throw new IllegalArgumentException("invalid unsigned token", bad);
                    }
                    if (family < previousFamily || (family == previousFamily
                            && Long.compareUnsigned(previousHash, hashed) >= 0)) {
                        throw new IllegalArgumentException("unsorted/nonunique token signal");
                    }
                    hashes.add(hashed);
                    families.add((byte) family);
                    if (hashes.size() > MAX_FEATURES) throw new IllegalArgumentException("too many signals");
                    previousFamily = family;
                    previousHash = hashed;
                    phase = 2;
                }
                case "E" -> {
                    if (fields.length != 4 || currentRow < 0
                            || !names.getLast().equals(unescape(fields[1]))) {
                        throw new IllegalArgumentException("edge owner/shape");
                    }
                    String call = unescape(fields[2]);
                    if (lastCall != null && call.compareTo(lastCall) <= 0) {
                        throw new IllegalArgumentException("unsorted/duplicate call shape");
                    }
                    EdgeTarget target = edgeTarget(call);
                    source.add(currentRow);
                    edgeTargets.add(target.name());
                    kinds.add(target.kind());
                    counts.add(positive(fields[3]));
                    if (source.size() > MAX_FEATURES) throw new IllegalArgumentException("too many edges");
                    lastCall = call;
                    phase = 3;
                }
                default -> throw new IllegalArgumentException("unknown source signal row: " + fields[0]);
            }
        }
        tokenOffsets.add(hashes.size());
        int rows = names.size();
        int[] namePositions = new int[rows + 1];
        ByteArrayOutputStream packedNames = new ByteArrayOutputStream();
        for (int i = 0; i < rows; i++) {
            namePositions[i] = packedNames.size();
            packedNames.writeBytes(names.get(i).getBytes(StandardCharsets.UTF_8));
        }
        namePositions[rows] = packedNames.size();

        IntLane graphSources = new IntLane();
        IntLane graphTargets = new IntLane();
        IntLane graphSites = new IntLane();
        ByteLane graphKinds = new ByteLane();
        int unknown = 0;
        long unknownSites = 0;
        for (int i = 0; i < source.size(); i++) {
            int target = Collections.binarySearch(names, edgeTargets.get(i));
            if (target < 0) {
                unknown++;
                unknownSites = Math.addExact(unknownSites, counts.get(i));
            } else {
                graphSources.add(source.get(i));
                graphTargets.add(target);
                graphSites.add(counts.get(i));
                graphKinds.add(kinds.get(i));
            }
        }
        M3SignalRelations graph = M3SignalRelations.of(rows, graphSources.toArray(),
                graphTargets.toArray(), graphKinds.toArray(), graphSites.toArray());
        long evidence = ByteBuffer.wrap(HexFormat.of().parseHex(digest), 0, 8).getLong();
        M3SignalColumns.Column[] columns = new M3SignalColumns.Column[Scalar.values().length];
        long[] validity = M3SignalColumns.allValid(rows);
        for (int d = 0; d < SCALARS; d++) {
            int[] values = new int[rows];
            for (int i = 0; i < rows; i++) values[i] = countRows.get(i)[d];
            columns[d] = new M3SignalColumns.Column(M3SignalColumns.Kind.INTEGER,
                    M3SignalColumns.Origin.STATIC, evidence ^ d, false,
                    validity, values, null, null);
        }
        long[] in = new long[rows], out = new long[rows];
        for (int i = 0; i < rows; i++) {
            in[i] = graph.incomingSites(i);
            out[i] = graph.outgoingSites(i);
        }
        columns[SCALARS] = new M3SignalColumns.Column(M3SignalColumns.Kind.LONG,
                M3SignalColumns.Origin.STATIC, evidence ^ SCALARS, false,
                validity, null, in, null);
        columns[SCALARS + 1] = new M3SignalColumns.Column(M3SignalColumns.Kind.LONG,
                M3SignalColumns.Origin.STATIC, evidence ^ (SCALARS + 1), false,
                validity, null, out, null);
        return new M3SourceSignalImage(new M3SignalColumns(rows, columns), graph,
                namePositions, packedNames.toByteArray(), tokenOffsets.toArray(),
                hashes.toArray(), families.toArray(), digest, evidence, unknown, unknownSites);
    }

    public int rowCount() { return namesOffsets.length - 1; }
    public M3SignalColumns columns() { return signals; }
    public M3SignalRelations relationships() { return relationships; }
    public String sourceSha256() { return sourceSha256; }
    /** Non-certifying 64-bit index hint from SHA-256; full source identity is sourceSha256(). */
    public long sourceIdentityHint() { return sourceIdentityHint; }
    public int externalCandidateRecords() { return externalCandidateRecords; }
    public long externalCandidateSites() { return externalCandidateSites; }
    public String typeName(int row) {
        int at = Objects.checkIndex(row, rowCount());
        return new String(nameBytes, namesOffsets[at],
                namesOffsets[at + 1] - namesOffsets[at], StandardCharsets.UTF_8);
    }
    public int hashedFeatures(int row) {
        int r = Objects.checkIndex(row, rowCount());
        return tokenOffsets[r + 1] - tokenOffsets[r];
    }
    public long featureHash(int row, int ordinal) { return featureHashes[featureIndex(row, ordinal)]; }
    public Feature featureFamily(int row, int ordinal) {
        return FEATURES[featureKinds[featureIndex(row, ordinal)]];
    }
    private int featureIndex(int row, int ordinal) {
        int r = Objects.checkIndex(row, rowCount());
        return tokenOffsets[r] + Objects.checkIndex(ordinal, hashedFeatures(r));
    }

    private record EdgeTarget(String name, byte kind) { }

    private static EdgeTarget edgeTarget(String call) {
        int a = call.indexOf(':'), b = call.indexOf(':', a + 1);
        if (a < 0 || b < 0 || b + 1 >= call.length()) {
            throw new IllegalArgumentException("malformed attributed edge");
        }
        String shape = call.substring(0, a);
        if (!shape.equals("call") && !shape.equals("new") && !shape.equals("reference")) {
            throw new IllegalArgumentException("unknown call-expression shape");
        }
        byte kind = switch (call.substring(a + 1, b)) {
            case "DIRECT" -> M3SignalRelations.DIRECT;
            case "DISPATCH_CANDIDATE" -> M3SignalRelations.DISPATCH_CANDIDATE;
            default -> throw new IllegalArgumentException("unknown call-site classification");
        };
        int marker = call.indexOf('#', b + 1);
        if (marker <= b + 1 || marker == call.length() - 1) {
            throw new IllegalArgumentException("missing attributed target type");
        }
        return new EdgeTarget(call.substring(b + 1, marker), kind);
    }

    private static int familyOrdinal(String name) {
        try { return Feature.valueOf(name).ordinal(); }
        catch (IllegalArgumentException bad) { throw new IllegalArgumentException("unknown token family", bad); }
    }
    private static int nonnegative(String text) {
        try {
            int value = Integer.parseInt(text);
            if (value >= 0) return value;
        } catch (NumberFormatException bad) {
            throw new IllegalArgumentException("invalid scalar", bad);
        }
        throw new IllegalArgumentException("negative compiler scalar");
    }
    private static int positive(String text) {
        int value = nonnegative(text);
        if (value == 0) throw new IllegalArgumentException("zero edge multiplicity");
        return value;
    }
    private static String unescape(String text) {
        StringBuilder output = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '\\') { output.append(c); continue; }
            if (++i == text.length()) throw new IllegalArgumentException("trailing source escape");
            switch (text.charAt(i)) {
                case '\\' -> output.append('\\');
                case 't' -> output.append('\t');
                case 'n' -> output.append('\n');
                case 'r' -> output.append('\r');
                default -> throw new IllegalArgumentException("unknown source escape");
            }
        }
        return output.toString();
    }
    private static String sha256(String content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    /** One invocation-local dynamic primitive lane; never retained as a boxed list. */
    private static final class IntLane {
        private int[] values = new int[16];
        private int size;
        void add(int value) { if (size == values.length) values = Arrays.copyOf(values, size * 2); values[size++] = value; }
        int get(int index) { return values[Objects.checkIndex(index, size)]; }
        int size() { return size; }
        int[] toArray() { return Arrays.copyOf(values, size); }
    }
    private static final class LongLane {
        private long[] values = new long[16];
        private int size;
        void add(long value) { if (size == values.length) values = Arrays.copyOf(values, size * 2); values[size++] = value; }
        int size() { return size; }
        long[] toArray() { return Arrays.copyOf(values, size); }
    }
    private static final class ByteLane {
        private byte[] values = new byte[16];
        private int size;
        void add(byte value) { if (size == values.length) values = Arrays.copyOf(values, size * 2); values[size++] = value; }
        byte get(int index) { return values[Objects.checkIndex(index, size)]; }
        byte[] toArray() { return Arrays.copyOf(values, size); }
    }
}
