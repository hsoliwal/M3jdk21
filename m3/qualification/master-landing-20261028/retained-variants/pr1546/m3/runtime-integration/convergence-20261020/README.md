<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 — non-destructive product superset, 2026-10-20

**M3JDK21 is the JDK product, not an M3 + JDK adaptor.** Existing `java.base`, `jdk.compiler`, `m3/collections`, filesystem and parser/JDT source owners remain authoritative. The private Synexia repository holds canonical development recipes/donor convergence, not a runtime requirement.

## Selected exact product histories

The second commit has **five Git parents**: a recipe/admission parent followed by these exact four independently reviewable parent heads:

| Parent | Exact head | What is retained |
| --- | --- | --- |
| Recipe first | This branch's preceding commit | Manifest/receipt runner, contract tests, categorical donor rationale, Maven/OpenRewrite source-seal dataset and workflow |
| [#1539](https://github.com/hsoliwal/M3jdk21/pull/1539) | `8a3540d43c25ce92dafe2dd6b3f3e61ee7783dda` | Stacked #1538: actual Java/JNI collection, `java.util.SortedSet`, parser, compiler, filesystem, JDT-compatible provider model and runtime qualification changes (51 files) |
| [#1535](https://github.com/hsoliwal/M3jdk21/pull/1535) | `6b0557f3fd47fd63ef0d336c46fec04d670e4dfb` | MIndexAST-derived exact file/package/module/project L0-L5 row selection, original file/atom dispatch, test and recipe (13 files) |
| [#1506](https://github.com/hsoliwal/M3jdk21/pull/1506) | `f4f942ddeedf24d2a7bc1a2e9da2ff8ab1c70084` | JDK-native array/slot/state-mask live views; value/state epochs, stable primitive index sort, regression tests/recipes (16 files) |
| [#1516](https://github.com/hsoliwal/M3jdk21/pull/1516) | `ebb83cbb0775df3b9b91ad0b18376653077c9dce` | Original JDT-owned array node list and real M3AST ingress qualification, licence/source custody (81 files) |

All four component changesets are compared to `M3JDK21/master@d6d76b563050fc13c4fa79bc494edf29f1614b37`. **161 changed source/proof paths are disjoint**, with no deleted files and no conflicting file ownership. This is an additive Git-tree union; it preserves original commits and original per-file recipe lineage rather than cherry-picking a “winner.” The JDT source-owner runtime remains Eclipse's JDT; M3JDK21 uses exact occurrence/offset/mask projections, not a parallel parser.

The original canonical source receipts and licence notices are retained byte for byte. No challenge-site source bodies are copied. In particular, object payloads stay GC-visible Java references while JNI receives bounded primitive index/offset/mask arrays, not movable Java object pointers.

## Admission/proof and reproduction

`verify_superset.py` inspects **Git trees** and refuses any overlap, altered source blob or mode, deleted or missing donor path, foreign edit, or incorrect merge-parent order. It never rewrites source. A stable JSON receipt describes the four parent commits and their preserved path counts. The test suite uses actual synthetic Git histories to establish success, overwrite refusal, extra-change refusal and fixed-point behavior.

```sh
python3 -m unittest discover -s m3/runtime-integration/convergence-20261020 -p 'test_verify_superset.py' -v
python3 m3/runtime-integration/convergence-20261020/verify_superset.py --repository . --merged HEAD
```

The five added admission/CI/catalogue files are bound to a new **text** dataset `m3-convergence-20261020`, reusing unchanged `M3HashPinnedTextSnapshotRecipe` and its actual Maven/OpenRewrite SDK. The first commit carries those recipe inputs **before** the second product merge. No new competing recipe runner, AST file owner or JNI implementation is introduced.

The focused product gate additionally compiles/tests `M3ASTLeafIndex` and JDK-internal `M3IndexedArray`/`M3IndexedView` on Java 21. Its result is separate from the original component's JDT/PDE, parser, JNI and full image qualification. A passing blob-preservation proof **does not** prove semantic composition of all runtime paths.

## Outstanding gates (draft/hold)

- Full source-built M3JDK21/HotSpot image, all relevant jtreg/JCK, cross-platform JNI, Android/ART and whole-SDK ABI/GC/heap/CPU qualification.
- Real NetBeans/Eclipse JDT/Maven/Gradle original-task lifecycle and per-file, per-package, module/project isolated compilation against unchanged supplied binaries. A live `JavacTask` is not portable across processes.
- Guaranteed one-parse-per-source-generation across every external parser, detached semantic-environment validation, persistent lazy MIndexAST reuse, all-parser parity, JSON 100 MiB true low-retained-heap proof.
- The remaining overlapping (not admitted) candidate PRs such as #1514 and #1513 require **new current-source recipes and reconciliation**; their code must not be silently added on top of #1539.
- All JDK/Guava/Eclipse Collections/Apache Commons Collections/Spring filtered, sorted and mutable wrapper contracts, and measured superiority for each chosen algorithm.

**Review rule:** preserve the superset, review one behavioral atom and source file at a time, fix and re-run its canonical recipe, verify real tests and original-API behavior, then admit it. Do not treat another merged PR title, Git ancestor alone, or a newly written catalogue as completion.
