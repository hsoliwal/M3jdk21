/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 *
 * @test
 * @summary Recover MIndexString char[] ingress and fingerprint boundaries in actual M3 String
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringCharArrayHistoryTest
 * @run main/othervm --add-opens=java.base/java.lang=ALL-UNNAMED -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringCharArrayHistoryTest
 */

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Random;

public class M3StringCharArrayHistoryTest {
    private static final Field M3;
    private static final Field OWNER;
    private static final Field COORDINATE;
    private static int checks;

    static {
        try {
            M3 = String.class.getDeclaredField("m3");
            M3.setAccessible(true);
            Class<?> type = Class.forName("java.lang.M3String");
            OWNER = type.getDeclaredField("owner");
            OWNER.setAccessible(true);
            COORDINATE = type.getDeclaredField("value");
            COORDINATE.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        mutationAndFreshShadows();
        isolatedSurrogatesAndRanges();
        randomizedUtf16Snapshots();
        exactIdentityNotCandidateFingerprint();
        failures();
        System.out.println("M3_STRING_CHAR_ARRAY_HISTORY_PASS|checks=" + checks);
    }

    private static void mutationAndFreshShadows() throws Exception {
        char[] source = {'A', '\uD83D', '\uDE00', '\u0000', 'Z'};
        String value = new String(source);
        Object backing = requireM3(value);
        source[0] = 'X';
        source[1] = '?';
        require(value.length() == 5, "source length is stable");
        require(value.charAt(0) == 'A' && value.charAt(1) == '\uD83D',
                "caller mutations cannot change admitted owner");
        char[] first = value.toCharArray();
        first[0] = 'Q';
        char[] second = value.toCharArray();
        require(first != second && second[0] == 'A',
                "caller-owned char shadows must be fresh");
        require(backing == M3.get(value), "M3 value remains retained after compatibility export");
    }

    private static void isolatedSurrogatesAndRanges() throws Exception {
        char[] input = {'x', '\uD83D', 'a', '\uDE00', 'z'};
        String middle = new String(input, 1, 3);
        requireM3(middle);
        input[1] = '!'; input[2] = '!'; input[3] = '!';
        require(middle.length() == 3
                        && middle.charAt(0) == '\uD83D'
                        && middle.charAt(1) == 'a'
                        && middle.charAt(2) == '\uDE00',
                "partial unpaired-surrogate range keeps exact UTF-16 units");

        char[] factory = {'!', '\uD83D', '\uDE03', '!'};
        String fromValue = String.valueOf(factory, 1, 2);
        String fromCopy = String.copyValueOf(factory, 1, 2);
        requireM3(fromValue);
        requireM3(fromCopy);
        factory[1] = '?'; factory[2] = '?';
        require(fromValue.equals("\uD83D\uDE03") && fromCopy.equals("\uD83D\uDE03"),
                "both char[] factories snapshot their input ranges");
    }

    private static void randomizedUtf16Snapshots() throws Exception {
        Random random = new Random(0x3A5E2106L);
        char[] alphabet = {'\0', '\n', 'A', '\u00E9', '\u0100', '\uD83D',
                '\uDE03', '\uFFFF', '\uDC00', '\uD800'};
        for (int iteration = 0; iteration < 512; iteration++) {
            char[] source = new char[iteration % 67];
            for (int index = 0; index < source.length; index++) {
                source[index] = alphabet[random.nextInt(alphabet.length)];
            }
            int start = source.length == 0 ? 0 : random.nextInt(source.length + 1);
            int count = random.nextInt(source.length - start + 1);
            char[] snapshot = Arrays.copyOfRange(source, start, start + count);
            String value = new String(source, start, count);
            String viaFactory = String.valueOf(source, start, count);
            for (int index = 0; index < source.length; index++) {
                source[index] ^= 0x0055;
            }
            require(value.length() == snapshot.length, "random range length");
            require(Arrays.equals(snapshot, value.toCharArray()), "random constructor snapshot");
            require(Arrays.equals(snapshot, viaFactory.toCharArray()), "random factory snapshot");
            require(hash(snapshot) == value.hashCode(), "Java UTF-16 hash parity");
            if (count > 0) requireM3(value);
        }
    }

    private static void exactIdentityNotCandidateFingerprint() throws Exception {
        String parent = new String(new char[] {'a', 'b', 'c', 'd'});
        String peer = new String(new char[] {'a', 'b', 'c', 'd'});
        String range = parent.substring(0, 3);
        Object fullM3 = requireM3(parent);
        Object peerM3 = requireM3(peer);
        Object rangeM3 = requireM3(range);
        require(OWNER.get(fullM3) == OWNER.get(peerM3),
                "equal char arrays use the canonical owner while both values remain live");
        require(OWNER.get(fullM3) == OWNER.get(rangeM3),
                "substring retains the original owner");
        require(COORDINATE.getLong(fullM3) != COORDINATE.getLong(rangeM3),
                "range coordinate is part of the exact identity");
        require(parent.equals(peer) && !parent.equals(range),
                "similar or shared owner metadata cannot replace String equality");
    }

    private static void failures() {
        expect(NullPointerException.class, () -> new String((char[]) null));
        expect(NullPointerException.class, () -> new String((char[]) null, 0, 0));
        expect(NullPointerException.class, () -> String.valueOf((char[]) null));
        char[] data = {'a', 'b'};
        expect(IndexOutOfBoundsException.class, () -> new String(data, -1, 1));
        expect(IndexOutOfBoundsException.class, () -> new String(data, 0, -1));
        expect(IndexOutOfBoundsException.class, () -> new String(data, 1, 2));
        expect(IndexOutOfBoundsException.class, () -> String.copyValueOf(data, 1, 2));
    }

    private static int hash(char[] units) {
        int hash = 0;
        for (char unit : units) hash = 31 * hash + unit;
        return hash;
    }

    private static Object requireM3(String value) throws IllegalAccessException {
        Object backing = M3.get(value);
        require(backing != null, "nonempty admitted String must be M3-backed");
        return backing;
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
            throw new AssertionError("expected " + type.getSimpleName());
        } catch (Throwable caught) {
            if (!type.isInstance(caught)) throw new AssertionError("unexpected exception", caught);
        }
    }

    private static void require(boolean ok, String reason) {
        checks++;
        if (!ok) throw new AssertionError(reason);
    }
}
