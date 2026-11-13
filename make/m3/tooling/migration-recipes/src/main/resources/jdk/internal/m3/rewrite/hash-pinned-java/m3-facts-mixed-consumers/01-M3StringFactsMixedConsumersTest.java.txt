// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary Prepared M3StringFacts gate the mixed-side String consumers (flat literal needle, prefix,
 *          suffix, replace target) and the ASCII case lane applies to every locale without
 *          language-specific casing: the flat-needle gates agree exactly with the M3-needle gates,
 *          never reject a present needle, and the locale lane matches the stock mapping
 * @modules java.base/java.lang:+open
 * @run main M3StringFactsMixedConsumersTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringFactsMixedConsumersTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class M3StringFactsMixedConsumersTest {
    private static final String[] ATOMS = {
        "a", "bc", "XYZ", "hello", " ", "\t", "é", "ÿ", "Ā", " ", "🙂",
        "\ud83d", "\ude42", "\u0000", "abcabc", "needle", "The quick brown fox", "0123456789", "ab"
    };
    private static long checks;
    private static Method internChars;
    private static Method concat;
    private static Method facts;
    private static Method mayContainFacts;
    private static Method mayContainFlat;
    private static Method prefixFacts;
    private static Method prefixFlat;
    private static Method suffixFacts;
    private static Method suffixFlat;

    public static void main(String[] args) throws Exception {
        bind();
        gates();
        localeLane();
        consumers();
        System.out.println("M3StringFactsMixedConsumersTest checks=" + checks);
    }

    private static void bind() throws Exception {
        Class<?> pool = Class.forName("java.lang.M3StringPool");
        Class<?> m3 = Class.forName("java.lang.M3String");
        Class<?> factsClass = Class.forName("java.lang.M3StringFacts");
        internChars = accessible(pool.getDeclaredMethod("internChars", char[].class, int.class, int.class));
        concat = accessible(m3.getDeclaredMethod("concat", m3));
        facts = accessible(m3.getDeclaredMethod("facts"));
        mayContainFacts = accessible(factsClass.getDeclaredMethod("mayContain", factsClass));
        mayContainFlat = accessible(factsClass.getDeclaredMethod("mayContain", String.class));
        prefixFacts = accessible(factsClass.getDeclaredMethod("prefixMayMatch", factsClass));
        prefixFlat = accessible(factsClass.getDeclaredMethod("prefixMayMatch", String.class));
        suffixFacts = accessible(factsClass.getDeclaredMethod("suffixMayMatch", factsClass));
        suffixFlat = accessible(factsClass.getDeclaredMethod("suffixMayMatch", String.class));
        for (Field field : factsClass.getDeclaredFields()) {
            check(java.lang.reflect.Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive(),
                    "facts stay primitive: " + field);
        }
    }

    private static Method accessible(Method method) {
        method.setAccessible(true);
        return method;
    }

    /** Build an M3String for the spelling: a scalar atom, or a tuple of two atoms when split. */
    private static Object m3(String spelling, int split) throws Exception {
        if (split <= 0 || split >= spelling.length()) {
            char[] chars = spelling.toCharArray();
            return internChars.invoke(null, chars, 0, chars.length);
        }
        return concat.invoke(m3(spelling.substring(0, split), 0), m3(spelling.substring(split), 0));
    }

    /** Agreement with the M3-needle gates and soundness for present needles, prefixes and suffixes. */
    private static void gates() throws Exception {
        Random random = new Random(0x4d334641435453L);
        long rejectedAbsent = 0;
        long absent = 0;
        for (int round = 0; round < 2_500; round++) {
            String source = spell(random, 1 + random.nextInt(5));
            if (source.isEmpty()) continue;
            Object sourceFacts = facts.invoke(m3(source, random.nextBoolean() ? random.nextInt(source.length()) : 0));
            // Present needles: random spans of the source, including the whole, a prefix and a suffix.
            for (int probe = 0; probe < 4; probe++) {
                int begin = random.nextInt(source.length());
                int end = begin + 1 + random.nextInt(source.length() - begin);
                String needle = source.substring(begin, end);
                Object needleFacts = facts.invoke(m3(needle, 0));
                boolean viaFacts = (boolean) mayContainFacts.invoke(sourceFacts, needleFacts);
                boolean viaFlat = (boolean) mayContainFlat.invoke(sourceFacts, needle);
                check(viaFacts == viaFlat, "mayContain agreement " + show(source) + " / " + show(needle));
                check(viaFlat, "present needle rejected " + show(source) + " / " + show(needle));
                if (begin == 0) {
                    check((boolean) prefixFlat.invoke(sourceFacts, needle), "present prefix rejected " + show(needle));
                    check((boolean) prefixFlat.invoke(sourceFacts, needle)
                            == (boolean) prefixFacts.invoke(sourceFacts, needleFacts), "prefix agreement");
                }
                if (end == source.length()) {
                    check((boolean) suffixFlat.invoke(sourceFacts, needle), "present suffix rejected " + show(needle));
                    check((boolean) suffixFlat.invoke(sourceFacts, needle)
                            == (boolean) suffixFacts.invoke(sourceFacts, needleFacts), "suffix agreement");
                }
            }
            // Random needles: agreement only (absence is a candidate verdict, never asserted).
            for (int probe = 0; probe < 3; probe++) {
                String needle = spell(random, 1 + random.nextInt(2));
                if (needle.isEmpty()) continue;
                Object needleFacts = facts.invoke(m3(needle, 0));
                boolean viaFacts = (boolean) mayContainFacts.invoke(sourceFacts, needleFacts);
                boolean viaFlat = (boolean) mayContainFlat.invoke(sourceFacts, needle);
                check(viaFacts == viaFlat, "random agreement " + show(source) + " / " + show(needle));
                check((boolean) prefixFlat.invoke(sourceFacts, needle)
                        == (boolean) prefixFacts.invoke(sourceFacts, needleFacts), "random prefix agreement");
                check((boolean) suffixFlat.invoke(sourceFacts, needle)
                        == (boolean) suffixFacts.invoke(sourceFacts, needleFacts), "random suffix agreement");
                if (!source.contains(needle)) {
                    absent++;
                    if (!viaFlat) rejectedAbsent++;
                } else {
                    check(viaFlat, "present random needle rejected " + show(source) + " / " + show(needle));
                }
            }
        }
        check((boolean) mayContainFlat.invoke(facts.invoke(m3("abc", 0)), ""), "empty needle");
        check((boolean) prefixFlat.invoke(facts.invoke(m3("abc", 0)), ""), "empty prefix");
        check((boolean) suffixFlat.invoke(facts.invoke(m3("abc", 0)), ""), "empty suffix");
        check(!(boolean) mayContainFlat.invoke(facts.invoke(m3("abc", 0)), "abcd"), "longer needle");
        System.out.println("absent random needles=" + absent + " rejected by facts=" + rejectedAbsent);
    }

    /** The ASCII lane condition must match the stock mapping for every available locale. */
    private static void localeLane() {
        String[] samples = {"Hello World", "ABC", "abc", "MiXeD 123", "I", "i", "TITLE case", ""};
        List<Locale> locales = new ArrayList<>(List.of(Locale.getAvailableLocales()));
        locales.add(Locale.ROOT);
        locales.add(Locale.forLanguageTag("tr-TR"));
        locales.add(Locale.forLanguageTag("az-Latn-AZ"));
        locales.add(Locale.forLanguageTag("lt-LT"));
        int lane = 0;
        for (Locale locale : locales) {
            String lang = locale.getLanguage();
            boolean asciiLane = !(lang.equals("tr") || lang.equals("az") || lang.equals("lt"));
            for (String sample : samples) {
                String lower = sample.toLowerCase(locale);
                String upper = sample.toUpperCase(locale);
                if (asciiLane) {
                    lane++;
                    check(lower.equals(asciiCase(sample, false)), "lower lane parity " + locale + " " + sample);
                    check(upper.equals(asciiCase(sample, true)), "upper lane parity " + locale + " " + sample);
                }
            }
        }
        check(!"I".toLowerCase(Locale.forLanguageTag("tr")).equals("i"), "tr excluded for a reason");
        check(!"I".toLowerCase(Locale.forLanguageTag("az")).equals("i"), "az excluded for a reason");
        check(lane > 0, "lane locales present");
    }

    /** Mixed-side consumers against the stock results (flag-on: M3 receiver, flat literal argument). */
    private static void consumers() {
        Random random = new Random(0x4d3343554d455253L);
        for (int round = 0; round < 1_500; round++) {
            String spelling = spell(random, 1 + random.nextInt(5));
            String m3Side = new String(spelling.toCharArray());
            String needle = round % 3 == 0 && !spelling.isEmpty()
                    ? spelling.substring(random.nextInt(spelling.length()))
                    : spell(random, 1 + random.nextInt(2));
            check(m3Side.indexOf(needle) == stockIndexOf(spelling, needle, 0), "indexOf " + show(spelling) + " / " + show(needle));
            int from = spelling.isEmpty() ? 0 : random.nextInt(spelling.length() + 1);
            check(m3Side.indexOf(needle, from) == stockIndexOf(spelling, needle, from), "indexOf from");
            check(m3Side.lastIndexOf(needle) == stockLastIndexOf(spelling, needle), "lastIndexOf");
            check(m3Side.startsWith(needle) == spelling.startsWith(needle), "startsWith");
            check(m3Side.endsWith(needle) == spelling.endsWith(needle), "endsWith");
            check(m3Side.contains(needle) == spelling.contains(needle), "contains");
            String replaced = m3Side.replace(needle.isEmpty() ? "x" : needle, "<>");
            check(replaced.equals(spelling.replace(needle.isEmpty() ? "x" : needle, "<>")), "replace");
            if (!needle.isEmpty() && !spelling.contains(needle)) {
                check(replaced == m3Side, "replace without a match returns this");
            }
            check(m3Side.toLowerCase().equals(spelling.toLowerCase()), "toLowerCase default");
            check(m3Side.toUpperCase(Locale.GERMANY).equals(spelling.toUpperCase(Locale.GERMANY)), "toUpperCase de");
            check(m3Side.toLowerCase(Locale.forLanguageTag("tr")).equals(spelling.toLowerCase(Locale.forLanguageTag("tr"))), "toLowerCase tr");
        }
    }

    private static int stockIndexOf(String source, String needle, int from) {
        StringBuilder flat = new StringBuilder(source);
        return flat.indexOf(needle, from);
    }

    private static int stockLastIndexOf(String source, String needle) {
        StringBuilder flat = new StringBuilder(source);
        return flat.lastIndexOf(needle);
    }

    private static String asciiCase(String value, boolean upper) {
        StringBuilder out = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char unit = value.charAt(index);
            if (upper && unit >= 'a' && unit <= 'z') unit = (char) (unit - ('a' - 'A'));
            else if (!upper && unit >= 'A' && unit <= 'Z') unit = (char) (unit + ('a' - 'A'));
            out.append(unit);
        }
        return out.toString();
    }

    private static String spell(Random random, int parts) {
        StringBuilder out = new StringBuilder();
        for (int index = 0; index < parts; index++) out.append(ATOMS[random.nextInt(ATOMS.length)]);
        return out.toString();
    }

    private static String show(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char unit = value.charAt(index);
            out.append(unit >= 0x20 && unit <= 0x7e ? String.valueOf(unit) : String.format("\\u%04x", (int) unit));
        }
        return out.append('"').toString();
    }

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
}
