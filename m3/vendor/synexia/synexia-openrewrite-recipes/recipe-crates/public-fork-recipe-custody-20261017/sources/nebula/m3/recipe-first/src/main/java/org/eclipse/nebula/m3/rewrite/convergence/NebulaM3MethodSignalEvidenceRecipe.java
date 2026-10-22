// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

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
 * Source-sealed installer for Nebula method-signal evidence.
 *
 * <p>The recipe changes only the M3 control plane. Product/widget source, native source and public
 * APIs are outside its target set. Existing file-level evidence remains intact; reviewed
 * postimages add method-coordinate evidence and reuse the established donor/JNI policies.</p>
 */
public final class NebulaM3MethodSignalEvidenceRecipe
        extends ScanningRecipe<NebulaM3MethodSignalEvidenceRecipe.Inventory> {

    private static final String ROOT =
            "/org/eclipse/nebula/m3/rewrite/method-signal-evidence/";
    private static final Map<String, NebulaM3MethodSignalEvidenceManifest.Target> TARGETS =
            NebulaM3MethodSignalEvidenceManifest.targets();

    public static final class Inventory {
        private final Map<String, String> seen = new LinkedHashMap<>();
        private final List<String> conflicts = new ArrayList<>();
        private boolean active;
    }

    @Override
    public String getDisplayName() {
        return "Install Nebula M3 method-signal evidence";
    }

    @Override
    public String getDescription() {
        return "Adds method-coordinate structural/search/JNI evidence to the existing Nebula M3 "
                + "inventory and review policies without changing product source.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "nebula",
                "method-atom",
                "inventory-first",
                "fast-search",
                "java-before-jni",
                "recipe-first",
                "read-only-evidence",
                "contract-preserving");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public Inventory getInitialValue(ExecutionContext context) {
        TARGETS.forEach(NebulaM3MethodSignalEvidenceRecipe::requireResources);
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
                NebulaM3MethodSignalEvidenceManifest.Target target = TARGETS.get(path);
                if (target == null) return tree;
                inventory.active = true;
                String hash = gitBlob(source.printAll());
                if (inventory.seen.putIfAbsent(path, hash) != null) {
                    inventory.conflicts.add("duplicate method-signal target: " + path);
                }
                if (!(source instanceof J.CompilationUnit)) {
                    inventory.conflicts.add("method-signal target is not Java: " + path);
                }
                if (!hash.equals(target.before()) && !hash.equals(target.after())) {
                    inventory.conflicts.add("method-signal source drift: " + path);
                }
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
                                    "required method-signal preimage missing: " + path);
                        }
                        generated.add(parse(path, target.afterResource(), context));
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
                if (!(tree instanceof SourceFile source)) return tree;
                stopAfterPreVisit();
                String path = canonical(source.getSourcePath());
                NebulaM3MethodSignalEvidenceManifest.Target target = TARGETS.get(path);
                if (target == null) return tree;
                String current = gitBlob(source.printAll());
                // already at the reviewed postimage (including files generated in this cycle)
                if (current.equals(target.after())) return source;
                String scanned = inventory.seen.get(path);
                if (scanned == null
                        || !current.equals(scanned)
                        || !current.equals(target.before())) {
                    throw new IllegalStateException(
                            "method-signal target changed after scan: " + path);
                }
                SourceFile candidate = parse(path, target.afterResource(), context);
                SourceFile replacement = candidate.withId(source.getId());
                replacement = replacement.withSourcePath(source.getSourcePath());
                replacement = replacement.withMarkers(source.getMarkers());
                replacement = replacement.withFileAttributes(source.getFileAttributes());
                replacement = replacement.withCharset(source.getCharset());
                replacement = replacement.withCharsetBomMarked(source.isCharsetBomMarked());
                return replacement.withChecksum(null);
            }
        };
    }

    public boolean productSourceMutationAuthority() {
        return false;
    }

    public boolean nativeExecutionAuthority() {
        return false;
    }

    public boolean donorSourceCopyAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }

    static Map<String, NebulaM3MethodSignalEvidenceManifest.Target> targetManifest() {
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

    private static void requireAdmissible(Inventory inventory) {
        if (!inventory.conflicts.isEmpty()) {
            throw new IllegalStateException(String.join("; ", inventory.conflicts));
        }
        TARGETS.forEach(
                (path, target) -> {
                    if (target.required() && !inventory.seen.containsKey(path)) {
                        throw new IllegalStateException(
                                "required method-signal owner missing: " + path);
                    }
                });
    }

    private static void requireResources(
            String path,
            NebulaM3MethodSignalEvidenceManifest.Target target) {
        if (!target.after().equals(gitBlob(resource(target.afterResource())))) {
            throw new IllegalStateException("method-signal postimage drift: " + path);
        }
        if (target.beforeResource() != null
                && !target.before().equals(gitBlob(resource(target.beforeResource())))) {
            throw new IllegalStateException("method-signal preimage drift: " + path);
        }
    }

    private static SourceFile parse(
            String path,
            String resource,
            ExecutionContext context) {
        String body = resource(resource);
        try (var parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(Parser.Input.fromString(Path.of(path), body)),
                                null,
                                context)) {
            List<SourceFile> files = parsed.toList();
            if (files.size() != 1
                    || !(files.getFirst() instanceof J.CompilationUnit)
                    || !body.equals(files.getFirst().printAll())) {
                throw new IllegalStateException(
                        "method-signal Java roundtrip failed: " + path);
            }
            return files.getFirst();
        }
    }

    private static String canonical(Path path) {
        String value = path.normalize().toString().replace('\\', '/');
        String prefix = "m3/recipe-first/";
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    private static String resource(String relative) {
        try (InputStream input =
                NebulaM3MethodSignalEvidenceRecipe.class.getResourceAsStream(
                        ROOT + relative)) {
            if (input == null) {
                throw new IllegalStateException(
                        "missing method-signal resource: " + relative);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read method-signal resource: " + relative,
                    failure);
        }
    }
}
