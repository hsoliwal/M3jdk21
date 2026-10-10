/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package java.lang;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Contract proof for the Synexia TextFacts to the canonical M3String receiver. */
public final class M3StringFactsContractTest {
    private static int checks;

    private static void check(boolean condition) {
        checks++;
        if (!condition) {
            throw new AssertionError("check " + checks);
        }
    }

    private static void sameFacts(M3StringFacts left, M3StringFacts right) {
        check(left.utf16Length == right.utf16Length);
        check(left.codePointCount == right.codePointCount);
        check(left.unpairedSurrogateCount == right.unpairedSurrogateCount);
        check(left.nonBmpCodePointCount == right.nonBmpCodePointCount);
        check(left.javaHashCode == right.javaHashCode);
        check(left.ascii == right.ascii);
        check(left.latin1 == right.latin1);
        check(left.containsWhitespace == right.containsWhitespace);
    }

    public static void main(String[] args) throws Exception {
        String expected = "A\ud83d\ude00\ud800\u00ff\u2003";
        M3StringFacts facts = M3String.canonicalize(expected).facts();
        check(facts.utf16Length == expected.length());
        check(facts.codePointCount == expected.codePointCount(0, expected.length()));
        check(facts.unpairedSurrogateCount == 1);
        check(facts.nonBmpCodePointCount == 1);
        check(facts.javaHashCode == expected.hashCode());
        check(facts.javaHash == facts.javaHashCode);
        check(!facts.ascii);
        check(!facts.latin1);
        check(facts.containsWhitespace);

        M3StringFacts left = M3String.canonicalize("x\ud83d").facts();
        M3StringFacts right = M3String.canonicalize("\ude00y").facts();
        M3StringFacts joined = M3StringFacts.compose(left, right);
        check(joined.javaHashCode == "x\ud83d\ude00y".hashCode());
        check(joined.codePointCount == 3);
        check(joined.unpairedSurrogateCount == 0);
        check(joined.nonBmpCodePointCount == 1);
        check(!joined.ascii);
        check(!joined.latin1);
        check(!joined.containsWhitespace);

        M3StringFacts rightFacts = M3String.canonicalize("\u00e9\u2003").facts();
        M3StringFacts associativeLeft = M3StringFacts.compose(left, right);
        M3StringFacts associative = M3StringFacts.compose(associativeLeft, rightFacts);
        M3StringFacts direct = M3String.canonicalize("x\ud83d\ude00y\u00e9\u2003").facts();
        sameFacts(associative, direct);

        M3StringFacts empty = M3String.empty().facts();
        sameFacts(M3StringFacts.compose(empty, joined), joined);
        sameFacts(M3StringFacts.compose(joined, empty), joined);

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
        check(columns[6].equals("java.lang.M3StringFacts"));
        check(columns[7].contains("scan(M3String)"));
        check(columns[7].contains("compose(M3StringFacts,M3StringFacts)"));
        check(columns[8].equals("Apache-2.0"));
        check(columns[9].equals("ADMITTED_TYPED_RECEIVER"));

        System.out.println("M3JDK_STRING_FACTS_CONTRACT_PASS checks=" + checks
                + " fields=8 composition=PASS materialization=NONE");
    }
}
