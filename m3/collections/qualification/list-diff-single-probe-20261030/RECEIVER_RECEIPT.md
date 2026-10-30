<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3ListDiff one-pass primitive index — receiving custody

Canonical recipe: [hsoliwal/com.synexia PR #14136](https://github.com/hsoliwal/com.synexia/pull/14136) (merged into `develop`).
Dataset: `synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017/datasets/list-diff-probe-20261029`.
Source target: `m3/collections/src/main/java/com/m3/collections/M3ListDiff.java`.
Exact preimage Git blob: `a5b22ab394e609c40a28243e368f65d0cc269271`.
Source SHA-256 (before): `e285357c40f9310af676a85a60bce98e752a5c037f2d0e4228109688a242c10e`.
Source SHA-256 (candidate): `fc3b925f70d1611bf80ad0f95786cc95c8908cf535a733eb54f9b3d663e23c56`.
Added test SHA-256: `3af07ee00f8161c8159c6a5fbb92304f6682db36a5384a6751528892426b4ae6`.
Base M3JDK21 master commit: `5860598b515885ebc2275f6552d9a5a1348fdb15`.

This receiving draft places the byte-for-byte canonical **authored postimage template** and JUnit test into an isolated PR; it is **not yet an SDK-emitted receipt**. Actual Maven/OpenRewrite run, fixed point and exact target JUnit must execute before any merge. It must be reconciled if the source, canonical recipe, or base branch changes.

## Contract

- Replaces one private `containsKey` then `put` sequence with `put`'s documented insertion-result boolean.
- Reused `M3LongLongHashMap` has no byte changes. Existing M3 native symbols, package `com.m3.collections`, class names, public/protected methods, exceptions and serialized forms are unchanged.
- `after` duplicate validation still precedes `before`, first offending value and message remain exactly the same.
- The duplicated `put` mutates only a map being constructed but not published; its failed duplicate state cannot escape.
- No copied donor implementation; LeetCode #217, HackerRank Sparse Arrays, GeeksforGeeks are category comparisons only.
- No new runtime dependency, no JNI crossing.

## Executed and pending gates

- Java 21 standalone model (`ListDiffSingleProbeKernelProof.java` in canonical packet) **PASS** 100,000 seeded logical map comparisons, re-run locally on stock Java 21 with `javac --release 21 -Xlint:all -Werror`.
- **NOT RUN:** current Maven/OpenRewrite SDK on exact source epoch; five recipe JUnit tests; receiver fixed-point; full M3JDK21 Maven reactor/JUnit, modified-JDK native, JMH, all-platform checks.
- Keep draft; do not merge on metadata-only checks. Source archive and original license headers retained exactly.

## Execution

```sh
cd synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017
mvn -B -ntp -Pcontract -Dm3.dataset=list-diff-probe-20261029 verify
python3 receive.py --dataset list-diff-probe-20261029 --target /absolute/m3jdk21-isolated --check
python3 receive.py --dataset list-diff-probe-20261029 --target /absolute/m3jdk21-isolated
python3 receive.py --dataset list-diff-probe-20261029 --target /absolute/m3jdk21-isolated
# Then current M3JDK21 Java21 reactor/module JUnit, ABI, relevant JNI and JMH gates
```
