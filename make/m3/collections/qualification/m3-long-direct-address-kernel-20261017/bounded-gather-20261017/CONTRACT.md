<!-- SPDX-License-Identifier: Apache-2.0 -->
# Bounded gather receiving contract

Copyright 2026 Hitesh Soliwal and contributors. 2026-10-17.
Baseline: 5abb627329d67072667237f62c7ef95511da927b, existing PR #1092.
Canonical recipe contract: Synexia commit 048e169acce296f9ee90891f61e9ba7ab5210da2,
synexia-openrewrite-recipes/recipes/jdk-bounded-address-gather-20261017/CONTRACT.md.

Preserve M3LongAddressKernel, M3LongIndexProjection, M3IndexMaskCompactor and all
public Java APIs. Refine only the existing native gather: <=256-index chunks,
3072 bytes fixed own scratch, contiguous region reads, at most one payload critical
pointer at a time, released before publishing output. No pointer survives a chunk.
Keep ordering, duplicates, offsets, no-alias boundary, and valid-prefix writes when
an invalid physical index is encountered. Caller keeps arrays stable; no new
concurrent-mutation, atomic snapshot, absolute latency or OOM-atomicity promise.

The original native template and workflow template must change with their emitted
counterparts so the earlier receiver's checks remain coherent. Add exact emitted
Java self-test and JUnit adapter; retain the original regression. No new library,
object wrapper, payload owner, native comparator or mask representation.

Synexia owns the reusable Maven/OpenRewrite recipe and its source seals. This target
receives actual qualified SDK Results and its own runtime tests/provenance. Existing
receiver source, historical tests and receipts remain intact. No default-branch or
unrelated PR mutation. Naming-map disposition: existing owner and JNI symbol retained;
no runtime family/name added and no String -> arrays -> collections phase promoted.

Acceptance is bounded to the selected Java/C owners, actual SDK replay, Linux native
runtime and protocol instrumentation. Whole-JDK image/jtreg/JCK, full collection
reactor, original target receiver Maven self-test, all collectors/platforms and GUI
integration remain separate gates. The protocol fixture is not a JVM. Diagnostics
must disclose irregular-gather overhead; Java remains the unchanged default.
