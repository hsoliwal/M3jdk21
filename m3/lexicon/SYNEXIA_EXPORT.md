# Synexia lexicon export into M3JDK

Synexia is the canonical converged donor for language, translation, SI-unit,
acronym, proper-name, frequency, n-gram and number mappings. M3JDK consumes an
explicit export; it does not rename, reallocate or infer a replacement lexicon.

`m3/lexicon/synexia-source-manifest.tsv` is the reviewed source-family map. It
keeps the Synexia path, source identity field, mapping fields, and target
precompute owner visible. It is a catalog, not a license to redistribute the
bulk datasets named by it.

## Export contract

The operator supplies a UTF-8 TSV snapshot. The original ten-column form remains
accepted for compatibility:

```text
source_id  source_path  source_kind  language_tag  record_id  lexeme
mapping_id mapping_name translation_profile precompute_profile
```

For complete owner-level precompute, use the v2 form with one additional final
column:

```text
source_id  source_path  source_kind  language_tag  record_id  lexeme
mapping_id mapping_name translation_profile precompute_profile precompute_payload
```

`precompute_payload` is a bounded JSON object supplied by the Synexia owner. It
is canonicalized (sorted keys, compact separators, ASCII escapes) and retained
opaque in the mapping sidecar, so fields such as corpus/document counts,
frequency rank, stem/lemma/phonetic IDs, POS/morphology masks and relation IDs
survive without being falsely presented as `java.lang.String` facts. Duplicate
keys, non-object values, non-finite numbers and payloads over 1 MiB are
rejected. Legacy input receives the explicit canonical payload `{}`.
The field-level donor mapping is maintained in
`m3/lexicon/synexia-precompute-field-map.tsv`; it covers the `IndexWordFacts`,
`IndexWordSignalEnrichment`, `IndexWordSignalProfile` and `IndexWordSignalFlags`
families without making their values part of `java.lang.String`.

`source_id + record_id` is the immutable source identity. The exporter rejects
duplicates, unknown source families, source-path drift, empty fields and
embedded separators. Each record's `precompute_profile` must exactly match the
reviewed `precompute_target` for its Synexia source family. It preserves mapping
IDs/names and translation profiles verbatim. Physical `image_row` values are only a sorted `M3LEX001` projection;
they are never substituted for a Synexia ID.

Lexeme fields in the TSV sidecars preserve UTF-16 exactly while remaining valid
UTF-8: backslashes, controls and unpaired UTF-16 surrogates use `\\uHHHH`
escapes; valid surrogate pairs are emitted as their Unicode scalar. The image
itself remains the authoritative UTF-16LE payload.

```text
python3 m3/runtime-integration/export-synexia-lexicon.py \
  --source-manifest m3/lexicon/synexia-source-manifest.tsv \
  --records /operator/synexia-snapshot/records.tsv \
  --output /operator/m3jdk-lexicon/export \
  --source-repository https://github.com/hsoliwal/com.synexia \
  --source-commit 3e85c872adf556901a341a9eb1c3b59864918da1
```

The output is:

- `synexia.m3lex` (or `synexia-000000.m3lex`, ...): existing M3LEX001 version 2,
  sorted UTF-16 records and precomputed Java hashes, consumed by the mapped M3
  String runtime. Large exports are deterministic shards capped to the existing
  image record/size boundary;
- `synexia.shards.tsv`: shard order, file, lexical bounds, counts and hashes;
- `synexia.records.tsv`: exact source IDs, names, mappings, language/profile
  coordinates, physical shard/image row and canonical owner payload;
- `synexia.precompute-index.tsv`: every exact Synexia precompute-owner profile,
  source-row and physical-coordinate cardinality, and deterministic coverage
  fingerprint;
- `synexia.precompute.tsv`: bounded UTF-16/code-point/hash/ASCII/Latin-1/
  whitespace facts plus the Synexia precompute profile;
- `synexia.export.json`: source pins, input/output hashes, counts and policy.

`com.m3.text.SharedLexiconCatalog.open(exportDirectory)` validates the shard
manifest and all four sidecars together. It exposes stable
`(shardId,imageRow)` coordinates, one-to-many source mappings and immutable
precompute facts plus the complete profile catalog without joining image
payloads. `textAt` is the explicit single-record materialization boundary;
source IDs and mapping names are not converted into VM-local `String` identities.
`prefix(value, limit)` is the first mapped query surface: it returns ordered
coordinates through shard-local lower bounds and exact UTF-16 prefix checks.

The image is suitable for the existing `-Djdk.mindex.lexicon=/absolute/file`
boundary. The sidecars remain language-layer metadata; they are not fields of
`java.lang.String`, and they do not make Synexia resolver IDs interchangeable
with VM-local M3 IDs.

The repository contains only the schema, source map and synthetic proof. Full
LangDex/Hugging Face/Kaggle/WordNet/Wikipedia or operator-owned dictionary data
must be supplied under its exact revision and license custody. No network
download is performed by the exporter, and no claim is made that a bulk corpus
has been checked into this public JDK repository.

## Proof

```text
python3 -m unittest m3/runtime-integration/tests/test_synexia_export.py
python3 m3/runtime-integration/verify-synexia-lexicon.py /operator/m3jdk-lexicon/export
```

The proof covers all number IDs `0..10000`, multilingual/proper-name and unit
mapping rows, rich owner payloads, legacy-input compatibility, deterministic
replay, M3LEX001 version 2 metadata, precompute sidecars, conflict refusal
before output creation, and source-blind rejection of a post-export mutation.
