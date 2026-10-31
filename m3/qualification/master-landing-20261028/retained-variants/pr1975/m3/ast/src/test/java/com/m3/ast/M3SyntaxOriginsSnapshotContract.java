// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.ast;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.Random;

/** Deterministic binary provenance codec differential/refusal test, JDK 21. */
public final class M3SyntaxOriginsSnapshotContract {
    private M3SyntaxOriginsSnapshotContract() { }
    private static int checks;
    private static void check(boolean value, String explanation) {
        if (!value) throw new AssertionError(explanation);
        checks++;
    }
    private static void refuse(Class<? extends Throwable> expected, Runnable action) {
        try { action.run(); }
        catch (Throwable actual) {
            if (!expected.isInstance(actual))
                throw new AssertionError("incorrect refusal " + expected, actual);
            checks++;
            return;
        }
        throw new AssertionError("accepted an invalid source-origin packet");
    }
    private static byte[] utf8(String text) { return text.getBytes(StandardCharsets.UTF_8); }
    private static String sha(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static M3SyntaxTree tree(String grammar, byte[] source) {
        var builder = M3SyntaxTree.builder(grammar, source);
        builder.open("source", "", 0, M3SyntaxTree.NAMED);
        builder.close(source.length);
        return builder.build();
    }
    private static byte[] changeAndSeal(byte[] packet, int offset, byte changed) {
        byte[] other = packet.clone();
        other[offset] = changed;
        byte[] digest = HexFormat.of().parseHex(sha(Arrays.copyOf(other, other.length - 32)));
        System.arraycopy(digest, 0, other, other.length - 32, 32);
        return other;
    }
    private static void writeText(DataOutputStream out, String s) throws IOException {
        out.writeInt(s.length());
        for (int i = 0; i < s.length(); i++) out.writeChar(s.charAt(i));
    }
    /**
     * Independent compatibility oracle for the original wire writer. This
     * deliberately builds the first-use dictionaries from the public Origin
     * accessors rather than using any optimized package-private integer lanes.
     * Its packet must be byte-identical to the lean writer for every fixture.
     */
    private static byte[] referencePacket(M3SyntaxOrigins origins) {
        var ids = new java.util.LinkedHashMap<String, Integer>();
        var hashes = new java.util.LinkedHashMap<String, String>();
        for (int row = 0; row < origins.count(); row++) {
            var origin = origins.origin(row);
            ids.computeIfAbsent(origin.sourceId(), ignored -> ids.size());
            String existing = hashes.putIfAbsent(origin.sourceId(), origin.sha256());
            if (existing != null && !existing.equals(origin.sha256()))
                throw new AssertionError("source generation contradiction");
        }
        try {
            var bytes = new ByteArrayOutputStream();
            var out = new DataOutputStream(bytes);
            out.writeLong(0x4d334f524947494eL);
            out.writeInt(1);
            out.write(HexFormat.of().parseHex(origins.projectionSha256()));
            out.writeInt(origins.count());
            out.writeInt(ids.size());
            for (String id : ids.keySet()) {
                writeText(out, id);
                out.write(HexFormat.of().parseHex(hashes.get(id)));
            }
            for (int row = 0; row < origins.count(); row++) {
                var origin = origins.origin(row);
                out.writeInt(origins.generatedStart(row));
                out.writeInt(origins.generatedEnd(row));
                out.writeInt(ids.get(origin.sourceId()));
                out.writeInt(origin.startByte());
                out.writeInt(origin.endByte());
                out.writeInt(origin.relation().ordinal());
            }
            out.flush();
            out.write(HexFormat.of().parseHex(sha(bytes.toByteArray())));
            return bytes.toByteArray();
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
    }
    private record Source(String name, String sha256) { }
    private record Row(int from, int to, int source, int originalFrom, int originalTo, int relation) { }
    private static byte[] manuallyBuild(M3SyntaxTree tree, Source[] sources, Row[] rows) {
        try {
            var bytes = new ByteArrayOutputStream();
            var out = new DataOutputStream(bytes);
            out.writeLong(0x4d334f524947494eL);
            out.writeInt(1);
            out.write(HexFormat.of().parseHex(tree.projectionSha256()));
            out.writeInt(rows.length);
            out.writeInt(sources.length);
            for (Source source : sources) {
                writeText(out, source.name());
                out.write(HexFormat.of().parseHex(source.sha256()));
            }
            for (Row row : rows) {
                out.writeInt(row.from()); out.writeInt(row.to());
                out.writeInt(row.source()); out.writeInt(row.originalFrom());
                out.writeInt(row.originalTo()); out.writeInt(row.relation());
            }
            out.flush();
            out.write(HexFormat.of().parseHex(sha(bytes.toByteArray())));
            return bytes.toByteArray();
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
    }
    private static M3SyntaxOrigins decode(byte[] packet, M3SyntaxTree tree) {
        return M3SyntaxOriginsSnapshot.decode(packet, tree, 20_000, 20_000, packet.length);
    }

    public static void main(String[] args) {
        byte[] generated = utf8("abα😀Z");
        M3SyntaxTree syntax = tree("polyglot/α", generated);
        byte[] main = utf8("__abα😀__");
        byte[] second = utf8("Z!!");
        String firstId = "source/α😀", secondId = "source/\ud800";
        String sha1 = sha(main), sha2 = sha(second);
        var origins = M3SyntaxOrigins.builder(syntax)
                .add(0, 2, firstId, sha1, 2, 4, M3SyntaxOrigins.Relation.EXACT_BYTES)
                .add(2, 4, firstId, sha1, 4, 6, M3SyntaxOrigins.Relation.EXACT_BYTES)
                .add(2, 8, secondId, sha2, 0, 1, M3SyntaxOrigins.Relation.MACRO_EXPANSION)
                .add(4, 8, firstId, sha1, 6, 10, M3SyntaxOrigins.Relation.EXACT_BYTES)
                .add(8, 9, secondId, sha2, 0, 1, M3SyntaxOrigins.Relation.EXACT_BYTES)
                .freeze();
        byte[] packet = M3SyntaxOriginsSnapshot.encode(origins, 20_000);
        check(Arrays.equals(packet, referencePacket(origins)),
                "optimized writer retains exact original on-wire bytes");
        M3SyntaxOrigins read = decode(packet, syntax);
        check(Arrays.equals(M3SyntaxOriginsSnapshot.encode(read, 20_000), packet),
                "canonical binary fixed point");
        check(read.count() == origins.count() && read.projectionSha256().equals(syntax.projectionSha256()),
                "five original source relations and projection retained");
        check(read.origin(0).sourceId().equals(firstId)
                && read.origin(2).sourceId().equals(secondId)
                && read.origin(2).relation() == M3SyntaxOrigins.Relation.MACRO_EXPANSION,
                "exact UTF16 source IDs and macro relation");
        check(read.generatedStart(4) == 8 && read.generatedEnd(4) == 9
                && read.origin(4).startByte() == 0 && read.origin(4).endByte() == 1,
                "physical byte ranges and reuse of the second source");
        var proof = read.verify(syntax, id -> id.equals(firstId) ? main : second,
                2, 1000);
        check(proof.exactAtByte(0) != null && proof.exactAtByte(0).value() == 'a'
                && proof.exactAtByte(8) != null && proof.exactAtByte(8).value() == 'Z',
                "independent files and exact source bytes verified after decoding");
        check(proof.exactAtByte(3) == null && proof.exactAtByte(4) == null,
                "macro-expansion ambiguity not promoted to exact original byte");
        byte[] corrupted = packet.clone();
        corrupted[20] ^= 7;
        refuse(IllegalArgumentException.class, () -> decode(corrupted, syntax));
        refuse(IllegalArgumentException.class, () -> decode(Arrays.copyOf(packet, packet.length - 1), syntax));
        refuse(IllegalArgumentException.class, () -> M3SyntaxOriginsSnapshot.decode(packet, syntax,
                4, 2, packet.length));
        refuse(IllegalArgumentException.class, () -> M3SyntaxOriginsSnapshot.decode(packet, syntax,
                5, 1, packet.length));
        refuse(IllegalArgumentException.class, () -> M3SyntaxOriginsSnapshot.decode(packet, syntax,
                5, 2, packet.length - 1));
        refuse(IllegalArgumentException.class, () -> M3SyntaxOriginsSnapshot.encode(origins,
                packet.length - 1));
        M3SyntaxTree stale = tree("polyglot/α", utf8("abα😀Y"));
        refuse(IllegalArgumentException.class, () -> decode(packet, stale));
        refuse(IllegalArgumentException.class, () -> decode(changeAndSeal(packet, 11, (byte) 7), syntax));
        refuse(IllegalArgumentException.class, () -> decode(changeAndSeal(packet, 8, (byte) 17), syntax));

        // The no-origin image is a valid, deterministic minimal 84-byte packet.
        M3SyntaxOrigins empty = M3SyntaxOrigins.builder(syntax).freeze();
        byte[] emptyPacket = M3SyntaxOriginsSnapshot.encode(empty, 84);
        check(emptyPacket.length == 84 && decode(emptyPacket, syntax).count() == 0,
                "zero-source, zero-row packet is canonical and bounded");
        refuse(IllegalArgumentException.class, () -> M3SyntaxOriginsSnapshot.encode(empty, 83));

        Source[] dictionaries = {new Source("A", sha1), new Source("B", sha2)};
        Row[] rows = {
            new Row(0, 1, 0, 2, 3, 0), new Row(1, 2, 0, 3, 4, 0),
            new Row(2, 3, 0, 4, 5, 0)
        };
        // A valid SHA over the packet doesn't make noncanonical dictionaries safe.
        refuse(IllegalArgumentException.class, () -> decode(manuallyBuild(syntax, dictionaries, rows), syntax));
        refuse(IllegalArgumentException.class, () -> decode(manuallyBuild(syntax,
                new Source[] {dictionaries[0], dictionaries[0]},
                new Row[]{rows[0], new Row(1, 2, 1, 3, 4, 0)}), syntax));
        refuse(IllegalArgumentException.class, () -> decode(manuallyBuild(syntax,
                new Source[] {dictionaries[0], dictionaries[1]},
                new Row[]{new Row(0, 1, 1, 2, 3, 0), new Row(1, 2, 0, 3, 4, 0)}), syntax));
        refuse(IllegalArgumentException.class, () -> decode(manuallyBuild(syntax,
                new Source[]{dictionaries[0]}, new Row[]{new Row(0, 1, 0, 2, 3, 97)}), syntax));
        refuse(IllegalArgumentException.class, () -> decode(manuallyBuild(syntax,
                new Source[]{dictionaries[0]}, new Row[]{new Row(2, 1, 0, 2, 3, 0)}), syntax));

        // Randomized source ranges include invalid UTF-8: the AST wire format
        // must not silently re-encode, normalize, or alter physical bytes.
        Random random = new Random(0x4d334f524947494eL);
        for (int iteration = 0; iteration < 350; iteration++) {
            byte[] raw = new byte[1 + random.nextInt(260)];
            random.nextBytes(raw);
            var t = tree("raw/fixture", raw);
            int sourceCount = 1 + random.nextInt(32);
            var b = M3SyntaxOrigins.builder(t);
            String hash = sha(raw);
            int offset = 0;
            while (offset < raw.length) {
                int end = Math.min(raw.length, offset + 1 + random.nextInt(7));
                int id = random.nextInt(sourceCount);
                b.add(offset, end, "src-" + id, hash, offset, end,
                        M3SyntaxOrigins.Relation.EXACT_BYTES);
                offset = end;
            }
            M3SyntaxOrigins sidecar = b.freeze();
            byte[] wire = M3SyntaxOriginsSnapshot.encode(sidecar, 32768);
            check(Arrays.equals(wire, referencePacket(sidecar)),
                    "reference writer binary compatibility " + iteration);
            M3SyntaxOrigins back = M3SyntaxOriginsSnapshot.decode(wire, t, 512, 32, 32768);
            check(Arrays.equals(wire, M3SyntaxOriginsSnapshot.encode(back, 32768)),
                    "fixed-seed canonical wire round trip " + iteration);
            check(back.verify(t, ignored -> raw, 32, 32768).exactAtByte(0) != null,
                    "strict independent byte-range verification " + iteration);
            int sample = random.nextInt(raw.length);
            var point = back.verify(t, ignored -> raw, 32, 32768).exactAtByte(sample);
            check(point != null && point.byteOffset() == sample && point.value() == raw[sample],
                    "physical byte remains unchanged " + iteration);
        }
        // Deterministic stress with many distinct source identities; the wire
        // decoder must bound source count without scanning every source row.
        byte[] x = utf8("x");
        var bigTree = tree("massive/origins", x);
        var many = M3SyntaxOrigins.builder(bigTree);
        String dig = sha(x);
        for (int i = 0; i < 4096; i++)
            many.add(0, 1, "source/" + i, dig, 0, 1, M3SyntaxOrigins.Relation.EXACT_BYTES);
        M3SyntaxOrigins massOrigins = many.freeze();
        byte[] mass = M3SyntaxOriginsSnapshot.encode(massOrigins, 1024 * 1024);
        check(Arrays.equals(mass, referencePacket(massOrigins)),
                "4096 distinct source IDs preserve the historical byte-for-byte packet");
        var massAgain = M3SyntaxOriginsSnapshot.decode(mass, bigTree, 4096, 4096, 1024 * 1024);
        check(massAgain.count() == 4096 && massAgain.origin(4095).sourceId().equals("source/4095"),
                "4096 independent source provenance records and dictionary identities");
        refuse(IllegalArgumentException.class, () -> M3SyntaxOriginsSnapshot.decode(mass,
                bigTree, 4096, 4095, 1024 * 1024));
        System.out.println("M3_SYNTAX_ORIGINS_SNAPSHOT_PASS checks=" + checks);
    }
}
