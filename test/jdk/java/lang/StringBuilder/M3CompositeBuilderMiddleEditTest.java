/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors.
 * GPLv2 with the Classpath exception.
 */
/*
 * @test id=flagOff
 * @summary Composite builder middle edits preserve UTF-16, snapshots, capacity, and piece ownership
 * @modules java.base/java.lang:+open
 * @run main/othervm M3CompositeBuilderMiddleEditTest
 * @run main/othervm -Xint M3CompositeBuilderMiddleEditTest
 * @run main/othervm -XX:-CompactStrings M3CompositeBuilderMiddleEditTest
 */
/*
 * @test id=flagOn
 * @summary Public append admits M3 pieces retained through StringBuilder and StringBuffer edits
 * @modules java.base/java.lang:+open
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3CompositeBuilderMiddleEditTest flagOn
 */

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

/**
 * The text oracle is a flat char[] rewritten by plain copies, independent of
 * production lane coordinates. A flat builder checks exact exceptions only.
 * Real toString snapshots are tested terminally: admission-refused pieces may
 * legitimately take the existing flat compatibility path during toString.
 */
public class M3CompositeBuilderMiddleEditTest {
    private static final Method PIECE;
    private static final Field LANE;
    private static final Field VALUE;
    private static final Field TEXT;
    private static final Field PIECES;
    private static boolean publicAdmission;
    private static long checks;

