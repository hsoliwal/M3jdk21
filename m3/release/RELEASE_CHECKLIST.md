# M3JDK21 release checklist (state as of 2026-10-30)

Source of the rows: `m3/release/audit-20261030/BLOCKER_QUEUE.tsv` (priority order) plus the steps
`m3/release/release-contract.json` requires. One line per step; the owner updates the State column
when a row lands. States: DONE (on master), PATCH_READY (commit in an open PR), PENDING_USER (needs
the repository owner's click), PENDING (no patch yet; the named lane owns it), NOT_RUN (not executed
in the 2026-10-30 shaping pass).

| # | Step | State | Where / evidence |
| --- | --- | --- | --- |
| 1 | Blocker 1: `main.yml` valid again (0-job startup failure on every master push since 2026-10-27) | DONE | master `05c7d1a` (remove-bundles `needs` repaired) + PR #2105 merged 2026-10-30T02:36Z; first green master run of main.yml not yet observed |
| 2 | Blocker 4: CI queue starvation (677 queued runs hid the superset state) | DONE | concurrency groups per PR #2106 landed with the superset; queue depth not re-measured in this pass |
| 3 | Blocker 5: `m3-first-release-admission.yml` runs on master pushes (receipts for M3_FOUNDATION_TESTS, PRECOMPUTE_*, M3_MAVEN_CONTROL_REACTOR, PROOF_PACK_SHA256) | PATCH_READY | PR `m3/release-blockers-20261030`, commit "blocker 5" |
| 4 | Blocker 6: intake policy installs the source-built `rewrite-java:8.17.1-m3-source-1` closure before `mvn verify` (streak 20) | PATCH_READY | same PR, commit "blocker 6" |
| 5 | Blocker 9: Backport Recipe DAG: custody `M3ScopeInference` re-pin + spectrum `CORPUS.tsv` regenerated at master `cda404e4` with its owning generator | PATCH_READY | same PR, commits "blocker 9 part 1/2"; the two hierdoc tests need com.synexia PR #14195 then a copy refresh; 33 other surefire failures in run 40879580255 stay M3jdk21-owned (`fixes/blocker9.README.md`) |
| 6 | Blocker 10: Synexia pin drift (A3 commit 404, original-provider SDK binding, M3String successor blob) | PATCH_READY | same PR, commits "blocker 10 part A/B/C" |
| 7 | Blocker 3: execution-evidence manifest `m3/release/queue/current/evidence.json` + receipts for the 10 mandatory gates | PENDING | gate M3_MAVEN_CONTROL_REACTOR is owned by hsoliwal/com.synexia; the other receipts come from the admission run on master (step 3); `release_ready` stays false in both modes until the manifest exists |
| 8 | Blocker 7: tokenizer image doclint `-Werror` on `java/util/regex/TokenMatcher.java:189,198,209` | PENDING | String/regex lane; three one-sentence Javadoc main descriptions |
| 9 | Blocker 8: `verify_donation.py` Tools4j predecessor rows (collection contracts streak 105) | PENDING | collections lane; no patch delivered on 2026-10-30 |
| 10 | Blocker 11: `m3-jni-common` windows-2022 C4310 cast in `m3_jni_core_contract.c:51` | PENDING | native lane (PRECOMPUTE_JNI_NATIVE_PARITY) |
| 11 | Blocker 12: `m3/ports/swing-viewport` `-Xlint:all -Werror` (this-escape, serial) | PENDING | viewport lane |
| 12 | Blocker 13: generated-mirror / frozen-recipe drift (`M3StateMaskHistogram`, `M3SemanticHasher`; `M3StreamProgress` already resolved on master `dbf2bbd3d52`) | PATCH_READY | same PR, commits "blocker 13 part a/b": successor recipe `state-mask-histogram-20261030` + 5 regenerated mirrors + workflow, `m3/compatibility` ledger re-pins + checker state |
| 13 | Blocker 14: Android and iOS JDK build lanes | PENDING | platforms lane; next release (`MOBILE_DONORS.tsv`) |
| 14 | Release workflow `.github/workflows/m3-release.yml`: tag `m3jdk-*` -> linux-x64, macos-x64, macos-aarch64, windows-x64 bundles, jmods zips, SHA256SUMS, rights files, draft GitHub Release | PATCH_READY | PR `m3/release-workflow-20261030`; checked with actionlint + `reusable_call_check.py`; never executed |
| 15 | OpenJDK product build + tier1 at the tag commit (OPENJDK_SANITY_MATRIX) | NOT_RUN | no local toolchain; CI evidence = `main.yml` on the push + `m3-release.yml` on the tag |
| 16 | Claims: the 7 upgradable claims stay NOT_CLAIMED; `immutable_text_foundation` = IMPLEMENTED_WITH_REPOSITORY_TEST_SURFACE | DONE | contract text; changes only through receiving-order receipts; its gate run on master arrives with step 3 |
| 17 | Release-mode admission (`verify_release_admission.py --release`) at the tag commit | PENDING | blocked by `git.single_release_commit` (master is ahead of `890adb64` by 15899+ commits) and `evidence.manifest` (step 7); the P0 preview tag therefore ships outside contract admission and its notes say so |
| 18 | Tag `m3jdk-21.0.14-m3.1-preview` on the master commit that contains this checklist (PR A merged) | PENDING_USER | commands in the PR A description; `DEFAULT_VERSION_UPDATE=14` in `make/conf/version-numbers.conf` |
| 19 | Publish the draft GitHub Release created by `m3-release.yml` after reviewing SHA256SUMS and RELEASE_NOTES.md | PENDING_USER | after the step 18 workflow run is green |
| 20 | Blocker 2 (contract route): `release/first-clean-boundary-20261007` as ONE fork commit above `890adb64` with a green admission run, then tag there | PENDING_USER | the branch exists at `277a81e` (2026-10-07, exactly one commit above the baseline, stale against master); re-cutting it is a non-fast-forward push only the owner performs |
