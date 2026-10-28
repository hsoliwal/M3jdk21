<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->
# M3JDK21 exact ranked Boolean Top-K source receiver (2026-10-25)

This is result-only receiving source in the original modified M3JDK21 product, not a compatibility layer and not a recipe mirror. The canonical Maven/OpenRewrite source-sealed master, donor category catalogue, and private correction memory remain exclusively in [Synexia #13267](https://github.com/hsoliwal/com.synexia/pull/13267).

The two original source owners are M3SignalBooleanQuery and M3SignalTopK, without duplicated array/value storage, second sorter, JNI export, per-row Entry/Node or additional runtime dependency. The new `Compiled.evaluateTopK(k,precedence,version,SHA[,monitor])` reuses the original postfix Boolean bitmap and feeds it to the existing M3SignalTopK primitive worst-first heap via `topKByMask`. Rank order is the original exact typed, missing-last, stable physical encounter ordering, not semantic object equality or certified heuristic pruning. Product source/readback must match all four SHA-256-sealed Synexia Java templates.

The new admission validates physical mask width and padding bits, negative result limit, original full SHA-256 identity and epoch, and a bounded heap allocation against JVM maxMemory. Cancellation, progress completion and any reentrant invalidation continue through the original M3ProgressMonitor and M3SignalDependencyEpochs owners. The existing evaluate, and/or, evaluateWindow, sort and Top-K APIs retain their original signatures.

Local strict OpenJDK 21 compiler rehearsal using byte-matched original Boolean/TopK plus API-compatible supporting owners passed `javac --release 21 -proc:none -Xlint:all -Werror` and **11,894 deterministic assertions** in normal and `-Xint` each. Includes 660 seeded matrices, six bounded K requests each, 64-bit extreme values, NaN/signed-zero, missing masks, padding refusal, source-version mismatch, cancellation, physical order and original viewport regression. These are bounded proofs, NOT original M3JDK21 Maven/JUnit or installed JDK source-built admission.

The previous hosted original JUnit test-compilation blocker, a missing `L` on a preexisting test long literal, is already repaired by the exact canonical Synexia #13198 successor received in this target PR. The new focused workflow does not skip any requested collection JUnit or relax compiler flags.

**DRAFT / HOLD** until genuine original-source Java21 and owning Maven/JUnit tests, canonical Synexia SDK apply/fixed-point/drift-refusal, full modified JDK/jmods, JTReg/JCK, JNI/HotSpot/GC/JMM, Windows/Linux parity, retained heap/native memory and CPU throughput gates pass. No full PRD or release completion claimed.
