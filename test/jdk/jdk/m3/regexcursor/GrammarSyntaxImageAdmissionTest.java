/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Prepared M3 syntax tables must bind each decision to its production owner
 * @modules java.base/jdk.m3.regexcursor
 * @run main/othervm GrammarSyntaxImageAdmissionTest
 */

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import jdk.m3.regexcursor.GrammarSpecification;
import jdk.m3.regexcursor.StaticGrammarLexicon;

/** Validates sealed syntax table structural ownership and result independence from input mutation. */
public final class GrammarSyntaxImageAdmissionTest {
    private static int assertions;
    private GrammarSyntaxImageAdmissionTest() { }

    public static void main(String[] args) throws Exception {
        var fixed = List.of(new StaticGrammarLexicon.Literal("a", 1, false),
                new StaticGrammarLexicon.Literal("b", 2, false));
        var productions = List.of(new GrammarSpecification.Production(-1, new int[] {1}),
                new GrammarSpecification.Production(-2, new int[] {2}));
        GrammarSpecification compiled = GrammarSpecification.compile("syntax-owner", "2026-11-18",
                fixed, List.of(), -1, productions);
        byte[] image = compiled.syntaxImage();
        GrammarSpecification loaded = load(fixed, productions, image);
        yes(loaded.accepts(new int[] {1}));
        yes(!loaded.accepts(new int[] {2}));
        yes(Arrays.equals(image, loaded.syntaxImage()));

        ByteBuffer table = ByteBuffer.wrap(image.clone()).order(ByteOrder.BIG_ENDIAN);
        eq(table.getInt(), 0x4d334731); // M3G1
        eq(table.getInt(), 1);
        table.position(table.position() + 64); // ASCII source grammar fingerprint
        eq(table.getInt(), -1);
        int nonterminalCount = table.getInt();
        eq(nonterminalCount, 2);
        table.position(table.position() + nonterminalCount * Integer.BYTES);
        int terminalCount = table.getInt();
        eq(terminalCount, 2);
        table.position(table.position() + terminalCount * Integer.BYTES);
        int decisions = table.getInt();
        eq(decisions, 2);
        int firstDecisionAt = table.position();
        // The first key belongs to sorted nonterminal -2 and token b (ID 2).
        eq(table.getLong(firstDecisionAt), 2L);
        eq(table.getInt(firstDecisionAt + Long.BYTES), 1);
        eq(table.getLong(firstDecisionAt + 12), (1L << 32) | 1L);
        eq(table.getInt(firstDecisionAt + 20), 0);

        // Previously accepted: a signed entry for -2 named a production whose lhs is -1.
        byte[] wrongLhs = image.clone();
        ByteBuffer.wrap(wrongLhs).putInt(firstDecisionAt + Long.BYTES, 0);
        reseal(wrongLhs);
        refuse(() -> load(fixed, productions, wrongLhs));

        // The converse is also forbidden: -1 cannot expand a production owned by -2.
        byte[] otherWrongLhs = image.clone();
        ByteBuffer.wrap(otherWrongLhs).putInt(firstDecisionAt + 20, 1);
        reseal(otherWrongLhs);
        refuse(() -> load(fixed, productions, otherWrongLhs));

        // A valid hash trailer is not proof that a transition is in range.
        byte[] wrongIndex = image.clone();
        ByteBuffer.wrap(wrongIndex).putInt(firstDecisionAt + Long.BYTES, 2);
        reseal(wrongIndex);
        refuse(() -> load(fixed, productions, wrongIndex));

        // The immutable restored program never retains the caller's mutable image.
        byte[] mutable = image.clone();
        GrammarSpecification snapshot = load(fixed, productions, mutable);
        Arrays.fill(mutable, (byte) 0);
        yes(snapshot.accepts(new int[] {1}));
        yes(!snapshot.accepts(new int[] {2}));

        // Thousands of correct and malformed snapshots exercise the admission guard.
        Random rng = new Random(0x51a11);
        for (int i = 0; i < 2_000; i++) {
            byte[] trial = image.clone();
            if ((i & 1) != 0) {
                int slot = (i & 2) == 0 ? firstDecisionAt + 8 : firstDecisionAt + 20;
                ByteBuffer.wrap(trial).putInt(slot, rng.nextBoolean() ? -1 : 99);
                reseal(trial);
                refuse(() -> load(fixed, productions, trial));
            } else {
                var accepted = load(fixed, productions, trial);
                yes(accepted.accepts(new int[]{1}));
            }
        }
        System.out.println("GrammarSyntaxImageAdmissionTest PASS assertions=" + assertions);
    }

    private static GrammarSpecification load(List<StaticGrammarLexicon.Literal> fixed,
            List<GrammarSpecification.Production> productions, byte[] image) {
        return GrammarSpecification.compileWithSyntaxImage("syntax-owner", "2026-11-18",
                fixed, List.of(), -1, productions, image);
    }

    private static void reseal(byte[] image) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(
                    Arrays.copyOf(image, image.length - 32));
            System.arraycopy(hash, 0, image, image.length - 32, hash.length);
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static void yes(boolean value) {
        if (!value) throw new AssertionError("assertion " + assertions);
        assertions++;
    }

    private static void eq(long actual, long expected) { yes(actual == expected); }

    private static void refuse(Runnable task) {
        try { task.run(); throw new AssertionError("expected refusal " + assertions); }
        catch (IllegalArgumentException expected) { assertions++; }
    }
}
