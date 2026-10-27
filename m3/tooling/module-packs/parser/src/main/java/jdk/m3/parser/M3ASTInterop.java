// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
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
 * offsets require an admitted strict UTF-8 {@link M3File} snapshot. Original
 * Unicode scalar/codepoint offsets instead use the source's sparse surrogate
 * index, never integer arithmetic that mistakes them for UTF-16 units.</p>
 */
public final class M3ASTInterop {
    public enum Coordinates { UTF16, UTF8_BYTES, CODE_POINTS }

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

        /**
         * Optional, bounded, parent-local edge snapshot. A parser with named
         * structural properties can enumerate its direct children once instead
         * of rescanning a list for each ordinal/role. This cursor is dropped
         * as soon as the depth-first parent frame completes; no foreign tree
         * or second persistent AST representation is retained.
         */
        default Edges<N> childEdges(N node, int maxEdges) { return null; }

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

    /** Ordered direct edges only, bounded independently of the AST node budget. */
    public interface Edges<N> {
        int count();
        N child(int ordinal);
        default String role(int ordinal) { return null; }
        default int rolePosition(int ordinal) { return -1; }
    }

    private static final int SURFACE_FLAGS = M3ASTIndex.NAMED | M3ASTIndex.TOKEN
            | M3ASTIndex.TRIVIA | M3ASTIndex.MISSING | M3ASTIndex.ERROR | M3ASTIndex.RECOVERED;

    private M3ASTInterop() { }

    /**
     * Opt-in projection of the original Eclipse JDT DOM tree and its actual
     * StructuralPropertyDescriptors. JDT remains the sole parser/AST owner;
     * this method has no compile-time or mandatory runtime JDT dependency.
     * Without an original source stamp, all ranges remain unverified.
     */
    public static M3ASTIndex eclipseJdtDom(M3File source, Object root,
            M3ASTIngress.Limits limits) {
        return eclipseJdtDom(source, root, null, limits);
    }

    /** A stamp is an explicit assertion from the original JDT source owner. */
    public static M3ASTIndex eclipseJdtDom(M3File source, Object root,
            SourceStamp parserStamp, M3ASTIngress.Limits limits) {
        return project(source, root, "eclipse.jdt.core.dom", Coordinates.UTF16,
                new JdtDomSurface(root), parserStamp, limits);
    }

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
        M3File.CodePointOffsets codePoints = coordinates == Coordinates.CODE_POINTS
                ? source.codePointOffsets() : null;
        M3ASTIndex.Builder index = M3ASTIndex.builder(source, M3ASTIndex.Domain.GENERIC,
                limits.maxNodes(), providerId);

        // Only a bounded depth stack is retained: never an external node graph.
        int capacity = Math.min(32, limits.maxDepth());
        Object[] nodes = new Object[capacity];
        int[] indexedRows = new int[capacity];
        int[] childCounts = new int[capacity];
        int[] next = new int[capacity];
        Edges<?>[] edgeFrames = new Edges<?>[capacity];
        int rootRow = append(index, -1, root, null, -1, coordinates,
                offsets, codePoints, surface, sourceVerified);
        nodes[0] = root;
        indexedRows[0] = rootRow;
        edgeFrames[0] = surface.childEdges(root, limits.maxNodes() - 1);
        childCounts[0] = count(surface, root, castEdges(edgeFrames[0]), limits.maxNodes() - 1);
        int depth = 1;
        while (depth > 0) {
            int frame = depth - 1;
            if (next[frame] == childCounts[frame]) {
                nodes[frame] = null;
                edgeFrames[frame] = null;
                depth--;
                continue;
            }
            N parent = cast(nodes[frame]);
            int ordinal = next[frame]++;
            Edges<N> edges = castEdges(edgeFrames[frame]);
            N child = Objects.requireNonNull(edges == null ? surface.child(parent, ordinal)
                    : edges.child(ordinal), "foreign AST child");
            String role = edges == null ? surface.childRole(parent, ordinal) : edges.role(ordinal);
            int rolePosition = edges == null ? surface.childRolePosition(parent, ordinal)
                    : edges.rolePosition(ordinal);
            if (depth == limits.maxDepth()) throw new IllegalArgumentException("AST depth budget exceeded");
            int row = append(index, indexedRows[frame], child, role, rolePosition,
                    coordinates, offsets, codePoints, surface, sourceVerified);
            if (depth == nodes.length) {
                int size = Math.min(limits.maxDepth(), nodes.length * 2);
                nodes = java.util.Arrays.copyOf(nodes, size);
                indexedRows = java.util.Arrays.copyOf(indexedRows, size);
                childCounts = java.util.Arrays.copyOf(childCounts, size);
                next = java.util.Arrays.copyOf(next, size);
                edgeFrames = java.util.Arrays.copyOf(edgeFrames, size);
            }
            nodes[depth] = child;
            indexedRows[depth] = row;
            int remaining = limits.maxNodes() - row - 1;
            edgeFrames[depth] = surface.childEdges(child, remaining);
            childCounts[depth] = count(surface, child, castEdges(edgeFrames[depth]), remaining);
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
        return coordinates != Coordinates.UTF8_BYTES || stamp.byteSha256() != null;
    }

