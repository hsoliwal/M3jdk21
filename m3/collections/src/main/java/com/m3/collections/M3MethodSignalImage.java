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
 * Verified primitive receiving image for the existing javac methodOut sidecar.
 * The compiler manufactures source-syntax counts and attributed call candidates;
 * neither this index nor those observations certify runtime call frequency,
 * virtual dispatch targets, semantic equivalence, purity or Java object identity.
 *
 * <p>Transient parser strings do not survive publication. The retained method
 * identities are one UTF-8 byte lane with offsets; facts use the existing typed
 * column owner and relationships use its existing primitive CSR graph owner.
 */
public final class M3MethodSignalImage {
    private static final String HEADER = "# M3JDK21-METHOD-SIGNALS/1\n"
            + "# M=exact source AST counts; C=attributed method-call candidates, not runtime observations\n"
            + "# method identity is the attributed owner/signature, not a lexical hash\n";
    private static final String TRAILER = "# sha256\t";
    private static final int SOURCE_COUNTS = 14;
    private static final int MAX_BYTES = 64 * 1024 * 1024;
    private static final int MAX_METHODS = 1_000_000;
    private static final int MAX_CALL_SHAPES = 2_000_000;

    /** Exact local syntax counts followed by known-in-image relationship counts. */
    public enum Scalar {
        INVOCATIONS, NEW_OBJECTS, NEW_ARRAYS, MEMBER_REFERENCES,
        DIRECT_CANDIDATES, DISPATCH_CANDIDATES, UNRESOLVED_CANDIDATES,
        LOOPS, BRANCHES, TRY_BLOCKS, CATCHES, LAMBDAS,
        FIELD_REFERENCES, FIELD_WRITE_SYNTAX,
        INTERNAL_INCOMING_SITES, INTERNAL_OUTGOING_SITES
    }

    private final M3SignalColumns columns;
    private final M3SignalRelations relations;
    private final byte[] names;
    private final int[] nameOffsets;
    private final String sourceSha256;
    private final long sourceIdentityHint;
    private final int externalCandidateRecords;
    private final long externalCandidateSites;
    private final long unresolvedCandidateSites;

    private M3MethodSignalImage(M3SignalColumns columns, M3SignalRelations relations,
            byte[] names, int[] offsets, String sourceSha256, long identityHint,
            int externalCandidateRecords, long externalCandidateSites,
            long unresolvedCandidateSites) {
        this.columns = columns;
        this.relations = relations;
        this.names = names;
        this.nameOffsets = offsets;
        this.sourceSha256 = sourceSha256;
        this.sourceIdentityHint = identityHint;
        this.externalCandidateRecords = externalCandidateRecords;
        this.externalCandidateSites = externalCandidateSites;
        this.unresolvedCandidateSites = unresolvedCandidateSites;
    }

