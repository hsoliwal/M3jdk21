# M3: Shared Structure and Reusable Computation in a Java Runtime

**Hitesh Soliwal and Contributors to the Synexia Project**  
Technical design paper • Version 0.2 • 7 October 2026

Copyright 2026 Hitesh Soliwal and contributors. Applicable licensing and
upstream attribution are described in the linked project rights notices.

## Abstract

M3 investigates a runtime architecture in which immutable values preserve shared
payload identity, ranges, and composition, while indexed metadata allows related
operations to reuse prior computation. Within the M3 programme, M3JDK21 is the
Java 21 runtime subset of the wider planned M3SDK.
The initial focus is M3 String inside `java.lang.String`, followed by regular
expressions and shared precompute, collections, and SWT/Eclipse integration.
The broader design direction also examines multilingual text resources,
language-specific ASTs, and reuse across structured data. M3SDK is the proposed
SDK delivery direction for the wider Synexia capability portfolio.
This paper describes the design premise, intended architecture, compatibility
constraints, and a reproducible evaluation programme. It presents design
hypotheses rather than measured performance results.

## 1. Motivation and scope

Immutability provides an opportunity to attach reusable knowledge to data whose
content does not change. M3 asks whether canonical payload identity and persistent
composition can make that knowledge useful across more operations than a
single-object cache. The intended benefits are less repeated scanning, copying,
allocation, and computation. Those benefits depend on workload reuse and on the
cost of establishing and retaining shared structures.

The starting point is an existing, sophisticated Java runtime, not an assumption
that the JDK lacks optimisation. Compact Strings already address representation
cost through Latin-1/UTF-16 storage [1]. The String API defines the observable
contract [2]. M3 explores a different axis: shared structure and reusable
operation metadata. Any comparison must use a specified OpenJDK baseline and
account for the optimisations that baseline already provides.

This paper focuses on runtime design. It does not establish novelty relative to
all prior research, claim universal speedups, or report a completed compatibility
qualification. A systematic related-work comparison and implementation-linked
results belong in subsequent revisions.

## 2. Design premise

The proposed representation has three cooperating layers:

| Layer | Intended responsibility |
| --- | --- |
| Canonical payload store | Immutable byte/UTF-16 payloads with stable logical identities and explicit lifetime rules. |
| Value structure | Atom references, ranges, and compositions describing a value without requiring a new owned payload for each value. |
| Indexed metadata | Shared hashes, search indexes, and reusable operation plans associated with appropriate payload or composition identities. |

A range describes part of a canonical payload. A composition describes the
ordered combination of ranges or atoms. Metadata records properties that can be
reused when their assumptions remain valid. Stable logical identifiers are
distinct from movable Java object addresses.

For example, repeated values resembling `customer:123` and `customer:456` may
share the prefix payload. That sharing alone does not eliminate the cost of
searching or hashing either complete value. The design question is which
properties can be composed, cached, or indexed economically. A whole-value hash
must still obey the specified String contract; a cryptographic digest generally
cannot be obtained by simply combining component digests.

The canonical design aims to avoid per-value owned payload arrays. APIs or native
boundaries that require contiguous storage may need a compatibility
materialization. That cost, its lifetime, and its frequency must be visible in
the evaluation.

## 3. Runtime architecture and ownership

Synexia is the canonical convergence workspace for reusable algorithms, recipes,
and provenance. M3JDK21 owns the runtime implementation. String precompute resides
within M3JDK21, in `java.base`, HotSpot, or JNI as appropriate; the runtime does not
depend on Synexia.

Three engineering routes support the programme:

1. Explicit M3 views exercise range/composition operations and contract tests.
2. Compiler and transformation recipes lower eligible operations to M3 plans.
3. Runtime integration makes M3 String the internal representation supporting
   `java.lang.String`.

These are intended routes, not interchangeable proofs of integration. A correct
explicit-view implementation does not by itself demonstrate that ordinary String
operations, VM intrinsics, or JNI use it.

VM-local canonical storage is the initial ownership boundary to qualify.
Cross-process or OS-level sharing is an extension requiring its own encoding,
synchronization, security, and lifecycle design. Metadata sidecars need bounded
retention, safe publication, and unambiguous versioning.


