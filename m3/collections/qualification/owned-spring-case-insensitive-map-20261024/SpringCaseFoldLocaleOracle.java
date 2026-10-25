/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Locale differential oracle for the D2 fold fast path: the owned map against the Spring 6.2.0 provider, both built
 * with the same locale, run the same seeded operations over keys whose case folding differs by locale (ASCII
 * letters, dotted and dotless i, U+0130, the Kelvin sign, final sigma, sharp s, a titlecase digraph, a ligature and
 * mixed ASCII/non-ASCII spellings). After every operation the returned value and every query's get, containsKey
 * and getOrDefault must agree, and the ordered entries must be equal. Prints one PASS line with the counts.
 * Usage: SpringCaseFoldLocaleOracle (provider and owned classes on the class path).
 */
public final class SpringCaseFoldLocaleOracle {
    private static final String PROVIDER = "org.springframework.util.LinkedCaseInsensitiveMap";
    private static final String OWNED = "com.m3.collections.spring.M3LinkedCaseInsensitiveMap";
    private static final String[] KEYS = {
        "Content-Type", "content-type", "CONTENT-TYPE", "Accept", "accept", "X-Id", "x-id", "",
        "I", "i", "İ", "ı", "Ii", "TITLE", "title", "K", "k", "K", "Kelvin", "Kelvin",
        "ΣΑΣ", "σας", "σασ", "Straße", "STRASSE", "ǅ",
        "Ǆ", "ǆ", "ﬀ", "ff", "123", "a-b_c.d", "Été", "éTÉ"};
    private static final Locale[] LOCALES = {
        Locale.ROOT, Locale.ENGLISH, Locale.US, Locale.GERMAN, Locale.FRENCH, Locale.forLanguageTag("tr"),
        Locale.forLanguageTag("az"), Locale.forLanguageTag("lt"), Locale.forLanguageTag("el"),
        Locale.forLanguageTag("nl")};
    private static final int OPERATIONS = 4_000;
    private static long checks;

    private SpringCaseFoldLocaleOracle() {
    }

    public static void main(String[] args) throws Exception {
        for (Locale locale : LOCALES) {
            run(locale, new Random(0xD2_F01DL ^ locale.toLanguageTag().hashCode()));
        }
        System.out.println("PASS SPRING_CASE_FOLD_LOCALE_ORACLE locales=" + LOCALES.length + " keys=" + KEYS.length
                + " operations=" + (long) OPERATIONS * LOCALES.length + " checks=" + checks);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Integer> create(String type, Locale locale) throws Exception {
        Constructor<?> constructor = Class.forName(type).getConstructor(Locale.class);
        return (Map<String, Integer>) constructor.newInstance(locale);
    }

    private static void run(Locale locale, Random random) throws Exception {
        Map<String, Integer> provider = create(PROVIDER, locale);
        Map<String, Integer> owned = create(OWNED, locale);
        for (int step = 0; step < OPERATIONS; step++) {
            String key = KEYS[random.nextInt(KEYS.length)];
            Integer value = random.nextInt(8) == 0 ? null : random.nextInt(100);
            int operation = random.nextInt(6);
            Object expected = apply(provider, operation, key, value);
            Object actual = apply(owned, operation, key, value);
            check(expected, actual, locale, step, "operation " + operation + " on " + key);
            for (String query : KEYS) {
                check(provider.get(query), owned.get(query), locale, step, "get " + query);
                check(provider.containsKey(query), owned.containsKey(query), locale, step, "containsKey " + query);
                check(provider.getOrDefault(query, -1), owned.getOrDefault(query, -1), locale, step,
                        "getOrDefault " + query);
            }
            check(entries(provider), entries(owned), locale, step, "ordered entries");
        }
    }

    private static Object apply(Map<String, Integer> map, int operation, String key, Integer value) {
        try {
            return switch (operation) {
                case 0, 1 -> map.put(key, value);
                case 2 -> map.putIfAbsent(key, value);
                case 3 -> map.computeIfAbsent(key, spelling -> value == null ? null : value + spelling.length());
                case 4 -> map.remove(key);
                default -> map.keySet().remove(key);
            };
        } catch (RuntimeException failure) {
            return failure.getClass().getName();
        }
    }

    private static List<String> entries(Map<String, Integer> map) {
        List<String> entries = new ArrayList<>();
        map.forEach((key, value) -> entries.add(key + "=" + value));
        return entries;
    }

    private static void check(Object expected, Object actual, Locale locale, int step, String where) {
        checks++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(locale.toLanguageTag() + " step " + step + " " + where + ": provider=" + expected
                    + " owned=" + actual);
        }
    }
}
