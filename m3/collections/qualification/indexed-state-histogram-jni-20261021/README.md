# M3JDK21 indexed JNI state-mask histogram — original-owner receiving
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

Product: one M3JDK21 Java21/HotSpot/JNI SDK. Original Java `M3StateMaskHistogram`
and original C `m3_collections_statehist` native library remain authoritative.
This PR does **not** create an M3 + JDK adapter, another collection payload
store, or a parallel JNI runtime.

Canonical source and exact recipe:
[Synexia #12702](https://github.com/hsoliwal/com.synexia/pull/12702).
Class-backed `M3HashPinnedJavaSnapshotRecipe` for the Java owner and the
two new test owners; `M3HashPinnedTextSnapshotRecipe` for original C,
composed by `com.synexia.m3.M3IndexedHistogramNative20261021`.
Both source preimages were checked byte-for-byte before mutation.

## Actual ordered product transformation
1. `M3StateMaskHistogram.java`: retains public signatures, direct Java path
   and complete offset validation. New private indexed ABI probe plus native
   invocation removes `new byte[count]` from new-bin indexed native requests.
   Old binaries keep the old aligned **native** tally and the Java materialization.
2. `m3_state_mask_histogram.c`: adds a native capability probe and one native
   entrypoint to read original index/state planes through bounded JNI regions.
   A direct histogram uses two 256-entry int scratch planes and a 256-byte
   state page; no critical array pins, JNI global references, Java callbacks
   or native-heap copy. Output is published only after successful validation.
3. `M3StateMaskHistogramIndexedNativeContract.java`: deterministic Java21
   5,000-seed differential Java/native oracle, valid reverse/strided paths,
   duplicate physical occurrences, native raw-input refusal and output
   nonmutation on invalid arguments.
4. `M3StateMaskHistogramIndexedNativeTest.java`: owning JUnit5 adapter.

`indexedAbiNative()I` and `tallyIndexedNative([I[BIII[I)V` are additive
JNI descriptors. Existing `tallyNative([BIII[I)V` remains unchanged.
No new public Java API is added. Java-facing collection and sorted wrapper
semantics remain those of the original owner.

## Reproduce actual native test with a JDK21 checkout

Run from this repository root, in an isolated build directory:
```bash
set -eu
OUT=/tmp/m3jdk21-indexed-hist-proof
mkdir -p "$OUT/classes" "$OUT/headers" "$OUT/native"
JDK_HOME="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}"
javac -Xlint:all,-serial -Werror --release 21 -h "$OUT/headers" -d "$OUT/classes" \
  m3/collections/src/main/java/com/m3/collections/M3StateMaskHistogram.java \
  m3/collections/src/test/java/com/m3/collections/M3StateMaskHistogramIndexedNativeContract.java
cc -std=c17 -Wall -Wextra -Werror -fPIC -shared \
  -I"$JDK_HOME/include" -I"$JDK_HOME/include/linux" \
  m3/collections/src/main/native/collections/m3_state_mask_histogram.c \
  -o "$OUT/native/libm3_collections_statehist.so"
java -Xcheck:jni -Djava.library.path="$OUT/native" \
  -cp "$OUT/classes" com.m3.collections.M3StateMaskHistogramIndexedNativeContract
```

Then run the unchanged existing histogram self-test, actual owning
Maven/JUnit suite and full M3JDK21 make images/jtreg/JNI under product Java.
The example Linux include path must be adapted for Windows/macOS. The
standalone API-shaped kernel proof previously passed 5,003 checks, but
these exact owning-source commands have **not** been executed in this chat.

## Contract gates and superset joining

- Stable input arrays for each call; concurrent mutation during JNI remains
  caller-disallowed, exactly as the original owner documents.
- Indexed offsets identify physical slots, but duplicate offsets count as
  distinct logical occurrences. Preserve reverse, strides and 256 unsigned
  bit-state frequencies plus inclusion-exclusion queries.
- This is a memory/ownership refinement: sparse/random input may still
  be slower than HotSpot Java or the older aligned C path. Do not claim a
  performance win without comparative CPU, Java allocated bytes and native
  process memory measurements on representative distributions.
- Keep the sorted/filtered live wrappers, including null/comparator/equality,
  iteration and exception/serialization/concurrency contracts, unchanged.
- Existing original M3JDK21 PRs #1619, #1624, #1643, #1653 and later
  competing candidates must be resolved by Git-tree/source/hash proof, not
  wholesale replacement. One parent-owned dynamic-array/offset/mask store.
- Global String -> arrays -> collections admission order is unchanged.
  Remaining Maven/Gradle/FindBugs shared M3AST and M3File delta work stays
  under separate original-owner contract gates.

Canonical donor/category review and MOP append-only recipe memory are in
Synexia #12702. The target PR remains draft pending actual complete JDK/JNI,
OpenRewrite SDK and cross-platform gates.
