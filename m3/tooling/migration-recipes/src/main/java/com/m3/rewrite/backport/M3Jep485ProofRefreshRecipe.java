// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.text.PlainText;

/**
 * Source-sealed proof refresh for the already-materialized JEP 485 Stream Gatherers packet.
 *
 * <p>This recipe owns only the proof workflow and current-tree receipt. Gatherers product source
 * remains owned by {@link M3Jep485StreamGatherersBackportRecipe}; this recipe has no product-source
 * mutation or promotion authority.</p>
 */
public final class M3Jep485ProofRefreshRecipe
        extends ScanningRecipe<M3Jep485ProofRefreshRecipe.Inventory> {
    private static final String ROOT =
            "/com/m3/rewrite/backport/jep485-proof-refresh-20261013/";

    private static final Map<String, Target> TARGETS =
            Map.of(
                    ".github/workflows/m3-jep485-stream-gatherers.yml",
                    new Target(
                            "workflow.before.yml",
                            "workflow.after.yml",
                            "ba39126bba0ae88a6d8282f7bf1ff0c72208a094",
                            "c0336d5c48ceeafdf1d9c811e33ebb0fc4435027"),
                    "m3/backports/recipes/j485/CURRENT_TREE_RECEIPT.tsv",
                    new Target(
                            "receipt.before.tsv",
                            "receipt.after.tsv",
                            "7c8178213ab51cd152a3990db563c556728fd978",
                            "d44c29c8dc09d0d3b7fa1063e9f3c71d4a65c532"));

    static final class Inventory {
        private final java.util.Map<String, String> states = new java.util.HashMap<>();
    }

    private record Target(
            String beforeResource,
            String afterResource,
            String beforeBlob,
            String afterBlob) {}

    @Override
    public String getDisplayName() {
        return "Refresh JEP 485 current-master proof";
    }

    @Override
    public String getDescription() {
        return "Replays only the current-master Stream Gatherers proof workflow and receipt from "
                + "exact preimages while preserving the already-materialized Gatherers product tree.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "jep-485",
                "stream-gatherers",
                "proof-refresh",
                "hash-pinned",
                "plain-text",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public Inventory getInitialValue(ExecutionContext context) {
        TARGETS.forEach(
                (path, target) -> {
                    requireBlob(target.beforeResource(), target.beforeBlob());
                    requireBlob(target.afterResource(), target.afterBlob());
                });
        return new Inventory();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Inventory inventory) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof SourceFile source)) return tree;
                stopAfterPreVisit();
                String path = normalized(source.getSourcePath());
                Target target = TARGETS.get(path);
                if (target == null) return tree;
                if (!(source instanceof PlainText)) {
                    throw new IllegalStateException(
                            "JEP 485 proof target is not PlainText: " + path);
                }
                String blob = gitBlob(source.printAll());
                if (!blob.equals(target.beforeBlob()) && !blob.equals(target.afterBlob())) {
                    throw new IllegalStateException("JEP 485 proof target drift: " + path);
                }
                if (inventory.states.putIfAbsent(path, blob) != null) {
                    throw new IllegalStateException(
                            "duplicate JEP 485 proof target: " + path);
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(
            Inventory inventory, ExecutionContext context) {
        for (String path : TARGETS.keySet()) {
            if (!inventory.states.containsKey(path)) {
                throw new IllegalStateException("missing JEP 485 proof target: " + path);
            }
        }
        return List.of();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Inventory inventory) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof SourceFile source)) return tree;
                stopAfterPreVisit();
                String path = normalized(source.getSourcePath());
                Target target = TARGETS.get(path);
                if (target == null) return tree;
                String current = gitBlob(source.printAll());
                String scanned = inventory.states.get(path);
                if (!Objects.equals(current, scanned)) {
                    throw new IllegalStateException(
                            "JEP 485 proof target changed after scan: " + path);
                }
                if (current.equals(target.afterBlob())) return tree;
                if (!current.equals(target.beforeBlob())) {
                    throw new IllegalStateException(
                            "JEP 485 proof target preimage drift: " + path);
                }
                return PlainText.builder()
                        .sourcePath(source.getSourcePath())
                        .text(resource(target.afterResource()))
                        .build()
                        .withId(source.getId())
                        .withMarkers(source.getMarkers())
                        .withFileAttributes(source.getFileAttributes())
                        .withCharset(source.getCharset())
                        .withCharsetBomMarked(source.isCharsetBomMarked())
                        .withChecksum(null);
            }
        };
    }

    public boolean productSourceMutationAuthority() {
        return false;
    }

    public boolean publicApiMutationAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }

    static String before(String path) {
        return resource(requireTarget(path).beforeResource());
    }

    static String after(String path) {
        return resource(requireTarget(path).afterResource());
    }

    static Set<String> targetPaths() {
        return Set.copyOf(TARGETS.keySet());
    }

    private static Target requireTarget(String path) {
        Target target = TARGETS.get(path);
        if (target == null) throw new IllegalArgumentException(path);
        return target;
    }

    private static void requireBlob(String resource, String expected) {
        String actual = gitBlob(resource(resource));
        if (!actual.equals(expected)) {
            throw new IllegalStateException(
                    "JEP 485 proof resource drift: "
                            + resource
                            + " expected="
                            + expected
                            + " actual="
                            + actual);
        }
    }

    private static String resource(String relative) {
        try (var input =
                M3Jep485ProofRefreshRecipe.class.getResourceAsStream(ROOT + relative)) {
            if (input == null) {
                throw new IllegalStateException(
                        "missing JEP 485 proof resource: " + relative);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read JEP 485 proof resource", failure);
        }
    }

    static String gitBlob(String text) {
        byte[] bytes = Objects.requireNonNull(text, "text").getBytes(StandardCharsets.UTF_8);
        byte[] prefix =
                ("blob " + bytes.length + "\0").getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(prefix);
            digest.update(bytes);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String normalized(Path path) {
        return path.normalize().toString().replace('\\', '/');
    }
}
