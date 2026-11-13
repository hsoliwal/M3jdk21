<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 arrayified Apache NodeCachingLinkedList receiver — DRAFT/HOLD

**Exact Synexia recipe candidate**: [hsoliwal/com.synexia PR #17268](https://github.com/hsoliwal/com.synexia/pull/17268), dataset
`m3-apache-nodecache-owner-20261113` inside the existing canonical Maven/OpenRewrite
`m3-collections-contract-refinement-20261017` crate.

This branch is a **review-only receiving candidate**. It is not release-qualified:
the canonical Maven/OpenRewrite named recipe has not executed in this session and
full target acceptance has not passed. Do **not** merge into `master` until all
mandatory gates are green. No reusable recipe, manifest template, recipe executor
or improvement memory is copied into the public product.

## Actual source relocation, not a wrapper

- Target owner: `src/jdk.m3.collections/share/classes/jdk/m3/collections/apache/M3NodeCachingLinkedList.java`
  in the **integrated** `jdk.m3.collections` JDK module.
- Retire the previous `m3/collections/implementations/apache-node-caching-linked-list/src/main/java/jdk/m3/collections/apache/M3NodeCachingLinkedList.java` all-throw scaffold
  (original Git blob `7cccf877127ca6d3b52165cac3b32b16806310a8`).
  Its exact preimage source, hash and donor licence remain pinned in private Synexia.
- New Java body has Git blob `3efc29e90f0bc0affa4af0567e5837e583e95eb3`,
  SHA-256 `927f4faf79963228c5038330778a8c62f45281074f771cea527220bca0e3d2e8`.
- Add `exports jdk.m3.collections.apache` to existing `module-info.java`.
  No new module, wrapper JAR, or Synexia runtime requirement is introduced.
- New target standalone regression source: `test/jdk/jdk/m3/collections/ArrayLinkedListContract.java`,
  blob `b2fbd597168171adda796ab6639bcea789182a06`,
  SHA-256 `ffd87664f77c934fdb44e22f59d220cf6380d07e61e1033e964d5d240199ef97`.
- A separate jtreg driver runs the existing regression source under the
  module boundary, granting test-only `--add-opens` for the reflected slot
  inspection without opening the production module to applications.

## Representation

The class owns an `Object[]` value pool, primitive
`int[] previous/next` physical links, a packed `long[]` live mask and an
integer free-slot chain. It allocates **no Node/Entry per list element**.
The code uses JDK 21 syntax, explicit Java imports, `@Serial`,
four-space formatting, checked capacity geometry, and JDK collection
views/iterators. It is an implementation, not a scaffold or provider delegate.

## Actual scoped evidence (independent of GitHub CI)

- OpenJDK 21.0.12.1: `javac --release 21 -Xlint:all -Werror` **PASS**
  on complete source and differential test.
- A `java.util.LinkedList` oracle passed **569,965 checks over 60,000
  seeded operations** (position insert/delete, null/duplicates, iterator
  mutation, reversed/subList mutation, serialization and ref release).
- Isolated Java module experiment: compiled as
  `module jdk.m3.collections { exports jdk.m3.collections.apache; }`
  with `-Xlint:all,-module -Werror`; external classpath test compiled
  against that module and passed the same **569,965** checks.
  Existing `jdk.m3.collections` module name produces the JDK's
  `[module]` naming warning under `-Xlint:all`; the probe suppressed only
  that warning. A test-only `--add-opens` enabled reflective state checks.
  **This is not an installed OpenJDK image or the complete M3 module.**
- Retained heap preflight, 200,000 shared references, `-Xms512m -Xmx512m
  -XX:+UseSerialGC`: 4,800,792 bytes for `LinkedList`, versus 2,914,576
  for the array-backed M3 owner, in two repeated isolated process runs
  (~39% reduction for that workload; no throughput claim).

## Mandatory HOLDs before merging

1. Execute **real** canonical Maven/OpenRewrite SDK recipe, source-refusal,
   fixed point and exact recipient source/postimage hash receipt in Synexia.
2. Confirm M3JDK21 naming map/capability ID and
   `m3/collections` Maven bridge dependency closure after source movement.
3. Build actual JDK image/JMOD; run jtreg, full affected Commons 4.5.0
   provider contract, JDK API/ABI/reflection/serialization and GC/JNI gates.
4. Run comparative JMH throughput, retained heap, process/native memory and
   skewed workloads; validate Windows source-built JDK21.
5. Preserve original Apache/Synexia attribution and obtain required
   `src/**` licence/rights review (first-party dual licence is not an
   automatic OpenJDK relicensing grant).

**Status:** code-bearing draft branch, not integrated master; all listed
HOLDs remain open. Do not describe it as a finished five-provider convergence.
