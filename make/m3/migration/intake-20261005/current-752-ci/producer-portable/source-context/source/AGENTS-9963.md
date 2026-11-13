# Synexia M3 agent invariants

## Recipe first, for every LLM-assisted source task

Before changing production source, inventory the actual checked-out tree and its dependency/API
owners. Create or reuse a Maven/OpenRewrite task recipe crate. Improve the recipe and its tests;
materialize reviewed candidates through that recipe instead of repeating one-off edits in files.
Use the existing OpenRewrite parsers, semantic trees, recipes, catalogues and M3 machinery first.
A narrow exact-source Java/Maven replay recipe is acceptable for pinned Java/JNI changes; it is
not a replacement rewrite engine or permission to bypass canonical M3 promotion.

Bind the crate to the repository revision, target paths, preimage/postimage SHA-256, source and
license provenance, contract tests, and proof commands. Reject source/template drift. Show the
recipe's fixed point and refusal tests. New source files need explicit absent-before admission.
Test fixtures, temporary extraction and stubs must never be reported as full runtime integration.

Qualify Atomizer and Patternizer recipes with bounded in-memory projects containing real code.
Keep code-looking strings, regex, text blocks and comments opaque to source transformations;
use attributed syntax and compiled observations to distinguish executable code from payloads.
Freeze the corpus, expected hypotheses, recipe orders, provider identities and resource limits
before execution. Compile and compare externally observed behavior after every pass; retain
failed candidates, ambiguity and missing provider evidence. Establish source fixed points and
evidence replay separately. Static precompute may reuse only exact, hash-bound verified results;
measure cold preparation and warm reuse independently. Passing a finite corpus grants no broader
semantic, performance or promotion authority.

## Short Synexia names; do not rename JDK contracts

New Synexia-facing concepts should establish a short canonical term/acronym instead of exporting
another long descriptive implementation class. Prefer compact domain names such as LIR, MIR, TIR,
L2G, L2V, VUB, VCPU32 and VGPU. Once a longer public Synexia name has shipped, keep it as a
compatibility/provider surface and add the short facade additively; do not break callers merely to
rename it.

JDK-owned classes, packages, descriptors and public contracts are not renamed or wrapped with long
Synexia-specific names just to import Synexia terminology. Any explicit JDK-internal experiment
remains recipe-scoped, compatibility-checked and separately authorized.

## Locked contracts, replaceable internal atoms

Preserve existing public interfaces, exception/null semantics and working behavior. Review any
intentional policy/API change explicitly. Refine private behavioral atoms by purpose; do not split
code into arbitrary tiny methods. Inventory/catalogue work on independent leaves may fan out;
each writing leaf must have exclusive ownership, with deterministic, serial, verified fan-in.
Escalate verification file -> package -> module -> reactor -> external API. Java 21 is the baseline.
Use the existing progress monitor contract for new long-running entry points, with a null/no-op
compatibility default. Core transformation and verification must remain deterministic; no LLM
is required in the runtime core.

## One MIndex payload authority

A MIndexString is a composition of existing canonical interned atoms, ranges/masks, and derived
facts. Reuse immutable OS-shared lexicon payloads. Genuine misses enter the existing VM-local
canonical overlay. Do not introduce another retained spelling store or flatten compositions just
to run an operation. JDK String, char[] and byte[] are explicit ingress/export/ABI boundaries.
Operation bridges must reuse atom/range/composition facts and existing index owners. Derived
search tables are metadata, not another text identity. Hash/filter equality is not exact match proof.

## Donor and category passes

For each targeted problem category, check the existing catalogue and first-party implementation,
then evaluate pinned GitHub donors by source, license, semantic fit and measured cost. LeetCode,
HackerRank and GeeksforGeeks problem categories inform the review; they do not authorize copying
submission/editorial bodies or overriding String contracts. Preserve the Java semantic oracle and
exact verifier behind optional native candidate filters. Do not claim an exhaustive catalogue
review or a speedup without evidence.

## Third-party code is inlined and offline by default

Third-party library source that is compiled, linked, generated into, or shipped with Synexia must
be repository-owned at the exact reviewed revision. Vendor it under the canonical module
`third_party` owner (or an equivalent repository-local source mirror), retain the upstream
license/notice and provenance, and bind every vendored file or subtree to an exact commit/tree/blob
identity. Git submodules, floating branches, build-time FetchContent/downloads, package-manager
network fetches, and caller-supplied source checkouts are not acceptable production dependencies.

