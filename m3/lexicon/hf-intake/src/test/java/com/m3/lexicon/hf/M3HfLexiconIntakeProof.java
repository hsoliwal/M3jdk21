// SPDX-License-Identifier: Apache-2.0
package com.m3.lexicon.hf;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

/** Dependency-free differential/refusal proof; executable by javac and Maven. */
public final class M3HfLexiconIntakeProof {
    private static int checks;
    private static final String REVISION = "0123456789abcdef0123456789abcdef01234567";
    private M3HfLexiconIntakeProof() { }

    private static void require(boolean valid, String label) {
        checks++;
        if (!valid) throw new AssertionError(label);
    }

    private static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static M3HfLexiconIntakeRecipe.Approval approved(byte[] bytes,
            M3HfLexiconIntakeRecipe.Kind kind, long maxBytes, long maxRows) throws Exception {
        return new M3HfLexiconIntakeRecipe.Approval("owner/reviewed-dataset", REVISION,
                "language/rows.tsv", sha(bytes), "apache-2.0", "Operator-reviewed attribution",
                URI.create("https://example.org/rights/evidence"), "hi",
                kind, kind == M3HfLexiconIntakeRecipe.Kind.GRAMMAR_CONLLU ? 1 : 0,
                kind == M3HfLexiconIntakeRecipe.Kind.FREQUENCY_TSV ? 1 : -1, false,
                maxBytes, maxRows, "APPROVED");
    }

    private static void fails(IoAction action, String reason) throws Exception {
        boolean refused = false;
        try { action.run(); }
        catch (IOException | IllegalArgumentException expected) { refused = true; }
        require(refused, reason);
    }

    @FunctionalInterface private interface IoAction { void run() throws Exception; }

    private static List<String> resourceLines(String resource) throws IOException {
        try (var input = M3HfLexiconIntakeProof.class.getResourceAsStream(resource)) {
            require(input != null, "catalogue resource exists: " + resource);
            if (input == null) return List.of();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .filter(line -> !line.isBlank() && !line.startsWith("#")).toList();
        }
    }

    private static String[] splitTsv(String line) {
        return line.split(String.valueOf((char) 9), -1);
    }

    private static String discoveryRecord(List<String> lines, String sourceId) {
        return lines.stream().skip(1)
                .filter(line -> sourceId.equals(splitTsv(line)[0]))
                .findFirst().orElse("");
    }

