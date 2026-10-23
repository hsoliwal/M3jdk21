// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package jdk.m3.parser;

import com.sun.source.tree.Tree;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Portable-within-a-matched-compiler, immutable image of an existing M3AST javac
 * preorder projection. The original javac task is the only parser. The image
 * retains primitive row slots only; it does not retain or reconstruct Tree nodes,
 * attribution, Element or TypeMirror objects.
 *
 * <p>The caller supplies the full compiler/build-context SHA-256. A content
 * match alone does NOT authorize semantic/ABI reuse or skipping any plugin.
 * The image's capabilities are deliberately restricted to syntax geometry.
 */
public final class M3JavacFactImage {
    public static final String SCHEMA = "m3jdk21.javac.geometry.v1";
    public static final int SYNTAX = 1, POSITIONS = 2, PARENTS = 4;
    private static final int CAPABILITIES = SYNTAX | POSITIONS | PARENTS;
    private static final int MAGIC = 0x4d334146; // M3AF
    private static final int VERSION = 1;
    private static final int HEADER = 120;
    private static final int ROW = 16;
    private static final int SEAL = 32;
    private static final Tree.Kind[] COMPILER_KINDS = Tree.Kind.values();
    private final byte[] image;
    private final int count;
    private final int offset;
    private final M3File source;

    private M3JavacFactImage(byte[] image, int count, int offset, M3File source) {
        this.image = image;
        this.count = count;
        this.offset = offset;
        this.source = source;
    }

    /** Deterministic and bounded image creation, without a second javac traversal. */
    public static byte[] capture(M3JavacTree tree, byte[] contextSha256, int maxBytes) throws IOException {
        Objects.requireNonNull(tree, "already projected javac tree");
        requireDigest(contextSha256);
        if (maxBytes < HEADER + SEAL) throw new IllegalArgumentException("maxBytes");
        M3File source = tree.source();
        byte[] uri = source.uri().toASCIIString().getBytes(StandardCharsets.UTF_8);
        int rows = tree.nodeCount();
        long length = (long) HEADER + uri.length + (long) rows * ROW + SEAL;
        if (rows < 1 || length > maxBytes || length > Integer.MAX_VALUE) {
            throw new IOException("javac fact image budget exceeded");
        }
        ByteBuffer out = ByteBuffer.allocate((int) length).order(ByteOrder.BIG_ENDIAN);
        out.putInt(MAGIC).putInt(VERSION).putInt(rows).putInt(uri.length);
        out.putInt(source.length()).putInt(CAPABILITIES);
        out.put(kindSeal()).put(HexFormat.of().parseHex(source.utf16Sha256())).put(contextSha256);
        out.put(uri);
        for (int row = 0; row < rows; row++) {
            out.putInt(tree.kind(row).ordinal()).putInt(tree.parent(row));
            out.putInt(tree.start(row)).putInt(tree.end(row));
        }
        byte[] image = out.array();
        byte[] hash = digest(image, 0, image.length - SEAL);
        System.arraycopy(hash, 0, image, image.length - SEAL, SEAL);
        return image;
    }

    /**
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
     * Capture from the consolidated JAVAC-domain index through its geometry view.
     * The bytes equal a capture of the geometry projection of the same unit.
     */
    public static byte[] capture(M3ASTIndex index, byte[] contextSha256, int maxBytes) throws IOException {
        return capture(M3JavacTree.of(index), contextSha256, maxBytes);
    }

