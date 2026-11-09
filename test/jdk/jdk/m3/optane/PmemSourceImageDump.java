// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Differential dump driver of the PMem source-image family (gate 3 of recipe/pmem-source-image-20261106; not a test).
 * The donor family (the donor package of RELOCATION_MAP.tsv, type prefix "") and the relocated family (jdk.m3.optane, type
 * prefix "") replay the same seeded inputs through reflection: the source-path index lanes (census L026: empty,
 * small, 63/64-name boundary, 16/17/256-name equal-hash collision corpora, a 20,000-name corpus and a seeded random
 * corpus, every query compared with Arrays.binarySearch), the source-image oracle (pack, open, every accessor, both
 * readSealed forms, verifySealed with 7-byte, 1 KiB and 16 KiB windows, fileIndexOf, indexOf against a naive search,
 * warm, closed refusals, unsafe paths, duplicates, budgets, manifest corruption, truncation, same-size drift before and
 * after open), the source-slices geometry and lifetime lanes over a region of the SDK, then {@code operations} seeded
 * random operations over three images. In native mode (Linux, libm3pmemsdk on java.library.path) every image and slice
 * lane also runs over a POSIX region and the corpus-atom lane (POSIX only) runs. Every result and every refusal
 * (exception class and erased message) is printed; temporary paths, the package names and the M3 type prefix are
 * erased, so the two dumps must be byte-identical.
 * usage: PmemSourceImageDump &lt;package&gt; &lt;type prefix&gt; &lt;operations&gt; [native]
 */
public final class PmemSourceImageDump {
    private static final long SEED = 20_261_106L;
    private static final int FILES = 300;
    private static final int MIB = 1 << 20;

    private final String pkg;
    private final String prefix;
    private final boolean nativeMode;
    private final PrintStream out;
    private final Path root;
    private final Method pack;
    private final Method open;
    private final Constructor<?> limits;
    private final Constructor<?> slices;
    private final Method build;
    private final Method find;
    private final Method atomRead;
    private final Method sdkOpen;
    private final Object readOnly;
    private final Object readWrite;
    private final Object fileBackend;
    private final Object posixBackend;
    private final Class<?> imageType;
    private final Class<?> slicesType;
    private final Class<?> regionType;
    private long exceptions;
    private long operations;

    private PmemSourceImageDump(String pkg, String prefix, boolean nativeMode, PrintStream out) throws Exception {
        this.pkg = pkg;
        this.prefix = prefix;
        this.nativeMode = nativeMode;
        this.out = out;
        this.root = Files.createTempDirectory("m3-pmem-source-dump-").toRealPath();
        imageType = type("PmemSourceImage");
        Class<?> limitsType = type("PmemSourceImage$Limits");
        slicesType = type("PmemSourceSlices");
        regionType = type("PmemRegion");
        Class<?> access = type("PmemAccess");
        Class<?> backend = type("PmemBackend");
        pack = imageType.getMethod("pack", Path.class, List.class, Path.class, limitsType);
        open = imageType.getMethod("open", Path.class, backend, limitsType);
        limits = limitsType.getConstructor(int.class, long.class, int.class, int.class);
        slices = slicesType.getConstructor(regionType, long[].class, int[].class, byte[].class, int.class);
        Class<?> index = type("PmemSourcePathIndex");
        build = accessible(index.getDeclaredMethod("build", String[].class));
        find = accessible(index.getDeclaredMethod("find", String[].class, int[].class, String.class));
        atomRead = accessible(type("PmemCorpusAtom").getDeclaredMethod("read", Path.class, String.class, long.class,
                String.class, long.class, int.class));
        sdkOpen = type("PmemSdk").getMethod("open", Path.class, long.class, long.class, access, backend);
        readOnly = constant(access, "READ_ONLY");
        readWrite = constant(access, "READ_WRITE");
        fileBackend = constant(backend, "FILE");
        posixBackend = constant(backend, "POSIX");
    }

