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
        require(lines.size() == 117, "113 language-resource candidates plus header");
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
                "hf-afri-dict"
        )), "exact 116 language-resource source IDs match the pinned catalogue");
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
        require(lines.size() == 66, "63 algorithm patterns plus header");
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
                "privacy_thresholded_search_demand"
        )), "all 65 algorithm categories match the pinned catalogue");
        for (String category : List.of("spoken_language_phoneme_morphology_alignment",
                "medical_translation_regression", "low_resource_african_parallel_corpus",
                "multi_source_parallel_corpus_provenance", "document_level_translation_regression",
                "named_entity_surface_candidate_spans",
                "unicode_cldr_transliteration_precompute",
                "orthographic_skeleton_lookup",
                "privacy_thresholded_search_demand")) {
            String row = lines.stream().skip(1)
                    .filter(line -> category.equals(splitTsv(line)[0]))
                    .findFirst().orElse("");
            require(!row.isEmpty() && "EVALUATE_ONLY".equals(splitTsv(row)[3]),
                    "new dataset pattern remains evaluation-only: " + category);
        }
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