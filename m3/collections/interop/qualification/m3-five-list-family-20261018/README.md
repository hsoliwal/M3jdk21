<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Five-library indexed List/Multimap family: implementation and proof

## Exact code authority and parent

Canonical convergence recipe: hsoliwal/com.synexia,
`com.synexia.m3.Jdk21FiveListFamily20261018`.
Public M3JDK21 receiver is a **separate draft PR on current master**.
This handoff preserves existing `M3LinkedHashMap`, `M3LinkedList`,
`M3IndexPathMasks`, `M3EclipseListMultimap`,
`M3SpringArrayMultiValueMap`, `M3ApacheListValuedMap` and all prior
source owners. No earlier API or factory implementation is overwritten.
All six targets were confirmed ABSENT-before on the selected recipient
master. No replacement parser, other transformation engine, donor package
renaming or JNI symbol is introduced.

## Actual Java21 implementation, not only naming

`M3GuavaListMultimap<K,V>` implements actual Guava
`com.google.common.collect.ListMultimap<K,V>`. It owns one existing
M3 hash/encounter-order key directory and M3 linked-array value list
per key. The implementation supplies the full declared Multimap and
ListMultimap method set: live get, remove/put/putAll/replaceValues,
removeAll, entry/key/values/key-multiset/asMap views, pair-count size
and equality/hash, list sublists and mutation-aware iterators.
Duplicates, null keys/values and lazy absent-key views are supported.
No retained per-value Entry or Node. The group iterator is sequential
O(pairs), with an O(distinct keys) temporary key directory snapshot;
`M3LinkedList` arbitrary indexed element access remains O(k), not O(1).

`M3FiveListFamilies` exposes independently constructed, *real*
JDK Map-of-Lists, Guava ListMultimap, Eclipse MutableListMultimap,
Spring MultiValueMap and Apache Commons ListValuedMap interfaces,
each mapped to existing M3 owners. No generic interface is falsely
treated as semantically interchangeable with another. The new
`Snapshot` uses the existing Java/C17 JNI 32-bit indexed selector and
materializes independent typed results for all five without taking
Java object references, comparators, predicates or callbacks native.
Existing Guava/Spring/Apache/Eclipse vendor artifacts remain optional
build dependencies, not a required M3 core runtime dependency.

## Exact source seals

`M3GuavaListMultimap.java` SHA-256:
`1e8e1e58e4d1cd3f18574914ec371dcc847a8fb4ec47c55b31923accb178b63f`.
`M3FiveListFamilies.java` SHA-256:
`25f4051dcff340507c0e662413104b74a3bbb295bbe6bc78250f1ec7ea92a236`.
Four test postimages, all ABSENT-before, are pinned by
`hash-pinned-java/m3jdk21-five-list-family-20261018/manifest.tsv`.

## Actually executed on local Java21 vs still required

- Local `javac --release 21 -proc:none -Xlint:all,-serial,-processing -Werror`:
  **PASS** for all six Java production/test units using **test-only
  Guava/Eclipse/Spring/Apache and M3 Java signature stand-ins**.
- Standalone 6,400-operation JDK oracle:
  **PASS 26,421 assertions** on the full Guava view/mutation surface.
  Five-provider independent projection smoke: **PASS 56 assertions**.
- These stand-ins do NOT prove authentic library JAR compatibility or
  actual M3 key/link implementation behavior. They are not committed
  to either repo and no stub success is called provider parity.
- Real Guava `ArrayListMultimap` 6,000-step seeded differential JUnit,
  direct Spring/Eclipse/Apache typed JUnit, full M3 owning-module
  Maven/old tests, full C17 `-Xcheck:jni` selection, actual
  OpenRewrite LST fixed-point/drift/occupied-file refusal and
  source hash checks are **AUTHORED, NOT PROVEN PASSED** until actual
  exact-head GitHub jobs report success.
- Full five upstream libraries comprise many unrelated classes,
  algorithms and factories. This PR implements the **complete selected
  List-valued family API in Guava and typed five-provider composition**,
  NOT every public API of every class, all concurrent/immutable/weak/
  primitive/serialization families, JDK image/jtreg/JCK/GC/JMM or
  benchmarked superiority. Those remain in catalogue and stay HOLD.

## Donors and licensing

Source/body original Apache-2.0, 2026 Hitesh Soliwal and contributors.
Guava ListMultimap/Multimap interface references pinned to
google/guava commit 5e82f2f528fbbd970e00748025b48e3eb42ed702,
under Apache-2.0. Eclipse upstream original EPL/EDL, Commons/Spring
Apache-2.0 and OpenJDK GPL-2.0-with-Classpath-Exception ownership
boundaries are unchanged. No upstream proprietary/challenge source is
copied; LeetCode/HackerRank/GeeksforGeeks are categorization and
adversarial-shape references only.

Source passes remain serial per FILE, proof-gated promotion to package/
module/whole-repository. Never rebase Synexia develop or manually
patch generated M3 product sources. Correct recipe/templates first,
re-execute, then receive exact source bytes.
