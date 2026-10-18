<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 five-library indexed materialization receiver

Canonical Synexia PR: https://github.com/hsoliwal/com.synexia/pull/12207 at exact source commit `01b8eadb479b0a15a749f8ff65b2b84646e80594`. This reviewable receiver is stacked upon M3JDK21 source-census PR #1212 at exact base `b1fdd73bf68e045a711177871952de8973b1a280` and preserves the Apache BidiMap predecessor PR #1210.

One existing M3 snapshot class acquires eight new typed materializations from the same array/index/state representation. JDK Set; Guava BiMap, Multiset, Table; Eclipse ListMultimap/SetMultimap; Apache MultiSet; Spring last-value MultiValueMap. Nothing replaces the public `java.util` or vendor classes; results implement their real original library interface through the already present M3 adapters. No fresh payload owner, per-occurrence Entry/Node hierarchy, or JNI ABI. Sorted and filtered row facts remain immutable; each resulting provider object is independent and mutable per its API.

**Four source-sealed postimages** must be created by the canonical OpenRewrite recipe: M3FiveLibraryIndexViews.java, its expanded seeded real-provider JUnit oracle, full 34-file source-census exact digest receipt, and its SHA-256 guard. All four before/after identities are in `SOURCE_RECEIPT.tsv`; the earlier five history identities and all 34 allowed source names remain. Maven/OpenRewrite fixed point, explicit drift/missing/duplicate refusals, actual five-library JUnit and checked native C17 are required CI gates.

This is a bounded contract-composition contribution to the requested comprehensive SDK. It is not proof of all upstream methods, JDK installed image, comparative speed, or replacement of existing live filtered wrappers. Keep DRAFT until hosted exact-head evidence passes.
