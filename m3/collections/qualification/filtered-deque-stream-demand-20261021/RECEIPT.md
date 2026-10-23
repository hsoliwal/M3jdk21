<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Demand-driven filtered Deque receiving receipt (2026-10-21)

M3JDK21 is the integrated Java/HotSpot/JNI SDK and this changes the existing `com.m3.collections` owner only; it is not an optional M3+JDK layer. The original Deque and its M3 arrays/slots own all payload. Existing `M3IndexMaskCompactor` and JNI handle precomputed byte-state selection; this stream change never calls Java predicates from JNI, adds no native symbols and creates no second element graph.

## Contract-bearing atom
`M3FilteredDeques.View.spliterator()` inherits `AbstractCollection` collection-sized iteration and scans every predicate to obtain an exact size before first `Stream.findFirst` delivery. The substitute uses the existing `M3SlotSpliterators` implementation family for an unknown-size, ordered, lazy iterator supplier. `ReadOnly` delegates that metadata and traversal to the selected owner rather than remeasuring. Nulls, duplicates, encounter order, reverse traversal, thrown exception identity and parent-backed mutation semantics are unchanged. `SIZED`/`SUBSIZED` are **optional** characteristics, deliberately not reported by this live predicate stream. `trySplit` is unsplittable in this bounded slice; parallel throughput needs separate benchmark admission.

## Narrow proof
A separate Java21 prototype on 100000 rows showed the preexisting collection-sized first-match demand evaluates a predicate 100001 times versus one evaluation for the late-bound supplier. This is **isolated reproducible algorithm evidence**, not proof that the complete Maven module or installed M3JDK21 image has passed.

The new standalone `M3FilteredDequeStreamContract` and JUnit `M3FilteredDequeStreamTest` verify late binding, stream short-circuit, read-only delegation, null and duplicate order, reversed views, consumer exception identity, randomized comparison and original `M3LinkedList` owner continuity.

## Exact sealed receiving inputs
Target master: `7f657d6ca05760af7bee7d4d2d9e14ae91ddd942`.
Canonical Synexia develop at preparation: `408373f4101fdca2fb3625bec8af1234e5fd3e26`.

- `m3/collections/src/main/java/com/m3/collections/M3SlotSpliterators.java` 9257ce0c9c95e3afef20624631ba048aa3d60ef53a1189cd2bbabc07da2bba2f -> f8140cd6cf0cd7c05791be1d858381bd750d865e377ca9201e444bd771fd0238
- `m3/collections/src/main/java/com/m3/collections/M3FilteredDeques.java` b6aec5a921f9eaba6feb14461bf6dca7e67cee26bb84fee3ee3143f873df5121 -> 4a41e6e4135f2a9c6ce5fc216ed249d9e43b75909c73f076637da0bbe0946be4
- `m3/collections/src/test/java/com/m3/collections/M3FilteredDequeStreamContract.java` ABSENT -> c258c1d1a32190ca0c9f8d751bd03cc4919a5bb23b42d1c3bdd8db28f9fdddbb
- `m3/collections/src/test/java/com/m3/collections/M3FilteredDequeStreamTest.java` ABSENT -> 4733eef17ec65be39137340e81ccffa6db5a856bcf691ab6292e71bade679727
- `m3/collections/algorithm-catalog/filtered-deque-stream-demand-20261021.tsv` ABSENT -> c0dea43359f91cf7686acde1bc36c43dbb7a6a5d5419bc60ff6212e0829df86a

Canonical source recipe: `hsoliwal/com.synexia`, existing `synexia-openrewrite-recipes/recipe-crates/m3-ast-build-reverse-20261017` Maven/OpenRewrite crate, dataset `filtered-deque-lazy-20261021`. The product candidate is a byte-identical sealed postimage, **not** evidence that OpenRewrite executed it.

## Admission
- Java21 isolated synthetic `javac -Xlint:all -Werror` demand proof: **PASS**.
- Exact preimage/postimage preparation: **SEALED; recipe must replay**.
- OpenRewrite SDK results / fixed point / refusal: **PENDING**.
- Actual `mvn -f m3/collections/pom.xml verify` / JUnit / JNI/native parity: **PENDING**.
- Full five-library compatibility, linked/installed image, broader regressions, CPU/heap/JNI benchmarks, exact-head CI: **PENDING**.
- Do not promote this draft until those gates run and real receipts are recorded.

## Donor decisions
The machine-searchable catalogue `m3/collections/algorithm-catalog/filtered-deque-stream-demand-20261021.tsv` contains JDK, original M3, LeetCode, HackerRank, GeeksforGeeks and JNI choices with complexity, rights and rejection rationale. No challenge or third-party implementation was copied.