    /**
     * Fail closed on altered hashes, wrong schema, noncanonical escaping, order
     * drift, impossible counts, corrupt coordinates or an oversized image.
     */
    public static M3MethodSignalImage parse(String text) {
        Objects.requireNonNull(text, "text");
        if (text.length() > MAX_BYTES || !text.startsWith(HEADER) || !text.endsWith("\n")
                || text.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new IllegalArgumentException("unsupported or oversized method signal image");
        }
        int at = text.lastIndexOf(TRAILER);
        if (at < HEADER.length() || at == 0 || text.charAt(at - 1) != '\n'
                || text.length() != at + TRAILER.length() + 65) {
            throw new IllegalArgumentException("invalid method signal trailer");
        }
        String digest = text.substring(at + TRAILER.length(), text.length() - 1);
        String payload = text.substring(0, at);
        if (!digest.matches("[0-9a-f]{64}") || !digest.equals(sha256(payload))) {
            throw new IllegalArgumentException("method signal SHA-256 mismatch");
        }

        List<String> identities = new ArrayList<>();
        int[][] facts = new int[SOURCE_COUNTS][16];
        IntLane sources = new IntLane();
        IntLane occurrences = new IntLane();
        ByteLane kinds = new ByteLane();
        List<String> callees = new ArrayList<>();
        String lastCall = null;
        long direct = 0, dispatch = 0, unresolved = 0;
        int current = -1;
        for (int start = HEADER.length(); start < payload.length();) {
            int end = payload.indexOf('\n', start);
            if (end < 0) throw new IllegalArgumentException("unterminated method record");
            String line = payload.substring(start, end);
            start = end + 1;
            if (line.isEmpty()) throw new IllegalArgumentException("empty method record");
            String[] fields = line.split("\t", -1);
            if (fields[0].equals("M")) {
                if (fields.length != SOURCE_COUNTS + 2) {
                    throw new IllegalArgumentException("method scalar geometry");
                }
                if (current >= 0) checkCallTotals(facts, current, direct, dispatch);
                String symbol = unescape(fields[1]);
                if (symbol.isBlank() || symbol.indexOf('#') <= 0
                        || (!identities.isEmpty()
                        && symbol.compareTo(identities.get(identities.size() - 1)) <= 0)) {
                    throw new IllegalArgumentException("unsorted/invalid method identity");
                }
                if (identities.size() >= MAX_METHODS) {
                    throw new IllegalArgumentException("method budget exceeded");
                }
                current = identities.size();
                identities.add(symbol);
                if (current == facts[0].length) {
                    for (int d = 0; d < SOURCE_COUNTS; d++) {
                        facts[d] = Arrays.copyOf(facts[d], facts[d].length * 2);
                    }
                }
                for (int d = 0; d < SOURCE_COUNTS; d++) {
                    facts[d][current] = nonnegative(fields[d + 2]);
                }
                direct = 0;
                dispatch = 0;
                unresolved = Math.addExact(unresolved,
                        facts[Scalar.UNRESOLVED_CANDIDATES.ordinal()][current]);
                lastCall = null;
            } else if (fields[0].equals("C")) {
                if (fields.length != 4 || current < 0
                        || !identities.get(current).equals(unescape(fields[1]))) {
                    throw new IllegalArgumentException("call record without its method owner");
                }
                String label = unescape(fields[2]);
                if (lastCall != null && label.compareTo(lastCall) <= 0) {
                    throw new IllegalArgumentException("unsorted/duplicate method call shape");
                }
                CallTarget target = target(label);
                int count = positive(fields[3]);
                if (sources.size() >= MAX_CALL_SHAPES) {
                    throw new IllegalArgumentException("call-shape budget exceeded");
                }
                sources.add(current);
                callees.add(target.symbol());
                kinds.add(target.kind());
                occurrences.add(count);
                if (target.kind() == M3SignalRelations.DIRECT) {
                    direct = Math.addExact(direct, count);
                } else {
                    dispatch = Math.addExact(dispatch, count);
                }
                lastCall = label;
            } else {
                throw new IllegalArgumentException("unknown method signal record");
            }
        }
        if (current >= 0) checkCallTotals(facts, current, direct, dispatch);

        IntLane internalSources = new IntLane();
        IntLane internalTargets = new IntLane();
        IntLane internalSites = new IntLane();
        ByteLane internalKinds = new ByteLane();
        int externalRecords = 0;
        long externalSites = 0;
        for (int edge = 0; edge < sources.size(); edge++) {
            int target = Collections.binarySearch(identities, callees.get(edge));
            if (target < 0) {
                externalRecords++;
                externalSites = Math.addExact(externalSites, occurrences.get(edge));
            } else {
                internalSources.add(sources.get(edge));
                internalTargets.add(target);
                internalKinds.add(kinds.get(edge));
                internalSites.add(occurrences.get(edge));
            }
        }
        int rows = identities.size();
        M3SignalRelations graph = M3SignalRelations.of(rows, internalSources.toArray(),
                internalTargets.toArray(), internalKinds.toArray(), internalSites.toArray());
        long hint = ByteBuffer.wrap(HexFormat.of().parseHex(digest), 0, 8).getLong();
        long[] valid = M3SignalColumns.allValid(rows);
        M3SignalColumns.Column[] typed = new M3SignalColumns.Column[Scalar.values().length];
        for (int d = 0; d < SOURCE_COUNTS; d++) {
            typed[d] = new M3SignalColumns.Column(M3SignalColumns.Kind.INTEGER,
                    M3SignalColumns.Origin.STATIC, hint ^ d, false,
                    valid, Arrays.copyOf(facts[d], rows), null, null);
        }
        long[] incoming = new long[rows], outgoing = new long[rows];
        for (int row = 0; row < rows; row++) {
            incoming[row] = graph.incomingSites(row);
            outgoing[row] = graph.outgoingSites(row);
        }
        typed[SOURCE_COUNTS] = new M3SignalColumns.Column(M3SignalColumns.Kind.LONG,
                M3SignalColumns.Origin.STATIC, hint ^ SOURCE_COUNTS, false,
                valid, null, incoming, null);
        typed[SOURCE_COUNTS + 1] = new M3SignalColumns.Column(M3SignalColumns.Kind.LONG,
                M3SignalColumns.Origin.STATIC, hint ^ (SOURCE_COUNTS + 1), false,
                valid, null, outgoing, null);
        int[] offsets = new int[rows + 1];
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int row = 0; row < rows; row++) {
            offsets[row] = bytes.size();
            bytes.writeBytes(identities.get(row).getBytes(StandardCharsets.UTF_8));
        }
        offsets[rows] = bytes.size();
        return new M3MethodSignalImage(new M3SignalColumns(rows, typed), graph,
                bytes.toByteArray(), offsets, digest, hint, externalRecords, externalSites,
                unresolved);
    }

    public int rowCount() { return nameOffsets.length - 1; }
    public M3SignalColumns columns() { return columns; }
    public M3SignalRelations relationships() { return relations; }
    public String sourceSha256() { return sourceSha256; }
    /** Index hint only: collisions must be checked against sourceSha256(). */
    public long sourceIdentityHint() { return sourceIdentityHint; }
    public int externalCandidateRecords() { return externalCandidateRecords; }
    public long externalCandidateSites() { return externalCandidateSites; }
    public long unresolvedCandidateSites() { return unresolvedCandidateSites; }

    public String methodName(int row) {
        int checked = Objects.checkIndex(row, rowCount());
        return new String(names, nameOffsets[checked],
                nameOffsets[checked + 1] - nameOffsets[checked], StandardCharsets.UTF_8);
    }

    /** Local attributed call-expression sites only; excludes missing/runtime edges. */
    public long attributedSites(int caller, int callee, boolean includeDispatchCandidates) {
        Objects.checkIndex(caller, rowCount());
        Objects.checkIndex(callee, rowCount());
        long total = 0;
        for (int edge = 0; edge < relations.outgoingEdges(caller); edge++) {
            if (relations.target(caller, edge) == callee
                    && (includeDispatchCandidates
                    || relations.kind(caller, edge) == M3SignalRelations.DIRECT)) {
                total = Math.addExact(total, relations.callSites(caller, edge));
            }
        }
        return total;
    }

    /** Bounded reachability in the known compiler image, never whole-program closure. */
    public int reachableMethods(int root, int hops, boolean includeDispatchCandidates) {
        return relations.reachableTypes(root, hops, includeDispatchCandidates);
    }

    private static void checkCallTotals(int[][] facts, int row, long direct, long dispatch) {
        if (direct != facts[Scalar.DIRECT_CANDIDATES.ordinal()][row]
                || dispatch != facts[Scalar.DISPATCH_CANDIDATES.ordinal()][row]) {
            throw new IllegalArgumentException("method call candidates disagree with source counts");
        }
    }

    private record CallTarget(String symbol, byte kind) { }

    private static CallTarget target(String label) {
        int a = label.indexOf(':'), b = label.indexOf(':', a + 1);
        if (a <= 0 || b <= a + 1 || b == label.length() - 1) {
            throw new IllegalArgumentException("malformed method call shape");
        }
        String shape = label.substring(0, a);
        if (!shape.equals("call") && !shape.equals("new") && !shape.equals("reference")) {
            throw new IllegalArgumentException("unknown method call shape");
        }
        byte kind = switch (label.substring(a + 1, b)) {
            case "DIRECT" -> M3SignalRelations.DIRECT;
            case "DISPATCH_CANDIDATE" -> M3SignalRelations.DISPATCH_CANDIDATE;
            default -> throw new IllegalArgumentException("unrecognized dispatch evidence");
        };
        String symbol = label.substring(b + 1);
        if (symbol.indexOf('#') <= 0) throw new IllegalArgumentException("missing callee signature");
        return new CallTarget(symbol, kind);
    }

    private static int nonnegative(String field) {
        try {
            int count = Integer.parseInt(field);
            if (count >= 0 && field.equals(Integer.toString(count))) return count;
        } catch (NumberFormatException bad) {
            throw new IllegalArgumentException("invalid method count", bad);
        }
        throw new IllegalArgumentException("noncanonical/negative method count");
    }

    private static int positive(String field) {
        int count = nonnegative(field);
        if (count == 0) throw new IllegalArgumentException("zero call occurrences");
        return count;
    }

    private static String unescape(String text) {
        StringBuilder decoded = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '\\') { decoded.append(c); continue; }
            if (++i == text.length()) throw new IllegalArgumentException("trailing escape");
            switch (text.charAt(i)) {
                case '\\' -> decoded.append('\\');
                case 't' -> decoded.append('\t');
                case 'n' -> decoded.append('\n');
                case 'r' -> decoded.append('\r');
                default -> throw new IllegalArgumentException("unknown escape sequence");
            }
        }
        String result = decoded.toString();
        if (!escape(result).equals(text)) {
            throw new IllegalArgumentException("noncanonical source escaping");
        }
        return result;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\t", "\\t")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String sha256(String payload) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    private static final class IntLane {
        private int[] values = new int[16];
        private int size;
        int size() { return size; }
        int get(int index) { return values[Objects.checkIndex(index, size)]; }
        void add(int value) {
            if (size == values.length) values = Arrays.copyOf(values, size * 2);
            values[size++] = value;
        }
        int[] toArray() { return Arrays.copyOf(values, size); }
    }

    private static final class ByteLane {
        private byte[] values = new byte[16];
        private int size;
        byte get(int index) { return values[Objects.checkIndex(index, size)]; }
        void add(byte value) {
            if (size == values.length) values = Arrays.copyOf(values, size * 2);
            values[size++] = value;
        }
        byte[] toArray() { return Arrays.copyOf(values, size); }
    }
}
