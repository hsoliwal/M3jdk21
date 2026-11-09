// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Differential dump driver of the PMem task log and replay (gate 3 of recipe/pmem-task-log-replay-20261106; not a test).
 * The donor family (the donor package of RELOCATION_MAP.tsv, type prefix "") and the relocated family (jdk.m3.optane, type
 * prefix "") replay the same seeded inputs through reflection over three region kinds: a byte-array region behind a
 * proxy of the family's PmemRegion interface (logical origin above 3 GiB, failure injection at each of the four append
 * steps: body write, body persist, marker write, marker persist), a FILE region of the SDK and, in native mode (Linux,
 * libm3pmemsdk on java.library.path), a POSIX region and FILE/POSIX regions of a sparse file at a file offset above
 * 2 GiB. Lanes: open refusals, appends with invalid identities and capacity exhaustion, reopen and read, committed
 * corruption at every header field, a record beyond the tail, interrupted appends, progress cancellation, replay
 * reconciliation with every refusal of the contract, then {@code operations} seeded random append/read/reopen/
 * reconcile/fail/corrupt operations. Every result and every refusal (exception class and erased message) is printed;
 * temporary paths, the package names and the M3 type prefix are erased, so the two dumps must be byte-identical.
 * usage: PmemTaskLogDump &lt;package&gt; &lt;type prefix&gt; &lt;operations&gt; [native]
 */
public final class PmemTaskLogDump {
    private static final long SEED = 20_261_106L;
    private static final int RECORD = 144;
    private static final long ABOVE_3_GIB = 3L << 30;
    private static final long ABOVE_2_GIB = (1L << 31) + 4_096;
    private static final String[] STEPS = {"none", "write-body", "persist-body", "write-marker", "persist-marker"};

    private final String pkg;
    private final String prefix;
    private final boolean nativeMode;
    private final PrintStream out;
    private final Path root;
    private final Class<?> regionType;
    private final Class<?> logMonitorType;
    private final Class<?> replayMonitorType;
    private final Class<?> readerType;
    private final Method logOpen;
    private final Method append;
    private final Method read;
    private final Method committedCount;
    private final Method capacity;
    private final Method recovered;
    private final Method reconcile;
    private final Method sdkOpen;
    private final Constructor<?> expected;
    private final Object[] statuses;
    private final Object readOnly;
    private final Object readWrite;
    private final Object fileBackend;
    private final Object posixBackend;
    private final Object pageGranularity;
    private long exceptions;
    private long operations;

    private PmemTaskLogDump(String pkg, String prefix, boolean nativeMode, PrintStream out) throws Exception {
        this.pkg = pkg;
        this.prefix = prefix;
        this.nativeMode = nativeMode;
        this.out = out;
        this.root = Files.createTempDirectory("m3-pmem-task-dump-").toRealPath();
        Class<?> log = type("PmemTaskLog");
        Class<?> status = type("PmemTaskLog$Status");
        Class<?> replay = type("PmemTaskReplay");
        regionType = type("PmemRegion");
        logMonitorType = type("PmemTaskLog$ProgressMonitor");
        replayMonitorType = type("PmemTaskReplay$ProgressMonitor");
        readerType = type("PmemTaskReplay$ContentReader");
        Class<?> access = type("PmemAccess");
        Class<?> backend = type("PmemBackend");
        logOpen = log.getMethod("open", regionType, long.class, long.class, logMonitorType);
        append = log.getMethod("append", status, String.class, String.class, String.class);
        read = log.getMethod("read", long.class);
        committedCount = log.getMethod("committedCount");
        capacity = log.getMethod("capacity");
        recovered = log.getMethod("recoveredIncompleteTail");
        reconcile = replay.getMethod("reconcile", log, List.class, readerType, replayMonitorType);
        expected = type("PmemTaskReplay$Expected").getConstructor(long.class, String.class, String.class);
        sdkOpen = type("PmemSdk").getMethod("open", Path.class, long.class, long.class, access, backend);
        statuses = status.getEnumConstants();
        readOnly = constant(access, "READ_ONLY");
        readWrite = constant(access, "READ_WRITE");
        fileBackend = constant(backend, "FILE");
        posixBackend = constant(backend, "POSIX");
        pageGranularity = constant(type("PmemGranularity"), "PAGE");
    }