    /** The dump of one family as text (used by the JUnit wrapper); {@link #main} prints the same to stdout. */
    public static String replay(String pkg, String prefix, int operations, boolean nativeMode) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(1 << 20);
        try (PrintStream stream = new PrintStream(bytes, false, StandardCharsets.UTF_8)) {
            new PmemSourceImageDump(pkg, prefix, nativeMode, stream).run(operations);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("usage: PmemSourceImageDump <package> <type prefix> <operations> [native]");
            System.exit(2);
        }
        boolean nativeMode = args.length > 3 && args[3].equals("native");
        PrintStream stream = new PrintStream(System.out, false, StandardCharsets.UTF_8);
        new PmemSourceImageDump(args[0], args[1], nativeMode, stream).run(Integer.parseInt(args[2]));
        stream.flush();
    }

    private void run(int count) throws Exception {
        try {
            line("M3_PMEM_SOURCE_IMAGE_DUMP native=" + nativeMode + " operations=" + count);
            pathIndex();
            List<Object> backends = new ArrayList<>(List.of(fileBackend));
            if (nativeMode) backends.add(posixBackend);
            for (Object backend : backends) {
                imageOracle(backend);
                refusalOracle(backend);
                slicesOracle(backend);
            }
            if (nativeMode) corpusAtom();
            random(count, backends);
            line("TOTAL operations=" + operations + " exceptions=" + exceptions);
        } finally {
            deleteTree(root);
        }
    }

    // ---------------------------------------------------------------- reflection and formatting

    private Class<?> type(String donorName) throws ClassNotFoundException {
        return Class.forName(pkg + "." + prefix + donorName);
    }

    private static Method accessible(Method method) {
        method.setAccessible(true);
        return method;
    }

    private static Object constant(Class<?> enumType, String name) {
        return Arrays.stream(enumType.getEnumConstants()).filter(c -> ((Enum<?>) c).name().equals(name))
                .findFirst().orElseThrow();
    }

    private void line(String text) {
        out.print(text);
        out.print('\n');
    }

    /** The raw result of a reflective call, or a Refusal holding the formatted exception. */
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

    private Method method(Class<?> owner, String name, Class<?>... parameters) {
        try {
            return owner.getMethod(name, parameters);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("missing method " + name, e);
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
        if (result instanceof byte[] b) return "bytes:" + b.length + ":" + sha16(b, 0, b.length);
        if (result instanceof int[] ints) return "ints:" + ints.length + ":" + sha16(ints);
        if (result instanceof Path) return "path";
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
        return erased.replace("\u0000", "<NUL>");
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String sha16(byte[] bytes, int offset, int count) {
        MessageDigest digest = sha256();
        digest.update(bytes, offset, count);
        return HexFormat.of().formatHex(digest.digest(), 0, 8);
    }

    private static String sha16(int[] ints) {
        ByteBuffer buffer = ByteBuffer.allocate(ints.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.asIntBuffer().put(ints);
        return sha16(buffer.array(), 0, buffer.capacity());
    }

    private static String fullSha(byte[] bytes) {
        return HexFormat.of().formatHex(sha256().digest(bytes));
    }

    private record Refusal(String text) { }

    // ---------------------------------------------------------------- census L026: the source-path index

    private void pathIndex() {
        indexLane("empty", new String[0]);
        indexLane("small", new String[] {"a", "Aa", "BB", "snow-\u96ea", "\ud834\udd1e", "z"});
        indexLane("boundary-63", numbered(63));
        indexLane("boundary-64", numbered(64));
        indexLane("collision-16", colliding(4, 16));
        indexLane("collision-17", colliding(5, 17));
        indexLane("collision-256", colliding(8, 256));
        String[] corpus = new String[20_000];
        for (int i = 0; i < corpus.length; i++) corpus[i] = String.format(Locale.ROOT, "src/pkg%03d/File%05d.java", i / 100, i);
        indexLane("twenty-thousand", corpus);
        Random rng = new Random(SEED);
        String[] random = new String[1_000];
        for (int i = 0; i < random.length; i++) random[i] = randomName(rng, i);
        indexLane("random-1000", Arrays.stream(random).distinct().toArray(String[]::new));
        line("PATHINDEX build(null) -> " + invoke(build, null, (Object) null));
    }

    private static String[] numbered(int count) {
        String[] names = new String[count];
        for (int i = 0; i < count; i++) names[i] = String.format(Locale.ROOT, "Class%03d.java", i);
        return names;
    }

    private static String[] colliding(int bits, int count) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < (1 << bits); i++) {
            StringBuilder name = new StringBuilder("src/");
            for (int b = 0; b < bits; b++) name.append((i & (1 << b)) == 0 ? "Aa" : "BB");
            names.add(name.append(".java").toString());
        }
        Collections.sort(names);
        return names.subList(0, count).toArray(String[]::new);
    }

    private static String randomName(Random rng, int ordinal) {
        String alphabet = "abcXYZ019_-./\u00e4\u96ea";
        StringBuilder name = new StringBuilder();
        int length = 1 + rng.nextInt(24);
        for (int i = 0; i < length; i++) name.append(alphabet.charAt(rng.nextInt(alphabet.length())));
        return name.append('#').append(ordinal % 7).toString();
    }

    private void indexLane(String label, String[] unsorted) {
        String[] names = unsorted.clone();
        Arrays.sort(names);
        Object slots = call(build, null, (Object) names);
        List<String> queries = new ArrayList<>(Arrays.asList(names));
        for (int i = 0; i < 512; i++) queries.add(String.format(Locale.ROOT, "src/unlisted%06d.java", i));
        queries.addAll(List.of("", "src/Aa.java", "SRC/UPPER.java", "src/\u96ea.java", "a/../b", "Class999.java"));
        MessageDigest digest = sha256();
        long mismatches = 0;
        for (String query : queries) {
            Object found = call(find, null, names, slots, query);
            int oracle = Arrays.binarySearch(names, query);
            if (!(found instanceof Integer value) || value != (oracle < 0 ? -1 : oracle)) mismatches++;
            digest.update(format(found).getBytes(StandardCharsets.UTF_8));
        }
        line("PATHINDEX " + label + " files=" + names.length + " slots=" + format(slots) + " queries=" + queries.size()
                + " results=" + HexFormat.of().formatHex(digest.digest(), 0, 8) + " mismatches=" + mismatches
                + " null=" + invoke(find, null, names, slots, null));
    }

    // ---------------------------------------------------------------- the source image

    private Object limits(int files, long corpus, int file, int io) {
        return construct(limits, files, corpus, file, io);
    }

    private Path repository(String name, Random rng, int count) throws IOException {
        Path repository = Files.createDirectories(root.resolve(name));
        for (int i = 0; i < count; i++) {
            String relative = i % 37 == 0 ? String.format(Locale.ROOT, "\u96ea/\u00e4%03d.txt", i)
                    : String.format(Locale.ROOT, "pkg%02d/File%03d.java", i % 11, i);
            Path file = repository.resolve(relative);
            Files.createDirectories(file.getParent());
            int length = i % 50 == 3 ? 0 : i == 7 ? 70_000 : rng.nextInt(5_000);
            byte[] content = new byte[length];
            rng.nextBytes(content);
            if (i % 5 == 0) Arrays.fill(content, 0, Math.min(length, 64), (byte) 'a');
            Files.write(file, content);
        }
        return repository;
    }

    private static List<String> listed(Path repository) throws IOException {
        try (Stream<Path> walk = Files.walk(repository)) {
            return walk.filter(Files::isRegularFile).map(p -> repository.relativize(p).toString().replace('\\', '/'))
                    .sorted().toList();
        }
    }

    private void imageOracle(Object backend) throws IOException {
        String tag = "IMAGE " + format(backend);
        Random rng = new Random(SEED + 1);
        Path repository = repository("repo-" + format(backend), rng, FILES);
        List<String> paths = new ArrayList<>(listed(repository));
        Collections.shuffle(paths, rng);
        Object wide = limits(400, 10_000_000L, MIB, 4_096);
        Path image = root.resolve("image-" + format(backend));
        line(tag + " pack -> " + invoke(pack, null, repository, paths, image, wide));
        Object opened = call(open, null, image, backend, wide);
        line(tag + " open -> " + format(opened));
        if (opened instanceof Refusal) return;
        accessors(tag, opened, repository);
        searches(tag, opened, repository, rng);
        line(tag + " root-determinism " + rootDeterminism(repository, backend, wide));
        closed(tag, opened);
    }

    private void accessors(String tag, Object image, Path repository) throws IOException {
        Method count = method(imageType, "fileCount");
        int files = (Integer) call(count, image);
        line(tag + " fileCount=" + files + " sourceRoot=" + invoke(method(imageType, "sourceRoot"), image));
        Method path = method(imageType, "relativePathAt", int.class);
        Method length = method(imageType, "lengthAt", int.class);
        Method sha = method(imageType, "sha256At", int.class);
        Method sealed = method(imageType, "readSealed", int.class);
        Method sealedInto = method(imageType, "readSealed", int.class, byte[].class, int.class);
        Method verify = method(imageType, "verifySealed", int.class, byte[].class);
        Method indexOfPath = method(imageType, "fileIndexOf", String.class);
        byte[][] scratch = {new byte[7], new byte[1_024], new byte[16_384]};
        for (int i = 0; i < files; i++) {
            Object relative = call(path, image, i);
            byte[] expected = Files.readAllBytes(repository.resolve((String) relative));
            Object bytes = call(sealed, image, i);
            byte[] target = new byte[expected.length + 9];
            Arrays.fill(target, (byte) 0x5a);
            Object into = call(sealedInto, image, i, target, 4);
            StringBuilder verified = new StringBuilder();
            for (byte[] window : scratch) verified.append(format(call(verify, image, i, window))).append(',');
            boolean exact = bytes instanceof byte[] b && Arrays.equals(b, expected)
                    && Arrays.equals(Arrays.copyOfRange(target, 4, 4 + expected.length), expected)
                    && target[3] == 0x5a && target[4 + expected.length] == 0x5a;
            line(tag + " file " + i + " " + format(relative) + " len=" + invoke(length, image, i) + " sha="
                    + invoke(sha, image, i).substring(0, 16) + " sealed=" + format(bytes) + " into=" + format(into)
                    + " verify=" + verified + " exact=" + exact + " index=" + invoke(indexOfPath, image, relative));
            if (!exact || !fullSha(expected).equals(call(sha, image, i))) line(tag + " MISMATCH file " + i);
        }
        for (String miss : List.of("", "missing.java", "PKG00/File000.java", "pkg00/File000.java/", "\u96ea"))
            line(tag + " fileIndexOf(" + miss + ") -> " + invoke(indexOfPath, image, miss));
        line(tag + " fileIndexOf(null) -> " + invoke(indexOfPath, image, (Object) null));
        Method warm = method(imageType, "warm", long.class);
        for (long budget : new long[] {0, 1, 4_095, 4_097, 1_000_000, Long.MAX_VALUE, -1})
            line(tag + " warm(" + budget + ") -> " + invoke(warm, image, budget));
        for (int bad : new int[] {-1, files, Integer.MAX_VALUE}) {
            line(tag + " bad " + bad + " path=" + invoke(path, image, bad) + " len=" + invoke(length, image, bad)
                    + " sha=" + invoke(sha, image, bad) + " sealed=" + invoke(sealed, image, bad) + " into="
                    + invoke(sealedInto, image, bad, new byte[8], 0) + " verify=" + invoke(verify, image, bad, new byte[8]));
        }
        line(tag + " verify(0, empty) -> " + invoke(verify, image, 0, new byte[0]) + " verify(0, 16MiB+1) -> "
                + invoke(verify, image, 0, new byte[16 * MIB + 1]) + " verify(0, null) -> "
                + invoke(verify, image, 0, null) + " into(short) -> " + invoke(sealedInto, image, 7, new byte[8], 0)
                + " into(offset) -> " + invoke(sealedInto, image, 1, new byte[1 << 13], -1));
    }

    private void searches(String tag, Object image, Path repository, Random rng) throws IOException {
        Method indexOf = method(imageType, "indexOf", int.class, byte[].class);
        Method path = method(imageType, "relativePathAt", int.class);
        int files = (Integer) call(method(imageType, "fileCount"), image);
        for (int q = 0; q < 200; q++) {
            int file = rng.nextInt(files);
            byte[] content = Files.readAllBytes(repository.resolve((String) call(path, image, file)));
            byte[] pattern = pattern(rng, content, q);
            Object found = call(indexOf, image, file, pattern);
            long naive = naiveIndex(content, pattern);
            line(tag + " indexOf file=" + file + " len=" + pattern.length + " -> " + format(found)
                    + (found instanceof Long value && value == naive ? "" : " NAIVE=" + naive));
        }
        line(tag + " indexOf(-1) -> " + invoke(indexOf, image, -1, new byte[] {1}) + " indexOf(null) -> "
                + invoke(indexOf, image, 0, null) + " indexOf(1MiB+1) -> " + invoke(indexOf, image, 0, new byte[MIB + 1]));
    }

    private static byte[] pattern(Random rng, byte[] content, int q) {
        if (q % 10 == 0) return new byte[0];
        if (q % 10 == 1 || content.length == 0) {
            byte[] absent = new byte[1 + rng.nextInt(12)];
            rng.nextBytes(absent);
            return absent;
        }
        int start = rng.nextInt(content.length);
        int length = 1 + rng.nextInt(Math.min(64, content.length - start));
        return Arrays.copyOfRange(content, start, start + length);
    }

    private static long naiveIndex(byte[] content, byte[] pattern) {
        outer:
        for (int i = 0; i + pattern.length <= content.length; i++) {
            for (int j = 0; j < pattern.length; j++) if (content[i + j] != pattern[j]) continue outer;
            return i;
        }
        return -1;
    }

    private String rootDeterminism(Path repository, Object backend, Object wide) throws IOException {
        Path copy = root.resolve("repo-copy-" + format(backend));
        List<String> paths = listed(repository);
        for (String relative : paths) {
            Path target = copy.resolve(relative);
            Files.createDirectories(target.getParent());
            Files.copy(repository.resolve(relative), target);
        }
        List<String> reversed = new ArrayList<>(paths);
        Collections.reverse(reversed);
        Path first = root.resolve("det-a-" + format(backend));
        Path second = root.resolve("det-b-" + format(backend));
        String packs = invoke(pack, null, repository, paths, first, wide) + "/" + invoke(pack, null, copy, reversed, second, wide);
        Object a = call(open, null, first, backend, wide);
        Object b = call(open, null, second, backend, wide);
        Method rootOf = method(imageType, "sourceRoot");
        String same = format(call(rootOf, a)).equals(format(call(rootOf, b))) ? "same-root" : "DIFFERENT-ROOT";
        boolean manifests = Arrays.equals(Files.readAllBytes(first.resolve("manifest.bin")),
                Files.readAllBytes(second.resolve("manifest.bin")));
        closeQuietly(a);
        closeQuietly(b);
        return packs + " " + same + " manifests-equal=" + manifests;
    }

    private void closeQuietly(Object image) {
        if (image != null && !(image instanceof Refusal)) invoke(method(imageType, "close"), image);
    }

    private void closed(String tag, Object image) {
        Method close = method(imageType, "close");
        line(tag + " close -> " + invoke(close, image) + " again -> " + invoke(close, image));
        line(tag + " closed fileCount=" + invoke(method(imageType, "fileCount"), image) + " root="
                + invoke(method(imageType, "sourceRoot"), image) + " sealed="
                + invoke(method(imageType, "readSealed", int.class), image, 0) + " verify="
                + invoke(method(imageType, "verifySealed", int.class, byte[].class), image, 0, new byte[8]) + " index="
                + invoke(method(imageType, "fileIndexOf", String.class), image, "a") + " warm="
                + invoke(method(imageType, "warm", long.class), image, 1L));
    }

    // ---------------------------------------------------------------- refusals: paths, budgets, corruption, drift

    private void refusalOracle(Object backend) throws IOException {
        String tag = "REFUSE " + format(backend);
        Path repository = repository("refuse-" + format(backend), new Random(SEED + 2), 12);
        List<String> paths = listed(repository);
        Object wide = limits(400, 10_000_000L, MIB, 4_096);
        for (String unsafe : List.of("", "/abs.java", "a/../b", "a//b", "a\\b", "C:/x", "a:b", ".", "..", "a/./b",
                "nul\u0000", "trailing/", "\ud800")) {
            line(tag + " unsafe[" + unsafe.replace("\u0000", "\\0") + "] -> "
                    + invoke(pack, null, repository, List.of(unsafe), root.resolve("u-" + format(backend)), wide));
        }
        line(tag + " duplicate -> " + invoke(pack, null, repository, List.of(paths.get(0), paths.get(0)),
                root.resolve("dup-" + format(backend)), wide));
        line(tag + " missing -> " + invoke(pack, null, repository, List.of("absent.java"), root.resolve("miss-" + format(backend)), wide));
        line(tag + " directory -> " + invoke(pack, null, repository, List.of("pkg00"), root.resolve("dir-" + format(backend)), wide));
        Path existing = Files.createDirectories(root.resolve("exists-" + format(backend)));
        line(tag + " existing -> " + invoke(pack, null, repository, paths, existing, wide));
        line(tag + " count-budget -> " + invoke(pack, null, repository, paths, root.resolve("c-" + format(backend)), limits(3, 10_000_000L, MIB, 64)));
        line(tag + " corpus-budget -> " + invoke(pack, null, repository, paths, root.resolve("b-" + format(backend)), limits(400, 100L, MIB, 64)));
        line(tag + " file-budget -> " + invoke(pack, null, repository, paths, root.resolve("f-" + format(backend)), limits(400, 10_000_000L, 16, 64)));
        for (int[] bad : new int[][] {{-1, 1, 1}, {1_000_001, 1, 1}, {1, -1, 1}, {1, 16 * MIB + 1, 1}, {1, 1, 0}, {1, 1, 16 * MIB + 1}})
            line(tag + " limits" + Arrays.toString(bad) + " -> " + format(limits(bad[0], 10L, bad[1], bad[2])));
        Path image = root.resolve("good-" + format(backend));
        line(tag + " pack -> " + invoke(pack, null, repository, paths, image, wide));
        line(tag + " reopen-budget -> " + format(openClose(image, backend, limits(2, 10_000_000L, MIB, 64))));
        corruption(tag, image, backend, wide);
        drift(tag, image, backend, wide);
    }

    private Object openClose(Path image, Object backend, Object limit) {
        Object opened = call(open, null, image, backend, limit);
        if (opened instanceof Refusal) return opened;
        closeQuietly(opened);
        return "opened";
    }

    private void corruption(String tag, Path image, Object backend, Object wide) throws IOException {
        byte[] manifest = Files.readAllBytes(image.resolve("manifest.bin"));
        byte[] backing = Files.readAllBytes(image.resolve("source.bin"));
        int[] flips = {0, 9, 13, 17, 25, 30, manifest.length / 2, manifest.length - 1};
        for (int at : flips) {
            Path copy = variant(image, "flip" + at, manifest, backing);
            byte[] bad = manifest.clone();
            bad[at] ^= 0x41;
            Files.write(copy.resolve("manifest.bin"), bad);
            line(tag + " manifest-flip@" + at + " -> " + format(openClose(copy, backend, wide)));
        }
        Path trailing = variant(image, "trailing", manifest, backing);
        Files.write(trailing.resolve("manifest.bin"), Arrays.copyOf(manifest, manifest.length + 1));
        line(tag + " manifest-trailing -> " + format(openClose(trailing, backend, wide)));
        Path truncated = variant(image, "truncated", manifest, backing);
        Files.write(truncated.resolve("source.bin"), Arrays.copyOf(backing, backing.length - 1));
        line(tag + " backing-truncated -> " + format(openClose(truncated, backend, wide)));
        Path grown = variant(image, "grown", manifest, backing);
        Files.write(grown.resolve("source.bin"), Arrays.copyOf(backing, backing.length + 1));
        line(tag + " backing-grown -> " + format(openClose(grown, backend, wide)));
        Path drifted = variant(image, "drifted", manifest, backing);
        byte[] changed = backing.clone();
        changed[changed.length / 3] ^= 1;
        Files.write(drifted.resolve("source.bin"), changed);
        line(tag + " backing-drift -> " + format(openClose(drifted, backend, wide)));
        Path missing = variant(image, "missing", manifest, backing);
        Files.delete(missing.resolve("manifest.bin"));
        line(tag + " manifest-missing -> " + format(openClose(missing, backend, wide)));
    }

    private Path variant(Path image, String name, byte[] manifest, byte[] backing) throws IOException {
        Path copy = Files.createDirectories(image.resolveSibling(image.getFileName() + "-" + name));
        Files.write(copy.resolve("manifest.bin"), manifest);
        Files.write(copy.resolve("source.bin"), backing);
        return copy;
    }

    private void drift(String tag, Path image, Object backend, Object wide) throws IOException {
        Path copy = variant(image, "live", Files.readAllBytes(image.resolve("manifest.bin")),
                Files.readAllBytes(image.resolve("source.bin")));
        Object opened = call(open, null, copy, backend, wide);
        line(tag + " live open -> " + format(opened));
        if (opened instanceof Refusal) return;
        Method sealed = method(imageType, "readSealed", int.class);
        Method verify = method(imageType, "verifySealed", int.class, byte[].class);
        int files = (Integer) call(method(imageType, "fileCount"), opened);
        try (FileChannel channel = FileChannel.open(copy.resolve("source.bin"), StandardOpenOption.WRITE,
                StandardOpenOption.READ)) {
            ByteBuffer one = ByteBuffer.allocate(1);
            channel.read(one, 0);
            one.flip();
            byte value = one.get(0);
            channel.write(ByteBuffer.wrap(new byte[] {(byte) (value ^ 0x7f)}), 0);
            channel.force(true);
        }
        StringBuilder results = new StringBuilder();
        for (int i = 0; i < files; i++) {
            results.append(i).append('=').append(invoke(sealed, opened, i)).append('|')
                    .append(invoke(verify, opened, i, new byte[1_024])).append(' ');
        }
        line(tag + " live drift " + results.toString().trim());
        closeQuietly(opened);
    }

    // ---------------------------------------------------------------- source slices over an SDK region

    private void slicesOracle(Object backend) throws IOException {
        String tag = "SLICES " + format(backend);
        byte[] image = new byte[8_192];
        new Random(SEED + 3).nextBytes(image);
        Path file = root.resolve("slices-" + format(backend) + ".bin");
        Files.write(file, image);
        long[] offsets = {0, 100, 100, 4_000, 8_000};
        int[] lengths = {100, 0, 3_000, 4_000, 192};
        byte[] digests = new byte[offsets.length * 32];
        for (int i = 0; i < offsets.length; i++) {
            byte[] digest = sha256().digest(Arrays.copyOfRange(image, (int) offsets[i], (int) offsets[i] + lengths[i]));
            System.arraycopy(digest, 0, digests, i * 32, 32);
        }
        Object region = call(sdkOpen, null, file, 0L, (long) image.length, readOnly, backend);
        line(tag + " region -> " + format(region));
        if (region instanceof Refusal) return;
        Object view = construct(slices, region, offsets, lengths, digests, 4_096);
        line(tag + " view -> " + format(view));
        Method count = method(slicesType, "fileCount");
        Method sealed = method(slicesType, "readSealed", int.class);
        Method into = method(slicesType, "readSealed", int.class, byte[].class, int.class);
        Method sha = method(slicesType, "sha256At", int.class);
        line(tag + " fileCount=" + invoke(count, view));
        for (int i = -1; i <= offsets.length; i++) {
            line(tag + " " + i + " sha=" + invoke(sha, view, i) + " sealed=" + invoke(sealed, view, i) + " into="
                    + invoke(into, view, i, new byte[4_200], 7) + " intoTight=" + invoke(into, view, i, new byte[100], 1));
        }
        geometry(tag, region, offsets, lengths, digests);
        Object writable = call(sdkOpen, null, file, 0L, (long) image.length, readWrite, backend);
        line(tag + " read-write source -> " + format(construct(slices, writable, offsets, lengths, digests, 4_096)));
        closeRegion(writable);
        closeRegion(region);
        line(tag + " after-close fileCount=" + invoke(count, view) + " empty=" + invoke(sealed, view, 1) + " emptyInto="
                + invoke(into, view, 1, new byte[1], 0) + " nonempty=" + invoke(sealed, view, 0));
        line(tag + " closed source -> " + format(construct(slices, region, offsets, lengths, digests, 4_096)));
    }

    private void geometry(String tag, Object region, long[] offsets, int[] lengths, byte[] digests) {
        line(tag + " null-source -> " + format(construct(slices, null, offsets, lengths, digests, 4_096)));
        line(tag + " mismatch -> " + format(construct(slices, region, offsets, new int[] {1}, digests, 4_096)));
        line(tag + " digest-length -> " + format(construct(slices, region, offsets, lengths, new byte[3], 4_096)));
        line(tag + " overlap -> " + format(construct(slices, region, new long[] {0, 50}, new int[] {100, 10}, new byte[64], 4_096)));
        line(tag + " negative -> " + format(construct(slices, region, new long[] {0}, new int[] {-1}, new byte[32], 4_096)));
        line(tag + " beyond -> " + format(construct(slices, region, new long[] {8_000}, new int[] {193}, new byte[32], 4_096)));
        line(tag + " start-beyond -> " + format(construct(slices, region, new long[] {8_193}, new int[] {0}, new byte[32], 4_096)));
        line(tag + " over-budget -> " + format(construct(slices, region, new long[] {0}, new int[] {4_097}, new byte[32], 4_096)));
        line(tag + " budget-zero -> " + format(construct(slices, region, offsets, lengths, digests, 0)));
        line(tag + " budget-16MiB+1 -> " + format(construct(slices, region, offsets, lengths, digests, 16 * MIB + 1)));
        line(tag + " long-span -> " + format(construct(slices, region, new long[] {Long.MAX_VALUE}, new int[] {1}, new byte[32], 4_096)));
    }

    private void closeRegion(Object region) {
        if (region instanceof Refusal || region == null) return;
        line("region close -> " + invoke(method(regionType, "close"), region));
    }

    // ---------------------------------------------------------------- corpus atom (POSIX only)

    private void corpusAtom() throws IOException {
        Path atomRoot = Files.createDirectories(root.resolve("atom"));
        byte[] raw = new byte[150_123];
        for (int i = 0; i < raw.length; i++) raw[i] = (byte) (i * 17 + 13);
        Files.write(atomRoot.resolve("source.java"), raw);
        String hash = fullSha(raw);
        long[][] ranges = {{0, 0}, {0, 1}, {0, raw.length}, {65_535, 8}, {65_533, 65_545}, {raw.length - 1, 1},
            {raw.length, 0}, {Long.MAX_VALUE, 1}, {0, MIB + 1}, {-1, 1}};
        for (long[] range : ranges) line("ATOM " + range[0] + "+" + range[1] + " -> " + atom(atomRoot, "source.java", raw.length, hash, range[0], (int) range[1]));
        for (String bad : List.of("../source.java", "source.java/../source.java", "/source.java", "", "a\\b", "x\u0001"))
            line("ATOM path[" + bad.replace("\u0001", "\\1") + "] -> " + atom(atomRoot, bad, raw.length, hash, 0, 1));
        line("ATOM length+1 -> " + atom(atomRoot, "source.java", raw.length + 1, hash, 0, 1));
        line("ATOM zero-hash -> " + atom(atomRoot, "source.java", raw.length, "0".repeat(64), 0, 1));
        line("ATOM bad-hash -> " + atom(atomRoot, "source.java", raw.length, "BAD", 0, 1));
        Files.write(atomRoot.resolve("empty.java"), new byte[0]);
        line("ATOM empty -> " + atom(atomRoot, "empty.java", 0, fullSha(new byte[0]), 0, 0));
        Files.write(atomRoot.resolve("source.java"), "changed".getBytes(StandardCharsets.UTF_8));
        line("ATOM changed -> " + atom(atomRoot, "source.java", raw.length, hash, 0, 1));
    }

    private String atom(Path atomRoot, String relative, long length, String hash, long start, int width) {
        Object atom = call(atomRead, null, atomRoot, relative, length, hash, start, width);
        if (atom instanceof Refusal r) return r.text();
        try {
            Class<?> type = atom.getClass();
            return "len=" + accessible(type.getDeclaredMethod("sourceLength")).invoke(atom) + " sha="
                    + ((String) accessible(type.getDeclaredMethod("sha256")).invoke(atom)).substring(0, 16) + " start="
                    + accessible(type.getDeclaredMethod("start")).invoke(atom) + " bytes="
                    + format(accessible(type.getDeclaredMethod("bytes")).invoke(atom)) + " atomSha="
                    + ((String) accessible(type.getDeclaredMethod("atomSha256")).invoke(atom)).substring(0, 16);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("atom accessors", e);
        }
    }

    // ---------------------------------------------------------------- seeded random operations

    private void random(int count, List<Object> backends) throws IOException {
        Random rng = new Random(SEED + 4);
        List<Object> images = new ArrayList<>();
        List<Path> repositories = new ArrayList<>();
        for (int r = 0; r < 3; r++) {
            Path repository = repository("random-" + r, rng, 40 + 30 * r);
            Path image = root.resolve("random-image-" + r);
            invoke(pack, null, repository, listed(repository), image, limits(400, 10_000_000L, MIB, 1_024 + r));
            for (Object backend : backends) {
                images.add(call(open, null, image, backend, limits(400, 10_000_000L, MIB, 1_024 + r)));
                repositories.add(repository);
            }
        }
        Method[] ops = {method(imageType, "readSealed", int.class), method(imageType, "readSealed", int.class, byte[].class, int.class),
            method(imageType, "verifySealed", int.class, byte[].class), method(imageType, "fileIndexOf", String.class),
            method(imageType, "indexOf", int.class, byte[].class), method(imageType, "warm", long.class),
            method(imageType, "lengthAt", int.class), method(imageType, "sha256At", int.class),
            method(imageType, "relativePathAt", int.class)};
        for (int n = 0; n < count; n++) {
            int slot = rng.nextInt(images.size());
            Object image = images.get(slot);
            int files = (Integer) call(method(imageType, "fileCount"), image);
            int file = rng.nextInt(10) == 0 ? rng.nextInt(files + 4) - 2 : rng.nextInt(files);
            int op = rng.nextInt(ops.length);
            String result = switch (op) {
                case 0, 6, 7, 8 -> invoke(ops[op], image, file);
                case 1 -> invoke(ops[op], image, file, new byte[rng.nextInt(6_000)], rng.nextInt(9) - 1);
                case 2 -> invoke(ops[op], image, file, new byte[rng.nextInt(20) == 0 ? 0 : 1 + rng.nextInt(5_000)]);
                case 3 -> invoke(ops[op], image, rng.nextBoolean() && file >= 0 && file < files
                        ? call(method(imageType, "relativePathAt", int.class), image, file) : randomName(rng, n));
                case 4 -> invoke(ops[op], image, file, randomPattern(rng, repositories.get(slot), image, file, files));
                default -> invoke(ops[op], image, (long) rng.nextInt(400_000) - 10);
            };
            line("R " + n + " s" + slot + " op" + op + " f" + file + " -> " + result);
        }
        line("RANDOM operations=" + count);
        images.forEach(this::closeQuietly);
    }

    private byte[] randomPattern(Random rng, Path repository, Object image, int file, int files) throws IOException {
        if (file < 0 || file >= files || rng.nextInt(4) == 0) {
            byte[] absent = new byte[rng.nextInt(9)];
            rng.nextBytes(absent);
            return absent;
        }
        byte[] content = Files.readAllBytes(repository.resolve((String) call(method(imageType, "relativePathAt", int.class), image, file)));
        return pattern(rng, content, rng.nextInt(10));
    }

    private static void deleteTree(Path tree) throws IOException {
        try (Stream<Path> walk = Files.walk(tree)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
}
