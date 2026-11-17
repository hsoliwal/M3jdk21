/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
import jdk.m3.text.LexiconTool;
import jdk.m3.text.M3PrecomputePayload;
import jdk.m3.text.SharedLexiconCatalog;
import jdk.m3.text.SharedLexiconImage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Black-box contract of {@code LexiconTool build --frequency}: word id = frequency rank (count
 * descending, ties in UTF-16 order), a v2 export the catalog admits, the export-m3-lexicon.py
 * bytes for the same input (pinned SHA-256 per file), determinism and input refusals.
 */
public final class SharedLexiconExportContractTest {
    private static final String TINY = "the\t9\nof\t9\n\uff5e\t5\n\ud83d\ude00\t5\na\u00a0b\t3\n"
            + "say\"hi\t3\nback\\slash\t2\ngoto\t0\n";
    /**
     * SHA-256 of each file export-m3-lexicon.py writes for TINY as tiny.tsv (source id
     * lexicon.tiny, tag und). Its m3.export.json is text mode: these are the LF bytes.
     */
    private static final Map<String, String> PYTHON = Map.of(
            "m3.m3lex", "e07e7b38feaa306170d51dd7c64fc686d0bd3c5e370194f7a5e60abdbbcacb95",
            "m3.shards.tsv", "7d95a8ad1ecb16bed2ba69e5a221d7276926fe1da355382b6d847bef74f13efd",
            "m3.records.tsv", "94026c35d67826d91e505485f2d2587f638332c590ef1de25d2f4c92a1e05ac8",
            "m3.precompute.tsv", "5e91b6453b1cf52ab980e77bedfe2e07f442d1b214e826cd317f2609740c36cd",
            "m3.precompute-index.tsv",
            "32b1c2ed23592cd9d3aebfa2cc90b1ee7bce136639e0980b6b8fb2a1b85d6cf0",
            "m3.export.json", "6013bb1b2b69bcce8ac8b11e9fea103f82758abca79dab27ebb1efc4e59b45af");
    private static int checks;

    private SharedLexiconExportContractTest() { }

