/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package java.lang;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Contract proof for the Synexia TextFacts to M3StringFacts target map. */
public final class M3StringFactsContractTest {
    private static int checks;

    private static void check(boolean condition) {
        checks++;
        if (!condition) {
            throw new AssertionError("check " + checks);
        }
    }

    public static void main(String[] args) throws Exception {
        String expected = "A\ud83d\ude00\ud800\u00ff\u2003";
        M3StringFacts facts = M3StringFacts.of(new HiddenSequence(expected));
        check(facts.utf16Length() == expected.length());
        check(facts.codePointCount() == expected.codePointCount(0, expected.length()));
        check(facts.unpairedSurrogateCount() == 1);
        check(facts.nonBmpCodePointCount() == 1);
        check(facts.javaHashCode() == expected.hashCode());
        check(!facts.ascii());
        check(!facts.latin1());
        check(facts.containsWhitespace());

        M3StringFacts left = M3StringFacts.of(new HiddenSequence("x\ud83d"));
        M3StringFacts right = M3StringFacts.of(new HiddenSequence("\ude00y"));
        M3StringFacts joined = M3StringFacts.compose(left, right, '\ud83d', '\ude00');
        check(joined.javaHashCode() == "x\ud83d\ude00y".hashCode());
        check(joined.codePointCount() == 3);
        check(joined.unpairedSurrogateCount() == 0);
        check(joined.nonBmpCodePointCount() == 1);
        check(joined.ascii());
        check(joined.latin1());
        check(!joined.containsWhitespace());

        M3StringFacts rightFacts = M3StringFacts.of(new HiddenSequence("\u00e9\u2003"));
        M3StringFacts associativeLeft = M3StringFacts.compose(left, right, '\ud83d', '\ude00');
        M3StringFacts associative = M3StringFacts.compose(
                associativeLeft, rightFacts, 'y', '\u00e9');
        M3StringFacts direct = M3StringFacts.of(
                new HiddenSequence("x\ud83d\ude00y\u00e9\u2003"));
        check(associative.equals(direct));

        M3StringFacts empty = M3StringFacts.of(new HiddenSequence(""));
        M3StringFacts emptyJoin = M3StringFacts.compose(empty, joined, 'x', 'x');
        check(emptyJoin.equals(joined));
        check(M3StringFacts.compose(joined, empty, 'y', 'x').equals(joined));

        String map = Files.readString(
                Path.of("lexicon", "synexia-string-facts-target-map.tsv"),
                StandardCharsets.UTF_8);
        String row = Arrays.stream(map.split("\\n", -1))
                .filter(line -> line.startsWith("M3JDK_STRING_FACTS_TARGET_MAP_V1\t"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("string-facts target row missing"));
        String[] columns = row.split("\\t", -1);
        check(columns.length == 11);
        check(columns[1].equals("m3string.text-facts"));
        check(columns[2].contains("MindexStringView.java"));
        check(columns[3].equals("TextFacts"));
        check(columns[5].contains("contains_whitespace"));
        check(columns[6].equals("com.m3.text.M3StringFacts"));
        check(columns[8].equals("Apache-2.0"));
        check(columns[9].equals("ADMITTED_TYPED_RECEIVER"));

        System.out.println("M3JDK_STRING_FACTS_CONTRACT_PASS checks=" + checks
                + " fields=8 composition=PASS materialization=NONE");
    }

    private static final class HiddenSequence implements CharSequence {
        private final String value;

        HiddenSequence(String value) {
            this.value = value;
        }

        @Override
        public int length() {
            return value.length();
        }

        @Override
        public char charAt(int index) {
            return value.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new HiddenSequence(value.substring(start, end));
        }

        @Override
        public String toString() {
            throw new AssertionError("M3StringFacts materialized the CharSequence");
        }
    }
}
