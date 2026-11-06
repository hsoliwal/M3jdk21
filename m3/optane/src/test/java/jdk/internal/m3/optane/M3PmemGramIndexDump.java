// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.optane;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

/**
 * Differential dump driver of the PMem gram index (gate 3 of recipe/pmem-gram-index-20261106; not a test). The donor
 * family (the SDK and campaign packages of RELOCATION_MAP.tsv, type prefix "") and the relocated family (jdk.m3.optane and
 * jdk.internal.m3.optane, type prefix "M3") replay the same seeded inputs through reflection over real source images
 * (pack/open of the family's SDK; UTF-8 text, random binary, repeated-prefix adversaries, dense 50,000-byte binaries,
 * empty and tiny files): index builds with every budget and cancellation refusal, then queries whose gram-filtered result
 * must equal the exact selected first-hit search hit for hit (random and full selections, binary/UTF-8/empty/one- and
 * two-byte patterns, repeated-prefix adversaries, hit budgets) and equal an independent naive Java search; every selection,
 * budget, pattern, generation and closed-image refusal; the whole-image corpus search against the naive search; a source
 * drift before the build; then {@code operations} seeded random queries. In native mode (Linux, libm3pmemsdk and
 * libm3gramoracle on java.library.path) the image also opens over POSIX and every exact result is checked against the
 * independent C17 brute-force oracle (M3NativeByteOracle). Disagreements print UNEXPECTED; every result and refusal
 * (exception class and erased message) is printed; temporary paths, the package names and the M3 type prefix are erased,
 * so the two dumps must be byte-identical.
 * usage: M3PmemGramIndexDump &lt;sdk package&gt; &lt;campaign package&gt; &lt;type prefix&gt; &lt;operations&gt; [native]
 */
public final class M3PmemGramIndexDump {
    private static final long SEED = 20_261_106L;
    private static final int FILES = 240;
    private static final int MIB = 1 << 20;

    private final String sdk;
    private final String campaign;
    private final String prefix;
    private final boolean nativeMode;
    private final PrintStream out;
    private final Path root;
    private final Class<?> imageType;
    private final Method pack;
    private final Method open;
    private final Object limits;
    private final Method build;
    private final Method gramHits;
    private final Method selectedHits;
    private final Method corpusHits;
    private final Method readSealed;
    private final Method fileCount;
    private final Method close;
    private final Object fileBackend;
    private final Object posixBackend;
    private final Method oracle;
    private long exceptions;
    private long operations;

    private M3PmemGramIndexDump(String sdk, String campaign, String prefix, boolean nativeMode, PrintStream out)
            throws Exception {
        this.sdk = sdk;
        this.campaign = campaign;
        this.prefix = prefix;
        this.nativeMode = nativeMode;
        this.out = out;
        this.root = Files.createTempDirectory("m3-pmem-gram-dump-").toRealPath();
        imageType = Class.forName(sdk + "." + prefix + "PmemSourceImage");
        Class<?> limitsType = Class.forName(sdk + "." + prefix + "PmemSourceImage$Limits");
        Class<?> backend = Class.forName(sdk + "." + prefix + "PmemBackend");
        pack = imageType.getMethod("pack", Path.class, List.class, Path.class, limitsType);
        open = imageType.getMethod("open", Path.class, backend, limitsType);
        limits = limitsType.getConstructor(int.class, long.class, int.class, int.class).newInstance(1_000, 100_000_000L, MIB, 8_192);
        Class<?> gram = Class.forName(campaign + "." + prefix + "PmemCorpusGramIndex");
        Class<?> selected = Class.forName(campaign + "." + prefix + "PmemCorpusSelectedSearch");
        Class<?> corpus = Class.forName(campaign + "." + prefix + "PmemCorpusSearch");
        build = accessible(gram.getDeclaredMethod("build", imageType, long.class, BooleanSupplier.class));
        gramHits = accessible(gram.getDeclaredMethod("firstHits", imageType, int[].class, byte[].class, int.class, BooleanSupplier.class));
        selectedHits = accessible(selected.getDeclaredMethod("firstHits", imageType, int[].class, byte[].class, int.class, BooleanSupplier.class));
        corpusHits = accessible(corpus.getDeclaredMethod("firstHits", imageType, byte[].class, int.class, BooleanSupplier.class));
        readSealed = imageType.getMethod("readSealed", int.class);
        fileCount = imageType.getMethod("fileCount");
        close = imageType.getMethod("close");
        fileBackend = constant(backend, "FILE");
        posixBackend = constant(backend, "POSIX");
        oracle = nativeMode ? accessible(Class.forName("jdk.m3.optane.M3NativeByteOracle")
                .getDeclaredMethod("firstHit", byte[].class, byte[].class)) : null;
    }

