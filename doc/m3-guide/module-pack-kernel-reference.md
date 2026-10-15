<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Module-pack inspection kernel reference

## Scope and authority

This reference covers all four production owners in `com.m3.tools.modulepack`: `M3ModuleArtifact`, `M3ModuleInspector`, `M3ModuleGraph` and `M3ModuleInventory`, at public source revision `510dfa27b0679ce33348140afc57b558b8853802`. Their 335 source lines, declared members, generated record surface and seven kernel test/helper files are routed in the [source and evidence ledger](module-pack-kernel-source-coverage.md). This is a complete source-contract review of that bounded four-owner kernel, with bounded caller evidence, not completion of the module-pack project, other packs or the repository. Builds, tests, recipes, policy verifiers, image assembly, runtime/native qualification and benchmarks were **NOT_RUN** in this documentation authoring/review pass.

Start with the existing [module-pack README][readme] for project purpose, commands, assembly, JavaFX and historical-proof boundaries. This page adds call-level contracts rather than another assembly plan. The [community admission document][admission] remains the distribution-planning authority and explicitly labels its own status `PROPOSED_ADMISSION_POLICY`. The [porting invariant][invariant], [naming/receiving map][mapping] and [agent instructions][agents] retain their ownership, rights and qualification rules. Documentation does not advance the active String qualification stage or any later receiving phase. Preserve per-file Apache-2.0, OpenJDK and third-party rights; an inventory or digest is not licensing approval.

### Build and module boundary

The declared control-reactor route is [m3/pom.xml][reactor] → [tooling/module-packs/pom.xml][parent-pom] → [kernel/pom.xml][kernel-pom]. Aggregation is not parent inheritance: `module-packs` is independently parentless; `kernel` inherits `com.m3.tooling:module-packs:0.1.0-SNAPSHOT` and declares artifact ID `module-pack-kernel`. Its [JPMS descriptor][module-info] names `com.m3tooling.modulepack` and exports `com.m3.tools.modulepack`; it declares no additional `requires`. These are external tooling APIs, not Java SE additions or bootstrap dependencies.

The module-pack parent declares compiler release 21, Java `[21,22)`, Maven `[3.9,)`, UTF-8, compiler warnings as errors with `-Xlint:all`, and classpath compilation/testing (`useModulePath=false`). Kernel has only a test-scope JUnit dependency; the managed version is `5.10.2-m3-source-1`, not the unmodified `5.10.2` property value. Surefire adds `jdk.jlink,java.compiler`, fails if no tests are found, and the JaCoCo `verify` check requires bundle line and branch covered ratios of `0.99`, with no configured coverage exclusions. Optional atom-pattern profiles attach external Synexia/OpenRewrite authoring plugins; their presence is not a production dependency or proof they ran. POM declarations do not establish dependency availability, effective-build success or achieved coverage.

## Artifact value and ownership

[`M3ModuleArtifact`][artifact] is a public record with six components:

| Component/accessor | Actual stored contract |
| --- | --- |
| `path()` | Non-null `Path`; the inspector supplies `toRealPath()` output. The public constructor also accepts an arbitrary relative or nonexistent path. |
| `descriptor()` | Non-null `ModuleDescriptor`. The constructor does not insist that it is explicit, inspect its archive or cross-check its packages. |
| `sha256()` | Non-null string. Inspector results contain lowercase SHA-256 hex; direct construction accepts any string, including empty or non-digest text. |
| `kind()` | Non-null string. Inspector results use `JAR` or `JMOD`; the constructor does not enforce those values. |
| `nativeEntries()` | `List.copyOf` snapshot; null list/elements fail with `NullPointerException`. No sorting or filename validation in the constructor. |
| `resources()` | Same snapshot/null behavior; this is a name inventory, not retained resource bytes. |

The compact constructor performs those checks/copies only. The generated accessors return the stored values; generated `equals(Object)`, `hashCode()` and `toString()` have record value semantics across all six components, including list order. No path canonicalization, archive digest computation or archive I/O occurs through these record methods. The immutable list snapshots prevent later mutation of caller lists from changing the record, but the record does not freeze the filesystem object named by its path or certify the truth of caller-supplied metadata. No stream, native handle or close obligation is transferred to the artifact consumer.

