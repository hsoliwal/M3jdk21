# JEP 458 current-tree recovery — Launch Multi-File Source-Code Programs

Status: dependency-complete recovery work packet; not yet a completion claim.

## Upstream authority

- OpenJDK issue/feature commit: `517b1788198fc325961df61161f9b365c7b2524e`
- commit message: `8306914: Implement JEP 458: Launch Multi-File Source-Code Programs`
- upstream commit changes 27 paths.

The older M3 branch `m3/backport-jep458-multifile-source-launcher-20261003` is retained as evidence
but is not a complete implementation. It added only nine launcher helper classes. It did not switch
the native launcher entry point, add `SourceLauncher`, wire module/build dependencies, adapt compiler
internals/resources, or carry the upstream jtreg proof.

## Compatibility adaptation

M3JDK21 does not cherry-pick the JDK22 commit verbatim.

The locked Java 21 contract requires these explicit adaptations:

- preserve `com.sun.tools.javac.launcher.Main` instead of deleting it;
- switch the native source-launcher entry point to the new `SourceLauncher` only after the complete
  launcher graph is installed;
- adapt `List.getFirst()` calls to Java-21-compatible `get(0)`;
- adapt upstream `jdk.internal.misc.MethodFinder` use to JDK21 `MainMethodFinder`;
- import only the minimal `Preview` access leaf required by `MemoryContext.MemoryPreview`, not the
  JDK22 preview-feature table;
- keep all upstream license headers and OpenJDK licensing; M3 tooling/evidence remains Apache-2.0.

## Atom/DAG model

Every concrete file create/modify is a FILE recipe atom. Files that form the javac launcher package
rejoin at an explicitly approved PACKAGE node. The complete feature re-enters the verification tail
through an explicitly approved MULTI_MODULE join.

This is intentional: mechanical replay can fan out per file, but feature admission cannot pretend
that a java.base + jdk.compiler + native launcher change is file-local.

## Verification boundary

Required before promotion:

1. exact current-master preimages;
2. source-sealed Java/text OpenRewrite crates;
3. recipe JUnit and second-pass fixed point;
4. Java 21 parser/compile acceptance of adapted Java postimages;
5. OpenJDK configure/make build;
6. focused launcher/javac jtreg, including multi-file and modular source launcher tests;
7. jdeps module-dependency test;
8. runtime source-launch smoke tests;
9. no regression of legacy single-file `Main` compatibility;
10. serial promotion only after the evidence-bound DAG is green.

No JEP 458 completion or performance claim is made by this document.
