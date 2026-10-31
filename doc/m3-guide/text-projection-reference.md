<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Text projections, offline tools and source receipts

[Guide](README.md) · [All 17 text sources](text-source-coverage.md) · [Storage contracts](m3-packages.md#textstorage-api-contracts) · [Catalog contracts](lexicon-catalog-reference.md) · [Coverage](coverage.md)

This reference reads the complete bodies and declared members of five owners in
`m3/core/src/com/m3/text` at
`8758991645037126985d56f16a208fec8a1eef38`: `LexiconTool`, `M3NumberSpace`,
`M3InstanceIndexPrecompute`, `M3LanguageGrammarSupport` and
`SynexiaLangDexSourceEnvelope`. The existing storage and catalog references keep
their original scope and provenance. This page adds the five missing owner
contracts; it does not repeat or replace those twelve earlier reviews.

All examples, builds, tests, scripts, runtime checks and measurements are
**NOT_RUN in this documentation pass**. Test descriptions below identify
assertions present in source, not results from executing them. The source map
does not establish inline-package documentation, whole-project completion or
receiving acceptance. The [porting invariant](../../m3/docs/m3jdk21-porting-invariant.md)
and [name mapping](../../m3/docs/name-mapping.json) retain their authority.

## Choose the boundary

| Owner | What it provides | What the result does not establish |
| --- | --- | --- |
| [LexiconTool][CLI] | Explicit file-to-image build or warmed-image diagnostic process | Automatic String integration, atomic publication by the CLI itself, or a measured memory saving |
| [M3NumberSpace][NUM] | M3-generated decimal views for the inclusive integer range 0..10000 | Imported donor corpus rows, a locale-aware number parser, or cross-process canonical identity |
| [M3InstanceIndexPrecompute][INST] | Immutable forward/reverse views over operator-provided proper-name records and frozen concept IDs | A port of an executable Synexia instance API, a bundled dataset, or generic lexicon-export admission |
| [M3LanguageGrammarSupport][GRAM] | A fixed sixteen-code Boolean support signal | Grammar rules, a grammar engine, or successful translation of any token |
| [SynexiaLangDexSourceEnvelope][ENV] | Wire-grammar/count/digest verification against an expected receipt | Producer authentication, row identity resolution, catalog admission or derived precompute payloads |

## Build and module ownership

The [descriptor][MODULE] declares `module com.m3.text { exports com.m3.text; }`.
It describes an independent experimental substrate, not a replacement for
`java.lang.String`. The [core build script][BUILD] requires `M3_JDK`, changes to
the `m3` directory, compiles every Java source under `core/src` with
`--release 21`, and packages `build/com.m3.text.jar`. Its environment error asks
for an isolated JDK 21 directory, but the script only checks that `M3_JDK` is
nonempty and invokes its `bin` tools; it does not verify version, vendor or
isolation. The module is not a listed
child of the [Maven control reactor][POM], including its declared profiles.
Do not substitute reactor membership for this explicit build route.

The script compiles an explicit list of main-based Java test classes, then runs
that list in four modes: default JIT, `-Xint`, `-XX:-CompactStrings`, and
`-Xbatch -XX:-TieredCompilation`, with assertions enabled. The list includes
the five focused test classes for number space, number contracts, grammar
support, instance indexing and LangDex source envelopes, as well as the separate
LangDex projection-receiver test. `M3NumberSpaceExportFixture` is not in that
script's compile/run list. The script finally builds and publishes the demo
image as described below. These are source-backed steps, not proof that any
mode has passed at this documentation head.

## LexiconTool: explicit file effects and diagnostic lifetime

The complete declared surface is its private no-argument constructor and
`public static void main(String[] args) throws Exception`; there are no fields
or nested types. `main` recognizes exactly two three-argument forms. [Source][CLI]

| Invocation | Order and effects | Failure boundary |
| --- | --- | --- |
| `build INPUT OUTPUT` | `Path.of(OUTPUT)` → `Path.of(INPUT)` → `Files.readAllLines` → `SharedLexiconImage.create(outputPath, lines)` | Java evaluates the output-path argument first, so its failure can precede input I/O. Input is read fully using the `Files.readAllLines(Path)` UTF-8 default; the CLI performs no sort, deduplication or normalization. Path, decoding, filesystem and image-creation failures propagate |
| `map IMAGE HOLD_MILLIS` | Open image → `warm()` → print `pid=… image=… records=… bytes=…` → flush stdout → parse milliseconds → sleep → `Reference.reachabilityFence(image)` | Milliseconds are parsed only after mapping and printing. Bad numeric input, a negative sleep value or interruption can fail after those effects. There is no catch/retry/close wrapper |
| Any other argument count or command | Throw `IllegalArgumentException` with `build INPUT OUTPUT \| map IMAGE HOLD_MILLIS` | No help/status subcommand or option parser exists; null programmatic arguments are not sanitized |

Image ordering, duplicate refusal, version, size and output-creation rules belong
to [SharedLexiconImage](m3-packages.md#sharedlexiconimage-file-and-lifetime-contract).
In particular, its creator requires sorted unique words and creates a new file;
the CLI does not overwrite an existing destination or provide an atomic file
publication protocol. Failed creation may leave a newly created partial file.
The diagnostic has no explicit unmap API. On the successful sleep path the
reachability fence keeps the image reachable through the requested hold period;
it is not a promise of permanent physical residency.

[build.sh][BUILD] adds a publication protocol around the CLI: build into a
private temporary directory, compute the file SHA-256, remove write permissions,
use a content-addressed `english-demo-<sha256>.m3lex` name, and perform `mv -n`.
If the temporary image still exists, `cmp` checks it against the existing
destination before deleting the temporary copy. The script records the image
path and artifact hashes. Those shell-level steps are separate from
`LexiconTool.main`; they are not a general durability, locking or adversarial
concurrent-writer guarantee.

The separate [Linux shared-mapping diagnostic][MAPTEST] launches two `map`
processes with a 5000 ms hold, reads their reports, and inspects each
`/proc/<pid>/maps` for `r--s` mappings of the same image inode. It checks process
exit and cleans up still-running processes. Its own evidence explicitly limits
the claim to a shared read-only file mapping, not PSS or memory savings. This
diagnostic and the CLI were not executed here. `FoundationTest.images` exercises
the delegated image owner directly; it is not a CLI argument/error test.

## M3NumberSpace: generated range and explicit materialization

### Identity and initialization

The seven public constants/fields are:

- `SOURCE_ID = "dictlang.numbers.0-10000"`
- `SOURCE_REVISION = "64a2ea61c73b548413fed6686a9daeeb0b9b0564"`
- `SOURCE_RECORD_ID_FIELD = "id"`
- `RECORD_ID = "number"`, the compatibility family key, not a per-value ID
- `MIN_VALUE = 0` and `MAX_VALUE = 10_000`, both inclusive
- `INSTANCE`, eagerly constructed by class initialization

The private constructor computes total decimal lengths using `Math.addExact`,
builds one temporary `char[]` plus offset/length arrays, writes the canonical
`Integer.toString(value)` spellings, copies the characters through
`LocalM3Arena.copyUtf16`, then caches 10,001 `LocalM3StringPiece` subsequences.
Its two retained fields are `backing` and `values`; it does not retain the
temporary decimal Strings or offset/length arrays. All cached pieces retain the
same immutable UTF-16 backing owner. A small piece can therefore retain the
whole backing. Initialization allocates; this is not a no-allocation or
mapped-file representation. [Source][NUM]

The [number target map][NUMMAP] calls this `ADMITTED_TYPED_RECEIVER`; that is
the specific numeric source/target mapping recorded there. It does not admit
unrelated instances or LangDex rows. The [source manifest][MANIFEST] and
[export contract][EXPORT] explicitly identify the range and spellings as
M3-generated. The donor scalar/decimal-ID relationship does not imply donor
rows for the whole range. `sourceRecordId(value)` returns the generated decimal
ID, while `RECORD_ID` remains the fixed family key.

### All methods and parser cases

| Declared member | Contract |
| --- | --- |
| `number(int)` | Validate the inclusive range and return the same cached piece object for that value on repeated calls |
| `spelling(int)` | Call `number(value).flatten()`; ordinary String materialization is explicit and is not a cached String result |
| `sourceRecordId(int)` | Delegate to `spelling`; the returned text equals the canonical decimal value, not an image coordinate or allocated identifier |
| `parse(CharSequence)` | Reject null with `NullPointerException`; read `length()`/`charAt()` without invoking the input's `toString()`; return the cached piece for the parsed value |
| `precomputedValueCount()` | Return the cached array length, 10,001 |
| `storageIdentity()` | Return the backing's `StorageIdentity`; this identifies shared local storage rather than value equality or a portable identifier |
| private `checkValue(int)` | Throw `IndexOutOfBoundsException` outside 0..10000; shared by `number` and the methods that delegate to it |
| private `describe(CharSequence)` | Use an existing String directly; otherwise copy the input by `charAt` into a StringBuilder for an error message, without calling the input's `toString()` |

`parse` permits only ASCII decimal digits, no leading zero except the single
`"0"`, and a value at most 10000. Empty input and leading zeros produce
`NumberFormatException`; non-digits do likewise. Its pre-multiply check
`value > (MAX_VALUE - digit) / 10` rejects out-of-range values without integer
overflow. Thus `+1`, `-1`, spaces, decimal points and non-ASCII digits are outside
this parser, even if another numeric API accepts them. For a rejected non-String
input, error construction can copy the whole input: early numeric refusal is
not a bound on diagnostic allocation or a hostile-CharSequence sandbox.

Callers must keep a mutable/custom CharSequence stable during parsing. There is
no snapshot, synchronization, cancellation or validation of that object's own
behavior. Exceptions thrown by its methods and allocation failures propagate.
After initialization these methods do not mutate the number cache; no
concurrency stress or resource measurement was performed in this pass.

### Numeric export is a separate boundary

[M3NumberSpaceExportFixture][NUMFIX] reads the actual Java owner, emits eight
columns for every value, verifies decimal identity/spelling and shared backing,
and writes a caller-named UTF-8 TSV. `Files.newBufferedWriter(output, UTF_8)`
uses the default create/truncate/write options, so an existing named output
can be truncated; there is no atomic publication protocol. Its `main` accepts
exactly one output path; it is a test fixture, not a public exporter method on
`M3NumberSpace`.

The [Python integration test][NUMPY] requires that emitted fixture and an
existing scratch directory. Its assertions build number-family records using
`language_tag=und` solely as an export envelope, `source_kind=number`,
`translation_profile=-`, decimal `mapping_id`, `NUMBER_<record_id>` names and
canonical decimal lexemes. It checks all eight fields against the manifest and
field map, invokes the existing exporter twice, invokes the source-blind
verifier, compares every emitted file byte-for-byte, and checks duplicate
source/record refusal. It does not turn a physical image row into a source ID.
This describes the read test's intended proof; neither Java fixture nor Python
export/verifier was run here, and this page is not a full exporter implementation
review.

## M3InstanceIndexPrecompute: operator records and exact scope

This owner is a target-side proposal. The
[instance contract row][INSTMAP] has status
`SEMANTIC_REFERENCE_ONLY_TARGET_SCHEMA_UNPROVEN`: the documented source
distinction keeps proper-name instances outside the X-spine and permits concept
references. The schema, validation, normalization, duplicate handling and lookup
behavior below are M3-owned choices. No data is bundled or admitted to generic
lexicon export by constructing this object; an exact operator source, license
and separately reviewed export contract are still required. [Source][INST]

### Constructor, records and validation

The public constructor receives four identity Strings, `Set<Long> frozenConceptX`
and `Map<String, InstanceRecord> recordsByName`.

1. `sourceId`, `sourceRevision`, `sourceFingerprint` and `normalizationVersion`
   must be nonnull and not `String.isBlank()`. They are retained verbatim, without
   trimming, hex-format checks, fingerprint recomputation or UTF-8 validation.
   A normalization-version label does not choose an algorithm: this
   implementation always uses its fixed ASCII fold.
2. The concept set is required, copied into a natural-order `TreeSet`, and
   exposed unmodifiable. Each entry must be a nonnull positive long. The helper
   also rejects a repeated entry while copying; a conforming Set normally
   already excludes duplicates. Empty sets are accepted.
3. The record map, each key and each record are required. Keys and record names
   must normalize to the same text. Every `instanceOfX` must belong to the copied
   frozen set. Two distinct input keys that fold to the same normalized name are
   rejected with `IllegalArgumentException`. A normal Map has already replaced
   any repeated exact key before this constructor sees it.
4. The constructor builds independent forward and reverse map structures.
   Existing immutable record objects are retained; caller maps/sets are not
   retained as mutable backing collections. No synchronization protects a
   caller mutating inputs during copying.

The nested enum `InstanceType` contains exactly `BOOK_TITLE`, `AUTHOR`, `PERSON`,
`PLACE`, `DATE`, `CHARACTER`, `ORGANIZATION` and `EVENT`. The enum value is not
checked against the referenced concept's semantic kind.

`InstanceRecord(String nameUtf8, InstanceType type, long instanceOfX,
Map<String,String> metadata)` has a compact constructor and the four ordinary
record component accessors. It validates the name, requires a nonnull type,
requires a positive concept ID, and copies metadata into an unmodifiable map.
Its `nameUtf8` is a Java String, not a byte array; the validator checks paired
surrogates so the name can be encoded losslessly as UTF-8. The record retains
the original case and spacing. Generated record equality/hash/toString remain
ordinary record behavior; they do not confer canonical identity or source trust.

Name validation rejects null, empty, all-ASCII-whitespace values, any code unit
below U+0020, and unpaired high or low surrogates. The whitespace helper tests
only space, tab, CR and LF; the subsequent control check rejects other C0
controls. Leading/trailing ordinary spaces around nonblank text are preserved.
DEL, non-ASCII whitespace and non-ASCII letter case are not filtered or folded.
No NFC/NFKC normalization occurs. Metadata keys use the same name validator but
are not lowercased. Metadata values must be nonnull and have paired surrogates;
empty values and controls are accepted. Keys are copied with unsigned UTF-8
ordering; duplicate validated keys are refused if encountered. No field schema,
content-size cap or metadata provenance validation is added.

Null input collections, null record-map keys/records and null record types
produce `NullPointerException`. Invalid/blank identity labels, invalid names,
null/nonpositive concept entries, key/name mismatch, unfrozen references,
normalized duplicate names, nonpositive record concept IDs and null metadata
values produce `IllegalArgumentException`. Source behavior should be used rather
than assuming every invalid input has the same exception category.

### Every lookup and ordering rule

| Declared member | Returned scope and behavior |
| --- | --- |
| `sourceId()`, `sourceRevision()`, `sourceFingerprint()`, `normalizationVersion()` | The four exact retained labels |
| `frozenConceptX()` | All supplied frozen positive concept IDs, including unreferenced ones, as an unmodifiable natural-order set |
| `recordsByName()` | Unmodifiable normalized-name → original record map, ordered by unsigned UTF-8 bytes |
| `namesByConceptX()` | Unmodifiable referenced-concept → unmodifiable normalized-name set map; concept keys have natural long order |
| `size()` | Number of normalized forward records |
| `referencedConceptCount()` | Number of concept keys with records, not the size of the frozen set |
| `appliesTo(source, revision, fingerprint, normalization)` | Exact equality on all four labels; null candidates compare false. It neither authenticates nor re-derives the labels |
| `findByName(String)` | Validate and ASCII-fold the query, then return the record or null for a valid missing name; an invalid query still throws |
| `instancesOf(long)` | Return `List.of()` for an absent concept, including nonpositive lookup values; otherwise allocate an immutable list in the reverse-name set's order |
| `normalizedInstanceName(String)` | Validate, then replace only ASCII A–Z with a–z in a newly built String; preserve all other code units |

The final reverse-name sets use **natural Java String order**, not the forward
map's unsigned UTF-8 comparator. Construction initially accumulates names in
UTF-8-ordered sets, but `reverse` is declared `TreeMap<Long, Set<String>>`; the
subsequent `new TreeSet<>(names)` therefore selects the Collection constructor
and natural ordering. `instancesOf` traverses that final set. The orders agree
for the existing ASCII fixture and can differ for supplementary versus BMP
names. This is a source-observed implementation boundary, not an executed
regression result or a silent fix. [Constructor lines 54–77][INST-ORDER]

The eight fields are the `UTF8_ORDER` comparator; four identity Strings; and
`frozenConceptX`, `recordsByName`, `namesByConceptX`. All are private final, with
the comparator static. Internal helper coverage is complete:

- `immutableConcepts` and `immutableMetadata` implement the collection copies
  and validation described above
- `text` implements only the identity-label null/blank check
- `validateName`, `asciiWhitespace` and `validateUtf8` implement the distinct
  name/control and paired-surrogate rules
- `compareUtf8` converts both Strings to UTF-8 byte arrays on each comparison,
  compares unsigned bytes, then compares lengths when one is a prefix

Thus deterministic ordering is not allocation-free: comparator calls encode
temporary byte arrays, queries allocate normalized names, and populated reverse
lookups build result lists. No serialization, refresh, interner, graph traversal,
close/free API or concurrent mutation API is declared. The immutable result's
lifetime follows ordinary reachability; caller-provided labels remain assertions.

## M3LanguageGrammarSupport: one immutable signal

This final utility declares `SOURCE_ID = "translate.rows"`,
`TARGET_FIELD = "translation_grammar_supported"`, private static `SUPPORTED`,
a private no-argument constructor and two public static methods. [Source][GRAM]

`supports(String languageCode)` returns false for null. Otherwise it applies
`trim().toLowerCase(Locale.ROOT)` and tests membership in the fixed set:
`ca`, `de`, `el`, `en`, `es`, `fr`, `it`, `nl`, `pl`, `pt`, `ro`, `ru`, `sk`,
`sl`, `ta`, `uk`. It does not parse locale tags or fall back from `en-US` to `en`;
`trim` is the Java String operation, not arbitrary Unicode-whitespace stripping.
`supportedCodes()` returns the same immutable set. Its membership is stable;
`Set.of` supplies no sorted or stable iteration-order contract.

The [source manifest][MANIFEST] and [field map][FIELDS] locate the signal in
translation payload metadata alongside other translation owners. A true result
does not load rules, validate a sentence, infer anything from token text or
guarantee that an operator dataset contains a translation. The helper has no
dataset, configuration, I/O, language-code registration or rule-evaluation API.
It is distinct from the translation identity and projection contracts in the
[catalog reference](lexicon-catalog-reference.md#translation-and-spelling).

## SynexiaLangDexSourceEnvelope: verification without admission

### Receipt and exact-byte contract

The only public operation is `verify(InputStream input, Receipt expected)`.
The nested public `Receipt` record declares `sourceRevision`, `lexemeRows`,
`conceptLinkRows` and `sha256`; its compact constructor rejects a null or
non-lowercase-40-hex revision, negative row counts, and a null or
non-lowercase-64-hex digest with `IllegalArgumentException`. The record supplies
ordinary component accessors and value equality. `sourceRevision` identifies
the dataset claimed by this receipt; it is not the M3 source pin or a verified
producer-code revision. [Source][ENV]

`verify` rejects null arguments with `NullPointerException`, creates a SHA-256
digest, wraps the input in `DigestInputStream` and `BufferedInputStream`, and
checks the whole wire against the expected receipt. On success it returns a
new receipt with the checked revision, observed counts and actual digest; it
does not promise object identity with `expected`. Wire mismatch or receipt-field
mismatch throws `IOException`; underlying stream I/O failures propagate.

The input is **left open** on both normal and exceptional paths. Successful
verification reads to EOF. Failure can consume input and buffering can read
ahead; the API supplies no rollback, reset or retry position. The caller owns
stream closure and must not concurrently consume or modify the input. There is
no shared mutable verification state between calls, but no concurrent-stream
qualification was performed.

The digest authenticates nothing by itself. It establishes equality to bytes
described by the supplied receipt, whose provenance must be trusted or verified
through a separate authenticated/signature mechanism. Supplying a digest for
untrusted bytes does not make the producer trusted. A receipt is not a license
or data-redistribution grant. [Envelope documentation][EXPORT-ENV]

### Wire grammar and accepted identity values

The first LF-terminated line must be
`#M3-LANGDEX-SOURCE-1<TAB><expected sourceRevision>`. The second is exactly the
six-column header `row_type`, `lexeme_id`, `glottocode_b64`, `lemma_b64`,
`source_b64`, `concept_id`, separated by tabs. Every subsequent row has exactly
six tab-separated fields, preserving empty trailing fields during splitting.

| Row type | lexeme_id | Three text columns | concept_id |
| --- | --- | --- | --- |
| `LEXEME` | Canonical signed-64-bit decimal or `~` | Each is `~` or `b64:` followed by canonical standard Base64 decoding to valid UTF-8 | Exactly `-` |
| `CONCEPT_LINK` | Canonical signed-64-bit decimal or `~` | Each exactly `-` | Canonical signed-64-bit decimal or `~` |

Canonical integer validation parses with `Long.parseLong` and compares back to
`Long.toString(parsed)`. It allows negative values and both signed-long extrema;
it rejects plus signs, leading zeros, `-0`, whitespace and out-of-range values.
It does not require IDs to be positive or present. `~` is null, distinct from
unused `-`; a text `b64:` encodes an empty String, distinct from null. Base64 is
decoded, re-encoded and compared exactly, then checked by a UTF-8 decoder with
malformed/unmappable reporting. The decoded text is discarded, not returned,
normalized or inserted anywhere. Embedded tabs/newlines and non-ASCII text can
be represented inside the encoded fields.

All wire bytes before LF must be ASCII; CR and bytes above 0x7f are rejected.
Every nonempty final line must terminate in LF; CRLF is rejected. A line can
contain at most 16 MiB before its LF. This is an M3 receiver policy, not an
assertion that the producer imposes the same limit. An extra blank line is an
invalid row. Each row type is rejected before incrementing if it would exceed
that type's expected count. At EOF both counts and the exact full-wire SHA-256
must match. Unknown row types, wrong column counts, wrong sentinels, invalid
encoding, malformed IDs and any count/digest discrepancy fail verification.

Neither row ordering, ID uniqueness nor link integrity is checked. Repeated
lexeme IDs, duplicate links, null IDs and links to absent lexemes can pass if
the wire grammar and supplied receipt match. No source row is exposed; no
concept join, deduplication, source-record mapping, image coordinate or
`SynexiaPrecomputePayload` is produced. The separate
[LangDex projection-receiver fixture][PROJTEST] constructs an already-shaped
catalog bundle and exercises the catalog/payload owners. Its success condition
would not establish an envelope-to-catalog conversion. The
[manifest's LangDex row][MANIFEST] still requires an exact licensed snapshot and
lossless row-identity policy; its concept-link rows are separately unmapped.

### All private members and resource limits

| Private declaration | Role |
| --- | --- |
| `MAGIC`, `HEADER` | Exact version/revision prefix and tabular header |
| `UNUSED`, `NULL_VALUE`, `VALUE_PREFIX` | The `-`, `~`, `b64:` marker grammars |
| `MAX_LINE_BYTES` | Per-line 16 MiB bound |
| `REVISION`, `SHA256` | Lowercase hex Patterns used by `Receipt` |
| Private no-argument constructor | Prevent utility instantiation |
| `readAsciiLine(InputStream)` | Accumulate one bounded ASCII line, consume LF, distinguish clean EOF from an unterminated line |
| `requireCanonicalLong(String, boolean)` | Optional null marker and canonical signed-long validation; all three current row-ID call sites pass true |
| `decodeNullable(String)` | Optional null marker, canonical Base64 and strict UTF-8 validation only |
| `requireUnused(String)` | Require exactly `-` |
| `increment(long)` | Use `Math.addExact`; translate count overflow to IOException |
| `sha256()` | Obtain the platform SHA-256 implementation; turn an unavailable required algorithm into AssertionError |

The line cap bounds an individual row's input size, not total artifact bytes,
row count, elapsed time or all concurrent heap use. Line buffering, String
splitting, Base64 bytes and UTF-8 decoding allocate temporary objects; the
receiver does not retain all rows. Expected counts may themselves be large.
There is no total-work budget, timeout, cancellation callback, signature check
or resource-failure recovery. Allocation errors are not caught as malformed
input. No resource or throughput measurement was performed.

## Test attribution and open qualification

The following complete test/fixture bodies were read. Every row is **NOT_RUN**.

| Evidence | Assertions or path actually present | Not established by those assertions |
| --- | --- | --- |
| [M3NumberSpaceTest][NUMTEST] | All 10,001 canonical spellings, endpoint cached identity, shared storage, view parsing with throwing `toString`, empty/leading-zero/out-of-range/nondigit refusal, numeric bounds, source/family constants | Null/custom-mutating inputs, locale/non-ASCII digits, initialization/resource failures or concurrency qualification |
| [M3NumberSpaceContractTest][NUMCONTRACT] | Exact constants/revision; UTF16_LE; shared backing, generated IDs and cached parse result across the entire range; selected invalid inputs; manifest, target-map and field-map relationships | Export execution, recipient/runtime acceptance or the meaning of arbitrary donor data |
| [M3NumberSpaceExportFixture][NUMFIX] and [Python integration test][NUMPY] | Java-owner TSV projection, all IDs/eight fields, two deterministic export invocations, independent verifier calls and duplicate-ID refusal are present in the source | Any observed execution result here; these are separate from the core build script's test list |
| [M3InstanceIndexPrecomputeTest][INSTTEST] | Three records/two concepts; ASCII case lookup; missing results; Alice-before-Paris reverse order; revision mismatch; non-ASCII preservation beside ASCII fold; input-map isolation; selected unmodifiable views; key mismatch, unfrozen concept, blank/control/unpaired-surrogate name and zero-concept rejection | Exhaustive null/metadata failures, all identity mismatches, normalized duplicate keys, supplementary-vs-BMP reverse ordering, or operator dataset/serialization admission |
| [M3LanguageGrammarSupportTest][GRAMTEST] | All sixteen codes and uppercase/outer-space variants, null/blank/zh/auto false, constants and translation manifest field mapping | Sorted iteration order, arbitrary locale-tag/Unicode-whitespace handling or grammar-rule execution |
| [LangDexSourceEnvelopeTest][ENVTEST] | Receipt equality for a six-lexeme/six-link expected-wire fixture; null/duplicate IDs and links, non-ASCII/embedded-control text, large IDs; added signed-long extrema; rejection of changed magic/header/revision, malformed/noncanonical Base64, invalid UTF-8, noncanonical ID, unused-field mutation, CRLF, missing final LF, changed digest and too-low counts | Execution of the producer writer; all receipt-constructor/null-input cases; line-size edge, unknown row type, wrong column count, deficient counts at EOF, numeric overflow, stream-open assertion, stream I/O/resource faults or authenticated provenance |
| [LangDexProjectionReceiverTest][PROJTEST] | Separate synthetic one-record catalog bundle, exact source/physical mapping, typed payload values, legacy optional fields, stable payload lookup, fixed facts and noncanonical JSON refusal | Intake-to-catalog admission, duplicate-source row-identity resolution or a real licensed snapshot |
| [FoundationTest][FOUNDATION] and [shared-mapping diagnostic][MAPTEST] | Image creation/open/copy/error checks in the former; two-process read-only same-inode inspection in the latter | Dedicated CLI malformed-argument tests or measured PSS/memory savings |

The LangDex expected-wire fixture says explicitly that it mirrors expected lines
from a pinned producer test; it is **not an artifact captured by running that
producer**. The instance test mutates a metadata input map, but its subsequent
`findByName("mutated") == null` check is not a direct assertion that the copied
record lacks that metadata key. The metadata copy is established by reading the
constructor, while returned-map mutation refusal is directly asserted.

The LangDex corrupted-wire cases reuse the original expected digest, and their
helper accepts any `IOException`. They assert overall rejection of each changed
artifact, but do not independently isolate the named grammar guard from a later
digest-mismatch rejection. The grammar-specific contracts above are established
by source reading; these tests alone are weaker than guard-isolating proofs.

No dedicated LexiconTool test source appears in the complete core test listing.
The selected callers above are concrete source-backed routes, not a complete
repository-wide inbound call graph. Full error-injection, cross-platform mapping,
concurrent-input/resource tests, trusted external-source admission and exact-head
build/runtime qualification remain open. Hosted CI, if reported separately,
must retain its own commit and scope; it is not evidence produced by this review.

[CLI]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/com/m3/text/LexiconTool.java
[NUM]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/com/m3/text/M3NumberSpace.java
[INST]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/com/m3/text/M3InstanceIndexPrecompute.java
[INST-ORDER]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/com/m3/text/M3InstanceIndexPrecompute.java#L54-L77
[GRAM]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/com/m3/text/M3LanguageGrammarSupport.java
[ENV]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/com/m3/text/SynexiaLangDexSourceEnvelope.java
[MODULE]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/module-info.java
[BUILD]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/build.sh
[POM]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/pom.xml
[MAPTEST]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/verify-shared-mapping.py
[NUMMAP]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/lexicon/synexia-number-target-map.tsv
[INSTMAP]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/lexicon/synexia-instance-index-contract.tsv
[MANIFEST]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/lexicon/synexia-source-manifest.tsv
[FIELDS]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/lexicon/synexia-precompute-field-map.tsv
[EXPORT]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/lexicon/SYNEXIA_EXPORT.md
[EXPORT-ENV]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/lexicon/SYNEXIA_EXPORT.md#langdex-source-envelope-intake-boundary
[NUMTEST]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/test/M3NumberSpaceTest.java
[NUMCONTRACT]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/test/M3NumberSpaceContractTest.java
[NUMFIX]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/test/M3NumberSpaceExportFixture.java
[NUMPY]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/runtime-integration/tests/test_m3_number_space_export.py
[INSTTEST]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/test/M3InstanceIndexPrecomputeTest.java
[GRAMTEST]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/test/M3LanguageGrammarSupportTest.java
[ENVTEST]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/test/LangDexSourceEnvelopeTest.java
[PROJTEST]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/test/LangDexProjectionReceiverTest.java
[FOUNDATION]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/test/FoundationTest.java
