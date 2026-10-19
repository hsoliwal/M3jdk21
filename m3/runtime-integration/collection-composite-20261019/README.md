<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Integrated M3JDK21 collection composition — candidate for review

Product: M3JDK21. Parent: PR1447, ffc08348874ffcf3037d1c0dcf02f7685945daea.
This packet preserves that branch's existing java.util.Collections/M3CollectionViews/
M3CollectionMasks/libjava implementation. It proposes direct per-file implementation
changes, not an M3-plus-JDK wrapper or required Synexia runtime.

CANDIDATE.patch exposes the complete exact13-path review diff: seven production files,
two modified tests, and four added tests/probes. Public donor outputs come from PRs1451,
1444 and1445. SYNEXIA_HANDOFF.json pins the original source blobs and pre/post SHA-256.
No production output has been applied in this PR.

The canonical Maven/OpenRewrite composition and fixtures are in
[Synexia PR12474](https://github.com/hsoliwal/com.synexia/pull/12474), exact commit
5e972536d98ac7f4aab37fb66333ed4696584bdd, crate
synexia-openrewrite-recipes/recipe-crates/m3jdk21-collection-composite-20261019.
Reuse its existing Java/text recipe engines. Run their actual named tests and
materialize SDK Results to fresh bounded scratch; verify the handoff then apply
those results serially to the exact target. Never hand-patch generated source.

The refinements cover interior-null filtered navigation, stable equal-value run
ordering on existing index arrays, recursive sorted split comparator metadata,
bounded native row spans, signed-stride state histograms and binary-search membership.
No public descriptors, JNI symbols or retained per-entry stores are introduced.
Original per-file Apache/OpenJDK licenses remain authoritative.

CATALOGUE.tsv records the problem-category comparisons. The tools4j shortstring
donor remains reference-only: its format does not equal the unsigned base36 IDE codec.
No previously private IDE code is published and no unsupported codec substitution is made.

This is a draft review packet, not completed implementation or runtime qualification.
The recipe-first gate requires proven output before production application.
Composite Maven/OpenRewrite, Java21/JNI/API/ABI, resource/performance, formal review,
full installed M3JDK21/jtreg/platform and wider file/family gates remain open.
SCOPE.tsv explicitly retains the full user request; this13-file pass does not close it.
