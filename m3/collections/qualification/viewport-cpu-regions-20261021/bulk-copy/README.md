# M3JDK21 primitive index / JNI candidate custody

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0

This is review material for the existing M3JDK21 PR 1759, not an M3 runtime layer.
Runtime sources remain unchanged. These .txt files are exact, inert first-party
Apache-2.0 source custody snapshots, not another canonical recipe or active build
source. Do not copy them manually into runtime paths or apply the old patch on
top of them. The canonical five-target recipe applies one coherent source set.

Canonical source: hsoliwal/com.synexia PR 12855 at
5a0e2eaa2f1c5db1106db5068e614e2ff5e9fa2f, dataset
synexia-openrewrite-recipes/recipe-crates/m3-ast-build-reverse-20261017/datasets/viewport-cpu-regions-20261021.
Existing M3HashPinnedJavaSnapshotRecipe/M3HashPinnedTextSnapshotRecipe and receive.py
remain the only transformation/application authority. No recipe is forked here.

M3IndexSort changes only callback-free primitive merge tails to arraycopy, keeping
stable order, the comparison schedule and partial callback-failure states.
m3maskviewport.c skips the second mask read only when complete preflight proves
a dense stable-input window; offsets still enter the existing selected scratch
in regions of at most 128 ints, followed by one output write. The new contract
composes mask filtering, stable physical-index sorting and viewport paging.

The three custody Git blobs and all five source pre/post SHA-256 seals are in
RECEIPT.json. Original Java page improvements and the original viewport test
are unchanged from the existing proposal. The new native candidate supersedes
the earlier native candidate; the old owner-candidate.patch is historical.

Local host execution passed strict Java21/GCC/Clang diagnostics, descriptor/header/
export parity, normal and interpreted Java/JNI tests, -Xcheck:jni and fatal UBSan.
The new oracle reports 36,424 Java and 37,384 JNI checks per execution. The original
72,591/108,891-check corpus also passes. Counts overlap across repeated executions.
An instrumented JNI-table fixture, separate from real JVM tests, observes 4096 dense
rows at 96 rather than 128 region reads, 32 rather than 64 state reads, unchanged
128-region sparse behavior, and one output write. No throughput/RSS claim is made.

Maven/OpenRewrite SDK replay, fixed point, refusal tests and actual product receiving
have NOT RUN. Neither these source snapshots nor a queued job advances the ordered
receiving phase. Full wrapper/provider contracts, installed JDK/HotSpot/jtreg,
widget/GPU/theme/language/IME/accessibility and comparative resources remain pending.
The existing live-view reentrancy donor PR 1734 / Synexia 12818 is not silently imported.
Keep draft. No merge, rebase, default-branch change or runtime promotion is claimed.