    /** The dump of one family as text (used by the JUnit wrapper); {@link #main} prints the same to stdout. */
    public static String replay(String sdk, String campaign, String prefix, int operations, boolean nativeMode)
            throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(1 << 20);
        try (PrintStream stream = new PrintStream(bytes, false, StandardCharsets.UTF_8)) {
            new M3PmemGramIndexDump(sdk, campaign, prefix, nativeMode, stream).run(operations);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("usage: M3PmemGramIndexDump <sdk package> <campaign package> <type prefix> <operations> [native]");
            System.exit(2);
        }
        boolean nativeMode = args.length > 4 && args[4].equals("native");
        PrintStream stream = new PrintStream(System.out, false, StandardCharsets.UTF_8);
        new M3PmemGramIndexDump(args[0], args[1], args[2], nativeMode, stream).run(Integer.parseInt(args[3]));
        stream.flush();
    }

    private void run(int count) throws Exception {
        try {
            line("M3_PMEM_GRAM_INDEX_DUMP native=" + nativeMode + " operations=" + count);
            Path packed = image("A", new Random(SEED), FILES);
            List<Object> backends = new ArrayList<>(List.of(fileBackend));
            if (nativeMode) backends.add(posixBackend);
            for (Object backend : backends) {
                Object image = call(open, null, packed, backend, limits);
                line("IMAGE " + format(backend) + " -> " + format(image));
                List<byte[]> files = contents(image);
                Object index = builds(image);
                queries(format(backend), image, index, files, new Random(SEED + 1));
                refusals(image, index, packed);
                corpus(image, files);
                random(count, image, index, files, new Random(SEED + 2));
                line("close -> " + invoke(close, image) + " closed query -> "
                        + invoke(gramHits, index, image, new int[] {0}, new byte[] {1}, 1, never()));
            }
            drift();
            line("TOTAL operations=" + operations + " exceptions=" + exceptions);
        } finally {
            deleteTree(root);
        }
    }

    // ---------------------------------------------------------------- reflection and formatting

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

    private String refusal(Throwable cause) {
        exceptions++;
        return "!" + cause.getClass().getSimpleName() + ": " + erase(cause.getMessage());
    }

    private String format(Object result) {
        if (result == null) return "ok";
        if (result instanceof Refusal r) return r.text();
        if (result instanceof Enum<?> e) return e.name();
        if (result instanceof String || result instanceof Number || result instanceof Boolean) return erase(result.toString());
        return "object";
    }

    private String erase(String text) {
        if (text == null) return "null";
        String erased = text.replace(root.toString(), "<tmp>").replace(root.toString().replace('\\', '/'), "<tmp>")
                .replace(campaign + ".", "").replace(sdk + ".", "");
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

    private record Refusal(String text) { }

    /** A package-private accessor of a result object, by name. */
    private Object get(Object target, String name, Object... args) {
        try {
            Class<?>[] types = Arrays.stream(args).map(a -> int.class).toArray(Class<?>[]::new);
            return accessible(target.getClass().getDeclaredMethod(name, types)).invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("accessor " + name, e);
        }
    }

    private static BooleanSupplier never() {
        return () -> false;
    }

    private static BooleanSupplier after(int calls) {
        int[] seen = {0};
        return () -> ++seen[0] > calls;
    }

    // ---------------------------------------------------------------- images

    private Path image(String name, Random rng, int count) throws IOException {
        Path repository = Files.createDirectories(root.resolve("repo-" + name));
        List<String> paths = new ArrayList<>();
        String[] words = {"class", "final", "return", "M3GRAM", "void", "雪", "byte", "index", "ab", "abc"};
        for (int i = 0; i < count; i++) {
            String relative = String.format(java.util.Locale.ROOT, "p%02d/F%04d.%s", i % 9, i, i % 6 == 0 ? "java" : "bin");
            byte[] content = switch (i % 6) {
                case 0 -> {
                    StringBuilder text = new StringBuilder();
                    int length = rng.nextInt(400);
                    for (int w = 0; w < length; w++) text.append(words[rng.nextInt(words.length)]).append(w % 13 == 0 ? '\n' : ' ');
                    yield text.toString().getBytes(StandardCharsets.UTF_8);
                }
                case 1 -> random(rng, rng.nextInt(6_000));
                case 2 -> {
                    byte[] adversary = new byte[100 + rng.nextInt(3_000)];
                    Arrays.fill(adversary, (byte) 'a');
                    adversary[adversary.length - 1 - rng.nextInt(50)] = 'b';
                    yield adversary;
                }
                case 3 -> i % 12 == 3 ? random(rng, 50_000) : random(rng, 200);
                case 4 -> random(rng, rng.nextInt(4));
                default -> {
                    byte[] zeros = new byte[1_000 + rng.nextInt(9_000)];
                    if (i % 11 == 5) System.arraycopy("M3GRAM".getBytes(StandardCharsets.US_ASCII), 0, zeros, 213, 6);
                    yield zeros;
                }
            };
            Path file = repository.resolve(relative);
            Files.createDirectories(file.getParent());
            Files.write(file, content);
            paths.add(relative);
        }
        Path packed = root.resolve("image-" + name);
        line("PACK " + name + " files=" + count + " -> " + invoke(pack, null, repository, paths, packed, limits));
        return packed;
    }

    private static byte[] random(Random rng, int length) {
        byte[] bytes = new byte[length];
        rng.nextBytes(bytes);
        return bytes;
    }

    private List<byte[]> contents(Object image) {
        int files = (Integer) call(fileCount, image);
        List<byte[]> contents = new ArrayList<>(files);
        for (int i = 0; i < files; i++) contents.add((byte[]) call(readSealed, image, i));
        return contents;
    }

    // ---------------------------------------------------------------- lanes

    private Object builds(Object image) {
        for (long budget : new long[] {-1, 0, 1, 300_000, 512L * MIB + 1})
            line("BUILD budget=" + budget + " -> " + describeIndex(call(build, null, image, budget, never())));
        for (int calls : new int[] {0, 1, 100, 239, 479})
            line("BUILD cancel-after=" + calls + " -> " + describeIndex(call(build, null, image, 64L * MIB, after(calls))));
        line("BUILD null-image -> " + invoke(build, null, null, 64L * MIB, never()) + " null-cancel -> "
                + invoke(build, null, image, 64L * MIB, null));
        Object index = call(build, null, image, 64L * MIB, never());
        line("BUILD index -> " + describeIndex(index) + " again-equal="
                + get(index, "indexSha256").equals(get(call(build, null, image, 64L * MIB, never()), "indexSha256")));
        StringBuilder frequencies = new StringBuilder();
        for (int gram : new int[] {0, 0x6162, 0x6161, 0x4d33, 0xffff, 0x0a63, 0x2020}) {
            frequencies.append(Integer.toHexString(gram)).append('=').append(get(index, "documentFrequency", gram)).append(' ');
        }
        line("FREQUENCY " + frequencies.toString().trim());
        return index;
    }

    private String describeIndex(Object index) {
        if (index instanceof Refusal r) return r.text();
        return "files=" + get(index, "fileCount") + " dense=" + get(index, "denseFileCount") + " retained="
                + get(index, "retainedPrimitiveBytes") + " index=" + ((String) get(index, "indexSha256")).substring(0, 16)
                + " root=" + ((String) get(index, "sourceRoot")).substring(0, 16);
    }

    /**
     * Every first hit of a selection by naive search over the sealed contents (and by the C17 oracle in native mode),
     * without the hit budget: {@link #matches} applies the budget and derives the expected completeness.
     */
    private long[][] expected(List<byte[]> files, int[] selection, byte[] pattern) {
        List<long[]> hits = new ArrayList<>();
        for (int file : selection) {
            long found = naive(files.get(file), pattern);
            if (oracle != null) {
                try {
                    long c = (Long) oracle.invoke(null, files.get(file), pattern);
                    if (c != found) line("UNEXPECTED C17 oracle file=" + file + " c=" + c + " java=" + found);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("C17 oracle", e);
                }
            }
            if (found >= 0) hits.add(new long[] {file, found});
        }
        return hits.toArray(long[][]::new);
    }

    private static long naive(byte[] content, byte[] pattern) {
        outer:
        for (int i = 0; i + pattern.length <= content.length; i++) {
            for (int j = 0; j < pattern.length; j++) if (content[i + j] != pattern[j]) continue outer;
            return i;
        }
        return -1;
    }

    private String hits(Object result) {
        int count = (Integer) get(result, "hitCount");
        MessageDigest digest = sha256();
        StringBuilder first = new StringBuilder();
        for (int h = 0; h < count; h++) {
            String hit = get(result, "fileIndexAt", h) + "@" + get(result, "fileOffsetAt", h);
            digest.update(hit.getBytes(StandardCharsets.UTF_8));
            if (h < 3) first.append(hit).append(',');
        }
        return "hits=" + count + " [" + first + "] digest=" + HexFormat.of().formatHex(digest.digest(), 0, 6);
    }

    /**
     * The gram result equals the exact selected result hit for hit; completeness may differ only where the contract
     * allows it (the gram result is complete once every candidate is searched, the exact one once every selected file
     * is), so an exact complete result with an incomplete gram result is a disagreement.
     */
    private boolean sameHits(Object gram, Object exact) {
        int count = (Integer) get(gram, "hitCount");
        if (count != (Integer) get(exact, "hitCount")) return false;
        if ((Boolean) get(exact, "complete") && !(Boolean) get(gram, "complete")) return false;
        for (int h = 0; h < count; h++) {
            if (!get(gram, "fileIndexAt", h).equals(get(exact, "fileIndexAt", h))
                    || !get(gram, "fileOffsetAt", h).equals(get(exact, "fileOffsetAt", h))) return false;
        }
        return true;
    }

    /**
     * The exact search stops at the hit budget: it returns the first {@code maxHits} hits of the selection and is complete
     * when fewer hits exist or the last of them is in the last selected file.
     */
    private boolean matches(Object result, long[][] all, int[] selection, int maxHits) {
        long[][] expected = all.length > maxHits ? Arrays.copyOf(all, maxHits) : all;
        boolean complete = all.length < maxHits
                || selection.length > 0 && expected[expected.length - 1][0] == selection[selection.length - 1];
        if ((Boolean) get(result, "complete") != complete || (Integer) get(result, "hitCount") != expected.length) return false;
        for (int h = 0; h < expected.length; h++) {
            if ((Integer) get(result, "fileIndexAt", h) != expected[h][0] || (Long) get(result, "fileOffsetAt", h) != expected[h][1]) {
                return false;
            }
        }
        return true;
    }

    private String query(String tag, Object image, Object index, List<byte[]> files, int[] selection, byte[] pattern, int maxHits) {
        Object gram = call(gramHits, index, image, selection, pattern, maxHits, never());
        Object exact = call(selectedHits, null, image, selection, pattern, maxHits, never());
        if (gram instanceof Refusal || exact instanceof Refusal) {
            return tag + " gram=" + format(gram) + " exact=" + format(exact);
        }
        boolean same = sameHits(gram, exact);
        boolean oracleMatch = matches(exact, expected(files, selection, pattern), selection, maxHits);
        return tag + " selected=" + get(gram, "selectedFiles") + " pruned=" + get(gram, "prunedFiles") + " candidates="
                + get(gram, "candidateFiles") + " searched=" + get(gram, "searchedCandidates") + " complete="
                + get(gram, "complete") + "/" + get(exact, "complete") + " " + hits(gram) + (same ? " EQUAL" : " UNEXPECTED gram!=exact " + hits(exact))
                + (oracleMatch ? " ORACLE" : " UNEXPECTED oracle");
    }

    private void queries(String backend, Object image, Object index, List<byte[]> files, Random rng) {
        int count = files.size();
        int[] all = new int[count];
        for (int i = 0; i < count; i++) all[i] = i;
        List<byte[]> patterns = new ArrayList<>(List.of(new byte[0], new byte[] {'a'}, "ab".getBytes(StandardCharsets.US_ASCII),
                "aab".getBytes(StandardCharsets.US_ASCII), "aaaaaaaab".getBytes(StandardCharsets.US_ASCII),
                "M3GRAM".getBytes(StandardCharsets.US_ASCII), "雪".getBytes(StandardCharsets.UTF_8),
                "class final".getBytes(StandardCharsets.UTF_8), "XYZ-not-there".getBytes(StandardCharsets.US_ASCII),
                new byte[] {0, 0, 0}, new byte[] {0}, "a".repeat(200).getBytes(StandardCharsets.US_ASCII)));
        for (int q = 0; q < 24; q++) patterns.add(slice(rng, files));
        for (int p = 0; p < patterns.size(); p++) {
            for (int maxHits : new int[] {1, 7, 20_000}) {
                line(query("QUERY " + backend + " all p" + p + " len=" + patterns.get(p).length + " max=" + maxHits, image, index,
                        files, all, patterns.get(p), maxHits));
            }
            line(query("QUERY " + backend + " subset p" + p, image, index, files, subset(rng, count), patterns.get(p), 20_000));
        }
        line(query("QUERY " + backend + " empty-selection", image, index, files, new int[0], patterns.get(5), 5));
    }

    private static byte[] slice(Random rng, List<byte[]> files) {
        for (int attempt = 0; attempt < 32; attempt++) {
            byte[] content = files.get(rng.nextInt(files.size()));
            if (content.length < 2) continue;
            int start = rng.nextInt(content.length - 1);
            int length = 1 + rng.nextInt(Math.min(48, content.length - start));
            return Arrays.copyOfRange(content, start, start + length);
        }
        return new byte[] {7, 7};
    }

    private static int[] subset(Random rng, int count) {
        int[] picked = new int[count];
        int size = 0;
        int rate = 1 + rng.nextInt(5);
        for (int i = 0; i < count; i++) if (rng.nextInt(rate) == 0) picked[size++] = i;
        return Arrays.copyOf(picked, size);
    }

    private void refusals(Object image, Object index, Path packed) {
        byte[] p = "ab".getBytes(StandardCharsets.US_ASCII);
        int files = (Integer) call(fileCount, image);
        int[][] selections = {{1, 0}, {3, 3}, {-1}, {files}, {0, files - 1, files}};
        for (int[] selection : selections) {
            line("REFUSE selection " + Arrays.toString(selection) + " gram=" + invoke(gramHits, index, image, selection, p, 5, never())
                    + " exact=" + invoke(selectedHits, null, image, selection, p, 5, never()));
        }
        int[] tooMany = new int[20_001];
        for (int i = 0; i < tooMany.length; i++) tooMany[i] = i;
        line("REFUSE selection 20001 gram=" + invoke(gramHits, index, image, tooMany, p, 5, never()) + " exact="
                + invoke(selectedHits, null, image, tooMany, p, 5, never()));
        for (int maxHits : new int[] {0, -1, 20_001})
            line("REFUSE maxHits " + maxHits + " gram=" + invoke(gramHits, index, image, new int[] {0}, p, maxHits, never())
                    + " exact=" + invoke(selectedHits, null, image, new int[] {0}, p, maxHits, never()));
        byte[] huge = new byte[MIB + 1];
        line("REFUSE pattern 1MiB+1 gram=" + invoke(gramHits, index, image, new int[] {0}, huge, 1, never()) + " exact="
                + invoke(selectedHits, null, image, new int[] {0}, huge, 1, never()));
        line("REFUSE nulls gram=" + invoke(gramHits, index, null, new int[] {0}, p, 1, never()) + " | "
                + invoke(gramHits, index, image, null, p, 1, never()) + " | " + invoke(gramHits, index, image, new int[] {0}, null, 1, never())
                + " | " + invoke(gramHits, index, image, new int[] {0}, p, 1, null));
        int[] all = new int[files];
        for (int i = 0; i < files; i++) all[i] = i;
        for (int calls : new int[] {0, 1, 50})
            line("REFUSE cancel-after=" + calls + " gram=" + invoke(gramHits, index, image, all, p, 20_000, after(calls))
                    + " exact=" + invoke(selectedHits, null, image, all, p, 20_000, after(calls)));
        try {
            Path other = image("B", new Random(SEED + 9), 30);
            Object second = call(open, null, other, fileBackend, limits);
            line("REFUSE other-generation gram=" + invoke(gramHits, index, second, new int[] {0}, p, 1, never()));
            invoke(close, second);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void corpus(Object image, List<byte[]> files) {
        List<byte[]> patterns = List.of(new byte[0], "M3GRAM".getBytes(StandardCharsets.US_ASCII), "ab".getBytes(StandardCharsets.US_ASCII),
                "aaaaaaaab".getBytes(StandardCharsets.US_ASCII), "absent-pattern".getBytes(StandardCharsets.US_ASCII));
        int[] all = new int[files.size()];
        for (int i = 0; i < all.length; i++) all[i] = i;
        for (byte[] pattern : patterns) {
            for (int maxHits : new int[] {1, 20_000}) {
                Object result = call(corpusHits, null, image, pattern, maxHits, never());
                if (result instanceof Refusal r) {
                    line("CORPUS len=" + pattern.length + " max=" + maxHits + " -> " + r.text());
                    continue;
                }
                boolean oracleMatch = matches(result, expected(files, all, pattern), all, maxHits);
                line("CORPUS len=" + pattern.length + " max=" + maxHits + " scanned=" + get(result, "scannedFiles") + " "
                        + hits(result) + (oracleMatch ? " ORACLE" : " UNEXPECTED oracle"));
            }
        }
        line("CORPUS refusals max0=" + invoke(corpusHits, null, image, new byte[] {1}, 0, never()) + " huge="
                + invoke(corpusHits, null, image, new byte[MIB + 1], 1, never()) + " null="
                + invoke(corpusHits, null, image, null, 1, never()) + " cancel="
                + invoke(corpusHits, null, image, new byte[] {1}, 20_000, after(3)));
    }

    private void random(int count, Object image, Object index, List<byte[]> files, Random rng) {
        int size = files.size();
        for (int n = 0; n < count; n++) {
            int[] selection = rng.nextInt(20) == 0 ? new int[] {rng.nextInt(size), rng.nextInt(size)} : subset(rng, size);
            byte[] pattern = switch (rng.nextInt(6)) {
                case 0 -> new byte[rng.nextInt(3)];
                case 1 -> random(rng, 1 + rng.nextInt(4));
                default -> slice(rng, files);
            };
            int maxHits = rng.nextInt(10) == 0 ? rng.nextInt(3) : 1 + rng.nextInt(300);
            line(query("R " + n, image, index, files, selection, pattern, maxHits));
        }
        line("RANDOM operations=" + count);
    }

    private void drift() throws IOException {
        Path packed = image("C", new Random(SEED + 3), 20);
        Object image = call(open, null, packed, fileBackend, limits);
        try (FileChannel channel = FileChannel.open(packed.resolve("source.bin"), StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            ByteBuffer one = ByteBuffer.allocate(1);
            channel.read(one, 7);
            channel.write(ByteBuffer.wrap(new byte[] {(byte) (one.get(0) ^ 0x33)}), 7);
            channel.force(true);
        }
        line("DRIFT build -> " + describeIndex(call(build, null, image, 64L * MIB, never())));
        invoke(close, image);
    }

    private static void deleteTree(Path tree) throws IOException {
        try (Stream<Path> walk = Files.walk(tree)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
}
