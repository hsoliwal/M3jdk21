<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# Canonical five-provider direct-Spring SDK recipe — 2026-10-17

This is a source-first additive successor stacked on **M3JDK21 PR #1174** (the 33-factory/20-collector five-provider SDK), while reusing the already-authored independent Spring implementation from **M3JDK21 PR #1190** and its source-first Synexia **PR #12181**. No new map or list storage engine.

## Contract identities

Correct original package types remain:
- JDK `java.util.SequencedMap`, M3 `M3LinkedHashMap`;
- Guava `com.google.common.collect.BiMap` and `Multiset`, M3 `M3GuavaBiMap` and `M3GuavaMultiset`;
- Eclipse Collections `org.eclipse.collections.api.bag.MutableBag`, M3 `M3EclipseBag`;
- Apache Commons Collections `org.apache.commons.collections4.MultiSet` and `BidiMap`, M3 `M3ApacheMultiSet` and `M3ApacheBidiMap`;
- Spring `org.springframework.util.MultiValueMap`, direct `M3SpringArrayMultiValueMap`.

The directly implemented Spring class is **not** Spring `MultiValueMapAdapter`. It delegates all underlying key/value-reference ownership to the already-admitted M3LinkedHashMap / M3LinkedList. Caller-provided list identity and live `asSingleValueMap` semantics are tested with real Spring 6.2.0; detached `toSingleValueMap` reuses an independent M3 map.

The old five-provider factory signatures and historical receipts are **preserved**. `M3FiveProviderDirectSdk` is a **new opt-in named entry point**, not a second data-structure hierarchy. The 33 legacy factories/20 collectors remain intact; their older Spring factory still exists for compatibility until an exact-preimage controlled later replacement is qualified.

## Mandatory recipe-first source

New Maven crate reuses existing unchanged Synexia `M3GpuToolJoinRecipe` and `M3GpuToolJoinManifest`. Four ABSENT-only SHA-256 pinned Java sources are generated:
(1) original direct Spring implementation, copied byte-exact from PR #1190;
(2) added five-provider SDK entry points;
(3) source-adjusted Spring's 4500-step differential test to exercise the new opt-in entry point;
(4) a separate cross-five provider suite including one shared M3ObjectCounts owner.

Java source admission verifies exact installed files against templates; second pass is a fixed point; occupied sources must refuse. Existing five-provider recipe owners are not rewritten. For existing file refinements use the canonical OpenRewrite LST recipe in a separate reviewed follow-up.

## JNI and evidence

No JNI production code is created or modified in this slice. The original five-provider platform workflow remains responsible for the existing checked native mask/count Java/C17 parity; the new workflow independently compiles and runs real provider JUnit against the current five-provider branch with pinned official JARs. No Java predicates or payload references cross JNI, no native speedup claim.

Copyright/license: first-party Apache-2.0 source and templates, preserving Spring 6.2 API copyright and original project notices; Eclipse EPL/EDL APIs remain external dependencies. LeetCode/HackerRank/GeeksforGeeks category references are **reference only**; no challenge source copied.

Status: target exact-head Maven, JUnit, Java/JNI, full installed OpenJDK, jtreg/JCK and every one of the five libraries' APIs are not yet all independently qualified. No promotion beyond the STRING-first receiving gate.
