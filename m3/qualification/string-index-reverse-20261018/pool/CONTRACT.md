# M3JDK21 frozen String index and reverse metadata contract

<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0 -->

## Ownership and serial receiving

This is refinement inside `java.lang.M3StringPool` in M3JDK21. Existing atom, tuple, owner and facts representations remain the contract owners. Reusable source-sealed OpenRewrite recipes live in `hsoliwal/com.synexia`. Each input is admitted at its exact pinned preimage or declared absence; the receiver consumes retained actual SDK Results, preflights all paths, applies files serially, and reaches a zero-change repeat. No receiving phase or family-completion status is advanced by this leaf.

## Frozen behavioral atoms

1. Frozen exact lookup uses an optional precomputed two-level displacement address over the existing validated Java hash + UTF-16-length key. Admission groups equal keys once and places each distinct key into primitive power-of-two displacement/row lanes capped at 2^22 row slots (16 MiB). A normal hit performs one 64-bit mix, one displacement load and one row-slot load: there is no runtime collision-chain probe. Exact UTF-16 comparison still decides every indexed hit. A genuinely ambiguous Java-hash/length group carries a negative candidate marker and delegates to the unchanged exact binary authority, preserving the original row chosen for duplicate SYNARR01 spellings. Non-fixed compatibility mode also retains the binary authority if optional direct metadata cannot be built; configured fixed-address admission refuses a non-empty lexicon when its direct-address index cannot be created.
2. The existing validation pass computes mapped Java hash and Latin-1 eligibility together. An optional byte per row avoids a cold reverse coder scan. The original endian-aware and CompactStrings-aware scan remains when metadata is absent. Mandatory atom arrays precede optional allocations. Shared-record validation retains int[4] scratch geometry; the transient offset sign-bit flag is removed before publication.
3. Refused scalar or tuple admission retires an empty metadata bucket under its existing monitor. Existing retired-bucket retry, weak references, native release, byte budgets and tuple accounting remain. Tuple cleanup reuses its captured boxed key.

Public/protected declarations, M3LEX/SYNARR01 bytes, ordering, row/record identities, exact unpaired-surrogate behavior and mapping lifetime are unchanged. A mapped lexical row is not a permanent semantic concept identifier. Optional slots and coders are VM-local metadata; payload mapping retains existing OS sharing.

## Catalogue and licensing

Synexia MappedCompiledLanguageImage and MIndexStringSearchIndex supply reviewed primitive indexing patterns. The new java.base code is an adaptation owned by M3JDK21; no Apache implementation body is copied into GPL code. Existing source notices remain. Agrona primitive open addressing is reference-only. Challenge platforms nominate categories, with no submission/editorial code copied. The canonical searchable catalogue records candidate selection and rejected format/semantic changes.

## Required ordered gates

Lint, compile all actual M3String family owners, compile both new jtreg tests, run the complete source-invariant gate and its mutation suite, build a fresh strictly qualified matching M3JDK21 image, then execute the selected full-owner jtreg matrix. Preserve every failed attempt and its corrective pass. SDK replay and source-gate mutation results do not establish runtime or RAM-speed qualification. Structural scale proof covers 60,000 and 600,000 distinct keys with the same one-candidate lookup shape; wall-clock latency remains a separate measured gate.

The runtime selection is M3StringFrozenLexiconIndexTest, M3StringPoolMetadataBoundsTest, M3StringPoolBoundsTest, M3StringInternTest, M3StringUtf16DifferentialTest and M3StringFactsCompositionTest under test/jdk/java/lang/String. The frozen tests cover versioned images, all UTF-16 ingress windows, coder modes, duplicate rows, collision/cap fallbacks, optional metadata absence and corruption refusal. The metadata test covers repeated zero-budget refused admissions. Runtime and performance results remain pending until a source-matched full image executes them.
