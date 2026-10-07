// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.io.IOException;
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
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.text.PlainText;

/** Splits JEP 496 released-state paths into private whole-file and shared semantic-recipe lanes. */
public final class M3Jep496SharedOwnerSplitRecipe
        extends ScanningRecipe<M3Jep496SharedOwnerSplitRecipe.Inventory> {
    private static final String ROOT =
            "/com/m3/rewrite/backport/jep496-shared-owner-split-20261007/";

    private static final Map<String, Target> TARGETS = Map.of(
            ".github/workflows/m3-jep496-mlkem-inventory.yml",
            new Target("workflow.before.yml", "workflow.after.yml",
                    "286a153c3c674f97a246c368d1531af75af4fe77",
                    "4a509e67c9d6415bc0529a6067c9819bfd97ce08"),
            "m3/backports/recipes/jep-496-mlkem/README.md",
            new Target("README.before.md", "README.after.md",
                    "a0fe18001dab48aef0c25c056a703fd2df8f99e1",
                    "5e562dd9bad4a625a532e4421434fa0ca4e9f49b"),
            "m3/backports/recipes/jep-496-mlkem/MATERIALIZE_PATHS.txt",
            new Target("MATERIALIZE.before.txt", "MATERIALIZE.after.txt",
                    "b3110fd3c8cdc523cefeb8073774b9040d1a152f",
                    "7d89c37cb0f1a3660f2310bc5cfdfd91a86fceb3"),
            "m3/backports/recipes/jep-496-mlkem/SHARED_OWNER_PATHS.txt",
            new Target(null, "SHARED_OWNER_PATHS.txt", "ABSENT",
                    "a718d947f0b67209754b37c94bb5dfcfdc3a6b81"));

    static final class Inventory {
        final Map<String,String> seen = new HashMap<>();
    }

    private record Target(String beforeResource, String afterResource, String beforeBlob, String afterBlob) {}

    @Override public String getDisplayName() { return "Split JEP 496 shared owners"; }

    @Override public String getDescription() {
        return "Prevents broad JDK24-GA whole-file absorption for shared security owners; only feature-private paths remain snapshot candidates.";
    }

    @Override public Set<String> getTags() {
        return Set.of("m3","jdk21","jep-496","shared-owner","semantic-recipe","file-atomic","candidate-only");
    }

    @Override public int maxCycles() { return 1; }

    @Override public Inventory getInitialValue(ExecutionContext context) {
        TARGETS.forEach((path,target) -> {
            if (!"ABSENT".equals(target.beforeBlob())) requireBlob(target.beforeResource(), target.beforeBlob());
            requireBlob(target.afterResource(), target.afterBlob());
        });
        return new Inventory();
    }

    @Override public TreeVisitor<?,ExecutionContext> getScanner(Inventory inventory) {
        return new TreeVisitor<Tree,ExecutionContext>() {
            @Override public Tree preVisit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof SourceFile source)) return tree;
                stopAfterPreVisit();
                String path = normalized(source.getSourcePath());
                Target target = TARGETS.get(path);
                if (target == null) return tree;
                if (!(source instanceof PlainText)) throw new IllegalStateException("not PlainText: " + path);
                String blob = gitBlob(source.printAll());
                if (!blob.equals(target.beforeBlob()) && !blob.equals(target.afterBlob())) {
                    throw new IllegalStateException("JEP496 split drift: " + path);
                }
                if (inventory.seen.putIfAbsent(path, blob) != null) {
                    throw new IllegalStateException("duplicate JEP496 split target: " + path);
                }
                return tree;
            }
        };
    }

    @Override public Collection<? extends SourceFile> generate(Inventory inventory, ExecutionContext context) {
        List<SourceFile> generated = new ArrayList<>();
        for (Map.Entry<String,Target> entry : TARGETS.entrySet()) {
            if (!inventory.seen.containsKey(entry.getKey())) {
                if (!"ABSENT".equals(entry.getValue().beforeBlob())) {
                    throw new IllegalStateException("missing JEP496 split target: " + entry.getKey());
                }
                generated.add(PlainText.builder()
                        .sourcePath(Path.of(entry.getKey()))
                        .text(resource(entry.getValue().afterResource()))
                        .build());
            }
        }
        return generated;
    }

    @Override public TreeVisitor<?,ExecutionContext> getVisitor(Inventory inventory) {
        return new TreeVisitor<Tree,ExecutionContext>() {
            @Override public Tree preVisit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof SourceFile source)) return tree;
                stopAfterPreVisit();
                String path = normalized(source.getSourcePath());
                Target target = TARGETS.get(path);
                if (target == null) return tree;
                String current = gitBlob(source.printAll());
                if (current.equals(target.afterBlob())) return tree;
                if (!current.equals(target.beforeBlob())) {
                    throw new IllegalStateException("JEP496 split changed after scan: " + path);
                }
                return PlainText.builder().sourcePath(source.getSourcePath())
                        .text(resource(target.afterResource())).build()
                        .withId(source.getId())
                        .withMarkers(source.getMarkers())
                        .withFileAttributes(source.getFileAttributes())
                        .withCharset(source.getCharset())
                        .withCharsetBomMarked(source.isCharsetBomMarked())
                        .withChecksum(null);
            }
        };
    }

    public boolean productSourceMutationAuthority() { return false; }
    public boolean promotionAuthority() { return false; }

    static String before(String path) {
        Target t=require(path);
        if ("ABSENT".equals(t.beforeBlob())) throw new IllegalArgumentException("absent preimage");
        return resource(t.beforeResource());
    }
    static String after(String path) { return resource(require(path).afterResource()); }
    static Set<String> targetPaths() { return Set.copyOf(TARGETS.keySet()); }

    private static Target require(String path) {
        Target t=TARGETS.get(path);
        if (t==null) throw new IllegalArgumentException(path);
        return t;
    }
    private static void requireBlob(String res,String expected) {
        String actual=gitBlob(resource(res));
        if (!actual.equals(expected)) throw new IllegalStateException("resource drift: "+res);
    }
    private static String resource(String name) {
        try (var in=M3Jep496SharedOwnerSplitRecipe.class.getResourceAsStream(ROOT+name)) {
            if (in==null) throw new IllegalStateException("missing resource: "+name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) { throw new IllegalStateException(e); }
    }
    static String gitBlob(String text) {
        byte[] bytes=Objects.requireNonNull(text).getBytes(StandardCharsets.UTF_8);
        byte[] prefix=("blob "+bytes.length+"\0").getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest d=MessageDigest.getInstance("SHA-1");
            d.update(prefix); d.update(bytes);
            return HexFormat.of().formatHex(d.digest());
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static String normalized(Path path) { return path.normalize().toString().replace('\\','/'); }
}
