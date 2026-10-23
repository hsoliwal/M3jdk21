<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original Java/C JNI + atomic-fused array owner

**Status:** DRAFT / candidate-only. Source template equality is not a
substitute for executed Synexia OpenRewrite SDK or actual runtime proof.

## Product ownership

**M3JDK21 is the actual Java/HotSpot/JNI JDK**, not a library layered on
another JDK. The original internal `jdk.internal.m3.M3IndexedArray`,
`M3IndexedView`, `M3IndexMaskWindow`, original `java.util.CollectionViews`,
and JDK `libjava/M3IndexMaskWindow.c` remain the sole owners. There is no
new exported `java.*` class, per-entry Node/Entry payload store, secondary
`Object[]`, JNI callback or application runtime dependency.

The canonical private Synexia source recipe is
[Synexia #13223](https://github.com/hsoliwal/com.synexia/pull/13223),
`com.synexia.m3.M3Jdk21AtomicFusedOriginalJni20261023`.
All executable recipe Java/YAML/POM, templates, fixtures, algorithm
catalogue and improvement memory stay in Synexia. This JDK contains only
the candidate source postimages, native JDK test script and data-only
provenance.

## Exact receiving manifest

Base: [M3JDK21 #1879](https://github.com/hsoliwal/M3jdk21/pull/1879),
Git head `20f2b38e86c4317552cbd66f1a3e68f3788e336b`.
Prior verified separate JNI donor:
[M3JDK21 #1878](https://github.com/hsoliwal/M3jdk21/pull/1878),
head `9a66f1973420efa3ea6f8d1e0cc42b986952f2a0`.

Target original Java mask source:
`f42473470989a103062696f1a3f4aa348e524308391edcaffff3df311d35eacf`
→ `91c7a99d394fe76a47af0c1eaaa35ffd6d070ee6e2497a63668db8301dd86329` (SHA-256).

Target original libjava C source:
`153ec9727301febd6fff24a55877ec549850ebac997366fe25bb04e9b9bee81b`
→ `4a9c0ab3e8d7a0aac0027e8fb4789f768ee5a48b2db18b7a54e2fd543c5c7f91` (SHA-256).

Three exact original #1878 JNI test sources are additive (ABSENT-before).
The integrated verifier is additive, and the existing #1879 atomic-growth
workflow is extended without removing its checks.

## Explicit no-overwrite invariant

Do **not** replace existing `M3IndexedArray` Git blob
`e6059aeb9b2e7f332fb0baea2684395c268a5414`: it contains
seven-array failure-atomic growth, fused mask+predicate selectors and
original sorted index state.

Do **not** replace current original `java.util.CollectionViews` blob
`2fe6e7ede2af18d4523f3a5531038e9a865cdb6d` with historical
#1878's `2ff7ec57ab93c8bd118ceb320fd76aae1c0e7d66`.
The current branch includes stronger stored-member predicate, deletion
and reentrant Spliterator behavior. Retain current source bytes exactly.

## Required proof and promotion law

Existing independently passed host proofs:
- #1878 JNI + CollectionViews host CI [39909653352](https://github.com/hsoliwal/M3jdk21/actions/runs/39909653352).
- #1879 Java21 seven-site fault agent, fused sort and state-mask host CI
  [39915843843](https://github.com/hsoliwal/M3jdk21/actions/runs/39915843843).

These are separate heads. The **combined head** must execute the
existing 21 fault-injection cases in -Xint/-Xmixed **and** the original
JNI C17 count/scan, javac-generated headers, -Xcheck:jni, normal/-Xint
and later CollectionViews contract oracles. Every source input is
SHA-256 pinned and refuses drift; generated/suppressed test PASS is not
accepted. Entire image and full release still require the actual
M3JDK21 built HotSpot/JVM, jtreg/JCK, GC/CDS/JVMTI, JNI platform
matrix, five-provider compatibility and comparable CPU/heap/native
process measurements. Unexecuted gates = NOT_RUN/HOLD, never PASS.

No rebase, reset, force update or automatic merge of any convergence
or product branch. The String→arrays→collections admission sequence is
not advanced by this draft.
