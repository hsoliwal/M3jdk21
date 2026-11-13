# Hosted source CI audit: published recipe qualification

This evidence records hosted CI for [com.synexia PR #9479](https://github.com/hsoliwal/com.synexia/pull/9479), exact source head `bc06298218dd88f0164ed9b67e74fd8234ed70b2` and root tree `a72565e0e67f330f12c712dcc7a3fcbe1d80d24e`. The snapshot audit is dated 2026-10-06 06:12:48 UTC. It does not evaluate the receiving M3 commit.

The [report](snapshot/publication/hosted-ci-audit/REPORT.md) and [machine-readable summary](snapshot/publication/hosted-ci-audit/SUMMARY.json) preserve the observed result: 176 exact-head workflow runs, 169 failed and seven queued, with no successes. All 169 failed workflows had zero check runs. The three sampled Java/reactor, recipe-first, and JNI-proof runs each returned zero jobs. Their precise pre-job rejection reason was not exposed. The evidence does not establish billing, validation, platform, compiler, or test causation.

Finite local recipe/compiler/JUnit qualifications remain separately scoped evidence. Hosted CI, full-repository, native, and JDK admission are not promoted by this audit. Read-only endpoint limitations and the unsuccessful bounded public-web retrieval are preserved. No workflow dispatch, rerun, cancellation, account setting, or source change was made by this audit.

## Evidence closure

The `snapshot` directory preserves every original input file enumerated by [EVIDENCE_MANIFEST.json](snapshot/publication/hosted-ci-audit/EVIDENCE_MANIFEST.json), plus that unchanged manifest itself. Paths in that manifest are relative to `snapshot`. Original request/response capture bytes and hashes are preserved; nested copies inside tool responses have not been compacted or rewritten. The original `build_report.py` is retained as part of the evidence. It can reproduce the original report and summary using the captured responses without network access.

The later check-suite census contains one additional failed GitHub Actions suite beyond the earlier 176-run workflow census. The report records these successive snapshots explicitly. `run_started_at` alone is not used as evidence of runner execution.
