// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3ASTInterop;
import jdk.m3.parser.M3File;

/** Independent Java 21 contract, without a bundled Tree-sitter/JNI dependency. */
public final class M3ASTTreeSitterContract {
    private static int checks;
    private static final int NAMED = 1, MISSING = 2, EXTRA = 4, ERROR = 8, CONTAINS_ERROR = 16;
    private static final M3ASTIngress.Limits LIMITS = new M3ASTIngress.Limits(256, 64, 12);
    private static final URI URI_VALUE = URI.create("file:///m3/treesitter.json");

    private record Node(String type, int begin, int end, int bits, String[] fields, Node... children) {
        Node {
            if (fields.length != children.length) throw new IllegalArgumentException("fields/children");
        }
    }

    private static final M3ASTInterop.TreeSitterNodes<Node> API = new M3ASTInterop.TreeSitterNodes<>() {
        @Override public String type(Node node) { return node.type(); }
        @Override public int childCount(Node node) { return node.children().length; }
        @Override public Node child(Node node, int ordinal) { return node.children()[ordinal]; }
        @Override public int startByte(Node node) { return node.begin(); }
        @Override public int endByte(Node node) { return node.end(); }
        @Override public boolean isNamed(Node node) { return (node.bits() & NAMED) != 0; }
        @Override public boolean isMissing(Node node) { return (node.bits() & MISSING) != 0; }
        @Override public boolean isExtra(Node node) { return (node.bits() & EXTRA) != 0; }
        @Override public boolean isError(Node node) { return (node.bits() & ERROR) != 0; }
        @Override public boolean hasError(Node node) { return (node.bits() & CONTAINS_ERROR) != 0; }
        @Override public String fieldNameForChild(Node parent, int ordinal) {
            return parent.fields()[ordinal];
        }
    };

    private M3ASTTreeSitterContract() { }

