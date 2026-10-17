<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Filtered and sorted wrappers: qualified source, explicit target gates

M3CollectionViews and M3FilteredMaps reuse Synexia PR11813/11865 and the direct-owner membership/removal atom of PR11824. Additive separately-built Apache-2.0 M3 module; public JDK names and existing storage owners stay unchanged. No Synexia runtime dependency.

## Contracts

Collection/Set/SortedSet/NavigableSet and Map/SortedMap/NavigableMap filters preserve runtime capabilities. All live views retain caller-owned storage, with nested predicates evaluated inner then outer. Hidden entries survive explicit/bulk removal. No copied index or retained per-element adapter store. Sorted membership tests stored comparator-equivalent representatives; ordinary membership delegates to the backing owner and requires predicate stability under its equivalence. Identity membership/removal is tested, not universal identity-Set equality/hash.

Bounded/reversed navigation accepts probes outside the range; illegal range construction/writes remain illegal. Traversal entries enforce predicate admission through arrays, streams and callbacks; navigation/poll returns immutable entry snapshots. Filtered iterator.remove is deliberately unsupported. Bulk removal additionally needs the owner's removable iterator. Collection addAll prechecks predicates; map putAll remains prefix-update. Nulls require both owner and predicate admission. Concurrent, weak-key, effectful-predicate and serialization contracts are not inferred.

Outer JDK read-only/checked/synchronized wrappers and real Eclipse/Spring/Guava adapters compose with these views. Shared access requires one external common lock, including iteration. Existing primitive filtered/sorted work in PR978/981 is separate and not duplicated.

sortedSnapshot creates an independent read-only stable-sorted list, retaining duplicate element references. The receiver reads each source element once instead of calling the filtered toArray/size prepass. Dense ArrayList is reused; growth/spare capacity can cost copies. No throughput or memory-superiority claim. Snapshots do not deep-copy mutable elements. Live stream cardinality stays unknown; no eager predicate count.

## Recipe-first reproduction

mvn -B -ntp -f m3/collections/wrappers/recipe/pom.xml test
mvn -B -ntp -f m3/collections/wrappers/recipe/pom.xml -Dm3.wrappers.target="$PWD" test

The POM references the already retained hash-pinned Java/text OpenRewrite providers; no engine is copied or modified. Seven Java and five native/document/data targets are sealed SDK outputs. Exclusive checkout ownership required; per-file writes are not a multi-file transaction. Fix recipes/templates and regenerate, never patch output.

The native RunSearchJni peer is reused unchanged as an independent TEST-ONLY navigation oracle. It releases JNI array access within every call; it is not a product generic native map/filter accelerator. Inherited production count/scalar JNI and BiMap suites are retained.

Generic map stored-key hits can scan O(n); filtered size scans; stable sorting is O(n log n). Full provider signature/allocating-return closure, all purpose wrappers, concurrency/reference/serialization, benchmarks, full reactor/jtreg/installed image and String -> arrays -> collections phase promotion remain separate gates. No queued workflow is passing evidence.
