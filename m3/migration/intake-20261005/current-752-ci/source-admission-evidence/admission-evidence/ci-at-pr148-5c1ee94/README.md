# M3 PR 148: bounded exact-head CI review

At the recorded 2026-10-05 inspection, head `5c1ee9490e76943dfdb3ca27fade0d093d017226` has real executed CI failures with actionable diagnostics. This review freezes three representative failed jobs; it does not inspect or diagnose every workflow.

The pull-request-filtered workflow wrapper returned 11 runs on its first page: 9 failed and 2 succeeded. The wrapper did not expose a total count. The exact-head check-runs page returned all 33 reported checks: 19 failed, 3 succeeded, 3 skipped, and 8 queued. That collection includes checks outside the wrapper's pull-request-only result. The combined legacy status response contains no statuses and does not override these check results.

| Selected workflow | Failed step | Concrete diagnostic |
| --- | --- | --- |
| [OpenRewrite recipe proof](https://github.com/hsoliwal/M3jdk21/actions/runs/37310184488/job/111763397210) | Maven JUnit OpenRewrite proof | Four test-compilation diagnostics in the two A3 regex/memory recipe tests listed below. |
| [Foundation contracts](https://github.com/hsoliwal/M3jdk21/actions/runs/37310184801/job/111763397781) | Verify OpenRewrite convergence DAG | Maven compile rejects `release version 21 not supported`. |
| [JDK proof kit](https://github.com/hsoliwal/M3jdk21/actions/runs/37310184481/job/111763396934) | Attempt the complete retained control reactor without hiding failures | Repeats the Java syntax failure; also fails importing `compatibility_policy` because source code contains null bytes. |

## Exact Java locations

Under `m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/`:

- `M3A3RegexMemoryLabDeliveryRecipeTest.java`: line 97, column 54, `unclosed character literal`; line 98, column 53, `';' expected`.
- `M3A3RegexMemoryWorkflowRecipeTest.java`: line 100, column 54, `unclosed character literal`; line 101, column 53, `';' expected`.

See [the exact compilation excerpt](java-test-compile-excerpt.txt) and full [OpenRewrite job log](job-111763397210.log). This identifies actionable source locations without attributing their introduction to the reconciliation diff.

## Foundation compiler selection boundary

The log downloads and checksums Temurin JDK `21+35`, then writes `M3_JDK=$RUNNER_TEMP/jdk-21+35` to `GITHUB_ENV`. The later logged invocation is exactly:

```sh
mvn -B -ntp -f m3/tooling/migration-recipes/pom.xml clean verify
```

Its logged step environment contains `M3_JDK: /home/runner/work/_temp/jdk-21+35`. The log does not expose an explicit `JAVA_HOME` or `PATH` assignment for that compiler selection, a `mvn -version` output, or the actual selected JVM/compiler version. Downloading the desired JDK does not establish that this Maven invocation uses it. The concrete finding is that the selected compile invocation rejects release 21; the next action is to bind and observe Maven's JVM/compiler against the intended pinned JDK.

[foundation-command-excerpts.txt](foundation-command-excerpts.txt) preserves the original command, environment and failure lines, including timestamps and ANSI sequences; no additional redaction was applied. The complete [foundation log](job-111763397781.log) is retained as returned.

## Python import boundary

The JDK proof-kit traceback reaches `m3/backports/test_compatibility_policy.py`, line 9, at `import compatibility_policy`, then raises `ValueError: source code string cannot contain null bytes`. See the [exact import stack](python-import-excerpt.txt) and [full proof-kit log](job-111763396934.log). The physical file supplying the offending imported source is not resolved by this log review; inspect the exact module bytes and source-generation path before making a repair.

[REPORT.json](REPORT.json) records the exact heads, observations, step results, coverage limits and file hashes. Returned bodies and logs are kept separately and in [ACQUISITIONS.json](ACQUISITIONS.json), with tool names and arguments. The three direct job-detail URLs rejected by the connector are retained as tooling limitations; allowed run-specific job inventories and dedicated log tools provided the needed evidence.

No remote reruns, retries, workflow edits, source edits, remote writes or local tests were performed. No billing/account cause is claimed. CI investigation stops at these three representative failures.
