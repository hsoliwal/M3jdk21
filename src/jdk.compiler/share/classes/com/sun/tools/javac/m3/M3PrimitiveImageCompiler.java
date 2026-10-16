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
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
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
 */
public final class M3PrimitiveImageCompiler {
    private static final long MAGIC = 0x4d335052494d3031L;
    private static final int VERSION = 1;
    private static final int HEADER_BYTES = 64;
    private static final int ROW_BYTES = 16;
    private static final int MAX_ROWS = 1 << 20;

    private M3PrimitiveImageCompiler() {}

    private record Entry(int kind, long bits) {}

    private static final Comparator<Entry> ORDER = Comparator.comparingInt(Entry::kind)
            .thenComparing((a, b) -> Long.compareUnsigned(a.bits(), b.bits()));

    public static void main(String[] args) throws Exception {
        Path out = null;
        Path slots = null;
        List<Path> sources = new ArrayList<>();
        for (int n = 0; n < args.length; n++) {
            switch (args[n]) {
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
            throw new IllegalArgumentException("usage: --out IMAGE [--slots TSV] java-sources...");
        }
        emit(out, slots, sources);
    }

    private static void emit(Path out, Path slots, List<Path> sources) throws Exception {
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
        // Do not silently publish a new image with mismatched / stale metadata.
        write(out, buffer.array());
        if (slots != null) write(slots, rows.toString().getBytes(StandardCharsets.UTF_8));
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
        values.add(new Entry(kind, bits));
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
