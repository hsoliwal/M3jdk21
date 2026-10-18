/*
 * Copyright (c) 2026.
 * SPDX-License-Identifier: Apache-2.0
 *
 * Deterministic source inventory recipe. Source files are never edited.
 */
package org.synexia.m3.recipe;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Inventory-first Maven-compatible, AST-driven M3 recipe.
 *
 * <p>This recipe emits reviewable SHA-256 atoms and unproven problem-category
 * candidates. It never changes or automatically promotes source. Downstream
 * recipes can only be enabled after category-specific proof and compiler/tests.
 */
public final class M3InventoryRecipe {
    private M3InventoryRecipe() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: M3InventoryRecipe <java-source-root> <output.jsonl>");
        }
        inventory(Path.of(args[0]), Path.of(args[1]));
    }

    public static void inventory(Path root, Path output) throws IOException {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(output, "output");
        final Path absoluteRoot = root.toAbsolutePath().normalize();
        if (!Files.isDirectory(absoluteRoot)) {
            throw new IllegalArgumentException("Not a source directory: " + root);
        }
        final List<Path> files;
        try (Stream<Path> walk = Files.walk(absoluteRoot)) {
            files = walk.filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".java"))
                    .sorted(Comparator.comparing(path -> relative(absoluteRoot, path)))
                    .toList();
        }
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("A full JDK (jdk.compiler) is required");
        }
        final StringBuilder jsonl = new StringBuilder();
        for (Path file : files) {
            scanOne(compiler, absoluteRoot, file, jsonl);
        }
        final Path parent = output.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(output, jsonl.toString(), StandardCharsets.UTF_8);
    }

    private static void scanOne(JavaCompiler compiler, Path root, Path file,
                                StringBuilder destination) throws IOException {
        final String source = Files.readString(file, StandardCharsets.UTF_8);
        final String fileName = relative(root, file);
        record(destination, fileName, 1, "FILE", "source_inventory", sha256(source));
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(
                diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            final var fileObjects = manager.getJavaFileObjects(file.toFile());
            final JavacTask task = (JavacTask) compiler.getTask(null, manager,
                    diagnostics, List.of("-proc:none"), null, fileObjects);
            final List<CompilationUnitTree> parsed = new ArrayList<>();
            for (CompilationUnitTree unit : task.parse()) {
                parsed.add(unit);
            }
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                    throw new IOException(fileName + ":" + diagnostic.getLineNumber()
                            + ": parse failed: " + diagnostic.getMessage(Locale.ROOT));
                }
            }
            final SourcePositions sourcePositions = Trees.instance(task).getSourcePositions();
            for (CompilationUnitTree unit : parsed) {
                new TreeScanner<Void, Void>() {
                    @Override
                    public Void visitMethod(MethodTree node, Void unused) {
                        evidence(node, "METHOD", "behavioral_atom");
                        return super.visitMethod(node, unused);
                    }

                    @Override
                    public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
                        final String category = invocationCategory(node.getMethodSelect().toString());
                        if (category != null) {
                            evidence(node, "INVOCATION", category);
                        }
                        return super.visitMethodInvocation(node, unused);
                    }

                    @Override
                    public Void visitNewArray(NewArrayTree node, Void unused) {
                        evidence(node, "ALLOCATION", "dynamic_arrays");
                        return super.visitNewArray(node, unused);
                    }

                    private void evidence(Tree node, String kind, String category) {
                        final long start = sourcePositions.getStartPosition(unit, node);
                        final long end = sourcePositions.getEndPosition(unit, node);
                        if (start < 0 || end < start || end > source.length()) {
                            return;
                        }
                        final long line = unit.getLineMap().getLineNumber(start);
                        final String atom = source.substring((int) start, (int) end);
                        record(destination, fileName, line, kind, category, sha256(atom));
                    }
                }.scan(unit, null);
            }
        }
    }

    private static String invocationCategory(String expression) {
        final int end = expression.lastIndexOf('.');
        final String name = end < 0 ? expression : expression.substring(end + 1);
        return switch (name) {
            case "arraycopy", "copyOf", "copyOfRange" -> "dynamic_arrays";
            case "sort", "sorted", "sortThis", "sortThisBy" -> "stable_sorted_views";
            case "filter", "select", "reject", "filtered", "filteredSorted" -> "filtered_views";
            case "substring", "slice", "subSequence", "concat" -> "composite_strings";
            case "retain", "release", "ownerCount" -> "storage_ownership";
            case "shallowSize", "getObjectSize" -> "object_sizing";
            default -> null;
        };
    }

    private static String relative(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }

    private static void record(StringBuilder out, String file, long line,
                               String kind, String category, String fingerprint) {
        out.append("{\"file\":\"").append(escape(file))
                .append("\",\"line\":").append(line)
                .append(",\"kind\":\"").append(kind)
                .append("\",\"category\":\"").append(category)
                .append("\",\"sha256\":\"").append(fingerprint)
                .append("\",\"proof\":\"UNPROVEN\"}\n");
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String sha256(String input) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 must exist in JDK21", exception);
        }
    }
}
