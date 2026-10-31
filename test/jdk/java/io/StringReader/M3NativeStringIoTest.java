/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 */

/*
 * @test
 * @summary M3 String I/O preserves UTF-16, owner lifetime, views and callback contracts
 * @modules java.base/jdk.internal.access java.base/java.lang:open
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:-UseM3StringStorage M3NativeStringIoTest
 * @run main/othervm -Xmx128m -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3NativeStringIoTest requireCursor
 * @run main/othervm -Xmx128m -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3NativeStringIoTest requireCursor
 * @run main/othervm -Xint -Xmx128m -XX:+UseSerialGC -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3NativeStringIoTest requireCursor
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntUnaryOperator;

/**
 * Newly reconstructed oracle after loss of an unpublished workspace. Its source
 * identity and check counts are independent of the earlier unpublished test.
 */
public class M3NativeStringIoTest {
    private static final char[] ALPHABET = {
        0, 'a', '\u00ff', '\u0100', '\uffff', '\ud800', '\udc00', '\ud83d', '\ude42'
    };
    private static long checks;
    private static int cursorObservations;
    private static volatile Object pressure;

    public static void main(String[] args) throws Exception {
        boolean contractsOnly = Arrays.asList(args).contains("contracts");
        boolean requireCursor = Arrays.asList(args).contains("requireCursor");
        publicSurface();
        for (int length : new int[] {0, 1, 63, 64, 257, 8191, 8192, 8193, 24593}) {
            char[] expected = units(length);
            String text = compose(expected);
            readers(text, expected);
            buffers(text, expected);
            if (!contractsOnly) cursor(text, expected);
            if (length > 40) {
                char[] clipped = Arrays.copyOfRange(expected, 17, length - 19);
                String slice = text.substring(17, length - 19);
                readers(slice, clipped);
                buffers(slice, clipped);
                if (!contractsOnly) cursor(slice, clipped);
            }
        }
        validation();
        mutableSequence();
        writerCallbacks();
        writerSnapshots();
        transferCapacity();
        closeDuringBlockedTransfer();
        encoderCallbacks();
        if (!contractsOnly) cursorLifetime();
        if (requireCursor) check(cursorObservations > 0, "M3-enabled action exercised a cursor");
        System.out.println("M3NativeStringIoTest reconstructed: " + checks
                + " checks; cursors=" + cursorObservations + "; contractsOnly=" + contractsOnly);
    }

    /** Freeze declarations of exported public reader types; internal helpers remain replaceable. */
    private static void publicSurface() throws Exception {
        check(StringReader.class.getMethod("transferTo", Writer.class).getDeclaringClass() == Reader.class,
                "transferTo retains its Reader declaration owner");
        check(surface(Reader.class).equals(List.of(
                "C:protected:[]:[]",
                "C:protected:[java.lang.Object]:[]",
                "F:protected:lock:java.lang.Object",
                "M:public abstract:close:void:[]:[java.io.IOException]",
                "M:public abstract:read:int:[char[], int, int]:[java.io.IOException]",
                "M:public static:nullReader:java.io.Reader:[]:[]",
                "M:public:mark:void:[int]:[java.io.IOException]",
                "M:public:markSupported:boolean:[]:[]",
                "M:public:read:int:[]:[java.io.IOException]",
                "M:public:read:int:[char[]]:[java.io.IOException]",
                "M:public:read:int:[java.nio.CharBuffer]:[java.io.IOException]",
                "M:public:ready:boolean:[]:[java.io.IOException]",
                "M:public:reset:void:[]:[java.io.IOException]",
                "M:public:skip:long:[long]:[java.io.IOException]",
                "M:public:transferTo:long:[java.io.Writer]:[java.io.IOException]")),
                "Reader declared public/protected surface");
        check(surface(StringReader.class).equals(List.of(
                "C:public:[java.lang.String]:[]",
                "M:public:close:void:[]:[]",
                "M:public:mark:void:[int]:[java.io.IOException]",
                "M:public:markSupported:boolean:[]:[]",
                "M:public:read:int:[]:[java.io.IOException]",
                "M:public:read:int:[char[], int, int]:[java.io.IOException]",
                "M:public:ready:boolean:[]:[java.io.IOException]",
                "M:public:reset:void:[]:[java.io.IOException]",
                "M:public:skip:long:[long]:[java.io.IOException]")),
                "StringReader declared public/protected surface");
    }

