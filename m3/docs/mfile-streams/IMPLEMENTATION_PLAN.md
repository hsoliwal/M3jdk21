<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# MFile streams implementation plan

> For agentic workers: use superpowers:subagent-driven-development or
> superpowers:executing-plans to implement task by task. This document is a
> review plan with a bounded-core bridge; richer runtime tasks remain pending.

**Goal:** Deliver one M3JDK21-owned file/archive entry API with lambda composition,
qualified ownership and reusable source facts.

**Architecture:** Extend the current `com.m3.fs.MFiles`/`MFile` facade in `jdk.m3fs`,
using the relocated `com.m3.fs.vfs` provider runtime. Reuse existing M3FS metadata,
snapshot and compiler owners where their actual contracts apply. The present donor
object model is not an array-only redesign. Standard streams remain single-use.
Native acceleration is conditional on actual compatible bulk consumers.

**Tech stack:** Java 21, existing M3JDK21 Java/JNI owners, qualified archive readers,
canonical Synexia Maven/OpenRewrite recipes and current target module/image tooling.

**Spec:** [DESIGN.md](DESIGN.md)

## Current bounded integration and remaining work

`m3fs-vfs-20261020` supplies the source for a single-root replayable plan, metadata
predicates, cheap name/extension selection, subtree pruning, DFS/BFS order,
selective archive layers, depth limits, lambda result projection, provider factories,
and stream-owned input/output. Default methods share monitored I/O and synchronous
shadows. Explicit observations/deltas reuse existing content/structure identities. It also supplies the attributed VFS core profile and the
existing M3FS JMOD packaging integration. Exact test and image results belong to
[`qualification.json`](../../fs/verification/vfs-20261020/qualification.json) and
[`RESULTS.json`](../../fs/verification/vfs-20261020/RESULTS.json), not this task table.

| Task | Bounded source overlap | Remaining richer contract |
| --- | --- | --- |
| T1 | Directory/provider traversal, immutable metadata, no link descent | Public richer entry facts and optional follow/cycle policy |
| T2 | One-root Plan replay and stream-owned manager/readers/writers | Public sessions, content snapshots, inspection and durable recipe descriptors |
| T3 | Explicit layered roots and deferred predicate-selected nested archives | Physical duplicate-member identity, scoped scan and format admission beyond the selected profile |
| T4 | Name/extension and metadata filters; standard Stream.map | Content predicates, immutable rename/content/text transforms and fan-out |
| T5 | Evidence-based deltas between same-source observations | Multi-source union/overlay, join, directory reconciliation, exact deduplication and global ordering |
| T6 | Raw and qualified structure observations reuse existing manifest/precompute owners | Content/decoding/context-qualified source and AST bridge |
| T7 | Explicit owner-scoped input/output and shared default methods | MFiles output terminals, preflight, collision and commit receipts |
| T8 | Child-monitor callback bridge, synchronous per-layer shadows and downstream deltas | Automatic incremental reconciliation and pressure-aware retention |
| T9 | Dedicated bounded-core packaging and consumer lane | Full admitted capability/platform/JNI gates and workload measurements |

Every row in `ACCEPTANCE.tsv` remains `NOT_RUN_RUNTIME`, including rows whose simpler
behavior overlaps a core test. Record a future pass only against the complete stated
expectation. In particular the donor logical namespace can collapse duplicate archive
records, so A15 and A21 are not satisfied by a successful ZIP traversal. A `.zip` Path
is not automatically expanded; use a layered URI or explicit `enterArchives`
selection in current consumers. Git working trees use the
file provider; Git commit trees and LFS hydration are deferred provider work.

The current runtime owner supersedes the earlier suggested `m3/file` destination for
this facade. Extend `m3/fs`; preserve the distinct existing `com.m3.file.M3File` and
compiletime snapshot owner. All future interfaces below are proposed unless listed
in the current API guide. No additional API is introduced by this document update.

## Global constraints

- Java 21 is the baseline.
- M3JDK21 owns runtime; Synexia owns reusable recipes; no Synexia runtime dependency.
- Preserve existing public contracts and the String -> arrays -> collections ->
  AST/compiler -> remaining-family receiving order.
- Use the existing M3File/M3FS/source/AST owners; keep their distinct contracts.
- Source changes follow inventory -> dated additive convergence -> catalogue ->
  Inline -> Atomize -> Patternize -> Comment -> applicable gates -> reviewable PR.
- Every new runtime source path needs exact absent-before or current-preimage admission.
- Every runtime capability and acceptance case remains pending until its own evidence exists.
- No mandatory external collection-capacity hints; no silent value loss under pressure.
- Existing first-party copyright/Apache-2.0 and third-party/OpenJDK rights are preserved.

## Review focus

