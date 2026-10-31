<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# OpenNLP × M3String: source-pinned opportunity map

Status: research and candidate design only, inspected 2026-10-15. In this documentation authoring/review pass, no OpenNLP/M3 source, configuration or policy changed and no Java, Maven, model training, tests, benchmarks, recipe, build or repository program was run. Hosted CI results, if reported separately for an exact published commit, are not local runtime qualification. No speedup, reduced memory or delivered integration is claimed.

## 1. Recommendation

Start with a matched-runtime experiment on unchanged OpenNLP binaries, then a span-first application harness that materializes one token `String[]` per sentence. Prioritize repeated substring/feature construction and dictionary hashing only after profiling that baseline. Keep inference, feature schemas, model vocabulary, normalization and adaptive document state in a versioned NLP layer. They are not intrinsic properties of a string.

The useful surface is real: classic tokenizer features repeatedly construct prefixes/suffixes; POS and chunking concatenate token/context labels; dictionary lookups construct `StringList`; model evaluation looks up feature strings. But these observations identify work to measure, not evidence that M3's shared owners are faster. Shared slices can increase retained memory, especially under OpenNLP's default strong interner.

## 2. Exact baselines and policy

| Baseline | Evidence | Meaning |
| --- | --- | --- |
| OpenNLP fork main | [4713d82577dd3e9e48cd9855ccd891c2739324a4][on-tree] | Public 3.0.0-SNAPSHOT, modular API/runtime layout; source reviewed here |
| Existing fork documentation | [OpenNLP draft PR #3][on-pr], head `2607b298af01371d822b33615fdc6803106cfeef` | Open/draft when checked; package inventory and integration contracts, not executable qualification |
| M3 documentation branch | [M3jdk21 PR #902][m3-pr], head `1de629d26ea82d2729a5993bfc88099f83221923` | M3 authority/source pin for this map |
| Already received M3 NLP tool pack | [OpenNLP provenance][m3-provenance], upstream `8c586ddebd84db4d66582aab3a885bf02c75e41f`, version 2.4.0 | Distinct from this 3.x fork; 729 source files declared in its receipt, not revalidated here |
| Existing recipe overlay | [2.4 StringList overlay][m3-overlay], blob `c0676946821ca6d7d01bdc11a7e0f1ff4f29df34` | Lazy, volatile hash fact for the exact base class; subclasses retain live behavior |

The inspected 3.x fork [root POM][on-build] declares Java 21 and compiler release 21, with a Java-version Enforcer lower bound of 21 (blob `d2691ca988b085b8d3bd7fdeb59b4fced490aaa3`). This establishes the proposed JDK 21 build baseline, not successful compilation or matched-M3 execution.

The [M3 tool naming map][m3-tool-map] explicitly maps the OpenNLP 2.4.0 tools to their original API/module identity and identifies `opennlp.tools.util.StringList` as an internal optimization owner. Its [receiver README][m3-receiver] describes full source builds and exact recipe overlays, not runtime bridge adapters. The [source receipt][m3-receipt] records previous local Linux Java 21 qualification, while full JDK/jtreg, full upstream suites, full Synexia reactor/admission, other platforms and an existing mixed-literal String CI failure remain pending in that receipt. These are repository-reported historical results; this research did not rerun them or establish their current CI state.

The same naming map keeps lexical/word/bigram/semantic images in separate lexicon/language/relation planes with namespace, generation and provenance. It does not authorize an NLP prediction cache on `java.lang.String`. [M3 policy][m3-policy] requires reusable recipes and proof custody in Synexia, exact pre/postimages, refusal/fixed-point tests and target qualification. The ordered receiving phase remains String qualification; this map advances no phase.

[OpenNLP contribution policy][on-contributing] prefers code-point scans and `StringUtil` to production regex, with narrowly documented user-pattern exceptions. Preserve this policy and its exact existing semantics: the classic token context generator still deliberately uses `charAt` features, so a supposedly cleaner code-point rewrite is not equivalence-preserving. The [PR checklist][on-checklist] calls for tests and license review for code changes. No such change is proposed for publication here.

## 3. Accessibility and compatibility tracks

`java.lang.M3String` is declared `final class M3String implements CharSequence`, without `public`, at [the source declaration][m3-class]. Its instance state is canonical owner plus packed coordinate. It is not an application-importable class or a proposed replacement for OpenNLP's public `String` type.

1. Custom-runtime track: unchanged OpenNLP sees ordinary `java.lang.String` and `String[]`. In the inspected M3 runtime, [String.substring][m3-substring] creates a new String wrapper over an owner/range slice for a nonempty proper substring when the receiver is M3-backed; otherwise it retains the flat path. Full-range requests return the receiver and empty ranges return the empty String before backed slicing. [M3String.slice][m3-slice] preserves the owner and adjusts the coordinate. This supports a hypothesis of less payload copying while still allocating wrappers, arrays and feature objects. Admission, readiness, budgets, flat fallback, compiler lowering and target runtime qualification remain separate conditions. Source presence is not an ABI/runtime certification.
2. Stock-JDK track: an application can preserve immutable source text plus UTF-16 spans, or implement its own explicitly named `CharSequence` view. Such a view is not M3String and cannot be passed where OpenNLP requires String. Materialize ordinary strings at the documented boundary. `SentenceDetector.sentPosDetect(CharSequence)` accepts a view but its implementation/context generators can still materialize internally. Do not promise end-to-end zero-copy.
3. Source-adaptation track: changing context generation, lookup structures or model encodings requires a separately authorized, source-sealed recipe and exact behavior tests. Reuse existing owners; do not introduce a second string payload store or expose bootstrap internals. Reconcile the 2.4 receiver before backporting any idea from the 3.x fork.

## 4. Ranked compatibility matrix

Rank is research priority, not a predicted speedup. “Unchanged API” does not mean implementation qualified.

| Rank / candidate | Exact boundary and existing work | Custom M3 runtime | Stock JDK / explicit bridge | Main acceptance gate |
| --- | --- | --- | --- | --- |
| P0: seal baseline/version split | Existing 2.4 StringList overlay versus 3.x cached hash | Benchmark exact existing receiver separately | No substitution of 3.x APIs into 2.4 harness | Pin tool/model/runtime, preserve original API/module and hash semantics |
| P1: sentence/token spans and lazy extraction | [SentenceDetector][sentence-api], [Tokenizer][token-api], [Span][span]; String results use `getCoveredText(...).toString()` | M3-backed substring may share storage behind normal String ABI | Source + spans remains application metadata; Tokenizer still takes String | Same UTF-16 half-open spans, strings, types and probability results; retention measured |
| P1: token/context substring and concat costs | [TokenizerME][token-me] creates whitespace-token substrings; [DefaultTokenContextGenerator][token-context] builds `p=`, `s=`, neighboring-char features at split positions | Existing String slicing/concat is the first experiment | Full-string API still requires materialization; source recipe is later work | Exact ordered feature strings, duplicates, char-unit index behavior, model probabilities |
| P2: dictionary/repeated query strings | [Dictionary][dictionary] uses StringList; [StringList][string-list] already interns tokens and caches its hash | Potential reuse of String hash/equality and slice payload | Public API unchanged; avoid a redundant interner and full-document retention | Dictionary case rules, exact hash behavior, mutation/revision boundary and subclass behavior |
| P2: text-only lexical feature fragments | [DefaultPOSContextGenerator][pos-context] prefix/suffix, hyphen, ASCII uppercase/digit; [TokenFeatureGenerator][token-feature] simple lowercasing | Intrinsic exact character facts may help existing operations | Bounded external cache or generator adapter only after contract proof | Distinguish ASCII/BMP/code-point semantics; exact features before decode |
| P2: normalization reuse with alignment | [StringUtil][string-util], [CaseFold][case-fold], [FullCaseFold][full-fold], [AlignedText][aligned], [Alignment][alignment] | Shared original/derived text is plausible; no generic lower-case fact substitution | Existing aligned APIs usable; transformed String boundary explicit | Exact normalizer sequence/options/tables and original-offset mapping; measure alignment arrays |
| P3: feature vocabulary/model labels | [AbstractModel][model], [GISModel][gis] map feature strings to parameters; [WordpieceTokenizer][wordpiece] owns immutable vocabulary copy | Repeated spelling/hash is a substrate candidate | Model-local symbol/ID adapter needs separate implementation and model equivalence proof | Full exact label identity, unknown tokens, feature order/multiplicity and serialization compatibility |
| P3: POS/chunk/name-finder context fragments | [POS context][pos-context], [chunk context][chunk-context], [name context][name-context] | Can share lexical substrings, not context-dependent results by token alone | Context keys include neighboring tokens/tags/prior outcomes and generator config | Exact full context, sequence scores and labels under fixed model |
| Excluded from string substrate: final inference/adaptation | [NameFinderME][name-update], [PreviousMapFeatureGenerator][previous-map] update state after prediction | Keep model/context/document state outside M3String | Application/session owner with explicit lifecycle | No cross-document or cross-thread adaptive-state leak; no blanket precompute/GPU claim |

## 5. Concrete semantic boundaries

### Spans, immutable ownership and materialization

`Tokenizer.tokenizePos(String)` and `SentenceDetector.sentPosDetect(CharSequence)` return text offsets. `Span` is a generic half-open interval; its coordinate system comes from the API. For these Java text APIs, indices address UTF-16 code units. `TokenNameFinder.find(String[])` and chunk-as-spans use token indices instead. Never equate those with byte, code-point, grapheme or text-character offsets.

Keep sentence base offset and token-relative spans. For a nonempty entity token interval `[a,b)`, map to `[sentenceBase + tokenSpans[a].start, sentenceBase + tokenSpans[b-1].end)`. Preserve any gaps between tokens. If tokenization ran on normalized text, compose with the exact alignment to the original, rather than adding offsets across a changed-length transform. Validate empty/out-of-range spans separately.

Span-only calls save final result extraction, not all allocation. TokenizerME still creates `tok = d.substring(...)` and evaluates String[] features; sentence detection scans candidates and builds contexts. Return value wrappers and original/derived alignment metadata also cost memory. Mutable input CharSequences must be frozen or rejected by the application harness before sharing; public CharSequence is not an immutability promise.

WordPiece is a separate contract: [tokenizePos][wordpiece] always throws UnsupportedOperationException. The implementation adds special/continuation tokens, isolates punctuation, creates a char array, uses longest-match vocabulary candidates, limits code-point length and collapses a partially unmatched word to unknown. Do not fabricate original-text spans for these tokens or replace this with classic tokenization. BPE/ONNX tensor paths need their own model/tokenizer-specific study; no byte projection was needed to justify the classic token work here.

### Feature identity and inference

DefaultTokenContextGenerator emits full prefix/suffix labels at each UTF-16 split index and character-class labels. DefaultPOSContextGenerator emits up to four prefixes and suffixes plus lexical/context labels; short tokens repeat some fragments. DefaultChunkerContextGenerator produces word, tag and prior-prediction conjunctions. DefaultNameContextGenerator mixes generator output with previous outcomes, and [NameFinderME][name-update] updates adaptive data after decoding. Preserve the ordered String[] exactly, including separators, repeated entries, empty fragments and previous outcomes; an unordered set, approximate fingerprint or ambiguous joined key is insufficient.

GISModel resolves each feature through `pmap.get(context[i])`, then runs prior/scoring. Saving lexical construction does not precompute the whole model evaluation. Numeric feature values, outcome order, model parameters, prior, beam size, validation and tie behavior stay authoritative. Any future integer-label shortcut must prove the same unknown-feature handling and accumulation order, and retain the original serialization format unless separately authorized.

### Three different “lowercase” operations

- `StringUtil.toLowerCase(CharSequence)` maps `Character.toLowerCase` over code points and constructs a String. It is context-free simple mapping, used by token features and StringList hashing
- `CaseFoldCharSequenceNormalizer` calls `text.toString().toLowerCase(locale)`, default Locale.ROOT. This can use context-sensitive and expanding mappings
- `FullCaseFoldCharSequenceNormalizer` uses bundled Unicode C/F full mappings, excluding S/T; its inspected resource is CaseFolding-17.0.0, git blob `a0b0f07fd64598e698fc7da3e55ca72077dd6071`. It implements offset-aware normalization

The existing [locale test][locale-test] demonstrates a model-label distinction at Greek final sigma. SentenceDetectorME's abbreviation index explicitly applies simple mapping to a bounded window and warns that full String lowercasing would corrupt offsets. Do not replace these paths with one ASCII-folded or locale-neutral “normalized atom.” Length-preserving output alone is not semantic equivalence.

[WhitespaceMode][whitespace] is a process-wide setting used by StringUtil and classic token features; its source says the mode is not stored in the model. Pin it before component use and record it alongside model provenance. Do not switch it while decoding. Current tests distinguish U+0085 and U+001C. SentenceDetectorME's whitespace trimming instead explicitly uses its Unicode CharClass. Cache the actual method's semantics, not a generic whitespace label.

Raw UTF-16 slices must preserve lone surrogates as well as valid pairs. Classic tokenizer `charAt` features can see individual surrogate units; preserve the oracle even if a later code-point-aware algorithm is desirable. If a future external bridge encodes bytes, separately key charset, malformed/unmappable action and replacement bytes. Never infer encoding equivalence from raw-string equality; no UTF-8 fast-path replacement is admitted by this map.

### Existing caches and retained memory

- StringList constructors already copy their token array and intern each token through StringInterners. Default [CHMStringInterner][interner] holds strong keys and values with no visible bound/eviction, and [StringInterners][interners] owns the selected interner statically. New one-off dictionary query strings therefore enter that retention path. Under shared backing, retaining a tiny String can retain a large atom/tuple document owner. This is a required adversarial benchmark, not a measured leak claim
- Current 3.x StringList caches `StringUtil.toLowerCase(String.join(",", tokens)).hashCode()` in a boxed field. The existing 2.4 overlay caches the lowercased `toString()` result including brackets, uses a primitive volatile presence bit and bypasses caching for subclasses. Neither algorithm is a drop-in preimage for the other. Do not propose “add hash caching” as new work without version-specific gap proof
- [CachedFeatureGenerator][feature-cache] has a per-thread cache, size 100, indexed by token index within one String[] object; switching array identity clears it. Recreating equal arrays destroys hits. Reusing and mutating an old array is unsafe. Previous outcomes and adaptive revisions are not part of this wrapper's cache key, so only the configured generator's proven invariants justify reuse
- [BeamSearch][beam] has a separate optional per-thread context cache and score buffer. Its keys are String[] and the inspected [Cache][cache] is a plain LinkedHashMap subclass, with no array-content equality wrapper. Do not silently convert identity keys to content keys. Cache's Javadoc says LRU, but the constructor does not request access order; record actual behavior instead of assuming access-order eviction
- SentenceDetectorME already builds an immutable abbreviation index and bounds its probe window by entry lengths. Benchmark against that implementation, not a historical whole-document scan
- Alignment stores two int arrays indexed by normalized UTF-16 length. Alignment memory can dominate the shared spelling benefit; include it in retained-heap measurements

Each candidate cache needs an entry/byte cap, maximum admitted key/owner size, exact collision verification, observed hit/miss/refusal counts, and a lifecycle. For externally held views, record backing size versus live-slice size. A weak key whose value references the same owner is not a retention solution. `new String(existing)` does not establish independent storage. Any detachment policy requires verified runtime semantics and cost; otherwise refuse caching large-backing slices and retain the ordinary noncached path.

## 6. State and cache-key ownership

Separate these keys conceptually; do not force all metadata into M3String:

| Owner | Required identity and options | Invalidation / sharing |
| --- | --- | --- |
| Generic text fact | exact immutable UTF-16 content/range; owner generation if internal owner identity is used; operation semantics | String/owner lifetime; no language/model fields; hash collisions require exact equality |
| Normalized projection | source/range + ordered transform IDs/options + locale + JDK Unicode behavior + bundled table digest | New key for changed transform/data/runtime; retain exact alignment; bounded external owner |
| Lexical fragment | normalized/raw identity + feature schema/revision + relevant character/whitespace policy + dictionary revision if consulted | Model-independent only where proved; no adaptive outcomes hidden inside |
| Model predicate mapping | exact feature labels and optional numeric values + model binary digest + outcome/predicate table version | Model-lifetime scope; never reuse IDs across models |
| Full context / prediction | token sequence and position + relevant neighbors/tags/prior outcomes + model/config/additional context + adaptive session epoch | Usually document/session or invocation scope; token-only key forbidden |

These are proposed keys, not implemented APIs. On stock JDK, internal M3 owner coordinates are unavailable. Use exact immutable application-owned identity and validated equality instead of reflection or illegal java.lang access. Preserve current array identity semantics for existing OpenNLP caches.

This 3.x snapshot documents thread-safe ME instances with per-thread state; that is not a universal promise for 2.4, custom factories or mutable dictionaries. Obtain `probs()` on the same thread immediately after its computation, before the next invocation replaces last-result state. NameFinder adaptive state is logically document-scoped even where stored in ThreadLocal: begin/end a document on the owning worker, clear adaptation in `finally`, and avoid migrating a partial document across workers. [NameFinderME.clearThreadLocalState][name-me] explicitly does not clear every beam/feature-generator ThreadLocal; lifecycle cleanup must cover the real object graph rather than claiming one method removes all state. Do not share mutable per-call token/context arrays merely because the model is shareable.

## 7. Next bounded work and qualification

The bounded plan below defines a no-source-change prototype, recipe refusal conditions, differential corpus and matched-runtime benchmark design. The first go/no-go is exact source/API/model compatibility and a reproducible current baseline. Choose one hotspot only after that evidence.

This document is a candidate M3-side design reference. Publication or source work is a separate decision. The companion [source coverage](opennlp-string-opportunity-source-coverage.md) records exact paths/blobs and bounded coverage. Research source copies retain their original ASF headers, LICENSE and NOTICE; they are not a production import. No model binary or licensed corpus was downloaded. M3 source is cited, not copied into an Apache file; its per-file OpenJDK/GPL/Classpath and other rights are unchanged.

Design only. All compilation, runtime, unit/differential/corpus tests, JMH and target qualification below are NOT RUN in this documentation authoring/review pass. Hosted exact-commit CI must be reported separately and does not imply these proposed experiments ran. Proposed controls are benchmark configuration, not changes made to an application.

## A. First prototype: unchanged OpenNLP, explicit extraction boundary

Build an isolated application harness later, with no OpenNLP source modification and no `java.lang.M3String` import. Use already supplied, checksummed model files rather than language constructors that may download defaults. Record source/model/dictionary license and provenance before admitting data.

1. Freeze one raw Java String per input document. Run the supplied sentence detector's span API. Keep original UTF-16 coordinates, not a reconstructed string formed by joining sentences
2. For each sentence, obtain the ordinary String needed by Tokenizer; on M3 that may use its existing backed substring path, on stock JDK it is a normal extraction. Call tokenizePos once; retain sentence base offset and token spans
3. Materialize `String[]` once when POS, chunking or name finding first needs it, using the exact original spans. Reuse that same immutable-by-convention array through related calls; do not rebuild equal arrays and accidentally invalidate identity caches. Do not expose a mutable cached array to unrelated callers
4. Run unchanged models/generators. Capture their probability result immediately on the computing thread. Preserve document order and reset adaptive state at the original document boundary in a finally block
5. Return raw spans and annotations. Release document-owned views/arrays at the end of their required lifetime; separately account for model/interner/adaptive references that outlive them

Provide two modes: span-only consumer, and full String[] NLP pipeline. Compare span-only only against an equivalent span-only baseline; do not report avoided POS/NER work as a string optimization. A duplicate-extraction control may demonstrate a better application pipeline, but the main stock-versus-M3 comparison must use the same optimized harness and same OpenNLP artifact.

Normalization is off by default in this first prototype. Add a second explicit aligned-normalization fixture only when the model requires that exact preprocessor. Use `OffsetAwareNormalizer`/`AlignedText` on the 3.x lane where supported; do not substitute the feature lowercasing rule or assume these APIs exist in the 2.4 receiver.

## B. Recipe candidates after baseline evidence

The recipe owner remains the existing Synexia convergence workspace/crate. This is a candidate plan, not a new competing implementation or a request to apply a recipe.

| Candidate | Narrow potential delta | Refusal / fallback conditions |
| --- | --- | --- |
| B1: lexical fragment reuse | Cache only an exact generator's proven context-independent prefix/suffix or simple-case fragments outside the string substrate | Unknown/custom generator; changed source blob/signature; missing transform/Unicode/whitespace key; mutable input; oversized backing/key; low-reuse/high-cardinality workload |
| B2: dictionary lookup work | Reuse an already immutable query/lexical result or exact local hash fact, without changing dictionary equality, array ownership, existing interner behavior or public surface | Dictionary can mutate without a revision/snapshot; unproved case/equality/hash behavior; subclass overrides; implicit whole-document retention; redundant work already removed in chosen baseline |
| B3: model predicate strings | Share exact immutable labels within a fixed model lifetime or investigate a model-local lookup accelerator | Different model digest/outcome schema; approximate-only equality; changed unknown-feature behavior; change to accumulation order, numeric values, serialized format or beam semantics |

Keep these independent of a 3.x migration and independent of enabling/disabling existing caches. An adapter's normal refusal returns the original operation; do not swallow unexpected failures or change existing null/exception behavior. No blanket dictionary precompute, process-global unbounded token registry, final prediction cache or GPU layer is justified by this source map.

For any authorized implementation later, require: exact donor/target source seals; per-file license and change notices; attributed syntax rather than textual search/replace; protected public/protected API and module identity; original source as oracle; deterministic recipe output; second application/fixed point; preimage drift refusal; no changes to unrelated literals/comments; and rollback by restoring the original path. Keep declaration/recipe success separate from actual target runtime acceptance. Existing M3 policy/build checks are read instructions here, not executed receipts.

## C. Differential tests required before a performance claim

1. Text and offsets: empty/blank input, one token, long words, mixed punctuation, repeated abbreviations, CR/LF/CRLF, leading/trailing whitespace, keepNewLines settings, BMP/non-BMP text, combining marks, ZWJ emoji, NUL, valid pairs and each lone surrogate. Test nested slices and supplementary pairs split at view/tuple seams. Compare start/end/type and probability fields individually, plus every extracted UTF-16 code unit. Do not rely on Span.equals alone or lossy UTF-8 serialization of surrogate fixtures
2. Normalization: Turkish/Azeri I, U+0130, Greek final sigma, sharp-s, ligatures, decomposed/precomposed accents, expansions/collapses/deletions, and composition of alignments. Compare the exact selected operation, never compare all “lowercase” APIs as if equivalent. Record table SHA, JDK/Unicode implementation and transform order. Test empty normalized spans and deletions at both boundaries
3. Whitespace/model policy: run separately under pinned UNICODE and LEGACY settings, including U+0085/U+001C, NBSP and other relevant separators. No concurrent mode switching. Ensure model metadata supplied by the harness records the training policy when the binary itself does not
4. Feature identity: for each candidate context and previous-outcome sequence, compare String[] lengths, ordered contents and duplicates before scoring. Include short-token repeated affixes, absent/present dictionary words, ambiguous conjunctions, prefixes with delimiters and unknown features. No token-array mutation during a decoding operation
5. Predictions: fixed model bytes, configuration, prior, beam/validator, input order and adaptive reset schedule. Compare tokenizer/sentence spans, POS/chunk/name labels and top-K order. Require exact probabilities where arithmetic order/runtime should match; if cross-runtime math needs a tolerance, declare it before testing, report maximum error, and still require identical decisions/tie behavior. Quality scores alone are insufficient for a semantics-preserving substrate change
6. State/thread behavior: shared versus per-thread ME instances for the exact version's supported contract; repeated documents A/B; document reset; ordered sentences within one document; same contents in different arrays; reused unmodified array; explicit cleanup on worker exit. Verify a tenant/document does not inherit another's adaptive map. A multi-thread-safe class does not make adaptation migrate with a logical request
7. Lookup/model lifecycle: dictionary mutation or snapshot changes, model reload, unknown labels, cache-disabled versus enabled, eviction/refusal, key collisions with unequal spellings, equal spellings in distinct owners, large-owner tiny slices, and hot model labels mixed with unique query tokens. Retain current identity cache behavior. Include overridden StringList methods for the 2.4 overlay's subclass fallback
8. Runtime qualification: applicable public String tests, M3 admission/refusal and mixed flat/backed paths, the currently supported interpreter lane, compact-string settings, GC/native owner lifetime and JNI/serialization boundaries where exercised. C1/C2 and other disabled facilities remain separate unsupported/pending lanes, not requirements claimed runnable with active M3 storage. Passing an application corpus cannot substitute for those target gates

Reuse the pinned existing tokenizer-context/locale/cache tests and JMH sources listed in the source coverage page, but audit their actual assertions/configuration before using them. Their presence is not proof of passing, current compilation or comprehensive model accuracy.

## D. Benchmark design and gates

Separate four baseline lanes: (1) original OpenNLP 2.4 source; (2) existing M3 2.4 overlay; (3) unchanged current 3.x fork; (4) one later candidate against its exact matching lane. For each runtime comparison hold library artifact and harness constant. Do not combine library migration, algorithm change, cache tuning and runtime change into one speedup number.

Record exact JVM distribution/commit, M3 activation and admission evidence, flags, CPU/OS, cores, memory limits, GC, artifact hashes, model/dictionary hashes and corpus seed/license. If the required matched M3 image or model is unavailable, mark the comparison BLOCKED rather than simulating M3 on stock JDK.

The [current M3 runtime integration gate](https://github.com/hsoliwal/M3jdk21/blob/1de629d26ea82d2729a5993bfc88099f83221923/m3/runtime-integration/README.md) says UseM3StringStorage is default-off, forces interpreter mode, and excludes compiled mode, CDS, JFR, JVMCI and String deduplication while active. Therefore compare stock JDK 21 with -Xint, the matched M3 image with the flag off in interpreter mode, and that image with the flag on. Keep ordinary JIT-enabled stock/M3-flag-off results as separate product-context references, never causal estimates of the storage change. Do not enable JFR or unsupported deduplication in the active lane; use only supported external/process/GC measurements and disclose missing allocation visibility.

Measure layers separately: raw extraction; exact feature construction; dictionary/vocabulary lookup; fixed-model scoring; full document pipeline; model loading/startup. Existing TokenizerMEBenchmark compares fresh/per-thread/shared instances; SentenceDetectorMEAbbreviationBenchmark already has a bounded-window baseline; StringDeduplicationBenchmark varies cardinality. Use those as design references and keep training/loading out of steady-state measurements while reporting them separately as cold costs.

Workloads must include repeated vocabulary and fresh high-cardinality strings; short and long tokens/documents; ASCII/Latin-1, CJK and supplementary text; punctuation-heavy tokens and large abbreviation dictionaries; tiny retained token slices from large distinct documents; warm cache reuse and churn beyond capacity. Run cold process, first request and steady-state separately. At minimum use independent JVM forks, sufficient warmup, consumed results/checksums and confidence intervals. Report 1-thread and bounded multi-thread contention runs without moving adaptive documents between workers.

Hold cache/interner defaults constant for the main result. Then run isolated controls in fresh JVMs: the supported feature-cache switch, explicit beam-cache size and supported interner choices. StringInterners initializes once, so changing a system property mid-run does not establish a new baseline. Do not introduce or change extension allowlists/security configuration as part of this documentation pass. Never change identity to content-key semantics silently.

Report throughput and latency distribution, CPU/request, allocation bytes/request, heap live set after an equivalent full-GC observation, GC frequency/pause, peak/process RSS and native/mapped memory, cache entries/hits/misses/refusals, owner/tuple counts and retained-backing-to-live-text ratio where supported. Mark unavailable metrics explicitly rather than inventing counters: at this pin, CachedFeatureGenerator.getNumberOfCacheHits() and getNumberOfCacheMisses() throw UnsupportedOperationException. Any later instrumentation needs its own authorized scope and measured overhead. Include alignment arrays, feature arrays, interner tables and cached numeric buffers. Native/off-heap bytes are not free because a heap allocation counter omits them.

Promotion gate: exact differential contract pass; no unexplained prediction drift; reproducible matched-artifact measurements; no unbounded retention; cold and high-cardinality regressions disclosed; source/recipe/license/runtime gates recorded individually. Choose any acceptable latency/memory tradeoff before promotion. A positive microbenchmark alone does not justify production adoption. Refuse the candidate if performance gain relies on a changed model, omitted work or retaining an unbounded number of document owners.


[on-tree]: https://github.com/hsoliwal/opennlp/tree/4713d82577dd3e9e48cd9855ccd891c2739324a4
[on-pr]: https://github.com/hsoliwal/opennlp/pull/3
[m3-pr]: https://github.com/hsoliwal/M3jdk21/pull/902
[m3-class]: https://github.com/hsoliwal/M3jdk21/blob/1de629d26ea82d2729a5993bfc88099f83221923/src/java.base/share/classes/java/lang/M3String.java#L27-L76
[m3-substring]: https://github.com/hsoliwal/M3jdk21/blob/1de629d26ea82d2729a5993bfc88099f83221923/src/java.base/share/classes/java/lang/String.java#L3242-L3260
[m3-slice]: https://github.com/hsoliwal/M3jdk21/blob/1de629d26ea82d2729a5993bfc88099f83221923/src/java.base/share/classes/java/lang/M3String.java#L270-L311
[m3-map]: https://github.com/hsoliwal/M3jdk21/blob/1de629d26ea82d2729a5993bfc88099f83221923/m3/docs/name-mapping.json#L307-L312
[m3-policy]: https://github.com/hsoliwal/M3jdk21/blob/1de629d26ea82d2729a5993bfc88099f83221923/m3/docs/m3jdk21-porting-invariant.md
[m3-receiver]: https://github.com/hsoliwal/M3jdk21/blob/1de629d26ea82d2729a5993bfc88099f83221923/m3/tooling/module-packs/nlp-search/README.md
[m3-receipt]: https://github.com/hsoliwal/M3jdk21/blob/1de629d26ea82d2729a5993bfc88099f83221923/m3/tooling/module-packs/nlp-search/SOURCE_REF.json
[m3-provenance]: https://github.com/hsoliwal/M3jdk21/blob/1de629d26ea82d2729a5993bfc88099f83221923/m3/vendor/third_party/opennlp-tools/PROVENANCE.json
[m3-overlay]: https://github.com/hsoliwal/M3jdk21/blob/1de629d26ea82d2729a5993bfc88099f83221923/m3/vendor/third_party-m3/nlp-search/third_party/opennlp-tools/src/main/java/opennlp/tools/util/StringList.java#L169-L184

[on-contributing]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/.github/CONTRIBUTING.md#L15-L25
[on-checklist]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/.github/PULL_REQUEST_TEMPLATE.md
[sentence-api]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-api/src/main/java/opennlp/tools/sentdetect/SentenceDetector.java#L27-L46
[token-api]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-api/src/main/java/opennlp/tools/tokenize/Tokenizer.java#L45-L62
[span]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-api/src/main/java/opennlp/tools/util/Span.java#L242-L257
[token-me]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-core/opennlp-runtime/src/main/java/opennlp/tools/tokenize/TokenizerME.java#L204-L258
[token-context]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-core/opennlp-runtime/src/main/java/opennlp/tools/tokenize/DefaultTokenContextGenerator.java#L72-L154
[dictionary]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-core/opennlp-runtime/src/main/java/opennlp/tools/dictionary/Dictionary.java#L79-L105
[string-list]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-core/opennlp-runtime/src/main/java/opennlp/tools/util/StringList.java#L77-L89
[pos-context]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-core/opennlp-runtime/src/main/java/opennlp/tools/postag/DefaultPOSContextGenerator.java#L59-L195
[token-feature]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-core/opennlp-runtime/src/main/java/opennlp/tools/util/featuregen/TokenFeatureGenerator.java#L51-L59
[string-util]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-api/src/main/java/opennlp/tools/util/StringUtil.java#L545-L559
[case-fold]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-core/opennlp-runtime/src/main/java/opennlp/tools/util/normalizer/CaseFoldCharSequenceNormalizer.java#L43-L81
[full-fold]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-core/opennlp-runtime/src/main/java/opennlp/tools/util/normalizer/FullCaseFoldCharSequenceNormalizer.java#L28-L121
[aligned]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-api/src/main/java/opennlp/tools/util/normalizer/AlignedText.java#L22-L105
[alignment]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-api/src/main/java/opennlp/tools/util/normalizer/Alignment.java#L24-L88
[model]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-core/opennlp-ml/opennlp-ml-commons/src/main/java/opennlp/tools/ml/model/AbstractModel.java#L38-L84
[gis]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-core/opennlp-ml/opennlp-ml-maxent/src/main/java/opennlp/tools/ml/maxent/GISModel.java#L113-L120
[wordpiece]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-api/src/main/java/opennlp/tools/tokenize/WordpieceTokenizer.java#L185-L286
[chunk-context]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-core/opennlp-runtime/src/main/java/opennlp/tools/chunker/DefaultChunkerContextGenerator.java#L34-L149
[name-context]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-core/opennlp-runtime/src/main/java/opennlp/tools/namefind/DefaultNameContextGenerator.java#L83-L118
[name-me]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-core/opennlp-runtime/src/main/java/opennlp/tools/namefind/NameFinderME.java#L240-L252
[previous-map]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-core/opennlp-runtime/src/main/java/opennlp/tools/util/featuregen/PreviousMapFeatureGenerator.java#L32-L57
[locale-test]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-api/src/test/java/opennlp/tools/util/StringUtilLocaleTest.java#L49-L66
[whitespace]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-api/src/main/java/opennlp/tools/util/WhitespaceMode.java#L26-L101
[interner]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-core/opennlp-runtime/src/main/java/opennlp/tools/util/jvm/CHMStringInterner.java#L33-L48
[interners]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-core/opennlp-runtime/src/main/java/opennlp/tools/util/jvm/StringInterners.java#L25-L67
[feature-cache]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-core/opennlp-runtime/src/main/java/opennlp/tools/util/featuregen/CachedFeatureGenerator.java#L26-L121
[beam]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-core/opennlp-ml/opennlp-ml-commons/src/main/java/opennlp/tools/ml/BeamSearch.java#L224-L257
[cache]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-api/src/main/java/opennlp/tools/util/Cache.java#L24-L40

[m3-tool-map]: https://github.com/hsoliwal/M3jdk21/blob/1de629d26ea82d2729a5993bfc88099f83221923/m3/docs/name-mapping.json#L525-L538

[on-build]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/pom.xml#L249-L252
[name-update]: https://github.com/hsoliwal/opennlp/blob/4713d82577dd3e9e48cd9855ccd891c2739324a4/opennlp-core/opennlp-runtime/src/main/java/opennlp/tools/namefind/NameFinderME.java#L130-L145
