<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Lexicon catalog identity, admission and lifetime

[Guide](README.md) · [Text/image foundation](m3-packages.md#textstorage-api-contracts) · [Coverage](coverage.md)

This is a source-level contract review of six public-repository owners under
`m3/core/src/com/m3/text`, pinned to
`c038343521587d96c9fa190715ce2f4b9f8c490a`. It covers their public methods,
constructors and nested records, with actual test assertions distinguished from
source-only observations. All tests, examples, compiler, runtime, native and
performance checks are **NOT_RUN** in this documentation pass.

These are independently scoped receiver/derived-data APIs. They do not introduce
fields into `java.lang.String`, create a second canonical text interner, or
establish target receiving acceptance. The [porting invariant](../../m3/docs/m3jdk21-porting-invariant.md)
and [name map](../../m3/docs/name-mapping.json) remain authoritative. The existing
[text-piece and image reference](m3-packages.md#sharedlexiconimage-file-and-lifetime-contract)
covers the six storage owners; this page adds catalog and derived-owner contracts
without repeating that storage review. Instance-index, number-space, LangDex and
language-grammar siblings remain outside this six-owner closure.

## Choose the correct owner

| Owner | Responsibility | Important boundary |
| --- | --- | --- |
| [SharedLexiconCatalog][CAT] | Base manifest, mapped shard slots, source mappings, fixed text facts, optional sidecars and typed per-record payload lookup | Eager or first-touch image admission; physical coordinates are export-local, not source IDs |
| [SharedLexiconFamilySidecarCatalog][FAM] | Read and verify the optional five-family `m3lex-family-v2` file bundle | Carries source revision/owner scope and versioned keys; does not join them back to the base catalog |
| [SharedLexiconPrecomputeCatalog][PC] | Build an in-memory snapshot of already-admitted typed families, plus dictionary facts/signals | No file parser or automatic conversion from the family sidecar; key schemas differ |
| [SharedRelatedLexemeCatalog][REL] | Directed source-record-to-related-text metadata | A single source record can have multiple targets; targets need not be image records |
| [SynexiaPrecomputePayload][PAY] | Restricted JSON field model, typed access and deterministic serialization | Parse/type validity does not prove source meaning, completeness or authenticity |
| [M3LexiconPrecompute][VAL] | Immutable typed values for translation, spelling, token hashes/counts/frequencies, dictionary facts/signals and SI units | Constructors enforce selected shape constraints, not a derivation from original source text |

### Three identities that must remain separate

1. `SharedLexiconCatalog.Coordinate(shardId, imageRow)` addresses one physical
   record in one export. Both fields are zero-based. Repacking another export can
   change them; a coordinate carries no catalog identity, revision or digest.
2. `SourceIdentity(sourceId, recordId)` preserves one exact logical source record.
   Multiple identities may map to the same lexeme and coordinate. A record ID
   alone is not unique across sources. `findMapping` and payload/SI lookups use
   both fields, never a physical row substituted for `recordId`.
3. Derived-family keys add the particular scope needed by that API: language
   direction, lexicon/value/source fingerprints, source revision/owner fingerprint,
   tokenizer or derivation version, token range or token ID. None is inferred
   from spelling equality. Matching caller-supplied fingerprints is an equality
   check on metadata, not a signature or a recomputation of the underlying facts.

The two nested `Coordinate` types are different Java types. The builder catalog's
`Coordinate` rejects negative fields in its constructor; the base catalog's
record does not. Neither constructor can prove membership in a particular image.
[Source: CAT records][CAT-records], [PC keys][PC-keys].

## SharedLexiconCatalog: startup, first touch and close

### What opening actually validates

Both `open(directory)` and `openLazy(directory)` normalize an absolute local path
and read the manifest plus `synexia.records.tsv`, `synexia.precompute.tsv` and
`synexia.precompute-index.tsv`. A null path throws `NullPointerException`; missing,
unreadable or malformed required files normally fail with checked `IOException`.
The base manifest must have the exact header, contiguous shard IDs starting at
zero, positive record counts, non-inverted bounds and strictly disjoint/increasing
Java UTF-16 lexeme ranges. Shard file names must be simple children, without slash,
backslash, `.` or `..`. This lexical path check is not symlink containment or an
OS security sandbox. [Source][CAT-admission].

Mappings require every field, in-range coordinates and a unique `(sourceId,
recordId)`. Several mappings can share a coordinate. Each retained payload must
parse and be byte-for-character equal to `canonicalJson()`; standalone parser
acceptance alone is insufficient here. Fixed facts require unique coordinates,
nonnegative counts, an unsigned 32-bit Java-hash value represented as `long`, and
literal `True`/`False` boolean fields. Every image coordinate needs mapping and
fixed-fact coverage. Mapping profiles must occur in the fixed-fact profile list
(split on the literal separator ` + `). The profile index must have exactly the
mapped profiles, strictly sorted, with matching source/unique-image cardinalities
and a SHA-256 of `profile + "\t" + sourceCount + "\t" + imageCount + "\n"`.
That descriptor fingerprint does not hash every payload. [Source][CAT-sidecars].

| Opening mode | Image work before return | Failure/retry meaning |
| --- | --- | --- |
| `open(Path)` | Reads each whole-file SHA-256, opens/maps every image, requires M3LEX v2 and matching record count/UTF-16-unit count/first/last text; checks mappings against image text and fixed-fact UTF-16 length/Java hash | Checked I/O failure prevents publication of the catalog. There is no returned partially open catalog or explicit deterministic unmap of images opened before a later failure |
| `openLazy(Path)` | No image mapping or image digest/readability check yet. Manifest/required metadata and both optional sidecar receivers are still processed immediately | Can return even when a shard is missing or corrupt. A later operation needing that shard can fail with `UncheckedIOException`; malformed startup sidecars still fail during opening |
| First touch of a lazy shard | Under that shard's monitor, performs the eager image checks and compares all that shard's mapping lexemes and fixed-fact UTF-16 lengths/Java hashes before assigning the volatile image reference | An `IOException` is wrapped in `UncheckedIOException("unable to open shard ...", cause)`. Failed images are not published or cached; a later caller retries. Already successful shards remain admitted |

The image checks rely on [SharedLexiconImage][IMAGE], which also verifies image
shape, internal checksum, ordering and v2 record hashes. The catalog explicitly
requires v2 although the image class itself also reads v1 and its `create` method
emits v1. Do not use `SharedLexiconImage.create` alone as a catalog-export writer.
The Java catalog does not recompute every exported code-point, surrogate,
whitespace or encoding fact; those are retained data with the exporter/verifier
boundary still outstanding. [Source][CAT-admission], [image contract](m3-packages.md#sharedlexiconimage-file-and-lifetime-contract).

Retry repairs must restore bytes matching the manifest and the metadata already
captured at `openLazy`. Editing sidecars on disk does not reload an existing
catalog. A successful image is not reopened on each call. A new export generation
needs a new catalog; successful admission does not authorize later mutation or
truncation of a mapped file. No whole-export atomic snapshot, signed provenance,
read budget or aggregate catalog-size cap is supplied here. The image's per-file
limits do not bound total metadata, payload-cache or multi-shard memory.

### Public method matrix

`NPE`, `IAE`, `IOOBE` and `ISE` below mean `NullPointerException`,
`IllegalArgumentException`, `IndexOutOfBoundsException` and `IllegalStateException`.
For ordinary guarded operations, `ensureOpen` runs before argument validation, so
a closed catalog normally reports `ISE` even for an invalid argument. Any method
marked “touch” may perform lazy I/O and throw the deferred `UncheckedIOException`.

| Public surface | Result, inputs and effects | Source / assertion evidence |
| --- | --- | --- |
| `open`, `openLazy` | See admission table; required metadata and optional sidecar validation occur before return. Related-sidecar constructor failures can also escape as unchecked IAE, as described below | [CAT opening][CAT-open]; [FoundationTest.catalog][T-foundation], [family admission][T-admission], [payload receiver][T-receiver] |
| `shardCount`, `recordCount`, `shardFiles` | Retained descriptor values; `recordCount` counts physical image records, not source mappings. File list is unmodifiable. No image touch, lock or closed check; these remain readable after close | [CAT accessors][CAT-lookups]; counts/files asserted by [FoundationTest][T-foundation]; post-close behavior source-only |
| `find(String)` | NPE for null; exact Java UTF-16 ordering/equality with no normalization/case folding. Binary-searches manifest bounds, then touches at most one candidate shard and binary-searches its image. Missing text is `Optional.empty`; an out-of-bounds miss can avoid image I/O entirely | [CAT search][CAT-search]; [FoundationTest][T-foundation] |
| `prefix(String, int)` | Null/empty prefix or limit outside `1..100000` is IAE. Returns an unmodifiable, lexically ordered list of physical coordinates, capped at the limit. Visits shards in order, touching each visited shard even when its bounds could exclude the prefix; a damaged unrelated visited shard can therefore fail the call | [CAT search][CAT-search]; [FoundationTest][T-foundation] checks prefix order/empty rejection; full bounds and visitation source-only |
| `textAt(Coordinate)` | NPE for null, IOOBE for shard/row outside manifest dimensions; touch, then allocate exactly the requested image record as a String, preserving raw UTF-16 including unpaired surrogates | [CAT search][CAT-search]; [FoundationTest][T-foundation] |
| `mappingsAt(Coordinate)` | Same null/bounds checks; touches the shard even though mapping metadata is already retained. Returns its unmodifiable source-mapping list in sidecar encounter order; successful admission supplies at least one mapping per valid coordinate | [CAT accessors][CAT-lookups]; [FoundationTest][T-foundation] checks two identities sharing one record |
| `findMapping(sourceId, recordId)` | Both strings non-null; exact source-pair lookup. Absent pair is empty without touching an image; a hit touches its shard before returning the mapping | [CAT accessors][CAT-lookups]; [FoundationTest][T-foundation] |
| `findMappings(String)` | Calls `find` then `mappingsAt`, or returns an empty list on a miss. Those are two separate locked operations: close can intervene between them and cause ISE | [CAT accessors][CAT-lookups]; successful/missing cases in [FoundationTest][T-foundation]; close race source-only |
| `precomputeAt(Coordinate)` | Same null/bounds/touch behavior; returns the retained `PrecomputeFacts` for that coordinate, not newly derived facts | [CAT accessors][CAT-lookups]; [FoundationTest][T-foundation] |
| `precomputeProfiles()` | Guarded unmodifiable descriptor list; no image touch | [CAT accessors][CAT-lookups]; [FoundationTest][T-foundation] |
| `familyPrecompute()` | Guarded optional already-loaded five-family receiver; does not map a shard or assemble an in-memory builder catalog | [CAT sidecar access][CAT-lifecycle]; [family admission][T-admission] checks both opening modes, legacy absence and post-close refusal |
| `relatedLexemes()` | Guarded optional already-loaded related receiver; no image touch. Returned receiver can itself be closed independently | [CAT sidecar access][CAT-lifecycle]; [family admission][T-admission] checks two directed targets |
| `findPrecomputePayload(sourceId, recordId)` | Non-null source pair; absent mapping is empty. A hit touches the shard and caches the parsed immutable payload by source identity with `ConcurrentHashMap.computeIfAbsent`. The admitted canonical JSON had already been parsed during opening | [CAT payload][CAT-payload]; [payload receiver][T-receiver] asserts exact identity reuse, fields and absent pair |
| `findSiUnitPrecompute(sourceId, recordId)` | Non-null pair; only exact source family `dictlang.si-units` qualifies. Missing/wrong-family mapping returns empty without image touch. A hit touches, reuses the payload cache and constructs a new typed SI value from four required fields. Missing/wrong fields or SI constraints can fail at lookup, despite prior JSON admission | [CAT SI][CAT-si]; [SI decoder][T-si] checks family/record isolation, constraints and post-close refusal |
| `warm()` | Guarded loop touching/loading every shard in sequence; no flattening into one payload. Failure can leave earlier shards opened/warmed. `MappedByteBuffer.load` is not a portable residency/latency guarantee | [CAT lifecycle][CAT-lifecycle]; [FoundationTest][T-foundation] calls warm, without asserting OS residency |
| `close()` | Idempotent, takes lifecycle write lock, marks closed, closes the optional related receiver and clears every published image slot. Does not clear mapping/fact/profile/payload metadata or unmap the OS buffers directly | [CAT lifecycle][CAT-lifecycle]; [FoundationTest][T-foundation], [family admission][T-admission], [SI decoder][T-si] check selected closed methods; race/unmap behavior source-only |

### Public records and retained values

| Record, including all generated component accessors | Constructor/equality/meaning | Evidence |
| --- | --- | --- |
| `Coordinate(shardId, imageRow)` | Unvalidated pair; even negative values can be constructed. Catalog lookup performs bounds validation. Structural record equality does not encode export ownership | [CAT records][CAT-records]; coordinate use in [FoundationTest][T-foundation] |
| `SourceIdentity(sourceId, recordId)` | Unvalidated pair; public construction permits null. Lookup entry points reject null arguments. Equality is the pair, not spelling equality | [CAT records][CAT-records]; source separation in [SI decoder][T-si] |
| `SourceMapping(sourceId, sourcePath, sourceKind, languageTag, recordId, lexeme, coordinate, mappingId, mappingName, translationProfile, precomputeProfile, precomputePayload)` | Plain unvalidated record; constructor does not admit it into any catalog. Admitted values retain provenance labels, decoded lexeme and canonical payload text | [CAT records][CAT-records]; [FoundationTest][T-foundation], [payload receiver][T-receiver] |
| `PrecomputeFacts(coordinate, utf16Units, javaHash, codePoints, unpairedSurrogates, nonBmpCodePoints, ascii, latin1, containsWhitespace, precomputeProfile)` | Plain unvalidated record; `javaHash` is retained unsigned-32-bit-in-long data for admitted rows. No constructor recomputation | [CAT records][CAT-records]; [FoundationTest][T-foundation] |
| `PrecomputeProfile(profile, sourceRecords, imageRecords, fingerprint)` | Plain unvalidated descriptor. Admitted cardinalities/fingerprint are checked by the reader, not the public record constructor | [CAT records][CAT-records], [CAT sidecars][CAT-sidecars]; [FoundationTest][T-foundation] |

Generated record `equals`/`hashCode` use components; they are not admission checks.
Returned Strings/records/lists remain ordinary retained Java values after catalog
close. A captured family-sidecar catalog has no close state and remains usable;
a captured related catalog is closed by parent close. Captured related lists and
relation records remain readable even then.

The main catalog's read lock spans individual guarded operations, including slow
first-touch I/O and `warm`; close waits for those operations to release it. The
per-shard monitor prevents simultaneous successful first-touch publication for
the same slot. A failed attempt leaves the slot unset so another caller may try
again. This does not supply lock-free progress, a close deadline, cross-call
transactions or a concurrent-file-writer contract. In particular, the related
receiver has only its own volatile flag, not the parent's read lock.

Mapped image channels close after mapping; reachability and the VM govern buffer
reclamation. Catalog close drops its references, not an explicit unmap operation.
The catalog's `recordText` path does not perform the separate per-record checksum
check used by `SharedLexiconImage.copyRecord`. Therefore “read-only mapping” and
“validated at startup” must not be read as an immutable-file guarantee. Caller
publication must keep admitted files stable. Opening and lazy sidecar validation
also materialize record Strings internally; `textAt` is the explicit public text
projection, not the only allocation of text anywhere in the implementation.

## Five-family file sidecars

[SharedLexiconFamilySidecarCatalog][FAM] reads these fixed files as a complete bundle:

| Family | File | Lookup/key |
| --- | --- | --- |
| Prefix counts | `synexia.prefix-counts.tsv` | `prefixAt(PrefixKey)` |
| Spell index | `synexia.spell.tsv` | `spellAt(SpellKey)`; several delete rows form one index |
| Token frequency | `synexia.token-frequency.tsv` | `frequencyAt(FrequencyKey)` |
| Token hash precompute | `synexia.token-hash.tsv` | `tokenHashAt(TokenHashKey)` |
| Translation projection | `synexia.translation.tsv` | `translationAt(TranslationKey)` |

`SCHEMA_VERSION` is `m3lex-family-v2`; `INDEX_FILE` is
`synexia.precompute-family-index.tsv`. The index must describe all five families
in the implementation's fixed order with their exact names/files, nonnegative
row counts and lowercase 64-hex SHA-256 file digests. Each file is read completely,
checksummed, decoded with strict UTF-8, checked against its exact header/row width
and declared count, and required to be strictly sorted by its serialized key.
CRLF is not canonical for this receiver; its wire format differs from the base
quoted-TSV parser. A final LF is allowed, but extra blank data rows are not.
[Source][FAM-parser].

`openOptional(Path)` checks `Files.exists` on the index and all five expected
files. If none exist it returns empty, even for a nonexistent directory. Any
presence invokes `open`: partial, corrupt or wrongly shaped bundles fail instead
of becoming absence. Direct `open` requires a directory and the complete bundle.
All parsed maps are copied before publication; no partial receiver escapes.
I/O/format failures are checked `IOException`; invalid row constructors are
wrapped in `IOException` by the row loop. Null directory is NPE.
[Source][FAM-open]; [sidecar tests][T-family].

### Key and lookup matrix

All listed records include generated component accessors, structural equality,
hash code and diagnostic string representation. Here “text” means non-null and
nonempty, without tab/LF/CR; failure is IAE. These labels are not parsed as hashes,
language standards or approved version identifiers. Record scope/coordinate
references are non-null (NPE); the base `Coordinate` object itself remains
unvalidated when directly supplied to a key constructor.

| Public surface | Contract | Evidence |
| --- | --- | --- |
| `SourceScope(sourceId, recordId, sourceManifestRevision, ownerFingerprint)` | Four required text labels; every file-family key carries this scope | [FAM keys][FAM-keys]; [sidecar][T-family], [admission][T-admission] use a complete scope |
| `TranslationKey(scope, sourceLanguage, targetLanguage, lexiconFingerprint, sourceFingerprint)` | Scope plus four required text labels. Direction is part of identity | [FAM keys][FAM-keys]; [sidecar][T-family] |
| `SpellKey(scope, lexiconFingerprint, language, maxEditDistance, prefixLength, sourceFingerprint)` | Required labels; edit distance/prefix length must be nonnegative (IAE). Delete strings are rows within this scope, not separate indexes | [FAM keys][FAM-keys]; [sidecar][T-family] |
| `TokenHashKey(scope, coordinate, valueFingerprint, tokenizerVersion, rangeStart, rangeEnd)` | Required labels; `0 <= rangeStart <= rangeEnd`. Range is in tokens, not UTF-16 units | [FAM keys][FAM-keys]; [sidecar][T-family] |
| `PrefixKey(scope, coordinate, derivationVersion, valueFingerprint, tokenId)` | Required labels; nonnegative token ID | [FAM keys][FAM-keys]; [sidecar][T-family] |
| `FrequencyKey(scope, coordinate, derivationVersion, valueFingerprint)` | Required labels; exact key match includes derivation version | [FAM keys][FAM-keys]; [sidecar][T-family] |
| `openOptional`, `open` | Presence/whole-bundle behavior described above; no mapping or image opening | [FAM opening][FAM-open]; [sidecar][T-family], [admission][T-admission] |
| `translationAt`, `spellAt`, `tokenHashAt`, `prefixAt`, `frequencyAt` | NPE for null key, empty Optional for no exact key, otherwise retained immutable typed value. No fallback by text, shortened identity or another version | [FAM lookups][FAM-lookups]; all five successes in [sidecar][T-family] |
| `rowCounts()` | Unmodifiable map of physical row counts per family; spell row count can exceed number of spell indexes. Iteration order is not promised by `Map.copyOf` | [FAM lookups][FAM-lookups]; [sidecar][T-family] asserts two spell rows |

Parsing additionally checks nonnegative physical coordinates/ranges/token IDs,
nonnegative and sorted frequency entries (token-frequency counts fit `int`),
strictly increasing spell candidate IDs, nondecreasing prefix counts
starting at zero, exact token-digest count and 32-byte digests, and a translation
mapped count equal to changed vocabulary positions. Spell rows in one scope must
have distinct delete keys and identical frequency maps. Duplicate logical keys
are refused. `-` represents the supported empty arrays/maps. The file parser
rejects an empty spell delete key even though the standalone `SpellIndex`
constructor accepts `""`. [Source][FAM-parser], [typed values][VAL].

This is bundle consistency, not provenance authentication. The reader does not
load the source manifest revision to prove it matches, recompute derived token
facts, check that a coordinate exists in the base image, or join `SourceScope`
back to `SourceMapping`. `SharedLexiconCatalog` invokes this reader but adds no
such cross-check. Safe use still requires a producer/admission process that
binds these scopes to the intended source generation. There is no close method,
automatic eviction or later on-disk reload.

## In-memory SharedLexiconPrecomputeCatalog

The [builder catalog][PC] is a separate API, not the file receiver renamed. It
accepts already-verified typed values and copies its maps on `build`. It neither
reads JSON nor opens files, and it does not retain all the family-v2 revision and
derivation-version fields automatically. Do not discard those distinctions when
adapting rows to this narrower schema; keep the external generation/admission
context that makes such an adaptation valid.

Here required text means non-null and nonempty (IAE); unlike the family-v2 keys,
these constructors do not reject tabs/newlines. Non-null object requirements
throw NPE. No key constructor establishes a source relationship or file membership.

| Public key record, including generated accessors | Validation and exact identity | Evidence |
| --- | --- | --- |
| `TranslationIdentity(sourceId, recordId, sourceLanguage, targetLanguage, lexiconFingerprint, sourceFingerprint)` | All six required labels participate; source and record variants/direction coexist | [PC keys][PC-keys]; [typed test][T-typed] |
| `Coordinate(shardId, imageRow)` | Nonnegative integers, IAE otherwise; different type from `SharedLexiconCatalog.Coordinate` | [PC keys][PC-keys]; constructor refusal source-only |
| `SpellScope(lexiconFingerprint, language, maxEditDistance, prefixLength, sourceFingerprint)` | Required labels, nonnegative distance/prefix length | [PC keys][PC-keys]; [typed test][T-typed] |
| `TokenRange(coordinate, valueFingerprint, tokenizerVersion, startInclusive, endExclusive)` | Non-null coordinate, required labels, `0 <= start <= end`; token units | [PC keys][PC-keys]; [typed test][T-typed] |
| `ValueToken(coordinate, valueFingerprint, tokenId)` | Non-null coordinate, required fingerprint, nonnegative token ID | [PC keys][PC-keys]; [typed test][T-typed] |
| `ValueScope(coordinate, valueFingerprint)` | Non-null coordinate and required fingerprint | [PC keys][PC-keys]; [typed test][T-typed] |
| `WordIdentity(sourceId, recordId, lexeme, sourceRevision)` | Four required labels; source revision and exact lexeme participate. No physical coordinate | [PC keys][PC-keys]; [typed test][T-typed] |

| Public method/constructor | Result, mutation and refusal | Evidence |
| --- | --- | --- |
| `builder()`, public default `Builder()` | New mutable, unsynchronized empty builder. Empty catalogs are allowed | [PC builder][PC-build]; empty/default-constructor behavior source-only |
| `Builder.translation(identity, value)` | Fluent `this`; NPE for null. Explicitly compares source/target languages and lexicon/source fingerprints with the value; mismatch is IAE. Value has no sourceId/recordId to cross-check | [PC builder][PC-build]; [typed test][T-typed] checks fingerprint mismatches and independent identities |
| `Builder.spell`, `tokenHashes`, `prefixCounts`, `tokenFrequency` | Fluent insertion; only non-null key/value and duplicate-key refusal beyond construction of those objects. These setters do **not** compare the key's scope/range/fingerprint/token ID with the value's fields | [PC builder][PC-build]; successful uses in [typed test][T-typed]; missing cross-checks source-only |
| `Builder.wordFacts(identity, value)` | Requires exact `dictlang.dictionary` source and identity revision equal to value revision, then inserts. Wrong family/revision is IAE; null identity/value is NPE | [PC builder][PC-build]; [typed test][T-typed] checks family, revision and duplicate refusal |
| `Builder.wordSignal(identity, value)` | Requires exact `dictlang.frequency` source and matching revision, then inserts; same null/mismatch rules | [PC builder][PC-build]; [typed test][T-typed] |
| All seven builder insertion methods | `putIfAbsent` refuses a repeated key with IAE even when the value is the same object. No replacement occurs on duplicate failure; earlier successful insertions remain. No transaction across multiple calls | [PC builder][PC-build]; duplicate translation/word-facts checks in [typed test][T-typed]; other duplicates source-only |
| `Builder.build()` | Snapshots all seven maps with `Map.copyOf`; builder remains reusable. Later builder insertions do not change previous catalogs. Does not deep-validate facts or consume/close the builder | [PC builder][PC-build]; snapshot independence source-only |
| `translationAt`, `spellAt`, `tokenHashesAt`, `prefixCountsAt`, `tokenFrequencyAt`, `wordFactsAt`, `wordSignalAt` | Non-null exact key or NPE; absent is empty Optional; present returns retained typed object. No I/O, lazy computation or close state | [PC lookup][PC-lookup]; [typed test][T-typed] checks all families and cross-family miss |

The immutable snapshot is suitable for read sharing after safe publication; the
builder is not a concurrent collector. Caller-owned input arrays/maps must stay
stable during typed-value construction and copying. No exported precompute value
is authenticated merely by being accepted into this builder.

## Related lexemes: direction and multiplicity

[SharedRelatedLexemeCatalog][REL] reads `synexia.records.tsv`,
`synexia.related-sources.tsv` (a nonempty, strictly sorted source-ID whitelist)
and `synexia.related.tsv`. It requires each relation source in that whitelist,
exact source/record membership in the mappings, matching lexeme/coordinate and
strict order/uniqueness by `(sourceId, recordId, lexeme, relatedLexeme)`.
It preserves direction; it does not synthesize a reverse relation, normalize
text, require the target lexeme in an image, or assign String identity.

The standalone reader checks the mapping file, not the shard manifest/image.
Its coordinates are merely nonnegative and equal to that mapping. Use of the
reader through the base catalog still does not touch a lazy image merely to
obtain `relatedLexemes()`.

| Public surface | Contract | Evidence |
| --- | --- | --- |
| `FILE` | `synexia.related.tsv` | [REL source][REL] |
| `Relation(sourceId, recordId, lexeme, relatedLexeme, coordinate)` and all five accessors | NPE for null components; IAE for empty string components. Does not validate coordinate bounds, source admission or target existence | [REL relation][REL-record]; selected admitted values in [family admission][T-admission] |
| `Relation.identity()` | Creates the exact base-catalog `SourceIdentity` pair | [REL relation][REL-record]; source-only direct method contract |
| `openOptional(Path)` | Null is NPE. Checks `Files.isRegularFile` on either relation or source-list file. Neither regular file means empty; either means call `open`. A lone directory at an expected file path does not trigger presence the way family-v2 `Files.exists` does | [REL opening][REL-open]; absent/invalid-path variants source-only |
| `open(Path)` | Checked I/O failures for missing files, headers, row shapes, sorting, duplicate mapping identities, whitelist/mapping mismatches and bad encoded coordinates. Empty relation strings rejected by the record constructor can escape as unchecked IAE: there is no catch/wrap around that constructor | [REL opening][REL-open]; successful multi-target opening via [family admission][T-admission]; refusal matrix source-only |
| `relations()` | Guarded unmodifiable list in admitted order | [REL methods][REL-methods]; source-only |
| `find(sourceId, recordId)` | Non-null strings. Returns a value only when exactly one relation exists. Empty Optional means either zero **or multiple** targets, so it is not an existence predicate | [REL methods][REL-methods]; multiplicity interpretation source-only |
| `findAll(sourceId, recordId)` | Non-null strings; retained unmodifiable list or empty list. Use this to distinguish missing from multi-target identities | [REL methods][REL-methods]; [family admission][T-admission] checks two sorted targets |
| `findBySource(sourceId)` | Non-null source; scans/filter-copies to an unmodifiable list, preserving relation order | [REL methods][REL-methods]; source-only |
| `close()` | Idempotent volatile flag assignment; subsequent guarded calls throw ISE. Does not clear lists/maps. An operation already past its flag check can return after a concurrent close | [REL methods][REL-methods]; concurrency and retained-list behavior source-only |

All lookup methods check the closed flag before ordinary argument work. `find`
checks again through `findAll`. There is no read/write lock here, no close-time
wait for readers and no file mapping to unmap. The sidecar reader has no file
checksum/signature mechanism of its own; do not confuse its structural checks
with the five-family bundle's indexed SHA-256 checks.

## Typed per-record payload

[SynexiaPrecomputePayload][PAY] parses a flat object whose names match
`[a-z][a-z0-9_]*`. Values are booleans, signed-long integers, finite doubles,
Strings or arrays of signed-long integers. Nested values, null, decimal-array
members, duplicate names, trailing content, numeric overflow and malformed
strings are rejected. Known field names additionally enforce the implemented
type map: for example `flags` is int, `memberships` is int-array,
`concept_ids` is long-array, `source_revision` is String, `si_offset` is numeric,
and `lexicon_fingerprint` allows either String or integer. Unknown names remain
readable in their parsed kind; missing known fields are not filled or rejected
until a caller requires them. [Source][PAY-parser]; [payload test][T-payload],
[translation map test][T-translation].

The parser does not insist that the input is already canonical: it accepts
whitespace and noncanonical key order; its number scanner also accepts leading
zero digits. `canonicalJson()` sorts keys, removes spacing, escapes strings and
uses the stored numeric representation (`Double.toString` for decimals). The
base catalog separately requires exact equality to this serialization. “Bounded
value model” means restricted value kinds, not a configured byte/field/array-size
or CPU budget. Parsing and serializing allocate proportionally to input/output;
callers still need an external resource limit for untrusted large payloads.

| Public surface | Contract and failure | Evidence |
| --- | --- | --- |
| `Kind` | Enum values `BOOLEAN`, `INTEGER`, `DECIMAL`, `STRING`, `INTEGER_ARRAY`; ordinary enum `values`/`valueOf` behavior applies | [PAY fields][PAY-field]; parser kinds in [payload test][T-payload] |
| Six-argument `Field(kind, booleanValue, integerValue, decimalValue, integerArrayValue, stringValue)` | Non-null kind; array kind requires/copies an array and other kinds reject a non-null array; String kind requires a String and others reject a non-null String. Non-decimal kinds reject nonzero decimal lanes; non-integer/non-array kinds reject nonzero integer lanes. These checks are not the full parser validator: direct DECIMAL construction does not check finiteness, and irrelevant boolean lanes are not forced false | [PAY fields][PAY-field]; constructor edge cases source-only |
| Five-argument `Field(...)` overload | Delegates with null String, so cannot construct a valid String-kind field | [PAY fields][PAY-field]; source-only |
| `Field.kind`, `booleanValue`, `integerValue`, `decimalValue`, `stringValue` | Generated accessors expose stored lanes; callers must use `kind` to interpret them | [PAY fields][PAY-field]; source-only direct-access contract |
| `Field.integerArrayValue()` | Returns a clone for array kind and null otherwise | [PAY fields][PAY-field]; source-only direct clone assertion |
| `Field.equals`, `hashCode` | Compare all stored lanes, using array-content equality/hash and `Double.doubleToLongBits`; not the default reference-array record equality. Diagnostic record `toString` is not canonical JSON | [PAY fields][PAY-field]; source-only |
| `parse(String)` | Null is NPE; syntax/type/range failures are IAE. Produces an unmodifiable, key-sorted field snapshot; no source lookup or file I/O | [PAY parser][PAY-parser]; [payload test][T-payload], [translation map test][T-translation] |
| `fieldNames`, `fields` | Unmodifiable key-set/map views of the retained sorted fields | [PAY access][PAY-access]; field names in [payload test][T-payload] |
| `field(name)` | NPE for null; absent name is empty Optional | [PAY access][PAY-access]; source-only miss/null contract |
| `requireInt`, `requireLong` | Require INTEGER kind; int additionally enforces int range. Null name is NPE; missing/wrong kind or int overflow is IAE | [PAY access][PAY-access]; [payload test][T-payload] |
| `requireBoolean`, `requireString` | Exact kind; null name is NPE, missing/wrong kind is IAE | [PAY access][PAY-access]; [payload test][T-payload], [translation map test][T-translation] |
| `requireDouble` | Accepts INTEGER or DECIMAL; integer conversion can round large longs to double. Wrong kind is IAE. Missing name yields NPE through `Objects.requireNonNull(fields.get(...))`, unlike the other `require` methods | [PAY access][PAY-access]; numeric success in [payload test][T-payload]; missing/rounding source-only |
| `requireIntArray`, `requireLongArray` | Require INTEGER_ARRAY; return detached arrays. Int version validates every element. Null name is NPE; missing/wrong kind or int overflow is IAE | [PAY access][PAY-access]; [payload test][T-payload] |
| `canonicalJson()` | Deterministic serialization of the admitted sorted model; returns a new String. It is not a signature or proof of donor semantics | [PAY access][PAY-access]; round trips in [payload test][T-payload] and [translation map test][T-translation]; catalog canonical rejection in [receiver test][T-receiver] |

## M3LexiconPrecompute typed values

These classes perform no I/O and have no close/eviction lifecycle. They copy
mutable arrays/maps at construction and expose immutable scalars, detached arrays
or unmodifiable maps. This protects subsequent caller edits; it does not make
concurrent mutation during construction safe. Required String labels are
non-null/nonempty (IAE); array/map/object nulls normally produce NPE. Unless
explicitly described as records, these classes retain ordinary object-identity
`equals`/`hashCode`. Repeated equivalent construction does not canonicalize them.

### Translation and spelling

| Public surface | Contract | Evidence |
| --- | --- | --- |
| `TranslationProjection` five-argument constructor | Required lexicon/source-language/target-language/source-fingerprint labels; copies IDs, rejects negative IDs and derives `mappedTokenCount` as positions where `translatedTokenIds[i] != i`. Zero is a valid ID, not a universal “unmapped” sentinel | [VAL translation][VAL-translation]; [typed test][T-typed], [translation map test][T-translation] |
| Six-argument constructor with `mappedTokenCount` | Same derivation; supplied count must be `0..array.length` and exactly equal to derived changed-position count, else IAE | [VAL translation][VAL-translation]; [translation map test][T-translation] checks inconsistent count |
| `lexiconFingerprint`, `sourceLanguage`, `targetLanguage`, `sourceFingerprint`, `mappedTokenCount` | Retained scalar metadata/count; does not identify the originating source record without an enclosing key | [VAL translation][VAL-translation]; [typed test][T-typed] |
| `translatedTokenIdAt(index)`, `translatedTokenIds()` | Checked index (IOOBE), or complete cloned array | [VAL translation][VAL-translation]; clone isolation in [typed test][T-typed] |
| `appliesTo(lexicon, source, target, fingerprint)` | Exact four-label equality; null comparison arguments yield false. No lookup/derivation | [VAL translation][VAL-translation]; positive/negative fingerprints in [typed test][T-typed] |
| `SpellIndex(...)` constructor | Required labels; nonnegative edit distance/prefix length. Copies delete rows to sorted String keys; candidate IDs must be nonnegative and strictly increasing. Empty delete key is allowed in this constructor. Copies frequency map to sorted nonnegative token IDs/counts (`long`) | [VAL spell][VAL-spell]; sorted keys, empty key, duplicate candidate refusal in [typed test][T-typed] |
| `lexiconFingerprint`, `language`, `maxEditDistance`, `prefixLength`, `sourceFingerprint` | Retained scope metadata; does not run a spell-correction algorithm | [VAL spell][VAL-spell]; successful scope setup in [typed test][T-typed] |
| `tokenIdsForDelete(deleteKey)` | Null is NPE; exact match returns a clone; miss returns a new empty array | [VAL spell][VAL-spell]; clone isolation in [typed test][T-typed] |
| `deleteToTokenIds()` | New unmodifiable sorted map with cloned arrays; arrays inside that returned map are caller-owned mutable copies | [VAL spell][VAL-spell]; ordering in [typed test][T-typed]; nested-copy boundary source-only |
| `frequencyAt(tokenId)`, `frequencies()` | Negative ID is IAE; absent ID returns zero, indistinguishable from explicit zero via this method. Map accessor is the retained unmodifiable sorted map | [VAL spell][VAL-spell]; present/absent frequency in [typed test][T-typed] |

Spell candidates/frequencies are stored data; candidate IDs are not checked for
membership in a supplied vocabulary and candidate sets need not have corresponding
frequency entries. Translation IDs are nonnegative but need not be within the
projection array's length. Those relationships belong to the source admission
contract, not these constructors.

### Token hashes, prefixes and frequencies

| Public surface | Contract | Evidence |
| --- | --- | --- |
| `RangeFingerprint(first, second, length)` and all accessors | Record with arbitrary two long lanes and nonnegative length; negative length is IAE. Component equality is not cryptographic validation | [VAL token][VAL-token]; [typed test][T-typed] |
| `TokenHashPrecompute(...)` constructor | Required value fingerprint, `0 <= rangeStart <= rangeEnd`; one digest per token in the half-open range; each token digest and range digest exactly 32 bytes; supplied range fingerprint length must match. Deep-copies digests; shape mismatches are IAE | [VAL token][VAL-token]; clone isolation and count mismatch in [typed test][T-typed] |
| `valueFingerprint`, `rangeStart`, `rangeEnd`, `rangeFingerprint` | Retained labels/token bounds and immutable fingerprint record | [VAL token][VAL-token]; [typed test][T-typed] |
| `tokenSha256`, `rangeSha256` | Deep clone of matrix / clone of range digest | [VAL token][VAL-token]; token-matrix isolation in [typed test][T-typed] |
| `appliesTo(fingerprint, start, end, range)` | Exact value-label, token bounds and record equality. Null fingerprint/range yields false. Does not compare/recompute SHA-256 bytes or carry tokenizer version; that version belongs in the enclosing key | [VAL token][VAL-token]; positive case in [typed test][T-typed]; limits source-only |
| `PrefixCounts(valueFingerprint, tokenId, prefixCounts)` | Required label, nonnegative token ID; nonempty counts array beginning at zero and monotonically nondecreasing. Copies input; invalid shape is IAE | [VAL prefix][VAL-prefix]; [typed test][T-typed] checks initial value, empty/decreasing arrays |
| `valueFingerprint`, `tokenId`, `prefixCounts()` | Retained scalar metadata / cloned cumulative-count array | [VAL prefix][VAL-prefix]; clone isolation in [typed test][T-typed] |
| `rangeCount(startInclusive, endExclusive)` | Returns `prefixCounts[end] - prefixCounts[start]`. Successful calls require `0 <= start <= end < prefixCounts.length`; see boundary caveat below | [VAL prefix][VAL-prefix]; normal range and oversized range in [typed test][T-typed] |
| `TokenFrequency(valueFingerprint, frequencies)` | Required label; copies to an unmodifiable sorted integer map; null keys/values are NPE, negative IDs/counts are IAE | [VAL frequency][VAL-frequency]; sorting in [typed test][T-typed] |
| `valueFingerprint`, `frequencyAt(tokenId)`, `frequencies()` | Retained label/map; negative queried ID is IAE, absent ID returns zero, so use map membership to distinguish explicit zero from absence | [VAL frequency][VAL-frequency]; [typed test][T-typed] |

Boundary caveat: `PrefixCounts.rangeCount` calls
`Objects.checkFromToIndex(start, end, prefixCounts.length)` and then indexes both
endpoints. The preliminary check permits `end == prefixCounts.length`, but the
array access then throws `ArrayIndexOutOfBoundsException`. Do not pass the array
length as a cumulative-count endpoint; a cumulative array of length `N + 1`
supports token endpoints through `N`. This edge is observed in the source, not
covered by the selected test's `(0, 6)` refusal on a length-five array. No source
fix or executed regression result is claimed here.

### Dictionary facts, signals and SI units

| Public surface | Contract | Evidence |
| --- | --- | --- |
| `IndexWordFacts(...)` constructor | Required `sourceRevision`; nonnegative language ID, word count, total corpus tokens/documents, corpus count and document frequency. Clones all four arrays. Fingerprint/flags and array element values are retained without additional vocabulary/range/consistency checks | [VAL word facts][VAL-wordfacts]; [typed test][T-typed] checks negative corpus total and copy isolation |
| `sourceRevision`, `languageId`, `wordCount`, `lexiconFingerprint`, `totalCorpusTokens`, `totalDocuments`, `flags`, `corpusCount`, `documentFrequency` | Retained scalar values; counts are not recomputed from arrays or text | [VAL word facts][VAL-wordfacts]; selected assertions in [typed test][T-typed] |
| `memberships`, `conceptIds`, `subjects`, `expansionWordIds` | Detached copies of respective arrays | [VAL word facts][VAL-wordfacts]; membership isolation in [typed test][T-typed] |
| `membershipCount`, `conceptCount`, `subjectCount`, `expansionSize` | Actual respective array lengths, independent of `wordCount` | [VAL word facts][VAL-wordfacts]; selected counts in [typed test][T-typed] |
| `membershipAt`, `conceptAt`, `subjectAt`, `expansionWordIdAt` | Checked index into corresponding array; IOOBE outside `0..length-1` | [VAL word facts][VAL-wordfacts]; successful access in [typed test][T-typed]; bounds source-only |
| `appliesTo(language, fingerprint)` | Compares only stored language ID and long fingerprint; does not compare source revision, source record or lexeme | [VAL word facts][VAL-wordfacts]; [typed test][T-typed] |
| `IndexWordSignal(...)` constructor | Required revision; nonnegative UTF-16/code-point lengths with code points no greater than units; nonnegative frequency/lexical ranks and script ordinal. Boundaries must be `-1` or `Character.isValidCodePoint`; zero code points requires both `-1`, positive count requires nonnegative boundaries. Optional lemma/phonetic/stem IDs accept `-1` or nonnegative. Violations are IAE | [VAL word signal][VAL-wordsignal]; [typed test][T-typed] checks boundaries and optional IDs |
| `sourceRevision`, `utf16Length`, `codePointLength`, `firstCodePoint`, `lastCodePoint`, `flags`, `frequencyRank`, `lemmaId`, `lexicalRank`, `morphologyMask`, `phoneticId`, `posMask`, `presence64`, `scriptOrdinal`, `simHash64`, `stemId` | Retained scalar getters, no text recomputation. `Character.isValidCodePoint` is the numeric Unicode range check, not a Unicode-scalar/surrogate exclusion; script ordinal has no upper-bound enum check here | [VAL word signal][VAL-wordsignal]; selected lengths/ranks/IDs in [typed test][T-typed]; broader limits source-only |
| `SiUnitPrecompute(decimalExponent, dimensionPacked, offset, prefixable)` and four accessors | Record; exponent must be `-100..100`, offset finite, otherwise IAE. Packed dimension bits and boolean are retained without a unit-system proof | [VAL SI][VAL-si]; [SI decoder][T-si] |

The dictionary constructor names do not prove dictionary lineage. Source-family
and revision constraints are supplied selectively by the builder's `wordFacts`
and `wordSignal` paths. The separate dictionary-frequency manifest test checks
manifest/revision/field-map declarations; it does not instantiate these owners.

## Bounded caller examples

These snippets illustrate reviewed signatures and control flow; they are
**NOT_RUN**. `exportDirectory`, `sourceId` and `recordId` are caller-provided
values from the intended admitted export, not discovered by guessing row numbers.
`SharedLexiconCatalog` has a `close` method but does **not** implement
`AutoCloseable`, so use an explicit `finally` rather than declaring it directly
in try-with-resources.

```java
SharedLexiconCatalog catalog = SharedLexiconCatalog.openLazy(exportDirectory);
try {
    var mapping = catalog.findMapping(sourceId, recordId);
    if (mapping.isPresent()) {
        String text = catalog.textAt(mapping.orElseThrow().coordinate());
        // Use text as a detached Java String; retain source identity separately.
    }
} catch (java.io.UncheckedIOException deferredImageFailure) {
    // Inspect the cause; repair/retry only under the export owner's policy.
    throw deferredImageFailure;
} finally {
    catalog.close();
}
```

Checked `IOException` from the initial `openLazy` must also be handled or declared
by the enclosing caller. Do not blindly retry unchanged corruption. A successful
retry is possible only while the catalog is open and the repaired shard matches
its captured manifest/sidecars.

```java
var source = new SharedLexiconPrecomputeCatalog.TranslationIdentity(
        "translate.rows", "row-1", "en", "fr", "lex-v1", "source-v1");
var projection = new M3LexiconPrecompute.TranslationProjection(
        "lex-v1", "en", "fr", "source-v1", new int[] {0, 7, 2});
var derived = SharedLexiconPrecomputeCatalog.builder()
        .translation(source, projection).build();
int changedPositions = derived.translationAt(source).orElseThrow().mappedTokenCount();
// Expected from the source contract: 1, because only vocabulary position 1 changed.
```

This constructed example demonstrates shape/identity behavior only. Its arbitrary
labels/IDs do not establish a real translation, approved corpus or source lineage.

```java
var prefixes = new M3LexiconPrecompute.PrefixCounts(
        "value-v1", 7, new long[] {0, 1, 1, 2});
long count = prefixes.rangeCount(1, 3); // Expected: 1; endpoint 3 is valid.
// Endpoint 4 is the array length and is invalid for this implementation.
```

For related metadata, obtain `findAll(sourceId, recordId)` while the receiver is
open. Do not interpret `find(...).isEmpty()` as “no relation” when the record can
have multiple targets.

## Test attribution and remaining qualification

All ten Java test files below were read in full at the same public source pin;
assertions are available source evidence, **not passing runs**. The Python file
was read only at the two selected boundaries identified below.

| Test | Actual scope read; what it does not establish |
| --- | --- |
| [FoundationTest][T-foundation] | `catalog` constructs two v2 shards, retains multiple mappings per lexeme, checks exact/unpaired-UTF-16 lookups, prefix/miss/materialization, profiles, unsafe file name/wrong hash/dropped profile rejection, corrupt lazy second-shard failure then file replacement/retry, and selected post-close refusals. `exportedCatalog` is conditional on one command-line argument. Other methods cover storage/pieces; their concurrency checks concern arena identities, not catalog close races |
| [SynexiaPrecomputeReceiverTest][T-receiver] | Lazy payload hit/miss, same cached object and rejection of noncanonical JSON ordering during startup |
| [SharedLexiconCatalogFamilyAdmissionTest][T-admission] | Both eager/lazy opening admit five families and two related targets; legacy absence, missing family file and closed family getter |
| [SharedLexiconFamilySidecarCatalogTest][T-family] | Five typed lookups, grouping two spell rows, row counts, legacy absence, partial bundle and checksum-tamper rejection. Does not assert every row-shape or cross-generation case |
| [SharedLexiconFamilyContractTest][T-contract] | Reads field-map TSV and compares the expected family declarations. No construction or lookup of either catalog; no runtime lifecycle evidence |
| [SynexiaPrecomputePayloadTest][T-payload] | Typed reads, unknown scalar, empty object, canonical round trip, duplicate/type/int-array-range/nonfinite/null/nested rejection |
| [M3LexiconPrecomputeTest][T-typed] | Typed copying/order/range/scope behavior, all seven builder lookup families, translation-key variants, duplicate insertion, dictionary/frequency family/revision refusal. Does not prove all setter key/value relationships |
| [SynexiaSiUnitDecoderTest][T-si] | Exact family/record SI lookup, exponent/finite-offset refusal and post-close decoder refusal |
| [M3TranslationReceiverMapTest][T-translation] | Manifest/field-map declarations plus actual translation derived/explicit mapped counts and mismatch refusal; String fingerprint round trip and numeric fingerprint acceptance, control/non-ASCII-digit refusal |
| [M3DictionaryFrequencyContractTest][T-dictionary] | Manifest/revision/field-map shape and identity declarations; its exceptions exercise test-local parsers, not the typed receiver constructors |
| [Selected Python export tests][T-python] | `test_directed_related_lexeme_sidecar_round_trips_and_requires_input` covers Python export/verifier metadata; `test_java_catalog_opens_actual_python_export` defines a javac/Java FoundationTest subprocess path. Neither was executed, and their imported exporter/verifier implementations are not claimed fully reviewed here |

Open qualification includes catalog first-touch/close stress and fairness,
external-file replacement/truncation behavior, allocation failure/resource limits,
all malformed sidecar combinations, full key/value/source-generation binding,
SI missing-field failures, related lookup/close edge cases, the exact prefix end
boundary, and platform mapping reclamation. The file receiver's five-family
schema, base catalog facts and in-memory builder are distinct admission surfaces;
this guide does not close their exporter/codec/provider or source-authenticity
proofs. No test count, hash or new prose advances the receiving phase.

## Source identity ledger

All links below are in the public `hsoliwal/M3jdk21` repository at the pinned
commit. Git blob IDs identify bytes, not executed behavior.

| Complete production body | Git blob |
| --- | --- |
| [SharedLexiconCatalog][CAT] | `aa8cf83bbc8349a2ed5a0a7dad818c2d7240f577` |
| [SharedLexiconFamilySidecarCatalog][FAM] | `e991a6ba84d7074e9bd260cce331d5092e7ad9f5` |
| [SharedLexiconPrecomputeCatalog][PC] | `b952a245b2724140904bb666669c2880ce32cd64` |
| [SharedRelatedLexemeCatalog][REL] | `182042dfc5115a683449622dcdc6055e916f4e4c` |
| [SynexiaPrecomputePayload][PAY] | `682e7f2abd59efd2adf30fb039616c61a16aa029` |
| [M3LexiconPrecompute][VAL] | `edec2ae3a82bde34159abd9a4d1c670ad59f031f` |

`SharedLexiconImage` is a rechecked dependency boundary (blob
`7d0dd69b3f92bc6e589eed4024d22686020b421c`), already described in the foundation
reference. This is six additional owner files, not six packages and not completion
of all 17 non-descriptor `com.m3.text` files. The package denominator and other
unreviewed APIs retain their [open status](coverage.md).

[CAT]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SharedLexiconCatalog.java
[FAM]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SharedLexiconFamilySidecarCatalog.java
[PC]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SharedLexiconPrecomputeCatalog.java
[REL]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SharedRelatedLexemeCatalog.java
[PAY]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SynexiaPrecomputePayload.java
[VAL]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/M3LexiconPrecompute.java
[IMAGE]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SharedLexiconImage.java
[CAT-open]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SharedLexiconCatalog.java#L93-L137
[CAT-records]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SharedLexiconCatalog.java#L139-L159
[CAT-lookups]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SharedLexiconCatalog.java#L161-L204
[CAT-lifecycle]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SharedLexiconCatalog.java#L206-L244
[CAT-search]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SharedLexiconCatalog.java#L246-L308
[CAT-payload]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SharedLexiconCatalog.java#L310-L374
[CAT-si]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SharedLexiconCatalog.java#L376-L400
[CAT-admission]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SharedLexiconCatalog.java#L401-L489
[CAT-sidecars]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SharedLexiconCatalog.java#L490-L784
[FAM-keys]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SharedLexiconFamilySidecarCatalog.java#L36-L116
[FAM-open]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SharedLexiconFamilySidecarCatalog.java#L140-L194
[FAM-lookups]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SharedLexiconFamilySidecarCatalog.java#L196-L211
[FAM-parser]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SharedLexiconFamilySidecarCatalog.java#L213-L477
[PC-keys]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SharedLexiconPrecomputeCatalog.java#L23-L90
[PC-lookup]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SharedLexiconPrecomputeCatalog.java#L110-L134
[PC-build]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SharedLexiconPrecomputeCatalog.java#L136-L205
[REL-record]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SharedRelatedLexemeCatalog.java#L39-L54
[REL-open]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SharedRelatedLexemeCatalog.java#L56-L112
[REL-methods]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SharedRelatedLexemeCatalog.java#L114-L149
[PAY-field]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SynexiaPrecomputePayload.java#L28-L76
[PAY-access]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SynexiaPrecomputePayload.java#L87-L153
[PAY-parser]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/SynexiaPrecomputePayload.java#L192-L372
[VAL-si]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/M3LexiconPrecompute.java#L24-L33
[VAL-translation]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/M3LexiconPrecompute.java#L35-L99
[VAL-spell]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/M3LexiconPrecompute.java#L101-L172
[VAL-wordfacts]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/M3LexiconPrecompute.java#L175-L260
[VAL-wordsignal]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/M3LexiconPrecompute.java#L262-L334
[VAL-token]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/M3LexiconPrecompute.java#L336-L391
[VAL-prefix]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/M3LexiconPrecompute.java#L393-L419
[VAL-frequency]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/src/com/m3/text/M3LexiconPrecompute.java#L421-L445
[T-foundation]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/test/FoundationTest.java
[T-admission]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/test/SharedLexiconCatalogFamilyAdmissionTest.java
[T-family]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/test/SharedLexiconFamilySidecarCatalogTest.java
[T-receiver]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/test/SynexiaPrecomputeReceiverTest.java
[T-contract]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/test/SharedLexiconFamilyContractTest.java
[T-si]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/test/SynexiaSiUnitDecoderTest.java
[T-payload]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/test/SynexiaPrecomputePayloadTest.java
[T-typed]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/test/M3LexiconPrecomputeTest.java
[T-translation]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/test/M3TranslationReceiverMapTest.java
[T-dictionary]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/core/test/M3DictionaryFrequencyContractTest.java
[T-python]: https://github.com/hsoliwal/M3jdk21/blob/c038343521587d96c9fa190715ce2f4b9f8c490a/m3/runtime-integration/tests/test_synexia_export.py
