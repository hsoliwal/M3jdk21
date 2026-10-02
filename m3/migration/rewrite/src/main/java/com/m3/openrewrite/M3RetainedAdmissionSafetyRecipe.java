/* Copyright 2026 Hitesh Soliwal. SPDX-License-Identifier: Apache-2.0 */
package com.m3.openrewrite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

/**
 * Exact-source M3 text safety transition. Scan the complete pinned source closure before editing.
 * This is not a generic String compiler transform, semantic-equivalence oracle or VM gate.
 * The matching dependency-free recipe replay owns on-disk atomic promotion and rollback.
 */
public final class M3RetainedAdmissionSafetyRecipe
        extends ScanningRecipe<M3RetainedAdmissionSafetyRecipe.Scan> {
    record Atom(String path, String before, String after, String preimage, String postimage) { }
    static final Atom[] ATOMS = {
        new Atom("m3/core/src/com/m3/text/JoinedM3StringPiece.java", "694ee6a9fa06803244fd56d7a846da16f1560d242a0660ae2f4a927c43fc3dcb", "fe7b4aee4e9b1c8f53cbcc254b9a881668f8d65f376007380abe51379ff155f8", "JoinedM3StringPiece.java.before", "JoinedM3StringPiece.java.after"),
        new Atom("m3/core/src/com/m3/text/LocalM3StringPiece.java", "06370c193953ead287f6fd6c423e12bd7761da8bf1c825dacd7445afe1876b51", "83ca042b4f7e41620c5198a179ca876481a81bb0852d82e0c8858df6577d0fba", "LocalM3StringPiece.java.before", "LocalM3StringPiece.java.after"),
        new Atom("m3/core/src/com/m3/text/M3PieceCursor.java", null, "4b274752fbcff86f64049b152a4fa1fa73fc14b2e092011a37be9a586868e07a", null, "M3PieceCursor.java.after"),
        new Atom("m3/core/src/com/m3/text/M3StringPiece.java", "d43e66b414d96ba4b16b8cc94f64fa5a01c2dcb350ad58050105b04996486479", "cdf6fc984bfae2648e31d9e74c4c9919862efdfd7aa4ed79c248ab4c591831dc", "M3StringPiece.java.before", "M3StringPiece.java.after"),
        new Atom("m3/core/src/com/m3/text/M3Text.java", "6085d74e8f7d170c90382bcac2461a9bf8571c3cd5c1e4959621b4cf219d036a", "acc51c980afed7db24a5eba1b1b7aa6e44877286fd90c70b34914e3fe051ff4c", "M3Text.java.before", "M3Text.java.after"),
        new Atom("m3/core/src/com/m3/text/M3TextCompilerRuntime.java", "17aa8f162a9bbdf97de3e98b6ddf45ad5da507798dcde3992ef58e8045a757f9", "61d8412ef06e89666323e7218902172590831150873a3c8ee7a201e5e5e25a32", "M3TextCompilerRuntime.java.before", "M3TextCompilerRuntime.java.after"),
        new Atom("m3/recipes/manifest.json", "15b96d363bd24cc27484e8f8250b071a71a3b4f15554e662d4ce516721e4790c", "ce4b88cd0af96e39207bb003f17d8396a0ea88531c23070548c88c3d7e20ebd6", "foundation-manifest.json.before", "foundation-manifest.json.after")
    };
    public static final class Scan {
        final int[] states = new int[ATOMS.length]; // 0 absent, 1 preimage, 2 postimage, 3 conflict
    }
    @Override public String getDisplayName() { return "Repair M3 retained-text admission and lifetime"; }
    @Override public String getDescription() {
        return "Apply the reviewed exact-source closure without changing existing public declarations; "
                + "refuse missing owners, drift and mixed installations before generating any source.";
    }
    @Override public int maxCycles() { return 1; }
    @Override public Scan getInitialValue(ExecutionContext context) {
        for (Atom atom : ATOMS) {
            resource(atom, false);
            if (atom.before() != null) resource(atom, true);
        }
        return new Scan();
    }
    static int index(Path path) {
        String text = path.toString().replace('\\', '/');
        for (int i = 0; i < ATOMS.length; i++) if (ATOMS[i].path().equals(text)) return i;
        return -1;
    }
    @Override public TreeVisitor<?, ExecutionContext> getScanner(Scan scan) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override public Tree visit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof SourceFile source)) return tree;
                int i = index(source.getSourcePath());
                if (i < 0) return tree;
                String hash = sha(source.printAll());
                synchronized (scan) {
                    if (scan.states[i] != 0 || (ATOMS[i].path().endsWith(".java")
                            && !(source instanceof J.CompilationUnit))) scan.states[i] = 3;
                    else scan.states[i] = hash.equals(ATOMS[i].before()) ? 1
                            : hash.equals(ATOMS[i].after()) ? 2 : 3;
                }
                return tree;
            }
        };
    }
    /** True for a complete preimage; false for a complete, idempotent postimage. */
    static boolean needsApply(Scan scan) {
        boolean before = true, after = true;
        for (int i = 0; i < ATOMS.length; i++) {
            before &= scan.states[i] == (ATOMS[i].before() == null ? 0 : 1);
            after &= scan.states[i] == 2;
        }
        if (before) return true;
        if (after) return false;
        throw new IllegalStateException("M3 safety drift, missing owner or mixed installation");
    }
    @Override public Collection<? extends SourceFile> generate(
            Scan scan, Collection<SourceFile> generated, ExecutionContext context) {
        boolean apply = needsApply(scan);
        for (SourceFile file : generated) {
            if (index(file.getSourcePath()) >= 0) throw new IllegalStateException("competing M3 generated owner");
        }
        List<SourceFile> output = new ArrayList<>();
        if (apply) for (Atom atom : ATOMS) {
            if (atom.before() == null) output.add(parse(atom, resource(atom, false), context));
        }
        return output;
    }
    @Override public TreeVisitor<?, ExecutionContext> getVisitor(Scan scan) {
        final boolean apply = needsApply(scan);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override public Tree visit(Tree tree, ExecutionContext context) {
                if (!apply || !(tree instanceof SourceFile source)) return tree;
                int i = index(source.getSourcePath());
                if (i < 0) return tree;
                Atom atom = ATOMS[i];
                String current = sha(source.printAll());
                if (current.equals(atom.after())) return tree; // newly generated atom
                if (!current.equals(atom.before())) throw new IllegalStateException("M3 source moved after scan");
                return parse(atom, resource(atom, false), context).withMarkers(source.getMarkers());
            }
        };
    }
    private static SourceFile parse(Atom atom, String body, ExecutionContext context) {
        Path path = Path.of(atom.path());
        if (!atom.path().endsWith(".java")) {
            return PlainText.builder().id(Tree.randomId()).sourcePath(path).text(body).build();
        }
        try (var parsed = JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(path, body)), null, context)) {
            List<SourceFile> files = parsed.toList();
            if (files.size() != 1 || !(files.getFirst() instanceof J.CompilationUnit)
                    || !body.equals(files.getFirst().printAll())) throw new IllegalStateException("M3 Java roundtrip");
            return files.getFirst().withSourcePath(path);
        }
    }
    static String resource(Atom atom, boolean before) {
        String name = before ? atom.preimage() : atom.postimage();
        try (var input = M3RetainedAdmissionSafetyRecipe.class.getResourceAsStream(
                "/com/m3/migration/safety/" + name)) {
            if (input == null) throw new IllegalStateException("missing M3 safety resource");
            byte[] bytes = input.readNBytes(262145);
            if (bytes.length > 262144) throw new IllegalStateException("M3 resource bound");
            String body = new String(bytes, StandardCharsets.UTF_8);
            if (!java.util.Arrays.equals(bytes, body.getBytes(StandardCharsets.UTF_8))
                    || !sha(body).equals(before ? atom.before() : atom.after())) {
                throw new IllegalStateException("M3 resource integrity");
            }
            return body;
        } catch (IOException error) { throw new IllegalStateException(error); }
    }
    static String sha(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
