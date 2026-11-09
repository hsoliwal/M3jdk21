# M3JDK21 frozen String index and reverse metadata contract

<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0 -->

## Ownership and serial receiving

This is refinement inside `java.lang.M3StringPool` in M3JDK21. Existing atom, tuple, owner and facts representations remain the contract owners. Reusable source-sealed OpenRewrite recipes live in `hsoliwal/com.synexia`. Each input is admitted at its exact pinned preimage or declared absence; the receiver consumes retained actual SDK Results, preflights all paths, applies files serially, and reaches a zero-change repeat. No receiving phase or family-completion status is advanced by this leaf.

## Frozen behavioral atoms

1. Frozen exact lookup uses an optional primitive row-plus-one directory, existing validated Java hashes and UTF-16 lengths, a power-of-two mask, half occupancy, a 64-probe budget and at most 2^22 slots (16 MiB for the configured frozen owner). Exact UTF-16 comparison decides every indexed hit. The old binary method remains authoritative when the directory is unavailable, oversized or over its construction/probe budget. Ambiguous hash/length groups use binary lookup, preserving the original row chosen for duplicate SYNARR01 spellings.
2. The existing validation pass computes mapped Java hash and Latin-1 eligibility together. An optional byte per row avoids a cold reverse coder scan. The original endian-aware and CompactStrings-aware scan remains when metadata is absent. Mandatory atom arrays precede optional allocations. Shared-record validation retains int[4] scratch geometry; the transient offset sign-bit flag is removed before publication.
3. Refused scalar or tuple admission retires an empty metadata bucket under its existing monitor. Existing retired-bucket retry, weak references, native release, byte budgets and tuple accounting remain. Tuple cleanup reuses its captured boxed key.

Public/protected declarations, M3LEX/SYNARR01 bytes, ordering, row/record identities, exact unpaired-surrogate behavior and mapping lifetime are unchanged. A mapped lexical row is not a permanent semantic concept identifier. Optional slots and coders are VM-local metadata; payload mapping retains existing OS sharing.

## Catalogue and licensing

Synexia MappedCompiledLanguageImage and MIndexStringSearchIndex supply reviewed primitive indexing patterns. The new java.base code is an adaptation owned by M3JDK21; no Apache implementation body is copied into GPL code. Existing source notices remain. Agrona primitive open addressing is reference-only. Challenge platforms nominate categories, with no submission/editorial code copied. The canonical searchable catalogue records candidate selection and rejected format/semantic changes.

## Required ordered gates

Lint, compile all actual M3String family owners, compile both new jtreg tests, run the complete source-invariant gate and its mutation suite, build a fresh strictly qualified matching M3JDK21 image, then execute the selected full-owner jtreg matrix. Preserve every failed attempt and its corrective pass. SDK replay and source-gate mutation results do not establish runtime or RAM-speed qualification.

The runtime selection is M3StringFrozenLexiconIndexTest, M3StringPoolMetadataBoundsTest, M3StringPoolBoundsTest, M3StringInternTest, M3StringUtf16DifferentialTest and M3StringFactsCompositionTest under test/jdk/java/lang/String. The frozen tests cover versioned images, all UTF-16 ingress windows, coder modes, duplicate rows, collision/cap fallbacks, optional metadata absence and corruption refusal. The metadata test covers repeated zero-budget refused admissions. Runtime and performance results remain pending until a source-matched full image executes them.
