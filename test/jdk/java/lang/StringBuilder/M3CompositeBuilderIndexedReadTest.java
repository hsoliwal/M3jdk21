// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
/*
 * @test id=flagOff
 * @summary Composite builder indexed reads preserve pieces, flat backing and UTF-16 semantics
 * @modules java.base/java.lang:+open
 * @run main/othervm M3CompositeBuilderIndexedReadTest
 * @run main/othervm -Xint M3CompositeBuilderIndexedReadTest
 * @run main/othervm -Xbatch -XX:TieredStopAtLevel=1 M3CompositeBuilderIndexedReadTest
 * @run main/othervm -Xbatch -XX:-TieredCompilation M3CompositeBuilderIndexedReadTest
 * @run main/othervm -XX:-CompactStrings M3CompositeBuilderIndexedReadTest
 * @run main/othervm -XX:+UseSerialGC M3CompositeBuilderIndexedReadTest
 */
/*
 * @test id=flagOn
 * @summary Indexed reads retain public-append M3 pieces on a matched M3 HotSpot image
 * @modules java.base/java.lang:+open
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3CompositeBuilderIndexedReadTest flagOn
 */

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * The oracle receives only char[] payload copies, so its indexed reads remain flat.
 * Piece insertion uses the existing package entry in flag-off executions. No test
 * reimplements the production piece search, and no expected value uses lane metadata.
 * Field snapshots inspect all occurrence arrays without naming the successor index.
 */
public class M3CompositeBuilderIndexedReadTest {
    private static final Class<?> ASB;
    private static final Method PIECE;
    private static final Method STRING_M3;
    private static final Field LANE;
    private static final Field VALUE;
    private static final Field COUNT;
    private static final Field CODER;
    private static final Field MAYBE_LATIN1;
    private static long checks;
    private static long readChecks;
    private static volatile long sink;

