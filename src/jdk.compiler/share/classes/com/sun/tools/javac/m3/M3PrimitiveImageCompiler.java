/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.sun.tools.javac.m3;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Opt-in compile-time emitter of immutable, type-sensitive primitive intern images.
 *
 * <p>Uses real javac attribution for compile-time constant variables and scans
 * primitive literal expressions. It never executes application methods or
 * annotation processors and never rewrites Java ==, boxed identity, class files,
 * initialization order, or observable exceptions. Rejected compilation produces
 * no output. Invoked as:
 * <pre>java -m jdk.compiler/com.sun.tools.javac.m3.M3PrimitiveImageCompiler
 *      --out constants.m3prim --slots constants.tsv Source.java ...</pre>
 *
 * <p>{@code --fold-expressions} additionally harvests primitive expressions whose
 * values javac has already established during attribution. This opt-in can add
 * rows and therefore changes the image namespace and slots; regenerate every
 * consumer sidecar together. Without it, valid legacy inputs retain identical
 * image bytes. No evaluator, application call, or new bytecode rewrite is used.
 * A compiler-known value is not permission to remove evaluation of its source
 * expression: receiver evaluation and other effects must still be preserved.
 *
 * <p>Output/source aliases, symlinks in output paths, and overlapping outputs
 * are rejected before publication. Two independent output renames are NOT a
 * multi-file transaction; consumers must check the sidecar's image namespace.
 * Publication assumes exclusive ownership of the output paths.</p>
 *
 * <p>Optional Java export: {@code --java-out Bounds.java --java-class example.Bounds
 * --constant LOW:long:ffffffffffffffe0 --constant HIGH:long:20}. These three options
 * are required together. Up to 128 unique uppercase ASCII names select typed raw
 * values already in the image. A request never adds a value. Output is deterministic
 * under request reordering and contains namespace, geometry, kind, slot, row offset,
 * raw-bits offset and raw bits as Java constants. No fixed virtual address is exported.
 * Recompile consumers after image changes: Java constant inlining can otherwise leave
 * stale coordinates. Validate the compiled namespace against the actual immutable
 * image and retain bounds/lifetime checks. All requested values must resolve before
 * any output is written; independent output renames are still not a transaction.</p>
 */
public final class M3PrimitiveImageCompiler {
    private static final long MAGIC = 0x4d335052494d3031L;
    private static final int VERSION = 1;
    private static final int HEADER_BYTES = 64;
    private static final int ROW_BYTES = 16;
    private static final int MAX_ROWS = 1 << 20;
    private static final int MAX_CONSTANTS = 128;

    private M3PrimitiveImageCompiler() {}

    private record Entry(int kind, long bits) {}

    private static final Comparator<Entry> ORDER = Comparator.comparingInt(Entry::kind)
            .thenComparing((a, b) -> Long.compareUnsigned(a.bits(), b.bits()));

    public static void main(String[] args) throws Exception {
        Path out = null;
        Path slots = null;
        boolean foldExpressions = false;
        Path javaOut = null;
        String javaClass = null;
        Map<String, Entry> constants = new TreeMap<>();
        List<Path> sources = new ArrayList<>();
        for (int n = 0; n < args.length; n++) {
            switch (args[n]) {
                case "--java-out" -> {
                    if (javaOut != null || ++n == args.length) throw new IllegalArgumentException("--java-out");
                    javaOut = Path.of(args[n]);
                }
                case "--java-class" -> {
                    if (javaClass != null || ++n == args.length) throw new IllegalArgumentException("--java-class");
                    javaClass = checkedClass(args[n]);
                }
                case "--constant" -> {
                    if (++n == args.length) throw new IllegalArgumentException("--constant");
                    request(constants, args[n]);
                }
                case "--fold-expressions" -> {
                    if (foldExpressions) throw new IllegalArgumentException("duplicate --fold-expressions");
                    foldExpressions = true;
                }
                case "--out" -> {
                    if (out != null || ++n == args.length) throw new IllegalArgumentException("--out");
                    out = Path.of(args[n]);
                }
                case "--slots" -> {
                    if (slots != null || ++n == args.length) throw new IllegalArgumentException("--slots");
                    slots = Path.of(args[n]);
                }
                default -> {
                    if (args[n].startsWith("--") || !args[n].endsWith(".java")) {
                        throw new IllegalArgumentException("unknown argument " + args[n]);
                    }
                    sources.add(Path.of(args[n]));
                }
            }
        }
        if (out == null || sources.isEmpty() || (slots != null && slots.equals(out))) {
            throw new IllegalArgumentException("usage: --out IMAGE [--slots TSV] [--fold-expressions] java-sources...");
        }
        boolean exporting = javaOut != null || javaClass != null || !constants.isEmpty();
        if (exporting && (javaOut == null || javaClass == null || constants.isEmpty())) {
            throw new IllegalArgumentException("--java-out, --java-class and --constant are required together");
        }
        out = checkedOutput(out);
        if (slots != null) slots = checkedOutput(slots);
        if (javaOut != null) {
            javaOut = checkedOutput(javaOut);
            String simple = javaClass.substring(javaClass.lastIndexOf('.') + 1);
            if (!javaOut.getFileName().toString().equals(simple + ".java")) {
                throw new IllegalArgumentException("Java output filename must match class name");
            }
        }
        List<Path> outputs = new ArrayList<>(List.of(out));
        if (slots != null) outputs.add(slots);
        if (javaOut != null) outputs.add(javaOut);
        for (int i = 0; i < outputs.size(); i++) {
            Path output = outputs.get(i);
            for (int j = 0; j < i; j++) {
                Path other = outputs.get(j);
                if (aliases(output, other) || output.startsWith(other) || other.startsWith(output)) {
                    throw new IOException("image, sidecar and Java outputs overlap");
                }
            }
            for (Path source : sources) {
                if (aliases(output, source)) throw new IOException("output aliases input source: " + source);
            }
        }
        emit(out, slots, sources, foldExpressions, javaOut, javaClass, constants);
    }

