# A3 replay closure

Two existing files change: the A3 source-sealed recipe and its original JUnit class. No template, manifest, public signature, POM, dependency, suppression, coverage include/exclude or canonical branch is changed.

Real Maven compilation reproduced a Tree/SourceFile inference failure at withFileAttributes. Typed local assignments preserve the existing setter order. Real scheduler execution then exposed four generated exact ABSENT files incorrectly rejected by the edit visitor. The fix reuses the admission distinction already present in the repository's hash-pinned snapshot owner: scanned files must match their scan; unscanned files must be exact Java ABSENT postimages. Missing required owners, drift, duplicates and wrong source kinds still fail closed.

Eight real JUnit tests pass, retaining the original three and adding lifecycle refusal, generation, mixed-state resume and non-target cases. Metadata tests now carry nonempty markers, file attributes, nondefault charset/BOM and an obsolete checksum. Existing markers are preserved and separately checked against the actual scheduler's RecipesThatMadeChanges provenance marker; only the stale checksum is removed.

The recovered dependency kit is genuine Maven3.9.16/OpenRewrite8.17.1/JUnit. All current selected owner/test/resource preimages were checked. The recovered full source is b0c2e495b8465dc802ef309c06810844b04e08e9, not complete master db2017c1aa4aace99543417fba79e3f7a307c26b. Independent full archived module execution reached 124 tests, with 3 failures and 2 errors in verbatim descriptor and String/JNI recipe tests; those failures remain recorded, not skipped. An earlier full verify attempt hit the execution time budget; the later actual module test run finished and failed normally.

A3 owner coverage was measured at 92/101 lines and 48/56 branches in the accumulated local JaCoCo run; it is not a 99% or isolated-test coverage claim. Existing POM thresholds remain unchanged. Full current-head reactor, whole-JDK build/jtreg, compatible backports, source normalization, String/JNI/GC/CDS, desired modules and platform acceptance remain separate unfinished obligations.

This candidate has no promotion authority. Source and logs are evidence; the PR's merge state is not test proof.
