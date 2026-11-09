# Monitored content and synchronous shadow streams

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

`MFileContent` provides shared default operations around a provider's original
content stream. `MFileStreams.Input` and `MFileStreams.Output` add progress,
cancellation, byte counts, SHA-256 observations and an optional shadow output.
The implementing provider keeps authority over read/write capability, append
behavior, consistency and resource lifetime.

## Shared default methods

A read-only provider implements only `InputStream openInputStream()`. The default
`openOutputStream(boolean append)` throws `IOException`; writable providers
explicitly override it. The remaining operations are inherited:

```java
try (MFileStreams.Input input = entry.openInputStream(progress)) {
    // Consume with an existing parser, decoder, copy operation or other reader.
    input.transferTo(destination);
}

try (MFileStreams.Output output = entry.openOutputStream(false, progress)) {
    output.write(bytes);
}

long copied = entry.transferTo(destination, progress);
```

The convenience transfer closes its newly opened input and borrows the supplied
output. It neither flushes nor closes that output. Progress counts input bytes
consumed; if the destination fails, this can exceed bytes confirmed at the
output. Wrapping both sides permits separate read and write progress domains.

The defaults validate progress/layer arguments and check cancellation before
opening a provider resource. If wrapping a newly opened stream fails, that stream
is closed and cleanup errors are suppressed onto the original failure.

The same defaults can bind progress and shadows once for every subsequently
opened stream, including the provider's no-argument input and append-aware
output methods:

```java
MFileContent observed = entry
        .progressify(progress)
        .shadow("stored-bytes", metadataShadow.andThen(parserShadow));

try (InputStream input = observed.openInputStream()) {
    existingParser.parse(input);
}
try (OutputStream output = observed.openOutputStream(false)) {
    output.write(bytes);
}
```

These immutable views are lazy and flatten their configuration. Each actual open
creates one stream observation state. Repeated `progressify` replaces the bound
monitor; an explicit per-open monitor overrides it. Repeated `shadow` appends
observers in declaration order. An explicit per-open output shadow is appended
after the bound observers. A later explicit layer label overrides the bound label;
without a label the defaults are `input` and `output`. The content view delegates
`observation()` to its underlying provider.

## Borrow the existing child progress monitor

There is no second child-progress framework in M3FS. Applications already using
the existing `com.m3.collections.M3ProgressMonitor` split a child there and pass
its JDK callback bridge to the stream:

```java
M3ProgressMonitor child = parent.split(100);
child.beginTask("Read source", expectedByteCount); // UNKNOWN is supported by that owner.
M3FsProgress progress = M3FsProgress.of(child::isCanceled, child::worked);

try (MFileStreams.Input input = entry.openInputStream(progress)) {
    input.transferTo(destination);
    if (!input.complete()) {
        throw new IOException("Full input was not consumed");
    }
}
child.done(); // Only after this caller's intended task has actually succeeded.
```

M3FS depends only on its existing `M3FsProgress` callbacks; this bridge introduces
no collections-module dependency. The original child monitor continues to own
weighting, known/unknown totals, overflow-safe arithmetic and task generations.
The stream reports actual byte increments and cancellation. It never begins,
finishes or closes a borrowed child, even on EOF or normal close. Partial reads
therefore cannot silently award the child's remaining parent work.

## Shadow streams advance with their primary

The shadow can be a caller-owned `OutputStream` sink or an `MFileShadow` lambda.
It receives bytes synchronously
from primary reads or successful primary writes. It cannot independently pull
bytes, move ahead, or trigger background reading. A shadow can feed an existing
incremental parser, checksum sink or output sink without reopening the source.
Its `write` implementation must consume each supplied range synchronously; the
wrapper does not provide an immutable retained copy of the caller's buffer. An
`MFileShadow.onBytes` callback must treat that slice as read-only.

```java
try (MFileStreams.Input input = entry.openInputStream(
        progress, shadowSink, "stored-bytes")) {
    existingParser.parse(input); // Application parser, not an M3FS parser.
}
```

`MFileShadow.of(shadowSink)` adapts an existing sink. `first.andThen(second)` calls
observers in order for both byte slices and completion and stops at the first
failure. Earlier observers may already have changed state when a later one fails;
this is not a transaction. No observer is automatically flushed or closed.

For an existing stream from any source, named factories avoid ambiguous overloads
with the older output-sink API:

```java
MFileStreams.Input input = MFileStreams.observingInput(
        existingInput, progress, metadataShadow, "decoded-bytes");
MFileStreams.Output output = MFileStreams.observingOutput(
        existingOutput, progress, metadataShadow, "written-bytes");
```

These factories take ownership of the primary only after validation succeeds.
They retain the existing input/output observation semantics and do not add
provider metadata assertions to an arbitrary positioned stream.

The wrapper owns and closes its primary stream. The caller owns the shadow and
its buffering policy: the wrapper never flushes or closes that shadow. An
observer or progress failure stops the primary operation and closes the primary;
cleanup errors remain suppressed on the original failure. The primary may
already have consumed or written bytes when an observer fails.

`skip` deliberately reads through a bounded temporary buffer, so skipped bytes
also reach the digest and shadow. `mark/reset` is unsupported because replaying
bytes would otherwise duplicate observations. Wrappers are sequential; no replay
history, concurrent cursor or retained full-file payload is added.

## Layered parsing keeps separate byte domains

Each wrapper has a caller-supplied `layer()` label and a local byte `position()`.
Wrap the source before decompression to observe compressed bytes; wrap the
resulting decoder to observe decoded bytes:

