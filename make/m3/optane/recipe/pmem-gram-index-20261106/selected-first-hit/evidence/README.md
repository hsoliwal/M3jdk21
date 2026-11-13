# PMEMSDK — selected-file first-hit search (recipe first)

This is a two-commit additive Synexia patch to be applied on the exact appropriate canonical PMEMSDK source-campaign PR branch. Commit 1 adds the versioned canonical Maven/OpenRewrite recipe and proofs; commit 2 materializes **only** the two reviewed Java/JUnit postimages.

The original `PmemCorpusSearch` public API, JNI ABI and file-local KMP are unchanged. The new **package-private** `PmemCorpusSelectedSearch` accepts a sorted unique array of already reviewed source file indices and dispatches only those file searches to the existing `PmemSourceImage.indexOf` native/FILE implementation.

## Contract

- The chosen source set is immutable/defensively copied and content-bound by `selectionSha256()` (original source-root + canonical indices). The binary pattern is also SHA-bound. Neither data array escapes.
- `Result.complete()` means *every selected file* was considered, not all repository files. `complete=false` prevents downstream code interpreting a budget-exhausted result as proof of absence. No result is returned after cancellation.
- Excluded files are not searched or semantically eliminated. Source immutability, actual model context capacity, catalogue/quality completeness, donor license review, and contract validation remain with their pre-existing owners. Search hits alone grant no code-copy, inference or rewrite authority.

## Running

On a full hsoliwal/com.synexia checkout:

```bash
bash synexia-openrewrite-recipes/recipe-crates/m3-pmem-selected-first-hit-20261102/verify-java21.sh
M3_USE_PMDK=off mvn -B -ntp -f m3-pmem-sdk/pom.xml verify
mvn -B -ntp -f m3-pmem-sdk/campaign/pom.xml -Dtest=PmemCorpusSelectedJUnitTest -Dpmem.corpus.selected.native=true test
mvn -B -ntp -f synexia-openrewrite-recipes/recipe-crates/m3-pmem-selected-first-hit-20261102/pom.xml verify
mvn -B -ntp -f m3-pmem-sdk/campaign/pom.xml -Dtest=PmemCorpusSelectedJUnitTest -Dpmem.corpus.selected.scale=true test
```

`verify-junit-source.sh` runs only a test-only surrogate JUnit/FILE host; it is **not** the genuine Maven/JUnit acceptance gate.

## GitHub status and PR safety

The intended parent is the existing PMEMSDK source-campaign chain culminating in `hsoliwal/com.synexia` PR #15383. The original source SHA must equal the pinned hash before applying. Do not rebase or force-update `develop` and do not merge or publish to result-only M3JDK21/Eclipse forks until full review and actual acceptance pass. The patch is prepared locally; no remote PR is claimed.
