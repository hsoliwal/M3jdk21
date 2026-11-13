<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
     SPDX-License-Identifier: Apache-2.0 -->
# M3 long-path input-work budget — fork proving ground and product handoff

**Scope:** additive primitive Java 21, backed by the existing M3 Java/JNI mask engines. This is a separately built `m3/collections` receiver, not a `java.util` replacement or a new JNI ABI. The canonical Synexia owner is the Maven-built source-sealed recipe in `synexia-m3-recipe/recipes/m3-long-path-input-budget-20261018`.

## Why this work is distinct

The existing `M3LongIndexPathMasks.scanWindow` caps *admitted output* but can inspect the entire remaining source to fill a sparse page. `M3LongIndexPathBudget.scan` caps *inspected input occurrences* independently of accepted output; it preserves the source order, offsets, 32-bit state planes, signed strides and duplicate payloads. Empty pages may advance the input cursor without producing results; a zero budget or zero output limit returns the unchanged cursor. The caller must hold a stable input generation between calls.

This is a **composition** over the original Java and JNI selectors. It invokes their established validation/alias/geometry checks with a zero-output pass, then projects the requested input ordinal into a bounded subpath and delegates to the same selector. Neither M3 `long[]` storage nor mask interpretation is duplicated; there are no per-row objects. `Backend.NATIVE_REQUIRED` never falls back to Java. The existing native Region implementation (branch containing M3JDK21 PR #1267) is reused without changing its symbols. The added method's time is O(stages * min(maxExamined, remaining)); Java heap scratch is O(1), excluding caller-owned output and underlying JNI implementation details.

The source will reside in M3JDK21 as `com.m3.collections.M3LongIndexPathBudget` and `M3LongIndexPathBudgetSelfTest`. The development forks are **experimental workspaces**, not the canonical product or recipe authority. The Commons Collections fork is only a pinned algorithm comparison; its predicated mutable collection has different acceptance and mutation semantics, so no third-party source has been transplanted.

## Recipe-first source custody

The canonical recipe is Maven-built on Java 21, with SHA-256-sealed Java source and test templates. It preflights *all* paths, refuses symlinks, drifted output and altered templates, creates only absent files, and converges to a byte-for-byte fixed point. The generator does not manually edit existing production files. For any later existing-Java transformation use the existing Synexia OpenRewrite engine.

```sh
ROOT=m3/collections/qualification/m3-long-path-input-budget-20261018/recipe
mvn -B -ntp -f "$ROOT/pom.xml" package
java -cp "$ROOT/target/classes" com.synexia.m3.M3LongPathInputBudgetRecipeSelfTest "$ROOT/templates"
java -cp "$ROOT/target/classes" com.synexia.m3.M3LongPathInputBudgetRecipe --check . "$ROOT/templates"
```

For a fresh isolated receiver, use `--apply /fresh/checkout "$ROOT/templates"`, then `--check` and repeat `--apply` without changes.

## Selected Java / JNI proof

Compile with `javac --release 21 -proc:none -Xlint:all -Werror`, using the existing `M3LongIndexPathMasks`, `M3LongIndexPathMasksJNI`, and the two generated files. For JNI, build the existing `m3_long_index_path_masks_jni.c` into `libm3_collections_long_path_masks` with JNI headers, strict C17 warnings, and run with `-Xcheck:jni` and `-Djava.library.path=...`. A second C build with `-fsanitize=undefined -fno-sanitize-recover=undefined` verifies the existing native boundary again. The PR workflow supplies those commands and source seals.

Executed locally with Linux x86_64 / OpenJDK 21.0.12.1 / GCC C17:

- Java-only standalone differential proof: **PASS, 27,250 assertions / 2,200 randomized scenarios**.
- Real JNI (Java and native parity, `-Xcheck:jni`): **PASS, 54,497 assertions / 2,200 randomized scenarios**.
- JNI binary rebuilt with UBSan: **PASS, 54,497 assertions / 2,200 randomized scenarios**.
- Strict Java 21 compile (sources and recipe crate): PASS; recipe fresh apply, exact check, fixed-point replay, tamper refusal: PASS.

These are bounded local proofs. Hosted exact-PR-head Actions, Maven execution, full reactor/JDK image, Gradle/Maven fork source builds, Windows JNI, GC correctness, and performance/memory benchmarks are separate gates. No speedup is asserted by differential assertions.

## Provenance and promotion

First-party source and recipe are Apache-2.0. Existing M3JDK21 and prior JNI rights remain unchanged. The `CATALOG.tsv` joins source-owner blob hashes, problem categories and donor licenses to an explicit reuse/rejection rationale. LeetCode, HackerRank, and GeeksforGeeks are problem-taxonomy references only, never unlicensed source donors. The product's public interface and native ABI remain unchanged. Serial file-level recipe promotion proceeds only after exact preimage/postimage and contract qualification.