Source inlining does not authorize blind source copying into Synexia APIs. Reuse existing Synexia
owners first; otherwise port/adapt through a recipe-owned candidate with differential tests.
Third-party code that cannot be legally redistributed must remain reference-only and must not enter
the build/runtime dependency graph. Keep upstream source byte-for-byte where it is vendored; place
Synexia glue, stubs, wrappers, namespace adaptation and JNI bridges outside the third-party tree.

JDK/OS/compiler/CMake/Maven executables are toolchain inputs rather than vendored library source,
but their versions/images must be pinned by the reproducible build. Maven/Gradle/native libraries
and code generators still count as third-party code: repository-wide completion is blocked until
their artifacts/source are available from an approved repository-local/offline mirror and no
network resolution is required by the declared build/test path.

## Source integration, not ancestry-only completion

A PR marked merged is not proof that its implementation is present. Compare the delivered tree
and actual owner contents, not only commit ancestry. An ancestry-only merge that retains an older
tree must be labelled as history preservation, not a delivered additive source superset. Recover
missing capabilities through source-reviewed, contract-tested recipes; never force-push or perform
a destructive whole-file replacement to disguise unresolved overlap.

No task is repository-complete until the relevant formatting, compilation, static analysis, unit,
integration, contract and reactor gates pass on the delivered tree. Report exactly what ran and
what remains blocked. Do not weaken CI, permissions, branch protection or tests to obtain a pass.


## NRD — Never Rebase Develop; history is executable evidence

`develop` is the append-only canonical evidence chain. Never rebase, force-push, squash away, or
otherwise rewrite its useful ancestry. Corrections, reversions, restorations, reconciliations, and
supersessions are additive commits. A merge that only preserves ancestry is not proof that source
capability survived in the delivered tree.

Before changing an existing production owner, traverse its relevant history backward and forward.
Treat commit diffs, pre/post blobs, recipes, tests, benchmark receipts, and restore/replay commits as
first-class CCPS/PSE evidence. HEAD is not automatically the best implementation. Classify a
historical atom as SURVIVES, SUPERSEDED_PROVEN, LOST, OVERWRITTEN_UNPROVEN, REVERTED, RESTORED,
REPLAYED, or PROOF_REQUIRED. Historical code is a candidate, never automatic replacement authority.

Promotion must produce an additive semantic superset: preserve today's sealed contracts and every
best-known proven capability unless a newer implementation has explicit contract/performance proof
that supersedes it. A candidate that silently drops a previously proven optimization, allocation
property, zero-copy path, precompute, test, recipe, or CI gate fails admission. Prefer merging the
stronger historical atom into the current owner over resurrecting a parallel stale architecture.

## Completed-work reuse is an M3 invariant

For each operation, inventory reusable exact results, range facts, prepared plans and unresolved
work before selecting another scan. Bind evidence to canonical owner/snapshot, source range,
operation parameters and semantic context. Distinguish exact answers, rejection proofs, bounds
and candidate filters; a hash hit, partial scan or different tokenization is not equality proof.
Keep derived state in existing shared owners or explicit caller-owned plans/results, never extra
MIndexString payload fields or a second spelling store. Document retention and budget limits.
Do not turn an early positive match into an invented last position or count. Enumerating outputs
still costs at least their size. Pattern-state summaries must account for composition seams.

Review the existing catalogue, then LeetCode -> HackerRank -> GeeksforGeeks by problem category;
record mismatched contracts instead of treating similarly named challenges as interchangeable.
Adapt licensed, revision-pinned GitHub donors only after exact semantics and ownership review.
Run repeated serial file-local passes through the task recipe. Preserve earlier recipe gates when
extending their owners; reconcile sealed postimages rather than disabling a gate. Prove correctness
and reduced work separately, with cold preparation and warm reuse measured separately. Mark
unexecuted integration/platform/performance gates explicitly. No global all-pairs precompute by default.


## Collection density and memory pressure are M3 invariants

Before creating another collection hierarchy, inventory and reuse the existing packed, segmented,
ring, sorted, sparse, heap, trie/DAG, direct-index, or other owner that already satisfies the
required contract. Choose the physical lookup/storage strategy from the operation contract and
measured workload. Hashing is an optional index strategy, not the default storage answer.

