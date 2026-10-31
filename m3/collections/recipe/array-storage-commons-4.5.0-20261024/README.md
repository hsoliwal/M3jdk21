<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Commons Collections 4.5.0 array-storage receiving recipe (E2)

`com.synexia.m3.ArrayStorageCommons450V20261024` runs the unchanged canonical engine `M3HashPinnedJavaSnapshotRecipe`
(PlainText custody mode) on crate `array-storage-commons-450`. It replaces the exact received 4.5.0 bodies of
SparseBloomFilter, IteratorChain, PermutationIterator, TreeList and CircularFifoQueue with their array-storage
derivatives, and installs the seven array-storage tests. The preimages are the `afterSHA256` values of
`receiving/commons-collections-4.5.0-20261018` and are kept as `*.before.txt`. Test resources and the
`m3/array-operations-20261019/verification` sources lie outside the engine's Maven roots; the successor receipt binds
them.

`M3ArrayStorageCommons450RecipeTest`: 128 transition states, covering every subset of already-transitioned bodies,
tests absent or present, and both input orders, each with a fixed point; 29 refusals (missing, drifted or duplicated
preimages).
