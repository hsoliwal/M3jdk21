/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Contract proof for dictionary/frequency source rows and typed M3JDK receivers. */
public final class M3DictionaryFrequencyContractTest {
    private static int checks;

    private static void check(boolean condition) {
        checks++;
        if (!condition) {
            throw new AssertionError("check " + checks);
        }
    }

    private static String[] row(String manifest, String sourceId) {
        return Arrays.stream(manifest.split("\\n", -1))
                .filter(line -> line.startsWith(sourceId + "\t"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(sourceId + " manifest row missing"))
                .split("\t", -1);
    }

    public static void main(String[] args) throws Exception {
        String manifest = Files.readString(
                Path.of("lexicon/synexia-source-manifest.tsv"), StandardCharsets.UTF_8);
        String[] dictionary = row(manifest, "dictlang.dictionary");
        String[] frequency = row(manifest, "dictlang.frequency");
        check(dictionary.length == 9 && frequency.length == 9);
        check(dictionary[5].equals("M3LexiconPrecompute.IndexWordFacts"));
        check(frequency[5].equals("M3LexiconPrecompute.IndexWordSignal"));
        check(dictionary[8].equals(
                "concept_ids,corpus_count,document_frequency,expansion_word_ids,flags,"
                        + "language_id,lexicon_fingerprint,memberships,subjects,"
                        + "total_corpus_tokens,total_documents,word_count"));
        check(frequency[8].equals(
                "code_point_length,first_code_point,flags,frequency_rank,last_code_point,"
                        + "lemma_id,lexical_rank,morphology_mask,phonetic_id,pos_mask,presence64,"
                        + "script_ordinal,sim_hash64,stem_id,utf16_length"));

        String fieldMap = Files.readString(
                Path.of("lexicon/synexia-precompute-field-map.tsv"), StandardCharsets.UTF_8);
        check(fieldMap.contains("IndexWordFacts\tlanguageId\tint\tlanguage_id"));
        check(fieldMap.contains("IndexWordFacts\tconceptCount/conceptAt\tlong[]\tconcept_ids"));
        check(fieldMap.contains("IndexWordSignal\tfrequencyRank\tint\tfrequency_rank"));
        check(fieldMap.contains("IndexWordSignal\tstemId\tlong\tstem_id"));
        check(!fieldMap.contains("IndexWordSignalProfile"));
        System.out.println("M3JDK_DICTIONARY_FREQUENCY_CONTRACT_PASS checks=" + checks);
    }
}
