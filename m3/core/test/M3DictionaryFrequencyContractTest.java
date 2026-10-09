/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Contract proof for dictionary/frequency source rows and typed M3JDK receivers. */
public final class M3DictionaryFrequencyContractTest {
    private static int checks;

    private static void check(boolean condition) {
        checks++;
        if (!condition) {
            throw new AssertionError("check " + checks);
        }
    }

    private static final String MANIFEST_HEADER =
            "source_id\tcanonical_name\tsynexia_path\trecord_id_field"
                    + "\tmapping_fields\tprecompute_target\tdata_license"
                    + "\tdata_policy\tprecompute_fields";

    private static Map<String, String[]> parseManifest(String manifest) {
        String[] lines = manifest.split("\n", -1);
        if (lines.length < 2 || !MANIFEST_HEADER.equals(lines[0])
                || !lines[lines.length - 1].isEmpty()) {
            throw new IllegalArgumentException("manifest header/termination");
        }
        Map<String, String[]> rows = new LinkedHashMap<>();
        for (int line = 1; line < lines.length - 1; line++) {
            String[] fields = lines[line].split("\t", -1);
            if (fields.length != 9 || fields[0].isEmpty()
                    || rows.putIfAbsent(fields[0], fields) != null) {
                throw new IllegalArgumentException("manifest row shape or duplicate source");
            }
        }
        return rows;
    }

    private static String[] row(Map<String, String[]> rows, String sourceId) {
        String[] value = rows.get(sourceId);
        if (value == null) throw new AssertionError(sourceId + " manifest row missing");
        return value;
    }

    private static Map<String, String[]> parseSourceRevisions(String text) {
        String[] lines = text.split("\n", -1);
        if (lines.length < 2 || !"source_id\tsource_revision\tsource_blob_sha".equals(lines[0])
                || !lines[lines.length - 1].isEmpty()) {
            throw new IllegalArgumentException("source revision header/termination");
        }
        Map<String, String[]> revisions = new LinkedHashMap<>();
        for (int line = 1; line < lines.length - 1; line++) {
            String[] fields = lines[line].split("\t", -1);
            if (fields.length != 3 || fields[0].isEmpty() || fields[1].isEmpty()
                    || !fields[2].matches("[0-9a-f]{40}")
                    || revisions.putIfAbsent(fields[0], fields) != null) {
                throw new IllegalArgumentException("source revision row shape or duplicate");
            }
        }
        return revisions;
    }

    private static Map<String, Set<String>> parseFieldMap(String fieldMap) {
        String[] lines = fieldMap.split("\n", -1);
        String header = "donor_type\tdonor_field\tdonor_java_type"
                + "\tcanonical_payload_field\tm3jdk_storage\tstatus\tpreservation_rule";
        if (lines.length < 2 || !header.equals(lines[0]) || !lines[lines.length - 1].isEmpty()) {
            throw new IllegalArgumentException("field map header/termination");
        }
        Map<String, Set<String>> fields = new LinkedHashMap<>();
        for (int line = 1; line < lines.length - 1; line++) {
            String[] columns = lines[line].split("\t", -1);
            if (columns.length != 7 || !"MAPPED".equals(columns[5])) {
                throw new IllegalArgumentException("field map row shape/status");
            }
            Set<String> owner = fields.computeIfAbsent(columns[0], ignored -> new LinkedHashSet<>());
            if (!owner.add(columns[1])) {
                throw new IllegalArgumentException("duplicate donor field");
            }
        }
        return fields;
    }

    private static void expect(Class<? extends Throwable> type, Runnable body) {
        try {
            body.run();
            throw new AssertionError("missing " + type.getName());
        } catch (Throwable failure) {
            check(type.isInstance(failure));
        }
    }

