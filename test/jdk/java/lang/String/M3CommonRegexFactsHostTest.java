/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Exact prepared common-regex class facts on actual M3 native atom, tuple and range owners
 * @modules java.base/java.lang java.base/jdk.internal.misc
 * @compile --patch-module java.base=${test.src} M3CommonRegexFactsHostTest.java
 * @run main/othervm --patch-module java.base=${test.classes} java.lang.M3CommonRegexFactsHostTest candidate
 * @run main/othervm --patch-module java.base=${test.classes} -XX:-CompactStrings java.lang.M3CommonRegexFactsHostTest candidate
 */
package java.lang;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jdk.internal.misc.Unsafe;

/**
 * Host qualification using complete actual M3 classes and native-backed atoms.
 * Stock Pattern on StringBuilder is the independent semantic oracle. This test
 * does not exercise product String.matches dispatch or a matched HotSpot image.
 */
public final class M3CommonRegexFactsHostTest {
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int ALL_CLASSES = (1 << 14) - 1;
    private static final String[] CLASS_EXPRESSIONS = {
        "\\d", "\\w", "\\s", "\\p{Lower}", "\\p{Upper}", "\\p{Alpha}",
        "\\p{Alnum}", "\\p{XDigit}", "\\p{ASCII}", "[0-9a-f]", "[0-9A-F]",
        ".", "\\h", "\\v"
    };
    private static final String[] CLASS_FIELDS = {
        "REGEX_DIGIT", "REGEX_WORD", "REGEX_SPACE", "REGEX_LOWER", "REGEX_UPPER",
        "REGEX_ALPHA", "REGEX_ALNUM", "REGEX_HEX", "REGEX_ASCII", "REGEX_LOWER_HEX",
        "REGEX_UPPER_HEX", "REGEX_DOT", "REGEX_HORIZONTAL", "REGEX_VERTICAL"
    };
    private static final String[] BRACKETS = {
        "0-9", "a-z", "A-Z", "a-zA-Z", "A-Za-z", "a-zA-Z0-9", "A-Za-z0-9",
        "a-zA-Z_0-9", "a-zA-Z0-9_", "A-Za-z0-9_", "0-9a-fA-F", "0-9A-Fa-f",
        "A-Fa-f0-9", "a-fA-F0-9", "0-9a-f", "0-9A-F"
    };
    private static final String[] QUANTIFIERS = {
        "", "?", "*", "+", "{0}", "{1}", "{2}", "{3}", "{0,}", "{1,}",
        "{2,}", "{0,1}", "{1,3}", "{2,3}", "{3,3}", "{0001}",
        "{2147483647}", "{0,2147483647}"
    };
    private static long checks;
    private static long maskInputs;
    private static long regexChecks;
    private static long fieldChecks;
    private static long nextId;
    private static boolean factsOnly;
    private static volatile Object lastScannedFacts;
    private static volatile long workChecksum;
    private static Field allClasses;
    private static Field anyClasses;
    private static List<Field> primitiveFields;
    private static final Pattern[] MEMBERSHIP = new Pattern[CLASS_EXPRESSIONS.length];
    private static int[] independentMasks;

