// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.a3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashMap;
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
 * Source-sealed A3 complete-denominator upgrade.
 *
 * <p>The recipe owns exactly four tool-plane files in the A3 Maven module. It never targets OpenJDK
 * product source. Exact preimages and reviewed postimages are Git-blob pinned; drift, duplicate
 * owners and missing required targets fail closed. Once materialized, a second application is a
 * fixed point.</p>
 */
public final class M3A3CompleteDenominatorRecipe
        extends ScanningRecipe<M3A3CompleteDenominatorRecipe.Inventory> {

    private static final String ROOT =
            "/com/m3/rewrite/a3-complete-denominator/";

    private enum Kind {
        JAVA,
        TEXT
    }

    private record Target(
            String path,
            String before,
            String after,
            Kind kind,
            String beforeResource,
            String afterResource) {}

    private static final List<Target> TARGETS =
            List.of(
                    new Target(
                            "pom.xml",
                            "65b98add24ba910fc9ad7a919b760495a5c5780d",
                            "f2ced8ad1d56984f655c38ff1915d5ccf62b67a9",
                            Kind.TEXT,
                            "before/pom.xml.txt",
                            "after/pom.xml.txt"),
                    new Target(
                            "src/main/java/com/m3/a3/A3.java",
                            "cc8473795b7023cce15a604b3c7164531f0e49fc",
                            "a6eeb7705008e0f8ac047906eeb3229cb29cd104",
                            Kind.JAVA,
                            "before/A3.java.txt",
                            "after/A3.java.txt"),
                    new Target(
                            "src/main/java/com/m3/a3/A3Plan.java",
                            "19b90c3f9064d55d28a1d2dbb9395ac0aefac966",
                            "23b3663b19efd37a0ecbba0b198d12c3326a6161",
                            Kind.JAVA,
                            "before/A3Plan.java.txt",
                            "after/A3Plan.java.txt"),
                    new Target(
                            "src/test/java/com/m3/a3/A3PlanTest.java",
                            "3e186021ae1b8997f5bb01ddb08f9b32bf5ae7a9",
                            "9c1b72b06303fe33a88db6c92373b8f3aae35c97",
                            Kind.JAVA,
                            "before/A3PlanTest.java.txt",
                            "after/A3PlanTest.java.txt"));

    public static final class Inventory {
        private final Map<String, String> seen = new LinkedHashMap<>();
        private boolean refused;
    }

    @Override
    public String getDisplayName() {
        return "M3 A3 complete released-change denominator";
    }

    @Override
    public String getDescription() {
        return "Upgrades A3 planning to consume the complete generated upstream change inventory "
                + "and the authority-free challenge/search taxonomy while preserving existing "
                + "JEP/JBS/community decisions.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "a3",
                "atomize",
                "patternize",
                "absorb",
                "complete-denominator",
                "recipe-first",
                "hash-pinned",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public Inventory getInitialValue(ExecutionContext context) {
        for (Target target : TARGETS) {
            requireImage(target, false);
            requireImage(target, true);
        }
        return new Inventory();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Inventory inventory) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof SourceFile source)) return tree;
                Target target = target(normalized(source.getSourcePath()));
                if (target == null) return tree;

                String hash = gitBlob(source.printAll());
                if (inventory.seen.putIfAbsent(target.path(), hash) != null
                        || !correctKind(source, target.kind())
                        || (!target.before().equals(hash) && !target.after().equals(hash))) {
                    inventory.refused = true;
                    throw new IllegalStateException(
                            "A3 complete-denominator target drift: " + target.path());
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(
            Inventory inventory, ExecutionContext context) {
        requireAdmissible(inventory);
        return List.of();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Inventory inventory) {
        requireAdmissible(inventory);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof SourceFile source)) return tree;
                Target target = target(normalized(source.getSourcePath()));
                if (target == null) return tree;

                String current = gitBlob(source.printAll());
                String scanned = inventory.seen.get(target.path());
                if (!current.equals(scanned)) {
                    throw new IllegalStateException(
                            "A3 complete-denominator target moved after scan: "
                                    + target.path());
                }
                if (current.equals(target.after())) return source;
                if (!current.equals(target.before())) {
                    throw new IllegalStateException(
                            "A3 complete-denominator preimage changed: "
                                    + target.path());
                }

                SourceFile replacement = parseAfter(target, context);
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

    public boolean jdkProductMutationAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }

    static List<String> targetPaths() {
        return TARGETS.stream().map(Target::path).toList();
    }

    static String beforeImage(String path) {
        Target target = requireTarget(path);
        return resource(target.beforeResource());
    }

    static String afterImage(String path) {
        Target target = requireTarget(path);
        return resource(target.afterResource());
    }

    static String beforeHash(String path) {
        return requireTarget(path).before();
    }

    static String afterHash(String path) {
        return requireTarget(path).after();
    }

    static boolean textTarget(String path) {
        return requireTarget(path).kind() == Kind.TEXT;
    }

    static String gitBlob(String text) {
        byte[] bytes =
                Objects.requireNonNull(text, "text")
                        .getBytes(StandardCharsets.UTF_8);
        byte[] prefix =
                ("blob " + bytes.length + "\0")
                        .getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(prefix);
            digest.update(bytes);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static void requireAdmissible(Inventory inventory) {
        if (inventory.refused || inventory.seen.size() != TARGETS.size()) {
            throw new IllegalStateException(
                    "A3 complete-denominator requires all exact tool-plane targets");
        }
        for (Target target : TARGETS) {
            if (!inventory.seen.containsKey(target.path())) {
                throw new IllegalStateException(
                        "missing A3 complete-denominator target: "
                                + target.path());
            }
        }
    }

    private static SourceFile parseAfter(
            Target target, ExecutionContext context) {
        String text = resource(target.afterResource());
        if (target.kind() == Kind.TEXT) {
            SourceFile parsed =
                    PlainTextParser.builder()
                            .build()
                            .parse(text)
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "A3 POM postimage did not parse"));
            return parsed.withSourcePath(Path.of(target.path()));
        }

        try (var parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(
                                        Parser.Input.fromString(
                                                Path.of(target.path()), text)),
                                null,
                                context)) {
            List<SourceFile> files = parsed.toList();
            if (files.size() != 1
                    || !(files.getFirst() instanceof J.CompilationUnit unit)
                    || !text.equals(unit.printAll())) {
                throw new IllegalStateException(
                        "A3 Java postimage roundtrip drift: "
                                + target.path());
            }
            return unit.withSourcePath(Path.of(target.path()));
        }
    }

    private static boolean correctKind(SourceFile source, Kind kind) {
        return kind == Kind.JAVA
                ? source instanceof J.CompilationUnit
                : source instanceof PlainText;
    }

    private static Target target(String path) {
        return TARGETS.stream()
                .filter(candidate -> candidate.path().equals(path))
                .findFirst()
                .orElse(null);
    }

    private static Target requireTarget(String path) {
        Target target = target(path);
        if (target == null) {
            throw new IllegalArgumentException(
                    "unowned A3 complete-denominator path: " + path);
        }
        return target;
    }

    private static String normalized(Path path) {
        String value = path.normalize().toString().replace('\\', '/');
        String prefix = "m3/tooling/a3/";
        return value.startsWith(prefix)
                ? value.substring(prefix.length())
                : value;
    }

    private static void requireImage(Target target, boolean after) {
        String image =
                resource(
                        after
                                ? target.afterResource()
                                : target.beforeResource());
        String expected = after ? target.after() : target.before();
        if (!expected.equals(gitBlob(image))) {
            throw new IllegalStateException(
                    "A3 complete-denominator resource drift: "
                            + target.path()
                            + (after ? " after" : " before"));
        }
    }

    private static String resource(String relative) {
        try (InputStream input =
                M3A3CompleteDenominatorRecipe.class.getResourceAsStream(
                        ROOT + relative)) {
            if (input == null) {
                throw new IllegalStateException(
                        "missing A3 complete-denominator resource: "
                                + relative);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read A3 complete-denominator resource: "
                            + relative,
                    failure);
        }
    }
}