    static {
        try {
            Class<?> owner = Class.forName("java.lang.AbstractStringBuilder");
            PIECE = owner.getDeclaredMethod("m3AppendPiece", String.class, int.class, int.class);
            PIECE.setAccessible(true);
            LANE = owner.getDeclaredField("m3Lane");
            LANE.setAccessible(true);
            VALUE = owner.getDeclaredField("value");
            VALUE.setAccessible(true);
            Class<?> lane = Class.forName("java.lang.M3StringBuilderLane");
            TEXT = lane.getDeclaredField("text");
            TEXT.setAccessible(true);
            PIECES = lane.getDeclaredField("pieces");
            PIECES.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private static final class FlatText {
        char[] text = new char[0];
        int capacity = 16;
        void splice(int start, int end, char[] replacement) {
            int length = text.length - (end - start) + replacement.length;
            char[] next = new char[length];
            System.arraycopy(text, 0, next, 0, start);
            System.arraycopy(replacement, 0, next, start, replacement.length);
            System.arraycopy(text, end, next, start + replacement.length, text.length - end);
            text = next;
            ensure(length);
        }
        void append(char[] value) { splice(text.length, text.length, value); }
        void ensure(int minimum) {
            if (minimum > capacity) capacity = Math.max(minimum, capacity * 2 + 2);
        }
    }

    private static final class Subject {
        final Object target;
        final FlatText expected = new FlatText();
        final char[] immutableExpected;
        final String immutableSource;

        Subject(boolean buffer, int seed) throws Throwable {
            this(buffer, seed, false);
        }
        Subject(boolean buffer, int seed, boolean ascii) throws Throwable {
            target = buffer ? new StringBuffer() : new StringBuilder();
            flat(new char[] {'/', '/', '\r', '\n', ascii ? 'x' : '\ud83d'});
            immutableExpected = new char[257];
            Random random = new Random(seed);
            for (int i = 0; i < immutableExpected.length; i++) {
                immutableExpected[i] = ascii ? (char) ('a' + random.nextInt(26)) : unit(random);
            }
            if (!ascii) {
                immutableExpected[1] = '\ude00';
                immutableExpected[64] = '\ud800';
                immutableExpected[65] = '\udc00';
            }
            immutableSource = new String(immutableExpected);
            piece(immutableSource, 1, 160);
            flat(new char[] {'x', '\u0000', ascii ? 'y' : '\u20ac'});
            piece(immutableSource, 20, 200);
            if (!publicAdmission) piece(immutableSource, 20, 20);
            flat(new char[] {ascii ? 'z' : '\udc00', ';', '\n'});
            verify("initial");
        }
        void flat(char[] chars) {
            if (target instanceof StringBuilder builder) builder.append(chars);
            else ((StringBuffer) target).append(chars);
            expected.append(chars);
        }
        void flat(char unit) {
            if (target instanceof StringBuilder builder) builder.append(unit);
            else ((StringBuffer) target).append(unit);
            expected.append(new char[] {unit});
        }
        void flat(String value) {
            if (target instanceof StringBuilder builder) builder.append(value);
            else ((StringBuffer) target).append(value);
            expected.append(value.toCharArray());
        }
        void ensure(int minimum) {
            if (target instanceof StringBuilder builder) builder.ensureCapacity(minimum);
            else ((StringBuffer) target).ensureCapacity(minimum);
            expected.ensure(minimum);
        }
        void piece(String value, int start, int end) throws Throwable {
            if (publicAdmission) {
                if (target instanceof StringBuilder builder) builder.append(value, start, end);
                else ((StringBuffer) target).append(value, start, end);
            } else {
                try { PIECE.invoke(target, value, start, end); }
                catch (InvocationTargetException failure) { throw failure.getCause(); }
                // The fixture entry skips StringBuffer's ordinary cache invalidation.
                if (target instanceof StringBuffer buffer) buffer.append("");
            }
            char[] chars = new char[end - start];
            value.getChars(start, end, chars, 0);
            expected.append(chars);
        }
        int length() { return ((CharSequence) target).length(); }
        int capacity() {
            return target instanceof StringBuilder builder ? builder.capacity()
                    : ((StringBuffer) target).capacity();
        }
        void edit(int operation, int start, int end, String value, char unit) throws Throwable {
            Object flatPayload = VALUE.get(target);
            Object returned = invoke(target, operation, start, end, value, unit);
            if (operation != 0) check(returned == target, "fluent edit identity");
            check(VALUE.get(target) == flatPayload, "middle edit replaced mutable flat payload");
            switch (operation) {
                case 0 -> expected.text[start] = unit;
                case 1 -> expected.splice(start, Math.min(end, expected.text.length), new char[0]);
                case 2 -> expected.splice(start, start + 1, new char[0]);
                case 3 -> expected.splice(start, Math.min(end, expected.text.length), value.toCharArray());
                case 4 -> expected.splice(start, start, (value == null ? "null" : value).toCharArray());
                default -> throw new AssertionError(operation);
            }
            verify("operation " + operation);
        }
        void verify(String where) throws IllegalAccessException {
            equal(length(), expected.text.length, where + " length");
            equal(capacity(), expected.capacity, where + " capacity");
            CharSequence source = (CharSequence) target;
            String unitMessage = where + " UTF-16 unit";
            String atMessage = where + " codePointAt";
            String beforeMessage = where + " codePointBefore";
            for (int i = 0; i < expected.text.length; i++) {
                equal(source.charAt(i), expected.text[i], unitMessage);
                if (i % 13 == 0) {
                    int codePoint = target instanceof StringBuilder builder ? builder.codePointAt(i)
                            : ((StringBuffer) target).codePointAt(i);
                    equal(codePoint, Character.codePointAt(expected.text, i), atMessage);
                    int before = target instanceof StringBuilder builder ? builder.codePointBefore(i + 1)
                            : ((StringBuffer) target).codePointBefore(i + 1);
                    equal(before, Character.codePointBefore(expected.text, i + 1), beforeMessage);
                }
            }
            checkText(immutableSource, immutableExpected, "unchanged source piece");
            if (length() != 0) check(LANE.get(target) != null, where + " flattened composite lane");
        }
        void finish() {
            String snapshot = target.toString();
            char[] frozen = expected.text.clone();
            checkText(snapshot, frozen, "terminal snapshot");
            checkText(target.toString(), frozen, "cached terminal snapshot");
            if (length() > 0) {
                char changed = (char) (frozen[0] ^ 0xffff);
                invoke(target, 0, 0, 0, null, changed);
                expected.text[0] = changed;
                checkText(target.toString(), expected.text, "cache invalidated after edit");
                checkText(snapshot, frozen, "snapshot remains immutable after edit");
            }
        }
    }

    private static Object invoke(Object target, int operation, int start, int end, String value, char unit) {
        if (target instanceof StringBuilder builder) {
            return switch (operation) {
                case 0 -> { builder.setCharAt(start, unit); yield builder; }
                case 1 -> builder.delete(start, end);
                case 2 -> builder.deleteCharAt(start);
                case 3 -> builder.replace(start, end, value);
                case 4 -> builder.insert(start, value);
                default -> throw new AssertionError(operation);
            };
        }
        StringBuffer buffer = (StringBuffer) target;
        return switch (operation) {
            case 0 -> { buffer.setCharAt(start, unit); yield buffer; }
            case 1 -> buffer.delete(start, end);
            case 2 -> buffer.deleteCharAt(start);
            case 3 -> buffer.replace(start, end, value);
            case 4 -> buffer.insert(start, value);
            default -> throw new AssertionError(operation);
        };
    }
    private static String failure(Object target, int operation, int start, int end) {
        try { invoke(target, operation, start, end, null, '\u20ac'); return "success"; }
        catch (RuntimeException failure) {
            return failure.getClass().getName() + ":" + failure.getMessage();
        }
    }
    private static void exceptions(boolean buffer) throws Throwable {
        Subject subject = new Subject(buffer, 4481);
        StringBuilder flat = new StringBuilder(subject.capacity()).append(subject.expected.text);
        int length = subject.length();
        int[][] ranges = {{-1, 0}, {length + 1, length + 10}, {4, 3},
                {Integer.MIN_VALUE, Integer.MAX_VALUE}, {0, -1}};
        for (int[] range : ranges) for (int operation : new int[] {1, 3}) {
            String actual = failure(subject.target, operation, range[0], range[1]);
            String expected = failure(flat, operation, range[0], range[1]);
            check(actual.equals(expected), "bounds/null precedence: " + actual + " != " + expected);
            subject.verify("invalid range");
        }
        check(failure(subject.target, 3, length, Integer.MAX_VALUE)
                .equals(failure(flat, 3, length, Integer.MAX_VALUE)), "valid range then null replacement");
        for (int index : new int[] {-1, Integer.MIN_VALUE, Integer.MAX_VALUE, length + 1}) {
            for (int operation : new int[] {0, 2, 4}) {
                check(failure(subject.target, operation, index, 0)
                        .equals(failure(flat, operation, index, 0)), "invalid offset exception");
                subject.verify("invalid offset");
            }
        }
        subject.finish();
    }
    private static void boundaries(boolean buffer) throws Throwable {
        Subject subject = new Subject(buffer, 912);
        for (int position : new int[] {0, 4, 5, 63, 64, 163, 164, 166, 167, 200, 349}) {
            if (position < subject.length()) {
                subject.edit(0, position, 0, null, '\ud800');
                subject.edit(0, position, 0, null, '\udc00');
            }
        }
        subject.edit(1, 63, 171, null, '\0');
        subject.edit(3, 3, 7, "\u0000\r\n\udc00\ud800\u20ac", '\0');
        subject.edit(4, 0, 0, null, '\0');
        subject.edit(4, subject.length(), 0, "end", '\0');
        subject.edit(3, 17, 17, "", '\0');
        subject.edit(1, 17, 17, null, '\0');
        subject.edit(2, 6, 0, null, '\0');
        subject.edit(1, subject.length(), Integer.MAX_VALUE, null, '\0');
        subject.edit(3, subject.length(), Integer.MAX_VALUE, "tail", '\0');
        subject.edit(1, 2, Integer.MAX_VALUE, null, '\0');
        subject.edit(3, 0, 2, "", '\0');
        subject.finish();
    }
    private static void random(boolean buffer, int seed) throws Throwable {
        Subject subject = new Subject(buffer, seed);
        Random random = new Random(seed ^ 0x78e33d2a);
        for (int step = 0; step < 200; step++) {
            int length = subject.length();
            int operation = length < 8 ? 4 : random.nextInt(5);
            int start = random.nextInt(length + 1);
            int end = start + Math.min(random.nextInt(12), length - start);
            char[] chars = new char[random.nextInt(21)];
            for (int i = 0; i < chars.length; i++) chars[i] = unit(random);
            if (operation == 0 || operation == 2) start = random.nextInt(length);
            subject.edit(operation, start, end, new String(chars), unit(random));
        }
        subject.finish();
    }
    private static boolean retains(Object target, String source) throws IllegalAccessException {
        Object lane = LANE.get(target);
        check(lane != null, "missing lane during ownership check");
        String[] text = (String[]) TEXT.get(lane);
        int pieces = PIECES.getInt(lane);
        for (int i = 0; i < pieces; i++) if (text[i] == source) return true;
        return false;
    }
    private static void latinTransitions(boolean buffer) throws Throwable {
        Subject subject = new Subject(buffer, 402, true);
        subject.edit(0, 6, 0, null, '\u20ac');
        subject.flat('!');
        subject.verify("Latin1 set then append char");
        subject.edit(2, 6, 0, null, '\0');
        subject.edit(4, 13, 0, "\ud83d\ude00", '\0');
        subject.flat(new char[] {'a', 'b', 'c'});
        subject.verify("Latin1 insert pair then append array");
        subject.edit(1, 13, 15, null, '\0');
        subject.edit(0, 0, 0, null, '\ud800');
        subject.flat("tail");
        subject.verify("unpaired set then append short String");
        subject.edit(2, 0, 0, null, '\0');
        subject.flat(new char[] {'\ud800', 'x', '\udc00'});
        subject.verify("UTF16 mutable tail after Latin1 pieces");
        subject.edit(3, subject.length() - 3, subject.length(), "ascii", '\0');
        subject.ensure(-1);
        subject.ensure(0);
        subject.ensure(subject.capacity() * 2 + 3);
        subject.verify("sticky UTF16 capacity after Unicode removed");
        char[] tail = new char[513];
        for (int i = 0; i < tail.length; i++) tail[i] = (char) ('A' + i % 26);
        subject.flat(tail);
        subject.verify("flat payload grows within reserved logical capacity");
        subject.edit(0, subject.length() - 1, 0, null, '\udc00');
        subject.flat("end");
        subject.verify("edit former flat tail then append String");
        subject.finish();
    }
    private static void ownership(boolean buffer) throws Throwable {
        Subject subject = new Subject(buffer, 122);
        check(retains(subject.target, subject.immutableSource), "initial immutable source identity");
        subject.edit(0, 0, 0, null, 'x');
        check(retains(subject.target, subject.immutableSource), "flat edit retains unaffected source");
        subject.edit(3, 8, 11, "\u20ac", '\0');
        check(retains(subject.target, subject.immutableSource), "partial edit retains substantial spans");
        subject.edit(1, 0, 5, null, '\0');
        check(retains(subject.target, subject.immutableSource), "prefix deletion retains source spans");
        subject.finish();

        char[] payload = new char[16384];
        for (int i = 0; i < payload.length; i++) payload[i] = (char) ('a' + i % 26);
        payload[payload.length / 2] = '\u20ac';
        String huge = new String(payload);
        Object target = buffer ? new StringBuffer() : new StringBuilder();
        if (publicAdmission) {
            if (target instanceof StringBuilder builder) builder.append(huge);
            else ((StringBuffer) target).append(huge);
        } else {
            try { PIECE.invoke(target, huge, 0, huge.length()); }
            catch (InvocationTargetException failure) { throw failure.getCause(); }
            if (target instanceof StringBuffer value) value.append("");
        }
        check(retains(target, huge), "large source admitted before tiny-remnant edit");
        Object flatPayload = VALUE.get(target);
        int oldCapacity = target instanceof StringBuilder builder ? builder.capacity()
                : ((StringBuffer) target).capacity();
        invoke(target, 1, 1, huge.length(), null, '\0');
        check(VALUE.get(target) == flatPayload, "tiny-remnant edit replaced flat payload");
        check(!retains(target, huge), "tiny surviving range pins original large String");
        CharSequence remaining = (CharSequence) target;
        equal(remaining.length(), 1, "tiny remnant length");
        equal(remaining.charAt(0), payload[0], "tiny remnant UTF-16 unit");
        equal(target instanceof StringBuilder builder ? builder.capacity()
                : ((StringBuffer) target).capacity(), oldCapacity, "tiny remnant capacity");
        checkText(huge, payload, "large source immutable after edit");
        checkText(target.toString(), new char[] {payload[0]}, "tiny terminal snapshot");
    }
    private static char unit(Random random) {
        return switch (random.nextInt(8)) {
            case 0 -> '\u0000'; case 1 -> '\r'; case 2 -> '\n'; case 3 -> '\u20ac';
            case 4 -> (char) (0xd800 + random.nextInt(0x800));
            default -> (char) random.nextInt(128);
        };
    }
    private static void checkText(CharSequence actual, char[] expected, String where) {
        equal(actual.length(), expected.length, where + " length");
        String unitMessage = where + " unit";
        for (int i = 0; i < expected.length; i++) equal(actual.charAt(i), expected[i], unitMessage);
    }
    private static void equal(long actual, long expected, String where) {
        checks++;
        if (actual != expected) throw new AssertionError(where + ": " + actual + " != " + expected);
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Throwable {
        publicAdmission = Arrays.asList(args).contains("flagOn");
        for (boolean buffer : new boolean[] {false, true}) {
            exceptions(buffer);
            boundaries(buffer);
            ownership(buffer);
            latinTransitions(buffer);
            for (int seed : new int[] {1, 7, 39, 611, 883, -19, 104729}) random(buffer, seed);
        }
        System.out.println("M3CompositeBuilderMiddleEditTest PASS checks=" + checks
                + " publicAdmission=" + publicAdmission);
    }
}