    /** The dump of one family as text (used by the JUnit wrapper); {@link #main} prints the same to stdout. */
    public static String replay(String pkg, String prefix, int operations, boolean nativeMode) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(1 << 20);
        try (PrintStream stream = new PrintStream(bytes, false, StandardCharsets.UTF_8)) {
            new PmemTaskLogDump(pkg, prefix, nativeMode, stream).run(operations);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("usage: PmemTaskLogDump <package> <type prefix> <operations> [native]");
            System.exit(2);
        }
        boolean nativeMode = args.length > 3 && args[3].equals("native");
        PrintStream stream = new PrintStream(System.out, false, StandardCharsets.UTF_8);
        new PmemTaskLogDump(args[0], args[1], nativeMode, stream).run(Integer.parseInt(args[2]));
        stream.flush();
    }

    private void run(int count) throws Exception {
        try {
            line("M3_PMEM_TASK_LOG_DUMP native=" + nativeMode + " operations=" + count);
            openRefusals();
            for (String kind : kinds()) {
                appendLane(kind);
                corruptionLane(kind);
            }
            interruptedLane();
            monitorLane();
            replayLane();
            random(count);
            line("TOTAL operations=" + operations + " exceptions=" + exceptions);
        } finally {
            deleteTree(root);
        }
    }

    private List<String> kinds() {
        List<String> kinds = new ArrayList<>(List.of("memory", "file"));
        if (nativeMode) kinds.addAll(List.of("posix", "file-sparse", "posix-sparse"));
        return kinds;
    }

    // ---------------------------------------------------------------- reflection and formatting

    private Class<?> type(String donorName) throws ClassNotFoundException {
        return Class.forName(pkg + "." + prefix + donorName);
    }

    private static Object constant(Class<?> enumType, String name) {
        return Arrays.stream(enumType.getEnumConstants()).filter(c -> ((Enum<?>) c).name().equals(name))
                .findFirst().orElseThrow();
    }

    private void line(String text) {
        out.print(text);
        out.print('\n');
    }

    private Object call(Method method, Object target, Object... args) {
        operations++;
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            return new Refusal(refusal(e.getCause()));
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new IllegalStateException("reflective call failed: " + method, e);
        }
    }

    private String invoke(Method method, Object target, Object... args) {
        return format(call(method, target, args));
    }

    private Object construct(Constructor<?> constructor, Object... args) {
        operations++;
        try {
            return constructor.newInstance(args);
        } catch (InvocationTargetException e) {
            return new Refusal(refusal(e.getCause()));
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            throw new IllegalStateException("reflective construction failed: " + constructor, e);
        }
    }

    private String refusal(Throwable cause) {
        exceptions++;
        return "!" + cause.getClass().getSimpleName() + ": " + erase(cause.getMessage());
    }

    private String format(Object result) {
        if (result == null) return "ok";
        if (result instanceof Refusal r) return r.text();
        if (result instanceof Enum<?> e) return e.name();
        if (result instanceof String || result instanceof Number || result instanceof Boolean || result instanceof Record) {
            return erase(result.toString());
        }
        return "object";
    }

    private String erase(String text) {
        if (text == null) return "null";
        String erased = text.replace(root.toString(), "<tmp>").replace(root.toString().replace('\\', '/'), "<tmp>")
                .replace(pkg + ".", "");
        if (!prefix.isEmpty()) erased = erased.replace(prefix + "Pmem", "Pmem").replace(prefix + "NativePmem", "NativePmem");
        return erased;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String hex(byte[] bytes) {
        return HexFormat.of().formatHex(sha256().digest(bytes));
    }

    private static String identity(String label, long n) {
        return hex((label + ":" + n).getBytes(StandardCharsets.UTF_8));
    }

    private record Refusal(String text) { }

    // ---------------------------------------------------------------- regions

    /** A byte-array region (state of the proxy below): logical origin, injected failure step, persist count. */
    private static final class Memory {
        final long base;
        final byte[] data;
        boolean closed;
        boolean writable = true;
        int failStep;
        int step;
        long persists;

        Memory(long base, int size) {
            this.base = base;
            this.data = new byte[size];
        }

        int index(long offset, long length) {
            if (offset < base || length < 0 || offset - base > data.length - length) {
                throw new IndexOutOfBoundsException("memory region range");
            }
            return Math.toIntExact(offset - base);
        }

        /** Counts the append steps (write, persist, write, persist) and throws at the injected one. */
        void tick(boolean persist) throws IOException {
            step++;
            if (failStep != 0 && step == failStep) {
                failStep = 0;
                if (persist) throw new IOException("injected persist failure");
                throw new IllegalStateException("injected write failure");
            }
        }
    }

    private Object memoryRegion(Memory memory) {
        return Proxy.newProxyInstance(regionType.getClassLoader(), new Class<?>[] {regionType}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "length": return memory.base + memory.data.length;
                case "fileOffset": return 0L;
                case "path": return Path.of("pmem-task-memory");
                case "access": return memory.writable ? readWrite : readOnly;
                case "backend": return fileBackend;
                case "persistenceGranularity": return pageGranularity;
                case "isOpen": return !memory.closed;
                case "close": memory.closed = true; return null;
                case "hashCode": return System.identityHashCode(proxy);
                case "equals": return proxy == args[0];
                case "toString": return "memory-region";
                default: break;
            }
            if (memory.closed) throw new IllegalStateException("memory region is closed");
            long offset = (Long) args[0];
            if (method.getName().equals("persist")) {
                memory.index(offset, (Long) args[1]);
                memory.tick(true);
                memory.persists++;
                return null;
            }
            byte[] bytes = (byte[]) args[1];
            int start = (Integer) args[2];
            int count = (Integer) args[3];
            int at = memory.index(offset, count);
            if (method.getName().equals("read")) {
                System.arraycopy(memory.data, at, bytes, start, count);
            } else {
                memory.tick(false);
                System.arraycopy(bytes, start, memory.data, at, count);
            }
            return null;
        });
    }

    /** A region of the given kind with room for {@code records} receipts after {@code origin}; null after a refusal. */
    private Region region(String kind, long origin, int records) throws IOException {
        long length = origin + (long) records * RECORD;
        if (kind.equals("memory")) {
            Memory memory = new Memory(ABOVE_3_GIB, Math.toIntExact(length));
            return new Region(memoryRegion(memory), memory, ABOVE_3_GIB + origin);
        }
        boolean sparse = kind.endsWith("-sparse");
        long fileOffset = sparse ? ABOVE_2_GIB : 0;
        Path file = root.resolve(kind + "-" + operations + ".bin");
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw")) {
            raf.setLength(fileOffset + length);
        }
        Object backend = kind.startsWith("posix") ? posixBackend : fileBackend;
        Object opened = call(sdkOpen, null, file, fileOffset, length, readWrite, backend);
        if (opened instanceof Refusal r) {
            line("REGION " + kind + " -> " + r.text());
            return null;
        }
        return new Region(opened, null, origin);
    }

    private record Region(Object handle, Memory memory, long origin) { }

    private void closeRegion(Region region) {
        if (region != null) invoke(method(regionType, "close"), region.handle());
    }

    private static Method method(Class<?> owner, String name, Class<?>... parameters) {
        try {
            return owner.getMethod(name, parameters);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("missing method " + name, e);
        }
    }

    private Object open(Region region, long capacity) {
        return call(logOpen, null, region.handle(), region.origin(), capacity, null);
    }

    // ---------------------------------------------------------------- lanes

    private void openRefusals() throws IOException {
        Region region = region("memory", 0, 4);
        line("OPEN null -> " + invoke(logOpen, null, null, 0L, 1L, null));
        long origin = region.origin();
        for (long[] bad : new long[][] {{origin, 0}, {origin, -1}, {-1, 1}, {origin, 5}, {origin + 4L * RECORD + 1, 1},
            {Long.MAX_VALUE, 1}, {origin, Long.MAX_VALUE}, {0, 1}}) {
            line("OPEN origin=" + (bad[0] - ABOVE_3_GIB) + " capacity=" + bad[1] + " -> "
                    + invoke(logOpen, null, region.handle(), bad[0], bad[1], null));
        }
        region.memory().writable = false;
        line("OPEN read-only -> " + format(open(region, 4)));
        region.memory().writable = true;
        Object log = open(region, 4);
        line("OPEN empty -> committed=" + invoke(committedCount, log) + " capacity=" + invoke(capacity, log) + " tail="
                + invoke(recovered, log));
        region.memory().closed = true;
        line("OPEN closed -> " + format(open(region, 4)));
    }

    private void appendLane(String kind) throws IOException {
        String tag = "APPEND " + kind;
        int capacity = 300;
        Region region = region(kind, 4_096, capacity);
        if (region == null) return;
        Object log = open(region, capacity);
        line(tag + " open -> committed=" + invoke(committedCount, log));
        String one = identity("one", 1);
        String[] bad = {null, one.toUpperCase(java.util.Locale.ROOT), one.substring(1), one.substring(1) + "g", "", one + "0"};
        for (String value : bad) {
            line(tag + " bad[" + value + "] task=" + invoke(append, log, statuses[0], value, one, one) + " source="
                    + invoke(append, log, statuses[1], one, value, one) + " result=" + invoke(append, log, statuses[2], one, one, value));
        }
        line(tag + " null-status -> " + invoke(append, log, null, one, one, one));
        MessageDigest receipts = sha256();
        for (int i = 0; i < capacity; i++) {
            String receipt = invoke(append, log, statuses[i % statuses.length], identity("task", i), identity("source", i % 7),
                    identity("result", i));
            receipts.update(receipt.getBytes(StandardCharsets.UTF_8));
            if (i < 3 || i == capacity - 1) line(tag + " append " + i + " -> " + receipt);
        }
        line(tag + " appended digest=" + HexFormat.of().formatHex(receipts.digest(), 0, 8) + " exhausted -> "
                + invoke(append, log, statuses[0], one, one, one));
        Object again = open(region, capacity);
        String committedAfter = invoke(committedCount, again);
        line(tag + " reopen -> committed=" + committedAfter + " tail=" + invoke(recovered, again)
                + (committedAfter.equals(Integer.toString(capacity)) ? "" : " UNEXPECTED"));
        MessageDigest reads = sha256();
        for (long i = 0; i < capacity; i++) reads.update(invoke(read, again, i).getBytes(StandardCharsets.UTF_8));
        line(tag + " read digest=" + HexFormat.of().formatHex(reads.digest(), 0, 8) + " read(-1) -> " + invoke(read, again, -1L)
                + " read(capacity) -> " + invoke(read, again, (long) capacity));
        line(tag + " smaller-capacity -> committed=" + invoke(committedCount, open(region, 100)));
        closeRegion(region);
    }

    private void corruptionLane(String kind) throws IOException {
        String tag = "CORRUPT " + kind;
        int capacity = 8;
        int[] offsets = {0, 3, 4, 5, 6, 7, 8, 40, 72, 103, 104, 135, 136, 143};
        int[] values = {0, 4, 255};
        for (int at : offsets) {
            for (int value : (at == 5 ? values : new int[] {-1})) {
                Region region = region(kind, 0, capacity);
                if (region == null) return;
                Object log = open(region, capacity);
                for (int i = 0; i < 4; i++) call(append, log, statuses[i % 3], identity("t", i), identity("s", i), identity("r", i));
                byte[] frame = new byte[RECORD];
                Method readRegion = method(regionType, "read", long.class, byte[].class, int.class, int.class);
                Method writeRegion = method(regionType, "write", long.class, byte[].class, int.class, int.class);
                long record = region.origin() + RECORD;
                call(readRegion, region.handle(), record, frame, 0, RECORD);
                frame[at] = value < 0 ? (byte) (frame[at] ^ 0x5a) : (byte) value;
                call(writeRegion, region.handle(), record, frame, 0, RECORD);
                Object reopened = open(region, capacity);
                line(tag + " record1@" + at + (value < 0 ? "^5a" : "=" + value) + " -> " + (reopened instanceof Refusal r ? r.text()
                        : "committed=" + invoke(committedCount, reopened) + " tail=" + invoke(recovered, reopened)));
                closeRegion(region);
            }
        }
        Region region = region(kind, 0, capacity);
        if (region == null) return;
        Object log = open(region, capacity);
        for (int i = 0; i < 2; i++) call(append, log, statuses[0], identity("t", i), identity("s", i), identity("r", i));
        Method writeRegion = method(regionType, "write", long.class, byte[].class, int.class, int.class);
        call(writeRegion, region.handle(), region.origin() + 5L * RECORD + 17, new byte[] {1}, 0, 1);
        line(tag + " beyond-gap -> " + format(open(region, capacity)));
        call(writeRegion, region.handle(), region.origin() + 5L * RECORD + 17, new byte[] {0}, 0, 1);
        call(writeRegion, region.handle(), region.origin() + RECORD + 136, new byte[8], 0, 8);
        Object tail = open(region, capacity);
        line(tag + " zero-marker -> " + (tail instanceof Refusal r ? r.text() : "committed=" + invoke(committedCount, tail)
                + " tail=" + invoke(recovered, tail) + " append=" + invoke(append, tail, statuses[1], identity("x", 1),
                identity("s", 1), identity("r", 1)) + " reopen=" + invoke(committedCount, open(region, capacity))));
        closeRegion(region);
    }

    private void interruptedLane() throws IOException {
        for (int step = 1; step < STEPS.length; step++) {
            Region region = region("memory", 0, 3);
            Object log = open(region, 3);
            call(append, log, statuses[0], identity("a", 0), identity("s", 0), identity("r", 0));
            region.memory().step = 0;
            region.memory().failStep = step;
            String failed = invoke(append, log, statuses[1], identity("a", 1), identity("s", 1), identity("r", 1));
            String after = invoke(append, log, statuses[2], identity("a", 2), identity("s", 2), identity("r", 2));
            Object reopened = open(region, 3);
            String state = reopened instanceof Refusal r ? r.text() : "committed=" + invoke(committedCount, reopened) + " tail="
                    + invoke(recovered, reopened) + " resume=" + invoke(append, reopened, statuses[2], identity("a", 2),
                    identity("s", 2), identity("r", 2)) + " final=" + invoke(committedCount, open(region, 3));
            line("INTERRUPT " + STEPS[step] + " failed=" + failed + " after=" + after + " reopen " + state
                    + " persists=" + region.memory().persists);
        }
    }

    private Object logMonitor(long throwAt, long[] seen) {
        return Proxy.newProxyInstance(logMonitorType.getClassLoader(), new Class<?>[] {logMonitorType}, (proxy, method, args) -> {
            if (!method.getName().equals("checkpoint")) return method.getName().equals("hashCode") ? 0 : null;
            seen[0]++;
            if (seen[0] == throwAt) throw new IllegalStateException("cancelled at " + args[0] + "/" + args[1]);
            return null;
        });
    }

    private void monitorLane() throws IOException {
        Region region = region("memory", 0, 50);
        Object log = open(region, 50);
        for (int i = 0; i < 20; i++) call(append, log, statuses[i % 3], identity("m", i), identity("s", i), identity("r", i));
        for (long at : new long[] {1, 10, 21, 50, 51, 1_000}) {
            long[] seen = {0};
            Object reopened = call(logOpen, null, region.handle(), region.origin(), 50L, logMonitor(at, seen));
            line("MONITOR open throwAt=" + at + " -> " + format(reopened) + " checkpoints=" + seen[0]);
            if (!(reopened instanceof Refusal)) {
                long[] more = {0};
                Object again = call(logOpen, null, region.handle(), region.origin(), 50L, logMonitor(Long.MAX_VALUE, more));
                line("MONITOR read(5) -> " + invoke(read, again, 5L) + " append -> " + invoke(append, again, statuses[0],
                        identity("m", 99), identity("s", 99), identity("r", 99)) + " checkpoints=" + more[0]);
            }
        }
    }

    private Object reader(Map<String, byte[]> store, String mode) {
        return Proxy.newProxyInstance(readerType.getClassLoader(), new Class<?>[] {readerType}, (proxy, method, args) -> {
            if (!method.getName().equals("read")) return method.getName().equals("hashCode") ? 0 : null;
            byte[] content = store.get((String) args[0]);
            return switch (mode) {
                case "null" -> null;
                case "tampered" -> "tampered".getBytes(StandardCharsets.UTF_8);
                case "oversize" -> new byte[64 * 1024 + 1];
                case "throw" -> throw new IOException("content store unavailable");
                default -> content == null ? null : content.clone();
            };
        });
    }

    private Object replayMonitor(long throwAt) {
        long[] seen = {0};
        return Proxy.newProxyInstance(replayMonitorType.getClassLoader(), new Class<?>[] {replayMonitorType}, (proxy, method, args) -> {
            if (!method.getName().equals("checkpoint")) return method.getName().equals("hashCode") ? 0 : null;
            if (++seen[0] == throwAt) throw new IOException("cancelled");
            return null;
        });
    }

    private List<Object> expectedList(List<String[]> tasks) {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) list.add(construct(expected, (long) i, tasks.get(i)[0], tasks.get(i)[1]));
        return list;
    }

    private void replayLane() throws IOException {
        int capacity = 2_000;
        Region region = region("memory", 0, capacity);
        Object log = open(region, capacity);
        Map<String, byte[]> store = new HashMap<>();
        List<String[]> tasks = new ArrayList<>();
        for (int i = 0; i < capacity; i++) {
            byte[] content = ("candidate " + i).getBytes(StandardCharsets.UTF_8);
            String result = hex(content);
            store.put(result, content);
            String[] task = {identity("task", i), identity("source", i % 3), result};
            tasks.add(task);
            if (i < 1_500) call(append, log, statuses[i % 3], task[0], task[1], task[2]);
        }
        Object reopened = open(region, capacity);
        List<Object> reviewed = expectedList(tasks);
        line("REPLAY full -> " + invoke(reconcile, null, reopened, reviewed, reader(store, "store"), replayMonitor(-1)));
        line("REPLAY prefix-1500 -> " + invoke(reconcile, null, reopened, reviewed.subList(0, 1_500), reader(store, "store"), replayMonitor(-1)));
        line("REPLAY short-1499 -> " + invoke(reconcile, null, reopened, reviewed.subList(0, 1_499), reader(store, "store"), replayMonitor(-1)));
        line("REPLAY empty -> " + invoke(reconcile, null, reopened, List.of(), reader(store, "store"), replayMonitor(-1)));
        for (String mode : List.of("null", "tampered", "oversize", "throw"))
            line("REPLAY content-" + mode + " -> " + invoke(reconcile, null, reopened, reviewed, reader(store, mode), replayMonitor(-1)));
        line("REPLAY cancel-400 -> " + invoke(reconcile, null, reopened, reviewed, reader(store, "store"), replayMonitor(401)));
        line("REPLAY nulls -> " + invoke(reconcile, null, null, reviewed, reader(store, "store"), replayMonitor(-1)) + " | "
                + invoke(reconcile, null, reopened, null, reader(store, "store"), replayMonitor(-1)) + " | "
                + invoke(reconcile, null, reopened, reviewed, null, replayMonitor(-1)) + " | "
                + invoke(reconcile, null, reopened, reviewed, reader(store, "store"), null));
        List<Object> over = new ArrayList<>(reviewed);
        over.add(construct(expected, (long) capacity, identity("task", capacity), identity("source", 0)));
        line("REPLAY over-capacity -> " + invoke(reconcile, null, reopened, over, reader(store, "store"), replayMonitor(-1)));
        driftCases(reopened, reviewed, tasks, store);
        line("EXPECTED refusals ordinal=" + format(construct(expected, -1L, identity("t", 0), identity("s", 0))) + " task="
                + format(construct(expected, 0L, "BAD", identity("s", 0))) + " source="
                + format(construct(expected, 0L, identity("t", 0), null)) + " upper="
                + format(construct(expected, 0L, identity("t", 0).toUpperCase(java.util.Locale.ROOT), identity("s", 0))));
    }

    private void driftCases(Object log, List<Object> reviewed, List<String[]> tasks, Map<String, byte[]> store) {
        int[] positions = {0, 10, 1_499, 1_500, 1_999};
        for (int at : positions) {
            List<Object> drift = new ArrayList<>(reviewed);
            drift.set(at, construct(expected, (long) at, identity("other", at), tasks.get(at)[1]));
            String task = invoke(reconcile, null, log, drift, reader(store, "store"), replayMonitor(-1));
            drift.set(at, construct(expected, (long) at, tasks.get(at)[0], identity("other-source", at)));
            String source = invoke(reconcile, null, log, drift, reader(store, "store"), replayMonitor(-1));
            drift.set(at, construct(expected, (long) at + 1, tasks.get(at)[0], tasks.get(at)[1]));
            String ordinal = invoke(reconcile, null, log, drift, reader(store, "store"), replayMonitor(-1));
            drift.set(at, construct(expected, (long) at, tasks.get(at == 0 ? 1 : at - 1)[0], tasks.get(at)[1]));
            String duplicate = invoke(reconcile, null, log, drift, reader(store, "store"), replayMonitor(-1));
            line("DRIFT @" + at + " task=" + task + " source=" + source + " ordinal=" + ordinal + " duplicate=" + duplicate);
        }
    }

    // ---------------------------------------------------------------- seeded random operations

    private final class Lane {
        final String kind;
        final Region region;
        final int capacity;
        final List<String[]> committed = new ArrayList<>();
        final Map<String, byte[]> store = new HashMap<>();
        Object log;
        String[] pending;

        Lane(String kind, Region region, int capacity) {
            this.kind = kind;
            this.region = region;
            this.capacity = capacity;
            this.log = open(region, capacity);
        }
    }

    private void random(int count) throws IOException {
        Random rng = new Random(SEED);
        List<Lane> lanes = new ArrayList<>();
        lanes.add(new Lane("memory", region("memory", 288, 6_000), 6_000));
        lanes.add(new Lane("file", region("file", 0, 3_000), 3_000));
        if (nativeMode) lanes.add(new Lane("posix-sparse", region("posix-sparse", 144, 3_000), 3_000));
        long serial = 0;
        for (int n = 0; n < count; n++) {
            Lane lane = lanes.get(rng.nextInt(lanes.size()));
            int op = rng.nextInt(100);
            String result;
            if (op < 55) {
                result = randomAppend(rng, lane, serial++);
            } else if (op < 75) {
                long ordinal = rng.nextInt(lane.committed.size() + 3) - 1;
                result = "read " + ordinal + " " + invoke(read, lane.log, ordinal);
            } else if (op < 83) {
                result = reopen(lane);
            } else if (op < 93) {
                result = randomReconcile(rng, lane);
            } else if (op < 97 && lane.region.memory() != null) {
                lane.region.memory().step = 0;
                lane.region.memory().failStep = 1 + rng.nextInt(4);
                result = "inject " + STEPS[lane.region.memory().failStep] + " " + randomAppend(rng, lane, serial++);
            } else {
                result = corruptAndRestore(rng, lane);
            }
            line("R " + n + " " + lane.kind + " -> " + result);
        }
        line("RANDOM operations=" + count);
        for (Lane lane : lanes) closeRegion(lane.region);
    }

    private String randomAppend(Random rng, Lane lane, long serial) {
        byte[] content = new byte[rng.nextInt(64)];
        rng.nextBytes(content);
        String result = hex(content);
        String task = identity("random-task", serial);
        String source = identity("random-source", rng.nextInt(16));
        boolean invalid = rng.nextInt(25) == 0;
        Object status = statuses[rng.nextInt(statuses.length)];
        Object receipt = call(append, lane.log, status, invalid ? task.substring(2) : task, source, result);
        if (!(receipt instanceof Refusal)) {
            lane.committed.add(new String[] {task, source, result});
            lane.store.put(result, content);
        } else if (!invalid) {
            lane.pending = new String[] {task, source, result};
            lane.store.put(result, content);
        }
        return "append " + format(receipt);
    }

    private String reopen(Lane lane) {
        Object reopened = open(lane.region, lane.capacity);
        if (reopened instanceof Refusal r) return "reopen " + r.text();
        lane.log = reopened;
        long count = (Long) call(committedCount, reopened);
        if (lane.pending != null && count == lane.committed.size() + 1) lane.committed.add(lane.pending);
        lane.pending = null;
        return "reopen committed=" + count + " tracked=" + lane.committed.size() + " tail=" + invoke(recovered, reopened)
                + (count == lane.committed.size() ? "" : " UNEXPECTED");
    }

    private String randomReconcile(Random rng, Lane lane) {
        List<String[]> tasks = new ArrayList<>(lane.committed);
        int extra = rng.nextInt(4);
        for (int i = 0; i < extra; i++) tasks.add(new String[] {identity("future", tasks.size() * 31L + i), identity("s", i), ""});
        if (tasks.isEmpty() || tasks.size() > lane.capacity) return "reconcile skipped size=" + tasks.size();
        String mode = rng.nextInt(10) == 0 ? "tampered" : "store";
        return "reconcile " + mode + " " + invoke(reconcile, null, lane.log, expectedList(tasks), reader(lane.store, mode),
                replayMonitor(rng.nextInt(20) == 0 ? 1 + rng.nextInt(Math.max(1, tasks.size())) : -1));
    }

    private String corruptAndRestore(Random rng, Lane lane) {
        if (lane.committed.isEmpty()) return "corrupt skipped";
        Method readRegion = method(regionType, "read", long.class, byte[].class, int.class, int.class);
        Method writeRegion = method(regionType, "write", long.class, byte[].class, int.class, int.class);
        long record = lane.region.origin() + (long) rng.nextInt(lane.committed.size()) * RECORD;
        int at = rng.nextInt(RECORD);
        byte[] one = new byte[1];
        call(readRegion, lane.region.handle(), record + at, one, 0, 1);
        byte original = one[0];
        if (lane.region.memory() != null) lane.region.memory().failStep = 0;
        call(writeRegion, lane.region.handle(), record + at, new byte[] {(byte) (original ^ 0x11)}, 0, 1);
        String corrupted = format(open(lane.region, lane.capacity));
        call(writeRegion, lane.region.handle(), record + at, new byte[] {original}, 0, 1);
        return "corrupt +" + at + " -> " + corrupted + " restored " + reopen(lane);
    }

    private static void deleteTree(Path tree) throws IOException {
        try (Stream<Path> walk = Files.walk(tree)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
}
