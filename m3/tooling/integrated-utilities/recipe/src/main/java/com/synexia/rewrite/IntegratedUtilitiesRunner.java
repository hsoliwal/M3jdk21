// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/**
 * Actual OpenRewrite SDK caller and serial Result emitter. It writes only into a fresh,
 * source-disjoint directory after whole-dataset admission and an actual SDK fixed point.
 * Use an exclusively owned input checkout; emission is not a multi-file filesystem transaction.
 * This runner never edits product sources and does not claim compiler/runtime qualification.
 */
public final class IntegratedUtilitiesRunner {
    private IntegratedUtilitiesRunner() { }

    public static void main(String[] args) throws IOException {
        boolean check = args.length == 2 && args[0].equals("--check");
        boolean emit = args.length == 3 && args[0].equals("--emit");
        if (!check && !emit) {
            throw new IllegalArgumentException("--check source-root | --emit source-root fresh-output-root");
        }
        var report = run(new M3IntegratedUtilitiesRecipe(), Path.of(args[1]),
                emit ? Path.of(args[2]) : null, check, () -> { });
        System.out.println("ActualOpenRewriteResults=" + report.size()
                + " secondSdkRunChanges=0 runtimeContracts=not-run-by-recipe-engine");
    }

    static List<Result> qualifiedResults(M3GpuToolJoinRecipe recipe, List<SourceFile> sources,
            Runnable checkpoint) {
        Objects.requireNonNull(recipe, "recipe");
        Objects.requireNonNull(sources, "sources");
        Objects.requireNonNull(checkpoint, "checkpoint");
        var failures = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(failures::add);
        context.putMessage(M3GpuToolJoinRecipe.CHECKPOINT_MESSAGE, checkpoint);
        List<Result> results;
        try {
            results = recipe.run(new InMemoryLargeSourceSet(sources), context)
                    .getChangeset().getAllResults();
        } catch (RuntimeException failure) {
            throw new IllegalStateException("actual OpenRewrite SDK refused candidate generation", failure);
        }
        if (!failures.isEmpty()) {
            throw new IllegalStateException("actual OpenRewrite SDK reported " + failures.size()
                    + " error(s); no candidate output is admitted", failures.get(0));
        }
        return List.copyOf(results);
    }

