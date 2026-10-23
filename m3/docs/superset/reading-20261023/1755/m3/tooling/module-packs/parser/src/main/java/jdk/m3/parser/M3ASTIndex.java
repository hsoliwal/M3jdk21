// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser;

import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import javax.lang.model.element.Modifier;
=======
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507

/**
 * One source-owned, primitive-coordinate *index* of a parser's existing AST.
 *
 * <p>It is not a replacement parser, semantic tree, source owner or mutable AST.
 * The external parser remains authoritative. This index retains only kind IDs,
 * source geometry, occurrence-parent coordinates and sibling chains. Optional
 * detail lanes retain detached names and canonical scalar literal Strings; no
 * tree, symbol, binding, Name, Map or compiler context is retained.
 * Nodes are occurrence identities within this immutable source generation.
 */
public final class M3ASTIndex {
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
    public enum Domain { ECLIPSE_JDT, MAVEN_POM, GRADLE_GROOVY, GRADLE_KOTLIN, JACKSON_JSON, JAVAC, OPENREWRITE }
    public enum LiteralKind { NONE, NULL, BOOLEAN, CHAR, STRING, INT, LONG, FLOAT, DOUBLE }
    private static final LiteralKind[] LITERAL_KINDS = LiteralKind.values();
    public static final String DETAILS_SCHEMA = "M3_AST_DETAILS_V1";
    public static final String SOURCE_RANGE_HASH_SCHEMA = "M3_SOURCE_RANGE_UTF16BE_SHA256_V1";
    public static final String STRUCTURAL_HASH_SCHEMA = "M3_AST_STRUCTURE_SHA256_V1";
    /** Stable V1 modifier encoding, separate from the original four index flags. */
    public static final long PUBLIC = 1L, PROTECTED = 1L << 1, PRIVATE = 1L << 2;
    public static final long ABSTRACT = 1L << 3, DEFAULT = 1L << 4, STATIC = 1L << 5;
    public static final long FINAL = 1L << 6, TRANSIENT = 1L << 7, VOLATILE = 1L << 8;
    public static final long SYNCHRONIZED = 1L << 9, NATIVE = 1L << 10, STRICTFP = 1L << 11;
    public static final long SEALED = 1L << 12, NON_SEALED = 1L << 13, INTERFACE = 1L << 14;
    private static final long ALL_MODIFIERS = (1L << 15) - 1;
=======
    public enum Domain { ECLIPSE_JDT, MAVEN_POM, GRADLE_GROOVY, GRADLE_KOTLIN, JACKSON_JSON, JAVAC }
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
    public static final int NO_RANGE = -1;
    /** The origin parser guarantees an exact half-open UTF-16 source range. */
    public static final int EXACT_RANGE = 1;
    /** Syntax exists, but semantic binding/effective build model is not certified. */
    public static final int SYNTAX_ONLY = 2;
    /** The external source owner could not be verified against the original AST. */
    public static final int SOURCE_UNVERIFIED = 4;
    public static final int RECOVERED = 8;
    /** Resolved from original javac declaration modifiers only; never inferred from spelling. */
    public static final int STATIC = 16, FINAL = 32, INTERFACE = 64;
    private final M3File source;
    private final Domain domain;
    private final String[] names;
    private final int[] kinds, parents, starts, ends, flags, first, next, childCounts;
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
    private final int[] startLines, endLines, enclosingTypes, enclosingExecutables;
    private final byte[] literalKinds;
    private final long[] modifiers;
    private final String[] nodeNames, literalValues;
=======
    /** Optional signed 1-based name/literal IDs. Zero means no metadata for this row. */
    private final int[] semantic;
    private final String[] semanticValues;
    /** Built only for consumers requesting source lines; no per-node line objects. */
    private volatile int[] lineStarts;
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507

