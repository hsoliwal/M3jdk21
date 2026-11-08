<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# m3/compiler/mat-sortable: the mat sortable annotation processor

Module `org.m3jdk.compiler.matsortable` (package `jdk.m3.compiler.matsortable`), Maven coordinates
`com.m3:m3-compiler-mat-sortable:0.1.0-SNAPSHOT`, Java 21; a default module of `m3/pom.xml`.

`M3MatSortableProcessor` is the opt-in JSR-269 annotation processor of the mat sortables. For every class or record
carrying `jdk.m3.collections.M3Sortable` it writes one deterministic companion `<Type>Sortable`: the schema and
order-policy SHA-256 seals, the structural declaration facts, `compareExact` over the `@M3SortField` columns (or the
`@M3SortSignal`), `exactComparator`, `primary` / `signals`, the validated `@M3SortDistance` and the `ComparisonEvidence`
receipt. It locates `M3Sortable`, `M3SortField`, `M3SortSignal`, `M3SortDistance` and `M3SortDependency` by canonical
name, never mutates the annotated type, loads nothing reflectively and assumes no instance values. `java.compiler` is
`requires static transitive`: the processor runs inside javac, run-time consumers of the companions never need it, and
`m3-collections` is not a compile dependency at all (test scope only, for the fixtures). There is no service
registration: consumers put the module on the processor path (`-processor jdk.m3.compiler.matsortable.M3MatSortableProcessor`).

Relocated by recipe, never by hand: [recipe/mat-sortable-processor-20261107](recipe/mat-sortable-processor-20261107/README.md)
(donor `hsoliwal/com.synexia` develop `56cbdc631259`). Two members are held with their owners of the mat lane
(the capability companion emitter and the `stableSignalOrder` delegation to `MatIntOrder`): see its
[HANDOFF.tsv](recipe/mat-sortable-processor-20261107/HANDOFF.tsv).

```sh
mvn -B -ntp -f m3/compiler/mat-sortable/pom.xml clean package
```