    /**
=======
>>>>>>> 3e70045b5ed2ac07406f046784901725740e3507
     * Admit a prior image only with exactly matching source and build-context
     * identities. No file I/O, source parsing or javac task is performed here.
     */
    public static M3JavacFactImage read(byte[] image, M3File source,
            byte[] contextSha256, int maxBytes, int maxNodes) throws IOException {
        Objects.requireNonNull(image, "image");
        Objects.requireNonNull(source, "source");
        requireDigest(contextSha256);
        if (maxBytes < HEADER + SEAL || maxNodes < 1 ||
                image.length < HEADER + SEAL || image.length > maxBytes) {
            throw new IOException("invalid javac fact image budget");
        }
        if (!MessageDigest.isEqual(digest(image, 0, image.length - SEAL),
                Arrays.copyOfRange(image, image.length - SEAL, image.length))) {
            throw new IOException("javac fact image checksum mismatch");
        }
        ByteBuffer buffer = ByteBuffer.wrap(image).order(ByteOrder.BIG_ENDIAN);
        if (buffer.getInt() != MAGIC || buffer.getInt() != VERSION) {
            throw new IOException("unsupported javac fact image version");
        }
        int n = buffer.getInt(), uriBytes = buffer.getInt(), sourceLength = buffer.getInt();
        int caps = buffer.getInt();
        if (n < 1 || n > maxNodes || uriBytes < 1 || sourceLength != source.length() ||
                caps != CAPABILITIES || HEADER + (long) uriBytes + ROW * (long) n + SEAL != image.length) {
            throw new IOException("invalid javac fact image geometry/capabilities");
        }
        byte[] kinds = new byte[SEAL], content = new byte[SEAL], context = new byte[SEAL];
        buffer.get(kinds).get(content).get(context);
        if (!MessageDigest.isEqual(kinds, kindSeal()) ||
                !MessageDigest.isEqual(content, HexFormat.of().parseHex(source.utf16Sha256())) ||
                !MessageDigest.isEqual(context, contextSha256)) {
            throw new IOException("foreign javac kind/source/build-context generation");
        }
        byte[] rawUri = new byte[uriBytes]; buffer.get(rawUri);
        String uri;
        try {
            uri = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(rawUri)).toString();
        } catch (CharacterCodingException invalid) { throw new IOException("malformed javac source URI", invalid); }
        if (!uri.equals(source.uri().toASCIIString())) throw new IOException("foreign javac source URI");
        int rowsOffset = buffer.position();
        for (int row = 0; row < n; row++) {
            int kind = buffer.getInt(), parent = buffer.getInt(), start = buffer.getInt(), end = buffer.getInt();
            if (kind < 0 || kind >= COMPILER_KINDS.length ||
                    (row == 0 ? parent != -1 : parent < 0 || parent >= row) ||
                    (start != -1 && (start < 0 || start > sourceLength)) ||
                    (end != -1 && (end < 0 || end > sourceLength)) ||
                    (start >= 0 && end >= 0 && start > end)) {
                throw new IOException("malformed javac fact row " + row);
            }
        }
        return new M3JavacFactImage(image.clone(), n, rowsOffset, source);
    }

    public int size() { return count; }
    public int capabilities() { return CAPABILITIES; }
    public M3File source() { return source; }
    public Tree.Kind kind(int row) { return COMPILER_KINDS[value(row, 0)]; }
    public int parent(int row) { return value(row, 1); }
    public int start(int row) { return value(row, 2); }
    public int end(int row) { return value(row, 3); }
    public boolean hasSourceRange(int row) { return start(row) >= 0 && end(row) >= 0; }
    public String sourceText(int row) {
        if (!hasSourceRange(row)) throw new IllegalStateException("javac source coordinates unavailable");
        return source.subSequence(start(row), end(row));
    }
    /** Caller receives an independent image; no mutable internal array is exposed. */
    public byte[] bytes() { return image.clone(); }

    private int value(int row, int column) {
        Objects.checkIndex(row, count);
        int first = offset + row * ROW + column * Integer.BYTES;
        return ((image[first] & 0xff) << 24) | ((image[first + 1] & 0xff) << 16)
                | ((image[first + 2] & 0xff) << 8) | (image[first + 3] & 0xff);
    }
    private static void requireDigest(byte[] digest) {
        if (Objects.requireNonNull(digest, "build context digest").length != SEAL)
            throw new IllegalArgumentException("expected a 32-byte SHA-256");
    }
    private static byte[] kindSeal() {
        MessageDigest digest = sha256();
        for (Tree.Kind kind : COMPILER_KINDS) {
            digest.update(kind.name().getBytes(StandardCharsets.UTF_8)); digest.update((byte) 0);
        }
        return digest.digest();
    }
    private static byte[] digest(byte[] input, int start, int length) {
        MessageDigest digest = sha256(); digest.update(input, start, length); return digest.digest();
    }
    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
