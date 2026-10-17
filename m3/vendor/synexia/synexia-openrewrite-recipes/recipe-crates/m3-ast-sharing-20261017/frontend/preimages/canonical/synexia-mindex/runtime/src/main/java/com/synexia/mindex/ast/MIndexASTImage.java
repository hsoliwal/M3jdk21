// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.ast;

import com.synexia.mindex.MIndexString;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

/** Canonical reachable-DAG image. UTF-16 payloads preserve even unpaired surrogates. */
public final class MIndexASTImage {
    private static final int MAGIC = 0x4d415331;
    private MIndexASTImage() {}

    /** Compatibility object projection; internal execution uses primitive Order directly. */
    public static List<MIndexAST.Ref> order(MIndexAST.Ref root) {
        var order = MIndexASTTraversal.order(root);
        List<MIndexAST.Ref> result = new ArrayList<>(order.size());
        for (int i = 0; i < order.size(); i++) result.add(root.owner().ref(order.nodeId(i)));
        return List.copyOf(result);
    }

    public static byte[] encode(MIndexAST.Ref root) {
        try {
            var order = MIndexASTTraversal.order(root);
            MIndexAST owner = root.owner();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(MAGIC);
            out.writeInt(order.size());
            for (int ordinal = 0; ordinal < order.size(); ordinal++) {
                int node = order.nodeId(ordinal);
                writeText(out, owner.kind(node).toString());
                writeText(out, owner.payload(node).toString());
                int count = owner.childCount(node); out.writeInt(count);
                for (int i = 0; i < count; i++) out.writeInt(order.ordinal(owner.childId(node, i)));
            }
            byte[] payload = bytes.toByteArray();
            out.write(digest(payload));
            return bytes.toByteArray();
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    public static MIndexAST.Ref decode(byte[] image, MIndexAST store, int maxNodes, int maxEdges,
                                      int maxTextUnits, int maxBytes) throws IOException {
        if (maxNodes < 1 || maxEdges < 0 || maxTextUnits < 0 || image.length > maxBytes || image.length < 40) {
            throw new IOException("AST image bounds");
        }
        byte[] payload = Arrays.copyOf(image, image.length - 32);
        if (!MessageDigest.isEqual(digest(payload), Arrays.copyOfRange(image, payload.length, image.length))) {
            throw new IOException("AST checksum");
        }
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload));
        if (in.readInt() != MAGIC) throw new IOException("AST format");
        int count = in.readInt();
        if (count < 1 || count > maxNodes || count > in.available() / 12) throw new IOException("AST node count");
        // Validate the entire frame before admitting anything to the caller's store.
        String[] kinds = new String[count];
        String[] values = new String[count];
        int[] offsets = new int[Math.addExact(count, 1)];
        int[] children = new int[Math.min(maxEdges, 16)];
        long totalEdges = 0;
        long textUnits = 0;
        for (int n = 0; n < count; n++) {
            kinds[n] = readText(in, maxTextUnits);
            values[n] = readText(in, maxTextUnits);
            textUnits += kinds[n].length() + (long) values[n].length();
            if (textUnits > maxTextUnits || kinds[n].isEmpty()) throw new IOException("AST text limit");
            int length = in.readInt();
            totalEdges += length;
            if (length < 0 || totalEdges > maxEdges || length > in.available() / 4) throw new IOException("AST edge limit");
            if (totalEdges > Integer.MAX_VALUE) throw new IOException("AST edge count");
            if (totalEdges > children.length) children = Arrays.copyOf(children,
                    (int) Math.min(maxEdges, Math.max(totalEdges, (long) children.length * 2)));
            offsets[n + 1] = (int) totalEdges;
            for (int c = 0; c < length; c++) {
                int child = in.readInt();
                if (child < 0 || child >= n) throw new IOException("AST non-topological child");
                children[offsets[n] + c] = child;
            }
        }
        if (in.available() != 0) throw new IOException("AST trailing bytes");
        // An isolated store also rejects duplicate/noncanonical frames before touching the target.
        MIndexAST staged = new MIndexAST(maxNodes, maxEdges);
        MIndexAST.Ref root = admit(staged, kinds, values, offsets, children);
        if (!Arrays.equals(image, encode(root))) throw new IOException("AST noncanonical image");
        return admit(store, kinds, values, offsets, children);
    }

    public static String fingerprint(MIndexAST.Ref root) { return HexFormat.of().formatHex(digest(encode(root))); }

    private static MIndexAST.Ref admit(MIndexAST store, String[] kinds, String[] values, int[] offsets, int[] children) {
        int[] mapped = new int[kinds.length];
        int[] remappedEdges = new int[offsets[kinds.length]];
        for (int n = 0; n < mapped.length; n++) {
            for (int e = offsets[n]; e < offsets[n + 1]; e++) remappedEdges[e] = mapped[children[e]];
            mapped[n] = store.internIds(MIndexString.literal(kinds[n]), MIndexString.literal(values[n]),
                    remappedEdges, offsets[n], offsets[n + 1] - offsets[n]);
        }
        return store.ref(mapped[mapped.length - 1]);
    }

    private static void writeText(DataOutputStream out, String text) throws IOException {
        out.writeInt(text.length());
        for (int i = 0; i < text.length(); i++) out.writeChar(text.charAt(i));
    }
    private static String readText(DataInputStream in, int limit) throws IOException {
        int length = in.readInt();
        if (length < 0 || length > limit || length > in.available() / 2) throw new IOException("AST text bounds");
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) chars[i] = in.readChar();
        return new String(chars);
    }
    private static byte[] digest(byte[] bytes) {
        try { return MessageDigest.getInstance("SHA-256").digest(bytes); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    /** Attributed/role-aware MIDXAST1 format; separate from the Ref/MAS1 format above. */
    public static byte[] encode(MIndexAST root, int maxBytes) throws IOException {
        return MIndexAttributedASTImage.encode(root, maxBytes);
    }
    public static MIndexAST decode(byte[] image, String expectedSha256, MIndexASTLimits limits) throws IOException {
        return MIndexAttributedASTImage.decode(image, expectedSha256, limits);
    }
    public static String write(MIndexAST root, Path path, int maxBytes) throws IOException {
        return MIndexAttributedASTImage.write(root, path, maxBytes);
    }
    public static MIndexAST read(Path path, String expectedSha256, MIndexASTLimits limits) throws IOException {
        return MIndexAttributedASTImage.read(path, expectedSha256, limits);
    }
}
