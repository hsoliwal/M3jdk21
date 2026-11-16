/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 *
 * @test
 * @summary Whole-array monitored I/O preserves virtual hooks, range behavior and ownership
 * @run main/othervm MonitoredIoArrayOverloads
 */
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ProgressMonitor;

/**
 * Reflection lets the same compiled test and old subclass binaries exercise an
 * exact baseline before the new overloads exist. The default jtreg run requires
 * all five new declarations. The canonical proof also compiles a typed client.
 */
public class MonitoredIoArrayOverloads {
    private static int checks;

    private enum Operation {
        BYTE_READ(InputStream.class, "readMonitored", byte[].class, "input", true),
        BYTE_BOUNDED(InputStream.class, "readNBytesMonitored", byte[].class, "input", true),
        BYTE_WRITE(OutputStream.class, "writeMonitored", byte[].class, "output", false),
        CHAR_READ(Reader.class, "readMonitored", char[].class, "reader", true),
        CHAR_WRITE(Writer.class, "writeMonitored", char[].class, "writer", false);

        final Class<?> owner;
        final String method;
        final Class<?> arrayType;
        final String selector;
        final boolean read;

        Operation(Class<?> owner, String method, Class<?> arrayType,
                  String selector, boolean read) {
            this.owner = owner;
            this.method = method;
            this.arrayType = arrayType;
            this.selector = selector;
            this.read = read;
        }

        Object array(int size) {
            return arrayType == byte[].class ? new byte[size] : new char[size];
        }

        int length(Object array) {
            return arrayType == byte[].class ? ((byte[]) array).length : ((char[]) array).length;
        }

        Method method(boolean whole) throws NoSuchMethodException {
            return whole
                    ? owner.getDeclaredMethod(method, arrayType, ProgressMonitor.class)
                    : owner.getDeclaredMethod(method, arrayType, int.class, int.class,
                            ProgressMonitor.class);
        }

