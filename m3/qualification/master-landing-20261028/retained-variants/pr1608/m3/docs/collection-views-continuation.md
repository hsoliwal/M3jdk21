<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Filtered and sorted collection continuation

M3JDK21 owns the receiving implementations. Synexia owns reusable recipes,
algorithm comparisons and correction memory. This pass refines the existing
owners without adding a runtime integration layer or another payload store.

M3FilteredViews restores unconditional read-only Java 21 endpoint refusal,
including empty, bounded and reversed sorted sets. It reuses the existing
private reversed guard and the JDK default traversal. This is a documented
defect repair: 218 retained read-only checks failed before and now pass.
Comparator, null, stored-representative, live-view and read behavior remain
covered by the existing frozen contracts; public factory descriptors do not change.

M3PrimitiveIndexView evaluates each predicate/state once per logical occurrence.
It records selection in packed primitive words only after the first rejection,
then emits exact row indices in order. Full selection shares the private
immutable row array behind a fresh view. Exports remain defensive, generation
checks retain their order, signed masks retain their existing meaning, and
sorting/top-K/slicing still use the existing index owners. No retained per-row
Entry/Node or selection bitmap is added.

The existing collection pack now compares all JNI exports with javac-generated
headers, requires the legacy selector, refuses missing/extra symbols, and runs
15 contract mains in mixed/interpreted modes on both source-built and linked
M3JDK21 runtimes. Test classes alone use module patches. The existing full
collection runner also passes all ten native libraries with fatal UBSan and
checked JNI: 465 owning tests, no failures/errors/skips in its required-native
run; 19 explicit native contracts and missing-library checks. Four module-pack
JUnit tests pass using repository-source-built dependencies.

The primitive change reduces allocation; it is not uniformly faster. At 65536
rows, three-fork medians allocate 8256/73792/32 bytes per none/quarter/all-selected
call versus 262208/327744/262192. Recorded CPU is 3–24% slower on these workloads.
Root plus 64 all-selected views retain 262144 row-payload bytes versus 17039360;
these counts exclude object headers and shared store lanes. The catalogue
retains this tradeoff, earlier candidates and raw measurements.

Canonical source: [Synexia PR12609](https://github.com/hsoliwal/com.synexia/pull/12609),
`synexia-openrewrite-recipes/recipe-crates/m3-runtime-progress-facade-20261018`,
task `collection-views-20261021`, recipe
`com.synexia.m3jdk.CollectionsContractRefinement20261017`, final dataset
`collection-final-20261021`. Immutable correction memory is sealed into SDK
inputs and checked before application. Read CATALOGUE.tsv and DISCOVERY.md
before another pass; do not replay older templates over stronger current code.

See collection-views-qualification-20261021.json for exact receiving hashes and
gates. Global phase remains STRING. These receipts do not establish completion
of all collection providers, all parsers, cross-platform VM/JNI admission,
full jtreg or automatic repository-wide MOP capture.
