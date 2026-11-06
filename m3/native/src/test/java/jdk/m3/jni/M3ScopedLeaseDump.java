// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.jni;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Reflective dump driver of the differential gate (recipe/scoped-leases-20261106/differential.sh), not a test.
 * It replays the same inputs through a lease family named by its package (the donor family compiled from the donor
 * tree, or this module's jdk.m3.jni) and prints every observable result: the 1,025 concat parity cases of the donor
 * probe on the Java path, the deterministic full scan and window scans of the donor self-tests, and a seeded stream
 * of random lease, pin, pool, JNI-borrow, scan, concat and scope operations with their results or refusals. Two
 * dumps of the same inputs must be byte-identical. Package names inside messages are erased so the two sides print
 * the same text. usage: java -cp &lt;classes&gt; jdk.m3.jni.M3ScopedLeaseDump &lt;package&gt; [operations=100000]
 */
//FLLDCIM: RELOCATE - differential driver of recipe m3/native/recipe/scoped-leases-20261106 (new, test scope)
public final class M3ScopedLeaseDump {
    static final int CONCAT_CASES = 1025;
    static final long CONCAT_SEED = 0x4D334A4E494CL;
    static final long SCAN_SEED = 0x4d335f6c696665L;
    static final long WINDOW_SEED = 0x4d33_2026_1014L;
    static final long RANDOM_SEED = 0x4d33_6c65_6173_65L;
    static final int DEFAULT_OPERATIONS = 100_000;
    static final int[] WINDOW_CAPS = {1, 2, 7, 127, 128, 129, 1024};
    private static final int INLINE_IDS = 16;
    private static final int OPERATION_KINDS = 26;
    private static final int COUNTER_PERIOD = 1000;

    /** A refusal observed through reflection, printed as class and message. */
    record Failure(Throwable cause) { }

    private final String pkg;
    private final PrintStream out;
    private final Class<?> table;
    private final Class<?> scan;
    private final Class<?> window;
    private final Class<?> scopes;
    private final Class<?> references;
    private final Class<?> concat;

    public M3ScopedLeaseDump(String pkg, PrintStream out) throws ClassNotFoundException {
        this.pkg = pkg;
        this.out = out;
        table = Class.forName(pkg + ".M3ScopedLeaseTable");
        scan = Class.forName(pkg + ".M3NativeLeaseScan");
        window = Class.forName(pkg + ".M3LeaseWindowScan");
        scopes = Class.forName(pkg + ".M3LeaseScopes");
        references = Class.forName(pkg + ".M3JniArrayReferences");
        concat = Class.forName(pkg + ".M3JniByteConcat");
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) throw new IllegalArgumentException("<package> [operations]");
        int operations = args.length == 2 ? Integer.parseInt(args[1]) : DEFAULT_OPERATIONS;
        M3ScopedLeaseDump dump = new M3ScopedLeaseDump(args[0], System.out);
        dump.out.println("M3ScopedLeaseDump nativeScan=" + dump.staticCall(dump.scan, "nativeAvailable")
                + " nativeConcat=" + dump.staticCall(dump.concat, "nativeLibraryLoaded")
                + " maxScopedArrays=" + dump.references.getField("MAX_SCOPED_ARRAYS").get(null));
        int cases = dump.concatParity();
        dump.deterministicScan();
        dump.windowScans();
        dump.randomOperations(operations);
        dump.out.println("TOTAL concat_cases=" + cases + " window_caps=" + WINDOW_CAPS.length
                + " operations=" + operations);
    }

    // ---- reflection plumbing -------------------------------------------------------------------------------------

    private Method method(Class<?> type, String name, Class<?>... params) {
        try {
            Method found = type.getDeclaredMethod(name, params);
            found.setAccessible(true);
            return found;
        } catch (NoSuchMethodException missing) {
            throw new IllegalStateException(type.getName() + "." + name, missing);
        }
    }

    /** The result of the call, or a Failure holding what the callee threw. */
    private Object call(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException thrown) {
            return new Failure(thrown.getCause());
        } catch (IllegalAccessException | IllegalArgumentException wrong) {
            throw new IllegalStateException(method.toString(), wrong);
        }
    }

    private Object tableCall(Object instance, String name, Class<?>[] params, Object... args) {
        return call(method(table, name, params), instance, args);
    }

    private Object idCall(Object instance, String name, long id) {
        return tableCall(instance, name, new Class<?>[] {long.class}, id);
    }

    private Object staticCall(Class<?> type, String name) {
        return call(method(type, name), null);
    }

    private Object newTable() {
        try {
            return table.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private long allocate(Object instance) {
        return (Long) tableCall(instance, "allocate", new Class<?>[0]);
    }

    private Object register(Object instance, long id, Object array) {
        return tableCall(instance, "registerJniArray", new Class<?>[] {long.class, Object.class}, id, array);
    }

    private Object concat(Object instance, long leftId, byte[] left, long rightId, byte[] right, boolean prefer) {
        Method m = method(concat, "concat", table, long.class, byte[].class, long.class, byte[].class, boolean.class);
        return call(m, null, instance, leftId, left, rightId, right, prefer);
    }

    // ---- text -----------------------------------------------------------------------------------------------------

    static String sha16(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)).substring(0, 16);
        } catch (NoSuchAlgorithmException missing) {
            throw new IllegalStateException(missing);
        }
    }

    static byte[] bytesOf(long[] values, int count) {
        ByteBuffer buffer = ByteBuffer.allocate(count * Long.BYTES);
        for (int i = 0; i < count; i++) buffer.putLong(values[i]);
        return buffer.array();
    }

    String text(Object value) {
        if (value instanceof Failure failure) {
            Throwable cause = failure.cause();
            String message = cause.getMessage() == null ? "" : cause.getMessage().replace(pkg + ".", "");
            return "!" + cause.getClass().getSimpleName() + "(" + message + ")";
        }
        if (value instanceof byte[] bytes) return "bytes[" + bytes.length + "]:" + sha16(bytes);
        if (value instanceof long[] ids) return ids(ids, ids.length);
        return String.valueOf(value);
    }

    static String ids(long[] values, int count) {
        if (count <= INLINE_IDS) return Arrays.toString(Arrays.copyOf(values, count));
        return "long[" + count + "]:" + sha16(bytesOf(values, count));
    }

    // ---- section A: the probe's 1,025 concat cases on the Java path ---------------------------------------------

    /** The donor probe's inputs (seed, lengths, duplicate case) through concat(false) and concat(true). */
    public int concatParity() {
        Random random = new Random(CONCAT_SEED);
        int cases = 0;
        for (int n = 0; n < CONCAT_CASES - 1; n++) {
            Object instance = newTable();
            long leftId = allocate(instance);
            long rightId = allocate(instance);
            byte[] left = new byte[n % 17 == 0 ? n + 1031 : n % 102];
            byte[] right = new byte[n % 19 == 0 ? n + 4103 : n % 89];
            random.nextBytes(left);
            random.nextBytes(right);
            register(instance, leftId, left);
            register(instance, rightId, right);
            cases += concatCase(instance, "n=" + n, leftId, left, rightId, right);
            out.println("close " + text(tableCall(instance, "close", new Class<?>[0])));
        }
        Object instance = newTable();
        long id = allocate(instance);
        byte[] array = {9, -1, 0, 120};
        register(instance, id, array);
        cases += concatCase(instance, "duplicate", id, array, id, array);
        out.println("CONCAT_PARITY cases=" + cases + " native=" + staticCall(concat, "nativeLibraryLoaded"));
        return cases;
    }

    private int concatCase(Object instance, String label, long leftId, byte[] left, long rightId, byte[] right) {
        byte[] expected = Arrays.copyOf(left, left.length + right.length);
        System.arraycopy(right, 0, expected, left.length, right.length);
        Object java = concat(instance, leftId, left, rightId, right, false);
        Object preferred = concat(instance, leftId, left, rightId, right, true);
        boolean equal = java instanceof byte[] j && Arrays.equals(j, expected) && j != left && j != right
                && preferred instanceof byte[] p && Arrays.equals(p, expected) && p != left && p != right;
        out.println("concat " + label + " left=" + left.length + " right=" + right.length + " java=" + text(java)
                + " pref=" + text(preferred) + " eq=" + equal + " borrows=" + text(idCall(instance, "jniArrayBorrowCount", leftId))
                + "," + text(idCall(instance, "jniArrayBorrowCount", rightId)) + " pins=" + text(idCall(instance, "pinCount", leftId))
                + "," + text(idCall(instance, "pinCount", rightId)) + " eligible=" + text(idCall(instance, "eligible", leftId))
                + "," + text(idCall(instance, "eligible", rightId)));
        if (!equal) throw new AssertionError("concat parity " + label);
        return 1;
    }

    // ---- section B: the self-tests' deterministic scans ----------------------------------------------------------

    /** M3ScopedLeaseTableSelfTest.deterministicScan: 1,000 slots, seeded acquire/pin/dirty, full scans. */
    public void deterministicScan() {
        Random random = new Random(SCAN_SEED);
        Object instance = newTable();
        int count = 1000;
        for (int i = 0; i < count; i++) allocate(instance);
        for (long id = 1; id <= count; id++) {
            switch (random.nextInt(4)) {
                case 0 -> idCall(instance, "acquire", id);
                case 1 -> idCall(instance, "pin", id);
                case 2 -> idCall(instance, "markDirty", id);
                default -> { }
            }
        }
        long[] java = new long[count];
        long[] preferred = new long[count];
        Class<?>[] params = {long[].class, boolean.class};
        Object a = tableCall(instance, "collectEligible", params, java, false);
        Object b = tableCall(instance, "collectEligible", params, preferred, true);
        out.println("scan java=" + a + " pref=" + b + " ids=" + ids(java, (Integer) a) + " prefIds=" + ids(preferred, (Integer) b));
        releaseAll(instance, count, new boolean[count + 1]);
        out.println("scan all=" + tableCall(instance, "collectEligible", params, preferred, true)
                + " close=" + text(tableCall(instance, "close", new Class<?>[0])));
    }

    private void releaseAll(Object instance, int count, boolean[] retired) {
        for (long id = 1; id <= count; id++) {
            if (retired[(int) id]) continue;
            if ((Integer) idCall(instance, "leaseCount", id) != 0) idCall(instance, "release", id);
            if ((Integer) idCall(instance, "pinCount", id) != 0) idCall(instance, "unpin", id);
            idCall(instance, "markCommitted", id);
        }
    }

    /** M3ScopedLeaseWindowSelfTest.sparseAndDenseParity: 65,536 slots, every cap, Java and preferred paths. */
    public void windowScans() {
        Random random = new Random(WINDOW_SEED);
        Object instance = newTable();
        int count = 65536;
        boolean[] retired = new boolean[count + 1];
        for (int i = 0; i < count; i++) {
            long id = allocate(instance);
            switch (random.nextInt(6)) {
                case 0 -> idCall(instance, "acquire", id);
                case 1 -> idCall(instance, "pin", id);
                case 2 -> idCall(instance, "markDirty", id);
                case 3 -> retired[(int) id] = (Boolean) idCall(instance, "tryRetire", id);
                default -> { }
            }
        }
        long[] expected = new long[count];
        int full = (Integer) tableCall(instance, "collectEligible", new Class<?>[] {long[].class}, expected);
        out.println("window full=" + full + " ids=" + ids(expected, full));
        for (int cap : WINDOW_CAPS) {
            out.println("window cap=" + cap + " java=" + windowCollect(instance, count, cap, false)
                    + " pref=" + windowCollect(instance, count, cap, true));
        }
        releaseAll(instance, count, retired);
        out.println("window close=" + text(tableCall(instance, "close", new Class<?>[0])));
    }

    private String windowCollect(Object instance, int count, int capacity, boolean prefer) {
        Method scanMethod = method(window, "scan", table, int.class, long[].class, boolean.class);
        Method nextIndex = method(window, "nextIndex", long.class);
        Method written = method(window, "written", long.class);
        long[] buffer = new long[capacity];
        long[] received = new long[count];
        int first = 0;
        int total = 0;
        int steps = 0;
        while (first < count) {
            Object result = call(scanMethod, null, instance, first, buffer, prefer);
            if (result instanceof Failure) return "steps=" + steps + " " + text(result);
            int next = (Integer) call(nextIndex, null, result);
            int n = (Integer) call(written, null, result);
            System.arraycopy(buffer, 0, received, total, n);
            total += n;
            first = next;
            steps++;
        }
        return "steps=" + steps + " total=" + total + " ids=" + ids(received, total);
    }

    // ---- section C: seeded random operations ---------------------------------------------------------------------

    /** Every public lane of the family, chosen at random with occasional invalid IDs, each result printed. */
    public void randomOperations(int operations) {
        Random random = new Random(RANDOM_SEED);
        Object instance = newTable();
        List<byte[]> arrays = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            allocate(instance);
            arrays.add(null);
        }
        for (int i = 0; i < operations; i++) {
            out.println("#" + i + " " + operation(random, instance, arrays, random.nextInt(OPERATION_KINDS)));
            if (i % COUNTER_PERIOD == 0) out.println("counters " + counters(instance));
        }
        out.println("RANDOM operations=" + operations + " " + counters(instance)
                + " close=" + text(tableCall(instance, "close", new Class<?>[0])));
    }

    private String counters(Object instance) {
        return "size=" + text(tableCall(instance, "size", new Class<?>[0]))
                + " registered=" + text(tableCall(instance, "registeredJniArrayCount", new Class<?>[0]))
                + " active=" + text(tableCall(instance, "activeJniArrayCount", new Class<?>[0]))
                + " begins=" + text(tableCall(instance, "jniArrayLeaseBegins", new Class<?>[0]));
    }

    private static long pickId(Random random, int size) {
        int kind = random.nextInt(64);
        if (kind == 0) return 0L;
        if (kind == 1) return size + 1L;
        if (kind == 2) return Long.MAX_VALUE;
        return 1L + random.nextInt(size);
    }

    private String operation(Random random, Object instance, List<byte[]> arrays, int kind) {
        long id = pickId(random, arrays.size());
        return switch (kind) {
            case 0 -> allocateOperation(instance, arrays);
            case 1 -> "acquire " + id + " -> " + text(idCall(instance, "acquire", id));
            case 2 -> "release " + id + " -> " + text(idCall(instance, "release", id));
            case 3 -> "pin " + id + " -> " + text(idCall(instance, "pin", id));
            case 4 -> "unpin " + id + " -> " + text(idCall(instance, "unpin", id));
            case 5 -> registerOperation(random, instance, arrays, id);
            case 6 -> "beginJni " + id + " -> " + text(idCall(instance, "beginJniArrayAccess", id));
            case 7 -> "endJni " + id + " -> " + text(idCall(instance, "endJniArrayAccess", id));
            case 8 -> "retainPool " + id + " -> " + text(idCall(instance, "retainPoolReference", id));
            case 9 -> "releasePool " + id + " -> " + text(idCall(instance, "releasePoolReference", id));
            case 10 -> "borrowPool " + id + " -> " + text(idCall(instance, "borrowPoolReference", id));
            case 11 -> "returnPool " + id + " -> " + text(idCall(instance, "returnPoolReference", id));
            case 12 -> "discardPool " + id + " -> " + text(idCall(instance, "discardPoolBorrow", id));
            case 13 -> "markDirty " + id + " -> " + text(idCall(instance, "markDirty", id));
            case 14 -> "markCommitted " + id + " -> " + text(idCall(instance, "markCommitted", id));
            case 15 -> "tryRetire " + id + " -> " + text(idCall(instance, "tryRetire", id));
            case 16 -> "state " + id + " eligible=" + text(idCall(instance, "eligible", id))
                    + " poolReuse=" + text(idCall(instance, "poolReuseEligible", id))
                    + " managed=" + text(idCall(instance, "managedReferenceCount", id))
                    + " pool=" + text(idCall(instance, "poolReferenceCount", id));
            case 17 -> "counts " + id + " leases=" + text(idCall(instance, "leaseCount", id))
                    + " pins=" + text(idCall(instance, "pinCount", id))
                    + " borrows=" + text(idCall(instance, "jniArrayBorrowCount", id));
            case 18 -> collectOperation(random, instance, arrays.size());
            case 19 -> windowOperation(random, instance, arrays.size());
            case 20 -> concatOperation(random, instance, arrays);
            case 21 -> scopeOperation(instance, "withLease", "leaseCount", id);
            case 22 -> scopeOperation(instance, "withPin", "pinCount", id);
            case 23 -> arrayOperation(instance, id);
            case 24 -> concatScopeOperation(random, instance, arrays.size(), id);
            default -> batchOperation(random, instance, arrays.size());
        };
    }

    private String allocateOperation(Object instance, List<byte[]> arrays) {
        Object id = tableCall(instance, "allocate", new Class<?>[0]);
        if (id instanceof Long) arrays.add(null);
        return "allocate -> " + text(id);
    }

    private String registerOperation(Random random, Object instance, List<byte[]> arrays, long id) {
        int shape = random.nextInt(8);
        Object array = shape == 0 ? new Object[2] : shape == 1 ? new Object() : new byte[random.nextInt(49)];
        if (array instanceof byte[] bytes) random.nextBytes(bytes);
        Object result = register(instance, id, array);
        if (!(result instanceof Failure) && array instanceof byte[] bytes && id >= 1 && id <= arrays.size()) {
            arrays.set((int) id - 1, bytes);
        }
        String kind = array instanceof byte[] bytes ? "bytes[" + bytes.length + "]" : array.getClass().getSimpleName();
        return "register " + id + " " + kind + " -> " + text(result);
    }

    private String collectOperation(Random random, Object instance, int size) {
        int extra = random.nextInt(3) - 1;
        boolean prefer = random.nextBoolean();
        long[] output = new long[Math.max(0, size + extra)];
        Object count = tableCall(instance, "collectEligible", new Class<?>[] {long[].class, boolean.class}, output, prefer);
        String written = count instanceof Integer n ? ids(output, n) : text(count);
        return "collect capacity=" + output.length + " pref=" + prefer + " -> " + written;
    }

    private String windowOperation(Random random, Object instance, int size) {
        int first = random.nextInt(size + 2);
        int capacity = random.nextInt(5);
        boolean prefer = random.nextBoolean();
        long[] output = new long[capacity];
        Object packed = call(method(window, "scan", table, int.class, long[].class, boolean.class), null,
                instance, first, output, prefer);
        if (packed instanceof Failure) return "window first=" + first + " cap=" + capacity + " -> " + text(packed);
        int next = (Integer) call(method(window, "nextIndex", long.class), null, packed);
        int n = (Integer) call(method(window, "written", long.class), null, packed);
        return "window first=" + first + " cap=" + capacity + " pref=" + prefer + " -> next=" + next + " ids=" + ids(output, n);
    }

    private static int registeredId(Random random, List<byte[]> arrays) {
        int[] registered = new int[arrays.size()];
        int count = 0;
        for (int i = 0; i < arrays.size(); i++) if (arrays.get(i) != null) registered[count++] = i + 1;
        return count == 0 ? 0 : registered[random.nextInt(count)];
    }

    private String concatOperation(Random random, Object instance, List<byte[]> arrays) {
        int leftId = registeredId(random, arrays);
        int rightId = random.nextInt(4) == 0 ? leftId : registeredId(random, arrays);
        if (leftId == 0 || rightId == 0) return "concat skipped (no registered byte array)";
        boolean prefer = random.nextBoolean();
        byte[] left = arrays.get(leftId - 1);
        byte[] right = arrays.get(rightId - 1);
        Object result = concat(instance, leftId, left, rightId, right, prefer);
        return "concat " + leftId + "+" + rightId + " pref=" + prefer + " -> " + text(result)
                + " borrows=" + text(idCall(instance, "jniArrayBorrowCount", leftId)) + ","
                + text(idCall(instance, "jniArrayBorrowCount", rightId));
    }

    private String scopeOperation(Object instance, String name, String counter, long id) {
        String[] inner = {"unreached"};
        Runnable body = () -> inner[0] = text(idCall(instance, counter, id));
        Object result = call(method(scopes, name, table, long.class, Runnable.class), null, instance, id, body);
        return name + " " + id + " inner=" + inner[0] + " -> " + text(result) + " after=" + text(idCall(instance, counter, id));
    }

    private String arrayOperation(Object instance, long id) {
        String[] inner = {"unreached"};
        Runnable body = () -> inner[0] = text(idCall(instance, "jniArrayBorrowCount", id));
        Object result = call(method(references, "withArray", table, long.class, Runnable.class), null, instance, id, body);
        return "withArray " + id + " inner=" + inner[0] + " -> " + text(result)
                + " after=" + text(idCall(instance, "jniArrayBorrowCount", id));
    }

    private String concatScopeOperation(Random random, Object instance, int size, long leftId) {
        long rightId = pickId(random, size);
        Supplier<String> body = () -> text(idCall(instance, "jniArrayBorrowCount", leftId)) + ","
                + text(idCall(instance, "jniArrayBorrowCount", rightId));
        Object result = call(method(references, "withConcat", table, long.class, long.class, Supplier.class), null,
                instance, leftId, rightId, body);
        return "withConcat " + leftId + "," + rightId + " -> " + text(result)
                + " after=" + text(idCall(instance, "jniArrayBorrowCount", leftId)) + ","
                + text(idCall(instance, "jniArrayBorrowCount", rightId));
    }

    private String batchOperation(Random random, Object instance, int size) {
        long[] batch = new long[random.nextInt(6)];
        for (int i = 0; i < batch.length; i++) batch[i] = pickId(random, size);
        Supplier<String> body = () -> {
            StringBuilder counts = new StringBuilder();
            for (long id : batch) counts.append(text(idCall(instance, "jniArrayBorrowCount", id))).append(',');
            return counts.toString();
        };
        Object result = call(method(references, "withArrayBatch", table, long[].class, Supplier.class), null,
                instance, batch, body);
        return "withArrayBatch " + Arrays.toString(batch) + " -> " + text(result) + " after=" + body.get();
    }
}
