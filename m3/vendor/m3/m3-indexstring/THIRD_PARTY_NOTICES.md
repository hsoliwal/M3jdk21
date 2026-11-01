# Third-party notices

## MIndexString token-preserving transformations

The token-preserving `MIndexString.replace(char,char)` and literal
`replace(CharSequence,CharSequence)` implementations compose existing Synexia resolver metrics,
local interning, immutable token tuples, the KMP search pattern, and the optional direct JNI UTF-16
admission lane. OpenJDK `java.lang.String` replacement behavior is a conformance reference only;
no JDK implementation source is copied. The Rule-22 decision and executed heap/native receipts
are recorded in the repository `FOSS_REUSE_DECISION.tsv` ledger.

The character-replacement fallback for a base `IndexResolver` reuses the literal composer with
single-unit `CharSequence` views rather than materializing the complete source. The indexed
`charAt` cursor supplies partial atoms without resolving a joined spelling; an append-guarded
plain-resolver proof covers the contract. No third-party implementation or dependency was added.

`withAsciiUpperRange` and `withAsciiLowerRange` use the same indexed composition membrane when a
resolver has no encoded range-mask capability. The requested units are validated without a joined
source `String`, unchanged ranges retain their source atoms, and changed overlaps use a local
transformation view. OpenJDK ASCII-range behavior is a conformance reference only; no external
implementation or dependency was copied.

The `Locale.ROOT` lower/upper-case fast path first proves that every indexed UTF-16 unit is ASCII,
then reuses the range transformation membrane. This is an exact ASCII specialization only;
non-ASCII and non-root locale semantics remain on the existing JDK-compatible fallback. OpenJDK
locale behavior is a conformance reference, not copied implementation source.

Partial `MIndexString.substring` composition reuses the existing resolver/native range membrane:
token-aligned slices remain direct tuple views, while cross-token boundaries are admitted as local
views without a temporary joined Java `String`. OpenJDK substring behavior is a contract reference;
no implementation source or dependency was copied.

`substringPositions()` uses the existing indexed UTF-16 cursor and `MatchPositions` accumulator for
plain as well as metric-aware resolvers. It preserves overlap and empty-needle semantics across
token seams without flattening the source. OpenJDK literal-search behavior is a contract reference;
no implementation source or dependency was copied.

`indexOf` and `lastIndexOf` retain their bit-signal prefilter because plain-resolver `charAt` already
streams through indexed UTF-16 units. The append-guarded regression covers bounded origins,
overlapping matches, token seams, and misses; no replacement implementation or donor dependency was
needed for this seam.

`matches(MIndexString)` adds a conservative ordinary-literal fast path using the existing
`contentEquals` cursor. It does not copy a regex engine: backslashes and JDK metacharacters remain
on the existing JDK-compatible materializing fallback. OpenJDK regex behavior is a contract
reference only, and no third-party implementation or dependency was added.

`split(MIndexString,int)` and `splitWithDelimiters(MIndexString,int)` use the existing indexed
`indexOf`/`substring` membrane for non-empty ordinary-literal patterns. JDK limit and trailing-empty
behavior is covered by differential proof; empty and syntax-bearing patterns retain the JDK
`Pattern` fallback. No third-party splitter implementation or dependency was added.

The whitespace boundary operations (`trim`, `strip`, `stripLeading`, `stripTrailing`, and
`isBlank`) likewise use existing indexed UTF-16/code-point iteration and resolver-owned boundary
composition. OpenJDK whitespace semantics are a conformance reference; no implementation source or
runtime dependency is copied, and partial spans remain resolver/native atoms.


## MIndexString fuzzy precompute references

The fuzzy-search superset is Synexia-owned Java/JNI code composed over existing MIndex dictionary,
n-gram, exact-distance, native-trigram and top-K contracts. The following projects were reviewed as
algorithm/reference evidence; no source or challenge-solution body is copied into Synexia.

- `wolfgarbe/SymSpell`, revision `c239062ae02961df18ab7da1671d01b4388204e0`, MIT:
  symmetric-delete candidate generation and dictionary-side precomputation.
- `rapidfuzz/rapidfuzz-cpp`, revision `82662f3623b3ca3645e543f677fc32fb8bd1fb95`, MIT:
  bounded/cutoff fuzzy refinement and batch-oriented distance optimization.
- `apache/lucene`, revision `cec7bdd1a0c07838f38ef8e576f8d9b3deed5416`, Apache-2.0:
  conservative fuzzy candidate/query architecture and exact verification separation.
- `apache/commons-text`, revision `6ef66a612f48f6150654601a17612759329250b6`, Apache-2.0:
  distance/similarity semantics used as a behavioral cross-check.

LeetCode, HackerRank, and GeeksforGeeks are consumed only through the repository's existing
M3 challenge/category catalogue as problem-shape evidence. Their problem statements and solution
implementations are not source donors. Approximate evidence (delete signatures, q-grams, SimHash,
MinHash, Jaccard, Jaro-Winkler, presence bits and native trigrams) is candidate/ranking evidence
only; `MIndexTextDistance.withinCompact` remains the exact admission authority for returned hits.

## OpenRewrite Maven plugin

The CPULLM per-file mechanical stage wraps `org.openrewrite.maven:rewrite-maven-plugin:5.23.1`.
The Maven plugin and OpenRewrite core/rewrite-java engine used by the class-backed Synexia recipe
lane are Apache-2.0. The separately versioned `rewrite-static-analysis:2.25.0`,
`rewrite-migrate-java:3.25.0`, and `rewrite-logging-frameworks:3.21.0` recipe packs are
published under the Moderne Source Available License at the reviewed revisions; they must not be
misclassified as Apache-2.0 FOSS donors.

No OpenRewrite source is copied into Synexia. `synexia-openrewrite-recipes` composes the
Apache-2.0 core Recipe APIs and core Java recipes through class hooks. The legacy YAML lane may
reference separately licensed external recipe packs, but those packs are not source donors for
Synexia-owned implementations. Every rewrite remains a candidate until the M3 proof chain admits it.
The Rule-22 decision is recorded in `docs/transpile/FOSS_REUSE_DECISION.tsv`.

## Google RE2/J

This module optionally uses the Java port of RE2 for the explicit hybrid-regex API.

- Project: Google RE2/J
- Repository: https://github.com/google/re2j
- Release pinned by this module: RE2/J 1.8 (`re2j-1.8`)
- License: BSD 3-Clause

RE2/J is used as a dependency; its source is not copied into Synexia. The Synexia hybrid dispatcher,
MIndex zero-copy match views, affix-tree pruning and finite-domain precomputation are original
integration code.

The ordinary String-compatible `MIndexString.matches/replaceAll/split` surface continues to use
JDK semantics. RE2/J is invoked only through the explicit `MIndexHybridRegex` API.

## Automaton and accelerator design references

The following public projects were reviewed as architecture donors for the detached MIndex regex
batch and Bloom prefilter surface. No source was copied and no native dependency is introduced by
the core module.

- `dmikushin/gpu-grep`, https://github.com/dmikushin/gpu-grep, MIT: parallel GPU regex execution
  over a compiled automaton and independent input lanes.
- `vqd8a/DFAGE`, https://github.com/vqd8a/DFAGE, BSD 3-Clause: DFA transition images and batched
  rule/input execution on CUDA; prototype correctness caveat retained in our boundary decision.
- `NVIDIA/cuvs`, https://github.com/NVIDIA/cuvs, Apache-2.0: accelerator/provider separation and
  build-on-GPU/deploy-on-CPU index shape.
- `beehive-lab/TornadoVM`, https://github.com/beehive-lab/TornadoVM: Apache-2.0 API with a GPLv2
  Classpath-exception runtime/driver; reviewed only for its Java primitive-array kernel boundary.

The optional AST SimHash provider in `synexia-indexstring-tornadovm` uses only the published
TornadoVM API/runtime boundary. Its physical OpenCL qualification was run with the pinned SDK
argfile on an RTX 4060 environment; the Java oracle remains authoritative and the measured
transfer-inclusive crossover was not admitted.

The exact reuse classification and admission rows are in
`docs/MINDEX_MATHEMATICAL_QUERY_DONORS.tsv`.

## AST-kind automaton batch provider

TheAlgorithms Java Aho-Corasick implementation at
https://github.com/TheAlgorithms/Java, revision
`3027ce1c968bafcf4d81c8f44d3390d5a4d9fc72`, is MIT licensed and was reviewed for its
trie/failure-link and many-pattern batch shape. No source, dependency, or String-based scanner is
copied. The provider-neutral `MIndexASTKindAutomatonBatchProvider` composes the existing exact
`Tree.Kind`/preorder automaton with detached result arrays and a proof-bound selection receipt;
Java remains the semantic oracle and accelerator outputs are candidate-only until independently
validated. The Rule-22 decision is recorded in `docs/transpile/FOSS_REUSE_DECISION.tsv`.

## AST-kind automaton JNI boundary

`JniMIndexASTKindAutomatonBatchProvider` is an optional, fail-closed native boundary informed by
the reviewed `gpu-grep`, `DFAGE`, and TornadoVM accelerator shapes. No CUDA, JNI, TornadoVM, or
native source is copied into the core module. The core has no native implementation for this
provider: automatic selection remains Java until a separately built module registers the probe,
passes parity/crossover evidence, and opts into admission. Forced unavailable requests reject, and
any future native result is compared with the exact Java AST-kind oracle before publication. See
`docs/MINDEX_AST_KIND_AUTOMATON_JNI_BOUNDARY_PROOF_20260928.md` and the Rule-22 ledger.

## MIndexAST source-index cold projection

