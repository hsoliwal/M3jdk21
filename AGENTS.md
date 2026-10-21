<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 agent instructions

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

Synexia (`hsoliwal/com.synexia`) is the donor-convergence workspace. M3JDK21 owns the delivered
Java/HotSpot/JNI runtime and target acceptance. Keep reusable Maven/OpenRewrite recipes, fixtures,
source seals, provenance and convergence receipts in Synexia, then apply qualified recipes to
materialize target changes. Target-local proof-tooling mirrors keep exact Synexia revision/hash/
license lineage and do not become competing canonical recipe owners. Return improvements as
new Synexia convergence evidence.

Eligible Synexia-owned Apache-2.0 source **and recipes** may be freely reused under that license,
including fixtures, catalogues and proof tooling. Retain license, NOTICE, attribution and change
notices. Preserve OpenJDK and other upstream licenses per file/payload; the recipe license does
not relicense generated target code or embedded donors. Do not introduce a Synexia runtime service
or Maven/package dependency into Java/HotSpot/JNI to borrow its source.

M3Index-family ownership is additionally pinned by the Synexia invariant
`docs/M3-SCALE/invariants/SYNEXIA-M3JDK21-CANONICAL-OWNERSHIP-1.md` in the donor repository.
Within this target, `m3/synexia-import/m3index-family-receiver.tsv` is receiver evidence only:
`M3Index/MIndex`, IndexString, AST/compiler, data structures, precompute API, DB and reusable
recipes continue to evolve canonically in Synexia. A copy under `m3/vendor/synexia/` is exact
custody/proof, not a second implementation authority. OpenJDK runtime integration remains
M3JDK21-owned.

Preserve public JDK contracts and use the existing target owners and naming map. Qualify exact
source/target preimages and postimages with compiler/behavior, recipe fixed-point/refusal and
applicable jtreg/native/runtime gates. Resource-sensitive changes require CPU, retained-heap and
native/process-memory evidence. Do not equate a finite donor pass or merge ancestry with complete
target delivery; retain pending gates and partial coverage explicitly.


## Full Synexia lineage is mandatory

Read `m3/compatibility/synexia-full-family-pin.tsv`, `m3/docs/synexia-full-family-catalogue.json`, and the M3-SYNEXIA-RECEIVER-1 section of the canonical porting invariant before MIndex*/String/AST/precompute work. Also read the current successor snapshot under `m3/synexia-import/current-full-borrow/` and run `m3/compatibility/check_synexia_full_borrow_receiver.py`; the successor snapshot adds exact current Synexia family-tree identities but does not supersede historical catalogue evidence or advance any target phase. The pinned Synexia source catalogue covers4,770namedpaths within13checkedroots and keeps every row unevaluated until exact target evidence exists. Carry the whole canonical String/data-structure/AST/static-regex lineage forward; representative tables or the literal replacement atom do not prove full receiving parity. Preserve Apache-2.0 Synexia first-party attribution, `Copyright 2026 Hitesh Soliwal and contributors`, and per-file third-party/OpenJDK licensing.

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
chat-only recollection is not sufficient. Preserve canonical Synexia recipe ownership and link
M3JDK21 target acceptance evidence rather than creating a competing recipe implementation.
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