    private static List<String> surface(Class<?> type) {
        List<String> result = new ArrayList<>();
        for (var constructor : type.getDeclaredConstructors()) {
            if (exposed(constructor.getModifiers())) {
                result.add("C:" + java.lang.reflect.Modifier.toString(constructor.getModifiers())
                        + ":" + types(constructor.getParameterTypes()) + ":" + types(constructor.getExceptionTypes()));
            }
        }
        for (Field field : type.getDeclaredFields()) {
            if (exposed(field.getModifiers())) {
                result.add("F:" + java.lang.reflect.Modifier.toString(field.getModifiers())
                        + ":" + field.getName() + ":" + field.getType().getTypeName());
            }
        }
        for (Method method : type.getDeclaredMethods()) {
            if (exposed(method.getModifiers())) {
                result.add("M:" + java.lang.reflect.Modifier.toString(method.getModifiers())
                        + ":" + method.getName() + ":" + method.getReturnType().getTypeName()
                        + ":" + types(method.getParameterTypes()) + ":" + types(method.getExceptionTypes()));
            }
        }
        result.sort(String::compareTo);
        return result;
    }

    private static boolean exposed(int modifiers) {
        return java.lang.reflect.Modifier.isPublic(modifiers) || java.lang.reflect.Modifier.isProtected(modifiers);
    }

    private static String types(Class<?>[] types) {
        return Arrays.stream(types).map(Class::getTypeName).toList().toString();
    }

    private static char[] units(int length) {
        char[] result = new char[length];
        for (int i = 0; i < length; i++) result[i] = ALPHABET[i % ALPHABET.length];
        return result;
    }

    private static String compose(char[] expected) {
        String result = "";
        for (int at = 0; at < expected.length; at += 97) {
            result = result.concat(new String(expected, at, Math.min(97, expected.length - at)));
        }
        return result;
    }

    private static void readers(String text, char[] expected) throws Exception {
        try (StringReader reader = new StringReader(text)) {
            reader.mark(0);
            for (char c : expected) check(reader.read() == c, "scalar reader");
            check(reader.read() == -1, "reader EOF");
            check(reader.skip(-1) == 0, "negative skip at EOF");
            check(reader.read(new char[0], 0, 0) == 0, "zero read at EOF");
            reader.reset();
            char[] buffer = new char[23];
            int at = 0;
            for (int count; (count = reader.read(buffer, 2, 17)) != -1;) {
                for (int i = 0; i < count; i++) check(buffer[i + 2] == expected[at++], "bulk reader");
            }
            check(at == expected.length, "bulk reader length");
            reader.reset();
            StringWriter writer = new StringWriter();
            check(reader.transferTo(writer) == expected.length, "transfer count");
            check(Arrays.equals(writer.toString().toCharArray(), expected), "transfer content");
            check(reader.transferTo(writer) == 0, "transfer at EOF");
            reader.reset();
            if (expected.length > 4) {
                check(reader.skip(3) == 3, "positive skip");
                check(reader.skip(-2) == -2, "negative skip");
                check(reader.read() == expected[1], "read after skip");
                reader.mark(0);
                reader.read();
                reader.reset();
                check(reader.read() == expected[2], "mark reset");
            }
        }
    }

