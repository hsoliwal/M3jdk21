// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.test;

import com.sun.source.tree.Tree;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3File;
import jdk.m3.parser.M3JavacFactImage;
import jdk.m3.parser.M3JavacTree;

/**
 * Fact image codec contract. The geometry comes from a public JAVAC-domain index
 * viewed through {@link M3JavacTree#of(M3ASTIndex)}; no module-private constructor
 * is reached from this consumer package, so the test tree compiles as a consumer.
 */
public final class M3JavacFactImageContract {
    private static final int SYNTAX_POSITIONS_PARENTS = 7;
    private static final int MAX_IMAGE_BYTES = 10_000;
    private static final int MAX_NODES = 10;
    private static final int CORRUPT_OFFSET = 130;
    private static int checks;

    private M3JavacFactImageContract() { }

    @FunctionalInterface private interface Throwing { void run() throws IOException; }

    public static void main(String[] args) throws Exception {
        String code = "class Demo { int answer = 42; }";
        M3File source = M3File.of(URI.create("memory:/Demo.java"), code);
        byte[] context = MessageDigest.getInstance("SHA-256").digest("compiler-context".getBytes(StandardCharsets.UTF_8));
        M3JavacTree tree = M3JavacTree.of(index(source, code));
        byte[] image = M3JavacFactImage.capture(tree, context, MAX_IMAGE_BYTES);
        var view = M3JavacFactImage.read(image, source, context, MAX_IMAGE_BYTES, MAX_NODES);
        check(view.size() == 3, "three-indexed-nodes");
        check(view.capabilities() == SYNTAX_POSITIONS_PARENTS, "syntax-only capabilities");
        check(view.parent(2) == 1, "preorder parent");
        check(view.kind(2) == Tree.Kind.VARIABLE, "original javac tree kind");
        check(view.sourceText(1).equals(code), "source identity shared");
        check(view.hasSourceRange(2), "bounded span");
        check(Arrays.equals(image, view.bytes()), "canonical image roundtrip");
        check(Arrays.equals(image, M3JavacFactImage.capture(tree, context, MAX_IMAGE_BYTES)), "deterministic codec");
        check(Arrays.equals(image, M3JavacFactImage.capture(tree.index().orElseThrow(), context, MAX_IMAGE_BYTES)),
                "index capture equals view capture");
        view.bytes()[7] = 100;
        check(view.size() == 3, "defensive bytes");
        byte[] corrupt = image.clone();
        corrupt[CORRUPT_OFFSET] ^= 1;
        refuses(() -> M3JavacFactImage.read(corrupt, source, context, MAX_IMAGE_BYTES, MAX_NODES));
        refuses(() -> M3JavacFactImage.read(image, M3File.of(URI.create("memory:/Other.java"), code), context, MAX_IMAGE_BYTES, MAX_NODES));
        refuses(() -> M3JavacFactImage.read(image, M3File.of(source.uri(), code + " "), context, MAX_IMAGE_BYTES, MAX_NODES));
        refuses(() -> M3JavacFactImage.read(image, source, new byte[32], MAX_IMAGE_BYTES, MAX_NODES));
        refuses(() -> M3JavacFactImage.read(image, source, context, image.length - 1, MAX_NODES));
        refuses(() -> M3JavacFactImage.read(image, source, context, MAX_IMAGE_BYTES, 2));
        try {
            view.sourceText(999);
            throw new AssertionError("bad row");
        } catch (IndexOutOfBoundsException expected) {
            checks++;
        }
        System.out.println("M3_JAVAC_FACT_IMAGE_PASS checks=" + checks);
    }

    /** Compilation unit, class and field with exact ranges, through the public builder. */
    private static M3ASTIndex index(M3File source, String code) {
        int flags = M3ASTIndex.EXACT_RANGE | M3ASTIndex.SYNTAX_ONLY;
        M3ASTIndex.Builder builder = M3ASTIndex.builder(source, M3ASTIndex.Domain.JAVAC, MAX_NODES);
        int unit = builder.add(-1, Tree.Kind.COMPILATION_UNIT.name(), 0, code.length(), flags);
        int type = builder.add(unit, Tree.Kind.CLASS.name(), 0, code.length(), flags);
        builder.add(type, Tree.Kind.VARIABLE.name(), code.indexOf("int"), code.indexOf(';') + 1, flags);
        return builder.seal();
    }

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }

    private static void refuses(Throwing action) {
        checks++;
        try {
            action.run();
            throw new AssertionError("accepted invalid evidence");
        } catch (IOException expected) {
            // refused as required
        }
    }
}
