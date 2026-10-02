/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

import com.m3.rewrite.scope.M3FileScopedRecipe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.text.PlainText;

/** Exact new-file JNI candidate installer. Existing divergent C is never replaced. */
public final class M3SegmentedLaneNativeRecipe extends ScanningRecipe<M3SegmentedLaneNativeRecipe.State> implements M3FileScopedRecipe {
    public static final String TARGET = "src/main/native/collections/segmented_bit_lane.c";
    private static final String RESOURCE = "/com/synexia/rewrite/segmented-lane-native/segmented_bit_lane.c.txt";
    private static final String SHA = "4ff7fb4557c58cc59de7fbb24498f3df2a197083a5cd467670dc24b9441f1210";

    public static final class State {
        private final String text;
        private int seen;
        private boolean conflict;
        private State(String text) { this.text = text; }
    }

    @Override public String targetPath() { return TARGET; }
    @Override public String getDisplayName() { return "M3 segmented bitmap JNI candidate"; }
    @Override public String getDescription() {
        return "Adds a sealed JNI bitmap counter only when its target is absent or already identical.";
    }
    @Override public int maxCycles() { return 1; }

    @Override public State getInitialValue(ExecutionContext context) {
        try (var input = getClass().getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("missing native template");
            byte[] bytes = input.readNBytes(65537);
            if (bytes.length > 65536 || !sha(bytes).equals(SHA)) {
                throw new IllegalStateException("native template drift/budget");
            }
            return new State(new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException failure) { throw new IllegalStateException(failure); }
    }

    @Override public TreeVisitor<?, ExecutionContext> getScanner(State state) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override public Tree preVisit(Tree tree, ExecutionContext context) {
                if (tree instanceof SourceFile file) {
                    stopAfterPreVisit();
                    if (file.getSourcePath().normalize().toString().replace('\\', '/').equals(TARGET)) {
                        synchronized (state) {
                            state.seen++;
                            state.conflict |= !file.printAll().equals(state.text);
                        }
                    }
                }
                return tree;
            }
        };
    }

    @Override public Collection<? extends SourceFile> generate(State state, ExecutionContext context) {
        synchronized (state) {
            if (state.conflict || state.seen > 1) throw new IllegalStateException("native target drift/duplicate");
            if (state.seen != 0) return List.of();
            return List.of(PlainText.builder().sourcePath(Path.of(TARGET)).text(state.text).build());
        }
    }

    private static String sha(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException impossible) { throw new ExceptionInInitializerError(impossible); }
    }
}
