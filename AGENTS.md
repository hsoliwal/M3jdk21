<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 agent instructions

Read and follow the existing [porting and naming invariant](m3/docs/m3jdk21-porting-invariant.md)
and [naming/migration map](m3/docs/name-mapping.json) before M3 source work. They remain the target
policy authorities; this file makes the existing invariant discoverable to repository agents.

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

Read `m3/compatibility/synexia-full-family-pin.tsv`, `m3/docs/synexia-full-family-catalogue.json`, and the M3-SYNEXIA-RECEIVER-1 section of the canonical porting invariant before MIndex*/String/AST/precompute work. The pinned Synexia source catalogue covers4,770namedpaths within13checkedroots and keeps every row unevaluated until exact target evidence exists. Carry the whole canonical String/data-structure/AST/static-regex lineage forward; representative tables or the literal replacement atom do not prove full receiving parity. Preserve Apache-2.0 Synexia first-party attribution, `Copyright 2026 Hitesh Soliwal and contributors`, and per-file third-party/OpenJDK licensing.

Repository policy distinguishes the copyrighted first-party work from abstract ideas: Synexia-original implementation/source, recipes, tests, fixtures, original expressive design documentation, manifests/schemas and original generated configuration retain `Copyright 2026 Hitesh Soliwal and contributors` and Apache-2.0; abstract ideas/algorithms/concepts/methods are not relabeled as copyrighted source expression. Third-party/OpenJDK rights remain unchanged.

## Remember the ordered M3JDK21 receiving invariant

**String -> arrays -> collections -> AST/compiler -> remaining families.** Read `m3/docs/m3jdk21-porting-invariant.md`
and its existing machine plan before each continuation. String qualification is the active stage;
no phase advances from a donor test, catalogue count, copied snapshot or merge alone. Preserve
public JDK contracts, one canonical payload owner, original copyright/Apache-2.0 expression and
per-file OpenJDK/third-party rights. Implement each change through the Synexia-owned recipe and
record target evidence or an explicit pending/excluded disposition. Repository policy, not chat
memory, carries this invariant into every later task.
