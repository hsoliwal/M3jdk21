// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.synexia.rewrite.sorts;

import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import com.synexia.rewrite.M3HashPinnedTextSnapshotRecipe;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

/**
 * Thin, additive filesystem receiver for the exact sorting recipe packet.
 *
 * <p>All source mutation comes from actual canonical OpenRewrite Recipe.run results.
 * This receiver checks raw byte custody, validates the complete candidate and fixed point,
 * then creates files serially. It never replaces an existing file and never deletes a file.
 * Apply in an isolated checkout with no concurrent writers; a interrupted additive apply
 * can be replayed from any exact subset of the sealed postimages.</p>
 */
public final class M3JavaUtilSortsApply {
    private M3JavaUtilSortsApply() {}

    record Target(boolean java, String path, String before, String after) {}
    record Evaluation(List<SourceFile> sources, List<String> changed) {}
    record Application(int targets, List<String> changed, String treeSha256, String receipt) {}

    public static void main(String[] args) throws Exception {
        if (args.length != 3 || args[0].equals("UNSET")
                || !(args[1].equals("apply") || args[1].equals("dry-run")
                     || args[1].equals("check"))) {
            throw new IllegalArgumentException(
                    "Required: -DtargetRoot=/exact/M3jdk21 -DapplyMode=dry-run|apply|check");
        }
        if (Runtime.version().feature() != 21) {
            throw new IllegalStateException("This sealed recipe requires a Java 21 runtime");
        }
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        Path receipt = Path.of(args[2]).toAbsolutePath().normalize();
        if (receipt.startsWith(root)) {
            throw new IllegalArgumentException("The receipt must be outside the target checkout");
        }
        String actualHead = targetHead(root);
        if (!args[1].equals("check") && !actualHead.equals(M3JavaUtilSortsRecipe.TARGET_BASE)) {
            throw new IllegalStateException("Expected target HEAD "
                    + M3JavaUtilSortsRecipe.TARGET_BASE + "; found " + actualHead);
        }
        Application result = args[1].equals("check")
                ? check(root) : apply(root, args[1].equals("apply"));
        Files.createDirectories(receipt.getParent());
        String checkedReceipt = result.receipt().replace(
                "target_base\t" + M3JavaUtilSortsRecipe.TARGET_BASE + "\n",
                "target_base\t" + M3JavaUtilSortsRecipe.TARGET_BASE + "\nchecked_revision\t"
                        + actualHead + "\n");
        Files.writeString(receipt, checkedReceipt, StandardCharsets.UTF_8);
        System.out.println("mode=" + args[1] + " targets=" + result.targets()
                + " changed=" + result.changed().size() + " fixedPoint=0 treeSha256="
                + result.treeSha256() + " receipt=" + receipt);
    }

    static Application check(Path root) throws IOException {
        Application result = apply(root, false);
        if (!result.changed().isEmpty()) {
            throw new IllegalStateException("Incomplete sealed postimage: "
                    + result.changed().size() + " target files are missing");
        }
        return new Application(result.targets(), result.changed(), result.treeSha256(),
                result.receipt().replace("mode\tdry-run\n", "mode\tcheck\n"));
    }

