<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# Gradle provider for the existing M3 compiler session

This optional Java 21 provider implements the source-published Gradle fork SPI
`org.gradle.api.internal.tasks.compile.M3JavacASTProvider`. It attaches the exact
`JavacTask` that Gradle already created to the public
`com.sun.source.util.M3AST` compiler owner. Source inputs, file managers and
diagnostics pass through with their original identities. The provider never
constructs a compiler task, reads a source file, invokes a parser, runs separate
attribution or builds a replacement AST.

The Gradle contract is pinned to `hsoliwal/gradle` commit
`419a77cf61af3924f60678510ed2440230047671`. The receiving implementation class is
`org.gradle.internal.m3.ast.M3GradleASTProvider`, as named by that fork's existing
`M3_AST_INTEGRATION.md`. Ordinary upstream Gradle distributions do not provide
this custom admission SPI.

## Lifecycle and consumer ownership

`prepare` retains the original compiler-input iterable without enumerating or
reading it. `attach` obtains the existing M3AST handle for the original task and
admits consumers before compiler events begin. The returned task forwards
`setProcessors`, `setLocale`, `addModules` and normal `call()` to the original
task. Completion, compiler failure and explicit close release this provider's
compiler references. Gradle continues to own its file manager, incremental
collectors, processor wrappers and per-compilation asset classloader.

There is exactly one consumer-discovery owner. The provider reads the existing
M3AST owner's `M3AST.isPluginSelected(originalTask)` observational signal for the original task's resolved compiler
options when attaching. Javac's actual option and operand processing supplies
this decision; the provider does not maintain another option parser.

| Compiler selection | Consumer discovery |
| --- | --- |
| Original compiler task does not select the builtin `M3AST` plugin | The provider loads `M3AST.Consumer` services from its explicit asset classloader. |
| Original compiler task selects the builtin `M3AST` plugin | The builtin compiler plugin discovers services through javac's original processor/plugin loader; the provider joins the same session and performs no service discovery. |

The plugin route therefore requires consumer JARs on javac's normal configured
processor/plugin path. The provider-only route requires consumer JARs among the
explicit provider assets. A different plugin name such as `M3ASTIndex` does not
select the builtin plugin or suppress provider discovery. An option value such
as the classpath operand in `--class-path -Xplugin:M3AST` is also not a plugin
selection. Javac's effective option table, exact plugin name, ASCII whitespace
delimiters and programmatic NUL-separated plugin segments remain authoritative.
The actual compiler option strings are passed through unchanged.

Provider-only consumer classes must resolve from that same invocation's explicit
asset loader. A service declaration that resolves to a parent-loaded consumer is
refused before its constructor runs; the host's public `M3AST` and Gradle SPI
classes keep their existing parent-loaded identities. Do not copy either API
into the provider assets.

The provider preserves the existing syntax-retention default. Select the
builtin `-Xplugin:M3AST lossless` route when an admitted converter requires that
policy; consumer placement then follows the plugin row above. Consumers share
a projection by reusing the exact `M3AST.Projection` key instance. Equal names
are not an equivalence or reuse authority.

Consumers run in the existing M3AST callback scope. Retain detached results
only. A live compiler tree, type, element, task or callback Unit cannot cross a
worker or process boundary. Separate Maven and Gradle invocations require
qualified detached data and complete compilation-context identity for any
reuse across invocations. This provider creates no process-wide cache.

## Build and select

Build with an actual M3JDK21 compiler and the worker JAR from the pinned custom
Gradle distribution:

```sh
mvn -B -ntp -Dm3.gradle.worker.jar=/absolute/gradle/lib/plugins/gradle-java-compiler-worker.jar package
```

The Gradle worker SPI is a provided host contract and is not packaged inside the
provider JAR. The optional proof profile `patched-compiler` accepts
`-Dm3.compiler.patch=/absolute/compiler-classes` when a separately qualified
patch of the actual M3 compiler sources is being tested on stock Java 21. Such a
run is compiler-module qualification; it does not establish a full source-built
M3JDK image or a source-built Gradle distribution. A stock module descriptor has
no `M3AST.Consumer` service declaration or builtin `M3AST` plugin provider.
Consequently the builtin-plugin coexistence cases require the real M3
compiler module descriptor in a qualified Java 21 image; adding class files
with `--patch-module` alone is insufficient for that gate.

Use the custom Gradle fork's existing input owner:

```groovy
import org.gradle.api.internal.tasks.compile.M3ASTCompilerArguments

tasks.withType(JavaCompile).configureEach {
    javaCompiler = javaToolchains.compilerFor {
        languageVersion = JavaLanguageVersion.of(21)
    }
    options.compilerArgumentProviders.add(new M3ASTCompilerArguments(
        'org.gradle.internal.m3.ast.M3GradleASTProvider',
        files(m3ProviderJar, m3ConsumerJars),
        []))
}
```

Those provider assets participate in the fork's existing `@Classpath`
fingerprint; provider identity and ordered options remain task inputs. A
JavaCompile task restored from cache or considered up-to-date runs no compiler
and creates no new callback data. Command-line javac bypasses the JSR199
provider hook; its builtin M3AST plugin remains the compiler-process entrypoint.

## Qualification boundary

The source tests and standalone probe use the real published Gradle SPI and
the actual M3 compiler owner. They compare original and selected compilation,
source reads, compiler/tree/context identity, one shared projection requested
by two independent service consumers, generated sources, annotation processing,
diagnostics and class bytes. The selected provider must be loaded from its own
artifact when exercising the real Gradle admission path.

Retain exact source, recipe, class/JAR and compiler-module hashes with the
execution logs. Maven/OpenRewrite recipe replay, compiler qualification,
standalone runtime execution and full Gradle-distribution execution are separate
gates. Do not infer a gate from another gate's success or from historical PR
claims. No performance improvement or whole-JDK/whole-Gradle compatibility is
claimed by this provider.