The authenticated `MIndexASTSourceIndexCodec` is an adapted first-party Synexia projection
pattern from `f9de6da4ea8`. It composes the existing weighted `MIndexASTPartialCache`,
`MIndexASTTieredLoader`, and `MIndexASTSqliteStore`; no second AST model, parser, LRU, or source
authority is introduced. JDK `DataInputStream`/`DataOutputStream` and Xerial SQLite JDBC are
dependency/API references only. The sidecar remains rebuildable and is admitted only against the
already-validated base document identity and structure. The proof and current-develop decision are
in `docs/MINDEX_AST_SOURCE_INDEX_COLD_PROOF_20260930.md` and
`docs/FOSS_REUSE_DECISION.tsv`.

## MIndexAST navigation cold projection

The authenticated `MIndexASTPrecomputeCodec` is an adapted first-party Synexia
navigation projection pattern from `6fefb5241d3`. It composes the existing
`MIndexASTPartialCache`, `MIndexASTTieredLoader`, and `MIndexASTSqliteStore`; no
second AST model, parser, LRU, or source authority is introduced. JDK
`DataInputStream`/`DataOutputStream` and Xerial SQLite JDBC are dependency/API
references only. The sidecar remains rebuildable and is admitted only against
the already-validated base document identity and structure. The proof and
current-develop decision are in
`docs/MINDEX_AST_NAVIGATION_COLD_PROOF_20260930.md` and
`docs/FOSS_REUSE_DECISION.tsv`.

## MIndexAST LCA and pool cold projections

The LCA shape was reviewed against `TheAlgorithms/Java` `LCA.java` at revision
`3f067087f217afc6c16f0d0ca90d194bd7a55726` (MIT). It is an algorithm-category reference only;
no source, adjacency representation, scanner, or dependency is copied. The authenticated
`MIndexASTLcaCodec` and `MIndexASTPoolPrecomputeCodec` compose the existing occurrence-tree,
canonical-pool, weighted-LRU, file-sidecar, and SQLite owners. OpenJDK stream/array/digest APIs
are reference surfaces only. Corrupt or stale projections fail closed and rebuild from the already
validated base document. See `docs/MINDEX_AST_LCA_POOL_COLD_DONOR_INVENTORY_20260930.md` and
the superseding Rule-22 rows in `docs/FOSS_REUSE_DECISION.tsv`.

## MIndexAST formal-language tree snapshot codec

`MIndexASTSpecTreeSnapshotCodec` composes the existing first-party
`MIndexASTSpecTreePrecompute.Snapshot/restore` validator with the established bounded,
digest-authenticated stream mechanics used by the MIndexAST projection codecs. It stores no source
text, parser, resolver dictionary, AST pool, cache, SQLite connection, JNI handle, or alternate
grammar authority. OpenJDK stream/digest/array APIs are reference surfaces; the algorithm
repositories are catalogue references only. No third-party source or dependency is copied. See
`docs/MINDEX_AST_SPEC_TREE_SNAPSHOT_CODEC_PROOF_20261002.md` and the Rule-22 ledger.

## Apache Lucene query architecture

Apache Lucene was reviewed at `e357029271b3de560a6ff13c9cab4d4c8103b53b` for its public
postings-separation and query-time candidate-iteration architecture.  The MIndex distance
receipt composes Synexia-owned frozen index, image, and bit-DAG contracts; no Lucene source,
dependency, postings format, or query implementation is copied.  Lucene is Apache-2.0 and is
recorded as `ADAPT_COMPOSE` in `docs/transpile/FOSS_REUSE_DECISION.tsv`.

The UTF-16 n-gram candidate receipt uses the same clean-room postings boundary. Candidate IDs are
necessary-condition evidence only and must be verified against the dictionary's exact substring
semantics before admission.

## Chrome2API candidate advisor boundary

The pinned `xszwow/Chrome2api` project at `406c5c7a33a6c364c0bd42892250ad1b6a103dad` (MIT) was
reviewed only for its local model-helper boundary.  `ProjectionAdviceReceipt` composes the
existing data-only `MatIndexStructureAdvisor` and bounded `ProjectionAdvice` contracts; no
Chrome2API source, native runner, model asset, or network transport is copied into this module.
Compiler schema and bounded sample hashes remain authoritative, and every advisor result is
permanently `CANDIDATE_ONLY`.  The Rule-22 decision is recorded in
`docs/transpile/FOSS_REUSE_DECISION.tsv`.

`MatIndexChrome2ApiStructureAdvisor` adds the explicit higher-level binding around that boundary.
It admits only HTTP loopback endpoints and delegates to the existing OpenAI-compatible client;
it copies no Chrome2API source, browser automation, model asset, or transport implementation.
See `docs/MINDEX-CHROME2API-ADAPTER-PROOF-20260928.md` and the Rule-22 ledger.

## RoaringBitmap population-filter architecture

RoaringBitmap was reviewed at `f2289086f4204df51a1d4c9e494d50ffa3ee8988` for immutable bitmap
population algebra and query-time intersection. The `MatIndexDistancePopulation` contract is a
clean-room composition over Synexia's checked `BitWords` primitive; no RoaringBitmap source,
dependency, serialization format, or native code is copied. The Java `BitWords` implementation,
domain-root binding, and tail-bit validation remain the semantic authority. RoaringBitmap is
Apache-2.0 and is recorded as `ADAPT_COMPOSE` in `docs/transpile/FOSS_REUSE_DECISION.tsv`.

## Eclipse JDT Core

Eclipse JDT Core was reviewed at `a9ba670e61dc6b35c345a34ee97f61d1267bc132` as a reference for
keeping AST identity/index boundaries separate from source locations. No JDT source or dependency
is copied into Synexia; `MIndexASTReuseKey` is a clean-room composition over existing MIndex
exact/structural/logic/SimHash lanes. The donor is EPL-2.0 and is recorded as reference-only in
`docs/transpile/FOSS_REUSE_DECISION.tsv`.

## AST reuse candidate index

The cross-file `MIndexASTReuseCandidateIndex` adapts the same Eclipse JDT identity/index
separation into deterministic exact, normalized-logic, structural and approximate postings. No JDT
source or dependency is copied. Candidate rows are advisory only; `MIndexASTReuseAdmission` and
the actual AST contract proof remain mandatory before a rewrite recipe, compiled artifact, or
execution result is reused. The exact donor decision and strict-JDK proof are recorded in
`docs/transpile/FOSS_REUSE_DECISION.tsv` and
`docs/MINDEX_AST_REUSE_CANDIDATE_INDEX_PROOF_20260928.md`.

## Transpiler structural-reuse bridge

`MIndexTranspileReuseBridge` composes the existing OpenRewrite-backed one-file worklist receipts
with the MIndex AST candidate image. OpenRewrite's Maven-plugin recipe boundary and Eclipse JDT's
AST identity/index separation are references only; no donor source, recipe implementation, or
dependency is copied. The bridge is a pre-recipe lookup accelerator: source paths must be safe,
AST candidate lanes must be present, and candidate rows remain non-authoritative until actual AST
comparison and executable behavior proof are attached. See
`docs/MINDEX_TRANSPILE_REUSE_BRIDGE_PROOF_20260928.md` and the Rule-22 ledger.

## Transpiler receipt codec

`MIndexTranspileReceiptCodec` composes the JDK `DataOutputStream`/`DataInputStream` primitive
binary framing contract and adds explicit length-prefixed fields as a bounded transport reference
for durable transpiler records, including the optional accumulated recipe-catalogue root. No
OpenJDK source or
dependency is copied. The codec owns its magic/version/schema, field limits, strict UTF-8
decoding, trailing-byte rejection, and receipt-root validation; candidate receipts still cannot
authorize reuse without a passing executable proof step. OpenJDK's GPL-2.0 with Classpath
Exception is recorded as a reference in `docs/transpile/FOSS_REUSE_DECISION.tsv`; the proof is
`docs/MINDEX_TRANSPILE_RECEIPT_CODEC_PROOF_20260928.md`.

## Parallel transpile worklist

`MIndexTranspileWorklist.runParallel` composes the JDK `ExecutorService`/`Future` concurrency
contract to run independent file transformations with bounded workers. It canonicalizes inputs
before dispatch, collects outputs in canonical path order, preserves the sequential manifest
root, and cancels the batch on worker failure. No OpenJDK source or dependency is copied; the
Rule-22 decision and strict proof are in `docs/transpile/FOSS_REUSE_DECISION.tsv` and
`docs/MINDEX_TRANSPILE_WORKLIST_PARALLEL_PROOF_20260928.md`.

## Transpile worklist codec

`MIndexTranspileWorklistCodec` composes JDK primitive `DataInputStream`/`DataOutputStream` framing
to transport ordered target bytes together with the optional accumulated OpenRewrite/M3 recipe
catalogue root and nested per-file receipts. It recomputes and checks
the outer manifest root, validates path/field/image budgets, and rejects corruption, truncation,
trailing bytes, and detached target data. No OpenJDK source or dependency is copied. The Rule-22
decision and strict proof are recorded in `docs/transpile/FOSS_REUSE_DECISION.tsv` and
`docs/MINDEX_TRANSPILE_WORKLIST_CODEC_PROOF_20260928.md`.

## Transpile scope fan-in

`MIndexTranspileScopeFanIn` composes Eclipse JDT's AST/index hierarchy as a clean-room reference
for deterministic file-to-package-to-module-to-project fan-in. It stores child IDs and exact
receipt roots for scheduling and index lookup; it is not a compiler or behavior-equivalence
authority. No JDT source or dependency is copied. The Rule-22 decision and strict proof are in
`docs/transpile/FOSS_REUSE_DECISION.tsv` and
`docs/MINDEX_TRANSPILE_SCOPE_FANIN_PROOF_20260928.md`.
The optional accumulated OpenRewrite/M3 recipe-catalogue root is also bound to the project plan;
mixed roots fail closed while empty roots preserve legacy plan identity. This extension is recorded
as `mindex-transpile-scope-recipe-root-20260928` and proved in
`docs/MINDEX_TRANSPILE_SCOPE_RECIPE_ROOT_PROOF_20260928.md`.

## Transpile scope fan-in codec