## 3.1 M3SDK: the wider delivery direction

The intended programme extends across Synexia rather than stopping at a String
library. **M3SDK** is the proposed SDK delivery surface for the reusable Synexia
portfolio: language and lexicon resources, exact translation, text precompute,
AST/compiler mechanisms, arrays and collections, indexed data structures,
storage, native kernels, transformation recipes and bounded integration adapters.

**Within the M3 programme, M3JDK21 is a runtime-focused subset of M3SDK.**
The wider SDK scope includes qualified language, translation, AST, tooling,
storage and integration capabilities beyond what belongs inside a JDK.

Synexia remains the canonical source and convergence workspace. M3SDK would
package qualified capabilities with explicit contracts, examples, provenance
and reproducible evidence. M3JDK21 owns the Java runtime subset and its
internal String/precompute integration. An SDK packaging goal does not add a
Synexia runtime dependency to the JDK or transfer runtime ownership.

The scope goal is to inventory every Synexia module and capability and assign
an explicit delivery disposition: qualified SDK component, product-specific
adapter, independently deployed application/service, pending qualification, or
documented exclusion. A repository-wide SDK cannot be established by renaming
packages or collecting every source directory into one artifact.

SDK components should retain API/SPI contracts, headless core implementations,
bounded bridges, optional UI and explicit Java/JNI ownership. Versioned manifests
must identify source revisions, artifact bindings, dependencies, platform support,
licenses, ABI/format boundaries and acceptance evidence. Third-party code and
datasets retain their own licensing and provenance.

This is an intended distribution and integration direction, not a declaration
that all of Synexia is already delivered as M3SDK. Component qualification follows
the existing receiving priorities and preserves prior contracts and source
history. The SDK can make reusable mechanisms available without treating one
component's tests as acceptance of the entire portfolio.


## 4. String compatibility

Representation changes must preserve externally observable String behavior [2]:
UTF-16 indexing, Unicode and surrogate handling, equality, hash codes, ordering,
search, case conversion, exceptions, and relevant serialization and native
interfaces. Identity sharing is an implementation opportunity; it does not
replace content equality for arbitrary Java strings.

Integration must also examine interpreter and compiled execution, intrinsics,
GC interactions, class loading, and affected CDS paths. Raw object addresses
cannot serve as durable identities across moving garbage collection. JNI
materialization and acquisition/release behavior require differential tests
against a Java semantic oracle.

Range views create a retention tradeoff: a small live view may retain a much
larger payload. Canonicalization also has lookup and synchronization costs.
Those costs are part of the design rather than incidental implementation details.

## 5. Regex and shared precompute

Regex work includes reusable compiled plans and indexes whose applicability is
defined by pattern content, flags, runtime assumptions, and data identity.
Java Pattern semantics remain the reference for the supported surface [3].
Captures, backreferences, lookaround, matching regions, flags, and error behavior
require explicit qualification.

A prefix/suffix or substring index can help an eligible search but cannot
generally substitute for arbitrary regex evaluation. The planner must identify
valid shortcuts and preserve a semantic fallback. Mutable matcher state belongs
to the relevant invocation or owner rather than an indiscriminately shared cache.

Precompute must be selective. Exhaustive substring metadata can grow
quadratically in input length; arbitrary regex/input combinations cannot be
precomputed without bound. Admission policies therefore need limits, reuse
signals, eviction, and version checks. Cold misses, low-reuse inputs, adversarial
patterns, and concurrent access belong in the test corpus.


## 5.1 Natural languages and exact text identity

The language direction is to preserve exact text once and associate reusable,
language-aware knowledge with it. Shared storage does not require every language
to use the same tokenizer, dictionary, collation, or interpretation. A value can
span languages and scripts while retaining its original units and range identity.

The proposed multilingual model separates three responsibilities:

| Responsibility | Proposed representation | Qualification boundary |
| --- | --- | --- |
| Exact spelling | Immutable canonical payloads and ordered ranges/compositions | Preserve the target API's indexing, equality and source text. |
| Language resources | Versioned lexicons and language/script annotations referencing payload identities | Record resource provenance, coverage, ambiguity and updates. |
| Derived operations | Bounded segmentation, search, normalization and linguistic sidecars | Bind results to the exact input and the selected operation/provider/version. |

