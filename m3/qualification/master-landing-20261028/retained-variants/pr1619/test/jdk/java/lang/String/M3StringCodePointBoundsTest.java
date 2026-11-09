/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 */

/*
 * @test
 * @summary Native code-point conversion checks capacity and coder before writing each UTF-16 unit
 * @modules java.base/java.lang:open java.base/jdk.internal.misc:open
 * @run main/othervm -Xmx128m -XX:+UnlockExperimentalVMOptions -XX:-UseM3StringStorage M3StringCodePointBoundsTest
 * @run main/othervm -Xmx128m -XX:+UnlockExperimentalVMOptions -XX:-UseM3StringStorage -XX:-CompactStrings M3StringCodePointBoundsTest
 * @run main/othervm -Xmx128m -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringCodePointBoundsTest requireM3
 * @run main/othervm -Xmx128m -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringCodePointBoundsTest requireM3
 */

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Direct malformed-size cases require the repaired factory. Baseline authoring
 * evidence runs these only with bounds-checking write instrumentation, never with
 * raw Unsafe. Valid cases independently verify native units and public String
 * construction; the optional argument requires actual installed M3 String admission.
 */
public final class M3StringCodePointBoundsTest {
    private static Method factory;
    private static Method charAt;
    private static Field address;
    private static Field length;
    private static Method freeMemory;
    private static Object unsafe;
    private static long checks;

    private record Invalid(String name, int[] points, int length, byte coder,
            Class<? extends Throwable> failure) { }

    private record Valid(String name, int[] points, int offset, int count, byte coder,
            char[] expected) { }

    public static void main(String[] args) throws Exception {
        boolean requireM3 = args.length == 1 && args[0].equals("requireM3");
        bind();
        Invalid[] invalid = {
            new Invalid("zero-length", new int[] {'A'}, 0, (byte) 1, InternalError.class),
            new Invalid("negative-length", new int[] {'A'}, -1, (byte) 1, InternalError.class),
            new Invalid("minimum-length", new int[] {'A'}, Integer.MIN_VALUE, (byte) 1, InternalError.class),
            new Invalid("bmp-tail", new int[] {'A', 'B'}, 1, (byte) 1, InternalError.class),
            new Invalid("pair-single-slot", new int[] {0x1f642}, 1, (byte) 1, InternalError.class),
            new Invalid("pair-tail-slot", new int[] {'A', 0x1f642}, 2, (byte) 1, InternalError.class),
            new Invalid("latin1-pair-room", new int[] {0x1f642}, 2, (byte) 0, IllegalArgumentException.class),
            new Invalid("latin1-pair-short", new int[] {0x1f642}, 1, (byte) 0, IllegalArgumentException.class),
            new Invalid("latin1-bmp", new int[] {0x100}, 1, (byte) 0, IllegalArgumentException.class),
            new Invalid("invalid-low", new int[] {-1}, 1, (byte) 1, IllegalArgumentException.class),
            new Invalid("invalid-high", new int[] {0x110000}, 1, (byte) 1, IllegalArgumentException.class),
            new Invalid("excess-capacity", new int[] {'A'}, 2, (byte) 1, InternalError.class)
        };
        for (Invalid scenario : invalid) {
            Throwable failure = null;
            Object atom = null;
            try {
                atom = factory.invoke(null, scenario.points(), 0, scenario.points().length,
                        scenario.length(), scenario.coder(), 101L, 201L);
            } catch (InvocationTargetException observed) {
                failure = observed.getCause();
            } finally {
                if (atom != null) freeMemory.invoke(unsafe, address.getLong(atom));
            }
            check(failure != null && failure.getClass() == scenario.failure(),
                    scenario.name() + ": expected " + scenario.failure().getName()
                            + ", observed " + failure);
        }
        Valid[] valid = {
            new Valid("empty", new int[0], 0, 0, (byte) 0, new char[0]),
            new Valid("latin1", new int[] {0, 'A', 0xff}, 0, 3, (byte) 0,
                    new char[] {0, 'A', '\u00ff'}),
            new Valid("bmp-surrogates", new int[] {0x100, 0xd800, 0xdc00, 0x10ffff}, 0, 4, (byte) 1,
                    new char[] {'\u0100', '\ud800', '\udc00', '\udbff', '\udfff'}),
            new Valid("supplementary", new int[] {0x1f642, 0x10000, 0x10ffff}, 0, 3, (byte) 1,
                    new char[] {'\ud83d', '\ude42', '\ud800', '\udc00', '\udbff', '\udfff'}),
            new Valid("offset", new int[] {-1, 0x1f642, 'x', -1}, 1, 2, (byte) 1,
                    new char[] {'\ud83d', '\ude42', 'x'})
        };
        for (Valid scenario : valid) {
            Object atom = factory.invoke(null, scenario.points(), scenario.offset(), scenario.count(),
                    scenario.expected().length, scenario.coder(), 301L, 401L);
            try {
                check(length.getInt(atom) == scenario.expected().length, scenario.name() + ": native length");
                for (int i = 0; i < scenario.expected().length; i++) {
                    check((char) charAt.invoke(atom, i) == scenario.expected()[i],
                            scenario.name() + ": independent native UTF-16 unit");
                }
            } finally {
                freeMemory.invoke(unsafe, address.getLong(atom));
            }
            String text = new String(scenario.points(), scenario.offset(), scenario.count());
            check(Arrays.equals(text.toCharArray(), scenario.expected()),
                    scenario.name() + ": public String code-point construction");
            if (requireM3 && scenario.expected().length != 0) {
                Field storage = String.class.getDeclaredField("m3");
                storage.setAccessible(true);
                check(storage.get(text) != null, scenario.name() + ": public String admitted M3 owner");
            }
        }
        System.out.println("M3_STRING_CODEPOINT_BOUNDS_PASS checks=" + checks
                + " malformed=" + invalid.length + " valid=" + valid.length
                + " requireM3=" + requireM3);
    }

    private static void bind() throws Exception {
        Class<?> atom = Class.forName("java.lang.M3StringAtom");
        factory = atom.getDeclaredMethod("localCodePoints", int[].class, int.class, int.class,
                int.class, byte.class, long.class, long.class);
        factory.setAccessible(true);
        charAt = atom.getDeclaredMethod("charAt", int.class);
        charAt.setAccessible(true);
        address = atom.getDeclaredField("address");
        address.setAccessible(true);
        length = Class.forName("java.lang.M3StringOwner").getDeclaredField("length");
        length.setAccessible(true);
        Class<?> unsafeType = Class.forName("jdk.internal.misc.Unsafe");
        Field singleton = unsafeType.getDeclaredField("theUnsafe");
        singleton.setAccessible(true);
        unsafe = singleton.get(null);
        freeMemory = unsafeType.getDeclaredMethod("freeMemory", long.class);
        freeMemory.setAccessible(true);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