    private static int verifyLanguageResourceDiscovery() throws IOException {
        List<String> lines = resourceLines(
                "/com/m3/lexicon/hf/language-resource-discovery.tsv");
        require(lines.size() == 235, "234 language-resource candidates plus header");
        List<String> expectedHeader = List.of("source_id", "resource", "category",
                "data_shape_and_identity", "rights_and_attribution", "m3_candidate_use",
                "admission_blockers", "state", "evidence");
        require(List.of(splitTsv(lines.getFirst())).equals(expectedHeader),
                "language-resource catalogue schema");
        java.util.Set<String> sourceIds = new java.util.HashSet<>();
        for (int i = 1; i < lines.size(); i++) {
            String[] fields = splitTsv(lines.get(i));
            require(fields.length == 9, "language-resource row has nine fields: " + i);
            if (fields.length != 9) continue;
            require(!fields[0].isBlank() && sourceIds.add(fields[0]),
                    "unique nonempty language-resource source ID: " + i);
            require(!fields[1].isBlank() && !fields[2].isBlank()
                    && !fields[3].isBlank() && !fields[4].isBlank()
                    && !fields[5].isBlank() && !fields[6].isBlank(),
                    "catalogue row has data shape, rights, fit and blockers: " + i);
            require("REFERENCE_ONLY".equals(fields[7]),
                    "discovery never implies data admission: " + fields[0]);
            require(java.util.Arrays.stream(fields[8].split(";", -1))
                    .allMatch(uri -> uri.startsWith("https://")),
                    "catalogue evidence links are HTTPS: " + fields[0]);
        }
        require(sourceIds.equals(java.util.Set.of(
                "iana-language-subtags",
                "wikidata-lexemes",
                "unicode-cldr-48-2",
                "unicode-ucd-17",
                "iso-3166-code-elements",
                "universal-dependencies",
                "unimorph-language-files",
                "glottolog-5-3",
                "grambank",
                "phoible-2",
                "wals",
                "lexibank",
                "wiktionary-wiktextract",
                "panlex",
                "open-multilingual-wordnet",
                "open-english-wordnet-namenet-2025",
                "leipzig-corpora",
                "google-books-ngrams-v3",
                "tatoeba",
                "opus-hplt-v3",
                "geonames",
                "bipm-si-brochure-2026",
                "mozilla-common-voice-26",
                "hf-vocabradar-wordfreq",
                "hf-finefreq",
                "hf-lefff-morphology",
                "hf-seacrowd-unimorph",
                "hf-multilingual-core-vocabulary",
                "hf-langdex",
                "kaggle-langdex-notebook",
                "iso-4217-code-elements",
                "hf-langdex-dictionaries",
                "dgt-tm",
                "elrc-share",
                "apertium-language-data",
                "wordfreq-3-0-2",
                "subtlex-us",
                "nga-geonames",
                "unicode-uax29-18",
                "clarin-vlo-api-4-15",
                "olac-archive-network-2026",
                "elra-language-resource-catalogue",
                "concepticon-3-4",
                "norare-1-0",
                "asjp-database",
                "clics-4",
                "wold",
                "asl-lex-2",
                "mudidi-hf-2026",
                "openslr-beep",
                "dobes-archive",
                "elar-archive",
                "elarspeech-preprint-2026",
                "ai4bharat-indic-frequency-lists",
                "morphynet-multilingual-morphology",
                "cmudict-us-english-pronunciations",
                "hunspell-language-dictionaries",
                "global-wordnet-discovery-index",
                "babelnet-restricted-lexical-graph",
                "hf-paranames-1-0",
                "giellalt-language-repositories",
                "gf-resource-grammar-library",
                "sigmorphon-2022-segmentation",
                "hf-world-languages-7130",
                "wordfreq-3-0-2-data",
                "reflex-africa-v1-0",
                "opus-corpus-index-2026",
                "google-fleurs-102",
                "google-waxalnlp",
                "dictionaria-cldf-dictionaries",
                "nchlt-south-african-resource-bundles",
                "sails-south-american-grammar",
                "word2word-bilingual-lexicons",
                "wordhoard-frequency-morphology",
                "muse-bilingual-dictionary-benchmarks",
                "numeralbank-chan-numerals-2019",
                "numeralbank-cosinus-v2-2026",
                "ai4bharat-aksharantar-26m",
                "ai4bharat-samanantar-v0-3",
                "ai4bharat-indiccorp-v2",
                "ai4bharat-naamapadam-11",
                "mozilla-common-voice-text-lid-300",
                "dbnary-ontolex-wiktionary-26",
                "masakhaner2-african-20",
                "ids-intercontinental-dictionary-series",
                "autotyp-typology-database-v1-1-1",
                "apics-creole-grammar-cldf",
                "dplace-language-culture-environment",
                "apertium-language-data-packs",
                "parseme-multilingual-mwe-corpus-v2",
                "wikipron-pronunciation-lexicons",
                "hplt-monolingual-v3",
                "oldi-flores-plus-4-6",
                "doreco-spoken-documentation-2",
                "tico-19-medical-translation",
                "mafand-mt-african-news",
                "opus-100-parallel-corpus",
                "afridoc-mt-health",
                "multiconer-v2-entities",
                "google-smol-gatitos",
                "sprakbanken-flex",
                "sprakbanken-kubord-lex",
                "sprakbanken-lexin",
                "sprakbanken-parole-lexplus",
                "sprakbanken-swedish-names-2023",
                "linguabase-english-lexical-graph",
                "bashkir-frequency-index",
                "bashkir-word-ngram-index",
                "eurovoc-4-24",
                "ufal-morfflex-cz-2-1",
                "dmii-core-icelandic",
                "unidic-ja-3-1-0",
                "taigi-common-use-dictionary",
                "hf-uzbek-lexicon-skeleton",
                "hf-khmer-search-frequency",
                "hf-afri-dict",
                "hf-fineweb-2",
                "sign-language-dataset-compendium-v1-5",
                "easier-multilingual-sign-language-wordnet",
                "corola-romanian-frequency-lists",
                "hf-mainlp-word-level-translations",
                "hf-jnext-chinese-word-frequency",
                "hf-google-books-ngram-pos",
                "hf-dillm-persian-grammar-corpus",
                "hf-pashto-grammar-100",
                "hf-qor-af-soomaali-frequency",
                "hf-kablex-kabyle-lexicon",
                "hf-kabinflect-kabyle-morphology",
                "hf-hmar-heritage-wordlist",
                "hf-kencorpus-kenpos",
                "hf-galahad-dutch-frequency",
                "hf-sango-vocabulary-grammar",
                "hf-haajidheere-somali-dictionary",
                "hf-nltk-word-lists",
                "hf-nltk-swadesh-24",
                "massive-nlu-1-1",
                "madar-arabic-dialect-lexicon",
                "samer-arabic-readability-v2",
                "sil-bloom-library-discovery",
                "arbml-masader-catalogue",
                "hf-vocab-bloom-hub-en",
                "madlad-400-v1-5",
                "hf-wikidata-label-maps-2025",
                "ai4bharat-bhasaanuvaad",
                "iate-eu-terminology",
                "crossgram-magram",
                "gff-geez-data-archive",
                "kelly-language-frequency-lists",
                "clear-global-gamayun-kits",
                "afrispeech-africa-corpus",
                "ldc-global-yoruba-lexical-database",
                "frequencywords-opensubtitles",
                "lexique-fr-4-10",
                "fao-agrovoc",
                "gemet-environment-thesaurus",
                "google-dakshina-v1",
                "lexibank-cldf-v2-2",
                "lexibank-austronesian-comparative-dictionary",
                "dictionaria-teanu",
                "shabd-cstt-indian-technical-terminology",
                "iwn-en-indowordnet",
                "multilingual-open-text-mot-release1",
                "multi-simlex-12-language",
                "tsammalex-v0-3",
                "dicovalence-french-valency-v2",
                "morphalou-3-1",
                "rl-fr-2025-03",
                "bel-rl-fr-3-1",
                "prolexbase-2-2",
                "unicode-unilex",
                "polyglotta-africana-cldf-v2-0",
                "lexik-fr-1-0-2026-10",
                "ealex-east-african-lexome-2026",
                "nzonza-cialr-44",
                "dannet-danish-wordnet-2026-08-21",
                "aiatsis-austlang-live-2026",
                "afwn-2022-release-1-0",
                "crubadan-hf-character-trigrams-449",
                "elg-catalogue-release-3-0-2026",
                "oanc-open-frequency-derivation",
                "elexis-dictionary-rest-discovery",
                "hf-multilingual-frequency-lists",
                "hf-mlnorm-resources-composite",
                "hf-multilexnorm2026-full-pub",
                "hf-nanai-russian-dictionary",
                "hf-haitian-creole-word-freq",
                "hf-sindhi-open-lexicon",
                "hf-javanese-multilingual-lexicon",
                "hf-tajiknlp-ar-ru-en-dictionary",
                "hf-tigre-data-lexicon",
                "hf-tigre-generated-dictionary",
                "nict-japanese-english-kyoto-wiki-corpus",
                "kaggle-google-web-english-frequency-333333",
                "hf-ratman-african-stopword-frequency",
                "hf-amharic-gemination-lexicon-v3",
                "hf-khmer-dictionary-44k",
                "hf-khpos-seanghay-2023",
                "hf-sasak-benchmark-100k",
                "hf-georgian-case-alignment",
                "hf-tibetan-word-segmentation-100",
                "hf-bridgeconn-sign-dictionary-isl",
                "hf-hmar-numeral-words",
                "hf-hmar-unigrams",
                "hf-hmar-corpus-archive",
                "hf-robinson-hausa-en-1914",
                "hf-african-tokenizer-challenge-v1",
                "hf-naath-en-nuer-dinka-sw-corpus",
                "hf-dusun-dictionary-corpus",
                "hf-okinawan-dictionary-index",
                "hf-tigre-parallel-multilingual",
                "hf-multilingual-endangered-cultural-heritage",
                "hf-nenets-mixed-schema",
                "cefrlex-six-language",
                "clearpond-five-language-neighborhoods",
                "manulex-french-child-directed-lexicons",
                "multi-lex-french-ngrams-2022",
                "hf-malayalam-pronunciation-lexicon",
                "hf-nusax-mt",
                "hf-maori-language-eval",
                "hf-afrisud-treebanks",
                "hf-wolof-french-jolof",
                "hf-apertium-breton-french",
                "inuktitutcomputing-uqailaut-roots",
                "nrc-nunavut-hansard-3",
                "hf-americasnli",
                "hf-teara-maori-english",
                "jmdict-edrdg",
                "jmnedict-edrdg",
                "kanjidic2-edrdg",
                "cc-cedict-v2",
                "iso-639-2023",
                "iso-15924-script-codes",
                "clts-2-3-0",
                "multext-east-v6-morphosyntax"
        )), "exact 234 language-resource source IDs match the pinned catalogue");
        String jmdict = discoveryRecord(lines, "jmdict-edrdg");
        require(jmdict.contains("JMdict_e/JMdict_b")
                && jmdict.contains("legacy generation")
                && jmdict.contains("parallel NG generation"),
                "JMdict edition and generation remain pinned separately");
        String kanjidic2 = discoveryRecord(lines, "kanjidic2-edrdg");
        require(kanjidic2.contains("CC BY-NC-SA 4.0")
                && kanjidic2.contains("exclude SKIP from any commercial payload"),
                "KANJIDIC2 SKIP rights stay field-scoped and noncommercial");
        String ccCedict = discoveryRecord(lines, "cc-cedict-v2");
        require(ccCedict.contains("case-sensitively")
                && ccCedict.contains("[Wang2]") && ccCedict.contains("[wang2]")
                && ccCedict.contains("case-fold Pinyin"),
                "CC-CEDICT V2 preserves case-sensitive Pinyin identity");
        String iso639 = discoveryRecord(lines, "iso-639-2023");
        require(iso639.contains("Edition 2 (2023)")
                && iso639.contains("ISO 639-1") && iso639.contains("ISO 639-5")
                && iso639.contains("free-of-charge use")
                && iso639.contains("BCP 47 tags"),
                "ISO language-code sets, code-use rights and namespace boundaries stay explicit");
        String iso15924 = discoveryRecord(lines, "iso-15924-script-codes");
        require(iso15924.contains("four-letter alphabetic")
                && iso15924.contains("three-digit numeric")
                && iso15924.contains("not language IDs")
                && iso15924.contains("Unicode Script values"),
                "ISO script-code forms and distinct language/Unicode identities stay explicit");
        String clts = discoveryRecord(lines, "clts-2-3-0");
        require(clts.contains("five transcription systems")
                && clts.contains("22 transcription datasets")
                && clts.contains("six sound-class systems")
                && clts.contains("CC BY 4.0"),
                "CLTS version, transcription coverage, feature schemes and attribution stay explicit");
        String multext = discoveryRecord(lines, "multext-east-v6-morphosyntax");
        require(multext.contains("2022-06-24") && multext.contains("compact MSD tag")
                && multext.contains("does not establish one blanket redistribution license")
                && multext.contains("unknown versus absent"),
                "MULTEXT-East versioned language-specific grammar specs and rights stay explicit");
        String sindhi = discoveryRecord(lines, "hf-sindhi-open-lexicon");
        require(sindhi.contains("223,342") && sindhi.contains("lexical_id")
                && sindhi.contains("entry_id") && sindhi.contains("schema casting"),
                "Sindhi source IDs, heterogeneous schema and license review stay explicit");
        String jav = discoveryRecord(lines, "hf-javanese-multilingual-lexicon");
        require(jav.contains("202,912") && jav.contains("32 historical dictionaries")
                && jav.contains("CC BY-NC 4.0") && jav.contains("contact-gated"),
                "Javanese original IDs, historical varieties and noncommercial gate stay explicit");
        String tajik = discoveryRecord(lines, "hf-tajiknlp-ar-ru-en-dictionary");
        require(tajik.contains("80,093") && tajik.contains("no Tajik-language entries")
                && tajik.contains("underlying lexical-file rights"),
                "Tajik publisher label, actual language pairs and source rights stay distinct");
        String tigre = discoveryRecord(lines, "hf-tigre-data-lexicon");
        require(tigre.contains("419,627") && tigre.contains("contact information")
                && tigre.contains("stable source ID"),
                "Tigre data remains gated and source identity unverified");
        String tigreCandidates = discoveryRecord(lines, "hf-tigre-generated-dictionary");
        require(tigreCandidates.contains("30,000+") && tigreCandidates.contains("6,164-entry")
                && tigreCandidates.contains("not verified translation"),
                "Tigre embedding output remains ranked candidate evidence, not gold translation");
        String kyoto = discoveryRecord(lines, "nict-japanese-english-kyoto-wiki-corpus");
        require(kyoto.contains("14,111") && kyoto.contains("50,000")
                && kyoto.contains("CC BY-SA 3.0"),
                "NICT Kyoto corpus and term-pair source coordinates stay explicit");
        String kaggleFrequency = discoveryRecord(lines, "kaggle-google-web-english-frequency-333333");
        require(kaggleFrequency.contains("333,333") && kaggleFrequency.contains("no license grant")
                && kaggleFrequency.contains("no payload download"),
                "Kaggle frequency lead remains rights-unresolved discovery only");
        String ratman = discoveryRecord(lines, "hf-ratman-african-stopword-frequency");
        require(ratman.contains("Lugbarati Official") && ratman.contains("incompatible schemas")
                && ratman.contains("Apache-2.0"),
                "Ratman preserves per-language file shapes and the viewer/license caveats");
        String amharic = discoveryRecord(lines, "hf-amharic-gemination-lexicon-v3");
        require(amharic.contains("86,022") && amharic.contains("8,506")
                && amharic.contains("8,526") && amharic.contains("no source-native lexical ID"),
                "Amharic reading ambiguity and source identity remain explicit");
        String khmerDictionary = discoveryRecord(lines, "hf-khmer-dictionary-44k");
        require(khmerDictionary.contains("44,706") && khmerDictionary.contains("not commercial")
                && khmerDictionary.contains("no source-native stable entry ID"),
                "Khmer dictionary rights and repeated-headword identity remain gated");
        String khpos = discoveryRecord(lines, "hf-khpos-seanghay-2023");
        require(khpos.contains("12,000 sentence records") && khpos.contains("25,626 words")
                && khpos.contains("CC BY-NC-SA 4.0") && khpos.contains("sentence ID"),
                "Khmer POS fixture preserves segmentation, tags, and noncommercial terms");
        String sasak = discoveryRecord(lines, "hf-sasak-benchmark-100k");
        require(sasak.contains("100,000") && sasak.contains("2,761")
                && sasak.contains("meaning_id") && sasak.contains("underlying content rights"),
                "Sasak records preserve dialect/source IDs and block unverified rights");
        String georgian = discoveryRecord(lines, "hf-georgian-case-alignment");
        require(georgian.contains("370 examples") && georgian.contains("seven verb-series")
                && georgian.contains("CC BY 4.0") && georgian.contains("not a lexeme ID"),
                "Georgian case contrasts preserve source coordinates and grammar-only semantics");
        String tibetan = discoveryRecord(lines, "hf-tibetan-word-segmentation-100");
        require(tibetan.contains("100 modern Lhasa") && tibetan.contains("2,634")
                && tibetan.contains("U+0F0B") && tibetan.contains("no inter-annotator agreement"),
                "Tibetan segmentation retains script boundary, dialect and annotation limitations");
        String signIsl = discoveryRecord(lines, "hf-bridgeconn-sign-dictionary-isl");
        require(signIsl.contains("3,077 lexical items") && signIsl.contains("7.11 GB")
                && signIsl.contains("CC BY-SA 4.0") && signIsl.contains("signer consent"),
                "Indian Sign Language media stays reference-only pending consent and identity review");
        String hmarNumerals = discoveryRecord(lines, "hf-hmar-numeral-words");
        require(hmarNumerals.contains("999,999") && hmarNumerals.contains("zero is omitted")
                && hmarNumerals.contains("Apache-2.0") && hmarNumerals.contains("REFERENCE_ONLY"),
                "Hmar number forms stay language-scoped, observed-only, and gated");
        String hmarUnigrams = discoveryRecord(lines, "hf-hmar-unigrams");
        require(hmarUnigrams.contains("110,603") && hmarUnigrams.contains("4,832,070")
                && hmarUnigrams.contains("ṭ/ț") && hmarUnigrams.contains("raw counts"),
                "Hmar raw counts preserve exact orthography and remain distinct from Zipf scores");
        String hmarArchive = discoveryRecord(lines, "hf-hmar-corpus-archive");
        require(hmarArchive.contains("0001..0012") && hmarArchive.contains("CC BY-NC-SA 4.0")
                && hmarArchive.contains("per-item source/license/community review"),
                "Hmar archive IDs do not transfer rights to linked payloads");
        String robinson = discoveryRecord(lines, "hf-robinson-hausa-en-1914");
        require(robinson.contains("20,628") && robinson.contains("robinson_1914_vol2")
                && robinson.contains("no stable source-entry ID") && robinson.contains("OCR"),
                "Robinson Hausa entries remain historical OCR candidates pending source identity and rights review");
        String tokenizer = discoveryRecord(lines, "hf-african-tokenizer-challenge-v1");
        require(tokenizer.contains("240,000-train/24,000-validation")
                && tokenizer.contains("b04c8d1ceb2f5cd4588862100d08de323dccfbaa")
                && tokenizer.contains("not a gold word-segmentation lexicon"),
                "African tokenizer data remain source-addressed text diagnostics, not lexical gold labels");
        String naath = discoveryRecord(lines, "hf-naath-en-nuer-dinka-sw-corpus");
        require(naath.contains("803k") && naath.contains("CC BY-NC-SA 4.0")
                && naath.contains("no source-native row ID"),
                "Naath sentence pairs remain noncommercial sentence-level fixtures, not M3 lexeme IDs");
        String dusun = discoveryRecord(lines, "hf-dusun-dictionary-corpus");
        require(dusun.contains("24,000") && dusun.contains("9,000 dictionary words")
                && dusun.contains("15,000 phrases/sentences") && dusun.contains("no stable row-ID column"),
                "Dusun bilingual word and phrase rows stay separated and source identity is not invented");
        String okinawan = discoveryRecord(lines, "hf-okinawan-dictionary-index");
        require(okinawan.contains("9.25k") && okinawan.contains("ninjal_sakuin_00001-style")
                && okinawan.contains("Shuri dialect only") && okinawan.contains("CC BY 4.0"),
                "Okinawan index preserves NINJAL row IDs, page and dialect-specific scope");
        String tigreParallel = discoveryRecord(lines, "hf-tigre-parallel-multilingual");
        require(tigreParallel.contains("66.1k") && tigreParallel.contains("132,288")
                && tigreParallel.contains("CC BY 2.0 FR") && tigreParallel.contains("no row ID field"),
                "Tigre parallel corpus preserves mixed source licenses and unresolved count discrepancy");
        String endangered = discoveryRecord(lines, "hf-multilingual-endangered-cultural-heritage");
        require(endangered.contains("1,050") && endangered.contains("269 source-described real")
                && endangered.contains("745 synthetic") && endangered.contains("CC0 1.0"),
                "endangered-language material keeps synthetic and source-described records separate");
        String nenets = discoveryRecord(lines, "hf-nenets-mixed-schema");
        require(nenets.contains("653275c014918400943f924f035089ea9e4a4755")
                && nenets.contains("articles_yamal_expedition.csv")
                && nenets.contains("source/example_nn/example_ru") && nenets.contains("text/title/link"),
                "Nenets cast failure remains an explicit schema-refusal discovery, not an ingested lexicon");
        String cefrlex = discoveryRecord(lines, "cefrlex-six-language");
        require(cefrlex.contains("Seven resources") && cefrlex.contains("six languages")
                && cefrlex.contains("A1") && cefrlex.contains("no explicit data license"),
                "CEFRLex level-frequency scope and unresolved dataset license stay explicit");
        String clearpond = discoveryRecord(lines, "clearpond-five-language-neighborhoods");
        require(clearpond.contains("Dutch, English, French, German and Spanish")
                && clearpond.contains("bigram/biphone probabilities")
                && clearpond.contains("no separate license"),
                "CLEARPOND cross-language neighbor signals and dataset/paper rights stay distinct");
        String manulex = discoveryRecord(lines, "manulex-french-child-directed-lexicons");
        require(manulex.contains("48,886 wordforms") && manulex.contains("23,812 lemmas")
                && manulex.contains("CC BY-NC-SA 3.0"),
                "Manulex grade, identity and noncommercial-sharealike scope stay explicit");
        String multiLex = discoveryRecord(lines, "multi-lex-french-ngrams-2022");
        require(multiLex.contains("2-, 3-, 4- and 5-word")
                && multiLex.contains("license field is blank")
                && multiLex.contains("about 1.4 GB"),
                "Multi-LEX n-gram order, scale and unresolved derivative rights stay explicit");
        String malayalam = discoveryRecord(lines, "hf-malayalam-pronunciation-lexicon");
        require(malayalam.contains("174,849") && malayalam.contains("Nine categorized subsets")
                && malayalam.contains("automatically generated")
                && malayalam.contains("CC BY-SA 4.0"),
                "Malayalam IPA rows preserve generated status, category and separate upstream-rights review");
        String nusax = discoveryRecord(lines, "hf-nusax-mt");
        require(nusax.contains("132 directed language pairs") && nusax.contains("builder-generated enumeration IDs")
                && nusax.contains("CC BY-SA 4.0"),
                "NusaX pair views retain source row provenance and do not invent lexeme equivalence");
        String maoriEval = discoveryRecord(lines, "hf-maori-language-eval");
        require(maoriEval.contains("551 paragraph pairs") && maoriEval.contains("pending kaitiakitanga review")
                && maoriEval.contains("do not train"),
                "Māori legal benchmark remains evaluation-only with its unresolved community license");
        String afrisud = discoveryRecord(lines, "hf-afrisud-treebanks");
        require(afrisud.contains("eight language configurations") && afrisud.contains("nine")
                && afrisud.contains("CC BY-NC 4.0"),
                "AfriSUD language count discrepancy and noncommercial SUD scope remain explicit");
        String wolof = discoveryRecord(lines, "hf-wolof-french-jolof");
        require(wolof.contains("12,084 rows") && wolof.contains("MIT")
                && wolof.contains("no documented source-native row ID"),
                "Wolof export packaging license is not mistaken for cleared underlying source rights");
        String breton = discoveryRecord(lines, "hf-apertium-breton-french");
        require(breton.contains("17,952") && breton.contains("GPL-2.0")
                && breton.contains("lose analyses"),
                "Breton flat pairs retain Apertium license and morphology-loss caveat");
        String uqailaut = discoveryRecord(lines, "inuktitutcomputing-uqailaut-roots");
        require(uqailaut.contains("3,221 text lines") && uqailaut.contains("no distinct license")
                && uqailaut.contains("not a native lexical identifier"),
                "UQAILAUT data rights are not inferred from the repository code license");
        String nunavut = discoveryRecord(lines, "nrc-nunavut-hansard-3");
        require(nunavut.contains("1.3 million aligned sentence pairs") && nunavut.contains("CC BY 4.0")
                && nunavut.contains("not an official transcript or translation"),
                "Nunavut Hansard retains NRC record identity, required disclaimer and permitted-use limits");
        String americas = discoveryRecord(lines, "hf-americasnli");
        require(americas.contains("13.9K") && americas.contains("ten varieties")
                && americas.contains("CC BY-SA 4.0") && americas.contains("three-way NLI label"),
                "AmericasNLI remains variety-scoped task evaluation rather than a translation lexicon");
        String teara = discoveryRecord(lines, "hf-teara-maori-english");
        require(teara.contains("6.49K") && teara.contains("CC BY-NC 3.0")
                && teara.contains("item-level source URL") && teara.contains("item-level exceptions"),
                "Te Ara exports preserve noncommercial and item-specific rights and do not invent source IDs");
        String massive = discoveryRecord(lines, "massive-nlu-1-1");
        require(massive.contains("52 languages") && massive.contains("SLURP") && massive.contains("CC BY 4.0")
                && massive.contains("not a canonical dictionary"),
                "MASSIVE locale, SLURP provenance and non-lexicon scope stay explicit");
        String madar = discoveryRecord(lines, "madar-arabic-dialect-lexicon");
        require(madar.contains("25 city dialect varieties") && madar.contains("CODA")
                && madar.contains("CAPHI") && madar.contains("noncommercial"),
                "MADAR variety and orthographic/phonological coordinates stay explicit");
        String samer = discoveryRecord(lines, "samer-arabic-readability-v2");
        require(samer.contains("40,000 lemmas") && samer.contains("no sublicensing")
                && samer.contains("or modification without separate agreement"),
                "SAMER remains internal-evaluation-only and non-distributable");
        String bloom = discoveryRecord(lines, "sil-bloom-library-discovery");
        require(bloom.contains("363 languages") && bloom.contains("32 language families")
                && bloom.contains("licenses vary by artifact"),
                "Bloom is a discovery collection with per-artifact rights");
        String masader = discoveryRecord(lines, "arbml-masader-catalogue");
        require(masader.contains("GPL-3.0") && masader.contains("does not license the datasets")
                && masader.contains("catalogue records are discovery coordinates"),
                "Masader code license is not attributed to indexed dataset payloads");
        String vocabBloom = discoveryRecord(lines, "hf-vocab-bloom-hub-en");
        require(vocabBloom.contains("seven languages") && vocabBloom.contains("28 grammar-pattern rows")
                && vocabBloom.contains("word plus part_of_speech")
                && vocabBloom.contains("AI-generated-content notice"),
                "Vocab Bloom schema, translation scope and generated-content notice stay explicit");
        String madlad = discoveryRecord(lines, "madlad-400-v1-5");
        require(madlad.contains("ODC-By") && madlad.contains("language/script shard")
                && madlad.contains("not lexeme ID"),
                "MADLAD corpus coordinates and underlying source-rights review stay explicit");
        String wikidataLabels = discoveryRecord(lines, "hf-wikidata-label-maps-2025");
        require(wikidataLabels.contains("725,274,530") && wikidataLabels.contains("613 language codes")
                && wikidataLabels.contains("CC0-1.0") && wikidataLabels.contains("not lexical entries"),
                "Wikidata label snapshot preserves entity identity and avoids lexeme conflation");
        String bhasa = discoveryRecord(lines, "ai4bharat-bhasaanuvaad");
        require(bhasa.contains("44,400 hours") && bhasa.contains("17 million aligned text segments")
                && bhasa.contains("curated, mined and synthetic"),
                "BhasaAnuvaad alignment scale and source mix remain explicit");
        String iate = discoveryRecord(lines, "iate-eu-terminology");
        require(iate.contains("7 million terms") && iate.contains("24 official EU languages plus Latin")
                && iate.contains("no dataset-wide license is inferred"),
                "IATE term identities, language coverage and reuse gate stay explicit");
        String gf = discoveryRecord(lines, "gf-resource-grammar-library");
        require(gf.contains("over 30 languages") && gf.contains("LGPL-3.0 and BSD")
                && gf.contains("file-specific external sources"), "GF source licensing and scope");
        String magram = discoveryRecord(lines, "crossgram-magram");
        require(magram.contains("992 grammaticalization paths") && magram.contains("CC BY 4.0"), "MAGRAM scope/rights");
        String gff = discoveryRecord(lines, "gff-geez-data-archive");
        require(gff.contains("8,743-name list") && gff.contains("preliminary"), "Ge'ez archive caveat");
        String kelly = discoveryRecord(lines, "kelly-language-frequency-lists");
        require(kelly.contains("74,258 lemmas") && kelly.contains("artifact-specific"), "KELLY rights/coverage");
        String gamayun = discoveryRecord(lines, "clear-global-gamayun-kits");
        require(gamayun.contains("CC BY-SA 4.0") && gamayun.contains("Tatoeba"), "Gamayun rights/source");
        String afrispeech = discoveryRecord(lines, "afrispeech-africa-corpus");
        require(afrispeech.contains("693 African languages") && afrispeech.contains("builder code"), "AfriSpeech rights");
        String ldc = discoveryRecord(lines, "ldc-global-yoruba-lexical-database");
        require(ldc.contains("LDC User Agreement") && ldc.contains("tone/diacritics"), "Yoruba data constraints");
        String freqwords = discoveryRecord(lines, "frequencywords-opensubtitles");
        require(freqwords.contains("CC BY-SA 4.0") && freqwords.contains("MIT generator code"), "FrequencyWords license split");
        String lexique = discoveryRecord(lines, "lexique-fr-4-10");
        require(lexique.contains("190,000 French forms") && lexique.contains("CC BY-SA 4.0"), "Lexique release/rights");
        String agrovoc = discoveryRecord(lines, "fao-agrovoc");
        require(agrovoc.contains("CC BY IGO 3.0") && agrovoc.contains("contributor institutions"), "AGROVOC per-language rights");
        String gemet = discoveryRecord(lines, "gemet-environment-thesaurus");
        require(gemet.contains("37 languages") && gemet.contains("uneven translation completeness"), "GEMET coverage caveat");
        String dakshina = discoveryRecord(lines, "google-dakshina-v1");
        require(dakshina.contains("Twelve languages") && dakshina.contains("CC BY-SA 4.0")
                && dakshina.contains("not semantic translation"), "Dakshina romanization scope");
        String lexibank = discoveryRecord(lines, "lexibank-cldf-v2-2");
        require(lexibank.contains("134 datasets") && lexibank.contains("component/source"), "Lexibank component rights");
        String acd = discoveryRecord(lines, "lexibank-austronesian-comparative-dictionary");
        require(acd.contains("146,733 lexemes") && acd.contains("38,249 records"), "ACD missing-source caveat");
        String teanu = discoveryRecord(lines, "dictionaria-teanu");
        require(teanu.contains("1,900 entries") && teanu.contains("2,800 sense definitions"), "Teanu sense structure");
        String shabd = discoveryRecord(lines, "shabd-cstt-indian-technical-terminology");
        require(shabd.contains("3M+ cumulative headwords")
                && shabd.contains("license/redistribution grant was not established"), "CSTT no implied data license");
        String iwn = discoveryRecord(lines, "iwn-en-indowordnet");
        require(iwn.contains("CC BY-NC-SA 4.0") && iwn.contains("native synset IDs"), "IndoWordnet rights/identity");
        String mot = discoveryRecord(lines, "multilingual-open-text-mot-release1");
        require(mot.contains("44 languages") && mot.contains("43;")
                && mot.contains("CC BY 4.0") && mot.contains("MIT code"),
                "MOT release-count discrepancy and data/code license split stay explicit");
        String multiSimlex = discoveryRecord(lines, "multi-simlex-12-language");
        require(multiSimlex.contains("12 languages") && multiSimlex.contains("1,888")
                && multiSimlex.contains("CC BY-NC-ND 4.0") && multiSimlex.contains("pair number"),
                "Multi-SimLex remains a language-scoped evaluation benchmark");
        String tsammalex = discoveryRecord(lines, "tsammalex-v0-3");
        require(tsammalex.contains("CC BY 4.0") && tsammalex.contains("community consent")
                && tsammalex.contains("Kalahari") && tsammalex.contains("taxon"),
                "Tsammalex domain, consent and taxon identity remain explicit");
        String dicovalence = discoveryRecord(lines, "dicovalence-french-valency-v2");
        require(dicovalence.contains("3,700") && dicovalence.contains("8,000")
                && dicovalence.contains("LGPL-LR") && dicovalence.contains("CC BY-NC 4.0")
                && dicovalence.contains("conflict"),
                "DICOVALENCE source scope and unresolved mirrored-license conflict stay explicit");
        String signCompendium = discoveryRecord(lines, "sign-language-dataset-compendium-v1-5");
        require(signCompendium.contains("87") && signCompendium.contains("v1.5.0")
                && signCompendium.contains("no blanket redistribution grant"),
                "sign-language compendium remains an index with per-resource rights");
        String signWordnet = discoveryRecord(lines, "easier-multilingual-sign-language-wordnet");
        require(signWordnet.contains("BSL, DGS, DSGS, GSL, LSF, NGT, PJM and STS")
                && signWordnet.contains("CC BY-NC-SA 4.0")
                && signWordnet.contains("only to sign-ID/synset-ID links"),
                "eight sign varieties and link-only noncommercial license scope stay explicit");
        String corola = discoveryRecord(lines, "corola-romanian-frequency-lists");
        require(corola.contains("2,260,992") && corola.contains("1,635,250")
                && corola.contains("Rights/License field is blank"),
                "CoRoLa form/lemma sizes and missing export license remain explicit");
        String mainlp = discoveryRecord(lines, "hf-mainlp-word-level-translations");
        require(mainlp.contains("100 query concepts") && mainlp.contains("25-concept example pool")
                && mainlp.contains("MIT"),
                "MaiNLP split and dataset-card license remain explicit");
        String chineseFrequency = discoveryRecord(lines, "hf-jnext-chinese-word-frequency");
        require(chineseFrequency.contains("12,472,823") && chineseFrequency.contains("Jieba")
                && chineseFrequency.contains("source corpus identity and redistribution rights are not established"),
                "Chinese frequency tokenizer, scale and upstream-rights gap remain explicit");
        String googlePos = discoveryRecord(lines, "hf-google-books-ngram-pos");
        require(googlePos.contains("6,320,259") && googlePos.contains("1800-2019")
                && googlePos.contains("CC BY 3.0"),
                "Google Books POS/time slice retains size, vintage and attribution");
        String dillm = discoveryRecord(lines, "hf-dillm-persian-grammar-corpus");
        require(dillm.contains("41,922") && dillm.contains("3.5 million tokens")
                && dillm.contains("not parsed rule tables"),
                "Persian grammar prose is not mistaken for structured rules");
        String pashtoGrammar = discoveryRecord(lines, "hf-pashto-grammar-100");
        require(pashtoGrammar.contains("One hundred JSONL")
                && pashtoGrammar.contains("CC BY 4.0")
                && pashtoGrammar.contains("not comprehensive"),
                "Pashto grammar QA remains a small fixture, not a full grammar");
        String qorFrequency = discoveryRecord(lines, "hf-qor-af-soomaali-frequency");
        require(qorFrequency.contains("8,971") && qorFrequency.contains("36,840")
                && qorFrequency.contains("v0.2.1") && qorFrequency.contains("v0.4.0")
                && qorFrequency.contains("CC BY-SA 4.0"),
                "Somali frequency counts, stale corpus revision and share-alike terms stay explicit");
        String kabLex = discoveryRecord(lines, "hf-kablex-kabyle-lexicon");
        require(kabLex.contains("366,892") && kabLex.contains("17,090")
                && kabLex.contains("per-entry licence") && kabLex.contains("94% of entries"),
                "Kabyle lexicon scale, per-source rights and verb skew stay explicit");
        String kabInflect = discoveryRecord(lines, "hf-kabinflect-kabyle-morphology");
        require(kabInflect.contains("1,323") && kabInflect.contains("33,065")
                && kabInflect.contains("0% lemma overlap") && kabInflect.contains("emphatic ṛ"),
                "Kabyle inflection split and orthography limitation stay explicit");
        String hmar = discoveryRecord(lines, "hf-hmar-heritage-wordlist");
        require(hmar.contains("43,509") && hmar.contains("dictionary file, id")
                && hmar.contains("contact information") && hmar.contains("historical print"),
                "Hmar entry identity, gated access and source-rights caveat stay explicit");
        String kenpos = discoveryRecord(lines, "hf-kencorpus-kenpos");
        require(kenpos.contains("Dholuo") && kenpos.contains("Bukusu")
                && kenpos.contains("Marachi") && kenpos.contains("Lulogooli")
                && kenpos.contains("sentence_id") && kenpos.contains("CC BY 4.0"),
                "KenPOS language coverage, occurrence coordinates and license stay explicit");
        String galahad = discoveryRecord(lines, "hf-galahad-dutch-frequency");
        require(galahad.contains("14,137") && galahad.contains("390,534")
                && galahad.contains("share contact information")
                && galahad.contains("no redistribution license"),
                "Galahad frequency scale, access gate and unresolved rights stay explicit");
        String sango = discoveryRecord(lines, "hf-sango-vocabulary-grammar");
        require(sango.contains("590 vocabulary entries") && sango.contains("13 structured grammar rules")
                && sango.contains("316 entries") && sango.contains("under review"),
                "Sango grammar scope and row-level provenance/license disputes stay explicit");
        String somaliDictionary = discoveryRecord(lines, "hf-haajidheere-somali-dictionary");
        require(somaliDictionary.contains("239,096") && somaliDictionary.contains("single text field")
                && somaliDictionary.contains("third-party-origin licensing")
                && somaliDictionary.contains("ambiguous terms"),
                "Somali dictionary scale, unstructured shape and rights/identity blockers stay explicit");
        String nltkLists = discoveryRecord(lines, "hf-nltk-word-lists");
        require(nltkLists.contains("235,886") && nltkLists.contains("2,809")
                && nltkLists.contains("configuration-specific")
                && nltkLists.contains("CC BY-SA 4.0"),
                "NLTK word-list configurations retain their separate frequency and license semantics");
        String swadesh = discoveryRecord(lines, "hf-nltk-swadesh-24");
        require(swadesh.contains("24 languages") && swadesh.contains("207 Swadesh concept indices")
                && swadesh.contains("Church Slavonic has 174 rows")
                && swadesh.contains("CC BY-SA 3.0"),
                "Swadesh concept/language coverage and share-alike provenance stay explicit");
        String cldr = discoveryRecord(lines, "unicode-cldr-48-2");
        require(cldr.contains("48.2") && cldr.contains("common/transforms")
                && cldr.contains("not reversible"),
                "CLDR transform version, source path and non-reversibility caveat remain explicit");
        String idsRecord = lines.stream().skip(1)
                .filter(line -> "ids-intercontinental-dictionary-series".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(idsRecord.contains("437,902") && idsRecord.contains("352,262")
                && idsRecord.contains("CC BY 4.0"),
                "IDS count, missing-source provenance and data license remain explicit");
        String apertiumRecord = lines.stream().skip(1)
                .filter(line -> "apertium-language-data-packs".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(apertiumRecord.contains("per exact language/pair repository")
                && apertiumRecord.contains("GPL-3.0"),
                "Apertium package-specific rights remain explicit");
        String dplaceRecord = lines.stream().skip(1)
                .filter(line -> "dplace-language-culture-environment".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(dplaceRecord.contains("CC BY-NC 4.0")
                && dplaceRecord.contains("CC-BY-4.0")
                && dplaceRecord.contains("Licensing conflict"),
                "D-PLACE release-license discrepancy remains visible");
        String parsemeRecord = lines.stream().skip(1)
                .filter(line -> "parseme-multilingual-mwe-corpus-v2".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(parsemeRecord.contains("No blanket license verified")
                && parsemeRecord.contains("preserve token spans"),
                "PARSEME corpus rights and span identity stay explicit");
        String paranames = lines.stream().skip(1)
                .filter(line -> "hf-paranames-1-0".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(paranames.contains("MIT") && paranames.contains("CC BY 4.0")
                && paranames.contains("Conflicting license evidence"),
                "ParaNames license discrepancy stays visible and non-admitted");
        String giella = lines.stream().skip(1)
                .filter(line -> "giellalt-language-repositories".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(giella.contains("per repository") && giella.contains("GPL-3.0"),
                "GiellaLT language-package license scope remains explicit");
        String sigmorphon = lines.stream().skip(1)
                .filter(line -> "sigmorphon-2022-segmentation".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(sigmorphon.contains("CC BY-SA 3.0") && sigmorphon.contains("Wiktionary"),
                "SIGMORPHON task attribution and share-alike remain explicit");
        String panlex = lines.stream().skip(1).filter(line -> "panlex".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(panlex.contains("CC BY-NC-SA 4.0") && panlex.contains("CC0")
                && panlex.contains("clarification"),
                "conflicting PanLex license statements remain unresolved and conservative");
        String indicFrequency = lines.stream().skip(1)
                .filter(line -> "ai4bharat-indic-frequency-lists".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(indicFrequency.contains("CC BY-NC 4.0")
                && indicFrequency.contains("CC BY-NC-SA 4.0")
                && indicFrequency.contains("while"),
                "Indic frequency license mismatch remains visible and non-admitted");
        String morphynet = lines.stream().skip(1)
                .filter(line -> "morphynet-multilingual-morphology".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(morphynet.contains("CC BY-SA 3.0") && morphynet.contains("Wiktionary"),
                "MorphyNet attribution and share-alike terms remain attached");
        String hunspell = lines.stream().skip(1)
                .filter(line -> "hunspell-language-dictionaries".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(hunspell.contains("per-dictionary licenses"),
                "Hunspell engine rights are not mistaken for dictionary payload rights");
        String babelnet = lines.stream().skip(1)
                .filter(line -> "babelnet-restricted-lexical-graph".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(babelnet.contains("non-commercial") && babelnet.contains("research-institution"),
                "BabelNet restrictions remain explicit");
        String langdex = lines.stream().skip(1)
                .filter(line -> "hf-langdex".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(langdex.contains("preserve donor IDs and reject ambiguous rows")
                && langdex.contains("occurrence-coordinate"),
                "LangDex keeps native IDs and rejects ambiguous row identity");
        String dannet = discoveryRecord(lines, "dannet-danish-wordnet-2026-08-21");
        require(dannet.contains("69,851 concepts")
                && dannet.contains("61,994 lexical entries")
                && dannet.contains("77,284 lexicalizations")
                && dannet.contains("CC BY-SA 4.0")
                && dannet.contains("CILI")
                && dannet.contains("REFERENCE_ONLY"),
                "DanNet release, license, concept links and reference-only state stay explicit");
        String austlang = discoveryRecord(lines, "aiatsis-austlang-live-2026");
        require(austlang.contains("CC BY 4.0") && austlang.contains("alpha-numeric")
                && austlang.contains("approximate"),
                "Austlang stable variety IDs and approximate locations remain qualified");
        String afwn = discoveryRecord(lines, "afwn-2022-release-1-0");
        require(afwn.contains("nine South African languages") && afwn.contains("CC BY 4.0")
                && afwn.contains("synset"),
                "African Wordnet release coverage, rights and sense identity remain explicit");
        String multilingualFrequency = discoveryRecord(lines, "hf-multilingual-frequency-lists");
        require(multilingualFrequency.contains("five language configurations")
                && multilingualFrequency.contains("AGPL-3.0")
                && multilingualFrequency.contains("scraped Netflix subtitles"),
                "multilingual lemma frequency source and subtitle-rights caveat stay explicit");
        String mlnormBundle = discoveryRecord(lines, "hf-mlnorm-resources-composite");
        require(mlnormBundle.contains("17-variety") && mlnormBundle.contains("component-level")
                && mlnormBundle.contains("not downloaded"),
                "mlnorm bundle retains component-specific licensing and no-download boundary");
        String multiLexNorm = discoveryRecord(lines, "hf-multilexnorm2026-full-pub");
        require(multiLexNorm.contains("share contact information") && multiLexNorm.contains("README is empty")
                && multiLexNorm.contains("No license"),
                "gated MultiLexNorm remains inaccessible and unlicensed pending review");
        String nanai = discoveryRecord(lines, "hf-nanai-russian-dictionary");
        require(nanai.contains("2,494 unique term IDs") && nanai.contains("887 Nanai translations")
                && nanai.contains("No source-dictionary"),
                "Nanai dictionary preserves source identity, missing pairs and rights uncertainty");
        String haitian = discoveryRecord(lines, "hf-haitian-creole-word-freq");
        require(haitian.contains("17.9k rows") && haitian.contains("could not be confirmed")
                && haitian.contains("strictly reference-only"),
                "Haitian frequency lead remains unverified and reference-only");
        String crubadan = discoveryRecord(lines, "crubadan-hf-character-trigrams-449");
        require(crubadan.contains("449 writing systems") && crubadan.contains("GPL-3.0")
                && crubadan.contains("ISO 639-3") && crubadan.contains("boundary markers"),
                "Crúbadán codes, character n-gram semantics and license remain explicit");
        String elg = discoveryRecord(lines, "elg-catalogue-release-3-0-2026");
        require(elg.contains("October 2026") && elg.contains("grammars")
                && elg.contains("record-level rights"),
                "ELG remains a discovery catalogue with per-resource rights");
        String oanc = discoveryRecord(lines, "oanc-open-frequency-derivation");
        require(oanc.contains("15-million-word") && oanc.contains("unrestricted")
                && oanc.contains("ANC Second Release frequency exports"),
                "OANC-derived frequencies stay distinct from ANC2 exports");
        String elexis = discoveryRecord(lines, "elexis-dictionary-rest-discovery");
        require(elexis.contains("Public Domain") && elexis.contains("X-API-KEY")
                && elexis.contains("separate access"),
                "ELEXIS protocol rights stay distinct from dictionary rights");
        String worldLanguages = lines.stream().skip(1)
                .filter(line -> "hf-world-languages-7130".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(worldLanguages.contains("7,130") && worldLanguages.contains("Glottocode")
                && worldLanguages.contains("ISO 639-3") && worldLanguages.contains("ambiguous mappings"),
                "world-language metadata preserves language-variety identity boundaries");
        String wordfreq = lines.stream().skip(1)
                .filter(line -> "wordfreq-3-0-2-data".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(wordfreq.contains("CC BY-SA 4.0") && wordfreq.contains("Apache-2.0")
                && wordfreq.contains("2021") && wordfreq.contains("bare CSV"),
                "wordfreq data/code rights, vintage and attribution constraints remain explicit");
        String reflex = lines.stream().skip(1)
                .filter(line -> "reflex-africa-v1-0".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(reflex.contains("927,845") && reflex.contains("815")
                && reflex.contains("CC BY-NC-SA 4.0"),
                "RefLex scale, languoid scope and noncommercial share-alike rights remain explicit");
        String opus = lines.stream().skip(1)
                .filter(line -> "opus-corpus-index-2026".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(opus.contains("1,214") && opus.contains("1,038")
                && opus.contains("blanket license"),
                "OPUS scale and corpus-specific rights remain explicit");
        String fleurs = lines.stream().skip(1)
                .filter(line -> "google-fleurs-102".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(fleurs.contains("102-language") && fleurs.contains("CC BY 4.0")
                && fleurs.contains("FLoRes"),
                "FLEURS language coverage, license and upstream text provenance remain explicit");
        String waxal = lines.stream().skip(1)
                .filter(line -> "google-waxalnlp".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(waxal.contains("CC BY-SA 4.0") && waxal.contains("CC BY 4.0")
                && waxal.contains("unresolved"),
                "WAXAL license metadata conflict remains unresolved and non-admitted");
        String dictionaria = lines.stream().skip(1).filter(line -> "dictionaria-cldf-dictionaries".equals(splitTsv(line)[0])).findFirst().orElse("");
        require(dictionaria.contains("CLDF") && dictionaria.contains("CC BY 4.0")
                && dictionaria.contains("dictionary-scoped"), "Dictionaria local IDs and rights scope remain explicit");
        String nchlt = lines.stream().skip(1).filter(line -> "nchlt-south-african-resource-bundles".equals(splitTsv(line)[0])).findFirst().orElse("");
        require(nchlt.contains("CC BY 2.5 South Africa") && nchlt.contains("frequency list")
                && nchlt.contains("20.500.12185/321"), "NCHLT artifact license and source handle remain explicit");
        String sails = lines.stream().skip(1).filter(line -> "sails-south-american-grammar".equals(splitTsv(line)[0])).findFirst().orElse("");
        require(sails.contains("CC BY-NC-ND 2.0 Germany") && sails.contains("non-uniform")
                && sails.contains("REFERENCE_ONLY"), "SAILS domain and restrictive terms remain visible");
        String word2word = lines.stream().skip(1).filter(line -> "word2word-bilingual-lexicons".equals(splitTsv(line)[0])).findFirst().orElse("");
        require(word2word.contains("3,564") && word2word.contains("62 languages")
                && word2word.contains("no separate license"), "word2word coverage and unresolved payload rights remain explicit");
        String wordhoard = lines.stream().skip(1).filter(line -> "wordhoard-frequency-morphology".equals(splitTsv(line)[0])).findFirst().orElse("");
        require(wordhoard.contains("CC BY-SA 4.0") && wordhoard.contains("MIT")
                && wordhoard.contains("five implemented languages"), "WordHoard code/data rights and scope remain explicit");
        String muse = lines.stream().skip(1).filter(line -> "muse-bilingual-dictionary-benchmarks".equals(splitTsv(line)[0])).findFirst().orElse("");
        require(muse.contains("110 directed") && muse.contains("CC BY-NC 4.0")
                && muse.contains("archived read-only"), "MUSE evaluation scope and noncommercial limit remain explicit");
        String chan = lines.stream().skip(1).filter(line -> "numeralbank-chan-numerals-2019".equals(splitTsv(line)[0])).findFirst().orElse("");
        require(chan.contains("5,332 varieties") && chan.contains("100 concepts")
                && chan.contains("0-10,000"), "Chan numeral coverage is not overstated as a full number generator");
        String cosinus = lines.stream().skip(1).filter(line -> "numeralbank-cosinus-v2-2026".equals(splitTsv(line)[0])).findFirst().orElse("");
        require(cosinus.contains("NumberValue") && cosinus.contains("CC BY 4.0")
                && cosinus.contains("under review"), "CoSiNuS identifiers, rights and research status stay explicit");
        String aksharantar = lines.stream().skip(1).filter(line -> "ai4bharat-aksharantar-26m".equals(splitTsv(line)[0])).findFirst().orElse("");
        require(aksharantar.contains("manual") && aksharantar.contains("mined")
                && aksharantar.contains("CC0"), "Aksharantar source-specific mixed licensing remains visible");
        String samanantar = lines.stream().skip(1).filter(line -> "ai4bharat-samanantar-v0-3".equals(splitTsv(line)[0])).findFirst().orElse("");
        require(samanantar.contains("49.6M") && samanantar.contains("source files are grouped as existing or created")
                && samanantar.contains("does not establish one uniform license"), "Samanantar alignment and rights are source-scoped");
        String indicCorpV2 = lines.stream().skip(1).filter(line -> "ai4bharat-indiccorp-v2".equals(splitTsv(line)[0])).findFirst().orElse("");
        require(indicCorpV2.contains("20.9B tokens") && indicCorpV2.contains("24 languages")
                && indicCorpV2.contains("source text"), "IndicCorpV2 is a versioned corpus, not a lexical-ID source");
        String naamapadam = lines.stream().skip(1).filter(line -> "ai4bharat-naamapadam-11".equals(splitTsv(line)[0])).findFirst().orElse("");
        require(naamapadam.contains("11 Indic languages") && naamapadam.contains("CC0")
                && naamapadam.contains("not owned"), "Naamapadam packaging license does not clear source text");
        String textLid = lines.stream().skip(1).filter(line -> "mozilla-common-voice-text-lid-300".equals(splitTsv(line)[0])).findFirst().orElse("");
        require(textLid.contains("19M") && textLid.contains("300 languages")
                && textLid.contains("sentence plus language"), "Common Voice LID label and dedupe scope remain explicit");
        String dbnary = lines.stream().skip(1).filter(line -> "dbnary-ontolex-wiktionary-26".equals(splitTsv(line)[0])).findFirst().orElse("");
        require(dbnary.contains("OntoLex RDF") && dbnary.contains("about 26 Wiktionary language editions")
                && dbnary.contains("CC BY-SA 3.0"), "DBnary data and extractor licenses remain separated");
        String masakha = lines.stream().skip(1).filter(line -> "masakhaner2-african-20".equals(splitTsv(line)[0])).findFirst().orElse("");
        require(masakha.contains("20 African languages") && masakha.contains("AFL 3.0")
                && masakha.contains("source-text rights"), "MasakhaNER artifact terms and source rights remain explicit");
        String wikipron = lines.stream().skip(1)
                .filter(line -> "wikipron-pronunciation-lexicons".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(wikipron.contains("v2.2.0")
                && wikipron.contains("d282e848a211ea31cfd730f0ced8bc8cdab9e83d")
                && wikipron.contains("5 million") && wikipron.contains("348 languages"),
                "WikiPron source release, scale and language coverage stay pinned");
        require(wikipron.contains("Apache-2.0")
                && wikipron.contains("separate licensing terms")
                && wikipron.contains("do not treat software license as payload license"),
                "WikiPron software and Wiktionary-derived data rights remain distinct");
        String hplt = lines.stream().skip(1)
                .filter(line -> "hplt-monolingual-v3".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(hplt.contains("50 terabytes") && hplt.contains("198 languages")
                && hplt.contains("Common Crawl") && hplt.contains("Internet Archive"),
                "HPLT scale, language coverage and crawl provenance remain explicit");
        require(hplt.contains("CC0") && hplt.toLowerCase(java.util.Locale.ROOT).contains("do not ingest the 50 tb release wholesale")
                && hplt.contains("per-shard quality warnings"),
                "HPLT per-language terms and shard-level safeguards remain explicit");
        String floresPlus = lines.stream().skip(1)
                .filter(line -> "oldi-flores-plus-4-6".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(floresPlus.contains("230 language varieties")
                && floresPlus.contains("997 dev") && floresPlus.contains("1012 devtest"),
                "FLORES+ release coverage and split sizes remain explicit");
        require(floresPlus.contains("CC BY-SA 4.0") && floresPlus.contains("gated")
                && floresPlus.contains("not for training")
                && floresPlus.contains("automatic scraping"),
                "FLORES+ access, evaluation-only use and redistribution terms remain explicit");
        String doreco = discoveryRecord(lines, "doreco-spoken-documentation-2");
        require(doreco.contains("50 languages") && doreco.contains("one million words")
                && doreco.contains("morpheme breaks"),
                "DoReCo spoken-language and morphology coverage remain explicit");
        require(doreco.contains("CC BY 4.0") && doreco.contains("permanent identifier")
                && doreco.contains("audio rights"),
                "DoReCo source citation and separate media rights remain explicit");
        String tico = discoveryRecord(lines, "tico-19-medical-translation");
        require(tico.contains("37 languages") && tico.contains("3,071 sentences")
                && tico.contains("stringID") && tico.contains("translator ID"),
                "TICO-19 benchmark scale and row identity remain explicit");
        require(tico.contains("CC0") && tico.contains("row-level terms")
                && tico.contains("source-text license"),
                "TICO translation license is not generalized to source text");
        String mafand = discoveryRecord(lines, "mafand-mt-african-news");
        require(mafand.contains("CC BY-NC 4.0") && mafand.contains("Luo")
                && mafand.contains("copyright"),
                "MAFAND restrictions and withheld-language caveat remain explicit");
        String opus100 = discoveryRecord(lines, "opus-100-parallel-corpus");
        require(opus100.contains("55 million sentence pairs")
                && opus100.contains("100 languages") && opus100.contains("99 English-centric pairs"),
                "OPUS-100 scope and source composition remain explicit");
        require(opus100.contains("no blanket corpus license")
                && opus100.contains("upstream corpus licenses differ"),
                "OPUS-100 does not receive an inferred aggregate license");
        String afridoc = discoveryRecord(lines, "afridoc-mt-health");
        require(afridoc.contains("334 health") && afridoc.contains("technology portion was removed")
                && afridoc.contains("CC BY-NC-SA 3.0"),
                "AFRIDOC-MT available health scope and restrictions remain explicit");
        String multiconer = discoveryRecord(lines, "multiconer-v2-entities");
        require(multiconer.contains("13 configurations") && multiconer.contains("sample IDs")
                && multiconer.contains("CC BY 4.0") && multiconer.contains("TBD"),
                "MultiCoNER candidate spans retain IDs, declared terms and provenance gap");

        String gatitos = discoveryRecord(lines, "google-smol-gatitos");
        require(gatitos.contains("one-to-many") && gatitos.contains("CC BY 4.0")
                && gatitos.contains("upstream data/source terms"),
                "GATITOS translation pairs retain one-to-many shape and upstream-rights caveat");
        String flex = discoveryRecord(lines, "sprakbanken-flex");
        require(flex.contains("per million") && flex.contains("novels, newspapers")
                && flex.contains("CC BY 4.0"),
                "Flex frequencies preserve corpus domains and attribution");
        String kubord = discoveryRecord(lines, "sprakbanken-kubord-lex");
        require(kubord.contains("39,496,308") && kubord.contains("not necessarily unique lemmas"),
                "Kubord record volume is not misreported as unique lemmas");
        String lexin = discoveryRecord(lines, "sprakbanken-lexin");
        require(lexin.contains("second edition") && lexin.contains("29,111")
                && lexin.contains("CC BY 4.0"),
                "LEXIN edition, coverage size and license remain pinned");
        String parole = discoveryRecord(lines, "sprakbanken-parole-lexplus");
        require(parole.contains("29,621") && parole.contains("JSONL")
                && parole.contains("wordforms from lemmas"),
                "Parole+ morphology keeps its release and form/lemma identity");
        String names = discoveryRecord(lines, "sprakbanken-swedish-names-2023");
        require(names.contains("160,390") && names.contains("not person identity")
                && names.contains("CC BY 4.0"),
                "Swedish name forms are not conflated with people");
        String linguabase = discoveryRecord(lines, "linguabase-english-lexical-graph");
        require(linguabase.contains("13-table") && linguabase.contains("CC0")
                && linguabase.contains("not a canonical universal lexicon"),
                "Linguabase graph table identities and aggregation caveat remain explicit");
        String bashkirFrequency = discoveryRecord(lines, "bashkir-frequency-index");
        require(bashkirFrequency.contains("document-frequency")
                && bashkirFrequency.contains("upstream source-corpus redistribution rights"),
                "Bashkir counts preserve field distinctions and source-rights blocker");
        String bashkirNgrams = discoveryRecord(lines, "bashkir-word-ngram-index");
        require(bashkirNgrams.contains("unigram/bigram/trigram")
                && bashkirNgrams.contains("conflict on version and row/count totals"),
                "Bashkir n-gram release inconsistencies remain a blocker");
        String eurovoc = discoveryRecord(lines, "eurovoc-4-24");
        require(eurovoc.contains("4.24") && eurovoc.contains("24 EU official languages")
                && eurovoc.contains("does not state an asset-level license"),
                "EuroVoc version, language coverage and missing license remain explicit");
        String morfflex = discoveryRecord(lines, "ufal-morfflex-cz-2-1");
        require(morfflex.contains("126,906,921") && morfflex.contains("CC BY-NC-SA 4.0")
                && morfflex.contains("commercial use"),
                "MorfFlex scale and noncommercial restriction remain explicit");
        String dmii = discoveryRecord(lines, "dmii-core-icelandic");
        require(dmii.contains("CC BY-SA 4.0") && dmii.contains("immutable API release")
                && dmii.contains("GUID"),
                "DMII identity, release and share-alike terms remain explicit");
        String unidic = discoveryRecord(lines, "unidic-ja-3-1-0");
        require(unidic.contains("3.1.0") && unidic.contains("GPL-2.0")
                && unidic.contains("component-level"),
                "UniDic package-specific licenses are not collapsed");
        String taigi = discoveryRecord(lines, "taigi-common-use-dictionary");
        require(taigi.contains("14,000") && taigi.contains("CC BY-ND 3.0 Taiwan")
                && taigi.contains("do not create or redistribute derivatives"),
                "Taigi no-derivatives term and orthography scope remain explicit");
        String uzbek = discoveryRecord(lines, "hf-uzbek-lexicon-skeleton");
        require(uzbek.contains("4,892,125") && uzbek.contains("23,160,605")
                && uzbek.contains("4,711,419") && uzbek.contains("CC BY 4.0")
                && uzbek.contains("no lemmas"),
                "Uzbek frequency, skeleton, license and surface-form limitations stay distinct");
        String khmerDemand = discoveryRecord(lines, "hf-khmer-search-frequency");
        require(khmerDemand.contains("9,428") && khmerDemand.contains("sessions")
                && khmerDemand.contains("clients") && khmerDemand.contains("not corpus token counts")
                && khmerDemand.contains("CC BY-SA 4.0"),
                "Khmer lookup demand is not conflated with token frequency");
        String afriDict = discoveryRecord(lines, "hf-afri-dict");
        require(afriDict.contains("67,970") && afriDict.contains("entry_id")
                && afriDict.contains("MIT") && afriDict.contains("do not reconcile"),
                "Afri-Dict keeps donor identity and unresolved count/rights caveats");
        String fineWeb2 = discoveryRecord(lines, "hf-fineweb-2");
        require(fineWeb2.contains("1,868 filtered language-script pairs")
                && fineWeb2.contains("5,018,505,566 documents")
                && fineWeb2.contains("3,339,271,691,958 assigned-word-tokenizer word counts"),
                "FineWeb2 corpus scale and assigned word-count semantics stay explicit");
        require(fineWeb2.contains("ODC-By 1.0")
                && fineWeb2.contains("Common Crawl Terms of Use")
                && fineWeb2.contains("pin immutable dataset commit")
                && fineWeb2.contains("REFERENCE_ONLY"),
                "FineWeb2 rights, immutable source pin, and no-ingestion state stay explicit");
        String morphalou = discoveryRecord(lines, "morphalou-3-1");
        require(morphalou.contains("159,271 lemmas") && morphalou.contains("976,570")
                && morphalou.contains("522,662") && morphalou.contains("LGPL-LR"),
                "Morphalou form/phonetic counts, provenance and data license stay explicit");
        String rlfr = discoveryRecord(lines, "rl-fr-2025-03");
        require(rlfr.contains("March 2025") && rlfr.contains("polysemy")
                && rlfr.contains("CC BY 4.0") && rlfr.contains("not translations"),
                "RL-fr version, typed lexical relations and scope stay explicit");
        String bel = discoveryRecord(lines, "bel-rl-fr-3-1");
        require(bel.contains("31,987 citations") && bel.contains("53,013")
                && bel.contains("underlying text rights")
                && bel.contains("citation-level provenance") && bel.contains("REFERENCE_ONLY"),
                "BEL-RL citation identity and per-source rights remain gated");
        String prolexbase = discoveryRecord(lines, "prolexbase-2-2");
        require(prolexbase.contains("67,000") && prolexbase.contains("65,500")
                && prolexbase.contains("2.0 instead carries CC BY-SA")
                && prolexbase.contains("LGPL-LR"),
                "Prolexbase release-specific identity and licensing stay distinct");
        String unilex = discoveryRecord(lines, "unicode-unilex");
        require(unilex.contains("limited") && unilex.contains("Unicode Terms of Use")
                && unilex.contains("final format is undecided"),
                "Unilex remains sparse, versioned and not a complete vocabulary");
        String polyglotta = discoveryRecord(lines, "polyglotta-africana-cldf-v2-0");
        require(polyglotta.contains("200 language varieties") && polyglotta.contains("57,593")
                && polyglotta.contains("CC BY 4.0") && polyglotta.contains("1854"),
                "Polyglotta retains historical variety/concept identity and citation");
        String lexik = discoveryRecord(lines, "lexik-fr-1-0-2026-10");
        require(lexik.contains("839,262") && lexik.contains("2,580,286")
                && lexik.contains("400,000") && lexik.contains("CC BY-SA 4.0")
                && lexik.contains("unverified"),
                "LeXiK special forms, names, provenance and share-alike remain visible");
        String ealex = discoveryRecord(lines, "ealex-east-african-lexome-2026");
        require(ealex.contains("420-language inventory") && ealex.contains("1,737-item")
                && ealex.contains("in progress") && ealex.contains("REFERENCE_ONLY"),
                "EALex inventory is not misreported as completed lexical coverage");
        String nzonza = discoveryRecord(lines, "nzonza-cialr-44");
        require(nzonza.contains("90,000+") && nzonza.contains("44 languages")
                && nzonza.contains("no dataset-wide license") && nzonza.contains("prohibit scraping"),
                "Nzonza remains a consent- and license-gated discovery lead");
        return lines.size() - 1;
    }

    private static int verifyAlgorithmCatalogue() throws IOException {
        Path catalogue = Path.of("algorithm-catalogue.tsv");
        if (!Files.isRegularFile(catalogue)) {
            catalogue = Path.of("m3/lexicon/hf-intake/algorithm-catalogue.tsv");
        }
        require(Files.isRegularFile(catalogue), "algorithm catalogue exists at Maven invocation root");
        if (!Files.isRegularFile(catalogue)) return 0;
        List<String> lines = Files.readAllLines(catalogue, StandardCharsets.UTF_8);
        require(lines.size() == 79, "78 algorithm patterns plus header");
        List<String> header = List.of("problem_category", "discovery_source", "candidate",
                "decision", "owner_or_reason", "qualification");
        require(!lines.isEmpty() && List.of(splitTsv(lines.getFirst())).equals(header),
                "algorithm catalogue schema");
        java.util.Set<String> categories = new java.util.HashSet<>();
        for (int i = 1; i < lines.size(); i++) {
            String[] fields = splitTsv(lines.get(i));
            require(fields.length == 6, "algorithm catalogue row has six fields: " + i);
            if (fields.length != 6) continue;
            require(java.util.Arrays.stream(fields).allMatch(field -> !field.isBlank()),
                    "algorithm choice has rationale and qualification: " + i);
            require(categories.add(fields[0]), "unique algorithm problem category: " + fields[0]);
        }
        require(categories.equals(java.util.Set.of(
                "sorted_token_dedup",
                "filtered_offsets",
                "sparse_frequency_counts",
                "index_lookup",
                "prefix_dictionary",
                "lexical_search",
                "morphology",
                "hf_provenance",
                "language_resource_discovery",
                "concept_identity_crosswalk",
                "lexical_norm_precompute",
                "colexification_semantics",
                "comparative_basic_vocabulary",
                "loanword_etymology",
                "signed_language_lexicon",
                "dictionary_digitization_benchmark",
                "pronunciation_lexicon",
                "endangered_language_archival_discovery",
                "indic_token_frequency",
                "morphological_relation_graph",
                "affix_dictionary_analysis",
                "pronunciation_variants",
                "wordnet_source_discovery",
                "restricted_semantic_graph",
                "multilingual_entity_names",
                "minority_language_morphology",
                "computational_grammar",
                "morphology_segmentation",
                "language_variety_crosswalk",
                "multilingual_frequency_lookup",
                "african_lexicon_admission",
                "translation_corpus_discovery",
                "speech_text_alignment",
                "low_resource_speech_discovery",
                "cldf_dictionary_lookup",
                "african_lexicon_frequency_names",
                "typological_grammar_feature_lookup",
                "ranked_translation_candidates",
                "frequency_morphology_composition",
                "bilingual_dictionary_regression",
                "numeral_form_and_composition",
                "cross_script_transliteration",
                "parallel_sentence_bridge",
                "corpus_frequency_recompute",
                "named_entity_surface_candidates",
                "text_language_identification",
                "ontolex_dictionary_projection",
                "concept_aligned_comparative_lexicon",
                "finite_state_morphology_and_translation",
                "morphosyntactic_typology_precompute",
                "creole_contact_grammar_features",
                "language_culture_environment_crosswalk",
                "multiword_expression_span_index",
                "multilingual_pronunciation_lexicon",
                "offline_corpus_frequency_and_ngram_precompute",
                "multilingual_translation_regression",
                "spoken_language_phoneme_morphology_alignment",
                "medical_translation_regression",
                "low_resource_african_parallel_corpus",
                "multi_source_parallel_corpus_provenance",
                "document_level_translation_regression",
                "named_entity_surface_candidate_spans",
                "unicode_cldr_transliteration_precompute",
                "orthographic_skeleton_lookup",
                "privacy_thresholded_search_demand",
                "observed_form_normalization",
                "cross_lingual_embedding_retrieval",
                "khmer_segmentation_pos_tagging",
                "georgian_case_alignment_masked_contrast",
                "tibetan_word_segmentation_evaluation",
                "balanced_multilingual_tokenizer_fertility",
                "cefr_level_frequency_precompute",
                "cross_lingual_neighborhood_precompute",
                "child_directed_lexical_signal_precompute",
                "multiword_ngram_frequency_index",
                "regional_parallel_pair_views",
                "african_sud_treebank_precompute",
                "indigenous_language_task_partitioned_regression"
        )), "all 78 algorithm categories match the pinned catalogue");
        for (String category : List.of("spoken_language_phoneme_morphology_alignment",
                "medical_translation_regression", "low_resource_african_parallel_corpus",
                "multi_source_parallel_corpus_provenance", "document_level_translation_regression",
                "named_entity_surface_candidate_spans",
                "unicode_cldr_transliteration_precompute",
                "orthographic_skeleton_lookup",
                "privacy_thresholded_search_demand",
                "observed_form_normalization",
                "cross_lingual_embedding_retrieval",
                "khmer_segmentation_pos_tagging",
                "georgian_case_alignment_masked_contrast",
                "tibetan_word_segmentation_evaluation",
                "balanced_multilingual_tokenizer_fertility",
                "bilingual_dictionary_regression",
                "multilingual_translation_regression",
                "language_resource_discovery",
                "cefr_level_frequency_precompute",
                "cross_lingual_neighborhood_precompute",
                "child_directed_lexical_signal_precompute",
                "multiword_ngram_frequency_index",
                "regional_parallel_pair_views",
                "african_sud_treebank_precompute",
                "indigenous_language_task_partitioned_regression")) {
            String row = lines.stream().skip(1)
                    .filter(line -> category.equals(splitTsv(line)[0]))
                    .findFirst().orElse("");
            require(!row.isEmpty() && "EVALUATE_ONLY".equals(splitTsv(row)[3]),
                    "algorithm candidate remains evaluation-only: " + category);
        }
        String bilingual = lines.stream().skip(1)
                .filter(line -> "bilingual_dictionary_regression".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(splitTsv(bilingual).length == 6
                && splitTsv(bilingual)[1].contains("DusunDictionary.com")
                && splitTsv(bilingual)[1].contains("NINJAL Okinawa")
                && splitTsv(bilingual)[4].contains("lack row IDs"),
                "bilingual candidates retain source-native IDs, direction and type distinctions");
        String tigrePattern = lines.stream().skip(1)
                .filter(line -> "low_resource_african_parallel_corpus".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(splitTsv(tigrePattern).length == 6
                && splitTsv(tigrePattern)[1].contains("BeitTigreAI")
                && splitTsv(tigrePattern)[5].contains("66.1k vs 132,288")
                && splitTsv(tigrePattern)[5].contains("source-specific licenses"),
                "African parallel evaluation records Tigre source/count/license boundaries");
        String endangeredPattern = lines.stream().skip(1)
                .filter(line -> "multilingual_translation_regression".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(splitTsv(endangeredPattern).length == 6
                && splitTsv(endangeredPattern)[1].contains("Multilingual Endangered Languages")
                && splitTsv(endangeredPattern)[4].contains("synthetic records")
                && splitTsv(endangeredPattern)[5].contains("isolate synthetic"),
                "multilingual translation evaluation isolates synthetic text and provenance");
        String nenetsPattern = lines.stream().skip(1)
                .filter(line -> "language_resource_discovery".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(splitTsv(nenetsPattern).length == 6
                && splitTsv(nenetsPattern)[1].contains("shnyakov/nenets")
                && splitTsv(nenetsPattern)[4].contains("viewer currently fails")
                && splitTsv(nenetsPattern)[5].contains("refusal test"),
                "schema-discovery catalogue preserves the observed Nenets fail-closed path");
        String khmerSegmentation = lines.stream().skip(1)
                .filter(line -> "khmer_segmentation_pos_tagging".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        String[] khmerSegmentationFields = splitTsv(khmerSegmentation);
        require(khmerSegmentationFields.length == 6
                && khmerSegmentationFields[2].contains("CRF")
                && khmerSegmentationFields[2].contains("trigram HMM")
                && khmerSegmentationFields[2].contains("RDR")
                && khmerSegmentationFields[5].contains("boundary precision/recall/F1"),
                "Khmer sequence-tagging baselines remain an evaluation plan, not an implementation claim");
        String georgianCase = lines.stream().skip(1)
                .filter(line -> "georgian_case_alignment_masked_contrast".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(splitTsv(georgianCase).length == 6
                && splitTsv(georgianCase)[2].contains("nominative/dative/ergative")
                && splitTsv(georgianCase)[4].contains("not translation links"),
                "Georgian case evaluation remains grammar-scoped and evaluation-only");
        String tibetanSegmentation = lines.stream().skip(1)
                .filter(line -> "tibetan_word_segmentation_evaluation".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(splitTsv(tibetanSegmentation).length == 6
                && splitTsv(tibetanSegmentation)[2].contains("Botok")
                && splitTsv(tibetanSegmentation)[5].contains("tsek U+0F0B"),
                "Tibetan boundary evaluation retains source-script invariants");
        String tokenizerFertility = lines.stream().skip(1)
                .filter(line -> "balanced_multilingual_tokenizer_fertility".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(splitTsv(tokenizerFertility).length == 6
                && "EVALUATE_ONLY".equals(splitTsv(tokenizerFertility)[3])
                && splitTsv(tokenizerFertility)[4].contains("not gold word-boundary accuracy")
                && splitTsv(tokenizerFertility)[5].contains("b04c8d1ceb2f5cd4588862100d08de323dccfbaa")
                && splitTsv(tokenizerFertility)[5].contains("no train/validation leakage"),
                "tokenizer-fertility comparison stays evaluation-only and split-safe");
        String corpusPrecompute = lines.stream().skip(1)
                .filter(line -> "offline_corpus_frequency_and_ngram_precompute".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        String[] corpusFields = splitTsv(corpusPrecompute);
        require(corpusFields.length == 6 && "EVALUATE_ONLY".equals(corpusFields[3])
                && corpusFields[1].contains("FineWeb2 v2.1.1")
                && corpusFields[4].contains("not interchangeable")
                && corpusFields[5].contains("review each source's terms"),
                "FineWeb2 remains an evaluated corpus source, not imported lexical frequency");
        String normalization = lines.stream().skip(1)
                .filter(line -> "observed_form_normalization".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        String[] normalizationFields = splitTsv(normalization);
        require(normalizationFields.length == 6 && "EVALUATE_ONLY".equals(normalizationFields[3])
                && normalizationFields[2].contains("raw-to-normal form")
                && normalizationFields[4].contains("not translation")
                && normalizationFields[5].contains("component licenses"),
                "normalization algorithm evaluation preserves source identity and non-translation semantics");
        return lines.size() - 1;
    }

    private static int verifyLangDexTableAudit() throws IOException {
        List<String> lines = resourceLines(
                "/com/m3/lexicon/hf/langdex-table-admission.tsv");
        require(lines.size() == 12, "11 audited LangDex tables plus header");
        List<String> expectedHeader = List.of("dataset", "revision", "table",
                "reported_rows", "native_id_candidate", "identity_evidence",
                "null_duplicate_audit", "upstream_license", "combined_release_license",
                "admission_state", "blockers", "evidence");
        require(List.of(splitTsv(lines.getFirst())).equals(expectedHeader),
                "LangDex audit table schema");
        java.util.Set<String> tables = new java.util.HashSet<>();
        for (int i = 1; i < lines.size(); i++) {
            String[] fields = splitTsv(lines.get(i));
            require(fields.length == 12, "LangDex audit row has twelve fields: " + i);
            if (fields.length != 12) continue;
            require("Asakiri/langdex".equals(fields[0])
                    && "e4e1d8aacd13eff1f8ba7f3df89bc872ba68f349".equals(fields[1]),
                    "LangDex source revision pinned: " + i);
            require(tables.add(fields[2]), "unique LangDex table name: " + fields[2]);
            require(Long.parseLong(fields[3]) > 0, "positive reported LangDex table count");
            require("NOT_AUDITED".equals(fields[6])
                    && "REFERENCE_ONLY".equals(fields[9]),
                    "unproven identity constraints cannot be admitted: " + fields[2]);
            require(fields[8].contains("CC-BY-SA-3.0")
                    && fields[11].startsWith("https://"),
                    "composite attribution and evidence retained: " + fields[2]);
            require(!fields[4].toLowerCase(java.util.Locale.ROOT).contains("occurrence"),
                    "do not substitute occurrence coordinates for donor IDs: " + fields[2]);
            if ("etymology".equals(fields[2])) {
                require(fields[10].contains("never synthesize occurrence coordinates"),
                        "ambiguous LangDex edges are rejected, not renumbered");
            }
            if ("cognate_sets".equals(fields[2])) {
                require(fields[5].contains("reversible")
                        && fields[10].contains("do not strip prefixes or invent row IDs"),
                        "native cognate ID must round-trip exactly");
            }
            if ("lexeme_concept".equals(fields[2])) {
                require(fields[10].contains("reject ambiguous duplicate pairs"),
                        "ambiguous duplicate donor mappings are rejected");
            }
        }
        return tables.size();
    }

    public static void main(String[] args) throws Exception {
        int discoveredSources = verifyLanguageResourceDiscovery();
        int algorithmPatterns = verifyAlgorithmCatalogue();
        int auditedTables = verifyLangDexTableAudit();
        Path scratch = Files.createTempDirectory("m3-hf-intake-proof-");
        try {
            byte[] frequency = "अदरक\t34\nß\t7\nss\t5\nß\t8\n".getBytes(StandardCharsets.UTF_8);
            Path source = scratch.resolve("frequency.tsv");
            Path emitted = scratch.resolve("out.tsv");
            Path replay = scratch.resolve("replay.tsv");
            Files.write(source, frequency);
            var approval = approved(frequency, M3HfLexiconIntakeRecipe.Kind.FREQUENCY_TSV, 2048, 4);
            var receipt = M3HfLexiconIntakeRecipe.apply(approval, source, emitted);
            require(receipt.bytes() == frequency.length, "raw source bytes");
            require(receipt.sourceLines() == 4 && receipt.exportedRows() == 4, "per-row identities retain duplicates");
            List<String> rows = Files.readAllLines(emitted, StandardCharsets.UTF_8);
            require(rows.size() == 5 && rows.get(0).contains("precompute_payload"), "v2 header");
            require(rows.get(1).contains("अदरक") && rows.get(2).contains("ß"), "Unicode retained");
            require(rows.get(3).contains("\tss\t") && rows.get(4).contains("\tß\t"), "no false normalization or dedupe");
            require(rows.get(1).contains("\"frequency_count\":34"), "typed count precompute");
            require(rows.get(1).contains("\"source_revision\":\"" + REVISION + "\""), "pinned revision");
            require(rows.get(1).contains("\"license\":\"apache-2.0\""), "license provenance retained");
            M3HfLexiconIntakeRecipe.apply(approval, source, replay);
            require(Files.mismatch(emitted, replay) == -1, "recipe deterministic replay");
            byte[] preserved = Files.readAllBytes(emitted);
            fails(() -> M3HfLexiconIntakeRecipe.apply(approval, source, emitted), "no overwrite");
            require(java.util.Arrays.equals(preserved, Files.readAllBytes(emitted)), "output retained");
            var wrongHash = approved("something else".getBytes(StandardCharsets.UTF_8),
                    M3HfLexiconIntakeRecipe.Kind.FREQUENCY_TSV, 2048, 4);
            Path mismatch = scratch.resolve("mismatch.tsv");
            fails(() -> M3HfLexiconIntakeRecipe.apply(wrongHash, source, mismatch), "sha mismatch refused");
            require(!Files.exists(mismatch), "no publication after hash failure");
            fails(() -> M3HfLexiconIntakeRecipe.apply(
                    approved(frequency, M3HfLexiconIntakeRecipe.Kind.FREQUENCY_TSV, 3, 4),
                    source, scratch.resolve("too-large.tsv")), "byte budget");
            fails(() -> M3HfLexiconIntakeRecipe.apply(
                    approved(frequency, M3HfLexiconIntakeRecipe.Kind.FREQUENCY_TSV, 2048, 3),
                    source, scratch.resolve("too-many.tsv")), "row budget");
            Path invalid = scratch.resolve("malformed.txt");
            byte[] badUtf8 = {(byte) 0xc3, (byte) 0x28, (byte) '\n'};
            Files.write(invalid, badUtf8);
            fails(() -> M3HfLexiconIntakeRecipe.apply(
                    approved(badUtf8, M3HfLexiconIntakeRecipe.Kind.WORDS, 64, 3), invalid,
                    scratch.resolve("malformed.tsv")), "strict UTF8");
            byte[] grammar = ("# sent_id = 1\n1\tDogs\tdog\tNOUN\t_\tNumber=Plur\t0\troot\t_\t_\n"
                    + "2-3\tcan't\t_\t_\t_\t_\t_\t_\t_\t_\n"
                    + "2\tcan\tcan\tAUX\t_\t_\t1\taux\t_\t_\n").getBytes(StandardCharsets.UTF_8);
            Files.write(scratch.resolve("grammar.conllu"), grammar);
            M3HfLexiconIntakeRecipe.apply(approved(grammar,
                    M3HfLexiconIntakeRecipe.Kind.GRAMMAR_CONLLU, 8192, 4),
                    scratch.resolve("grammar.conllu"), scratch.resolve("grammar.tsv"));
            List<String> grammarOut = Files.readAllLines(scratch.resolve("grammar.tsv"));
            require(grammarOut.size() == 3, "CoNLL-U comments and ranges skipped");
            require(grammarOut.get(1).contains("\"lemma\":\"dog\"")
                    && grammarOut.get(1).contains("\"upos\":\"NOUN\""), "typed grammar facts");
            fails(() -> new M3HfLexiconIntakeRecipe.Approval("owner/ok", REVISION, "../unsafe",
                    sha(grammar), "apache-2.0", "attribution", URI.create("https://example.org"),
                    "en", M3HfLexiconIntakeRecipe.Kind.WORDS, 0, -1, false, 8192, 4, "APPROVED"),
                    "source path traversal rejected");
            fails(() -> new M3HfLexiconIntakeRecipe.Approval("owner/ok", REVISION, "safe.txt",
                    sha(grammar), "unknown", "attribution", URI.create("https://example.org"),
                    "en", M3HfLexiconIntakeRecipe.Kind.WORDS, 0, -1, false, 8192, 4, "APPROVED"),
                    "unknown license refused");
            fails(() -> new M3HfLexiconIntakeRecipe.Approval("owner/ok", REVISION, "safe.txt",
                    sha(grammar), "apache-2.0", "attribution", URI.create("http://example.org"),
                    "en", M3HfLexiconIntakeRecipe.Kind.WORDS, 0, -1, false, 8192, 4, "APPROVED"),
                    "non-HTTPS evidence refused");
            byte[] withHeader = "word\tfreq\nNamaste\t42\n".getBytes(StandardCharsets.UTF_8);
            Path headerSource = scratch.resolve("header.tsv");
            Files.write(headerSource, withHeader);
            var headerApproval = new M3HfLexiconIntakeRecipe.Approval("owner/reviewed-dataset",
                    REVISION, "language/rows.tsv", sha(withHeader), "apache-2.0",
                    "Operator-reviewed attribution", URI.create("https://example.org/rights/evidence"),
                    "hi", M3HfLexiconIntakeRecipe.Kind.FREQUENCY_TSV, 0, 1, true,
                    1024, 2, "APPROVED");
            Path headerOutput = scratch.resolve("header-output.tsv");
            var headerReceipt = M3HfLexiconIntakeRecipe.apply(headerApproval,
                    headerSource, headerOutput);
            require(headerReceipt.exportedRows() == 1 && headerReceipt.sourceLines() == 2,
                    "source header counted but not projected");
            require(Files.readAllLines(headerOutput).get(1).contains("\"frequency_count\":42"),
                    "typed header source accepted");
            byte[] onlyHeader = "word\tfreq\n".getBytes(StandardCharsets.UTF_8);
            Path emptySource = scratch.resolve("only-header.tsv");
            Files.write(emptySource, onlyHeader);
            var emptyApproval = new M3HfLexiconIntakeRecipe.Approval("owner/reviewed-dataset",
                    REVISION, "language/rows.tsv", sha(onlyHeader), "apache-2.0",
                    "Operator-reviewed attribution", URI.create("https://example.org/rights/evidence"),
                    "hi", M3HfLexiconIntakeRecipe.Kind.FREQUENCY_TSV, 0, 1, true,
                    1024, 1, "APPROVED");
            fails(() -> M3HfLexiconIntakeRecipe.apply(emptyApproval, emptySource,
                    scratch.resolve("empty-out.tsv")), "no empty lexical image admitted");
            require(!Files.exists(scratch.resolve("empty-out.tsv")), "empty image not published");
            require(approval.pinnedUrl().toString().contains(REVISION), "immutable Hugging Face address");
            System.out.println("M3_HF_INTAKE_PROOF_PASS|checks=" + checks + "|types=3|language_sources="
                    + discoveredSources + "|algorithm_patterns=" + algorithmPatterns
                    + "|langdex_tables=" + auditedTables + "|jni_owner=reused");
        } finally {
            try (var files = Files.walk(scratch)) {
                for (var path : files.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }
}