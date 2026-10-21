<!-- SPDX-License-Identifier: Apache-2.0 -->
# Original M3JDK21 primitive patch run composition, not a collection overlay

M3JDK21 is the **integrated JDK21/HotSpot/Java/JNI SDK**, not M3 layered
over another JDK. The original `com.m3.collections.M3Patch` remains
the sole patch/array owner used by `M3ListDiff`; no additional
collection hierarchy, index store, Entry object or JNI export is added.

**Canonical Maven/OpenRewrite source recipe:**
[Synexia #12894](https://github.com/hsoliwal/com.synexia/pull/12894)
at `43c78fbedd065c35d78c34ef27d0a76677129262`, existing `com.synexia.m3jdk.AstBuildReverse20261017`
and `M3HashPinnedJavaSnapshotRecipe`, new three-Java-target dataset
`patch-runs-20261021`. No parallel transformation engine or
Maven/OpenRewrite dependency in the product runtime.

One original source method `M3Patch.apply(long[],long[])` is refined.
`M3ListDiff` already generates descending primitive removal runs,
ascending primitive insertion runs and moved-key metadata. Reapplying
these runs one element at a time to a dynamic `M3LongArrayList`
repeatedly shifts the backing storage, and then `toArray()` makes
another copy. The new method walks the two original run cursors and
copies unchanged and inserted contiguous spans directly to a **single
new long[]** with `System.arraycopy`. O(n+number of runs) primitive
work, O(1) temporary state beyond the required returned array.
Everything about `M3Patch.applyTo(Target)`, `M3ListDiff.diff`, move
coordinates, stable order, public signatures, JDK naming and provider
contracts remains unchanged for valid source-generated patches.

Original source Git blob:
`6155a2df20860b031ae742b56f024994be3d9f3e`,
SHA256 `7ec4e720caf64549d89a93447e435492a7c5c77d75750f7313118cbe2e375711`.
Reviewed candidate Git blob:
`be4975dd9e493af627a9a8b28991b4b30d8bea11`,
SHA256 `f6a64f760de80b7cf3866eb9449d8b4815c111e9212bca0eb5946e85c442f3f1`.
Executable original-owner test blob:
`33a99aacca6a9c1d74b7472d0f958b9f59208e3a`,
JUnit wrapper blob:
`31c2e1d19f03585169b6c66e2dda333c4ff979e2`.

## Executed (actual complete owner source, Java 21)

The complete candidate `M3Patch.java` was locally compiled together
with `M3PatchApplyRunsContract.java` under real OpenJDK21.0.12.1,
`javac --release 21 -Xlint:all -Werror`. Its local source Git blob
matched the canonical **full source** byte-for-byte.
The oracle passed `M3_PATCH_RUN_APPLY_PASS checks=125036` in
default, `-Xint`, C1 and non-tiered JVM modes. 25,000 seeded randomized
remove/insert patches, explicit move, no-op/null-after, borrowed
source immutability, independent old `Target.applyTo` behavior and
200,000-row alternating edit stress were covered. The old method's
separate reconstructed model passed the smaller 125,034-check oracle.
No generalized elapsed-time or memory speedup claim is made.

The exact-head GitHub workflow compiles and runs the **full original
`M3Patch` source** and checks three canonical postimage hashes; it
does not rely on a test-only M3 patch implementation.

## Required pending gates

Canonical **actual** Maven/OpenRewrite SDK `Recipe.run`, SDK Result
receipt/receiver fixed point, whole m3/collections JUnit including
original `M3ListDiffTest`, complete M3JDK21 java.base/HotSpot/JNI
image/jtreg/JCK, SWT/JFace/Workbench/EMF event and pixel differential,
and measured CPU/heap/JNI/GPU performance are **NOT_RUN** here.
Keep this receiver **DRAFT** pending real Maven and exact-head workflow
evidence. Source identity is not complete product acceptance.
All first-party source retains its Apache-2.0 header; educational
problem taxonomies were considered without copying donor code.
No direct change to master or Synexia develop, no force push/rebase.

### Repeat the executed narrow test

```sh
mkdir -p /tmp/m3-patch-classes
javac --release 21 -Xlint:all -Werror -d /tmp/m3-patch-classes \
  m3/collections/src/main/java/com/m3/collections/M3Patch.java \
  m3/collections/src/test/java/com/m3/collections/M3PatchApplyRunsContract.java
java -cp /tmp/m3-patch-classes com.m3.collections.M3PatchApplyRunsContract
```
