<!-- SPDX-License-Identifier: Apache-2.0 -->
# Recipe utf16-stitch-20261106: stitched sparse UTF-16 first-index scan

Census leaf L016 of the M3JDK convergence programme (crate `m3-utf16-stitch-20261022`, status NO_OWNER on master).
Relocates, byte for byte, the stitched scanner and its closure from hsoliwal/com.synexia develop `ca1aafc4d94f` into
`jdk.m3.precompute`:

| Target | Scope | Donor |
|---|---|---|
| `M3SparseUtf16Stitched` | main, package-private | crate `m3-utf16-stitch-20261022` postimage = `m3-literal-batch-precompute-20261022` main path, blob `8b7b57679739` |
| `M3SparseUtf16LiteralPlan` | main | reviewed postimage of crate `m3-sparse-java-first-completion-20261026`, blob `baa2148760ba` (newest lineage; the main-path blob `1b4a6a1f4c3c` is its preimage) |
| `M3LiteralBatchCandidate` | main | crate `m3-literal-batch-precompute-20261022`, blob `c5be09d9289a` (constants read by both; its own ASCII Shift-And batch) |
| `M3SparseUtf16StitchedOracle`, `M3SparseUtf16StitchedTest` | test | the stitch crate's oracle and JUnit gate (renamed after the owner) |
| `M3SparseUtf16StitchedDump`, `M3SparseUtf16StitchedReferenceTest` | test | M3JDK-authored differential dump and brute-force reference-model test |

## Owner check

No type named Stitch*, SparseUtf16*, *LiteralPlan or *LiteralBatch* exists on master; no java.base class consumes it.
Multi-literal owners that do exist answer different contracts and are recorded as NEC rows (equivalence not decided):
`jdk.m3.tools.cpu.M3AhoCorasick` (first offsets over a whole CharSequence), `jdk.m3.text.M3NativeAho` (ordinal
bitmap prefilter), `MultiLiteralSearch` (all hits), `jdk.m3.search.M3BitapExactSearch` (one byte[] needle). Target:
the Maven text precompute lane `m3/ports/precompute`, beside `M3LcsPlan` and `M3Utf16Glob` (#2504).

## Run

```sh
python -I relocate.py --donor-git <com.synexia clone> --donor-rev ca1aafc4d94f9dd1ee288226dcca070cc60991c9 [--check]
JAVA_HOME=<JDK 21> MAVEN_REPO_LOCAL=<repo> MVN=mvn bash verify.sh <com.synexia clone> ca1aafc4d94f9dd1ee288226dcca070cc60991c9 <scratch> <baseline module copy>
```

`verify.sh` runs every gate of `GATES.tsv`: recipe fixed point, `mvn package` (`-Xlint:all -Werror`), naming plan,
`differential.sh` (donor vs relocated dumps, JIT and `-Xint`, plus the superseded main-path lineage), `javap_parity.sh`
and `class_identity.sh` (every class file of a master build of the module unchanged).

## Held

The two C17 JNI kernels (`m3_utf16_sparse_probe.c`, `m3_literal_batch_probe.c`) stay in com.synexia until a native
consumer asks; the Java natives are declared and the explicit native lanes throw `UnsatisfiedLinkError` without a
library (never a silent Java fallback). The OpenRewrite recipe test and the sibling `M3LiteralBatchAutomaton` are OMIT
rows. Lessons: `RECIPE_MEMORY.tsv`.
