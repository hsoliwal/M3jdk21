<!-- SPDX-License-Identifier: Apache-2.0 -->
# 2026-10-17 — M3JDK21 DOOP primitive stream DAG evidence

| Gate | Actual observed status |
| --- | --- |
| `javac --release 21 -proc:none -Xlint:all -Werror` (3 new production files + contract with source-compatible stand-ins) | PASS |
| 291 assertions, `-Xmx16m`, including 10,001 DAG node inventory and 100,000-row primitive accumulation | PASS |
| Java 21 immutable DAG, bounds/refusal, nullable client state, single-pass collector fan-in, JDK stream differential | PASS (stand-in source owners) |
| Actual receiver original classes compiled and runtime tested | NOT_RUN (full checkout absent from local container) |
| Original `M3LongSortedViews` contract + actual JNI oracle under `-Xcheck:jni` | NOT_RUN |
| OpenRewrite SDK/JUnit generated candidates/32 mixed resumes/fixed point | NOT_RUN (Maven unavailable in local environment) |
| Maven full `m3/collections` reactor / JUnit / Spotless / PMD / Checkstyle / SpotBugs | NOT_RUN |
| OpenJDK build and jtreg, other native operating systems | NOT_RUN |
| Performance gain or JDK java.util integration | NOT_CLAIMED |

The first receiver PR should remain draft until its CI checks run. Do not interpret
successful tests with source-compatible doubles as proof of the full original M3
storage, iterator, JNI, or sorted-set implementation. `verify.sh` refuses owner drift;
fix source-generation recipes and rerun rather than patching generated output.
