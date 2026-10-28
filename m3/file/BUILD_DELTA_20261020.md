<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 build input delta — source/class evidence owner

M3JDK21 is one Java/HotSpot/JNI SDK, not an external M3 wrapper around an unmodified JDK. This change extends its existing `com.m3.file` module. The original `M3File` NIO owner and the public `java.nio.file`, `java.util`, Maven and Gradle interfaces are unchanged. Accepted M3JDK21 build-tool consumers must use this owner and the existing M3AST/javac facts, not create a competing file scanner, parser, cache or native payload store.

## Scope and exact semantics

`M3FileDelta` holds sorted `String[]` paths, `byte[]` roles, `long[]` sizes, packed 32-byte SHA-256 lanes, and optional caller-supplied class ABI evidence. Each observation has a typed role (SOURCE, CLASS, BUILD_INPUT, DEPENDENCY, RESOURCE). The comparison uses a stable two-pointer merge of typed identities and reports ADDED, REMOVED, MODIFIED or ATTESTATION_CHANGED. A record projection allocates only on request; the snapshot retains no per-file entry objects. The whole snapshot binds the caller's opaque build-context SHA-256 to its exact bytes and versioned policy.

`M3FileDeltaCli` reads versioned, strict UTF-8 manifests via `M3File`, then emits a deterministic read-only receipt. It runs through the existing module entry point as `java --module-path <compiled-module-path> -m com.m3.file/com.m3.file.M3FileDeltaCli before.tsv after.tsv before-context-sha256 after-context-sha256`. No source or cache entries are modified.

**The host must supply a complete inventory and context hash.** Build tool, compiler flags, toolchain/JDK, build scripts, plugin/processor versions, dependency resolution, environment and all observed task inputs are part of the host's admission contract. Same source/class SHA alone does not authorize skipping tasks. A class ABI digest requires trusted compiler facts; filenames and content hash do not imply ABI equality. Annotation processing, incremental invalidation, task dependencies, output publication and cache keys remain owned by Maven/Gradle/javac. Changed compiler environment with identical files is not a cache hit.

Captures hash file bytes rather than timestamps and use existing `M3File.of(path)` with NOFOLLOW_LINKS and bounded bytes. File path segments and ancestral symbolic links are checked; a concurrent rename remains a filesystem race without an owning generation. Nonregular files, oversized reads, duplicate typed paths, traversal, malformed digests and invalid manifests are refused.

## Recipe ownership and target qualification

The canonical named recipe is `com.synexia.m3.M3Jdk21BuildDelta20261020` under `hsoliwal/com.synexia:synexia-openrewrite-recipes`. Its exact source-sealed Java/text manifests and fixtures must be executed before promotion; copied postimages do **not** constitute an OpenRewrite SDK run. This source is first-party Apache-2.0, preserving original authored expression and downstream OpenJDK licences separately. LeetCode/HackerRank/GeeksforGeeks are problem-category references only; no challenge or donor code is copied.

Locally executed against real existing `M3File` and OpenJDK 21.0.12.1: `javac --release 21 -Xlint:all,-module -Werror` compiled the complete module and test sources; standalone real-javac/JVM regression reported 58 checks, and an independent contract probe 15 checks. The Maven/JUnit test adapter is in `src/test/java`. Maven, Gradle fork tests, canonical OpenRewrite SDK execution, full M3JDK21 installed-image/JMOD and native/JNI qualification, cold/warm benchmark and processor/ABI integration remain **NOT_RUN**. The target receiving order remains String -> arrays -> collections -> AST/compiler; no admission phase advances from this candidate.

## Product integration decisions still requiring proof

- Maven: preserve lifecycle/plugin contracts and consider the official Maven build-cache extension before changes to reactor behavior; consume this delta only as advisory evidence until end-to-end input/output closure is proved.
- Gradle: preserve configuration cache, incremental compile, ABI-based compile avoidance and task/build cache; a scan performed after Gradle has tracked task inputs cannot replace Gradle's own invalidation decision.
- Javac/M3AST: expose compiler-produced source-to-class, ABI and attributed symbol dependencies under an exact epoch. Only a compiler-verified reverse-dependency graph may select affected compilation units.
- JNI/HotSpot: no new JNI entrypoint is warranted for byte hashing without benchmarks and an explicit ownership/region/pin-release contract. Existing native M3FS/content-addressed stores remain authoritative for persistent immutable artifacts.

No universal speedup, semantic equivalence or complete fork integration is claimed by this bounded SDK-owned receiver.
