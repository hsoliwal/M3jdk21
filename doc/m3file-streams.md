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
