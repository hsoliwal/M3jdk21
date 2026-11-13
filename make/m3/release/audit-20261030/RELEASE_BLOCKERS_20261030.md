# M3JDK21 release-blocker audit — 2026-10-30

Read-only audit. No code changes, no merges, no PR. Owner session (com-synexia-d9) commits these files on its superset branch.

| Item | Value |
| --- | --- |
| master head | `5860598b515885ebc2275f6552d9a5a1348fdb15` (2026-10-29T17:00:44Z, "Merge packed integer and VInt SDK APIs") |
| superset head (PR #2151) | `ce9d68bcfc38a2f5d5f5f0fb19e6656ee77e71dc`, branch `m3/superset-landing-20261030`, 3706 commits ahead of master, merge-base = master head |
| upstream baseline (contract) | `890adb6410dab4606a4f26a942aed02fb2f55387` (openjdk/jdk21); merge-base check PASS on both refs |
| audit clone | `S:/m3-scratch/worktrees/m3jdk21-release-audit` (blobless, sparse: m3/release, m3/collections, m3/mop, m3/tooling, m3/ports, m3/docs, m3/arrays, .github; left on branch `superset-landing`) |
| GitHub Releases / tags | 0 / 0 |
| OpenJDK make build | NOT_RUN locally (no toolchain); CI equivalent is invalid (below) |
| API budget | 91 REST calls + 4 GraphQL (cap 300) |

## Files

| File | Rows | Content |
| --- | ---: | --- |
| `CONTRACT_CLAIMS.tsv` | 24 | every `current_claims` entry, mandatory gate and tornadovm option with its gate and state, master and superset |
| `CI_RED.tsv` | 13 | red workflows per ref with BLLSE record (job, step, file, line, severity, message, class); 11 master, 1 superset, 1 queue-starvation row |
| `COMPILE_BLOCKERS.tsv` | 54 | 27 modules x 2 refs: javac `--release 21 -Xlint:all,-module -Werror`, error/warning counts, first error, failure class |
| `PLATFORM_MATRIX.tsv` | 10 | per platform: workflow, job, runner, last result master/superset, last green anywhere, donor rows |
| `PACKAGING_GAPS.tsv` | 10 | required vs produced artifacts (release, bundles, jmods, SHA256SUMS, notices, SBOM, evidence manifest, benchmarks) |
| `MOBILE_DONORS.tsv` | 10 | Android/iOS donor rows in the owner-ledger column layout (append-ready; no duplicates of rows 582-586) |
| `BLOCKER_QUEUE.tsv` | 14 | prioritized blockers with owner lane, evidence line, proposed fix PR/recipe |
| `evidence/` | — | admission JSONs (4), contract diff, grouped CI tables, raw run lists (700 + 384 runs), failed-step logs (13), donor GraphQL responses, `compile_audit.py` |

## Contract state (verify_release_admission.py, python -I)

| Ref | Mode | release_ready | Non-PASS checks |
| --- | --- | --- | --- |
| master | development | true | none (23 checks) |
| master | --release | false | BLOCK `git.single_release_commit` ahead=15899 parent=0ab28ea86ccb |
| superset | development | **false** | HOLD `git.single_release_commit` ahead=19605; HOLD `legacy_recipe_custody` (810 candidates); PENDING `evidence.manifest` (m3/release/queue/current/evidence.json missing); PENDING all 10 mandatory gates "no current execution receipt" |
| superset | --release | false | same |

All seven upgradable claims are `NOT_CLAIMED` on both refs; `immutable_text_foundation` is `IMPLEMENTED_WITH_REPOSITORY_TEST_SURFACE` but its gate workflow (`m3-first-release-admission.yml`) has never run on a master push (trigger = PR paths / `release/**` / dispatch). `test_release_admission.py`: master 4/4; superset 27/28 (one Windows-only NTFS failure, `extra\nquoted.java`).

## CI (gh run list, 700 master runs across 19 workflows; 384 superset runs across 345 workflows)

Master: 11 workflows red at their last run. Longest streaks: M3 collection contracts 105, M3 Synexia intake policy 20, M3JDK21 Backport Recipe DAG 15, M3 original mapping provider 13, M3 A3 absorption proof 13, M3 OpenRewrite recipe proof 11, OpenJDK GHA Sanity Checks 7.

Superset: 1 completed failure (OpenJDK GHA Sanity Checks, 0 jobs), 1 success, 342 queued, 2 pending. Repo-wide 677 queued / 2 in progress at 01:36Z: superset CI state is NOT_AVAILABLE.

`.github/workflows/main.yml` ("OpenJDK GHA Sanity Checks") fails before creating any job on both refs ("This run likely failed because of a workflow file issue"). Static checks pass locally (reusable-workflow inputs, `needs`, 23 local action refs); the annotation text needs a logged-in UI. Last green: run 39250083857 at `bb6b279495ce` (branch m3/upstream-sync-20261019-jdk21u-head, 2026-10-19) with every linux/macos/windows/cross build and macos-aarch64 tier1 green. The 14 CI files differ from that head by +185/-201.

## Compile (JDK 21 javac, both refs identical)

12 PASS (m3/arrays, m3/collections 298 files, m3/mop, module-packs/kernel, ports/precompute, parallel, vcpu, capability-runtime, collections, file, indexstring, regex-cursor) + 3 vendored ASM preps. 10 FAIL: 5 external dependencies not on the probe classpath (jackson, lucene/opennlp, openrewrite x2, javafx, tornado-api), 2 need the built M3JDK21 image (`jdk.internal.vm.parallel`: ports/parallel-bulk, ports/tornadovm-bulk), 2 intra-repo closure (parser, nlp-search), 1 genuine lint blocker (ports/swing-viewport: this-escape + serial under -Werror). 2 PROBE_SCOPE rows (build-support sources live under upstream/ subtrees; module-packs/collections is module-info only). No source-level compile error exists in the audited trees on either ref.

## Platforms

Desktop (linux x64/x86, macos x64/aarch64, windows x64/aarch64, cross aarch64/ppc64le/riscv64/s390x/arm) all depend on main.yml, which is invalid. Android and iOS have no JDK build lane; `m3-jni-common.yml` only compiles C11 headers with the runner NDK (Android, green once on a feature branch) and cross-compiles C sources for iOS (never green; cancelled). PR #2105 (native macos-15 arm64, windows-2022, retained SHA256SUMS bundles) is open; its preview run 40853790908 has been queued since 2026-10-29T09:58Z with 0 steps executed.

Mobile donors: the requested `openjdk/mobile` branches `ios-port` / `android-port` and any `openjdk/jdk-sandbox` mobile branch do not exist on GitHub (only `master` c1ed06aaef34); `aosp-mirror/platform_libcore` does not exist (ROM forks LineageOS/GrapheneOS do); BellSoft and ojdkbuild publish no mobile source; `gluonhq/graal` (22.1-gluon) is the only public iOS/Android SubstrateVM target source. Details in `MOBILE_DONORS.tsv`.

## Top blockers (full list in BLOCKER_QUEUE.tsv)

1. main.yml invalid on master and superset: no platform build/tier1 evidence (run 40501206433, 40983950894).
2. No release branch/tag/Release; `--release` mode blocks on single fork commit (ahead 15899 / 19605).
3. Superset contract demands an execution-evidence manifest and receipts for 10 gates; none exist; 810 legacy recipe candidates HOLD.
4. Queue starvation (677 queued) hides the superset CI state.
5. Admission workflow never runs on master pushes.
6. Control reactor cannot resolve `org.openrewrite:rewrite-java:8.17.1-m3-source-1` (streak 20).
7. Tokenizer image build: doclint -Werror on `java/util/regex/TokenMatcher.java:189,198,209`.
8. Collection contracts streak 105: `verify_donation.py` Tools4j predecessor rows.
9. Backport Recipe DAG: 4 JUnit failures in Synexia recipe tests.
10. Synexia pin drift/404 (A3 commit a062661e 404; binding drift; M3String successor blob 35ed9985 != 8a57ca4f).
