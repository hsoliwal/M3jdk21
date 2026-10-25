// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3ASTInterop;
import jdk.m3.parser.M3File;

/** Standalone Java 21 oracle: no external parser, source reload or JNI needed. */
public final class M3ASTInteropContract {
    private static int checks;
    private M3ASTInteropContract() { }

    private static final class Node {
        final String kind;
        final int start, end, flags;
        final Node[] children;
        final String[] roles;
        Node(String kind, int start, int end, int flags, Node[] children, String[] roles) {
            this.kind = kind;
            this.start = start;
            this.end = end;
            this.flags = flags;
            this.children = children;
            this.roles = roles;
        }
        Node(String kind, int start, int end, int flags) {
            this(kind, start, end, flags, new Node[0], new String[0]);
        }
    }

    private static final M3ASTInterop.Surface<Node> SURFACE = new M3ASTInterop.Surface<>() {
        @Override public String kind(Node node) { return node.kind; }
        @Override public int childCount(Node node) { return node.children.length; }
        @Override public Node child(Node node, int row) { return node.children[row]; }
        @Override public String childRole(Node node, int row) { return node.roles[row]; }
        @Override public int start(Node node) { return node.start; }
        @Override public int end(Node node) { return node.end; }
        @Override public int syntaxFlags(Node node) { return node.flags; }
        @Override public String name(Node node) { return "emoji".equals(node.kind) ? "cat" : null; }
        @Override public M3ASTIndex.LiteralKind literalKind(Node node) {
            return "emoji".equals(node.kind) ? M3ASTIndex.LiteralKind.STRING : M3ASTIndex.LiteralKind.NONE;
        }
        @Override public String literalValue(Node node) { return "emoji".equals(node.kind) ? "😺" : null; }
    };

    private static final class CountedInput extends ByteArrayInputStream {
        int readCount, closes;
        CountedInput(byte[] contents) { super(contents); }
        @Override public synchronized int read(byte[] target, int start, int count) {
            int n = super.read(target, start, count);
            if (n > 0) readCount += n;
            return n;
        }
        @Override public void close() throws IOException { closes++; super.close(); }
    }