- Retained duplicate ZIP members read out of enumeration order: T3/A15.
- Stable metadata mistaken for pinned content during external replacement: T2/A11-A12.
- Stateful callback order and source changes between preview and commit: T2/A13; T7/A49.
- Same bytes under different compiler/charset/filename context: T6/A39-A40.
- Partial writes and memory pressure mistaken for successful atomic completion: T7/A46;
  T8/A52-A53.

## Planning boundary

T1-T5 and T7 name the proposed file-facing API. T6/T8 deliberately reuse existing source,
compiler, monitor and delta owners whose exact current exported signatures must be
inventoried before their implementation recipe is admitted. This is an explicit dependency
gate, not permission to invent substitute APIs. T9 depends on all admitted runtime slices.

Each task maps to concrete acceptance rows below. The acceptance table is the richer
specification, not the bounded integration test result. This documentation component
generates no production code; the separate integration recipe owns runtime changes.

## T1. Stable entries and directory traversal

**Capabilities:** C01-C04

**Files:** Extend `m3/fs/src/main/java/com/m3/fs/MFile.java` and `MFiles.java` and the existing M3FS tests through the canonical recipe. Do not recreate them under `m3/file`.

**Interfaces:** Builds on the current Path/provider-address facade. The richer entry shape and options overload in design sections 2/3 remain proposed; current callers use Plan configuration.

- [ ] Inventory exact owner, relevant history, public contracts, build module and candidate donor; seal this task's current paths and dependency closure.
- [ ] Write contract tests for A01, A02, A03, A04, A05, A06, A07 using the exact input and expected observation in ACCEPTANCE.tsv; run them against the preimage and record the expected missing/incorrect behavior.
- [ ] Extend the existing canonical Maven/OpenRewrite task recipe and source templates; qualify source drift, duplicate/missing-source refusal and fixed point before applying it.
- [ ] Generate the reviewed target candidate and run its focused Java 21 contract tests; failure or missing coverage remains PENDING.
- [ ] Run the applicable module/provider/platform/JNI gates, preserve existing tests and record every command, exit and exact source identity.
- [ ] Apply again for a zero-change fixed point, record code/recipe/provenance together, and publish an additive reviewable PR for the task.

## T2. Plans sessions snapshots and recipe descriptors

**Capabilities:** C05-C07,C19-C20

**Files:** Extend the existing `com.m3.fs.MFiles.Plan`; admit proposed public session/options additions under the same owner only when their contracts are implemented. Keep operation descriptors in the existing recipe export boundary.

**Interfaces:** Produces MFiles.from(Path...), immutable Plan.stream(), caller-owned Session.stream(), and explicit snapshot modes. Descriptors use existing recipe DAG identities; no new graph engine.

- [ ] Inventory exact owner, relevant history, public contracts, build module and candidate donor; seal this task's current paths and dependency closure.
- [ ] Write contract tests for A08, A09, A10, A11, A12, A13, A14 using the exact input and expected observation in ACCEPTANCE.tsv; run them against the preimage and record the expected missing/incorrect behavior.
- [ ] Extend the existing canonical Maven/OpenRewrite task recipe and source templates; qualify source drift, duplicate/missing-source refusal and fixed point before applying it.
- [ ] Generate the reviewed target candidate and run its focused Java 21 contract tests; failure or missing coverage remains PENDING.
- [ ] Run the applicable module/provider/platform/JNI gates, preserve existing tests and record every command, exit and exact source identity.
- [ ] Apply again for a zero-change fixed point, record code/recipe/provenance together, and publish an additive reviewable PR for the task.

## T3. Archive readers nested traversal and scoped scan

**Capabilities:** C08-C12

**Files:** Extend the existing `m3/fs` facade/provider boundary and reuse `com.m3.fs.vfs` where its logical-namespace contract is sufficient. Stronger record-preserving readers require explicit source/dependency admission; add no second filesystem owner.

**Interfaces:** Consumes T1/T2 identities and sessions. Produces Plan.enterArchives(Predicate<MFile>, int) and <R> ScanReport scanFiles(EntryReader<R>, ResultConsumer<? super R>) throws IOException. EntryReader receives MFile and borrowed InputStream; ResultConsumer receives R.

- [ ] Inventory exact owner, relevant history, public contracts, build module and candidate donor; seal this task's current paths and dependency closure.
- [ ] Write contract tests for A15, A16, A17, A18, A19, A20, A21, A22, A23, A56 using the exact input and expected observation in ACCEPTANCE.tsv; run them against the preimage and record the expected missing/incorrect behavior.
- [ ] Extend the existing canonical Maven/OpenRewrite task recipe and source templates; qualify source drift, duplicate/missing-source refusal and fixed point before applying it.
- [ ] Generate the reviewed target candidate and run its focused Java 21 contract tests; failure or missing coverage remains PENDING.
- [ ] Run the applicable module/provider/platform/JNI gates, preserve existing tests and record every command, exit and exact source identity.
- [ ] Apply again for a zero-change fixed point, record code/recipe/provenance together, and publish an additive reviewable PR for the task.

