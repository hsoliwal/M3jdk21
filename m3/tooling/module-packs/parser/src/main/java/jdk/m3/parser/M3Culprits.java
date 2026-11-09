// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * M3 hard-limit culprits as pure lane queries over one JAVAC-domain {@link M3ASTIndex}: loops, long methods and
 * classes, {@code return null}, console output, magic numbers and floating strings. No javac tree, no second parser
 * and no OpenRewrite LST are touched; every rule reads kind, parent, child, line, name, literal and modifier lanes.
 * The rules are the ones the Synexia multipass marks and the recipe compiler's lint apply, so a mark produced here
 * and a mark produced by the OpenRewrite recipe name the same site. Results are two primitive lanes (occurrence row,
 * culprit ordinal); the caller decides what becomes a comment mark, a facts row or a TODO event.
 */
public final class M3Culprits {
    public enum Culprit { LOOP, METHOD_LENGTH, CLASS_LENGTH, RETURN_NULL, SYSTEM_OUT, MAGIC_NUMBER, FLOATING_STRING }

    /** Line limits; the M3 defaults are 15 lines per method and 300 per top-level class. */
    public record Limits(int maxMethodLines, int maxClassLines) {
        public Limits {
            if (maxMethodLines < 1 || maxClassLines < 1) throw new IllegalArgumentException("line limits must be positive");
        }
        public static Limits defaults() { return new Limits(15, 300); }
    }

    private static final Culprit[] CULPRITS = Culprit.values();
    private static final Set<String> LOOP_KINDS = Set.of("FOR_LOOP", "ENHANCED_FOR_LOOP", "WHILE_LOOP", "DO_WHILE_LOOP");
    private static final Set<String> TYPE_KINDS = Set.of("CLASS", "INTERFACE", "ENUM", "RECORD", "ANNOTATION_TYPE");
    private static final Set<String> CONSTANT_KINDS = Set.of("ANNOTATION", "TYPE_ANNOTATION", "CASE");
    private static final Set<String> CONSOLE_STREAMS = Set.of("out", "err");
    private static final Set<String> METADATA_METHODS = Set.of("getDisplayName", "getDescription", "getTags", "getInstanceNameSuffix");
    private static final Set<String> MESSAGE_METHODS = Set.of("format", "formatted", "info", "warn", "error", "debug", "trace",
            "requireNonNull", "checkArgument", "checkState", "assertTrue", "assertFalse", "assertEquals", "assertNotNull",
            "fail", "println", "print", "append", "getProperty", "getenv", "of", "matches");
    private static final String METHOD = "METHOD";
    private static final String RETURN = "RETURN";
    private static final String NULL_LITERAL = "NULL_LITERAL";
    private static final String MEMBER_SELECT = "MEMBER_SELECT";
    private static final String IDENTIFIER = "IDENTIFIER";
    private static final String METHOD_INVOCATION = "METHOD_INVOCATION";
    private static final String VARIABLE = "VARIABLE";
    private static final String THROW = "THROW";
    private static final String SYSTEM = "System";
    private static final int ROOT = 0;
    private static final long STATIC_FINAL = M3ASTIndex.STATIC | M3ASTIndex.FINAL;
    private static final double INDEX_ARITHMETIC_LIMIT = 2.0;

    private M3Culprits() { }

    /** Scans every occurrence once; the index must be a JAVAC-domain index with details. */
    public static Report scan(M3ASTIndex index, Limits limits) {
        Objects.requireNonNull(index, "index");
        Objects.requireNonNull(limits, "limits");
        if (index.domain() != M3ASTIndex.Domain.JAVAC) {
            throw new IllegalArgumentException("culprit lanes require a JAVAC domain index");
        }
        Report report = new Report(index);
        for (int row = 0; row < index.nodeCount(); row++) {
            String kind = index.kind(row);
            if (LOOP_KINDS.contains(kind)) report.add(row, Culprit.LOOP);
            if (METHOD.equals(kind) && index.spanLines(row) > limits.maxMethodLines()) report.add(row, Culprit.METHOD_LENGTH);
            if (TYPE_KINDS.contains(kind) && index.parent(row) == ROOT && index.spanLines(row) > limits.maxClassLines()) {
                report.add(row, Culprit.CLASS_LENGTH);
            }
            if (RETURN.equals(kind) && childKind(index, row, NULL_LITERAL)) report.add(row, Culprit.RETURN_NULL);
            if (MEMBER_SELECT.equals(kind) && consoleStream(index, row)) report.add(row, Culprit.SYSTEM_OUT);
            scanLiteral(index, row, report);
        }
        return report.seal();
    }

    private static void scanLiteral(M3ASTIndex index, int row, Report report) {
        M3ASTIndex.LiteralKind literal = index.literalKind(row);
        if (literal == M3ASTIndex.LiteralKind.NONE || literal == M3ASTIndex.LiteralKind.NULL
                || literal == M3ASTIndex.LiteralKind.BOOLEAN || literal == M3ASTIndex.LiteralKind.CHAR
                || constantPosition(index, row)) {
            return;
        }
        if (literal == M3ASTIndex.LiteralKind.STRING) {
            if (!index.literalValue(row).orElse("").isEmpty() && !messagePosition(index, row)) {
                report.add(row, Culprit.FLOATING_STRING);
            }
        } else if (magic(numericValue(literal, index.literalValue(row).orElse("0")))) {
            report.add(row, Culprit.MAGIC_NUMBER);
        }
    }

