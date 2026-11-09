# M3FS VFS core: Java, JMOD and linked-image qualification

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

The relocated Apache Commons VFS 2.10.0 provider core belongs to the existing
`jdk.m3fs` module. `com.m3.fs` remains exported; the compiled VFS API/SPI packages
under `com.m3.fs.vfs` are exported alongside it. This does not introduce a second
filesystem module or rename the external Commons dependency modules.

## Build boundaries

The default Maven profile is `m3-vfs-core`. All 311 imported Java files stay in
source custody. The profile compiles 237 core/local/RAM/URL/resource/temporary/
ZIP/JAR/GZIP/BZIP2/TAR/TGZ/TBZ2 source files and omits the FTP, FTPS, HTTP client
families, SFTP, `LRUFilesCache`, and the old HTTPClient-3-backed `URLFileName` and
`URLFileNameParser`. The real URL provider remains compiled using the upstream
generic URL types and explicitly registers `jrt` for named-module resource access.
The donor manager/parser recognize registered schemes; arbitrary unregistered URL
schemes are not claimed as supported. Source presence does not establish
remote-provider support. The network-free hosted-name probe uses an isolated
manager with explicit generic-provider registration, without enabling omitted
HTTP clients in the core registry.

Maven compiles the imported provider sources in a separate `process-sources`
execution with `-Xlint:all`, preserving their historical warnings in build logs.
M3-owned production sources retain `-Xlint:all -Werror`. The module descriptor
uses `--module-path` with the compile dependency closure. Its two related lint
exceptions are `requires-automatic` and `requires-transitive-automatic`, because the unchanged Commons
Logging 1.2 Maven input is an automatic module. The JMOD lane gives that archive
the reviewed explicit descriptor via the existing packer's manifest interface.

The native CMake configure/build executions, required JNI property, `-Xcheck:jni`,
and CTest verification execution are retained. There is no native skip option in
the runtime packaging wrapper.

```sh
mvn -B -ntp -f m3/fs/pom.xml -Pm3-vfs-core clean verify
python3 m3/fs/verification/test_vfs_packaging.py
```

The observed offline baseline failed before compilation because
`org.codehaus.mojo:exec-maven-plugin:3.3.0` was unavailable in the supplied Maven
cache. CMake, SQLite development headers, and the native build outputs were also
unavailable in this qualification environment. A Java helper pass is not a Maven
or native gate pass.

## Pinned dependency closure

`tools/vfs-dependencies.lock.json` records each exact archive SHA-256 and both
original legal-resource hashes. It admits exactly these five modules:

| Artifact | Version | Module | Descriptor input |
| --- | --- | --- | --- |
| Commons Logging | 1.2 | `commons.logging` | Injected reviewed descriptor |
| Commons Lang | 3.18.0 | `org.apache.commons.lang3` | Published descriptor |
| Commons IO | 2.21.0 | `org.apache.commons.io` | Published descriptor |
| Commons Compress | 1.28.0 | `org.apache.commons.compress` | Published descriptor |
| Commons Codec | 1.19.0 | `org.apache.commons.codec` | Published descriptor |

The first actual descriptor compilation retained `-Werror` and refused an
automatic-module warning. Adding both specific automatic-module lint exceptions
was then checked with the actual JDK 21, complete compiled product classes, and
pinned dependencies; descriptor compilation completed with no diagnostics. This
exception applies only to descriptor compilation.

These versions are the exact supplied artifacts qualified for this bounded
profile; some differ from the Apache VFS 2.10.0 parent dependency defaults.
Upstream identity, license text, and notices remain unchanged. The pinned
Commons Compress descriptor brings its required Java desktop/logging and Commons
runtime closure; its static optional integrations are not silently enabled.

Only Commons Logging and Commons Compress are transitive requirements of
`jdk.m3fs`, because the exported provider SPI exposes `Log` and TAR types.
Commons IO, Commons Lang, and `java.xml` are implementation requirements.

## Explicit Java qualification lane

After canonical recipe materialization, seal its reviewed source inventory as a
TSV with exactly `path` and `sha256` columns, relative to `m3/fs`. The required
inventory is returned by `vfs_pack_support.source_inventory(module_root)`. It
includes all imported Java custody files, current owned production sources,
module descriptors, resources, legal files, POM, qualification tools/dependency
lock, and the independent consumer source. Record the lock's hash with the
canonical materialization receipt. Do not replace an existing source lock by
rehashing unexplained changes.

