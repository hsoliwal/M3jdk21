<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 filtered List — lazy cursor correction (2026-10-17)

Receiver parent: M3JDK21 #1021 source at
`99ddda2ac83cd78cddd96df1937136d59d2628ad`.
Canonical Synexia parent: #11989 at
`a636a5de77fd6da141307fe6d9938e32dd2097e0`.
Canonical reusable correction: Synexia #12007,
`com.synexia.rewrite.M3Jdk21FilteredListLateBinding20261017`.
Original Synexia donor: #11977 `dcc01cbf67317709e8dfad123acbfc5d187699ad`
Apache-2.0, Git blob `ab51de6af292a2286ef3326398994a89b50725fc`.

This corrective FILE atom retains `M3FilteredLists`'s source/predicate
members and JDK interfaces. A `spliterator()` or `stream()` call is not
a traversal; `View.this.iterator()` must be created only inside the first
valid `tryAdvance`. Before binding: backing edits are observable without
stale iterator CME. After binding: retain the backing JDK ListIterator's
fail-fast behavior. No `SIZED`, `SORTED`, `DISTINCT` or `SUBSIZED`
claims, no split-buffer creation, no extra per-value structures.

Original source SHA-256:
`7a9d818d38beae4df178317f7913050b47d6442e85f1ef5d5873a0df89262101`.
Reviewed corrected source SHA-256:
`94e3976440ee1cec2477e73d7a77caeda2523969d53a9407a03b8adbd5d765d9`.
Original public `filtered` and `readOnlyFiltered` signatures remain
unchanged. JDK21 List/SequencedCollection remains the reference contract.

Verification packet has deterministic 512-round pre-terminal edit histories,
late-bound readonly, nested/reversed filters, invalid action refusal,
ORDERED-only metadata and explicit post-bind fail-fast checks. The original
source must fail a frozen RED witness and the corrected template must pass
GREEN before target qualification. Source SHA-256, Maven/OpenRewrite SDK
fixed point, drift refusal, exact-head Java21/JUnit and installed-runtime
proof are separate. No new JNI symbols; inherited primitive JNI parity
remains a distinct gate. No String → arrays → collections phase advance.

The prior name-map record remains as historical provenance and is extended
with an explicit source-sealed transition; existing mapping entries retain
their original serialized content. LeetCode/HackerRank/GfG challenge-shaped
cases are problem taxonomy only, not copied implementation code.