    /** INT/LONG are canonical decimal; FLOAT/DOUBLE are unsigned decimal raw IEEE bits (the details contract). */
    static double numericValue(M3ASTIndex.LiteralKind kind, String value) {
        return switch (kind) {
            case INT -> Integer.parseInt(value);
            case LONG -> Long.parseLong(value);
            case FLOAT -> Float.intBitsToFloat(Integer.parseUnsignedInt(value));
            case DOUBLE -> Double.longBitsToDouble(Long.parseUnsignedLong(value));
            default -> 0;
        };
    }

    static boolean magic(double value) {
        return Math.abs(value) > INDEX_ARITHMETIC_LIMIT || value != Math.rint(value);
    }

    private static boolean childKind(M3ASTIndex index, int row, String kind) {
        for (int child = index.firstChild(row); child >= 0; child = index.nextSibling(child)) {
            if (kind.equals(index.kind(child))) return true;
        }
        return false;
    }

    private static boolean consoleStream(M3ASTIndex index, int row) {
        if (!index.name(row).filter(CONSOLE_STREAMS::contains).isPresent()) return false;
        int target = index.firstChild(row);
        return target >= 0 && IDENTIFIER.equals(index.kind(target)) && index.name(target).filter(SYSTEM::equals).isPresent();
    }

    /** static final variables, interface fields, annotation arguments and case labels never hold magic values. */
    static boolean constantPosition(M3ASTIndex index, int row) {
        for (int ancestor = index.parent(row); ancestor >= 0; ancestor = index.parent(ancestor)) {
            String kind = index.kind(ancestor);
            if (CONSTANT_KINDS.contains(kind)) return true;
            if (VARIABLE.equals(kind)) {
                if ((index.modifierBits(ancestor) & STATIC_FINAL) == STATIC_FINAL) return true;
                int owner = index.enclosingType(ancestor);
                if (index.enclosingExecutable(ancestor) < 0 && owner >= 0
                        && (index.modifierBits(owner) & M3ASTIndex.INTERFACE) != 0) return true;
            }
        }
        return false;
    }

    /** throw statements, recipe metadata methods and message-style invocations carry their strings legitimately. */
    static boolean messagePosition(M3ASTIndex index, int row) {
        int executable = index.enclosingExecutable(row);
        if (executable >= 0 && index.name(executable).filter(METADATA_METHODS::contains).isPresent()) return true;
        for (int ancestor = index.parent(row); ancestor >= 0; ancestor = index.parent(ancestor)) {
            String kind = index.kind(ancestor);
            if (THROW.equals(kind)) return true;
            if (METHOD_INVOCATION.equals(kind) && invokedName(index, ancestor).filter(MESSAGE_METHODS::contains).isPresent()) {
                return true;
            }
        }
        return false;
    }

    /** The invoked method's simple name: the first IDENTIFIER or MEMBER_SELECT child of the invocation. */
    static Optional<String> invokedName(M3ASTIndex index, int invocation) {
        for (int child = index.firstChild(invocation); child >= 0; child = index.nextSibling(child)) {
            String kind = index.kind(child);
            if (IDENTIFIER.equals(kind) || MEMBER_SELECT.equals(kind)) return index.name(child);
        }
        return Optional.empty();
    }

    /** Two primitive lanes per finding; rows are original preorder occurrence coordinates. */
    public static final class Report {
        private final M3ASTIndex index;
        private int[] nodes = new int[16];
        private byte[] culprits = new byte[16];
        private int size;
        private boolean sealed;

        private Report(M3ASTIndex index) { this.index = index; }

        private void add(int row, Culprit culprit) {
            if (size == nodes.length) {
                nodes = Arrays.copyOf(nodes, size * 2);
                culprits = Arrays.copyOf(culprits, size * 2);
            }
            nodes[size] = row;
            culprits[size++] = (byte) culprit.ordinal();
        }

        private Report seal() {
            nodes = Arrays.copyOf(nodes, size);
            culprits = Arrays.copyOf(culprits, size);
            sealed = true;
            return this;
        }

        public M3ASTIndex index() { return index; }
        public int size() { return size; }
        public int node(int finding) { return nodes[Objects.checkIndex(finding, size)]; }
        public Culprit culprit(int finding) { return CULPRITS[culprits[Objects.checkIndex(finding, size)]]; }
        /** One-based start line of the finding's occurrence, or -1 without geometry. */
        public int line(int finding) { return index.startLine(node(finding)); }
        public int count(Culprit culprit) {
            int n = 0;
            for (int i = 0; i < size; i++) if (culprits[i] == culprit.ordinal()) n++;
            return n;
        }
        public boolean sealed() { return sealed; }
        /** Retained primitive payload: one int and one byte per finding. */
        public long primitiveBytes() { return (long) size * (Integer.BYTES + 1); }

        /** {@code line\tculprit\tkind\tname} per finding, in occurrence order. */
        public String tsv() {
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < size; i++) {
                out.append(line(i)).append('\t').append(culprit(i)).append('\t').append(index.kind(nodes[i]))
                        .append('\t').append(index.name(nodes[i]).orElse("")).append('\n');
            }
            return out.toString();
        }
    }
}