    private M3CommonRegexFactsHostTest() {}

    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "candidate" : args[0];
        if (mode.equals("resource")) {
            resourceObservation();
            return;
        }
        if (mode.equals("cold-single")) {
            coldSingleCharacter();
            return;
        }
        if (mode.equals("baseline") || mode.equals("regression")) {
            try (NativeText text = new NativeText("123")) {
                check(text.value.factsIfPrepared() == null, "fresh real owner starts cold");
                text.value.facts();
                int actual = M3StringRegexDp.tryMatch(text.value, "\\d+");
                System.out.println("M3_COMMON_REGEX_BASELINE actual=" + actual
                        + " expectedPrepared=1 owner=" + text.value.owner().getClass().getName());
                check(actual == (mode.equals("baseline") ? -1 : 1),
                        "prepared \\d+ must decide true; actual=" + actual);
            }
            return;
        }
        factsOnly = mode.equals("facts-only");
        if (!factsOnly) coldSingleCharacter();
        bind();
        exhaustiveMembership();
        if (!factsOnly) coldAndRefusal();
        deterministic();
        compositionAndRanges();
        longPrepared();
        if (factsOnly) {
            try (NativeText text = new NativeText("123")) {
                text.value.facts();
                check(M3StringRegexDp.tryMatch(text.value, "\\d+") == -1,
                        "facts-only stage retains original regex decision");
            }
        }
        System.out.println("M3_COMMON_REGEX_ACTUAL_OWNER_PASS phase=" + mode + " checks=" + checks
                + " maskInputs=" + maskInputs + " regexChecks=" + regexChecks
                + " primitiveFieldChecks=" + fieldChecks + " fields=" + primitiveFields.size()
                + " sourceRemapping=false ownerStubs=false productImage=false");
    }

    private static void bind() throws Exception {
        allClasses = M3StringFacts.class.getDeclaredField("regexAllClasses");
        anyClasses = M3StringFacts.class.getDeclaredField("regexAnyClasses");
        allClasses.setAccessible(true);
        anyClasses.setAccessible(true);
        for (int index = 0; index < CLASS_FIELDS.length; index++) {
            Field constant = M3StringFacts.class.getDeclaredField(CLASS_FIELDS[index]);
            constant.setAccessible(true);
            check(constant.getInt(null) == 1 << index, "class mask bit " + CLASS_FIELDS[index]);
            MEMBERSHIP[index] = Pattern.compile(CLASS_EXPRESSIONS[index]);
        }
        primitiveFields = new ArrayList<>();
        for (Field field : M3StringFacts.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                check(field.getType().isPrimitive(), "facts retain primitive fields only");
                field.setAccessible(true);
                primitiveFields.add(field);
            }
        }
    }

    /** Every BMP code unit, including isolated surrogate halves, is an independent oracle input. */
    private static void exhaustiveMembership() throws Exception {
        independentMasks = new int[Character.MAX_VALUE + 1];
        StringBuilder unit = new StringBuilder(" ");
        Matcher[] matchers = new Matcher[MEMBERSHIP.length];
        for (int bit = 0; bit < matchers.length; bit++) matchers[bit] = MEMBERSHIP[bit].matcher(unit);
        for (int code = Character.MIN_VALUE; code <= Character.MAX_VALUE; code++) {
            unit.setCharAt(0, (char) code);
            int expected = 0;
            for (int bit = 0; bit < matchers.length; bit++) {
                if (matchers[bit].reset(unit).matches()) expected |= 1 << bit;
            }
            independentMasks[code] = expected;
            try (NativeText text = new NativeText(unit.toString())) {
                M3StringFacts facts = text.value.facts();
                check(allClasses.getInt(facts) == expected, "all mask unit=" + code);
                check(anyClasses.getInt(facts) == expected, "any mask unit=" + code);
                maskInputs++;
                for (int bit = 0; !factsOnly && bit < CLASS_EXPRESSIONS.length; bit++) {
                    int actual = M3StringRegexDp.tryMatch(text.value, CLASS_EXPRESSIONS[bit]);
                    check(actual == ((expected & (1 << bit)) == 0 ? 0 : 1),
                            "one-class match code=" + code + " class=" + CLASS_EXPRESSIONS[bit]);
                    regexChecks++;
                }
            }
        }
    }

    private static void coldAndRefusal() {
        for (String expression : new String[] {"\\d+", "\\w*", "\\s?", "\\h{1,}",
                "\\v", "[0-9]{3}", "[^a-z]+", "\\p{ASCII}+", "."}) {
            try (NativeText text = new NativeText("123")) {
                check(text.value.factsIfPrepared() == null, "cold owner has no facts");
                check(M3StringRegexDp.tryMatch(text.value, expression) == -1,
                        "cold class expression must defer " + expression);
                check(text.value.factsIfPrepared() == null, "cold matching must not prepare facts");
            }
        }
        String[] refused = {"", "\\", "[", "[^", "\\p{", "\\p{}", "\\d{", "\\d{}",
            "\\d{,}", "\\d{,1}", "\\d{2,1}", "\\d{2147483648}", "\\d{0,2147483648}",
            "\\d{9999999999999999999999999}", "\\d{1,2,3}", "\\d{1x}", "\\d{-1}",
            "\\d+?", "\\d++", "\\d??", "\\d?+", "\\d*?", "\\d*+", "\\d{2}?",
            "\\d{2}+", "\\d{1,2}?", "\\d{1,2}+", "(\\d+)", "(?i)[a-z]+", "(?U)\\w+",
            "\\p{L}+", "\\p{javaLowerCase}+", "^\\d+$", "[0-9&&[123]]+", "[0-9a-z]+",
            "\\Q\\d\\E", "[a-z]{1}x", "\\d+\\w+", ".".repeat(129)};
        try (NativeText text = new NativeText("123")) {
            text.value.facts();
            for (String expression : refused) {
                check(M3StringRegexDp.tryMatch(text.value, expression) == -1,
                        "unsupported/invalid expression must defer " + expression);
            }
            for (String expression : new String[] {"a*", "a*b*", ".*", "a.*b", "0*1*2*3*"}) {
                int actual = M3StringRegexDp.tryMatch(text.value, expression);
                boolean expected = Pattern.compile(expression).matcher(new StringBuilder("123")).matches();
                check(actual == -1 || actual == (expected ? 1 : 0), "preserved old regex lane");
            }
        }
    }

    /** Existing one-character refusals must retain the original allocation-free early exit. */
    private static void coldSingleCharacter() throws Exception {
        Field tables = M3StringRegexDp.class.getDeclaredField("tables");
        tables.setAccessible(true);
        for (String expression : new String[] {".", "a"}) {
            // Single-threaded test state reset, not a production lifecycle operation.
            tables.set(null, null);
            try (NativeText text = new NativeText("123")) {
                check(text.value.factsIfPrepared() == null, "single-character source is cold");
                check(M3StringRegexDp.tryMatch(text.value, expression) == -1,
                        "cold single-character expression must defer");
                check(tables.get(null) == null,
                        "cold single-character expression must not initialize optional regex tables: " + expression);
                check(text.value.factsIfPrepared() == null, "single-character refusal must remain cold");
            }
        }
        System.out.println("M3_COLD_SINGLE_CHARACTER_NO_ALLOCATION_PASS expressions=2");
    }

    private static List<Pattern> expressions() {
        List<String> tokens = new ArrayList<>(List.of("\\d", "\\D", "\\w", "\\W", "\\s", "\\S",
                "\\h", "\\H", "\\v", "\\V", "."));
        for (String name : new String[] {"Lower", "Upper", "ASCII", "Alpha", "Digit", "Alnum", "XDigit", "Space"}) {
            tokens.add("\\p{" + name + "}");
            tokens.add("\\P{" + name + "}");
        }
        for (String body : BRACKETS) {
            tokens.add("[" + body + "]");
            tokens.add("[^" + body + "]");
        }
        List<Pattern> result = new ArrayList<>();
        for (String token : tokens) {
            for (String quantifier : QUANTIFIERS) result.add(Pattern.compile(token + quantifier));
        }
        return result;
    }

    private static void deterministic() throws Exception {
        List<Pattern> patterns = expressions();
        String[] spellings = {"", "0", "123", "a", "Z", "abc", "ABC", "aZ9", "_", "a_9",
            " ", "\t\n\u000b\f\r ", "\r\n", "\u0000", "\u007f", "\u0080", "\u0085",
            "\u00a0", "\u1680", "\u180e", "\u2000\u200a", "\u2028\u2029", "\u202f\u205f\u3000",
            "é", "éA", "\u0100", "\u0661", "\uff11", "\ud83d\ude42", "\ud83d", "\ude42",
            "\ud83d\ude42\ud83d", "1\ud83d\ude42", "a\n", "0aF", "0af", "0AF", "9z", "!!!"};
        for (String spelling : spellings) {
            int cut = spelling.length() / 2;
            try (NativeText flat = new NativeText(spelling);
                 NativeText left = new NativeText(spelling.substring(0, cut));
                 NativeText right = new NativeText(spelling.substring(cut));
                 NativeText carrier = new NativeText("!" + spelling + "?")) {
                M3String tuple = tuple(left.value, right.value);
                M3String range = carrier.value.slice(1, 1 + spelling.length());
                for (M3String value : new M3String[] {flat.value, tuple, range}) {
                    verifyFacts(value, spelling);
                    sameFields(flat.value.facts(), value.facts(), "deterministic composition");
                    for (Pattern pattern : patterns) match(value, spelling, pattern);
                }
            }
        }
    }

    private static void compositionAndRanges() throws Exception {
        Random random = new Random(0x4d33434c415353L);
        char[] alphabet = "09azAZ_! \t\n\r\u000b\f\u0085\u00a0\u180e\u2000\u2028\u2029\u3000é\ud83d\ude42".toCharArray();
        Pattern[] patterns = {Pattern.compile("\\d+"), Pattern.compile("\\D+"), Pattern.compile("\\w*"),
            Pattern.compile("\\W*"), Pattern.compile("\\h+"), Pattern.compile("\\v+"), Pattern.compile(".*"),
            Pattern.compile(".{0,3}"), Pattern.compile("\\P{ASCII}+"), Pattern.compile("[^0-9A-F]{1,}")};
        for (int trial = 0; trial < 1_000; trial++) {
            int length = random.nextInt(49);
            StringBuilder spelling = new StringBuilder(length);
            for (int index = 0; index < length; index++) spelling.append(alphabet[random.nextInt(alphabet.length)]);
            String text = spelling.toString();
            int first = random.nextInt(length + 1);
            int second = first + random.nextInt(length - first + 1);
            try (NativeText flat = new NativeText(text);
                 NativeText left = new NativeText(text.substring(0, first));
                 NativeText middle = new NativeText(text.substring(first, second));
                 NativeText right = new NativeText(text.substring(second))) {
                M3String tree = tuple(tuple(left.value, middle.value), right.value);
                verifyFacts(tree, text);
                sameFields(flat.value.facts(), tree.facts(), "random tree=" + trial);
                int begin = random.nextInt(length + 1);
                int end = begin + random.nextInt(length - begin + 1);
                M3String range = tree.slice(begin, end);
                String slice = text.substring(begin, end);
                try (NativeText rangeOracle = new NativeText(slice)) {
                    verifyFacts(range, slice);
                    sameFields(rangeOracle.value.facts(), range.facts(), "random range=" + trial);
                    for (Pattern pattern : patterns) match(range, slice, pattern);
                }
            }
        }
        for (int length : new int[] {1023, 1024, 1025, 2047, 2048, 2049, 4097}) {
            String text = "aZ9_\u00a0\u0085\ud83d\ude42".repeat((length + 7) / 8).substring(0, length);
            try (NativeText flat = new NativeText(text);
                 NativeText left = new NativeText(text.substring(0, Math.min(1023, length)));
                 NativeText right = new NativeText(text.substring(Math.min(1023, length)))) {
                M3String tree = tuple(left.value, right.value);
                verifyFacts(flat.value, text);
                sameFields(flat.value.facts(), tree.facts(), "scan window length=" + length);
                for (Pattern pattern : patterns) match(tree, text, pattern);
            }
        }
        String splitPair = "1".repeat(1023) + "\ud83d\ude42" + "2".repeat(1024);
        try (NativeText flat = new NativeText(splitPair);
             NativeText left = new NativeText(splitPair.substring(0, 1024));
             NativeText right = new NativeText(splitPair.substring(1024))) {
            M3String tree = tuple(left.value, right.value);
            sameFields(flat.value.facts(), tree.facts(), "surrogate pair across scan and tuple seams");
            for (Pattern pattern : patterns) match(tree, splitPair, pattern);
        }
    }

    private static void longPrepared() throws Exception {
        for (String text : new String[] {"12345678".repeat(2048), "\ud83d\ude42".repeat(9000),
                "\u00a0".repeat(17000), "\n".repeat(17000)}) {
            try (NativeText value = new NativeText(text)) {
                verifyFacts(value.value, text);
                for (String expression : new String[] {"\\d+", "\\D+", "\\w*", "\\W+", "\\s+",
                        "\\h+", "\\v+", ".*", ".{9000}", ".{18000}", "\\D{9000}",
                        "[^0-9]{9000}", "\\P{ASCII}{9000}", "\\p{ASCII}+"}) {
                    match(value.value, text, Pattern.compile(expression));
                }
            }
        }
    }

    private static void verifyFacts(M3String value, String spelling) throws Exception {
        M3StringFacts facts = value.facts();
        int all = ALL_CLASSES;
        int any = 0;
        for (int index = 0; index < spelling.length(); index++) {
            int mask = independentMasks[spelling.charAt(index)];
            all &= mask;
            any |= mask;
        }
        check(allClasses.getInt(facts) == all, "aggregate all mask");
        check(anyClasses.getInt(facts) == any, "aggregate any mask");
        check(facts.codePointCount == spelling.codePointCount(0, spelling.length()), "code point count");
        check(facts.javaHash == spelling.hashCode(), "hash retained");
        check(facts.utf16Length == spelling.length(), "length retained");
        maskInputs++;
    }

    private static void sameFields(M3StringFacts expected, M3StringFacts actual, String label) throws Exception {
        for (Field field : primitiveFields) {
            check(field.get(expected).equals(field.get(actual)), label + " field=" + field.getName());
            fieldChecks++;
        }
    }

    private static void match(M3String source, String spelling, Pattern pattern) {
        if (factsOnly) return;
        boolean expected = pattern.matcher(new StringBuilder(spelling)).matches();
        int actual = M3StringRegexDp.tryMatch(source, pattern.pattern());
        check(actual == (expected ? 1 : 0), "regex=" + pattern.pattern()
                + " units=" + spelling.length() + " expected=" + expected + " actual=" + actual);
        regexChecks++;
    }

    private static M3String tuple(M3String left, M3String right) {
        return M3String.whole(new M3StringTuple(left, right, ++nextId, nextId));
    }

    private static void check(boolean success, String message) {
        checks++;
        if (!success) throw new AssertionError(message);
    }

    /** Frozen local work observation, independent of semantic admission and image promotion. */
    private static void resourceObservation() throws Exception {
        String spelling = "1234567890".repeat(410).substring(0, 4096);
        StringBuilder oracleText = new StringBuilder(spelling);
        Pattern fallback = Pattern.compile("\\d+");
        try (NativeText text = new NativeText(spelling)) {
            M3StringFacts prepared = text.value.facts();
            java.lang.reflect.Method size = Class.forName("M3FactsSizeAgent", true,
                    ClassLoader.getSystemClassLoader()).getMethod("sizeOf", Object.class);
            long factsBytes = ((Long) size.invoke(null, prepared)).longValue();
            long tableBytes = 0;
            try {
                Field table = M3StringFacts.class.getDeclaredField("ASCII_REGEX_CLASSES");
                table.setAccessible(true);
                tableBytes = ((Long) size.invoke(null, table.get(null))).longValue();
            } catch (NoSuchFieldException baselineHasNoTable) {
                // This absence is part of the original baseline, not an oracle substitute.
            }
            for (int index = 0; index < 2_000; index++) {
                lastScannedFacts = M3StringFacts.scan(text.value);
            }
            for (int index = 0; index < 5_000; index++) {
                int decision = M3StringRegexDp.tryMatch(text.value, "\\d+");
                workChecksum += decision == -1 ? (fallback.matcher(oracleText).matches() ? 1 : 0) : decision;
            }
            System.out.println("M3_REGEX_SHALLOW_SIZE facts_bytes=" + factsBytes
                    + " shared_ascii_table_bytes=" + tableBytes);
            for (int sample = 0; sample < 3; sample++) {
                long checksum = 0;
                long started = System.nanoTime();
                for (int index = 0; index < 500; index++) {
                    M3StringFacts scanned = M3StringFacts.scan(text.value);
                    lastScannedFacts = scanned;
                    checksum += scanned.javaHash;
                }
                long scanNanos = System.nanoTime() - started;
                int directDecisions = 0;
                started = System.nanoTime();
                for (int index = 0; index < 10_000; index++) {
                    int decision = M3StringRegexDp.tryMatch(text.value, "\\d+");
                    if (decision != -1) directDecisions++;
                    checksum += decision == -1 ? (fallback.matcher(oracleText).matches() ? 1 : 0) : decision;
                }
                long matchNanos = System.nanoTime() - started;
                workChecksum = checksum;
                System.out.println("M3_REGEX_LOCAL_WORK sample=" + sample + " units=4096 scan_repetitions=500"
                        + " scan_nanos=" + scanNanos + " warm_repetitions=10000 warm_nanos=" + matchNanos
                        + " direct_decisions=" + directDecisions + " checksum=" + workChecksum);
            }
        }
    }

    private static final class NativeText implements AutoCloseable {
        final M3String value;
        final long address;

        NativeText(String text) {
            if (text.isEmpty()) {
                value = M3String.empty();
                address = 0L;
                return;
            }
            char[] chars = text.toCharArray();
            M3StringAtom owner = M3StringAtom.localChars(chars, 0, chars.length,
                    String.UTF16, ++nextId, nextId);
            value = M3String.whole(owner);
            address = owner.address;
        }

        @Override public void close() {
            if (address != 0L) UNSAFE.freeMemory(address);
        }
    }
}
