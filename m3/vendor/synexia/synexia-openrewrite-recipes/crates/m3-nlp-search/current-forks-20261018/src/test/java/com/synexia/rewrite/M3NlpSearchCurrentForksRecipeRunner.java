// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.text.PlainText;

/** Task-local SDK qualification and emission harness; mutation belongs to the existing recipes. */
public final class M3NlpSearchCurrentForksRecipeRunner {
    private M3NlpSearchCurrentForksRecipeRunner() {}
    record Target(String path, String before, String after) {}

    static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    static List<SourceFile> parse(String kind, Map<String, String> texts) {
        if (!kind.equals("java")) {
            return texts.entrySet().stream().map(entry -> (SourceFile) PlainText.builder()
                    .sourcePath(Path.of(entry.getKey())).text(entry.getValue()).build()).toList();
        }
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        List<SourceFile> parsed = JavaParser.fromJavaVersion().build().parseInputs(texts.entrySet()
                .stream().map(entry -> Parser.Input.fromString(Path.of(entry.getKey()), entry.getValue())).toList(),
                null, context).toList();
        require(errors.isEmpty(), "input Java parse failed: " + errors);
        require(parsed.size() == texts.size(), "input Java source count changed");
        for (SourceFile source : parsed) require(Objects.equals(texts.get(source.getSourcePath().toString()),
                source.printAll()), "input Java print drift: " + source.getSourcePath());
        return parsed;
    }

    static ScanningRecipe<?> recipe(String kind, String name) {
        return switch (kind) {
            case "java" -> new M3HashPinnedJavaSnapshotRecipe(name);
            case "text" -> new M3HashPinnedTextSnapshotRecipe(name);
            case "descriptor" -> new com.m3.rewrite.jmod.M3ModuleDescriptorRecipe(name);
            default -> throw new IllegalArgumentException("java, text or descriptor required");
        };
    }

    static Map<String, String> apply(ScanningRecipe<?> recipe, List<SourceFile> sources) {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        var changes = recipe.run(new InMemoryLargeSourceSet(sources), context, 1)
                .getChangeset().getAllResults();
        require(errors.isEmpty(), "SDK execution failed: " + errors);
        var output = new TreeMap<String, String>();
        for (var change : changes) {
            var after = Objects.requireNonNull(change.getAfter(), "snapshot recipe deleted source");
            require(output.put(after.getSourcePath().toString(), after.printAll()) == null,
                    "SDK returned duplicate output path");
        }
        return output;
    }

    static <S> void refusal(ScanningRecipe<S> recipe, List<SourceFile> sources, String reason) {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        boolean rejected = false;
        try {
            S state = recipe.getInitialValue(context);
            var scanner = recipe.getScanner(state);
            for (var source : sources) scanner.visit(source, context);
            recipe.generate(state, context);
            recipe.getVisitor(state);
        } catch (IllegalStateException | IllegalArgumentException expected) {
            rejected = true;
        }
        require(rejected || !errors.isEmpty(), reason + " was not refused before emission");
    }

    static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    static Path contained(Path root, String relative) throws IOException {
        Path path = root.resolve(relative).normalize();
        require(!Path.of(relative).isAbsolute() && path.startsWith(root) && !path.equals(root),
                "target escapes root: " + relative);
        if (Files.exists(path)) require(path.toRealPath().startsWith(root.toRealPath()),
                "target follows external symlink: " + relative);
        return path;
    }

    static List<Target> manifest(String kind, String name, Path resourceRoot) throws IOException {
        Path manifest = kind.equals("descriptor") ? Path.of(name) :
                resourceRoot.resolve("com/synexia/rewrite/hash-pinned-" + kind + "/" + name + "/manifest.tsv");
        var rows = new ArrayList<Target>();
        for (String line : Files.readString(manifest, StandardCharsets.UTF_8).lines().toList()) {
            if (line.isBlank() || line.startsWith("#") || line.startsWith("module\tartifacts\t")) continue;
            String[] cells = line.split("\t", -1);
            if (kind.equals("descriptor")) {
                require(cells.length == 6, "descriptor manifest row width");
                rows.add(new Target(cells[3], "ABSENT", cells[4]));
            } else {
                require(cells.length == 4, "snapshot manifest row width");
                rows.add(new Target(cells[0], cells[1], cells[2]));
            }
        }
        require(!rows.isEmpty(), "empty manifest");
        return rows;
    }