    private static void buffers(String text, char[] expected) {
        CharBuffer root = CharBuffer.wrap(text);
        for (char c : expected) check(root.get() == c, "relative buffer");
        root.rewind();
        for (int i = expected.length - 1; i >= 0; i--) check(root.get(i) == expected[i], "reverse buffer");
        Random random = new Random(0x4d33494fL);
        for (int i = 0; i < Math.min(1000, expected.length); i++) {
            int at = random.nextInt(expected.length);
            check(root.get(at) == expected[at], "random buffer");
        }
        int from = expected.length / 5;
        int to = expected.length - expected.length / 7;
        root.position(from).limit(to).mark();
        for (CharBuffer view : new CharBuffer[] {
                root.duplicate(), root.asReadOnlyBuffer(), root.slice(),
                root.slice(from, to - from), root.subSequence(0, to - from)}) {
            int start = view.position();
            char[] actual = new char[to - from + 4];
            Arrays.fill(actual, '?');
            view.get(actual, 2, to - from);
            check(view.position() == start + to - from, "relative bulk advances position");
            check(actual[0] == '?' && actual[1] == '?' && actual[actual.length - 1] == '?', "bulk guards");
            for (int i = from; i < to; i++) check(actual[i - from + 2] == expected[i], "bulk view");
            view.position(start).mark();
            Arrays.fill(actual, '?');
            view.get(start, actual, 2, to - from);
            check(view.position() == start, "absolute bulk preserves position");
            view.reset();
            for (int i = from; i < to; i++) check(actual[i - from + 2] == expected[i], "absolute view");
        }
        root.reset();
        check(root.position() == from, "views have independent positions");
    }

    private static IntUnaryOperator operator(String text) throws Exception {
        Class<?> accessClass = Class.forName("jdk.internal.access.JavaLangAccess");
        Method method;
        try {
            method = accessClass.getMethod("stringCodeUnitCursor", String.class);
        } catch (NoSuchMethodException missing) {
            throw new AssertionError("Native String cursor bridge is missing", missing);
        }
        Object access = Class.forName("jdk.internal.access.SharedSecrets")
                .getMethod("getJavaLangAccess").invoke(null);
        return (IntUnaryOperator) method.invoke(access, text);
    }

    private static void cursor(String text, char[] expected) throws Exception {
        IntUnaryOperator cursor = operator(text);
        Field storage = String.class.getDeclaredField("m3");
        storage.setAccessible(true);
        check((storage.get(text) == null) == (cursor == null), "cursor does not admit flat storage");
        if (cursor == null) return;
        cursorObservations++;
        for (int i = 0; i < expected.length; i++) check(cursor.applyAsInt(i) == expected[i], "cursor forward");
        for (int i = expected.length - 1; i >= 0; i--) check(cursor.applyAsInt(i) == expected[i], "cursor reverse");
        Random random = new Random(77);
        for (int i = 0; i < Math.min(257, expected.length); i++) {
            int at = random.nextInt(expected.length);
            check(cursor.applyAsInt(at) == expected[at], "cursor random");
        }
        expect(IndexOutOfBoundsException.class, () -> cursor.applyAsInt(-1));
        expect(IndexOutOfBoundsException.class, () -> cursor.applyAsInt(expected.length));
    }

    private record DetachedCursor(IntUnaryOperator cursor, char[] expected) { }

    private static DetachedCursor detachedCursor() throws Exception {
        char[] source = units(4099);
        for (int i = 0; i < source.length; i += 13) source[i] = (char) (i * 79);
        String text = compose(source).substring(11, source.length - 7);
        return new DetachedCursor(operator(text), Arrays.copyOfRange(source, 11, source.length - 7));
    }

    private static void cursorLifetime() throws Exception {
        DetachedCursor detached = detachedCursor();
        if (detached.cursor() == null) return;
        cursorObservations++;
        for (int round = 0; round < 12; round++) {
            pressure = new byte[131072];
            System.gc();
            for (int i = round; i < detached.expected().length; i += 17) {
                check(detached.cursor().applyAsInt(i) == detached.expected()[i], "detached cursor retains owner");
            }
        }
    }

