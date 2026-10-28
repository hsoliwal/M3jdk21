<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 progress, shadow streams and lazy file traversal

M3JDK21's InputStream and OutputStream now offer `withProgress(ProgressMonitor)`.
Use the existing default `child` method to assign a subprogress budget:

```java
ProgressMonitor read = parent.child("read", 100, expectedBytes);
try (InputStream input = file.openInputStream().withProgress(read)) {
    // Consume input here. Only completed reads and positive skips count.
}
// The owner decides whether to finish the child; close does not imply success.
read.done();
```

Use `ProgressMonitor.UNKNOWN_WORK` when the length is unknown (zero is not a child
estimate). Output streams use the same overload. Mark/reset rereads count again.
Bulk output remains bulk; failed writes cannot expose an exact partial byte count.
A monitor callback may throw after I/O occurred. Closing never finishes a borrowed
monitor and remains available after cancellation. Layered wrappers need distinct
budgets to avoid accounting for the same bytes twice.

`java.io.ShadowInputStream` keeps one fixed byte ring, primitive cursor/state arrays
and a bounded number of API handles. Only the main stream advances its source:

```java
try (var main = new ShadowInputStream(input, 8192, 2)) {
    try (InputStream syntax = main.shadow(); InputStream hashes = main.shadow()) {
        byte[] buffer = new byte[4096];
        int count;
        while ((count = main.read(buffer)) != -1) {
            consumeSyntax(syntax.readNBytes(count));
            consumeHashBytes(hashes.readNBytes(count));
        }
    }
}
```

A shadow attaches at current consumption, sees main skips, and cannot read ahead.
At a live empty shadow, reads throw IOException rather than report a false EOF.
Lagging shadows fill the ring: the main stream refuses before another source read
until that shadow drains or closes. Main close ends the journal; shadows may drain
captured bytes before EOF. Mark/reset are unsupported. Layers can wrap a shadow
in another ShadowInputStream, retaining the same consumption order. Calls serialize
on each journal. Native provider I/O remains in the existing provider/JNI owner;
this change adds no native exports or threads.

`M3File.walk(maxDepth, pathPredicate)` reuses `Files.walk` over the original NIO
provider. It filters paths before projecting M3File objects, never reads file
contents, and allows further stream filters as traversal layers. Close the stream
on every exit, including short-circuiting. Depth zero denotes the root. Traversal
is depth-first, does not follow symlinks, and retains provider sibling order. The
predicate selects results; it does not prune directories or sort the whole tree.

```java
try (var files = root.walk(12, p -> p.toString().endsWith(".json"))) {
    files.filter(f -> wanted(f.path())).forEach(this::consume);
}
```

Canonical recipes and retained correction records live in Synexia's existing
`synexia-openrewrite-recipes/recipe-crates/m3-runtime-progress-facade-20261018`.
This pass reuses its source-qualified Javadoc parser and extends that owner for
Java 21 snippet documentation. Atomize → Patternize → Annotate → Document applies
to each correction: source hashes, failed attempts, regression fixtures, lessons,
applicability and executed receipts travel with the recipe. Attribution: Hitesh
Soliwal and contributors, Synexia Machine-Oriented Programming (MOP).

Reproduce the bounded target gate with Java 21:

```sh
python3 m3/compatibility/verify_stream_shadow.py --root . --jdk /path/to/jdk21 --output /new/proof/path
```

The gate compiles exact java.base sources and runs the stream contract under mixed
and interpreted execution with JNI checks, plus native/ZIP traversal and named
com.m3.file module execution. It is not a complete source-built JDK distribution
or universal parser qualification. Global receiving remains at STRING.

M3File snapshot/delta work is separately reviewable in M3JDK21 #1581; metadata
cannot establish unchanged contents on all providers. Provider change-token
admission, structural/M3AST delta linkage, scoped interning, directory pruning,
other traversal orders, universal recipe-memory loading and complete image gates
remain open. Do not replace newer M3File snapshot work with this older whole-file
preimage: reconcile forward through a new source-sealed recipe after it lands.