    private static void eq(Object expected, Object actual) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("expected " + expected + " but got " + actual + " at " + checks);
        }
    }

    private static void ok(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("failed at " + checks);
    }

    private static void rejects(Class<? extends RuntimeException> type, Runnable operation) {
        checks++;
        try { operation.run(); }
        catch (RuntimeException failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("unexpected refusal", failure);
        }
        throw new AssertionError("expected refusal " + type);
    }

    private static Node leaf(String type, int start, int end, int bits) {
        return new Node(type, start, end, bits, new String[0]);
    }

    private static Node fixture() {
        Node name = leaf("string_content", 2, 6, NAMED);
        Node quoted = new Node("string", 1, 7, NAMED, new String[] {null}, name);
        Node pair = new Node("pair", 1, 9, NAMED,
                new String[] {"key", null, "value"},
                quoted, leaf(":", 7, 8, 0), leaf("number", 8, 9, NAMED));
        Node object = new Node("object", 0, 10, NAMED,
                new String[] {null, "entry", null},
                leaf("{", 0, 1, 0), pair, leaf("}", 9, 10, 0));
        Node comment = leaf("newline-extra", 10, 11, EXTRA);
        Node missing = leaf("MISSING", 11, 11, NAMED | MISSING);
        return new Node("document", 0, 11, NAMED | CONTAINS_ERROR,
                new String[] {"body", null, "missing"}, object, comment, missing);
    }

    private static void exercise() throws IOException {
        String text = "{\"😺\":0}\n";
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        eq(11, bytes.length);
        M3File file = M3File.read(URI_VALUE, new ByteArrayInputStream(bytes),
                StandardCharsets.UTF_8, 64);
        M3ASTInterop.SourceStamp stamp = new M3ASTInterop.SourceStamp(file.uri(),
                file.utf16Sha256(), file.byteSha256().orElseThrow());
        M3ASTIndex index = M3ASTInterop.treeSitter(file, fixture(),
                "tree-sitter/json@0.24.8", API, stamp, LIMITS);
        eq(M3ASTIndex.Domain.GENERIC, index.domain());
        eq("tree-sitter/json@0.24.8", index.providerId());
        eq(11, index.nodeCount());
        eq(3, index.childCount(0));
        eq(3, index.childCount(1));
        eq("document", index.kind(0));
        eq("pair", index.kind(3));
        eq("string_content", index.kind(5));
        eq("😺", index.sourceText(5));
        eq(2, index.start(5));
        eq(4, index.end(5));
        eq(3, index.parent(4));
        eq("key", index.incomingRole(4).orElseThrow());
        eq("value", index.incomingRole(7).orElseThrow());
        eq("entry", index.incomingRole(3).orElseThrow());
        eq(-1, index.incomingRolePosition(3));
        ok((index.flags(5) & M3ASTIndex.NAMED) != 0);
        ok((index.flags(5) & M3ASTIndex.TOKEN) != 0);
        ok((index.flags(6) & M3ASTIndex.NAMED) == 0);
        ok((index.flags(6) & M3ASTIndex.TOKEN) != 0);
        ok((index.flags(0) & M3ASTIndex.RECOVERED) != 0);
        ok((index.flags(9) & M3ASTIndex.TRIVIA) != 0);
        ok((index.flags(10) & M3ASTIndex.MISSING) != 0);
        ok((index.flags(10) & M3ASTIndex.RECOVERED) != 0);
        ok((index.flags(10) & M3ASTIndex.ERROR) == 0);
        eq("", index.sourceText(10));
        ok(index.hasExactRange(5));
        ok((index.flags(5) & M3ASTIndex.SOURCE_UNVERIFIED) == 0);
        ok(!index.hasExactFullRange(5));
        ok(index.structuralHashes(64, 1024).sha256(0) != null);

        M3ASTIndex unstamped = M3ASTInterop.treeSitter(file, fixture(),
                "tree-sitter/json@0.24.8", API, LIMITS);
        eq(11, unstamped.nodeCount());
        ok((unstamped.flags(5) & M3ASTIndex.SOURCE_UNVERIFIED) != 0);
        ok(!unstamped.hasExactRange(5));

        M3ASTInterop.SourceStamp unsealedBytes = new M3ASTInterop.SourceStamp(file.uri(),
                file.utf16Sha256(), null);
        M3ASTIndex incomplete = M3ASTInterop.treeSitter(file, fixture(),
                "tree-sitter/json@0.24.8", API, unsealedBytes, LIMITS);
        ok((incomplete.flags(0) & M3ASTIndex.SOURCE_UNVERIFIED) != 0);
        rejects(IllegalArgumentException.class, () -> M3ASTInterop.treeSitter(file,
                fixture(), "tree-sitter/json@0.24.8", API,
                new M3ASTInterop.SourceStamp(URI.create("file:///wrong.json"),
                        file.utf16Sha256(), file.byteSha256().orElseThrow()), LIMITS));
        rejects(IllegalArgumentException.class, () -> M3ASTInterop.treeSitter(file,
                fixture(), "tree-sitter/json@0.24.8", API,
                new M3ASTInterop.SourceStamp(file.uri(), file.utf16Sha256(), "bad"), LIMITS));
        rejects(IllegalArgumentException.class, () -> M3ASTInterop.treeSitter(
                M3File.of(file.uri(), text), fixture(), "tree-sitter/json@0.24.8", API, LIMITS));
        rejects(IllegalArgumentException.class, () -> M3ASTInterop.treeSitter(file,
                leaf("bad-mid-code-point", 3, 6, NAMED), "tree-sitter/json@0.24.8", API,
                stamp, LIMITS));
        rejects(IllegalArgumentException.class, () -> M3ASTInterop.treeSitter(file,
                fixture(), "tree-sitter/json@0.24.8", API, stamp,
                new M3ASTIngress.Limits(256, 64, 2)));
        rejects(NullPointerException.class, () -> M3ASTInterop.treeSitter(file,
                fixture(), "tree-sitter/json@0.24.8", null, stamp, LIMITS));
        rejects(IllegalArgumentException.class, () -> M3ASTInterop.treeSitter(file,
                fixture(), "", API, stamp, LIMITS));
        for (int run = 0; run < 75; run++) {
            M3ASTIndex replay = M3ASTInterop.treeSitter(file, fixture(),
                    "tree-sitter/json@0.24.8", API, stamp, LIMITS);
            eq(index.structuralHashes(64, 1024).sha256(0),
                    replay.structuralHashes(64, 1024).sha256(0));
            eq(index.nodeCount(), replay.nodeCount());
        }
    }

    public static void main(String[] args) throws IOException {
        exercise();
        System.out.println("M3_AST_TREESITTER_PASS checks=" + checks);
    }
}
