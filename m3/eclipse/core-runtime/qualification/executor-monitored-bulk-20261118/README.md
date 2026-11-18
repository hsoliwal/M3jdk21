<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->
# Monitored executor bulk defaults — direct M3JDK21 java.base receiver

- Exact product parent: `5809ffb3b027fe094833a7d1a785f36fde8d6f79`
- Canonical Maven/OpenRewrite recipe: [Synexia PR #18756](https://github.com/hsoliwal/com.synexia/pull/18756), commit `5405adc4155ad66542e2a78ccc0d889acbfc90e7`, named `com.synexia.m3.ExecutorMonitoredBulk20261118`
- Existing `ExecutorService.java` blob `4a28c807cd3cdb26ede339f0b23a348b6bdf405d`, preimage SHA256 `d37156c3aa56b24a2c4e4c264868e23d33e908e5dda0f01ff17c7f8cb219dd3a`
- New source SHA256 `03a8645b9fe711e9a750a2577f1cb1d1e68905d25f9af75ad2a65ebc764a681a`
- Corrected GPL-2.0-only jtreg SHA256 `811ac60bb0072c25e390d031cb3b6dfa2e0e8fa979261b50d2096ba0746d0f64` (ABSENT before).
- Full images and manifest are canonical in Synexia, product contains the generated JDK source and test only.

## In-place behavior
New opt-in default methods `invokeAllMonitored` and `invokeAnyMonitored`, each timed/untimed, reuse original ExecutorService execution, Future ordering, exception and cancellation semantics. One private static helper prepares borrowed ProgressMonitors.wrap closures; no alternate scheduler or runtime module, no JNI symbol, no retained per-item collection node. No old Java API signature is changed. Task-specific monitors are borrowed: do not begin, finish, or cancel the caller's owner as a side effect.

Problem category: bounded bulk scheduling, progress scoping, first-success and indexed Future order. LeetCode, HackerRank and GeeksforGeeks provide category shapes only. Algorithm decision and no-copied-donor rights are recorded in Synexia `CANDIDATES.tsv`.

## Proof vs holds
- Independent Java21 executor delegate fixture: PASS 52 checks, with concurrently executed task-scope assertions and no shared-counter races. **Not a patched SDK binary or jtreg run**.
- Canonical source/hash seal: the production output matches Synexia's source template; the jtreg test matches the corrected canonical template after this successor correction. Recipe JUnit: NOT RUN in this environment.
- Installed M3JDK21 java.base compile, jtreg `test/jdk/java/util/concurrent/ExecutorService/M3ExecutorBulkProgressTest.java`, HotSpot/JNI, Eclipse JobManager/OSGi and full SDK build: NOT RUN. Remain draft until real receipts exist.
- No JNI acceleration for Runnable/Callable application callbacks: these are Java-managed scheduling and cancellation semantics; existing JNI array/mask engines are unchanged and remain separately qualified.

## Source-readback successor

This additive correction restores the corrected GPLv2-only jtreg header and exact canonical template identity (`69596453efbaa790b1eea4b1b8a811e6c47b75d4`) after the earlier product PR was merged from its first branch head. No executor production source, JNI symbol, scheduler, or public API changes are made in this successor. Canonical Synexia recipe commit: `5405adc4155ad66542e2a78ccc0d889acbfc90e7`. Full built-JDK jtreg, Maven OpenRewrite runtime qualification, Eclipse OSGi/Equinox and native platform gates remain pending.
