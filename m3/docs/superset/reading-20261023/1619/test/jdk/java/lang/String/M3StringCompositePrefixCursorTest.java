/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 *
 * @test
 * @summary Reuse M3String composite leaf cursors for long UTF-16 prefix comparisons
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringCompositePrefixCursorTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringCompositePrefixCursorTest
 */
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

public class M3StringCompositePrefixCursorTest {
    private static final Field M3;
    private static final Field OWNER;
    private static final Field VALUE;
    private static final Method DIRECT;
    private static final Method BRIDGE;
    private static long checks;

    static {
        try {
            Class<?> m3 = Class.forName("java.lang.M3String");
            Class<?> membrane = Class.forName("java.lang.M3StringPrecomputeBridge");
            M3 = String.class.getDeclaredField("m3");
            OWNER = m3.getDeclaredField("owner");
            VALUE = m3.getDeclaredField("value");
            DIRECT = m3.getDeclaredMethod("startsWith", m3, int.class);
            BRIDGE = membrane.getDeclaredMethod("startsWith", m3, String.class, int.class);
            for (Field f : new Field[] {M3, OWNER, VALUE}) f.setAccessible(true);
            DIRECT.setAccessible(true);
            BRIDGE.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private static void check(boolean ok, String why) {
        checks++;
        if (!ok) throw new AssertionError(why);
    }

    private static boolean oracle(char[] source, String prefix, int offset) {
        if (offset < 0 || offset > source.length - prefix.length()) return false;
        for (int i = 0; i < prefix.length(); i++) {
            if (source[offset + i] != prefix.charAt(i)) return false;
        }
        return true;
    }

    private static String composite(char[] text, int pieces) {
        String result = "";
        int length = text.length;
        int cursor = 0;
        for (int i = 0; i < pieces; i++) {
            int end = (int) ((long) (i + 1) * length / pieces);
            if (end > cursor) {
                String piece = new String(Arrays.copyOfRange(text, cursor, end));
                result = result.concat(piece);
            }
            cursor = end;
        }
        return result;
    }

    private static Object body(String value) throws Exception {
        value.length();
        return M3.get(value);
    }

    private static void parity(char[] sourceChars, String source, String prefix, int offset)
            throws Exception {
        boolean expected = oracle(sourceChars, prefix, offset);
        check(source.startsWith(prefix, offset) == expected,
                "public UTF-16 startsWith offset=" + offset + " length=" + prefix.length());
        Object backing = body(source);
        check(backing != null, "source uses canonical M3 storage");
        if (offset < 0 || offset > sourceChars.length - prefix.length()) {
            return; // The public String boundary owns invalid-offset handling.
        }
        check((boolean) BRIDGE.invoke(null, backing, prefix, offset) == expected,
                "original flat/composite bridge parity");

        Object prefixBacking = body(prefix);
        if (prefixBacking != null) {
            check((boolean) DIRECT.invoke(backing, prefixBacking, offset) == expected,
                    "direct M3 composite-prefix parity");
        }
    }

    private static void directed() throws Exception {
        char[] text = new char[8_192];
        for (int i = 0; i < text.length; i++) {
            text[i] = switch (i % 12) {
                case 0 -> '\uD83D';
                case 1 -> '\uDE03';
                case 2 -> '\uD800';
                case 3 -> '\uDC00';
                case 4 -> '\0';
                case 5 -> '\uFFFF';
                case 6 -> '\u00FF';
                case 7 -> '\u0100';
                default -> (char) ('a' + i % 26);
            };
        }
        String source = composite(text, 257);
        Object backing = body(source);
        check(backing != null, "source has M3 storage");
        Object owner = OWNER.get(backing);
        long coordinate = VALUE.getLong(backing);
        check(owner.getClass().getName().equals("java.lang.M3StringTuple"),
                "source is a shared tuple DAG");

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
        for (int length : new int[] {0, 1, 2, 31, 63, 64, 65, 66, 127, 256, 1000, 4096, 8192}) {
=======
        for (int length : new int[] {0, 1, 2, 31, 63, 64, 65, 66, 127, 255, 256, 257, 1000, 4096, 8192}) {
>>>>>>> 0cd0944136c1a87b1bb8e15e8052ed1fbdfb74c5
            for (int offset : new int[] {-1, 0, 1, 31, 32, 1024, 4096, text.length - length,
                    text.length - length + 1, Integer.MAX_VALUE}) {
                if (offset >= 0 && offset <= text.length - length) {
                    String prefix = new String(Arrays.copyOfRange(text, offset, offset + length));
                    parity(text, source, prefix, offset);
                    if (length >= 2) {
                        char[] miss = prefix.toCharArray();
                        miss[miss.length - 1] ^= 1;
                        parity(text, source, new String(miss), offset);
                    }
                } else {
                    parity(text, source, "", offset);
                }
            }
        }
        // Composite prefix and independently constructed source: no identity-based shortcuts.
        for (int offset : new int[] {0, 11, 31, 127, 1024}) {
            char[] prefix = Arrays.copyOfRange(text, offset, offset + 2_048);
            String compositePrefix = composite(prefix, 73);
            parity(text, source, compositePrefix, offset);
        }
        check(owner == OWNER.get(body(source))
                && coordinate == VALUE.getLong(body(source)),
                "prefix probes never replace or flatten canonical source coordinates");

        char[] materialized = source.toCharArray();
        check(Arrays.equals(materialized, text), "UTF-16 copies remain byte-for-byte exact");
        String flat = new String(text);
        check(Arrays.equals(source.getBytes(StandardCharsets.UTF_8),
                flat.getBytes(StandardCharsets.UTF_8)),
                "JNI-visible UTF-8 projection remains unchanged");
    }

    private static void randomized() throws Exception {
        Random random = new Random(0x4d33535452494e47L);
        for (int trial = 0; trial < 240; trial++) {
            int size = 80 + random.nextInt(1200);
            char[] original = new char[size];
            for (int i = 0; i < size; i++) {
                original[i] = switch (random.nextInt(10)) {
                    case 0 -> '\uD800';
                    case 1 -> '\uDC00';
                    case 2 -> '\0';
                    case 3 -> '\uFFFF';
                    default -> (char) random.nextInt(0x10000);
                };
            }
            String source = composite(original, 2 + random.nextInt(80));
            int offset = random.nextInt(size);
            int length = random.nextInt(size - offset + 1);
            String prefix = new String(Arrays.copyOfRange(original, offset, offset + length));
            parity(original, source, prefix, offset);
            parity(original, source, prefix, offset + 1);
            parity(original, source, prefix, -1);
            parity(original, source, prefix, size);
        }
    }

    public static void main(String[] ignored) throws Exception {
        directed();
        randomized();
        System.out.println("M3_STRING_COMPOSITE_PREFIX_CURSOR_PASS checks=" + checks);
    }
}
