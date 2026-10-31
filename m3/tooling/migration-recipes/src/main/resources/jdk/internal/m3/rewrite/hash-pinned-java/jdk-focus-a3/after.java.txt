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
import java.util.HexFormat;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

/** Source-sealed recipe for the A3 algorithm catalogue bridge. */
public final class M3A3AlgorithmCatalogueRecipe
        extends ScanningRecipe<M3A3AlgorithmCatalogueRecipe.Inventory> {

    private static final String ROOT =
            "/com/m3/rewrite/a3-algorithm-catalogue/";
    private static final Map<String, M3A3AlgorithmCatalogueManifest.Target> TARGETS =
            M3A3AlgorithmCatalogueManifest.targets();

    public static final class Inventory {
        private final Map<String, String> seen = new LinkedHashMap<>();
        private final Set<String> generated = new HashSet<>();
        private final List<String> conflicts = new ArrayList<>();
        private boolean active;
    }

    @Override
    public String getDisplayName() {
        return "Install A3 algorithm catalogue bridge";
    }

    @Override
    public String getDescription() {
        return "Installs the compact A3Alg validator and ALG planning join from exact current "
                + "A3 preimages and reviewed additive postimages.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "a3",
                "algorithm",
                "catalogue",
                "donor",
                "recipe-first",
                "file-local",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public Inventory getInitialValue(ExecutionContext context) {
        TARGETS.forEach(M3A3AlgorithmCatalogueRecipe::requireResources);
        return new Inventory();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Inventory inventory) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof SourceFile file)) return tree;
                stopAfterPreVisit();
                String path = canonical(file.getSourcePath());
                M3A3AlgorithmCatalogueManifest.Target target = TARGETS.get(path);
                if (target == null) return tree;
                observe(inventory, path, target, file);
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(
            Inventory inventory,
            ExecutionContext context) {
        if (!inventory.active) return List.of();
        requireAdmissible(inventory);
        ArrayList<SourceFile> generated = new ArrayList<>();
        TARGETS.forEach(
                (path, target) -> {
                    if (!inventory.seen.containsKey(path)) {
                        if (!"ABSENT".equals(target.before())) {
                            throw new IllegalStateException(
                                    "required A3Alg preimage missing: " + path);
                        }
                        generated.add(parse(path, target.afterResource(), context));
                        inventory.generated.add(path);
                    }
                });
        return List.copyOf(generated);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Inventory inventory) {
        if (!inventory.active) return new TreeVisitor<Tree, ExecutionContext>() {};
        requireAdmissible(inventory);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof SourceFile file)) return tree;
                stopAfterPreVisit();
                String path = canonical(file.getSourcePath());
                M3A3AlgorithmCatalogueManifest.Target target = TARGETS.get(path);
                if (target == null) return tree;
                return replace(inventory, path, target, file, context);
            }
        };
    }

    public boolean targetSourceMutationAuthority() {
        return false;
    }

    public boolean productSourceMutationAuthority() {
        return false;
    }

    public boolean challengeSourceCopyAuthority() {
        return false;
    }

    public boolean donorSourceCopyAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }

    static Map<String, M3A3AlgorithmCatalogueManifest.Target> targetManifest() {
        return TARGETS;
    }

    static String sourceImage(String relative) {
        return resource(relative);
    }

    static String gitBlob(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] prefix = ("blob " + bytes.length + "\0").getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(prefix);
            digest.update(bytes);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-1 unavailable", impossible);
        }
    }

    private static void observe(
            Inventory inventory,
            String path,
            M3A3AlgorithmCatalogueManifest.Target target,
            SourceFile file) {
        inventory.active = true;
        String current = gitBlob(file.printAll());
        if (inventory.seen.putIfAbsent(path, current) != null) {
            inventory.conflicts.add("duplicate A3Alg target: " + path);
        }
        if (!(file instanceof J.CompilationUnit)) {
            inventory.conflicts.add("A3Alg target is not Java: " + path);
        }
        if (!current.equals(target.before()) && !current.equals(target.after())) {
            inventory.conflicts.add("A3Alg source drift: " + path);
        }
    }

    private static SourceFile replace(
            Inventory inventory,
            String path,
            M3A3AlgorithmCatalogueManifest.Target target,
            SourceFile file,
            ExecutionContext context) {
        String scanned = inventory.seen.get(path);
        String current = gitBlob(file.printAll());
        boolean matchesScan = scanned == null
                ? inventory.generated.contains(path)
                        && "ABSENT".equals(target.before())
                        && current.equals(target.after())
                : current.equals(scanned);
        if (!matchesScan
                || (!current.equals(target.before())
                        && !current.equals(target.after()))) {
            throw new IllegalStateException("A3Alg target changed after scan: " + path);
        }
        if (current.equals(target.after())) return file;
        SourceFile candidate = parse(path, target.afterResource(), context);
        candidate = candidate.withId(file.getId());
        candidate = candidate.withSourcePath(file.getSourcePath());
        candidate = candidate.withMarkers(file.getMarkers());
        candidate = candidate.withFileAttributes(file.getFileAttributes());
        candidate = candidate.withCharset(file.getCharset());
        candidate = candidate.withCharsetBomMarked(file.isCharsetBomMarked());
        return candidate.withChecksum(null);
    }

    private static void requireAdmissible(Inventory inventory) {
        if (!inventory.conflicts.isEmpty()) {
            throw new IllegalStateException(String.join("; ", inventory.conflicts));
        }
        TARGETS.forEach(
                (path, target) -> {
                    if (target.required() && !inventory.seen.containsKey(path)) {
                        throw new IllegalStateException(
                                "required A3Alg owner missing: " + path);
                    }
                });
    }

    private static SourceFile parse(
            String path,
            String resource,
            ExecutionContext context) {
        String text = resource(resource);
        try (var parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(
                                        Parser.Input.fromString(
                                                Path.of(path), text)),
                                null,
                                context)) {
            List<SourceFile> files = parsed.toList();
            if (files.size() != 1
                    || !(files.getFirst() instanceof J.CompilationUnit)
                    || !text.equals(files.getFirst().printAll())) {
                throw new IllegalStateException(
                        "A3Alg reviewed postimage did not Java-roundtrip: " + path);
            }
            return files.getFirst();
        }
    }

    private static void requireResources(
            String path,
            M3A3AlgorithmCatalogueManifest.Target target) {
        if (!target.after().equals(gitBlob(resource(target.afterResource())))) {
            throw new IllegalStateException("A3Alg reviewed resource drift: " + path);
        }
        if (target.beforeResource() != null
                && !target.before().equals(
                        gitBlob(resource(target.beforeResource())))) {
            throw new IllegalStateException("A3Alg preimage resource drift: " + path);
        }
    }

    private static String canonical(Path path) {
        String value = path.normalize().toString().replace('\\', '/');
        String prefix = "m3/tooling/a3/";
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    private static String resource(String relative) {
        try (InputStream input =
                M3A3AlgorithmCatalogueRecipe.class.getResourceAsStream(
                        ROOT + relative)) {
            if (input == null) {
                throw new IllegalStateException(
                        "missing A3Alg reviewed resource: " + relative);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read A3Alg reviewed resource: " + relative,
                    failure);
        }
    }
}
