// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.synexia.mindex.compiler.ast;

import com.synexia.mindex.ast.MIndexAST;
import com.synexia.mindex.ast.MIndexASTImage;
import com.synexia.mindex.ast.MIndexASTLimits;
import com.synexia.mindex.ast.MIndexASTTraversal;
import com.synexia.mindex.experiment.Digests;
import com.synexia.mindex.swap.TieredSwapCache;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Bounded source/occurrence frame around the existing canonical AST image codec. */
public final class M3ASTCodec implements TieredSwapCache.Codec<M3AST> {
    private static final long MAGIC = 0x4d33415354444f31L;
    private final MIndexASTLimits limits;
    private final int maxBytes;

    public M3ASTCodec(MIndexASTLimits limits) {
        this.limits = Objects.requireNonNull(limits, "limits");
        long bound = 256L + limits.imageBytes() + 2L * limits.sourceChars()
                + 12L * limits.nodes() + 32_768L;
        maxBytes = (int) Math.min(Integer.MAX_VALUE - 8L, bound);
    }

    public int maxBytes() { return maxBytes; }

    @Override public byte[] encode(M3AST value) throws IOException {
        MIndexASTDocument document = value.asMIndex();
        if (document.source().length() > limits.sourceChars()
                || document.occurrences().size() > limits.nodes()) throw new IOException("AST document budget");
        if (!legacySourceSha256(document.source()).equals(document.sourceUtf16Sha256())) {
            throw new IOException("AST document source digest mismatch");
        }
        byte[] image = MIndexASTImage.encode(document.root(), limits.imageBytes());
        MIndexASTTraversal.Order order = MIndexASTTraversal.order(document.root());
        List<MIndexAST> atoms = MIndexASTImage.order(document.root());
        var buffer = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(buffer)) {
            out.writeLong(MAGIC);
            writeText(out, document.sourceName(), 16_384);
            writeText(out, document.source(), limits.sourceChars());
            out.writeInt(image.length);
            out.write(image);
            out.writeInt(document.occurrences().size());
            for (MIndexASTOccurrence occurrence : document.occurrences()) {
                int ordinal = order.ordinal(occurrence.atom().contentIndex());
                if (ordinal < 0 || !atoms.get(ordinal).equals(occurrence.atom())) {
                    throw new IOException("occurrence outside syntax root");
                }
                out.writeInt(ordinal);
                out.writeInt(occurrence.start());
                out.writeInt(occurrence.end());
            }
        }
        byte[] payload = buffer.toByteArray();
        if ((long) payload.length + 32 > maxBytes) throw new IOException("AST document exceeds image budget");
        byte[] result = Arrays.copyOf(payload, payload.length + 32);
        byte[] checksum = java.util.HexFormat.of().parseHex(Digests.sha256(payload));
        System.arraycopy(checksum, 0, result, payload.length, checksum.length);
        return result;
    }

    @Override public M3AST decode(byte[] bytes) throws IOException {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length < 52 || bytes.length > maxBytes) throw new IOException("AST document size");
        byte[] stable = bytes.clone();
        byte[] payload = Arrays.copyOf(stable, stable.length - 32);
        if (!java.security.MessageDigest.isEqual(java.util.HexFormat.of().parseHex(Digests.sha256(payload)),
                Arrays.copyOfRange(stable, stable.length - 32, stable.length))) {
            throw new IOException("AST document checksum");
        }
        try (var in = new DataInputStream(new ByteArrayInputStream(payload))) {
            if (in.readLong() != MAGIC) throw new IOException("AST document version");
            String name = readText(in, 16_384);
            String source = readText(in, limits.sourceChars());
            int length = bounded(in.readInt(), Math.min(limits.imageBytes(), in.available()), "AST image");
            byte[] image = in.readNBytes(length);
            MIndexAST root = MIndexASTImage.decode(image, Digests.sha256(image), limits);
            List<MIndexAST> atoms = MIndexASTImage.order(root);
            int count = bounded(in.readInt(), Math.min(limits.nodes(), in.available() / 12), "occurrences");
            List<MIndexASTOccurrence> occurrences = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                int ordinal = bounded(in.readInt(), atoms.size() - 1, "atom ordinal");
                occurrences.add(new MIndexASTOccurrence(atoms.get(ordinal), in.readInt(), in.readInt()));
            }
            if (in.available() != 0) throw new IOException("trailing AST document data");
            M3AST result = new M3AST(new MIndexASTDocument(name, source, legacySourceSha256(source), root, occurrences));
            if (!Arrays.equals(stable, encode(result))) throw new IOException("noncanonical AST document");
            return result;
        } catch (IllegalArgumentException | IllegalStateException | ArithmeticException invalid) {
            throw new IOException("invalid AST document", invalid);
        }
    }

    /** Declared cache accounting estimate; it is not a measurement or bound on the JVM heap. */
    @Override public long weight(M3AST value) {
        MIndexASTDocument document = value.asMIndex();
        return 1024L + 2L * (document.source().length() + document.sourceName().length())
                + 64L * document.occurrences().size() + 512L * document.root().reachableAtoms();
    }

    private static String legacySourceSha256(String source) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            for (int i = 0; i < source.length(); i++) {
                digest.update((byte) (source.charAt(i) >>> 8));
                digest.update((byte) source.charAt(i));
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    private static void writeText(DataOutputStream out, String text, int limit) throws IOException {
        if (text.length() > limit) throw new IOException("source text budget");
        out.writeInt(text.length());
        for (int i = 0; i < text.length(); i++) out.writeChar(text.charAt(i));
    }

    private static String readText(DataInputStream in, int limit) throws IOException {
        int length = bounded(in.readInt(), Math.min(limit, in.available() / 2), "UTF-16 length");
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) chars[i] = in.readChar();
        return new String(chars);
    }

    private static int bounded(int value, int max, String label) throws IOException {
        if (value < 0 || value > max) throw new IOException("invalid " + label);
        return value;
    }
}
