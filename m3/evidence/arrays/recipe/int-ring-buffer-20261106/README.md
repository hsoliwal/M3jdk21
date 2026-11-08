<!-- SPDX-License-Identifier: Apache-2.0 -->
# Recipe int-ring-buffer-20261106: bounded primitive int ring

Census leaf L019 of the M3JDK convergence programme (crate `m3-mat-ring-20261030`, NO_OWNER on master). Relocates, by
recipe, the Synexia MAT `MatIntRingBuffer` as `jdk.m3.arrays.M3IntRingBuffer`: a bounded int ring of any fixed
capacity >= 1 (no power-of-two rounding, never grows); `offerFirst` / `offerLast` return false when full, `peek*` /
`remove*` at both ends throw `NoSuchElementException` when empty, `isFull`, an independent `snapshot` copied in two
`arraycopy` runs, `primitivePayloadBytes` = 4 x capacity.

| Target | Scope | Donor (hsoliwal/com.synexia develop `ca1aafc4d94f`) |
|---|---|---|
| `M3IntRingBuffer` | main | `synexia-mat-collections/.../MatIntRingBuffer.java`, blob `8e2f4be2cd4e` (main path = the crate postimage; 20 lines) |
| `M3IntRingBufferContract` (+ `...Test`) | test | crate oracle `MatIntRingBufferAtomOracle`, blob `842dff4a9847` (ArrayDeque model) |
| `M3IntRingBufferDump`, `M3IntRingBufferReferenceTest` | test | M3JDK-authored differential dump and brute-force reference model |

## Owner check

`M3IntArrayDeque` and `M3PrimitiveArrayDeque` (m3/collections, protected, never edited) grow at power-of-two
capacities and never refuse an add: a different contract, so this is not ALREADY. `M3EvictingQueue` evicts the head of
an object queue when full; `M3OffHeapRingBuffer` is an Agrona-style message-ring scaffold. All four are NEC rows of
`RELOCATION_MAP.tsv` (equivalence not decided, nothing changed). Target: m3/arrays, the synexia-mat lineage.

## Run

```sh
python -I relocate.py --donor-git <com.synexia clone> --donor-rev ca1aafc4d94f9dd1ee288226dcca070cc60991c9 [--check]
JAVA_HOME=<JDK 21> MAVEN_REPO_LOCAL=<repo> MVN=mvn bash verify.sh <com.synexia clone> ca1aafc4d94f9dd1ee288226dcca070cc60991c9 <scratch> <baseline module copy>
```

`verify.sh` runs every gate of `GATES.tsv`. The benchmark, the OpenRewrite recipe test, the pass-1 stage and the
family-wide MAT contract are OMIT rows. Lessons: `RECIPE_MEMORY.tsv`.