        Object invoke(Object receiver, Object array, ProgressMonitor monitor,
                      boolean whole) throws Exception {
            try {
                return whole ? method(true).invoke(receiver, array, monitor)
                        : method(false).invoke(receiver, array, 0, length(array), monitor);
            } catch (InvocationTargetException failure) {
                Throwable cause = failure.getCause();
                if (cause instanceof Exception exception) throw exception;
                if (cause instanceof Error error) throw error;
                throw new AssertionError(cause);
            }
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static final class UntouchedMonitor implements ProgressMonitor {
        private AssertionError touched() { return new AssertionError("wrapper touched monitor"); }
        @Override public void begin(String task, long total) { throw touched(); }
        @Override public void worked(long units) { throw touched(); }
        @Override public void message(String text) { throw touched(); }
        @Override public boolean isCancelled() { throw touched(); }
        @Override public void cancel() { throw touched(); }
        @Override public void checkCancelled() { throw touched(); }
        @Override public void done() { throw touched(); }
    }

    private static final class Hook {
        Object array;
        ProgressMonitor monitor;
        int offset;
        int length;
        int calls;
        int lifecycle;
        int result;
        String operation;
        Throwable failure;

        int accept(String operation, Object array, int offset, int length,
                   ProgressMonitor monitor) throws IOException {
            this.operation = operation;
            this.array = array;
            this.offset = offset;
            this.length = length;
            this.monitor = monitor;
            calls++;
            if (failure instanceof IOException io) throw io;
            if (failure instanceof RuntimeException runtime) throw runtime;
            if (failure instanceof Error error) throw error;
            return result;
        }
    }

    private static final class OldInput extends InputStream {
        final Hook hook;
        OldInput(Hook hook) { this.hook = hook; }
        @Override public int read() { throw new AssertionError("primitive read bypass"); }
        @Override public int readMonitored(byte[] b, int o, int n, ProgressMonitor m)
                throws IOException { return hook.accept("BYTE_READ", b, o, n, m); }
        @Override public int readNBytesMonitored(byte[] b, int o, int n, ProgressMonitor m)
                throws IOException { return hook.accept("BYTE_BOUNDED", b, o, n, m); }
        @Override public void close() { hook.lifecycle++; }
    }

    private static final class OldOutput extends OutputStream {
        final Hook hook;
        OldOutput(Hook hook) { this.hook = hook; }
        @Override public void write(int value) { throw new AssertionError("primitive write bypass"); }
        @Override public void writeMonitored(byte[] b, int o, int n, ProgressMonitor m)
                throws IOException { hook.accept("BYTE_WRITE", b, o, n, m); }
        @Override public void close() { hook.lifecycle++; }
        @Override public void flush() { hook.lifecycle++; }
    }

    private static final class OldReader extends Reader {
        final Hook hook;
        OldReader(Hook hook) { this.hook = hook; }
        @Override public int read(char[] b, int o, int n) {
            throw new AssertionError("character read bypass");
        }
        @Override public int readMonitored(char[] b, int o, int n, ProgressMonitor m)
                throws IOException { return hook.accept("CHAR_READ", b, o, n, m); }
        @Override public void close() { hook.lifecycle++; }
    }

    private static final class OldWriter extends Writer {
        final Hook hook;
        OldWriter(Hook hook) { this.hook = hook; }
        @Override public void write(char[] b, int o, int n) {
            throw new AssertionError("character write bypass");
        }
        @Override public void writeMonitored(char[] b, int o, int n, ProgressMonitor m)
                throws IOException { hook.accept("CHAR_WRITE", b, o, n, m); }
        @Override public void close() { hook.lifecycle++; }
        @Override public void flush() { hook.lifecycle++; }
    }

    private static Object oldSubclass(Operation operation, Hook hook) {
        return switch (operation) {
            case BYTE_READ, BYTE_BOUNDED -> new OldInput(hook);
            case BYTE_WRITE -> new OldOutput(hook);
            case CHAR_READ -> new OldReader(hook);
            case CHAR_WRITE -> new OldWriter(hook);
        };
    }

    private static void forwarding(Operation operation, boolean whole) throws Exception {
        if (whole) {
            Method method = operation.method(true);
            int modifiers = method.getModifiers();
            check(Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)
                    && !Modifier.isAbstract(modifiers) && !Modifier.isFinal(modifiers)
                    && !Modifier.isSynchronized(modifiers), operation + " virtual method shape");
            check(method.getReturnType() == (operation.read ? int.class : void.class),
                    operation + " return type");
            check(Arrays.equals(method.getExceptionTypes(), new Class<?>[] { IOException.class }),
                    operation + " checked exception declaration");
        }
        for (int size : new int[] { 0, 17 }) {
            for (ProgressMonitor monitor : new ProgressMonitor[] { new UntouchedMonitor(), null }) {
                Hook hook = new Hook();
                hook.result = size == 0 ? 0 : size - 3;
                Object receiver = oldSubclass(operation, hook);
                Object array = operation.array(size);
                Object result = operation.invoke(receiver, array, monitor, whole);
                check(hook.calls == 1, operation + " exact virtual call count");
                check(hook.operation.equals(operation.name()), operation + " selected correct hook");
                check(hook.array == array && hook.monitor == monitor, operation + " argument identity");
                check(hook.offset == 0 && hook.length == size, operation + " full array range");
                check(Objects.equals(result, operation.read ? Integer.valueOf(hook.result) : null),
                        operation + " result identity");
                check(hook.lifecycle == 0, operation + " borrowed ownership");
            }
        }
        for (Throwable failure : new Throwable[] {
                new IOException("io-sentinel"), new IllegalStateException("runtime-sentinel"),
                new AssertionError("error-sentinel") }) {
            Hook hook = new Hook();
            hook.failure = failure;
            try {
                operation.invoke(oldSubclass(operation, hook), operation.array(4),
                        new UntouchedMonitor(), whole);
                throw new AssertionError("missing exact failure: " + operation);
            } catch (Throwable observed) {
                check(observed == failure, operation + " unchanged failure object");
                check(hook.calls == 1 && hook.lifecycle == 0, operation + " failure ownership");
            }
        }
        if (whole) {
            Hook hook = new Hook();
            try {
                operation.invoke(oldSubclass(operation, hook), null, new UntouchedMonitor(), true);
                throw new AssertionError("null array accepted: " + operation);
            } catch (NullPointerException expected) {
                check(hook.calls == 0 && hook.lifecycle == 0, operation + " null before dispatch");
            }
        }
    }

    private static final class Monitor implements ProgressMonitor {
        final List<String> events = new ArrayList<>();
        long completed;
        long cancelAt = Long.MAX_VALUE;
        boolean cancelled;
        RuntimeException workFailure;
        int lifecycle;

        @Override public void checkCancelled() {
            events.add("check");
            ProgressMonitor.super.checkCancelled();
        }
        @Override public boolean isCancelled() { return cancelled || completed >= cancelAt; }
        @Override public void worked(long units) {
            events.add("worked:" + units);
            completed += units;
            if (workFailure != null) throw workFailure;
        }
        @Override public void begin(String task, long total) { lifecycle++; }
        @Override public void message(String text) { lifecycle++; }
        @Override public void done() { lifecycle++; }
    }

    private static final class State {
        final byte[] inputBytes;
        final char[] inputChars;
        final ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        final StringBuilder outputChars = new StringBuilder();
        final int limit;
        int position;
        int calls;
        int lifecycle;
        IOException failure;