    private static void validation() throws Exception {
        CharBuffer relative = CharBuffer.wrap("abc").position(1).limit(2).mark();
        expect(IndexOutOfBoundsException.class, () -> relative.get(new char[2], 3, 2));
        expect(NullPointerException.class, () -> relative.get(null, -1, -1));
        expect(BufferUnderflowException.class, () -> relative.get(new char[2], 0, 2));
        check(relative.position() == 1, "failed relative get keeps position");
        relative.reset();
        expect(IndexOutOfBoundsException.class, () -> relative.get(2, null, 0, 1));
        expect(NullPointerException.class, () -> relative.get(1, null, 0, 1));
        expect(IndexOutOfBoundsException.class, () -> relative.get(1, new char[1], -1, 1));
        check(relative.position() == 1, "failed absolute get keeps position");
        relative.reset();
        check(relative.get(2, new char[0], 0, 0) == relative, "zero absolute read at limit");
        check(relative.get(new char[0], 0, 0) == relative, "zero relative read");

        StringReader reader = new StringReader("abcd");
        check(reader.markSupported() && reader.ready(), "reader readiness");
        expect(NullPointerException.class, () -> reader.read(null, 0, 0));
        expect(IndexOutOfBoundsException.class, () -> reader.read(new char[1], 2, 1));
        check(reader.read() == 'a', "failed reader validation does not consume");
        check(reader.skip(Long.MIN_VALUE) == -1, "negative skip bounded by beginning");
        check(reader.read() == 'a', "negative skip position");
        check(reader.skip(Long.MAX_VALUE) == 3, "positive skip bounded by end");
        check(reader.skip(-4) == 0, "EOF skip contract");
        reader.close();
        reader.close();
        expect(IOException.class, reader::read);
        expect(IOException.class, reader::ready);
        expect(IOException.class, () -> reader.read(null, -1, -1));
        expect(IOException.class, () -> reader.skip(0));
        expect(IOException.class, reader::reset);
        expect(IOException.class, () -> reader.mark(0));
        expect(IllegalArgumentException.class, () -> reader.mark(-1));
        expect(NullPointerException.class, () -> reader.transferTo(null));
        expect(IOException.class, () -> reader.transferTo(new StringWriter()));
    }

    private static final class MutableSequence implements CharSequence {
        final char[] value = {'a', 'b', 'c', 'd', 'e'};
        int calls;
        int failAt = -1;
        public int length() { return value.length; }
        public char charAt(int index) {
            calls++;
            if (index == failAt) throw new IllegalStateException("source refusal");
            return value[index];
        }
        public CharSequence subSequence(int start, int end) { return new String(value, start, end - start); }
    }

    private static void mutableSequence() {
        MutableSequence sequence = new MutableSequence();
        CharBuffer root = CharBuffer.wrap(sequence).position(1).limit(4);
        CharBuffer view = root.slice();
        sequence.value[1] = 'Q';
        check(view.get() == 'Q' && root.get() == 'Q', "generic sequence views remain live");
        check(sequence.calls == 2, "generic scalar callbacks preserved");
        sequence.failAt = 1;
        sequence.calls = 0;
        CharBuffer relative = CharBuffer.wrap(sequence).mark();
        char[] actual = {'?', '?', '?'};
        expect(IllegalStateException.class, () -> relative.get(actual, 0, 3));
        check(relative.position() == 0 && sequence.calls == 2, "generic bulk failure retains incumbent position");
        relative.reset();
        check(actual[0] == 'a' && actual[1] == '?', "generic bulk partial destination");
        sequence.calls = 0;
        CharBuffer absolute = CharBuffer.wrap(sequence).position(3).mark();
        Arrays.fill(actual, '?');
        expect(IllegalStateException.class, () -> absolute.get(0, actual, 0, 3));
        check(absolute.position() == 3 && sequence.calls == 2, "generic absolute failure leaves position");
        absolute.reset();
        check(actual[0] == 'a' && actual[1] == '?', "generic absolute partial destination");
    }

    private static final class TrackingReader extends StringReader {
        int reads;
        TrackingReader(String text) { super(text); }
        @Override public int read(char[] buffer, int offset, int length) throws IOException {
            reads++;
            return super.read(buffer, offset, length);
        }
    }

    private static final class TrackingWriter extends StringWriter {
        final List<Integer> chunks = new ArrayList<>();
        int strings;
        @Override public void write(char[] source, int offset, int length) {
            chunks.add(length);
            super.write(source, offset, length);
        }
        @Override public void write(String source, int offset, int length) {
            strings++;
            super.write(source, offset, length);
        }
    }

