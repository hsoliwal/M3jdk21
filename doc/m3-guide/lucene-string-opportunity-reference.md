<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Lucene integration opportunities for canonical String storage

[Guide](README.md) · [Source scope and hashes](lucene-string-opportunity-source-coverage.md) · [String ownership](bootstrap-string-ownership.md) · [String contracts](bootstrap-string-contracts.md) · [Indexed-string bridge](indexed-string-reference.md)

This is a **source-backed integration proposal**, not an implemented Lucene adapter or a performance result. It maps selected Lucene APIs to M3 ownership, compatibility and measurement gates. No repository compilation, recipe execution, tests, native build, benchmark or matched-runtime qualification was run for this reference. Existing hosted CI and historical qualification retain their own scope.

## 1. Recommendation and version lanes

The first useful slice is an application boundary that **already retains or replays analyzed tokens**: share immutable finalized spellings within a bounded batch, preserve each token's metadata, and replay into Lucene's existing mutable buffers. First measure unchanged Lucene on M3JDK21; introduce the adapter only where profiling and the application's retention requirement justify it.

Two different Lucene pins are used:

| Purpose | Exact source | Version gate |
| --- | --- | --- |
| Current fork design reference | [hsoliwal/lucene@1169cd2007d37c1e5354fdccf9aa7e4eef93d21a](https://github.com/hsoliwal/lucene/blob/1169cd2007d37c1e5354fdccf9aa7e4eef93d21a/README.md#L41-L49) | README requires JDK 25; this main branch is not a runnable M3JDK21 baseline |
| Concrete JDK21 experiment lane | [Apache Lucene 10.3.1@51190f35a16d2ce433139abfe0fd8365791b352a](https://github.com/apache/lucene/blob/51190f35a16d2ce433139abfe0fd8365791b352a/README.md#L39-L45) | README specifies OpenJDK 21; [versions.toml](https://github.com/apache/lucene/blob/51190f35a16d2ce433139abfe0fd8365791b352a/versions.toml#L21-L25) sets minJava=21 and [javac configuration](https://github.com/apache/lucene/blob/51190f35a16d2ce433139abfe0fd8365791b352a/gradle/java/javac.gradle#L20-L28) targets that release |
| M3 ownership/contracts | [M3JDK21@1de629d26ea82d2729a5993bfc88099f83221923](https://github.com/hsoliwal/M3jdk21/blob/1de629d26ea82d2729a5993bfc88099f83221923/src/java.base/share/classes/java/lang/M3String.java#L27-L41) | Requires its matching Java/HotSpot/JNI components and target qualification |

The Lucene 10.3.1 annotated tag object is `e40de7e3cdc5b67420dc465808d47f3d751d6572`. JDK21 compatibility in upstream build declarations is not proof that this release is qualified on M3.

The [M3 runtime guide](https://github.com/hsoliwal/M3jdk21/blob/1de629d26ea82d2729a5993bfc88099f83221923/m3/runtime-integration/README.md#L7-L11) describes `UseM3StringStorage` as default-off and interpreter-constrained; compiled mode, CDS, JFR, JVMCI and dedup are unsupported while enabled. Include a stock JDK21 interpreter control and a matched M3 flag-off control. A stock-JIT versus enabled-M3 interpreter comparison does not isolate the storage change. Do not enable unsupported runtime features to obtain a benchmark.

## 2. Ownership and API placement

[M3String](https://github.com/hsoliwal/M3jdk21/blob/1de629d26ea82d2729a5993bfc88099f83221923/src/java.base/share/classes/java/lang/M3String.java#L27-L41) is package-private `java.lang` storage behind ordinary String. Its canonical owner plus packed UTF-16 coordinate follows the recorded [MIndexString lineage](https://github.com/hsoliwal/M3jdk21/blob/1de629d26ea82d2729a5993bfc88099f83221923/m3/docs/m3string-synexia-lineage.md); it is not an application-importable replacement class. Preserve the public String contract and use existing target owners.

The public portable [M3Text bridge](https://github.com/hsoliwal/M3jdk21/blob/1de629d26ea82d2729a5993bfc88099f83221923/m3/ports/indexstring/src/main/java/com/m3/text/compat/M3Text.java#L42-L99) is a different explicit surface: `asReader()` traverses shared joined storage; `getChars()` copies into caller storage; `toString()` materializes. A portable bridge experiment does not establish bootstrap String integration.

The existing [name map](https://github.com/hsoliwal/M3jdk21/blob/1de629d26ea82d2729a5993bfc88099f83221923/m3/docs/name-mapping.json#L307-L334) already keeps lexical/language/model facts in separate namespaced/versioned image planes and explicitly excludes `MIndexLucenePostingPlan` from `java.lang.String`. Posting schedules remain higher-level search/index metadata. Do not add Lucene, ICU, OpenNLP or Synexia runtime dependencies to `java.base`, or introduce another authoritative spelling store.

The [porting invariant](../../m3/docs/m3jdk21-porting-invariant.md) remains recipe-first: reusable donor convergence/recipes are Synexia-owned, while M3 owns target integration and acceptance. Documentation does not advance String → arrays → collections → AST/compiler → remaining-family receiving phases.

## 3. Ranked compatibility map

| Priority | Actual integration point | Candidate benefit to measure | Required boundary |
| --- | --- | --- | --- |
| 1: unchanged runtime lane | [Analyzer.tokenStream/normalize](https://github.com/apache/lucene/blob/51190f35a16d2ce433139abfe0fd8365791b352a/lucene/core/src/java/org/apache/lucene/analysis/Analyzer.java), [Term](https://github.com/apache/lucene/blob/51190f35a16d2ce433139abfe0fd8365791b352a/lucene/core/src/java/org/apache/lucene/index/Term.java) and application-held ordinary Strings | Sharing of repeated retained spellings and reuse of existing String facts without a Lucene patch | Include native admission, traversal, compatibility copies and fallback. char[]-centric analysis is not automatically accelerated |
| 2: retained finalized tokens | [TokenStream](https://github.com/apache/lucene/blob/51190f35a16d2ce433139abfe0fd8365791b352a/lucene/core/src/java/org/apache/lucene/analysis/TokenStream.java), [CharTermAttributeImpl](https://github.com/apache/lucene/blob/51190f35a16d2ce433139abfe0fd8365791b352a/lucene/core/src/java/org/apache/lucene/analysis/tokenattributes/CharTermAttributeImpl.java), [CachingTokenFilter](https://github.com/apache/lucene/blob/51190f35a16d2ce433139abfe0fd8365791b352a/lucene/core/src/java/org/apache/lucene/analysis/CachingTokenFilter.java) | Bounded batch dictionary for repeated spellings; smaller retained token records where replay/retention is already required | Freeze at the last relevant mutating stage; retain offsets/graph/custom metadata per occurrence; replay to existing writable attributes |
| 3: repeated normalization/UTF-8 exports | [Analyzer.normalize(field,text)](https://github.com/apache/lucene/blob/51190f35a16d2ce433139abfe0fd8365791b352a/lucene/core/src/java/org/apache/lucene/analysis/Analyzer.java), [BytesRef](https://github.com/apache/lucene/blob/51190f35a16d2ce433139abfe0fd8365791b352a/lucene/core/src/java/org/apache/lucene/util/BytesRef.java), [UnicodeUtil](https://github.com/apache/lucene/blob/51190f35a16d2ce433139abfe0fd8365791b352a/lucene/core/src/java/org/apache/lucene/util/UnicodeUtil.java) | Caller-scoped reuse or vocabulary-batch preparation of repeated normalization and byte exports | Exact field/config/resource/Unicode version and encoding policy in keys; immutable ownership and explicit copy boundaries; arbitrary analyzers are not presumed memoizable |
| 4: dictionaries and label projections | [CharArrayMap](https://github.com/apache/lucene/blob/51190f35a16d2ce433139abfe0fd8365791b352a/lucene/core/src/java/org/apache/lucene/analysis/CharArrayMap.java), [SynonymMap.Builder](https://github.com/apache/lucene/blob/51190f35a16d2ce433139abfe0fd8365791b352a/lucene/analysis/common/src/java/org/apache/lucene/analysis/synonym/SynonymMap.java), [FSTCompiler](https://github.com/apache/lucene/blob/51190f35a16d2ce433139abfe0fd8365791b352a/lucene/core/src/java/org/apache/lucene/util/fst/FSTCompiler.java), [SortedDocValues.lookupOrd](https://github.com/apache/lucene/blob/51190f35a16d2ce433139abfe0fd8365791b352a/lucene/core/src/java/org/apache/lucene/index/SortedDocValues.java) | Share retained input spellings/facts during dictionary construction, or cache repeated decoded labels per live reader | Beat existing char[] lookup, byte pooling, FST compression and DocValues dictionaries. Reader/field/ordinal cache is bounded and discarded on close/change |
| 5: repeated query artifacts | [CompiledAutomaton](https://github.com/apache/lucene/blob/51190f35a16d2ce433139abfe0fd8365791b352a/lucene/core/src/java/org/apache/lucene/util/automaton/CompiledAutomaton.java), [FuzzyTermsEnum](https://github.com/apache/lucene/blob/51190f35a16d2ce433139abfe0fd8365791b352a/lucene/core/src/java/org/apache/lucene/search/FuzzyTermsEnum.java), [TermInSetQuery](https://github.com/apache/lucene/blob/51190f35a16d2ce433139abfe0fd8365791b352a/lucene/core/src/java/org/apache/lucene/search/TermInSetQuery.java) | Reuse compiled query work proven safely reusable for repeated identical semantics | Include all flags/options/version/budget; respect existing sharing and cached hashes. Candidate facts may reject only when mathematically sound |
| Defer | [TermsHashPerField](https://github.com/apache/lucene/blob/51190f35a16d2ce433139abfe0fd8365791b352a/lucene/core/src/java/org/apache/lucene/index/TermsHashPerField.java), codecs, term dictionaries, postings and DocValues storage | Investigate text conversion/ownership only if profiles identify it | Numeric postings and compressed blocks are not String payloads. Keep format/codec compatibility and Lucene's term-byte/ordinal machinery intact |

The [SynonymGraphFilter token-to-wordID FST TODO](https://github.com/hsoliwal/lucene/blob/1169cd2007d37c1e5354fdccf9aa7e4eef93d21a/lucene/analysis/common/src/java/org/apache/lucene/analysis/synonym/SynonymGraphFilter.java#L39-L40) is evidence of an exploratory design point, not an implemented optimization. Any future token-ID FST needs its own deterministic vocabulary mapping and full graph tests; its IDs cannot replace Lucene term ordinals or serialized term bytes.

## 4. Compatibility rules

### Mutable buffers and capture/replay

[TokenStream](https://github.com/hsoliwal/lucene/blob/1169cd2007d37c1e5354fdccf9aa7e4eef93d21a/lucene/core/src/java/org/apache/lucene/analysis/TokenStream.java#L43-L72) intentionally reuses AttributeImpl instances. [CharTermAttribute.buffer()](https://github.com/hsoliwal/lucene/blob/1169cd2007d37c1e5354fdccf9aa7e4eef93d21a/lucene/core/src/java/org/apache/lucene/analysis/tokenattributes/CharTermAttribute.java#L24-L48) returns a writable char[] that downstream filters may change directly. [CharTermAttributeImpl.getBytesRef()](https://github.com/hsoliwal/lucene/blob/1169cd2007d37c1e5354fdccf9aa7e4eef93d21a/lucene/core/src/java/org/apache/lucene/analysis/tokenattributes/CharTermAttributeImpl.java#L90-L95) encodes into a reusable builder. A lazy atom/UTF-8 cache cannot detect arbitrary writes to the exposed array; replacing this attribute with immutable storage is not a drop-in change.

Freeze only an already-final value at an explicit retention boundary, or use a separately controlled representation. Keep clear/copy/clone/capture/restore and reset/end/close semantics. [AttributeSource.captureState()](https://github.com/hsoliwal/lucene/blob/1169cd2007d37c1e5354fdccf9aa7e4eef93d21a/lucene/core/src/java/org/apache/lucene/util/AttributeSource.java#L294-L310) already deep-copies attributes; retaining that full state plus a duplicate spelling may increase memory.

[TermToBytesRefAttribute](https://github.com/apache/lucene/blob/51190f35a16d2ce433139abfe0fd8365791b352a/lucene/core/src/java/org/apache/lucene/analysis/tokenattributes/TermToBytesRefAttribute.java), [BytesRefIterator](https://github.com/hsoliwal/lucene/blob/1169cd2007d37c1e5354fdccf9aa7e4eef93d21a/lucene/core/src/java/org/apache/lucene/util/BytesRefIterator.java#L24-L34), and DocValues lookups may reuse their buffers. BytesRef.clone and CharsRef.clone are shallow; persistent consumers need deep copies or proven immutable ownership. BytesRef requires heap byte[], and CharsRef requires char[]; native/mapped M3 storage is not a drop-in backing.

[Term(String,BytesRef)](https://github.com/hsoliwal/lucene/blob/1169cd2007d37c1e5354fdccf9aa7e4eef93d21a/lucene/core/src/java/org/apache/lucene/index/Term.java#L46-L65) copies, but [StringField(String,BytesRef,Store)](https://github.com/hsoliwal/lucene/blob/1169cd2007d37c1e5354fdccf9aa7e4eef93d21a/lucene/core/src/java/org/apache/lucene/document/StringField.java#L69-L85) does not clone and requires its value to stay unchanged until indexing completes. A cache must not expose a writable authoritative byte[] to arbitrary consumers.

### Original offsets and token graphs

[OffsetAttribute](https://github.com/apache/lucene/blob/51190f35a16d2ce433139abfe0fd8365791b352a/lucene/core/src/java/org/apache/lucene/analysis/tokenattributes/OffsetAttribute.java#L24-L48) permits endOffset-startOffset to differ from the transformed term length. [StandardTokenizer](https://github.com/hsoliwal/lucene/blob/1169cd2007d37c1e5354fdccf9aa7e4eef93d21a/lucene/core/src/java/org/apache/lucene/analysis/standard/StandardTokenizer.java#L163-L184) uses correctOffset and emits final offset/skipped-position state; [CharFilter](https://github.com/hsoliwal/lucene/blob/1169cd2007d37c1e5354fdccf9aa7e4eef93d21a/lucene/core/src/java/org/apache/lucene/analysis/CharFilter.java) corrects offsets back to the original source.

Keep original UTF-16 source offsets, not UTF-8 byte positions, code-point indexes or M3 storage coordinates. Preserve positionIncrement, positionLength, gaps, type, flags, payloads, keyword/custom attributes and end state. Sharing the same spelling does not merge token occurrences.

[SynonymGraphFilter](https://github.com/apache/lucene/blob/51190f35a16d2ce433139abfe0fd8365791b352a/lucene/analysis/common/src/java/org/apache/lucene/analysis/synonym/SynonymGraphFilter.java#L63-L74) distinguishes query-time graph expansion from index-time flattening and cannot consume arbitrary incoming graphs. A token-spelling cache cannot omit that context. ASCIIFoldingFilter with preserveOriginal may emit another token at position increment zero.

### Encoding, equality and ordering

Lucene [UnicodeUtil.UTF16toUTF8](https://github.com/hsoliwal/lucene/blob/1169cd2007d37c1e5354fdccf9aa7e4eef93d21a/lucene/core/src/java/org/apache/lucene/util/UnicodeUtil.java#L156-L176) emits EF BF BD (U+FFFD) for an unpaired surrogate. M3's [JDK-compatible UTF-8 encoder](https://github.com/hsoliwal/M3jdk21/blob/1de629d26ea82d2729a5993bfc88099f83221923/src/java.base/share/classes/java/lang/M3String.java#L812-L841) emits '?' in that case. Rewriting Lucene conversion to String.getBytes(UTF_8) changes term bytes unless well-formed input is proven or Lucene's exact error policy is preserved. Include surrogate pairs split across atoms and slices retaining one half. Binary terms, collation keys and JNI modified UTF-8 are different domains.

[BytesRef](https://github.com/apache/lucene/blob/51190f35a16d2ce433139abfe0fd8365791b352a/lucene/core/src/java/org/apache/lucene/util/BytesRef.java) uses Lucene's seeded MurmurHash over bytes and unsigned byte order. [CharsRef](https://github.com/apache/lucene/blob/51190f35a16d2ce433139abfe0fd8365791b352a/lucene/core/src/java/org/apache/lucene/util/CharsRef.java) uses the Java UTF-16 hash but equals only CharsRef; [M3Text.equals](https://github.com/hsoliwal/M3jdk21/blob/1de629d26ea82d2729a5993bfc88099f83221923/m3/ports/indexstring/src/main/java/com/m3/text/compat/M3Text.java#L101-L124) equals only M3Text. Matching hashes do not prove equality or interchangeable map keys. UTF-16 String order and UTF-8/scalar order differ for some BMP/supplementary pairs, so dictionary/FST ordering must match the selected symbol domain.

M3 owner identity alone does not identify a slice: use owner plus exact range and the relevant namespace/generation. Process-local atom IDs, persisted dictionary IDs, batch ordinals, Lucene segment/reader-specific term ordinals and DocValues ordinals remain different identities. Merges, refresh and reopen may change ordinal mappings. Never serialize an M3 address/ID in place of Lucene term bytes or codec ordinals.

### Transformation and search semantics

[LowerCaseFilter](https://github.com/hsoliwal/lucene/blob/1169cd2007d37c1e5354fdccf9aa7e4eef93d21a/lucene/core/src/java/org/apache/lucene/analysis/LowerCaseFilter.java) uses CharacterUtils code-point lowercasing; it is not a blanket rewrite to locale-sensitive String.toLowerCase. [ASCIIFoldingFilter](https://github.com/hsoliwal/lucene/blob/1169cd2007d37c1e5354fdccf9aa7e4eef93d21a/lucene/analysis/common/src/java/org/apache/lucene/analysis/miscellaneous/ASCIIFoldingFilter.java#L104-L156) already skips unchanged ASCII. [ICUNormalizer2Filter](https://github.com/apache/lucene/blob/51190f35a16d2ce433139abfe0fd8365791b352a/lucene/analysis/icu/src/java/org/apache/lucene/analysis/icu/ICUNormalizer2Filter.java) already quick-checks and defaults to NFKC casefold. Normalization may interact across joined seams; independently normalizing pieces and concatenating is not generally equivalent.

Cache only a known deterministic operation under a stable analyzer/configuration/resource/model/Unicode-data version. Analyzer object identity alone does not prove immutable resources. Analyzer.normalize is a one-token API, not full analysis or synonym-graph processing.

[FuzzyAutomatonBuilder](https://github.com/hsoliwal/lucene/blob/1169cd2007d37c1e5354fdccf9aa7e4eef93d21a/lucene/core/src/java/org/apache/lucene/search/FuzzyAutomatonBuilder.java#L48-L56) uses code points plus prefix/transposition options. Portable M3 edit-distance plans documented in the [indexed-string reference](indexed-string-reference.md) use UTF-16 units. They are not interchangeable. Existing Lucene automata remain the exact oracle; approximate hashes/signals never establish acceptance. CompiledAutomaton is not a blanket immutable or cross-thread-safe value: it exposes byte views, may retain a supplied binary Automaton, uses mutable transition scratch and may cache NFA execution state. Reuse requires explicit ownership and threading proof for the exact object graph; otherwise confine or reconstruct it.

## 5. Existing optimizations and retention budgets

The baseline already contains Analyzer component reuse, mutable token/byte builders, CharArrayMap lookup without String conversion, BytesRefHash byte-block pooling, FST suffix deduplication, compiled synonym FSTs, fuzzy automaton sharing, prefix-coded TermInSetQuery data/cached hashes, and deduplicated Sorted/SortedSet DocValues dictionaries.

Prefer bounded per-atom/range facts or finite vocabulary batches over all-pairs caches. Include entry count, byte count, token count, largest token, duration, negative-cache entries and cache metadata in the budget. A high-cardinality ID/log corpus must have a semantics-preserving bypass.

[M3StringPool](https://github.com/hsoliwal/M3jdk21/blob/1de629d26ea82d2729a5993bfc88099f83221923/src/java.base/share/classes/java/lang/M3StringPool.java#L34-L51) already uses weak native owners, native-byte admission limits and tuple-retention limits. Refusal can follow queue processing, bounded backoff and GC, then preserve the flat path. External strong caches can keep these owners alive; a small range can retain a large owner/mapping. Weak interning is not a whole-process memory bound.

Necessary heap exports are explicit, bounded compatibility projections, not another unbounded canonical spelling store. Measure copies and JNI transitions. Bulk traversal or batch preparation may amortize boundaries, but no public M3 native export or allocation-free operation is implied by this proposal.

## 6. Small future recipe/prototype

The proposed files below belong in an external test-only application/recipe fixture, not Lucene core or java.base. **They are not implemented.**

| Proposed artifact | Role and refusal boundary |
| --- | --- |
| TokenSnapshotFixture.java | Fixed corpora, exact analyzer/resource configurations, complete token-trace oracle |
| CanonicalTermBatch.java | Ordinary immutable String spellings plus a finite dictionary and per-occurrence metadata; batch IDs remain local; budget refusal preserves original behavior |
| ReplayTokenStream.java | Copies retained spelling into reusable CharTermAttribute and restores metadata/end state; unknown attributes use existing capture/restore or refuse compact mode |
| LuceneBoundaryDifferentialTest.java | Original/replay trace and index/query parity on stock and matched M3 runtimes |
| LuceneBoundaryBenchmark.java | Separate collect/admit, replay, normalize, encode, full indexing and query measurements |

The first snapshot may use a copied ordinary String from the final term buffer. This intentionally measures M3's public String boundary without exposing M3String. Do not retain the mutable input array. Replay may use getChars into resizeBuffer plus setLength, with the cost measured.

Compare equivalent supported attributes against (a) original streaming when retention is not needed, (b) CachingTokenFilter, (c) ordinary copied String records, and (d) existing public Lucene dictionary/pool approaches where appropriate. Do not claim a win by omitting new snapshot/preparation work or dropping attributes.

A second prototype may reuse a known pure Analyzer.normalize result or prepare a repeated vocabulary's Lucene-compatible UTF-8 bytes. Key exact text/range, field, config/resources/model generation, Unicode/ICU version and encoding policy. Do not memoize arbitrary analyzers. Public mutable output boundaries require copies unless consumer ownership is proven.

A future narrow recipe must pin eligible donor expressions/license, the exact Lucene release preimage, target postimage and fixtures; verify compile/behavior, fixed point/idempotence, refusal and rollback; and preserve existing canonical recipe ownership. This is not a bulk String-to-M3String replacement. The [Lucene contribution policy](https://github.com/hsoliwal/lucene/blob/1169cd2007d37c1e5354fdccf9aa7e4eef93d21a/AI_POLICY.md) is separate: no Lucene contribution is authorized or performed by this reference.

## 7. Required tests and benchmark decision

| Gate | Required evidence |
| --- | --- |
| Runtime/version | Exact Lucene jars/source, M3 Java/HotSpot/JNI identities and flags, JDK build, hardware, corpus hashes; stock interpreter and matched flag-off controls |
| Mutation/lifecycle | reset/end/close, empty streams, reuse, mutated original arrays, downstream buffer writes, payload reuse, custom attributes, clone/capture/restore, final state |
| Text/encoding | ASCII/Latin-1/BMP/supplementary text, NUL, isolated/reversed surrogates, joined/sliced surrogate seams; exact Lucene term bytes and malformed-input behavior |
| Normalization/graphs | Combining seams, Turkish I, sigma, ß, ligatures/fullwidth/default ignorables, unchanged ASCII, CharFilter changes, stop gaps, preserveOriginal folding, multi-token synonyms and multi-value field gaps |
| Equality/order/IDs | Hash collisions, equal spelling/different owner, same owner/different range, map-key symmetry, U+E000 versus U+10000 order, binary terms, reader/segment merge/reopen/close |
| Index/query parity | Terms/order, frequencies, postings positions/offsets/payloads, term vectors, stored fields and DocValues, phrase/fuzzy/synonym results and comparable scores; unmodified matching Lucene can read output |
| Budget/failure | Tiny/zero admission budget, cache eviction/churn, oversized/high-cardinality tokens, supported fallback/native absence, reclamation and worker teardown; no output truncation or silent metadata loss |

Run deterministic fixtures and randomized/property cases with recorded seeds. Exact index-file bytes may differ because of segment identifiers/metadata; compare semantic content and codec readability rather than assuming byte-for-byte equality. Finite tests do not prove universal correctness.

Measure cold preparation, warm hits, misses, eviction and high-cardinality bypass separately across token sizes/scripts, scalar/joined/sliced M3 values, batch sizes and worker counts. Report CPU/throughput, latency distribution, allocations, retained heap, GC frequency/pause/CPU, native/direct/mapped bytes, RSS/PSS, JNI transitions, copied/materialized bytes, admissions/refusals and cache entries/hits/evictions. Measure peak dictionary-build memory and release after batch/reader close.

Use JFR only in supported runtime lanes; enabled-M3 measurements require supported counters, GC logs, native/process tools or instrumentation. Do not treat lower heap as lower total memory or one fast microbenchmark as faster indexing/querying. Keep Analyzer/TokenStream scratch thread-confined.

Set acceptance budgets and regression limits before interpreting results. Accept only the measured workload regime after correctness and total-resource gates pass; scope or refuse optimization when unique inputs, retention, JNI overhead or tail latency worsen. **Performance benefit, runtime qualification and allocation-free/zero-copy behavior remain unestablished.**

## 8. Licensing and scope

Preserve Lucene [LICENSE](https://github.com/hsoliwal/lucene/blob/1169cd2007d37c1e5354fdccf9aa7e4eef93d21a/LICENSE.txt), [NOTICE](https://github.com/hsoliwal/lucene/blob/1169cd2007d37c1e5354fdccf9aa7e4eef93d21a/NOTICE.txt) and per-file ASF headers for copied expression, independent ICU/resource/model notices, M3/Synexia original attribution and OpenJDK per-file rights. A recipe license does not relicense inputs or generated postimages. Abstract algorithmic ideas are distinct from copied expression.

This focused proposal does not claim complete Lucene package/member coverage, a new integration implementation, index-format changes, donor qualification, or whole-M3 runtime acceptance. The [source ledger](lucene-string-opportunity-source-coverage.md) records the exact inspected identities and remaining execution gates.

