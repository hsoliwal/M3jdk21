/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors
 */

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Deterministic UTF-16 edge corpus shared by stock and M3-backed String runs. */
final class M3StringUtf16DifferentialCorpus {
    private static final long SEED = 0x4d335554463136L;

    private M3StringUtf16DifferentialCorpus() {}

    static List<char[]> values() {
        ArrayList<char[]> values = new ArrayList<>();
        values.add(new char[0]);
        values.add(chars('a'));
        values.add(chars('\u0000', '\uffff'));
        values.add(chars('\ud800'));
        values.add(chars('\udfff'));
        values.add(chars('\ud800', '\udc00'));
        values.add(chars('\ud83d', '\ude42'));
        values.add(chars('a', '\ud83d', '\ude42', 'b'));
        values.add(chars('\ud83d', 'X', '\ude42'));
        values.add(chars('\ud800', '\ud800', '\udc00', '\udfff'));
        values.add(chars('\udc00', '\ud800'));
        values.add(chars('\u00ff', '\u0100', '\u20ac'));

        Random random = new Random(SEED);
        char[] alphabet = {
                0, 1, 'A', 'a', '\u00ff', '\u0100', '\u20ac', '\uffff',
                '\ud800', '\udbff', '\udc00', '\udfff'
        };
        for (int trial = 0; trial < 512; trial++) {
            int length = random.nextInt(96);
            char[] value = new char[length];
            for (int i = 0; i < value.length; i++) {
                value[i] = alphabet[random.nextInt(alphabet.length)];
            }
            values.add(value);
        }
        return List.copyOf(values);
    }

    static String fresh(char[] value) {
        return new String(value.clone());
    }

    static String composed(char[] value) {
        String flat = fresh(value);
        int a = flat.length() / 3;
        int b = (flat.length() * 2) / 3;
        return String.join("", flat.substring(0, a), flat.substring(a, b), flat.substring(b));
    }

    static byte[] utf8Oracle(char[] value) {
        return new String(value).getBytes(StandardCharsets.UTF_8);
    }

    private static char[] chars(char... value) {
        return value;
    }
}
