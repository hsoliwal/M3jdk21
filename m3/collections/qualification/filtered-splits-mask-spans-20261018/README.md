# Filtered split ordering and bounded JNI payload spans

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

This receiving pass improves existing collection owners while retaining their public APIs.
Filtered sorted/navigable sets now carry their comparator onto every split child, including
descending/range views and mutable map key sets. Comparator lookup remains lazy; predicate
calls, iterator batching, size queries, backing behavior and mutation policies are retained.

The primitive state-mask JNI scanner records relative ordinals in a fixed 128-byte stack
buffer and transfers only the enclosing row span needed by the output window. Stable order,
payload identity, offset guards, masks, bounded progress and both native entry points are
unchanged. Arbitrary Java predicates and comparators remain in Java.

Canonical owner: `hsoliwal/com.synexia`, recipe
`com.synexia.m3.FilteredSplitsMaskSpans20261018`. `RECEIVER.json` binds its immutable commit,
source/template hashes, target preimages and proof. No recipe implementation is forked here.
`CATALOGUE.tsv` is the exact handoff catalogue: category, candidate, pinned donor, license,
cost, decision and validation. Search categories with `rg`, and add future comparisons to
the canonical Synexia crate before changing another owner.

| Category | Existing owner | This pass |
| --- | --- | --- |
| Sorted stream boundaries | M3SlotSpliterators | Reuse comparator adapter; retain lazy getter on split children |
| Readonly filtered views | M3FilteredViews, M3FilteredNavigableSet | Delegate sorted splits to that adapter |
| Mutable filtered views | M3MutableFilteredViews | Same split correction, including navigable map key projections |
| State-mask array windows | M3IndexMaskWindow and native scanner | Trim JNI payload spans; retain Java scalar oracle and ABI |
| Dynamic arrays, projections, other masks | Existing collection owners | Preserved; no blanket replacement or completion claim |

Run the current-tree proof with Java 21, Linux C17 and the standard JUnit console:

```sh
JAVA_HOME=/path/to/jdk21 JUNIT_CONSOLE_JAR=/path/to/junit-platform-console-standalone-1.12.2.jar \
  python3 m3/collections/qualification/filtered-splits-mask-spans-20261018/verify.py
```

The runner compiles every current collection production/test source, then runs the selected
contract suite, actual checked JNI, fatal UBSan, native transfer/fault probes and missing
library refusal. It does not download dependencies. The shared collections workflow adds
this proof as an independent job and verifies the checked-out PR head SHA.

See `LOCAL_VERIFICATION.md` for executed gates and limitations. Source-count assertions from
Synexia PR #8896 are not established by these collection tests. No JDK class, runtime phase,
third-party license or whole-corpus acceptance is changed by this handoff.
