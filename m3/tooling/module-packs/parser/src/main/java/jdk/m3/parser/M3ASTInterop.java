// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser;

import java.net.URI;
import java.util.Objects;

/**
 * Minimal read-only ingress from an already parsed foreign syntax tree into
 * the original {@link M3ASTIndex} primitive occurrence lanes. No parser,
 * intermediate AST, source copy, external node reference or reflection is
 * retained. A provider implements a few cursor operations over its existing
 * nodes: javac, Tree-sitter, JDT, ANTLR, OpenRewrite, Roslyn and other trees
 * can supply the same information without pretending that their kinds or
 * semantic bindings are interchangeable.
 *
 * <p>Child edges, rather than nodes, carry field roles. The same external
 * object in two positions yields two indexed occurrences. Acyclic, stable
 * traversal is the provider's obligation; budgets refuse cycles/depth growth.
 * Detached source geometry is always UTF-16 half-open. Optional UTF-8 byte
 * offsets require an admitted strict UTF-8 {@link M3File} snapshot.</p>
 */
public final class M3ASTInterop {
    public enum Coordinates { UTF16, UTF8_BYTES }

    /** Immutable facts originally recorded by the parser's source owner. */
    public record SourceStamp(URI uri, String utf16Sha256, String byteSha256) {
        public SourceStamp {
            Objects.requireNonNull(uri, "parser source URI");
            Objects.requireNonNull(utf16Sha256, "parser source UTF-16 digest");
        }
    }

    /** The only required bridge to the foreign provider is kind/children. */
    public interface Surface<N> {
        String kind(N node);
        int childCount(N node);
        N child(N node, int ordinal);

        /** Optional named field on the parent's ordered child edge. */
        default String childRole(N parent, int ordinal) { return null; }
        /** -1 for scalar/unknown edges; 0+ for a position in a named list field. */
        default int childRolePosition(N parent, int ordinal) { return -1; }
        /** Unknown coordinates are exactly (-1,-1), never inferred. */
        default int start(N node) { return M3ASTIndex.NO_RANGE; }
        default int end(N node) { return M3ASTIndex.NO_RANGE; }
        /** Optional trivia-inclusive source envelope in the same original coordinate unit. */
        default int fullStart(N node) { return M3ASTIndex.NO_RANGE; }
        default int fullEnd(N node) { return M3ASTIndex.NO_RANGE; }
        /** Only NAMED, TOKEN, TRIVIA, MISSING, ERROR and RECOVERED are accepted. */
        default int syntaxFlags(N node) { return 0; }
        /** Optional detached semantic-independent facts, supplied by the original owner. */
        default String name(N node) { return null; }
        default M3ASTIndex.LiteralKind literalKind(N node) { return M3ASTIndex.LiteralKind.NONE; }
        default String literalValue(N node) { return null; }
        default long modifierBits(N node) { return 0L; }
    }

    private static final int SURFACE_FLAGS = M3ASTIndex.NAMED | M3ASTIndex.TOKEN
            | M3ASTIndex.TRIVIA | M3ASTIndex.MISSING | M3ASTIndex.ERROR | M3ASTIndex.RECOVERED;

    private M3ASTInterop() { }

    /**
     * One iterative, bounded visit of the already parsed source generation.
     * A null stamp preserves SOURCE_UNVERIFIED; a matching stamp is only as
     * authoritative as its supplying parser, never fabricated by this method.
     * Byte-coordinate verification also requires the actual raw byte digest.
     */
    public static <N> M3ASTIndex project(M3File source, N root, String providerId,
            Coordinates coordinates, Surface<N> surface, SourceStamp parserStamp,
            M3ASTIngress.Limits limits) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(root, "original root");
        Objects.requireNonNull(coordinates, "coordinates");
        Objects.requireNonNull(surface, "original parser surface");
        Objects.requireNonNull(limits, "limits");
        if (source.length() > limits.maxSourceUnits()) {
            throw new IllegalArgumentException("original source exceeds AST ingress budget");
        }
        boolean sourceVerified = matches(source, parserStamp, coordinates);
        M3File.Utf8Offsets offsets = coordinates == Coordinates.UTF8_BYTES
                ? source.utf8Offsets().orElseThrow(() ->
                        new IllegalArgumentException("byte-offset AST requires strict original UTF-8 snapshot"))
                : null;
        M3ASTIndex.Builder index = M3ASTIndex.builder(source, M3ASTIndex.Domain.GENERIC,
                limits.maxNodes(), providerId);