`MIndexTranspileScopeFanInCodec` composes JDK primitive stream framing to persist the hierarchical
scope plan. Existing scope node and plan constructors recompute child and project roots on decode;
the version-2 codec carries an optional recipe-catalogue root and still decodes version-1 images.
The codec therefore rejects detached, reordered, corrupted, truncated, oversized, or trailing
data. No OpenJDK source or dependency is copied. The Rule-22 decision and strict proof are in
`docs/transpile/FOSS_REUSE_DECISION.tsv` and
`docs/MINDEX_TRANSPILE_SCOPE_FANIN_CODEC_PROOF_20260928.md`.

## AST atomization receipt codec

`MIndexASTAtomizationReceiptCodec` composes JDK primitive stream framing to persist file-local
partition and topology evidence. The existing atomization receipt remains the semantic authority:
coverage, ordering, defensive copies, and the derived root are revalidated during decode. No
OpenJDK source or dependency is copied. The Rule-22 decision and strict proof are recorded in
`docs/transpile/FOSS_REUSE_DECISION.tsv` and
`docs/MINDEX_AST_ATOMIZATION_RECEIPT_CODEC_PROOF_20260928.md`.

## AST reuse candidate receipt codec

`MIndexASTReuseCandidateReceiptCodec` composes JDK primitive stream framing to persist candidate
index evidence. The frozen index root, probe lanes, threshold, ordered rows, and derived receipt
root are revalidated during decode; candidates remain advisory and cannot authorize reuse. No
OpenJDK source or dependency is copied. The Rule-22 decision and strict proof are recorded in
`docs/transpile/FOSS_REUSE_DECISION.tsv` and
`docs/MINDEX_AST_REUSE_CANDIDATE_RECEIPT_CODEC_PROOF_20260928.md`.

## AST memory state snapshot

`MIndexASTMemorySnapshot` is a first-party detached projection over the existing weighted heap
LRU, tiered cold loader, adaptive demand ratio, active pressure budget, and selected file/SQLite
cold backend. It adds no cache, serializer, eviction policy, or storage authority; the existing
`MIndexASTPartialCache` and `MIndexASTTieredLoader` remain authoritative. OpenJDK immutable-record
and validation semantics plus the algorithm-catalogue repositories are reference surfaces only;
no donor source or dependency is copied. The Rule-22 decision and strict proof are recorded in
`docs/FOSS_REUSE_DECISION.tsv` and
`docs/MINDEX_AST_MEMORY_SNAPSHOT_PROOF_20260930.md`.

## OpenJDK compiler-tree API

OpenJDK JDK 21 was reviewed as the platform reference for the standard
`JavacTask`/`Trees` attributed compiler-tree API used by the compile-time
MIndex shape audit. No OpenJDK source, compiler implementation, or dependency
is copied into Synexia; the existing Synexia scanner and shape facts remain the
semantic authority. OpenJDK is GPL-2.0 with Classpath Exception and is recorded
as an `ADMITTED_REPAIR` reference in `docs/transpile/FOSS_REUSE_DECISION.tsv`.

The compiler's default identifier profile also uses the standard JDK `Character` and `Locale`
contracts through the existing deterministic segmenter. These APIs are platform interfaces, not
vendored code; provider identity and language id are sealed into each compiled shape.

## Kotlin lexical profile references

The explicit Kotlin identifier profile references the formal Kotlin grammar in
`antlr/grammars-v4` at `7e08234262d2a7c58557d74f2cb8ce90c72d356f`
(Apache-2.0), specifically the lexer/parser and Unicode-class grammar paths
listed in `docs/donors/PUBLISHED_LANGUAGE_GRAMMARS.tsv`. No grammar source,
parser, runtime, or dependency is copied. `MatIndexKotlinIdentifierStemmer`
adds only the typed language binding and deterministic backtick-escaped
identifier rule, composing the existing Java lexical provider. See
`docs/transpile/FOSS_REUSE_DECISION.tsv` and
`docs/MINDEX-KOTLIN-STEMMER-PROFILE-PROOF-20260928.md`.

## C# lexical profile references

The explicit C# identifier profile references `antlr/grammars-v4` at
`7e08234262d2a7c58557d74f2cb8ce90c72d356f`, specifically
`csharp/v6/CSharpLexer.g4` and `csharp/v6/CSharpParser.g4`. Those grammar files
carry EPL-1.0 headers. No grammar source, parser, runtime, or dependency is
copied. `MatIndexCSharpIdentifierStemmer` adds only the typed language binding,
ordinary-provider composition, and deterministic `@`-verbatim identifier rule.
See `docs/transpile/FOSS_REUSE_DECISION.tsv` and
`docs/MINDEX-CSharp-STEMMER-PROFILE-PROOF-20260928.md`.

## Rust lexical profile references

The explicit Rust identifier profile references `antlr/grammars-v4` at
`7e08234262d2a7c58557d74f2cb8ce90c72d356f`, specifically
`rust/RustLexer.g4` and `rust/RustParser.g4`. The Rust grammar is MIT-licensed
at the pinned revision. No grammar source, parser, runtime, or dependency is
copied. `MatIndexRustIdentifierStemmer` adds only the typed language binding,
ordinary-provider composition, and deterministic `r#` raw-identifier rule.
See `docs/transpile/FOSS_REUSE_DECISION.tsv` and
`docs/MINDEX-RUST-STEMMER-PROFILE-PROOF-20260928.md`.

## Go lexical profile references

The explicit Go identifier profile references `antlr/grammars-v4` at
`7e08234262d2a7c58557d74f2cb8ce90c72d356f`, specifically
`golang/GoLexer.g4` and `golang/GoParser.g4`. The grammar reference is
BSD-3-Clause under the pinned `golang/README.md`. No grammar source, parser,
runtime, or dependency is copied. `MatIndexGoIdentifierStemmer` adds only the
typed language binding and composes the existing deterministic segmenter.
See `docs/transpile/FOSS_REUSE_DECISION.tsv` and
`docs/MINDEX-GO-STEMMER-PROFILE-PROOF-20260928.md`.

## Bash lexical profile reference

The planned Bash identifier profile references `tree-sitter/tree-sitter-bash` at
`a06c2e4415e9bc0346c6b86d401879ffb44058f`, specifically `src/grammar.json`.
The donor is MIT-licensed and is used only as a lexical reference; no grammar
source, parser, runtime, or dependency is copied. The profile will preserve
parameter/special-parameter sigils and shell operators in explicit namespaces,
compose the existing deterministic segmenter for ordinary names, and reject
malformed or cross-language inputs. See `docs/transpile/FOSS_REUSE_DECISION.tsv`.

## Stemmer provider catalog

The provider catalog composes the admitted built-in language profiles and performs exact language
id/identity resolution. It references only the JDK `String` identity, switch, and exception API
contracts; no OpenJDK source or dependency is copied. Unknown values fail closed rather than
falling back to a different language. See
`docs/MINDEX-STEMMER-CATALOG-PROOF-20260928.md` and the Rule-22 ledger.

## Index-search accelerator selection

The search accelerator selection receipt composes the existing Java/JNI/JNA provider boundary and
keeps immutable index images separate from execution providers. NVIDIA cuVS is a clean-room
reference only; no cuVS source, runtime, or serialization is copied. Java remains the exact
semantic oracle, and forced unavailable providers fail closed. See
`docs/INDEX_SEARCH_ACCELERATOR_SELECTION_PROOF_20260928.md` and the Rule-22 ledger.

## Artifact hash provider boundary

`ossf/gpu-hashlib` is an API/backend reference for optional GPU artifact hashing at pinned
revision `3442d6379a087cfb17e560fd331bfd4f50ef5c52` (dual MIT/Apache-2.0). No Rust, CUDA, SYCL,
signing, or serialization source is copied. Synexia currently admits only the exact Java SHA-256
oracle; native providers must independently prove stable-file, byte-budget, cancellation, and
digest equivalence before admission. See `docs/INDEX_ARTIFACT_HASH_PROVIDER_PROOF_20260928.md`
and the Rule-22 ledger.

## AST SimHash batch provider

NVIDIA cuVS is a provider and vector-search reference at pinned revision
`f633a36793943520f5186591dd84f5acc55032f0` (Apache-2.0). No cuVS source, CUDA kernel,
device runtime, or serialization is copied. The AST SimHash lane adds only a provider-neutral
selection/evidence seam over the existing exact Java Hamming oracle and JNI bridge. Candidate
distances are never structural or behavioral proof; a future CUDA/TornadoVM provider must pass
an independent differential proof before admission. See
`docs/MINDEX-AST-SIMHASH-BATCH-PROVIDER-PROOF-20260928.md` and the Rule-22 ledger.

The 2026-09-29 admission hardening rechecks provider availability, admission readiness, and the
minimum-row threshold at forced selection and immediately before execution. An unavailable or
unadmitted provider cannot be silently downgraded after producing output; see
`docs/MINDEX-AST-SIMHASH-ADMISSION-PROOF_20260929.md` and the Rule-22 ledger.

## TornadoVM AST SimHash provider

The optional `synexia-indexstring-tornadovm` module adds
`TornadoMIndexASTSimHashBatchProvider` against the existing provider SPI. TornadoVM is pinned at
`a86db1ace41e75d401dbc8ff4e45de3d9d7a7f9e`; its Apache-2.0 API and GPLv2-with-Classpath-
Exception runtime boundary is retained. No TornadoVM source is copied. The provider is
candidate-only, uses detached primitive lanes, and remains unavailable unless a GPU is observed
through the M3 inventory. See `docs/MINDEX-TORNADOVM-AST-SIMHASH-PROVIDER-PROOF-20260928.md`.

`TornadoMIndexASTSimHashCrossoverEvidenceCodec` composes the JDK 21
`DataInputStream`/`DataOutputStream` framing contract for a bounded, root-checked detached
qualification record. No OpenJDK source or dependency is copied; strict framing and corruption
rejection are proven in `docs/MINDEX-TORNADOVM-AST-SIMHASH-CROSSOVER-CODEC-PROOF-20260928.md`.

