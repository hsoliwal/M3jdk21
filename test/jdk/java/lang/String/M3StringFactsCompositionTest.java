/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary Verify every primitive M3StringFacts lane composes exactly across tuples and ranges
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringFactsCompositionTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class M3StringFactsCompositionTest {
    private static final Field STRING_M3;
    private static final Field M3_OWNER;
    private static final Method FACTS;
    private static final List<Field> FACT_FIELDS;
    private static long checks;

    static {
        try {
            STRING_M3 = String.class.getDeclaredField("m3");
            STRING_M3.setAccessible(true);
            Class<?> m3 = Class.forName("java.lang.M3String");
            M3_OWNER = m3.getDeclaredField("owner");
            M3_OWNER.setAccessible(true);
            FACTS = m3.getDeclaredMethod("facts");
            FACTS.setAccessible(true);
            Class<?> facts = Class.forName("java.lang.M3StringFacts");
            ArrayList<Field> fields = new ArrayList<>();
            for (Field field : facts.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    if (!field.getType().isPrimitive()) {
                        throw new AssertionError("non-primitive fact field " + field);
                    }
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
            FACT_FIELDS = List.copyOf(fields);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        deterministic();
        consumers();
        randomized();
        System.out.println(
                "M3_STRING_FACT_COMPOSITION_PASS|checks=" + checks
                        + "|fields=" + FACT_FIELDS.size());
    }

    private static void deterministic() throws Exception {
        String[] atoms = {
                "", "a", "ABC", " ", "\t", "\n", "\u2003", "\u00ff", "\u0100",
                "\ud83d", "\ude42", "\ud83d\ude42", "\u0000", "alpha", "omega"
        };
        for (String left : atoms) {
            for (String right : atoms) {
                assertComposedFacts(fresh(left), fresh(right), "pair");
            }
        }

        assertComposedFacts(fresh("\ud83d"), fresh("\ude42"), "split supplementary");
        assertComposedFacts(fresh(" \u2003"), fresh("alpha\u2002 "), "whitespace seam");
        assertComposedFacts(fresh("\u0000\u00ff"), fresh("\u0100\ud83d\ude42"), "width seam");

        String tuple = fresh(" \ud83d").concat(fresh("\ude42alpha\u2003 "));
        for (int begin = 0; begin <= tuple.length(); begin++) {
            for (int end = begin; end <= tuple.length(); end++) {
                String range = tuple.substring(begin, end);
                String flat = fresh(chars(range));
                sameFacts(range, flat, "range " + begin + ":" + end);
            }
        }
    }

    private static void consumers() throws Exception {
        String lowerOnly = fresh("alpha_123");
        same(lowerOnly, lowerOnly.toLowerCase(java.util.Locale.ROOT),
                "lowercase ASCII without uppercase returns same String");

        String upperOnly = fresh("ALPHA_123");
        same(upperOnly, upperOnly.toUpperCase(java.util.Locale.ROOT),
                "uppercase ASCII without lowercase returns same String");

        String mixed = fresh("AbC_123");
        equal("abc_123", mixed.toLowerCase(java.util.Locale.ROOT), "mixed lowercase content");
        equal("ABC_123", mixed.toUpperCase(java.util.Locale.ROOT), "mixed uppercase content");

        String noLineBreak = fresh("alpha beta\tomega");
        List<String> oneLine = noLineBreak.lines().toList();
        checks++;
        if (oneLine.size() != 1 || oneLine.get(0) != noLineBreak) {
            throw new AssertionError("line fast path must preserve singleton String identity");
        }

        String withBreaks = fresh("alpha\r\nbeta\ngamma\rdelta");
        List<String> lines = withBreaks.lines().toList();
        checks++;
        if (!lines.equals(List.of("alpha", "beta", "gamma", "delta"))) {
            throw new AssertionError("line semantics changed: " + lines);
        }

        String blank = fresh(" \t\n");
        checks++;
        if (!blank.isBlank()) throw new AssertionError("blank flag composition lost");

        String surrogate = fresh("\ud83d\ude42");
        Object surrogateFacts = FACTS.invoke(body(surrogate));
        Field characterFlags = surrogateFacts.getClass().getDeclaredField("characterFlags");
        characterFlags.setAccessible(true);
        int flags = characterFlags.getInt(surrogateFacts);
        Field hasSurrogate = surrogateFacts.getClass().getDeclaredField("FLAG_HAS_SURROGATE");
        hasSurrogate.setAccessible(true);
        checks++;
        if ((flags & hasSurrogate.getInt(null)) == 0) {
            throw new AssertionError("surrogate presence flag missing");
        }

        String categories = fresh("Aa_9 \t");
        Object categoryFacts = FACTS.invoke(body(categories));
        int categoryFlags = characterFlags.getInt(categoryFacts);
        for (String name : List.of(
                "FLAG_HAS_ASCII_UPPER",
                "FLAG_HAS_ASCII_LOWER",
                "FLAG_HAS_ASCII_DIGIT",
                "FLAG_HAS_ASCII_WORD",
                "FLAG_HAS_ASCII_SPACE")) {
            Field bit = categoryFacts.getClass().getDeclaredField(name);
            bit.setAccessible(true);
            checks++;
            if ((categoryFlags & bit.getInt(null)) == 0) {
                throw new AssertionError("character flag missing: " + name);
            }
        }
    }

    private static void same(Object expected, Object actual, String label) {
        checks++;
        if (expected != actual) throw new AssertionError(label);
    }

    private static void equal(String expected, String actual, String label) {
        checks++;
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void randomized() throws Exception {
        Random random = new Random(0x4d3346414354534cL);
        String[] atoms = {
                "", "a", "bc", "XYZ", " ", "\t", "\n", "\u2003", "\u2002",
                "\u00e9", "\u00ff", "\u0100", "\ud83d", "\ude42", "\ud83d\ude42",
                "\ud800", "\udc00", "\u0000"
        };

        for (int trial = 0; trial < 5_000; trial++) {
            String left = fresh(atoms[random.nextInt(atoms.length)]);
            String middle = fresh(atoms[random.nextInt(atoms.length)]);
            String right = fresh(atoms[random.nextInt(atoms.length)]);
            String tuple = left.concat(middle).concat(right);
            String flat = fresh(chars(tuple));
            sameFacts(tuple, flat, "random tuple " + trial);

            int begin = tuple.length() == 0 ? 0 : random.nextInt(tuple.length() + 1);
            int end = begin + random.nextInt(tuple.length() - begin + 1);
            String range = tuple.substring(begin, end);
            String flatRange = fresh(chars(range));
            sameFacts(range, flatRange, "random range " + trial);
        }
    }

    private static void assertComposedFacts(String left, String right, String label) throws Exception {
        String tuple = left.concat(right);
        char[] merged = new char[left.length() + right.length()];
        left.getChars(0, left.length(), merged, 0);
        right.getChars(0, right.length(), merged, left.length());
        String flat = fresh(merged);
        sameFacts(tuple, flat, label);
    }

    private static void sameFacts(String composed, String flat, String label) throws Exception {
        if (composed.length() == 0) {
            checks++;
            if (flat.length() != 0) {
                throw new AssertionError(label + " empty content mismatch");
            }
            return;
        }

        Object composedBody = body(composed);
        Object flatBody = body(flat);
        Object flatOwner = M3_OWNER.get(flatBody);
        checks++;
        if (!flatOwner.getClass().getName().equals("java.lang.M3StringAtom")) {
            throw new AssertionError(label + " flat oracle is not a scalar atom: " + flatOwner.getClass());
        }

        Object composedFacts = FACTS.invoke(composedBody);
        Object flatFacts = FACTS.invoke(flatBody);
        for (Field field : FACT_FIELDS) {
            Object left = field.get(composedFacts);
            Object right = field.get(flatFacts);
            checks++;
            if (!left.equals(right)) {
                throw new AssertionError(
                        label + " field=" + field.getName()
                                + " composed=" + left + " flat=" + right
                                + " content=" + printable(chars(composed)));
            }
        }
        checks++;
        if (!chars(composed).equals(chars(flat))) {
            throw new AssertionError(label + " logical content mismatch");
        }
    }

    private static Object body(String value) throws Exception {
        value.length();
        Object m3 = STRING_M3.get(value);
        if (m3 == null) {
            throw new AssertionError("String was not admitted to M3");
        }
        return m3;
    }

    private static String fresh(String value) {
        return new String(value.toCharArray());
    }

    private static String fresh(char[] value) {
        return new String(value);
    }

    private static String chars(String value) {
        char[] result = new char[value.length()];
        value.getChars(0, value.length(), result, 0);
        return new String(result);
    }

    private static String printable(String value) {
        StringBuilder out = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char unit = value.charAt(index);
            if (unit >= 0x20 && unit <= 0x7e) out.append(unit);
            else out.append(String.format("\\u%04x", (int) unit));
        }
        return out.toString();
    }
}
