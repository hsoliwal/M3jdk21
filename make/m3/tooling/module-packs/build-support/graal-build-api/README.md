<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# Graal Native Build Tools common APIs

This M3JDK21 build API pack source-builds the original common libraries from
GraalVM Native Build Tools **1.1.13**, commit
`71d6b71f52be9747ffc6b552bce8fe19687218eb`, and their original OpenJSON **1.0.13**
dependency, commit `a83081b08f7f0fad0744aa087620982ec5843c16`. Java callers use the
original packages directly. Maven and Gradle adapters can call these APIs;
their lifecycle receivers are a separate integration gate.

## Original owners

| Capability | Original API | Installed source-built JAR |
| --- | --- | --- |
| Tracing-agent configuration and mode arguments | `org.graalvm.buildtools.agent.AgentConfiguration`, `AgentMode` and its original implementations | `utils-1.1.13.jar` |
| Resource/configuration helpers and JAR scanning | `org.graalvm.buildtools.model.resources.*`, `org.graalvm.buildtools.utils.*` | `utils-1.1.13.jar` |
| Reachability metadata queries, selection and copying | `org.graalvm.reachability.GraalVMReachabilityMetadataRepository`, `Query`, `DirectoryConfiguration` | `graalvm-reachability-metadata-1.1.13.jar` |
| JSON parsing and bean conversion | `com.github.openjson.*` | `openjson-1.0.13.jar` |

The original concrete provider is
`org.graalvm.reachability.internal.FileSystemRepository(Path)`; its public
constructor remains in the upstream **internal** package, with that stability
boundary. There is no new M3 wrapper or alternative selection engine.

For example, a caller with a compatible local repository directory can use:

```java
GraalVMReachabilityMetadataRepository repository = new FileSystemRepository(repositoryDirectory);
Set<DirectoryConfiguration> selected = repository.findConfigurationsFor(query -> {
    query.forArtifacts("org.example:library:1.0");
    query.useLatestConfigWhenVersionIsUntested();
});
DirectoryConfiguration.copy(selected, generatedResources);
```

## Source custody and build

`source-lock.json` records each original path, commit, Git blob, byte hash and
license. The 52 original runtime Java files are unchanged. The additional
`VersionInfo.java` is the exact Linux/LF expansion of the retained upstream
generator, header, build convention and release catalogue. Its values are
Native Build Tools `1.1.13`, native JUnit launcher `1.1.13`, and metadata
repository `1.0.14`. The Gradle generator is provenance only, not a runtime
dependency. Original UPL and Apache licenses remain distinct.

With Python 3.11+ and the selected Java 21 SDK:

```sh
python3 build.py --java-home "$M3_SDK" --output "$PROOF/graal-build-api"
```

The offline builder checks the source inventory before compiling at release
17, builds all three JARs from source, retains the original default filter
resource and licenses, and emits `tool-packs.lock.json`. It refuses an existing
output directory. No Maven, Gradle, downloaded product JAR or metadata archive
is needed. Original OpenJSON's bean API requires `java.desktop`; common APIs
also require `java.net.http`, and `JarScanner` dynamically needs `jdk.zipfs`.
These explicit roots are retained alongside `java.base`.

Pass the emitted lock and `--tool-pack graal-build-api` to the existing
`m3/tooling/module-packs/build_pack.py` owner. Its existing loader and installer
place JAR assets in `lib/m3-tools/graal-build-api/` and provenance/licenses in
`legal/m3-graal-build-api/`. The builder implements no additional installer.
Callers use the installed JARs on an explicit classpath; no framework module
descriptor or launcher is invented.

## Qualification

`qualify.py --java-home "$M3_SDK" --build "$PROOF/graal-build-api"
--dependencies "$TEST_ARTIFACTS" --output "$PROOF/graal-upstream-tests"`
compiles and runs the retained original OpenJSON and common-library tests.
`test-dependencies.json` pins the test-only JUnit/Jimfs/Guava closure. Original
ZIP fixtures are stored as `.zip.base64.txt`, verified against their original
binary hashes, and decoded only into the proof workspace at their original
resource paths. The original reporting tests use local mock HTTP servers and
therefore need socket permission and `jdk.httpserver`; those are test needs.
The original OpenJSON performance class is retained but is outside the
upstream unit-test naming convention.

`contract/src/test/java/GraalBuildApiContract.java` is an independent finite installed-image
oracle. Compile it against the three installed JARs and run it with those JARs
and its own classes only. Its single argument is a new work directory. It
checks class origins inside `java.home`, agent option/filter behavior, exact,
latest/default, forced and missing metadata selection, required-module policy,
copying, malformed inputs, the ZIP provider and OpenJSON bean conversion. That
process uses no Maven, Gradle, test framework, network or Native Image driver.

Agent argument construction does not load the native agent. The full common
library retains utilities that may contact a configured service or invoke an
external tool when a caller explicitly uses them. This pack supplies neither
the agent native library nor the Native Image compiler, hosted SDK, SubstrateVM
driver or a GraalVM installation. AOT generation/execution, Java 21 support for
newer Graal layer workflows, whole-JDK bootstrap, cross-platform parity,
Maven/Gradle lifecycle receivers and comparative resource gains remain separate
gates. JNI/C/C++ compilation is owned by the separate native API pack.

Primary contracts: the retained `upstream/native-build-tools/common/docs/`
specifications, the [original common-library guidance](https://graalvm.github.io/native-build-tools/latest/alternative-build-systems.html),
and the pinned upstream source paths in `source-lock.json`.

### Text custody paths

The unchanged canonical text recipe admits these assets with a `.txt` suffix:
the three upstream Native Build Tools `LICENSE` files, OpenJSON `NOTICE`, the
release version catalogue `libs.versions.toml`, and the two encoded ZIP fixtures.
Their retained bodies are unchanged. `source-lock.json` keeps each original
upstream path, Git blob, byte hash and license, alongside its admitted custody
path. The builder restores original `META-INF/LICENSE` and `META-INF/NOTICE`
names in the JARs, and `qualify.py` restores test resource paths from
`upstream_path`. This layout correction changes no runtime Java or test body.
