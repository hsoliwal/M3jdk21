<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# String cursor and regex receiving handoff

The 12 receiving outputs keep the exact #1371 full clipped visible-leaf cursor and #1376
guarded closure, then select fresh v2 physical-width-one stopping at 64-unit boundaries
inside the existing Java/C regex owners. UTF-16 coordinates, nested slice clipping,
arbitrary forward/reverse reads, width2 late-surrogate UNKNOWN, tuple visitation,
native validation order, owner fences, thresholds and original signatures remain.
At most 63 further in-range byte units are consumed after a newly dead frontier.

Both unchanged-SDK recipe runs passed four tests and emitted the selected two refinement
owners and 12 receiving files. Java21 strict compilation covered 15 actual owners and
observed 49 production classes per side. Full member descriptors and generated/exported
JNI ABI matched. All 16 behavior JVMs passed JIT/interpreter and normal/UBSan comparisons;
eight used checked JNI. The owner oracle recorded 2,304,779 checks, digest 5e40f45bda4ca338;
the actual C packet oracle 71,967, digest 7bc2ab3713b204fa; the exact cursor fixture
1,746,792 checks in 35,914 scenarios per mode. No replacement platform String was loaded.

The frozen 24-case, three-paired-fork rule admitted v2. Java small-live width1 ratio
1.097532 and native validation width2 1.092432 are close to the 1.10 threshold. The first
per-unit candidate failed three cases and remains rejected historical evidence. There
is no universal speed claim, and the cursor is held equal on both sides of this matrix.
JNI implementation native allocation is unmeasured; process RSS is not retained heap.

Active runtime datasets: m3-catalogue-regex-latin1-recovery-v2-20261018 and
m3-catalogue-regex-receiving-recovery-v2-20261018. Original GPLv2/Classpath and
Apache/dual notices are retained. Helix regex-cursor is a pinned conceptual reference;
no Rust body, byte-index semantics, streaming feature or runtime dependency is imported.

The canonical runtime recipes and fresh qualification packet are pinned at
[Synexia C1 0a3e7f9f4b2fe5067532cc79b75f4dea2aa6c5b2](https://github.com/hsoliwal/com.synexia/tree/0a3e7f9f4b2fe5067532cc79b75f4dea2aa6c5b2).
The direct runtime owners remain in M3JDK21, based on a0e9d6c0c95960deb21d45f3800e592b890a4d28; canonical recipes
author exact source postimages, without a Synexia runtime dependency. This is a draft
review receipt and does not promote the current STRING phase or mark completion.

The adjacent receiving.json contains exact before/after SHA256 and Git blobs for this
lane's runtime files and C1 paths for their recipe manifests/templates. The original
48-output intake excludes these four new receipt documents, avoiding self-hashes.
This handoff uses the existing text engine (blob 9d0788d3717e017d58345c5c3e62022c2915a3c8) in the new
m3-catalogue-receiving-handoff-recovery-20261018 dataset. Its metadata-only recipe is published after C1; these documents
intentionally do not invent a C2 self-pin.

A new combined M3 VM image, whole jtreg/platform matrix, HotSpot/GC/CDS/JVMTI,
product dispatch/lifetime integration, all eight backends/bitlane and all-phase parity
remain separate gates. The older executor's raw logs are unavailable; fresh recovery
records carry the stated tests. Existing sealed runtime documentation stays unchanged.

## Canonical evidence

- [QUALIFICATION_SUMMARY.json](https://github.com/hsoliwal/com.synexia/blob/0a3e7f9f4b2fe5067532cc79b75f4dea2aa6c5b2/synexia-openrewrite-recipes/recipe-crates/m3-catalogue-refinement-20261018/convergence-recovery-20261019/string/publication/QUALIFICATION_SUMMARY.json) — SHA256 8f2106e401e6b62ceacb5798206e1353a9aa94f212db3d2b5f8cf9d69295bb6e
- [BENCHMARK_SELECTION.json](https://github.com/hsoliwal/com.synexia/blob/0a3e7f9f4b2fe5067532cc79b75f4dea2aa6c5b2/synexia-openrewrite-recipes/recipe-crates/m3-catalogue-refinement-20261018/convergence-recovery-20261019/string/qualification/runtime-v2-001/BENCHMARK_SELECTION.json) — SHA256 0ceb2986cba82ead0fece8e5d01c1612b36094b8296005ebe75ada2a8a6bcbc4
