// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.regexcursor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/** Long indexed, array, borrowed direct-buffer and negative-input parity. */
public final class GrammarStreamingProof {
    private static long checks;
    private GrammarStreamingProof() { }

    public static void main(String[] args) {
        var parsed = EbnfGrammar.normalize("S = ID, {'+', ID};", "S", Map.of("ID", 1, "+", 2));
        var spec = GrammarSpecification.compile("stream", "1",
                List.of(new StaticGrammarLexicon.Literal("+", 2, false)),
                List.of(new GrammarSpecification.RegexRule(1, "[a-z]+", 0, 0)),
                parsed.startSymbol(), parsed.productions());
        Random random = new Random(0x5a1beeeL);
        for (int trial = 0; trial < 50_000; trial++) {
            int count = random.nextInt(50);
            int[] tokens = new int[count];
            for (int i = 0; i < count; i++) tokens[i] = random.nextInt(5) - 1;
            boolean expected = spec.accepts(tokens);
            yes(expected == spec.accepts(count, index -> tokens[(int) index]));
            IntBuffer direct = ByteBuffer.allocateDirect((count + 5) * 4)
                    .order(ByteOrder.nativeOrder()).asIntBuffer();
            direct.position(3);
            direct.put(tokens);
            direct.position(3);
            direct.limit(3 + count);
            boolean result = spec.accepts(direct);
            yes(result == expected);
            yes(direct.position() == 3);
            yes(direct.limit() == count + 3);
        }
        int pairs = 500_000;
        long tokens = pairs * 2L - 1;
        AtomicLong reads = new AtomicLong();
        yes(spec.accepts(tokens, at -> {
            reads.incrementAndGet();
            return (at & 1L) == 0 ? 1 : 2;
        }));
        yes(reads.get() == tokens);
        yes(!spec.accepts(tokens, at -> at == 100 ? 9 : (at & 1L) == 0 ? 1 : 2));
        refuse(() -> spec.accepts(-1L, at -> 1));
        refuseNull(() -> spec.accepts(1L, null));
        refuseNull(() -> spec.accepts((IntBuffer) null));
        System.out.println("GrammarStreamingProof PASS checks=" + checks + " longTokens=" + tokens);
    }

    private static void yes(boolean result) {
        checks++;
        if (!result) throw new AssertionError("unexpected streaming decision at " + checks);
    }

    private static void refuse(Runnable action) {
        checks++;
        try { action.run(); throw new AssertionError("expected refusal " + checks); }
        catch (IllegalArgumentException expected) { /* pass */ }
    }

    private static void refuseNull(Runnable action) {
        checks++;
        try { action.run(); throw new AssertionError("expected null refusal " + checks); }
        catch (NullPointerException expected) { /* pass */ }
    }
}
