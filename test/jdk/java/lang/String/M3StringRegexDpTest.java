/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary Bounded composite-aware dot/star DP with independent memoized and JDK oracles
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringRegexDpTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringRegexDpTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class M3StringRegexDpTest {
    private static final Field STRING_M3;
    private static final Method TRY_MATCH;
    private static final Method CANONICALIZE;
    private static long checks;

    static {
        try {
            STRING_M3 = String.class.getDeclaredField("m3");
            STRING_M3.setAccessible(true);
            Class<?> owner = Class.forName("java.lang.M3String");
            Class<?> dp = Class.forName("java.lang.M3StringRegexDp");
            TRY_MATCH = dp.getDeclaredMethod("tryMatch", owner, String.class);
            TRY_MATCH.setAccessible(true);
            CANONICALIZE = owner.getDeclaredMethod("canonicalize", String.class);
            CANONICALIZE.setAccessible(true);
        } catch (ReflectiveOperationException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    public static void main(String[] arguments) throws Exception {
        deterministic();
        randomized();
        compositeDagsAndLongPlans();
        amalgamatedPrecompute();
        requiredNgramPrecompute();
        mandatoryLiteralAffixes();
        refusals();
        System.out.println("M3_STRING_REGEX_DP_PASS|checks=" + checks);
    }

    private static void deterministic() throws Exception {
        verify(String.join("", "aa", "b"), "c*a*b", 1);
        verify(String.join("", "aa", "ab"), "a*a*b", 1);
        verify(String.join("", "aa", "abb"), "a*a*b", 0);
        verify(String.join("", "a", "bbb"), "ab*", 1);
        verify(String.join("", "ab", "c"), ".*", 1);
        verify(String.join("", "xx", "bbb"), ".*b*", 1);
        verify(String.join("", "ab", "c"), "a*b*c*", 1);
        verify(String.join("", "aa", "b"), "a*b*", 1);
        verify(String.join("", "aa", "b"), "a*b*a", 0);
        verify(String.join("", "A0", "000"), "A*0*", 1);
        verify(String.join("", "a", "\u03b2"), "a.*", 1);
        verify(String.join("", "a", "\n", "b"), "a.*b", 0);
        verify(String.join("", "a", "\r\n", "b"), "a.*b", 0);
        verify(String.join("", "a", "\u0085", "b"), "a.*b", 0);
        verify(String.join("", "a", "\u2028", "b"), "a.*b", 0);
        verify(String.join("", "a", "\u2029", "b"), "a.*b", 0);
        verify(String.join("", "a", "bc"), "a.*d", 0);
        // A source with no canonical owner still uses the unchanged JDK path.
        equal("".matches("a*b*"), Pattern.matches("a*b*", new StringBuilder()),
                "empty source preserves regex matching");
    }

    private static void randomized() throws Exception {
        Random random = new Random(0x4d335244503231L);
        String[] atoms = {"a", "b", "c", ".", "a*", "b*", "c*", ".*"};
        String[] pieces = {"a", "b", "c", "aa", "bb", "ab", "cc"};
        for (int trial = 0; trial < 4_096; trial++) {
            int atomCount = 1 + random.nextInt(8);
            StringBuilder regex = new StringBuilder();
            for (int j = 0; j < atomCount; j++) {
                regex.append(atoms[random.nextInt(atoms.length)]);
            }
            int partCount = 1 + random.nextInt(7);
            String[] parts = new String[partCount];
            for (int j = 0; j < partCount; j++) {
                parts[j] = pieces[random.nextInt(pieces.length)];
            }
            String composite = String.join("", parts);
            String expression = regex.toString();
            int decision = decision(composite, expression);
            boolean expected = Pattern.compile(expression)
                    .matcher(new StringBuilder(composite)).matches();
            if (decision >= 0) {
                equal(decision == 1, expected, "DP/JDK parity trial=" + trial);
            }
            equal(composite.matches(expression), expected, "String/JDK parity trial=" + trial);
            // Independent top-down memoization is a LeetCode 10 oracle only;
            // inputs/patterns in this randomized lane are strictly ASCII a-c.
            equal(memoized(composite, expression), expected, "memo/JDK parity trial=" + trial);
        }
    }

    /**
     * The >32-atom rolling DP, not only the <=32-atom prepared bitset path.
     * Recover the shared-cursor donor tests without discarding newer regex logic.
     */
    private static void compositeDagsAndLongPlans() throws Exception {
        String shared = String.join("", "a", "b", "c");
        for (int depth = 0; depth < 8; depth++) {
            shared = shared.concat(shared);
        }
        String middle = shared.substring(1, shared.length() - 1);
        String accepting = "a*".repeat(36) + ".*";
        String rejecting = "a*".repeat(36) + "z";
        verify(shared, accepting, 1);
        verify(middle, accepting, 1);
        verify(shared, rejecting, 0);
        verify(middle, rejecting, 0);
        verify(shared, "a*".repeat(36) + "b*", 0);

        Random random = new Random(0x4d33524547455837L);
        String[] tokens = {"a*", "b*", "c*", ".*", "a", "b", "c"};
        String[] leaves = {"a", "b", "c", "ab", "bc", "aa", "",
                "\n", "\r", "\u0085", "\u2028", "\ud83d", "\ude42"};
        for (int trial = 0; trial < 1_024; trial++) {
            int atomCount = 33 + random.nextInt(30);
            StringBuilder pattern = new StringBuilder("a*");
            for (int i = 0; i < atomCount; i++) {
                pattern.append(tokens[random.nextInt(tokens.length)]);
            }
            String[] parts = new String[1 + random.nextInt(10)];
            for (int i = 0; i < parts.length; i++) {
                parts[i] = leaves[random.nextInt(leaves.length)];
            }
            String source = String.join("", parts);
            String regex = pattern.toString();
            int result = decision(source, regex);
            boolean oracle = Pattern.compile(regex)
                    .matcher(new StringBuilder(source)).matches();
            if (result >= 0) {
                equal(result == 1, oracle, "rolling DP/JDK parity " + trial);
            }
            equal(source.matches(regex), oracle,
                    "String/Pattern composite parity " + trial);
        }
        String splitPair = String.join("", "a", "\ud83d", "\ude42", "b");
        equal(decision(splitPair, accepting), -1,
                "rolling surrogate pair across tuple seam delegates");
        equal(splitPair.matches(accepting),
                Pattern.compile(accepting).matcher(new StringBuilder(splitPair)).matches(),
                "rolling surrogate JDK fallback");
    }

    /**
     * More than ten thousand existing and new differential cases. Precomputed
     * facts are optional: they can only prove impossibility, never a match.
     */
    private static void amalgamatedPrecompute() throws Exception {
        Class<?> storageType = Class.forName("java.lang.M3String");
        Method prepareFacts = storageType.getDeclaredMethod("facts");
        prepareFacts.setAccessible(true);

        String missing = String.join("", "a".repeat(192), "b".repeat(192));
        prepareFacts.invoke(STRING_M3.get(missing));
        verify(missing, "a*Q.*", 0);
        verify(missing, "a*b*Q", 0);
        verify(missing, "Qb*", 0);
        verify(missing, "a*Q", 0);
        verifyMissingRequiredQ(missing, "Q.*");
        verifyMissingRequiredQ(missing, "b*Q");
        verify(missing, "a*b*", 1);
        verify(missing, "a.*b", 1);
        verify(String.join("", "ab", "c"), "abc.*", 1);
        verify(String.join("", "x", "y"), "a*b*c*Q", 0);

        String splitPair = String.join("", "a", "\ud83d", "\ude42", "b");
        equal(decision(splitPair, "a.*b"), -1,
                "surrogate-containing possible match still delegates to Pattern");
        // Missing-unit rejection is available only after optional facts are published.
        prepareFacts.invoke(STRING_M3.get(splitPair));
        verify(splitPair, "a*Q.*", 0);

        Random random = new Random(0x4d3352414d414c47L);
        String[] tokens = {"a", "b", "c", "A", "Z", "Q", ".", "a*", "b*", "c*", ".*"};
        String[] units = {"a", "b", "A", "Z", "c", "ab", "bc", "",
                "\n", "\r", "\u2028", "\ud83d", "\ude42"};
        for (int trial = 0; trial < 8_192; trial++) {
            StringBuilder regex = new StringBuilder("a*");
            int atoms = 1 + random.nextInt(48);
            for (int i = 0; i < atoms; i++) {
                regex.append(tokens[random.nextInt(tokens.length)]);
            }
            int parts = 1 + random.nextInt(8);
            String[] leaves = new String[parts];
            for (int i = 0; i < parts; i++) {
                leaves[i] = units[random.nextInt(units.length)];
            }
            String composite = String.join("", leaves);
            String expression = regex.toString();
            boolean oracle = Pattern.compile(expression)
                    .matcher(new StringBuilder(composite)).matches();
            // Empty Strings have no canonical M3 owner in this JDK stage.
            // Test the authoritative public fallback instead of direct DP.
            if (composite.isEmpty()) {
                equal(composite.matches(expression), oracle,
                        "empty String/Pattern trial=" + trial);
                continue;
            }
            Object canonical = canonicalOwner(composite);
            if ((trial & 1) == 0) prepareFacts.invoke(canonical);
            int value = (Integer) TRY_MATCH.invoke(null, canonical, expression);
            if (value >= 0) {
                equal(value == 1, oracle, "amalgamated DP/Pattern trial=" + trial);
            }
            equal(composite.matches(expression), oracle,
                    "amalgamated String/Pattern trial=" + trial);
        }
    }


    /**
     * Source-fact pair/triple rejection must be sound even when required
     * individual letters (and, for triples, required pairs) are present.
     */
    private static void requiredNgramPrecompute() throws Exception {
        Class<?> dp = Class.forName("java.lang.M3StringRegexDp");
        Class<?> storage = Class.forName("java.lang.M3String");
        Class<?> planType = Class.forName("java.lang.M3StringRegexDp$Plan");
        Method facts = storage.getDeclaredMethod("facts");
        facts.setAccessible(true);
        Method tables = dp.getDeclaredMethod("tables");
        tables.setAccessible(true);
        Object table = tables.invoke(null);
        Method compile = dp.getDeclaredMethod("prepare", String.class, table.getClass());
        compile.setAccessible(true);
        Method preflight = dp.getDeclaredMethod("rejectsByPrecompute", storage, planType);
        preflight.setAccessible(true);

        String[] inputs = {
                String.join("", "bbb", "cccc", "aaaa"),
                String.join("", "abX", "Xbc", "YYac"),
                String.join("", "ab", "cXX"),
                String.join("", "a", "bcXX"),
                String.join("", "xyz", "abc", "abc")
        };
        String[] expressions = {
                "a*ab.*", "a*abc.*", "a*abc.*", "a*abc.*", ".*abc.*"
        };
        boolean[] rejected = {true, true, false, false, false};
        for (int i = 0; i < inputs.length; i++) {
            Object owner = STRING_M3.get(inputs[i]);
            facts.invoke(owner);
            Object plan = compile.invoke(null, expressions[i], table);
            equal((Boolean) preflight.invoke(null, owner, plan), rejected[i],
                    "required ngram preflight case=" + i);
            int result = decision(inputs[i], expressions[i]);
            boolean expected = Pattern.matches(expressions[i],
                    new StringBuilder(inputs[i]));
            if (result >= 0) equal(result == 1, expected, "ngram DP oracle=" + i);
            equal(inputs[i].matches(expressions[i]), expected,
                    "ngram public String oracle=" + i);
        }

        // Construct admitted patterns together with a guaranteed exact witness.
        // A necessary-condition bit collision can never create a false negative.
        Random random = new Random(0x4d334e4752414d53L);
        String alphabet = "aAbBcC01.";
        for (int trial = 0; trial < 2_048; trial++) {
            StringBuilder regex = new StringBuilder("a*");
            StringBuilder witness = new StringBuilder();
            int atoms = 1 + random.nextInt(30);
            for (int i = 0; i < atoms; i++) {
                char unit = alphabet.charAt(random.nextInt(alphabet.length()));
                boolean repeat = random.nextInt(3) == 0;
                regex.append(unit);
                if (repeat) regex.append('*');
                int copies = repeat ? random.nextInt(4) : 1;
                for (int j = 0; j < copies; j++) {
                    witness.append(unit == '.' ? 'x' : unit);
                }
            }
            if (witness.length() == 0) continue;
            String expression = regex.toString();
            String source = String.join("", witness.substring(0, witness.length() / 2),
                    witness.substring(witness.length() / 2));
            Object owner = STRING_M3.get(source);
            facts.invoke(owner);
            Object plan = compile.invoke(null, expression, table);
            equal((Boolean) preflight.invoke(null, owner, plan), false,
                    "matching witness cannot be rejected=" + trial);
            equal(Pattern.matches(expression, new StringBuilder(source)), true,
                    "constructed regex witness=" + trial);
            equal(source.matches(expression), true, "M3 String witness=" + trial);
        }
    }


    /**
     * Complete mandatory literal boundaries can reject a candidate whose
     * required letters, pairs, triples and first/last units are all present.
     * The guard is exact across canonical tuple seams and owner/range slices.
     */
    private static void mandatoryLiteralAffixes() throws Exception {
        AffixOracle oracle = new AffixOracle();
        String[] texts = {
                "aXcabcZZxyz", "abcZZxyzxYz", "abcxyz", "abcMIDxyz",
                "aXcZZxyz", "abcZZxQz", "abcZZxz", "bcZZxyz", "abcxy",
                "abc\nxyz", "abc\ud83d\ude42xyz", "abc\ud83dxyz",
                "aXcabc\ud83d\ude42xyz", "abc\ud83d\ude42xyzxYz"
        };
        String[] expressions = {
                "abc.*xyz", "abc.*xyz", "abc.*xyz", "abc.*xyz",
                "a.c.*xyz", "abc.*x.z", "abc.*xy*z", "a*bc.*xyz", "abc.*xyz*",
                "abc.*xyz", "abc.*xyz", "abc.*xyz",
                "abc.*xyz", "abc.*xyz"
        };
        boolean[] rejected = {
                true, true, false, false, false, false, false, false, false,
                false, false, false, true, true
        };
        for (int i = 0; i < texts.length; i++) {
            for (int shape = 0; shape < 3; shape++) {
                for (int prepared = 0; prepared < 2; prepared++) {
                    oracle.verify(affixComposite(texts[i], shape), expressions[i],
                            rejected[i], prepared != 0, "affix case=" + i
                                    + " shape=" + shape + " facts=" + prepared);
                }
            }
        }

        String shared = String.join("", "ab", "c", "xy", "z");
        for (int depth = 0; depth < 8; depth++) shared = shared.concat(shared);
        oracle.verify(shared, "abc.*xyz", false, false, "shared affix DAG");
        oracle.verify(shared.substring(6, shared.length() - 6), "abc.*xyz",
                false, true, "shared affix DAG range");

        // Equal-length ranges retain one owner but have different exact
        // coordinates. A cached full-range truth must never leak between them.
        String good = "abc" + "M".repeat(96) + "xyz";
        String bad = "aXc" + "M".repeat(93) + "abcxyz";
        String ranges = String.join("", good, bad, good);
        for (int index : new int[] {0, 0, 1, 1, 2, 0, 1, 2}) {
            verify(ranges.substring(index * good.length(), (index + 1) * good.length()),
                    "abc.*xyz", index == 1 ? 0 : 1);
        }

        // A definite ASCII boundary mismatch is decisive even when the middle
        // would otherwise require the unchanged JDK surrogate fallback.
        equal(decision(affixComposite("abc\ud83d\ude42xyz", 0), "abc.*xyz"), -1,
                "possible pair-containing affix match still delegates");
        equal(decision(affixComposite("abc\ud83dxyz", 1), "abc.*xyz"), -1,
                "possible lone-surrogate affix match still delegates");
        verify(affixComposite("aXcabc\ud83d\ude42xyz", 2), "abc.*xyz", 0);
        verify(affixComposite("abc\ud83d\ude42xyzxYz", 2), "abc.*xyz", 0);

        // Witnesses alternate the <=32-atom relation path and >32-atom rolling
        // path. Every negative contains the full required prefix/suffix in the
        // middle, so unordered source-fact signals cannot prove its rejection.
        Random random = new Random(0x4d33414646495831L);
        for (int trial = 0; trial < 1_024; trial++) {
            String prefix = randomLiteral(random, 2 + random.nextInt(8));
            String suffix = randomLiteral(random, 2 + random.nextInt(8));
            String middle = randomLiteral(random, random.nextInt(12));
            String expression = prefix + ((trial & 1) == 0
                    ? ".*" : "a*".repeat(33) + ".*") + suffix;
            String prefixMismatch = prefix.substring(0, 1)
                    + (prefix.charAt(1) == 'Q' ? 'R' : 'Q') + prefix.substring(2)
                    + prefix + middle + suffix;
            int penultimate = suffix.length() - 2;
            String suffixMismatch = prefix + middle + suffix
                    + suffix.substring(0, penultimate)
                    + (suffix.charAt(penultimate) == 'Q' ? 'R' : 'Q')
                    + suffix.substring(penultimate + 1);
            boolean prepared = (trial & 2) == 0;
            oracle.verify(affixComposite(prefix + middle + suffix, trial % 3),
                    expression, false, prepared, "affix witness=" + trial);
            oracle.verify(affixComposite(prefixMismatch, trial % 3),
                    expression, true, prepared, "prefix mismatch=" + trial);
            oracle.verify(affixComposite(suffixMismatch, trial % 3),
                    expression, true, prepared, "suffix mismatch=" + trial);
        }
    }

    private static String affixComposite(String text, int shape) {
        int first = Math.min(2, text.length());
        int second = Math.max(first, text.length() - 2);
        String composite = String.join("", text.substring(0, first),
                text.substring(first, second), text.substring(second));
        if (shape == 0) return composite;
        String padded = String.join("", "PAD", composite, "END");
        if (shape == 1) return padded.substring(3, 3 + text.length());
        String shared = padded.concat(padded);
        return shared.substring(padded.length() + 3,
                padded.length() + 3 + text.length());
    }

    private static String randomLiteral(Random random, int length) {
        String alphabet = "abcABC012";
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            result.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return result.toString();
    }

    private static final class AffixOracle {
        private final Method facts;
        private final Method factsIfPrepared;
        private final Method compile;
        private final Method preflight;
        private final Object table;

        AffixOracle() throws Exception {
            Class<?> storage = Class.forName("java.lang.M3String");
            Class<?> dp = Class.forName("java.lang.M3StringRegexDp");
            Class<?> plan = Class.forName("java.lang.M3StringRegexDp$Plan");
            facts = storage.getDeclaredMethod("facts");
            facts.setAccessible(true);
            factsIfPrepared = storage.getDeclaredMethod("factsIfPrepared");
            factsIfPrepared.setAccessible(true);
            Method tables = dp.getDeclaredMethod("tables");
            tables.setAccessible(true);
            table = tables.invoke(null);
            compile = dp.getDeclaredMethod("prepare", String.class, table.getClass());
            compile.setAccessible(true);
            preflight = dp.getDeclaredMethod("rejectsByPrecompute", storage, plan);
            preflight.setAccessible(true);
        }

        void verify(String source, String expression, boolean rejected,
                boolean prepareFacts, String label) throws Exception {
            Object owner = STRING_M3.get(source);
            if (prepareFacts) facts.invoke(owner);
            Object known = factsIfPrepared.invoke(owner);
            Object plan = compile.invoke(null, expression, table);
            equal((Boolean) preflight.invoke(null, owner, plan), rejected,
                    label + " exact necessary condition");
            equal(factsIfPrepared.invoke(owner) == known, true,
                    label + " must not prepare whole-source facts");
            boolean expected = Pattern.matches(expression, new StringBuilder(source));
            if (rejected) equal(expected, false, label + " rejection oracle");
            int result = decision(source, expression);
            if (result >= 0) equal(result == 1, expected, label + " DP oracle");
            if (rejected) equal(result, 0, label + " preflight decision");
            equal(source.matches(expression), expected, label + " String oracle");
        }
    }

    private static void refusals() throws Exception {
        String text = String.join("", "a", "bc");
        for (String expression : new String[] {
                "a**", "*a", "(a*)b", "a+b", "a?b", "a{2,3}",
                "a|b", "^ab", "[a-z]+", "\\Qa\\E", "(?i)ab", ""
        }) {
            equal(decision(text, expression), -1, "unsupported syntax must delegate: " + expression);
        }
        equal(decision(text, "abc"), -1, "existing literal equality owner retained");
        String supplementary = String.join("", "a", "\ud83d", "\ude42", "b");
        equal(decision(supplementary, "a.*b"), -1, "surrogate pair across leaves delegated");
        equal(supplementary.matches("a.*b"),
                Pattern.matches("a.*b", new StringBuilder(supplementary)),
                "surrogate fallback parity");
        String unpaired = String.join("", "a", "\ud83d", "b");
        equal(decision(unpaired, "a.*b"), -1, "unpaired surrogate delegated");
        String large = String.join("", "a".repeat(8_193), "b");
        equal(decision(large, "a*b*"), -1, "input-work budget delegated");
        String capped = String.join("", "a".repeat(7_900), "b");
        equal(decision(capped, ("a*").repeat(70)), -1, "pattern budget delegated");
        expectSyntaxError(() -> text.matches("a**"));
        expectSyntaxError(() -> text.matches("*a"));
        equal(text.matches("a+c"),
                Pattern.matches("a+c", new StringBuilder(text)), "full JDK syntax remains authoritative");
    }

    private static Object canonicalOwner(String text) throws Exception {
        Object owner = STRING_M3.get(text);
        if (owner == null) {
            // Singleton String.join intentionally preserves a flat input's backing.
            // Keep that original String and its public Pattern-fallback checks, while
            // explicitly admitting the same spelling for the direct DP obligation.
            owner = CANONICALIZE.invoke(null, text);
            if (owner == null) throw new AssertionError("canonical fixture admission refused");
            CharSequence value = (CharSequence) owner;
            equal(value.length(), text.length(), "canonical fixture UTF16 length");
            for (int i = 0; i < text.length(); i++) {
                equal(value.charAt(i), text.charAt(i), "canonical fixture UTF16 unit");
            }
        }
        return owner;
    }

    private static int decision(String text, String regex) throws Exception {
        return (Integer) TRY_MATCH.invoke(null, canonicalOwner(text), regex);
    }

    private static void verify(String source, String regex, int expectedDecision) throws Exception {
        equal(decision(source, regex), expectedDecision, "direct DP decision " + regex);
        equal(source.matches(regex), expectedDecision == 1, "M3 String.matches decision " + regex);
        equal(source.matches(regex), Pattern.compile(regex)
                .matcher(new StringBuilder(source)).matches(), "JDK oracle " + regex);
    }

    private static void verifyMissingRequiredQ(String source, String suffix) throws Exception {
        if (!suffix.equals("Q.*") && !suffix.equals("b*Q")) {
            throw new AssertionError("unexpected mandatory-Q witness");
        }
        String regex = "a*".repeat(36) + suffix;
        // Keep the original 384-unit witness and >32-state target pattern. A missing
        // mandatory Q proves false; independent top-down memoization bounds the oracle.
        // Adjacent identical a* languages are idempotent, so collapsed Pattern syntax
        // supplies a second oracle without enumerating all 36-star text partitions.
        equal(source.indexOf('Q'), -1, "required Q absent from original witness");
        equal(memoized(source, regex), false, "bounded independent missing-Q oracle");
        equal(Pattern.compile("a*" + suffix).matcher(new StringBuilder(source)).matches(),
                false, "equivalent bounded Pattern missing-Q oracle");
        equal(decision(source, regex), 0, "direct rolling DP missing-Q decision");
        equal(source.matches(regex), false, "M3 String.matches missing-Q decision");
    }

    private static boolean memoized(CharSequence text, String regex) {
        byte[][] memo = new byte[text.length() + 1][regex.length() + 1];
        return memoFrom(text, regex, 0, 0, memo);
    }

    private static boolean memoFrom(CharSequence text, String regex,
            int i, int j, byte[][] memo) {
        if (memo[i][j] != 0) return memo[i][j] == 2;
        boolean result;
        if (j == regex.length()) {
            result = i == text.length();
        } else {
            boolean consumes = i < text.length()
                    && (regex.charAt(j) == '.' || regex.charAt(j) == text.charAt(i));
            if (j + 1 < regex.length() && regex.charAt(j + 1) == '*') {
                result = memoFrom(text, regex, i, j + 2, memo)
                        || (consumes && memoFrom(text, regex, i + 1, j, memo));
            } else {
                result = consumes && memoFrom(text, regex, i + 1, j + 1, memo);
            }
        }
        memo[i][j] = (byte) (result ? 2 : 1);
        return result;
    }

    private static void expectSyntaxError(Runnable action) {
        try {
            action.run();
            throw new AssertionError("expected PatternSyntaxException");
        } catch (PatternSyntaxException expected) {
            checks++;
        }
    }

    private static void equal(boolean actual, boolean expected, String label) {
        checks++;
        if (actual != expected) throw new AssertionError(label + " actual=" + actual
                + " expected=" + expected);
    }

    private static void equal(int actual, int expected, String label) {
        checks++;
        if (actual != expected) throw new AssertionError(label + " actual=" + actual
                + " expected=" + expected);
    }
}
