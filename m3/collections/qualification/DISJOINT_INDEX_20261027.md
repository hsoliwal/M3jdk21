<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Indexed disjoint-set source receiver — 27 October 2026

**DRAFT / HOLD.** Product source baseline `hsoliwal/M3jdk21 master@4848f0f6f9c16ceada6a1de5d9281660cbf9e8f6`. Canonical first-party Maven/OpenRewrite recipe is [Synexia #13645](https://github.com/hsoliwal/com.synexia/pull/13645), source commit `7ad86fd1a71b4ea937cccecab59bb6a4505bff1a`, named `com.synexia.m3.M3Jdk21DisjointIndex20261027`. This is the original M3JDK21 product module `com.m3.collections`; the recipe source, donor experiments and tool project remain only in Synexia. Exact product files have no runtime dependency on Synexia.

## What is received

Four **new, exact source-postimage files**, preserving the existing Java/module interfaces, `java.util` and all previous M3 arrays/masks, filtered/sorted wrappers and JNI symbols:

- `M3DisjointIndex`: one growable `int[] parentOrSize` directory (negative root size, otherwise parent index), one parallel growable `int[] stateMasks` lane (full 32-bit caller flags). Zero is an ordinary index. Union-by-size with deterministic equal-size root selection; path compression, query count/size, mask updates and detached sorted root mask selection. No Node, Entry or per-member object store. No deletion/durable handle/concurrent mutation contract.
- `m3_disjoint_index.c`: one explicitly opt-in JNI `connectedBatch` with ephemeral scalar `GetIntArrayRegion`/`SetBooleanArrayRegion`, no held heap address or Java callback, bounded traversal and invalid parent refusal. No fallback when native requested and unavailable.
- Independently authored graph/BFS differential Java selftest plus JUnit 5 adapter.

All four are byte-identical to Synexia's canonical recipe's Git blobs. The source SHA-256 checklist is `m3/collections/qualification/disjoint-index-20261027.sha256`; no recipe project in this public product. `m3/collections/algorithm-catalog/disjoint-index-20261027.tsv` mirrors the first-party reviewed donor/category decisions (LeetCode → HackerRank → GeeksforGeeks, GitHub TheAlgorithms/Java @ `96ed2d7970606b51014afd25212c6c86d2af6255`). Challenge/editorial bodies and per-node GitHub donors were not copied. First-party Apache-2.0 copyright retained; external source rights unchanged.

## Actual focused local checks (exact template-produced sources)

Java21 `--release 21 -proc:none -Xlint:all -Werror` compiled; strict C17 GCC `-Wconversion -Wshadow -pedantic -Werror` with fatal UBSan built the JNI shared library. Java `-Xcheck:jni` executed the native batch. **17,064 standalone Java checks and 17,194 native-mode checks** on original random 4,096-operation graph/BFS oracle; wrong component count mutant compiled and was rejected. Synexia Java task recipe runner and its 18 fixed-point/preflight/cancellation/refusal tests were executed on the generated output.

A focused Action at `.github/workflows/m3-disjoint-index-jni-20261027.yml` runs the source seal and same independent Java/native differential from the product branch. Do not claim CI green until the exact PR head run completes.

## Promotion remains held

Actual Maven/OpenRewrite activation, full JUnit/module reactor and target installed image, jtreg/JCK, GC/JIT, non-Linux JNI and benchmarks were not run locally. The snapshot is not a replacement for all collection families or the installed JDK `java.util` implementations; `String → arrays → collections` remains the receiving order. No JNI throughput win is claimed without cold/warm measurements.