    private interface Action { void run() throws Exception; }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("m3lex-export-contract-");
        try {
            Path tiny = build(root, "tiny.tsv", TINY, "a");
            ranks(tiny);
            pythonBytes(tiny);
            check(files(tiny).equals(files(build(root, "tiny.tsv", TINY, "b"))),
                    "two builds are byte-identical");
            quotedBound(root);
            refusals(root);
        } finally {
            delete(root);
        }
        System.out.println("M3_LEXICON_EXPORT_CONTRACT_PASS checks=" + checks);
    }

    private static Path build(Path root, String name, String text, String run) throws Exception {
        Path input = Files.createDirectories(root.resolve("in-" + run)).resolve(name);
        Files.write(input, text.getBytes(StandardCharsets.UTF_8));
        Path output = root.resolve("out-" + run);
        LexiconTool.main(new String[] {"build", "--frequency", "lexicon.tiny", "und",
                input.toString(), output.toString()});
        return output;
    }

    private static void ranks(Path export) throws Exception {
        SharedLexiconCatalog catalog = SharedLexiconCatalog.open(export);
        List<String> expected = List.of("of", "the", "\ud83d\ude00", "\uff5e", "a\u00a0b",
                "say\"hi", "back\\slash", "goto");
        check(catalog.recordCount() == expected.size(), "one record per word");
        for (int rank = 0; rank < expected.size(); rank++) {
            String word = expected.get(rank);
            SharedLexiconCatalog.Coordinate at = catalog.find(word).orElseThrow();
            check(catalog.mappingsAt(at).get(0).recordId().equals(Integer.toString(rank)),
                    "word id is the frequency rank: " + rank);
            M3PrecomputePayload payload = catalog.findPrecomputePayload("lexicon.tiny",
                    Integer.toString(rank)).orElseThrow();
            check(payload.requireInt("frequency_rank") == rank, "payload frequency_rank");
        }
        check(catalog.findPrecomputePayload("lexicon.tiny", "7").orElseThrow()
                .requireLong("corpus_count") == 0L, "zero count keeps the last rank");
        SharedLexiconCatalog.Coordinate nbsp = catalog.find("a\u00a0b").orElseThrow();
        check(catalog.precomputeAt(nbsp).containsWhitespace(),
                "contains_whitespace follows Python str.isspace (U+00A0)");
        check(catalog.find("absent").isEmpty(), "a non-member has no coordinate");
        catalog.close();
        check(SharedLexiconImage.open(export.resolve("m3.m3lex")).version() == 2, "v2 shard");
    }

    private static void pythonBytes(Path export) throws Exception {
        for (Map.Entry<String, String> file : PYTHON.entrySet()) {
            byte[] bytes = Files.readAllBytes(export.resolve(file.getKey()));
            String actual = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
            check(actual.equals(file.getValue()), "export-m3-lexicon.py bytes: " + file.getKey());
        }
    }

    private static void quotedBound(Path root) throws Exception {
        Path export = build(root, "quoted.tsv", "\"lead\t2\nplain\t1\n", "quoted");
        String shards = Files.readString(export.resolve("m3.shards.tsv"));
        check(shards.contains("\t\"\"\"lead\"\t"), "csv quotes a shard bound holding a quote");
        SharedLexiconCatalog catalog = SharedLexiconCatalog.open(export);
        check(catalog.find("\"lead").isPresent(), "catalog reads the quoted shard bound");
        catalog.close();
    }

    private static void refusals(Path root) throws Exception {
        String usage = "build INPUT OUTPUT | build --frequency SOURCE_ID LANGUAGE_TAG INPUT"
                + " DIRECTORY | map IMAGE HOLD_MILLIS";
        refused(IllegalArgumentException.class, usage, () -> LexiconTool.main(
                new String[] {"build", "--frequency", "s", "und", "in"}));
        String[][] bad = {{"dup\t1\ndup\t2\n", "frequency line 2: duplicate word"},
                {"a\t01\n", "frequency line 1: count is not a canonical nonnegative long"},
                {"a\t-1\n", "frequency line 1: count is not a canonical nonnegative long"},
                {"a\t9223372036854775808\n",
                        "frequency line 1: count is not a canonical nonnegative long"},
                {"a 1\n", "frequency line 1: expected WORD TAB COUNT"},
                {"\t1\n", "frequency line 1: expected WORD TAB COUNT"},
                {"a\t1\t2\n", "frequency line 1: expected WORD TAB COUNT"},
                {"a\t1\r\n", "frequency line 1: expected WORD TAB COUNT"},
                {"a\t1\n\nb\t2\n", "frequency line 2: expected WORD TAB COUNT"},
                {"", "frequency input is empty"}};
        for (int i = 0; i < bad.length; i++) {
            String run = "bad-" + i;
            String[] input = bad[i];
            refused(IOException.class, input[1], () -> build(root, "x.tsv", input[0], run));
            check(Files.notExists(root.resolve("out-" + run)), "a refused build publishes nothing");
        }
        Path latin1 = Files.write(root.resolve("latin1.tsv"), new byte[] {'a', (byte) 0xe9, '\t',
                '1', '\n'});
        refused(java.nio.charset.MalformedInputException.class, null, () -> LexiconTool.main(
                new String[] {"build", "--frequency", "s", "und", latin1.toString(),
                        root.resolve("out-latin1").toString()}));
        check(Files.notExists(root.resolve("out-latin1")), "non-UTF-8 input publishes nothing");
        refused(IOException.class, "source id must match [A-Za-z0-9][A-Za-z0-9._+-]*",
                () -> LexiconTool.main(new String[] {"build", "--frequency", "a b", "und",
                        root.resolve("in-a/tiny.tsv").toString(), root.resolve("x").toString()}));
        Path busy = Files.createDirectories(root.resolve("out-busy"));
        Files.writeString(busy.resolve("keep.txt"), "keep");
        refused(IOException.class, "output directory is not empty: " + busy,
                () -> build(root, "tiny.tsv", TINY, "busy"));
    }

    private static Map<String, String> files(Path directory) throws Exception {
        Map<String, String> result = new TreeMap<>();
        try (Stream<Path> entries = Files.list(directory)) {
            for (Path file : entries.toList()) {
                result.put(file.getFileName().toString(), HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))));
            }
        }
        return result;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void refused(Class<? extends Throwable> type, String reason, Action action) {
        checks++;
        try {
            action.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown) && (reason == null || reason.equals(thrown.getMessage())))
                return;
            throw new AssertionError("expected " + type.getSimpleName() + " '" + reason + "'",
                    thrown);
        }
        throw new AssertionError("accepted, expected refusal: " + reason);
    }

    private static void delete(Path path) throws Exception {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                for (Path entry : entries.toList()) delete(entry);
            }
        }
        for (int attempt = 0; ; attempt++) {
            try {
                Files.deleteIfExists(path);
                return;
            } catch (IOException stillMappedOnWindows) {
                if (attempt == 100) throw stillMappedOnWindows;
                System.gc();
                Thread.sleep(20);
            }
        }
    }
}
