# M3FS: filtered traversal, progress, synchronous shadows, and captures

The example uses the frozen `MFiles`, `MFileContent`, `MFileShadow`, and
`MFileCapture` APIs. The caller supplies the existing progress owner, a factory
for one incremental shadow per file, and a consumer for detached captures.

```java
import com.m3.collections.M3ProgressMonitor;
import com.m3.fs.M3FsProgress;
import com.m3.fs.MFile;
import com.m3.fs.MFileCapture;
import com.m3.fs.MFileContent;
import com.m3.fs.MFileShadow;
import com.m3.fs.MFiles;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public final class CaptureSelectedFiles {
    private CaptureSelectedFiles() { }

    public static void run(
            Path root,
            M3ProgressMonitor task,
            Function<? super MFile, ? extends MFileShadow> shadowForFile,
            Consumer<? super MFileCapture> publish) throws IOException {
        // The file count need not be known before traversal starts.
        task.beginTask("Capture selected files", M3ProgressMonitor.UNKNOWN);

        try (Stream<MFile> files = MFiles.from(root)
                .extensions("txt", "json", "bin")
                .filter(MFile::isFile)
                .descendInto(directory -> !directory.name().equals("build"))
                .order(MFiles.TraversalOrder.DEPTH_FIRST)
                .enterArchives(file -> file.name().endsWith(".zip"), 1)
                .stream()) {
            Iterator<MFile> iterator = files.iterator();
            while (iterator.hasNext()) {
                task.checkCanceled();
                MFile file = iterator.next();
                M3ProgressMonitor child = task.split(1);
                child.beginTask(file.relativeName(),
                        file.size().orElse(M3ProgressMonitor.UNKNOWN));

                MFileContent content = file
                        .progressify(M3FsProgress.of(child::isCanceled, child::worked))
                        .shadow("selected-file-bytes", shadowForFile.apply(file))
                        .captureReads(publish);

                try (InputStream input = content.openInputStream()) {
                    // Pull once; the shadow sees exactly these consumed bytes.
                    input.transferTo(OutputStream.nullOutputStream());
                } // EOF + successful primary close precede shadow completion/capture.

                child.done(); // The caller completes its child only after success.
            }
        }
        task.done(); // Includes successful traversal cleanup.
    }
}
```

`shadowForFile` can adapt an existing synchronous parser or sink. For a borrowed
sink, return `MFileShadow.of(sink)`. A custom observer implements
`onBytes(byte[], int, int)` and may override
`onComplete(MFileStreams.Completion)`. The byte slice is read-only and valid only
during the callback. `first.andThen(second)` composes observers in declaration
order. The example drains input without retaining its payload; an application
may instead give that main input to its existing parser.

`extensions` is a case-sensitive cheap name filter. Rejected names do not prune
directories; `descendInto` controls that separately. Archive selection is also
independent of emitted-file filters, and the example permits one automatic ZIP
layer. `MFiles.from(providerAddress)` is the corresponding overload for a
configured provider address. Provider capability and archive mounting remain
explicit. Traverse and consume each file while its owning stream is open.

## What a capture contains

| Value | Exact accessor | Meaning |
| --- | --- | --- |
| Selected source | `capture.observation().sourceKey()` | The provider address bound to the observation |
| Capture date/time | `capture.capturedAtMillis()` | Wall-clock milliseconds adjacent to the metadata sample |
| Provider modification time | `capture.observation().modifiedTimeMillis()` | Optional provider timestamp; independent of capture time |
| Sampled size | `capture.observation().size()` | Optional metadata size sampled before opening input |
| Observed byte count | `capture.manifest().raw().size()` | Bytes consumed from the selected file layer through EOF |
| SHA-256 identity | `capture.manifest().raw().id()` | Existing `M3ContentId` reused from the stream digest |

The capture hook runs after configured and per-open shadow completions. A known
sampled size must agree with the observed byte count; disagreement raises checked
`IOException` and emits no capture. Unknown size remains explicitly unknown in
the metadata while the raw reference records the observed count.

Early close, cancellation, primary failure, failed primary cleanup, or an earlier
shadow failure prevents capture. Completion is attempted at most once; callback
failure propagates without retry. Observer composition has no rollback, so an
earlier callback can have effects even if a later callback fails.

The wrappers own their primary streams and borrow monitors and shadows. They do
not finish a child monitor or flush/close a shadow. Repeated `progressify` replaces
the bound monitor; an explicit per-open monitor overrides it. Repeated `shadow`
appends observers without nesting observation states. Output streams use the same
progress/shadow defaults but emit only `OUTPUT_SEQUENCE` completion and never
whole-file captures.

Metadata and bytes are separate observations. Equal size does not prove source
stability or an atomic snapshot. A custom `MFileContent` must supply matching
metadata and open the selected file layer at offset zero before using
`captureReads`. Standalone `observingInput` wrappers also support positioned or
decoded streams, but their EOF does not establish whole-file raw coverage.
Decoder EOF likewise does not establish transport EOF.

## Optional word-ID and MAST references

An existing dictionary/parser owner may compute structures from those same bytes.
Attach its existing references and derivation keys after a successful capture:

```java
static MFileCapture attachStructures(
        MFileCapture capture,
        com.m3.fs.M3ObjectRef wordIds,
        com.m3.fs.M3PrecomputeKey wordIdsKey,
        com.m3.fs.M3ObjectRef mast,
        com.m3.fs.M3PrecomputeKey mastKey) {
    return capture
            .withStructure("word-ids", wordIds, wordIdsKey)
            .withStructure("mast", mast, mastKey);
}
```

Each key's first input must be the capture's raw content ID; additional inputs and
configuration identify dictionary/parser context. The caller attests that the
referenced object was produced by that key. The manifest retains both references;
the observation selects the last attached structure for comparison. This method
runs no parser, allocates no file payload buffer, and does not establish durable
storage of the referenced bytes or structures.

## Focused integration review

Reviewed the frozen `MFileContentView`, stream terminal state, `MFileCapture`
construction/attachment rules, and provider-owned input close behavior together.
No additional concrete integration blocker was found. The primary close failure
and in-progress close guards prevent a later observing wrapper from treating
unsuccessful provider cleanup as successful completion. Capture validation reuses
the existing metadata/raw binding, and structure attachment reuses the existing
manifest and derivation-key checks. Final generated/runtime/image qualification
is tracked separately; this example introduces no production change.