    private static void emit(Path out, Path slots, List<Path> sources,
                             boolean foldExpressions, Path javaOut, String javaClass,
                             Map<String, Entry> constants) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("javac compiler unavailable");
        TreeSet<Entry> values = new TreeSet<>(ORDER);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(
                diagnostics, null, StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromPaths(sources);
            JavacTask task = (JavacTask) compiler.getTask(null, fm, diagnostics,
                    List.of("--release", "21", "-proc:none", "-implicit:none"), null, units);
            List<CompilationUnitTree> parsed = new ArrayList<>();
            task.parse().forEach(parsed::add);
            task.analyze();
            if (diagnostics.getDiagnostics().stream().anyMatch(
                    diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)) {
                diagnostics.getDiagnostics().stream().filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
                        .forEach(d -> System.err.println(d));
                throw new IllegalArgumentException("javac analysis failed; image not written");
            }
            Trees trees = Trees.instance(task);
            for (CompilationUnitTree unit : parsed) {
                new TreePathScanner<Void, Void>() {
                    @Override public Void scan(Tree tree, Void unused) {
                        if (foldExpressions && tree instanceof JCTree.JCExpression expression
                                && expression.type != null) {
                            Object constant = expression.type.constValue();
                            if (constant != null) {
                                TypeKind kind = expression.type.getKind();
                                // javac stores boolean/char constants as Integer, unlike
                                // VariableElement.getConstantValue's public representation.
                                Object value = switch (kind) {
                                    case BOOLEAN -> ((Number) constant).intValue() != 0;
                                    case CHAR -> (char) ((Number) constant).intValue();
                                    default -> constant;
                                };
                                admitConstant(values, kind, value);
                            }
                        }
                        return super.scan(tree, unused);
                    }

                    @Override public Void visitLiteral(LiteralTree literal, Void unused) {
                        switch (literal.getKind()) {
                            case BOOLEAN_LITERAL -> admit(values, 1, (Boolean) literal.getValue() ? 1 : 0);
                            case CHAR_LITERAL -> admit(values, 4, (Character) literal.getValue());
                            case INT_LITERAL -> admit(values, 5, Integer.toUnsignedLong((Integer) literal.getValue()));
                            case LONG_LITERAL -> admit(values, 6, (Long) literal.getValue());
                            case FLOAT_LITERAL -> admit(values, 7, Integer.toUnsignedLong(
                                    Float.floatToRawIntBits((Float) literal.getValue())));
                            case DOUBLE_LITERAL -> admit(values, 8, Double.doubleToRawLongBits(
                                    (Double) literal.getValue()));
                            default -> { } // Strings, null and class literals are outside this image.
                        }
                        return super.visitLiteral(literal, unused);
                    }

                    @Override public Void visitVariable(VariableTree tree, Void unused) {
                        Element element = trees.getElement(getCurrentPath());
                        if (element instanceof VariableElement variable) {
                            Object constant = variable.getConstantValue();
                            if (constant != null) admitConstant(values,
                                    variable.asType().getKind(), constant);
                        }
                        return super.visitVariable(tree, unused);
                    }
                }.scan(unit, null);
            }
        }
        if (values.size() > MAX_ROWS) throw new IOException("too many primitive canonical entries");
        int count = values.size();
        int length = Math.addExact(HEADER_BYTES, Math.multiplyExact(count, ROW_BYTES));
        ByteBuffer buffer = ByteBuffer.allocate(length).order(ByteOrder.BIG_ENDIAN);
        buffer.putLong(0, MAGIC);
        buffer.putInt(8, VERSION);
        buffer.putInt(12, count);
        buffer.putLong(16, length);
        List<Entry> ordered = List.copyOf(values);
        for (int i = 0; i < ordered.size(); i++) {
            Entry entry = ordered.get(i);
            int at = HEADER_BYTES + i * ROW_BYTES;
            buffer.put(at, (byte) entry.kind());
            buffer.putLong(at + 8, entry.bits());
        }
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new InternalError(impossible);
        }
        digest.update(buffer.array(), 0, 16);
        digest.update(buffer.array(), HEADER_BYTES, length - HEADER_BYTES);
        byte[] namespace = digest.digest();
        buffer.position(24);
        buffer.put(namespace);
        // Prepare all output bytes before any file mutation. Atomic renames are best effort.
        StringBuilder rows = new StringBuilder("# M3PRIM01 SHA-256 ")
                .append(HexFormat.of().formatHex(namespace)).append('\n')
                .append("# kind\traw-bits-hex\tslot\tbyte-offset\n");
        for (int i = 0; i < ordered.size(); i++) {
            Entry entry = ordered.get(i);
            rows.append(entry.kind()).append('\t')
                    .append(HexFormat.of().toHexDigits(entry.bits())).append('\t')
                    .append(i).append('\t').append(HEADER_BYTES + i * ROW_BYTES).append('\n');
        }
        // Resolve every requested value and prepare Java bytes BEFORE any output mutation.
        byte[] javaBytes = javaOut == null ? null
                : javaConstants(javaClass, constants, ordered, namespace, length);
        // Independent renames, not a group transaction; consumers must verify namespace.
        write(out, buffer.array());
        if (slots != null) write(slots, rows.toString().getBytes(StandardCharsets.UTF_8));
        if (javaOut != null) write(javaOut, javaBytes);
        System.out.println("M3_PRIMITIVE_IMAGE rows=" + count + " sha256="
                + HexFormat.of().formatHex(namespace));
    }

    private static void admitConstant(TreeSet<Entry> values, TypeKind kind, Object value) {
        switch (kind) {
            case BOOLEAN -> admit(values, 1, (Boolean) value ? 1 : 0);
            case BYTE -> admit(values, 2, ((Number) value).byteValue() & 0xffL);
            case SHORT -> admit(values, 3, ((Number) value).shortValue() & 0xffffL);
            case CHAR -> admit(values, 4, (Character) value);
            case INT -> admit(values, 5, Integer.toUnsignedLong((Integer) value));
            case LONG -> admit(values, 6, ((Number) value).longValue());
            case FLOAT -> admit(values, 7, Integer.toUnsignedLong(
                    Float.floatToRawIntBits(((Number) value).floatValue())));
            case DOUBLE -> admit(values, 8, Double.doubleToRawLongBits(
                    ((Number) value).doubleValue()));
            default -> { } // String constants never enter the primitive table.
        }
    }

    private static void admit(TreeSet<Entry> values, int kind, long bits) {
        if (values.add(new Entry(kind, bits)) && values.size() > MAX_ROWS) {
            throw new IllegalArgumentException("too many primitive canonical entries");
        }
    }

    /** Bounded ASCII source names: no escaping, source injection or unbounded class constants. */
    private static String checkedClass(String name) {
        if (name.equals("java") || name.startsWith("java.")
                || name.endsWith(".java") || name.length() > 192) throw new IllegalArgumentException("Java class name too long");
        for (String part : name.split("\\.", -1)) {
            if (!part.matches("[A-Za-z_$][A-Za-z0-9_$]{0,63}")
                    || SourceVersion.isKeyword(part, SourceVersion.RELEASE_21)
                    || List.of("var", "yield", "record", "sealed", "permits").contains(part)) {
                throw new IllegalArgumentException("unsupported Java class/package identifier: " + part);
            }
        }
        return name;
    }

    /** Request syntax is NAME:primitive-kind:unsigned-raw-hex, not a Java expression. */
    private static void request(Map<String, Entry> requests, String text) {
        String[] parts = text.split(":", -1);
        if (parts.length != 3 || !parts[0].matches("[A-Z][A-Z0-9_]{0,47}")
                || !parts[2].matches("[0-9a-fA-F]{1,16}")) {
            throw new IllegalArgumentException("expected NAME:primitive-kind:raw-hex");
        }
        int kind = switch (parts[1]) {
            case "boolean" -> 1;
            case "byte" -> 2;
            case "short" -> 3;
            case "char" -> 4;
            case "int" -> 5;
            case "long" -> 6;
            case "float" -> 7;
            case "double" -> 8;
            default -> throw new IllegalArgumentException("unknown primitive kind: " + parts[1]);
        };
        long bits = Long.parseUnsignedLong(parts[2], 16);
        boolean valid = switch (kind) {
            case 1 -> bits == 0 || bits == 1;
            case 2 -> (bits >>> 8) == 0;
            case 3, 4 -> (bits >>> 16) == 0;
            case 5, 7 -> (bits >>> 32) == 0;
            default -> true;
        };
        if (!valid || requests.containsKey(parts[0]) || requests.size() == MAX_CONSTANTS) {
            throw new IllegalArgumentException("invalid, duplicate or excess constant: " + parts[0]);
        }
        requests.put(parts[0], new Entry(kind, bits));
    }

    /** No per-value runtime objects or payload copies; only named, already-admitted coordinates. */
    private static byte[] javaConstants(String name, Map<String, Entry> requests,
                                       List<Entry> ordered, byte[] namespace, int length) {
        int dot = name.lastIndexOf('.');
        String simple = name.substring(dot + 1);
        StringBuilder source = new StringBuilder("// Generated by M3PrimitiveImageCompiler. Rebuild with the matching image.\n");
        if (dot >= 0) source.append("package ").append(name, 0, dot).append(";\n");
        source.append("/** Coordinates are not pointers. Validate namespace, mapping and lifetime before use. */\n")
                .append("public final class ").append(simple).append(" {\n")
                .append("    private ").append(simple).append("() {}\n")
                .append("    public static final java.lang.String IMAGE_NAMESPACE = \"")
                .append(HexFormat.of().formatHex(namespace)).append("\";\n")
                .append("    public static final long IMAGE_LENGTH = ").append(length).append("L;\n")
                .append("    public static final int IMAGE_ROWS = ").append(ordered.size()).append(";\n")
                .append("    public static final int IMAGE_VERSION = ").append(VERSION).append(";\n");
        for (var request : requests.entrySet()) {
            Entry entry = request.getValue();
            int slot = Collections.binarySearch(ordered, entry, ORDER);
            if (slot < 0) throw new IllegalArgumentException("constant not admitted to image: " + request.getKey());
            String prefix = "    public static final ";
            String symbol = request.getKey();
            long row = HEADER_BYTES + (long) ROW_BYTES * slot;
            source.append(prefix).append("int ").append(symbol).append("_KIND = ").append(entry.kind()).append(";\n")
                    .append(prefix).append("int ").append(symbol).append("_SLOT = ").append(slot).append(";\n")
                    .append(prefix).append("long ").append(symbol).append("_ROW_OFFSET = ").append(row).append("L;\n")
                    .append(prefix).append("long ").append(symbol).append("_BITS_OFFSET = ").append(row + 8).append("L;\n")
                    .append(prefix).append("long ").append(symbol).append("_RAW_BITS = 0x")
                    .append(HexFormat.of().toHexDigits(entry.bits())).append("L;\n");
        }
        return source.append("}\n").toString().getBytes(StandardCharsets.UTF_8);
    }

    private static boolean aliases(Path left, Path right) throws IOException {
        return left.toAbsolutePath().normalize().equals(right.toAbsolutePath().normalize())
                || (Files.exists(left) && Files.exists(right) && Files.isSameFile(left, right));
    }

    private static Path checkedOutput(Path path) throws IOException {
        Path target = Objects.requireNonNull(path).toAbsolutePath().normalize();
        for (Path at = target; at != null; at = at.getParent()) {
            if (Files.isSymbolicLink(at)) throw new IOException("refusing output symlink: " + at);
            if (Files.exists(at) && (at.equals(target)
                    ? !Files.isRegularFile(at) : !Files.isDirectory(at))) {
                throw new IOException("invalid output path: " + at);
            }
        }
        return target;
    }

    private static void write(Path path, byte[] bytes) throws IOException {
        Path target = Objects.requireNonNull(path).toAbsolutePath().normalize();
        Path parent = target.getParent();
        if (Files.isSymbolicLink(target)) throw new IOException("refusing symlink: " + target);
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, ".m3-primitive-", ".tmp");
        try {
            Files.write(temporary, bytes);
            try {
                Files.move(temporary, target,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