## Archive inspection

[`M3ModuleInspector()`][inspector] has no state. `inspect(Path)` returns a fully constructed artifact or propagates a failure; it does not load archive classes, extract entries, rewrite bytes, download dependencies, install modules or invoke `jlink`.

### Ordered inspection path

1. `input.toRealPath()` resolves an existing real path, normally following symbolic links. The resolved filename, not necessarily the originally supplied symlink name, must end with case-sensitive `.jar` or `.jmod`. There is no independent regular-file check. Filesystem errors can precede suffix validation.
2. `sha256(path)` reads the complete file before archive opening.
3. `open(path,jmod)` opens JARs through `JarFile` with verification enabled, read-only mode and the explicitly selected Java **21** view. For JMODs it first reads and checks the exact four-byte `JM 01 00` header (`0x4a4d0100`), closes that stream, then opens a `ZipFile` without rewriting the header.
4. `validateEntries` scans the **whole physical archive**, before descriptor selection or effective-entry processing. Exact duplicate entry names are refused, including directory entries. Signature-marker matching uppercases with `Locale.ROOT` and refuses names beginning `META-INF/` and ending `.SF`, `.RSA`, `.DSA` or `.EC`. This is a lexical marker rule over raw entry names, not comprehensive certificate/signature validation. In particular, it does not strip or normalize JMOD `classes/` prefixes before applying the rule.
5. `descriptor` reads `classes/module-info.class` directly for a JMOD using `ModuleDescriptor.read`. A JAR uses `ModuleFinder.of(path).findAll()`, demands exactly one module and refuses automatic descriptors. JAR descriptor discovery delegates to the executing JDK's module finder, whereas the archive entry view is explicitly pinned to 21; the declared supported build/test VM is Java 21. Do not extrapolate cross-JDK descriptor equivalence from the fixed entry-view argument.
6. `effectiveEntries` materializes `JarFile.versionedStream()` for JARs and `zip.stream()` for JMODs. For a properly marked multi-release JAR, classes/resources are processed through the effective Java-21 view; unused future overlays are not class-header checked. Whole-archive duplicate/signature checks above still apply. JMOD processing uses all entries, without an effective-version overlay.
7. Directory entries are skipped. Every remaining entry whose name ends with case-sensitive `.class` has its first eight class-header bytes inspected. All other entry names enter `resources`; native-looking names also enter `nativeEntries` under the rule below. The descriptor reader may parse more than a header for the module descriptor, but ordinary class bodies are not bytecode-verified by this loop.
8. A second complete-file SHA-256 must equal the first. Only then is the artifact returned with the first digest, resolved path, discovered descriptor, kind and lexically sorted immutable name lists. The archive remains inside try-with-resources until return/exception cleanup.

The fixed class-header rule in `validateClass` requires magic `0xcafebabe`, major **45 through 65 inclusive**, and minor **zero for majors 56 through 65**. For majors **45 through 55**, any unsigned 16-bit minor is permitted, including `65535`; describing all such values as rejected preview classes would be incorrect. Out-of-range majors or forbidden minors produce `NOT_JAVA21_NONPREVIEW`. Truncated headers fail through I/O/EOF rather than that token. This predicate is neither full bytecode verification nor API/linkage, module accessibility, native ABI, operating-system or behavioral compatibility proof.

### Resources and native-looking names

Classification operates on entry **names**, without native loading or resource-body extraction. For each non-class, non-directory name, lowercase using `Locale.ROOT`; mark native-looking when it ends `.dll`, `.dylib` or `.so`, or contains `.so.` anywhere. Thus versioned shared-object names are included, but a matching directory/path segment can also produce a lexical hit. The inventory retains original spelling and the names returned by the relevant archive view, including JMOD prefixes. Native entries are a subset of resources; legal notices may be resource names without their content or sufficiency being verified. There is no architecture, binary-format, linkage, license, cryptographic trust or malicious-content certification.

### Digest utility, resources and concurrency

`sha256(Path)` is separately callable for any readable file, without the inspector's suffix, descriptor, real-path or drift checks. It obtains `MessageDigest.getInstance("SHA-256")`, allocates one 65,536-byte read buffer, streams the file and returns lowercase hex. An unavailable provider becomes `IllegalStateException("Java platform lacks SHA-256", cause)`; input open/read/close failures propagate as `IOException`. Null paths are not converted to a kernel error token.

