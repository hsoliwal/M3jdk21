// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
// Gate tool (not shipped): Jackson 2.19.2 with StreamReadFeature.USE_FAST_DOUBLE_PARSER on vs off over one generated
// JSON corpus. Parser kinds: char (ReaderBasedJsonParser from a String), byte (UTF8StreamJsonParser from byte[]) and
// async (non-blocking byte[] parser fed in small chunks, so number tokens span feeds). getDoubleValue and
// getFloatValue run in separate passes (a float read after a double read converts the cached double). Every token's
// raw bits must match with the feature on and off and across parser kinds. The value digests are printed so builds
// (upstream FDP vs overlay FDP, stock vs M3 java.base) compare by hash.
// Usage: java -cp <harness>;<jackson-core classes>;<fdp.jar> M3JacksonFastDoubleDifferential <count> <report.tsv>
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.SplittableRandom;

public final class M3JacksonFastDoubleDifferential {
    private static final String FDP_FACADE = "ch.randelshofer.fastdoubleparser.JavaDoubleParser";
    private static final String[] KINDS = {"char", "byte", "async"};
    private static final String[] PASSES = {"double", "float"};
    private static final String[] SEPARATORS = {",", ", ", ",\n", " ,\t", ",\r\n  "};
    private static final String TAB = "\t";
    private static final String NEWLINE = "\n";
    private static final String SHA_256 = "SHA-256";
    private static final int NUMBERS_PER_DOCUMENT = 512;
    private static final int MAX_FEED_CHUNK = 9;
    private static final int EXAMPLES = 12;
    private static final int ARGUMENTS = 2;
    private static final int EXIT_USAGE = 2;
    private static final int EXIT_MISMATCH = 1;
    private static final int CHAR = 0;
    private static final int BYTE = 1;
    private static final int DOUBLE = 0;
    private static final int FAST = 0;
    private static final int PLAIN = 1;
    private static final long FLOAT_BITS = 0xffff_ffffL;
    private static final long SEPARATOR_SEED = M3FdpParseCorpus.SEED + 2;

