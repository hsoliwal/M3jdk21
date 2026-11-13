<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# m3/compiler/static-use: compile-time static-use admission

Module `org.m3jdk.compiler.staticuse` (package `jdk.m3.compiler.staticuse`), Maven coordinates
`com.m3:m3-compiler-static-use:0.1.0-SNAPSHOT`, Java 21; a default module of
`make/m3/pom.xml`.

`M3StaticUsePlan` turns compiler-proven facts into `DIRECT_PARENT`, `LAZY_HANDLE`, `MATERIALIZE` or `REFUSE` (unknown
is refused), gives the matching reference mode and the int-induction condition-only loop entry bound, and declares the
opt-in `@Analyze` / `@CompilerSummary` annotations; `M3StaticFactsProcessor` is that opt-in javac annotation processor
(one `@CompilerSummary` companion per `@Analyze` method and the `META-INF/m3/compiler-facts.tsv` ledger). Source use counts
are never runtime counts; nothing here rewrites bytecode or frees a Java object. `java.compiler` and `jdk.compiler` are
`requires static`: the processor runs inside javac, run-time consumers never need them.

Relocated by recipe, never by hand: [recipe/static-use-plan-20261107](recipe/static-use-plan-20261107/README.md)
(donor `hsoliwal/com.synexia` develop `d5ad5090d87e`).

```sh
mvn -B -ntp -f m3/compiler/static-use/pom.xml clean package
```
