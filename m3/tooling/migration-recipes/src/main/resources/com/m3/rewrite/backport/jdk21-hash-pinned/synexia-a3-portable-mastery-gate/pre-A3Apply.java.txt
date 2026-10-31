// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.tree.ParseError;

/** Explicit FILE-local candidate application. Never writes the OpenJDK source/test trees. */
public final class A3Apply {

    public record Receipt(
            String path,
            String recipe,
            String scope,
            String beforeSha,
            String afterSha,
            boolean changed,
            boolean fixedPoint) {

        public Receipt {
            path = text(path, "path");
            recipe = text(recipe, "recipe");
            scope = text(scope, "scope");
            beforeSha = sha(beforeSha, "beforeSha");
            afterSha = sha(afterSha, "afterSha");
            if (!fixedPoint) {
                throw new IllegalArgumentException("fixedPoint");
            }
        }
    }

    private A3Apply() {
    }

    public static List<Receipt> run(
            Path root,
            Path out,
            List<String> sources) throws IOException {
        Path checkedRoot = A3Fs.root(root);
        Path checkedOut = A3Fs.out(checkedRoot, out);
        List<String> ordered =
                sources.stream()
                        .map(value -> text(value, "source"))
                        .distinct()
                        .sorted()
                        .toList();
        if (ordered.isEmpty()) {
            throw new IllegalArgumentException("A3Apply requires explicit sources");
        }

        ArrayList<Receipt> receipts = new ArrayList<>();
        for (String source : ordered) {
            receipts.add(applyOne(checkedRoot, checkedOut, source));
        }
        receipts.sort(Comparator.comparing(Receipt::path));
        writeReceipt(checkedRoot, checkedOut, receipts);
        return List.copyOf(receipts);
    }

    private static Receipt applyOne(
            Path root,
            Path out,
            String source) throws IOException {
        Path file = A3Fs.source(root, Path.of(source));
        String relative = A3Fs.rel(root, file);
        if ((!relative.startsWith("src/")
                        && !relative.startsWith("test/"))
                || !relative.endsWith(".java")) {
            throw new IllegalArgumentException(
                    "A3Apply accepts only explicit JDK Java source/test files: "
                            + relative);
        }
        if (!Files.isRegularFile(file)) {
            throw new IOException("missing A3 source: " + file);
        }

        String before = Files.readString(file, StandardCharsets.UTF_8);
        SourceFile parsed = parse(relative, before);
        Recipe recipe = A3RecipeHome.recipe();
        String after = apply(recipe, parsed);
        SourceFile converged = parse(relative, after);

        var secondContext =
                new InMemoryExecutionContext(
                        error -> {
                            throw new IllegalStateException(error);
                        });
        var second =
                recipe.run(
                        new InMemoryLargeSourceSet(List.of(converged)),
                        secondContext,
                        8);
        if (!second.getChangeset().getAllResults().isEmpty()) {
            throw new IllegalStateException(
                    "A3 candidate did not reach fixed point: " + relative);
        }

        Path target = out.resolve("candidate").resolve(relative).normalize();
        if (!target.startsWith(out)) {
            throw new IllegalArgumentException("A3 candidate path escaped output");
        }
        A3Fs.write(root, target, after);

        return new Receipt(
                relative,
                recipe.getClass().getName(),
                "FILE",
                A3Fs.sha(before),
                A3Fs.sha(after),
                !before.equals(after),
                true);
    }

    private static SourceFile parse(String path, String source) {
        var context =
                new InMemoryExecutionContext(
                        error -> {
                            throw new IllegalStateException(error);
                        });
        List<SourceFile> parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(
                                        Parser.Input.fromString(
                                                Path.of(path),
                                                source)),
                                null,
                                context)
                        .toList();
        if (parsed.size() != 1 || parsed.getFirst() instanceof ParseError) {
            throw new IllegalArgumentException(
                    "A3 could not parse Java 21 source: " + path);
        }
        SourceFile file = parsed.getFirst();
        if (!file.printAll().equals(source)) {
            throw new IllegalArgumentException(
                    "A3 Java parse/print was not lossless: " + path);
        }
        return file;
    }

    private static String apply(Recipe recipe, SourceFile source) {
        var context =
                new InMemoryExecutionContext(
                        error -> {
                            throw new IllegalStateException(error);
                        });
        var run =
                recipe.run(
                        new InMemoryLargeSourceSet(List.of(source)),
                        context,
                        8);
        var results = run.getChangeset().getAllResults();
        if (results.isEmpty()) {
            return source.printAll();
        }
        if (results.size() != 1
                || results.getFirst().getAfter() == null) {
            throw new IllegalStateException(
                    "A3 FILE recipe produced unexpected change shape: "
                            + source.getSourcePath());
        }
        SourceFile after =
                Objects.requireNonNull(results.getFirst().getAfter(), "after");
        if (!after.getSourcePath().equals(source.getSourcePath())) {
            throw new IllegalStateException(
                    "A3 FILE recipe changed source path: "
                            + source.getSourcePath());
        }
        return after.printAll();
    }

    private static void writeReceipt(
            Path root,
            Path out,
            List<Receipt> receipts) throws IOException {
        StringBuilder tsv =
                new StringBuilder(
                        "path\trecipe\tscope\tbeforeSha\tafterSha\tchanged\tfixedPoint\n");
        for (Receipt receipt : receipts) {
            tsv.append(A3Fs.cell(receipt.path()))
                    .append('\t')
                    .append(A3Fs.cell(receipt.recipe()))
                    .append('\t')
                    .append(receipt.scope())
                    .append('\t')
                    .append(receipt.beforeSha())
                    .append('\t')
                    .append(receipt.afterSha())
                    .append('\t')
                    .append(receipt.changed())
                    .append('\t')
                    .append(receipt.fixedPoint())
                    .append('\n');
        }
        A3Fs.write(root, out.resolve("receipt.tsv"), tsv.toString());
    }

    private static String text(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        if (checked.isEmpty()
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0
                || checked.indexOf('\t') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked.replace('\\', '/');
    }

    private static String sha(String value, String field) {
        String checked = text(value, field);
        if (!checked.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }
}
