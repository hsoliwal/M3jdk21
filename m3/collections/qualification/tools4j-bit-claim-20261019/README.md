<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 indexed-mask claim: SDK receiving qualification

M3JDK21 is the integrated SDK, not an M3 overlay. Runtime owner: m3/collections/src/main/java/com/m3/collections/M3ConcurrentBitSet.java. Reusable recipe is canonical in hsoliwal/com.synexia, commit 95f3eef415246879fcec27380f4b79f532f21f77, under synexia-openrewrite-recipes/recipes/tools4j-bit-claim-20261019. The checked-in SDK Java source is the recipe-generated after fixture, not a separate handcrafted implementation. No tools4j or Synexia runtime dependency, new retained entry store, new JNI symbol or java.base public signature replacement was introduced.

| Seal | Exact value |
| --- | --- |
| Target preimage Git blob | feb93280cbc1481c40c52156ffc50e2be5e35b54 |
| Target preimage SHA-256 | bb1486f22518957d1fa1f951ef78b312364de5a736aff17ae5a543fe1765a4c5 |
| Recipe postimage Git blob | 10054cd805a34d084e38925fb3d50b2fa1ab360c |
| Recipe postimage SHA-256 | 2ce7fb5dfb48729c29e5ca3d724aa754e025f4438d9f012d0d2368c09a83b0f6 |
| Donor reference | tools4j/mmap at 3cdb6a5d43522b93f13b0e881b1c61fdf3e015ba, IdPool64.java (MIT, tools4j.org / Marco Terzer / Anton Anufriev) |
| Code rights | original M3 Apache-2.0; independent algorithm adaptation; no tools4j text copied |

Contract: existing bitset operations unchanged. New claimFirstClearBit scans parent-owned 64-bit words in ascending order; each successful CAS claims one lowest observed clear valid bit; unused last-word positions are never claimed. Concurrent claims do not duplicate a bit until released; concurrent clears permit a non-global exhaustion scan. This is not a replacement for the generation-checked M3SlotAllocator28 or an independent allocator/node store.

Verification: PASS_LOCAL Java 21 javac --release 21 -Xlint:all -Werror, exact-source recipe replay, fixed point, changed-code/comment refusal, independent JDK BitSet differential model, capacity tails and concurrent 8/16-thread uniqueness, source hashes. PENDING Maven canonical recipe execution and target JUnit/reactor, source-built OpenJDK and installed-image smoke, JNI/HotSpot/jtreg, JMH performance, CPU/heap/native memory, full five-provider contract acceptance.

The related searchable donor problem-category catalogue is m3/collections/algorithm-catalog/tools4j-lowlatency-20261019.tsv; unqualified candidates remain held. Preserve STRING -> arrays -> collections target receiving order and draft PR status until later gates are actually executed.