## AST file atomization

Eclipse JDT core (`a9ba670e61dc6b35c345a34ee97f61d1267bc132`, EPL-2.0) and OpenJDK 21 are
clean-room references for immutable occurrence-tree/index boundaries and compiler-tree identity.
No parser or compiler source is copied. `MIndexASTOccurrenceSource` is a minimal topology membrane;
`MIndexASTFileAtomizer` partitions rows and verifies exact coverage/topology before fan-in. GPU or
JNI output remains candidate-only. See `docs/MINDEX-AST-FILE-ATOMIZATION-PROOF-20260928.md` and
the Rule-22 ledger.

## TypeScript identifier profile

The TypeScript lexical profile uses `antlr/grammars-v4` revision
`e199816b3f1a7a49ea1ad84fb6b87c382ea36a33` only as a pinned grammar reference (MIT headers at
`javascript/typescript/TypeScriptLexer.g4` and `TypeScriptParser.g4`). No grammar, parser, runtime,
or dependency is copied. The provider composes the admitted ECMAScript segmentation while sealing
TypeScript with its own language id and identity. See
`docs/MINDEX-TYPESCRIPT-STEMMER-PROFILE-PROOF-20260928.md` and the Rule-22 ledger.

## PHP identifier profile

The PHP lexical profile references `antlr/grammars-v4` at
`7e08234262d2a7c58557d74f2cb8ce90c72d356f`, specifically `php/PhpLexer.g4`, whose pinned donor
header is MIT. No grammar, parser, runtime, or dependency is copied. The provider keeps PHP's
dollar-prefixed variable marker in a distinct namespace and composes the existing deterministic
identifier segmenter for ordinary names. It makes no PHP parsing, interpolation, namespace, or
type-resolution claim. See `docs/MINDEX-PHP-STEMMER-PROFILE-PROOF-20260928.md` and the Rule-22
ledger.

## Perl identifier profile

The Perl lexical profile references `tree-sitter-perl/tree-sitter-perl` revision
`f678e356280566100e7eed21716d6f59abdf96b7`, specifically `grammar.js`, under the donor's MIT
license. No grammar, parser, runtime, or dependency is copied. The provider keeps Perl scalar,
array, hash, subroutine, typeglob, package-qualified, and symbolic-operator spellings in explicit
namespaces. It makes no Perl parsing, package-resolution, interpolation, or dispatch claim. See
`docs/MINDEX-PERL-STEMMER-PROFILE-PROOF-20260928.md` and the Rule-22 ledger.

## R identifier profile

The R lexical profile references `r-lib/tree-sitter-r` revision
`58a22794466c0fc15b0d3b40531db751593721e8`, specifically `grammar.js`, under the donor's MIT
license. No grammar, parser, runtime, or dependency is copied. The provider keeps R dollar and at
symbols, namespace-qualified names, backtick/dot-leading symbols, and operators in explicit
namespaces. It makes no R parsing, evaluation, or dispatch-resolution claim. See
`docs/MINDEX-R-STEMMER-PROFILE-PROOF-20260928.md` and the Rule-22 ledger.

## SQL identifier profile

The SQL lexical profile references `DerekStride/tree-sitter-sql` revision
`97614d051eebfd3bc5d97c0bdb5a1638719ca811`, specifically `grammar.js`, under the donor's MIT
license. No grammar, parser, runtime, or dependency is copied. The provider keeps SQL quoted and
qualified identifiers, bind parameters, and symbolic operators in explicit namespaces. It makes no
dialect parsing, type inference, or query-plan claim. See
`docs/MINDEX-SQL-STEMMER-PROFILE-PROOF-20260928.md` and the Rule-22 ledger.

## C++ identifier profile

The C++ lexical profile references `antlr/grammars-v4` revision
`7e08234262d2a7c58557d74f2cb8ce90c72d356f`, specifically `cpp/CPP14Lexer.g4` and
`cpp/CPP14Parser.g4`, whose donor license is BSD-3-Clause. No grammar, parser, runtime, or
dependency is copied. The provider preserves destructor and overloaded-operator spellings in
explicit namespaces and composes the existing deterministic identifier segmenter for ordinary
names. It makes no full C++ parsing, macro, namespace, or type-resolution claim. See
`docs/MINDEX-CPP-STEMMER-PROFILE-PROOF-20260928.md` and the Rule-22 ledger.

## Scala identifier profile

The Scala lexical profile references `antlr/grammars-v4` revision
`7e08234262d2a7c58557d74f2cb8ce90c72d356f`, specifically `scala/scala2/Scala.g4`, whose pinned
donor header is BSD-3-Clause. No grammar, parser, runtime, or dependency is copied. The provider
preserves backtick and symbolic-operator spellings in explicit namespaces and composes the
existing deterministic identifier segmenter for ordinary names. It makes no full Scala parsing,
implicit-conversion, or type-resolution claim. See
`docs/MINDEX-SCALA-STEMMER-PROFILE-PROOF-20260928.md` and the Rule-22 ledger.

## Swift identifier profile

The Swift lexical profile references `antlr/grammars-v4` revision
`7e08234262d2a7c58557d74f2cb8ce90c72d356f`, specifically `swift/swift5/Swift5Lexer.g4`. The
repository metadata identifies the pinned donor as MIT; the grammar file has no separate license
header. No grammar, parser, runtime, or dependency is copied. The provider preserves backtick
identifiers, implicit parameters, property-wrapper projections, and symbolic operators in
explicit namespaces and composes the existing deterministic identifier segmenter for ordinary
names. It makes no full Swift parsing or operator-resolution claim. See
`docs/MINDEX-SWIFT-STEMMER-PROFILE-PROOF-20260928.md` and the Rule-22 ledger.

## Lua identifier profile

The Lua lexical profile references `antlr/grammars-v4` revision
`7e08234262d2a7c58557d74f2cb8ce90c72d356f`, specifically `lua/LuaLexer.g4`. The repository
metadata identifies the pinned donor as MIT; the grammar file has no separate license header.
No grammar, parser, runtime, or dependency is copied. The provider binds Lua's ASCII `NAME`
contract, retains underscore markers, exposes digit transitions, and deliberately performs no
English light stemming. It makes no full Lua parsing or metatable-resolution claim. See
`docs/MINDEX-LUA-STEMMER-PROFILE-PROOF-20260928.md` and the Rule-22 ledger.

## Ruby identifier profile

The Ruby lexical profile references `tree-sitter/tree-sitter-ruby` revision
`ad907a69da0c8a4f7a943a7fe012712208da6dee`, specifically `grammar.js`, under the donor's MIT
license. No grammar, parser, runtime, or dependency is copied. The provider keeps Ruby instance,
class, global, constant, predicate/bang-method, and operator spellings in explicit namespaces
and composes the existing deterministic segmenter for ordinary names. It makes no full Ruby
parsing, interpolation, or dispatch-resolution claim. See
`docs/MINDEX-RUBY-STEMMER-PROFILE-PROOF-20260928.md` and the Rule-22 ledger.

## Haskell identifier profile

The Haskell lexical profile references `tree-sitter/tree-sitter-haskell` revision
`0975ef72fc3c47b530309ca93937d7d143523628`, specifically `grammar.js`, under the donor's MIT
license. No grammar, parser, runtime, or dependency is copied. The provider keeps Haskell
variables, constructors, prime-suffixed names, qualified names, and symbolic operators in
explicit namespaces. It makes no layout parsing, type inference, or overloaded-operator
resolution claim. See `docs/MINDEX-HASKELL-STEMMER-PROFILE-PROOF-20260928.md` and the Rule-22
ledger.

## TweetyProject

TweetyProject was reviewed as a design/reference donor for the MIndex semantic precompute layer.

- Project: TweetyProject
- Repository: https://github.com/TweetyProjectTeam/TweetyProject
- Reviewed revision: `5940c1a18ee762fb16055bd7b72ef19b74246f70`
- Source copied into Synexia: **No**
- Use: clean-room review of public formula, signature, interpretation, graph, Dung argumentation,
  ADF acceptance-condition, conditional-reasoning, probabilistic-conditionals/model distributions,
  belief-revision, causal, model/query, dependency, and reachability abstraction boundaries
- License status at the reviewed revision: GitHub repository metadata reports `GPL-3.0`; the
  repository README states `LGPL-3.0` for versions since 1.6 except where noted

Because those license signals are not identical, Synexia does not incorporate TweetyProject source
in this layer. The Java implementations under `com.synexia.indexstring.precompute` are original
clean-room Synexia implementations. Pinned path-level provenance is retained in
`MIndexTweetyDonorCatalog`. Directed relation/pageable-graph review additionally records exact
upstream blob SHAs for `Graph`, `DefaultGraph`, `BinaryRelation`, `SerialisationGraph`, and
`BipolarArgumentationFramework` in
`docs/donors/TWEETY_DIRECTED_RELATION_PAGING.tsv`; every row is marked
`source_copied=false`.

## Apache Lucene compact-posting reference

The optional MIndex compressed n-gram cold projection was informed by the public compact-posting
and external-sort boundaries in Apache Lucene's `OfflineSorter` and `PackedInts` surfaces at
revision `3be9cd6e6629f5ac086b3a656b16c3e88bdaf801`:
https://github.com/apache/lucene

Apache Lucene is Apache-2.0. Synexia copies no Lucene source and adds no Lucene dependency. The
JDK-only `IndexNGramCompressedImage` uses deterministic delta-varint postings while preserving
the existing fixed-width n-gram image as canonical authority. See
`docs/MINDEX-COMPRESSED-NGRAM-IMAGE-PROOF-20260928.md` and the Rule-22 ledger for the exact
adaptation boundary.

The bounded `IndexNGramExternalSorter` uses the same donor direction for deterministic spill-run
sorting and k-way merge, with no Lucene source copied. Its exact root-parity proof is recorded in
`docs/MINDEX-NGRAM-EXTERNAL-SORT-PROOF-20260928.md`.

