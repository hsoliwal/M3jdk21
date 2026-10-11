import com.m3.text.M3PhrasePrecompute;
import com.m3.text.M3PhraseSidecarCatalog;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Executable proof for the m3phrase-v1 exporter/receiver boundary. */
public final class M3PhraseSidecarCatalogTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        assertSourceManifestPhraseFields();
        Path root = Files.createTempDirectory("m3phrase-sidecar-");
        try {
            String header = "schema_version\tsource_id\trecord_id\tsource_revision"
                    + "\tvocabulary_fingerprint\tsource_token_ids\ttarget_token_ids\n";
            String content = header
                    + "m3phrase-v1\ttranslate.index-phrases\tphrases-1\tsynexia-r1\tlexicon-a\t1,2\t8\n"
                    + "m3phrase-v1\ttranslate.index-phrases\tphrases-1\tsynexia-r1\tlexicon-a\t4\t-\n"
                    + "m3phrase-v1\ttranslate.index-phrases\tphrases-2\tsynexia-r1\tlexicon-b\t1\t7\n";
            Files.writeString(root.resolve(M3PhraseSidecarCatalog.FILE_NAME), content,
                    StandardCharsets.UTF_8);
            M3PhraseSidecarCatalog sidecar = M3PhraseSidecarCatalog.open(root);
            M3PhrasePrecompute.Scope scopeA =
                    new M3PhrasePrecompute.Scope("translate.index-phrases", "phrases-1",
                            "synexia-r1", "lexicon-a");
            M3PhrasePrecompute.Catalog catalogA = sidecar.phraseAt(scopeA).orElseThrow();
            check(sidecar.rowCount() == 3, "row count");
            check(Arrays.equals(catalogA.rewrite(new int[]{1, 2, 4}), new int[]{8}),
                    "longest-prefix and empty replacement");
            check(catalogA.longestMatchAt(new int[]{1, 2}, 0).orElseThrow()
                    .targetTokenIds()[0] == 8, "target IDs");
            M3PhrasePrecompute.Scope scopeB =
                    new M3PhrasePrecompute.Scope("translate.index-phrases", "phrases-2",
                            "synexia-r1", "lexicon-b");
            check(sidecar.phraseAt(scopeB).orElseThrow().rewrite(new int[]{1})[0] == 7,
                    "scope isolation");
            check(sidecar.phraseAt(new M3PhrasePrecompute.Scope("translate.index-phrases",
                    "phrases-1", "synexia-r1", "other")).isEmpty(), "foreign scope rejected");

            Files.writeString(root.resolve(M3PhraseSidecarCatalog.FILE_NAME),
                    content.replace("1,2\t8\n", "1,2\t8\n1,2\t9\n"),
                    StandardCharsets.UTF_8);
            expectIOException(() -> M3PhraseSidecarCatalog.open(root),
                    "duplicate rows rejected");
            System.out.println("M3JDK_PHRASE_SIDECAR_PASS checks=" + checks + " rows=3");
        } finally {
            deleteTree(root);
        }
    }


    private static void assertSourceManifestPhraseFields() throws Exception {
        String row = Files.readAllLines(Path.of("m3/lexicon/synexia-source-manifest.tsv"),
                        StandardCharsets.UTF_8).stream()
                .filter(line -> line.startsWith("translate.index-phrases\\t"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("phrase source manifest row missing"));
        String[] fields = row.split("\\t", -1);
        check(fields.length == 9, "phrase source manifest column count");
        String expected = Files.readAllLines(
                        Path.of("m3/lexicon/synexia-precompute-field-map.tsv"),
                        StandardCharsets.UTF_8).stream()
                .map(line -> line.split("\\t", -1))
                .filter(cells -> cells.length >= 6
                        && (cells[0].equals("M3PhrasePrecompute.Scope")
                        || cells[0].equals("M3PhrasePrecompute.Phrase"))
                        && cells[5].equals("MAPPED"))
                .map(cells -> cells[3])
                .collect(java.util.stream.Collectors.joining(","));
        check(expected.equals("source_id,record_id,source_revision,vocabulary_fingerprint,"
                        + "source_token_ids,target_token_ids"),
                "phrase field-map union remains complete");
        check(fields[8].equals(expected), "phrase manifest follows field-map union");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void expectIOException(ThrowingRunnable action, String message) throws Exception {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (java.io.IOException expected) {
            checks++;
        }
    }

    private static void deleteTree(Path root) throws Exception {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (java.io.IOException failure) {
                    throw new RuntimeException(failure);
                }
            });
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
