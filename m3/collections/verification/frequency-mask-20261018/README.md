<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Frequency-counter and native mask-window receiving proof

Canonical recipe/source packet: hsoliwal/com.synexia PR #12385, commit
`377a1c6627d815227639c327d852173b852649db`, under
`synexia-openrewrite-recipes/recipes/int-frequency-mask-20261018`.
Target preimage: `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`.

## Actual production changes

Only two existing target production files change: M3IntIntHashMap.java and
m3_index_mask_compactor.c. `addTo` reuses one discovered nonzero-key slot; a private
absent-insertion atom is shared with `put`. Preserve int overflow, zero-delta insertion,
zero-key policy, growth, physical slot order and iterator modification counts.

The existing JNI int/long window functions compact into the already-read values prefix
rather than retaining a separate accepted[256] stack array. `batch <= i` prevents an
unread chunk value being overwritten. JNI names, headers, Region calls, input validation,
state-drift diagnostics, independent offsets and detached-output behavior are unchanged.
No Java predicate/comparator callback or cached live-view membership is moved into native.

## Run target-source proof independently

Linux requirements: Java 21 JDK, GCC, Python 3. No Maven, network fetch, Synexia checkout or
Synexia runtime service is needed for this receiving check. The two exact Apache donor
sources under donor/ are verification-only fixtures, not product/runtime dependencies.

```sh
python3 m3/collections/verification/frequency-mask-20261018/verify.py \
  --output /absolute/new-directory-outside-this-checkout
```

This exact script was executed successfully against fresh copies of the received files.
It SHA-256-checks all source inputs, compiles actual Java with --release 21/-Xlint:all/-Werror,
generates JNI headers, builds actual C17 with warnings-as-errors and UBSan, and runs
Java, native (-Xcheck:jni), and explicit native-unavailable cases. Output checks respectively:
260388, 260407, 260390. These are standalone assertion checks, NOT JUnit test counts.
All retain observation digest 587f92aa87c2c49e474b3b7c364cf3e1df5d9ec551e095558ea68a6d312ddd31.

The frozen fixture covers 48000 mixed map operations, 1800 frequency/neighbor cases,
1000 paired-XOR algebra cases, and 6096 int plus 6096 long independent-offset windows,
including all 256 masks, empty/chunk-edge ranges, collisions/backshift, zero, overflow,
input isolation and native invalid-argument checks. Existing Synexia BitOpsArray.singleNumber
was inventoried, not modified/compiled; the XOR fixture tests the algebra and its domain.

## Recipe execution and resource evidence

EVIDENCE.txt records actual result counts, serial replay/refusal outcomes and compiler stack
observations; the full transcript/API hashes/JNI symbol lists are in the pinned canonical
packet's EVIDENCE.txt. Full transformation replay remains in canonical Synexia verify.py;
this thin receiver is only target-runtime proof. Actual materialization used the unchanged
canonical manifest kernel: KERNEL_REPLAY, not OpenRewrite SDK execution. The Maven/SDK/JUnit
tests are authored but NOT_RUN.

GCC 14.2 -O2 with UBSan/-fstack-usage reports int-window stack 2448 -> 1424 bytes and
long-window 4496 -> 2448 bytes on the tested Linux build. These are native stack values,
not retained Java heap improvements or a measured throughput ranking.

See ../../algorithm-catalog/frequency-mask-20261018.tsv and the pinned canonical catalogue
for existing-owner reuse, LeetCode/HackerRank/GFG categories, rights and rejected alternatives.
No challenge/editorial body was copied. Existing Apache-2.0 source headers remain intact.

## Pending gates — keep draft

Full module/reactor, Maven/OpenRewrite SDK/JUnit, global recipe/provenance registration,
target-built M3JDK21/HotSpot/jtreg, other platforms, allocation-failure injection, and isolated
performance/retained-heap qualification remain pending. This does not complete all
filtered/sorted wrapper work, change a public JDK API, or advance the ordered receiving phase.
All existing target owners and policy gates remain unchanged. Do not treat copied evidence,
this finite runtime pass, or PR ancestry as whole-product acceptance.
