<!-- SPDX-License-Identifier: Apache-2.0 -->

## Priority 0 — MOP SDK core source invariant (2026-11-18)

Before generating or changing code, follow [MOP SDK core invariants](doc/mop-sdk-core-invariants.md): top-down repository -> project -> module -> package -> file -> declarations/atoms inventory and contract freeze; bottom-up atom -> file -> package/interface -> module -> project -> system patternization with dependency-closed, proof-gated promotion. Model each file as documented meaningful private/static/instance atoms composed by higher methods where semantically safe; explicitly record NO_SAFE_EXTRACTION when necessary. Independent sealed files may be prepared concurrently, but each file has one writer and serial verified refinement. Preserve JDK/HotSpot/JNI public interfaces, effects, behavior, native lifecycle, existing owners, original rights and qualified additive history. The M3JDK21 product receives only source-sealed, executed Synexia recipe postimages and target-native tests; no recipe or memory store is copied here. No source, API, optimality or runtime claim may be promoted from documentation or unrun tests alone.


## Owner merge directive — history-complete additive superset (2026-11-18)

For every candidate change, treat earlier verified M3/JDK capabilities and upstream-compatible behavior as a protected subset. Inspect applicable historical commits, branches, tags, PRs, renamed paths, source blobs and tests back to available roots before proposing a merge; a merged PR or ancestry alone is not proof of delivered source. Recover losses via the canonical Synexia source-sealed recipe. Choose the best compatible atoms and add API only with source, binary, reflective and behavioral gates. Keep JDK product code and target tests here; keep history-loss review and its dated last-checked ledger in `hsoliwal/com.synexia`, not this clean-output repository. Preserve OpenJDK and every third-party license/NOTICE; Apache-2.0 for separately original, eligible work does not relicense GPLv2/OpenJDK material. Independently developed algorithms must not copy protected donor code or bypass patents.

# M3JDK21 agent instructions

## Clean output rule (owner directive, 2026-11-14) - read first

This rule overrides any older text in this file, including text that allows
receipts or qualification evidence in this repository.

- This repository is clean output: the JDK in its OpenJDK layout (`src/`, `make/`,
  `test/`, `doc/`). M3 work lives inside the JDK's own modules and classes. It is
  not stamped across folders.
- Do not add new files under `m3/`, in any subfolder: `m3/qualification`,
  `m3/evidence`, `m3/tooling`, `m3/release`, `m3/docs` or any other.
- Do not add any of the following anywhere in this repository. They belong in
  `hsoliwal/com.synexia`, the convergence workspace:
  - qualification bundles;
  - `ADMISSION_DECISION`/`WORK_ORDER` files;
  - evidence, receipts and benchmark tables;
  - donor copies (`*.original`, copied LICENSE/NOTICE files of donors);
  - recipe engines, recipe resources and recipe tests.
- Do not add parallel `M3`-prefixed types, or new `jdk.m3.*` modules or packages,
  where a JDK class already owns the behaviour. Change that JDK class in place.
- A pull request here contains only the product change, plus its tests under
  `test/` (jtreg) or the module's own test layout. Put the proof bundle in the
  paired `com.synexia` pull request and link to it.
- The existing `m3/` tree is being moved out: product code into `src/`, everything
  else into `com.synexia`. Edit existing `m3/` files only where a build needs it.




## Priority 0 — additive best-of-best API superset, full-history merge custody (2026-11-18)

