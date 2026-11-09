<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# Callable native compiler and linker APIs

This selected SDK pack source-builds the original Maven Native API and generic
C/C++ provider packages. Java callers use the existing
`org.codehaus.mojo.natives.compiler.Compiler` and
`org.codehaus.mojo.natives.linker.Linker` interfaces with their original
configuration classes, `CCompiler`, `CLinker` and `ArchiveLinker` providers.
Calling them requires no Maven project/session, plugin realm or Maven process.
The providers execute the chosen native compiler, linker and archiver using
their existing Plexus implementation.

## Caller-selected worksets

`CompilerConfiguration.isCheckStaleCompilation()` defaults to `true`, preserving
the original timestamp-based source/header dependency analysis. A caller which
has already admitted its complete intended workset can set
`setCheckStaleCompilation(false)`. The original compiler then compiles exactly
those supplied sources without obtaining its parser or traversing its source
dependency analyzer. An empty workset emits no compiler command. Missing sources
still fail with `NativeBuildException`.
The admitted mode checks all supplied sources for existence before scheduling
work. Its worker pool also shuts down if later command preparation fails.

```java
CCompiler provider = new CCompiler();
provider.enableLogging(new ConsoleLogger(Logger.LEVEL_INFO, "native"));
CompilerConfiguration inputs = new CompilerConfiguration();
inputs.setWorkingDirectory(workspace.toFile());
inputs.setOutputDirectory(objects.toFile());
inputs.setExecutable(trustedCompilerPath);
inputs.setNumberOfConcurrentCompilation(1);
inputs.setCheckStaleCompilation(false);
Compiler compiler = provider;
List<File> changedObjects = compiler.compile(inputs, admittedSources);
```

The caller must account for source/header content, include dependency edges,
flags, compiler/platform identity and missing outputs before selecting sources.
The returned object list covers the supplied sources; the caller retains the
complete object set needed for linking. The original object naming policy,
environment handling and error semantics remain in place. The new option is
not a dependency planner, a general cache or an ABI comparison. Default timestamp
checks remain insufficient for restored timestamps and changed compiler options.

The fixture uses the original already-qualified `com.m3.file.M3File` module and
one explicitly known header dependency to select worksets. Unchanged content
selects zero files. Same-timestamp source/header edits select their dependent
C++ source. The compiler consumes those selections with zero parser calls; it
does not hash the files again. An unrelated JNI C object remains untouched.

## Source build and selected SDK installation

`DONOR_PINS.json` retains all original source identities. `ADAPTATIONS.json`
records only the two adapted owner postimages. The five dependency JARs are
reviewed, externally supplied inputs at their exact pinned versions and hashes;
this build performs no downloads. The `pom.xml` is an alternative source-only
Maven build. Native execution qualification is the separate explicit fixture
below, not implied by `mvn package`.

```sh
python3 build.py build --java-home /inputs/selected-compiler-sdk \
  --dependencies /inputs/native-api-deps \
  --m3-file-jar /inputs/qualified-m3-file.jar --out /outputs/native-api-build
```

The build compiles all 51 original/adapted sources at their upstream Java 8
release level, retains original compiler warnings, compiles the independent
fixture strictly for Java 21, and packages exact JARs and matching JNI headers.
It creates the existing `com.m3.file` module archive from the already-qualified
JAR without recompiling that owner. `native-api-lock.json` uses the existing
`tool_packs.py` schema. Select that lock through the existing `build_pack.py`
assembler with `--tool-pack native-api`; the assembler owns module-graph
resolution and installation. Other selected API packs may share that image.

The installed classpath API resides at `lib/m3-tools/native-api/`; JNI headers
reside at its `include/` directory. The selected `com.m3.file` module is linked
into the SDK. No new native launcher or alternate pack installer is introduced.

```sh
python3 build.py verify --build /outputs/native-api-build \
  --image /outputs/selected-sdk --gcc /usr/bin/gcc --cxx /usr/bin/g++ \
  --ar /usr/bin/ar --out /outputs/native-api-proof
```

The verifier runs the API caller on the installed SDK and retained dependencies.
It obtains JNI headers through the SDK's JavaCompiler API, compiles real C/C++,
archives and links real objects, and checks native results through `System.load`.
It proves source/header content-delta worksets, unchanged objects, empty work,
default compatibility, missing source/executable refusal, and native compile/link
failure propagation. Separate child JVMs verify normal exit after concurrent
missing-source, object-naming, command-preparation and environment failures.
Receipts bind source, classes, dependency/header bytes,
installed assets and native output identities. A current failed invocation must
not leave a previous successful receipt authoritative.

## Remaining qualification

The bounded native oracle covers Linux with the actual supplied GCC/G++/ar
toolchain. Driver hashes and version output are retained; the complete host
compiler/sysroot closure is not locked. Source-built API providers are qualified
separately from prebuilt dependencies. There is no full upstream MojoHaus test
suite or full M3JDK21 source bootstrap claim. No general native dependency
planner, preprocessor graph, same-basename collision repair, concurrent output
policy, cross-platform response-file proof, process timeout redesign, GraalVM
native-image build, or benchmark improvement is established by this pack.
Canonical Synexia recipe receipt, installed-image execution, and any hosted
checks must be reported separately rather than inferred from source packaging.