    private M3ASTIndex(Builder builder) {
        source = builder.source;
        domain = builder.domain;
        names = Arrays.copyOf(builder.names, builder.nameCount);
        kinds = Arrays.copyOf(builder.kinds, builder.size);
        parents = Arrays.copyOf(builder.parents, builder.size);
        starts = Arrays.copyOf(builder.starts, builder.size);
        ends = Arrays.copyOf(builder.ends, builder.size);
        flags = Arrays.copyOf(builder.flags, builder.size);
        first = Arrays.copyOf(builder.first, builder.size);
        next = Arrays.copyOf(builder.next, builder.size);
        childCounts = Arrays.copyOf(builder.childCounts, builder.size);
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
        if (builder.literalKinds == null) {
            startLines = endLines = enclosingTypes = enclosingExecutables = null;
            literalKinds = null;
            modifiers = null;
            nodeNames = literalValues = null;
        } else {
            startLines = Arrays.copyOf(builder.startLines, builder.size);
            endLines = Arrays.copyOf(builder.endLines, builder.size);
            enclosingTypes = Arrays.copyOf(builder.enclosingTypes, builder.size);
            enclosingExecutables = Arrays.copyOf(builder.enclosingExecutables, builder.size);
            literalKinds = Arrays.copyOf(builder.literalKinds, builder.size);
            modifiers = Arrays.copyOf(builder.modifiers, builder.size);
            nodeNames = Arrays.copyOf(builder.nodeNames, builder.size);
            literalValues = Arrays.copyOf(builder.literalValues, builder.size);
        }
=======
        semantic = builder.semantic == null ? new int[0] : Arrays.copyOf(builder.semantic, builder.size);
        semanticValues = builder.semanticValues == null ? new String[0]
                : Arrays.copyOf(builder.semanticValues, builder.semanticCount);
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
    }

    public static Builder builder(M3File source, Domain domain, int maxNodes) {
        return new Builder(source, domain, maxNodes);
    }
    public M3File source() { return source; }
    public Domain domain() { return domain; }
    public int nodeCount() { return kinds.length; }
    public String kind(int node) { return names[kinds[check(node)]]; }
    public int parent(int node) { return parents[check(node)]; }
    public int flags(int node) { return flags[check(node)]; }
    public int start(int node) { return starts[check(node)]; }
    public int end(int node) { return ends[check(node)]; }
    public int firstChild(int node) { return first[check(node)]; }
    public int nextSibling(int node) { return next[check(node)]; }
    public int childCount(int node) { return childCounts[check(node)]; }
    public boolean hasExactRange(int node) {
        check(node);
        return starts[node] >= 0 && ends[node] >= 0 && (flags[node] & EXACT_RANGE) != 0;
    }
    /** Materialization is explicit, never implicit during indexing or navigation. */
    public String sourceText(int node) {
        if (!hasExactRange(node)) throw new IllegalStateException("unverified/unknown source range");
        return source.subSequence(starts[node], ends[node]);
    }
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
    public long primitiveBytes() { return (long) kinds.length * 8L * Integer.BYTES; }
    /** Optional primitive lanes only; excludes references, Strings and object headers. */
    public long metadataBytes() { return literalKinds == null ? 0L : nodeCount() * 25L; }
    public boolean hasDetails(int node) { check(node); return literalKinds != null && literalKinds[node] >= 0; }
    public int startLine(int node) { return hasDetails(node) ? startLines[node] : -1; }
    public int line(int node) { return startLine(node); }
    /** Line of the exclusive end offset, possibly the start of the next line. */
    public int endLine(int node) { return hasDetails(node) ? endLines[node] : -1; }
    public Optional<String> name(int node) { return hasDetails(node) ? Optional.ofNullable(nodeNames[node]) : Optional.empty(); }
    public LiteralKind literalKind(int node) {
        return hasDetails(node) ? LITERAL_KINDS[literalKinds[node]] : LiteralKind.NONE;
    }
    /** NONE and NULL have no value; CHAR is one UTF-16 unit, IEEE values are unsigned raw bits. */
    public Optional<String> literalValue(int node) {
        return hasDetails(node) ? Optional.ofNullable(literalValues[node]) : Optional.empty();
    }
    public long modifierBits(int node) { return hasDetails(node) ? modifiers[node] : 0L; }
    public int enclosingType(int node) { return hasDetails(node) ? enclosingTypes[node] : -1; }
    public int enclosingExecutable(int node) { return hasDetails(node) ? enclosingExecutables[node] : -1; }

