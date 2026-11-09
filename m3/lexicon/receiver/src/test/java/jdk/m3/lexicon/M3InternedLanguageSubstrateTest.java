// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.lexicon;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

//FLLDCIM: TEST - recipe/interned-language-20261106 gate 2: the substrate checks of the donor LexiconSelfTest (blob 0efa44a53b9d, stale against its own tree) and the plan's generated inputs (plans/m3scale.tsv rank 4)
/**
 * Substrate oracle of the relocated interned-language token types. Carries the seven checks of the donor's
 * {@code LexiconSelfTest} that touch only the substrate (token identity is numeric, a known word is not overflow,
 * a space is one process-independent symbol token, O(1) rope composition count, rope order, whitespace never enters
 * the overflow table) and the generated inputs of the convergence plan: every language id with the word-id
 * boundaries, random rope trees against their flattened leaves, one hundred thousand overflow strings across three
 * language ids, and every shared symbol through find/resolve. Figures are asserted and printed.
 */
final class M3InternedLanguageSubstrateTest {
    private static final long SEED = 20261106L;
    private static final int LANGUAGES = 0x10000;
    private static final int[] WORD_BOUNDARIES = {0, 1, Integer.MAX_VALUE};
    private static final int ROPE_LEAVES = 10_000;
    private static final int ROPE_DEPTH = 12;
    private static final int OVERFLOW_STRINGS = 100_000;
    private static final int OVERFLOW_LANGUAGES = 3;
    private static final int SYMBOL_COUNT = 22;

    @Test
    void donorSelfTestSubstrateChecks() {
        long helloA = M3TokenId.pack(0, 1);
        long helloB = M3TokenId.pack(0, 1);
        assertEquals(helloA, helloB, "known token identity is numeric");
        assertFalse(M3TokenId.isOverflow(helloA), "known word does not enter overflow pool");
        M3PackedText paragraph = new M3PackedText(new long[] {M3TokenId.pack(0, 1), M3SharedSymbols.find(" "), M3TokenId.pack(0, 0)});
        assertEquals(3, paragraph.size(), "word-space-word becomes three token ids");
        assertTrue(M3TokenId.isSymbol(paragraph.token(1)), "space is a fixed shared-symbol token");
        assertEquals(M3SharedSymbols.SPACE, paragraph.token(1), "space has one process-independent numeric id");
        M3OverflowInterner overflow = new M3OverflowInterner();
        assertNotEquals(-1L, M3SharedSymbols.find(" "), "common whitespace is a shared symbol");
        assertEquals(0, overflow.size(), "common whitespace never enters overflow strings");
        M3TokenRope composed = M3TokenRope.concat(M3TokenRope.leaf(paragraph), M3TokenRope.leaf(paragraph));
        assertEquals(6L, composed.tokenCount(), "O(1) rope composition count");
        long[] twice = new long[6];
        System.arraycopy(paragraph.toArray(), 0, twice, 0, 3);
        System.arraycopy(paragraph.toArray(), 0, twice, 3, 3);
        assertArrayEquals(twice, flatten(composed), "rope materialization order");
        System.out.println("SUBSTRATE donor_self_test_checks=7");
    }

    @Test
    void everyLanguageIdWithWordBoundariesRoundTrips() {
        long cases = 0;
        for (int language = 0; language < LANGUAGES; language++) {
            for (int word : WORD_BOUNDARIES) {
                long token = M3TokenId.pack(language, word);
                assertEquals(language, M3TokenId.languageId(token));
                assertEquals(word, M3TokenId.wordId(token));
                assertEquals(language == M3TokenId.SYMBOL_LANGUAGE_ID, M3TokenId.isSymbol(token));
                assertEquals(language == M3TokenId.OVERFLOW_LANGUAGE_ID, M3TokenId.isOverflow(token));
                assertEquals(((long) language << 32) | word, token);
                cases++;
            }
        }
        assertEquals(LANGUAGES * (long) WORD_BOUNDARIES.length, cases);
        assertEquals("languageId: 0..65535", assertThrows(IllegalArgumentException.class, () -> M3TokenId.pack(-1, 0)).getMessage());
        assertEquals("languageId: 0..65535", assertThrows(IllegalArgumentException.class, () -> M3TokenId.pack(LANGUAGES, 0)).getMessage());
        assertEquals("wordId must be non-negative", assertThrows(IllegalArgumentException.class, () -> M3TokenId.pack(0, -1)).getMessage());
        assertEquals(0xfffe, M3TokenId.SYMBOL_LANGUAGE_ID);
        assertEquals(0xffff, M3TokenId.OVERFLOW_LANGUAGE_ID);
        System.out.println("SUBSTRATE token_round_trips=" + cases + " refusals=3");
    }

