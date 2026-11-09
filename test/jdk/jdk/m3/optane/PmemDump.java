// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;
import java.util.zip.CRC32;

/**
 * Differential dump driver of the PMem region family (gate 3 of recipe/pmem-region-core-20261106; not a test). The
 * donor family (the dotted PACKAGE row of RELOCATION_MAP.tsv, type prefix "", test package test) and the relocated family (package
 * jdk.m3.optane, type prefix "", test package jdk.m3.optane) replay the same inputs through reflection: the FILE
 * oracle's deterministic lanes (metadata, seeded search, memmove and hard-link aliases, path replacement, provider
 * null keys, final symlink, sorted little-endian bounds above 2 GiB, read-only and closed refusals, bounded workload),
 * then {@code operations} seeded random read/write/persist/copy/equalRange/copyIfDifferent/indexOf/bound/close/reopen
 * operations over seven FILE regions (two spans of one file, a hard-link alias, a read-only span, a sparse span above
 * 2 GiB, a sorted lane) with invalid arguments and a foreign region proxy mixed in. In native mode (Linux, the library
 * on java.library.path) POSIX regions join the oracle lanes and the random operations, and the 19 public API atoms of
 * PmemAtomCases.verify run first. Every result and every refusal (exception class and message) is printed; temporary
 * paths, the package names and the M3 type prefix are erased from messages, so the two dumps must be byte-identical.
 * usage: PmemDump &lt;package&gt; &lt;type prefix&gt; &lt;test package&gt; &lt;operations&gt; [native]
 */
public final class PmemDump {
    private static final int SIZE = 262_144;
    private static final int SORTED_ELEMENTS = 4_096;
    private static final long SORTED_OFFSET = 11;
    private static final long SPARSE_OFFSET = (1L << 31) + 103;
    private static final int SPARSE_LENGTH = 65_536;
    private static final long SEED = 20_261_106L;
    private static final String[] ATOMS = {"capabilities", "open", "length", "fileOffset", "path", "access", "backend",
        "persistenceGranularity", "isOpen", "read", "write", "persist", "close", "copy", "equalRange",
        "copyIfDifferent", "indexOf", "lowerBoundLongLE", "upperBoundLongLE"};

    private final String pkg;
    private final String prefix;
    private final String testPkg;
    private final boolean nativeMode;
    private final PrintStream out;
    private final Path root;
    private final Class<?> regionType;
    private final Method open;
    private final Method capabilities;
    private final Method copy;
    private final Method equalRange;
    private final Method copyIfDifferent;
    private final Method indexOf;
    private final Method lowerBound;
    private final Method upperBound;
    private final Method length;
    private final Method fileOffset;
    private final Method pathOf;
    private final Method accessOf;
    private final Method backendOf;
    private final Method granularityOf;
    private final Method isOpen;
    private final Method read;
    private final Method write;
    private final Method persist;
    private final Method close;
    private final Object readOnly;
    private final Object readWrite;
    private final Object fileBackend;
    private final Object posixBackend;
    private long exceptions;
    private long operations;

    private PmemDump(String pkg, String prefix, String testPkg, boolean nativeMode, PrintStream out)
            throws Exception {
        this.pkg = pkg;
        this.prefix = prefix;
        this.testPkg = testPkg;
        this.nativeMode = nativeMode;
        this.out = out;
        this.root = Files.createTempDirectory("m3-pmem-dump-").toRealPath();
        Class<?> sdk = type("PmemSdk");
        Class<?> access = type("PmemAccess");
        Class<?> backend = type("PmemBackend");
        Class<?> transfer = type("PmemTransfer");
        Class<?> search = type("PmemSearch");
        regionType = type("PmemRegion");
        open = sdk.getMethod("open", Path.class, long.class, long.class, access, backend);
        capabilities = sdk.getMethod("capabilities");
        copy = transfer.getMethod("copy", regionType, long.class, regionType, long.class, long.class);
        equalRange = transfer.getMethod("equalRange", regionType, long.class, regionType, long.class, long.class);
        copyIfDifferent = transfer.getMethod("copyIfDifferent", regionType, long.class, regionType, long.class,
                long.class);
        indexOf = search.getMethod("indexOf", regionType, long.class, long.class, byte[].class);
        lowerBound = search.getMethod("lowerBoundLongLE", regionType, long.class, long.class, long.class);
        upperBound = search.getMethod("upperBoundLongLE", regionType, long.class, long.class, long.class);
        length = regionType.getMethod("length");
        fileOffset = regionType.getMethod("fileOffset");
        pathOf = regionType.getMethod("path");
        accessOf = regionType.getMethod("access");
        backendOf = regionType.getMethod("backend");
        granularityOf = regionType.getMethod("persistenceGranularity");
        isOpen = regionType.getMethod("isOpen");
        read = regionType.getMethod("read", long.class, byte[].class, int.class, int.class);
        write = regionType.getMethod("write", long.class, byte[].class, int.class, int.class);
        persist = regionType.getMethod("persist", long.class, long.class);
        close = regionType.getMethod("close");
        readOnly = constant(access, "READ_ONLY");
        readWrite = constant(access, "READ_WRITE");
        fileBackend = constant(backend, "FILE");
        posixBackend = constant(backend, "POSIX");
    }