    static {
        try {
            ASB = Class.forName("java.lang.AbstractStringBuilder");
            PIECE = ASB.getDeclaredMethod("m3AppendPiece", String.class, int.class, int.class);
            PIECE.setAccessible(true);
            STRING_M3 = String.class.getDeclaredMethod("m3");
            STRING_M3.setAccessible(true);
            LANE = field(ASB, "m3Lane");
            VALUE = field(ASB, "value");
            COUNT = field(ASB, "count");
            CODER = field(ASB, "coder");
            MAYBE_LATIN1 = field(ASB, "maybeLatin1");
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        Field result = type.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }

    private static final class Subject {
        final Object raw;
        final StringBuilder oracle = new StringBuilder();

        Subject(boolean buffer) {
            raw = buffer ? new StringBuffer() : new StringBuilder();
        }

        void flat(char... chars) {
            if (raw instanceof StringBuilder builder) builder.append(chars);
            else ((StringBuffer) raw).append(chars);
            oracle.append(chars);
        }

        String piece(char[] chars, int start, int end) throws Throwable {
            String source = new String(chars);
            try {
                PIECE.invoke(raw, source, start, end);
            } catch (InvocationTargetException failure) {
                throw failure.getCause();
            }
            // The package entry bypasses this existing StringBuffer cache invalidation.
            if (raw instanceof StringBuffer buffer) buffer.append("");
            oracle.append(chars, start, end - start);
            return source;
        }

        int read(int operation, int index) {
            if (raw instanceof StringBuilder builder) {
                return switch (operation) {
                    case 0 -> builder.charAt(index);
                    case 1 -> builder.codePointAt(index);
                    case 2 -> builder.codePointBefore(index);
                    default -> throw new AssertionError(operation);
                };
            }
            StringBuffer buffer = (StringBuffer) raw;
            return switch (operation) {
                case 0 -> buffer.charAt(index);
                case 1 -> buffer.codePointAt(index);
                case 2 -> buffer.codePointBefore(index);
                default -> throw new AssertionError(operation);
            };
        }

        int length() {
            return ((CharSequence) raw).length();
        }

        int capacity() {
            return raw instanceof StringBuilder builder ? builder.capacity()
                    : ((StringBuffer) raw).capacity();
        }

        void ensureCapacity(int capacity) {
            if (raw instanceof StringBuilder builder) builder.ensureCapacity(capacity);
            else ((StringBuffer) raw).ensureCapacity(capacity);
            oracle.ensureCapacity(capacity);
        }

        void edit() {
            if (raw instanceof StringBuilder builder) {
                builder.setCharAt(1, '\u20ac');
                builder.insert(2, new char[] {'\ud83d', '\ude00'});
                builder.delete(0, 1);
            } else {
                StringBuffer buffer = (StringBuffer) raw;
                buffer.setCharAt(1, '\u20ac');
                buffer.insert(2, new char[] {'\ud83d', '\ude00'});
                buffer.delete(0, 1);
            }
            oracle.setCharAt(1, '\u20ac');
            oracle.insert(2, new char[] {'\ud83d', '\ude00'});
            oracle.delete(0, 1);
        }
    }

    /** Retain identities and primitive descriptor values without interpreting coordinates. */
    private static final class Snapshot {
        final Object lane;
        final Object value;
        final byte[] valueBytes;
        final int count;
        final byte coder;
        final boolean maybeLatin1;
        final int length;
        final int capacity;
        final List<Field> fields = new ArrayList<>();
        final List<Object> references = new ArrayList<>();
        final List<Object> copies = new ArrayList<>();

        Snapshot(Subject subject) throws IllegalAccessException {
            lane = LANE.get(subject.raw);
            value = VALUE.get(subject.raw);
            valueBytes = ((byte[]) value).clone();
            count = COUNT.getInt(subject.raw);
            coder = CODER.getByte(subject.raw);
            maybeLatin1 = MAYBE_LATIN1.getBoolean(subject.raw);
            length = subject.length();
            capacity = subject.capacity();
            if (lane != null) {
                for (Field field : lane.getClass().getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) continue;
                    field.setAccessible(true);
                    Object reference = field.get(lane);
                    Object copy = null;
                    if (reference != null && field.getType().isArray()) {
                        int size = Array.getLength(reference);
                        copy = Array.newInstance(field.getType().getComponentType(), size);
                        System.arraycopy(reference, 0, copy, 0, size);
                    }
                    fields.add(field);
                    references.add(reference);
                    copies.add(copy);
                }
            }
        }

        void same(Subject subject, String where) throws IllegalAccessException {
            check(LANE.get(subject.raw) == lane, where + ": indexed read detached lane");
            check(VALUE.get(subject.raw) == value, where + ": replaced flat backing");
            check(Arrays.equals((byte[]) value, valueBytes), where + ": wrote flat payload");
            check(COUNT.getInt(subject.raw) == count, where + ": changed flat count");
            check(CODER.getByte(subject.raw) == coder, where + ": changed flat coder");
            check(MAYBE_LATIN1.getBoolean(subject.raw) == maybeLatin1,
                    where + ": changed coder hint");
            check(subject.length() == length, where + ": changed logical length");
            check(subject.capacity() == capacity, where + ": changed capacity");
            for (int index = 0; index < fields.size(); index++) {
                Field field = fields.get(index);
                Object before = references.get(index);
                Object after = field.get(lane);
                check(field.getType().isPrimitive() ? before.equals(after) : before == after,
                        where + ": changed lane field " + field.getName());
                Object copy = copies.get(index);
                if (copy == null) continue;
                for (int at = 0; at < Array.getLength(copy); at++) {
                    Object oldEntry = Array.get(copy, at);
                    Object newEntry = Array.get(after, at);
                    check(field.getType().getComponentType().isPrimitive()
                                    ? oldEntry.equals(newEntry) : oldEntry == newEntry,
                            where + ": changed occurrence array " + field.getName());
                }
            }
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static int flatRead(StringBuilder oracle, int operation, int index) {
        return switch (operation) {
            case 0 -> oracle.charAt(index);
            case 1 -> oracle.codePointAt(index);
            case 2 -> oracle.codePointBefore(index);
            default -> throw new AssertionError(operation);
        };
    }

    private static void read(Subject subject, char[] expected, int operation, int index) {
        int length = expected.length;
        boolean valid = operation == 2 ? index > 0 && index <= length : index >= 0 && index < length;
        if (valid) {
            int wanted = switch (operation) {
                case 0 -> expected[index];
                case 1 -> Character.codePointAt(expected, index, length);
                case 2 -> Character.codePointBefore(expected, index, 0);
                default -> throw new AssertionError(operation);
            };
            check(subject.read(operation, index) == wanted,
                    "UTF-16 result operation=" + operation + " index=" + index);
        } else {
            IndexOutOfBoundsException wanted;
            try {
                flatRead(subject.oracle, operation, index);
                throw new AssertionError("invalid oracle index accepted");
            } catch (IndexOutOfBoundsException failure) {
                wanted = failure;
            }
            try {
                subject.read(operation, index);
                throw new AssertionError("invalid subject index accepted");
            } catch (IndexOutOfBoundsException failure) {
                check(failure.getClass() == wanted.getClass(), "exception class");
                check(java.util.Objects.equals(failure.getMessage(), wanted.getMessage()),
                        "exception message operation=" + operation + " index=" + index);
            }
        }
        readChecks++;
    }

    private static void verify(Subject subject, String where, boolean exhaustive) throws Throwable {
        char[] expected = new char[subject.oracle.length()];
        subject.oracle.getChars(0, expected.length, expected, 0);
        Snapshot snapshot = new Snapshot(subject);
        check(subject.length() == expected.length, where + ": oracle length");
        // The baseline is expected to fail at this first successful indexed read.
        if (expected.length > 0) {
            read(subject, expected, 0, 0);
            snapshot.same(subject, where + "/first");
        }
        int[] extremes = {Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -2, -1,
                0, 1, expected.length - 1, expected.length, expected.length + 1,
                Integer.MAX_VALUE - 1, Integer.MAX_VALUE};
        for (int operation = 0; operation < 3; operation++) {
            for (int index : extremes) {
                read(subject, expected, operation, index);
                snapshot.same(subject, where + "/bounds");
            }
            if (exhaustive) {
                for (int index = 0; index <= expected.length; index++) {
                    read(subject, expected, operation, index);
                }
            } else if (expected.length > 0) {
                Random random = new Random(0x4e42494e4458L + expected.length + operation);
                for (int sample = 0; sample < 19; sample++) {
                    read(subject, expected, operation, random.nextInt(expected.length + 1));
                }
            }
        }
        snapshot.same(subject, where + "/all");
    }

    private static void seams(boolean buffer) throws Throwable {
        char[][] pairs = {{'\ud83d', '\ude00'}, {'\ud83d', 'X'}, {'X', '\ude00'},
                {'\ude00', '\ud83d'}, {'\u00e9', '\u20ac'}};
        for (int left = 0; left < 2; left++) {
            for (int right = 0; right < 2; right++) {
                for (char[] pair : pairs) {
                    Subject subject = new Subject(buffer);
                    subject.piece(new char[] {'x'}, 0, 0);
                    subject.flat('p');
                    if (left == 0) subject.flat(pair[0]);
                    else subject.piece(new char[] {'?', pair[0], '!'}, 1, 2);
                    subject.piece(new char[] {'x'}, 1, 1);
                    if (right == 0) subject.flat(pair[1]);
                    else subject.piece(new char[] {'?', pair[1], '!'}, 1, 2);
                    subject.piece(new char[] {'x'}, 1, 1);
                    subject.flat('q');
                    subject.piece(new char[] {'x'}, 0, 0);
                    verify(subject, "seam " + left + right, true);
                }
            }
        }
        Subject sliced = new Subject(buffer);
        sliced.piece(new char[] {'\u20ac', 'a', '\u00e9', '\u20ac'}, 1, 3);
        sliced.piece(new char[] {'x', '\ud83d', '\ude00', 'z'}, 1, 3);
        sliced.flat('\ud83d');
        sliced.piece(new char[] {'x', '\ude00', '\ud800', 'q'}, 1, 3);
        sliced.flat('z', '\udc00');
        verify(sliced, "slices and mixed coders", true);
    }

    private static void emptyAndFlat(boolean buffer) throws Throwable {
        Subject empty = new Subject(buffer);
        verify(empty, "flat empty", true);
        for (int index = 0; index < 17; index++) {
            empty.piece(new char[] {'\u20ac', 'x'}, index & 1, index & 1);
        }
        verify(empty, "all-empty lane", true);
        empty.flat('a', '\ud83d', '\ude00');
        verify(empty, "flat after empty ends", true);
        empty.piece(new char[] {'b'}, 0, 1);
        empty.piece(new char[] {'c'}, 1, 1);
        verify(empty, "piece after empty ends", true);
        Subject flat = new Subject(buffer);
        flat.flat('a', '\u00e9', '\u20ac', '\ud83d', '\ude00', '\ud800', 'z');
        verify(flat, "flat only", true);
    }

    private static Subject growth(boolean buffer) throws Throwable {
        Subject subject = new Subject(buffer);
        for (int piece = 0; piece < 257; piece++) {
            if (piece % 3 == 0) subject.flat((char) ('a' + piece % 26), '\ud83d');
            char[] source = {'!', '\ude00', (char) piece, '\ud800', '?'};
            int count = piece % 5;
            subject.piece(source, 1, 1 + count);
            if ((piece & (piece + 1)) == 0) verify(subject, "growth " + piece, false);
        }
        verify(subject, "growth final", true);
        subject.ensureCapacity(subject.capacity() + 4096);
        verify(subject, "capacity after reads", false);
        return subject;
    }

    private static void randomScripts(boolean buffer) throws Throwable {
        Random random = new Random(0x4e45544245414e53L);
        for (int script = 0; script < 96; script++) {
            Subject subject = new Subject(buffer);
            int operations = 2 + random.nextInt(39);
            for (int operation = 0; operation < operations; operation++) {
                char[] source = new char[2 + random.nextInt(13)];
                for (int at = 0; at < source.length; at++) {
                    source[at] = switch (random.nextInt(5)) {
                        case 0 -> (char) random.nextInt(256);
                        case 1 -> (char) (0xd800 + random.nextInt(0x400));
                        case 2 -> (char) (0xdc00 + random.nextInt(0x400));
                        default -> (char) random.nextInt(65536);
                    };
                }
                if (random.nextInt(4) == 0) subject.flat(source);
                else {
                    int start = random.nextInt(source.length + 1);
                    int end = start + random.nextInt(source.length - start + 1);
                    subject.piece(source, start, end);
                }
                if ((operation & 7) == 0) verify(subject, "random prefix", false);
            }
            verify(subject, "random final", true);
        }
    }

    private static void hotReads(Subject subject) throws Throwable {
        char[] expected = new char[subject.oracle.length()];
        subject.oracle.getChars(0, expected.length, expected, 0);
        Snapshot snapshot = new Snapshot(subject);
        long sum = 0;
        int state = 0x1b873593;
        for (int iteration = 0; iteration < 80_000; iteration++) {
            state ^= state << 13;
            state ^= state >>> 17;
            state ^= state << 5;
            int index = (state & Integer.MAX_VALUE) % expected.length;
            int unit = subject.read(0, index);
            int forward = subject.read(1, index);
            int backward = subject.read(2, index + 1);
            check(unit == expected[index], "hot unit");
            check(forward == Character.codePointAt(expected, index, expected.length), "hot forward");
            check(backward == Character.codePointBefore(expected, index + 1, 0), "hot backward");
            sum += unit + forward + backward;
            readChecks += 3;
        }
        sink = sum;
        snapshot.same(subject, "hot readers");
        subject.flat('x', '\ud83d');
        subject.piece(new char[] {'\ude00', 'y'}, 0, 2);
        verify(subject, "continued append", false);
        subject.edit();
        verify(subject, "edit after indexed reads", true);
        subject.piece(new char[] {'p', '\u20ac', 'q'}, 1, 3);
        verify(subject, "new lane after edit", true);
        check(subject.raw.toString().equals(subject.oracle.toString()), "final text after edit");
    }

    private record Retained(Subject subject, List<WeakReference<String>> owners) { }

    private static Retained retainedFixture(boolean buffer) throws Throwable {
        Subject subject = new Subject(buffer);
        List<WeakReference<String>> owners = new ArrayList<>();
        for (int piece = 0; piece < 48; piece++) {
            char[] source = new char[160];
            for (int at = 0; at < source.length; at++) source[at] = (char) (piece * 127 + at);
            source[1] = '\ude00';
            source[158] = '\ud83d';
            subject.flat('\ud83d');
            owners.add(new WeakReference<>(subject.piece(source, 1, 159)));
            subject.flat('\ude00');
        }
        return new Retained(subject, owners);
    }

    private static void retainedAfterGc(boolean buffer) throws Throwable {
        Retained retained = retainedFixture(buffer);
        // No Snapshot or cloned String[] is alive: only the builder strongly owns these Strings.
        for (int round = 0; round < 3; round++) {
            byte[][] pressure = new byte[6][];
            for (int index = 0; index < pressure.length; index++) pressure[index] = new byte[1 << 20];
            sink = pressure[round].length;
            System.gc();
        }
        for (WeakReference<String> owner : retained.owners) {
            check(owner.get() != null, "piece owner lost while builder remains live");
        }
        verify(retained.subject, "owners after GC", true);
        Reference.reachabilityFence(retained.subject);
    }

    private static void publicLift(boolean buffer) throws Throwable {
        Subject subject = new Subject(buffer);
        char[] sourceChars = new char[1024];
        for (int index = 0; index < sourceChars.length; index++) sourceChars[index] = (char) ('a' + index % 26);
        String source = new String(sourceChars);
        check(STRING_M3.invoke(source) != null, "flag-on source must have admitted M3 storage");
        if (subject.raw instanceof StringBuilder builder) builder.append(source);
        else ((StringBuffer) subject.raw).append(source);
        subject.oracle.append(sourceChars);
        check(LANE.get(subject.raw) != null, "flag-on public append must lift");
        verify(subject, "public admitted piece", true);
    }

    public static void main(String[] args) throws Throwable {
        boolean flagOn = args.length == 1 && args[0].equals("flagOn");
        for (String name : List.of("charAt", "codePointAt", "codePointBefore")) {
            check(Modifier.isSynchronized(StringBuffer.class.getDeclaredMethod(name, int.class).getModifiers()),
                    "StringBuffer monitor contract: " + name);
        }
        for (boolean buffer : new boolean[] {false, true}) {
            // Start with a nonempty lane so the unchanged baseline has an unambiguous failure.
            Subject first = new Subject(buffer);
            first.flat('a');
            first.piece(new char[] {'b', '\u20ac', '\ud83d', '\ude00'}, 0, 4);
            verify(first, "initial nonempty lane", true);
            seams(buffer);
            emptyAndFlat(buffer);
            Subject growing = growth(buffer);
            randomScripts(buffer);
            hotReads(growing);
            retainedAfterGc(buffer);
            if (flagOn) publicLift(buffer);
        }
        System.out.println("M3CompositeBuilderIndexedReadTest mode=" + (flagOn ? "flagOn" : "flagOff")
                + " scripts=192 reads=" + readChecks + " checks=" + checks + " sink=" + sink);
    }
}