## T4. Content predicates and immutable transformations

**Capabilities:** C13-C14,C16-C18

**Files:** Extend Plan; place named predicate/transform atoms next to the existing file owner; reuse admitted text/String facilities and scoped read leases.

**Interfaces:** Produces Plan.filter(Predicate<? super MFile>), filterContent(IOBiPredicate<MFile,InputStream>), filterText(Charset, Predicate<? super CharSequence>), rename(Function<MFile,String>), transformContent(IOBiFunction<MFile,InputStream,InputStream>) and transformText(Charset,UnaryOperator<String>); each returns a new Plan.

- [ ] Inventory exact owner, relevant history, public contracts, build module and candidate donor; seal this task's current paths and dependency closure.
- [ ] Write contract tests for A24, A25, A26, A27, A28, A29, A30 using the exact input and expected observation in ACCEPTANCE.tsv; run them against the preimage and record the expected missing/incorrect behavior.
- [ ] Extend the existing canonical Maven/OpenRewrite task recipe and source templates; qualify source drift, duplicate/missing-source refusal and fixed point before applying it.
- [ ] Generate the reviewed target candidate and run its focused Java 21 contract tests; failure or missing coverage remains PENDING.
- [ ] Run the applicable module/provider/platform/JNI gates, preserve existing tests and record every command, exit and exact source identity.
- [ ] Apply again for a zero-change fixed point, record code/recipe/provenance together, and publish an additive reviewable PR for the task.

## T5. Multiple sources comparison and exact deduplication

**Capabilities:** C21-C25

**Files:** Extend Plan with grouping/diff/dedup atoms; reuse existing M3 index/mask and spill owners rather than add retained Entry/Node collections.

**Interfaces:** Produces explicit source union/overlay; Stream<MFiles.Change> Plan.diff(Plan,Function<MFile,?>,Compare) throws IOException; Plan.distinctContent(); grouped joins require key/cardinality policy.

- [ ] Inventory exact owner, relevant history, public contracts, build module and candidate donor; seal this task's current paths and dependency closure.
- [ ] Write contract tests for A31, A32, A33, A34, A35, A36, A37 using the exact input and expected observation in ACCEPTANCE.tsv; run them against the preimage and record the expected missing/incorrect behavior.
- [ ] Extend the existing canonical Maven/OpenRewrite task recipe and source templates; qualify source drift, duplicate/missing-source refusal and fixed point before applying it.
- [ ] Generate the reviewed target candidate and run its focused Java 21 contract tests; failure or missing coverage remains PENDING.
- [ ] Run the applicable module/provider/platform/JNI gates, preserve existing tests and record every command, exit and exact source identity.
- [ ] Apply again for a zero-change fixed point, record code/recipe/provenance together, and publish an additive reviewable PR for the task.

## T6. Shared source text and compiler-fact reuse

**Capabilities:** C15,C26-C28

**Files:** Add the target-specific bridge beside the existing compiletime M3File/M3AST owner after inventorying its exact exported module; keep compiler dependencies out of java.base and the simple file module.

**Interfaces:** Consumes content-generation/entry identity, charset and parser context. Produces reuse of existing immutable SourceSnapshot/AST facts through that owner's actual API, whose exact signatures must be sealed from the current inventory before source work.

- [ ] Inventory exact owner, relevant history, public contracts, build module and candidate donor; seal this task's current paths and dependency closure.
- [ ] Write contract tests for A38, A39, A40, A41, A42 using the exact input and expected observation in ACCEPTANCE.tsv; run them against the preimage and record the expected missing/incorrect behavior.
- [ ] Extend the existing canonical Maven/OpenRewrite task recipe and source templates; qualify source drift, duplicate/missing-source refusal and fixed point before applying it.
- [ ] Generate the reviewed target candidate and run its focused Java 21 contract tests; failure or missing coverage remains PENDING.
- [ ] Run the applicable module/provider/platform/JNI gates, preserve existing tests and record every command, exit and exact source identity.
- [ ] Apply again for a zero-change fixed point, record code/recipe/provenance together, and publish an additive reviewable PR for the task.

## T7. Output planning and directory/archive writes

**Capabilities:** C29-C32

**Files:** Add proposed writer atoms beside the existing `com.m3.fs` facade and admitted provider boundary; preserve original NIO providers and source contents.

**Interfaces:** Produces WriteReport Plan.writeDirectory(Path,WriteOptions) and writeZip(Path,WriteOptions) throws IOException; preview returns names/collisions/capabilities bound to context; default collision FAIL and per-commit-unit guarantees apply.

