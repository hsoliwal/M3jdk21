/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary M3 GrammarSpecification mapped images, left-recursion safety, canonical token index
 * @modules java.base/jdk.m3.regexcursor java.base/jdk.internal.mindex
 * @run main/othervm GrammarSpecificationOwnerIntegrationTest
 */

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import jdk.internal.mindex.M3TokenIndex;
import jdk.m3.regexcursor.GrammarSpecification;
import jdk.m3.regexcursor.StaticGrammarLexicon;

/** Independent old/new-owner differential and malformed-but-resealed image fixture. */
public final class GrammarSpecificationOwnerIntegrationTest {
    private static long checks;
    private GrammarSpecificationOwnerIntegrationTest() { }

    public static void main(String[] args) throws Exception {
        var fixed = List.of(new StaticGrammarLexicon.Literal("a", 1, false));
        var productions = List.of(new GrammarSpecification.Production(-1, new int[] {1, -1}),
                new GrammarSpecification.Production(-1, new int[0]));
        var original = GrammarSpecification.compile("test", "2026", fixed,
                List.of(), -1, productions);
        byte[] bytes = original.image();
        String sha = GrammarSpecification.imageSha256(bytes);
        yes(original.accepts(new int[] {1, 1, 1}));
        yes(!original.accepts(new int[] {2}));
        var loaded = GrammarSpecification.fromImage(bytes, sha);
        yes(loaded.accepts(new int[0]));

        // Slice over a *direct* nonzero-offset image with a retained mark.
        ByteBuffer direct = ByteBuffer.allocateDirect(bytes.length + 7);
        direct.position(3);
        direct.put(bytes);
        direct.position(3);
        direct.mark();
        direct.limit(3 + bytes.length);
        var fromMappedView = GrammarSpecification.fromImage(direct, sha);
        eq(direct.position(), 3);
        direct.reset();
        eq(direct.position(), 3);
        yes(fromMappedView.accepts(new int[] {1, 1, 1}));
        yes(GrammarSpecification.fromImage(direct.asReadOnlyBuffer(), sha)
                .accepts(IntBuffer.wrap(new int[] {1, 1}).asReadOnlyBuffer()));

        byte[] copy = bytes.clone();
        var first = GrammarSpecification.fromImage(copy, sha);
        copy[5] ^= 1;
        yes(first.accepts(new int[] {1, 1}));
        refuse(() -> GrammarSpecification.fromImage(copy, sha));
        refuse(() -> GrammarSpecification.fromImage(ByteBuffer.wrap(bytes), "0".repeat(64)));

        // A correctly re-signed image must still reject a nullable left-corner cycle.
        // The grammar fingerprint and both SHA-256 layers are valid; only the
        // structural recursion invariant refuses this input.
        byte[] cycle = imageWithRecursiveProduction(true);
        String cycleSha = GrammarSpecification.imageSha256(cycle);
        try {
            GrammarSpecification.fromImage(cycle, cycleSha);
            throw new AssertionError("accepted signed left-recursive grammar image");
        } catch (IllegalArgumentException expected) {
            yes(expected.getMessage() != null
                    && expected.getMessage().contains("left-recursive"));
        }
        byte[] safe = imageWithRecursiveProduction(false);
        yes(GrammarSpecification.fromImage(safe, GrammarSpecification.imageSha256(safe))
                .accepts(new int[] {1, 1, 1}));

        Random random = new Random(0x51afeL);
        for (int round = 0; round < 10_000; round++) {
            int n = random.nextInt(30);
            int[] kinds = new int[2 * n + 1];
            int[] lexemes = new int[kinds.length];
            long[] starts = new long[kinds.length];
            long[] ends = new long[kinds.length];
            long[] flags = new long[kinds.length];
            for (int i = 0; i < kinds.length; i++) {
                kinds[i] = i % 2 == 0 ? 1 : 17;
                starts[i] = i;
                ends[i] = i + 1L;
                flags[i] = i % 2 == 0 ? 0L : M3TokenIndex.TRIVIA;
            }
            var index = M3TokenIndex.of(round + 1L, kinds, lexemes, starts, ends, flags);
            yes(loaded.accepts(index, false) == (n == 0));
            yes(loaded.accepts(index, true));
            yes(loaded.accepts(index, round + 1L, true));
            refuse(() -> loaded.accepts(index, -1L, true));
        }
        System.out.println("GrammarSpecificationOwnerIntegrationTest PASS checks=" + checks);
    }

    private static byte[] imageWithRecursiveProduction(boolean leftRecursive) throws Exception {
        var fixed = List.of(new StaticGrammarLexicon.Literal("a", 1, false));
        String lexicalDigest = StaticGrammarLexicon.compile("test", "2026", fixed).grammarSha256();
        int[][] rhs = leftRecursive ? new int[][] {{-1}, {}} : new int[][] {{1, -1}, {}};
        String fingerprint = fingerprint(lexicalDigest, -1, rhs);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(0x4d334752); // M3GR
        out.writeInt(2); // v2 UTF-16 text
        text(out, "test"); text(out, "2026");
        out.writeInt(1); text(out, "a"); out.writeInt(1); out.writeBoolean(false);
        out.writeInt(0); // no regex validators
        out.writeInt(-1); // start nonterminal
        out.writeInt(2); // two productions
        for (int[] symbols : rhs) {
            out.writeInt(-1);
            out.writeInt(symbols.length);
            for (int symbol : symbols) out.writeInt(symbol);
        }
        out.writeInt(2); // prediction entries sorted by unsigned terminal ID
        out.writeLong(0L); out.writeInt(1); // EOF -> epsilon
        out.writeLong(1L); out.writeInt(0); // token a -> first production
        text(out, fingerprint);
        out.flush();
        byte[] payload = bytes.toByteArray();
        byte[] image = Arrays.copyOf(payload, payload.length + 32);
        System.arraycopy(digest(payload), 0, image, payload.length, 32);
        return image;
    }

    private static String fingerprint(String lexicalDigest, int start, int[][] rhs) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            hashText(md, "M3-GRAMMAR-SPEC-V1");
            hashText(md, lexicalDigest);
            integer(md, 0); // zero regex validators
            integer(md, start);
            integer(md, rhs.length);
            for (int[] rule : rhs) {
                integer(md, -1);
                integer(md, rule.length);
                for (int symbol : rule) integer(md, symbol);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static void text(DataOutputStream out, String text) throws IOException {
        out.writeInt(text.length());
        for (int i = 0; i < text.length(); i++) out.writeChar(text.charAt(i));
    }

    private static void hashText(MessageDigest md, String text) {
        integer(md, text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            md.update((byte) (c >>> 8));
            md.update((byte) c);
        }
    }

    private static void integer(MessageDigest md, int value) {
        md.update((byte) (value >>> 24));
        md.update((byte) (value >>> 16));
        md.update((byte) (value >>> 8));
        md.update((byte) value);
    }

    private static byte[] digest(byte[] source) {
        try { return MessageDigest.getInstance("SHA-256").digest(source); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    private static void yes(boolean trueValue) {
        if (!trueValue) throw new AssertionError("test assertion " + checks);
        checks++;
    }
    private static void eq(int actual, int expected) { yes(actual == expected); }
    private static void refuse(Runnable action) {
        try { action.run(); throw new AssertionError("expected refusal " + checks); }
        catch (IllegalArgumentException expected) { checks++; }
    }
}