## tree-sitter-dart lexical reference

The optional Dart lexical identifier profile was informed by `nielsenko/tree-sitter-dart`,
revision `b57d734c84f510bbd524097902cab671e4dbfca9`, specifically its `grammar.js` lexical
boundaries: https://github.com/nielsenko/tree-sitter-dart

The donor is MIT-licensed. Synexia copies no grammar source, parser, runtime, or dependency.
The Java provider is a clean-room lexical composition that namespaces Dart interpolation,
annotations, cascade/symbolic operators, composes the existing identifier segmenter for ordinary
names, and rejects malformed prefixed spellings. See
`docs/MINDEX-DART-STEMMER-PROFILE-PROOF-20260928.md` and the Rule-22 ledger.

## tree-sitter-zig lexical reference

The optional Zig lexical identifier profile was informed by
`tree-sitter-grammars/tree-sitter-zig`, revision `6479aa13f32f701c383083d8b28360ebd682fb7d`,
specifically its `grammar.js` lexical boundaries:
https://github.com/tree-sitter-grammars/tree-sitter-zig

The donor is MIT-licensed. Synexia copies no grammar source, parser, runtime, or dependency.
The Java provider is a clean-room lexical composition that namespaces Zig builtins, escaped
identifiers, and symbolic operators, composes the existing identifier segmenter for ordinary
names, and rejects malformed spellings. See
`docs/MINDEX-ZIG-STEMMER-PROFILE-PROOF-20260928.md` and the Rule-22 ledger.

## tree-sitter-groovy lexical reference

The optional Groovy lexical identifier profile was informed by
`murtaza64/tree-sitter-groovy`, revision `2a6ddd558b6aa39c5b77d8db9fe9baf817486b2c`,
specifically its `grammar.js` lexical boundaries:
https://github.com/murtaza64/tree-sitter-groovy

The donor is MIT-licensed. Synexia copies no grammar source, parser, runtime, or dependency.
The Java provider is a clean-room lexical composition that namespaces Groovy interpolation,
annotations, and symbolic operators, composes the existing identifier segmenter for ordinary
names, and rejects malformed prefixed spellings. See
`docs/MINDEX-GROOVY-STEMMER-PROFILE-PROOF-20260928.md` and the Rule-22 ledger.

## tree-sitter-clojure lexical reference

The optional Clojure lexical identifier profile was informed by
`sogaiu/tree-sitter-clojure`, revision `e43eff80d17cf34852dcd92ca5e6986d23a7040f`,
licensed CC0-1.0. The reference grammar is available at
https://github.com/sogaiu/tree-sitter-clojure. Only `grammar.js` and
`src/grammar.json` were inventoried; no grammar source, parser, runtime, or dependency is
copied. `MatIndexClojureIdentifierStemmer` is a clean-room lexical composition that namespaces
Clojure keywords, metadata, reader vars, anonymous arguments, and qualified symbols. See
`docs/MINDEX-CLOJURE-STEMMER-PROFILE-PROOF-20260928.md` and the Rule-22 ledger.

## tree-sitter-elixir lexical reference

The optional Elixir lexical identifier profile was informed by
`elixir-lang/tree-sitter-elixir`, revision `4b0c7118760af58a2e7081bbc8396e136f820b37`,
licensed Apache-2.0. The reference grammar is available at
https://github.com/elixir-lang/tree-sitter-elixir. Only `grammar.js` and `src` were inventoried;
no grammar source, parser, runtime, or dependency is copied. `MatIndexElixirIdentifierStemmer`
is a clean-room lexical composition that namespaces aliases, sigils, module attributes, pins,
captures, special forms, and operators. See
`docs/MINDEX-ELIXIR-STEMMER-PROFILE-PROOF-20260928.md` and the Rule-22 ledger.

## tree-sitter Objective-C lexical reference

The planned Objective-C lexical profile is informed by
`tree-sitter-grammars/tree-sitter-objc`, revision
`181a81b8f23a2d593e7ab4259981f50122909fda`, licensed MIT. The reference grammar is available at
https://github.com/tree-sitter-grammars/tree-sitter-objc. Only the grammar paths were inventoried;
no grammar source, parser, runtime, or dependency is copied.

## tree-sitter OCaml lexical reference

The planned OCaml lexical profile is informed by `tree-sitter/tree-sitter-ocaml`, revision
`3b2e14e0697d405c9aa0beddfa09b71f45abc504`, licensed MIT. The reference grammar is available at
https://github.com/tree-sitter/tree-sitter-ocaml. Only grammar paths were inventoried; no grammar
source, parser, runtime, or dependency is copied.

## tree-sitter Erlang lexical reference

The optional Erlang lexical identifier profile was informed by
`WhatsApp/tree-sitter-erlang`, revision `6ba4c762eb3065495e3db85697ffeecdf364ce35`,
licensed Apache-2.0. The reference grammar is available at
https://github.com/WhatsApp/tree-sitter-erlang. Only the grammar paths were inventoried; no
grammar source, parser, runtime, or dependency is copied. `MatIndexErlangIdentifierStemmer`
is a clean-room lexical composition that namespaces variables, quoted atoms, qualified names,
attributes, macros, records, and operators.

## Frozen UTF-8 candidate boundary repair

The frozen UTF-8 candidate boundary repair is first-party Synexia code. The new
`candidateUtf8Unchecked` aliases delegate to the existing validated Java kernels in
`IndexDirectAddressLookup`, `IndexDirectInlineKey32`, and `IndexResidualKey32Lookup`; they do not
copy or introduce a third-party implementation. `IndexFrozenLookupHandle` retains the established
packed miss contract as `-1L`. The aliases exist to close the source-level boundary used by the
AST-kind automaton parser without creating a second lookup semantic. The Rule-22 decision and
strict JDK 21 proof are recorded in `docs/transpile/FOSS_REUSE_DECISION.tsv`.
## tree-sitter PowerShell lexical reference

The optional PowerShell lexical profile is informed by `airbus-cert/tree-sitter-powershell`,
revision `e7bd348c49fdfd5c853a146a670965ba516a6239`, licensed MIT. The reference grammar is
available at https://github.com/airbus-cert/tree-sitter-powershell. Only its grammar paths and
the repository's MIT license were inventoried before implementation; no grammar source, parser,
runtime, or dependency is copied. `MatIndexPowerShellIdentifierStemmer` is a candidate-only
lexical composition that namespaces variables, splats, type literals, and operators; it makes no
PowerShell parsing, expansion, command-resolution, or semantic-equivalence claim. See
`docs/MINDEX-POWERSHELL-STEMMER-PROFILE-PROOF-20260928.md` and the Rule-22 ledger.
## C lexical profile donor

The candidate-only C identifier profile is informed by
`tree-sitter/tree-sitter-c` at commit
`b780e47fc780ddc8da13afa35a3f4ed5c157823d` (MIT). Only lexical categories
were reviewed; no grammar source, parser, runtime, or dependency is vendored.
The profile does not claim C parsing or semantic equivalence.

## C++ qualified-name profile donor

The qualified-name increment is informed by `tree-sitter/tree-sitter-cpp` at commit
`c009222808634c1014f82438d4883753516a2c24` (MIT). Only namespace/qualified-name lexical
boundaries were reviewed; no grammar source, parser, runtime, or dependency is vendored. The
profile does not claim C++ parsing, template semantics, overload resolution, or ABI equivalence.

## Restored MIndex lexical profiles

The restored Julia, HTML, CSS, Markdown, JSON, YAML, TOML, XML, GraphQL, D,
Haxe, and Fortran identifier profiles were informed by the MIT-licensed grammar
repositories listed in the Rule-22 reuse ledger:
tree-sitter/tree-sitter-julia@e0f9dcd180fdcfcfa8d79a3531e11d99e79321d3,
tree-sitter/tree-sitter-html@73a3947324f6efddf9e17c0ea58d454843590cc0,
tree-sitter/tree-sitter-css@dda5cfc5722c429eaba1c910ca32c2c0c5bb1a3f,
tree-sitter-grammars/tree-sitter-markdown@a0a00f817d02412bd92c54d316f164d827b57b5c,
tree-sitter/tree-sitter-json@254c42a6476413b776221e03982ac8ae159eeb72,
tree-sitter-grammars/tree-sitter-yaml@a1c4812a73ec5e089de8e441fdea3a921e8d5079,
tree-sitter-grammars/tree-sitter-toml@64b56832c2cffe41758f28e05c756a3a98d16f41,
tree-sitter-grammars/tree-sitter-xml@5000ae8f22d11fbe93939b05c1e37cf21117162d,
antlr/grammars-v4@7e08234262d2a7c58557d74f2cb8ce90c72d356f,
gdamore/tree-sitter-d@64f27931b4e6fdd75af1102c79bacbca68a8dacc,
vantreeseba/tree-sitter-haxe@a25faf5efa842502a326ec803cfbe5a442a11e7c, and
stadelmanma/tree-sitter-fortran@2bc0220f34ca660ec9571c54ea57ed5363338de1.
No grammar, parser, runtime, generated code, or dependency is copied. The
Synexia providers are candidate-only lexical namespace adapters with stable
language ids and identities; malformed and cross-language inputs fail closed,
and parsing, typing, macro expansion, and semantic resolution remain outside
this boundary. Executable profile proofs and hashes are recorded in
docs/transpile/FOSS_REUSE_DECISION.tsv.

## MatIndexString accelerator admission boundary

The MatIndexString admission seam is an original Synexia composition of the existing
TornadoVM/JNI provider interfaces and the previously inventoried `gpu-grep`, `DFAGE`, and cuVS
accelerator shapes. No donor source, CUDA kernel, native runtime, or binary format is copied.
Providers are rejected unless live availability, admission readiness, row thresholds, detached
input ownership, and exact Java-oracle parity all hold. See
`docs/MINDEX_MAT_ACCELERATOR_ADMISSION_PROOF_20260929.md` and the Rule-22 ledger.

