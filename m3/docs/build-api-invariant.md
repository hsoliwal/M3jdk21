<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# Build capabilities are callable M3JDK21 APIs

Maven, Gradle and their integrations may consume installed build capabilities
through the original APIs. A Maven goal or command-line runner is an adapter to
those capabilities. It is not a required execution boundary for an API caller.
M3JDK21 owns the delivered product. Canonical recipes and convergence evidence
remain in Synexia, without a Synexia runtime dependency.

This contract composes the existing [content-delta contract](build-delta-invariant.md),
the [shared compiler frontend](../tooling/shared-frontend/README.md), and the
existing module-pack installer. It does not replace the Maven/Gradle build model,
dependency planner, native toolchain or OpenRewrite scheduler.

## Original public owners

| Capability | Callable owner | Caller supplies | Original result |
| --- | --- | --- | --- |
| Source/class content observations | `com.m3.file.M3File` | Bounded source/class file and previous admitted fingerprint | `Fingerprint` and added/removed/content-change mask |
| Attributed Java input | `com.sun.source.util.M3AST` and `org.m3jdk.tooling.rewrite.M3RewriteFrontend` | One actual compiler task, successful attribution, retained source and intended coverage | Original compiler units and converted OpenRewrite trees |
| Recipe execution | `org.m3jdk.tooling.rewrite.M3RewriteTool.execute` | Configured original `Recipe`, admitted `SourceFile` workset, caller `ExecutionContext` | Original `org.openrewrite.RecipeRun` |
| C/C++ compilation | `org.codehaus.mojo.natives.compiler.Compiler.compile` and generic-C provider `CCompiler` | Original `CompilerConfiguration` and admitted source files | Original object-file list |
| Native linking | `org.codehaus.mojo.natives.linker.Linker.link` and generic-C provider `CLinker` | Original `LinkerConfiguration` and admitted object files | Original linked output file |
| JNI header generation | `javax.tools.JavaCompiler` | Actual Java compilation with `-h` | Headers for the selected Java/native ABI |
| Graal agent configuration | `org.graalvm.buildtools.agent.AgentConfiguration` | Original agent mode/options and output location | Original agent command-line arguments |
| Graal reachability metadata | `org.graalvm.reachability.GraalVMReachabilityMetadataRepository` | Original query and selected metadata repository | Original directory configurations |

Original public packages, interface names, result types and donor licenses remain
intact. No new generic plugin facade, parser, cache or scheduler is required.
The source-built native API/provider intake is bounded to MojoHaus
`maven-native-1.1.0`; it does not imply every native provider is installed.

## OpenRewrite consumes retained trees

```java
RecipeRun run = M3RewriteTool.execute(recipe, admittedSources, execution);
```

The method snapshots list membership and retains the input tree objects. It
checks the existing full recipe configuration, invokes the original
`Recipe.run` scheduler, and returns the original changeset, recipe attribution
and data tables. It does not create a compiler task, invoke a parser, reread
source files or apply patches. The existing CLI uses the same method after its
one explicit Java compilation and admission checks.

The caller owns the execution context and output acceptance. OpenRewrite may
report a handled error through that context and still return a run. Exceptions
that escape OpenRewrite propagate without a new wrapper. The CLI retains its
separate rule that recipe errors prevent patch publication.

Workset admission remains semantic. A scanning, generating or cross-file recipe
may require unchanged members of the intended source set. Supply their already
admitted trees when required; do not reduce every recipe to changed files only.
An empty source set can still have meaning for a generating recipe. Reuse is
valid only while compiler/classpath/recipe context and the source-tree contract
remain valid. An unchanged byte digest alone does not validate old attribution.
Recipes may invoke their ordinary synthetic-template parsers or other code.
The API boundary makes no promise about an arbitrary recipe's internals.

## Native compilation consumes an admitted workset

The original compiler configuration gains an additive option:

```java
configuration.setCheckStaleCompilation(false);
List<File> objects = compiler.compile(configuration, admittedSourceFiles);
```

The default is `true`, preserving the upstream timestamp/dependency-analysis
path. When the caller sets `false`, the compiler validates source presence and
compiles the supplied files without calling its dependency parser or timestamp
analyzer. An empty admitted workset performs no native compilation. The original
provider still constructs and runs the selected native compiler commands, and
reports failures through its original API. Disabling stale checks transfers
selection responsibility to the caller; it does not introduce another planner.

