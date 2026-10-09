/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary Value-based Locale admission for canonical M3 ASCII String case transforms
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringAsciiLocaleLanguageValueTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringAsciiLocaleLanguageValueTest
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Random;

public class M3StringAsciiLocaleLanguageValueTest {
    private static final Field M3;
    private static final Method LANGUAGE_IS_ASCII_SAFE;
    private static long checks;

    static {
        try {
            M3 = String.class.getDeclaredField("m3");
            M3.setAccessible(true);
            LANGUAGE_IS_ASCII_SAFE =
                    String.class.getDeclaredMethod("asciiCaseMappingLocale", Locale.class);
            LANGUAGE_IS_ASCII_SAFE.setAccessible(true);
        } catch (ReflectiveOperationException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    public static void main(String[] args) throws Exception {
        casesForUnsafeLanguages();
        rootAndOtherSafeLanguages();
        randomizedAsciiValues();
        System.out.println("M3_STRING_ASCII_LOCALE_VALUE_PASS|checks=" + checks);
    }

    private static void casesForUnsafeLanguages() throws Exception {
        for (String language : new String[] {"tr", "az", "lt"}) {
            Locale candidate =
                    new Locale.Builder().setLanguage(new String(language)).build();
            check(!(boolean) LANGUAGE_IS_ASCII_SAFE.invoke(null, candidate),
                    language + " must bypass canonical ASCII case mapping");
        }

        for (String language : new String[] {"tr", "az"}) {
            Locale candidate =
                    new Locale.Builder().setLanguage(new String(language)).build();
            String lowercaseI = admitted('i');
            String uppercaseI = admitted('I');
            check(lowercaseI.toUpperCase(candidate).equals("\u0130"),
                    language + ": uppercase i needs dotted capital I");
            check(uppercaseI.toLowerCase(candidate).equals("\u0131"),
                    language + ": lowercase I needs dotless i");
        }

        Locale lithuanian = Locale.forLanguageTag("lt");
        String mark = new String(new char[] {'I', '\u0301'});
        requireM3(mark);
        check(mark.toLowerCase(lithuanian).equals("i\u0307\u0301"),
                "Lithuanian combining acute lowercasing keeps its dot-above rule");
    }

    private static void rootAndOtherSafeLanguages() throws Exception {
        for (Locale candidate : new Locale[] {
                Locale.ROOT, Locale.ENGLISH, Locale.GERMAN, Locale.JAPANESE
        }) {
            check((boolean) LANGUAGE_IS_ASCII_SAFE.invoke(null, candidate),
                    "safe ASCII language admitted: " + candidate);
            String text = new String(new char[] {'A', 'b', 'C', 'd', '9'});
            requireM3(text);
            String lower = text.toLowerCase(candidate);
            String upper = text.toUpperCase(candidate);
            check(lower.equals("abcd9"), "lower value " + candidate);
            check(upper.equals("ABCD9"), "upper value " + candidate);
            requireM3(lower);
            requireM3(upper);
        }
    }

    private static void randomizedAsciiValues() throws Exception {
        Random random = new Random(0xA5C110CAL);
        char[] alphabet =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_ -".toCharArray();
        for (int trial = 0; trial < 512; trial++) {
            char[] units = new char[1 + random.nextInt(70)];
            for (int i = 0; i < units.length; i++) {
                units[i] = alphabet[random.nextInt(alphabet.length)];
            }
            String original = new String(units);
            requireM3(original);

            char[] lower = units.clone();
            char[] upper = units.clone();
            for (int i = 0; i < units.length; i++) {
                if (lower[i] >= 'A' && lower[i] <= 'Z') lower[i] += 'a' - 'A';
                if (upper[i] >= 'a' && upper[i] <= 'z') upper[i] -= 'a' - 'A';
            }
            check(original.toLowerCase(Locale.ROOT).equals(new String(lower)),
                    "random ROOT lower " + trial);
            check(original.toUpperCase(Locale.ROOT).equals(new String(upper)),
                    "random ROOT upper " + trial);
        }
    }

    private static String admitted(char unit) throws Exception {
        String value = new String(new char[] {unit});
        requireM3(value);
        return value;
    }

    private static void requireM3(String input) throws IllegalAccessException {
        check(M3.get(input) != null, "String must be M3-backed");
    }

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }
}
