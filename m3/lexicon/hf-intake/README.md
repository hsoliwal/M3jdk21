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
release-aware catalogue of 91 **reference-only** candidates. In addition to
dictionaries, grammar, translation, frequency, names, units and speech sources,
it covers metadata discovery (CLARIN VLO, OLAC, ELRA), cross-linguistic concept
IDs and norms (Concepticon, NoRaRe), basic wordlists (ASJP), colexification and
loanword signals (CLICS⁴, WOLD), signed-language features (ASL-LEX), dictionary
digitization evaluation (MUDIDI), pronunciation dictionaries (OpenSLR), and
endangered-language archives and speech leads (DOBES, ELAR, ELARSpeech), Indic frequency lists, MorphyNet, Hunspell language dictionaries, CMUdict, the Global WordNet directory and the restricted BabelNet lead.

Additional candidates now include ParaNames for multilingual entity labels, GiellaLT language-specific finite-state lexicons and grammar, the GF Resource Grammar Library, and SIGMORPHON morphology fixtures. ParaNames' Hugging Face card and paper report conflicting license labels, so it remains explicitly blocked pending clarification. Every catalogue entry remains `REFERENCE_ONLY`: no corpus payload has been downloaded or admitted, and no catalogued license is treated as a blanket redistribution grant. Metadata aggregators point to source records; access, consent, source IDs, licenses, attribution, release hashes and any community conditions must still be checked at the individual resource or deposit level.
The expanded catalogue also records the Hugging Face World Languages metadata package (7,130 reported records; language-identity and upstream-rights checks remain open) and the upstream `wordfreq` 3.0.2 frequency data (CC BY-SA 4.0 data, frozen around 2021; no bare-CSV export). Both remain reference-only. RefLex CLLD adds a source-linked African lexicon but carries CC BY-NC-SA 4.0 terms; OPUS is a corpus-by-corpus discovery index rather than a blanket-licensed corpus. Google FLEURS contributes parallel speech/transcription fixtures under a CC BY 4.0 Hub declaration, with FLoRes provenance to review. WAXAL NLP is a new African speech lead whose Hub card lists both CC BY-SA 4.0 and CC BY 4.0, so its exact-artifact rights remain unresolved.

New reference-only material adds Dictionaria's source-linked CLDF dictionaries, SADiLaR's NCHLT isiZulu corpus/lexicon/frequency/name bundle, and SAILS typological grammar tables. Word2word adds a 3,564-directed-pair translation-candidate reference with unresolved payload rights; WordHoard provides a five-language frequency/morphology schema under CC BY-SA data terms; MUSE contributes noncommercial-only bilingual evaluation dictionaries. These remain unadmitted, and their native dictionary, corpus, pair, and language-variety coordinates stay distinct.

The latest discovery pass adds two numeral-system sources (Chan's 5,332-variety dataset has only 100 concepts, so it does not provide complete 0–10,000 forms; CoSiNuS provides composition/morpheme annotations), Aksharantar's mixed-rights Indic transliteration pairs, Samanantar's source-split parallel corpora, IndicCorpV2's versioned corpus for locally-derived frequency counts, Naamapadam and MasakhaNER named-entity token annotations, Common Voice's 19M-sentence/300+ language text-LID set, and DBnary's OntoLex RDF output. All nine remain reference-only. In particular, transliteration is not semantic translation; sentence-pair alignment is not lexeme equivalence; named-entity surface strings are not entity identity; and CC0 packaging does not necessarily clear upstream text rights.

A further source pass added six reference-only leads: IDS contributes concept-aligned lexical rows across 319 varieties but most records in the published export lack row-level source citations; Apertium provides package-specific finite-state morphology and bilingual transfer data with GPL/data-license review required per package; AUTOTYP and APiCS add separately identified typological and creole-grammar feature tables; D-PLACE joins language, culture and environment data but its download page and repository advertise conflicting license terms; and PARSEME 2.0 offers span-annotated multilingual multiword-expression evaluation data whose exact artifact and per-language licenses still need review. These six are not payload admissions, are represented in the Maven proof and algorithm catalogue, and preserve source-native IDs and evidence.

WikiPron v2.2.0 adds a pinned pronunciation resource: the upstream reports more than five million word/IPA pairs across 348 languages. Its tool is Apache-2.0, but its README says the Wiktionary-derived data has separate terms; the source is therefore a phonetic-index evaluation lead only, not an approved data payload.

Two additional primary-source candidates extend the catalogue: HPLT Monolingual 3.0 reports roughly 50 TB across 198 languages and labels its per-language shards CC0, making it a possible offline frequency/n-gram precompute source only after shard-level provenance and quality review; FLORES+ 4.6 reports 230 language varieties and provides sentence-level translation evaluation, but is gated, CC BY-SA 4.0, not for training, and subject to anti-scraping redistribution conditions. Both remain reference-only; neither is a lexical mapping payload.

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