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

Preserve public JDK contracts and use the existing target owners and naming map. Qualify exact
source/target preimages and postimages with compiler/behavior, recipe fixed-point/refusal and
applicable jtreg/native/runtime gates. Resource-sensitive changes require CPU, retained-heap and
native/process-memory evidence. Do not equate a finite donor pass or merge ancestry with complete
target delivery; retain pending gates and partial coverage explicitly.


## Current M3 String recipe authority

The reusable M3 String history-convergence recipe is owned only by Synexia:

- recipe: `com.synexia.rewrite.m3jdk.M3StringHistoryConvergence`
- repository: `hsoliwal/com.synexia`
- custody PR: `#9530`
- pinned custody revision: `ad5df8458ffeba643a6c343e82dd61a338facf29`

M3JDK21 keeps the applied runtime, target-native tests and a compact application receipt. Do not
recreate the deleted `m3-string-history-convergence` recipe descriptor, snapshot crates, or local
fixed-point recipe test here.