    /** The dump of one family as text (used by the JUnit wrapper); {@link #main} prints the same to stdout. */
    public static String replay(String pkg, String prefix, String testPkg, int operations, boolean nativeMode)
            throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(1 << 20);
        try (PrintStream stream = new PrintStream(bytes, false, StandardCharsets.UTF_8)) {
            new PmemDump(pkg, prefix, testPkg, nativeMode, stream).run(operations);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("usage: PmemDump <package> <type prefix> <test package> <operations> [native]");
            System.exit(2);
        }
        boolean nativeMode = args.length > 4 && args[4].equals("native");
        PrintStream stream = new PrintStream(System.out, false, StandardCharsets.UTF_8);
        new PmemDump(args[0], args[1], args[2], nativeMode, stream).run(Integer.parseInt(args[3]));
        stream.flush();
    }

    private void run(int count) throws Exception {
        try {
            line("M3_PMEM_DUMP native=" + nativeMode + " operations=" + count);
            line("capabilities " + invoke(capabilities, null));
            if (nativeMode) atoms();
            oracleMetadata();
            oracleSearch();
            oracleOverlap();
            oracleReplacement();
            oracleNullKeys();
            oracleSymlink();
            oracleBounds();
            oracleReadonlyClosed();
            oracleWorkload();
            random(count);
            line("TOTAL operations=" + operations + " exceptions=" + exceptions);
        } finally {
            deleteTree(root);
        }
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

    /** The formatted result of a reflective call, or the formatted refusal (class and erased message). */
    private String invoke(Method method, Object target, Object... args) {
        operations++;
        try {
            return format(method.invoke(target, args));
        } catch (InvocationTargetException e) {
            return refusal(e.getCause());
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new IllegalStateException("reflective call failed: " + method, e);
        }
    }

    private String refusal(Throwable cause) {
        exceptions++;
        return "!" + cause.getClass().getSimpleName() + ": " + erase(cause.getMessage());
    }

    private String format(Object result) {
        if (result == null) return "ok";
        if (result instanceof Enum<?> e) return e.name();
        if (result instanceof Path p) return "path";
        if (result instanceof Record r) return erase(r.toString());
        return erase(result.toString());
    }

    private String erase(String text) {
        if (text == null) return "null";
        String erased = text.replace(root.toString(), "<tmp>").replace(root.toString().replace('\\', '/'), "<tmp>")
                .replace(testPkg + ".", "").replace(pkg + ".", "");
        if (!prefix.isEmpty()) erased = erased.replace(prefix + "Pmem", "Pmem").replace(prefix + "NativePmem", "NativePmem");
        return erased;
    }

    private static String crc(byte[] bytes, int offset, int count) {
        CRC32 crc = new CRC32();
        crc.update(bytes, offset, count);
        return Long.toHexString(crc.getValue());
    }

    /** Opens a region or returns null after printing the refusal. */
    private Object region(String label, Path path, long offset, long len, boolean rw, boolean posix) {
        operations++;
        try {
            Object region = open.invoke(null, path, offset, len, rw ? readWrite : readOnly,
                    posix ? posixBackend : fileBackend);
            line(label + " open offset=" + offset + " length=" + len + " rw=" + rw + " posix=" + posix + " -> ok");
            return region;
        } catch (InvocationTargetException e) {
            line(label + " open offset=" + offset + " length=" + len + " rw=" + rw + " posix=" + posix + " -> "
                    + refusal(e.getCause()));
            return null;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private String readCrc(Object region, long index, int count) {
        byte[] target = new byte[count];
        String result = invoke(read, region, index, target, 0, count);
        return result.equals("ok") ? "crc=" + crc(target, 0, count) : result;
    }

    private byte[] readAll(Object region) throws Exception {
        byte[] bytes = new byte[(int) (long) length.invoke(region)];
        read.invoke(region, 0L, bytes, 0, bytes.length);
        return bytes;
    }

    private void closeQuietly(Object... regions) {
        for (Object region : regions) if (region != null) line("close -> " + invoke(close, region));
    }

    private Object foreignRegion() {
        return Proxy.newProxyInstance(regionType.getClassLoader(), new Class<?>[] {regionType},
                (proxy, method, args) -> { throw new UnsupportedOperationException("foreign " + method.getName()); });
    }

    private Path file(String name, byte[] content) throws IOException {
        Path path = root.resolve(name);
        Files.write(path, content);
        return path;
    }

    private Path sparseFile(String name, long size) throws IOException {
        Path path = root.resolve(name);
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "rw")) { file.setLength(size); }
        return path;
    }

    private static void deleteTree(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    private static long naiveIndex(byte[] b, int start, int count, byte[] p) {
        outer:
        for (int i = start; i <= start + count - p.length; i++) {
            for (int j = 0; j < p.length; j++) if (b[i + j] != p[j]) continue outer;
            return i;
        }
        return -1;
    }

    // ---------------------------------------------------------------- the 19 atoms (native mode)

    private void atoms() throws Exception {
        Class<?> cases = Class.forName(testPkg + "." + prefix + "PmemAtomCases");
        Method verify = cases.getMethod("verify", String.class);
        for (String atom : ATOMS) line("ATOM " + atom + " -> " + (invoke(verify, null, atom).equals("ok") ? "PASS" : "FAIL"));
    }

    // ---------------------------------------------------------------- the FILE oracle lanes

    private void oracleMetadata() throws Exception {
        Path p = file("metadata.bin", new byte[128]);
        Object r = region("ORACLE metadata", p, 7, 97, true, false);
        line("ORACLE metadata path-equals=" + p.equals(pathOf.invoke(r)) + " fileOffset=" + invoke(fileOffset, r)
                + " length=" + invoke(length, r) + " backend=" + invoke(backendOf, r) + " access=" + invoke(accessOf, r)
                + " granularity=" + invoke(granularityOf, r) + " isOpen=" + invoke(isOpen, r));
        byte[] b = {1, 2, 3, 4};
        line("ORACLE metadata write -> " + invoke(write, r, 2L, b, 1, 2) + " persist -> " + invoke(persist, r, 0L, 97L));
        byte[] six = new byte[6];
        line("ORACLE metadata read -> " + invoke(read, r, 2L, six, 3, 2) + " bytes=" + Arrays.toString(six));
        line("ORACLE metadata zero read -> " + invoke(read, r, 97L, six, 0, 0) + " zero write -> "
                + invoke(write, r, 97L, b, 0, 0) + " zero persist -> " + invoke(persist, r, 97L, 0L));
        closeQuietly(r);
        byte[] all = Files.readAllBytes(p);
        line("ORACLE metadata file[9]=" + all[9] + " file[10]=" + all[10] + " isOpen=" + invoke(isOpen, r));
    }

    private void oracleSearch() throws Exception {
        byte[] a = new byte[180_017];
        new Random(1397).nextBytes(a);
        Path p = file("search-a.bin", a);
        Path q = file("search-b.bin", new byte[a.length]);
        Object f = region("ORACLE search f", p, 0, a.length, true, false);
        Object n = region("ORACLE search n", q, 0, a.length, true, nativeMode);
        line("ORACLE search copy -> " + invoke(copy, null, f, 0L, n, 0L, (long) a.length) + " n-equals-a="
                + Arrays.equals(a, readAll(n)));
        line("ORACLE search equalRange -> " + invoke(equalRange, null, f, 0L, n, 0L, (long) a.length)
                + " copyIfDifferent -> " + invoke(copyIfDifferent, null, f, 0L, n, 0L, (long) a.length));
        line("ORACLE search write -> " + invoke(write, n, 65_535L, new byte[] {42}, 0, 1) + " copyIfDifferent -> "
                + invoke(copyIfDifferent, null, n, 0L, f, 0L, (long) a.length));
        a[65_535] = 42;
        line("ORACLE search f-equals-a=" + Arrays.equals(a, readAll(f)));
        Random rng = new Random(871);
        for (int run = 0; run < 120; run++) {
            int start = rng.nextInt(a.length + 1), count = rng.nextInt(a.length - start + 1);
            int k = rng.nextInt(20);
            byte[] pattern = new byte[k];
            rng.nextBytes(pattern);
            if (k <= count && run % 2 == 0) System.arraycopy(a, start + rng.nextInt(count - k + 1), pattern, 0, k);
            line("ORACLE search run=" + run + " start=" + start + " count=" + count + " k=" + k + " expected="
                    + naiveIndex(a, start, count, pattern) + " f=" + invoke(indexOf, null, f, (long) start, (long) count, pattern)
                    + " n=" + invoke(indexOf, null, n, (long) start, (long) count, pattern));
        }
        byte[] cross = {9, 8, 7, 6, 5, 4};
        line("ORACLE search cross write -> " + invoke(write, f, 65_533L, cross, 0, cross.length) + " indexOf="
                + invoke(indexOf, null, f, 0L, (long) a.length, cross) + " empty-at-end="
                + invoke(indexOf, null, f, (long) a.length, 0L, new byte[0]) + " absent="
                + invoke(indexOf, null, f, (long) a.length, 0L, new byte[] {1}));
        closeQuietly(f, n);
    }

    private void oracleOverlap() throws Exception {
        byte[] data = new byte[200_000];
        new Random(17).nextBytes(data);
        Path p = file("overlap.bin", data);
        Path q = file("overlap-copy.bin", new byte[data.length]);
        Path hard = root.resolve("overlap.bin.link");
        Files.createLink(hard, p);
        Object f = region("ORACLE overlap f", p, 0, data.length, true, false);
        Object g = region("ORACLE overlap g", q, 0, data.length, true, false);
        Object alias = region("ORACLE overlap alias", hard, 3, data.length - 3, true, false);
        Object nativeAlias = region("ORACLE overlap nativeAlias", p, 0, data.length, true, nativeMode);
        System.arraycopy(data, 0, data, 11, 170_000);
        line("ORACLE overlap forward -> " + invoke(copy, null, f, 0L, f, 11L, 170_000L) + " equals=" + Arrays.equals(data, readAll(f)));
        System.arraycopy(data, 11, data, 0, 170_000);
        line("ORACLE overlap backward -> " + invoke(copy, null, f, 11L, f, 0L, 170_000L) + " equals=" + Arrays.equals(data, readAll(f)));
        byte[] expected = data.clone();
        System.arraycopy(expected, 0, expected, 7, 150_000);
        line("ORACLE overlap copyIfDifferent -> " + invoke(copyIfDifferent, null, f, 0L, f, 7L, 150_000L) + " equals="
                + Arrays.equals(expected, readAll(f)) + " same -> " + invoke(copyIfDifferent, null, f, 0L, f, 0L, (long) data.length));
        line("ORACLE overlap f->g -> " + invoke(copy, null, f, 0L, g, 0L, (long) data.length) + " equalRange -> "
                + invoke(equalRange, null, f, 0L, g, 0L, (long) data.length));
        line("ORACLE overlap alias copy -> " + invoke(copy, null, f, 0L, alias, 0L, 100L) + " alias copyIfDifferent -> "
                + invoke(copyIfDifferent, null, alias, 0L, f, 3L, 100L) + " nativeAlias copy -> "
                + invoke(copy, null, f, 0L, nativeAlias, 1L, 100L));
        line("ORACLE overlap alias equalRange -> " + invoke(equalRange, null, f, 3L, alias, 0L, 100L) + " disjoint -> "
                + invoke(copy, null, f, 0L, alias, 150_000L, 100L) + " empty -> "
                + invoke(copy, null, f, (long) data.length, alias, (long) data.length - 3, 0L) + " emptyIfDifferent -> "
                + invoke(copyIfDifferent, null, f, (long) data.length, alias, (long) data.length - 3, 0L));
        closeQuietly(f, g, alias, nativeAlias);
    }

    private void oracleReplacement() throws Exception {
        for (boolean posix : nativeMode ? new boolean[] {false, true} : new boolean[] {false}) {
            Path directory = Files.createDirectory(root.resolve("identity-" + posix));
            Path source = directory.resolve("source.bin"), alias = directory.resolve("alias.bin");
            Path replacement = directory.resolve("replacement.bin");
            byte[] original = new byte[200_000], unrelated = new byte[original.length];
            new Random(1223).nextBytes(original);
            Arrays.fill(unrelated, (byte) 73);
            Files.write(source, original);
            Files.createLink(alias, source);
            Files.write(replacement, unrelated);
            Object a = region("ORACLE replacement a", source, 0, original.length, true, false);
            Object b = region("ORACLE replacement b", alias, 0, original.length, true, posix);
            String moved;
            try {
                Files.move(replacement, source, StandardCopyOption.REPLACE_EXISTING);
                moved = "moved";
            } catch (IOException e) {
                moved = refusal(e);
            }
            line("ORACLE replacement posix=" + posix + " move=" + moved + " copy -> " + invoke(copy, null, a, 0L, b, 11L, 170_000L)
                    + " a-equals=" + Arrays.equals(original, readAll(a)) + " b-equals=" + Arrays.equals(original, readAll(b))
                    + " copyIfDifferent -> " + invoke(copyIfDifferent, null, a, 0L, b, 11L, 170_000L)
                    + " alias-file-equals=" + Arrays.equals(original, Files.readAllBytes(alias))
                    + " source-file-unrelated=" + (moved.equals("moved") && Arrays.equals(unrelated, Files.readAllBytes(source))));
            closeQuietly(a, b);
        }
    }

    private void oracleNullKeys() throws Exception {
        Path archive = root.resolve("null-key.zip");
        try (FileSystem fs = FileSystems.newFileSystem(archive, Map.of("create", "true"))) {
            Path p = fs.getPath("/a.bin"), q = fs.getPath("/b.bin");
            byte[] data = new byte[128];
            for (int i = 0; i < data.length; i++) data[i] = (byte) i;
            Files.write(p, data);
            Files.write(q, new byte[data.length]);
            line("ORACLE nullkeys fileKey-null=" + (Files.readAttributes(p, BasicFileAttributes.class).fileKey() == null));
            Object a = region("ORACLE nullkeys a", p, 0, data.length, true, false);
            if (a == null) {
                String channel;
                try (FileChannel c = FileChannel.open(p, StandardOpenOption.READ, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
                    channel = "opened " + c.isOpen();
                } catch (IOException | IllegalArgumentException | UnsupportedOperationException e) {
                    channel = refusal(e);
                }
                line("ORACLE nullkeys provider refuses no-follow: channel=" + channel);
                return;
            }
            Object b = region("ORACLE nullkeys b", q, 0, data.length, true, false);
            line("ORACLE nullkeys empty -> " + invoke(copy, null, a, 0L, b, 0L, 0L) + " same-owner -> "
                    + invoke(copy, null, a, 0L, a, 1L, 16L) + " disjoint -> " + invoke(copy, null, a, 0L, b, 100L, 16L)
                    + " equalRange -> " + invoke(equalRange, null, a, 0L, b, 100L, 16L) + " overlap -> "
                    + invoke(copy, null, a, 0L, b, 0L, 16L) + " overlapIfDifferent -> " + invoke(copyIfDifferent, null, a, 0L, b, 0L, 16L));
            closeQuietly(a, b);
        }
    }

    private void oracleSymlink() throws Exception {
        Path directory = Files.createDirectory(root.resolve("final-symlink"));
        Path regular = directory.resolve("regular.bin"), link = directory.resolve("link.bin");
        byte[] original = {11, 22, 33, 44};
        Files.write(regular, original);
        try {
            Files.createSymbolicLink(link, regular.getFileName());
        } catch (IOException | UnsupportedOperationException e) {
            line("ORACLE symlink creation -> " + refusal(e));
            return;
        }
        for (boolean rw : new boolean[] {false, true}) {
            Object r = region("ORACLE symlink rw=" + rw, link, 0, original.length, rw, false);
            closeQuietly(r);
        }
        line("ORACLE symlink regular-intact=" + Arrays.equals(original, Files.readAllBytes(regular)));
    }

    private void oracleBounds() throws Exception {
        long[] values = {Long.MIN_VALUE, Long.MIN_VALUE, -8, -1, 0, 0, 1, Long.MAX_VALUE};
        long begin = (1L << 31) + 19;
        Path p = root.resolve("sparse-bounds.bin");
        ByteBuffer buf = ByteBuffer.allocate(values.length * 8 + 3).order(ByteOrder.LITTLE_ENDIAN);
        buf.position(3);
        for (long x : values) buf.putLong(x);
        try (FileChannel c = FileChannel.open(p, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            buf.flip();
            long position = begin;
            while (buf.hasRemaining()) position += c.write(buf, position);
        }
        Object f = region("ORACLE bounds f", p, begin, values.length * 8 + 3, true, false);
        for (long key : new long[] {Long.MIN_VALUE, -9, -8, -1, 0, 1, 2, Long.MAX_VALUE}) {
            int lo = 0, hi = 0;
            while (lo < values.length && values[lo] < key) lo++;
            while (hi < values.length && values[hi] <= key) hi++;
            line("ORACLE bounds key=" + key + " expected=" + lo + "/" + hi + " lower=" + invoke(lowerBound, null, f, 3L, (long) values.length, key)
                    + " upper=" + invoke(upperBound, null, f, 3L, (long) values.length, key));
        }
        long len = (long) length.invoke(f);
        line("ORACLE bounds empty -> " + invoke(lowerBound, null, f, len, 0L, 0L) + " huge -> "
                + invoke(lowerBound, null, f, 0L, Long.MAX_VALUE, 0L) + " copy-max -> " + invoke(copy, null, f, Long.MAX_VALUE, f, 0L, 8L)
                + " persist-max -> " + invoke(persist, f, 1L, Long.MAX_VALUE) + " pattern-max -> "
                + invoke(indexOf, null, f, 0L, 0L, new byte[(1 << 20) + 1]));
        closeQuietly(f);
        Object g = region("ORACLE bounds open-max", p, Long.MAX_VALUE, 2, true, false);
        Object h = region("ORACLE bounds open-beyond", p, begin, 1_000, true, false);
        closeQuietly(g, h);
    }

    private void oracleReadonlyClosed() throws Exception {
        Path p = file("readonly.bin", new byte[32]);
        Object f = region("ORACLE readonly f", p, 0, 32, false, false);
        line("ORACLE readonly write -> " + invoke(write, f, 0L, new byte[0], 0, 0) + " persist -> " + invoke(persist, f, 0L, 0L)
                + " copy -> " + invoke(copy, null, f, 0L, f, 0L, 0L));
        line("ORACLE readonly close -> " + invoke(close, f) + " close -> " + invoke(close, f) + " isOpen=" + invoke(isOpen, f));
        line("ORACLE closed read -> " + invoke(read, f, 0L, new byte[0], 0, 0) + " indexOf -> " + invoke(indexOf, null, f, 0L, 0L, new byte[0])
                + " lowerBound -> " + invoke(lowerBound, null, f, 0L, 0L, 0L) + " equalRange -> " + invoke(equalRange, null, f, 0L, f, 0L, 0L)
                + " negative -> " + invoke(read, f, -1L, new byte[0], 0, 0));
    }

    private void oracleWorkload() throws Exception {
        byte[] block = new byte[1 << 20];
        Arrays.fill(block, (byte) 'a');
        Path p = file("workload.bin", block);
        Object f = region("ORACLE workload f", p, 0, block.length, true, false);
        long hit = 0;
        for (int i = 0; i < 8; i++) hit += Long.parseLong(invoke(indexOf, null, f, 0L, (long) block.length, new byte[] {'a', 'a', 'b'}));
        line("ORACLE workload scanned=" + 8L * block.length + " hit=" + hit);
        closeQuietly(f);
    }

    // ---------------------------------------------------------------- seeded random operations

    /** One region slot: its open specification and its current instance (null while closed). */
    private static final class Slot {
        final String name;
        final Path path;
        final long offset;
        final long len;
        final boolean rw;
        final boolean posix;
        Object region;

        Slot(String name, Path path, long offset, long len, boolean rw, boolean posix) {
            this.name = name;
            this.path = path;
            this.offset = offset;
            this.len = len;
            this.rw = rw;
            this.posix = posix;
        }
    }

    private List<Slot> slots() throws IOException {
        Random seed = new Random(SEED ^ 0x5A5AL);
        byte[] content = new byte[SIZE];
        seed.nextBytes(content);
        Path f0 = file("random-0.bin", content);
        Path f1 = file("random-1.bin", new byte[SIZE]);
        Path link = root.resolve("random-0.bin.link");
        Files.createLink(link, f0);
        Path sparse = sparseFile("random-sparse.bin", SPARSE_OFFSET + SPARSE_LENGTH);
        long[] sorted = new long[SORTED_ELEMENTS];
        for (int i = 0; i < sorted.length; i++) sorted[i] = seed.nextLong() >> 20;
        Arrays.sort(sorted);
        ByteBuffer lane = ByteBuffer.allocate((int) SORTED_OFFSET + SORTED_ELEMENTS * Long.BYTES + 5).order(ByteOrder.LITTLE_ENDIAN);
        lane.position((int) SORTED_OFFSET);
        for (long value : sorted) lane.putLong(value);
        Path f3 = file("random-sorted.bin", lane.array());
        List<Slot> slots = new ArrayList<>(List.of(
                new Slot("r0", f0, 0, SIZE, true, false),
                new Slot("r1", f0, 4_096, 200_000, true, false),
                new Slot("r2", f1, 0, SIZE, true, false),
                new Slot("r3", f1, 100, 131_072, false, false),
                new Slot("r4", link, 3, SIZE - 3, true, false),
                new Slot("r5", sparse, SPARSE_OFFSET, SPARSE_LENGTH, true, false),
                new Slot("r6", f3, 0, lane.capacity(), false, false)));
        if (nativeMode) {
            slots.add(new Slot("r7", f0, 0, SIZE, true, true));
            slots.add(new Slot("r8", f1, 0, 131_072, true, true));
            slots.add(new Slot("r9", sparse, SPARSE_OFFSET, SPARSE_LENGTH, true, true));
        }
        return slots;
    }

    private void random(int count) throws Exception {
        List<Slot> slots = slots();
        for (Slot slot : slots) slot.region = region("RANDOM " + slot.name, slot.path, slot.offset, slot.len, slot.rw, slot.posix);
        Random rng = new Random(SEED);
        Object foreign = foreignRegion();
        for (int n = 0; n < count; n++) {
            Slot a = slots.get(rng.nextInt(slots.size()));
            Slot b = slots.get(rng.nextInt(slots.size()));
            int kind = rng.nextInt(100);
            if (kind >= 94 && kind < 96 || !invoke(isOpen, a.region).equals("true") && rng.nextInt(4) != 0) kind = 94;
            String result;
            if (kind < 36) result = opRead(rng, a);
            else if (kind < 52) result = opWrite(rng, a);
            else if (kind < 57) result = "persist " + a.name + " -> " + invoke(persist, a.region, span(rng, a), (long) rng.nextInt(8_192));
            else if (kind < 65) result = opTransfer(rng, copy, "copy", a, b);
            else if (kind < 71) result = opTransfer(rng, equalRange, "equalRange", a, b);
            else if (kind < 77) result = opTransfer(rng, copyIfDifferent, "copyIfDifferent", a, b);
            else if (kind < 88) result = opIndexOf(rng, a);
            else if (kind < 94) result = opBound(rng, slots.get(6));
            else if (kind < 96) result = opCloseReopen(a);
            else if (kind < 98) result = "meta " + a.name + " isOpen=" + invoke(isOpen, a.region) + " length=" + invoke(length, a.region)
                    + " fileOffset=" + invoke(fileOffset, a.region) + " backend=" + invoke(backendOf, a.region)
                    + " access=" + invoke(accessOf, a.region) + " granularity=" + invoke(granularityOf, a.region);
            else result = "foreign " + a.name + " copy -> " + invoke(copy, null, a.region, 0L, foreign, 0L, 1L) + " indexOf -> "
                    + invoke(indexOf, null, foreign, 0L, 0L, new byte[0]) + " equalRange -> " + invoke(equalRange, null, foreign, 0L, a.region, 0L, 0L);
            line("#" + n + " " + result);
        }
        for (Slot slot : slots) if (slot.region != null) line("RANDOM " + slot.name + " close -> " + invoke(close, slot.region));
        line("RANDOM operations=" + count);
    }

    /** A start index inside the region, or just outside it one time in sixteen. */
    private static long span(Random rng, Slot slot) {
        return rng.nextInt(16) == 0 ? slot.len + rng.nextInt(3) - 1 : (long) rng.nextInt((int) slot.len + 1);
    }

    private static int amount(Random rng, Slot slot, long index, int cap) {
        long room = Math.max(0, slot.len - index);
        int bounded = (int) Math.min(cap, room);
        return rng.nextInt(16) == 0 ? bounded + 1 + rng.nextInt(16) : rng.nextInt(bounded + 1);
    }

    private String opRead(Random rng, Slot a) {
        long index = span(rng, a);
        int count = amount(rng, a, index, 4_096);
        if (rng.nextInt(32) == 0) {
            byte[] target = new byte[Math.max(0, count - 1)];
            return "read " + a.name + " index=" + index + " count=" + count + " short-array -> " + invoke(read, a.region, index, target, 0, count);
        }
        return "read " + a.name + " index=" + index + " count=" + count + " -> " + readCrc(a.region, index, count);
    }

    private String opWrite(Random rng, Slot a) {
        long index = span(rng, a);
        int count = amount(rng, a, index, 4_096);
        byte[] source = new byte[count];
        rng.nextBytes(source);
        return "write " + a.name + " index=" + index + " count=" + count + " crc=" + crc(source, 0, count) + " -> "
                + invoke(write, a.region, index, source, 0, count);
    }

    private String opTransfer(Random rng, Method method, String name, Slot a, Slot b) {
        long from = span(rng, a), to = span(rng, b);
        long bytes = Math.min(amount(rng, a, from, 8_192), amount(rng, b, to, 8_192));
        if (rng.nextInt(8) == 0) bytes = 0;
        return name + " " + a.name + "@" + from + " -> " + b.name + "@" + to + " bytes=" + bytes + " -> "
                + invoke(method, null, a.region, from, b.region, to, bytes);
    }

    private String opIndexOf(Random rng, Slot a) {
        long start = span(rng, a);
        int count = amount(rng, a, start, 65_536);
        int k = rng.nextInt(6) == 0 ? 0 : rng.nextInt(6) == 0 ? 1 : rng.nextInt(6) == 0 ? 2 : 3 + rng.nextInt(14);
        byte[] pattern = new byte[k];
        if (rng.nextBoolean() && k > 0 && k <= count && start + count <= a.len) {
            byte[] window = new byte[k];
            String sampled = invoke(read, a.region, start + rng.nextInt(count - k + 1), window, 0, k);
            if (sampled.equals("ok")) pattern = window;
        } else {
            rng.nextBytes(pattern);
        }
        return "indexOf " + a.name + " start=" + start + " count=" + count + " pattern=" + crc(pattern, 0, k) + "/" + k + " -> "
                + invoke(indexOf, null, a.region, start, (long) count, pattern);
    }

    private String opBound(Random rng, Slot lane) {
        long key = switch (rng.nextInt(6)) {
            case 0 -> Long.MIN_VALUE;
            case 1 -> Long.MAX_VALUE;
            case 2 -> 0L;
            default -> rng.nextLong() >> 20;
        };
        long offset = rng.nextInt(8) == 0 ? rng.nextInt((int) lane.len + 1) : SORTED_OFFSET;
        long elements = rng.nextInt(8) == 0 ? rng.nextLong() : rng.nextInt(16) == 0 ? rng.nextInt(SORTED_ELEMENTS + 1) : SORTED_ELEMENTS;
        boolean upper = rng.nextBoolean();
        return (upper ? "upperBound " : "lowerBound ") + lane.name + " offset=" + offset + " elements=" + elements + " key=" + key + " -> "
                + invoke(upper ? upperBound : lowerBound, null, lane.region, offset, elements, key);
    }

    private String opCloseReopen(Slot a) {
        if (a.region != null && invoke(isOpen, a.region).equals("true")) return "close " + a.name + " -> " + invoke(close, a.region);
        Object reopened = region("reopen " + a.name, a.path, a.offset, a.len, a.rw, a.posix);
        if (reopened != null) a.region = reopened;
        return "reopened " + a.name + " isOpen=" + invoke(isOpen, a.region);
    }
}
