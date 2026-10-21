<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 mask-first sorted indexed snapshots: receiving state

**Canonical recipe:** `com.synexia.m3jdk21.PhysicalMaskSortedSnapshot20261018` in `hsoliwal/com.synexia`.  
**Product owner:** the existing `com.m3.collections.M3IndexedListSnapshot`; this is M3JDK21, not an M3 adapter over an unchanged JDK.

## What is actually received

The selected projection borrows a full-width `int[]` physical state plane, passes the existing `int[] positions` and offsets to the existing `M3IndexPathMasks.select` Java/C17 JNI kernel, and retains only the returned integer index directory. Sorting uses the existing stable indirect index sort, **after** mask rejection. There is no retained per-entry `Entry`, `Node` or `Cell`, no new object payload owner, no new native symbol, and no JNI Java-object callback.

Full-width bit 31, required/forbidden contradictions, partial `subList` index windows, null/duplicate values, selected physical identity, stable ties, immutable outputs, explicit native refusal and state-array nonretention are targeted.

## Focused reproduction (Java 21, Linux optional JNI)

```sh
set -eu
mkdir -p build/m3-physical-mask
src=m3/collections/src/main/java/com/m3/collections
proof=m3/collections/src/test/java/com/m3/collections/M3PhysicalStateSnapshotProof.java
javac --release 21 -Xlint:all -Werror -d build/m3-physical-mask \
  "$src/M3IndexMaskCompactor.java" \
  "$src/M3IndexPathMasks.java" \
  "$src/M3IndexedListSnapshot.java" "$proof"
java -cp build/m3-physical-mask com.m3.collections.M3PhysicalStateSnapshotProof
```

For actual JNI on Linux:

```sh
cc -std=c17 -Wall -Wextra -Werror -fPIC -shared \
  -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
  m3/collections/src/main/native/collections/m3_index_path_masks.c \
  -o build/m3-physical-mask/libm3_collections_pathmask.so
java -Xcheck:jni -Djava.library.path=build/m3-physical-mask \
  -cp build/m3-physical-mask \
  com.m3.collections.M3PhysicalStateSnapshotProof --native
```

A dedicated GitHub Actions workflow contains the same finite proof. The Maven JUnit entrypoint is `M3PhysicalStateSnapshotTest`; full-module Maven and `java.base`/jtreg/Image/JNI-platform gates remain separate.

## Admission boundaries

- Recipe source seals, SDK fixed point and refusal are the **Synexia** owner's obligations.
- Target Java21 compilation and randomized JDK-oracle/JNI differential tests are separate runtime obligations.
- JNI optional availability is not native performance evidence. Heap, process RSS and CPU measurements remain pending.
- Array and collection global receiving do not advance ahead of the active String stage.
- OpenJDK GPLv2/classpath-exception files are unchanged; these independently licensed `m3/collections` sources remain Apache-2.0.
