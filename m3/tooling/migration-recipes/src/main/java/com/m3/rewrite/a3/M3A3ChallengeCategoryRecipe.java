// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.a3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

/**
 * Source-sealed A3 challenge-category evidence join.
 *
 * <p>The challenge catalogue is evidence-only. This recipe cannot authorize target-source mutation
 * from external solution bodies, donor copying, or canonical promotion.</p>
 */
public final class M3A3ChallengeCategoryRecipe
        extends ScanningRecipe<M3A3ChallengeCategoryRecipe.Inventory> {
    static final String PLAN =
            "m3/tooling/a3/src/main/java/com/m3/a3/A3Plan.java";
    static final String TEST =
            "m3/tooling/a3/src/test/java/com/m3/a3/A3PlanTest.java";
    static final String CATALOGUE =
            "m3/backports/CHALLENGE_CATEGORY_EVIDENCE.tsv";
    static final String DOC = "m3/docs/a3.md";

    private static final String ROOT = "/com/m3/rewrite/a3/challenge-category/";

    private static final Map<String, Target> TARGETS =
            Map.of(
                    PLAN,
                    Target.javaTarget(
                            "19b90c3f9064d55d28a1d2dbb9395ac0aefac966",
                            "8a3df20715ea9c1372146f0c56e73b8daabe8619",
                            "A3Plan.java.before",
                            "A3Plan.java.after"),
                    TEST,
                    Target.javaTarget(
                            "3e186021ae1b8997f5bb01ddb08f9b32bf5ae7a9",
                            "200365eeffcf3578d7b7c19f41076992a894c2df",
                            "A3PlanTest.java.before",
                            "A3PlanTest.java.after"),
                    CATALOGUE,
                    Target.textTarget(
                            "ABSENT",
                            "89bbe2d551196579c3126587366bb8499ae780d0",
                            null,
                            "CHALLENGE_CATEGORY_EVIDENCE.tsv.after"),
                    DOC,
                    Target.textTarget(
                            "fd701b718e33b7ae4e4b09362029ff090e0bdc67",
                            "075316c1fcfb692af366e5e287d70107f8ad6527",
                            "a3.md.before",
                            "a3.md.after"));

    static final class Inventory {
        private final Map<String, String> seen = new HashMap<>();
        private final List<String> conflicts = new ArrayList<>();

        synchronized void observe(String path, String hash) {
            if (seen.putIfAbsent(path, hash) != null) {
                conflicts.add("duplicate A3 challenge target: " + path);
            }
        }

        synchronized String hash(String path) {
            return seen.get(path);
        }

        synchronized boolean has(String path) {
            return seen.containsKey(path);
        }

        synchronized void requireAdmissible() {
            for (Map.Entry<String, Target> entry : TARGETS.entrySet()) {
                if (!"ABSENT".equals(entry.getValue().before())
                        && !seen.containsKey(entry.getKey())) {
                    throw new IllegalStateException(
                            "required A3 challenge target missing: " + entry.getKey());
                }
            }
            if (!conflicts.isEmpty()) {
                throw new IllegalStateException(String.join("; ", conflicts));
            }
        }
    }

    private enum Kind {
        JAVA,
        TEXT
    }

    private record Target(
            String before, String after, String beforeResource, String afterResource, Kind kind) {
        static Target javaTarget(
                String before, String after, String beforeResource, String afterResource) {
            return new Target(before, after, beforeResource, afterResource, Kind.JAVA);
        }

        static Target textTarget(
                String before, String after, String beforeResource, String afterResource) {
            return new Target(before, after, beforeResource, afterResource, Kind.TEXT);
        }
    }

    @Override
    public String getDisplayName() {
        return "Join A3 challenge category evidence";
    }

    @Override
    public String getDescription() {
        return "Binds LeetCode, HackerRank and GeeksforGeeks category evidence to A3 REVIEW rows "
                + "without solution-copy or promotion authority.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "a3",
                "challenge-catalogue",
                "recipe-first",
                "evidence-only",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public Inventory getInitialValue(ExecutionContext context) {
        for (Target target : TARGETS.values()) {
            if (!target.after().equals(gitBlob(resource(target.afterResource())))) {
                throw new IllegalStateException("A3 challenge postimage resource drift");
            }
            if (!"ABSENT".equals(target.before())
                    && !target.before().equals(gitBlob(resource(target.beforeResource())))) {
                throw new IllegalStateException("A3 challenge preimage resource drift");
            }
        }
        return new Inventory();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Inventory inventory) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof SourceFile source)) return tree;
                stopAfterPreVisit();
                String path = canonical(source.getSourcePath());
                Target target = TARGETS.get(path);
                if (target == null) return tree;
                requireKind(path, target, source);
                String hash = gitBlob(source.printAll());
                if (!hash.equals(target.before()) && !hash.equals(target.after())) {
                    inventory.conflicts.add("A3 challenge source drift: " + path);
                }
                inventory.observe(path, hash);
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(
            Inventory inventory, ExecutionContext context) {
        inventory.requireAdmissible();
        List<SourceFile> generated = new ArrayList<>();
        for (Map.Entry<String, Target> entry : TARGETS.entrySet()) {
            if (!inventory.has(entry.getKey())) {
                if (!"ABSENT".equals(entry.getValue().before())) {
                    throw new IllegalStateException("missing required A3 challenge preimage");
                }
                generated.add(parse(entry.getKey(), entry.getValue(), context));
            }
        }
        return List.copyOf(generated);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Inventory inventory) {
        inventory.requireAdmissible();
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof SourceFile source)) return tree;
                stopAfterPreVisit();
                String path = canonical(source.getSourcePath());
                Target target = TARGETS.get(path);
                if (target == null) return tree;
                String scanned = inventory.hash(path);
                String current = gitBlob(source.printAll());
                if (!Objects.equals(scanned, current)) {
                    throw new IllegalStateException("A3 challenge target moved after scan: " + path);
                }
                if (current.equals(target.after())) return tree;
                if (!current.equals(target.before())) {
                    throw new IllegalStateException("A3 challenge target is not exact preimage: " + path);
                }
                SourceFile replacement = parse(path, target, context);
                return replacement
                        .withId(source.getId())
                        .withSourcePath(source.getSourcePath())
                        .withMarkers(source.getMarkers())
                        .withFileAttributes(source.getFileAttributes())
                        .withCharset(source.getCharset())
                        .withCharsetBomMarked(source.isCharsetBomMarked())
                        .withChecksum(null);
            }
        };
    }

    public boolean challengeSolutionCopyAuthority() {
        return false;
    }

    public boolean sourceMutationAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }

    static String resource(String name) {
        if (name == null) throw new IllegalArgumentException("resource");
        try (InputStream input =
                M3A3ChallengeCategoryRecipe.class.getResourceAsStream(ROOT + name)) {
            if (input == null) {
                throw new IllegalStateException("missing A3 challenge resource: " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read A3 challenge resource", failure);
        }
    }

    static String gitBlob(String text) {
        byte[] bytes = Objects.requireNonNull(text, "text").getBytes(StandardCharsets.UTF_8);
        byte[] prefix = ("blob " + bytes.length + "\0").getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(prefix);
            digest.update(bytes);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static SourceFile parse(
            String path, Target target, ExecutionContext context) {
        String text = resource(target.afterResource());
        if (target.kind() == Kind.TEXT) {
            return PlainTextParser.builder()
                    .build()
                    .parse(text)
                    .findFirst()
                    .orElseThrow()
                    .withSourcePath(Path.of(path));
        }
        try (var parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(Parser.Input.fromString(Path.of(path), text)),
                                null,
                                context)) {
            List<SourceFile> files = parsed.toList();
            if (files.size() != 1
                    || !(files.getFirst() instanceof J.CompilationUnit)
                    || !text.equals(files.getFirst().printAll())) {
                throw new IllegalStateException("A3 challenge Java roundtrip failed: " + path);
            }
            return files.getFirst().withSourcePath(Path.of(path));
        }
    }

    private static void requireKind(String path, Target target, SourceFile source) {
        if (target.kind() == Kind.JAVA && !(source instanceof J.CompilationUnit)) {
            throw new IllegalStateException("A3 challenge Java target is not Java: " + path);
        }
        if (target.kind() == Kind.TEXT && !(source instanceof PlainText)) {
            throw new IllegalStateException("A3 challenge text target is not PlainText: " + path);
        }
    }

    private static String canonical(Path path) {
        String value = path.normalize().toString().replace('\\', '/');
        if (TARGETS.containsKey(value)) return value;
        if (value.startsWith("src/main/java/com/m3/a3/")
                || value.startsWith("src/test/java/com/m3/a3/")) {
            return "m3/tooling/a3/" + value;
        }
        return value;
    }
}