        State(int available, int limit) {
            inputBytes = new byte[available];
            inputChars = new char[available];
            for (int i = 0; i < available; i++) {
                inputBytes[i] = (byte) (i * 37 + 11);
                inputChars[i] = (char) (i * 811 + 0xd800);
            }
            this.limit = limit;
        }

        void before() throws IOException {
            calls++;
            if (failure != null) throw failure;
        }

        Object receiver(Operation operation) {
            return switch (operation) {
                case BYTE_READ, BYTE_BOUNDED -> new InputStream() {
                    @Override public int read() { throw new AssertionError("unexpected scalar read"); }
                    @Override public int read(byte[] b, int o, int n) throws IOException {
                        before();
                        if (n == 0) return 0;
                        if (position == inputBytes.length) return -1;
                        int count = Math.min(Math.min(n, limit), inputBytes.length - position);
                        System.arraycopy(inputBytes, position, b, o, count);
                        position += count;
                        return count;
                    }
                    @Override public void close() { lifecycle++; }
                };
                case BYTE_WRITE -> new OutputStream() {
                    @Override public void write(int b) { throw new AssertionError("unexpected scalar write"); }
                    @Override public void write(byte[] b, int o, int n) throws IOException {
                        before();
                        outputBytes.write(b, o, n);
                    }
                    @Override public void close() { lifecycle++; }
                    @Override public void flush() { lifecycle++; }
                };
                case CHAR_READ -> new Reader() {
                    @Override public int read(char[] b, int o, int n) throws IOException {
                        before();
                        if (n == 0) return 0;
                        if (position == inputChars.length) return -1;
                        int count = Math.min(Math.min(n, limit), inputChars.length - position);
                        System.arraycopy(inputChars, position, b, o, count);
                        position += count;
                        return count;
                    }
                    @Override public void close() { lifecycle++; }
                };
                case CHAR_WRITE -> new Writer() {
                    @Override public void write(char[] b, int o, int n) throws IOException {
                        before();
                        outputChars.append(b, o, n);
                    }
                    @Override public void close() { lifecycle++; }
                    @Override public void flush() { lifecycle++; }
                };
            };
        }
    }

    private record Outcome(Object result, Exception failure) { }

    private static Outcome observe(Operation operation, State state, Object array,
                                   Monitor monitor, boolean whole) throws Exception {
        try {
            return new Outcome(operation.invoke(state.receiver(operation), array, monitor, whole), null);
        } catch (IOException | RuntimeException failure) {
            return new Outcome(null, failure);
        }
    }

    private static void parity(Operation operation, int size, int available, int limit,
                               int failureMode) throws Exception {
        State old = new State(available, limit);
        State next = new State(available, limit);
        Monitor before = new Monitor();
        Monitor after = new Monitor();
        Throwable sentinel = null;
        if (failureMode == 1) {
            before.cancelled = after.cancelled = true;
        } else if (failureMode == 2) {
            before.cancelAt = after.cancelAt = 1;
        } else if (failureMode == 3) {
            RuntimeException failure = new IllegalStateException("monitor-sentinel");
            before.workFailure = after.workFailure = failure;
            sentinel = failure;
        } else if (failureMode == 4) {
            IOException failure = new IOException("delegate-sentinel");
            old.failure = next.failure = failure;
            sentinel = failure;
        }
        Object oldArray = operation.array(size);
        Object newArray = operation.array(size);
        if (oldArray instanceof byte[] bytes) {
            for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) (i * 43 + 17);
            System.arraycopy(bytes, 0, newArray, 0, bytes.length);
        } else {
            char[] chars = (char[]) oldArray;
            for (int i = 0; i < chars.length; i++) chars[i] = (char) (i * 811 + 0xdfff);
            System.arraycopy(chars, 0, newArray, 0, chars.length);
        }
        Outcome expected = observe(operation, old, oldArray, before, false);
        Outcome actual = observe(operation, next, newArray, after, true);
        check(Objects.equals(expected.result(), actual.result()), operation + " range result parity");
        if (expected.failure() == null) {
            check(actual.failure() == null, operation + " unexpected failure");
        } else {
            check(actual.failure() != null
                    && expected.failure().getClass() == actual.failure().getClass()
                    && Objects.equals(expected.failure().getMessage(), actual.failure().getMessage()),
                    operation + " failure parity");
        }
        if (failureMode == 1 || failureMode == 2) {
            check(expected.failure() instanceof CancellationException
                    && actual.failure() instanceof CancellationException,
                    operation + " cancellation exercised");
        } else if (sentinel != null) {
            check(expected.failure() == sentinel && actual.failure() == sentinel,
                    operation + " callback/delegate failure identity");
        } else {
            check(expected.failure() == null && actual.failure() == null,
                    operation + " normal case succeeded");
        }
        check(oldArray instanceof byte[]
                        ? Arrays.equals((byte[]) oldArray, (byte[]) newArray)
                        : Arrays.equals((char[]) oldArray, (char[]) newArray),
                operation + " destination code-unit identity");
        check(Arrays.equals(old.outputBytes.toByteArray(), next.outputBytes.toByteArray())
                        && old.outputChars.toString().equals(next.outputChars.toString()),
                operation + " output identity");
        check(old.position == next.position && old.calls == next.calls,
                operation + " underlying invocation parity");
        check(before.completed == after.completed && before.events.equals(after.events),
                operation + " progress order and count");
        check(old.lifecycle == 0 && next.lifecycle == 0
                        && before.lifecycle == 0 && after.lifecycle == 0,
                operation + " caller retains lifecycle");
    }

