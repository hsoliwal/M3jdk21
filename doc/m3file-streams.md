# M3File directory and archive streams

`jdk.m3.fs.M3File` is the preferred entry type for filesystem traversal. It exposes
the same encountered metadata and lazy content operations for directories and
supported archive providers. It shares the existing M3FS implementation.

```java
try (Stream<String> names = M3File.stream(root,
        entry -> entry.isFile() && entry.name().endsWith(".txt"),
        M3File::relativeName)) {
    names.forEach(System.out::println);
}
```

Read an actual ZIP with `M3File.stream("zip:" + zip.toUri() + "!/")`.
The existing configured providers also handle JAR, TAR, TGZ, TBZ2, GZIP and BZIP2;
use their provider addresses. A plain local archive path emits that file.
Automatic expansion and nested archives require an explicit plan and layer limit:

```java
try (Stream<M3File> entries = M3File.stream(MFiles.from(root)
        .enterArchives(entry -> entry.name().endsWith(".zip"), 2)
        .descendInto(folder -> !folder.name().equals(".git")))) {
    entries.filter(M3File::isFile).forEach(entry -> {
        // Open/read content explicitly here while the traversal is open.
    });
}
```

Filters select emitted files/folders; they do not prune traversal. Use `descendInto`
to prune. Transformations execute lazily, after the selection filter. Resolution,
archive mounting and metadata can perform I/O, but payload streams are opened
only when content is requested. No eager payload buffering is introduced.

Always close the returned stream, including after `findFirst`, `limit` or an
exception. A terminal operation does not close it. Close content streams promptly.
Metadata remains readable after traversal close; live content does not. Non-default
NIO filesystems remain caller-owned. Names are display paths, not safe extraction
destinations; no archive extraction is performed by this API.

Compatibility: `MFile` remains the concrete implementation and now implements
`M3File`. Existing `MFiles` signatures, plans, content protocols, binary callers,
and providers remain available. New examples and new entry-facing code use
`M3File`; legacy names retained in compatibility APIs and historical tests are
intentional. The indexed/token file type `jdk.m3.file.M3File` is a different owner;
import the filesystem entry type `jdk.m3.fs.M3File` explicitly when both are used.

The existing Maven module includes the root-owned source and test directories.
The standalone VFS qualifier additionally requires `--entry-api-sha256` with the
reviewed hash of `src/jdk.m3fs/share/classes/jdk/m3/fs/M3File.java`; its original
module source lock remains mandatory. This input seal does not claim source-built
M3JDK21 qualification or replace native and full product gates.


## Qualifying the current module closure

The standalone `m3/fs/tools/qualify_vfs.py` and Linux
`m3/fs/tools/package_runtime.py` use the existing shared JMOD packer. Current
M3FS consumes the actual source-built `org.m3jdk.file`, `jdk.m3.collections`, and
`jdk.m3.arrays` artifacts. Build these modules in dependency order (arrays,
collections, file), retaining their own test results. The broader provider and
native collection lanes have their separate existing gates.

Supply `--product-dependency-lock /absolute/product-dependencies.json` to either
wrapper. It is a reviewed JSON object with `schema: 1` and exactly three
`dependencies` rows, one per product module. Each row contains `module`,
`repository_path` relative to the supplied dependency cache, `sha256` of the
entire JAR, and `legal_sha256` mapping every original META-INF license/notice
resource (including `m3collections.md`) to its SHA-256. All JARs require their
actual `module-info.class`; the compiler and packer validate module resolution.
No dependency is downloaded, inferred from a filename, or automatically resealed
by these wrappers. Missing, duplicate, altered or escaping inputs are refused.

Keep the existing Apache `vfs-dependencies.lock.json` and dependency-cache options.
Its original five input artifacts and legal pins are validated, including the
legacy Logging JAR. When Logging source/classes already belong to M3FS, that
external Logging module is omitted from the linked runtime, preserving one owner
for its packages. Legacy non-inlined inputs retain their explicit Logging lane.

The source qualifier still requires `--source-lock` for all module-owned inputs
and `--entry-api-sha256` for root-owned `src/jdk.m3fs/share/classes/jdk/m3/fs/M3File.java`.
It compiles inlined Logging, the selected VFS providers, then strict first-party
Java sources. The runtime wrapper additionally requires the actual M3FS JNI and
SQLite libraries and checks their origin in the linked image. Use fresh output
paths. These checks qualify a linked supplied-JDK image; a full source-built
M3JDK21 and its broader suites remain separate acceptance gates.
