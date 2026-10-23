<!-- SPDX-License-Identifier: Apache-2.0 -->
# Hugging Face language-data intake — M3Lexicons

This is a **standalone, Maven-invocable, offline DATA transformation recipe**. It
extends, rather than replaces, the Synexia source owner
`synexia-dictlang/shared/bridge-hf/.../DatasetLexiconImport.java` and the
M3JDK21 `dictlang.huggingface` family in `m3/lexicon/synexia-source-manifest.tsv`.
It emits 11-column records consumed by the existing
`m3/runtime-integration/export-synexia-lexicon.py` and its `M3LEX001` image,
`SharedLexiconCatalog`, M3 String facts and existing `M3WordRowsNative` JNI
receiver. It neither downloads a corpus nor creates another String interner,
lexicon coordinate allocation authority, bitmap backend, or JNI ABI.

## Import shape and categories

One approved **snapshot file** at a time, pinned to a 40-hex Hugging Face dataset
commit and raw SHA-256. The current tested formats are UTF-8 newline word lists
(`WORDS`), TSV word/count tables (`FREQUENCY_TSV`), and CoNLL-U morphology,
part-of-speech and dependency tokens (`GRAMMAR_CONLLU`). Optional first-line
headers are supported. No implicit spelling normalization: language-specific
case, canonical equivalence, unknown words and token identity remain with the
source Synexia owner. Repeated source rows remain separate stable record IDs
while the downstream M3LEX image may deduplicate identical UTF-16 values.

The companion `language-resource-discovery.tsv` is a searchable,
release-aware catalogue of 270 **reference-only** candidates.
release-aware catalogue of 239 **reference-only** candidates.
release-aware catalogue of 279 **reference-only** candidates.
An earlier discovery pass added 14 reference-only leads: GATITOS bilingual pairs; Swedish Flex/Kubord frequencies, LEXIN, Parole+ and given-name forms; Linguabase's English lexical graph; Bashkir frequency and n-gram indexes; EuroVoc; Czech MorfFlex; Icelandic DMII; Japanese UniDic; and Taiwan's Taigi dictionary. Every row carries its own version, identity and rights caveats. In particular, noncommercial/share-alike, no-derivatives, component-level licensing, unresolved upstream corpus rights, and release-metadata inconsistencies block payload admission. No data files were downloaded or copied.
 In addition to
dictionaries, grammar, translation, frequency, names, units and speech sources,
it covers metadata discovery (CLARIN VLO, OLAC, ELRA), cross-linguistic concept
IDs and norms (Concepticon, NoRaRe), basic wordlists (ASJP), colexification and
loanword signals (CLICS⁴, WOLD), signed-language features (ASL-LEX), dictionary
digitization evaluation (MUDIDI), pronunciation dictionaries (OpenSLR), and
endangered-language archives and speech leads (DOBES, ELAR, ELARSpeech), Indic frequency lists, MorphyNet, Hunspell language dictionaries, CMUdict, the Global WordNet directory and the restricted BabelNet lead.

Additional candidates now include ParaNames for multilingual entity labels, GiellaLT language-specific finite-state lexicons and grammar, the GF Resource Grammar Library, and SIGMORPHON morphology fixtures. ParaNames' Hugging Face card and paper report conflicting license labels, so it remains explicitly blocked pending clarification. Every catalogue entry remains `REFERENCE_ONLY`: no corpus payload has been downloaded or admitted, and no catalogued license is treated as a blanket redistribution grant. Metadata aggregators point to source records; access, consent, source IDs, licenses, attribution, release hashes and any community conditions must still be checked at the individual resource or deposit level.
The expanded catalogue also records the Hugging Face World Languages metadata package (7,130 reported records; language-identity and upstream-rights checks remain open) and the upstream `wordfreq` 3.0.2 frequency data (CC BY-SA 4.0 data, frozen around 2021; no bare-CSV export). Both remain reference-only. Upstream issue #120 asks whether a filtered/binary-derived frequency list must itself carry ShareAlike; it remains unanswered and grants no additional rights, so derived payloads stay blocked pending rights review. RefLex CLLD adds a source-linked African lexicon but carries CC BY-NC-SA 4.0 terms; OPUS is a corpus-by-corpus discovery index rather than a blanket-licensed corpus. Google FLEURS contributes parallel speech/transcription fixtures under a CC BY 4.0 Hub declaration, with FLoRes provenance to review. WAXAL NLP is a new African speech lead whose Hub card lists both CC BY-SA 4.0 and CC BY 4.0, so its exact-artifact rights remain unresolved.