A shared dictionary can describe known words; VM-local interning supplies a path
for names, new words, identifiers, mixed-script text and other unknown spellings.
Whitespace and punctuation remain explicit content. Dictionary membership is not
a prerequisite for storing or operating on an arbitrary String. Numeric logical
IDs need an explicit namespace and generation; a compact ID width is an
engineering choice whose capacity and overflow behavior must be qualified.

Exact storage interning must remain distinct from Unicode normalization.
For example, a precomposed accented letter and a combining sequence can be
canonically equivalent while having different units. Normalization is a selected
transformation with its own semantics [5]; sharing a payload does not authorize
silently changing its spelling, offsets or Java String equality.

UTF-16 units, code points and user-perceived characters also require different
coordinate systems. Grapheme and word boundaries can use specified segmentation
rules and tailoring [6]. Each sidecar must declare its coordinate system and
retain source-offset mappings when a transformation changes length. The runtime's
qualified Unicode data version must be recorded; citing a newer Unicode document
does not establish that an older JDK implements that version.

The donor's fixed 100-language registry is already implemented, as detailed
below. Broad corpus coverage and complete resources for those slots remain
research and qualification goals.
Initial qualification should name each corpus and cover Indic combining sequences,
Arabic and bidirectional text, CJK text without space-delimited words, Latin
variants, emoji sequences and code-switching. Language-specific stemming,
synonyms, transliteration, sentiment and contextual translation are proposed
optional providers; the existing exact lexical translation mechanisms are
identified below. Exact search remains separately available, and linguistic similarity
never substitutes for String equality. Resource licensing, preparation cost,
retained size and disagreement between providers belong in the evidence.


### Existing hierarchical lexicons and translation mechanisms

This direction already has concrete donor implementations. The following source
inspection is pinned to Synexia commit `d9098bb5341a1b95750814044b8bb52616cc2c61`.
It identifies mechanisms to reproduce faithfully; it does not establish that
M3JDK21 has received or qualified them.

