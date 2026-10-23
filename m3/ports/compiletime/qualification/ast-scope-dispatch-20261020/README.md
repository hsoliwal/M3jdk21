# M3JDK21 intrinsic MIndexAST source and scope selection

This is an additive refinement of the **original M3ASTLeafIndex**, preserving the MIndexAST/MIndexFile lineage. All L0-L5 arrays, atom handles, physical offsets, source digests, masks, parent-child edges, original API signatures and existing parser owners remain unchanged.

Scope lookup selects actual pre-existing rows: L1 file, L2 package, L3 module, L4 project. Names are length-framed exactly as the current `groupKey` builder. A relative source name is unique within (project,module) but can repeat across modules/projects. Missing scope returns -1 instead of parsing or silently increasing scope.

`dispatchFiles` delivers the existing FILE and FACTS row identities to the caller's compiler/parser owner, including zero-atom files. `dispatchAtoms` sends the original atom ID, distinct physical offset and full-width state word to the original AST owner. Both methods are **stateless and non-owning**; they never read source files, parse, attribute, create a new AST, allocate an output directory, or materialize text. Dispatch may stop at an owner exception without changing M3's immutable index.

This is selection/dispatch **only**, not an independent project compiler, parser framework, binary-cache owner or semantically attributed snapshot. `M3File` remains the original source/identity authority. The actual compiler/parser owner must verify its source-generation and semantic-environment identities before interpreting delivered file/atom references. Unchanged .class dependencies remain supplied binary inputs rather than automatically parsed sources.

Reproducer:

```sh
javac --release 21 -proc:none -Xlint:all -Werror -d /tmp/m3-scope \
  m3/ports/compiletime/src/main/java/com/m3/compiletime/M3ASTLeafIndex.java \
  m3/ports/compiletime/src/test/java/com/m3/compiletime/M3ASTLeafIndexSelfTest.java \
  m3/ports/compiletime/src/test/java/com/m3/compiletime/M3ASTScopeDispatchProof.java
java -cp /tmp/m3-scope com.m3.compiletime.M3ASTLeafIndexSelfTest
java -cp /tmp/m3-scope com.m3.compiletime.M3ASTScopeDispatchProof
```

Recipe: `m3/tooling/shared-frontend/recipe-custody/datasets/ast-scope-dispatch-20261020`, reusing `M3HashPinnedJavaSnapshotRecipe` and the existing Maven/OpenRewrite test/receiver. Only source/test targets are output; documentation, catalogue and CI are admission evidence. Keep **draft** until actual SDK and Java21 CI pass. No JNI change is justified by row traversal; the original M3 JNI/index machinery remains intact.

Remaining admission gates: actual chosen-file javac compilation with binary dependencies, full Eclipse JDT working-copy and NetBeans task ownership, Gradle/Maven plugin lifecycle, full source-built M3JDK21, cross-process cache invalidation and retained-heap benchmarks. Do not claim global single parse across separate JVMs from this narrow selector.
