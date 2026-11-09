// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.indexdb;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Differential dump driver of the hash-family leaf (not a test). differential.sh compiles this source twice: as is
 * against the relocated classes, and with the TYPE rows of RELOCATION_MAP.tsv reversed (and the donor package)
 * against the donor crate; both dumps must be byte-identical. Seeded inputs (named seeds below):
 * <ul>
 * <li>L lane: {@value #LHASH_INPUTS} byte inputs (all 256 single bytes, the empty input, 0..63-token boundary
 *     streams, code soup, Unicode text incl. supplementary Nd digits, adversarial repeats, random malformed UTF-8,
 *     window-boundary token counts, JSX/generics, string/comment torture incl. lone CR, CRLF code); per input the
 *     token count, H1..H4, H5 count and fold, and the L-Hash similarity to the previous input (IEEE bits); F lines:
 *     the lane FNV over the decoded UTF-16 text.</li>
 * <li>M lane: permutation families, edge sets, k/bands/rows variants (refusal included), {@value #MINHASH_PAIRS}
 *     random set pairs (signature, band keys, exact and estimated Jaccard bits), planted-group candidate pairs.</li>
 * <li>C lane: {@value #CDC_FILES} files of 0..4 MiB (boundary sizes first; random, zero, periodic, text-like and
 *     zero-run content) chunked with the default and the small parameters: chunk lanes, digest lanes, and one
 *     random single-byte insertion per file with the stability counts.</li>
 * </ul>
 * {@code --scale=xint} keeps the first 2,000 / 200 / 30 (at most 256 KiB) items for the -Xint run.
 * {@code --inputs=FILE} also writes the L lane inputs ({@code index TAB base64}) for the Python oracle;
 * {@code --fixtures=DIR} writes the fixture subset of the L lane as files plus {@code listing.txt}.
 */
//FLLDCIM: DIFFERENTIAL - recipe m3/indexdb/recipe/hash-family-20261106 differential.sh lanes L, M, C
final class M3HashFamilyDump {
    static final long LHASH_SEED = 0x4d334c4841534831L;
    static final long MINHASH_SEED = 0x4d334d494e4c5348L;
    static final long CDC_SEED = 0x4d33434443464c45L;
    static final int LHASH_INPUTS = 100_000;
    static final int MINHASH_PAIRS = 10_000;
    static final int CDC_FILES = 1_000;
    static final int CDC_MAX_BYTES = 4 << 20;
    static final int XINT_LHASH = 2_000;
    static final int XINT_PAIRS = 200;
    static final int XINT_FILES = 30;
    static final int XINT_MAX_BYTES = 256 << 10;
    static final int BOUNDARY_TOKENS = 64;
    static final int FIXTURES = 400;
    static final int FIXTURE_MAX_BYTES = 768;
    static final double ERROR_BOUND = 0.05;
    static final M3ChunkCdc.Parameters SMALL = new M3ChunkCdc.Parameters(256, 1024, 4096);
    static final String[] VOCABULARY = {"if", "elif", "else", "for", "foreach", "while", "do", "switch", "match", "case",
        "when", "try", "catch", "except", "rescue", "finally", "ensure", "return", "break", "continue", "next", "throw",
        "raise", "def", "function", "func", "fn", "fun", "class", "struct", "interface", "trait", "enum", "new", "true",
        "false", "True", "False", "null", "None", "nil", "undefined", "NULL", "a", "b", "foo", "_x", "$y", "Item",
        "List", "String", "x1", "Map", "0", "42", "3.14", "1e10", "2.5E-3", "7.", "0x1F", "0X", "0xg", "\u0663",
        "\uff11\uff12", "\ud835\udfce", "\"s\"", "'c'", "`t`", "\"e\\\"q\"", "\"open", "'\\", "// c\n", "# c\n",
        "/* b */", "/* open", "===", "!==", "==", "!=", "<=", ">=", "&&", "||", "=>", "->", "+=", "-=", "*=", "/=",
        "%=", "++", "--", "<", ">", "!", "+", "-", "*", "/", "%", "=", ".", "?", "{", "}", "(", ")", "[", "]", ";",
        ",", ":", "@", "^", "&", "|", "~", "\\", "<>", "\u03bb"};
    static final String[] SEPARATORS = {" ", "\n", "\t", "\r\n", "", " "};
    static final String[] REPEATS = {"(", "if (", "a(", "+=", "/*", "\"", "'", "`", "{", "[", "1", "0x", "//", "#",
        "x.y", "=>", "\u03bb", "\\", "else {", ")"};
    static final String[] TEMPLATES = {"class %s { List<%s> x; }", "const %s = () => <div><%s/></div>;",
        "Map<%s, List<%s>> m = new HashMap<>();", "def %s(%s): return %s + 1", "if (%s) { %s(); } else { return; }",
        "for (int %s = 0; %s < n; %s++) { s += %s; }", "fn %s(%s: i32) -> i32 { %s * 2 }"};
    static final String[] NAMES = {"a", "b", "alpha", "beta", "Item", "T", "Integer", "String", "x", "y_1", "$z"};
    static final String[] TORTURE = {"\"a\\\"b\"", "'\\''", "`x\\`y`", "\"no end", "/* a /* b */ c */", "//x\ry\n",
        "# h\r", "\r", "\\\n", "\"\\\r\n\"", "/*\r*/", "x\ry", "'\n'", "\"\\u0041\""};
    static final int[][] UNICODE = {{0x20, 0x7e}, {0xa0, 0xff}, {0x391, 0x3c9}, {0x410, 0x44f}, {0x4e00, 0x4e80},
        {0x966, 0x96f}, {0xff10, 0xff19}, {0x660, 0x669}, {0x1d7ce, 0x1d7ff}, {0x1f600, 0x1f64f}, {0x2000, 0x200b}};
    static final int[] CDC_BOUNDARIES = {0, 1, 2, 255, 256, 257, 1023, 1024, 1025, 2047, 2048, 2049, 4095, 4096, 4097,
        8191, 8192, 8193, 65535, 65536, 65537, 131071, 131072, 131073, 4 << 20};

    private final BufferedWriter out;
    private final boolean xint;

    private M3HashFamilyDump(BufferedWriter out, boolean xint) {
        this.out = out;
        this.xint = xint;
    }

    public static void main(String[] args) throws IOException {
        Path dump = Path.of(M3HashFamilyContractSupport.option(args, "--out=").orElseThrow());
        boolean xint = M3HashFamilyContractSupport.option(args, "--scale=").orElse("full").equals("xint");
        List<byte[]> inputs = lhashInputs(xint ? XINT_LHASH : LHASH_INPUTS);
        M3HashFamilyContractSupport.option(args, "--inputs=").ifPresent(path -> writeInputs(Path.of(path), inputs));
        M3HashFamilyContractSupport.option(args, "--fixtures=").ifPresent(path -> writeFixtures(Path.of(path), inputs));
        try (BufferedWriter writer = Files.newBufferedWriter(dump, StandardCharsets.UTF_8)) {
            M3HashFamilyDump driver = new M3HashFamilyDump(writer, xint);
            driver.lhash(inputs);
            driver.minhash();
            driver.cdc();
        }
    }

    // ---- L lane -------------------------------------------------------------------------------------------------

    static List<byte[]> lhashInputs(int count) {
        SplittableRandom random = new SplittableRandom(LHASH_SEED);
        List<byte[]> inputs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) inputs.add(lhashInput(i, random));
        return inputs;
    }

    static byte[] lhashInput(int i, SplittableRandom random) {
        if (i < 256) return new byte[] {(byte) i};
        if (i == 256) return new byte[0];
        if (i < 257 + BOUNDARY_TOKENS) return utf8(soup(random, i - 257, false));
        return switch (i % 8) {
            case 0 -> utf8(soup(random, random.nextInt(401), false));
            case 1 -> utf8(unicode(random, random.nextInt(301)));
            case 2 -> utf8(repeat(random));
            case 3 -> randomBytes(random, random.nextInt(513));
            case 4 -> utf8(soup(random, new int[] {7, 8, 9, 15, 16, 17}[random.nextInt(6)], false));
            case 5 -> utf8(template(random));
            case 6 -> utf8(torture(random));
            default -> utf8(soup(random, random.nextInt(201), true));
        };
    }

    static String soup(SplittableRandom random, int tokens, boolean crlf) {
        StringBuilder text = new StringBuilder();
        for (int t = 0; t < tokens; t++) {
            if (t > 0) text.append(crlf ? (random.nextBoolean() ? "\r\n" : " ") : SEPARATORS[random.nextInt(SEPARATORS.length)]);
            text.append(VOCABULARY[random.nextInt(VOCABULARY.length)]);
        }
        return text.toString();
    }

    static String unicode(SplittableRandom random, int codePoints) {
        StringBuilder text = new StringBuilder();
        for (int c = 0; c < codePoints; c++) {
            int[] range = UNICODE[random.nextInt(UNICODE.length)];
            text.appendCodePoint(range[0] + random.nextInt(range[1] - range[0] + 1));
        }
        return text.toString();
    }

    static String repeat(SplittableRandom random) {
        String token = REPEATS[random.nextInt(REPEATS.length)];
        String separator = random.nextBoolean() ? "" : " ";
        return String.join(separator, java.util.Collections.nCopies(1 + random.nextInt(3000), token));
    }

    static String template(SplittableRandom random) {
        String template = TEMPLATES[random.nextInt(TEMPLATES.length)];
        Object[] names = new Object[4];
        for (int n = 0; n < names.length; n++) names[n] = NAMES[random.nextInt(NAMES.length)];
        return String.format(Locale.ROOT, template, names);
    }

    static String torture(SplittableRandom random) {
        StringBuilder text = new StringBuilder();
        for (int p = 1 + random.nextInt(24); p > 0; p--) text.append(TORTURE[random.nextInt(TORTURE.length)]);
        return text.toString();
    }

    static byte[] randomBytes(SplittableRandom random, int length) {
        byte[] bytes = new byte[length];
        for (int b = 0; b < length; b++) bytes[b] = (byte) random.nextInt(256);
        return bytes;
    }

    static byte[] utf8(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private void lhash(List<byte[]> inputs) throws IOException {
        long[] previous = M3LogicHash.of("");
        long[] previousWindows = new long[0];
        for (int i = 0; i < inputs.size(); i++) {
            String text = M3LHashTokens.decode(inputs.get(i));
            M3LHashTokens.Tokens tokens = M3LHashTokens.tokenize(text);
            long[] hash = M3LogicHash.of(tokens);
            long[] windows = M3LogicHash.windows(tokens);
            double similarity = M3HashDistances.lhashSimilarity(hash, windows, previous, previousWindows);
            line("L%d\t%d\t%d\t%016x\t%016x\t%016x\t%016x\t%d\t%016x\t%016x", i, inputs.get(i).length, tokens.size(),
                    hash[0], hash[1], hash[2], hash[3], windows.length, hash[4], Double.doubleToLongBits(similarity));
            line("F%d\t%016x\t%016x", i, M3LaneFnv1a.ofChars(text), M3LaneFnv1a.step(M3LaneFnv1a.OFFSET, text.length()));
            previous = hash;
            previousWindows = windows;
        }
    }

    static void writeInputs(Path path, List<byte[]> inputs) {
        Base64.Encoder encoder = Base64.getEncoder();
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.US_ASCII)) {
            for (int i = 0; i < inputs.size(); i++) writer.write(i + "\t" + encoder.encodeToString(inputs.get(i)) + "\n");
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    /** Fixture subset: no lone CR (the oracle's text-mode read turns it into LF), at most 768 bytes, 400 inputs. */
    static void writeFixtures(Path dir, List<byte[]> inputs) {
        List<String> listing = new ArrayList<>();
        try {
            Files.createDirectories(dir);
            for (int i = 0; i < inputs.size() && listing.size() < FIXTURES; i++) {
                byte[] bytes = inputs.get(i);
                if (bytes.length > FIXTURE_MAX_BYTES || hasLoneCr(bytes) || (i >= 257 + BOUNDARY_TOKENS && i % 8 == 3)) continue;
                String name = String.format(Locale.ROOT, "case-%05d.txt", i);
                Files.write(dir.resolve(name), bytes);
                listing.add(name);
            }
            Files.write(dir.resolve("listing.txt"), listing, StandardCharsets.US_ASCII);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    static boolean hasLoneCr(byte[] bytes) {
        for (int b = 0; b < bytes.length; b++) {
            if (bytes[b] == '\r' && (b + 1 == bytes.length || bytes[b + 1] != '\n')) return true;
        }
        return false;
    }

    // ---- M lane -------------------------------------------------------------------------------------------------

    private void minhash() throws IOException {
        M3MinHash.Permutations permutations = M3MinHash.Permutations.of(M3MinHash.DEFAULT_K, M3MinHash.DEFAULT_SEED);
        line("Mperm\tk=%d\ta=%s\tb=%s", permutations.k(), digest(permutations.a()), digest(permutations.b()));
        long[][] edges = {{}, {0L}, {M3MinHash.MERSENNE_61}, {Long.MIN_VALUE}, {-1L}, {1L, 1L, 1L}, {Long.MAX_VALUE, 0L, -1L},
            {M3MinHash.MERSENNE_61 + 1, 1L}, {5L, 3L, 5L, 9L, 3L}};
        for (int e = 0; e < edges.length; e++) {
            long[] signature = M3MinHash.signature(edges[e], edges[e].length, permutations);
            line("Medge%d\t%s\t%s", e, digest(signature), digest(M3MinHashLsh.keys(signature, 32, 4)));
        }
        int[][] shapes = {{1, 1, 1}, {4, 2, 2}, {128, 32, 4}, {256, 64, 4}, {128, 16, 8}, {128, 33, 4}, {0, 0, 1}};
        for (int[] shape : shapes) variant(shape, edges[6]);
        pairs();
        groups();
    }

    private void variant(int[] shape, long[] set) throws IOException {
        M3MinHash.Permutations family = M3MinHash.Permutations.of(shape[0], M3MinHash.DEFAULT_SEED);
        long[] signature = M3MinHash.signature(set, set.length, family);
        String keys;
        try {
            keys = digest(M3MinHashLsh.keys(signature, shape[1], shape[2]));
        } catch (IllegalArgumentException refused) {
            keys = "REFUSED:" + refused.getMessage();
        }
        line("Mshape\tk=%d\tbands=%d\trows=%d\t%s\t%s", shape[0], shape[1], shape[2], digest(signature), keys);
    }

    private void pairs() throws IOException {
        M3MinHash.Permutations permutations = M3MinHash.Permutations.of(M3MinHash.DEFAULT_K, M3MinHash.DEFAULT_SEED);
        SplittableRandom random = new SplittableRandom(MINHASH_SEED);
        int count = xint ? XINT_PAIRS : MINHASH_PAIRS;
        double sum = 0;
        double max = 0;
        int within = 0;
        long[][] signatures = new long[2 * count][];
        for (int p = 0; p < count; p++) {
            long[] left = set(random, 100 + random.nextInt(900));
            long[] right = overlap(random, left, random.nextDouble());
            signatures[2 * p] = M3MinHash.signature(left, left.length, permutations);
            signatures[2 * p + 1] = M3MinHash.signature(right, right.length, permutations);
            double exact = M3HashDistances.jaccard(left, left.length, right, right.length);
            double estimate = M3MinHash.jaccardEstimate(signatures[2 * p], signatures[2 * p + 1]);
            double error = Math.abs(exact - estimate);
            sum += error;
            max = Math.max(max, error);
            within += error <= ERROR_BOUND ? 1 : 0;
            line("P%d\t%d\t%d\t%s\t%s\t%s\t%s\t%016x\t%016x", p, left.length, right.length, digest(signatures[2 * p]),
                    digest(signatures[2 * p + 1]), digest(M3MinHashLsh.keys(signatures[2 * p], 32, 4)),
                    digest(M3MinHashLsh.keys(signatures[2 * p + 1], 32, 4)), Double.doubleToLongBits(exact),
                    Double.doubleToLongBits(estimate));
        }
        long[] candidates = M3MinHashLsh.candidatePairs(signatures, 32, 4);
        line("Mall\tsignatures=%d\tcandidatePairs=%d\t%s", signatures.length, candidates.length, digest(candidates));
        line("MSUM\tpairs=%d\tmeanAbsError=%.6f\tmaxAbsError=%.6f\twithin0.05=%d", count, sum / count, max, within);
    }

    private void groups() throws IOException {
        M3MinHash.Permutations permutations = M3MinHash.Permutations.of(M3MinHash.DEFAULT_K, M3MinHash.DEFAULT_SEED);
        SplittableRandom random = new SplittableRandom(MINHASH_SEED ^ 1L);
        int groups = xint ? 2 : 50;
        int recalled = 0;
        for (int g = 0; g < groups; g++) {
            long[][] signatures = new long[60][];
            for (int i = 0; i < 20; i++) {
                long[] base = set(random, 200 + random.nextInt(800));
                long[] near = overlap(random, base, 0.75 + 0.2 * random.nextDouble());
                long[] other = set(random, 200 + random.nextInt(800));
                signatures[i] = M3MinHash.signature(base, base.length, permutations);
                signatures[20 + i] = M3MinHash.signature(near, near.length, permutations);
                signatures[40 + i] = M3MinHash.signature(other, other.length, permutations);
            }
            long[] pairs = M3MinHashLsh.candidatePairs(signatures, 32, 4);
            int found = 0;
            for (int i = 0; i < 20; i++) found += java.util.Arrays.binarySearch(pairs, ((long) i << 32) | (20 + i)) >= 0 ? 1 : 0;
            recalled += found;
            line("G%d\tpairs=%d\trecalled=%d/20\t%s", g, pairs.length, found, digest(pairs));
        }
        line("GSUM\tgroups=%d\tplanted=%d\trecalled=%d", groups, groups * 20, recalled);
    }

    static long[] set(SplittableRandom random, int size) {
        long[] lane = new long[size];
        for (int i = 0; i < size; i++) lane[i] = random.nextLong();
        return M3HashLanes.sortedDistinct(lane, size);
    }

    static long[] overlap(SplittableRandom random, long[] base, double keep) {
        long[] lane = new long[base.length];
        int count = 0;
        for (long value : base) {
            if (random.nextDouble() < keep) lane[count++] = value;
        }
        while (count < base.length) lane[count++] = random.nextLong();
        return M3HashLanes.sortedDistinct(lane, count);
    }

    // ---- C lane -------------------------------------------------------------------------------------------------

    private void cdc() throws IOException {
        SplittableRandom random = new SplittableRandom(CDC_SEED);
        int files = xint ? XINT_FILES : CDC_FILES;
        int limit = xint ? XINT_MAX_BYTES : CDC_MAX_BYTES;
        long[] totals = new long[7];
        for (int i = 0; i < files; i++) {
            int size = Math.min(limit, i < CDC_BOUNDARIES.length ? CDC_BOUNDARIES[i] : randomSize(random, limit));
            byte[] bytes = content(random, i % 5, size);
            chunked(i, bytes, M3ChunkCdc.DEFAULT, random, totals);
            chunked(i, bytes, SMALL, random, totals);
        }
        line("CSUM\tfiles=%d\tbytesChunked=%d\tchunks=%d\tfar=%d\tfarSurvived=%d\tmaxChanged=%d\tbeyond2max=%d\tmaxLostReach=%d",
                files, totals[0], totals[1], totals[2], totals[3], totals[4], totals[5], totals[6]);
    }

    static int randomSize(SplittableRandom random, int limit) {
        if (random.nextInt(10) < 3) return random.nextInt(limit + 1);
        return (int) Math.min(limit, Math.floor(Math.pow(2.0, random.nextDouble() * 22.0)));
    }

    static byte[] content(SplittableRandom random, int kind, int size) {
        byte[] bytes = new byte[size];
        switch (kind) {
            case 0 -> random.nextBytes(bytes);
            case 1 -> { }
            case 2 -> {
                byte[] period = randomBytes(random, 1 + random.nextInt(4096));
                for (int b = 0; b < size; b++) bytes[b] = period[b % period.length];
            }
            case 3 -> {
                byte[] text = utf8(soup(random, 1 + size / 3, false));
                System.arraycopy(text, 0, bytes, 0, Math.min(size, text.length));
            }
            default -> {
                random.nextBytes(bytes);
                for (int b = 0; b < size; b += 1 + random.nextInt(8192)) java.util.Arrays.fill(bytes, b, Math.min(size, b + random.nextInt(4096)), (byte) 0);
            }
        }
        return bytes;
    }

    private void chunked(int i, byte[] bytes, M3ChunkCdc.Parameters parameters, SplittableRandom random, long[] totals)
            throws IOException {
        M3ChunkCdc.Chunks chunks = M3ChunkCdc.chunk(bytes, parameters);
        M3ChunkSha.Digests digests = M3ChunkSha.of(bytes, chunks);
        line("C%d\t%d\t%s\t%d\t%s\t%s", i, bytes.length, parameters.text(), chunks.count(), digest(lanes(chunks)),
                digest(digests.lane()));
        int position = random.nextInt(bytes.length + 1);
        byte[] edited = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, edited, 0, position);
        edited[position] = (byte) random.nextInt(256);
        System.arraycopy(bytes, position, edited, position + 1, bytes.length - position);
        M3ChunkCdc.Chunks after = M3ChunkCdc.chunk(edited, parameters);
        M3ChunkSha.Digests afterDigests = M3ChunkSha.of(edited, after);
        Set<String> present = new HashSet<>();
        for (int c = 0; c < after.count(); c++) present.add(afterDigests.hex(c));
        int changed = 0;
        int far = 0;
        int farSurvived = 0;
        int window = 0;
        long lostReach = -1;
        for (int c = 0; c < chunks.count(); c++) {
            int start = chunks.offsets()[c];
            int end = start + chunks.lengths()[c];
            boolean survived = present.contains(digests.hex(c));
            changed += survived ? 0 : 1;
            if (!survived) lostReach = Math.max(lostReach, (long) start - position);
            if ((end <= position && end < bytes.length) || start >= position + 2 * parameters.max()) {
                far++;
                farSurvived += survived ? 1 : 0;
            } else {
                window++;
            }
        }
        boolean beyond = farSurvived != far;
        line("S%d\t%s\tposition=%d\tchanged=%d\twindow=%d\tfar=%d\tfarSurvived=%d\tlostReach=%d\tafter=%d\t%s", i,
                parameters.text(), position, changed, window, far, farSurvived, lostReach, after.count(),
                beyond ? "BEYOND_2MAX" : "ok");
        totals[0] += bytes.length;
        totals[1] += chunks.count();
        totals[2] += far;
        totals[3] += farSurvived;
        totals[4] = Math.max(totals[4], changed);
        totals[5] += beyond ? 1 : 0;
        totals[6] = Math.max(totals[6], lostReach);
    }

    static byte[] lanes(M3ChunkCdc.Chunks chunks) {
        ByteBuffer buffer = ByteBuffer.allocate(chunks.count() * 2 * Integer.BYTES);
        for (int c = 0; c < chunks.count(); c++) buffer.putInt(chunks.offsets()[c]).putInt(chunks.lengths()[c]);
        return buffer.array();
    }

    // ---- output -------------------------------------------------------------------------------------------------

    private void line(String format, Object... values) throws IOException {
        out.write(String.format(Locale.ROOT, format, values));
        out.write('\n');
    }

    static String digest(long[] lane) {
        return digest(M3HashLanes.bytes(lane, lane.length));
    }

    static String digest(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes), 0, 8);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
