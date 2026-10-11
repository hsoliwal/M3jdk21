/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.SharedLexiconCatalog;
import com.m3.text.SharedRelatedLexemeCatalog;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Proof for both Synexia related-lexeme families.
 *
 * <p>This fixture exercises the relation receiver directly. It does not
 * fabricate an M3LEX image; image admission remains the responsibility of
 * {@link SharedLexiconCatalog}. The receiver must still reject unmapped
 * identities, preserve direction and coordinates, and distinguish one target
 * from many.</p>
 */
public final class SharedRelatedLexemeCatalogTest {
    private static final String MAPPING_HEADER =
            "source_id\tsource_path\tsource_kind\tlanguage_tag\trecord_id\tlexeme"
                    + "\tshard_id\timage_row\tmapping_id\tmapping_name\ttranslation_profile"
                    + "\tprecompute_profile\tprecompute_payload";

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("m3-related-lexeme-replay-");
        try {
            writeFixture(root);
            check(SharedRelatedLexemeCatalog.openOptional(root).isPresent(),
                    "optional receiver present");

            try (SharedRelatedLexemeCatalog catalog = SharedRelatedLexemeCatalog.open(root)) {
                check(catalog.relations().size() == 3, "relation count");
                check(catalog.findBySource("dictlang.antonyms").size() == 2,
                        "antonym scope count");
                check(catalog.findBySource("dictlang.thesaurus").size() == 1,
                        "thesaurus scope count");

                List<SharedRelatedLexemeCatalog.Relation> antonyms =
                        catalog.findAll("dictlang.antonyms", "cold");
                check(antonyms.size() == 2, "multi-target findAll");
                check(antonyms.get(0).relatedLexeme().equals("hot"),
                        "first antonym direction");
                check(antonyms.get(1).relatedLexeme().equals("warm"),
                        "second antonym direction");
                check(antonyms.get(0).coordinate().equals(
                        new SharedLexiconCatalog.Coordinate(0, 0)),
                        "antonym coordinate");
                check(catalog.find("dictlang.antonyms", "cold").isEmpty(),
                        "multi-target find is empty");

                SharedRelatedLexemeCatalog.Relation thesaurus =
                        catalog.find("dictlang.thesaurus", "big").orElseThrow();
                check(thesaurus.lexeme().equals("big"), "thesaurus source lexeme");
                check(thesaurus.relatedLexeme().equals("large"),
                        "thesaurus target direction");
                check(thesaurus.coordinate().equals(
                        new SharedLexiconCatalog.Coordinate(0, 1)),
                        "thesaurus coordinate");
                check(catalog.findAll("dictlang.antonyms", "missing").isEmpty(),
                        "missing relation is empty");

                expectUnsupported(() -> catalog.relations().add(thesaurus),
                        "relations immutable");
            }

            SharedRelatedLexemeCatalog closed = SharedRelatedLexemeCatalog.open(root);
            closed.close();
            expectIllegalState(() -> closed.findBySource("dictlang.antonyms"),
                    "closed receiver");
            System.out.println("M3JDK_RELATED_LEXEME_REPLAY_PASS relations=3 "
                    + "antonyms=2 thesaurus=1");
        } finally {
            deleteTree(root);
        }
    }

    private static void writeFixture(Path root) throws Exception {
        String records = MAPPING_HEADER + "\n"
                + mapping("dictlang.antonyms", "antonyms.txt", "cold", "cold", 0, 0,
                        "map-ant-1") + "\n"
                + mapping("dictlang.thesaurus", "thesaurus.txt", "big", "big", 0, 1,
                        "map-th-1") + "\n";
        Files.writeString(root.resolve("synexia.records.tsv"), records,
                StandardCharsets.UTF_8);

        Files.writeString(root.resolve("synexia.related-sources.tsv"),
                "source_id\n"
                        + "dictlang.antonyms\n"
                        + "dictlang.thesaurus\n",
                StandardCharsets.UTF_8);

        String relations = "source_id\trecord_id\tlexeme\trelated_lexeme\tshard_id\timage_row\n"
                + "dictlang.antonyms\tcold\tcold\thot\t0\t0\n"
                + "dictlang.antonyms\tcold\tcold\twarm\t0\t0\n"
                + "dictlang.thesaurus\tbig\tbig\tlarge\t0\t1\n";
        Files.writeString(root.resolve(SharedRelatedLexemeCatalog.FILE), relations,
                StandardCharsets.UTF_8);
    }

    private static String mapping(String sourceId, String sourcePath, String recordId,
                                  String lexeme, int shardId, int imageRow,
                                  String mappingId) {
        return String.join("\t", sourceId, sourcePath, "resource", "und", recordId, lexeme,
                Integer.toString(shardId), Integer.toString(imageRow), mappingId,
                sourceId.substring(sourceId.lastIndexOf('.') + 1), "none", "none", "{}");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectUnsupported(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message + " accepted");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    private static void expectIllegalState(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message + " accepted");
        } catch (IllegalStateException expected) {
            // expected
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
}
