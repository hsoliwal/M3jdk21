<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# m3/compiler/parent-owned: the @M3ParentOwned annotation processor

Module `org.m3jdk.compiler.parentowned` (package `jdk.m3.compiler.parentowned`), Maven coordinates
`com.m3:m3-compiler-parent-owned:0.1.0-SNAPSHOT`, Java 21; a default module of `make/m3/pom.xml`.

`M3ParentOwnedProcessor` is the opt-in JSR-269 processor of `jdk.m3.collections.M3ParentOwned`. It writes the
deterministic, conservative `META-INF/m3/parent-owned-v1.tsv` sidecar: one row per annotated class, field and method
with owner, flags and element kind. Static reference counts and escape proofs are never fabricated (`UNKNOWN` /
`UNVERIFIED`). It never mutates the annotated type. `java.compiler` and `jdk.m3.collections` are `requires static`:
the processor runs inside javac, and the code under compilation already carries `jdk.m3.collections`. Opt-in through
`-processor`; there is no service registration.

It is a sibling of `m3/compiler/static-use` and `m3/compiler/mat-sortable`, not part of either. `static-use` stays
java.base-only, and this processor reads `M3ParentOwned.class`, so it needs `jdk.m3.collections`.

Relocated by recipe, never by hand: [recipe/parent-owned-processor-20261107](recipe/parent-owned-processor-20261107/README.md)
(donor `hsoliwal/com.synexia` develop `eaacff5931ef`).

```sh
mvn -B -ntp -f m3/compiler/parent-owned/pom.xml clean package
```
