<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 Collections canonical recipe receiving — wave 2 (2026-10-18)

Canonical source custodian: [Synexia PR #12246](https://github.com/hsoliwal/com.synexia/pull/12246).
Receiver base: `f29b0afe7ad253cc0562bc8dfdd48fc1acec710e`.

This fork's `canonical-wave2-source-custody-20261018.tsv` pins 49
original M3JDK21 source and fixture Git blobs, including ten first-party
Java 21 Maven source-installer recipe implementations now also in Synexia.

The `canonical-wave2-receiver-20261018.tsv` review compares 39
template/fixture assets to this pinned fork: 24 EXACT_PRESENT,
five EVOLVED_PRESERVED, and ten not directly byte-comparable.
Preserve the newer `M3CountedBag.java`, `M3IndexedListSnapshot.java`,
`M3MutableFilteredViews.java`, `M3CollectionSdk.java`, and
`M3CollectionSdkSelfTest.java`. A source-sealed template is not
permission to revert these implementations.

Recipe application must check the current receiver first, refuse
unrecognized sources, execute only compatible postimages, replay a
second fixed point, and verify no tracked or untracked product drift.
The attached Java 21 workflow is an isolated selftest and read-only
eligibility check. Real source-changing target application and
JNI/HotSpot/jtreg/reactor promotion remain separately gated.

Retain local recipe mirrors pending canonical Maven/OpenRewrite CI.
Do not relicense EPL/OpenJDK donor code or rebase Synexia develop.