- [ ] Inventory exact owner, relevant history, public contracts, build module and candidate donor; seal this task's current paths and dependency closure.
- [ ] Write contract tests for A43, A44, A45, A46, A47, A48, A49 using the exact input and expected observation in ACCEPTANCE.tsv; run them against the preimage and record the expected missing/incorrect behavior.
- [ ] Extend the existing canonical Maven/OpenRewrite task recipe and source templates; qualify source drift, duplicate/missing-source refusal and fixed point before applying it.
- [ ] Generate the reviewed target candidate and run its focused Java 21 contract tests; failure or missing coverage remains PENDING.
- [ ] Run the applicable module/provider/platform/JNI gates, preserve existing tests and record every command, exit and exact source identity.
- [ ] Apply again for a zero-change fixed point, record code/recipe/provenance together, and publish an additive reviewable PR for the task.

## T8. Incremental reconciliation progress and pressure

**Capabilities:** C33-C36

**Files:** Extend existing file-generation/delta and progress owners with the MFile consumer boundary; use current schedulers and M3FS/SQLite residency policies.

**Interfaces:** Consumes source generations, recipe/parse dependencies and provider change hints. Produces explicit invalidation/reconciliation, existing monitor/status callbacks, cancellation and bounded execution receipts.

- [ ] Inventory exact owner, relevant history, public contracts, build module and candidate donor; seal this task's current paths and dependency closure.
- [ ] Write contract tests for A50, A51, A52, A53 using the exact input and expected observation in ACCEPTANCE.tsv; run them against the preimage and record the expected missing/incorrect behavior.
- [ ] Extend the existing canonical Maven/OpenRewrite task recipe and source templates; qualify source drift, duplicate/missing-source refusal and fixed point before applying it.
- [ ] Generate the reviewed target candidate and run its focused Java 21 contract tests; failure or missing coverage remains PENDING.
- [ ] Run the applicable module/provider/platform/JNI gates, preserve existing tests and record every command, exit and exact source identity.
- [ ] Apply again for a zero-change fixed point, record code/recipe/provenance together, and publish an additive reviewable PR for the task.

## T9. Installed-image and performance qualification

**Capabilities:** C37-C38

**Files:** Extend the existing M3JDK21 module/image builder and target-owned qualification records; publish exact module/package/provider and platform evidence.

**Interfaces:** Consumes source-sealed generated target changes from T1-T8. Produces contract-specific acceptance or explicit PENDING dispositions; no full-family promotion from a documentation or donor test.

- [ ] Inventory exact owner, relevant history, public contracts, build module and candidate donor; seal this task's current paths and dependency closure.
- [ ] Write contract tests for A54, A55 using the exact input and expected observation in ACCEPTANCE.tsv; run them against the preimage and record the expected missing/incorrect behavior.
- [ ] Extend the existing canonical Maven/OpenRewrite task recipe and source templates; qualify source drift, duplicate/missing-source refusal and fixed point before applying it.
- [ ] Generate the reviewed target candidate and run its focused Java 21 contract tests; failure or missing coverage remains PENDING.
- [ ] Run the applicable module/provider/platform/JNI gates, preserve existing tests and record every command, exit and exact source identity.
- [ ] Apply again for a zero-change fixed point, record code/recipe/provenance together, and publish an additive reviewable PR for the task.

## Verification commands

The bounded runtime owner documents these qualification entry points:

~~~sh
mvn -B -ntp -f m3/fs/pom.xml -Pm3-vfs-core clean verify
python3 m3/fs/verification/test_vfs_packaging.py
~~~

See [VFS_PACKAGING.md](../../fs/verification/VFS_PACKAGING.md) for the sealed-source
Java/JMOD lane and required native wrapper inputs. Listing a command here does not
claim it passed; consult the checked receipt and cross-gate report.

Archive dependency admission, compiler bridge, native tests and installed-image runs must
use each inventoried owner's current build path; a command for another module is not
substitute evidence. T9 publishes exact commands and resulting modules/images for its
qualified revision. A bounded Java/image result does not establish the full Maven,
native SQLite, source-built JDK, all-platform or performance gates.

## Documentation recipe verification

The original separate design crate uses the existing M3HashPinnedTextSnapshotRecipe
with the actual OpenRewrite SDK. It creates only the six documentation/catalogue files
listed in its manifest. Its JUnit cases cover exact application, fixed point, drift and
duplicate refusal, permutation/deterministic replay, unrelated-content preservation,
catalogue consistency and receiver materialization.

The current integration reuses the same Java/TextSnapshot owners to materialize reviewed
postimages. A documentation recipe result verifies delivery only when its actual receipt
is present. It never substitutes for any runtime acceptance row. Neither source import
nor JMOD packaging proves a memory gain or an array-only provider implementation.
