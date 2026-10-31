<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21: stable descending indexed views over physical state masks

**Runtime owner:** `m3/collections/src/main/java/com/m3/collections/M3IndexedListSnapshot.java`  
**Recipe:** `com.synexia.m3jdk21.StableDescendingIndex20261019`  
**Preimage:** historical M3JDK21 PR #1501 head `3867520c06a89a57cecbc9160517677091a10e6d`  
**Receiving status:** stacked **draft**, not installed `java.base` promotion.

## Behavioural atom

One canonical GC-visible `Object[]` payload remains owned by the target's
`M3IndexedListSnapshot`. Physical positions are `int[]`; span boundaries are
`from/to`; full-width mask state remains caller-owned `int[][]` planes.

`sortedDescending(Comparator)` makes a detached primitive directory:
- If input positions are already comparator-ascending, scan equal-comparator
  groups right-to-left, copying each group forward unchanged (stable equality).
  Complexity: O(k) time, O(k) primitive int scratch, no entry nodes.
- Otherwise, reuse the **existing** `sortPositions` stable merge sorter with
  swapped comparator operands, rather than negating comparator results.
  Complexity: O(k log k) time, O(k) primitive scratch.

`sortedMaskedByPhysicalStatesDescending` first selects admitted physical
positions through existing `M3IndexPathMasks.select`, then orders the new
private index directory **in place**. The canonical payload and parent slices
remain unchanged; rejected objects are never passed to the comparator.

The runtime does **not** retain caller masks, expose `Object[]` to JNI, copy a
new payload for sorting, or introduce an Entry/Node/Cell hierarchy. The only
native component is the already-existing C17 path-mask kernel. A required
native backend never silently falls back.

## Source recipe and multipass law

`inventory -> dated additive convergence -> catalogue by problem category ->
INLINE -> ATOMIZE -> PATTERNIZE -> COMMENT -> mechanical forward/backward ->
algorithmic forward/backward -> mathematical forward/backward -> LLM last ->
diff -> linter -> compile -> tests -> JNI/runtime -> serial PR`

The existing Synexia `M3GpuToolJoinRecipe` and its SHA-256 manifest scanner are
reused without altering the recipe engine. Exact source preimage or known
postimage is required. Unknown bytes, missing owners or occupied additions
must refuse. A repeated application at the postimage must be a fixed point.
Never fix generated target code directly; change the canonical resource.

## Focused local execution

Java 21 standalone contract (not the Maven/JUnit or installed JDK gate):

```sh
set -eu
mkdir -p build/m3-stable-descending
src=m3/collections/src/main/java/com/m3/collections
javac --release 21 -Xlint:all -Werror -d build/m3-stable-descending \
  "$src/M3IndexMaskCompactor.java" "$src/M3IndexPathMasks.java" \
  "$src/M3IndexedListSnapshot.java" \
  m3/collections/src/test/java/com/m3/collections/M3StableDescendingSnapshotProof.java
java -cp build/m3-stable-descending com.m3.collections.M3StableDescendingSnapshotProof
```

Native differential (Linux, C17, explicit JNI required):

```sh
cc -std=c17 -Wall -Wextra -Werror -fPIC -shared \
  -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
  m3/collections/src/main/native/collections/m3_index_path_masks.c \
  -o build/m3-stable-descending/libm3_collections_pathmask.so
java -Xcheck:jni -Djava.library.path=build/m3-stable-descending \
  -cp build/m3-stable-descending \
  com.m3.collections.M3StableDescendingSnapshotProof --native
```

The included Java/JNI workflow must produce executed receipts. The independent
JDK comparator oracle covers null groups, stable duplicates, sliced paths,
pre-sorted and unsorted permutations, `Integer.MIN_VALUE` comparison results,
32-bit staged mask filtering, native refusal before callbacks, input isolation
and immutable/no-op mutator rejection.

## HOLD

This staged module is an M3JDK21 proving owner, not a blanket `java.util`
backend replacement. Public OpenJDK contracts, array covariance, concurrent
semantics, whole-collections Maven, jtreg, HotSpot, installed image and
resource/performance gates remain independent. Keep the existing receiving
phase `String -> arrays -> collections -> AST/compiler`; do not promote before
all preceding gates are actually satisfied.
