<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 java.util.CollectionViews — filtered one-shot/COW receiving candidate

Exact current master preimage `b032048eece51c897d8623fd7a69f7f16eac5a57` and original blob `2fe6e7ede2af18d4523f3a5531038e9a865cdb6d`.
Canonical Synexia recipe `com.synexia.m3.CollectionViewsOnePassCowSuperset20261023` on branch `codex/m3-collectionviews-recipe-20261023`, source commit `3fe010e384e2b0e6aecaa0e770b40bd546152949`.
The public target contains no recipe, Maven crate, template or external dependency.

Two file-local behavioral atoms in the original `java.util.CollectionViews` owner:

- `addAll`: validates every element once from non-repeatable input; stages a temporary `ArrayList` of admitted references before the owner is mutated. An invalid occurrence rejects the entire candidate batch before a source write.
- `removeMatching`: delegates exactly built-in `CopyOnWriteArrayList` and `CopyOnWriteArraySet` owners to their `removeIf`, preserving COW snapshot iterator contracts. All other backing collection classes retain the original iterator mutation path.

Live filter/sorted/navigable interfaces, JDK method descriptors, physical owner, exception behavior at existing fallback, nulls and duplicate references are unchanged. No new retained Node/Entry/Cell, no JNI exports; checked primitive JNI selectors remain the native owner. LeetCode/HackerRank/GeeksForGeeks supply category evidence only; no challenge code or third-party collection source copied.

**Gate:** draft/HOLD. Pre/post code hashes and literal replay are source evidence; OpenRewrite SDK, Maven, full java.base/jtreg, ABI/JNI/GC/CDS and performance are separately required and not claimed here.
