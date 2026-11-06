/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
import jdk.m3.text.M3WordRows;
import jdk.m3.text.M3WordRowsNative;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.LinkOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

/** Independent, deterministic format-3 direct-address/JNI contract proof. */
public final class M3WordRowsContractTest {
    private static long checks;
    private static final int COUNT = 3;
    private static final int ROWS_BASE = 128;
    private static final int MEMBERS = 512;
    private static final int CONCEPTS = 576;
    private static final int SUBJECTS = 640;
    private static final int EXPANSIONS = 704;
    private static final int END = 728;
    private static final int LANGUAGE = 42;
    private static final long FINGERPRINT = 0x1122334455667788L;

    private static final String WORKER_DIRECTORY = "m3.wordrows.contract.workerDirectory";
    private static final String DIRECTORY_PREFIX = "m3-word-row-proof-";
    private static final long WORKER_SECONDS = 60;
    private static final long STOP_SECONDS = 5;
    private static final List<String> FIXTURE_FILES = List.of(
            "fixture.m3rows", "corrupt-8.m3rows", "corrupt-20.m3rows",
            "corrupt-64.m3rows", "corrupt-204.m3rows", "corrupt-184.m3rows",
            "corrupt-12.m3rows");

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message + " check=" + checks);
    }

    private static void fails(Class<? extends Throwable> type, Runnable operation) {
        try {
            operation.run();
            throw new AssertionError("expected exception " + type.getName());
        } catch (Throwable found) {
            check(type.isInstance(found), "wrong exception: " + found);
        }
    }

    private static byte[] fixture() {
        ByteBuffer image = ByteBuffer.allocate(END).order(ByteOrder.BIG_ENDIAN);
        image.putLong(0, M3WordRows.MAGIC);
        image.putInt(8, M3WordRows.FORMAT);
        image.putInt(12, LANGUAGE);
        image.putInt(16, COUNT);
        image.putInt(20, M3WordRows.ROW_BYTES);
        image.putLong(24, FINGERPRINT);
        image.putLong(32, 0x8877665544332211L);
        image.putLong(40, 60L);
        image.putLong(48, 3L);
        image.putInt(56, 2);
        image.putInt(60, 2);
        image.putInt(64, ROWS_BASE);
        image.putInt(68, MEMBERS);
        image.putInt(72, COUNT);
        image.putInt(76, CONCEPTS);
        image.putInt(80, COUNT);
        image.putInt(84, SUBJECTS);
        image.putInt(88, COUNT);
        image.putInt(92, EXPANSIONS);
        image.putInt(96, COUNT);
        image.putInt(100, END);
        image.put(104, (byte) 'e');
        image.put(105, (byte) 'n');
        image.put(106, (byte) 'v');
        image.put(107, (byte) '3');
        String[] values = {"a", "ab", "b"};
        int[] reverseRanks = {0, 2, 1};
        int[] prefixEnds = {2, 2, 3};
        int[] suffixEnds = {1, 3, 3};
        for (int i = 0; i < COUNT; i++) {
            int p = ROWS_BASE + (i << 7);
            image.putInt(p, i * 5 + 5);
            image.putInt(p + 4, values[i].length());
            image.putInt(p + 8, values[i].length());
            image.putInt(p + 12, values[i].length());
            image.putInt(p + 16, values[i].hashCode());
            image.putInt(p + 20, 0x80 + i);
            image.putLong(p + 24, 0x100000000L + i);
            image.putLong(p + 32, 10L * (i + 1));
            image.putInt(p + 40, i + 1);
            image.putInt(p + 44, i);
            image.putInt(p + 48, reverseRanks[i]);
            image.putInt(p + 52, COUNT - i - 1);
            image.putInt(p + 56, prefixEnds[i]);
            image.putInt(p + 60, suffixEnds[i]);
            image.putInt(p + 72, MEMBERS + i * Integer.BYTES);
            image.putInt(p + 76, 1);
            image.putInt(p + 80, CONCEPTS + i * Long.BYTES);
            image.putInt(p + 84, 1);
            image.putInt(p + 88, SUBJECTS + i * Long.BYTES);
            image.putInt(p + 92, 1);
            image.putInt(p + 96, EXPANSIONS + i * Long.BYTES);
            image.putInt(p + 100, 1);
            image.putInt(p + 116, values[i].length() == 1 ? 31 : 961);
            image.putInt(MEMBERS + i * 4, 10 + i);
            image.putLong(CONCEPTS + i * 8, 100 + i);
            image.putLong(SUBJECTS + i * 8, 200 + i);
            image.putLong(EXPANSIONS + i * 8, 300 + i);
        }
        return image.array();
    }

    private static void bad(Path file, byte[] good, int offset, int value) throws Exception {
        byte[] broken = good.clone();
        ByteBuffer.wrap(broken).putInt(offset, value);
        Path corrupt = file.resolveSibling("corrupt-" + offset + ".m3rows");
        if (!FIXTURE_FILES.contains(corrupt.getFileName().toString())) {
            throw new IllegalArgumentException("unknown corrupt fixture offset");
        }
        Files.write(corrupt, broken, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        check(Arrays.equals(broken, Files.readAllBytes(corrupt)), "exact corrupt image bytes");
        check(ByteBuffer.wrap(broken).getInt(offset) == value, "intended corrupt image value");
        try {
            M3WordRows.open(corrupt);
            throw new AssertionError("expected corrupt image refusal at byte " + offset);
        } catch (java.io.IOException expected) {
            checks++;
            check(expected.getClass() == IOException.class, "decoder IOException class");
        }
        check(Arrays.equals(good, Files.readAllBytes(file)), "valid mapped backing remains unchanged");
    }

    private static Path workerDirectory(String value) throws IOException {
        Path base = Path.of(System.getProperty("java.io.tmpdir")).toRealPath();
        Path supplied = Path.of(value);
        if (!supplied.isAbsolute()) throw new IOException("worker directory must be absolute");
        Path dir = supplied.normalize();
        if (!base.equals(dir.getParent())
                || !dir.getFileName().toString().startsWith(DIRECTORY_PREFIX)
                || Files.isSymbolicLink(dir)
                || !Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)
                || !dir.toRealPath().equals(dir)) {
            throw new IOException("worker directory is outside its owned temp child");
        }
        return dir;
    }

    private static void printInputModes(String role, List<String> inputs) {
        System.out.println("M3_WORDROWS_" + role + "_INPUTS"
                + " interpreted=" + inputs.contains("-Xint")
                + " compactOff=" + inputs.contains("-XX:-CompactStrings")
                + " batch=" + inputs.contains("-Xbatch")
                + " tieredOff=" + inputs.contains("-XX:-TieredCompilation")
                + " jniCheck=" + inputs.contains("-Xcheck:jni"));
    }

    private static Throwable retain(Throwable first, Throwable next) {
        if (first == null) return next;
        if (first != next) first.addSuppressed(next);
        return first;
    }

    private static void stopWorker(Process child) throws IOException {
        boolean interrupted = Thread.interrupted();
        try {
            if (child == null) return;
            for (int attempt = 0; child.isAlive() && attempt < 2; attempt++) {
                if (attempt == 0) child.destroy();
                else child.destroyForcibly();
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(STOP_SECONDS);
                while (child.isAlive()) {
                    long remaining = deadline - System.nanoTime();
                    if (remaining <= 0) break;
                    try {
                        child.waitFor(remaining, TimeUnit.NANOSECONDS);
                    } catch (InterruptedException again) {
                        interrupted = true;
                    }
                }
            }
            if (child.isAlive()) throw new IOException("worker did not terminate; fixtures retained");
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    private static int cleanupKnownFiles(Path owned) throws IOException {
        Path dir = workerDirectory(owned.toString());
        IOException failure = null;
        int removed = 0;
        for (String name : FIXTURE_FILES) {
            Path file = dir.resolve(name).normalize();
            try {
                if (!dir.equals(file.getParent())
                        || Files.isSymbolicLink(file)
                        || (Files.exists(file, LinkOption.NOFOLLOW_LINKS)
                            && !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))) {
                    throw new IOException("fixture entry is not an owned regular file");
                }
                if (Files.deleteIfExists(file)) removed++;
            } catch (IOException problem) {
                if (failure == null) failure = problem;
                else failure.addSuppressed(problem);
            }
        }
        try {
            Files.delete(dir);
        } catch (IOException problem) {
            if (failure == null) failure = problem;
            else failure.addSuppressed(problem);
        }
        if (failure != null) throw failure;
        return removed;
    }

    private static void runWorker(String[] args) throws Exception {
        Path base = Path.of(System.getProperty("java.io.tmpdir")).toRealPath();
        Path dir = Files.createTempDirectory(base, DIRECTORY_PREFIX);
        Process child = null;
        Throwable failure = null;
        int removed = -1;
        try {
            dir = workerDirectory(dir.toString());
            List<String> inputs = ManagementFactory.getRuntimeMXBean().getInputArguments();
            printInputModes("PARENT", inputs);
            String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
            var command = new ArrayList<String>();
            command.add(Path.of(System.getProperty("java.home"), "bin", executable).toString());
            command.addAll(inputs);
            command.add("-D" + WORKER_DIRECTORY + "=" + dir);
            command.add("-cp");
            command.add(System.getProperty("java.class.path"));
            command.add(M3WordRowsContractTest.class.getName());
            command.addAll(Arrays.asList(args));
            ProcessBuilder builder = new ProcessBuilder(command).inheritIO();
            for (String name : List.of("_JAVA_OPTIONS", "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS")) {
                builder.environment().remove(name);
            }
            child = builder.start();
            if (!child.waitFor(WORKER_SECONDS, TimeUnit.SECONDS)) {
                throw new IOException("word-row worker exceeded its bounded wait");
            }
            if (child.exitValue() != 0) {
                throw new IOException("word-row worker failed with exit=" + child.exitValue());
            }
        } catch (Exception | Error problem) {
            failure = problem;
            if (problem instanceof InterruptedException) Thread.currentThread().interrupt();
        } finally {
            try {
                stopWorker(child);
            } catch (Exception | Error problem) {
                failure = retain(failure, problem);
            }
            if (child == null || !child.isAlive()) {
                try {
                    removed = cleanupKnownFiles(dir);
                } catch (Exception | Error problem) {
                    failure = retain(failure, problem);
                }
            } else {
                failure = retain(failure, new IOException("live worker; fixture cleanup refused"));
            }
        }
        if (failure instanceof Exception exception) throw exception;
        if (failure instanceof Error error) throw error;
        if (removed != FIXTURE_FILES.size()) {
            throw new IOException("successful worker did not produce all seven fixture files");
        }
        System.out.println("M3_WORDROWS_FIXTURE_CLEANUP_PASS files=7 childExited=true");
    }

    private static void exercise(String[] args, Path dir) throws Exception {
        Path file = dir.resolve("fixture.m3rows");
        byte[] source = fixture();
        Files.write(file, source);
        M3WordRows rows = M3WordRows.open(file);
        rows.requireGeneration(LANGUAGE, COUNT, FINGERPRINT, "en", "v3");
        check(rows.hotCoreBytes() == 64, "hot-core bytes");
        check(rows.mappedImageBytes() == END, "single mapped image");
        check(rows.totalCorpusTokens() == 60, "corpus total");
        check(rows.totalDocuments() == 3, "document total");
        for (int i = 0; i < COUNT; i++) {
            check(rows.rowByteOffset(i) == ROWS_BASE + i * 128, "row pointer arithmetic");
            check(rows.wordId(i) == ((long) LANGUAGE << 48) + i, "packed word ID");
            check(rows.localId(rows.wordId(i)) == i, "local id roundtrip");
            check(rows.flags(i) == i * 5 + 5, "flags");
            check(rows.utf16Length(i) == (i == 1 ? 2 : 1), "utf16");
            check(rows.codePointCount(i) == rows.utf16Length(i), "code point count");
            check(rows.utf8Length(i) == rows.utf16Length(i), "utf8");
            check(rows.corpusCount(i) == 10L * (i + 1), "corpus count");
            check(rows.documentFrequency(i) == i + 1, "document frequency");
            check(rows.membershipCount(i) == 1 && rows.membershipAt(i, 0) == i + 10,
                    "membership CSR");
            check(rows.conceptCount(i) == 1 && rows.conceptAt(i, 0) == i + 100,
                    "concept CSR");
            check(rows.subjectCount(i) == 1 && rows.subjectAt(i, 0) == i + 200,
                    "subject CSR");
            check(rows.expansionCount(i) == 1 && rows.expansionWordIdAt(i, 0) == i + 300,
                    "expansion CSR");
            check(rows.frequencyRank(i) == COUNT - i - 1, "frequency rank");
        }
        check(rows.isPrefixOf(0, 1), "a prefix of ab");
        check(!rows.isPrefixOf(1, 0), "ab not prefix of a");
        check(rows.isSuffixOf(2, 1), "b suffix of ab");
        check(!rows.isSuffixOf(0, 1), "a not suffix of ab");
        M3WordRows.Cursor a = rows.cursor().reset(0);
        M3WordRows.Cursor b = rows.cursor().reset(1);
        check(a.isPrefixOf(b) && !a.isSuffixOf(b) && !a.sameWord(b), "cursor relations");
        check(a.corpusCount() == 10L && b.flags() == 10, "cursor hot facts");
        check(a.sameWord(rows.cursor().reset(0)), "cursor word equality");
        fails(IllegalArgumentException.class, () -> rows.localId(rows.wordId(0) | M3WordRows.OVERLAY_BIT));
        fails(IllegalArgumentException.class, () -> rows.localId((43L << 48)));
        fails(IllegalArgumentException.class, () -> rows.localId((42L << 48) | 1000L));
        fails(IllegalArgumentException.class, () -> rows.requireGeneration(43, COUNT, FINGERPRINT, "en", "v3"));
        fails(IndexOutOfBoundsException.class, () -> rows.rowByteOffset(-1));
        fails(IndexOutOfBoundsException.class, () -> rows.rowByteOffset(COUNT));
        fails(IndexOutOfBoundsException.class, () -> rows.membershipAt(0, 1));
        M3WordRows foreign = M3WordRows.open(file);
        fails(IllegalArgumentException.class, () -> a.sameWord(foreign.cursor().reset(0)));
        int[] ids = new int[10000];
        Random rnd = new Random(0x4d33524f5753L);
        for (int i = 0; i < ids.length; i++) ids[i] = rnd.nextInt(COUNT);
        int[] expected = new int[ids.length];
        for (int i = 0; i < ids.length; i++) expected[i] = rows.flags(ids[i]);
        check(Arrays.equals(expected, M3WordRowsNative.flagsBatch(rows, ids)), "Java batch parity");
        if (args.length != 0) {
            M3WordRowsNative.load(Path.of(args[0]));
            check(M3WordRowsNative.available(), "native library loaded");
            check(Arrays.equals(expected, M3WordRowsNative.flagsBatch(rows, ids)), "JNI full-batch parity");
            check(Arrays.equals(new int[0], M3WordRowsNative.flagsBatch(rows, new int[0])), "JNI empty");
        }
        fails(IndexOutOfBoundsException.class, () -> M3WordRowsNative.flagsBatch(rows, new int[] {1, -1}));
        bad(file, source, 8, 2); // unsupported historical row image
        bad(file, source, 20, 64); // wrong row stride
        bad(file, source, 64, ROWS_BASE + 64); // corrupt section geometry
        bad(file, source, ROWS_BASE + 76, 10000); // row relation overflow
        bad(file, source, ROWS_BASE + 56, 0); // invalid prefix range
        bad(file, source, 12, -1); // invalid language
        System.out.println("M3_WORDROWS_PASS checks=" + checks
                + " JNI=" + M3WordRowsNative.available()
                + " imageFormat=3 scope=word-id-arithmetic-no-string-replacement");
    }

    public static void main(String[] args) throws Exception {
        String worker = System.getProperty(WORKER_DIRECTORY);
        if (worker != null) {
            Path dir = workerDirectory(worker);
            printInputModes("WORKER", ManagementFactory.getRuntimeMXBean().getInputArguments());
            exercise(args, dir);
        } else {
            runWorker(args);
        }
    }
}
