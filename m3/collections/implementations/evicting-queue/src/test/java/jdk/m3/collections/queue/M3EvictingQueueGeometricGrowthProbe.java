// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections.queue;

import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.SplittableRandom;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * Differential proof for the geometric-growth atom (qualification/collections-atoms-20261106, com.synexia crate
 * m3jdk21-evicting-queue-geometric-growth-20261024).
 *
 * <p>Prints the growth sequence of the live private {@code expandedLength} from capacity 1 past {@code 1 << 26}
 * and checks the rule (jump {@code oldLength + 2} below 64, {@code oldLength} from 64), checks the physical slot
 * counts of the donor table GROWTH_ALLOCATION_COMPARISON.tsv for the live class (candidate column) and for the
 * pre-change class (baseline column; master blob e6f8b47da01c compiled at run time from the hash-pinned fixture
 * into a separate package and class loader), then runs {@value #OPS} seeded add/poll/peek/remove/iterate/toArray/
 * iterator-remove/addAll operations over queues of several limits on both classes and requires identical dumps.
 * Run: {@code java -ea -cp target/classes:target/test-classes jdk.m3.collections.queue.M3EvictingQueueGeometricGrowthProbe}</p>
 */
public final class M3EvictingQueueGeometricGrowthProbe {
    private static final long SEED = 0x4D33_4556_4943_5451L;
    private static final int OPS = 1_000_000;
    private static final int TABLE_LIMIT = 1 << 26;
    private static final int TABLE_MAX_SIZE = 1_000_000;
    private static final int ARRAY_LIMIT = Integer.MAX_VALUE - 8;
    /** GROWTH_ALLOCATION_COMPARISON.tsv rows: occupancy, baseline_physical_slots, candidate_physical_slots. */
    private static final int[][] DONOR_TABLE = {
        {1024, 1188, 1120}, {65536, 68485, 71680}, {1_000_000, 1_000_001, 1_000_001}};
    private static final int[] QUEUE_LIMITS = {0, 1, 2, 15, 16, 63, 64, 65, 1000, 1 << 16};
    private static final int VALUES = 512;
    private static final String FIXTURE = "/jdk/m3/collections/queue/atoms/M3EvictingQueue.preimage.java.txt";
    private static final String FIXTURE_SHA256 = "55f35c295abb90edd459ecfbe36b6a03f08c9ee212716d5921ac0d55c88c0aa6";
    private static final String OWNER_PACKAGE_LINE = "package jdk.m3.collections.queue;";
    private static final String PREIMAGE_PACKAGE = "jdk.m3.collections.queue.atoms.preimage";

    private M3EvictingQueueGeometricGrowthProbe() { }

    public static void main(String[] ignored) throws Exception {
        Class<?> live = M3EvictingQueue.class;
        Class<?> preimage = compilePreimage();
        require(live != preimage, "pre-change class must be a distinct binary identity");
        growthTable(live, "live", true);
        growthTable(preimage, "preimage", false);
        donorTable(live, preimage);
        String liveDump = dump(live);
        String preimageDump = dump(preimage);
        require(liveDump.equals(preimageDump), "behavioural dumps differ: live " + liveDump + " preimage " + preimageDump);
        System.out.println("M3_EVICTING_QUEUE_GEOMETRIC_GROWTH_PASS ops=" + OPS + " seed=" + Long.toHexString(SEED)
                + " live=" + liveDump + " preimage=" + preimageDump);
    }

    private static void growthTable(Class<?> type, String label, boolean checkRule) throws Exception {
        Queue<Object> queue = create(type, Integer.MAX_VALUE);
        Method expanded = type.getDeclaredMethod("expandedLength", int.class);
        expanded.setAccessible(true);
        StringBuilder table = new StringBuilder(label).append(" growth: capacity -> expandedLength\n");
        int capacity = 1;
        int steps = 0;
        while (capacity < TABLE_LIMIT) {
            int next = ((Integer) expanded.invoke(queue, capacity)).intValue();
            require(next > capacity, label + " growth must increase at " + capacity);
            if (checkRule) {
                long expected = capacity < 64 ? 2L * capacity + 2 : 2L * capacity;
                require(next == Math.min(expected, ARRAY_LIMIT), label + " growth rule at " + capacity + ": " + next);
            }
            table.append(capacity).append(" -> ").append(next).append('\n');
            capacity = next;
            steps++;
        }
        System.out.print(table);
        System.out.println(label + " steps from 1 past " + TABLE_LIMIT + ": " + steps + ", final capacity " + capacity);
    }

    private static void donorTable(Class<?> live, Class<?> preimage) throws Exception {
        for (int[] row : DONOR_TABLE) {
            int liveSlots = slotsAfterFill(live, row[0]);
            int preimageSlots = slotsAfterFill(preimage, row[0]);
            System.out.println("donor row occupancy=" + row[0] + " baseline=" + row[1] + " candidate=" + row[2]
                    + " preimage=" + preimageSlots + " live=" + liveSlots);
            require(liveSlots == row[2], "live physical slots at " + row[0] + ": " + liveSlots + " expected " + row[2]);
            require(preimageSlots == row[1],
                    "preimage physical slots at " + row[0] + ": " + preimageSlots + " expected " + row[1]);
        }
    }

    private static int slotsAfterFill(Class<?> type, int occupancy) throws Exception {
        Queue<Object> queue = create(type, TABLE_MAX_SIZE);
        for (int i = 0; i < occupancy; i++) {
            queue.add(Integer.valueOf(i));
        }
        require(queue.size() == occupancy, "size after fill of " + occupancy);
        Field elements = type.getDeclaredField("elements");
        elements.setAccessible(true);
        return ((Object[]) elements.get(queue)).length;
    }

    @SuppressWarnings("unchecked")
    private static Queue<Object> create(Class<?> type, int maxSize) throws Exception {
        return (Queue<Object>) type.getMethod("create", int.class).invoke(null, maxSize);
    }

    private static String dump(Class<?> type) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        SplittableRandom random = new SplittableRandom(SEED);
        Method remaining = type.getMethod("remainingCapacity");
        List<Queue<Object>> queues = new ArrayList<>();
        for (int limit : QUEUE_LIMITS) {
            queues.add(create(type, limit));
        }
        byte[] record = new byte[13];
        for (int i = 0; i < OPS; i++) {
            int index = random.nextInt(queues.size());
            Queue<Object> queue = queues.get(index);
            int op = random.nextInt(32);
            long result;
            if (op < 18) {
                int value = random.nextInt(VALUES);
                result = queue.add(Integer.valueOf(value)) ? value : -1;
            } else if (op < 23) {
                Object polled = queue.poll();
                result = polled == null ? -1 : ((Integer) polled).longValue();
            } else if (op == 23) {
                Object head = queue.peek();
                result = head == null ? -1 : ((Integer) head).longValue();
            } else if (op < 26) {
                result = queue.remove(Integer.valueOf(random.nextInt(VALUES))) ? 1 : 0;
            } else if (op == 26) {
                int left = ((Integer) remaining.invoke(queue)).intValue();
                result = ((long) queue.size() << 32) | (left & 0xFFFF_FFFFL);
            } else if (op < 29) {
                long hash = 17;
                for (Object element : queue) {
                    hash = hash * 31 + ((Integer) element).intValue();
                }
                result = hash;
            } else if (op == 29) {
                Object[] array = queue.toArray();
                long hash = array.length;
                for (Object element : array) {
                    hash = hash * 31 + ((Integer) element).intValue();
                }
                result = hash;
            } else if (op == 30) {
                int size = queue.size();
                if (size == 0) {
                    result = -1;
                } else {
                    int target = random.nextInt(size);
                    Iterator<Object> cursor = queue.iterator();
                    Object removed = null;
                    for (int step = 0; step <= target; step++) {
                        removed = cursor.next();
                    }
                    cursor.remove();
                    result = ((Integer) removed).longValue();
                }
            } else {
                int count = random.nextInt(1, 18);
                List<Object> batch = new ArrayList<>(count);
                for (int k = 0; k < count; k++) {
                    batch.add(Integer.valueOf(random.nextInt(VALUES)));
                }
                result = queue.addAll(batch) ? count : -1;
            }
            record[0] = (byte) op;
            putInt(record, 1, index);
            putLong(record, 5, result);
            digest.update(record);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void putInt(byte[] record, int at, int value) {
        record[at] = (byte) (value >>> 24);
        record[at + 1] = (byte) (value >>> 16);
        record[at + 2] = (byte) (value >>> 8);
        record[at + 3] = (byte) value;
    }

    private static void putLong(byte[] record, int at, long value) {
        putInt(record, at, (int) (value >>> 32));
        putInt(record, at + 4, (int) value);
    }

    private static Class<?> compilePreimage() throws Exception {
        byte[] source;
        try (InputStream in = M3EvictingQueueGeometricGrowthProbe.class.getResourceAsStream(FIXTURE)) {
            require(in != null, "fixture missing: " + FIXTURE);
            source = in.readAllBytes();
        }
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source));
        require(FIXTURE_SHA256.equals(digest), "fixture digest " + digest + " is not master blob e6f8b47da01c");
        String text = new String(source, StandardCharsets.UTF_8);
        require(text.contains(OWNER_PACKAGE_LINE), "fixture package line");
        String rewritten = text.replace(OWNER_PACKAGE_LINE, "package " + PREIMAGE_PACKAGE + ";");
        Path out = Files.createTempDirectory("m3-evicting-preimage");
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        require(javac != null, "a JDK compiler is required");
        JavaFileObject unit = new SimpleJavaFileObject(
                URI.create("string:///" + PREIMAGE_PACKAGE.replace('.', '/') + "/M3EvictingQueue.java"),
                JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return rewritten; }
        };
        StringWriter diagnostics = new StringWriter();
        Boolean ok = javac.getTask(diagnostics, null, null,
                List.of("-d", out.toString(), "--release", "21", "-Xlint:all", "-Werror", "-proc:none"),
                null, List.of(unit)).call();
        require(Boolean.TRUE.equals(ok), "preimage compile failed: " + diagnostics);
        URLClassLoader loader = new URLClassLoader(new URL[] {out.toUri().toURL()},
                M3EvictingQueue.class.getClassLoader());
        return Class.forName(PREIMAGE_PACKAGE + ".M3EvictingQueue", true, loader);
    }

    private static void require(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
    }
}