    /** Closest strict ancestor with the exact provider kind name, or -1. */
    public int firstEnclosing(int node, String kind) {
        Objects.requireNonNull(kind, "kind");
        for (int row = parents[check(node)]; row >= 0; row = parents[row]) {
            if (names[kinds[row]].equals(kind)) return row;
=======
    public long primitiveBytes() {
        int[] lines = lineStarts;
        return (long) kinds.length * 8L * Integer.BYTES
                + (long) semantic.length * Integer.BYTES
                + (lines == null ? 0L : (long) lines.length * Integer.BYTES);
    }

    /** Optional original compiler identifier, interned in this source's dictionary. */
    public String simpleName(int node) {
        int row = check(node);
        int id = semantic.length == 0 ? 0 : semantic[row];
        return id > 0 ? semanticValues[id - 1] : null;
    }

    /** Optional original javac literal value, not a reconstructed source spelling. */
    public String literalValue(int node) {
        int row = check(node);
        int id = semantic.length == 0 ? 0 : semantic[row];
        return id < 0 ? semanticValues[-id - 1] : null;
    }

    /** Resolve the nearest enclosing node of an original kind without another AST walk. */
    public int firstEnclosing(int node, String kind) {
        Objects.requireNonNull(kind, "kind");
        for (int parent = parents[check(node)]; parent >= 0; parent = parents[parent]) {
            if (kind.equals(kind(parent))) return parent;
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
        }
        return -1;
    }

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
    /**
     * Covered source lines: unknown is -1, empty is 0, otherwise count through
     * end-1. An exclusive end at the next line's start does not cover that line.
     * CRLF is one line break; the boundary between CR and LF is on the old line.
     */
    public int spanLines(int node) {
        check(node);
        if (!hasDetails(node) || startLines[node] < 0 || starts[node] < 0) return -1;
        if (starts[node] == ends[node]) return 0;
        int lastLine = endLines[node];
        char last = source.charAt(ends[node] - 1);
        if (last == '\n' || (last == '\r'
                && (ends[node] == source.length() || source.charAt(ends[node]) != '\n'))) lastLine--;
        return lastLine - startLines[node] + 1;
    }

    /** Explicit bounded exact UTF-16BE range hash; source-verification flags are not changed. */
    public String sourceRangeSha256(int node, int maxUtf16Units) {
        if (maxUtf16Units < 0) throw new IllegalArgumentException("negative source hash budget");
        if (!hasExactRange(node)) throw new IllegalStateException("unknown/unavailable source range");
        if (ends[node] - starts[node] > maxUtf16Units) throw new IllegalArgumentException("source hash unit budget exceeded");
        MessageDigest digest = sha256();
        for (int i = starts[node]; i < ends[node]; i++) putChar(digest, source.charAt(i));
=======
    /** One-based Java source line of an exact UTF-16 start offset, or -1 if unknown. */
    public int line(int node) {
        int row = check(node);
        if (!hasExactRange(row)) return -1;
        int[] lines = lines();
        int found = Arrays.binarySearch(lines, starts[row]);
        return found >= 0 ? found + 1 : -found - 1;
    }

    /** Pack one-based inclusive start/end line IDs into a single long, without allocating. */
    public long spanLines(int node) {
        int row = check(node);
        if (!hasExactRange(row)) return -1L;
        int startLine = line(row);
        int endOffset = Math.max(starts[row], ends[row] - 1);
        int[] lines = lines();
        int found = Arrays.binarySearch(lines, endOffset);
        int endLine = found >= 0 ? found + 1 : -found - 1;
        return ((long) startLine << Integer.SIZE) | Integer.toUnsignedLong(endLine);
    }

    private int[] lines() {
        int[] lines = lineStarts;
        if (lines != null) return lines;
        synchronized (this) {
            lines = lineStarts;
            if (lines != null) return lines;
            int[] starts = new int[16];
            starts[0] = 0;
            int size = 1;
            for (int i = 0; i < source.length(); i++) {
                char value = source.charAt(i);
                if (value == '\r' && i + 1 < source.length() && source.charAt(i + 1) == '\n') i++;
                else if (value != '\r' && value != '\n') continue;
                if (size == starts.length) starts = Arrays.copyOf(starts, starts.length * 2);
                starts[size++] = i + 1;
            }
            lineStarts = lines = Arrays.copyOf(starts, size);
            return lines;
        }
    }

    /** Exact source UTF-16BE SHA-256. Computed only on request; no substring materialized. */
    public String exactSha256(int node) {
        int row = check(node);
        if (!hasExactRange(row)) throw new IllegalStateException("unverified AST source range");
        MessageDigest digest = sha256();
        byte[] scratch = new byte[4096];
        int used = 0;
        for (int i = starts[row]; i < ends[row]; i++) {
            char value = source.charAt(i);
            if (used == scratch.length) {
                digest.update(scratch);
                used = 0;
            }
            scratch[used++] = (byte) (value >>> 8);
            scratch[used++] = (byte) value;
        }
        digest.update(scratch, 0, used);
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
        return HexFormat.of().formatHex(digest.digest());
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * One bounded bottom-up pass over this detached occurrence index. The text
     * budget counts each row's kind, optional name and literal-value UTF-16 units.
     * Budgets are checked before allocating the 32-byte-per-row result. No source
     * span is hashed, no tree is retained, and no result is cached in the index.
     *
     * <p>V1 frames schema, provider domain, kind, SYNTAX_ONLY/RECOVERED flags,
     * detail presence, name, literal kind/value, modifiers and ordered child
     * digests. Source geometry, URI, enclosing coordinates and source-verification
     * flags are excluded. This is contextual structure evidence, not a proof of
     * semantic equivalence or compatibility with another owner's atom format.
     */
    public StructuralHashes structuralHashes(int maxNodes, long maxTextUnits) {
        if (maxNodes < 0 || maxTextUnits < 0) throw new IllegalArgumentException("negative structural hash budget");
        if (nodeCount() > maxNodes) throw new IllegalArgumentException("structural hash node budget exceeded");
        long units = 0;
        for (int row = 0; row < nodeCount(); row++) {
            units += names[kinds[row]].length();
            if (hasDetails(row)) {
                if (nodeNames[row] != null) units += nodeNames[row].length();
                if (literalValues[row] != null) units += literalValues[row].length();
            }
            if (units > maxTextUnits) throw new IllegalArgumentException("structural hash text budget exceeded");
        }
        byte[] result = new byte[Math.multiplyExact(nodeCount(), 32)];
        MessageDigest digest = sha256();
        for (int row = nodeCount() - 1; row >= 0; row--) {
            digest.reset();
            putText(digest, STRUCTURAL_HASH_SCHEMA);
            putText(digest, domain.name());
            putText(digest, names[kinds[row]]);
            putInt(digest, flags[row] & (SYNTAX_ONLY | RECOVERED));
            digest.update((byte) (hasDetails(row) ? 1 : 0));
            if (hasDetails(row)) {
                putText(digest, nodeNames[row]);
                putText(digest, LITERAL_KINDS[literalKinds[row]].name());
                putText(digest, literalValues[row]);
                putLong(digest, modifiers[row]);
            }
            putInt(digest, childCounts[row]);
            for (int child = first[row]; child >= 0; child = next[child]) digest.update(result, child * 32, 32);
            try { digest.digest(result, row * 32, 32); }
            catch (DigestException impossible) { throw new AssertionError(impossible); }
        }
        return new StructuralHashes(result, units);
    }

    public static final class StructuralHashes {
        private final byte[] values;
        private final long textUnits;
        private StructuralHashes(byte[] values, long textUnits) {
            this.values = values;
            this.textUnits = textUnits;
        }
        /** Internal count observed by the existing structural-hash admission. */
        long textUnits() { return textUnits; }
        /** Borrow the existing SHA bytes for primitive candidate-bucket lookup. */
        long prefix64(int node) {
            int start = Objects.checkIndex(node, nodeCount()) * 32;
            long result = 0;
            for (int i = 0; i < 8; i++) result = (result << 8) | (values[start + i] & 255L);
            return result;
        }
        boolean sameHash(int left, int right) {
            int a = Objects.checkIndex(left, nodeCount()) * 32;
            int b = Objects.checkIndex(right, nodeCount()) * 32;
            return Arrays.mismatch(values, a, a + 32, values, b, b + 32) < 0;
        }
        public String schema() { return STRUCTURAL_HASH_SCHEMA; }
        public int nodeCount() { return values.length / 32; }
        public String sha256(int node) {
            int start = Objects.checkIndex(node, nodeCount()) * 32;
            return HexFormat.of().formatHex(values, start, start + 32);
        }
    }

    public static long modifierBit(Modifier modifier) {
        return switch (Objects.requireNonNull(modifier, "modifier")) {
            case PUBLIC -> PUBLIC;
            case PROTECTED -> PROTECTED;
            case PRIVATE -> PRIVATE;
            case ABSTRACT -> ABSTRACT;
            case DEFAULT -> DEFAULT;
            case STATIC -> STATIC;
            case FINAL -> FINAL;
            case TRANSIENT -> TRANSIENT;
            case VOLATILE -> VOLATILE;
            case SYNCHRONIZED -> SYNCHRONIZED;
            case NATIVE -> NATIVE;
            case STRICTFP -> STRICTFP;
            case SEALED -> SEALED;
            case NON_SEALED -> NON_SEALED;
        };
=======
     * Order-sensitive SHA-256 of kind, modifier flags and child arity only.
     * Names, literal spelling and source coordinates are deliberately masked.
     * This is a discovery fingerprint, not a proof of semantic equivalence.
     */
    public String structuralSha256(int node) {
        int root = check(node);
        MessageDigest digest = sha256();
        updateText(digest, "M3AST-STRUCTURE-1");
        int[] pending = new int[32];
        int size = 1;
        pending[0] = root;
        while (size > 0) {
            int current = pending[--size];
            updateText(digest, kind(current));
            updateWord(digest, flags[current] & (STATIC | FINAL | INTERFACE));
            updateWord(digest, childCounts[current]);
            int firstPending = size;
            for (int child = first[current]; child >= 0; child = next[child]) {
                if (size == pending.length) pending = Arrays.copyOf(pending, pending.length * 2);
                pending[size++] = child;
            }
            for (int a = firstPending, b = size - 1; a < b; a++, b--) {
                int swap = pending[a]; pending[a] = pending[b]; pending[b] = swap;
            }
        }
        return HexFormat.of().formatHex(digest.digest());
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
    }

    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static void putChar(MessageDigest digest, char value) {
        digest.update((byte) (value >>> 8)); digest.update((byte) value);
    }
    private static void putInt(MessageDigest digest, int value) {
        digest.update((byte) (value >>> 24)); digest.update((byte) (value >>> 16));
        digest.update((byte) (value >>> 8)); digest.update((byte) value);
    }
    private static void putLong(MessageDigest digest, long value) {
        putInt(digest, (int) (value >>> 32)); putInt(digest, (int) value);
    }
    private static void putText(MessageDigest digest, String value) {
        putInt(digest, value == null ? -1 : value.length());
        if (value != null) for (int i = 0; i < value.length(); i++) putChar(digest, value.charAt(i));
    }
    /** Internal primitive queries reuse the original kind vocabulary. */
    int kindOrdinal(int node) { return kinds[check(node)]; }
    int kindVocabularySize() { return names.length; }
    String kindVocabularyName(int ordinal) {
        return names[Objects.checkIndex(ordinal, names.length)];
    }

    /** Compare exactly the retained structural atom lanes, not semantic bindings. */
    boolean sameStructuralAtom(int firstNode, int secondNode) {
        int a = check(firstNode), b = check(secondNode);
        if (kinds[a] != kinds[b] || childCounts[a] != childCounts[b]
                || (flags[a] & (SYNTAX_ONLY | RECOVERED))
                   != (flags[b] & (SYNTAX_ONLY | RECOVERED))) return false;
        boolean aDetails = literalKinds != null && literalKinds[a] >= 0;
        boolean bDetails = literalKinds != null && literalKinds[b] >= 0;
        if (aDetails != bDetails) return false;
        return !aDetails || (Objects.equals(nodeNames[a], nodeNames[b])
                && literalKinds[a] == literalKinds[b]
                && Objects.equals(literalValues[a], literalValues[b])
                && modifiers[a] == modifiers[b]);
=======
        catch (NoSuchAlgorithmException failure) { throw new IllegalStateException(failure); }
    }

    private static void updateWord(MessageDigest digest, int value) {
        digest.update((byte) (value >>> 24));
        digest.update((byte) (value >>> 16));
        digest.update((byte) (value >>> 8));
        digest.update((byte) value);
    }

    private static void updateText(MessageDigest digest, String value) {
        updateWord(digest, value.length());
        for (int i = 0; i < value.length(); i++) {
            char unit = value.charAt(i);
            digest.update((byte) (unit >>> 8));
            digest.update((byte) unit);
        }
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
    }

    private int check(int node) { return Objects.checkIndex(node, kinds.length); }

    /** Bounded, single-owner creation; discarded immediately after immutable publication. */
    public static final class Builder {
        private final M3File source;
        private final Domain domain;
        private final int maxNodes;
        private String[] names = new String[16];
        private int nameCount;
        private int[] kinds = new int[32], parents = new int[32], starts = new int[32];
        private int[] ends = new int[32], flags = new int[32], first = new int[32];
        private int[] last = new int[32], next = new int[32], childCounts = new int[32];
        private int[] startLines, endLines, enclosingTypes, enclosingExecutables;
        private byte[] literalKinds;
        private long[] modifiers;
        private String[] nodeNames, literalValues;
        private int size;
        private boolean sealed;
        private int[] semantic;
        private String[] semanticValues;
        private Map<String, Integer> semanticDictionary;
        private int semanticCount;

        private Builder(M3File source, Domain domain, int maxNodes) {
            this.source = Objects.requireNonNull(source, "source");
            this.domain = Objects.requireNonNull(domain, "domain");
            if (maxNodes < 1 || maxNodes > 20_000_000) {
                throw new IllegalArgumentException("AST node budget outside 1..20m");
            }
            this.maxNodes = maxNodes;
        }

        /**
         * Appends one occurrence with an already appended parent; first node is root.
         * Unknown source coordinates must use exactly (-1,-1), never guessed positions.
         */
        public int add(int parent, String kind, int start, int end, int flags) {
            if (sealed) throw new IllegalStateException("index already sealed");
            if (size == maxNodes) throw new IllegalArgumentException("AST index node budget exceeded");
            if (size == 0 ? parent != -1 : parent < 0 || parent >= size) {
                throw new IllegalArgumentException("bad AST parent or root coordinate");
            }
            if (!((start == NO_RANGE && end == NO_RANGE)
                    || (start >= 0 && end >= start && end <= source.length()))) {
                throw new IllegalArgumentException("invalid UTF-16 half-open AST range");
            }
            if ((flags & ~(EXACT_RANGE | SYNTAX_ONLY | SOURCE_UNVERIFIED | RECOVERED
                    | STATIC | FINAL | INTERFACE)) != 0) {
                throw new IllegalArgumentException("unknown AST index flags");
            }
            if ((flags & EXACT_RANGE) != 0 && start == NO_RANGE) {
                throw new IllegalArgumentException("exact range without source coordinates");
            }
            if (parent >= 0 && starts[parent] >= 0 && ends[parent] >= 0 && start >= 0
                    && (start < starts[parent] || end > ends[parent])) {
                throw new IllegalArgumentException("AST child escapes known parent range");
            }
            int name = nameId(Objects.requireNonNull(kind, "kind"));
            if (size == kinds.length) grow();
            int row = size++;
            kinds[row] = name;
            parents[row] = parent;
            starts[row] = start;
            ends[row] = end;
            this.flags[row] = flags;
            first[row] = last[row] = next[row] = -1;
            if (parent >= 0) {
                if (first[parent] == -1) first[parent] = row;
                else next[last[parent]] = row;
                last[parent] = row;
                childCounts[parent]++;
            }
            return row;
        }

        /** Attach original javac simple-name or literal metadata without retaining compiler nodes. */
        public void annotate(int node, String name, String literal) {
            if (sealed) throw new IllegalStateException("index already sealed");
            Objects.checkIndex(node, size);
            if ((name == null) == (literal == null)) {
                throw new IllegalArgumentException("exactly one identifier or literal required");
            }
            if (semantic == null) semantic = new int[kinds.length];
            if (semantic[node] != 0) throw new IllegalStateException("node already annotated");
            String value = name == null ? literal : name;
            if (value.length() > 1_048_576) throw new IllegalArgumentException("semantic literal too large");
            if (semanticDictionary == null) {
                semanticDictionary = new HashMap<>();
                semanticValues = new String[16];
            }
            Integer id = semanticDictionary.get(value);
            if (id == null) {
                if (semanticCount == 1_000_000) throw new IllegalArgumentException("semantic dictionary budget");
                id = semanticCount++;
                if (id == semanticValues.length)
                    semanticValues = Arrays.copyOf(semanticValues, semanticValues.length * 2);
                semanticValues[id] = value;
                semanticDictionary.put(value, id);
            }
            semantic[node] = name == null ? -(id + 1) : id + 1;
        }

        /** Complete a previously unknown block/source range, before publication. */
        public void completeRange(int node, int start, int end, boolean exact) {
            if (sealed) throw new IllegalStateException("index already sealed");
            Objects.checkIndex(node, size);
            if (starts[node] != NO_RANGE || ends[node] != NO_RANGE) {
                throw new IllegalArgumentException("node already has a source range");
            }
            if (start < 0 || end < start || end > source.length()) {
                throw new IllegalArgumentException("invalid completed source range");
            }
            int parent = parents[node];
            if (parent >= 0 && starts[parent] >= 0 && ends[parent] >= 0
                    && (start < starts[parent] || end > ends[parent])) {
                throw new IllegalArgumentException("completed range escapes parent");
            }
            for (int child = first[node]; child >= 0; child = next[child]) {
                if (starts[child] >= 0 && (starts[child] < start || ends[child] > end)) {
                    throw new IllegalArgumentException("completed AST range excludes an indexed child");
                }
            }
            starts[node] = start;
            ends[node] = end;
            if (exact) flags[node] |= EXACT_RANGE;
        }

        public M3ASTIndex seal() {
            if (sealed || size < 1) throw new IllegalStateException("AST index not open or empty");
            sealed = true;
            return new M3ASTIndex(this);
        }

        /**
         * Publish detached optional details once per row. Line numbers are
         * one-based in the supplied source; both unknown lines are -1. Enclosing
         * owners must be strict ancestors, never the row itself or a sibling.
         * FLOAT/DOUBLE values are unsigned decimal raw IEEE bits; integer values
         * are canonical decimal, CHAR exactly one UTF-16 unit, STRING any String.
         */
        public void details(int node, int startLine, int endLine, String name,
                            LiteralKind literalKind, String literalValue, long modifierBits,
                            int enclosingType, int enclosingExecutable) {
            if (sealed) throw new IllegalStateException("index already sealed");
            Objects.checkIndex(node, size);
            if (literalKinds != null && literalKinds[node] >= 0) throw new IllegalStateException("details already assigned");
            if (!((startLine == -1 && endLine == -1) || (startLine >= 1 && endLine >= startLine))) {
                throw new IllegalArgumentException("invalid one-based line pair");
            }
            if (starts[node] < 0 && startLine != -1) throw new IllegalArgumentException("line facts without source geometry");
            if (starts[node] == ends[node] && startLine != endLine) throw new IllegalArgumentException("empty range spans no line boundary");
            if ((modifierBits & ~ALL_MODIFIERS) != 0) throw new IllegalArgumentException("unknown modifier bits");
            checkAncestor(node, enclosingType);
            checkAncestor(node, enclosingExecutable);
            validateLiteral(Objects.requireNonNull(literalKind, "literalKind"), literalValue);
            if (literalKinds == null) allocateDetails(kinds.length);
            startLines[node] = startLine;
            endLines[node] = endLine;
            nodeNames[node] = name;
            literalKinds[node] = (byte) literalKind.ordinal();
            literalValues[node] = literalValue;
            modifiers[node] = modifierBits;
            enclosingTypes[node] = enclosingType;
            enclosingExecutables[node] = enclosingExecutable;
        }

        private void checkAncestor(int node, int ancestor) {
            if (ancestor == -1) return;
            int parent = parents[node];
            if (parent >= 0 && (parent == ancestor || (literalKinds != null && literalKinds[parent] >= 0
                    && (enclosingTypes[parent] == ancestor || enclosingExecutables[parent] == ancestor)))) return;
            for (int row = parents[node]; row >= 0; row = parents[row]) if (row == ancestor) return;
            throw new IllegalArgumentException("enclosing coordinate is not a strict ancestor");
        }

        private static void validateLiteral(LiteralKind kind, String value) {
            boolean valid = switch (kind) {
                case NONE, NULL -> value == null;
                case STRING -> value != null;
                case CHAR -> value != null && value.length() == 1;
                case BOOLEAN -> "true".equals(value) || "false".equals(value);
                case INT -> value != null && Integer.toString(Integer.parseInt(value)).equals(value);
                case LONG -> value != null && Long.toString(Long.parseLong(value)).equals(value);
                case FLOAT -> value != null && Integer.toUnsignedString(Integer.parseUnsignedInt(value)).equals(value);
                case DOUBLE -> value != null && Long.toUnsignedString(Long.parseUnsignedLong(value)).equals(value);
            };
            if (!valid) throw new IllegalArgumentException("noncanonical detached literal value");
        }

        private void allocateDetails(int capacity) {
            startLines = new int[capacity]; endLines = new int[capacity];
            enclosingTypes = new int[capacity]; enclosingExecutables = new int[capacity];
            literalKinds = new byte[capacity]; modifiers = new long[capacity];
            nodeNames = new String[capacity]; literalValues = new String[capacity];
            fillMissingDetails(0, capacity);
        }

        private void fillMissingDetails(int from, int to) {
            Arrays.fill(startLines, from, to, -1); Arrays.fill(endLines, from, to, -1);
            Arrays.fill(enclosingTypes, from, to, -1); Arrays.fill(enclosingExecutables, from, to, -1);
            Arrays.fill(literalKinds, from, to, (byte) -1);
        }

        private int nameId(String value) {
            if (value.isEmpty() || value.length() > 1024) {
                throw new IllegalArgumentException("invalid bounded AST kind");
            }
            for (int i = 0; i < nameCount; i++) {
                if (names[i].equals(value)) return i;
            }
            if (nameCount == 4096) throw new IllegalArgumentException("AST kind dictionary exceeded");
            if (nameCount == names.length) names = Arrays.copyOf(names, names.length * 2);
            names[nameCount] = value;
            return nameCount++;
        }

        private void grow() {
            int previous = kinds.length;
            int cap = (int) Math.min(maxNodes, Math.max((long) size + 1L, size + (long) (size >>> 1) + 1));
            kinds = Arrays.copyOf(kinds, cap);
            parents = Arrays.copyOf(parents, cap);
            starts = Arrays.copyOf(starts, cap);
            ends = Arrays.copyOf(ends, cap);
            flags = Arrays.copyOf(flags, cap);
            first = Arrays.copyOf(first, cap);
            last = Arrays.copyOf(last, cap);
            next = Arrays.copyOf(next, cap);
            childCounts = Arrays.copyOf(childCounts, cap);
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
            if (literalKinds != null) {
                startLines = Arrays.copyOf(startLines, cap); endLines = Arrays.copyOf(endLines, cap);
                enclosingTypes = Arrays.copyOf(enclosingTypes, cap);
                enclosingExecutables = Arrays.copyOf(enclosingExecutables, cap);
                literalKinds = Arrays.copyOf(literalKinds, cap); modifiers = Arrays.copyOf(modifiers, cap);
                nodeNames = Arrays.copyOf(nodeNames, cap); literalValues = Arrays.copyOf(literalValues, cap);
                fillMissingDetails(previous, cap);
            }
=======
            if (semantic != null) semantic = Arrays.copyOf(semantic, cap);
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
        }
    }
}
