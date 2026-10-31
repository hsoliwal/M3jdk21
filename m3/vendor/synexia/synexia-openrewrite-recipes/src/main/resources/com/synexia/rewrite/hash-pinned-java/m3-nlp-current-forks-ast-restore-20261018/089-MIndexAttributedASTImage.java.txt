// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.ast;

import com.synexia.mindex.experiment.Digests;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Canonical, pinned, length-bounded MIDXAST1 images of one reachable syntax DAG. */
final class MIndexAttributedASTImage {
    private static final long MAGIC = 0x4d49445841535431L;
    private MIndexAttributedASTImage() { }

    /** Encoding is independent of unrelated atoms and the original pool's insertion order. */
    public static byte[] encode(MIndexAST root, int maxBytes) throws IOException {
        Objects.requireNonNull(root, "root");
        if (maxBytes < 32) { throw new IllegalArgumentException("image limit"); }
        MIndexASTTraversal.Order order = MIndexASTTraversal.order(root);
        TreeMap<String, Integer> symbols = new TreeMap<>();
        long bytes = 16L;
        for (int ordinal = 0; ordinal < order.size(); ordinal++) {
            int id = order.nodeId(ordinal);
            MIndexASTNode row = root.store.node(id);
            symbols.put(root.store.symbol(row.kind).toString(), 0);
            for (int symbol : row.attributes) { symbols.put(root.store.symbol(symbol).toString(), 0); }
            for (int symbol : row.roles) { symbols.put(root.store.symbol(symbol).toString(), 0); }
            bytes += 12L + row.attributes.length * 4L + row.children.length * 8L;
            if (bytes > maxBytes) { throw new IOException("AST image exceeds byte limit"); }
        }
        int symbolOrdinal = 0;
        for (var entry : symbols.entrySet()) {
            entry.setValue(symbolOrdinal++);
            bytes += 4L + entry.getKey().length() * 2L;
            if (bytes > maxBytes) { throw new IOException("AST image exceeds byte limit"); }
        }
        var buffer = new ByteArrayOutputStream((int) bytes);
        var out = new DataOutputStream(buffer);
        out.writeLong(MAGIC);
        out.writeInt(symbols.size());
        for (String value : symbols.keySet()) { MIndexASTFingerprint.text(out, value); }
        out.writeInt(order.size());
        for (int ordinal = 0; ordinal < order.size(); ordinal++) {
            int id = order.nodeId(ordinal);
            MIndexASTNode row = root.store.node(id);
            out.writeInt(symbols.get(root.store.symbol(row.kind).toString()));
            out.writeInt(row.attributes.length / 2);
            for (int symbol : row.attributes) { out.writeInt(symbols.get(root.store.symbol(symbol).toString())); }
            out.writeInt(row.children.length);
            for (int i = 0; i < row.children.length; i++) {
                out.writeInt(symbols.get(root.store.symbol(row.roles[i]).toString()));
                out.writeInt(order.ordinal(row.children[i]));
            }
        }
        out.flush();
        return buffer.toByteArray();
    }

    /** The pin is checked before parsing or allocating count-derived arrays. */
    public static MIndexAST decode(byte[] supplied, String expectedSha256, MIndexASTLimits limits) throws IOException {
        Objects.requireNonNull(supplied, "bytes");
        Objects.requireNonNull(limits, "limits");
        Digests.requireSha256(expectedSha256);
        if (supplied.length > limits.imageBytes()) { throw new IOException("AST image exceeds byte limit"); }
        byte[] bytes = supplied.clone();
        if (!Digests.sha256(bytes).equals(expectedSha256)) { throw new IOException("AST image SHA-256 mismatch"); }
        try {
            var in = new DataInputStream(new ByteArrayInputStream(bytes));
            if (in.readLong() != MAGIC) { throw new IOException("unsupported AST image"); }
            int symbolCount = count(in, limits.symbols(), "symbols");
            if (symbolCount > in.available() / 4) { throw new IOException("truncated symbol table"); }
            String[] symbols = new String[symbolCount];
            long chars = 0;
            for (int i = 0; i < symbols.length; i++) {
                int length = count(in, in.available() / 2, "UTF-16 units");
                chars += length;
                if (chars > limits.symbolChars()) { throw new IOException("symbol character limit"); }
                char[] value = new char[length];
                for (int c = 0; c < length; c++) { value[c] = in.readChar(); }
                symbols[i] = new String(value);
                if (i > 0 && symbols[i - 1].compareTo(symbols[i]) >= 0) { throw new IOException("unordered symbols"); }
            }
            int nodes = count(in, limits.nodes(), "nodes");
            if (nodes == 0 || nodes > in.available() / 12) { throw new IOException("empty or truncated atom table"); }
            var pool = new MIndexASTPool(limits);
            for (int id = 0; id < nodes; id++) {
                String kind = symbol(in, symbols);
                int attributes = count(in, Math.min(limits.attributes(), in.available() / 8), "attributes");
                Map<String, String> attrs = new TreeMap<>();
                String previous = null;
                for (int a = 0; a < attributes; a++) {
                    String name = symbol(in, symbols);
                    if (previous != null && previous.compareTo(name) >= 0) { throw new IOException("unordered attributes"); }
                    attrs.put(name, symbol(in, symbols));
                    previous = name;
                }
                int edges = count(in, Math.min(limits.edges(), in.available() / 8), "edges");
                List<MIndexASTEdge> children = new ArrayList<>(edges);
                for (int e = 0; e < edges; e++) {
                    String role = symbol(in, symbols);
                    int child = in.readInt();
                    if (child < 0 || child >= id) { throw new IOException("forward or invalid child reference"); }
                    children.add(new MIndexASTEdge(role, pool.ref(child)));
                }
                if (pool.intern(kind, attrs, children).contentIndex() != id) { throw new IOException("duplicate atom"); }
            }
            if (in.available() != 0) { throw new IOException("trailing AST image data"); }
            MIndexAST root = pool.snapshot().ref(nodes - 1);
            if (!Arrays.equals(bytes, encode(root, limits.imageBytes()))) { throw new IOException("noncanonical AST image"); }
            return root;
        } catch (IllegalArgumentException | IllegalStateException | ArithmeticException invalid) {
            throw new IOException("invalid AST image", invalid);
        }
    }

    /** Create-new output; all encoding and validation happens before a destination is created. */
    public static String write(MIndexAST root, Path path, int maxBytes) throws IOException {
        byte[] bytes = encode(root, maxBytes);
        Files.write(path, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        return Digests.sha256(bytes);
    }

    /** Bounded stream read also guards against a file growing after a size check. */
    public static MIndexAST read(Path path, String expectedSha256, MIndexASTLimits limits) throws IOException {
        if (Files.size(path) > limits.imageBytes()) { throw new IOException("AST image exceeds byte limit"); }
        try (var input = Files.newInputStream(path)) {
            byte[] bytes = input.readNBytes(limits.imageBytes());
            if (input.read() != -1) { throw new IOException("AST image exceeds byte limit"); }
            return decode(bytes, expectedSha256, limits);
        }
    }

    private static int count(DataInputStream in, int max, String name) throws IOException {
        int count = in.readInt();
        if (count < 0 || count > max) { throw new IOException("invalid " + name + " count"); }
        return count;
    }

    private static String symbol(DataInputStream in, String[] symbols) throws IOException {
        int id = in.readInt();
        if (id < 0 || id >= symbols.length) { throw new IOException("invalid symbol coordinate"); }
        return symbols[id];
    }

}
