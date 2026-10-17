# Optional OpenRewrite projection of the shared javac invocation

The adapter converts the original, attributed Java 21 compiler tree into an OpenRewrite 8.17.1 `J.CompilationUnit`. It calls the existing public `ReloadableJava21ParserVisitor` directly with the original task's `Context`, the original `JCCompilationUnit`, and the identical frozen source `String`. It creates no independent compiler or parser and starts no explicit source parse or attribution pass. Publication is refused if the stock visitor advances the host compiler or causes an additional source parse.

This is an optional provider. Its emitted source belongs under `synexia-mindex/compiler-openrewrite/`; it must not enter the core compiler plugin's ordinary source set. The proof compiles and runs the core index with every OpenRewrite JAR absent. The optional provider needs the existing core plugin plus `rewrite-core`, `rewrite-java`, and `rewrite-java-21`, all pinned to 8.17.1.

## Compiler lifetime and whole-file authority

The host must select both options before creating/parsing its compilation task:

```text
-XDcompilePolicy=simple
-XDallowStringFolding=false
```

`ANALYZE` finishes separately for each top-level class. The actual default `BY_TODO` control in the proof produces this sequence: analyze `Alpha`, generate `Alpha`, analyze `Beta`, generate `Beta`, analyze `Gamma`, generate `Gamma`. Converting a complete file at the last event under that policy would encounter earlier types that have already been lowered. The provider refuses the default policy instead of publishing a misleading whole-file LST.

At PARSE, the provider records the identity of every original top-level class. It waits for each distinct class's finished ANALYZE event, resolves each class's environment through the same compiler `Enter`, and requires **exactly `CompileState.FLOW`** for every environment. It rejects missing, partially attributed, or lowered environments and compiler errors. Only then does it invoke the stock visitor once for that file. The rendered LST must equal the frozen source character for character, and each top-level OpenRewrite class must carry its mapped type.

Disabling parse-time string folding preserves the binary expression in `"a" /* comment */ + "b"`. Merely checking that the final printed source matches is insufficient to establish that this expression retained its original tree structure; the proof checks both.

OpenRewrite's type mapper can invoke the host compiler's `ClassSymbol.complete()` for unfinished dependency symbols. The adversarial fixture demonstrates the consequence: ordinary javac parses only `Main`, while mapping an unused method return type in a binary library makes the visitor parse a lazy source dependency. The provider rejects that actual phase re-entry, checks the core parse counter again after conversion in case a completion path swallowed a callback error, and publishes no result. This check detects the additional parse; it does not undo the source read that the completion attempt already performed. Ordinary lazy classfile loading remains possible and is not a zero-I/O claim.

The provider is confined to the compiler callback thread. It creates a fresh visitor per file and shares one `JavaTypeCache` within the invocation. Its completion listener releases all borrowed compiler trees, `JavacTask`, `Context`, source strings, consumer, and type-cache entries. It preserves the first callback failure when closing an incomplete run. Consumers own their emitted OpenRewrite trees.

## Integration

Create the core `M3JavacASTIndex` over the already-created task and its authoritative frozen source resolver, then attach this provider before parsing:

```java
try (M3JavacASTIndex index = new M3JavacASTIndex(
        task, sealedCompilerContext, frozenSourceResolver, limits, 21);
     M3OpenRewriteSharedAST provider = M3OpenRewriteSharedAST.attach(
        index, task, rewriteExecutionContext, List.of(), snapshot -> {
            consume(snapshot.tree());
        })) {
    boolean compilerSucceeded = task.call();
    // The host owns the normal compiler result and any subsequent recipe/application decision.
}
```

The resolver must return the exact immutable characters supplied to that unit's parser. The adapter trusts the core `View.source()` contract and does not reread the source object to reconstruct it. `FrozenCharacters`, a minimal `EncodingDetectingInputStream` subclass, returns that identical `String` to the visitor without an encode/decode copy. The LST uses UTF-8 as its rendering charset; an original file's byte encoding/BOM is not inferred from a character-only compiler view.

Compile and run the optional provider with explicit exports for these five packages:

```text
--add-exports jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
--add-exports jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
```

The compile command uses Java 21 `-source 21 -target 21` with these exports; javac cannot combine system-module exports with `--release`. The ordinary application loader must load the visitor against the host compiler classes. OpenRewrite's usual `JavaUnrestrictedClassLoader` defines separate copies of javac classes; those copies cannot accept the host task's `CompilationUnitTree` and `Context` identities.

## Recipe custody and replay

The reviewed template is `../templates/M3OpenRewriteSharedAST.java.txt`. `prepare.py` seals it into an ABSENT-to-exact-SHA-256 manifest. It does not write a receiving source tree. The existing canonical `M3HashPinnedJavaSnapshotRecipe`, used unchanged, emits the Java candidate through the named recipe `com.synexia.m3.OpenRewriteSharedAST20261017`.

From this directory:

```sh
python3 prepare.py
JAVA_HOME=/path/to/jdk21 /path/to/mvn -B -ntp \
  -Dm3.sharedAst.plugin=../target/m3-ast-sharing.jar test
python3 verify.py --jdk /path/to/jdk21 --plugin ../target/m3-ast-sharing.jar \
  --patch ../target/m3-javac-patch.jar
```

