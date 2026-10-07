<!-- SPDX-License-Identifier: Apache-2.0 -->
# Synexia PR #9042 -> M3JDK21 code-text / regex precompute handoff

Status: target-side lineage closure for an already implemented isolated M3JDK precompute port.

## Authority split

Synexia remains the convergence/donor workspace. M3JDK21 owns target names, packages, runtime
boundaries and promotion decisions.

The source capability was delivered by Synexia PR #9042:

- head: `aa9b4ff7bd69ff58beba0c3cdd2c66b878c781ed`
- additive merge: `d50ba4e3fe0d4c99259d006c725b1eb2d1eff234`

The exact source/target mapping is
[`synexia-pr9042-code-text-precompute.tsv`](../compatibility/synexia-pr9042-code-text-precompute.tsv).

## M3JDK adaptation

The receiver does **not** preserve Synexia runtime ABI:

- `MIndexCodeTextSignals` -> `com.m3.precompute.M3CodeTextSignals`
- donor batch SPI -> `M3CodeTextSignalBatch`
- Java reference provider -> `M3CodeTextSignalBatchJava`
- donor JNI provider -> target-owned `M3CodeTextSignalBatchNative`
- donor native C/JNI functions -> the existing shared `m3_precompute_signals` ABI
- donor read-only OpenRewrite evidence recipe -> target-owned hash-pinned
  `M3RegexStringPrecomputeSupersetRecipe`

No second regex-pair/native ABI is admitted merely because Synexia experimented with one.

## Semantic authority

The isolated precompute port is candidate/ranking infrastructure:

- SimHash/MinHash/Jaccard and code/regex scores rank or nominate cases only.
- Exact edit thresholds use exact M3/JDK verification.
- Regex truth remains `java.util.regex.Pattern` / `Matcher`.
- Java transformation truth remains compiler/JUnit/contract/atom-reconstruction evidence.
- JNI is optional acceleration and must remain differentially identical to the Java reference.

This handoff does not modify `java.lang.String`, `java.util.regex`, collection public APIs,
bootstrap behavior or HotSpot. Promotion into those owners requires a concrete consumer and the
existing compatibility, memory, CPU, lifecycle and native/JNI gates.

## Recipe-first invariant

Any future source-changing LLM task in this family must:

1. resolve the existing M3JDK owner and mapping first;
2. author or improve a Maven/OpenRewrite content-addressed recipe crate;
3. bind exact preimage/postimage identities;
4. compile and run differential/JUnit tests;
5. run Java/JNI parity when native code is involved;
6. run bounded permutation/combination and multipass convergence for transforms;
7. prove fixed-point replay;
8. preserve an explicit rollback/revert path;
9. update the target mapping/receipt instead of creating a competing naming catalogue.

An LLM may propose signals, recipes, donors and counterexamples. It cannot convert a failed
compiler/test/contract/native gate into a pass.

## Mechanical verification

Run:

```bash
python3 m3/compatibility/check_synexia_pr9042_code_text_precompute.py
mvn -B -ntp -f m3/ports/precompute/pom.xml test
mvn -B -ntp -f m3/ports/precompute/pom.xml -Pnative test
mvn -B -ntp -f m3/tooling/migration-recipes/pom.xml \
  -Dtest=M3RegexStringPrecomputeSupersetRecipeTest test
```

The first command checks exact target Git blob identities, M3-owned runtime naming/package
boundaries, source PR pins, absence of Synexia JNI symbols, and the isolated non-`java.base`
receiver boundary.