## Artifact hash provider admission boundary

The artifact hash admission seam is an original Synexia composition informed by
`ossf/gpu-hashlib` at commit `3442d6379a087cfb17e560fd331bfd4f50ef5c52` (MIT/Apache-2.0).
No donor source, CUDA/SYCL implementation, signing logic, native runtime, or serialization
format is copied. Native providers remain fail-closed until explicit admission readiness and
exact stable-file SHA-256 behavior are proven; the Java provider remains the oracle. See
`docs/INDEX_ARTIFACT_HASH_ADMISSION_PROOF_20260929.md` and the Rule-22 ledger.

## Structural-reuse candidate provider admission boundary

The cross-file AST reuse candidate boundary composes the existing Synexia SimHash provider SPI,
JDK `Long.bitCount` oracle, and bounded candidate index. The inventoried shapes from
`rapidsai/cuvs` (Apache-2.0) and `dmikushin/gpu-grep` (MIT) are references only; no donor source,
native runtime, CUDA kernel, or binary format is copied. Candidate queries reject unavailable,
unadmitted, and below-threshold providers before cache lookup or execution, recheck live admission
at execution, protect caller-owned input arrays, and require exact oracle parity. Candidate rows
remain advisory and cannot authorize AST or transpiler reuse. See
`docs/MINDEX_AST_REUSE_CANDIDATE_PROVIDER_ADMISSION_PROOF_20260929.md` and the Rule-22 ledger.

The current-develop lexicon primitive snapshot composes OpenJDK checked-array and defensive-copy
semantics with Synexia's existing flattened resolver-ID arena and authenticated mapped image
lanes. Algorithm-catalogue repositories remain reference-only; no donor source or dependency was
copied. See `docs/MINDEX_FILE_LEXICON_PRIMITIVE_SNAPSHOT_PROOF_20260929.md`.

The current-develop MIndexFile span and projection snapshots compose OpenJDK checked-range,
checked-index, immutable-value and defensive-array semantics with Synexia's existing admission-time
source-facts, candidate-fingerprint and ordered token-projection lanes. LeetCode, HackerRank, and
GeeksforGeeks repositories remain category references only; no donor source or dependency was
copied. See `docs/MINDEX_FILE_SPAN_COORDINATE_FINGERPRINT_SNAPSHOT_PROOF_20260929.md` and
`docs/MINDEX_FILE_PROJECTION_PRIMITIVE_SNAPSHOT_PROOF_20260929.md`.

The current-develop MIndexFile event reader composes OpenJDK length-delimited input framing with
Synexia's existing MIDXEVT2 event records and writer. It validates metadata, byte continuity,
lexicon/token ordering, source coordinates and final SHA-256 before replay; it does not copy donor
source, change the event format, or rebuild the canonical index. LeetCode, HackerRank, and
GeeksforGeeks repositories remain category references only. See
`docs/MINDEX_FILE_EVENT_READER_VALIDATED_REPLAY_PROOF_20260929.md`.

The current-develop mapped projection snapshot composes OpenJDK checked-index and defensive-array
semantics with Synexia's authenticated mapped kind lane and heap projection mask. It emits detached
semantic, trivia, explicit-kind, and empty selection snapshots without changing the MIndexFile
image format. LeetCode, HackerRank, and GeeksforGeeks repositories remain category references only.
See `docs/MINDEX_FILE_MAPPED_PROJECTION_PRIMITIVE_SNAPSHOT_PROOF_20260929.md`.

The current-develop token-span snapshot composes OpenJDK checked-index/value semantics with
Synexia's existing one-pass source-facts lanes and immutable `MIndexFileStreamEvents.Token`
record. It exposes local handle, kind, UTF-16, UTF-8, code-point, and per-local-token SHA-256
facts without a source reread or format change. LeetCode, HackerRank, and GeeksforGeeks
repositories remain category references only. See
`docs/MINDEX_FILE_TOKEN_SPAN_PRIMITIVE_SNAPSHOT_CURRENT_DEVELOP_PROOF_20260929.md`.
## MIndexFile byte-facts primitive snapshot

`MIndexFileByteFactsSnapshot` composes existing Synexia one-pass raw-byte facts with the JDK
checked-index, defensive-copy, SHA-256, and immutable-value API contracts. It adds no third-party
source or dependency and does not change the source-facts sidecar or file-image formats. OpenJDK
is retained as a platform reference under its GPL-2.0 with Classpath Exception terms; the
LeetCode, HackerRank, and GeeksforGeeks repositories were reviewed only as algorithm-catalogue
references. The executable proof is
`docs/MINDEX_FILE_BYTE_FACTS_PRIMITIVE_SNAPSHOT_PROOF_20260929.md`.

## MIndexFile source-facts primitive snapshot

`MIndexFileSourceFactsSnapshot` composes the existing Synexia one-pass source-facts owner with JDK
checked-index, defensive-copy, immutable-value, and digest API contracts. It exposes metadata,
byte-facts, UTF-8/code-point coordinates, token SHA-256 lanes, and range-fingerprint lanes without
source rereads or sidecar/image format changes. No third-party source or dependency is copied.
OpenJDK is retained as a GPL-2.0 with Classpath Exception platform reference; LeetCode, HackerRank,
and GeeksforGeeks repositories remain algorithm-catalogue references only. The executable proof is
`docs/MINDEX_FILE_SOURCE_FACTS_SNAPSHOT_PROOF_20260929.md`.

## MIndexFile primitive structure snapshot

`MIndexFilePrimitiveSnapshot` composes OpenJDK checked-array and defensive-copy
semantics with Synexia's existing frozen occurrence, coordinate, line, frequency,
CSR posting, and optional rolling lanes. It exports one detached JNI/structural
serialization boundary without rereading source text, adding a storage authority,
or changing MIDX/MIDXEVT2 bytes. The first-party historical Synexia snapshot
commit `ee8ed50f91943f5e6c4c9c6098f7b104c3e8685a` was inspected as a known-pattern
reference and adapted to current-develop ownership; no donor source or dependency
was copied. LeetCode, HackerRank, and GeeksforGeeks repositories remain
algorithm-catalogue references only. The executable proof is
`docs/MINDEX_FILE_PRIMITIVE_SNAPSHOT_PROOF_20260929.md`.

## Mapped MIndexFile primitive structure snapshot

`MappedMIndexFile.primitiveSnapshot()` composes the authenticated MIDX image lanes
with the existing defensive `MIndexFilePrimitiveSnapshot` shape. It exports mapped
handles, kinds, coordinates, line starts, frequency/first/last rows, and CSR
postings without retaining the mapped buffer, resolving source text, or changing
the image format. The current image format has no rolling lanes, and the proof
keeps that absence explicit. OpenJDK checked-array/defensive-copy semantics and
the first-party heap snapshot are reference surfaces; no donor source or dependency
was copied. LeetCode, HackerRank, and GeeksforGeeks repositories remain
algorithm-catalogue references only. The executable proof is
`docs/MINDEX_FILE_MAPPED_PRIMITIVE_SNAPSHOT_PROOF_20260929.md`.

## Unified MIndexFile transfer snapshot

`MIndexFileSnapshot` composes the existing lexicon, primitive structure, and
optional source-facts projections into one immutable JNI/structural-serializer
transfer packet. It validates cross-component token geometry without adding a
storage authority, rereading source bytes, retaining a mapped buffer, or changing
MIDX/MIDXEVT2 formats. Source facts remain explicit and fail closed when a heap
file was not physical-source admitted or when a mapped image does not persist the
sidecar. OpenJDK immutable/defensive-value semantics and first-party Synexia
snapshot owners are reference surfaces; no donor source or dependency was copied.
LeetCode, HackerRank, and GeeksforGeeks repositories remain algorithm-catalogue
references only. The executable proof is
`docs/MINDEX_FILE_UNIFIED_SNAPSHOT_PROOF_20260929.md`.

## MIndexString precomputation byte facts and authenticated codec

`MIndexStringPrecomputationByteFacts` is a detached, one-pass projection of the
existing retained `FrozenBytes` lanes. It records canonical charset order,
row-major encoded byte lengths, and SHA-256 lanes without rereading source
text or changing the existing token/lexicon contract.

`MIndexStringPrecomputationByteFactsCodec` is a bounded, deterministic,
authenticated transport for that detached primitive projection. It composes
the existing Synexia metadata/snapshot codec boundary and OpenJDK checked
streams/digests; it is not a second source or storage authority. LeetCode,
HackerRank, and GeeksforGeeks remain algorithm-catalogue references only. No
third-party source or runtime dependency was copied. Proofs are recorded in
`docs/MINDEX_STRING_PRECOMPUTATION_BYTE_FACTS_PROOF_20260930.md` and
`docs/MINDEX_STRING_PRECOMPUTATION_BYTE_FACTS_CODEC_PROOF_20260930.md`.

## MIndexSha256Batch precomputed-facts and lazy transfer

`MIndexSha256Batch` now derives its bounded row geometry from the existing
one-pass `MIndexStringPrecomputationByteFacts` projection. Java hashing copies
the already-authenticated per-row SHA-256 facts; the optional JNI path lazily
packs the retained bytes only when selected. The `cmdrvl/hash` surface is a
bounded ordered-hashing protocol reference only. No donor source, dependency,
JNI ABI, resolver authority, or durable wire format was copied or changed.
The proof is recorded in
`docs/MINDEX_SHA256_BATCH_PRECOMPUTED_FACTS_PROOF_20261001.md`.

## MIndexAST SQLite atomic invalidation

`MIndexASTSqliteStore` now composes the existing SQLite schema and JDK JDBC transaction
semantics so exact invalidation of the base AST image and its source-index, navigation,
LCA, and canonical-pool projections is all-or-nothing. Xerial SQLite JDBC remains a
dependency/API reference already present in this module; no donor source, parser, AST
model, LRU, serializer, or new storage authority was copied. The failure-injection and
cold-replay proof is recorded in
`docs/MINDEX_AST_SQLITE_ATOMIC_INVALIDATION_PROOF_20261001.md`.