Before any code merge, preserve the accepted upstream plus fork-specific **backward-compatible superset** (public API/ABI, behavior, fixes, native lifecycle, optimizations and applicable test contracts). Compare competing historical branches, tags and PRs and choose the best *proven* algorithm/implementation; never silently overwrite or delete an earlier accepted capability. Review earliest reachable ancestors through all relevant heads, both merge parents, merged/closed/reverted PRs, rename/delete history and regressions; never rebase `develop`. Record in the canonical Synexia last-checked register a pinned ref/head SHA, checked date, scope, oldest ancestry covered, last verified SHA, PR/branch coverage, lost-code findings, and PENDING/PARTIAL/VERIFIED/HOLD. A new HEAD snapshot is **not** a complete history audit. Source changes remain recipe-first and qualified in Synexia; this clean receiving product accepts executed postimages and product-native tests, not recipe copies or evidence crates. Prefer Apache-2.0 for newly authored separable contributions, but retain original OpenJDK/Eclipse/upstream licenses and NOTICE; LLM rewriting of donor code does not erase copyright or license obligations. Canonical policy: [Synexia full-history superset merge](https://github.com/hsoliwal/com.synexia/blob/governance/superset-history-last-checked-20261118/docs/M3-SCALE/invariants/SYNEXIA-ADDITIVE-MERGE-FULL-HISTORY-20261118.md). Initial 236-repo inventory is PENDING, not a certified historical audit.

## Hard boundary: postimages only, never recipes

**Synexia (`hsoliwal/com.synexia`) is the exclusive convergence, Maven/OpenRewrite
recipe-authoring, recipe-storage and recipe-execution workspace.**
Release qualification is not: every mandatory release gate, including the
`M3_MAVEN_CONTROL_REACTOR` receipt, is produced inside `hsoliwal/M3jdk21` from M3JDK21
sources (`execution_evidence.gate_owners` in
[`m3/release/release-contract.json`](m3/release/release-contract.json)).
This public M3JDK21 repository is a **receiver**: it may contain only the actual
executed Java/HotSpot/JNI/native/product postimages, target-authoritative tests,
license/NOTICE material and data-only source/recipe-ID/execution-gate receipts.
No Synexia-owned recipe Java/YAML/descriptor, Maven recipe crate, transformation
template, recipe executor, mastery fixture, copy of a recipe engine or recipe mirror
may be introduced or run in M3JDK21. Recipe improvement-memory events, lesson stores,
indexes and their writers/retrievers also remain exclusively in Synexia; no recipe
or improvement-memory mirror belongs in M3JDK21 or another public product fork.
Conceptual design documentation may describe these mechanisms, but is not an
executable recipe, memory store or permission to install either downstream.

Any new change must be re-created by improving and executing the named recipe in
Synexia against the exact pinned M3JDK21 preimage, followed by verified postimage
transfer, JDK/API/ABI/JNI/jtreg/runtime gates and a receiving PR. Existing
`m3/recipes/`, `.m3/openrewrite-recipes/`, `.m3/task-crates/` and similar recipe
copies on historical M3JDK21 branches are **migration residue, not permission**:
inventory them, seal exact content and dependencies in Synexia, then migrate with
a separate reviewed content-preserving cleanup. Do not delete historical source
before verifying its Synexia owner and affected callers. No new recipe assets are
admitted to the public target during migration.


## M3JDK21 is the SDK product, not "M3 + JDK"

**Do not implement or describe M3 as an overlay over an otherwise unchanged JDK.**
M3JDK21 is one integrated Java/HotSpot/JNI SDK and the product authority for
all admitted runtime capabilities. Public Java SE classes retain their actual
`java.util`, `java.lang` and `java.util.concurrent` names and exact JDK
contracts; M3 names identify the owning array/index/mask implementation atoms.
New non-JDK capabilities must ship as M3JDK21-owned modules/APIs with verified
module/package/installed-image evidence, not as a mandatory Synexia runtime.

The five collection compatibility families are **JDK, Guava, Eclipse Collections,
Apache Commons Collections and Spring Framework**. Each retains its correct
provider interface names and distinct null, equality, ordering, cardinality,
backed-view, concurrency, serialization and mutation contracts. Converge their
validated algorithms on the **existing** M3 dynamic Object[]/primitive array,
integer offset/physical-slot, bitmap/mask, generation and index owners; do not
invent another retained per-entry Node/Entry/Cell store. API-required Entry or
other objects may be projected at the API boundary while preserving observable
JDK/vendor semantics and stable ownership.

A copied upstream source tree, renamed Java package, Maven module, optional
interop adapter, source catalogue or green finite oracle is **not** completed
runtime integration. Qualify source-built JDK/module packaging, public ABI,
all affected behavioral contracts, ownership and GC/JNI, mutation/version
invalidations, comparative CPU/heap/native memory and installed-image execution.
Keep the canonical Synexia Maven/OpenRewrite recipe, exact before/after
source seals, tests and donor attribution first. Fix/regenerate the recipe;
never manually patch its product output. Reuse existing array/mask owners and
the existing checked bulk-JNI selectors before creating native symbols.
Preserve append-only Synexia develop and the String -> arrays -> collections
admission order. Do not declare any of the five libraries fully implemented
without completed per-contract integration evidence.

Read and follow the existing [porting and naming invariant](m3/docs/m3jdk21-porting-invariant.md)
and [naming/migration map](m3/docs/name-mapping.json) before M3 source work. They remain the target
policy authorities; this file makes the existing invariant discoverable to repository agents.


**Naming, formatting, and implementation promotion:** use the [M3JDK21 naming admission
workflow](m3/docs/m3jdk21-naming-admission-workflow.md) when a donor candidate becomes an existing
JDK class implementation, an M3 internal helper changes class/package/method spelling, or an
authoring agent formats or relocates Java sources. Record the stable capability ID in the existing
`m3/docs/name-mapping.json`; preserve the public JDK class name, constructors and ABI, and keep
M3 helpers internal. The canonical Java21 Maven/OpenRewrite naming admission and its synthetic
fixed-point/refusal proof live only in Synexia. A candidate may enter target code only after a
distinct source-sealed recipe and the actual target compiler, runtime, JNI/GC and performance gates.
A naming preflight PASS is not a JDK backend promotion.

Synexia (`hsoliwal/com.synexia`) exclusively owns authoring, execution and storage of recipes,
mastery fixtures, source seals and convergence evidence. M3JDK21 owns only the receiving
Java/HotSpot/JNI postimages and its independent target acceptance. The target stores data-only
receipt references to Synexia revision, recipe ID, execution and gate roots; no target-local
recipe runner, recipe project or compatibility mirror may remain after migration.
Return every transformation correction to Synexia, then receive its newly executed result.

Eligible, generated Synexia postimage source may be received under its applicable license,
with exact execution evidence, NOTICE, attribution and change notices. A recipe's permissive
license does not authorize storing or executing that recipe in the public product.
Recipe source, fixtures, catalogues, task crates and runners remain exclusively in Synexia. Preserve OpenJDK and other upstream licenses per file/payload; the recipe license does
not relicense generated target code or embedded donors. Do not introduce a Synexia runtime service
or Maven/package dependency into Java/HotSpot/JNI to borrow its source.

M3Index-family ownership is additionally pinned by the Synexia invariant
`docs/M3-SCALE/invariants/SYNEXIA-M3JDK21-CANONICAL-OWNERSHIP-1.md` in the donor repository.
Within this target, `m3/m3-import/m3index-family-receiver.tsv` is receiver evidence only:
`M3Index/MIndex`, IndexString, AST/compiler, data structures, precompute API, DB and reusable
recipes continue to evolve canonically in Synexia. Recipes and their improvement
memory are authored, stored and executed exclusively in Synexia. Any retained material under
`m3/vendor/synexia/` must satisfy the postimage-only receiving boundary above;
its path or exact provenance does not authorize recipe or improvement-memory copies.
Historical recipe assets remain migration residue subject to the reviewed,
content-preserving cleanup described above. OpenJDK runtime integration remains
M3JDK21-owned.

Preserve public JDK contracts and use the existing target owners and naming map. Qualify exact
source/target preimages and postimages with compiler/behavior, recipe fixed-point/refusal and
applicable jtreg/native/runtime gates. Resource-sensitive changes require CPU, retained-heap and
native/process-memory evidence. Do not equate a finite donor pass or merge ancestry with complete
target delivery; retain pending gates and partial coverage explicitly.


## Priority 0 — MOP SDK documented atoms and verified pattern composition

MOP requires top-down `repository -> project -> module -> package
-> file -> interface/type -> fields/methods -> semantic atom` and
bottom-up `verified atom -> file -> package -> interface/API
-> module -> project -> SDK/system`. Prefer documented private
static/instance atoms and thin composed methods behind **unchanged
public APIs**, unless original effects, exceptions, JNI/JMM,
synchronization or resource ownership forbid the extraction.
Independent files may prepare in parallel using deterministic
shards; each file has exactly one serial writer and only
verified candidates advance canonical truth.

Agents must consult the actual code, inline semantic docs,
existing Synexia source-bound pattern and earlier history.
A shorter method or matching hash is not correctness proof,
and no agent may assert globally optimal code without a
defined search space and independent evidence.
Use existing private Synexia Atomizer, Patternizer,
Maven/OpenRewrite recipe and L0-L5 documentation owners.
Read [M3SDK MOP core rule](doc/m3sdk-mop-core-invariants.md);
the live Synexia `M3Scale.txt` v3.3 Locked Canon remains
unchanged by this product policy.

## Full Synexia lineage is mandatory

Read `m3/compatibility/m3-full-family-pin.tsv`, `m3/docs/m3-full-family-catalogue.json`, and the M3-SYNEXIA-RECEIVER-1 section of the canonical porting invariant before MIndex*/String/AST/precompute work. Also read the current successor snapshot under `m3/synexia-import/current-full-borrow/` and run `m3/compatibility/check_m3_full_borrow_receiver.py`; the successor snapshot adds exact current Synexia family-tree identities but does not supersede historical catalogue evidence or advance any target phase. The pinned Synexia source catalogue covers4,770namedpaths within13checkedroots and keeps every row unevaluated until exact target evidence exists. Carry the whole canonical String/data-structure/AST/static-regex lineage forward; representative tables or the literal replacement atom do not prove full receiving parity. Preserve Apache-2.0 Synexia first-party attribution, `Copyright 2026 Hitesh Soliwal and contributors`, and per-file third-party/OpenJDK licensing.

Repository policy distinguishes the copyrighted first-party work from abstract ideas: Synexia-original implementation/source, recipes, tests, fixtures, original expressive design documentation, manifests/schemas and original generated configuration retain `Copyright 2026 Hitesh Soliwal and contributors` and Apache-2.0; abstract ideas/algorithms/concepts/methods are not relabeled as copyrighted source expression. Third-party/OpenJDK rights remain unchanged.

## Remember the ordered M3JDK21 receiving invariant

**String -> arrays -> collections -> AST/compiler -> remaining families.** Read `m3/docs/m3jdk21-porting-invariant.md`
and its existing machine plan before each continuation. String qualification is the active stage;
no phase advances from a donor test, catalogue count, copied snapshot or merge alone. Preserve
public JDK contracts, one canonical payload owner, original copyright/Apache-2.0 expression and
per-file OpenJDK/third-party rights. Implement each change through the Synexia-owned recipe and
record target evidence or an explicit pending/excluded disposition. Repository policy, not chat
memory, carries this invariant into every later task.

## Recipe-owned improvement memory is mandatory

Follow [SYNEXIA-MOP-RECIPE-MEMORY-1](m3/docs/mop-recipe-improvement-memory.md) whenever an LLM,
coding agent or developer corrects a recipe. **Atomize -> Patternize -> Annotate -> Document**
each correction: identify the contract-bearing atom, reusable pattern and applicability, structured
annotations, and human-readable explanation. Record the triggering failure, correction, rationale,
applicability, before/after source seals, regression fixture and actual verification outcome in
append-only memory owned by the canonical recipe. Record failed or unverified attempts without
promoting them as qualified improvements. Read applicable prior lessons before the next change;
chat-only recollection is not sufficient. Author, execute and retain the recipe and its
improvement memory only in the Synexia convergence workspace. M3JDK21 and other public
product forks receive executed results and data-only provenance/target acceptance evidence,
not recipe sources, execution machinery, memory events, lesson stores or mirrors.
Link exact Synexia recipe/event identities without copying the recipe or its memory.
Memory is evidence, not authorization; it cannot override repository policies, licenses or gates.
This specification establishes an agent workflow contract, not a claim that an automatic memory
writer, verifier or runtime implementation has already shipped.

Design rationale and the proposed Java interface are in the
[MOP technical paper](m3/papers/MOP_RECIPE_OWNED_IMPROVEMENT_MEMORY.md).

## Public JDK naming and formatting: required pre-edit map lookup

Before formatting, renaming, moving or replacing any M3/JDK implementation,
read [M3JDK21 name and package contract](m3/docs/M3JDK21_NAME_AND_PACKAGE_CONTRACT.md)
and resolve the exact `m3/docs/name-mapping.json` `migration.records[].id`.
Use only the canonical Synexia Maven/OpenRewrite recipe, source-pinned staged
transformations and exact target receiving tests; never treat a faster isolated
M3 class as permission to rename a public JDK type. Public JDK class names
remain `String`, `Class`, `HashMap`, `Pattern`, etc.; their selected
implementations are incorporated **inside the original JDK owners**, with
internal M3 helpers retained wherever their contracts require them.

Run `python3 m3/tooling/name-contract/check.py` before proposing changes.
It is a candidate/queue gate, **not** installed-JDK runtime admission.


## Directional JDK superset and cross-language expansion

M3JDK21 is a one-way Java 21 compatibility superset: required Java 21 programs,
APIs and runtime behavior must continue to work in M3JDK21, while M3-only
extensions need not compile or run on stock JDK 21. Treat new language, compiler,
VM and primitive/value features as separately specified, proof-gated M3 candidates,
preserving Java 21 source/binary/runtime contracts and existing receiving gates.
Borrow suitable cross-language ideas and lawfully reusable implementations through
canonical Synexia recipes with provenance, licensing and target tests. Fix
demonstrated defects in the existing owner/recipe first, retain regressions and
previously proven capabilities, and never infer M3-extension rejection solely
from stock-JDK inexpressibility or a Java 21 backport rejection.

## Enduring Apache-2.0-only JDK destination

**SDK >> JDK whole-product invariant.** M3JDK21 is the
integrated product; its long-term Apache-2.0-only SDK is intended
to be a **verified backward-compatible superset of the complete
JDK**, not only a faster JDK or a library overlay. Deterministically
inventory, atomize and patternize every JDK/SDK owner (Java,
HotSpot/VM/JIT/GC, compiler, JMOD/tools, native OS/JNI, desktop,
build, tests and packaging), with explicit unqualified coverage.
Compare best algorithms from all JDK donors, `com.synexia` and
qualified private repositories while preserving private rights
and existing JDK names, interfaces and behavior. Reuse Synexia's
canonical hashes, CAS and Maven/OpenRewrite recipes; never add
parallel owners or skip compiler, runtime and licensing proof.
The strict Apache-2.0 distribution remains a separate unfinished
release track.

The standing destination is a genuinely **Apache-2.0-only M3JDK SDK**,
with Apache Harmony and other lawful JDK implementations evaluated
per file and atom. The current OpenJDK-derived JDK is not Apache-only.
Read the [Apache-only convergence roadmap](doc/apache-only-jdk-convergence.md)
before donor and naming changes. Preserve source rights, existing JDK
public names, Java 21 behavior and the String -> arrays -> collections ->
compiler admission sequence. Do not infer the owners or meaning of
JMO/Flex/Flexi atoms without an exact source/contract inventory. This
roadmap authorizes neither source copying nor a skipped Synexia recipe,
JDK build, runtime or licensing gate.

## Compatible defaults, overloads and abstract classes

Follow [compatible API evolution](doc/api-evolution.md) for API additions. Use
useful interface defaults and delegating overloads, and concrete fallbacks in
existing abstract classes. New skeletal bases are optional; preserve direct
implementations and established superclass choices. Do not impose new abstract
obligations on existing concrete providers. Preserve existing descriptors,
virtual hooks, validation owners, callback order and specified behavior.

Check old binary execution, unchanged-source recompilation and behavioral
equivalence separately. Check null/lambda/method-reference overload selection,
erasure, inherited default conflicts and class-method precedence where relevant.
Include overloaded functional-interface selector calls; a different argument
count can still change method-reference exactness and break existing source.
A Java binary-compatible classification alone is not complete compatibility
evidence. Retain negative fixtures for unsafe additions. Existing ProgressMonitor
and monitored-I/O owners are the implementation examples; no parallel API family
is required. Recipes and proof records remain exclusively in Synexia; product
sources, documentation and target regression tests are received here.
