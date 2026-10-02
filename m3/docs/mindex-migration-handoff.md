# MIndex and MatIndex to M3 migration handoff

Status: **proposed migration specification, not an implementation or compatibility certification**.
Snapshot: 2026-10-02. Source discovery pin: `hsoliwal/com.synexia@bc49cd7610bf5974f6baa31ba64e6885f4117528`.
Target documentation base: `hsoliwal/M3jdk21@8bb6215372e07712f1fdf5a0cb912af495007b19`.

## Whole-JDK scope relationship

This handoff is the deep MIndex/MatIndex slice of the larger JDK program; it no longer defines the outer migration scope. Use [whole-jdk-migration-scope.md](whole-jdk-migration-scope.md) as the program overview, [whole-jdk-subsystem-matrix.md](whole-jdk-subsystem-matrix.md) for module-level planning dispositions, [../migration/docs/ARCHITECTURE.md](../migration/docs/ARCHITECTURE.md) for shared architecture, [whole-jdk-collections-replacement.md](whole-jdk-collections-replacement.md) for the canonical collection design, [../migration/docs/COLLECTIONS.md](../migration/docs/COLLECTIONS.md) for its execution overlay, and [../migration/docs/WORK_PACKETS.md](../migration/docs/WORK_PACKETS.md) for dependency-ordered subsystem packets.

Its dated source census, ownership distinctions, String/structure contracts and evidence limitations remain valid only for the exact scope and pins stated below. Do not reinterpret this slice as whole-JDK completion or as permission to collapse unrelated JDK subsystems into MIndex text representations.

## Read this first

The requested outcome is one coherent M3 architecture with a durable source-to-target map so future Synexia enhancements can be ported deliberately. It is not a global search-and-replace of MIndex or MatIndex, and it is not a promise that all indexed values become java.lang.String.

This handoff extends, rather than replaces:
- [Shared atoms and reference-only concatenation](shared-atom-concatenation.md): text ownership, no-copy joins, encoding, JNI and VM constraints
- [Existing Synexia reconciliation](synexia-reconciliation.md): retained owners and provisional P0 differences
- [Existing name map](name-mapping.json): historical, intentionally incomplete mapping
- [Mapping lifecycle](migration-mapping-lifecycle.md): proposed record format, review rules and enhancement replay
- [Acceptance matrix](migration-acceptance-matrix.md): work for implementers and reviewers
- [Worked text trace](migration-worked-text-trace.md): mixed owners, coordinates, slicing, regex and encoding across three routes
- [Worked port decisions](migration-worked-port-decisions.md): operational mapping crosswalk, enhancement conflicts and family decision cards
- [Source filename census](migration-source-census.tsv): source path and Git blob identity only

Nothing in this PR changes production source, Maven profiles, runtime flags, licenses or tests. Target names below are proposals. The mapping example is not evidence that anything has been ported.

## 1. Scope and inventory confidence

The scope of the eventual migration is every relevant MIndex*/MatIndex* family, plus dependencies that own its data, APIs, images, recipes, generated sources, native interfaces and tests. Scope includes text, matrices, collections, AST/DAG, objects, paths, algorithms, data structures, tools, libraries, frameworks, viewers and adapters. The same prefix does not imply the same representation.

The checked-in census contains **2,453 production Java filenames** matching MIndex*, MatIndex* or SubMIndex* in eight recursively inspected, non-truncated source subtrees. It records paths and blob SHAs at the source pin:
- synexia-indexstring: 959
- synexia-mindex: 1,317
- synexia-mindex-indexstring-bridge: 12
- synexia-indexstring-compiler: 92
- synexia-index-precompute: 43
- synexia-index-precompute-adapters: 4
- synexia-indexstring-maven-plugin: 26
- synexia-indexstring-jini: 0 matching filenames

Zero matching filenames does not mean no integration work. For example, a resolver, FrozenChars, a native header or an image codec may own essential behavior without the prefix. The small synexia-mat-index-string subtree was also inspected; its name must not be mistaken for the owner of every MatIndex implementation.

This is a **bounded filename inventory, not an exhaustive repository symbol inventory or semantic review**. It excludes nested declarations, nonmatching dependencies, tests, generated outputs outside src/main, resources, native symbols and other repository modules. Implementers must close those gaps before claiming “all migrated.” Never turn this census automatically into 2,453 approved renames.

## 2. Verified ownership landmarks

