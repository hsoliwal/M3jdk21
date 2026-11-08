<!-- SPDX-License-Identifier: Apache-2.0 -->
# Recipe hybrid-int-set-20261106: chunked hybrid int set

Census leaf L018 of the M3JDK convergence programme (crate `m3-mat-hybrid-sorted-unique-20261030`, NO_OWNER on
master). Relocates, by recipe, the Synexia MAT `MatHybridIntSet` as `jdk.m3.arrays.M3HybridIntSet`: an immutable set
of non-negative ints in 16-bit chunks, each chunk stored as ARRAY (char lane), RUNS (start/length pairs, pair count
packed into the count lane, binary search after a one-run fast path) or BITMAP (1,024 words), whichever is smallest;
`of(int...)`, `contains`, `toArray`, `union` / `intersect` / `xor` / `minus` (new sets), `chunkKind` / `chunkKey`,
`primitivePayloadBytes`.

| Target | Scope | Donor (hsoliwal/com.synexia develop `ca1aafc4d94f`) |
|---|---|---|
| `M3HybridIntSet` | main | `synexia-mat-collections/.../MatHybridIntSet.java`, blob `39f209d3f2ee` (main path = run-binary postimage, successor of the census crate's postimage) |
| `M3HybridIntSetSortedBuildContract` (+ `...Test`) | test | sorted-unique crate oracle `MatHybridSortedBuildOracle`, blob `e6d7564b6131` |
| `M3HybridIntSetRunSearchContract` (+ `...Test`) | test | run-binary crate oracle `MatHybridRunSearchOracle`, blob `fd404cb78877` |
| `M3HybridIntSetRunPackedMetadataContract` (+ `...Test`) | test | run-binary crate oracle `MatHybridRunPackedMetadataOracle`, blob `94ce30ad8f94` |
| `M3HybridIntSetDump`, `M3HybridIntSetReferenceTest` | test | M3JDK-authored differential dump and TreeSet reference model |

## Owner check

No int-set owner with this contract on master. `M3RoaringBitmap` / `M3FrozenIntSet` (m3/collections scaffolds,
different contracts and donors), `M3IntHashSet` (mutable hash set) and `M3SortedIntMultisets` (sorted-array algebra)
are NEC rows of `RELOCATION_MAP.tsv`: equivalence not decided, nothing changed.

## Run

```sh
python -I relocate.py --donor-git <com.synexia clone> --donor-rev ca1aafc4d94f9dd1ee288226dcca070cc60991c9 [--check]
JAVA_HOME=<JDK 21> MAVEN_REPO_LOCAL=<repo> MVN=mvn bash verify.sh <com.synexia clone> ca1aafc4d94f9dd1ee288226dcca070cc60991c9 <scratch> <baseline module copy>
```

`verify.sh` runs every gate of `GATES.tsv`. Benchmarks, OpenRewrite recipe tests and the family-wide MAT contracts
are OMIT rows. Lessons: `RECIPE_MEMORY.tsv`.
