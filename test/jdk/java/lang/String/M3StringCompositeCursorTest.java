/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Composite M3String equality and prefix compare traverse canonical tuple leaves without flattening
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringCompositeCursorTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringCompositeCursorTest
 */

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Random;

public class M3StringCompositeCursorTest {
    private static final Field M3;
    private static long checks;

    static {
        try {
            M3 = String.class.getDeclaredField("m3");
            M3.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private static void check(boolean pass, String message) {
        checks++;
        if (!pass) throw new AssertionError(message);
    }

    private static String fresh(char[] units) {
        return new String(units.clone());
    }

    private static String composite(char[] units, int seed) {
        String result = "";
        for (int at = 0; at < units.length;) {
            int count = Math.min(units.length - at, 1 + (at + seed) % 7);
            result = result.concat(fresh(Arrays.copyOfRange(units, at, at + count)));
            at += count;
        }
        return result;
    }

    private static boolean starts(char[] source, char[] prefix, int offset) {
        if (offset < 0 || offset > source.length - prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (source[offset + i] != prefix[i]) return false;
        }
        return true;
    }

    private static void oracle(Random random, int trial) throws Exception {
        char[] alphabet = {'A', 'a', 'z', '0', 0, '\u00ff', '\u2003', '\uD83D', '\uDE03', '\uD800', '\uDC00'};
        char[] content = new char[128 + random.nextInt(320)];
        for (int i = 0; i < content.length; i++) content[i] = alphabet[random.nextInt(alphabet.length)];
        String composed = composite(content, trial);
        check(M3.get(composed) != null, "M3 composite admission");
        String scalar = fresh(content);
        check(composed.equals(scalar), "tuple/scalar equality " + trial);
        check(scalar.equals(composed), "scalar/tuple equality " + trial);
        check(composed.equals(composite(content, trial + 2)), "tuple/tuple equality " + trial);
        char[] changed = content.clone();
        int mutation = random.nextInt(content.length);
        changed[mutation] = (char) (changed[mutation] ^ 1);
        check(!composed.equals(fresh(changed)), "unequal UTF-16 " + trial);
        int a = random.nextInt(content.length - 64);
        int len = 32 + random.nextInt(Math.min(96, content.length - a) - 31);
        char[] selected = Arrays.copyOfRange(content, a, a + len);
        String prefix = composite(selected, trial + 3);
        check(composed.startsWith(prefix, a), "matching offset prefix " + trial);
        String slice = composed.substring(a, a + len);
        check(slice.equals(fresh(selected)), "tuple slice equals scalar " + trial);
        check(slice.startsWith(prefix), "tuple slice prefix " + trial);
        for (int offset : new int[] {-1, 0, a, a + 1, content.length - len, content.length + 1}) {
            check(composed.startsWith(prefix, offset) == starts(content, selected, offset),
                    "offset prefix exact UTF-16 " + trial + "/" + offset);
        }
        check(!composed.startsWith(fresh(new char[] {'x'}), composed.length()),
                "out-of-bounds prefix " + trial);
    }

    public static void main(String[] args) throws Exception {
        Random random = new Random(0x4d335354524545L);
        for (int trial = 0; trial < 400; trial++) oracle(random, trial);
        System.out.println("M3_STRING_COMPOSITE_CURSOR_PASS checks=" + checks);
    }
}
