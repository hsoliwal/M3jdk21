# Published source CI audit

PR: https://github.com/hsoliwal/com.synexia/pull/9479
Head: `bc06298218dd88f0164ed9b67e74fd8234ed70b2`
Snapshot audit: 2026-10-06 06:12:48 UTC.

## Finding

Hosted CI has not passed. The two-page exact-head workflow census contains **176 distinct runs: 169 completed with failure, 7 queued, and zero successes**. All 169 failed runs map to GitHub Actions check suites with **zero check runs**. The three representative job-list calls each returned `total_count: 0` and an empty jobs array. These responses establish failure before any job is recorded for those samples; they do not identify a source compilation or test failure.

**The precise rejection reason remains unestablished.** The returned run and suite JSON contain no failure message. It would be unsupported to label these failures as billing, workflow validation, platform outage, or a production code regression. No such classification is made.

## Representative runs

| Run | Workflow | Created / updated (UTC) | Jobs | Check runs |
|---|---|---|---:|---:|
| [37422112578](https://github.com/hsoliwal/com.synexia/actions/runs/37422112578) | Java 21 Serial Reactor Progress | 2026-10-06T06:08:20Z / 2026-10-06T06:08:20Z | 0 | 0 |
| [37422112682](https://github.com/hsoliwal/com.synexia/actions/runs/37422112682) | M3 recipe-first invariant | 2026-10-06T06:08:20Z / 2026-10-06T06:08:21Z | 0 | 0 |
| [37422112648](https://github.com/hsoliwal/com.synexia/actions/runs/37422112648) | M3 native bulk count recipe and JNI proof | 2026-10-06T06:08:20Z / 2026-10-06T06:08:21Z | 0 | 0 |

The Java 21 sample also returns `billable: {}` from its timing endpoint and an empty decoded log response. There is no job ID from which to request compiler or test logs. `run_started_at` is present even on queued runs and must not be treated as evidence that a runner executed.

## API boundaries and remaining checks

The refreshed commit check-run collection contains 20 entries, all queued and all with zero annotations. None belongs to the three failed sample suites. Direct suite and suite/check-runs requests were rejected by the connector with HTTP 400 (`GitHub Fetch URL is not an allowed public GitHub repository or search endpoint`). The later commit-level two-page suite enumeration succeeds and contains 179 suites: 170 failed GitHub Actions suites with zero check runs, seven queued GitHub Actions suites, and two queued suites for other apps. All 176 previously enumerated workflow runs have a corresponding suite. The extra failed suite is recorded separately because these calls are successive snapshots.

A bounded public-web open of the same three run URLs returned `DisabledError` for each page; no failure annotation was exposed. No authenticated browser was used.

No full-repository or JNI hosted acceptance is established. Existing finite local recipe/compiler/JUnit results remain separately scoped evidence. Diagnosing the exact host rejection requires a surfaced GitHub run failure annotation or another supported authorized read of that diagnostic. No workflow, source file, branch, PR, or account setting was changed during this audit.

## Evidence

`SUMMARY.json` provides machine-readable findings. `EVIDENCE_MANIFEST.json` hashes the request/response captures and report inputs, including the original first-page census and status captures from the root publication directory. Unsupported endpoint responses and the empty log response are retained.
