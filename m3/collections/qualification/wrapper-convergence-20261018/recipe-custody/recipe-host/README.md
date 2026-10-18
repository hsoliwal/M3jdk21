<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# One-file wrapper convergence recipe proof host

This host adapts the existing array/collection custody Maven host and `M3TaskRecipeTest`.
It compiles the four exact canonical Synexia source owners identified by `ENGINE_SOURCES.tsv`;
it contains no replacement rewrite engine. The parent crate's `../CONTRACT.md` governs all product candidates.
The current engine revision is `98368e107f4a205d852ef958cd48c1ee633219b1`.

## Pass admission

Select exactly one pass with `-Dm3.pass=<id>`. A pass ID matches `[a-z0-9][a-z0-9-]{0,59}`.
The pass resource tree is `passes/<id>/resources` and the engine crate name is `m3-wrapper-<id>`.
Exactly one of the Java and text lanes must exist, and its manifest must contain exactly one target.
The target path is relative to the M3JDK21 receiving repository; neither target deletion nor path
renaming is admitted. Unknown preimages fail. Existing postimages are the exact fixed point.
`ABSENT` is allowed only when the sealed parent contract explicitly admits a new target.

For the Java lane, place these resources under
`com/synexia/rewrite/hash-pinned-java/m3-wrapper-<id>/`:

- `manifest.tsv`: one tab-separated row `target-path`, `preimage-sha256-or-ABSENT`, `postimage-sha256`, `template-filename`.
- `<template-filename>`: the exact reviewed UTF-8 postimage.
- `pre/<template-filename>`: the exact UTF-8 preimage, omitted only for `ABSENT`.

Use the identical resource protocol below `hash-pinned-text` for C/native input. Java remains a real
OpenRewrite Java compilation unit. C is admitted by the existing exact text engine; it gains no Java
semantics and still needs the separate native compiler/JNI/runtime gates.
A descriptor at `META-INF/rewrite/m3-wrapper-<id>.yml` must name
`com.synexia.m3.WrapperConvergence20261018` and compose the selected existing engine with its crate name.
Optional real source-owned Java parse context/dependency resources keep the canonical engine protocol.
`-Dm3.parser.classpath=<platform-separated-existing-paths>` supplies an explicit parser classpath when
actual compiled dependency owners are needed. No placeholder or fake source is permitted.

The `fixture-java` and `fixture-text` passes qualify the machinery only. They do not qualify product behavior.
Create a fresh pass ID when the source or contract changes; preserve earlier source and evidence.

## Proof and materialization

The retained existing tests run the real OpenRewrite SDK scheduler and named descriptor. They exercise
exact replay, fixed point, reversed source submission, mixed pre/post state, missing required source,
drift, duplicate source, wrong Java representation and post-scan mutation. An unrelated text source
contains code-shaped decoys and must survive byte-for-byte.
The final ordered test writes only after the preceding proof groups completed successfully. Output
comes from actual `Result.getAfter()` source files, not a template file copy.

Per-pass candidate files are written below `target/candidate/<id>/<target-path>`. The same directory
retains `SDK_RESULTS.tsv`, `SDK_INPUTS.tsv` and the actual patch. Existing equal files are read and left
unchanged; different files or symlink paths refuse rather than overwrite. Engine/source/resource pins
are checked and retained. An SDK replay proves only this exact transformation; target behavior, public
API/JNI parity, resource measurements and complete-repository acceptance remain separate.

## Ordered execution

At host creation, lint, compilation, tests and runtime have NOT RUN. The parent must authorize and
record each gate in order: source/template patch -> lint -> compile -> tests -> runtime.
Do not run a Maven goal that advances to a later phase before the prior gate passes.

After the parent records lint success, compile the selected host pass using:

```sh
mvn -B -ntp -f pom.xml -Dm3.pass=fixture-java -DskipTests test-compile
```

After compile success, execute the already compiled proof without rerunning lifecycle compilation:

```sh
mvn -B -ntp -f pom.xml -Dm3.pass=fixture-java surefire:test
```

Repeat for `fixture-text` and each sealed product pass. Use the local JDK21/Maven toolchain and an
explicit `-Dmaven.repo.local=...` cache. `recipe.owner.root` defaults to the enclosing canonical
`synexia-openrewrite-recipes` module; an override must still match every engine pin.
Maven raw logs, Surefire XML and return codes are retained by the caller. A failed, absent or skipped
result keeps the pass unqualified even if an intermediate candidate file exists.

## Dependency boundary

The proof host preserves the existing donor's pinned OpenRewrite 8.17.1 and JUnit Jupiter 5.10.2
coordinates. These are external verification dependencies, not product runtime additions.
The production collections module instead requires real repository-built `5.10.2-m3-source-1`
JUnit coordinates. `DEPENDENCIES.md` records its source closure and bootstrap command.
No official artifact may be relabeled as a source-owned version. Repository-wide offline/source
closure is separate from this bounded host's proof dependency resolution.