## MIndexString precomputation byte-facts owner binding

`MIndexStringPrecomputationByteFactsKey` binds the existing detached primitive byte-facts lanes
to the exact retained UTF-16 rows, canonical encodings, and row geometry of one immutable
precomputation. `MIndexStringPrecomputationByteFactsStore` composes that key with the existing
authenticated codec and same-directory atomic publication. It does not change the detached
`MSTRBF01` packet or reread source text. OpenJDK SHA-256/data-stream and file-move semantics are
platform references; no third-party source or runtime dependency was copied. LeetCode,
HackerRank, and GeeksforGeeks repositories remain algorithm-catalogue references only. The
executable proof is recorded in
`docs/MINDEX_STRING_PRECOMPUTATION_BYTE_FACTS_OWNER_BINDING_PROOF_20261001.md`.

## MIndexString precomputation snapshot root

`MIndexStringPrecomputation.snapshotSha256()` is a canonical root over the immutable row order,
retained UTF-16 units, resolver-token ID sequences, effective charset configuration, and existing
one-pass byte-facts lanes. It is a binding identity for JNI, detached transfer, and future cold
projection consumers; it does not replace the byte-facts codec, change MSTRBF01, reread source
text, or add storage authority. The first-party snapshot-root commit `18a7960293a` and OpenJDK
digest/framing semantics are references; no donor source or dependency was copied. LeetCode,
HackerRank, and GeeksforGeeks remain algorithm-catalogue references only. The executable proof is
recorded in `docs/MINDEX_STRING_PRECOMPUTATION_SNAPSHOT_ROOT_PROOF_20261001.md`.

`MIndexStringPrecomputationSnapshot` composes that root with detached UTF-16 and token-ID CSR
lanes plus the existing byte-facts packet. It is a defensive JNI/structural transfer boundary,
not a second owner or persistent codec; no source reread, re-encoding, resolver retention, or wire
format change is admitted. The proof is recorded in
`docs/MINDEX_STRING_PRECOMPUTATION_DETACHED_SNAPSHOT_PROOF_20261001.md`.

`MIndexStringPrecomputationSnapshotCodec` composes the detached snapshot with a bounded,
deterministic, authenticated `MSTRPS01` transport and nests the unchanged `MSTRBF01` byte-facts
packet. Its outer digest authenticates transport bytes while snapshot construction independently
recomputes the canonical root. It does not reread source text, invoke a resolver, retain a search
tree, change an existing wire format, or add storage authority. OpenJDK data/digest streams and
checked arithmetic are reference surfaces; no third-party source or runtime dependency was
copied. LeetCode, HackerRank, and GeeksforGeeks remain algorithm-catalogue references only. The
executable proof is recorded in
`docs/MINDEX_STRING_PRECOMPUTATION_SNAPSHOT_CODEC_PROOF_20261001.md`.

`MIndexStringPrecomputationSnapshotKey` and `MIndexStringPrecomputationSnapshotStore` address
the detached `MSTRPS01` packet by its canonical root and publish it through the existing
same-directory atomic replacement leaf. Cold reads return the detached primitive packet without a
live owner and fail closed when the key, root, digest, or packet is inconsistent. This is a
secondary projection only: it does not change `MSTRPS01`, nested `MSTRBF01`, source traversal,
resolver authority, or semantic equality. OpenJDK path/stream semantics and first-party snapshot
owners are reference surfaces; no third-party source or runtime dependency was copied. LeetCode,
HackerRank, and GeeksforGeeks remain algorithm-catalogue references only. The executable proof is
recorded in
`docs/MINDEX_STRING_PRECOMPUTATION_SNAPSHOT_STORE_PROOF_20261001.md`.

`MIndexStringPrecomputationSnapshotTokenNativeFacade` composes detached token-ID CSR rows with the
existing `IndexStringNative` hash, unsigned comparison, and search kernels. JNI remains optional;
the established Java implementations remain the semantic fallback. The facade is row-bound and
does not retain a resolver, native handle, array, source traversal, storage authority, or alter
the native ABI/MSTRPS01/MSTRBF01 formats. OpenJDK checked-index semantics and first-party
snapshot/native owners are reference surfaces; no third-party source or runtime dependency was
copied. LeetCode, HackerRank, and GeeksforGeeks remain algorithm-catalogue references only. The
executable proof is recorded in
`docs/MINDEX_STRING_PRECOMPUTATION_SNAPSHOT_TOKEN_NATIVE_FACADE_PROOF_20261001.md`.

## MIndexString compiler concatenation invariant

`MIndexStringCompilerRuntime.concat` and the value-level `concat` methods now compose existing
resolver token IDs and admit ordinary `CharSequence` segments directly into the VM-local
`ExactStringIndexResolver` dictionary. A foreign resolver is re-interned through its indexed
character view; resolver-scoped IDs are never mixed. This preserves the one-arena/immutable-token
invariant without claiming that the JVM or operating system exposes one resizable global Java
array. The builder also appends IDs without a temporary token-array projection. OpenJDK
`String.valueOf`/`CharSequence` behavior is a contract reference; no third-party source or
dependency was copied. LeetCode, HackerRank, and GeeksforGeeks repositories remain
algorithm-catalogue references only. The executable proof is recorded in
`docs/MINDEX_STRING_CONCATENATION_INVARIANT_PROOF_20261001.md`.

The varargs and iterable `MIndexString.join` overloads reuse this same first-party compiler
membrane. The JDK `String.join` behavior is retained at the delimiter/null contract boundary,
while indexed operands remain separate resolver-owned segments. No external implementation was
copied.

The native-backed rehome path adds a direct, native-order UTF-16 admission capability. It stages
indexed code units once, interns each lexical span as a native canonical atom, and composes native
IDs without a joined Java `String`. The existing native ABI version remains 2; the capability is
probed explicitly so an older library is not used for this path. No external JNI or storage code
was copied.

The literal replacement path uses the same additive JNI capability for arbitrary `CharSequence`
operands and partial source-boundary atoms. Java heap arrays are read only during admission; no JNI
pointer is retained, and no discontiguous Java `char[]` is represented as a fake array reference.

Whitespace boundary operations use the existing metric/code-point lanes and the same partial-span
composer; they do not materialize the source before trimming or blank testing. The updated proof
receipt is recorded in `docs/MINDEX_STRING_CONCATENATION_INVARIANT_PROOF_20261001.md`.

The `MIndexString.lines()` view uses a single indexed UTF-16 cursor and preserves lazy line atoms
for LF, CR, and CRLF, including terminators split across source tokens. JDK `String.lines()` is the
behavioral reference; no external implementation or dependency is copied. Whole source tokens
are retained and only boundary spans use the existing resolver/native admission path. The proof
receipt is recorded in `docs/MINDEX_STRING_CONCATENATION_INVARIANT_PROOF_20261001.md`.

`MIndexString.getBytes(Charset)` now uses the existing indexed UTF-8 encoder or a bounded JDK
`CharsetEncoder` over chunked UTF-16 units for non-UTF-8 charsets. This removes the intermediate
source `String` while retaining the public fresh `byte[]` result and JDK replacement behavior.
OpenJDK charset semantics are a reference surface; no external source or runtime dependency is
copied. The guarded proof covers UTF-8, UTF-16, UTF-16LE/BE, ISO-8859-1, ASCII, chunk boundaries,
supplementary pairs, and an unpaired surrogate.

`MIndexString.indent(int)` uses the existing indexed `lines()` cursor and builder to preserve the
same resolver/native atom membrane while matching the JDK prefix, negative-removal, line-feed, and
CR/CRLF normalization contract. OpenJDK `String.indent` behavior is a conformance reference only;
no external source or dependency is copied. The guarded proof covers positive indentation,
negative indentation, all-blank lines, `Integer.MIN_VALUE`, split-token lines, and empty input.

`MIndexString.stripIndent()` uses the same indexed line cursor and records Unicode whitespace
boundaries before composing the common outdent and normalized LF result. OpenJDK `String.stripIndent`
and its `outdent` behavior are conformance references only; no external source or dependency is
copied. The guarded proof covers trailing terminators, blank final lines, Unicode whitespace,
split-token boundaries, random UTF-16 units, and no-materialization rejection.

`MIndexString.translateEscapes()` validates and composes standard, octal, and line-continuation
escapes from indexed UTF-16 units before admitting output atoms. OpenJDK `String.translateEscapes`
is a grammar/conformance reference only; no external source or dependency is copied. Invalid input
is rejected before resolver output admission, and the proof covers all supported escape families,
token seams, unpaired UTF-16 units, malformed escapes, and the native-preferred source path.

## Mapped MIndexFile source-facts snapshot

`MappedMIndexFile.snapshotWithSourceFacts` composes the mapped semantic image with an external
source-facts sidecar only after `MIndexFileSourceFactsImage.readMappedFacts` verifies its
format-aware fingerprint, shape, checksum, metadata, and byte/token facts. It does not reread
source bytes, create a heap file, or change MIDX formats; the plain mapped snapshot remains
source-free and fail-closed. OpenJDK path/stream semantics and existing Synexia snapshot/image
owners are reference surfaces; no donor source or runtime dependency was copied. LeetCode,
HackerRank, and GeeksforGeeks repositories remain algorithm-catalogue references only. The
executable proof is recorded in
`docs/MINDEX_FILE_MAPPED_SOURCE_FACTS_SNAPSHOT_PROOF_20261001.md`.

## MIndexAST file-tier atomic invalidation

`MIndexASTTieredLoader` now composes the existing file-backed AST codecs with a
same-directory atomically published invalidation marker and per-key serialization.
The marker hides the base image and all derived sidecars while deletion is in
progress or interrupted; the next source-backed load recovers the rebuildable
family before publication. The SQLite transaction path, AST model, LRU, and image
formats are unchanged. OpenJDK file/path semantics and existing Synexia loader
contracts are reference surfaces; no third-party source or runtime dependency was
copied. LeetCode, HackerRank, and GeeksforGeeks repositories remain algorithm-
catalogue references only. The executable proof is recorded in
`docs/MINDEX_AST_FILE_ATOMIC_INVALIDATION_PROOF_20261001.md`.