    private static <N> int count(Surface<N> surface, N node) {
        int count = surface.childCount(node);
        if (count < 0) throw new IllegalArgumentException("negative foreign child count");
        return count;
    }

    private static <N> int count(Surface<N> surface, N node, Edges<N> edges, int maxEdges) {
        if (edges == null) return count(surface, node);
        int count = edges.count();
        if (count < 0 || count > maxEdges) {
            throw new IllegalArgumentException("foreign AST edge budget exceeded");
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private static <N> Edges<N> castEdges(Edges<?> value) { return (Edges<N>) value; }

    private static <N> int append(M3ASTIndex.Builder index, int parent, N node, String role,
            int rolePosition,
            Coordinates unit, M3File.Utf8Offsets offsets, M3File.CodePointOffsets codePoints,
            Surface<N> surface, boolean verified) {
        int start = surface.start(node), end = surface.end(node);
        if ((start == M3ASTIndex.NO_RANGE) != (end == M3ASTIndex.NO_RANGE)) {
            throw new IllegalArgumentException("partial foreign source range");
        }
        if (unit == Coordinates.UTF8_BYTES && start != M3ASTIndex.NO_RANGE) {
            start = offsets.toUtf16(start);
            end = offsets.toUtf16(end);
        } else if (unit == Coordinates.CODE_POINTS && start != M3ASTIndex.NO_RANGE) {
            start = codePoints.toUtf16(start);
            end = codePoints.toUtf16(end);
        }
        int fullStart = surface.fullStart(node), fullEnd = surface.fullEnd(node);
        if ((fullStart == M3ASTIndex.NO_RANGE) != (fullEnd == M3ASTIndex.NO_RANGE)) {
            throw new IllegalArgumentException("partial foreign full source range");
        }
        if (unit == Coordinates.UTF8_BYTES && fullStart != M3ASTIndex.NO_RANGE) {
            fullStart = offsets.toUtf16(fullStart);
            fullEnd = offsets.toUtf16(fullEnd);
        } else if (unit == Coordinates.CODE_POINTS && fullStart != M3ASTIndex.NO_RANGE) {
            fullStart = codePoints.toUtf16(fullStart);
            fullEnd = codePoints.toUtf16(fullEnd);
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

    /**
     * The JDT-specific work stops at its public DOM reflection surface.
     * Field metadata is read once per parent, into transient parallel arrays
     * held only by that parent's bounded traversal frame.
     */
    private static final class JdtDomSurface implements Surface<Object> {
        private final Class<?> nodes;
        private final Class<?> simpleNames;
        private final Method structuralProperties;
        private final Method structuralProperty;
        private final Method isChildProperty;
        private final Method isChildListProperty;
        private final Method propertyId;
        private final Method nodeStart;
        private final Method nodeLength;
        private final Method nodeFlags;
        private final Method identifier;
        private final int malformedFlag;
        private final int recoveredFlag;

        JdtDomSurface(Object root) {
            Objects.requireNonNull(root, "original Eclipse JDT root");
            try {
                ClassLoader loader = root.getClass().getClassLoader();
                nodes = Class.forName("org.eclipse.jdt.core.dom.ASTNode", false, loader);
                Class<?> descriptor = Class.forName(
                        "org.eclipse.jdt.core.dom.StructuralPropertyDescriptor", false, loader);
                simpleNames = Class.forName("org.eclipse.jdt.core.dom.SimpleName", false, loader);
                if (!nodes.isInstance(root)) {
                    throw new IllegalArgumentException("original root is not a JDT DOM ASTNode");
                }
                structuralProperties = nodes.getMethod("structuralPropertiesForType");
                structuralProperty = nodes.getMethod("getStructuralProperty", descriptor);
                isChildProperty = descriptor.getMethod("isChildProperty");
                isChildListProperty = descriptor.getMethod("isChildListProperty");
                propertyId = descriptor.getMethod("getId");
                nodeStart = nodes.getMethod("getStartPosition");
                nodeLength = nodes.getMethod("getLength");
                nodeFlags = nodes.getMethod("getFlags");
                identifier = simpleNames.getMethod("getIdentifier");
                malformedFlag = nodes.getField("MALFORMED").getInt(null);
                recoveredFlag = nodes.getField("RECOVERED").getInt(null);
            } catch (ReflectiveOperationException failure) {
                throw new IllegalArgumentException("Eclipse JDT DOM runtime is missing/incompatible", failure);
            }
        }

        @Override public String kind(Object node) {
            requireNode(node);
            return "JDT_" + node.getClass().getSimpleName();
        }

        @Override public int childCount(Object node) {
            return collect(node, Integer.MAX_VALUE).count();
        }

        @Override public Object child(Object node, int ordinal) {
            return collect(node, Integer.MAX_VALUE).child(ordinal);
        }

        @Override public Edges<Object> childEdges(Object node, int maxEdges) {
            return collect(node, maxEdges);
        }

        @Override public int start(Object node) {
            int at = (Integer) invoke(nodeStart, node);
            if (at < -1) throw new IllegalArgumentException("invalid JDT DOM source start");
            return at;
        }

        @Override public int end(Object node) {
            int at = start(node);
            int length = (Integer) invoke(nodeLength, node);
            if (length < 0) throw new IllegalArgumentException("invalid JDT DOM source length");
            return at == -1 ? -1 : Math.addExact(at, length);
        }

        @Override public int syntaxFlags(Object node) {
            int jdt = (Integer) invoke(nodeFlags, node);
            int bits = M3ASTIndex.NAMED;
            if ((jdt & recoveredFlag) != 0) bits |= M3ASTIndex.RECOVERED;
            if ((jdt & malformedFlag) != 0) {
                bits |= M3ASTIndex.ERROR | M3ASTIndex.RECOVERED;
            }
            return bits;
        }

        @Override public String name(Object node) {
            return simpleNames.isInstance(node) ? (String) invoke(identifier, node) : null;
        }

        private void requireNode(Object node) {
            if (!nodes.isInstance(node)) {
                throw new IllegalArgumentException("JDT DOM structural child is not an ASTNode");
            }
        }

        private Edges<Object> collect(Object parent, int maxEdges) {
            requireNode(parent);
            if (maxEdges < 0) throw new IllegalArgumentException("negative child budget");
            JdtEdges children = null;
            List<?> descriptors = (List<?>) invoke(structuralProperties, parent);
            for (Object descriptor : descriptors) {
                boolean scalar = (Boolean) invoke(isChildProperty, descriptor);
                boolean list = (Boolean) invoke(isChildListProperty, descriptor);
                if (!scalar && !list) continue;
                String role = (String) invoke(propertyId, descriptor);
                Object value = invoke(structuralProperty, parent, descriptor);
                if (scalar) {
                    if (value == null) continue;
                    requireNode(value);
                    if (children == null) children = new JdtEdges(maxEdges);
                    children.add(value, role, -1);
                } else {
                    if (!(value instanceof List<?> members)) {
                        throw new IllegalArgumentException("JDT child-list property is not a List");
                    }
                    for (int ordinal = 0; ordinal < members.size(); ordinal++) {
                        Object child = members.get(ordinal);
                        requireNode(child);
                        if (children == null) children = new JdtEdges(maxEdges);
                        children.add(child, role, ordinal);
                    }
                }
            }
            return children == null ? JdtEdges.EMPTY : children;
        }

        private static Object invoke(Method method, Object receiver, Object... arguments) {
            try {
                return method.invoke(receiver, arguments);
            } catch (InvocationTargetException failure) {
                throw new IllegalArgumentException("original JDT DOM traversal failed", failure.getCause());
            } catch (IllegalAccessException failure) {
                throw new IllegalArgumentException("original JDT DOM member inaccessible", failure);
            }
        }
    }

    private static final class JdtEdges implements Edges<Object> {
        static final JdtEdges EMPTY = new JdtEdges(0);
        private final int limit;
        private Object[] children;
        private String[] roles;
        private int[] positions;
        private int size;

        JdtEdges(int limit) {
            this.limit = limit;
            int capacity = Math.min(4, limit);
            children = new Object[capacity];
            roles = new String[capacity];
            positions = new int[capacity];
        }

        void add(Object child, String role, int position) {
            if (size == limit) throw new IllegalArgumentException("JDT child edge budget exceeded");
            if (size == children.length) {
                int grown = (int) Math.min((long) limit,
                        children.length + (long) (children.length >>> 1) + 1L);
                children = Arrays.copyOf(children, grown);
                roles = Arrays.copyOf(roles, grown);
                positions = Arrays.copyOf(positions, grown);
            }
            children[size] = child;
            roles[size] = role;
            positions[size++] = position;
        }

        @Override public int count() { return size; }
        @Override public Object child(int ordinal) { return children[Objects.checkIndex(ordinal, size)]; }
        @Override public String role(int ordinal) { return roles[Objects.checkIndex(ordinal, size)]; }
        @Override public int rolePosition(int ordinal) {
            return positions[Objects.checkIndex(ordinal, size)];
        }
    }

    @SuppressWarnings("unchecked")
    private static <N> N cast(Object object) { return (N) object; }
}
