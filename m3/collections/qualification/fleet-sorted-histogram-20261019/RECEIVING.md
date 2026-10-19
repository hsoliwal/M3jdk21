# M3JDK21 fleet convergence: sorted bitmap + state-mask JNI (19 October 2026)

**Product**: M3JDK21 Java21/C17 runtime. No M3-on-JDK layer, no second
array store, no Synexia runtime dependency.

**Existing owning branch**: hsoliwal/M3JDK21 #1405, stacked on #1391
(after #1354 sorting, #1365 reentrant filtered views, #1377 utilities).
All previous collection-payload, state-mask, ABI and test owners remain.

**Qualified donor source authority**: hsoliwal/com.synexia #12403,
exact canonical commit `1124f5b72744b306884beedebafd8558d49d4c4c`,
existing named Maven/OpenRewrite recipe
`com.synexia.m3.ArrayMaskLookup20261018`.
It generated M3JDK21 #1408's two existing production Java/C files and
their two existing Java contract tests. The four donor Git blobs and their
qualified SHA-256 files are recorded in CATEGORY_CATALOGUE.tsv.
Reusing the exact original implementation is preferable to a second port.

The four product parent source files match #1408's exact source preimages.
The new M3JDK21 product branch receives the **four full byte-identical**
canonical product postimages serially. These paths do not overlap #1405's
changed `M3IntIntHashMap.java`, `m3_index_mask_compactor.c`,
immutable filtered/sorted index directory, or related tests.

The added YAML-only recipe is
`com.synexia.m3.M3Jdk21CompositeSortedHistogram20261019`,
implemented by existing `M3HashPinnedYamlSnapshotRecipe` (no new recipe engine).
Its sole ABSENT target is a focused Java21 + C17 JNI workflow. The
workflow validates exact source SHA-256; compiles and exercises real
sorted bitmap Java/native projections; compiles state-mask histogram
Java and actual C JNI under UBSan and `-Xcheck:jni`; tests rejection
and output isolation. No duplicate Entry nodes or JNI symbol added.

Acceptance is **draft pending matched target execution and full Maven/HotSpot**:
previous first-party #1408 validation is donor evidence, not a new
M3JDK21 #1405 successor run. Current master and stacked ancestors retain
independent CI failures and are not promoted by focused tests.

Problem families: LeetCode/HackerRank/GFG binary search, sorted membership,
state-mask histogram / batched strided gather. No third-party challenge source
is copied. Existing first-party Apache-2.0 source and notices retained.

## First combined receiving execution (compile closure correction)

The new product CI initially failed **before executing assertions** because
`M3LongIndexProjection`'s original Java dependencies were missing from the
standalone `javac` command: `M3LongAddressKernel` and `M3IndexPathMasks`.
Neither owner nor the qualified #1408 Java/C atom changed. The canonical
source-sealed workflow now includes those two exact existing owners in the
compilation input set, preserving the original test and JNI proof scope.
The original failed CI remains visible; only a new actual green run can qualify
this composed product.