New reference-only material adds Dictionaria's source-linked CLDF dictionaries, SADiLaR's NCHLT isiZulu corpus/lexicon/frequency/name bundle, and SAILS typological grammar tables. Word2word adds a 3,564-directed-pair translation-candidate reference with unresolved payload rights; WordHoard provides a five-language frequency/morphology schema under CC BY-SA data terms; MUSE contributes noncommercial-only bilingual evaluation dictionaries. These remain unadmitted, and their native dictionary, corpus, pair, and language-variety coordinates stay distinct.

The latest discovery pass adds two numeral-system sources (Chan's 5,332-variety dataset has only 100 concepts, so it does not provide complete 0–10,000 forms; CoSiNuS provides composition/morpheme annotations), Aksharantar's mixed-rights Indic transliteration pairs, Samanantar's source-split parallel corpora, IndicCorpV2's versioned corpus for locally-derived frequency counts, Naamapadam and MasakhaNER named-entity token annotations, Common Voice's 19M-sentence/300+ language text-LID set, and DBnary's OntoLex RDF output. All nine remain reference-only. In particular, transliteration is not semantic translation; sentence-pair alignment is not lexeme equivalence; named-entity surface strings are not entity identity; and CC0 packaging does not necessarily clear upstream text rights.

A further source pass added six reference-only leads: IDS contributes concept-aligned lexical rows across 319 varieties but most records in the published export lack row-level source citations; Apertium provides package-specific finite-state morphology and bilingual transfer data with GPL/data-license review required per package; AUTOTYP and APiCS add separately identified typological and creole-grammar feature tables; D-PLACE joins language, culture and environment data but its download page and repository advertise conflicting license terms; and PARSEME 2.0 offers span-annotated multilingual multiword-expression evaluation data whose exact artifact and per-language licenses still need review. These six are not payload admissions, are represented in the Maven proof and algorithm catalogue, and preserve source-native IDs and evidence.

WikiPron v2.2.0 adds a pinned pronunciation resource: the upstream reports more than five million word/IPA pairs across 348 languages. Its tool is Apache-2.0, but its README says the Wiktionary-derived data has separate terms; the source is therefore a phonetic-index evaluation lead only, not an approved data payload.

Two additional primary-source candidates extend the catalogue: HPLT Monolingual 3.0 reports roughly 50 TB across 198 languages and labels its per-language shards CC0, making it a possible offline frequency/n-gram precompute source only after shard-level provenance and quality review; FLORES+ 4.6 reports 230 language varieties and provides sentence-level translation evaluation, but is gated, CC BY-SA 4.0, not for training, and subject to anti-scraping redistribution conditions. Both remain reference-only; neither is a lexical mapping payload.

A focused source pass adds six more reference-only resources: DoReCo 2.0 for phoneme-aligned documentary data and a subset with morpheme annotations; TICO-19 for 37-language medical translation regression; MAFAND-MT for low-resource African news bitext; OPUS-100 as a heterogeneous 100-language parallel-corpus index; AFRIDOC-MT's currently available health subset for document-context tests; and MultiCoNER v2 for fine-grained named-entity surface-span evaluation. MAFAND-MT is marked noncommercial and has source-specific restrictions; AFRIDOC-MT's technology subset was removed over permissions; OPUS-100 has no aggregate license grant on its overview; TICO rows preserve source URLs/licenses; and MultiCoNER's data-creation provenance remains incomplete. All six stay `REFERENCE_ONLY`; do not turn benchmark mentions or sentence pairs into canonical lexemes or entity identities.

CLDR 48.2's pinned transform rules are also catalogued as a script-conversion precompute candidate, not as translation: transform direction, locale selection, collisions and reversibility must be tested per rule, and Unicode attribution remains attached.

The catalogue is discovery, not an import adapter: Parquet, Arrow, JSONL,
streaming CSV quotations, proprietary/gated corpora, grammar models and
arbitrary Hub file types are not yet supported by the tested importer.

`approval.tsv` must contain exactly this header and one line of metadata:

```text
dataset revision file sha256 license attribution rights_evidence language kind word_column frequency_column has_header max_bytes max_rows approval
```

These are **tab-separated** columns, not space-separated. `approval` must be
`APPROVED`, after a human review of the dataset's terms and redistribution
requirements; `rights_evidence` is an HTTPS evidence URL, not an automated legal
opinion. Source ID and target owner are taken from the inspected donor mapping,
not caller-invented. No dataset payload or permission claim is checked in.

## Recipe-first usage

```bash
mvn -B -ntp -f m3/lexicon/hf-intake/pom.xml verify
java -cp m3/lexicon/hf-intake/target/classes \
  com.m3.lexicon.hf.M3HfLexiconIntakeRecipe \
  approval.tsv local-reviewed-snapshot.tsv output.records.tsv
```

The recipe streams strict UTF-8 and hashes **raw bytes**, checks source budget,
row budget, schema, numeric frequency and CoNLL-U fields, then publishes a
complete output using create-only hard-link semantics. It cannot overwrite an
existing generation. If a filesystem disallows hard links, publication fails
closed. A rerun to a new filename must yield identical output bytes. The output
preserves source `dataset@revision/file#physicalLine`, sorted-key ASCII JSON
precompute facts, licenses, attribution and reviewed evidence. The existing
M3 exporter owns image sorting, physical offsets, checksum and JNI bridge.

A meaningful implementation refinement must be made in this recipe and
replayed rather than hand-edited in emitted records. Java source refactoring
continues to use Synexia's established Maven/OpenRewrite owners; this fixture
is a **data** recipe, not a substitute source-rewrite recipe. No `java.base`
behavior, installed JMOD, native ABI, or public collection contract is changed.

The latest internet-source pass adds ten distinct leads: MASSIVE for localized intent/slot utterances; MADAR and SAMER for Arabic dialect forms and readability; SIL's Bloom collection and ARBML's Masader as discovery indexes; Vocab Bloom Hub for an AI-generated dictionary/grammar export; MADLAD-400 for language-sharded corpus precompute; Wikidata's 2025 multilingual entity labels; BhasaAnuvaad for Indic speech/text alignment; and IATE for domain terminology. All ten remain reference-only. In particular, SAMER is not distributable, Bloom and Masader are indexes rather than blanket-licensed payloads, IATE export terms must be checked, and generated or web-crawled resources are not treated as canonical language truth. No dataset payload was downloaded.


A follow-on source review adds 16 more reference-only records and strengthens the existing GF grammar row: MAGRAM grammaticalization paths, Ge'ez-family materials, KELLY learner/frequency mappings, low-resource Gamayun kits, AfriSpeech verse alignment, licensed Yoruba dictionaries, FrequencyWords and Lexique frequency/morphology, AGROVOC and GEMET domain terms, Dakshina script romanization, Lexibank/ACD comparative wordlists, Teanu dictionary senses, India's CSTT Shabd technical terms, and CFILT IndoWordnet synset links. These are complementary data types, not interchangeable dictionaries: romanization is not translation; source counts are not language-normalized probabilities; wordlist concepts/cognates are not sense equivalence; and language-specific or contributor-level rights must remain attached. Every new row is `REFERENCE_ONLY`; no corpus was downloaded.

A further source check found four non-duplicate resources: MOT Release 1 supplies versioned multilingual text/token fields for deriving corpus-specific counts (its paper says 44 languages while the current README lists 43, and data CC BY 4.0 is distinct from MIT code); Multi-SimLex is a 12-language, human-rated semantic-similarity benchmark under CC BY-NC-ND 4.0, not a translation dictionary; Tsammalex v0.3 links community-sourced plant/animal names to taxa under CC BY 4.0 with consent and per-item media caveats; and DICOVALENCE adds structured French verb-valency frames, but the Hugging Face card's CC BY-NC 4.0 conflicts with the cited ORTOLANG LGPL-LR metadata. All four remain `REFERENCE_ONLY`; no payload was downloaded. Leipzig Corpora, Universal Dependencies and Grambank were checked and not duplicated because they were already present.


A fresh source-discovery pass adds nine reference-only leads: Morphalou 3.1 for French lemma/inflection/phonetic coverage; RL-fr and BEL-RL-fr for typed lexical relations and citation-linked sense examples; Prolexbase 2.2 for pivoted proper names and inflections; Unicode Unilex for sparse lexical-property files; LeXiK 1.0 for French proper names, units, abbreviations, pronunciation, grapheme-phone alignment and frequency; and the historical Polyglotta Africana CLDF wordlist. East African Lexome is tracked as a developing inventory/source bibliography, not a completed 420-language lexicon. Nzonza CIALR is a community-reviewed discovery lead only: its site says dataset licensing is separate and contributor-specific, and its service terms prohibit scraping. All nine remain `REFERENCE_ONLY`; no payload was downloaded. Existing generic UniMorph, Wiktionary, PanLex, Common Voice, Apertium, OPUS, CLDR and French Lexique entries were checked rather than duplicated.

A follow-on WordNet review records DanNet's 2026-08-21 Danish release: its metadata reports 69,851 concepts, 61,994 lexical entries, 77,284 lexicalizations and 2,138,435 triples, with RDF, CSV and WN-LMF exports. DanNet data are CC BY-SA 4.0 (separate from MIT-licensed code). Its source-native dn:* URIs and CILI links are useful for a Danish sense/concept bridge, but CILI links are not exact translations; the export's inferred facts and COR/DDS/OEWN companion graphs must remain distinguishable. The record is REFERENCE_ONLY pending a pinned artifact/hash and receiver-level identity proof; no payload was downloaded.


The next internet-source pass adds six reference-only candidates: AIATSIS Austlang for stable Australian language-variety codes and alternate names; the 2022 African Wordnet release for nine South African languages; Hugging Face/NLTK Crúbadán character trigrams for 449 writing systems; ELG Release 3 as a European grammar/lexicon/corpus discovery index; OANC as a source for locally derived, document-aware frequency fixtures; and the ELEXIS dictionary API for federated metadata/entry discovery. Their coordinates remain source-specific. In particular, Crúbadán's Hub card declares GPL-3.0, OANC must not be conflated with ANC Second Release frequency exports, ELG/ELEXIS do not license provider payloads, and Austlang/AfWN attribution and identity coordinates must be retained. All six remain `REFERENCE_ONLY`; no payload was downloaded.


The next Hugging Face pass adds five more reference-only records. They cover an AGPL-labelled five-language lemma-frequency release derived from scraped subtitle material; the multi-component mlnorm bundle (normalization dictionaries, benchmark files, Hunspell packages, Leipzig counts and generated definitions) with per-component licenses; its gated MultiLexNorm 2026 upstream, whose files require contact-information sharing and whose card has no README; a Russian–Nanai dictionary conversion with 2,494 source term IDs, 887 missing Nanai forms and no stated data license; and a Haitian Creole frequency-list lead whose card and corpus rights could not be verified. The algorithm catalogue now records observed-form normalization as evaluation-only, distinct from semantic translation. These findings are not payload admissions; no additional files were downloaded.

A further web pass adds three distinct reference-only resources: Georgian nominative/dative/ergative case-alignment contrasts (370 examples), a 100-utterance modern Lhasa Tibetan segmentation evaluation set with 2,634 inter-syllable boundary positions, and BridgeConn's Indian Sign Language gloss/video/pose dictionary. The first two are small grammar/string regression candidates; the sign dictionary is a multimodal lead whose 7.11 GB shards are deliberately not fetched. Their language, sentence, clip and gloss coordinates are not M3 lexeme IDs. Georgian's underlying UD text, Tibetan's NICT-Tib1 transcripts, and signers' media consent/rights require source-level review; all three stay `REFERENCE_ONLY`. A separate VocabRadar 42-language compact frequency list and Kencorpus KenPOS were checked and excluded as duplicates of existing entries.

A further cross-language pass adds four reference-only resources with complementary precompute signals: CEFRLex supplies seven learner-frequency resources across six European languages; CLEARPOND exposes orthographic/phonological neighbors and biphone probabilities for five languages; Manulex adds French child-directed grade frequencies and sublexical/orthographic statistics under CC BY-NC-SA 3.0; and Multi-LEX provides POS-tagged French 2–5-gram frequencies, but the Zenodo record has no license value. They are distinct from general corpus frequency, exact translation and canonical lexeme identity. CLEARPOND/CEFRLex dataset rights and Multi-LEX derivative rights remain unresolved, so all four stay `REFERENCE_ONLY`; no payload was fetched.

## Testing, donor decisions, and limits

`M3HfLexiconIntakeProof` executes determinism, duplicate identity,
UTF-16-sensitive vocabulary, frequency, CoNLL-U, header, byte/row-budget,
malformed UTF-8, path traversal, license status, SHA and no-overwrite proofs.
The CI workflow additionally exercises the existing Python M3LEX exporter,
its verification, and the existing Java/C11/JNI word-row receiver across runtime
modes. The catalogue at `algorithm-catalogue.tsv` records why already-admitted
M3 collections and M3 String/regex owners are reused rather than duplicated.

This module is an **offline intake preprocessor**, not a full Java/JNI JDK
release, automated HF-wide crawl, full grammar runtime or declaration that
third-party datasets are redistributable. There is no production corpus in it.

A prior discovery pass added seven reference-only leads: Sindhi Open Lexicon (223,342 entries with publisher and original entry IDs, but a currently failing Hub schema preview); the historical Javanese/Kawi lexicon (32 sources, CC BY-NC and contact-gated); an Arabic/Russian/English dictionary collection whose Apache card does not establish upstream lexical-file rights and contains no Tajik entries; two distinct Tigre resources (a gated pronunciation lexicon and a VecMap-generated top-1 candidate dictionary seeded by 6,164 translations); and NICT's Japanese-English Kyoto corpus/term list under CC BY-SA 3.0. A Kaggle English frequency listing is also recorded as an unresolved-rights lead. Tigre nearest-neighbor outputs are candidates, not translation truth; all seven remain `REFERENCE_ONLY`, and no payload was downloaded.


The latest discovery pass adds five distinct reference-only resources: African Storybook-derived stopword/frequency files (eight language codes plus a Lugbarati variant, with incompatible file schemas); an Amharic gemination lexicon with nested readings and unresolved ambiguous-count discrepancy; a 44,706-row Khmer dictionary whose card says research-only/noncommercial; the Khmer khPOS segmentation/POS corpus under CC BY-NC-SA 4.0; and a Sasak morphology/lexicon bundle whose MIT tag does not settle underlying dictionary/corpus rights. All remain `REFERENCE_ONLY`: source-native identities, per-row rights, or commercial/privacy constraints must be cleared before payload admission. No corpus payload was downloaded.

The algorithm catalogue also adds `khmer_segmentation_pos_tagging` as `EVALUATE_ONLY`: reproduce the source's CRF, trigram-HMM, SVM, maximum-entropy and RDR baselines under the same split, compare boundary F1/POS accuracy/exact sequences, and keep the NC-SA corpus out of any default production payload.


The latest internet pass adds six reference-only resources: Hmar number forms (1..999,999; zero absent), Hmar unigram counts and an experimental archive index; a 1914 English–Hausa dictionary OCR conversion; a six-language African tokenizer challenge corpus; and an English–Nuer–Dinka–Swahili sentence corpus. These enrich numeral, frequency, archive discovery, historical bilingual, Unicode/tokenizer and low-resource sentence-regression coverage without minting lexeme IDs. Contact gates, noncommercial/share-alike terms, unresolved source/derivative rights, unstable row identity and OCR errors are preserved as blockers. Tokenizer segments are not gold word boundaries. All six remain `REFERENCE_ONLY`, all new patterns remain `EVALUATE_ONLY`, and no payload was downloaded.


A further Hugging Face pass adds five distinct reference-only records: a Kadazan Dusun dictionary/corpus with word, phrase and sentence types; NINJAL’s Shuri Okinawan dictionary index with source-native entry and page IDs; a mixed-source Tigre parallel corpus; a five-language endangered-language corpus that explicitly flags synthetic records; and a Nenets dataset whose current viewer fails on heterogeneous CSV schemas. The existing LangDex dictionaries companion was already catalogued and was not duplicated. Counts, licensing and identity caveats remain attached: Dusun rows lack a stable ID column; Tigre reports conflicting viewer/summary totals and per-source licenses; Okinawan is Shuri-specific; synthetic text is segregated from source-described real records; Nenets is a schema-refusal fixture only. All remain `REFERENCE_ONLY`; no payload was fetched.


A focused survey of additional language materials adds ten reference-only leads: a Malayalam word-to-IPA lexicon with frequency-ordered common words and automatically generated pronunciations; NusaX's twelve Indonesian/regional-language sentence columns; two complementary Māori resources (a macron/legal-language evaluation benchmark and Te Ara paired text); AfriSUD's African SUD treebanks; Wolof-French and Breton-French dictionary exports; UQAILAUT Inuktitut root entries; the NRC Nunavut Hansard Inuktitut-English corpus; and AmericasNLI across ten Indigenous language varieties. These cover pronunciation, source frequency, phrase translation, script-sensitive testing, morphology and sentence-level evaluation without turning any sentence alignment or generated IPA into canonical lexeme identity. Rights and identity controls are explicit: NusaX builder code is not executed, its generated pair IDs are not treated as donor IDs, Wolof's MIT Hub label does not clear LLACAN source rights, Apertium's GPL-2.0 obligations remain attached, UQAILAUT's data license is unverified, the Māori benchmark license remains pending community review, Te Ara has noncommercial and item-specific terms, and Nunavut corpus use must preserve NRC notices and political-use restrictions. All ten stay `REFERENCE_ONLY`; no payloads were fetched.

The algorithm catalogue now evaluates three corresponding reusable patterns: lazy regional-language pair views over one aligned row, SUD-aware treebank feature precompute, and task-partitioned Indigenous-language differential tests. These are evaluation candidates, not production defaults; M3 translation mappings remain a separately reviewed receiver.

This pass adds JMdict and JMnedict/ENAMDICT as separate EDRDG Japanese dictionary namespaces, KANJIDIC2 character/readings metadata, and CC-CEDICT's current V2 Chinese Traditional/Simplified/Pinyin dictionary. Their source keys stay intact: EDRDG `ent_seq` is scoped by corpus; KANJIDIC2 uses Unicode code points plus separate JIS mappings; CC-CEDICT's V2 tuple is a version-scoped key, not a persistent ID. Pin the exact JMdict full multilingual versus English-only (`JMdict_e`/`JMdict_b`) artifact and legacy versus NG generation. KANJIDIC2's SKIP field is separately CC BY-NC-SA 4.0 and must be excluded from commercial payloads absent separate permission; other contributed descriptors keep their own provenance. CC-CEDICT V2 Pinyin keys are case-sensitive, so `[Wang2]` and `[wang2]` are distinct and must not be case-folded during deduplication. EDRDG attribution/display/update conditions and CC-CEDICT CC BY-SA 4.0 attribution/share-alike must be reviewed before any payload admission. All four remain `REFERENCE_ONLY`; no payload was fetched. Existing exact-index, bilingual-lookup and source-scoped identity patterns are reused; this source-only pass does not add a new algorithm row.

Language and grammar specifications are first-class typed records in this same M3Lexicon discovery catalogue, not a parallel store and not lexical entries. This slice adds ISO 639:2023 language-code sets, ISO 15924 script codes, CLTS v2.3.0 transcription/feature mappings, and MULTEXT-East v6 morphosyntactic specifications. Their source namespaces, versions, feature systems, rights, and unknown-versus-absent semantics stay explicit. The MULTEXT-East v6 specification page states CC BY-SA 4.0; that specification license does not automatically cover associated lexica or corpora, which remain separate resources. They remain `REFERENCE_ONLY`; no payload is admitted. Any later runtime materialization must use a typed, versioned spec sidecar in the same M3Lexicon hierarchy and add only proven M3 precompute mappings, without overloading lexeme IDs or existing LangDex masks.

## Language-structure and interchange survey (2026-10-21)

This pass adds 36 structure-level/reference records to the discovery catalogue: lexical models and modules (DMLex, OntoLex-Lemon/Lexicog, LMF), feature/category vocabularies (LexInfo, OLiA, FrAC, ISO DCR), dictionary schemas/interchange (TEI P5, TEI Lex-0, LIFT, FreeDict, CLDF, WN-LMF), corpus and morphology structures (UD CoNLL-U v2, UniMorph schema v1, Xigt 1.1.1, HFST, MAF, LAF/GrAF, SynAF/Tiger XML, FoLiA, CHAT), multimodal/interlinear transcription (ELAN EAF, Praat TextGrid, EXMARaLDA, FLExText), and terminology/translation containers (TBX, XLIFF, TMX, MLIF, SemAF). CLDF-Meta remains a discovery catalogue, not a format or payload. The records are distinct from existing UD/UniMorph/CLDF/GiellaLT/GF/Apertium/OLAC data-source entries.

Selection rationale preserves source grain: lexeme/form/sense/concept, annotation category, token/span/time-slot/graph, interlinear morpheme alignment, translation-memory segment, terminology concept/term and resource metadata remain separate. WN-LMF is pinned to schema family 1.4; MAF 2025 supersedes MAF 2012; SynAF Part 1 model and Part 2 Tiger XML serialization are separate; ISO 12620-2:2022 has a draft successor; JLAF is a committee draft; Lexicog is an OntoLex module, not a replacement core model. Standards, implementation code, ontology files, examples and language payloads have independent rights. Every candidate stays `REFERENCE_ONLY`; this scan copies no donor code, standards text, schema or dataset payload.

## Additional GitHub language-structure donors (2026-10-21)

The refreshed inventory adds five source-scoped leads beyond the existing catalogue: Unitex/GramLab's paired DELA dictionaries and editable/compiled grammar graphs; ACoLi's OntoLex-Lemon/TIAD editions with per-dictionary identity and rights; ConceptNet's weighted, source- and license-bearing semantic edges; Panlexia's concept-ID-aligned language wordlists; and Cygnet's CILI-indexed SQLite WordNet view with separate row provenance. Each remains `REFERENCE_ONLY`; none of its code or data payloads is admitted.

The corresponding patterns remain `EVALUATE_ONLY`: graph-plus-dictionary differential checks, OntoLex/TIAD parity with clearance gates, source-weighted semantic candidate ranking, concept-ID projections that do not assert exact translation, and CILI/provenance indexed lookup. This is additive to the nine language-structure candidates and eight precompute patterns already proposed; exact translation and M3 IDs stay with the existing owners.

## Regex precomputation across natural and programming languages (2026-10-21)

The regex pass adds nine `EVALUATE_ONLY` patterns: engine/dialect/flags/Unicode-keyed compiled-plan reuse with per-call matcher state; version-pinned Unicode property pages and reusable input classifications; eligible multi-pattern automaton scans and bounded lazy-DFA states; sound MIndex negative prefilters owned by M3TQ/M3StringSearchPrecompute; segmented `CharSequence` scanning with explicit String/JNI materialization boundaries; source-AST plus dialect-parser inventory by programming language; and target-specific native JIT/database evaluation. This is static work over reusable patterns and Unicode facts—not one universal regex grammar. Regex syntax, Unicode properties, locale tables, flags, capture semantics, source-language escaping, ABI and CPU features remain keyed to their actual engine/compiler/target. Candidate filters may reject impossible matches but never decide a match; exact engine behavior remains authoritative. Native artifacts do not replace the portable Java path. All nine stay `EVALUATE_ONLY`, with fallback on unsupported syntax and unchanged public contracts.

Donor/source review (no third-party source or data payload copied): [OpenJDK JDK 21+35 `Pattern`](https://github.com/openjdk/jdk/blob/jdk-21%2B35/src/java.base/share/classes/java/util/regex/Pattern.java) and its `CharSequence` matcher contract; [RE2/J 1.8 `Pattern`](https://github.com/google/re2j/blob/re2j-1.8/java/com/google/re2j/Pattern.java) and String-input boundary (BSD-style); [Rust `regex`/`regex-automata` 0.4.16](https://github.com/rust-lang/regex/tree/regex-automata-0.4.16/regex-automata/src/hybrid) and `RegexSet` (MIT OR Apache-2.0, with Unicode table licensing tracked separately); [Google RE2 release 2025-11-05](https://github.com/google/re2/tree/2025-11-05/re2) mandatory-prefix analysis (BSD); [PCRE2 10.49](https://github.com/PCRE2Project/pcre2/tree/pcre2-10.49) compile/JIT (BSD-3-Clause with component exception; JIT component has separate terms); [Hyperscan 5.4.2](https://github.com/intel/hyperscan/tree/v5.4.2) multi-pattern databases (BSD); and [Unicode UTS #18 revision 25](https://unicode.org/reports/tr18/tr18-25.html)/UCD property semantics. Any future code port must pin the upstream revision, verify the exact license and notice for the selected files, and add its FOSS reuse decision before implementation. The Maven proof checks the catalogue rows and critical semantic constraints; runtime engine changes remain with the existing M3JDK owner.
The GitHub language-structure audit adds nine distinct reference-only candidates: ICU4X's portable complex-script segmenter/data provider; OpenCC's ordered, phrase-priority script conversion; Sudachi's V1 multi-granular Japanese morphology dictionaries; CAMeL's Arabic specification-to-database pipeline; Turkish Zemberek, Indic NLP resources, Estonian Vabamorf and the Russian pymorphy3/OpenCorpora builder; and CLDR 48.2 likely-subtag/grammatical-feature files. These supplement, rather than duplicate, the existing Apertium and GiellaLT entries. Code licenses are never treated as blanket data licenses: Sudachi, CAMeL, OpenCC and morphology-model payloads keep their component/source terms; every new row remains `REFERENCE_ONLY` and no source payload was downloaded.

The algorithm catalogue now evaluates eight distinct precompute patterns from that audit: ICU4X provider/model selection; OpenCC phrase-priority conversion; Sudachi A/B/C segmentation views; CAMeL specification-to-database indexing; Vabamorf paradigm/compound constraints; pymorphy suffix backoff; CLDR likely-subtag plus explicit `parentLocales` fallback and grammatical-feature selection; and Unicode 17 boundary-property conformance using matching UAX #29 and UAX #14 revisions. All are `EVALUATE_ONLY` with pinned-source, differential, memory/startup/latency or boundary-test gates; none replaces the current M3 runtime owners.
The GitHub language-structure audit adds nine distinct reference-only candidates: ICU4X's portable complex-script segmenter/data provider; OpenCC's ordered, phrase-priority script conversion; Sudachi's V1 multi-granular Japanese morphology dictionaries; CAMeL's Arabic specification-to-database pipeline; Turkish Zemberek, Indic NLP resources, Estonian Vabamorf and the Russian pymorphy3/OpenCorpora builder; and CLDR 48.2 likely-subtag/grammatical-feature files. These supplement, rather than duplicate, the existing Apertium and GiellaLT entries. Code licenses are never treated as blanket data licenses: Sudachi, CAMeL, OpenCC and morphology-model payloads keep their component/source terms; every new row remains `REFERENCE_ONLY` and no source payload was downloaded.