All explicitly opened archive, entry, header and hashing streams are closed by try-with-resources on success or failure; ordinary close failures and suppressed-exception rules remain those of Java. Inspector methods share no mutable instance state, so independent calls can use independent archives. This provides no file lock or atomic snapshot. Two equal digests detect some changes, but cannot guarantee coherent observation across separate hashing, descriptor discovery and archive reads, detect a change-and-restore race, or prevent mutation after return. Callers must supply stable immutable build inputs and later consumption must bind the same artifacts.

Streaming hashing avoids whole-file payload allocation, but inspection is not constant-memory: duplicate-name tracking, effective-entry materialization, descriptor data and sorted resource/native lists grow with archive contents. At least two full-file hash reads occur on the successful path, plus archive/descriptor/header reads. There is no configured file-size, entry-count, inflation, CPU, deadline or concurrency quota, and no benchmark/resource measurement is established here.

### Inspector failure routes

The following strings are implementation messages, not a separately versioned error-code API. `IllegalArgumentException` is used for the explicit refusal branches; other failures are not normalized into it.

| Route | Explicit refusal/message |
| --- | --- |
| Resolved suffix | `UNSUPPORTED_ARCHIVE: <path>` |
| JMOD header integer | `INVALID_JMOD_HEADER: <path>` |
| Physical duplicate name | `DUPLICATE_ENTRY: <name>` |
| Lexically recognized signature marker | `SIGNED_ARCHIVE_REQUIRES_REVIEW: <name>` |
| JMOD descriptor absent | `MISSING_DESCRIPTOR: <path>` |
| JAR module-finder result size other than one | `AMBIGUOUS_MODULE: <path>` |
| JAR automatic descriptor | `AUTOMATIC_MODULE: <module>` |
| Effective class magic | `INVALID_CLASS: <name>` |
| Effective class version policy | `NOT_JAVA21_NONPREVIEW: <name>` |
| Before/after hash differs | `INPUT_DRIFT: <path>` |

Path/ZIP/stream errors, including truncated headers, can propagate as `IOException`; module discovery/parsing can propagate JDK runtime failures such as `FindException` or `InvalidModuleDescriptorException`. Platform/security failures are not caught or relabeled; provider absence has the special `IllegalStateException` above. A missing path fails at real-path resolution, not automatically as `UNSUPPORTED_ARCHIVE`. Earlier steps can mask later defects, so malformed artifacts are not promised one universal failure message independent of their other defects.

## Selected metadata graph

[`M3ModuleGraph()` and `resolve(artifacts,roots)`][graph] are stateless descriptor-graph operations. Despite the method name, this is **not** `Configuration.resolve`, layer creation, class loading, Maven resolution, repository lookup, version conflict mediation or service binding. No file is opened and no digest is checked. Directly constructed artifact values can participate without ever passing the inspector.

1. `index` first scans **all** supplied artifacts into a module-name `TreeMap`. Any automatic descriptor or repeated module name is refused, even if not selected; repeated references to the same artifact still count as duplicates. Root validation happens after this indexing.
2. An empty root collection produces `EMPTY_ROOTS`. A `TreeSet` sorts and deduplicates roots; they are exact strings, not trimmed or parsed module expressions. Null collections/elements are not a supported input and can cause `NullPointerException`, with no custom null error contract.
3. Breadth-first pending names begin with sorted roots. An unavailable root or required dependency causes `MISSING_MODULE`. Each selected descriptor contributes all `requires` names **except** edges with modifier `STATIC`; names are sorted. `TRANSITIVE` does not receive a separate algorithm: a non-static transitive edge is included like any other required edge. `java.base` is not synthesized; when a descriptor requires it, its artifact must be supplied. Optional static targets enter only when explicitly rooted or reached through another non-static edge.
4. `checkPackages` visits the selected closure and every package recorded by its descriptors, including non-exported packages. A package with two selected owners causes `SPLIT_PACKAGE`. It does not inspect actual class entries or compare the descriptor's package inventory to archive contents.
5. `checkCycles` repeatedly removes modules without a remaining non-static dependency. If no leaf is available, it reports `MODULE_CYCLE` with the sorted remaining set. That set can include modules blocked by a cycle; it is not a minimal cycle path. Unselected splits/cycles are not examined, and static edges do not create closure-cycle edges even if both endpoints are selected.
6. The returned list is unmodifiable and ordered by module name, not dependency/topological order or caller input order. It contains the original artifact references. `uses`/`provides` do not add providers or dependencies; required provider modules must be explicit roots when not otherwise selected. Descriptor versions, qualified exports/opens, reflective access and service assignability are not resolved here.

