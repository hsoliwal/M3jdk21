<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Bounded checkpoint streams in jdk.m3.io

`PmemCheckpoint.commitStream` connects an encoder that writes to an
`OutputStream` to the existing durable block store. `readStream` connects the
same checkpoint to a decoder that reads an `InputStream`. Neither adapter
materializes the entire checkpoint payload in a heap array.

The existing `commit`, `inspect`, `restore` and `maxChunks` methods remain.
There is one checkpoint protocol: `M3CKPT01` version 1, with the existing chunk
and manifest key domains and the existing `PmemBlockStore` / `PmemTaskLog`
publication rules. A stream checkpoint is readable by the earlier chunk
restore API; a chunk checkpoint is readable as a stream, including manifests
with empty chunks. No native API or JNI symbol changes.

## API and ownership

```java
import jdk.m3.optane.PmemBlockStore;
import jdk.m3.optane.PmemCheckpoint;

PmemBlockStore store = openedStore;
PmemCheckpoint.Info info = PmemCheckpoint.commitStream(
        store, checkpointSha256, generation, maximumBytes, transferBuffer,
        output -> encoder.writeSnapshot(output));

Snapshot snapshot = PmemCheckpoint.readStream(
        store, checkpointSha256, maximumBytes, transferBuffer,
        input -> decoder.restoreSnapshot(input));
```

`encoder`, `decoder` and `Snapshot` above stand for application-owned codecs
and decoded state. The checkpoint does not choose the serialization schema.
Its SHA-256 identity must include the application's model, schema and
namespace. Include every value needed to resume: weights, optimizer,
scheduler, RNG or KV state as applicable. Persisting output tokens alone does
not reconstruct a decoder's active state.

Callbacks execute synchronously on the calling thread while the store is
reserved. Do not retain the stream, pass it to another thread, mutate its
borrowed transfer array, append to the store, close its region, or change its
backing bytes during a callback. Read/write arrays must not alias the transfer
array. Foreign-thread operations refuse before acquiring the store's monitor;
the owner therefore can wait for an attempted foreign operation without
deadlocking on that monitor. A stream expires when its callback ends, whether
the operation succeeds or fails.

The decoder must keep tentative application state private. `readStream`
returns the callback's value only after complete consumption and successful
final checks. It cannot undo side effects the decoder publishes itself or
release resources created by a failing decoder. The decoder owns that cleanup.

## Bounds and allocation

The writer takes a nonnegative maximum byte count and a nonempty transfer
array no larger than one store payload. Before calling the encoder it checks
the entire worst-case reservation:

```
maximumChunks = maximumBytes == 0 ? 0 : 1 + (maximumBytes - 1) / bufferLength
requiredSlots = maximumChunks + 1
```

The ceiling calculation does not add `bufferLength - 1` to the caller's long
bound. Impossible counts refuse before narrowing to an int or allocating a
manifest. The manifest remains subject to the existing payload-size bound and
16 MiB protocol limit. A bound reserves capacity for the maximum even if the
encoder ultimately writes less. The committed manifest records actual chunks
and bytes; an empty stream has zero chunks and one manifest.

The adapter retains the caller's transfer array and one bounded manifest. The
existing block store also uses its bounded checksum scratch. No per-payload
byte array is retained. The reader's transfer array must fit every stored
chunk, and the checkpoint's validated total must fit the caller's maximum;
both checks precede the decoder. The reader does not silently rechunk a
checkpoint created with larger chunks.

These bounds describe checkpoint transport. A decoder can explicitly request
an allocation with `readAllBytes` or `readNBytes(int)`, or construct an
application state larger than the transfer buffer. The adapter cannot make
those allocations bounded. Stream data incrementally and budget active model
or KV state separately. The dedicated heap test writes and reopens a 32 MiB
checkpoint with a 16 MiB maximum heap and a 256 KiB transfer buffer; it checks
content through incremental digests.

## Publication and failure

Full chunks are appended and persisted as they fill. `flush` does not split a
partial chunk. `close` seals a final partial chunk and becomes idempotent during
the callback. Neither operation publishes a manifest. The manifest is appended
only after the callback returns successfully and all scope checks pass.

Every stream failure poisons that checkpoint operation. Catching a bounds,
array, persistence, lifetime, thread or inherited stream-operation failure in
the callback does not turn it into a successful checkpoint. This includes a
reader's failed `transferTo`, `skipNBytes`, `readNBytes` or `reset` after it has
already consumed the final byte. Closing a reader early does not drain the
stream or authorize a partial decode.

A failed writer can leave durable unreferenced chunks. They do not replace the
previous complete checkpoint and are not reclaimed by this API. If a backend
write or persistence call has an uncertain outcome, reopen the store before
another operation. An uncertain final manifest append may have committed;
inspect the reopened checkpoint before choosing a retry generation. A
successful new generation must be strictly greater than the current complete
one.

Before invoking a decoder, the existing loader verifies the manifest and all
referenced chunk frames, identities, order, lengths and checksums. The reader
then revalidates chunks during transfer, compares their bytes with the manifest
hashes, and requires complete consumption before returning private decoded
state. This assumes exclusive ownership of immutable backing bytes throughout
the operation; another mapping or process must not modify them.

## Execution gates

The module's stream smoke class is `test.PmemCheckpointStreamSmokeTest`.
Its default mode exercises actual FILE mappings plus the independent
volatile/durable persistence model. `--require-posix` additionally requires
and exercises the native POSIX backend. `--heap-stream` is a separate mode
and must run in a JVM whose maximum heap is smaller than 32 MiB, normally
`-Xmx16m`. The product-owned `PmemCheckpointStreamJUnitTest` exposes FILE
coverage through the existing Maven bridge and adds actual POSIX coverage when
its existing `m3-optane-native` profile enables `pmem.streaming.native`.

```bash
mvn -f m3/optane/pom.xml test
cmake -S make/m3/optane -B build/optane-native
cmake --build build/optane-native
mvn -f m3/optane/pom.xml -Pm3-optane-native \
  -Dm3.optane.library.path="$PWD/build/optane-native" test
```

The test source under `test/jdk/jdk/m3/optane` also declares the independent
jtreg IDs `file`, `posix` and `bounded_heap`. Run these against a built JDK
containing `jdk.m3.io` and its required native libraries. The runtime owner is
the product module `jdk.m3.io`; the existing Maven control bridge retains the
name `org.m3jdk.optane` and compiles the same product Java sources.

A separately compiled product module, native libraries and linked Java 21
image provide finite module/runtime evidence. They do not constitute a
complete source-built modified OpenJDK/HotSpot or a formal jtreg harness run.

The tests cover old/new interoperability, actual close and reopen, partial
chunks, empty checkpoints, maximum-capacity preflight, generation monotonicity,
callback failures, incomplete decoding, caught default-method failures,
expired streams, foreign threads, store interference, corruption before and
during reading, and before/after failures at every persistence boundary of a
three-chunk checkpoint and its manifest.

FILE and POSIX execution and a simulated crash model do not establish
physical Optane/DAX or power-loss durability. Target source-built JDK tests,
actual platform qualification and publication gates remain independent.
