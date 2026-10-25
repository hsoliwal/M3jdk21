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
        final int[] rolePositions;
        Node(String kind, int start, int end, int flags, Node[] children, String[] roles) {
            this(kind, start, end, flags, children, roles, null);
        }
        Node(String kind, int start, int end, int flags, Node[] children, String[] roles,
             int[] rolePositions) {
            this.kind = kind;
            this.start = start;
            this.end = end;
            this.flags = flags;
            this.children = children;
            this.roles = roles;
            this.rolePositions = rolePositions;
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
        @Override public int childRolePosition(Node node, int row) {
            return node.rolePositions == null ? -1 : node.rolePositions[row];
        }
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

    private static boolean insidePair(String value, int index) {
        return index > 0 && index < value.length()
                && Character.isHighSurrogate(value.charAt(index - 1))
                && Character.isLowSurrogate(value.charAt(index));
    }

    private static void sourceDeltaContracts() throws IOException {
        URI uri = URI.create("memory:/same-source.java");
        M3File original = M3File.of(uri, "A😀B");
        M3File updated = M3File.of(uri, "A😺B");
        M3File.ChangeEnvelope changed = updated.changeSince(original, 20);
        equal(1, changed.previousStart());
        equal(3, changed.previousEnd());
        equal(1, changed.currentStart());
        equal(3, changed.currentEnd());
        ok(changed.textChanged());
        equal(M3File.RawDigestComparison.UNAVAILABLE, changed.rawDigestComparison());

        M3File.ChangeEnvelope noTextChange = original.changeSince(original, 8);
        ok(!noTextChange.textChanged());
        equal(0, noTextChange.previousEnd());
        refuses(IllegalArgumentException.class, () -> updated.changeSince(original, 7));
        refuses(IllegalArgumentException.class, () -> updated.changeSince(original, -1));
        refuses(IllegalArgumentException.class, () -> updated.changeSince(
                M3File.of(URI.create("memory:/renamed.java"), "A😀B"), 20));

        M3File utf8 = M3File.read(uri, new ByteArrayInputStream(new byte[] {65}),
                StandardCharsets.UTF_8, 4);
        M3File utf8Again = M3File.read(uri, new ByteArrayInputStream(new byte[] {65}),
                StandardCharsets.UTF_8, 4);
        M3File utf16 = M3File.read(uri, new ByteArrayInputStream(new byte[] {65, 0}),
                StandardCharsets.UTF_16LE, 4);
        equal(M3File.RawDigestComparison.SHA256_SAME,
                utf8Again.changeSince(utf8, 2).rawDigestComparison());
        M3File.ChangeEnvelope byteOnly = utf16.changeSince(utf8, 2);
        equal(M3File.RawDigestComparison.SHA256_DIFFERENT, byteOnly.rawDigestComparison());
        ok(!byteOnly.textChanged());

        M3File insert = M3File.of(uri, "ab😺c");
        M3File.ChangeEnvelope inserted = insert.changeSince(M3File.of(uri, "abc"), 8);
        equal(2, inserted.previousStart());
        equal(2, inserted.previousEnd());
        equal(4, inserted.currentEnd());

        Random random = new Random(0xD17A2026L);
        String[] pieces = {"a", "b", "中", "😺", "😀", "\r\n", "\n", "\t", "é"};
        for (int sample = 0; sample < 500; sample++) {
            StringBuilder prior = new StringBuilder();
            int pointCount = random.nextInt(48);
            for (int position = 0; position < pointCount; position++) {
                prior.append(pieces[random.nextInt(pieces.length)]);
            }
            String previousText = prior.toString();
            int[] points = previousText.codePoints().toArray();
            int from = random.nextInt(points.length + 1);
            int through = from + random.nextInt(points.length - from + 1);
            int start = previousText.offsetByCodePoints(0, from);
            int end = previousText.offsetByCodePoints(0, through);
            String replacement = pieces[random.nextInt(pieces.length)];
            String currentText = previousText.substring(0, start) + replacement
                    + previousText.substring(end);
            M3File.ChangeEnvelope delta = M3File.of(uri, currentText)
                    .changeSince(M3File.of(uri, previousText), 4096);
            ok(!insidePair(previousText, delta.previousStart()));
            ok(!insidePair(previousText, delta.previousEnd()));
            ok(!insidePair(currentText, delta.currentStart()));
            ok(!insidePair(currentText, delta.currentEnd()));
            equal(currentText, previousText.substring(0, delta.previousStart())
                    + currentText.substring(delta.currentStart(), delta.currentEnd())
                    + previousText.substring(delta.previousEnd()));
            equal(!previousText.equals(currentText), delta.textChanged());
        }
    }

    private static void fieldListPositionContracts() {
        M3File source = M3File.of(URI.create("memory:/list-fields.java"), "abc");
        Node reused = new Node("Name", 0, 1, M3ASTIndex.NAMED);
        Node root = new Node("Call", 0, 3, M3ASTIndex.NAMED,
                new Node[] {reused, reused, reused},
                new String[] {"arguments", "arguments", "arguments"},
                new int[] {0, 1, 0});
        M3ASTIndex index = project(source, root,
                new M3ASTInterop.SourceStamp(source.uri(), source.utf16Sha256(), null),
                M3ASTInterop.Coordinates.UTF16, new M3ASTIngress.Limits(16, 8, 2));
        equal(4, index.nodeCount());
        equal(-1, index.incomingRolePosition(0));
        equal(0, index.incomingRolePosition(1));
        equal(1, index.incomingRolePosition(2));
        equal(0, index.incomingRolePosition(3));
        equal("arguments", index.incomingRole(2).orElseThrow());
        equal(32L, index.metadataBytes()); // roles and positions: 2 optional int lanes
        M3ASTIndex.StructuralHashes hashes = index.structuralHashes(4, 1024);
        equal(M3ASTIndex.GENERIC_POSITIONED_STRUCTURE_HASH_SCHEMA, hashes.schema());
        ok(!hashes.sha256(1).equals(hashes.sha256(2)));
        equal(hashes.sha256(1), hashes.sha256(3));

        Node noOrdinals = new Node("Call", 0, 3, M3ASTIndex.NAMED,
                root.children, root.roles);
        M3ASTIndex oldShape = project(source, noOrdinals, null,
                M3ASTInterop.Coordinates.UTF16, new M3ASTIngress.Limits(16, 8, 2));
        equal(M3ASTIndex.GENERIC_STRUCTURE_HASH_SCHEMA,
                oldShape.structuralHashes(4, 1024).schema());
        equal(-1, oldShape.incomingRolePosition(1));
        equal(16L, oldShape.metadataBytes());
        M3ASTIndex.Builder illegal = M3ASTIndex.builder(source, M3ASTIndex.Domain.GENERIC, 3, "provider@1");
        refuses(IllegalArgumentException.class,
                () -> illegal.add(-1, "Root", 0, 3, 0, "root", 0));
        refuses(IllegalArgumentException.class,
                () -> illegal.add(-1, "Root", 0, 3, 0, null, 0));
        illegal.add(-1, "Root", 0, 3, 0);
        refuses(IllegalArgumentException.class,
                () -> illegal.add(0, "Child", 0, 1, 0, null, 0));
        refuses(IllegalArgumentException.class,
                () -> illegal.add(0, "Child", 0, 1, 0, "field", -2));
        M3ASTIndex.Builder legacy = M3ASTIndex.builder(source, M3ASTIndex.Domain.JAVAC, 3);
        legacy.add(-1, "Root", 0, 3, 0);
        refuses(IllegalArgumentException.class,
                () -> legacy.add(0, "Child", 0, 1, 0, "field", 0));
        equal(M3ASTIndex.STRUCTURAL_HASH_SCHEMA,
                legacy.seal().structuralHashes(3, 100).schema());

        M3ASTIndex.Builder grown = M3ASTIndex.builder(source, M3ASTIndex.Domain.GENERIC, 65, "provider@1");
        grown.add(-1, "Root", 0, 3, 0);
        for (int i = 0; i < 40; i++) {
            grown.add(0, "Name", 0, 1, 0, "arguments", i);
        }
        M3ASTIndex many = grown.seal();
        equal(41, many.nodeCount());
        for (int i = 0; i < 40; i++) equal(i, many.incomingRolePosition(i + 1));
        equal(328L, many.metadataBytes());
    }

    private static void sourceLineIndexContracts() {
        M3File source = M3File.of(URI.create("memory:/lines.java"), "A😺\r\n中\n");
        M3File.LineIndex lines = source.lineIndex(3);
        equal(3, lines.lineCount());
        equal(12L, lines.primitiveBytes());
        equal(0, lines.lineStart(1));
        equal(5, lines.lineStart(2));
        equal(7, lines.lineStart(3));
        equal(new M3File.LinePosition(1, 3), lines.positionAt(3)); // CR
        equal(new M3File.LinePosition(1, 4), lines.positionAt(4)); // LF in CRLF
        equal(new M3File.LinePosition(2, 0), lines.positionAt(5));
        equal(new M3File.LinePosition(3, 0), lines.positionAt(7)); // empty trailing line
        equal(4, lines.offsetAt(1, 4));
        equal(6, lines.offsetAt(2, 1));
        equal(7, lines.offsetAt(3, 0));
        ok(lines == source.lineIndex(3));
        refuses(IllegalArgumentException.class, () -> source.lineIndex(2));
        refuses(IllegalArgumentException.class, () -> source.lineIndex(0));
        refuses(IndexOutOfBoundsException.class, () -> lines.positionAt(8));
        refuses(IndexOutOfBoundsException.class, () -> lines.offsetAt(1, 5));
        refuses(IndexOutOfBoundsException.class, () -> lines.offsetAt(2, 2));
        refuses(IndexOutOfBoundsException.class, () -> lines.lineStart(0));
        refuses(IndexOutOfBoundsException.class, () -> lines.lineStart(4));
        M3File empty = M3File.of(URI.create("memory:/empty"), "");
        equal(1, empty.lineIndex(1).lineCount());
        equal(new M3File.LinePosition(1, 0), empty.lineIndex(1).positionAt(0));
        equal(0, empty.lineIndex(1).offsetAt(1, 0));

        Random random = new Random(0x11AE2026);
        String[] units = {"A", "中", "😺", "\r", "\n", "\r\n", "é", " "};
        for (int trial = 0; trial < 250; trial++) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0, count = random.nextInt(60); i < count; i++) {
                builder.append(units[random.nextInt(units.length)]);
            }
            M3File candidate = M3File.of(URI.create("memory:/seeded-lines"), builder.toString());
            M3File.LineIndex lineIndex = candidate.lineIndex(128);
            for (int offset = 0; offset <= candidate.length(); offset++) {
                M3File.LinePosition point = lineIndex.positionAt(offset);
                equal(offset, lineIndex.offsetAt(point.line(), point.column()));
            }
            if (lineIndex.lineCount() > 1) {
                int shortBudget = lineIndex.lineCount() - 1;
                refuses(IllegalArgumentException.class, () -> candidate.lineIndex(shortBudget));
            }
        }
    }

    public static void main(String[] args) throws IOException {
        byteAndIndexContracts();
        randomizedOffsets();
        sourceDeltaContracts();
        fieldListPositionContracts();
        sourceLineIndexContracts();
        System.out.println("M3_AST_INTEROP_PASS checks=" + checks);
    }
}