    private static void equal(Object expected, Object actual) {
        checks++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError("expected " + expected + ", got " + actual);
        }
    }
    private static void ok(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("contract failed at check " + checks);
    }
    private static void refuses(Class<? extends RuntimeException> kind, Runnable action) {
        checks++;
        try { action.run(); }
        catch (RuntimeException exception) {
            if (kind.isInstance(exception)) return;
            throw new AssertionError("wrong exception type", exception);
        }
        throw new AssertionError("expected " + kind.getSimpleName());
    }

    private static M3ASTIndex project(M3File source, Node root, M3ASTInterop.SourceStamp stamp,
            M3ASTInterop.Coordinates units, M3ASTIngress.Limits limits) {
        return M3ASTInterop.project(source, root, "tree-sitter/java@0.25", units, SURFACE, stamp, limits);
    }

    private static void byteAndIndexContracts() throws IOException {
        String sourceText = "A😺\r\n中\n";
        byte[] bytes = sourceText.getBytes(StandardCharsets.UTF_8);
        CountedInput input = new CountedInput(bytes);
        URI uri = URI.create("file:///tmp/m3-interop.java");
        M3File source = M3File.read(uri, input, StandardCharsets.UTF_8, 256);
        equal(bytes.length, input.readCount);
        equal(1, input.closes);
        equal(11, bytes.length);
        equal(7, source.length());
        M3File.Utf8Offsets offsets = source.utf8Offsets().orElseThrow();
        equal(11, offsets.byteLength());
        equal(7, offsets.utf16Length());
        equal(2, offsets.nonAsciiCount());
        equal(16L, offsets.primitiveBytes());
        for (int[] pair : new int[][] {{0,0},{1,1},{5,3},{6,4},{7,5},{10,6},{11,7}}) {
            equal(pair[1], offsets.toUtf16(pair[0]));
            equal(pair[0], offsets.toByte(pair[1]));
        }
        for (int byteOffset : new int[] {2,3,4,8,9}) {
            refuses(IllegalArgumentException.class, () -> offsets.toUtf16(byteOffset));
        }
        refuses(IllegalArgumentException.class, () -> offsets.toByte(2));
        refuses(IndexOutOfBoundsException.class, () -> offsets.toUtf16(12));

        Node reused = new Node("identifier", 7, 10, M3ASTIndex.NAMED | M3ASTIndex.TOKEN);
        Node root = new Node("unit", 0, 11, M3ASTIndex.NAMED,
                new Node[] {
                    new Node("identifier", 0, 1, M3ASTIndex.NAMED | M3ASTIndex.TOKEN),
                    new Node("emoji", 1, 5, M3ASTIndex.NAMED | M3ASTIndex.TOKEN),
                    new Node("comment", 5, 7, M3ASTIndex.TRIVIA | M3ASTIndex.TOKEN),
                    reused,
                    new Node("missing", 10, 10, M3ASTIndex.MISSING | M3ASTIndex.TOKEN),
                    reused,
                    reused,
                }, new String[] {"name", "value", "trivia", "left", "semicolon", "right", "right"});
        M3ASTInterop.SourceStamp stamp = new M3ASTInterop.SourceStamp(source.uri(),
                source.utf16Sha256(), source.byteSha256().orElseThrow());
        M3ASTIndex index = project(source, root, stamp, M3ASTInterop.Coordinates.UTF8_BYTES,
                new M3ASTIngress.Limits(256, 20, 3));
        equal(8, index.nodeCount());
        equal(M3ASTIndex.Domain.GENERIC, index.domain());
        equal("tree-sitter/java@0.25", index.providerId());
        equal(7, index.childCount(0));
        equal(0, index.parent(1));
        equal(1, index.start(2));
        equal(3, index.end(2));
        equal("😺", index.sourceText(2));
        equal("cat", index.name(2).orElseThrow());
        equal("😺", index.literalValue(2).orElseThrow());
        equal("value", index.incomingRole(2).orElseThrow());
        equal("left", index.incomingRole(4).orElseThrow());
        equal("right", index.incomingRole(6).orElseThrow());
        equal("right", index.incomingRole(7).orElseThrow());
        ok(index.incomingRole(0).isEmpty());
        ok((index.flags(3) & M3ASTIndex.TRIVIA) != 0);
        ok((index.flags(5) & (M3ASTIndex.MISSING | M3ASTIndex.RECOVERED))
                == (M3ASTIndex.MISSING | M3ASTIndex.RECOVERED));
        ok(index.hasExactRange(5));
        equal("", index.sourceText(5));
        equal(16L, offsets.primitiveBytes());
        equal(bytes.length, input.readCount); // projection did not reopen source
        M3ASTIndex.StructuralHashes hashes = index.structuralHashes(20, 4096);
        equal(M3ASTIndex.GENERIC_STRUCTURE_HASH_SCHEMA, hashes.schema());
        ok(!hashes.sha256(4).equals(hashes.sha256(6))); // edge role is structural
        equal(hashes.sha256(6), hashes.sha256(7)); // same role, same retained node

        M3ASTIndex unverified = project(source, root, null, M3ASTInterop.Coordinates.UTF8_BYTES,
                new M3ASTIngress.Limits(256, 20, 3));
        ok((unverified.flags(0) & M3ASTIndex.SOURCE_UNVERIFIED) != 0);
        ok(!unverified.hasExactRange(2));
        refuses(IllegalStateException.class, () -> unverified.sourceText(2));

        refuses(IllegalArgumentException.class, () -> project(source, root, stamp,
                M3ASTInterop.Coordinates.UTF8_BYTES, new M3ASTIngress.Limits(256, 6, 3)));
        refuses(IllegalArgumentException.class, () -> project(source, root, stamp,
                M3ASTInterop.Coordinates.UTF8_BYTES, new M3ASTIngress.Limits(256, 20, 1)));
        refuses(IllegalArgumentException.class, () -> project(source, root, stamp,
                M3ASTInterop.Coordinates.UTF8_BYTES, new M3ASTIngress.Limits(2, 20, 3)));
        refuses(IllegalArgumentException.class, () -> project(source, root,
                new M3ASTInterop.SourceStamp(uri, "wrong", null),
                M3ASTInterop.Coordinates.UTF8_BYTES, new M3ASTIngress.Limits(256, 20, 3)));
        refuses(IllegalArgumentException.class, () -> project(source, root,
                new M3ASTInterop.SourceStamp(uri, source.utf16Sha256(), "wrong"),
                M3ASTInterop.Coordinates.UTF8_BYTES, new M3ASTIngress.Limits(256, 20, 3)));

        M3File charsOnly = M3File.of(uri, sourceText);
        ok(charsOnly.utf8Offsets().isEmpty());
        refuses(IllegalArgumentException.class, () -> project(charsOnly, root, null,
                M3ASTInterop.Coordinates.UTF8_BYTES, new M3ASTIngress.Limits(256, 20, 3)));
        M3File utf16 = M3File.read(uri, new ByteArrayInputStream(
                sourceText.getBytes(StandardCharsets.UTF_16LE)), StandardCharsets.UTF_16LE, 256);
        ok(utf16.utf8Offsets().isEmpty());
        M3ASTIndex conventional = originalSchemaIndex(source);
        equal(M3ASTIndex.STRUCTURAL_HASH_SCHEMA, conventional.structuralHashes(2, 100).schema());
    }

    private static M3ASTIndex originalSchemaIndex(M3File source) {
        M3ASTIndex.Builder builder = M3ASTIndex.builder(source, M3ASTIndex.Domain.JAVAC, 2);
        builder.add(-1, "ROOT", 0, source.length(), M3ASTIndex.EXACT_RANGE);
        return builder.seal();
    }

    private static void randomizedOffsets() throws IOException {
        Random random = new Random(0x5EED2026);
        StringBuilder text = new StringBuilder(8192);
        for (int i = 0; i < 1600; i++) {
            int choice = random.nextInt(5);
            text.appendCodePoint(switch (choice) {
                case 0 -> 65 + random.nextInt(26);
                case 1 -> 0x100 + random.nextInt(0x200);
                case 2 -> 0x4E00 + random.nextInt(0x200);
                case 3 -> 0x1F600 + random.nextInt(75);
                default -> 10;
            });
        }
        String value = text.toString();
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        M3File source = M3File.read(URI.create("file:///unicode"), new ByteArrayInputStream(bytes),
                StandardCharsets.UTF_8, bytes.length);
        M3File.Utf8Offsets map = source.utf8Offsets().orElseThrow();
        int charIndex = 0, byteIndex = 0;
        while (charIndex < value.length()) {
            equal(charIndex, map.toUtf16(byteIndex));
            equal(byteIndex, map.toByte(charIndex));
            int point = value.codePointAt(charIndex);
            charIndex += Character.charCount(point);
            byteIndex += new String(Character.toChars(point)).getBytes(StandardCharsets.UTF_8).length;
        }
        equal(value.length(), map.toUtf16(bytes.length));
        equal(bytes.length, map.toByte(value.length()));
        M3File ascii = M3File.read(URI.create("file:///ascii"),
                new ByteArrayInputStream(new byte[] {65, 66, 67}), StandardCharsets.UTF_8, 4);
        equal(0, ascii.utf8Offsets().orElseThrow().nonAsciiCount());
        equal(3, ascii.utf8Offsets().orElseThrow().toByte(3));
    }

    public static void main(String[] args) throws IOException {
        byteAndIndexContracts();
        randomizedOffsets();
        System.out.println("M3_AST_INTEROP_PASS checks=" + checks);
    }
}
