/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Frozen M3 String prefix/suffix rank geometry and reuse in regex row precompute
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringAffixPrecomputeTest
 */
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.regex.Pattern;

public final class M3StringAffixPrecomputeTest {
    private static long checks;

    private static void check(boolean actual, String label) {
        checks++;
        if (!actual) throw new AssertionError("M3 affix invariant=" + label + " check=" + checks);
    }

    private static int[] brute(String[] values, String query, boolean suffix) {
        int[] rows = new int[values.length];
        int count = 0;
        for (int row = 0; row < values.length; row++) {
            if (suffix ? values[row].endsWith(query) : values[row].startsWith(query)) {
                rows[count++] = row;
            }
        }
        return Arrays.copyOf(rows, count);
    }

    public static void main(String... args) throws Exception {
        Class<?> m3 = Class.forName("java.lang.M3String");
        Class<?> affixType = Class.forName("java.lang.M3StringAffixPrecompute");
        Class<?> rowType = Class.forName("java.lang.M3StringRegexRowPrecompute");
        Class<?> arrayType = Array.newInstance(m3, 0).getClass();
        Field storage = String.class.getDeclaredField("m3");
        storage.setAccessible(true);
        Method freeze = affixType.getDeclaredMethod("tryPrepare", arrayType);
        freeze.setAccessible(true);
        Method prefix = affixType.getDeclaredMethod("prefixRange", CharSequence.class);
        Method suffix = affixType.getDeclaredMethod("suffixRange", CharSequence.class);
        prefix.setAccessible(true);
        suffix.setAccessible(true);
        Class<?> range = Class.forName("java.lang.M3StringAffixPrecompute$Range");
        Method sourceRows = affixType.getDeclaredMethod("rowsInSourceOrder", range, boolean.class);
        sourceRows.setAccessible(true);
        Method prefixRank = affixType.getDeclaredMethod("prefixRank", int.class);
        Method suffixRank = affixType.getDeclaredMethod("suffixRank", int.class);
        Method prefixBegin = affixType.getDeclaredMethod("prefixBegin", int.class);
        Method suffixBegin = affixType.getDeclaredMethod("suffixBegin", int.class);
        Method prefixEnd = affixType.getDeclaredMethod("prefixEnd", int.class);
        Method suffixEnd = affixType.getDeclaredMethod("suffixEnd", int.class);
        Method prefixParent = affixType.getDeclaredMethod("prefixParent", int.class);
        Method suffixParent = affixType.getDeclaredMethod("suffixParent", int.class);
        for (Method method : new Method[] {prefixRank, suffixRank, prefixBegin, suffixBegin,
                prefixEnd, suffixEnd, prefixParent, suffixParent}) method.setAccessible(true);

        Random random = new Random(20261017L);
        String[] sample = {"a", "ab", "abc", "a", "ab", "aaa", "b", "ba", "🙂", "a🙂",
                "🙂a", "\u0000", "a\u0000", "\uffff", "ab\uffff", "hello", "x", "xx", "x\ud83d", "x\ude00"};
        String alphabet = "abcXY01_\u0000\ud83d\ude00\uffff";
        String[] values = Arrays.copyOf(sample, 256);
        for (int i = sample.length; i < values.length; i++) {
            int width = 1 + random.nextInt(9);
            StringBuilder builder = new StringBuilder();
            for (int j = 0; j < width; j++) builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
            values[i] = builder.toString();
        }
        Object owners = Array.newInstance(m3, values.length);
        for (int row = 0; row < values.length; row++) {
            String value = new String(values[row].toCharArray());
            Object canonical = storage.get(value);
            check(canonical != null, "canonical row=" + row);
            Array.set(owners, row, canonical);
        }
        Object prepared = freeze.invoke(null, owners);
        check(prepared != null, "index admitted");
        for (int i = 0; i < values.length; i++) {
            int pr = (int) prefixRank.invoke(prepared, i);
            int sr = (int) suffixRank.invoke(prepared, i);
            int pb = (int) prefixBegin.invoke(prepared, i);
            int sb = (int) suffixBegin.invoke(prepared, i);
            int pe = (int) prefixEnd.invoke(prepared, i);
            int se = (int) suffixEnd.invoke(prepared, i);
            int pp = (int) prefixParent.invoke(prepared, i);
            int sp = (int) suffixParent.invoke(prepared, i);
            check(pb <= pr && pr < pe && sb <= sr && sr < se, "self intervals=" + i);
            check(pp < 0 || (values[i].startsWith(values[pp])
                    && values[pp].length() < values[i].length()), "prefix parent=" + i);
            check(sp < 0 || (values[i].endsWith(values[sp])
                    && values[sp].length() < values[i].length()), "suffix parent=" + i);
            for (int j = 0; j < values.length; j++) {
                int pj = (int) prefixRank.invoke(prepared, j);
                int sj = (int) suffixRank.invoke(prepared, j);
                check(values[j].startsWith(values[i]) == (pj >= pb && pj < pe),
                        "prefix subtree=" + i + "/" + j);
                check(values[j].endsWith(values[i]) == (sj >= sb && sj < se),
                        "suffix subtree=" + i + "/" + j);
            }
        }
        for (int trial = 0; trial < 1_024; trial++) {
            String word = values[random.nextInt(values.length)];
            int width = random.nextInt(word.length() + 1);
            String query = trial % 2 == 0 ? word.substring(0, width)
                    : word.substring(word.length() - width);
            Object p = prefix.invoke(prepared, query);
            Object s = suffix.invoke(prepared, query);
            check(Arrays.equals((int[]) sourceRows.invoke(prepared, p, false), brute(values, query, false)),
                    "prefix lookup query=" + trial);
            check(Arrays.equals((int[]) sourceRows.invoke(prepared, s, true), brute(values, query, true)),
                    "suffix lookup query=" + trial);
        }

        Method compile = rowType.getDeclaredMethod("compile", arrayType);
        compile.setAccessible(true);
        Method prepare = rowType.getDeclaredMethod("prepare", String.class, int.class);
        prepare.setAccessible(true);
        Field indexed = rowType.getDeclaredField("affixes");
        indexed.setAccessible(true);
        Object rowIndex = compile.invoke(null, owners);
        for (int i = 0; i < 4; i++) prepare.invoke(rowIndex, "ab.*", 0);
        check(indexed.get(rowIndex) != null, "reused regex prepares affix index");
        String[] expressions = {"ab.*", ".*ab", "ab", ".*abc", "abc.*", ".*x.*", "a|b", "(?i)ab.*"};
        for (String regex : expressions) {
            Object plan = prepare.invoke(rowIndex, regex, 0);
            Method candidates = plan.getClass().getDeclaredMethod("candidateRows");
            Method matching = plan.getClass().getDeclaredMethod("matchingRows");
            candidates.setAccessible(true);
            matching.setAccessible(true);
            int[] gotCandidates = (int[]) candidates.invoke(plan);
            int[] gotMatches = (int[]) matching.invoke(plan);
            boolean[] admitted = new boolean[values.length];
            int previous = -1;
            for (int row : gotCandidates) {
                check(row > previous && row < values.length, "candidate order " + regex);
                admitted[row] = true;
                previous = row;
            }
            int matched = 0;
            Pattern oracle = Pattern.compile(regex);
            for (int row = 0; row < values.length; row++) {
                if (oracle.matcher(values[row]).matches()) {
                    check(admitted[row], "regex candidate never omits true row " + regex);
                    matched++;
                }
            }
            check(gotMatches.length == matched, "regex exact match cardinality " + regex);
            for (int row : gotMatches) {
                check(oracle.matcher(values[row]).matches(), "regex authoritative match " + regex);
            }
        }
        System.out.println("M3_STRING_AFFIX_PRECOMPUTE_PASS checks=" + checks);
    }
}