        // Only a bounded depth stack is retained: never an external node graph.
        int capacity = Math.min(32, limits.maxDepth());
        Object[] nodes = new Object[capacity];
        int[] indexedRows = new int[capacity];
        int[] childCounts = new int[capacity];
        int[] next = new int[capacity];
        int rootRow = append(index, -1, root, null, -1, coordinates, offsets, surface, sourceVerified);
        nodes[0] = root;
        indexedRows[0] = rootRow;
        childCounts[0] = count(surface, root);
        int depth = 1;
        while (depth > 0) {
            int frame = depth - 1;
            if (next[frame] == childCounts[frame]) {
                nodes[frame] = null;
                depth--;
                continue;
            }
            N parent = cast(nodes[frame]);
            int ordinal = next[frame]++;
            N child = Objects.requireNonNull(surface.child(parent, ordinal), "foreign AST child");
            String role = surface.childRole(parent, ordinal);
            int rolePosition = surface.childRolePosition(parent, ordinal);
            if (depth == limits.maxDepth()) throw new IllegalArgumentException("AST depth budget exceeded");
            int row = append(index, indexedRows[frame], child, role, rolePosition,
                    coordinates, offsets, surface, sourceVerified);
            if (depth == nodes.length) {
                int size = Math.min(limits.maxDepth(), nodes.length * 2);
                nodes = java.util.Arrays.copyOf(nodes, size);
                indexedRows = java.util.Arrays.copyOf(indexedRows, size);
                childCounts = java.util.Arrays.copyOf(childCounts, size);
                next = java.util.Arrays.copyOf(next, size);
            }
            nodes[depth] = child;
            indexedRows[depth] = row;
            childCounts[depth] = count(surface, child);
            next[depth++] = 0;
        }
        return index.seal();
    }

    private static boolean matches(M3File source, SourceStamp stamp, Coordinates coordinates) {
        if (stamp == null) return false;
        if (!source.uri().equals(stamp.uri())
                || !source.utf16Sha256().equals(stamp.utf16Sha256())
                || (stamp.byteSha256() != null
                        && !source.byteSha256().filter(stamp.byteSha256()::equals).isPresent())) {
            throw new IllegalArgumentException("foreign parser source generation");
        }
        return coordinates == Coordinates.UTF16 || stamp.byteSha256() != null;
    }

    private static <N> int count(Surface<N> surface, N node) {
        int count = surface.childCount(node);
        if (count < 0) throw new IllegalArgumentException("negative foreign child count");
        return count;
    }

    private static <N> int append(M3ASTIndex.Builder index, int parent, N node, String role,
            int rolePosition,
            Coordinates unit, M3File.Utf8Offsets offsets, Surface<N> surface, boolean verified) {
        int start = surface.start(node), end = surface.end(node);
        if ((start == M3ASTIndex.NO_RANGE) != (end == M3ASTIndex.NO_RANGE)) {
            throw new IllegalArgumentException("partial foreign source range");
        }
        if (unit == Coordinates.UTF8_BYTES && start != M3ASTIndex.NO_RANGE) {
            start = offsets.toUtf16(start);
            end = offsets.toUtf16(end);
        }
        int fullStart = surface.fullStart(node), fullEnd = surface.fullEnd(node);
        if ((fullStart == M3ASTIndex.NO_RANGE) != (fullEnd == M3ASTIndex.NO_RANGE)) {
            throw new IllegalArgumentException("partial foreign full source range");
        }
        if (unit == Coordinates.UTF8_BYTES && fullStart != M3ASTIndex.NO_RANGE) {
            fullStart = offsets.toUtf16(fullStart);
            fullEnd = offsets.toUtf16(fullEnd);
        }
        int flags = surface.syntaxFlags(node);
        if ((flags & ~SURFACE_FLAGS) != 0) throw new IllegalArgumentException("unknown foreign syntax flags");
        if ((flags & (M3ASTIndex.MISSING | M3ASTIndex.ERROR)) != 0) flags |= M3ASTIndex.RECOVERED;
        flags |= M3ASTIndex.SYNTAX_ONLY;
        if (verified && start != M3ASTIndex.NO_RANGE) flags |= M3ASTIndex.EXACT_RANGE;
        if (!verified) flags |= M3ASTIndex.SOURCE_UNVERIFIED;
        int row = index.add(parent, surface.kind(node), start, end, flags, role, rolePosition);
        if (fullStart != M3ASTIndex.NO_RANGE) index.fullRange(row, fullStart, fullEnd, verified);
        String name = surface.name(node);
        M3ASTIndex.LiteralKind kind = Objects.requireNonNull(surface.literalKind(node), "literal kind");
        String literal = surface.literalValue(node);
        long modifiers = surface.modifierBits(node);
        if (name != null || kind != M3ASTIndex.LiteralKind.NONE || literal != null || modifiers != 0L) {
            index.details(row, -1, -1, name, kind, literal, modifiers, -1, -1);
        }
        return row;
    }

    @SuppressWarnings("unchecked")
    private static <N> N cast(Object object) { return (N) object; }
}