The original linker already provides `setCheckStaleLinkage(false)`. A caller
that has admitted a link action can use that policy without another timestamp
decision. Native compilers and linkers remain actual platform tools; the callable
Java API removes the Maven lifecycle requirement, not the toolchain itself.

Use the installed Java compiler's `-h` capability for JNI headers. The historical
`javah` API in the upstream source set does not restore the removed `javah` tool.
The selected native pack carries the matching JNI headers and their original
OpenJDK license notices.

## Graal common build APIs remain independent of Maven/Gradle

The selected `graal-build-api` capability source-builds the original Native Build
Tools `utils` and `graalvm-reachability-metadata` modules from release `1.1.13`,
commit `71d6b71f52be9747ffc6b552bce8fe19687218eb`. Their original UPL source,
public packages and behavior remain intact. The OpenJSON dependency is likewise
source-built from its original `1.0.13` source and retains Apache-2.0 licensing.

Callers use the original `AgentConfiguration`, `NativeImageUtils`,
`GraalVMReachabilityMetadataRepository`, `Query` and `DirectoryConfiguration`
owners. The existing `FileSystemRepository` supplies local repository behavior.
No new metadata parser, agent or Maven/Gradle adapter is needed to call these
APIs. Runtime callers own their query, metadata location and output policy.
Upstream permits other repository forms; a local installed fixture is not a
claim that every remote or archive mode has been qualified by that fixture.

Constructing agent arguments does not install or run `native-image-agent`.
Selecting metadata does not perform AOT compilation. Native Image driver,
hosted compiler, agent and generated-runtime installation retain separate
matched-toolchain and actual-execution gates.

## Delta admission is more than a file timestamp

`M3File` identifies raw content changes for sources, classes, headers and objects.
Its retained fingerprint is a byte count and four SHA-256 words. Comparing
previous/current observations performs no new I/O. File absence is an explicit
caller observation; read errors must not be converted to absence or equality.

The existing build owner must combine these observations with its semantic
dependency edges and configuration before selecting work:

| Observation | Required owner decision |
| --- | --- |
| Source bytes changed | Recompile affected units and apply existing dependency/processor rules |
| Class bytes changed | Distinguish implementation, ABI, inlined constants and compiler/processor inputs before deciding downstream work |
| Native header changed | Include all compilation units dependent on that header, including relevant generated/system-header context |
| Only metadata changed | Do not infer a content change from the timestamp alone |
| Toolchain, options, classpath, recipe or processor context changed | Invalidate the affected admitted work even if source bytes match |
| Expected output missing or changed | Reestablish the output contract before reusing a previous result |
| Inputs and full execution context still admitted | Reuse valid facts/results or supply an empty native workset as appropriate |

The APIs do not persist a second build cache. Maven/Gradle retain their original
configuration, dependency, ABI/constant, annotation-processor and generated-output
owners. Source changes, class changes and ABI changes are distinct facts.

## Installation and qualification

The existing module-pack owners select and install capabilities. OpenRewrite
keeps its original framework jars and public classes in the selected tool pack.
The native and Graal common packs build their original API/provider source,
preserve their dependency and license closure, and install the resulting callable
classes as selected capabilities. These libraries do not become `java.base`
dependencies.

Evidence must distinguish source compilation, recipe replay, module packaging,
installed API execution, full product bootstrap and platform qualification.
The finite native fixture owns an explicit source/header edge and uses actual
`M3File` fingerprints to select it. It tests native compile/link/JNI load and
no-rescan workset consumption; it does not qualify a general native dependency
planner or an arbitrary Maven/Gradle project.

GraalVM Native Image is a separate AOT toolchain. A successful C/C++/JNI build
does not establish Java-to-native-image compilation. Its plugin, hosted compiler,
reachability configuration, runtime/toolchain and platform gates require separate
source-bound admission.

Maven/Gradle planner receivers, other plugin families, all-platform behavior,
full M3JDK21 bootstrap and comparative CPU/heap/native-memory measurements remain
separate gates. No speedup, universal plugin absorption or ordered receiving-phase
advancement follows from the bounded API tests.
