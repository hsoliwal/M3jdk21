# Five-library M3 SDK: Generic bidirectional mapping slice

M3JDK21 is the public product; Synexia is the canonical convergence and recipe workspace. Existing JDK, Guava, Eclipse Collections, Apache Commons Collections and Spring classes retain correct public names and different API semantics. This packet adds M3-backed *real* Guava BiMap and Apache BidiMap APIs, not completeness for every type in five libraries.

Existing M3 factories already provide JDK Map, Eclipse MapAdapter, Spring MultiValueMap, Guava Multimap/Table and Apache ListValuedMap/SetValuedMap. This increment adds Guava and Apache bidirectional contracts using exactly the incumbent M3LinkedHashMap owner for forward/reverse indexes, one shared relation, no per-mapping retained Entry/Node objects, and provider-specific mutation rules.

## Design

- Guava: M3GuavaBiMap implements the real com.google.common.collect.BiMap; unique-value refusal, forcePut, live inverse, mutable key/values/entry views.
- Apache: M3ApacheBidiMap extends upstream AbstractDualBidiMap, supplies M3LinkedHashMap twice, retains Apache's upstream MapIterator and inverse views.
- Existing M3LibraryCollections gets **two** additive typed factories, with updates to its exact upstream name/owner binding list. Existing 22 signatures unchanged.
- Java references and callbacks stay in Java. Optional existing M3 JNI primitive mask engine is unaffected; no Java object pointers or new C/JNI symbols.

## Provenance and correctness

Source Apache-2.0, Copyright 2026 Hitesh Soliwal and contributors. Upstream library dependency source remains owned/licensed by each vendor; no code from Guava/Commons/HackerRank/LeetCode/GFG copied. Named problems are algorithmic categories, not evidence of imported challenge solutions.

Locally: Java21 -Xlint:all -Werror Guava adapter selected-source compilation and JDK-backed temporary-map oracle PASS 324001 assertions. This verifies algorithm logic only: local stand-ins were used for Guava BiMap interface and M3LinkedHashMap. No production M3 dependency, Guava or Apache JAR was installed locally. Actual Maven/OpenRewrite and real vendor differential suite and module/runtime/JNI gates are **PENDING**. Do not declare complete 5-library SDK, provider ABI parity or performance supremacy without exact-head evidence.

Source-sealed OpenRewrite recipe must materialize both new classes, the two vendor JUnit tests and changed factory/catalogue. Do not hand-edit target generated code. Preserve the String→arrays→collections gating and private Synexia develop append-only policy.
