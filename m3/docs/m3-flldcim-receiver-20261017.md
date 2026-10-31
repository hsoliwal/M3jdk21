<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# M3 FLLDCIM documentation receiver — L0 to L5

This is an **additive, byte-mirrored authoring recipe**, not a new `java.base`
runtime dependency or a declaration that all OpenJDK source is documented.
The original Synexia canonical recipe and test bodies are pinned at
[hsoliwal/com.synexia#12065](https://github.com/hsoliwal/com.synexia/pull/12065),
source commit `303969c826cba7da43fc2e8b264b762485c202ef`. Exact donor/target file blobs appear in
`m3/docs/m3-flldcim-receiver-provenance-20261017.tsv`.

## Hierarchy (documentation view, not the separate M3AST evidence hierarchy)

| Level | Document | Inventory contract |
| --- | --- | --- |
| L0 | File | Each **admitted OpenRewrite SourceFile**, printed UTF-8 digest, parser kind and recognized declarations |
| L1 | Package/folder | Child file links and SHA-256 rollup |
| L2 | Component/subproject | Child package links, scope and digest |
| L3 | Maven module | Child component links; module identity discovered from admitted `pom.xml` |
| L4 | Project | Module root rollup and descendants |
| L5 | Repository | Project digest and source trace |

The AST evidence index in `m3/ports/compiletime` has **different L0/L1/L2
definitions**; the two formats are not silently equated or renamed.

## Mechanical execution

The unchanged `M3HierarchicalDocumentationRecipe` is an OpenRewrite
`ScanningRecipe` that discovers admitted `SourceFile`s, emits
`.m3/generated-docs/` and refuses generated-path collision, duplicate owners,
stale generated files, non-UTF-8/binary misuse or unowned edits.
It cannot infer invisible files; a Git tree inventory and explicit typed
exclusions/parse failures are required before claiming full coverage.

Run the local zero-dependency projection smoke test with Java 21:

```sh
javac --release 21 -Xlint:all -Werror -d /tmp/m3-flldcim-docs \
  m3/tooling/migration-recipes/src/main/java/com/synexia/rewrite/M3HierarchicalDocumentation.java \
  m3/tooling/migration-recipes/src/test/java/com/synexia/rewrite/M3HierarchicalDocumentationSelfTest.java
java -cp /tmp/m3-flldcim-docs com.synexia.rewrite.M3HierarchicalDocumentationSelfTest
```

Run the owning Maven/OpenRewrite JUnit test on the target's own pinned Maven
dependency set:

```sh
mvn -B -ntp -f m3/tooling/migration-recipes/pom.xml \
  -Dtest=M3HierarchicalDocumentationRecipeTest test
```

A running recipe must bind the actual project and repository names and supply a
pinned, complete admitted file set. Recipe generation is candidate-only:
`diff → linter → compile → tests → runtime when applicable → serial promotion`.
No bulk `java.base`, HotSpot, GC or JNI mutation is authorized by this receipt.

## Evidence boundaries

- Local independent JDK 21 projection compile and small regression fixture: PASS.
- Original Synexia structural model/test: copied without source modification.
- Target Maven/OpenRewrite/JUnit, full OpenJDK Git inventory, binary/JNI source
  parser coverage, complete generated documentation and full-image jtreg: NOT_RUN
  as part of this local preparation. Check GitHub Actions at the **current PR head**.
- M3 receiving phase remains STRING active, arrays/collections/AST deferred for runtime
  promotion; this authoring recipe cannot advance phase state.

Reuse is of first-party Apache-2.0 sources and the OpenRewrite Apache-2.0
mechanism. Existing OpenJDK/GPLv2 and third-party sources retain their licenses.