    static Application apply(Path inputRoot, boolean write) throws IOException {
        Path root = inputRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)
                || !root.equals(root.toRealPath())) {
            throw new IllegalStateException("Target root must be a real directory");
        }
        List<Target> targets = targets();
        Map<String, String> observed = new TreeMap<>();
        List<SourceFile> before = readTargets(root, targets, observed);
        Recipe recipe = new M3JavaUtilSortsRecipe();
        Evaluation first = evaluate(recipe, before);
        Map<String, SourceFile> candidates = byPath(first.sources());
        if (candidates.size() != targets.size()) {
            throw new IllegalStateException("Incomplete or unowned recipe output");
        }
        for (Target target : targets) {
            SourceFile source = candidates.get(target.path());
            if (source == null || !sha256(source.printAll()).equals(target.after())) {
                throw new IllegalStateException("Unexpected recipe output: " + target.path());
            }
        }
        Evaluation fixedPoint = evaluate(recipe, first.sources());
        if (!fixedPoint.changed().isEmpty()) {
            throw new IllegalStateException("Recipe did not reach an exact source fixed point");
        }

        // Finish all source/recipe validation before any target write.
        for (Target target : targets) {
            Path file = safePath(root, target.path());
            String current = Files.exists(file, LinkOption.NOFOLLOW_LINKS)
                    ? fileDigest(file) : "ABSENT";
            if (!current.equals(observed.get(target.path()))) {
                throw new IllegalStateException("Target changed after inventory: " + target.path());
            }
        }
        if (write) {
            for (String relative : first.changed()) {
                Path file = safePath(root, relative);
                Files.createDirectories(file.getParent());
                safePath(root, relative);
                Files.writeString(file, candidates.get(relative).printAll(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            }
            for (Target target : targets) {
                if (!fileDigest(safePath(root, target.path())).equals(target.after())) {
                    throw new IllegalStateException("Receiving readback drift: " + target.path());
                }
            }
        }
        StringBuilder tree = new StringBuilder();
        for (Target target : targets) {
            tree.append(target.path()).append('\t').append(target.after()).append('\n');
        }
        String rootSha = sha256(tree.toString());
        StringBuilder receipt = new StringBuilder(
                "# SPDX-License-Identifier: Apache-2.0\n"
                + "# Copyright 2026 Hitesh Soliwal and contributors\n");
        receipt.append("schema\tM3_JAVA_UTIL_SORTS_APPLICATION_V1\n");
        receipt.append("recipe\tcom.synexia.m3.JavaUtilSorts20261018\n");
        receipt.append("target_base\t").append(M3JavaUtilSortsRecipe.TARGET_BASE).append('\n');
        receipt.append("engine_base\t").append(M3JavaUtilSortsRecipe.ENGINE_BASE).append('\n');
        receipt.append("java_manifest_sha256\t").append(sha256(resource(
                "/com/synexia/rewrite/hash-pinned-java/"
                        + M3JavaUtilSortsRecipe.CRATE + "/manifest.tsv"))).append('\n');
        receipt.append("text_manifest_sha256\t").append(sha256(resource(
                "/com/synexia/rewrite/hash-pinned-text/"
                        + M3JavaUtilSortsRecipe.CRATE + "/manifest.tsv"))).append('\n');
        receipt.append("java_runtime\t").append(Runtime.version()).append('\n');
        receipt.append("mode\t").append(write ? "apply" : "dry-run").append('\n');
        receipt.append("scope\tSOURCE_APPLICATION_ONLY\nfixed_point_changed_count\t0\n");
        receipt.append("target_tree_sha256\t").append(rootSha).append('\n');
        receipt.append("kind\tpath\tpreimage\tobserved_before\tpostimage\tchanged\n");
        Set<String> changed = Set.copyOf(first.changed());
        for (Target target : targets) {
            receipt.append(target.java() ? "JAVA" : "TEXT").append('\t').append(target.path())
                    .append('\t').append(target.before()).append('\t')
                    .append(observed.get(target.path())).append('\t').append(target.after())
                    .append('\t').append(changed.contains(target.path())).append('\n');
        }
        return new Application(targets.size(), first.changed(), rootSha, receipt.toString());
    }

    static List<Target> targets() {
        String crate = M3JavaUtilSortsRecipe.CRATE;
        var java = new M3HashPinnedJavaSnapshotRecipe(crate);
        var text = new M3HashPinnedTextSnapshotRecipe(crate);
        // The existing engines own manifest format, path admission and template hashes.
        var context = new InMemoryExecutionContext(error -> {
            throw new IllegalStateException("Manifest admission failed", error);
        });
        java.getInitialValue(context);
        text.getInitialValue(context);
        List<Target> targets = new ArrayList<>();
        readManifest(true, java.targetPaths(), targets);
        readManifest(false, text.targetPaths(), targets);
        targets.sort(Comparator.comparing(Target::path));
        Set<String> paths = new HashSet<>();
        for (Target target : targets) {
            if (!paths.add(target.path()) || !target.before().equals("ABSENT")) {
                throw new IllegalStateException(
                        "This additive task requires disjoint ABSENT targets: " + target.path());
            }
        }
        return List.copyOf(targets);
    }

    private static void readManifest(
            boolean java, List<String> admittedPaths, List<Target> targets) {
        String root = "/com/synexia/rewrite/hash-pinned-" + (java ? "java/" : "text/")
                + M3JavaUtilSortsRecipe.CRATE + "/";
        String text = resource(root + "manifest.tsv");
        List<String> paths = new ArrayList<>();
        for (String line : text.lines().toList()) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] cells = line.split("\t", -1);
            targets.add(new Target(java, cells[0], cells[1], cells[2]));
            paths.add(cells[0]);
        }
        if (!paths.equals(admittedPaths)) {
            throw new IllegalStateException("Manifest paths diverged from canonical admission");
        }
    }

    private static List<SourceFile> readTargets(
            Path root, List<Target> targets, Map<String, String> observed) throws IOException {
        List<Parser.Input> inputs = new ArrayList<>();
        List<SourceFile> sources = new ArrayList<>();
        Map<String, String> javaTexts = new TreeMap<>();
        for (Target target : targets) {
            Path file = safePath(root, target.path());
            if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
                observed.put(target.path(), "ABSENT");
                continue;
            }
            String text = decode(Files.readAllBytes(file));
            String digest = sha256(text);
            observed.put(target.path(), digest);
            if (!digest.equals(target.after())) {
                throw new IllegalStateException("Foreign occupant or source drift: " + target.path());
            }
            if (target.java()) {
                javaTexts.put(target.path(), text);
                inputs.add(Parser.Input.fromString(Path.of(target.path()), text));
            } else {
                sources.add(PlainText.builder().sourcePath(Path.of(target.path())).text(text).build());
            }
        }
        if (!inputs.isEmpty()) {
            List<Throwable> failures = new ArrayList<>();
            var context = new InMemoryExecutionContext(failures::add);
            List<SourceFile> parsed = JavaParser.fromJavaVersion().build()
                    .parseInputs(inputs, null, context).toList();
            failOnErrors(failures);
            if (parsed.size() != inputs.size()) {
                throw new IllegalStateException("Incomplete Java input parse");
            }
            for (SourceFile source : parsed) {
                if (!(source instanceof J.CompilationUnit)
                        || !source.printAll().equals(javaTexts.get(normalized(source)))) {
                    throw new IllegalStateException("Java parse/format drift: " + normalized(source));
                }
            }
            sources.addAll(parsed);
        }
        return List.copyOf(byPath(sources).values());
    }

    static Evaluation evaluate(Recipe recipe, List<SourceFile> before) {
        Map<String, SourceFile> result = byPath(before);
        List<Throwable> failures = new ArrayList<>();
        var context = new InMemoryExecutionContext(failures::add);
        var run = recipe.run(new InMemoryLargeSourceSet(before), context, 1);
        failOnErrors(failures);
        List<String> changed = new ArrayList<>();
        for (var change : run.getChangeset().getAllResults()) {
            SourceFile after = change.getAfter();
            if (after == null || (change.getBefore() != null
                    && !normalized(after).equals(normalized(change.getBefore())))) {
                throw new IllegalStateException("Unexpected removal or rename");
            }
            String path = normalized(after);
            if (changed.contains(path)) throw new IllegalStateException("Duplicate result: " + path);
            result.put(path, after);
            changed.add(path);
        }
        changed.sort(String::compareTo);
        return new Evaluation(List.copyOf(result.values()), List.copyOf(changed));
    }

    static Map<String, SourceFile> byPath(List<SourceFile> sources) {
        Map<String, SourceFile> result = new TreeMap<>();
        for (SourceFile source : sources) {
            String path = normalized(source);
            if (result.putIfAbsent(path, source) != null) {
                throw new IllegalStateException("Duplicate input: " + path);
            }
        }
        return result;
    }

    private static String normalized(SourceFile source) {
        return source.getSourcePath().normalize().toString().replace('\\', '/');
    }

    private static Path safePath(Path root, String relative) throws IOException {
        Path path = Path.of(relative);
        if (path.isAbsolute() || !path.equals(path.normalize()) || relative.indexOf('\\') >= 0) {
            throw new IllegalStateException("Unsafe target path: " + relative);
        }
        Path current = root;
        for (Path segment : path) {
            if (segment.toString().equals(".git")) {
                throw new IllegalStateException("Git metadata is outside target scope");
            }
            current = current.resolve(segment);
            if (Files.isSymbolicLink(current)) {
                throw new IllegalStateException("Symlink in target path: " + relative);
            }
            if (!current.equals(root.resolve(path))
                    && Files.exists(current, LinkOption.NOFOLLOW_LINKS)
                    && !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalStateException("Non-directory target ancestor: " + relative);
            }
        }
        if (!current.startsWith(root)
                || (Files.exists(current, LinkOption.NOFOLLOW_LINKS)
                    && !Files.isRegularFile(current, LinkOption.NOFOLLOW_LINKS))) {
            throw new IllegalStateException("Target is not a regular file: " + relative);
        }
        return current;
    }

    private static String fileDigest(Path file) throws IOException {
        return sha256(decode(Files.readAllBytes(file)));
    }

    private static String resource(String name) {
        try (var stream = M3JavaUtilSortsApply.class.getResourceAsStream(name)) {
            if (stream == null) throw new IllegalStateException("Missing task resource: " + name);
            return decode(stream.readAllBytes());
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot read task resource: " + name, failure);
        }
    }

    private static String decode(byte[] bytes) throws IOException {
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString();
    }

    static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static void failOnErrors(List<Throwable> failures) {
        if (!failures.isEmpty()) {
            throw new IllegalStateException("OpenRewrite refused candidate: "
                    + failures.getFirst().getMessage(), failures.getFirst());
        }
    }

    private static String targetHead(Path root) throws Exception {
        Process process = new ProcessBuilder("git", "rev-parse", "--verify", "HEAD^{commit}")
                .directory(root.toFile()).redirectErrorStream(true).start();
        if (!process.waitFor(15, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("Git base check timed out");
        }
        String actual = decode(process.getInputStream().readAllBytes()).strip();
        if (process.exitValue() != 0 || !actual.matches("[0-9a-f]{40}")) {
            throw new IllegalStateException("Cannot identify target Git HEAD: " + actual);
        }
        return actual;
    }
}
