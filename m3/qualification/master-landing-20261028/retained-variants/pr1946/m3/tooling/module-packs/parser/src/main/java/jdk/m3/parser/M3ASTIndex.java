// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser;

import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import javax.lang.model.element.Modifier;

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
    public enum Domain { ECLIPSE_JDT, MAVEN_POM, GRADLE_GROOVY, GRADLE_KOTLIN, JACKSON_JSON, JAVAC, OPENREWRITE, GENERIC }
    public enum LiteralKind { NONE, NULL, BOOLEAN, CHAR, STRING, INT, LONG, FLOAT, DOUBLE }
    private static final LiteralKind[] LITERAL_KINDS = LiteralKind.values();
    public static final String DETAILS_SCHEMA = "M3_AST_DETAILS_V1";
    public static final String SOURCE_RANGE_HASH_SCHEMA = "M3_SOURCE_RANGE_UTF16BE_SHA256_V1";
    public static final String STRUCTURAL_HASH_SCHEMA = "M3_AST_STRUCTURE_SHA256_V1";
    /** V2 is used only by GENERIC, whose identity includes its grammar and edge roles. */
    public static final String GENERIC_STRUCTURE_HASH_SCHEMA = "M3_AST_GENERIC_STRUCTURE_SHA256_V2";
    /** V3 additionally distinguishes ordered members of a named list-valued edge. */
    public static final String GENERIC_POSITIONED_STRUCTURE_HASH_SCHEMA = "M3_AST_GENERIC_STRUCTURE_SHA256_V3";
    /** Stable V1 modifier encoding, separate from the original four index flags. */
    public static final long PUBLIC = 1L, PROTECTED = 1L << 1, PRIVATE = 1L << 2;
    public static final long ABSTRACT = 1L << 3, DEFAULT = 1L << 4, STATIC = 1L << 5;
    public static final long FINAL = 1L << 6, TRANSIENT = 1L << 7, VOLATILE = 1L << 8;
    public static final long SYNCHRONIZED = 1L << 9, NATIVE = 1L << 10, STRICTFP = 1L << 11;
    public static final long SEALED = 1L << 12, NON_SEALED = 1L << 13, INTERFACE = 1L << 14;
    private static final long ALL_MODIFIERS = (1L << 15) - 1;
    public static final int NO_RANGE = -1;
    /** The origin parser guarantees an exact half-open UTF-16 source range. */
    public static final int EXACT_RANGE = 1;
    /** Syntax exists, but semantic binding/effective build model is not certified. */
    public static final int SYNTAX_ONLY = 2;
    /** The external source owner could not be verified against the original AST. */
    public static final int SOURCE_UNVERIFIED = 4;
    public static final int RECOVERED = 8;
    /** Optional CST/token facts; ordinary parser producers retain their existing flags. */
    public static final int NAMED = 16, TOKEN = 32, TRIVIA = 64, MISSING = 128, ERROR = 256;
    /** The external parser certified an optional trivia-inclusive full source range. */
    public static final int EXACT_FULL_RANGE = 512;
    private static final int LEGACY_FLAGS = EXACT_RANGE | SYNTAX_ONLY | SOURCE_UNVERIFIED | RECOVERED;
    private static final int KNOWN_FLAGS = EXACT_RANGE | SYNTAX_ONLY | SOURCE_UNVERIFIED
            | RECOVERED | NAMED | TOKEN | TRIVIA | MISSING | ERROR | EXACT_FULL_RANGE;
    private final M3File source;
    private final Domain domain;
    private final String providerId;
    private final String[] names;
    private final String[] roleNames;
    private final int[] roleIds;
    private final int[] rolePositions;
    private final int[] fullStarts, fullEnds;
    private final int[] kinds, parents, starts, ends, flags, first, next, childCounts;
    private final int[] startLines, endLines, enclosingTypes, enclosingExecutables;
    private final byte[] literalKinds;
    private final long[] modifiers;
    private final String[] nodeNames, literalValues;

    private M3ASTIndex(Builder builder) {
        source = builder.source;
        domain = builder.domain;
        providerId = builder.providerId;
        names = Arrays.copyOf(builder.names, builder.nameCount);
        roleNames = Arrays.copyOf(builder.roleNames, builder.roleCount);
        roleIds = builder.roleIds == null ? null : Arrays.copyOf(builder.roleIds, builder.size);
        rolePositions = builder.rolePositions == null ? null : Arrays.copyOf(builder.rolePositions, builder.size);
        fullStarts = builder.fullStarts == null ? null : Arrays.copyOf(builder.fullStarts, builder.size);
        fullEnds = builder.fullEnds == null ? null : Arrays.copyOf(builder.fullEnds, builder.size);
        kinds = Arrays.copyOf(builder.kinds, builder.size);
        parents = Arrays.copyOf(builder.parents, builder.size);
        starts = Arrays.copyOf(builder.starts, builder.size);
        ends = Arrays.copyOf(builder.ends, builder.size);
        flags = Arrays.copyOf(builder.flags, builder.size);
        first = Arrays.copyOf(builder.first, builder.size);
        next = Arrays.copyOf(builder.next, builder.size);
        childCounts = Arrays.copyOf(builder.childCounts, builder.size);
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
    }

    public static Builder builder(M3File source, Domain domain, int maxNodes) {
        if (domain == Domain.GENERIC) {
            throw new IllegalArgumentException("generic AST requires an explicit provider identity");
        }
        return new Builder(source, domain, maxNodes, Objects.requireNonNull(domain, "domain").name());
    }
    /** Build the original index for a specific, versioned foreign grammar owner. */
    public static Builder builder(M3File source, Domain domain, int maxNodes, String providerId) {
        return new Builder(source, domain, maxNodes, providerId);
    }
    public M3File source() { return source; }
    public Domain domain() { return domain; }
    /** Stable within the supplying parser/grammar contract; not a semantic binding. */
    public String providerId() { return providerId; }
    public int nodeCount() { return kinds.length; }
    public String kind(int node) { return names[kinds[check(node)]]; }
    public int parent(int node) { return parents[check(node)]; }
    public int flags(int node) { return flags[check(node)]; }
    public int start(int node) { return starts[check(node)]; }
    public int end(int node) { return ends[check(node)]; }
    public int firstChild(int node) { return first[check(node)]; }
    public int nextSibling(int node) { return next[check(node)]; }
    public int childCount(int node) { return childCounts[check(node)]; }
    /** Named incoming child edge, such as Tree-sitter's field or JDT's structural property. */
    public Optional<String> incomingRole(int node) {
        check(node);
        return roleIds == null || roleIds[node] < 0
                ? Optional.empty() : Optional.of(roleNames[roleIds[node]]);
    }
    /** List slot in the named incoming edge: -1 means scalar/unknown, 0+ means an ordered element. */
    public int incomingRolePosition(int node) {
        check(node);
        return rolePositions == null ? -1 : rolePositions[node];
    }
    /** Optional trivia-inclusive half-open UTF-16 range; -1 if the provider did not supply one. */
    public int fullStart(int node) { check(node); return fullStarts == null ? -1 : fullStarts[node]; }
    public int fullEnd(int node) { check(node); return fullEnds == null ? -1 : fullEnds[node]; }
    public boolean hasExactFullRange(int node) {
        check(node);
        return fullStarts != null && fullStarts[node] >= 0 && (flags[node] & EXACT_FULL_RANGE) != 0;
    }
    /** Reprint a certified trivia-inclusive span from the existing source payload. */
    public String fullSourceText(int node) {
        if (!hasExactFullRange(node)) throw new IllegalStateException("unverified/unknown full range");
        return source.subSequence(fullStarts[node], fullEnds[node]);
    }
    public boolean hasExactRange(int node) {
        check(node);
        return starts[node] >= 0 && ends[node] >= 0 && (flags[node] & EXACT_RANGE) != 0;
    }
    /** Materialization is explicit, never implicit during indexing or navigation. */
    public String sourceText(int node) {
        if (!hasExactRange(node)) throw new IllegalStateException("unverified/unknown source range");
        return source.subSequence(starts[node], ends[node]);
    }
    public long primitiveBytes() { return (long) kinds.length * 8L * Integer.BYTES; }
    /** Optional primitive lanes only; excludes references, Strings and object headers. */
    public long metadataBytes() {
        return (literalKinds == null ? 0L : nodeCount() * 25L)
                + (roleIds == null ? 0L : nodeCount() * Integer.BYTES)
                + (rolePositions == null ? 0L : nodeCount() * Integer.BYTES)
                + (fullStarts == null ? 0L : nodeCount() * 2L * Integer.BYTES);
    }
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
        }
        return -1;
    }

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
        return HexFormat.of().formatHex(digest.digest());
    }

    /**
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
            if (domain == Domain.GENERIC) {
                units += providerId.length();
                if (roleIds != null && roleIds[row] >= 0) units += roleNames[roleIds[row]].length();
            }
            if (hasDetails(row)) {
                if (nodeNames[row] != null) units += nodeNames[row].length();
                if (literalValues[row] != null) units += literalValues[row].length();
            }
            if (units > maxTextUnits) throw new IllegalArgumentException("structural hash text budget exceeded");
        }
        byte[] result = new byte[Math.multiplyExact(nodeCount(), 32)];
        MessageDigest digest = sha256();
        String schema = domain != Domain.GENERIC ? STRUCTURAL_HASH_SCHEMA
                : rolePositions == null ? GENERIC_STRUCTURE_HASH_SCHEMA
                : GENERIC_POSITIONED_STRUCTURE_HASH_SCHEMA;
        for (int row = nodeCount() - 1; row >= 0; row--) {
            digest.reset();
            putText(digest, schema);
            putText(digest, domain.name());
            if (domain == Domain.GENERIC) putText(digest, providerId);
            putText(digest, names[kinds[row]]);
            putInt(digest, flags[row] & (SYNTAX_ONLY | RECOVERED));
            if (domain == Domain.GENERIC) {
                putInt(digest, flags[row] & (NAMED | TOKEN | TRIVIA | MISSING | ERROR));
                putText(digest, roleIds == null || roleIds[row] < 0 ? null : roleNames[roleIds[row]]);
                if (rolePositions != null) putInt(digest, rolePositions[row]);
            }
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
        return new StructuralHashes(result, units, schema);
    }

    public static final class StructuralHashes {
        private final byte[] values;
        private final long textUnits;
        private final String schema;
        private StructuralHashes(byte[] values, long textUnits, String schema) {
            this.values = values;
            this.textUnits = textUnits;
            this.schema = schema;
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
        public String schema() { return schema; }
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
    }

    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
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
        if (domain == Domain.GENERIC && ((flags[a] & (NAMED | TOKEN | TRIVIA | MISSING | ERROR))
                != (flags[b] & (NAMED | TOKEN | TRIVIA | MISSING | ERROR))
                || !incomingRole(a).equals(incomingRole(b))
                || incomingRolePosition(a) != incomingRolePosition(b))) return false;
        boolean aDetails = literalKinds != null && literalKinds[a] >= 0;
        boolean bDetails = literalKinds != null && literalKinds[b] >= 0;
        if (aDetails != bDetails) return false;
        return !aDetails || (Objects.equals(nodeNames[a], nodeNames[b])
                && literalKinds[a] == literalKinds[b]
                && Objects.equals(literalValues[a], literalValues[b])
                && modifiers[a] == modifiers[b]);
    }

    private int check(int node) { return Objects.checkIndex(node, kinds.length); }

    /** Bounded, single-owner creation; discarded immediately after immutable publication. */
    public static final class Builder {
        private final M3File source;
        private final Domain domain;
        private final String providerId;
        private final int maxNodes;
        private String[] names = new String[16];
        private int nameCount;
        private String[] roleNames = new String[8];
        private int roleCount;
        private int[] roleIds;
        private int[] rolePositions;
        private int[] fullStarts, fullEnds;
        private int[] kinds = new int[32], parents = new int[32], starts = new int[32];
        private int[] ends = new int[32], flags = new int[32], first = new int[32];
        private int[] last = new int[32], next = new int[32], childCounts = new int[32];
        private int[] startLines, endLines, enclosingTypes, enclosingExecutables;
        private byte[] literalKinds;
        private long[] modifiers;
        private String[] nodeNames, literalValues;
        private int size;
        private boolean sealed;

        private Builder(M3File source, Domain domain, int maxNodes, String providerId) {
            this.source = Objects.requireNonNull(source, "source");
            this.domain = Objects.requireNonNull(domain, "domain");
            this.providerId = Objects.requireNonNull(providerId, "providerId");
            if (providerId.isBlank() || providerId.length() > 256) {
                throw new IllegalArgumentException("invalid AST provider identity");
            }
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
            return add(parent, kind, start, end, flags, null);
        }

        /** Attach an optional incoming field name without retaining an external AST node. */
        public int add(int parent, String kind, int start, int end, int flags, String role) {
            return add(parent, kind, start, end, flags, role, -1);
        }

        /** Preserve a list-valued parser field's zero-based position without a child wrapper. */
        public int add(int parent, String kind, int start, int end, int flags,
                       String role, int rolePosition) {
            if (sealed) throw new IllegalStateException("index already sealed");
            if (size == maxNodes) throw new IllegalArgumentException("AST index node budget exceeded");
            if (size == 0 ? parent != -1 : parent < 0 || parent >= size) {
                throw new IllegalArgumentException("bad AST parent or root coordinate");
            }
            if (!((start == NO_RANGE && end == NO_RANGE)
                    || (start >= 0 && end >= start && end <= source.length()))) {
                throw new IllegalArgumentException("invalid UTF-16 half-open AST range");
            }
            if ((flags & ~(domain == Domain.GENERIC ? KNOWN_FLAGS : LEGACY_FLAGS)) != 0) {
                throw new IllegalArgumentException("unknown AST index flags");
            }
            if ((flags & EXACT_FULL_RANGE) != 0) {
                throw new IllegalArgumentException("full-range certificate requires fullRange admission");
            }
            if (parent == -1 && role != null) {
                throw new IllegalArgumentException("root cannot have an incoming edge role");
            }
            if (rolePosition < -1 || (rolePosition >= 0 && (role == null || domain != Domain.GENERIC))) {
                throw new IllegalArgumentException("list position requires a generic named incoming edge");
            }
            if ((flags & EXACT_RANGE) != 0 && start == NO_RANGE) {
                throw new IllegalArgumentException("exact range without source coordinates");
            }
            if (parent >= 0 && starts[parent] >= 0 && ends[parent] >= 0 && start >= 0
                    && (start < starts[parent] || end > ends[parent])) {
                throw new IllegalArgumentException("AST child escapes known parent range");
            }
            int name = nameId(Objects.requireNonNull(kind, "kind"));
            int roleName = role == null ? -1 : roleId(role);
            if (size == kinds.length) grow();
            int row = size++;
            if (role != null && roleIds == null) {
                roleIds = new int[kinds.length];
                Arrays.fill(roleIds, -1);
            }
            if (roleIds != null) roleIds[row] = roleName;
            if (rolePosition >= 0 && rolePositions == null) {
                rolePositions = new int[kinds.length];
                Arrays.fill(rolePositions, -1);
            }
            if (rolePositions != null) rolePositions[row] = rolePosition;
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

        private int roleId(String role) {
            if (role.isBlank() || role.length() > 256) {
                throw new IllegalArgumentException("invalid bounded AST edge role");
            }
            for (int i = 0; i < roleCount; i++) if (roleNames[i].equals(role)) return i;
            if (roleCount == 4096) throw new IllegalArgumentException("AST role dictionary exceeded");
            if (roleCount == roleNames.length) roleNames = Arrays.copyOf(roleNames, roleCount * 2);
            roleNames[roleCount] = role;
            return roleCount++;
        }

        /**
         * Attach one optional provider-certified trivia-inclusive full span.
         * This is source geometry, deliberately excluded from structural hashes.
         * The source owner is not copied and a second AST is never retained.
         */
        public void fullRange(int node, int start, int end, boolean exact) {
            if (sealed) throw new IllegalStateException("index already sealed");
            Objects.checkIndex(node, size);
            if (domain != Domain.GENERIC) {
                throw new IllegalArgumentException("full span requires generic foreign AST");
            }
            if (exact && (flags[node] & SOURCE_UNVERIFIED) != 0) {
                throw new IllegalArgumentException("unverified source cannot certify full text");
            }
            if (start < 0 || end < start || end > source.length()) {
                throw new IllegalArgumentException("invalid full UTF-16 source span");
            }
            if (fullStarts != null && fullStarts[node] >= 0) {
                throw new IllegalStateException("full span already assigned");
            }
            if (starts[node] >= 0 && (start > starts[node] || end < ends[node])) {
                throw new IllegalArgumentException("full span excludes syntax core");
            }
            int parent = parents[node];
            if (parent >= 0 && fullStarts != null && fullStarts[parent] >= 0
                    && (start < fullStarts[parent] || end > fullEnds[parent])) {
                throw new IllegalArgumentException("full span escapes parent full span");
            }
            if (fullStarts != null) {
                for (int child = first[node]; child >= 0; child = next[child]) {
                    if (fullStarts[child] >= 0
                            && (fullStarts[child] < start || fullEnds[child] > end)) {
                        throw new IllegalArgumentException("full span excludes indexed child full span");
                    }
                }
            }
            if (fullStarts == null) {
                fullStarts = new int[kinds.length];
                fullEnds = new int[kinds.length];
                Arrays.fill(fullStarts, -1);
                Arrays.fill(fullEnds, -1);
            }
            fullStarts[node] = start;
            fullEnds[node] = end;
            if (exact) flags[node] |= EXACT_FULL_RANGE;
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
            if (fullStarts != null && fullStarts[node] >= 0
                    && (start < fullStarts[node] || end > fullEnds[node])) {
                throw new IllegalArgumentException("completed core escapes full span");
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
            if (roleIds != null) {
                roleIds = Arrays.copyOf(roleIds, cap);
                Arrays.fill(roleIds, previous, cap, -1);
            }
            if (rolePositions != null) {
                rolePositions = Arrays.copyOf(rolePositions, cap);
                Arrays.fill(rolePositions, previous, cap, -1);
            }
            if (fullStarts != null) {
                fullStarts = Arrays.copyOf(fullStarts, cap);
                fullEnds = Arrays.copyOf(fullEnds, cap);
                Arrays.fill(fullStarts, previous, cap, -1);
                Arrays.fill(fullEnds, previous, cap, -1);
            }
            if (literalKinds != null) {
                startLines = Arrays.copyOf(startLines, cap); endLines = Arrays.copyOf(endLines, cap);
                enclosingTypes = Arrays.copyOf(enclosingTypes, cap);
                enclosingExecutables = Arrays.copyOf(enclosingExecutables, cap);
                literalKinds = Arrays.copyOf(literalKinds, cap); modifiers = Arrays.copyOf(modifiers, cap);
                nodeNames = Arrays.copyOf(nodeNames, cap); literalValues = Arrays.copyOf(literalValues, cap);
                fillMissingDetails(previous, cap);
            }
        }
    }
}
