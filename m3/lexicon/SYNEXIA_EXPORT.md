# Synexia lexicon export into M3JDK

Synexia is the canonical converged donor for language, translation, SI-unit,
acronym, proper-name, frequency, n-gram and number mappings. M3JDK consumes an
explicit export; it does not rename, reallocate or infer a replacement lexicon.

The contract-only language-pack declaration is
`m3/lexicon/synexia-language-pack-contract.tsv`. It binds the Synexia
LanguagePack/LanguagePackSource/LanguageNgramModel surfaces to
`com.m3.text.M3LanguageNgramPrecompute` while keeping the n-gram maps
operator-supplied and out of this repository's bundled export. It is separate
from the five typed family sidecars: no corpus bytes, frequency files, or
translation data are admitted by this declaration.

The contract-only proper-name instance-index declaration is
`m3/lexicon/synexia-instance-index-contract.tsv`. It binds Synexia's validated
UTF-8 `InstanceIndex`/`InstanceRecord` surface to
`com.m3.text.M3InstanceIndexPrecompute`, preserving normalized-name lookup,
reverse concept lookup, instance types, metadata, frozen concept coordinates,
and source identity. It admits no proper-name payload or second lexical
interner; title catalogs remain outside this contract until a canonical title
source and typed owner exist.

`m3/lexicon/synexia-source-manifest.tsv` is the reviewed source-family map. It
keeps the Synexia path, source identity field, mapping fields, and target
precompute owner visible. Its final `precompute_fields` column is the v2
requirement map for owner payloads. It is a catalog, not a license to
redistribute the bulk datasets named by it.

The source manifest's legacy eight-column form remains accepted. The v2 form
appends `precompute_fields`, a sorted, unique comma-separated list of required
snake_case keys, or `-` when that source family has no inspected owner-field
contract. The exporter rejects a record before creating output when its
canonical `precompute_payload` omits a required key. The source-blind verifier
rechecks the same requirement map from `synexia.export.json`, so the mapping
cannot silently lose an owner precompute field between export and consumption.

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
`IndexWordSignalEnrichment`, `IndexWordSignalProfile`, `IndexWordSignalFlags`,
and reviewed LangDex primitive profile families without making their values part
of `java.lang.String`. Its
`donor_java_type` column records the inspected scalar or relation-array shape
(`boolean`, `double`, `int`, `long`, `int[]`, or `long[]`) so future validators can reject shape
drift without interpreting the language metadata as String semantics. When
that field map is present beside the source manifest (or is supplied with
`--field-map`), the exporter enforces those shapes and Java `int`/`long`
bounds; the source-blind verifier repeats the same check from the exported
type map, while the export manifest carries the field-map hash for provenance.

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
  --source-commit 3e85c872adf556901a341a9eb1c3b59864918da1 \
  --relations /operator/synexia-snapshot/related.tsv \
  --relation-policy gutenberg-antonyms-v1
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
- `synexia.related.tsv` (when the source manifest admits `related_lexeme`):
  sorted directed source/record/lexeme/related-lexeme rows with the physical
  M3LEX coordinate; multiple target rows for one source record are allowed,
  while duplicate directed pairs are rejected;
- `synexia.related-sources.tsv`: the sorted admitted source-family IDs used by
  the Java reader to reject relation rows from unrelated mapping families;
- `synexia.export.json`: source pins, input/output hashes, counts, policy and,
  when available, the canonical precompute field-type map and its SHA-256.

`com.m3.text.SharedLexiconCatalog.open(exportDirectory)` validates the shard
manifest and all four sidecars together. It exposes stable
`(shardId,imageRow)` coordinates, one-to-many source mappings and immutable
precompute facts plus the complete profile catalog without joining image
payloads. `textAt` is the explicit single-record materialization boundary;
source IDs and mapping names are not converted into VM-local `String` identities.
`prefix(value, limit)` is the first mapped query surface: it returns ordered
coordinates through shard-local lower bounds and exact UTF-16 prefix checks.
`findMapping(sourceId, recordId)` resolves the preserved Synexia identity without
requiring a physical coordinate, while `findMappings(text)` returns all
translation/dictionary/unit/number mappings attached to one exact lexeme.

When the source manifest declares `related_lexeme`, the operator must
provide a separate `--relations` TSV. The exporter rejects missing, duplicate,
unsorted, non-matching or incomplete directed relation rows. The source-blind
verifier and `SharedRelatedLexemeCatalog` repeat source-family admission,
identity, direction, UTF-16, coordinate and order checks. Relation data remains outside `java.lang.String`
identity and is never reversed or synthesized.

When the source manifest declares `related_lexeme`, the operator must provide
an explicit normalized directed-relation snapshot with a pinned `--relation-policy`
label. This is important for grouped Gutenberg-style antonym/thesaurus sources:
the exporter does not parse or guess raw prose, and it does not synthesize reverse
relations. It rejects missing, duplicate, unsorted, non-matching or incomplete
relation rows. Multiple related targets may share one source record; the exact
(source_id, record_id, related_lexeme) pair remains unique. The source-blind
verifier and `SharedRelatedLexemeCatalog` repeat identity, direction, UTF-16,
coordinate and order checks. Relation data remains outside `java.lang.String`
identity.

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
mapping rows, directed antonym/thesaurus relation rows, rich owner payloads,
  the reviewed source-manifest-to-field-map
lineage, legacy-input compatibility, deterministic replay, M3LEX001 version 2
metadata, precompute sidecars, conflict refusal before output creation, and
source-blind rejection of a post-export mutation.