    @Test
    void randomRopeTreesFlattenInLeafOrder() {
        SplittableRandom random = new SplittableRandom(SEED);
        List<M3TokenRope> leaves = new ArrayList<>(ROPE_LEAVES);
        List<long[]> arrays = new ArrayList<>(ROPE_LEAVES);
        long total = 0;
        for (int i = 0; i < ROPE_LEAVES; i++) {
            long[] tokens = new long[random.nextInt(0, 8)];
            for (int j = 0; j < tokens.length; j++) {
                tokens[j] = M3TokenId.pack(random.nextInt(LANGUAGES), random.nextInt(Integer.MAX_VALUE));
            }
            M3PackedText text = new M3PackedText(tokens);
            assertEquals(new M3PackedText(tokens), text, "packed text equality is by content");
            assertEquals(text.hashCode(), new M3PackedText(tokens.clone()).hashCode());
            leaves.add(M3TokenRope.leaf(text));
            arrays.add(tokens);
            total += tokens.length;
        }
        int trees = 0;
        int maxDepth = 0;
        int cursor = 0;
        while (cursor < ROPE_LEAVES) {
            int width = Math.min(ROPE_LEAVES - cursor, random.nextInt(1, 1 << random.nextInt(1, ROPE_DEPTH + 1)));
            List<M3TokenRope> level = new ArrayList<>(leaves.subList(cursor, cursor + width));
            long[] expected = concatenate(arrays.subList(cursor, cursor + width));
            int depth = 0;
            while (level.size() > 1) {
                List<M3TokenRope> next = new ArrayList<>();
                for (int i = 0; i < level.size(); i += 2) {
                    next.add(i + 1 < level.size() ? M3TokenRope.concat(level.get(i), level.get(i + 1)) : level.get(i));
                }
                level = next;
                depth++;
            }
            M3TokenRope tree = level.get(0);
            assertEquals(expected.length, tree.tokenCount(), "rope token count");
            assertArrayEquals(expected, flatten(tree), "rope forEach order equals flattened leaves");
            assertEquals(tree.fingerprint(), rebuild(leaves.subList(cursor, cursor + width)).fingerprint(), "fingerprint is structural and deterministic");
            maxDepth = Math.max(maxDepth, depth);
            trees++;
            cursor += width;
        }
        assertTrue(maxDepth <= ROPE_DEPTH, "depth bound");
        assertThrows(NullPointerException.class, () -> M3TokenRope.concat(null, leaves.get(0)));
        assertThrows(NullPointerException.class, () -> M3TokenRope.leaf(null));
        System.out.println("SUBSTRATE rope_leaves=" + ROPE_LEAVES + " rope_trees=" + trees + " rope_max_depth=" + maxDepth + " rope_tokens=" + total);
    }