    public static void main(String[] args) throws Exception {
        String manifest = Files.readString(
                Path.of("lexicon/synexia-source-manifest.tsv"), StandardCharsets.UTF_8);
        Map<String, String[]> sourceRevisions = parseSourceRevisions(Files.readString(
                Path.of("lexicon/synexia-word-source-revisions.tsv"), StandardCharsets.UTF_8));
        check(sourceRevisions.size() == 2
                && SOURCE_REVISION.equals(sourceRevisions.get("dictlang.dictionary")[1])
                && DICTIONARY_BLOB_SHA.equals(sourceRevisions.get("dictlang.dictionary")[2])
                && SOURCE_REVISION.equals(sourceRevisions.get("dictlang.frequency")[1])
                && FREQUENCY_BLOB_SHA.equals(sourceRevisions.get("dictlang.frequency")[2]));
        expect(IllegalArgumentException.class, () -> parseSourceRevisions(
                "source_id\tsource_revision\tsource_blob_sha\n"
                        + "dictlang.dictionary\t" + SOURCE_REVISION + "\t"
                        + DICTIONARY_BLOB_SHA + "\n"
                        + "dictlang.dictionary\t" + SOURCE_REVISION + "\t"
                        + DICTIONARY_BLOB_SHA + "\n"));
        Map<String, String[]> manifestRows = parseManifest(manifest);
        String[] dictionary = row(manifestRows, "dictlang.dictionary");
        String[] frequency = row(manifestRows, "dictlang.frequency");
        String[] thesaurus = row(manifestRows, "dictlang.thesaurus");
        String[] antonyms = row(manifestRows, "dictlang.antonyms");
        expect(IllegalArgumentException.class, () -> parseManifest(
                MANIFEST_HEADER + "\n" + String.join("\t", dictionary) + "\n"
                        + String.join("\t", dictionary) + "\n"));
        expect(IllegalArgumentException.class, () -> parseManifest(
                MANIFEST_HEADER + "\n" + String.join("\t", dictionary)
                        + "\textra\n"));
        check(dictionary.length == 9 && frequency.length == 9);
        check(thesaurus.length == 9 && antonyms.length == 9);
        check(dictionary[5].equals("M3LexiconPrecompute.IndexWordFacts"));
        check(frequency[5].equals("M3LexiconPrecompute.IndexWordSignal"));
        check(dictionary[8].equals(
                "concept_ids,corpus_count,document_frequency,expansion_word_ids,flags,"
                        + "language_id,lexicon_fingerprint,memberships,subjects,"
                        + "total_corpus_tokens,total_documents,word_count,source_revision"));
        check(frequency[8].equals(
                "code_point_length,first_code_point,flags,frequency_rank,last_code_point,"
                        + "lemma_id,lexical_rank,morphology_mask,phonetic_id,pos_mask,presence64,"
                        + "script_ordinal,sim_hash64,stem_id,utf16_length,source_revision"));
        check(thesaurus[5].equals("M3StringFacts")
                && thesaurus[8].equals("lexeme,related_lexeme"));
        check(antonyms[5].equals("M3StringFacts")
                && antonyms[8].equals("lexeme,related_lexeme"));
        check(!thesaurus[5].contains("IndexWordFacts")
                && !antonyms[5].contains("IndexWordFacts"));

        String fieldMap = Files.readString(
                Path.of("lexicon/synexia-precompute-field-map.tsv"), StandardCharsets.UTF_8);
        Map<String, Set<String>> fieldRows = parseFieldMap(fieldMap);
        check(fieldRows.get("IndexWordFacts").equals(Set.of(
                "languageId", "wordCount", "lexiconFingerprint", "totalCorpusTokens",
                "totalDocuments", "flags", "corpusCount", "documentFrequency",
                "membershipCount/membershipAt", "conceptCount/conceptAt",
                "subjectCount/subjectAt", "expansionSize/expansionWordIdAt", "sourceRevision")));
        check(fieldRows.get("IndexWordSignal").equals(Set.of(
                "stemId", "lemmaId", "phoneticId", "posMask", "morphologyMask",
                "lexicalRank", "utf16Length", "codePointLength", "firstCodePoint",
                "lastCodePoint", "scriptOrdinal", "presence64", "simHash64",
                "frequencyRank", "EMPTY/ASCII/LATIN1/...", "sourceRevision")));
        check(!fieldRows.containsKey("IndexWordSignalProfile"));
        System.out.println("M3JDK_DICTIONARY_FREQUENCY_CONTRACT_PASS checks=" + checks);
    }
}