```sh
python3 m3/fs/tools/qualify_vfs.py \
  --module-root /path/to/M3JDK21/m3/fs \
  --repository-root /path/to/M3JDK21 \
  --jdk-home /path/to/jdk21 \
  --dependency-cache /path/to/reviewed-maven-cache \
  --source-lock /path/to/canonical-materialization/SOURCE_LOCK.tsv \
  --out /new/empty-parent/m3fs-vfs-java-proof
```

The helper refuses source inventory/hash drift, missing dependencies, dependency
hash drift, descriptor drift, changed legal bytes, and an occupied output path.
It compiles all current M3FS product Java sources plus the selected VFS core,
creates the modular product JAR with the POM's OSGi metadata, and invokes
`m3/tooling/jmod-recipes/tools/jmod_pack.py` without replacing or modifying it.

`JMOD_IMAGE.tsv` carries the product JAR and four dependencies with published
descriptors. `DESCRIPTOR_MANIFEST.tsv` carries Commons Logging with exactly one
`unit_jars` path and the target
`src/main/dependency-modules/commons.logging/module-info.java`. The existing
module-pack graph selects the runtime closure and the existing packer calls
JMOD/jlink. The helper writes a temporary consumer module descriptor under its
output solely to test a separate named module requiring `jdk.m3fs`.

The consumer runs with the linked image's `bin/java`. It checks module identity
for `M3FS`, `MFiles`, `MFile`, `M3VFS`, the monitored-content types, and the
observation/delta types; exact dependency presence; provider registration; RAM
and temporary writes; local/ZIP/JAR/GZIP/BZIP2/TAR/TGZ/TBZ2 reads; and
resource-to-`jrt:` access through the generic URL provider. Network-free URL name checks cover relative
resolution, preserved queries, literal percent escapes, UTF-8 and explicit
charsets, and direct names with and without a host.

The same named consumer uses `MFileContent` default monitored input, borrowed
progress, a synchronous shadow, successful EOF/close and complete content hashes.
It attaches those full fixture reads to fresh observations and checks the
`MFileDelta` content-evidence result and callback. One `.extensions("txt")` /
`.enterArchives(..., 1)` plan selects the existing ZIP fixture's text entry and
reads its byte layer. These admission checks supplement the focused unit
consumers without claiming that a live source was atomically sampled. Public
logging and TAR types are imported to check the transitive SPI requirements.

The product JAR retains every M3FS legal file under `META-INF/m3fs-legal`. The
exact upstream VFS `LICENSE.txt` and `NOTICE.txt` reside beneath its
`commons-vfs2` subdirectory. Every dependency's original `META-INF/LICENSE.txt`
and `META-INF/NOTICE.txt` remain in its classes/resources. The shared packer's
single legal tree is additionally namespaced by owner and copied into each
resulting JMOD; legal-file duplication is intentional. The helper compares
these bytes in the JARs, JMODs, and actual image legal tree.

Evidence is written only after the corresponding action. Inspect
`inputs.json`, `provider-compile.log`, `m3fs-strict-compile.log`,
`module-description.txt`, both packer manifests, `shared-packer.log`,
`pack/SELECTION.json`, `consumer-compile.log`, `consumer-run.log`, and
`qualification.json`. A missing `qualification.json` means the helper did not
finish successfully. The JSON explicitly reports Maven, native SQLite, and the
source-built M3JDK21 gates separately.

## Full native runtime wrapper

Once the Maven/native artifacts exist, the original native wrapper accepts the
provider closure as an additive input. Native and SQLite library arguments stay
mandatory; Linux SONAME, `$ORIGIN`, JNI execution, and native mapping checks stay
active.

```sh
python3 m3/fs/tools/package_runtime.py \
  --repository-root /path/to/M3JDK21 \
  --jdk-home /path/to/jdk21 \
  --jar /path/to/M3JDK21/m3/fs/target/m3-fs-0.1.0-SNAPSHOT.jar \
  --native-library /path/to/M3JDK21/m3/fs/target/native/libm3fs.so \
  --sqlite-library /path/to/libsqlite3.so.0 \
  --provider-dependency-cache /path/to/reviewed-maven-cache \
  --out /new/empty-parent/m3fs-native-runtime
```

The wrapper refuses a VFS-enabled product JAR without the complete pinned
provider closure. It uses the same existing JMOD packer and retains the original
`M3FsRuntimeProbe` with `-Dm3fs.native=required`. Final distribution admission
still requires rebuilding from the actual modified OpenJDK/M3JDK21 source tree
and running that owner's required tests.
