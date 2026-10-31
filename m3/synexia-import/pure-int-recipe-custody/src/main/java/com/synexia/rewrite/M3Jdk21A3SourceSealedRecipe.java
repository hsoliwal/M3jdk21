// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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

/**
 * Canonical Synexia source-sealed replay atom for M3JDK21 A3 tooling packets.
 *
 * <p>The target repository owns only a compatibility receiver facade. Exact preimage/postimage
 * custody, duplicate/drift refusal, additive target generation and second-pass fixed-point
 * mechanics live here in Synexia.</p>
 */
abstract class M3Jdk21A3SourceSealedRecipe
        extends ScanningRecipe<M3Jdk21A3SourceSealedRecipe.Inventory> {

    record Target(
            String before,
            String after,
            String beforeResource,
            String afterResource,
            boolean required) {
        Target {
            before = hashOrAbsent(before, "before");
            after = hashOrAbsent(after, "after");
            beforeResource = optionalResource(beforeResource);
            afterResource = requiredResource(afterResource);
            if ("ABSENT".equals(after)) {
                throw new IllegalArgumentException("A3 target removal is not admitted by this packet");
            }
            if (!"ABSENT".equals(before) && beforeResource == null) {
                throw new IllegalArgumentException("non-additive target requires before resource");
            }
        }

        boolean addition() {
            return "ABSENT".equals(before);
        }
    }

    static final class Inventory {
        private final Map<String, String> seen = new LinkedHashMap<>();
        private final Set<String> generated = new java.util.HashSet<>();
        private final List<String> conflicts = new ArrayList<>();
        private boolean active;
    }

    protected abstract String resourceRoot();

    protected abstract Map<String, Target> targetManifest();

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public Inventory getInitialValue(ExecutionContext context) {
        targetManifest().forEach(this::requireResources);
        return new Inventory();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Inventory inventory) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof SourceFile file)) {
                    return tree;
                }
                stopAfterPreVisit();
                String path = canonical(file.getSourcePath());
                Target target = targetManifest().get(path);
                if (target == null) {
                    return tree;
                }
                observe(inventory, path, target, file);
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(
            Inventory inventory,
            ExecutionContext context) {
        if (!inventory.active) {
            return List.of();
        }
        requireAdmissible(inventory);
        ArrayList<SourceFile> generated = new ArrayList<>();
        targetManifest().forEach(
                (path, target) -> {
                    if (!inventory.seen.containsKey(path)) {
                        if (!target.addition()) {
                            throw new IllegalStateException(
                                    "required A3 preimage missing: " + path);
                        }
                        generated.add(parse(path, target.afterResource(), context));
                        inventory.generated.add(path);
                    }
                });
        return List.copyOf(generated);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Inventory inventory) {
        if (!inventory.active) {
            return TreeVisitor.noop();
        }
        requireAdmissible(inventory);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof SourceFile file)) {
                    return tree;
                }
                stopAfterPreVisit();
                String path = canonical(file.getSourcePath());
                Target target = targetManifest().get(path);
                if (target == null) {
                    return tree;
                }
                return replace(inventory, path, target, file, context);
            }
        };
    }

    public final boolean targetSourceMutationAuthority() {
        return false;
    }

    public final boolean productSourceMutationAuthority() {
        return false;
    }

    public final boolean challengeSourceCopyAuthority() {
        return false;
    }

    public final boolean donorSourceCopyAuthority() {
        return false;
    }

    public final boolean promotionAuthority() {
        return false;
    }

    final String sourceImage(String relative) {
        return resource(relative);
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
            throw new IllegalStateException("SHA-1 unavailable", impossible);
        }
    }

    private void observe(
            Inventory inventory,
            String path,
            Target target,
            SourceFile file) {
        inventory.active = true;
        String current = gitBlob(file.printAll());
        if (inventory.seen.putIfAbsent(path, current) != null) {
            inventory.conflicts.add("duplicate A3 target: " + path);
        }
        if (!(file instanceof J.CompilationUnit)) {
            inventory.conflicts.add("A3 target is not Java: " + path);
        }
        if (!current.equals(target.before()) && !current.equals(target.after())) {
            inventory.conflicts.add("A3 source drift: " + path);
        }
    }

    private SourceFile replace(
            Inventory inventory,
            String path,
            Target target,
            SourceFile file,
            ExecutionContext context) {
        String scanned = inventory.seen.get(path);
        String current = gitBlob(file.printAll());
        boolean matchesManifest =
                current.equals(target.before()) || current.equals(target.after());
        boolean matchesScan =
                scanned == null
                        ? inventory.generated.contains(path)
                                && target.addition()
                                && current.equals(target.after())
                        : current.equals(scanned);
        if (!(file instanceof J.CompilationUnit) || !matchesManifest || !matchesScan) {
            throw new IllegalStateException("A3 target changed after scan: " + path);
        }
        if (current.equals(target.after())) {
            return file;
        }
        SourceFile candidate = parse(path, target.afterResource(), context);
        candidate = candidate.<SourceFile>withId(file.getId());
        candidate = candidate.<SourceFile>withSourcePath(file.getSourcePath());
        candidate = candidate.<SourceFile>withMarkers(file.getMarkers());
        candidate = candidate.<SourceFile>withFileAttributes(file.getFileAttributes());
        candidate = candidate.<SourceFile>withCharset(file.getCharset());
        candidate = candidate.<SourceFile>withCharsetBomMarked(file.isCharsetBomMarked());
        return candidate.<SourceFile>withChecksum(null);
    }

    private void requireAdmissible(Inventory inventory) {
        if (!inventory.conflicts.isEmpty()) {
            throw new IllegalStateException(String.join("; ", inventory.conflicts));
        }
        targetManifest().forEach(
                (path, target) -> {
                    if (target.required() && !inventory.seen.containsKey(path)) {
                        throw new IllegalStateException(
                                "required A3 owner missing: " + path);
                    }
                });
    }

    private SourceFile parse(
            String path,
            String resource,
            ExecutionContext context) {
        String text = resource(resource);
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
                throw new IllegalStateException(
                        "A3 reviewed postimage did not Java-roundtrip: " + path);
            }
            return files.getFirst();
        }
    }

    private void requireResources(String path, Target target) {
        if (!target.after().equals(gitBlob(resource(target.afterResource())))) {
            throw new IllegalStateException("A3 reviewed resource drift: " + path);
        }
        if (target.beforeResource() != null
                && !target.before().equals(gitBlob(resource(target.beforeResource())))) {
            throw new IllegalStateException("A3 preimage resource drift: " + path);
        }
    }

    private static String canonical(Path path) {
        String value = path.normalize().toString().replace('\\', '/');
        String prefix = "m3/tooling/a3/";
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    private String resource(String relative) {
        String root = resourceRoot();
        if (root == null || !root.startsWith("/") || !root.endsWith("/")) {
            throw new IllegalStateException("invalid A3 recipe resource root");
        }
        try (InputStream input =
                M3Jdk21A3SourceSealedRecipe.class.getResourceAsStream(root + relative)) {
            if (input == null) {
                throw new IllegalStateException("missing A3 reviewed resource: " + relative);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read A3 reviewed resource: " + relative,
                    failure);
        }
    }

    private static String hashOrAbsent(String value, String field) {
        String checked = Objects.requireNonNull(value, field);
        if ("ABSENT".equals(checked)) {
            return checked;
        }
        if (!checked.matches("[0-9a-f]{40}")) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String optionalResource(String value) {
        return value == null ? null : requiredResource(value);
    }

    private static String requiredResource(String value) {
        String checked = Objects.requireNonNull(value, "resource");
        if (checked.isBlank()
                || checked.startsWith("/")
                || checked.contains("..")
                || checked.indexOf('\\') >= 0
                || checked.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("resource");
        }
        return checked;
    }
}