| Helper/phase | `IllegalArgumentException` message |
| --- | --- |
| Whole-input `index` | `AUTOMATIC_MODULE` (no name suffix in this helper), or `DUPLICATE_MODULE: <name>` |
| Root admission | `EMPTY_ROOTS` |
| Closure walk | `MISSING_MODULE: <name>` |
| Selected package ownership | `SPLIT_PACKAGE: <package>: <first-owner>,<next-owner>` |
| Selected dependency cycle check | `MODULE_CYCLE: <remaining-set>` |

Collections are not synchronized or copied into one atomic input snapshot. Supply stable collections and immutable facts; concurrent caller mutation has no stronger contract. The implementation allocates maps/sets/lists and repeatedly scans remaining dependencies during cycle removal. It has no module-count or work budget. Stable inputs give deterministic successful ordering; if several indexing defects exist, input iteration order can determine the first failure.

## CLI and path expansion

[`M3ModuleInventory`][inventory] has a private constructor, a public static `main(String[])`, public static `paths(Path)` and private `field(String)`.

- `main` requires at least two arguments: one comma-separated root string followed by one or more path strings. Otherwise it throws `IllegalArgumentException("Usage: roots archive-or-directory...")`. It does not supply default roots or JDK paths.
- Every later argument is converted with `Path.of`, expanded through `paths`, and inspected **before** graph selection or output. A bad archive outside the eventual selected closure can therefore fail the command. Paths are not deduplicated across arguments; repeated archives can produce a graph duplicate-module refusal.
- Root parsing is exactly `args[0].split(",", -1)` wrapped with `Arrays.asList`. Empty/trailing tokens are preserved, whitespace is retained, and repeated valid names are later deduplicated by the graph. An empty string becomes one missing-name root, not the graph's empty root collection.
- `paths` returns `List.of(input)` when `Files.isDirectory(input)` is false. It does not first prove the path exists or has an allowed suffix. For a directory, it closes a `Files.list` stream and returns an unmodifiable, naturally sorted list of immediate children whose path strings end case-sensitive `.jar` or `.jmod`. It does not recurse, inspect dependencies, normalize paths or require children to be regular files. A directory named `something.jar` can therefore pass this name filter and fail later during inspection. An empty matching directory yields an empty list.
- After all inspections and graph checks succeed, stdout receives a header and one row per selected artifact, ordered by module name. There is no output-file argument, stderr reporting layer, custom exit-code mapping or catch-and-continue mode. Checked I/O and unchecked path/inspection/graph failures propagate. The method does not call `System.exit`.

The nine tab-separated columns are:

`module`, `version`, `kind`, `sha256`, `path`, `requires`, `uses`, `provides`, `native_entries`.

Version uses `descriptor.rawVersion()` or empty string. Requires/provides are sorted descriptor-element `toString()` values rendered as list strings; uses are sorted service names rendered as a list string. Native entries are the sorted artifact names rendered as a list string. This is a human-readable TSV projection, not a lossless descriptor serialization: full resources, packages, exports and opens are not columns. No services are loaded by printing their names.

`field` replaces backslash first, then tab, carriage return and newline with `\\`, `\t`, `\r` and `\n` textual escapes. It is applied to module, version, path, requires, uses, provides and native-entry fields; inspector-produced `kind` and `sha256` are emitted directly. There is no CSV quoting or CLI unescape parser. No output is emitted before successful selection, but row printing itself is not transactional: process interruption or stream failure can leave partial output. `main` does not check `PrintStream.checkError()`, and concurrent invocations share process-global stdout.

## Direct caller and admission boundary

