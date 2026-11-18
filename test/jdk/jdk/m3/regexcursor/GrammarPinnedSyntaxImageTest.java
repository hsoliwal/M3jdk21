/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Independently pinned M3 syntax image admission cannot accept rechecksummed tables
 * @modules java.base/jdk.m3.regexcursor
 * @run main/othervm GrammarPinnedSyntaxImageTest
 */
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import jdk.m3.regexcursor.GrammarSpecification;
import jdk.m3.regexcursor.StaticGrammarLexicon;

/** The embedded M3G1 checksum is not a substitute for an external SHA-256 pin. */
public final class GrammarPinnedSyntaxImageTest {
    private static long assertions;
    private static final List<StaticGrammarLexicon.Literal> LEXEMES = List.of(
            new StaticGrammarLexicon.Literal("a", 1, false),
            new StaticGrammarLexicon.Literal("b", 2, false));
    private static final List<GrammarSpecification.Production> PRODUCTIONS = List.of(
            new GrammarSpecification.Production(-1, new int[]{1}),
            new GrammarSpecification.Production(-1, new int[]{2}));

    private GrammarPinnedSyntaxImageTest() { }

    public static void main(String[] args) throws Exception {
        GrammarSpecification original = GrammarSpecification.compile(
                "pin-proof", "2026-11-18", LEXEMES, List.of(), -1, PRODUCTIONS);
        byte[] authentic = original.syntaxImage();
        String pinned = digestHex(authentic);
        GrammarSpecification restored = load(authentic, pinned);
        check(restored.accepts(new int[]{1}));
        check(restored.accepts(new int[]{2}));
        check(!restored.accepts(new int[]{}));
        check(!restored.accepts(new int[]{1, 2}));
        check(restored.grammarSha256().equals(original.grammarSha256()));
        check(Arrays.equals(restored.syntaxImage(), authentic));
        check(load(authentic, pinned.toUpperCase(java.util.Locale.ROOT))
                .accepts(new int[]{1}));

        // Decode the exact M3G1 table shape, change the production decision
        // for lookahead 'a' to another production of the SAME nonterminal,
        // and recompute the unkeyed embedded trailer. Grammar fingerprint is
        // unchanged; only the *independently supplied* digest detects this.
        byte[] altered = authentic.clone();
        ByteBuffer table = ByteBuffer.wrap(altered);
        check(table.getInt() == 0x4d334731);
        check(table.getInt() == 1);
        table.position(table.position() + 64); // fixed source fingerprint
        check(table.getInt() == -1);             // start symbol
        int nonterminalCount = table.getInt();
        check(nonterminalCount == 1);
        table.position(table.position() + 4 * nonterminalCount);
        int terminalCount = table.getInt();
        check(terminalCount == 2);
        table.position(table.position() + 4 * terminalCount);
        int decisions = table.getInt();
        check(decisions == 2);
        check(table.getLong() == 1L);           // first lookahead
        int firstValueOffset = table.position();
        check(table.getInt() == 0);
        table.putInt(firstValueOffset, 1);      // same-LHS, WRONG decision
        recheckEmbeddedDigest(altered);
        check(!digestHex(altered).equals(pinned));

        reject(() -> load(altered, pinned));
        reject(() -> load(altered, "0".repeat(64)));
        reject(() -> load(authentic, "short"));
        reject(() -> load(authentic, "z".repeat(64)));
        rejectNull(() -> load(authentic, null));
        rejectNull(() -> load(null, pinned));
        reject(() -> load(new byte[17], pinned));

        // Even a structurally valid rechecksummed image with an unchanged
        // grammar fingerprint must be refused by a *separate* build pin.
        Random random = new Random(0x514dd7eeL);
        for (int trial = 0; trial < 4096; trial++) {
            byte[] mutation = authentic.clone();
            int offset = random.nextInt(mutation.length - 32);
            mutation[offset] ^= (byte) (1 << random.nextInt(8));
            recheckEmbeddedDigest(mutation);
            reject(() -> load(mutation, pinned));
        }
        check(restored.accepts(new int[]{1}));
        check(restored.accepts(new int[]{2}));
        System.out.println("GrammarPinnedSyntaxImageTest PASS assertions=" + assertions);
    }

    private static GrammarSpecification load(byte[] image, String sha) {
        return GrammarSpecification.compileWithPinnedSyntaxImage(
                "pin-proof", "2026-11-18", LEXEMES, List.of(), -1, PRODUCTIONS, image, sha);
    }

    private static void recheckEmbeddedDigest(byte[] bytes) throws Exception {
        byte[] trailer = MessageDigest.getInstance("SHA-256").digest(
                Arrays.copyOf(bytes, bytes.length - 32));
        System.arraycopy(trailer, 0, bytes, bytes.length - 32, trailer.length);
    }

    private static String digestHex(byte[] bytes) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static void check(boolean value) {
        if (!value) throw new AssertionError("pinned grammar assertion " + assertions);
        assertions++;
    }

    private static void reject(Runnable runnable) {
        try { runnable.run(); }
        catch (IllegalArgumentException correctRefusal) {
            assertions++;
            return;
        }
        throw new AssertionError("accepted unpinned grammar image at assertion " + assertions);
    }

    private static void rejectNull(Runnable runnable) {
        try { runnable.run(); }
        catch (NullPointerException correctRefusal) {
            assertions++;
            return;
        }
        throw new AssertionError("accepted null grammar input at assertion " + assertions);
    }
}
