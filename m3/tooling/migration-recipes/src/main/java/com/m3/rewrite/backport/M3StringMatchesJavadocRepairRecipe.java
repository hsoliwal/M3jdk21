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

/** Exact FILE repair restoring String.matches Javadoc ownership without behavior change. */
public final class M3StringMatchesJavadocRepairRecipe
        extends ScanningRecipe<M3StringMatchesJavadocRepairRecipe.Inventory> {
    static final String TARGET = "src/java.base/share/classes/java/lang/String.java";
    static final String BEFORE = "da6f50d2aaf66bfef0564ba79d155287e9f7de56";
    static final String AFTER = "b6990d278db6affa946e8285181c85d70e599a43";
    private static final String ROOT =
            "/com/m3/rewrite/backport/string-matches-javadoc/";
    private static final String BEFORE_RESOURCE = "String.java.before";
    private static final String AFTER_RESOURCE = "String.java.after";

    static final class Inventory { String state; }

    @Override public String getDisplayName() { return "Repair String.matches Javadoc ownership"; }
    @Override public String getDescription() {
        return "Moves the existing matches Javadoc below the private regex helper from exact current source.";
    }
    @Override public Set<String> getTags() {
        return Set.of("m3","jdk21","java.base","string","javadoc","file","fixed-point");
    }
    @Override public int maxCycles() { return 1; }
    @Override public Inventory getInitialValue(ExecutionContext ctx) {
        if (!BEFORE.equals(gitBlob(beforeImage())) || !AFTER.equals(gitBlob(afterImage()))) {
            throw new IllegalStateException("String recipe resource drift");
        }
        return new Inventory();
    }
    @Override public TreeVisitor<?, ExecutionContext> getScanner(Inventory inv) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override public Tree preVisit(Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile source)) return tree;
                stopAfterPreVisit();
                if (!TARGET.equals(source.getSourcePath().toString().replace('\\','/'))) return tree;
                if (!(source instanceof J.CompilationUnit)) throw new IllegalStateException("target not Java");
                String hash = gitBlob(source.printAll());
                if (!BEFORE.equals(hash) && !AFTER.equals(hash)) throw new IllegalStateException("String source drift");
                if (inv.state != null) throw new IllegalStateException("duplicate String target");
                inv.state = hash;
                return tree;
            }
        };
    }
    @Override public Collection<? extends SourceFile> generate(Inventory inv, ExecutionContext ctx) {
        if (inv.state == null) throw new IllegalStateException("missing String target");
        return List.of();
    }
    @Override public TreeVisitor<?, ExecutionContext> getVisitor(Inventory inv) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override public Tree preVisit(Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile source)) return tree;
                stopAfterPreVisit();
                if (!TARGET.equals(source.getSourcePath().toString().replace('\\','/'))) return tree;
                String current = gitBlob(source.printAll());
                if (!Objects.equals(current, inv.state)) throw new IllegalStateException("String changed after scan");
                if (AFTER.equals(current)) return tree;
                if (!BEFORE.equals(current)) throw new IllegalStateException("String preimage drift");
                J.CompilationUnit replacement = parse(ctx);
                return replacement.withId(source.getId())
                        .withSourcePath(source.getSourcePath())
                        .withMarkers(source.getMarkers())
                        .withFileAttributes(source.getFileAttributes())
                        .withCharset(source.getCharset())
                        .withCharsetBomMarked(source.isCharsetBomMarked())
                        .withChecksum(null);
            }
        };
    }
    public boolean publicApiChangeAuthority() { return false; }
    public boolean behaviorChangeAuthority() { return false; }
    public boolean promotionAuthority() { return false; }

    static String beforeImage() { return resource(BEFORE_RESOURCE); }
    static String afterImage() { return resource(AFTER_RESOURCE); }
    private static String resource(String name) {
        try (var in=M3StringMatchesJavadocRepairRecipe.class.getResourceAsStream(ROOT + name)) {
            if(in==null) throw new IllegalStateException("missing String recipe resource: " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch(IOException e) { throw new IllegalStateException(e); }
    }
    private static J.CompilationUnit parse(ExecutionContext ctx) {
        String text=afterImage();
        try (var parsed=JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(TARGET), text)), null, ctx)) {
            var files=parsed.toList();
            if(files.size()!=1 || !(files.getFirst() instanceof J.CompilationUnit unit)
                    || !text.equals(unit.printAll())) throw new IllegalStateException("String roundtrip drift");
            return unit;
        }
    }
    static String gitBlob(String text) {
        byte[] bytes=text.getBytes(StandardCharsets.UTF_8);
        byte[] prefix=("blob "+bytes.length+"\0").getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest d=MessageDigest.getInstance("SHA-1");
            d.update(prefix); d.update(bytes);
            return HexFormat.of().formatHex(d.digest());
        } catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