| Existing owner | Mechanism observed in source | Identity boundary |
| --- | --- | --- |
| [DictLang LexiconUniverse](https://github.com/hsoliwal/com.synexia/blob/d9098bb5341a1b95750814044b8bb52616cc2c61/synexia-dictlang/shared/core/src/main/java/com/synexia/dictshared/core/LexiconUniverse.java) | Parent-path lookup, exact literal admission, shared spelling IDs, concept-to-language surfaces, phrases and indexed relations | Lexicon coordinates, spelling identity, canonical aliases and concept identity are distinct. |
| [DictLang HierarchicalIndexStore](https://github.com/hsoliwal/com.synexia/blob/d9098bb5341a1b95750814044b8bb52616cc2c61/synexia-dictlang/shared/core/src/main/java/com/synexia/dictshared/core/HierarchicalIndexStore.java) | Mapped standard dictionaries, specialized namespaces, dynamic intern pools and canonical composite strings | Live handles belong to the store scope; persisted handles bind to their exact snapshot. |
| [IndexString IndexSymbolId](https://github.com/hsoliwal/com.synexia/blob/d9098bb5341a1b95750814044b8bb52616cc2c61/synexia-indexstring/src/main/java/com/synexia/indexstring/IndexSymbolId.java) | Packed language/namespace/entry coordinate | Resolver ownership and encoding must accompany a numeric ID. |
| [Common LanguageCatalog](https://github.com/hsoliwal/com.synexia/blob/d9098bb5341a1b95750814044b8bb52616cc2c61/synexia-common/src/main/java/com/synexia/common/language/LanguageCatalog.java) | Exactly 100 fixed language codes, with English permanently ID 0 and the other 99 sorted | Registry membership establishes an addressable slot, not dictionary completeness or translation quality. |
| [Common FrozenIntTranslationTable](https://github.com/hsoliwal/com.synexia/blob/d9098bb5341a1b95750814044b8bb52616cc2c61/synexia-common/src/main/java/com/synexia/common/language/FrozenIntTranslationTable.java) | Dense direct arrays or sparse primitive open-addressed lexical mapping tables | Token IDs remain scoped to the admitted vocabulary and language-pair mapping. |
| [IndexString exact translation bridge](https://github.com/hsoliwal/com.synexia/blob/d9098bb5341a1b95750814044b8bb52616cc2c61/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexStringTranslationBridge.java) | Admitted source IDs project to target IDs without joined source spelling in the translation loop | One target resolver owns each result; unmapped rows require explicit REJECT or DROP. |
| [IndexString translation bundle](https://github.com/hsoliwal/com.synexia/blob/d9098bb5341a1b95750814044b8bb52616cc2c61/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexLanguageTranslationBundle.java) | Composes already-admitted family dictionaries through the same bridge | Profiles and row conflicts are checked; composition does not infer grammar. |

The hierarchy extends beyond ordinary word dictionaries: names, places, titles,
acronyms, equations, units, identifiers, URLs, code, whitespace, punctuation,
phrases and exact unknown literals all have explicit classifications.
Shared spelling does not erase lexical distinctions. For example, entries for
`May` can share spelling storage while retaining different coordinates;
curated concept IDs can connect different language surfaces without declaring
their spellings equal.

There are multiple existing coordinate contracts, not one interchangeable
integer ABI. [LexiconCoordinate](https://github.com/hsoliwal/com.synexia/blob/d9098bb5341a1b95750814044b8bb52616cc2c61/synexia-dictlang/shared/api/src/main/java/com/synexia/dictshared/api/LexiconCoordinate.java)
uses a 32-bit lexicon lane and 32-bit entry lane, with positive validated IDs.
The [hierarchical IndexString store design](https://github.com/hsoliwal/com.synexia/blob/d9098bb5341a1b95750814044b8bb52616cc2c61/synexia-dictlang/shared/docs/HIERARCHICAL-INDEXSTRING.md)
describes a store-scoped 16-bit namespace/48-bit entry handle.
IndexString's `IndexSymbolId` uses a 16-bit language lane, a reserved clear bit,
a 15-bit namespace lane and a 32-bit entry lane. A migration must preserve or
explicitly adapt each contract, including its scope and validity rules.

Translation has two complementary substrates. Concept-to-surface relations
retain language-specific alternatives; admitted lexical projections use exact
source-to-target rows. The exact bridge checks source resolver ownership and
writes a target-owned composition. DROP deliberately omits unmapped tokens and
can change content; REJECT is the loss-averse boundary. Callers must choose the
policy, and a retained source ID must not silently enter another resolver space.

The [2 October bundle record](https://github.com/hsoliwal/com.synexia/blob/d9098bb5341a1b95750814044b8bb52616cc2c61/synexia-indexstring/docs/MINDEX_LANGUAGE_TRANSLATION_BUNDLE_PROOF_20261002.md)
documents deterministic composition of four selected lexical families.
The [full-language composition record](https://github.com/hsoliwal/com.synexia/blob/d9098bb5341a1b95750814044b8bb52616cc2c61/synexia-indexstring/docs/MINDEX_FULL_LANGUAGE_TRANSLATION_COMPOSITION_PROOF_20261002.md)
describes explicit bundle admission and first-touch loading.
The [bridge tests](https://github.com/hsoliwal/com.synexia/blob/d9098bb5341a1b95750814044b8bb52616cc2c61/synexia-indexstring/src/test/java/com/synexia/indexstring/MIndexStringTranslationBridgeTest.java)
cover composed values, unmapped rejection, foreign resolvers, missing rows and
typed units. These are existing source/tests and historical focused receipts;
this paper revision did not rerun them or resolve their documented reactor and
filesystem gaps.

The research contribution to evaluate is the combination: exact shared text,
hierarchical lexical classification, preserved compositions and bounded
precomputed relations can let eligible operations work on qualified identities
until output materialization. General sentence translation still needs
context, grammar, ambiguity resolution and quality evaluation; a lexical table
alone establishes none of those. Arbitrary N-token projection and rendering
remain proportional to the data they process.


## 5.2 Programming languages, ASTs and derived structures

The same design premise can be investigated for source code: canonical text
ranges provide spelling custody, while language-specific parsers establish syntax
and semantics. Tokens, syntax trees, symbol tables, dependency graphs and
transformation plans are derived structures with explicit owners and assumptions.

Repeated spelling is not repeated meaning. Two occurrences of `value` can share
text storage while referring to different declarations; two identical source
fragments can have different meanings in different scopes or build environments.
AST nodes therefore retain their language, grammar/compiler version, source
snapshot, location and binding context. A structural hash can select candidates
for reuse; collision checks and the required structural/semantic comparison
govern acceptance.

For example, editing a comment might leave many language-level bindings intact,
yet source positions can move. An incremental plan must distinguish reusable
structure from coordinates and diagnostics that need recomputation. Changes to
imports, classpaths, compiler options or dependencies can invalidate results
without changing the spelling of a subtree. Cross-language adapters may share
storage and selected representations, but do not grant grammar equivalence or
permission to apply one language's refactoring rules to another.

Synexia remains the convergence workspace for these recipes and qualified
algorithms. Product integrations receive explicit adapters and evidence.
The existing receiving order remains String, arrays, collections, AST/compiler,
then remaining families; describing AST opportunities does not advance that
qualification stage. Multi-language compiler integration is a proposed extension,
not a claim of a universal parser or completed compiler replacement.

## 5.3 Reuse across collections and other data structures

Arrays, collections, trees and DAGs offer related reuse questions, with different
mutability and identity contracts. Immutable components may share payloads and
metadata. Mutable structures require snapshots, generations or invalidation
before an earlier result can be reused.

A proposed reuse key binds the owner and snapshot/generation, exact range or
composition, operation, parameters, provider and relevant dependency versions.
An admission decision also records the requested budget. These fields describe
validity requirements, not a mandate to allocate a wrapper for every result;
indexed sidecars can store them in bounded primitive lanes.

The working principle is: establish reusable facts when their expected reuse
justifies preparation, then answer eligible operations through qualified lookups.
A miss or an invalid generation follows the normal computation path.
Persistent caches, process-shared storage and cross-language plans need separate
lifecycle and concurrency qualification. No global table of every possible
substring, pattern, language or structural relation is assumed.

The broader research question is whether exact shared representation and bounded
derived knowledge can reduce repeated work across text and structured data.
Its value must be measured separately for each operation and workload, including
the cost of preparation, lookup, invalidation and retained storage.

## 6. Collections and desktop integration

After runtime qualification, collections can explore reuse of canonical keys,
indexed signals, and search metadata. Equality, ordering, null handling,
iteration, views, concurrency, and serialization contracts remain decisive.
Sharing must not silently turn an equality-based collection into an
identity-based collection.

SWT, Eclipse Platform/UI, Nebula, and GEF are integration targets. Their proposed
role is to consume qualified runtime and collection improvements through bounded
adapters and recipes. Responsiveness, resource use, and target compatibility need
application-level measurements; a String microbenchmark cannot establish an IDE
benefit.

TornadoVM provides a separate exploration surface for suitable accelerated
precompute. CPU execution remains primary. Transfer, preparation, synchronization,
and device-memory costs must be included when evaluating acceleration. Tooling,
provider catalogs, and repository-service forks support the wider programme;
they do not become independent owners of the M3 String runtime.

## 7. Evaluation programme

The evaluation tests hypotheses, including outcomes that may reject them:

| Hypothesis | Experiment | Main counter-cost |
| --- | --- | --- |
| Shared payloads reduce storage for repeated structure | Compare duplicate-rich, composition-rich, and unique-input corpora | Canonical lookup, structure size, and retained payloads |
| Shared metadata reduces repeated-operation work | Measure cold construction and repeated search/hash/regex operations | Index construction, misses, eviction, and native memory |
| Runtime changes preserve required behavior | Differential API tests and relevant JDK/VM/native tests | Semantic differences and integration gaps |
| Runtime benefits reach applications | Collection and SWT/Eclipse workload measurements | Adapter overhead, startup, and UI latency |

Use JMH where appropriate for microbenchmarks [4], together with application
workloads. Bind results to candidate and baseline commits, binary checksums,
hardware/OS, flags, corpus seeds, and reproduction commands. Preserve warmup,
independent forks, raw iterations, and variance. Avoid timing only the warm
operation while hiding preparation.

Measure throughput/latency, CPU time, allocation, retained heap, native memory,
GC, startup, and precompute preparation/storage. Include Latin-1 and UTF-16,
supplementary characters, unpaired surrogates, empty/large inputs, repeated and
one-off workloads, and contention. The planned regex corpus contains at least
10,000 deterministic cases; case count is a coverage target, not a substitute
for semantic breadth.

A simple break-even model makes reuse assumptions explicit. If preparation costs
P, baseline operation cost is B, and reused operation cost including lookup is R,
then reuse can amortize preparation only when B > R and the operation count
exceeds P/(B-R). This model excludes memory opportunity costs and contention;
measurements must include those separately.

The repository's [benchmark and compatibility specification](../release/BENCHMARK_AND_COMPATIBILITY_SPEC.md)
provides the detailed evaluation contract. Quantitative findings should be
published only with their raw evidence and reproducible commands.

## 8. Development plan

The sequence is source inventory and faithful reproduction, String integration,
regex/precompute, runtime qualification, collections, and desktop integration.
Reusable transformations converge in Synexia before product application.
Compiler and contract tests govern promotion; provenance and upstream licensing
remain explicit.

The [home-page roadmap](../../README.md#project-goals-and-plan) records milestone
deliverables and acceptance criteria. This paper is a design account, not an
implementation inventory. Future revisions should link individual architectural
elements to implementation commits and evidence, add related-work analysis, and
report both positive and negative results.

## 9. Invitation

The opportunity is to make immutable structure useful beyond storage: as a basis
for reusable computation across a Java runtime. Readers are invited to examine
the representation, challenge the cost model, identify compatibility boundaries,
and contribute reproducible workloads. The value of M3 will be established by
where this design helps, what it costs, and how faithfully it preserves the
contracts applications rely on.

## References

1. OpenJDK. [JEP 254: Compact Strings](https://openjdk.org/jeps/254).
2. Oracle. [Java SE 21 String API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html).
3. Oracle. [Java SE 21 Pattern API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html).
4. OpenJDK. [Java Microbenchmark Harness](https://github.com/openjdk/jmh).
5. Unicode Consortium. [Unicode Standard Annex #15: Unicode Normalization Forms](https://www.unicode.org/reports/tr15/), consulted 7 October 2026.
6. Unicode Consortium. [Unicode Standard Annex #29: Unicode Text Segmentation](https://www.unicode.org/reports/tr29/), consulted 7 October 2026.

## Authorship and provenance

This paper records the M3 design direction attributed to Hitesh Soliwal and
Contributors to the Synexia Project. References identify background and contract
sources; citing or integrating upstream work does not transfer its ownership.
Applicable rights and licensing boundaries are described in
[M3-SYNEXIA-NOTICE](../../M3-SYNEXIA-NOTICE.md) and
[M3-SYNEXIA-RIGHTS](../../M3-SYNEXIA-RIGHTS.md).

For citation in JEPs, standards submissions, or downstream publications, see the
[proposal attribution policy and versioned publication record](../../M3-SYNEXIA-RIGHTS.md#jeps-standards-proposals-and-technical-publications).

### Revision and filing intent

Version 0.2 extends the design account with natural-language resources,
programming-language AST/binding boundaries, bounded reuse across other data
structures, and the wider M3SDK delivery direction for Synexia. The [v0.1 publication remains available at its original commit](https://github.com/hsoliwal/M3jdk21/blob/ace350f716614b50cf77d7462b1762b7eb8664ca/m3/papers/M3_SHARED_STRUCTURE_AND_REUSABLE_COMPUTATION.md).
Earlier source and design records remain relevant to chronology and attribution.

As of 7 October 2026, the author has expressed an intention to pursue patent
filing. This paper records that intention; it establishes no filed application,
pending status, patent grant, patentability or exclusive invention claim.
The applicable copyright and patent permissions in existing licenses remain
controlling. The original expressive account and implementation provenance are
covered by the project rights notice; abstract methods are not reclassified as
copyrighted expression.