N logical collection elements must not imply N retained collection-owned structural objects.
Canonical collection storage must not retain an Entry/Node/Bucket/Cell/link wrapper per logical
element. M3-native and primitive maps traverse key/value/slot lanes directly. When an existing
public Java Map/SequencedMap/NavigableMap signature explicitly requires Map.Entry, materialize a
fresh caller-owned compatibility projection for that request only. Never pool, cache, intern,
recycle, or retain those caller-visible Entry objects.

Memory pressure may reduce only explicitly optional residency such as caches or rebuildable
precompute/index pages. It must not evict canonical collection payload or change equality, order,
iteration, concurrency, serialization, or externally observable collection behavior. Pressure
shrink must release actual backing memory, not merely lower logical size. Recovery raises residency
budget metadata first and refills/rebuilds lazily on demand; it must not eagerly resurrect evicted
values or backing arrays merely because pressure fell. Pageable cold state must reuse the existing
MIndex swap plane and SQLite/JNI storage owner rather than introducing another cache/swap framework.

For repository-wide LLM source work, use the strict repository evidence mode
`M3RepositorySupersetExecutionCli.Options.strictLlmTask()` / `--strict-llm-task`. Both exact
Git-tree inventory coverage and exact recipe-capability coverage are required before source-changing
recipe execution can be considered ready. This is admission evidence only; promotion remains serial
and proof-gated.


## Cartesian fan-in creates new recipes

Fan-out may explore compatible atom, pattern and recipe combinations. Fan-in must retain every
distinct ordered composition and its parent lineage as a new recipe candidate; it must not flatten
new ideas into a union or silently pick one winner. Reuse only exact prior results with the same
source snapshot, contract, parameters and ordered parents. Keep rejected or unresolved candidates
and their reasons available for later recipe refinement.

Bound each Cartesian work packet before enumeration. If its product exceeds the declared budget,
refuse that packet explicitly and partition the work; never report an unvisited tail as completed.
A synthesized definition is candidate evidence, not semantic compatibility proof. Prove its actual
execution phases (including OpenRewrite scan versus visit ordering), fixed point, refusal behavior,
source seals, API/JNI contracts and required reactor gates. Then use the existing strict repository
admission and serial composition owners for promotion. Work on the reusable synthesis recipe and
its tests instead of editing each composed file by hand.



## Offline idea generation, mechanical recipe convergence

The LLM may prepare bounded fixtures, candidate recipes, category mappings and optimization
hypotheses offline. Runtime acceptance remains deterministic and uses the existing canonical
convergence lab: compile each pass, compare behavior and public/protected contracts, recompose
exact atoms, record refusal diagnostics and require a bounded fixed point. Extend that owner
instead of creating another compiler, parser or convergence engine. Compilation alone is not
correctness; finite permutations are not universal proof.

Prepared facts and candidate filters must declare their domain, exact verification, storage and
retention budget. Measure cold preparation separately from warm reuse. Preserve null/exception,
side-effect, ownership, cancellation, JNI and ABI contracts. Record operation-count improvements
separately from measured time/allocation gains. No LLM is required in the runtime acceptance loop.

## Compiler-guided recipe qualification is an M3 invariant

Refine a reusable task recipe against immutable in-memory Java projects before materializing its
reviewed atoms. Keep the source-convergence loop frozen during a trial; recipe improvement creates
a new artifact/options identity and a new experiment. Protect test/oracle fixtures independently
of candidate writable paths. Require external transformation postimages, explicit already-satisfied
or refused cases, every intermediate compile/API/behavior gate, deterministic replay and a full
no-change pass. A comment edit is not evidence of atom extraction.

Bind receipts to the exact source, protected fixtures, oracle, corpus, ordered recipe parents,
configuration, compiler/runtime context and declared budgets. Preserve each ordered composition's
lineage and refusal evidence. Budget exhaustion or an unvisited Cartesian tail cannot qualify.
Reuse the existing recipe-convergence owner and the Maven recipe-qualification task crate; do not
create another parallel laboratory. Static source/typed-pattern facts nominate work only and must
be invalidated when their source or semantic context changes. Java remains the semantic oracle
behind optional JNI candidate lookup. Report measured work and performance separately from finite
correctness evidence; qualification never substitutes for strict repository admission or promotion.
