<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 CPU masked-window statistics receiver — 2026-10-18

**Scope: independent candidate in m3/collections; JDK runtime promotion remains in STRING.**

Canonical authoring and recipe owner: Synexia, Maven/OpenRewrite composite
`com.synexia.rewrite.M3CpuMaskWindowStatsRecipe`.
Java implementation and self-test are exact SHA-256/Git-blob mirrors of Synexia's
reviewed resources; no OpenJDK java.base/java.util source changed.
The current JDK public collection surface and serialization are untouched.

## Contract and selection

`M3CpuMaskWindowStats.scan` consumes caller-owned integer row IDs, dynamic-array
physical offset lanes and full 32-bit stage masks. It delegates ordered selection
and explicit JNI availability to the existing `M3IndexPathMasks.scanWindow`.
Signed-stride traversal, duplicates, negative row IDs, full sign-bit mask values,
arbitrary alignment and source ownership are preserved. It emits bounded **tumbling**
windows of selected occurrences as (selectedOrdinal, count, sum, minimum, maximum).
The last partial window is emitted once, never an empty window. Width is 1..4096;
output count is separately bounded. It retains one scratch int[] with at most
4096 slots (16KiB primitive payload), and O(1) aggregate state; no per-row Entry,
String or result collection is retained.

This is **not** a sliding-window, CEP, time-window, histogram of payload objects,
group-by-key or JNI acceleration claim. Row IDs can denote interned M3String
atom IDs in a separately qualified namespace, but this operator sees only signed
ints and never converts them into text. M3IndexPathMasks remains the sole
native/Java backend owner.

## Proof and limits

Local Java 21 javac --release 21 -Xlint:all -Werror and a dependency-free
self-test passed under -Xmx24m with a *temporary API-compatible selector shim*:
900 deterministic randomized differential trials, signed-stride cases, duplicate
and extreme row IDs, 10,000 selected occurrences spanning selector windows,
zero/max output budgets and missing-JNI refusal. This verifies the additive
reducer only, **not** the live M3 selector, JNI, jtreg or complete image.

The target verifier verify_donation.py admits the two Java paths only through a
separately SHA-256-pinned successor manifest. Existing installed source receipts
are immutable. A new, malicious or aliased manifest is rejected, while omission
preserves the old source set. The existing collection JUnit/JNI verifier must
actually execute on a configured Linux/Java21 host to qualify the receiver.
The targeted GitHub Actions job compiles all real m3/collections main sources
and executes this oracle under -Xmx24m. Source/template hashes, exact real
OpenRewrite SDK generation, runtime behavior and optional native parity are
separate proof authorities.

No LeetCode, HackerRank, GeeksforGeeks or third-party implementation is copied.
Original first-party expression remains Apache-2.0 with copyright and NOTICE.