The [OpenRewrite inventory adapter][adapter] reuses `M3ModuleInventory.paths`, `M3ModuleInspector.inspect` and `M3ModuleGraph.resolve` in `getInitialValue`. Its constructor snapshots path/root lists; it wraps checked I/O as `UncheckedIOException`, while graph/runtime refusals propagate. The scanner is a no-op. `generate` inserts module/digest/path/requires/native-name DataTable rows and returns no source files. Read-only source behavior still includes artifact reads and DataTable output effects; it does not confer readiness. This bounded caller read does not document all recipe owners or their dependency closure.

The assembler, JavaFX, text/NLP/search packs, native libraries, upstream JDK and recipe implementation families remain outside the four-owner denominator. Reuse [the existing README][readme] and [admission rules][admission] for their routes. A successful inventory/metadata closure is only a necessary tooling observation: matching-JDK `jlink`, actual image/application execution, reflection/services/resources/native/platform proof, exact artifact/source/license locks, update/rollback handling and applicable target gates remain separate. Neither the CLI nor graph installs anything into `JAVA_HOME`.

## Validation and evidence interpretation

All seven kernel test/helper bodies were read: `M3ModulePackTest`, `M3ModuleChecks`, `ArchiveBoundaryTest`, `M3ModuleClassVersionChecks`, `M3ModuleClassVersionTest`, `VersionProbe`, and `VersionTest`. The last wrapper is named **VersionTest**, not VersionProbeTest. See the [assertion and helper map](module-pack-kernel-source-coverage.md#test-and-helper-map) for exact paths, hashes and limits.

The general suite's refusal helper accepts several exception classes without checking every message. Boundary tests add typed/message checks for particular branches. Each class-version wrapper expects 639 assertions from its own closely matching helper; these are not 639 JUnit test methods, two disjoint semantic corpora, exhaustive bytecode verification or a fresh pass here. Those helpers create files, invoke the compiler and `jmod`, and load controlled test class bodies; the production inspector does not load classes. `ArchiveBoundaryTest` temporarily removes/restores process-global security providers. None of these side effects was executed for this reference.

Historical [version-repair notes][version-report] report standalone Java-21 corpus/JIT/interpreter assertions and then-unexecuted JUnit/recipe gates; [later closure design][closure-design] records a reproduced recipe-fixture failure and recovery work; the [JDK proof continuation][proof] distinguishes its intended lifecycle and target gates. Read those documents with their dates, source pins and explicit limits. Their reports are not new execution at this reference's source pin, and a configured JaCoCo threshold is not measured coverage. Hosted CI is separate exact-commit/workflow evidence. This pass validates documentation/source identities and routing only. Mutable-input races, scale/resource limits, every platform/JDK combination, every malformed archive, full recipe/assembler closure and exact-head execution remain outside its proof.

[artifact]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/tooling/module-packs/kernel/src/main/java/com/m3/tools/modulepack/M3ModuleArtifact.java
[inspector]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/tooling/module-packs/kernel/src/main/java/com/m3/tools/modulepack/M3ModuleInspector.java
[graph]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/tooling/module-packs/kernel/src/main/java/com/m3/tools/modulepack/M3ModuleGraph.java
[inventory]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/tooling/module-packs/kernel/src/main/java/com/m3/tools/modulepack/M3ModuleInventory.java
[readme]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/tooling/module-packs/README.md
[admission]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/docs/community-capability-admission.md
[invariant]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/docs/m3jdk21-porting-invariant.md
[mapping]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/docs/name-mapping.json
[agents]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/AGENTS.md
[reactor]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/pom.xml
[parent-pom]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/tooling/module-packs/pom.xml
[kernel-pom]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/tooling/module-packs/kernel/pom.xml
[module-info]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/tooling/module-packs/kernel/src/main/java/module-info.java
[adapter]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/tooling/module-packs/recipes/src/main/java/com/m3/rewrite/packs/M3InventoryModulePackRecipe.java
[version-report]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/tooling/module-packs/tasks/version/FINAL_REPORT.md
[closure-design]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/tooling/module-packs/tasks/closure-20261005/DESIGN.md
[proof]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/tooling/module-packs/verification/jdk-execution/PROOF.md