Use the environment's normal Maven settings/cache as appropriate. The SDK test writes the actual Java LST Result to `target/candidate/` and an SDK receipt. The runtime verifier admits only those emitted bytes, validates their template/source seal and successful SDK report, freezes the core plugin and compiler patch JARs for the run, then compiles the candidate and adversarial proof using strict javac warnings. Both runtime JVMs use that exact compiler patch, including the core-without-OpenRewrite check. It does not materialize receiving repository sources. The parent frontend task owns any final receiving recipe and handoff.

## Executed evidence

The retained evidence records these completed checks on Temurin Java 21.0.2+13 and OpenRewrite 8.17.1:

| Check | Result |
| --- | --- |
| Actual named typed SDK recipe | 2 tests, 0 failures/errors/skips |
| Exact candidate emission | 1 Java LST Result, source seal matched |
| Fixed point and input order | 0 further Results |
| SDK refusal | Duplicate target, occupied/drifted target, wrong representation, and changed template |
| Original compiler task | 1 measured task, 2 original units, exactly 2 PARSE events |
| Frozen source custody | Exactly 2 compiler source reads; identical source `String` in the core views |
| Whole-file conversion | 4 top-level ANALYZE completions, exactly 2 LST projections |
| Host tree identity | Original javac node and type references unchanged across every callback |
| Exact syntax | CRLF, Unicode comment, Javadoc link, records, enums, lambda, generic calls, and string concatenation preserved |
| Type attribution | Class/method types populated; record type identity shared between LSTs |
| Generated class files | All 4 class files byte-equal to a separate reference compilation |
| Default compiler policy control | Early generation before the final ANALYZE reproduced |
| Runtime refusal | 9 cases: policy, folding, late attach, partial run, lowered tree, missing positions, module descriptor, foreign task, lazy source completion |
| Lazy dependency control | Ordinary compilation parses 1 file; stock mapping attempts a second parse; guarded run publishes 0 LSTs |
| Class loading | Stock visitor exercised; independent OpenRewrite parser/compiler implementation never loaded |
| Core provider without OpenRewrite | Compiles and runs; 1 parse, 0 demanded MIndex syntax captures |

Runtime classes and compiler outputs are created in a fresh unique temporary directory. An explicit `--work` directory must be empty. Only the resulting receipts and logs are copied back into the task, so a later run cannot admit a restored class output.

The runtime proof passed 841 assertions. `evidence/RESULTS.json` contains the exact adapter, core plugin, compiler patch, and three OpenRewrite artifact hashes. The final run used plugin `f4baf0ae24e58c290a7de403c0571dccbe35c825694729aca40c3408db5b905c` and compiler patch `3f50d59cca1cf7cf2811c0bb1da9314b71240c7f81f37854ed6fc021f03681b4`. `evidence/same-task.log`, `evidence/sdk.log`, and `evidence/without-openrewrite.log` retain actual output.

## Qualification boundary

This provider is pinned to Java 21 and OpenRewrite 8.17.1; other versions are refused. It currently accepts class-bearing compilation units and refuses module descriptors and files without a top-level class. Annotation processors and generated sources have not been qualified in this isolated proof. The ordinary Maven module's service discovery and explicit M3ASTIndex invocation are qualified by the separate normal-module proof.

An OpenRewrite LST is a newly allocated projection of javac's attributed tree. This implementation avoids a second compiler frontend pass and shares the frozen characters and per-invocation mapped types; it does not claim zero additional memory, zero traversal, or that OpenRewrite and javac use the same node representation. No speed or process-memory result is claimed. `SIMPLE` changes host compiler scheduling and may retain more attributed trees at once; the integration selects it explicitly.

Emitting an LST establishes the checked source/phase relationship for that file. A later compiler flow error in another file can still fail the invocation. Recipe changes to an emitted OpenRewrite tree are not automatically applied to javac's ongoing compilation. The provider adds no persistent cache or cross-invocation reuse authority and makes no whole-reactor, jtreg/JCK, production-build, or other-platform claim.

## Primary source lineage

The implementation uses the public constructor and `scan` call from the published OpenRewrite 8.17.1 artifact. Its manifest identifies source commit `3bd8271be963c025ce0856072d0f09360e3184cd`:

- [Java 21 visitor](https://github.com/openrewrite/rewrite/blob/3bd8271be963c025ce0856072d0f09360e3184cd/rewrite-java-21/src/main/java/org/openrewrite/java/isolated/ReloadableJava21ParserVisitor.java)
- [Java 21 parser's existing visitor invocation](https://github.com/openrewrite/rewrite/blob/3bd8271be963c025ce0856072d0f09360e3184cd/rewrite-java-21/src/main/java/org/openrewrite/java/isolated/ReloadableJava21Parser.java)
- [Published Java 21 source archive](https://repo.maven.apache.org/maven2/org/openrewrite/rewrite-java-21/8.17.1/rewrite-java-21-8.17.1-sources.jar)

The default-policy claim is additionally reproduced against the actual compiler used by the proof. Reusable recipe providers remain in the canonical recipe source tree; this task owns only its reviewed template, manifest, proof, and receiving candidate.
