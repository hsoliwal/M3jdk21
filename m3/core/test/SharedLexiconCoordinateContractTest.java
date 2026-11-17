/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Contract of the lexicon coordinate codecs of the package-private
 * {@code jdk.m3.text.SharedLexiconExport} (reached with {@code --add-opens}): canonical unsigned
 * LEB128, the wire coordinate, the 32-bit image lane with its refusals, the registry generation,
 * and the shape of {@code m3/lexicon/m3-lexicon-registry.tsv} (run from {@code m3/}).
 */
public final class SharedLexiconCoordinateContractTest {
    private static final int[] RANKS = {0, 127, 128, 16_383, 16_384, (1 << 21) - 1, (1 << 24) - 2};
    private static final int[] LEXICONS = {1, 127, 128, 255};
    private static final String REGISTRY = "lexicon/m3-lexicon-registry.tsv";
    private static final String HEADER = "lexicon_num\tlexicon_key\tsource_id\tkind\tlanguage_tag"
            + "\tversion\trank_rule\texport_sha256\tlicence\tstatus";
    private static final Pattern SOURCE_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._+-]*");
    private static final Pattern SHA256 = Pattern.compile("-|[0-9a-f]{64}");
    private static MethodHandle varintLength, writeVarint, readVarint, registryGeneration;
    private static MethodHandle coordinate, lexicon, rank, wireLength, writeWire, readWire;
    private static MethodHandle lane, ofLane;
    private static int checks;

    private SharedLexiconCoordinateContractTest() { }

    private interface Action { void run() throws Throwable; }

    public static void main(String[] args) throws Throwable {
        bind();
        varints();
        wires();
        lanes();
        naivePackDiscriminator();
        registry();
        System.out.println("M3_LEXICON_COORDINATE_CONTRACT_PASS checks=" + checks);
    }

    private static void bind() throws Throwable {
        Class<?> export = Class.forName("jdk.m3.text.SharedLexiconExport");
        Class<?> type = Class.forName("jdk.m3.text.SharedLexiconExport$LexiconCoordinate");
        MethodHandles.Lookup at = MethodHandles.privateLookupIn(export, MethodHandles.lookup());
        MethodHandles.Lookup in = MethodHandles.privateLookupIn(type, MethodHandles.lookup());
        varintLength = at.findStatic(export, "varintLength", MethodType.methodType(int.class,
                int.class));
        writeVarint = at.findStatic(export, "writeVarint", MethodType.methodType(int.class,
                byte[].class, int.class, int.class));
        readVarint = at.findStatic(export, "readVarint", MethodType.methodType(long.class,
                byte[].class, int.class));
        registryGeneration = at.findStatic(export, "registryGeneration",
                MethodType.methodType(byte[].class, byte[].class));
        coordinate = in.findConstructor(type, MethodType.methodType(void.class, int.class,
                int.class));
        lexicon = in.findVirtual(type, "lexicon", MethodType.methodType(int.class));
        rank = in.findVirtual(type, "rank", MethodType.methodType(int.class));
        wireLength = in.findVirtual(type, "wireLength", MethodType.methodType(int.class));
        writeWire = in.findVirtual(type, "writeWire", MethodType.methodType(int.class,
                byte[].class, int.class));
        readWire = in.findStatic(type, "readWire", MethodType.methodType(type, byte[].class,
                int.class));
        lane = in.findVirtual(type, "lane", MethodType.methodType(int.class));
        ofLane = in.findStatic(type, "ofLane", MethodType.methodType(type, int.class));
        check((int) at.findStaticGetter(export, "MAX_LANE_RANK", int.class).invoke()
                == (1 << 24) - 2, "lane rank limit is 2^24 - 2");
        check((int) at.findStaticGetter(export, "MAX_LANE_LEXICON", int.class).invoke() == 255,
                "lane lexicon limit is 255");
    }

    /** uLEB128 lengths, exact bytes, round trips at a nonzero offset, and refusals. */
    private static void varints() throws Throwable {
        int[] values = {0, 1, 127, 128, 16_383, 16_384, (1 << 21) - 1, 1 << 21, (1 << 28) - 1,
            1 << 28, Integer.MAX_VALUE};
        int[] lengths = {1, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5};
        for (int at = 0; at < values.length; at++) {
            int value = values[at];
            check((int) varintLength.invoke(value) == lengths[at], "varint length of " + value);
            byte[] buffer = new byte[9];
            int end = (int) writeVarint.invoke(buffer, 3, value);
            check(end == 3 + lengths[at], "varint end of " + value);
            long read = (long) readVarint.invoke(buffer, 3);
            check((int) read == value && (int) (read >>> 32) == end, "varint round trip " + value);
        }
        bytes(0, 0x00);
        bytes(127, 0x7f);
        bytes(128, 0x80, 0x01);
        bytes(16_383, 0xff, 0x7f);
        bytes(16_384, 0x80, 0x80, 0x01);
        bytes(Integer.MAX_VALUE, 0xff, 0xff, 0xff, 0xff, 0x07);
        refuses("negative varint length", () -> varintLength.invoke(-1));
        refuses("negative varint", () -> writeVarint.invoke(new byte[5], 0, -1));
        refusesRead("empty", new byte[0]);
        refusesRead("truncated", unsigned(0x80));
        refusesRead("overlong zero", unsigned(0x80, 0x00));
        refusesRead("overlong 127", unsigned(0xff, 0x00));
        refusesRead("above int", unsigned(0x80, 0x80, 0x80, 0x80, 0x08));
        refusesRead("continued fifth byte", unsigned(0xff, 0xff, 0xff, 0xff, 0x87, 0x00));
    }

    /** The wire form uLEB128(lexicon) uLEB128(rank) at every boundary, and its refusals. */
    private static void wires() throws Throwable {
        for (int lexiconNumber : LEXICONS) {
            for (int rankValue : RANKS) {
                Object value = coordinate.invoke(lexiconNumber, rankValue);
                int length = (int) wireLength.invoke(value);
                check(length == (int) varintLength.invoke(lexiconNumber)
                        + (int) varintLength.invoke(rankValue), "wire length");
                byte[] buffer = new byte[length + 2];
                check((int) writeWire.invoke(value, buffer, 1) == 1 + length, "wire end");
                Object back = readWire.invoke(buffer, 1);
                check(back.equals(value) && (int) lexicon.invoke(back) == lexiconNumber
                        && (int) rank.invoke(back) == rankValue,
                        "wire round trip " + lexiconNumber + ":" + rankValue);
            }
        }
        int[] expected = {2, 2, 3, 3, 4, 4, 5};
        for (int at = 0; at < RANKS.length; at++)
            check((int) wireLength.invoke(coordinate.invoke(1, RANKS[at])) == expected[at],
                    "wire bytes with a one-byte lexicon at rank " + RANKS[at]);
        wire(1, 0, 0x01, 0x00);
        wire(127, 127, 0x7f, 0x7f);
        wire(128, 128, 0x80, 0x01, 0x80, 0x01);
        refuses("lexicon 0", () -> coordinate.invoke(0, 0));
        refuses("negative lexicon", () -> coordinate.invoke(-1, 0));
        refuses("negative rank", () -> coordinate.invoke(1, -1));
        refuses("wire lexicon 0", () -> readWire.invoke(unsigned(0x00, 0x05), 0));
        refuses("wire without rank", () -> readWire.invoke(unsigned(0x01), 0));
        refuses("overlong wire rank", () -> readWire.invoke(unsigned(0x01, 0x80, 0x00), 0));
    }

    /** The image lane lexicon(8) | rank+1(24) at every boundary, and its refusals. */
    private static void lanes() throws Throwable {
        for (int lexiconNumber : LEXICONS) {
            for (int rankValue : RANKS) {
                Object value = coordinate.invoke(lexiconNumber, rankValue);
                int packed = (int) lane.invoke(value);
                check(packed >>> 24 == lexiconNumber && (packed & 0xff_ffff) == rankValue + 1,
                        "lane bits " + lexiconNumber + ":" + rankValue);
                check(ofLane.invoke(packed).equals(value),
                        "lane round trip " + lexiconNumber + ":" + rankValue);
            }
        }
        check((int) lane.invoke(coordinate.invoke(1, 0)) == 0x0100_0001, "lane of 1:0");
        check((int) lane.invoke(coordinate.invoke(255, (1 << 24) - 2)) == 0xffff_ffff,
                "lane of 255:2^24-2");
        check((int) lane.invoke(coordinate.invoke(128, 16_383)) == 0x8000_4000,
                "lane of 128:16383");
        refuses("lane rank 2^24 - 1", () -> lane.invoke(coordinate.invoke(1, (1 << 24) - 1)));
        refuses("lane rank 2^24 - 1 at lexicon 255",
                () -> lane.invoke(coordinate.invoke(255, (1 << 24) - 1)));
        refuses("lane lexicon 256", () -> lane.invoke(coordinate.invoke(256, 0)));
        refuses("lane 0", () -> ofLane.invoke(0));
        refuses("lane with entry 0", () -> ofLane.invoke(0x0100_0000));
        refuses("lane with lexicon 0", () -> ofLane.invoke(0x0000_0001));
    }

    /** A pack without the refusals silently decodes the two overflow inputs to another word. */
    private static void naivePackDiscriminator() throws Throwable {
        int[][] overflows = {{1, (1 << 24) - 1}, {256, 0}, {255, (1 << 24) - 1}};
        for (int[] input : overflows) {
            int naive = input[0] << 24 | (input[1] + 1);
            boolean same = naive >>> 24 == input[0] && (naive & 0xff_ffff) == input[1] + 1;
            check(!same, "naive pack corrupts " + input[0] + ":" + input[1]);
            refuses("lane refuses " + input[0] + ":" + input[1],
                    () -> lane.invoke(coordinate.invoke(input[0], input[1])));
        }
    }

    /** Registry shape and the generation pin: deterministic, CR LF neutral, change sensitive. */
    private static void registry() throws Throwable {
        byte[] bytes = Files.readAllBytes(Path.of(REGISTRY));
        String text = new String(bytes, StandardCharsets.UTF_8);
        check(text.endsWith("\n") && text.indexOf('\r') < 0, "registry is LF text");
        List<String> lines = List.of(text.substring(0, text.length() - 1).split("\n", -1));
        check(lines.get(0).equals(HEADER), "registry header");
        Set<String> keys = new HashSet<>();
        for (int number = 1; number < lines.size(); number++) {
            String[] cells = lines.get(number).split("\t", -1);
            check(cells.length == 10, "registry row " + number + " has 10 cells");
            check(cells[0].equals(Integer.toString(number)), "lexicon numbers are 1..N in order");
            check(cells[1].equals(cells[2] + "@" + cells[5]), "lexicon_key = source_id@version");
            check(keys.add(cells[1]), "lexicon_key is unique: " + cells[1]);
            check(SOURCE_ID.matcher(cells[2]).matches(), "catalog source id " + cells[2]);
            check(SOURCE_ID.matcher(cells[4]).matches(), "language tag " + cells[4]);
            for (int cell : new int[] {3, 5, 6, 8})
                check(!cells[cell].isEmpty(), "registry cell " + cell + " is filled");
            check(SHA256.matcher(cells[7]).matches(), "export_sha256 is - or SHA-256");
            check(cells[9].equals("ACTIVE") || cells[9].equals("RETIRED"), "registry status");
        }
        byte[] generation = (byte[]) registryGeneration.invoke(bytes);
        check(Arrays.equals(generation, (byte[]) registryGeneration.invoke(bytes.clone())),
                "registry generation is deterministic");
        check(Arrays.equals(generation, MessageDigest.getInstance("SHA-256").digest(bytes)),
                "registry generation of LF text is its SHA-256");
        byte[] crlf = text.replace("\n", "\r\n").getBytes(StandardCharsets.UTF_8);
        check(Arrays.equals(generation, (byte[]) registryGeneration.invoke(crlf)),
                "a CR LF checkout pins the same generation");
        byte[] changed = bytes.clone();
        changed[changed.length - 2] ^= 1;
        check(!Arrays.equals(generation, (byte[]) registryGeneration.invoke(changed)),
                "one changed byte changes the generation");
        byte[] loneCr = Arrays.copyOf(bytes, bytes.length + 1);
        loneCr[bytes.length] = '\r';
        refuses("lone CR at the end", () -> registryGeneration.invoke(loneCr));
        refuses("lone CR inside", () -> registryGeneration.invoke(
                "a\rb\n".getBytes(StandardCharsets.US_ASCII)));
    }

    private static void bytes(int value, int... expected) throws Throwable {
        byte[] buffer = new byte[expected.length];
        writeVarint.invoke(buffer, 0, value);
        check(Arrays.equals(buffer, unsigned(expected)), "varint bytes of " + value);
    }

    private static void wire(int lexiconNumber, int rankValue, int... expected) throws Throwable {
        Object value = coordinate.invoke(lexiconNumber, rankValue);
        byte[] buffer = new byte[(int) wireLength.invoke(value)];
        writeWire.invoke(value, buffer, 0);
        check(Arrays.equals(buffer, unsigned(expected)),
                "wire bytes of " + lexiconNumber + ":" + rankValue);
    }

    private static void refusesRead(String what, byte[] input) {
        refuses("varint " + what, () -> readVarint.invoke(input, 0));
    }

    private static byte[] unsigned(int... values) {
        byte[] result = new byte[values.length];
        for (int at = 0; at < values.length; at++) result[at] = (byte) values[at];
        return result;
    }

    private static void refuses(String what, Action action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            check(true, what);
            return;
        } catch (Throwable other) {
            throw new AssertionError(what + ": unexpected " + other, other);
        }
        throw new AssertionError(what + ": not refused");
    }

    private static void check(boolean condition, String what) {
        if (!condition) throw new AssertionError(what);
        checks++;
    }
}
