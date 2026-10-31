<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Portable primitive collection source coverage

[Contract reference](portable-primitive-collection-reference.md) · [Guide](README.md) · [Package map](m3-packages.md#package-11) · [Coverage](coverage.md)

Source review date: **15 October 2026**. This ledger pins the complete six-file production package
`m3/ports/collections/src/main/java/com/m3/util` at
`c842fab8cf5b799257e3c9ea07f8baddde80db09`. The five public types have 97 explicitly
declared public constructors/methods; `M3DenseHash` is package-private. The count
excludes enum constants, generated/inherited enum methods and non-public helpers.
All six bodies, their state/helper members, the one package test body and the
build/provenance files below were inspected. No repository code was executed.

**Builds, examples, tests, scripts, verifiers, recipes, benchmarks and native/runtime
qualification: NOT_RUN in this documentation pass.** Source hashes identify
reviewed bytes; they are not semantic equivalence or test-pass evidence. Hosted CI
is separate exact-commit evidence. Broader package/repository claims, inline
package documentation, exhaustive caller mapping and target promotion remain open.

## Six-source closure and hashes

All paths in this table are relative to the physical package above. The package
has no non-M3 Java owner or `package-info.java`. The complete receiver source tree
has no `module-info.java`. Its sibling `com.m3.ds.M3LongFenwickTree` is already
routed by [indexed algorithms](indexed-algorithm-reference.md) and is not included
in these six files or 97 callables. The separate `m3/collections` package and its
module descriptor are outside this ledger.

| Source | Lines | Target Git blob | Contract route |
| --- | ---: | --- | --- |
| [M3DenseHash.java](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3DenseHash.java) | 48 | `6fc4b0aedb977799b1e751416b6eb51c2fa977b6` | [package-private-hashing-support](portable-primitive-collection-reference.md#package-private-hashing-support) |
| [M3IntArrayDeque.java](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3IntArrayDeque.java) | 245 | `da09cc3ec10e828a8dcd686e0bf1c1683c97597a` | [specialized-int-deque](portable-primitive-collection-reference.md#specialized-int-deque) |
| [M3PrimitiveArrayDeque.java](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java) | 338 | `cf65c2fe522d762e3bf47dee3886b16d1da76458` | [typed-ring-deque](portable-primitive-collection-reference.md#typed-ring-deque) |
| [M3PrimitiveArrayList.java](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java) | 349 | `e00bf6456ffb9c755378545ad31c85446d982246` | [indexed-list](portable-primitive-collection-reference.md#indexed-list) |
| [M3PrimitiveKind.java](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveKind.java) | 325 | `d64394fc8f38b08e0151a624955caa0ba69bcda1` | [carrier-model-and-shared-storage](portable-primitive-collection-reference.md#carrier-model-and-shared-storage) |
| [M3PrimitiveMinMaxQueue.java](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveMinMaxQueue.java) | 334 | `42db8d5c109b30389a022a0acc3cd82f39ebd3fe` | [sorted-minmax-queue](portable-primitive-collection-reference.md#sorted-minmax-queue) |

### Target SHA-256 byte digests

These are target file bytes, not the donor `Source-Git-blob` header strings.

| Source | SHA-256 |
| --- | --- |
| `M3DenseHash.java` | `b887113334654c253f5ef06e5d463f294e0d6db81da82531c961fc82b49948f4` |
| `M3IntArrayDeque.java` | `04297f8e84f3f4f915d81021cbfb7e40b4e039558aa85025bf2bcb88790b5f10` |
| `M3PrimitiveArrayDeque.java` | `640dab2cd43ba06a8a39a1fee18b95e597ed6cf0bec8efa16d974ad533819366` |
| `M3PrimitiveArrayList.java` | `9611e10a1d20c4f6e348bcfb2fe3ce9bc67fe1cda9d4c6b19b0c54fbfa522daa` |
| `M3PrimitiveKind.java` | `a7ee642a4286ff3d93d6d94e719a7cb34f4ecd1955bf7e699d61402763fdda0b` |
| `M3PrimitiveMinMaxQueue.java` | `14ff23f18990427d92af83282199cf82e460e60f64a47b704402629cf602ef66` |

## Complete explicit public member routes

Every row links its declaration at the pinned target revision. The linked owner
section supplies the behavior together with the shared carrier/capacity rules;
rows are declaration coverage, not an executed test matrix.

### M3DenseHash

No public constructor or method. Its private constructor, two constants and
four package-private callable bodies are documented under [hashing support](portable-primitive-collection-reference.md#package-private-hashing-support).

### M3IntArrayDeque

| Explicit declaration | Contract route |
| --- | --- |
| [`public M3IntArrayDeque()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3IntArrayDeque.java#L33) | [M3IntArrayDeque](portable-primitive-collection-reference.md#specialized-int-deque) |
| [`public M3IntArrayDeque(int expected)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3IntArrayDeque.java#L41) | [M3IntArrayDeque](portable-primitive-collection-reference.md#specialized-int-deque) |
| [`public int size()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3IntArrayDeque.java#L48) | [M3IntArrayDeque](portable-primitive-collection-reference.md#specialized-int-deque) |
| [`public boolean isEmpty()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3IntArrayDeque.java#L53) | [M3IntArrayDeque](portable-primitive-collection-reference.md#specialized-int-deque) |
| [`public int capacity()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3IntArrayDeque.java#L58) | [M3IntArrayDeque](portable-primitive-collection-reference.md#specialized-int-deque) |
| [`public long payloadBytes()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3IntArrayDeque.java#L63) | [M3IntArrayDeque](portable-primitive-collection-reference.md#specialized-int-deque) |
| [`public void addFirst(int value)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3IntArrayDeque.java#L70) | [M3IntArrayDeque](portable-primitive-collection-reference.md#specialized-int-deque) |
| [`public void addLast(int value)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3IntArrayDeque.java#L82) | [M3IntArrayDeque](portable-primitive-collection-reference.md#specialized-int-deque) |
| [`public int peekFirst()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3IntArrayDeque.java#L93) | [M3IntArrayDeque](portable-primitive-collection-reference.md#specialized-int-deque) |
| [`public int peekLast()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3IntArrayDeque.java#L100) | [M3IntArrayDeque](portable-primitive-collection-reference.md#specialized-int-deque) |
| [`public int removeFirst()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3IntArrayDeque.java#L106) | [M3IntArrayDeque](portable-primitive-collection-reference.md#specialized-int-deque) |
| [`public int removeLast()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3IntArrayDeque.java#L119) | [M3IntArrayDeque](portable-primitive-collection-reference.md#specialized-int-deque) |
| [`public void clear()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3IntArrayDeque.java#L130) | [M3IntArrayDeque](portable-primitive-collection-reference.md#specialized-int-deque) |
| [`public void forEach(IntConsumer action)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3IntArrayDeque.java#L143) | [M3IntArrayDeque](portable-primitive-collection-reference.md#specialized-int-deque) |
| [`public int getFirst()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3IntArrayDeque.java#L152) | [M3IntArrayDeque](portable-primitive-collection-reference.md#specialized-int-deque) |
| [`public int getLast()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3IntArrayDeque.java#L158) | [M3IntArrayDeque](portable-primitive-collection-reference.md#specialized-int-deque) |
| [`public int element()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3IntArrayDeque.java#L164) | [M3IntArrayDeque](portable-primitive-collection-reference.md#specialized-int-deque) |
| [`public int remove()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3IntArrayDeque.java#L170) | [M3IntArrayDeque](portable-primitive-collection-reference.md#specialized-int-deque) |
| [`public void push(int value)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3IntArrayDeque.java#L177) | [M3IntArrayDeque](portable-primitive-collection-reference.md#specialized-int-deque) |
| [`public int pop()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3IntArrayDeque.java#L183) | [M3IntArrayDeque](portable-primitive-collection-reference.md#specialized-int-deque) |
| [`public int addAll(int[] source, int from, int length)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3IntArrayDeque.java#L196) | [M3IntArrayDeque](portable-primitive-collection-reference.md#specialized-int-deque) |
| [`public int[] toArray()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3IntArrayDeque.java#L214) | [M3IntArrayDeque](portable-primitive-collection-reference.md#specialized-int-deque) |

### M3PrimitiveArrayDeque

| Explicit declaration | Contract route |
| --- | --- |
| [`public M3PrimitiveArrayDeque(M3PrimitiveKind kind)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java#L37) | [M3PrimitiveArrayDeque](portable-primitive-collection-reference.md#typed-ring-deque) |
| [`public M3PrimitiveArrayDeque(M3PrimitiveKind kind, int initialCapacity)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java#L49) | [M3PrimitiveArrayDeque](portable-primitive-collection-reference.md#typed-ring-deque) |
| [`public M3PrimitiveKind kind()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java#L59) | [M3PrimitiveArrayDeque](portable-primitive-collection-reference.md#typed-ring-deque) |
| [`public int size()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java#L64) | [M3PrimitiveArrayDeque](portable-primitive-collection-reference.md#typed-ring-deque) |
| [`public boolean isEmpty()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java#L69) | [M3PrimitiveArrayDeque](portable-primitive-collection-reference.md#typed-ring-deque) |
| [`public int capacity()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java#L74) | [M3PrimitiveArrayDeque](portable-primitive-collection-reference.md#typed-ring-deque) |
| [`public long payloadBytes()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java#L79) | [M3PrimitiveArrayDeque](portable-primitive-collection-reference.md#typed-ring-deque) |
| [`public void addFirstBits(long bits)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java#L86) | [M3PrimitiveArrayDeque](portable-primitive-collection-reference.md#typed-ring-deque) |
| [`public void addLastBits(long bits)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java#L98) | [M3PrimitiveArrayDeque](portable-primitive-collection-reference.md#typed-ring-deque) |
| [`public long peekFirstBits()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java#L111) | [M3PrimitiveArrayDeque](portable-primitive-collection-reference.md#typed-ring-deque) |
| [`public long peekLastBits()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java#L122) | [M3PrimitiveArrayDeque](portable-primitive-collection-reference.md#typed-ring-deque) |
| [`public long removeFirstBits()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java#L132) | [M3PrimitiveArrayDeque](portable-primitive-collection-reference.md#typed-ring-deque) |
| [`public long removeLastBits()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java#L148) | [M3PrimitiveArrayDeque](portable-primitive-collection-reference.md#typed-ring-deque) |
| [`public long getBits(int index)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java#L164) | [M3PrimitiveArrayDeque](portable-primitive-collection-reference.md#typed-ring-deque) |
| [`public void clear()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java#L172) | [M3PrimitiveArrayDeque](portable-primitive-collection-reference.md#typed-ring-deque) |
| [`public Object toPrimitiveArray()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java#L182) | [M3PrimitiveArrayDeque](portable-primitive-collection-reference.md#typed-ring-deque) |
| [`public int addAllBits(Object source, int from, int length)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java#L204) | [M3PrimitiveArrayDeque](portable-primitive-collection-reference.md#typed-ring-deque) |
| [`public long getFirstBits()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java#L228) | [M3PrimitiveArrayDeque](portable-primitive-collection-reference.md#typed-ring-deque) |
| [`public long getLastBits()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java#L234) | [M3PrimitiveArrayDeque](portable-primitive-collection-reference.md#typed-ring-deque) |
| [`public long elementBits()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java#L240) | [M3PrimitiveArrayDeque](portable-primitive-collection-reference.md#typed-ring-deque) |
| [`public void pushBits(long bits)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java#L246) | [M3PrimitiveArrayDeque](portable-primitive-collection-reference.md#typed-ring-deque) |
| [`public long popBits()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java#L252) | [M3PrimitiveArrayDeque](portable-primitive-collection-reference.md#typed-ring-deque) |
| [`public boolean containsBits(long bits)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java#L258) | [M3PrimitiveArrayDeque](portable-primitive-collection-reference.md#typed-ring-deque) |
| [`public boolean removeFirstOccurrenceBits(long bits)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java#L267) | [M3PrimitiveArrayDeque](portable-primitive-collection-reference.md#typed-ring-deque) |
| [`public boolean removeLastOccurrenceBits(long bits)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java#L278) | [M3PrimitiveArrayDeque](portable-primitive-collection-reference.md#typed-ring-deque) |
| [`public void forEachBits(java.util.function.LongConsumer action)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayDeque.java#L290) | [M3PrimitiveArrayDeque](portable-primitive-collection-reference.md#typed-ring-deque) |

### M3PrimitiveArrayList

| Explicit declaration | Contract route |
| --- | --- |
| [`public M3PrimitiveArrayList(M3PrimitiveKind kind)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java#L36) | [M3PrimitiveArrayList](portable-primitive-collection-reference.md#indexed-list) |
| [`public M3PrimitiveArrayList(M3PrimitiveKind kind, int initialCapacity)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java#L48) | [M3PrimitiveArrayList](portable-primitive-collection-reference.md#indexed-list) |
| [`public M3PrimitiveKind kind()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java#L58) | [M3PrimitiveArrayList](portable-primitive-collection-reference.md#indexed-list) |
| [`public int size()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java#L63) | [M3PrimitiveArrayList](portable-primitive-collection-reference.md#indexed-list) |
| [`public boolean isEmpty()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java#L68) | [M3PrimitiveArrayList](portable-primitive-collection-reference.md#indexed-list) |
| [`public int capacity()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java#L73) | [M3PrimitiveArrayList](portable-primitive-collection-reference.md#indexed-list) |
| [`public long payloadBytes()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java#L78) | [M3PrimitiveArrayList](portable-primitive-collection-reference.md#indexed-list) |
| [`public void addBits(long bits)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java#L85) | [M3PrimitiveArrayList](portable-primitive-collection-reference.md#indexed-list) |
| [`public void addBits(int index, long bits)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java#L97) | [M3PrimitiveArrayList](portable-primitive-collection-reference.md#indexed-list) |
| [`public long getBits(int index)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java#L111) | [M3PrimitiveArrayList](portable-primitive-collection-reference.md#indexed-list) |
| [`public long setBits(int index, long bits)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java#L122) | [M3PrimitiveArrayList](portable-primitive-collection-reference.md#indexed-list) |
| [`public long removeAtBits(int index)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java#L135) | [M3PrimitiveArrayList](portable-primitive-collection-reference.md#indexed-list) |
| [`public boolean removeBits(long bits)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java#L150) | [M3PrimitiveArrayList](portable-primitive-collection-reference.md#indexed-list) |
| [`public int indexOfBits(long bits)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java#L162) | [M3PrimitiveArrayList](portable-primitive-collection-reference.md#indexed-list) |
| [`public void sort()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java#L172) | [M3PrimitiveArrayList](portable-primitive-collection-reference.md#indexed-list) |
| [`public int binarySearchBits(long bits)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java#L181) | [M3PrimitiveArrayList](portable-primitive-collection-reference.md#indexed-list) |
| [`public void clear()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java#L188) | [M3PrimitiveArrayList](portable-primitive-collection-reference.md#indexed-list) |
| [`public void ensureCapacity(int required)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java#L199) | [M3PrimitiveArrayList](portable-primitive-collection-reference.md#indexed-list) |
| [`public void trimToSize()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java#L207) | [M3PrimitiveArrayList](portable-primitive-collection-reference.md#indexed-list) |
| [`public Object toPrimitiveArray()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java#L216) | [M3PrimitiveArrayList](portable-primitive-collection-reference.md#indexed-list) |
| [`public void forEachBits(LongConsumer action)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java#L226) | [M3PrimitiveArrayList](portable-primitive-collection-reference.md#indexed-list) |
| [`public int addAllBits(Object source, int from, int length)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java#L243) | [M3PrimitiveArrayList](portable-primitive-collection-reference.md#indexed-list) |
| [`public int addAllBits(int index, Object source, int from, int length)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java#L261) | [M3PrimitiveArrayList](portable-primitive-collection-reference.md#indexed-list) |
| [`public void removeRange(int fromIndex, int toIndex)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java#L285) | [M3PrimitiveArrayList](portable-primitive-collection-reference.md#indexed-list) |
| [`public boolean removeIfBits(java.util.function.LongPredicate filter)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveArrayList.java#L306) | [M3PrimitiveArrayList](portable-primitive-collection-reference.md#indexed-list) |

### M3PrimitiveKind

| Explicit declaration | Contract route |
| --- | --- |
| [`public int widthBytes()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveKind.java#L52) | [M3PrimitiveKind](portable-primitive-collection-reference.md#carrier-model-and-shared-storage) |
| [`public long payloadBytes(int capacity)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveKind.java#L63) | [M3PrimitiveKind](portable-primitive-collection-reference.md#carrier-model-and-shared-storage) |

The eight constants, compiler-created `values`/`valueOf`, inherited enum distinction
and all package-private conversion, array, capacity and ring helpers are covered
in the [carrier section](portable-primitive-collection-reference.md#carrier-model-and-shared-storage).

### M3PrimitiveMinMaxQueue

| Explicit declaration | Contract route |
| --- | --- |
| [`public M3PrimitiveMinMaxQueue(M3PrimitiveKind kind)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveMinMaxQueue.java#L39) | [M3PrimitiveMinMaxQueue](portable-primitive-collection-reference.md#sorted-minmax-queue) |
| [`public M3PrimitiveMinMaxQueue(M3PrimitiveKind kind, int initialCapacity)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveMinMaxQueue.java#L49) | [M3PrimitiveMinMaxQueue](portable-primitive-collection-reference.md#sorted-minmax-queue) |
| [`public M3PrimitiveKind kind()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveMinMaxQueue.java#L59) | [M3PrimitiveMinMaxQueue](portable-primitive-collection-reference.md#sorted-minmax-queue) |
| [`public int size()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveMinMaxQueue.java#L64) | [M3PrimitiveMinMaxQueue](portable-primitive-collection-reference.md#sorted-minmax-queue) |
| [`public boolean isEmpty()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveMinMaxQueue.java#L69) | [M3PrimitiveMinMaxQueue](portable-primitive-collection-reference.md#sorted-minmax-queue) |
| [`public int capacity()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveMinMaxQueue.java#L74) | [M3PrimitiveMinMaxQueue](portable-primitive-collection-reference.md#sorted-minmax-queue) |
| [`public long payloadBytes()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveMinMaxQueue.java#L79) | [M3PrimitiveMinMaxQueue](portable-primitive-collection-reference.md#sorted-minmax-queue) |
| [`public void offerBits(long bits)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveMinMaxQueue.java#L86) | [M3PrimitiveMinMaxQueue](portable-primitive-collection-reference.md#sorted-minmax-queue) |
| [`public int offerAllBits(Object source, int from, int length)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveMinMaxQueue.java#L114) | [M3PrimitiveMinMaxQueue](portable-primitive-collection-reference.md#sorted-minmax-queue) |
| [`public int offerAllSortedBits(Object source, int from, int length)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveMinMaxQueue.java#L150) | [M3PrimitiveMinMaxQueue](portable-primitive-collection-reference.md#sorted-minmax-queue) |
| [`public long peekMinBits()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveMinMaxQueue.java#L175) | [M3PrimitiveMinMaxQueue](portable-primitive-collection-reference.md#sorted-minmax-queue) |
| [`public long peekMaxBits()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveMinMaxQueue.java#L182) | [M3PrimitiveMinMaxQueue](portable-primitive-collection-reference.md#sorted-minmax-queue) |
| [`public long getMinBits()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveMinMaxQueue.java#L188) | [M3PrimitiveMinMaxQueue](portable-primitive-collection-reference.md#sorted-minmax-queue) |
| [`public long getMaxBits()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveMinMaxQueue.java#L194) | [M3PrimitiveMinMaxQueue](portable-primitive-collection-reference.md#sorted-minmax-queue) |
| [`public long pollMinBits()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveMinMaxQueue.java#L202) | [M3PrimitiveMinMaxQueue](portable-primitive-collection-reference.md#sorted-minmax-queue) |
| [`public long pollMaxBits()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveMinMaxQueue.java#L216) | [M3PrimitiveMinMaxQueue](portable-primitive-collection-reference.md#sorted-minmax-queue) |
| [`public long removeMinBits()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveMinMaxQueue.java#L229) | [M3PrimitiveMinMaxQueue](portable-primitive-collection-reference.md#sorted-minmax-queue) |
| [`public long removeMaxBits()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveMinMaxQueue.java#L235) | [M3PrimitiveMinMaxQueue](portable-primitive-collection-reference.md#sorted-minmax-queue) |
| [`public boolean removeOneBits(long bits)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveMinMaxQueue.java#L242) | [M3PrimitiveMinMaxQueue](portable-primitive-collection-reference.md#sorted-minmax-queue) |
| [`public int countBits(long bits)`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveMinMaxQueue.java#L265) | [M3PrimitiveMinMaxQueue](portable-primitive-collection-reference.md#sorted-minmax-queue) |
| [`public void clear()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveMinMaxQueue.java#L268) | [M3PrimitiveMinMaxQueue](portable-primitive-collection-reference.md#sorted-minmax-queue) |
| [`public Object toPrimitiveArray()`](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/main/java/com/m3/util/M3PrimitiveMinMaxQueue.java#L274) | [M3PrimitiveMinMaxQueue](portable-primitive-collection-reference.md#sorted-minmax-queue) |

## Implementation-member closure

| Source | State and non-public callable coverage |
| --- | --- |
| `M3DenseHash.java` | State/constants: `DEFAULT_LOAD_FACTOR = 0.65f`; `MIN_CAPACITY = 8`. Callables: `private M3DenseHash()`; `static int capacityFor(int expectedSize, float loadFactor)`; `static int threshold(int capacity, float loadFactor)`; `static int mix(int value)`; `static int mix(long value)` |
| `M3IntArrayDeque.java` | State/constants: `private int[] elements`; `private int head`; `private int size`. Callables: `private void require()`; `private void ensure()`; `private void ensure(int required)`; `private void copyInto(int[] target, int offset)`; `static int initialCapacity(int expected)`; `static int requiredCapacity(int size, int extra)` |
| `M3PrimitiveArrayDeque.java` | State/constants: `private final M3PrimitiveKind kind`; `private Object elements`; `private int head`; `private int size`. Callables: `private void removeAt(int index)`; `Object backingArray()`; `private int physicalIndex(int logicalIndex)`; `private void ensure()`; `private void ensure(int required)`; `private static int inc(int index, int capacity)`; `private static int dec(int index, int capacity)` |
| `M3PrimitiveArrayList.java` | State/constants: `private final M3PrimitiveKind kind`; `private Object elements`; `private int size`. Callables: `Object backingArray()`; `private int check(int index)`; `private void ensure(int required)` |
| `M3PrimitiveKind.java` | State/constants: `BOOLEAN`; `BYTE`; `SHORT`; `CHAR`; `INT`; `LONG`; `FLOAT`; `DOUBLE`; `private final int widthBytes`. Callables: `M3PrimitiveKind(int widthBytes)`; `Object newArray(int length)`; `Object copyOf(Object array, int newLength)`; `void clear(Object array, int from, int to)`; `long readBits(Object array, int index)`; `void writeBits(Object array, int index, long bits)`; `long normalizeBits(long bits)`; `long storageBits(long bits)`; `int compareBits(long left, long right)`; `boolean equalBits(long left, long right)`; `int hashBits(long bits)`; `void sort(Object array, int from, int to)`; `int binarySearch(Object array, int from, int to, long bits)`; `void checkArray(Object array)`; `static int checkedSize(int size, int extra)`; `static int grownCapacity(int current, int required)`; `static int ringIndex(int head, int logical, int capacity)`; `static void copyRing(Object source, int head, int length, int capacity, Object target, int offset)`; `static void moveRing(Object array, int head, int capacity, int source, int target, int length)`; `int checkRange(Object array, int from, int length)`; `void copyStorage(Object source, int from, Object target, int offset, int length)`; `void clearRing(Object array, int head, int length, int capacity)` |
| `M3PrimitiveMinMaxQueue.java` | State/constants: `private final M3PrimitiveKind kind`; `private Object values`; `private int size`; `private int head`. Callables: `private int physical(int logical)`; `private int lowerBound(long bits)`; `private int upperBound(long bits)`; `private int bound(long bits, boolean upper)`; `private boolean sorted(Object array, int length)`; `private boolean sortedRange(Object array, int from, int length)`; `private void merge(Object incoming, int length, int required)`; `private void ensure(int required)`; `private static void checkLegacyRange(Object source, int from, int length)`; `private void requireNotEmpty()` |

These bodies determine conversion, validation order, wrap/overlap, growth,
compaction and merge behavior in the contract reference. None is promoted to
public API by this table. Public objects inherit ordinary `Object` behavior;
no undeclared collection interface, iterator or serialization protocol is implied.

## Inspected test callers

The following five methods are all `@Test` methods in the complete
`M3PrimitiveCollectionFacadesTest.java` body. Their actual assertions and missing
cases are separated in the [test matrix](portable-primitive-collection-reference.md#five-actual-package-tests).

| Method | Evidence state |
| --- | --- |
| [primitiveKindsPreserveLaneWidths](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/test/java/com/m3/util/M3PrimitiveCollectionFacadesTest.java#L21) | Source read; NOT_RUN |
| [primitiveArrayListPreservesExactWidthOwnerContract](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/test/java/com/m3/util/M3PrimitiveCollectionFacadesTest.java#L34) | Source read; NOT_RUN |
| [primitiveArrayDequePreservesLogicalOrderAndThrowingEmptyContract](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/test/java/com/m3/util/M3PrimitiveCollectionFacadesTest.java#L59) | Source read; NOT_RUN |
| [intArrayDequePreservesPrimitiveOwnerContract](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/test/java/com/m3/util/M3PrimitiveCollectionFacadesTest.java#L81) | Source read; NOT_RUN |
| [primitiveMinMaxQueuePreservesSortedExtremaContract](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/test/java/com/m3/util/M3PrimitiveCollectionFacadesTest.java#L99) | Source read; NOT_RUN |

These are bounded known callers, not a production call-graph completion claim.
The internal calls from list/deque/queue to the kind/ring helpers, from kind
hashing to `M3DenseHash`, and the absence of hashing use by those four containers
were inspected. No all-lane, randomized, concurrency, huge-capacity or
allocation-failure result is inferred from the small INT happy-path suite.

## Inspected build and provenance sources

| Public source | Target Git blob | Evidence purpose |
| --- | --- | --- |
| [m3/ports/collections/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/pom.xml) | `6b21183312160fe2c623576482a53ba3ed262d6d` | Standalone artifact, Java 21, exact source-suffixed JUnit dependency, compiler/Surefire and opt-in recipe profiles |
| [m3/ports/collections/README.md](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/README.md) | `afc678065621cd5a2fcc6bdb166af42dc1bb3258` | Receiver scope, seven declared owners and canonical ownership |
| [m3/ports/collections/PROVENANCE.tsv](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/PROVENANCE.tsv) | `31beb76f48caff0decf0137f4b65311ef13ab166` | Published donor pin/path/blob/license/renaming declarations, including test lineage |
| [m3/ports/collections/DONOR_REVIEW.tsv](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/DONOR_REVIEW.tsv) | `e0a53286b66b5d881a0f91ab6f8547b721810fa5` | Copy/adapt versus evidence-only/taxonomy boundaries |
| [m3/ports/collections/recipe.json](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/recipe.json) | `6d50ab910eef9a835c322d74ca833215cafd60be` | Isolated receiver mode and no runtime/public-API promotion flags |
| [m3/ports/collections/verify_collection_port_wave1.py](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/verify_collection_port_wave1.py) | `983e1e8427b67d148c78022be2d5357ee654577b` | Read-only interpretation of verifier checks; script NOT_RUN |
| [m3/ports/collections/src/test/java/com/m3/util/M3PrimitiveCollectionFacadesTest.java](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/collections/src/test/java/com/m3/util/M3PrimitiveCollectionFacadesTest.java) | `27674e541bd4605faa355c93a26d214a0fc5ec5d` | Complete five-test caller/assertion body, NOT_RUN |
| [.github/workflows/m3-collection-port-wave1.yml](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/.github/workflows/m3-collection-port-wave1.yml) | `16ba3cc7abdc629969786a5c9410e76963e58d9d` | Path selection and declared gate/test commands, NOT_RUN |
| [m3/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/pom.xml) | `693156006961717eccfc649585e724d67682b3df` | Control-reactor module/profile selection; no ports/collections declaration |

Public target metadata declares donor `8513abf8eb0d911873c9127a138007a42dd4ab60`;
its source-blob header values are provenance assertions distinct from this
ledger's target blobs. The verifier checks declarations/header text, not complete
body equivalence or runtime acceptance. Source identity, declared recipe custody,
executed qualification and later receiving admission are separate evidence layers.

The original [package sample](m3-packages.md#package-11) and [physical-package
inventory](packages/m3-01.md) remain historical sampled routing. This continuation
adds complete source-level contracts for these six files without rewriting those
baselines, their denominators, sealed recipe payloads or existing acceptance status.
