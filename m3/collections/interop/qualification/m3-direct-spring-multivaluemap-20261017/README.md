<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Direct provider implementation, not an interoperability adapter

The implementation target is **the full five-library collection contract families**.
JDK, Guava, Eclipse Collections, Spring Framework and Apache Commons Collections have
different public interfaces, ownership, serialization and semantic rules.
A factory/wrapper exposing an interface is not evidence that the donor's class/API
hierarchy is implemented. This source-first tranche qualifies a real Spring
`MultiValueMap<K,V>` directly and marks remaining direct families pending.

- New `M3SpringArrayMultiValueMap<K,V>` **implements** the real Spring 6.2
  `MultiValueMap` and extends only JDK `AbstractMap`. It neither extends
  `MultiValueMapAdapter` nor delegates provider operations to it.
- The value/key directory is incumbent `M3LinkedHashMap`, backed by arrays,
  slot indexes and primitive control metadata. Newly created per-key lists are
  `M3LinkedList`, using integer links; caller-supplied lists stay borrowed
  by identity, including their mutability/alias semantics.
- Live `entrySet`/`keySet`/value collection behavior, null/empty distinction,
  `getFirst`, `addIfAbsent`, `addAll`, `set`, `setAll`,
  detached `toSingleValueMap`, and live `asSingleValueMap` are directly owned.
- Keep the original adapter-based `M3SpringMultiValueMap` type available
  solely for existing binary/source users; the published provider factory now
  instantiates the direct M3 class. The mapped public return type is unchanged.
- Tests use **actual Spring 6.2.0** interface and existing Spring adapter **only
  as the differential oracle**; never as the implementation. Includes 4,500
  seeded provider operations, null/alias contracts and live views.
- No new JNI symbol: M3's primitive selection JNI remains the existing,
  independently qualified owner. Do not move Java object references/predicates
  to native memory.

**Recipe-first serial FILE sequence:** inventory and pin original two existing
sources -> compare donor semantics/license -> source-sealed Maven recipe
-> generate the one new actual provider class and one new test -> update the
existing factory and binding metadata -> strict Java21/provider tests ->
fixed-point and drift refusals -> reviewable M3JDK21 PR.

This does **not** claim the other four full provider hierarchies are complete.
No default branch edits, destructive retrofit, API forgery or automatic phase
promotion before M3 String → arrays → collections. The nontrivial mutation,
serialization, concurrency and all-provider compatibility matrix remains a gate.