    public static Map<String, Object> qualify(String kind, String name, Path resourceRoot,
            Path inputRoot, Path outputRoot) throws Exception {
        inputRoot = inputRoot.toAbsolutePath().normalize();
        outputRoot = outputRoot.toAbsolutePath().normalize();
        require(Files.isDirectory(inputRoot), "input root is not a directory");
        require(!inputRoot.equals(outputRoot), "SDK output must be an isolated overlay");
        List<Target> targets = manifest(kind, name, resourceRoot);
        var original = new TreeMap<String, String>();
        for (Target target : targets) {
            Path path = contained(inputRoot, target.path());
            if (Files.exists(path)) original.put(target.path(), Files.readString(path, StandardCharsets.UTF_8));
        }
        ScanningRecipe<?> recipe = recipe(kind, name);
        var before = parse(kind, original);
        var changes = apply(recipe, before);
        var after = new TreeMap<>(original);
        after.putAll(changes);
        require(after.size() == targets.size(), "SDK target count differs from exact manifest");
        for (Target target : targets) require(target.after().equals(sha(after.get(target.path()))),
                "SDK postimage hash differs: " + target.path());
        var postimages = parse(kind, after);
        require(apply(recipe(kind, name), postimages).isEmpty(), "second SDK application is not a fixed point");
        var drift = new TreeMap<>(after);
        String first = targets.getFirst().path();
        drift.put(first, drift.get(first) + "\n");
        refusal(recipe(kind, name), parse(kind, drift), "source drift");
        var duplicate = new ArrayList<>(postimages);
        duplicate.add(postimages.getFirst());
        refusal(recipe(kind, name), duplicate, "duplicate source");
        int missing = 0;
        if (!kind.equals("descriptor")) {
            Target required = targets.stream().filter(target -> !target.before().equals("ABSENT")).findFirst().orElse(null);
            if (required != null) {
                var omitted = new TreeMap<>(after);
                omitted.remove(required.path());
                refusal(recipe(kind, name), parse(kind, omitted), "missing source");
                missing = 1;
            }
        }
        // Validate every output destination before writing any SDK result.
        for (Target target : targets) {
            Path path = contained(outputRoot, target.path());
            if (Files.exists(path)) require(Files.readString(path, StandardCharsets.UTF_8)
                    .equals(after.get(target.path())), "existing overlay differs: " + target.path());
        }
        for (Target target : targets) {
            Path path = contained(outputRoot, target.path());
            Files.createDirectories(path.getParent());
            Files.writeString(path, after.get(target.path()), StandardCharsets.UTF_8);
        }
        var receipt = new LinkedHashMap<String, Object>();
        receipt.put("schema", "M3_NLP_CURRENT_FORKS_SDK_RECEIPT_V1");
        receipt.put("engine", recipe.getClass().getName());
        receipt.put("sdk_version", "8.17.1");
        receipt.put("kind", kind);
        receipt.put("crate", name);
        receipt.put("targets", targets.size());
        receipt.put("first_application_changes", changes.size());
        receipt.put("fixed_point_changes", 0);
        receipt.put("drift_refusals", 1);
        receipt.put("duplicate_refusals", 1);
        receipt.put("missing_refusals", missing);
        receipt.put("source_sha256", targets.stream().collect(java.util.stream.Collectors.toMap(
                Target::path, Target::after, (a, b) -> { throw new IllegalStateException("duplicate target"); },
                TreeMap::new)));
        return receipt;
    }

    public static void main(String[] args) throws Exception {
        require(args.length == 6, "kind crate-or-descriptor resources input-root output-root receipt required");
        Map<String, Object> receipt = qualify(args[0], args[1], Path.of(args[2]),
                Path.of(args[3]), Path.of(args[4]));
        Path output = Path.of(args[5]).toAbsolutePath().normalize();
        Files.createDirectories(output.getParent());
        new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(output.toFile(), receipt);
        System.out.println("SDK PASS: " + receipt.get("targets") + " targets; " +
                receipt.get("first_application_changes") + " changes; fixed point, drift, duplicate and admitted missing-source refusals verified");
    }
}