    private static void inheritedBehavior(Operation operation) throws Exception {
        for (int size : new int[] { 0, 1, 3, 31, 8191, 8192, 8193, 16385, 32769 }) {
            if (operation.read) {
                for (int available : new int[] { 0, size / 2, size + 7 }) {
                    for (int limit : new int[] { 3, Integer.MAX_VALUE }) {
                        parity(operation, size, available, limit, 0);
                    }
                }
            } else {
                parity(operation, size, 0, Integer.MAX_VALUE, 0);
            }
        }
        for (int failureMode = 1; failureMode <= 4; failureMode++) {
            parity(operation, 16385, 16392, Integer.MAX_VALUE, failureMode);
        }
        State interrupted = new State(10, 10);
        Thread.currentThread().interrupt();
        try {
            operation.invoke(interrupted.receiver(operation), operation.array(10),
                    ProgressMonitor.NONE, true);
            throw new AssertionError("interruption ignored: " + operation);
        } catch (CancellationException expected) {
            check(Thread.currentThread().isInterrupted(), operation + " interrupt preserved");
            check(interrupted.calls == 0 && interrupted.lifecycle == 0,
                    operation + " interruption before delegate");
        } finally {
            Thread.interrupted();
        }
    }

    private static void apiSurface() {
        TreeSet<String> rows = new TreeSet<>();
        for (Class<?> owner : new Class<?>[] {
                InputStream.class, OutputStream.class, Reader.class, Writer.class }) {
            rows.add("TYPE|" + owner.getName() + "|" + owner.toGenericString()
                    + "|" + Arrays.toString(owner.getGenericInterfaces()));
            for (Constructor<?> constructor : owner.getDeclaredConstructors()) {
                if (visible(constructor.getModifiers())) rows.add("CTOR|" + owner.getName() + "|"
                        + MethodType.methodType(void.class, constructor.getParameterTypes())
                                .toMethodDescriptorString() + "|" + constructor.toGenericString());
            }
            for (Method method : owner.getDeclaredMethods()) {
                if (visible(method.getModifiers())) rows.add("METHOD|" + owner.getName() + "|"
                        + method.getName() + "|" + MethodType.methodType(method.getReturnType(),
                                method.getParameterTypes()).toMethodDescriptorString()
                        + "|" + method.toGenericString() + "|bridge=" + method.isBridge()
                        + "|synthetic=" + method.isSynthetic() + "|varargs=" + method.isVarArgs());
            }
            for (Field field : owner.getDeclaredFields()) {
                if (visible(field.getModifiers())) rows.add("FIELD|" + owner.getName() + "|"
                        + field.getName() + "|" + field.getType().descriptorString()
                        + "|" + field.toGenericString());
            }
        }
        rows.forEach(System.out::println);
    }

    private static boolean visible(int modifiers) {
        return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
    }

    public static void main(String[] args) throws Exception {
        String selector = args.length == 0 ? "all" : args[0];
        if (selector.equals("api")) {
            apiSurface();
            return;
        }
        if (selector.equals("legacy")) {
            for (Operation operation : Operation.values()) forwarding(operation, false);
            System.out.println("MONITORED_IO_ARRAY_LEGACY_PASS checks=" + checks);
            return;
        }
        int operations = 0;
        for (Operation operation : Operation.values()) {
            if (!selector.equals("all") && !selector.equals(operation.selector)) continue;
            operations++;
            forwarding(operation, true);
            inheritedBehavior(operation);
        }
        check(operations > 0, "unknown operation selector: " + selector);
        System.out.println("MONITORED_IO_ARRAY_OVERLOADS_PASS operations="
                + operations + " checks=" + checks);
    }
}
