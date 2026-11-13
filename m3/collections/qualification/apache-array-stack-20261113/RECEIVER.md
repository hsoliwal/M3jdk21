<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 Apache ArrayStack source receiver — DRAFT / HOLD

The original `M3ArrayStack` 4.5.0 donor scaffold contained 48+
`UnsupportedOperationException` methods and no executable backing store.
This branch replaces that scaffold with a **complete 526-line Java21
dense-array stack in the existing `jdk.m3.collections` module**, at
`src/jdk.m3.collections/share/classes/jdk/m3/collections/apache/M3ArrayStack.java`.
The source is actual executable Java, not a wrapper or another JAR.

Source authority: [Synexia #17282](https://github.com/hsoliwal/com.synexia/pull/17282)
and the existing canonical Maven/OpenRewrite crate dataset
`m3-apache-array-stack-20261113`. No recipe code is received here.

## Exact source hash receipts

- Pinned M3JDK21 source baseline:
  `0b7bb3ab0e0e1fec6c5818428dad81ae30e86fbb`.
- All-throw scaffold original Git blob
  `3f6989ec756543f675d667c14ac1367d971276f2`,
  SHA256 `85a2b6afd218f35442114bd599b2bc14f5f4b4341719f1a6372d8e8ccdb8ffd1`.
- JDK-styled complete new Java Git blob
  `237c205c40f92d9ac135c64113fbdae1427a0ef2`,
  SHA256 `ce1b38af531d1d863f6eb67bd6e2f975a39a02979777ed5a2f0af3d1756e7e45`.
- New Java21 regression Git blob `9ba48379bddb5bd82c9903200258ef3db5296325`,
  SHA256 `0bc658e21c381ae896971c212d1d376fabf451ad1c5e528e41739ede4c7ab3d6`.
- The old package owner file was deleted to avoid a competing retained
  class, and its exact original preimage is preserved in private Synexia.

## Intrinsic storage and behavior

`Object[] elements` and `int size` directly own logical dense
positions; `[0,size)` provides complete occupancy without a redundant
persistent mask. A **temporary `long[]` rejection mask is created only on
the first rejected element** during `removeIf`, `removeAll` or
`retainAll`; only surviving contiguous payload runs are copied, and
discarded references are nulled. No per-entry Node or Entry allocations.

JDK `List`/`SequencedCollection`/`RandomAccess`, stack `push`,
`pop`, `peek`, `peek(depth)`, `search`, `empty`,
shallow clone, self-bulk-add, structural iterator fail-fast and
serialization are implemented in JDK-styled source. No third-party
Java runtime/Guava wrapper or new native JNI symbol is added.
The same `jdk.m3.collections.apache` module export is used by the
two arrayified LinkedList and TreeList classes in this PR.

## Actually tested locally

- OpenJDK 21.0.12.1 `javac --release 21 -Xlint:all -Werror`:
  **PASS** for actual complete class and regression.
- Independent `java.util.ArrayList` + Apache stack operation model:
  `PASS checks=484245 steps=60000 array-lanes=Object[]`.
  Checked stack depth exceptions, top search, index/iterator/view
  semantics, nulls, clone, serialization and stable bulk mask compaction.

## Mandatory release gates still HOLD

Real Maven/OpenRewrite SDK source rewrite/fixed point/refusal,
full M3JDK21 source-built module/Maven parent/JMOD and official
jtreg, Apache provider API/reflection/serialization compatibility,
Windows and GC/JNI/native race, and comparative JMH CPU/heap/native
process memory are NOT RUN here. The source is **committed on a
review-only public draft branch**, not merged into product master.
Do not merge before independent gate acceptance; preserve license
and attribution/NOTICE requirements.
