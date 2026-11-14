# JDK library convergence: collection receiving record

This directory is a passive qualification record for the five Java/JNI/test files emitted by canonical recipe `com.synexia.m3.JdkLibraryConvergence20261114`.

The canonical recipe, Maven crate, donor catalogue, histories, probes and complete evidence remain in [Synexia PR #17413](https://github.com/hsoliwal/com.synexia/pull/17413), at exact commit `351830384d50f86ec8620b025d44f7f52d09a286`. See the [canonical crate](https://github.com/hsoliwal/com.synexia/tree/351830384d50f86ec8620b025d44f7f52d09a286/synexia-openrewrite-recipes/recipe-crates/m3-jdk-library-convergence-20261114), [output manifest](https://github.com/hsoliwal/com.synexia/tree/351830384d50f86ec8620b025d44f7f52d09a286/synexia-openrewrite-recipes/recipe-crates/m3-jdk-library-convergence-20261114/OUTPUTS.json), [execution receipt](https://github.com/hsoliwal/com.synexia/tree/351830384d50f86ec8620b025d44f7f52d09a286/synexia-openrewrite-recipes/recipe-crates/m3-jdk-library-convergence-20261114/evidence/sdk/EXECUTION.tsv), and [collection replay instructions](https://github.com/hsoliwal/com.synexia/tree/351830384d50f86ec8620b025d44f7f52d09a286/synexia-openrewrite-recipes/recipe-crates/m3-jdk-library-convergence-20261114/collection-proof/PUBLICATION_README.md).

## Product change

The Java-default partial filtered snapshot uses the already known accepted count and copies selected object references directly. It removes an intermediate `int[accepted]` array and redundant mask-count scan. Full selected words use array copying; partial words enumerate set bits. Predicate order, exception identity, stable sorting, snapshot detachment and the all-pass path are preserved.

The existing opt-in JNI index routine gains dense full-word expansion, retaining its ABI, two-pass structure, bounded scratch space and flush boundaries. No native export or runtime dependency is added. The two product tests cover reference-copy contracts and known-count validation.

## Executed qualification

The executed output passed 11 portable product gates and 18 probe stages. Each Java/fallback/interpreter/C1 mode passed 2,632,902 assertions; actual JNI under `-Xcheck:jni` passed 2,650,978 assertions. Known-count checks passed 51 cases each in the Java and actual JNI modes. ASan/UBSan passed 8,132,052 checks. Leak detection was disabled because LeakSanitizer cannot operate in the traced environment. Full installed-image, jtreg and cross-platform qualification remain open.

Local public-path allocation measurements show 8,440 to 6,376 B/op for 1,024 entries / 512 accepted, and 789,400 to 529,912 B/op for 65,536 / 64,867. The all-pass allocation result is unchanged.

Dense native timing ratios were 0.527-0.769 of baseline in the recorded local replay. A sparse case initially measured 1.206; one focused 31-pair CPU follow-up measured median 1.034661 with substantial variance. Native remains opt-in. These observations do not establish broad native speedup or no regression. The canonical evidence retains both results and earlier rejected candidates.

## Source and history preservation

Runtime qualification used `7409479266ddb37c29182550e6354665066d9ff6`. The receiving commit is based on `ab6ef923dca4d32e1f003db71a800138911b5ffa`, preserving the intervening PR #2981 naming-record recovery. All relevant runtime and support inputs were verified unchanged. [Base advancement evidence](https://github.com/hsoliwal/com.synexia/tree/351830384d50f86ec8620b025d44f7f52d09a286/synexia-openrewrite-recipes/recipe-crates/m3-jdk-library-convergence-20261114/evidence/sdk/base-advancement.json) records that check.

`RECEIPT.json` identifies every exact output and its SHA-256/Git blob, the canonical execution and result seals, and remaining promotion gates. This receiving record contains data and documentation only; executable authoring, convergence and qualification owners remain in Synexia.