    private M3JacksonFastDoubleDifferential() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != ARGUMENTS) {
            System.err.println("usage: M3JacksonFastDoubleDifferential <count> <report.tsv>");
            System.exit(EXIT_USAGE);
        }
        List<String> numbers = M3FdpParseCorpus.jsonNumbers(Integer.parseInt(args[0]));
        List<String> documents = documents(numbers);
        JsonFactory[] factories = {
            JsonFactory.builder().enable(StreamReadFeature.USE_FAST_DOUBLE_PARSER).build(),
            JsonFactory.builder().disable(StreamReadFeature.USE_FAST_DOUBLE_PARSER).build(),
        };
        long[][][][] bits = new long[KINDS.length][PASSES.length][factories.length][];
        long errors = 0;
        for (int kind = 0; kind < KINDS.length; kind++) {
            for (int pass = 0; pass < PASSES.length; pass++) {
                for (int mode = 0; mode < factories.length; mode++) {
                    bits[kind][pass][mode] = new long[numbers.size()];
                    errors += values(factories[mode], kind, pass, documents, bits[kind][pass][mode]);
                }
            }
        }
        StringBuilder out = new StringBuilder();
        List<String> examples = new ArrayList<>();
        out.append("#M3JacksonFastDoubleDifferential").append(TAB).append("numbers=").append(numbers.size()).append(TAB)
                .append("documents=").append(documents.size()).append(TAB).append("sha256=")
                .append(M3FdpParseCorpus.sha256(numbers)).append(TAB).append("java=").append(Runtime.version())
                .append(TAB).append("fdp=").append(Class.forName(FDP_FACADE).getProtectionDomain().getCodeSource()
                        .getLocation().getPath().replaceAll(".*/", "")).append(NEWLINE);
        out.append("kind\tpass\ttokens\tfast_vs_plain_differ\tvs_char_differ_fast\tvs_char_differ_plain\n");
        long differ = 0;
        for (int kind = 0; kind < KINDS.length; kind++) {
            for (int pass = 0; pass < PASSES.length; pass++) {
                long modes = count(bits[kind][pass][FAST], bits[kind][pass][PLAIN], numbers, examples,
                        KINDS[kind] + "/" + PASSES[pass] + " fast-vs-plain");
                long fast = count(bits[kind][pass][FAST], bits[CHAR][pass][FAST], numbers, examples,
                        KINDS[kind] + "/" + PASSES[pass] + " vs char (fast)");
                long plain = count(bits[kind][pass][PLAIN], bits[CHAR][pass][PLAIN], numbers, examples,
                        KINDS[kind] + "/" + PASSES[pass] + " vs char (plain)");
                differ += modes + fast + plain;
                out.append(String.join(TAB, KINDS[kind], PASSES[pass], Integer.toString(numbers.size()),
                        Long.toString(modes), Long.toString(fast), Long.toString(plain))).append(NEWLINE);
            }
        }
        out.append("DIGEST").append(TAB).append("double_fast=").append(digest(bits[CHAR][DOUBLE][FAST])).append(TAB)
                .append("float_fast=").append(digest(bits[CHAR][1][FAST])).append(TAB)
                .append("double_plain=").append(digest(bits[CHAR][DOUBLE][PLAIN])).append(TAB)
                .append("float_plain=").append(digest(bits[CHAR][1][PLAIN])).append(NEWLINE);
        boolean pass = differ == 0 && errors == 0;
        out.append("RESULT").append(TAB).append(pass ? "PASS" : "FAIL").append(TAB).append("errors=").append(errors)
                .append(TAB).append("differ=").append(differ).append(NEWLINE);
        examples.forEach(e -> out.append("example").append(TAB).append(e).append(NEWLINE));
        Files.writeString(Path.of(args[1]), out, StandardCharsets.UTF_8);
        System.out.print(out);
        System.exit(pass ? 0 : EXIT_MISMATCH);
    }

    private static List<String> documents(List<String> numbers) {
        List<String> documents = new ArrayList<>();
        SplittableRandom random = new SplittableRandom(SEPARATOR_SEED);
        for (int start = 0; start < numbers.size(); start += NUMBERS_PER_DOCUMENT) {
            StringBuilder json = new StringBuilder("[");
            int end = Math.min(numbers.size(), start + NUMBERS_PER_DOCUMENT);
            for (int i = start; i < end; i++) {
                json.append(i == start ? "" : SEPARATORS[random.nextInt(SEPARATORS.length)]).append(numbers.get(i));
            }
            documents.add(json.append(']').toString());
        }
        return documents;
    }

    /** Parses every document with one parser kind and records each number token's raw bits; returns errors. */
    private static long values(JsonFactory factory, int kind, int pass, List<String> documents, long[] bits) {
        long errors = 0;
        int index = 0;
        SplittableRandom chunks = new SplittableRandom(SEPARATOR_SEED + kind);
        for (String document : documents) {
            try (JsonParser parser = parser(factory, kind, document)) {
                byte[] utf8 = document.getBytes(StandardCharsets.UTF_8);
                int fed = 0;
                for (JsonToken token = parser.nextToken(); token != null; token = parser.nextToken()) {
                    if (token == JsonToken.NOT_AVAILABLE) {
                        fed = feed((ByteArrayFeeder) parser.getNonBlockingInputFeeder(), utf8, fed, chunks);
                    } else if (token.isNumeric()) {
                        bits[index++] = pass == DOUBLE ? Double.doubleToRawLongBits(parser.getDoubleValue())
                                : Float.floatToRawIntBits(parser.getFloatValue()) & FLOAT_BITS;
                    }
                }
            } catch (IOException | RuntimeException failure) {
                errors++;
                System.err.println(KINDS[kind] + "/" + PASSES[pass] + ": " + failure);
            }
        }
        return errors + (index == bits.length ? 0 : 1);
    }

    private static JsonParser parser(JsonFactory factory, int kind, String document) throws IOException {
        return kind == CHAR ? factory.createParser(document)
                : kind == BYTE ? factory.createParser(document.getBytes(StandardCharsets.UTF_8))
                : factory.createNonBlockingByteArrayParser();
    }

    private static int feed(ByteArrayFeeder feeder, byte[] utf8, int fed, SplittableRandom chunks) throws IOException {
        if (fed == utf8.length) {
            feeder.endOfInput();
            return fed;
        }
        int end = Math.min(utf8.length, fed + 1 + chunks.nextInt(MAX_FEED_CHUNK));
        feeder.feedInput(utf8, fed, end);
        return end;
    }

    private static long count(long[] a, long[] b, List<String> numbers, List<String> examples, String label) {
        long differ = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                differ++;
                if (examples.size() < EXAMPLES) {
                    examples.add(label + TAB + numbers.get(i) + TAB + "0x" + Long.toHexString(a[i]) + TAB
                            + "0x" + Long.toHexString(b[i]));
                }
            }
        }
        return differ;
    }

    private static String digest(long[] values) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * Long.BYTES);
        buffer.asLongBuffer().put(values);
        return HexFormat.of().formatHex(MessageDigest.getInstance(SHA_256).digest(buffer.array()));
    }
}
