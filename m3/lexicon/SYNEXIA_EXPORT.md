# Synexia lexicon export into M3JDK

Synexia is the canonical converged donor for language, translation, SI-unit,
acronym, proper-name, frequency, n-gram and number mappings. M3JDK consumes an
explicit export; it does not rename, reallocate or infer a replacement lexicon.

`m3/lexicon/synexia-source-manifest.tsv` is the reviewed source-family map. It
keeps the Synexia path, source identity field, mapping fields, and target
precompute owner visible. It is a catalog, not a license to redistribute the
bulk datasets named by it.

## Export contract

The operator supplies a UTF-8 TSV snapshot with the exact columns below:

```text
source_id  source_path  source_kind  language_tag  record_id  lexeme
mapping_id mapping_name translation_profile precompute_profile
```

`source_id + record_id` is the immutable source identity. The exporter rejects
duplicates, unknown source families, source-path drift, empty fields and
embedded separators. It preserves mapping IDs/names and translation profiles
verbatim. Physical `image_row` values are only a sorted `M3LEX001` projection;
they are never substituted for a Synexia ID.

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
  coordinates and their physical shard/image row;
- `synexia.precompute.tsv`: bounded UTF-16/code-point/hash/ASCII/Latin-1/
  whitespace facts plus the Synexia precompute profile;
- `synexia.export.json`: source pins, input/output hashes, counts and policy.

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
mapping rows, deterministic replay, M3LEX001 version 2 metadata, precompute
sidecars, conflict refusal before output creation, and source-blind rejection
of a post-export mutation.
