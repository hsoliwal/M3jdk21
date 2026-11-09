# M3String leverage in the collection programme

Date: 2026-10-21. Disposition: read-only, targeted source assessment and proposed all-family evaluation matrix. This is not a full repository census, completed transformation, benchmark, or merge.

## Decision

String-keyed and String-valued collections are an explicit axis of the existing collection programme. Container arrayification and text representation are separate optimization axes. Public String and collection APIs remain unchanged. M3JDK21 is the integrated product; no additional runtime wrapper, mandatory Synexia service, or duplicate string pool is proposed.

## Current source observations

Pinned M3JDK21 revision: `a4415af418a9b0f21a06cd5801d238577fbb6eb7`.

1. M3String retains an immutable owner plus packed range coordinate. The current owner hierarchy includes atom, tuple and immediate forms. It is not a globally unique content-ID guarantee for all equivalent constructions.
2. M3StringOwner holds length, coder, Java hash, structural hash and lazily prepared facts, including four bounded owner-local range-fact entries.
3. M3String.hashCodeValue uses owner.javaHash for whole-owner values and range facts for slices. sameCoordinate compares owner reference and packed coordinate. identityHash64 mixes owner identity, coordinate and structural hash; it is not a content hash.
4. M3String.contentEquals has an exact same-coordinate shortcut, eligible hash mismatch rejection, and exact character fallback. Unequal coordinates do not prove unequal text.
5. Ordinary String.equals dispatches through M3StringPrecomputeBridge when either side has M3 storage. Ordinary String.hashCode retains its existing per-object cache and calls the bridge for an M3-backed value.
6. M3SlotArrays.hash calls the original key.hashCode and spreads the result. M3LinkedHashMap.Table.find retains query-first Objects.equals; keys are stored in Object[], hashes in int[], and order/index state in primitive arrays. Rehash already reuses stored hashes.

Therefore an existing map of actual String objects can reach M3-aware hashing and equality through its ordinary calls. This establishes a source path, not measured speed or full runtime qualification. It does not require replacing Map<String,V> with Map<M3String,V>.

## Proposed internal optimization rule

Reuse the immutable text owner/range, existing content hash and exact comparison. Preserve the collection's key representatives, equality direction, null and ordering rules, callback effects, lifetime and concurrency. Collection metadata may retain indexes or qualified handles; it must not create another text payload owner.

The safe lookup path is: validate using the existing owner -> inspect already available String storage without admission -> reuse the Java content hash -> probe the existing index -> take proven same-value shortcuts -> exact equality fallback -> return the original representative/value under the original collection semantics. Non-String queries keep their original generic path.

M3String.canonicalize may admit a previously flat string. Calling it for every get/contains/remove would turn miss-heavy reads into interning work; this is not an admitted optimization. Lack of a canonical handle does not prove absence unless every stored key is covered by the same exact dictionary domain.

## Where to evaluate

ACTION_QUEUE.tsv is a proposed family/operation matrix, not an assertion that its rows were exhaustively inspected. It covers maps/sets; bags and multisets; multimaps, bimaps and tables; sorted/navigable collections; immutable/persistent collections; prefix indexes and labelled graphs; streams/grouping/joins; composite keys; String values in sequences; mixed-key collections; concurrent collections; identity collections; weak/reference collections; and downstream String-key clients.

Reuse collection array/index/mask primitives after semantic selection. String keys can nominate slots, and existing gather, compaction, ordering, merge and window operations can operate on those slots. Live filtered views cannot be converted to snapshots. Arbitrary callbacks cannot be cached merely because their input strings are immutable.

## Non-negotiable discriminators

- Java object identity, immutable storage coordinate and logical text equality are distinct.
- Same exact owner/range is a positive equality shortcut; different owners, ranges or composition shapes require exact comparison when content equality is possible.
- Never substitute identityHash64, structuralHash64, raw pointer, SimHash or a non-collision-free signal for a content-key hash/equality contract.
- String.equals must retain its String type restriction; a CharSequence with equal characters is not automatically an equal String key.
- Natural String order is UTF-16 lexical order, not dictionary-ID order or arbitrary byte order. User comparators and locale/case policies remain authoritative.
- IdentityHashMap must retain reference equality. Weak-key storage must not become strongly retaining through a handle cache or pool. Concurrent maps retain publication and linearization guarantees.
- Preserve original inserted representatives where current behavior exposes them, stream encounter order and duplicate occurrence identity. A canonical spelling is not a token occurrence, graph node, scope binding or list position.
- Mutable generic keys retain existing behavior; String specialization needs actual runtime-type and ownership proof, not a generic signature alone.
- Handles, if admitted, need namespace and generation/lifetime rules. Persisted or cross-process IDs need dictionary identity; raw native addresses are not portable identifiers.
- A tiny slice may retain a large owner. Measure owner lifetime and retained native/heap memory, not just allocated bytes.

## Candidate algorithms and reuse

The existing LeetCoder, atomizer, patternizer, catalogue and source-sealed recipe owners remain canonical in Synexia. This assessment does not add a new recipe engine. Extend existing category records for exact string lookup, dictionary/range identity, prefix lookup, lexical range search, stable grouping and index selection. LeetCode-style patterns nominate candidates; collection contracts decide admission.

Lucene BytesRefHash is a reference for byte-to-ID indexing and non-admitting find, including reuse by pool offset; its storage and byte semantics are not a drop-in replacement for M3String or Java String. Do not copy its payload into a second pool simply to adopt the pattern.

For compound strings, the derived Java-hash identity is h(A||B)=h(A)*31^|B|+h(B) modulo 2^32 over UTF-16 code units. Reuse existing summaries when available; the equation does not establish that every needed range/power summary is already implemented or constant-time.

## Acceptance workload

Compare original and proposed implementations using repeated identical String objects, equal distinct objects, equal text with different owners/DAG partitions, flat/M3 mixed keys, slices, empty/zero-hash values, Aa/BB collisions, Latin-1 and UTF-16 including unpaired surrogates, raw non-String probes, nulls, custom comparators, live/reversed/range views, callback reentrancy, weak-key collection and concurrent mutation. Read misses must not grow admission state.

Measure key-construction/admission cost separately from steady-state lookup. Include cold and warm facts, short and long keys, low and high repetition, miss-heavy traffic, repeated prefixes, sparse domains, multiple collections sharing owners, deletion/eviction and GC/native owner reclamation. Report CPU distribution, transient allocation, retained heap, native bytes and pool retention. Do not claim O(1) processing of previously unseen arbitrary-length text.

## Execution boundary

Executed: GitHub reads at the pinned revision, targeted code tracing, official Java/Lucene documentation review, creation of this local analysis packet and its hashes.

Not executed: a full collection/fork/caller census, any production patch, recipe application, compilation, tests, resource benchmarks, upstream publication or merge. Direct container network retrieval failed with DNS resolution error; connected GitHub retrieval succeeded. No source bytes or default branches changed.

Source references and inspection ranges are in PROVENANCE.tsv; next decisions are in ACTION_QUEUE.tsv. All candidate statuses remain evaluation-required.