```java
try (MFileStreams.Input raw = MFileStreams.input(
        source, rawProgress, rawShadow, "gzip-bytes");
     MFileStreams.Input decoded = MFileStreams.input(
        new GZIPInputStream(raw), decodedProgress, decodedShadow, "decoded-bytes")) {
    existingParser.parse(decoded);
    // Inspect each wrapper's position, complete and contentHash separately.
}
```

A decoder can read ahead, so raw progress can advance while decoded progress has
not. Parser lookahead likewise differs from committed token/AST progress. These
positions are not interchangeable and no offset conversion is invented here.

Decoder EOF does not automatically establish transport EOF. A decoder can finish
an inner member while the underlying source has unread bytes, another member,
trailing data, or has simply not returned `-1`. The raw observation becomes
complete only when its own reader observes EOF. The caller must decide whether
draining the remaining transport is correct for the enclosing format.

SHA-256 observations use the existing `M3ContentId` owner. Raw-byte, decoded-byte
and parser-derived structural hashes require their own input/domain identities.
The shadow does not construct or cache an AST. Existing parser/AST owners may
publish derived artifacts using their established input/configuration provenance.

## Completion and change-detection boundaries

| Observation | When the digest is available | What it identifies |
| --- | --- | --- |
| Input | Its own EOF has been observed, without a later observation/close failure | Bytes read from the supplied stream's starting position |
| Output | Primary flush and close both succeed after all writes | The sequence of primary write calls that returned successfully |
| Partial/failed stream | Unavailable | Counted work remains observable; no complete digest is claimed |

`MFileShadow.onComplete(MFileStreams.Completion)` is a later publication boundary.
It runs only after the primary closes successfully, and for input only if that
wrapper observed EOF. The immutable result carries `layer`, `byteCount`, the
existing `M3ContentId`, and `INPUT_EOF` or `OUTPUT_SEQUENCE`. Cancellation is checked
again after cleanup before completion. Completion is attempted at most once, even
when a callback throws or closes the same wrapper recursively. Callback failures
propagate and invalidate the wrapper's complete/hash status; publication is not
retried. A callback cannot undo effects of an earlier observer.

Reading exactly an expected byte count is not an EOF observation. Closing an input
early leaves `complete()` false and `contentHash()` empty. A wrapped input can
already be positioned inside a file; its complete digest then covers that range,
not the whole file. A completed stream also does not pin an externally changing
filesystem generation.

The output digest does not identify the entire destination file. Append mode,
previous data, external writes and a provider that partially writes before
throwing all prevent that inference. A throwing primary write is excluded from
confirmed count, digest and shadow; the primary may still have changed physically.
Failed observations expose `failed()` and never publish a complete hash.

Downstream file deltas must attach these observations to the correct file,
generation and layer. Metadata timestamps/lengths, provider change tokens,
complete byte digests and structural hashes remain different kinds of evidence.

## Capturing file metadata with a completed read

`captureReads` connects the generic stream lifecycle to the existing metadata and
manifest owners without reopening or rehashing content:

```java
MFileContent captured = entry
        .progressify(progress)
        .shadow(incrementalParserShadow)
        .captureReads(capture -> {
            MFileObservation evidence = capture.observation();
            M3FileManifest manifest = capture.manifest();
            // Publish or compare evidence using the application's existing owner.
        });
try (InputStream input = captured.openInputStream()) {
    input.transferTo(destination);
}
```

For each input this view samples `observation()` once and records the adjacent
capture time immediately before opening the provider's raw input. The provider
must open the selected layer at offset zero. The capture consumer runs after all
configured/per-open shadow completions and after clean primary close. A known
metadata size that conflicts with the observed byte count is a checked failure;
no capture is emitted. Repeated consumers run in order, fail fast and are not
retried. Output opens do not sample metadata or fabricate whole-file captures.

The capture reuses the stream's SHA-256 and count to build its existing RAW_BYTES
reference and initial `M3FileManifest`. Its capture time is separate from a
provider's modification timestamp. Metadata sampling and the byte read do not
establish an atomic filesystem snapshot; equal sampled size does not prove that
the source stayed unchanged. Providers without observation support retain the
functional `MFileContent` API and report checked unsupported observation when
capture is requested.

Optional word-ID or MAST evidence belongs to an existing incremental parser or
structure owner. Its shadow can consume the same observed bytes and attach an
already computed object through `MFileCapture.withStructure`, with the existing
`M3PrecomputeKey` identifying the raw input and dictionary/parser context. This
path does not invent a parser, dictionary, payload store or full-file buffer.

## Verification

The authored candidate has 35 real JUnit behavior tests. They cover read/write
ordering, no construction-time read-ahead, skip, zero reads, EOF and partial close,
cancellation, shadow/progress/primary failures, suppressed cleanup, ownership,
default methods and real nested GZIP raw/decoded observations. The additional
defaults/SPI cases cover flattened configuration, explicit monitor overrides,
ordered shadows, clean-close publication, callback failure/reentrancy,
terminal cancellation and adapter ownership/self-shadow validation. Java 21 compilation
uses `-Xlint:all -Werror`. The initial baseline refuses compilation because the new
API is absent; that RED evidence is separate from runtime verification. The
terminal-cancellation regression additionally demonstrated a runtime RED before
its fix. Capture integration is verified by the separate facade suite.

Final receipt status belongs to `../vfs-20261020/RESULTS.json`; an authored pass is
not by itself a generated-receiver, linked-image, native or full-M3JDK21 gate.
