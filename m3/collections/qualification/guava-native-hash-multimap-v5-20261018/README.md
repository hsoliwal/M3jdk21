<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Latest qualified increment: native HashMultimap

HashMultimap uses existing M3LinkedHashMap arrays with primitive indices, order, state, offsets and masks. Authoritative storage has no structural Entry/Node objects. A bounded 128-slot primitive M3LFUCache adaptation manages immutable pair API projections. Parent iterators may still allocate temporary entries.

The historical POM package and sole-JAR run passed 16,336 cases. The unchanged fresh receiver passed five storage cases and 16,337 combined sole-JAR cases separately. The full report binds strict Java 21 compilation, five Java/seven XML recipe checks, typed clients, callbacks, storage/cache/readers and independent-JVM logical streams.

Effective API descriptors are preserved. Explicit forEach changes bridge/synthetic and generic reflection metadata. Detached ValueSet streams passed with the same provider; stock-provider reading is unqualified. Inherited live asMap/keys entry cache policy remains open.

Three fresh JVMs per provider confirmed a local source-level result against the prior native M3 provider: Course Schedule WIDE allocation fell from 12,392.648 to 128.664 B/graph, with 3.0% greater median latency. All nine median latency ratios stayed within 1.15. No peak, online percentile, zero-allocation or final-JAR performance claim is made. Rejected variants remain preserved.

Current artifact: target-hash-multimap-stream-qualified-v1/guava-33.4.8-m3-java21-hash-multimap-stream-qualified-v1.jar.
Artifact SHA-256: 90e42d9d9eb7cb46b330905cde592b2424d1bf72f1b4b3e570cd07b58fd74b27.
Full report SHA-256: 15c93d13c756cde7ebfff246cc4f52ee3e70e211c1f0cdd29d7106eaf2d81fb8.
Canonical recipe/proof: https://github.com/hsoliwal/com.synexia/tree/c7a2de8816d2097c73125bc4b8e95227b3a83605/synexia-openrewrite-recipes/recipe-crates/guava-native-hash-multimap-v5-20261018.
Canonical draft: https://github.com/hsoliwal/com.synexia/pull/12465.

The 107-row catalogue retains HashMultimap owner -, proposed M3SetMultimap, PENDING. No M3 alias, java.base installation or superset completion is claimed. Autoreview remains NO_VERDICT after its mandatory no-tools refusal, without retry or bypass. Hosted CI/review remain pending.

Native Guava draft: https://github.com/hsoliwal/guava/pull/11 at f6d0341a226e7ba7781b702642efc6a918227fcc.