    private static void writerCallbacks() throws Exception {
        char[] expected = units(24593);
        String text = compose(expected);
        TrackingWriter writer = new TrackingWriter();
        try (StringReader reader = new StringReader(text)) {
            check(reader.transferTo(writer) == expected.length, "writer subclass transfer count");
        }
        check(writer.chunks.equals(List.of(8192, 8192, 8192, 17)), "writer subclass chunk callbacks");
        check(writer.strings == 0, "writer subclass receives char-array overload");
        check(Arrays.equals(writer.toString().toCharArray(), expected), "writer subclass content");
        TrackingReader reader = new TrackingReader(text);
        StringWriter exact = new StringWriter();
        check(reader.transferTo(exact) == expected.length, "reader subclass transfer count");
        check(reader.reads == 5, "reader subclass includes EOF callback");
        check(Arrays.equals(exact.toString().toCharArray(), expected), "reader subclass content");
        reader.close();

        StringReader failing = new StringReader(text);
        int[] writes = {0};
        Writer refusal = new Writer() {
            public void write(char[] chars, int offset, int length) throws IOException {
                check(length == 8192, "failing writer chunk");
                if (++writes[0] == 2) throw new IOException("writer refusal");
            }
            public void flush() { }
            public void close() { }
        };
        expect(IOException.class, () -> failing.transferTo(refusal));
        check(writes[0] == 2 && failing.read() == expected[16384], "writer failure follows reader advancement");
        failing.close();
    }

    private static void writerSnapshots() throws Exception {
        char[] expected = units(257);
        StringWriter writer = new StringWriter();
        writer.write("pre");
        StringBuffer buffer = writer.getBuffer();
        String snapshot = writer.toString();
        try (StringReader reader = new StringReader(compose(expected))) { reader.transferTo(writer); }
        check(writer.getBuffer() == buffer, "writer buffer identity");
        check(snapshot.equals("pre"), "writer snapshot remains immutable");
        char[] all = new char[expected.length + 3];
        all[0] = 'p'; all[1] = 'r'; all[2] = 'e';
        System.arraycopy(expected, 0, all, 3, expected.length);
        check(Arrays.equals(writer.toString().toCharArray(), all), "writer accumulated content");
        String accumulatedSnapshot = writer.toString();
        buffer.setCharAt(0, 'X');
        check(writer.toString().charAt(0) == 'X', "writer buffer remains live");
        check(accumulatedSnapshot.charAt(0) == 'p', "live buffer mutation preserves old snapshot");
    }

    private static void transferCapacity() throws Exception {
        for (int length : new int[] {0, 1, 15, 16, 17, 8191, 8192, 8193}) {
            char[] expected = units(length);
            String text = compose(expected);
            for (int capacity : new int[] {0, 16, 65}) {
                for (String prefix : new String[] {"", "abc", "\u0100\ud800x"}) {
                    StringWriter actual = new StringWriter(capacity);
                    StringWriter reference = new StringWriter(capacity);
                    actual.write(prefix);
                    reference.write(prefix);
                    StringBuffer visible = actual.getBuffer();
                    try (StringReader reader = new StringReader(text)) {
                        check(reader.transferTo(actual) == length, "capacity transfer count");
                    }
                    for (int at = 0; at < expected.length; at += 8192) {
                        reference.write(expected, at, Math.min(8192, expected.length - at));
                    }
                    check(actual.getBuffer() == visible, "capacity transfer buffer identity");
                    check(visible.capacity() == reference.getBuffer().capacity(), "capacity matches char-array chunks");
                    check(Arrays.equals(actual.toString().toCharArray(), reference.toString().toCharArray()), "capacity reference content");
                    int requested = visible.capacity() + 3;
                    visible.ensureCapacity(requested);
                    reference.getBuffer().ensureCapacity(requested);
                    check(visible.capacity() == reference.getBuffer().capacity(), "subsequent capacity growth");
                }
            }
        }
    }

