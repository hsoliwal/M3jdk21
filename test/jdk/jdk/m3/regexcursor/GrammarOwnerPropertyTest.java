/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary GrammarSpecification image and M3TokenIndex cross-accessor differential corpus
 * @modules java.base/jdk.m3.regexcursor java.base/jdk.internal.mindex
 * @run main/othervm GrammarOwnerPropertyTest
 */
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.internal.mindex.M3TokenIndex;
import jdk.m3.regexcursor.GrammarSpecification;
import jdk.m3.regexcursor.StaticGrammarLexicon;

public final class GrammarOwnerPropertyTest {
    private static long assertions;
    private GrammarOwnerPropertyTest() { }

    public static void main(String[] args) {
        Random random = new Random(0x75434037c86aL);
        for (int grammar = 0; grammar < 32; grammar++) {
            int terminalCount = 1 + (grammar % 12);
            int[] ids = new int[terminalCount];
            List<StaticGrammarLexicon.Literal> fixed = new ArrayList<>();
            List<GrammarSpecification.Production> rules = new ArrayList<>();
            for (int k = 0; k < terminalCount; k++) {
                int id = 101 + k * 3;
                ids[k] = id;
                fixed.add(new StaticGrammarLexicon.Literal("word-" + k, id, false));
                rules.add(new GrammarSpecification.Production(-1, new int[]{id, -1}));
            }
            rules.add(new GrammarSpecification.Production(-1, new int[0]));
            GrammarSpecification compiled = GrammarSpecification.compile("fuzz", "r" + grammar,
                    fixed, List.of(), -1, rules);
            byte[] image = compiled.image();
            GrammarSpecification read = GrammarSpecification.fromImage(image,
                    GrammarSpecification.imageSha256(image));
            GrammarSpecification pred = GrammarSpecification.compileWithSyntaxImage(
                    "fuzz", "r" + grammar, fixed, List.of(), -1, rules, compiled.syntaxImage());
            verify(compiled.productionCount() == read.productionCount());
            verify(compiled.productionCount() == pred.productionCount());
            for (int round = 0; round < 1600; round++) {
                int count = random.nextInt(45);
                int[] tokens = new int[count];
                for (int i = 0; i < count; i++) {
                    int choice = random.nextInt(20);
                    tokens[i] = choice == 0 ? 0 : choice == 1 ? Integer.MAX_VALUE
                            : choice == 2 ? -1 : ids[random.nextInt(ids.length)];
                }
                boolean expected = Arrays.stream(tokens)
                        .allMatch(id -> Arrays.binarySearch(ids, id) >= 0);
                verify(compiled.accepts(tokens) == expected);
                verify(read.accepts(tokens) == expected);
                verify(pred.accepts(tokens) == expected);
                verify(read.accepts(IntBuffer.wrap(tokens).asReadOnlyBuffer()) == expected);
                verify(read.accepts((long) count, i -> tokens[(int) i]) == expected);
                // Streaming API explicitly reserves 0 for permanent EOF; do not
                // model an embedded 0 as a token. Negative IDs also violate its
                // positive-token contract, so fuzz them only in indexed views.
                if (Arrays.stream(tokens).allMatch(id -> id > 0)) {
                    AtomicInteger supplierAt = new AtomicInteger();
                    boolean streamed = read.accepts(() -> {
                        int at = supplierAt.getAndIncrement();
                        return at == tokens.length ? 0 : tokens[at];
                    });
                    verify(streamed == expected);
                }
                int length = count * 2 + 1;
                int[] kinds = new int[length];
                int[] lexemes = new int[length];
                long[] starts = new long[length];
                long[] ends = new long[length];
                long[] flags = new long[length];
                for (int i = 0; i < length; i++) {
                    starts[i] = i;
                    ends[i] = i + 1L;
                    if ((i & 1) == 0) {
                        kinds[i] = i == length - 1 ? 888 : tokens[i >>> 1];
                        flags[i] = i == length - 1 ? M3TokenIndex.TRIVIA : 0;
                    } else {
                        kinds[i] = 888;
                        flags[i] = M3TokenIndex.TRIVIA;
                    }
                }
                M3TokenIndex index = M3TokenIndex.of(42, kinds, lexemes, starts, ends, flags);
                verify(read.accepts(index, true) == expected);
                verify(read.accepts(index, 42L, true) == expected);
                if (count > 0) verify(!read.accepts(index, false));
                try {
                    read.accepts(index, 43L, true);
                    throw new AssertionError("stale generation accepted");
                } catch (IllegalArgumentException expectedFailure) {
                    assertions++;
                }
            }
        }
        System.out.println("GrammarOwnerPropertyTest PASS assertions=" + assertions);
    }
    private static void verify(boolean good) {
        if (!good) throw new AssertionError("grammar differential assertion " + assertions);
        assertions++;
    }
}