Paths in this section refer to the pinned source tree. The primary structural owners are in com.synexia.indexstring; similarly named collections/structure/graph classes are separate surfaces, not replacements selected by spelling.

| Family / source landmark | Established distinction | Proposed M3 placement; decision still required |
| --- | --- | --- |
| [com.synexia.indexstring.MIndexString](https://github.com/hsoliwal/com.synexia/blob/bc49cd7610bf5974f6baa31ba64e6885f4117528/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexString.java) | Resolver/token-coordinate text family; lexical identity must remain explicit | com.m3.text.M3String facade over selected existing text owners |
| FrozenChars, FrozenBytes, LocalM3StringPiece, FrozenByteInterner, MIndexJoinedChars / MIndexJoinedBytes in synexia-indexstring | Immutable backing, local ownership, derived encoded forms and joined geometry are distinct responsibilities | com.m3.storage and com.m3.text, with provenance-preserving adapters before any relocation |
| com.synexia.indexstring.MatIndexString and com.synexia.indexstring.mat.MatIndexString | Distinct qualified names, associated builders/maps/pools/runtime surfaces | com.m3.text.matrix.M3TextMatrix and compatibility facades; audit contracts separately |
| [MIndexAtomStore](https://github.com/hsoliwal/com.synexia/blob/bc49cd7610bf5974f6baa31ba64e6885f4117528/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexAtomStore.java) | Immutable primitive lanes; resolver/language payload coordinates; shared structural atom arena | com.m3.structure.M3AtomStore |
| [MIndexAst](https://github.com/hsoliwal/com.synexia/blob/bc49cd7610bf5974f6baa31ba64e6885f4117528/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexAst.java) | Parser-neutral immutable AST over MIndexAtomStore; ordered children and explicit AST spec | com.m3.structure.M3Ast; parser libraries stay adapters |
| [MIndexDag](https://github.com/hsoliwal/com.synexia/blob/bc49cd7610bf5974f6baa31ba64e6885f4117528/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexDag.java) | Same atom store, primitive edge lanes, modes/kinds/types, topological and CSR indexes | com.m3.graph.M3Dag, retaining typed interaction semantics |
| [MIndexInteraction](https://github.com/hsoliwal/com.synexia/blob/bc49cd7610bf5974f6baa31ba64e6885f4117528/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexInteraction.java) | Role-by-role-by-mode classifier; not the same thing as an interaction store | com.m3.semantic.M3Interaction; preserve table semantics and enum persistence decisions |
| com.synexia.mindex.MIndexString, its pool and experiment.MIndexString | Whole-string pool and experimental token-space families are separate from canonical IndexString resolver identities | Compatibility lane; do not collapse IDs into the canonical resolver |
| [MIndexStringConversionBridge](https://github.com/hsoliwal/com.synexia/blob/bc49cd7610bf5974f6baa31ba64e6885f4117528/synexia-mindex-indexstring-bridge/src/main/java/com/synexia/mindex/indexstring/MIndexStringConversionBridge.java) | Explicit content conversion between runtime pool, experiment token space and canonical IndexString; receipts avoid owner-local IDs | com.m3.compat conversion layer; record current materialization honestly |
| com.synexia.mindex.MIndexObject, MIndexObjectPool / Shape / Program / State | Runtime object family with its own handles and operations | com.m3.object.M3Object family, not a text alias |
| foundation.MIndexPath, runtime MIndexPath / MIndexPathIndex and structure.MIndexPath | Multiple path APIs exist; coordinate and normalization contracts need reconciliation | com.m3.path family with qualified compatibility map |
| foundation.MIndexAlgorithm and algorithm.MIndexAlgorithm | Foundation and dedicated module surfaces both exist | com.m3.algorithm; distinguish description/catalog from executable provider |
| foundation.MIndexDataStructure and structure.MIndexDataStructure | Foundation and dedicated data-structure module surfaces both exist | com.m3.structure; preserve topology and mutation/version models |
| foundation.MIndexTool / Library / Framework and dedicated tool / library / framework modules | Real families, including registries, requests/results, entries and snapshots | com.m3.tool, com.m3.library, com.m3.framework; dependencies remain explicit |
| Regex, search, precompute, language/model vocabulary and word facets | Derived indexes and semantic projections must identify their base owner and version | com.m3.search / precompute / language; no new universal owner by default |
| Viewer, Swing/Eclipse/Jini, compiler, native and GPU adapters | Consumers and projections over owners; service registrations and binary names matter | Adapter modules outside the minimal storage substrate |

The six foundation landmarks are under synexia-mindex/foundation/src/main/java/com/synexia/mindex/foundation. The dedicated families are under synexia-mindex/{algorithm,data-structure,tool,library,framework}/src/main/java. Runtime objects and paths are under synexia-mindex/runtime/src/main/java/com/synexia/mindex. The census supplies exact paths for every matching file within the scanned scope.

Word-related facets, vocabularies and signal projections are in scope. A standalone canonical MIndexWord owner has **not been established by this review**; do not invent one or silently equate a semantic word, lexical token, language coordinate and UTF-16 atom.

## 3. Target layering and naming decisions

Proposed dependency direction:

1. Storage owners: immutable local payload, mapped image owner, namespace/generation and bounded derived facts
2. Text and structural values: ranges, compositions, atom store, AST, DAG, object and path views
3. Domain services: search, regex, language facets, algorithms, tools, libraries and frameworks
4. Integration: compatibility facades, compiler lowering, native/GPU providers, UI and complete modified JDK

Lower layers must not initialize higher-level registries, network clients, user interfaces or language packs during String bootstrap. A viewer row ID is not a storage identity. A GPU buffer is a disposable projection, not the canonical owner.

Before accepting any proposed class name, check the target package/module for collisions, existing P0 types and exported API commitments. Keep old qualified names as additive facades where source/binary compatibility requires them. Record a deliberate split or many-to-one consolidation rather than overwriting similarly named classes.

The existing com.m3.text.LocalM3StringPiece P0 is a provisional byte-format experiment. It is not already compatible with com.synexia.indexstring.LocalM3StringPiece. Preserve the established owner until an explicit contract-and-adapter decision is reviewed. Moving code into java.lang is a special complete-JDK decision, not the default destination for application families.

## 4. Canonical storage and identity contract

Canonical payload must have exactly one declared authority for each identity domain. A descriptor retains an owner or validated lifetime handle plus namespace, generation, atom/record identity and range. Persistent records contain offsets and versioned identifiers, never Java references or process addresses.

Distinguish:
- Exact UTF-16 content equality and Java content hash
- Resolver lexical IDs and language coordinates
- Local pool record IDs and owner generations
- Structural atom IDs, node IDs, edge IDs and graph versions
- Composition/tuple identities
- Java object identity, monitor behavior and String.intern behavior
- Native handles, mapped-image offsets, GPU projection rows and receipt hashes

Numeric equality across domains proves nothing. Cross-owner conversion must admit or translate content under a declared policy and return a traceable result. A hash is a filter or integrity claim under its algorithm; collision checks and exact equality still matter.

The verified conversion bridge already makes this boundary explicit and currently materializes ordinary text in the inspected conversion methods. Preserve that disclosure until a separately verified owner-compatible no-copy path exists.

### Shared lexicon and local misses

Publish immutable, versioned, licensed lexicon images. English can be warmed after bootstrap; language/domain shards may load lazily. A missing shard or unknown token must fall back to local immutable storage while preserving exact input, without modifying the shared dictionary implicitly.

File-backed pages can be shared by independent JVMs mapping the same underlying immutable file. Their virtual addresses and object identities differ. Read-only mapping alone cannot prevent external mutation or truncation. Enforce the publication/lifetime contract and retain old generations while live values refer to them.

Keep user application text and secrets out of cross-user persistent dictionaries unless separately authorized. Derived indexes and sidecars can be rebuilt, but must bind the canonical namespace, format and validated generation/prefix. Corruption must not silently redefine text or graph identity.

### No-copy composition, slicing and materialization

Conceptual example: A contains "Who ", B contains "am I". A joined descriptor references A[0,4) and B[0,4), length eight. It does not need a third eight-character payload. It still needs metadata, lifetime ownership and possibly canonical-lookup work.

A binary rope join may be constant metadata work before balancing; flattening a segment directory is proportional to segment count. Admission may read every input character. Search, encoding and equality can scan content. Do not advertise blanket O(1), zero allocation or a CPU saving without operation-specific evidence.

Slicing retains ranges and owners. No-copy small slices can retain large mappings. A separately named compaction/materialization operation may trade copying for retained memory; do not hide that tradeoff.

Contiguous stock-String, char[], byte[], serialization and native ABI outputs may require materialization. Write directly into the requested destination when possible. Cache optional flattened/encoded forms under explicit budgets; eviction cannot invalidate live values.

## 5. The three routes to the same storage design

These routes share ownership and exact-content contracts. They do not share an identical compatibility claim.

### Route A: explicit Java view

An application explicitly uses a CharSequence-style M3 view, joined values and range-aware consumers on a stock JVM. This is the smallest independently reviewable route.

Conceptual flow, not an existing API promise:

    left = admit("Who ")
    right = admit("am I")
    joined = joinViews(left, right)
    consumeCharSequence(joined)
    legacyStringOnlyApi(materializeString(joined))

admit, joinViews and materializeString above are explanatory placeholders. Implementers must bind them to reviewed real APIs. The view retains immutable backing; the String-only call is a named copy boundary. Ordinary String + on a stock JDK does not automatically become this operation.

Acceptance requires exact charAt/length/range behavior, stable backing under caller mutation/GC/eviction, and compatible consuming APIs. Do not implement asymmetric equals(String) merely to imitate String; review the equality contract of each explicit type.

### Route B: compiler lowering

A source/bytecode transformation rewrites only operations whose semantics it can prove safe to the same owner and view operations. It must preserve evaluation order, null conversion, primitive formatting, overload resolution, exceptions and external ABI behavior.

For example, an internal expression conceptually resembling prefix + value + suffix may be lowered only after resolving types and evaluation behavior. Keep side-effecting operand evaluation in order. Materialize at an unconverted String-only boundary. Do not rewrite reflection, serialization, string-switch, identity comparisons, intern/monitor use or unresolved calls by textual substitution.

Maintain source maps, before/after hashes, reversible recipes and a refusal path for incomplete AST/type information. Existing compiler/runtime and Maven-plugin families are inputs to this route, not proof that all transformations are complete. Test transformed and untransformed programs against the same oracle.

### Route C: complete modified JDK

Ordinary java.lang.String internally participates in the indexed/segmented representation. Build and distribute a matched complete JDK image. String is final and VM-sensitive; a standalone replacement class file cannot make the stock VM understand a new layout.

Coordinate Java methods, bootstrap, interpreter, VM String readers, GC scanning/barriers/dedup, StringTable/intern, C1/C2/intrinsics, architecture-specific paths, JNI/JVMTI, CDS, JFR, JVMCI, serviceability, serialization and charset/regex boundaries. An interpreter-only experiment must refuse unsupported compiled modes rather than silently claim transparent compatibility.

The entire JDK route must preserve public Java semantics, including unpaired surrogates and permitted code-unit slices. Native functions requiring contiguous modified UTF-8 or UTF-16 still need defined temporary storage and release rules.

### Choosing and sequencing

Begin with source contracts and ownership mapping shared by all three routes. Route A can establish the substrate and explicit boundaries; Route B can remove application-level conversion where proven; Route C can integrate ordinary String only with independent VM acceptance. Progress in one route does not waive another route's gates.

## 6. Unicode, regex and native requirements

The canonical Java contract is UTF-16 code units. Preserve NUL, supplementary pairs and unpaired surrogates exactly. No implicit normalization, case folding or inserted word separators. Language classification is metadata, not permission to change bytes or characters.

A concrete incompatibility needs an explicit decision: indexstring.SubMIndexString is a code-unit range view, while runtime com.synexia.mindex.SubMIndexString.of rejects endpoints that split surrogate pairs. Both are real APIs. Preserve the stricter API under its own contract or introduce a separately specified general code-unit view. Do not silently route String.substring/subSequence behavior through the stricter one.

A surrogate pair can straddle atoms. Code-point iteration and encoding must carry boundary state. Separately encoding each fragment is not generally equivalent to encoding the concatenation, particularly with split surrogates or stateful charsets. Charset, byte order and malformed/unmappable policy belong in encoded-fact keys.

Regex engine identity and flags are part of the contract. RE2/J-style support does not imply java.util.regex backreferences, lookarounds, captures or anchor semantics. Define fallback/rejection and region/capture coordinate conversion; an approximate candidate filter never proves a match. Cached transitions need all context that affects the result.

JNI array access may copy; a global reference is not permanent pinning. A direct ByteBuffer represents one contiguous native address range and does not automatically own that memory. Scatter/gather consumers need an explicit descriptor ABI. Record acquisitions/releases, exception paths, overflow checks and mapping/arena lifetime. See the [JNI specification](https://docs.oracle.com/en/java/javase/21/docs/specs/jni/functions.html).

## 7. Current target reconciliation

PR metadata on 2026-10-02 shows [#3](https://github.com/hsoliwal/M3jdk21/pull/3), [#5](https://github.com/hsoliwal/M3jdk21/pull/5), [#6](https://github.com/hsoliwal/M3jdk21/pull/6) and documentation [#7](https://github.com/hsoliwal/M3jdk21/pull/7) as merged. Convergence PRs [#8](https://github.com/hsoliwal/M3jdk21/pull/8), [#9](https://github.com/hsoliwal/M3jdk21/pull/9) and [#10](https://github.com/hsoliwal/M3jdk21/pull/10) are also marked merged.

The integration history therefore includes distinct candidate lines, convergence commits and the current master snapshot. **Merged PR state is not proof that every candidate file is retained or that the combined tree passes.** At target base 8bb6215372e07712f1fdf5a0cb912af495007b19, connector reads of m3/runtime-integration/README.md and m3/runtime-integration/evidence/results.json returned 404, while those files remain readable at #6's pinned head. This observation concerns those two artifact paths; it does not establish absence of all runtime code.

The [#6 results at 3776d6e674d6c9b04539ca24aca2504aa88d4a57](https://github.com/hsoliwal/M3jdk21/blob/3776d6e674d6c9b04539ca24aca2504aa88d4a57/m3/runtime-integration/evidence/results.json) report image build exit 0, 86 flag-off passes, and 16 enabled passes plus two failing tests with six StringJoiner OOME expectations. They report interpreter-only limitations and outstanding compiled/full jtreg/JCK/serviceability gates. These are inspected historical receipts, not tests run for this documentation PR or proof for current master.

Before implementation starts, reconcile source trees and identify the actual retained runtime representation and flags. Before acceptance, rebuild exact combined-tree images and rerun applicable flag-off/on, compiler, GC, native, charset/regex, bootstrap, conformance and sharing gates. Keep old evidence attached to its original commit.

At source pin bc49cd7610bf5974f6baa31ba64e6885f4117528, synexia-indexstring/pom.xml contains two profiles with ID m3-mindex-native-string-admission. Record this source-build configuration blocker separately; this documentation does not fix or execute that build.

Source [PR #7554](https://github.com/hsoliwal/com.synexia/pull/7554), head 4574363a63c74c52183eb2deae134163d409816f, is a separate sidecar/geometry candidate. Do not adopt its geometry export merely because it claims no payload copying: the inspected copyLiteralGeometryPage method's languages output parameter shadows the stored language lane, so its language assignment reads the caller output array at the source-row index. That can return incorrect metadata or go out of bounds on later pages. This source-level defect is documented, not fixed or runtime-tested here. Require a separately reviewed fix and tests for nonzero start rows, skipped descriptor rows, small output buffers and distinct language values before porting.

## 7a. Follow-on evidence reconciliation (2026-10-02 02:32 UTC)

The original source census and claims above remain bound to their original pins. This update records later observations without silently moving that baseline.

### Runtime branch and retained landmarks

PR #6's actual base branch is **m3/segmented-string-experiment**, not master. Its merged state therefore does not establish a retained master runtime. At master 8bb6215372e07712f1fdf5a0cb912af495007b19:
- src/java.base/share/classes/java/lang/MIndexString.java returned 404
- src/java.base/share/classes/java/lang/M3StringStorage.java returned 404
- The fetched java/lang/String.java contained neither the inspected mindex marker nor M3StringStorage marker

At #6's pinned head 3776d6e674d6c9b04539ca24aca2504aa88d4a57, String.java does contain the mindex marker. These concrete landmarks strengthen the requirement to reconcile the actual trees; they are not an exhaustive VM audit or a new build result. Convergence PR titles/history do not substitute for retained-file verification.

### Source build profile correction

At newer source develop snapshot 8830a34a042d79e2d1b89b850d177c3038bf975e, synexia-indexstring/pom.xml contains **one** m3-mindex-native-string-admission profile. The duplicate-profile observation at the earlier census pin remains historical. The configuration defect is source-corrected at the newer pin; Maven model validation, compilation and tests were not executed by this documentation review. Do not continue to describe the duplicate as an established blocker on that newer snapshot.

### Companion work for implementers to reconcile

- [Synexia #7584](https://github.com/hsoliwal/com.synexia/pull/7584), inspected head 6d1aece6d1685d22a8f788c16c76ad56c0e56212, supplies a three-route contract candidate. Its documented distinction is explicit source-wrapper-retaining view versus compiler-style owning/canonical substring versus ordinary String in the modified-JDK route. It also documents primitive source/range coordinates, split-surrogate substring behavior and exact exception-class requirements. Reconcile this candidate's actual source and tests before porting; its PR description is not an exact-head hosted pass. Preserve the distinction between zero-copy payload reuse and whether the source wrapper itself is retained.
- [Synexia #7606](https://github.com/hsoliwal/com.synexia/pull/7606), implementation head 35305ee40d2302cc8efc3567c4cc3a6553323278, provides an inventory-workflow companion at source pin 75fb1abaecb56969bea2914520bbe819f130632b. It uses the existing scanner and private-repository execution path. Workflow publication or a tracked-tree listing does not establish full semantic/dependency coverage; inspect the actual artifacts and keep private implementation payloads private.
- [Synexia #7607](https://github.com/hsoliwal/com.synexia/pull/7607), implementation head 2cf305306e1bdf871680b709e5bb13e1b4135b4d, describes a six-file retained-concat/copy closure and pinned recipe. Its reported local differential/facade/JNI checks are scoped receipts, not full-reactor, OpenRewrite scheduler or modified-JDK/JIT acceptance. This is a partial owner increment, not all-family migration.
- [M3jdk21 #12](https://github.com/hsoliwal/M3jdk21/pull/12) has a [hosted log](https://github.com/hsoliwal/M3jdk21/actions/runs/36953420865/job/110671088585) showing 15 valid mappings and six passing validator cases, then generated m3/migration/COVERAGE.md drift and exit 1 **before Java compilation**. Preserve the generator-consistency gate and reconcile its output; do not weaken the gate, remove the non-exhaustive caveat or report these validator results as Java/runtime acceptance.
- [M3jdk21 #13](https://github.com/hsoliwal/M3jdk21/pull/13) is the target companion referenced by the source inventory/port-lineage work. Later exact-tree inspection found its merged head 84c2711c7ff24d11ba46478a11584194c425553b carries nine tooling files; its broader description does not establish a retained migration manifest. The operational candidate manifest is verified on #12's feature-stack head d543255294ae85e4c8015812a3c8a97aaf498354, not master 26c442d3f1400e01eeb11a44e803b63e1874735e. See [worked port decisions](migration-worked-port-decisions.md) for exact locations/fields. Reconcile that operational authority instead of populating a second registry from the illustrative docs schema.

PR #7554's language-output shadowing observation remains pinned to 4574363a63c74c52183eb2deae134163d409816f. No fix or new execution evidence for that defect is claimed here.

## 8. Implementation handoff

Contributors should fill the mapping and acceptance evidence, not infer implementation from this document.

1. Freeze source and target baselines and reconcile concurrent PRs without discarding others' work
2. Complete symbol/dependency/native/resource inventory beyond the bounded census
3. Decide authority and identity domain per family; document compatibility divergences
4. Approve target placement and old-name facades; preserve licenses and donor provenance
5. Create source-pinned mechanical recipes with drift refusal and rollback
6. Port one coherent owner plus its adapters/tests, then prove behavior at boundaries
7. Attach exact-head receipts to mapping records; keep failures and unsupported routes explicit
8. Review source enhancements since the recorded pin and disposition every affected mapping
9. Broaden acceptance only after family, integration and exact-image gates pass

Keep changes additive until compatibility and migration policy explicitly permit removal. Do not delete a failing test, relax an expected exception or mark an unrun gate passed to complete a row.

## 9. Publication and licensing

This handoff contains architectural summaries and source-path/blob metadata, not copied source implementations, private datasets, credentials or conversation transcripts. Source access may require the reader's existing repository authorization. Do not make private repositories public to satisfy CI.

Preserve OpenJDK notices and license obligations separately from independently authored M3 material. Existing Apache-2.0 headers do not authorize relicensing unrelated donor code or lexicon data. Each import requires file-level provenance, attribution, dependency and redistribution review. Mapping metadata and documentation do not themselves grant a license to move an implementation.
