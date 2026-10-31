<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Eclipse resource lock bounded admission, 2026-10-21

Product owner: original `com.m3.file.M3ResourceLocks` inside M3JDK21's `com.m3.file` module. No second scheduler, filesystem, per-entry store or JNI symbol.

Canonical Synexia Maven/OpenRewrite recipe handoff: https://github.com/hsoliwal/com.synexia/pull/12842; recipe source commit `c060ae1e996bd5d057bea4db9506a10807630a56`; dataset `eclipse-lock-timeout-20261021` in the existing `m3-ast-build-reverse-20261017` crate. This is a draft source-sealed candidate; source was submitted for review before an actual Maven/OpenRewrite materialization gate could be executed. Do **not** promote it until that replay succeeds on an isolated worktree.

Original target base commit: `c831c4a77f0e3e10aac94178217efda882ba4be5`.

- `m3/file/src/main/java/com/m3/file/M3ResourceLocks.java`: 99a90eca92fea1e45604e29c0f1c1295e9e4e3c05dc3bbb3b27c48890f007575 -> 7d0e50075f26400bfce311919457996468321a987d86cf0145737da9fde024c5
- `m3/file/src/test/java/com/m3/file/M3ResourceLocksTimedContract.java`: ABSENT -> 72be431440e875dd4d3600ceb8b88bf9d3b07cb23c8f489f348e6d41a9a77257
- `m3/file/verify.py`: 293d370ccd7da9fe173c661f854516e1f5eae31d8edd8ee68989bbdb33f50255 -> f78386e9c6917b0ebfb0aa1bcf1b731345c3ac35c8cd226cd303360cc6e45212

Scope: overload `tryAcquire(Scope,long,TimeUnit)` with interruptible deadline covering mutex+condition, null on timeout, original lease scope nesting, lock ownership and release. Restore omitted original `M3ResourceLocksContract` to the file-module contract runner; add timed stress contract. Provider, OS, workspace and scheduling-rule authority unchanged. No Eclipse source copy.

## Actual gates

- Original source identity and canonical candidate SHA-256: **pinned**.
- Isolated Java21 strict `javac` timed-lock proof: **PASS** (not installed target proof).
- Canonical OpenRewrite Maven `-Pcontract verify`, replay twice, drift refusal: **PENDING**.
- M3JDK21 `mvn -f m3/file/pom.xml verify` with original module/linked-image contracts: **PENDING**.
- Hosted exact PR-head test, full SDK bootstrap, Eclipse plugin compatibility, CI/performance: **PENDING**.

No admission is inferred from the proposed code or source seals. See Synexia recipe README for exact replay command.