    static Map<String, String> run(M3GpuToolJoinRecipe recipe, Path sourceRoot, Path outputRoot,
            boolean checkOnly, Runnable checkpoint) throws IOException {
        Objects.requireNonNull(checkpoint, "checkpoint");
        Path source = sourceRoot.toRealPath();
        if (!Files.isDirectory(source)) throw new IllegalStateException("source root is not a directory");
        Path output = outputRoot == null ? null : outputRoot.toAbsolutePath().normalize();
        if (!checkOnly) {
            validateOutput(source, output);
        }
        var manifest = M3GpuToolJoinManifest.load(recipe.resourceRoot(),
                IntegratedUtilitiesRunner.class::getResourceAsStream, checkpoint);
        var originals = new LinkedHashMap<String, String>();
        var inputs = new ArrayList<SourceFile>();
        for (var target : manifest.targets()) {
            manifest.checkpoint();
            if (target.removal()) throw new IllegalStateException("this additive receiver does not emit removals");
            if (target.path().equals("RECIPE_RESULTS.json")) throw new IllegalStateException("reserved report path");
            Path file = checkedPath(source, target.path());
            if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
                if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IllegalStateException("source target is not a regular file: " + target.path());
                }
                String text = Files.readString(file, StandardCharsets.UTF_8);
                originals.put(target.path(), text);
                inputs.add(text(target.path(), text));
            }
        }
        List<Result> results = qualifiedResults(recipe, inputs, checkpoint);
        var byPath = new LinkedHashMap<String, SourceFile>();
        for (var result : results) {
            var after = result.getAfter();
            if (after == null) throw new IllegalStateException("unexpected removal Result");
            String path = M3GpuToolJoinManifest.normalized(after.getSourcePath());
            var target = manifest.target(path);
            if (target == null || target.text() == null || !after.printAll().equals(target.text())
                    || !M3GpuToolJoinManifest.sha256(after.printAll()).equals(target.after())
                    || byPath.putIfAbsent(path, after) != null) {
                throw new IllegalStateException("unexpected or unsealed OpenRewrite Result: " + path);
            }
        }
        var finalInputs = new ArrayList<SourceFile>();
        var outputTexts = new LinkedHashMap<String, String>();
        // The existing manifest order determines serial candidate output order.
        for (var target : manifest.targets()) {
            manifest.checkpoint();
            String original = originals.get(target.path());
            boolean changed = original == null || !M3GpuToolJoinManifest.sha256(original).equals(target.after());
            SourceFile result = byPath.get(target.path());
            if (changed != (result != null)) {
                throw new IllegalStateException("SDK Result coverage differs from the sealed transition: " + target.path());
            }
            if (result != null) {
                finalInputs.add(result);
                outputTexts.put(target.path(), result.printAll());
            } else {
                if (original == null) throw new IllegalStateException("missing fixed-point owner");
                finalInputs.add(text(target.path(), original));
            }
        }
        if (!qualifiedResults(recipe, finalInputs, checkpoint).isEmpty()) {
            throw new IllegalStateException("actual second SDK run is not a fixed point");
        }
        // Refuse a stale snapshot before emitting any candidate file.
        for (var target : manifest.targets()) {
            manifest.checkpoint();
            Path file = checkedPath(source, target.path());
            String current = Files.exists(file, LinkOption.NOFOLLOW_LINKS)
                    ? Files.readString(file, StandardCharsets.UTF_8) : null;
            if (!Objects.equals(originals.get(target.path()), current)) {
                throw new IllegalStateException("source changed after SDK generation: " + target.path());
            }
        }
        if (checkOnly) {
            if (!outputTexts.isEmpty()) throw new IllegalStateException("recipe has " + outputTexts.size() + " pending changes");
            return Map.copyOf(outputTexts);
        }
        manifest.checkpoint();
        validateOutput(source, output);
        Files.createDirectories(output.getParent());
        Files.createDirectory(output);
        for (var entry : outputTexts.entrySet()) {
            manifest.checkpoint();
            Path file = output.resolve(entry.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file, entry.getValue(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        }
        Files.writeString(output.resolve("RECIPE_RESULTS.json"), report(outputTexts), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        return java.util.Collections.unmodifiableMap(outputTexts);
    }

    private static void validateOutput(Path source, Path output) {
        if (output == null || output.startsWith(source) || source.startsWith(output)) {
            throw new IllegalStateException("output must be disjoint from the source tree");
        }
        Path ancestor = output.getRoot();
        for (Path part : output) {
            ancestor = ancestor.resolve(part);
            if (Files.isSymbolicLink(ancestor)) {
                throw new IllegalStateException("symlink output ancestor refused: " + ancestor);
            }
        }
        if (Files.exists(output, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("output directory must not exist");
        }
    }

    private static Path checkedPath(Path root, String relative) {
        Path path = root;
        for (Path part : Path.of(relative)) {
            path = path.resolve(part);
            if (Files.isSymbolicLink(path)) throw new IllegalStateException("symlink target refused: " + relative);
        }
        return path;
    }

    private static SourceFile text(String path, String value) {
        return PlainText.builder().sourcePath(Path.of(path)).text(value).build();
    }

    private static String report(Map<String, String> outputs) {
        StringBuilder result = new StringBuilder("{\n  \"engine\": \"actual OpenRewrite SDK\",\n"
                + "  \"second_sdk_run_changes\": 0,\n"
                + "  \"runtime_contracts\": \"NOT_RUN_BY_RECIPE_ENGINE\",\n  \"outputs\": [\n");
        int index = 0;
        for (var entry : outputs.entrySet()) {
            if (index++ != 0) result.append(",\n");
            result.append("    {\"path\": ").append(quote(entry.getKey()))
                    .append(", \"sha256\": ").append(quote(M3GpuToolJoinManifest.sha256(entry.getValue())))
                    .append('}');
        }
        return result.append("\n  ]\n}\n").toString();
    }

    private static String quote(String value) {
        StringBuilder result = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char unit = value.charAt(i);
            if (unit == '\\' || unit == '"') result.append('\\');
            result.append(unit);
        }
        return result.append('"').toString();
    }
}
