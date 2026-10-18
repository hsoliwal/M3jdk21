<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 five-provider indexed projections — canonical Synexia recipe

This source-sealed Maven/OpenRewrite crate reuses existing M3GpuToolJoinRecipe and M3GpuToolJoinManifest. Synexia is the convergence/recipe owner; M3JDK21 owns public Java APIs, packaged dependencies and native ABI. Source- and test-level changes are made only by applying this named recipe to exact pinned preimages, never by manually patching the generated Java receiver.

Four exact changes: existing `M3FiveLibraryIndexViews`, its real-provider JUnit oracle, and the complete 34-path source-census receipt and verifier. No new storage engine, no new JNI C symbols, no package-spoofed vendor names. Original public APIs and other source hashes are retained. The result shape remains an immutable indexed snapshot, not a live filtered view. Each materialization creates its own M3-backed mutable adapter and keeps the named vendor API's contracts, including Guava BiMap value uniqueness, Guava Table ordinal columns, Eclipse duplicate handling, Apache MultiSet counts, Spring key-count map-of-lists and ordinary JDK Set equality.

Only existing M3 components used: M3LinkedHashMap, M3LinkedHashSet, M3CountedBag, M3FiveLibraryIndexViews, M3IndexMaskCompactor/Window, and the existing five provider adapters. No per-row Entry/Node retained by M3, no Java callback crosses JNI; JNI is opt-in and strictly native-required. JDK 21/Maven + real provider JARs and unchanged JNI are CI gates, not presumed evidence. The original 34-file historical source-census allowlist and historical five source identities remain unchanged; only two affected source row digests and the receipt hash change.

**State:** candidate/draft. The five-library SDK-wide superset remains the overall target; a named contract has not passed merely because its entrypoint exists. Preserve Apache-2.0 first-party licensing and original dependency NOTICE/provenance.
