<!-- SPDX-License-Identifier: Apache-2.0 -->
# Native M3 collection substrate for the five-library SDK

**Product destination:** `hsoliwal/M3jdk21/m3/collections` — NOT `m3/collections/interop`.
**Canonical recipe owner:** `hsoliwal/com.synexia`, branch `m3/native-five-provider-core-20261018-recipe`.
**Input census:** already-merged M3JDK21 PR #1212 source-admission catalogue. It names vendor interop Java files, not implementations in core.

## New Java 21 product owners (all package com.m3.collections)

1. **M3IndexedBiMap<K,V>**: two existing M3LinkedHashMap indexed-array directories, unique values, shared live inverse identity, forcePut and write-through key/entry/value mutation. No retained entry/node graph; only API-facing transient entries. Not a Guava or Apache interface impersonation.
2. **M3IndexedSetMultiMap<K,V>**: existing M3LinkedHashMap per-key directory plus existing M3LinkedHashSet value lanes, pair counts, duplicate suppression, live per-key set, detached indexed snapshots, prepared filter and stable sort through existing M3IndexedListSnapshot, opt-in existing M3IndexMaskCompactor JNI.
3. **M3ListMultiValueMap<K,V>**: existing M3LinkedHashMap keyed Map-of-Lists; internally created values are actual M3ArrayList, externally supplied lists preserve aliases; key-count size, absent/empty/null separation, independent single-value snapshot versus live asSingleValueMap.
4. **M3NativeFiveLibrarySdk**: actual M3-only factories reusing the canonical list/set/map/tree, counted bag, indexed list multimap, indexed table, and primitive indexed snapshot owners.

No `org.apache.commons`, `org.eclipse.collections`, `org.springframework`, `com.google.common`, or `java.util` source replacement/renaming is introduced; ordinary JDK Map/List interfaces remain necessary boundaries. No new JNI symbol or C implementation is introduced; existing vetted primitive index selectors are used unchanged. The original provider adapter module is **not** used as a runtime dependency for these M3 core owners.

## Source-sealed Maven recipe first
`M3NativeFiveLibraryRecipe` installs **five ABSENT source/test leaves only**. Each Java source is authored as a canonical template, SHA-256 sealed, refusal preflighted for drift/symlink/missing, and idempotently generated. Correct the canonical template/recipe, not the output. Existing class semantic transformations must reuse the established OpenRewrite Java LST visitor; no existing production file is being rewritten by this additive recipe.

**Local executed evidence:** Java 21 `javac --release 21 -Xlint:all -Werror` PASS, deterministic stand-alone tests `PASS M3NativeFiveLibrarySelfTest checks=1653019`. **Important:** this isolated local proof used test-only JDK-backed stand-ins for the unchanged M3LinkedHashMap/M3ArrayList and API stand-ins for other M3 owners. It is **not actual M3 ownership or JNI integration proof**. Recipe direct-Java `--selftest`/apply/check PASS missing/drift/fixed-point and SHA template checks; Maven was unavailable locally.

**Pending:** exact target-head full M3 core Maven/JUnit, real existing M3 map/list/tree/bag contract integration, JNI checked runtime, installed JDK/jtreg, heap/performance, complete provider API source/binary compatibility. No blanket five-library SDK completion claim. See `M3_NATIVE_36_FILE_CROSSWALK.tsv` to track the source census and the exact M3 destination; a mapping does not imply a full adapter replacement.

**Licenses:** all newly authored source and recipe are first-party Copyright 2026 Hitesh Soliwal and contributors, Apache-2.0. Vendor names and category references are NOT copies of third-party source.
