<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Runtime proof from the complete Maven staging build

These receipts were produced after the complete reconstructed backend passed
its Maven install gate on 2026-10-19. The wrapper consumed the actual modular
JAR and JNI library under `/tmp/m3fs-root-work/qualification/m3/fs/target`.
The earlier exploratory image built from a standalone production-only JAR is
excluded from this evidence set.

The wrapper delegates to the existing M3JDK21
`m3/tooling/jmod-recipes/tools/jmod_pack.py` without changing that packer or its
module-packs owner. It links roots `java.se`, `jdk.m3fs`, `jdk.unsupported`, and
`jdk.zipfs`, using the supplied prebuilt JDK 21.

## Files and their meaning

| File | Meaning |
| --- | --- |
| `staging-inputs.json` | Exact Maven JAR/JNI, SQLite runtime, existing packer, probe, JDK release and legal input identities |
| `staging-runtime-proof.json` | Linked-image hashes and successful module/native-origin probe |
| `staging-run.log` | Original pack, probe-compilation, and probe outputs, labeled by source log |
| `staging-legal-proof.json` | Exact byte identities of all five legal files present in the linked image |
| `source-inventory.json` | Hashes of the frozen wrapper and Java probe sources |
| `staging-reproduce.txt` | Exact staging command and launch constraints |

The image is `/tmp/m3fs-native-work/runtime-final-qualification/image`.
`staging-*` identifies this qualification stage. It must not be reported as a
build from the generated receiver checkout; a later generated-receiver run
has separate receipts and input paths.

The probe established `jrt:/jdk.m3fs/com/m3/fs/M3FS.class`, then checked Linux
`/proc/self/maps` for JNI and SQLite loaded from this image's own `lib`
directory. Its launch has no backend JAR, native-path override, or
`java.library.path` override. The wrapper removes `LD_LIBRARY_PATH`,
`LD_PRELOAD`, all `DYLD_*` variables, `JAVA_TOOL_OPTIONS`, `JDK_JAVA_OPTIONS`,
`_JAVA_OPTIONS`, and `CLASSPATH` from the child environment. It exercises
direct I/O, retained snapshot versus live reads, clone isolation, scrub,
checkpoint, and read-only reopening.

**Input JNI and linked JNI hashes are separate observations.** The existing
packer uses `jlink --strip-debug`, which can change the copied native bytes.
`staging-inputs.json` records the unstripped input hash;
`staging-runtime-proof.json` records the actual image hash. SQLite is copied
under its versioned SONAME and its actual image hash is recorded independently.

The packaged legal proof covers the byte-preserved Apache license,
`NOTICE.txt`, `THIRD_PARTY_NOTICES.md`, M3FS native attribution, and the supplied
SQLite distribution copyright. The supplied JDK retains its own legal files.

This is a Linux linked-module and native-runtime proof on a prebuilt JDK 21.
It is not evidence that a full modified M3JDK21/OpenJDK source tree has been
built or has passed the complete OpenJDK test suite. No binary, database,
classfile, dependency archive, Maven settings, or raw JVM-property dump is
included in this receipt directory.
