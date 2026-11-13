<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Immutable range-set source successor admission

Three historical workflows hard-code the original 2026-10-17 recipe's `f865b006...` Java postimage. The existing modernized M3 source had already evolved through the source-sealed `ca18bcda...` snapshot before the three current LeetCodify atoms received `37f4f354...`. A workflow comparing only the original SHA against the current source necessarily fails before Java/JNI compilation. Do **not** weaken original manifest or original `verify.py` to avoid this.

`verify.py` in this successor first reconstructs the exact historical source stage from the unchanged original generated templates plus Git-blob-pinned original JNI/Java peers, and **executes the unchanged original `long-range-set-20261017/verify.py`** on that isolated stage. It then proves the preimage snapshot and current Java/test postimages by SHA256 and Git blob, pins all six active Java/JNI peers, and runs six negative digest-mutation controls. It rejects drift, symlinks and missing source files. Its samples retain old source license headers. It does not silently certify any later owner drift.

The postimage and new JUnit test also remain anchored in Synexia's original `M3HashPinnedJavaSnapshotRecipe` crate `doop-key-value-range-20261102`: [Synexia canonical PR #14952](https://github.com/hsoliwal/com.synexia/pull/14952). This verifier is a receiving source-custody guard, **not** a replacement for executing OpenRewrite. The original M3 Maven OpenRewrite create-only recipe and its negative tests remain unchanged.

The three incoming workflows invoke this successor admission before compiling or running the current owner. Their Java tests include `M3LongRangeSetAtomTest`; the native lane executes both original native self-tests and the new Java/native parity JUnit in a JVM with `-Xcheck:jni` and the actual checked C17/JNI shared library. Exact compiler/hosted acceptance results must be read from those workflow runs, not inferred from source custody.

Catalogue: lower-bound and interval manipulation (LeetCode 34/57, GFG lower-bound), HackerRank Array Manipulation (rejected for dynamic unions) and pinned MIT GitHub donors (discovered, rejected for body reuse). No donor solution body copied. No standard JDK or native C symbol changed.

**Merge HOLD:** This is a source-admission and test-enablement repair; other independent historical source-closure failures in M3JDK21 remain. Run hosted Maven, Java, JNI, all source-closure guards and final JDK image/ABI gates before lifting the draft.
