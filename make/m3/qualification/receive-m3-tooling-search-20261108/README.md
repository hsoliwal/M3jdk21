# Receipt: make/m3/tooling/search (com.synexia sorted batch bounds, Java 21 + C17/JNI)

New module `make/m3/tooling/search` (`jdk.m3.tooling.search`). It is an opt-in tooling primitive: lower and upper
bounds for a sorted `int[]` or `long[]` segment, answered for a whole batch of keys. Java and JNI implementations
are kept at parity. `java.util.Arrays.binarySearch` is not touched.

## Where the files come from

The module was built in com.synexia as a stack of receiver crates:

| Step | PR | Crate | Changes |
|---|---|---|---|
| base | #15087 | `m3jdk-sorted-batch-bounds-20261101` | module, Java + JNI int bounds, self-test, benchmark, verify.sh |
| 1 | #15144 | `m3jdk-sorted-empty-keys-20261101` | C: no native snapshot for a zero-key batch |
| 2 | #15178 | `m3jdk-sorted-empty-range-20261101` | Java + C: empty source interval answered without reading inputs |
| 3 | #15178 | `m3jdk-sorted-lower-reuse-20261102` | Java + C: upper search starts at the proven lower bound |
| long | #15120 | `m3jdk-sorted-long-batch-bounds-20261101` | `long[]` variant, its own self-test, benchmark and verify script |

Steps 1 to 3 are on develop `ded4a4acdffe`. The base crate (#15087) and the long-bounds crate (#15120) were merged
into `m3/superset-live-fold-independent-recipes-20261101`, whose commits are ancestors of develop. Their crate
directories are not in develop's tree: an ancestry-only fold dropped them. Their files are taken from the PR heads.

`M3SortedIntBounds.java` and `m3_native_sorted_bounds.c` are the step-3 postimages. Every other file is the
base or long-bounds postimage. All 14 files are byte-identical to their donor blobs (`PROVENANCE.tsv`).

## Gates (OpenJDK 21 + GCC C17, Linux)

`GATES.tsv` has the results; `gate.sh` is the runner. The three step proofs run on a mirror of the com.synexia
crate layout, because each pins its predecessor and successor by SHA-256.

## Side branches not received

`DECISION.tsv`: #15108 is superseded by steps 1 and 2. #15133 changes the out-of-memory contract and needs an
owner decision. #15117 adds two orthogonal features and is left for a separate union leaf.