    @Test
    void overflowInternerAcrossThreeLanguages() {
        M3OverflowInterner interner = new M3OverflowInterner();
        SplittableRandom random = new SplittableRandom(SEED);
        List<String> raws = new ArrayList<>(OVERFLOW_STRINGS);
        List<Long> tokens = new ArrayList<>(OVERFLOW_STRINGS);
        int duplicates = 0;
        for (int i = 0; i < OVERFLOW_STRINGS; i++) {
            String raw = (i % 7 == 0 ? "résumé-" : "w") + random.nextInt(OVERFLOW_STRINGS);
            int language = i % OVERFLOW_LANGUAGES;
            int before = interner.size();
            long token = interner.intern(language, raw);
            assertTrue(M3TokenId.isOverflow(token), "overflow tokens carry the overflow language id");
            assertEquals(token, interner.intern(language, raw), "re-interning returns the same token");
            if (interner.size() == before) {
                duplicates++;
            }
            raws.add(raw);
            tokens.add(token);
        }
        assertEquals(OVERFLOW_STRINGS - duplicates, interner.size(), "ids are dense");
        for (int i = 0; i < OVERFLOW_STRINGS; i++) {
            M3OverflowInterner.Entry entry = interner.entry(M3TokenId.wordId(tokens.get(i)));
            assertEquals(i % OVERFLOW_LANGUAGES, entry.sourceLanguageId());
            assertEquals(Normalizer.normalize(raws.get(i), Normalizer.Form.NFC), entry.text(), "entries are NFC normalized");
        }
        assertEquals(interner.intern(0, "résumé"), interner.intern(0, "résumé"), "NFC boundary normalization");
        assertNotEquals(interner.intern(0, "same"), interner.intern(1, "same"), "the source language is part of the key");
        assertThrows(NullPointerException.class, () -> interner.intern(0, null));
        assertThrows(IndexOutOfBoundsException.class, () -> interner.entry(interner.size()));
        System.out.println("SUBSTRATE overflow_strings=" + OVERFLOW_STRINGS + " overflow_languages=" + OVERFLOW_LANGUAGES + " overflow_entries=" + interner.size() + " overflow_duplicates=" + duplicates);
    }

    @Test
    void everySharedSymbolRoundTrips() {
        assertEquals(SYMBOL_COUNT, M3SharedSymbols.size());
        assertEquals(M3TokenId.pack(M3TokenId.SYMBOL_LANGUAGE_ID, 0), M3SharedSymbols.SPACE);
        assertEquals(M3TokenId.pack(M3TokenId.SYMBOL_LANGUAGE_ID, 1), M3SharedSymbols.NEWLINE);
        assertEquals(M3TokenId.pack(M3TokenId.SYMBOL_LANGUAGE_ID, 2), M3SharedSymbols.TAB);
        assertEquals(" ", M3SharedSymbols.resolve(M3SharedSymbols.SPACE));
        assertEquals("\n", M3SharedSymbols.resolve(M3SharedSymbols.NEWLINE));
        assertEquals("\t", M3SharedSymbols.resolve(M3SharedSymbols.TAB));
        for (int id = 0; id < M3SharedSymbols.size(); id++) {
            long token = M3TokenId.pack(M3TokenId.SYMBOL_LANGUAGE_ID, id);
            String text = M3SharedSymbols.resolve(token);
            assertEquals(token, M3SharedSymbols.find(text), "find(resolve(id)) == id for " + id);
            assertTrue(M3TokenId.isSymbol(token));
        }
        assertEquals(-1L, M3SharedSymbols.find("zz"));
        assertEquals(-1L, M3SharedSymbols.find(""));
        assertThrows(IllegalArgumentException.class, () -> M3SharedSymbols.resolve(M3TokenId.pack(0, 0)));
        assertThrows(IndexOutOfBoundsException.class, () -> M3SharedSymbols.resolve(M3TokenId.pack(M3TokenId.SYMBOL_LANGUAGE_ID, SYMBOL_COUNT)));
        System.out.println("SUBSTRATE shared_symbols=" + M3SharedSymbols.size() + " round_trips=" + M3SharedSymbols.size());
    }

    private static long[] flatten(M3TokenRope rope) {
        long[] out = new long[Math.toIntExact(rope.tokenCount())];
        int[] cursor = {0};
        rope.forEach(token -> out[cursor[0]++] = token);
        assertEquals(out.length, cursor[0]);
        return out;
    }

    private static long[] concatenate(List<long[]> arrays) {
        int length = 0;
        for (long[] array : arrays) {
            length += array.length;
        }
        long[] out = new long[length];
        int offset = 0;
        for (long[] array : arrays) {
            System.arraycopy(array, 0, out, offset, array.length);
            offset += array.length;
        }
        return out;
    }

    private static M3TokenRope rebuild(List<M3TokenRope> leaves) {
        List<M3TokenRope> level = new ArrayList<>(leaves);
        while (level.size() > 1) {
            List<M3TokenRope> next = new ArrayList<>();
            for (int i = 0; i < level.size(); i += 2) {
                next.add(i + 1 < level.size() ? M3TokenRope.concat(level.get(i), level.get(i + 1)) : level.get(i));
            }
            level = next;
        }
        return level.get(0);
    }
}