## MIndex BLAKE3 immutable digest sidecar

The optional MIndexString BLAKE3 JNI lane vendors the official C implementation from
`BLAKE3-team/BLAKE3` at commit
`6aab490a26124663329dfd3961b8469f8fdb158b` (upstream C library version 1.8.7).
The copied C core, portable implementation, x86 SIMD implementations, ARM NEON implementation,
headers, and upstream Apache-2.0/CC0 license texts are retained under
`native/third_party/blake3-6aab490`.

Apache Commons Codec 1.22.1 remains the independent Java BLAKE3 oracle. The BLAKE3 sidecar is
additive: Java String hashCode, current exact SHA-256 byte facts, SHA-256 JNI/CUDA/OpenSSL paths,
resolver identity, joined storage, fuzzy indexes, and durable byte-facts formats remain unchanged.

## Joined-builder range composition references

The current-develop immutable joined-builder increment reviewed Protocol Buffers
`RopeByteString` at commit `74211c0dfc2777318ab53c2cd2c317a2ef9012de` and Netty
`CompositeByteBuf` at commit `594e01c026bba51bdf805ea3dccd90a93d25208` for
range partitioning and component-offset mechanics. Their licenses are BSD-3-Clause
and Apache-2.0 respectively. No donor source, dependency, rope/buffer payload owner,
or mutable native builder was copied. Existing Synexia joined descriptors, atom
interner, and immutable native arena remain authoritative. The bounded adaptation and
proof are recorded in
`docs/MINDEX_NATIVE_JOINED_BUILDER_RANGE_COMPOSE_CURRENT_DEVELOP_PROOF_20261003.md`.


## MIndexTupleReferences resolver-cache isolation

The tuple resolver-cache isolation atom is first-party Synexia code. It composes the existing
persistent tuple DAG with the JDK 21 `java.lang.ref.WeakReference` contract as a reference for
non-owning reachability and identity checks. No OpenJDK source, LeetCode solution, HackerRank
solution, GeeksforGeeks solution, or external runtime dependency is copied. The weak owner key
prevents resolver-dependent derived metrics, text facts, and bit signals from crossing resolver
boundaries while preserving the existing immutable tuple and JNI interfaces. The executable proof
is recorded in `docs/MINDEX_TUPLE_RESOLVER_CACHE_ISOLATION_PROOF_20261003.md`.

## MIndexFile token-range UTF-8 SHA-256 projection

This additive atom uses the OpenJDK 21 MessageDigest and UTF-8 charset contracts as platform
references and reuses Synexia's existing MatHash256, MIndexFileSourceFacts and resolver-ID
owners. Protocol Buffers RopeByteString and Apache Pekko ByteString are segmented-composition
review surfaces; LeetCode, HackerRank and GeeksforGeeks are category references. No donor source,
problem text, runtime dependency, JNI ABI, source reread or file-image format is copied. The
recipe and focused proof are recorded in
synexia-m3-recipe/recipes/mindex-file-token-range-utf8-sha256-20261004.yaml and
docs/MINDEX_FILE_TOKEN_RANGE_UTF8_SHA256_PROOF_20261004.md.

## Mechanical precompute donor convergence

The canonical mechanical-precompute lane is
`synexia-indexstring/recipes/mechanical-precompute-donors-v4-20261007`.

- Google RE2/J is pinned at `57278921a609461c14d9cdb057d7aa9511c8f7ac`
  (RE2/J 1.8, BSD-3-Clause) and remains an explicit dependency/reference; Synexia does not copy
  or fork its regex engine.
- Apache Lucene is reviewed at `3be9cd6e6629f5ac086b3a656b16c3e88bdaf801`
  (Apache-2.0) for postings separation, compact posting/external-sort and candidate-iteration
  mechanics. Synexia composes its own primitive postings/images and copies no Lucene source or
  file format.
- TweetyProject is reviewed at `5940c1a18ee762fb16055bd7b72ef19b74246f70`.
  Because recorded license signals are not treated as automatic source-copy authority, this lane
  is clean-room/reference-only and `sourceCopied=false`.
- LeetCode, HackerRank and GeeksForGeeks are problem-taxonomy/evaluation references only. Problem
  statements, editorials, articles and solution source are not copied or admitted. A separately
  pinned, license-compatible public repository must pass the ordinary donor-rights gate before
  implementation source can enter Synexia.

`MIndexMechanicalPrecompute` delegates to existing Synexia authorities and does not introduce
another regex, postings, reasoning, text or storage owner:

- RE2/J execution remains behind `MIndexRe2Plan`; the cache identity binds both the ordered
  finite-domain query-model snapshot and exact UTF-16 expression.
- Lucene-style posting conjunction uses `MIndexLucenePostingPlan` only as immutable precompute
  identity/budget metadata; execution delegates through `ChallengePostingQueries` to the existing
  `ChallengePostingQuery/ChallengePostingIntersection` cardinality/galloping/JNI-capable kernels.
- Tweety-inspired query publication remains clean-room `MIndexTweetyGraphQueryIndex`; the generic
  pass is admitted only when the scope source SHA-256 binds the complete logical graph plus
  relation kind and normalized included-node set.

The competitive catalogue maps external labels only to stable `MIndexAlgorithmPattern`
identifiers. Generic content-addressed precompute publication requires authenticated deterministic
source/query identity; Java object identity is never a persistent cache key.


### Competitive taxonomy precompute profile

`MIndexCompetitivePrecomputeProfile` is original Synexia internal metadata over the existing
LeetCode / HackerRank / GeeksForGeeks taxonomy catalogue. It retains only platform/pattern codes,
mechanical-intent bits/counts and SHA-256 identities. No problem statement, title/category payload,
editorial, article, submission or solution implementation is copied into the frozen profile.
Challenge sites remain `TAXONOMY_ONLY`; the artifact is Synexia-native and grants no source-copy,
semantic, execution, replacement or promotion authority.

### Mechanical precompute v4 additive facts

The v4 packet adds only Synexia-authored mechanical metadata on top of the existing donor boundaries:
RE2/J candidate-envelope helpers, Lucene posting range/cardinality envelopes, and
`MIndexTweetyMechanicalFacts` over the already-authenticated Synexia graph-query image.
No RE2/J, Lucene, TweetyProject, LeetCode, HackerRank, or GeeksForGeeks source body is copied by
this additive pass. Positive candidate facts never replace exact matching/posting/reasoning authority.

### Required-literal multi-pattern automaton

`MIndexRequiredLiteralAutomaton` is original Synexia general mechanical code. It consumes only
literal constraints already proved necessary by Synexia's conservative regex planner. The
multi-pattern trie/failure/output-link technique is general algorithmic knowledge; no RE2/J,
Lucene, TweetyProject, LeetCode, HackerRank or GeeksForGeeks source is copied into this owner.
RE2/J remains an admitted matcher/reference where selected and does not own this implementation.

## Google RE2/J mechanical precompute boundary

Google RE2/J is used as the bounded non-backtracking regex dependency/reference for the MIndex
regex lane.

- Project: `google/re2j`
- Reviewed revision: `57278921a609461c14d9cdb057d7aa9511c8f7ac`
- License: BSD-3-Clause
- Source copied into Synexia by this mechanical-precompute packet: **No**
- Use: regex execution boundary plus review of finite-state/non-backtracking mechanics

`MIndexRe2MechanicalFacts` is original Synexia code. It freezes conservative candidate facts
already derived by Synexia's own regex descriptors/search plans. RE2/J remains match-semantic
authority for the admitted RE2 surface; positive precompute facts never prove a match.

## Mechanical Lucene/Tweety query expansion

The Lucene adaptation also records a cost-led conjunction/disjunction schedule in
`MIndexLucenePostingPlan`: cardinality-first conjunction order and a heap-vs-linear disjunction
partition inspired by the pinned Lucene iterator architecture. These arrays are Synexia-owned,
budgeted routing metadata only; exact canonical postings and Synexia query kernels remain the
result authority. No Lucene source or runtime format is copied.


The mechanical-precompute lane additionally adapts the already recorded Apache Lucene
conjunction/disjunction/candidate-iteration direction into Synexia-owned primitive posting plans.
`MIndexLuceneBooleanPlan` and `MIndexPostingCanonicalizer` do not copy Lucene source, formats,
scorers, or index ownership.

TweetyProject remains clean-room/reference-only. The convenience query methods on
`MIndexTweetyGraphQueryIndex` are delegates over Synexia-owned `MIndexReasoningGraph`,
`MIndexReasoningClosure`, and `MIndexAttackParityIndex`; they do not implement or claim
Tweety semantics beyond the explicitly modeled Synexia relation/closure/parity contracts.

LeetCode, HackerRank, and GeeksForGeeks remain taxonomy/evaluation sources only. Their category
labels may select candidate `MIndexAlgorithmPattern` mechanics; problem statements, editorials,
and solution source are not admitted by this lane.


### P0 mechanical reasoning receipt

The additive P0 receipt includes seven deterministic derived sidecars from the already-existing
clean-room reasoning implementation: formula truth, conditional dependency, causal reachability,
formula probability aggregation, probabilistic assessment, reusable probabilistic query geometry,
and calibration statistics. This does **not** import TweetyProject code or reasoning ownership.
`MIndexReasoningPrecomputeBundle`, `MIndexProbabilisticReasoningBundle`, their frozen
formula/interpretation/probability inputs, and exact Synexia implementations remain behavioral
authority. The P0 fan-in records provenance/content hashes and retained primitive geometry only.

Probability conditioning remains a caller query over frozen truth and a caller-supplied
distribution; it is not promoted to canonical knowledge or a universal retained precompute lane.
