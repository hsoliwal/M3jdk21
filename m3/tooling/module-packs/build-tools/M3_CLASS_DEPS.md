<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 native classfile dependency build tool

**M3JDK21 is the integrated JDK/HotSpot/JNI product, not a wrapper.**
This compiler-adjacent tool is installed directly in the existing
`jdk.jdeps` JDK module through `java.util.spi.ToolProvider`, with name
`m3-class-deps`. It **reuses the original** OpenJDK
`com.sun.tools.classfile.ClassFile` parser and
`Dependencies.getClassDependencyFinder()` /
`Dependencies.getAPIFinder(ACC_PUBLIC)`. No second bytecode parser, Maven
runtime dependency, new filesystem, JNI ABI or separate plugin classloader
is introduced.

The API `M3BuildClassDeps.inspect(path, scope, maxBytes, maxEdges)` reads
one regular .class file with NOFOLLOW_LINKS, bounded up to 32 MiB,
calculates exact SHA-256 of the bytes analyzed, and retains a sorted
unique `String[]` of dependency target class names. During collection
the only state index is a dynamic open-addressed `String[]`; there is no
retained per-reference Entry/Node object graph. Dependency identities
are derived from the original JDK dependency visitor, not guessed from
source text or printed compiler ASTs.

Wire schema `M3JDK21_CLASS_DEPS_V1`:

```text
M3JDK21_CLASS_DEPS_V1<TAB>CLASS|PUBLIC_API<TAB>sha256hex<TAB>Base64URL(class name)<TAB>dependency count
D<TAB>Base64URL(dependency class name)
```

Names are lossless, encoded Base64URL without padding so arbitrary JVM
identifier bytes cannot corrupt tab/newline framing. This format is
distinct from M3AST/javac source facts, plugin-specific rule results,
Maven effective dependency models or ABI lookups.

## Actual validation

Stock OpenJDK 21.0.12.1 `--patch-module jdk.jdeps` compiled the exact
new source with `javac -Xlint:all,-module -Werror`. The original-JDK
differential `M3BuildClassDepsContract` ran against genuinely compiled
Java21 class files: **M3_JDK_BUILD_DEPS_PASS checks=283**. It compared
the emitted class dependency targets with the existing jdeps finder,
exercised API-only vs full-class dependencies, exact hash, 250
deterministic trials, malformed files, symlinks, edge/byte budgets,
and direct ToolProvider output.

## Honest pending qualification

The modified `src/jdk.jdeps/share/classes/module-info.java` additionally
registers the new provider, preserving both original providers. Full
source-built M3JDK21 module-image and ToolProvider discovery, JDK jtreg,
Windows/Android, JNI integration, matched Maven 4 and Gradle source builds,
classfile verification under adversarial concurrent writers and warm/cold
performance have **not** been executed. The patch-module test uses a stock
JDK module as a donor ABI, not a complete integrated HotSpot product proof.

This first tool covers a *single class file*, not JAR/repository resolution
or full dependency:analyze. It produces candidate class or declared-API
dependency edges, not proof of source lookup/annotation/ABI completeness.
M3File, M3AST and M3FS remain authorities for source generation, compiler
semantics and durable reuse. Original Maven/Gradle compilers retain toolchain,
processor, lifecycle and task-output ownership.