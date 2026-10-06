/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary Verify owned internal M3 regex, term-FST and reasoning precompute engines
 * @modules java.base/jdk.internal.mindex
 * @run main M3OwnedPrecomputeTest
 */

import java.util.LinkedHashMap;
import java.util.Map;
import jdk.internal.mindex.M3ReasoningClosure;
import jdk.internal.mindex.M3ReasoningProgram;
import jdk.internal.mindex.M3RegexAutomaton;
import jdk.internal.mindex.M3TermFst;

public class M3OwnedPrecomputeTest {
    private static long checks;

    public static void main(String[] args) {
        testRegex();
        testTermFst();
        testReasoning();
        System.out.println("M3_OWNED_PRECOMPUTE_PASS checks=" + checks);
    }

    private static void testRegex() {
        M3RegexAutomaton regex = M3RegexAutomaton.compile("^a(b|c)+d$");
        check(regex.matches("abd"), "regex simple match");
        check(regex.matches("accbd"), "regex repeated alternation");
        check(!regex.matches("ad"), "regex plus requires one");
        check(!regex.find("xxabd"), "anchored regex search");
        check(regex.prefixCanStillMatch("ac"), "regex prefix candidate");
        check(!regex.prefixCanStillMatch("ax"), "regex prefix rejection");

        M3RegexAutomaton digits = M3RegexAutomaton.compile("\\d+");
        check(digits.find("id=12042;"), "regex unanchored find");
        check(!digits.find("none"), "regex unanchored negative");
        check(
                digits.rootHash().equals(M3RegexAutomaton.compile("\\d+").rootHash()),
                "regex deterministic image");
        expectFailure(() -> M3RegexAutomaton.compile("(a)\\1"), "backreference rejection");
        expectFailure(() -> M3RegexAutomaton.compile("a{2,4}"), "counted quantifier rejection");
    }

    private static void testTermFst() {
        Map<String, Long> terms = new LinkedHashMap<>();
        terms.put("car", 7L);
        terms.put("bar", 7L);
        terms.put("cat", 11L);
        terms.put("do", 13L);
        terms.put("dog", 17L);

        M3TermFst fst = M3TermFst.freeze(terms);
        check(fst.contains("car"), "fst exact car");
        check(!fst.contains("ca"), "fst partial not exact");
        check(fst.lookup("cat", -1L) == 11L, "fst output");
        check(fst.longestPrefixLength("dogmatic") == 3, "fst longest prefix length");
        check(fst.longestPrefixOutput("dogmatic", -1L) == 17L, "fst longest prefix output");
        check(fst.hasPrefix("ca"), "fst prefix exists");
        check(!fst.hasPrefix("zx"), "fst missing prefix");
        M3TermFst sentinel = M3TermFst.freeze(Map.of("min", Long.MIN_VALUE));
        check(sentinel.contains("min"), "fst minimum-long output remains present");
        check(sentinel.lookup("min", 7L) == Long.MIN_VALUE, "fst minimum-long output");
        check(
                fst.rootHash().equals(
                        M3TermFst.freeze(
                                Map.of("dog", 17L, "do", 13L, "cat", 11L, "bar", 7L, "car", 7L))
                            .rootHash()),
                "fst deterministic image");
    }

    private static void testReasoning() {
        M3ReasoningProgram program =
                M3ReasoningProgram.compile(
                        """
                        fact ready
                        fact !blocked
                        rule ready & !blocked -> start
                        rule start -> !ready
                        support ready -> start
                        attack blocked -> start
                        """);
        M3ReasoningClosure closure = program.evaluate();

        check(closure.entails("ready"), "reasoning initial fact");
        check(closure.entails("start"), "reasoning derived fact");
        check(closure.refutes("blocked"), "reasoning negative fact");
        check(closure.refutes("ready"), "reasoning derived negation");
        check(closure.inconsistent("ready"), "reasoning contradiction");
        check(!closure.inconsistent("start"), "reasoning consistent symbol");
        check(closure.proofRule("ready") == -1, "reasoning fact provenance");
        check(closure.proofRule("start") >= 0, "reasoning rule provenance");
        check(closure.supports("ready", "start"), "reasoning support");
        check(closure.attacks("blocked", "start"), "reasoning attack");
        check(
                program.rootHash().equals(
                        M3ReasoningProgram.compile(
                                """
                                attack blocked -> start
                                rule start -> !ready
                                support ready -> start
                                rule !blocked & ready -> start
                                fact !blocked
                                fact ready
                                """)
                            .rootHash()),
                "reasoning canonical compilation");
    }

    private static void expectFailure(Runnable action, String label) {
        checks++;
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(label);
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
