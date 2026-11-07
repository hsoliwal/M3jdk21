<!-- SPDX-License-Identifier: Apache-2.0 -->
# Recipe planner-family-20261107: the index precompute planner, multipass compiler and their closure

Follow-up leaf of `consumer-profiles-20261107` (PR #2665), which held the planner family. Relocates, by recipe
(RELOCATE_PLANNER_FAMILY v4), the donor `synexia-index-precompute` planner family into `jdk.m3.precompute`
(`m3/ports/precompute`): 26 main types and the 3 crate tests of the family, from hsoliwal/com.synexia develop
`0616288bfa7d`.

| Group | Targets (`jdk.m3.precompute`) |
|---|---|
| planning | `M3IndexPrecomputePlanner` (exact search up to 20 candidates, density greedy above), `M3IndexPrecomputeCandidate`, `M3IndexPrecomputeRecommendation`, `M3IndexPrecomputePlan` |
| multipass build | `M3IndexPrecomputeMultipassCompiler`, `M3IndexPrecomputeSource`, `M3IndexPrecomputeProvider(s)`, `M3IndexPrecomputeFacet`, `M3IndexPrecomputeFacetGroup`, `M3IndexPrecomputeSnapshot`, `M3IndexPrecomputeSnapshotBuilder` |
| evidence | `M3IndexPrecomputeReceipt`, `M3IndexPrecomputeManifest`, `M3IndexPrecomputeManifestImage`, `M3IndexPrecomputeDependencyImage`, `M3PrecomputeHashes` |
| tuple lookups | `M3FrozenLongTupleIndex`, `M3LongTupleIndexView`, `M3LongTupleIndexImage`, `M3NativePrecomputeOps` (Java fallback; native optional), `M3IndexPrecomputeKernel`, `M3IndexPrecomputeAccelerator(s)`, `M3JavaIndexPrecomputeAccelerator`, `M3JniIndexPrecomputeAccelerator` |
| crate tests | `M3IndexPrecomputeDependencyImageTest`, `M3IndexPrecomputeMultipassCompilerTest`, `M3FrozenLongTupleIndexPairRegressionTest` |
| authored (test) | `M3IndexPrecomputePlannerDump` (reflective donor/relocated dump + reference model), `M3IndexPrecomputePlannerReferenceTest` |

## Decisions

- Closure computed from the donor tree (`closure.py` next to this file: same-package identifiers, imports, qualified
  references; comments excluded). `IndexPrecomputeCapability` / `IndexPrecomputeWorkload` are already relocated by
  consumer-profiles (TYPE rows without FILE rows).
- Lineage: every main path was last written by `4a2e6452a7ad` (2026-10-31); conflict copies and provenance-recipe
  preimages are older. `NativePrecomputeOps` reads the hash-pinned postimage of crate
  `m3-tuple-fallback-scalar-java21-20261102`, whose before is the develop main path.
- `com.synexia.job.IProgressMonitor` (written fully qualified in `IndexPrecomputeSnapshot`) maps to the module's own
  `M3Progress` through the new QUALIFIED rows (recipe v4); `noop()` becomes `M3Progress.none()`.
- LITERAL rows apply the naming-rule renames to two system properties, the library name (`m3_precompute`, the module's
  CMake library, which has no tuple-lookup symbol yet: the Java path runs) and the tuple root-hash domain
  (`M3_FROZEN_LONG_TUPLE_V1:`), so root hashes differ from the donor's by declaration.
- DEFERRED: the C kernels `precompute_jni.c` / `tuple_lookup_jni.c` (WSL native gate not run). OMIT: the provider
  ServiceLoader descriptor and the crate tests whose closure leaves the family (mindex, indexstring, execution).
- NEC, untouched: vendor `MIndexPrecomputePlan`, `MIndexPrecomputePressureBudget`, `m3/arrays` `M3LongCoordinateIndex`,
  `m3/fs` `M3PrecomputeKey`.
- The consumer-profiles map gains TYPE rows for the two planner links of its javadoc (module doclint errors 2 -> 0).
- Recorded donor behaviour, not fixed: the greedy closure recursion of the planner overflows the stack on a cyclic
  dependency graph (more than 20 candidates); the dump's CYCLE_PROBE line pins it on both sides.

## Run

```
python -I m3/ports/precompute/recipe/planner-family-20261107/relocate.py --donor-git <com.synexia clone> --donor-rev 0616288bfa7d91eb0abb754c7a29707f10318752 [--check]
bash m3/ports/precompute/recipe/planner-family-20261107/verify.sh <com.synexia clone> 0616288bfa7d91eb0abb754c7a29707f10318752 <scratch> <baseline m3/ports/precompute build> <junit-platform-console-standalone.jar>
```

Gates: GATES.tsv; provenance: PROVENANCE.tsv; lessons: RECIPE_MEMORY.tsv.