    private static void closeDuringBlockedTransfer() throws Exception {
        char[] expected = units(24593);
        StringReader reader = new StringReader(compose(expected));
        StringWriter writer = new StringWriter();
        StringBuffer writerMonitor = writer.getBuffer();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            try { reader.transferTo(writer); }
            catch (Throwable problem) { failure.set(problem); }
        }, "m3-reader-transfer-oracle");
        worker.setDaemon(true);
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        try {
            synchronized (writerMonitor) {
                worker.start();
                long deadline = System.nanoTime() + 10_000_000_000L;
                ThreadInfo blocked = null;
                while (System.nanoTime() < deadline) {
                    ThreadInfo info = threads.getThreadInfo(new long[] {worker.threadId()}, true, true)[0];
                    if (info != null && info.getThreadState() == Thread.State.BLOCKED
                            && info.getLockOwnerId() == Thread.currentThread().threadId()) {
                        blocked = info;
                        break;
                    }
                    Thread.sleep(1);
                }
                check(blocked != null, "transfer blocked on exact writer monitor");
                for (MonitorInfo monitor : blocked.getLockedMonitors()) {
                    check(monitor.getIdentityHashCode() != System.identityHashCode(reader),
                            "writer operation must not retain reader monitor");
                }
                reader.close();
            }
        } finally {
            worker.join(10000);
        }
        check(!worker.isAlive(), "blocked transfer terminates after close");
        check(failure.get() instanceof IOException, "next read observes concurrent close");
        check(Arrays.equals(writer.toString().toCharArray(), Arrays.copyOf(expected, 8192)),
                "captured first chunk survives concurrent close");
    }

    private static final class ArrayReader extends Reader {
        final char[] source;
        int at;
        ArrayReader(char[] source) { this.source = source; }
        public int read(char[] target, int offset, int length) {
            if (length == 0) return 0;
            if (at == source.length) return -1;
            int count = Math.min(length, source.length - at);
            System.arraycopy(source, at, target, offset, count);
            at += count;
            return count;
        }
        public void close() { }
    }

    private static final class TraceCharset extends Charset {
        final List<String> events = new ArrayList<>();
        TraceCharset() { super("X-M3-Oracle", new String[0]); }
        public boolean contains(Charset other) { return other instanceof TraceCharset; }
        public CharsetDecoder newDecoder() {
            return new CharsetDecoder(this, 1, 1) {
                protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
                    while (in.hasRemaining()) {
                        if (!out.hasRemaining()) return CoderResult.OVERFLOW;
                        out.put((char) (in.get() & 255));
                    }
                    return CoderResult.UNDERFLOW;
                }
            };
        }
        public CharsetEncoder newEncoder() {
            return new CharsetEncoder(this, 1, 1, new byte[] {'?'}) {
                protected CoderResult encodeLoop(CharBuffer in, ByteBuffer out) {
                    events.add(in.getClass().getName() + ":" + in.hasArray() + ":"
                            + in.isReadOnly() + ":" + in.position() + ":" + in.limit());
                    while (in.hasRemaining()) {
                        if (!out.hasRemaining()) return CoderResult.OVERFLOW;
                        out.put((byte) (in.get() & 127));
                    }
                    return CoderResult.UNDERFLOW;
                }
            };
        }
    }

    private static void encoderCallbacks() throws Exception {
        char[] expected = units(24593);
        TraceCharset actualCharset = new TraceCharset();
        TraceCharset referenceCharset = new TraceCharset();
        ByteArrayOutputStream actual = new ByteArrayOutputStream();
        ByteArrayOutputStream reference = new ByteArrayOutputStream();
        try (Reader reader = new StringReader(compose(expected));
             Writer writer = new OutputStreamWriter(actual, actualCharset)) {
            check(reader.transferTo(writer) == expected.length, "encoder transfer count");
        }
        try (Reader reader = new ArrayReader(expected);
             Writer writer = new OutputStreamWriter(reference, referenceCharset)) {
            check(reader.transferTo(writer) == expected.length, "reference encoder count");
        }
        check(actualCharset.events.equals(referenceCharset.events), "custom encoder input representation and callbacks");
        check(Arrays.equals(actual.toByteArray(), reference.toByteArray()), "encoder reference bytes");
        byte[] actualBytes = actual.toByteArray();
        check(actualBytes.length == expected.length, "encoder byte count");
        for (int i = 0; i < expected.length; i++) check(actualBytes[i] == (byte) (expected[i] & 127), "encoder independent byte oracle");
    }

    @FunctionalInterface
    private interface Action { void run() throws Exception; }

    private static void expect(Class<? extends Throwable> type, Action action) {
        try {
            action.run();
        } catch (Throwable problem) {
            check(type.isInstance(problem), "expected " + type.getName() + ", got " + problem);
            return;
        }
        throw new AssertionError("Expected " + type.getName());
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
