<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Persistent AI reuse contracts

The `jdk.m3.io` module exposes these APIs through `jdk.m3.optane`.
They compose existing mapped regions, immutable block records, source images,
vector access and transactional checkpoint receivers. Applications retain ownership
of model state, serializers, complete input identities and validators.

Java examples are method-body fragments with application-owned inputs named in the
surrounding text. Use imports from `jdk.m3.optane`, `java.nio`, `java.nio.channels`,
and `java.nio.file` as appropriate. No model-engine or third-party runtime dependency
is introduced.

## Ownership and opening a store

PmemSdk.open opens an existing regular-file range. FILE uses positional channel I/O; native POSIX and PMDK map the range. It does not create the file or a DAX namespace. Keep exactly one writable PmemBlockStore owner for each exclusively reserved range. PmemMemoStore, PmemPrefixCache and PmemFloatRows borrow their backing owner; they do not close it. Close the PmemRegion when all borrowed views are finished.

The following statements belong inside a method declaring IOException. `path`, `capacity`, `maxPayloadBytes`, and the lowercase SHA-256 `schemaSha256` are application inputs. This initialization is for a NEW file only:

```java
long mappedBytes = PmemBlockStore.requiredBytes(capacity, maxPayloadBytes);
try (FileChannel file = FileChannel.open(path,
        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
    file.position(mappedBytes - 1);
    ByteBuffer lastByte = ByteBuffer.wrap(new byte[] { 0 });
    while (lastByte.hasRemaining()) file.write(lastByte);
    file.force(true);
}
try (PmemRegion region = PmemSdk.open(path, 0, mappedBytes,
        PmemAccess.READ_WRITE, PmemBackend.FILE)) {
    PmemBlockStore blocks = PmemBlockStore.create(
            region, 0, capacity, maxPayloadBytes, schemaSha256);
    // Construct borrowed memo/prefix facades and use blocks within this lifetime.
}
```

After a normal close, reopen the existing file with PmemSdk.open and call PmemBlockStore.open with the SAME origin, capacity, maximum payload and schema digest. Do not call create to reset an existing store, truncate it, or silently create a new store after an open failure. Open validates the complete committed prefix and performs recovery persistence barriers; it requires a writable region even when the application's next operation is a read.

The backend can be selected explicitly as FILE, POSIX or PMDK, or negotiated through AUTO. Inspect region.backend() and region.persistenceGranularity() for the actual result. A successful ordinary-file run, nativeAvailable(), AUTO choice or PAGE granularity is not evidence of physical persistent memory or power-loss qualification. FILE works without the optional JNI library. Native availability and platform requirements are separate from these storage contracts.

## Incremental source memoization

Use the existing PmemSourceImage to supply immutable source identities. Do not reparse a file just to build its memo key. The source-image factory binds file path, complete file digest and selected byte range; SourceContext additionally binds analyzer/parser/compiler, recipe or prompt contract, dependencies, configuration and result schema.

```java
PmemMemoKey.SourceContext analysisContext = new PmemMemoKey.SourceContext(
        analyzerSha256, recipeSha256, dependenciesSha256,
        configurationSha256, resultSchemaSha256);

PmemMemoKey key = PmemMemoKey.source(
        sourceImage, fileIndex, analysisContext,
        PmemMemoKey.SourceScope.FILE_DEPENDENCIES);

PmemMemoStore memo = new PmemMemoStore(blocks, "source-analysis/v1");
PmemMemoStore.Snapshot generation = memo.snapshot();
int count = memo.getCandidate(generation, key, output, 0);
if (count < 0) {
    // Invoke the application's existing analyzer once, producing analysisBytes.
    // Its inputs must match sourceImage and every declared context identity.
    memo.putCandidate(generation, key, analysisBytes, 0, analysisBytes.length);
}
```

`output` is a caller-owned byte array large enough for the expected stored result. `analysisBytes` is application-serialized output computed on a miss; it is not an SDK variable or automatic parser result. The generation must be captured before starting that computation. A new namespace invalidation can make putCandidate reject the result as stale.

FILE_DEPENDENCIES is an explicit optimization contract: the caller must establish that dependenciesSha256 covers EVERY other source, module/classpath artifact, transitive input and external state consulted by the analysis. If an unrelated file changes while the selected file and its complete declared context remain identical, this key remains reusable. A changed selected file, range or context changes the key. No digest proves that a dependency list is complete.

The simpler source(sourceImage, fileIndex, analysisContext) overload defaults to IMAGE_GENERATION. It also binds sourceImage.sourceRoot(), so any change to that captured source generation invalidates the key conservatively. The two scopes occupy separate key domains. Both have range overloads: source(image, index, firstByte, byteCount, context[, scope]).

For other AI computations, PmemMemoKey.of(operation, namedDimensions) canonicalizes a bounded map of text dimensions. Include the complete model, tokenizer, adapters, prompt/tool inputs, policy, retrieval/index generation and external-state validity boundary that affect the result. Equivalent input maps have order-independent identity; similar text is not an exact match. Names/values are length-framed UTF-8, malformed surrogate sequences are rejected, and no case folding or Unicode normalization is implicit.

## Candidate and externally validated results

Candidate persistence does not make an LLM response a verified fact. Candidate and validated entries use different storage identities. A validated call requires an explicit Validation containing the validator contract/version digest, immutable evidence-receipt digest and exact output digest:

```java
PmemMemoStore.Validation approved = new PmemMemoStore.Validation(
        validatorSha256, evidenceReceiptSha256, payloadSha256);
memo.putValidated(generation, key, approved, resultBytes, 0, resultBytes.length);
int restored = memo.getValidated(generation, key, approved, output, 0);
```

The application obtains this receipt from its trusted validator. For example, payloadSha256 can be computed with MessageDigest.getInstance("SHA-256") and HexFormat; PmemBlockStore's internal hash helper is not a public application API. The evidence receipt must bind the complete key and result. The store checks the supplied identities and hashes its owned copy of the published bytes. It does not run the validator, authenticate the evidence, grant rewrite/promotion authority, or infer factual correctness.

getCandidate/getValidated return -1 for a miss and zero for a stored empty value. A validated miss is expected when any supplied validator, evidence or result identity differs. A malformed committed memo envelope throws; it is not converted into a cache miss. Memo reads validate a private complete envelope before copying, so rejected reads leave the caller's destination untouched. Raw PmemBlockStore.read has a different contract: an I/O failure after preflight can leave partially copied output.

## Durable namespace invalidation

```java
PmemMemoStore.Snapshot before = memo.snapshot();
PmemMemoStore.Snapshot after = memo.invalidate(before);
// Calls using before now reject the stale namespace generation.
// Calls using after cannot discover entries from before's generation.
```

The epoch is persisted in the same block store. It survives close/reopen, and multiple facades borrowing the same store observe the same generation. Capture a snapshot before work and use it for lookup and publication. invalidate(expected) refuses an already-stale expected snapshot and generation overflow. It does not reclaim old slots, invalidate a different memo namespace, or implicitly invalidate PmemPrefixCache or PmemCheckpoint.

If persistence fails after a write begins, the block store requires reopen. A failed acknowledgement can still mean the operation committed. Reopen and inspect snapshot()/checkpoint state before deciding whether to retry; do not assume failure means no bytes became durable.

## Typed reusable prefix blocks

The cache holds explicitly supplied inactive/reusable bytes. It does not capture a live decoder's KV, attach to a model engine, transfer to a GPU, or convert dtype. The complete model/tokenizer/adapter artifacts and engine/attention/RoPE/sharding/quantization context must be identified by the caller.

```java
PmemPrefixKey.Context context = new PmemPrefixKey.Context(
        "model-prefix/v1", modelSha256, tokenizerSha256,
        "fp32-unquantized", engineAttentionAndShardingIdentity, adaptersSha256);
PmemPrefixKey prefix = PmemPrefixKey.contiguous(
        context, tokenIds, 0, tokenIds.length, firstAbsolutePosition);
PmemTensorSpec tensor = new PmemTensorSpec(
        PmemTensorSpec.Scalar.FLOAT32, ByteOrder.LITTLE_ENDIAN,
        2, tokenIds.length, 8);
PmemPrefixCache prefixes = new PmemPrefixCache(blocks, 1024);
prefixes.put(prefix, tensor, tensorBytes, 0, tensorBytes.length);
byte[] restored = new byte[Math.toIntExact(tensor.byteCount())];
int length = prefixes.get(prefix, tensor, restored, 0);
prefixes.releaseIndex();
```

Here `[2, tokenCount, 8]` is an application-declared contiguous row-major shape, not a prescribed model layout. tensorBytes must contain exactly tensor.byteCount() bytes in the declared representation. A zero rank is one scalar; any zero-sized axis makes the tensor empty. Dimensions and totals use checked long arithmetic. Strided/sub-byte formats require an application codec and are not represented by PmemTensorSpec.

Typed keys occupy a separate domain from the existing raw prefix API. A different shape, scalar format or endian order is a different key. Typed reads verify the complete descriptor and envelope before copying. The caller must supply space for the full expected tensor, even on a lookup that would miss. The optional 1024-entry index above retains primitive digest/ordinal arrays (40 bytes per entry, excluding JVM headers); releaseIndex drops it while leaving durable entries usable. Index collisions fall back to the full 256-bit digest and store validation. Replacements still consume append-only capacity.

## FP32 retrieval and bounded gather

Given an already-open immutable embedding region, a region-relative byte offset, row count and finite nonzero query, exact search and row gathering compose on the same storage owner:

```java
PmemFloatRows rows = new PmemFloatRows(
        embeddingRegion, byteOffset, rowCount, query.length);
PmemSearch.VectorMatches hits = PmemSearch.topKCosineFloatLE(
        embeddingRegion, byteOffset, rowCount, query, requestedK);
long[] selected = new long[hits.size()];
for (int i = 0; i < selected.length; i++) selected[i] = hits.row(i);
float[] vectors = new float[Math.multiplyExact(selected.length, query.length)];
rows.gather(selected, 0, selected.length, vectors, 0);
```

VectorMatches exposes size(), row(index) and score(index); it does not return document IDs. Map row ordinals to document metadata in the application's existing index owner. This is exact cosine scanning over tightly packed row-major LITTLE-ENDIAN FP32, with descending-score and ascending-row tie order; it is not an ANN graph, index builder, embedding generator or guaranteed acceleration. Search rejects nonfinite or zero-norm stored vectors. PmemFloatRows is a byte-to-float copy view and separately permits zeros, infinities and NaNs; it performs no arithmetic validation.

The six-argument topKCosineFloatLE overload accepts java.util.function.LongConsumer progress. The callback runs under the read lease and must not write, persist or close that region. Search cooperatively checks interruption, preserves the interrupt flag and throws CancellationException. Both search and gather hold the mapping owner's read lease across each operation, but external processes or distinct mappings must not mutate/truncate the backing file. A gather I/O failure after preflight may leave a copied prefix; discard output from a failed gather.

## Checkpoint composition

PmemCheckpoint publishes immutable chunks first and a complete hashed manifest last. Applications supply serialization and decide what constitutes one coherent generation. Capture model, optimizer, scheduler, RNG, sampler/input position and gradient-accumulation state at one consistent training step where those are required; no such state is discovered automatically. Distributed rank/shard agreement must be established by the application before publication.

```java
// source implements PmemCheckpoint.ChunkSource:
// int readChunk(int index, byte[] transferBuffer) throws IOException.
PmemCheckpoint.Info committed = PmemCheckpoint.commit(
        blocks, checkpointIdentitySha256, nextGeneration,
        chunkCount, transferBuffer, source);

PmemCheckpoint.Info available = PmemCheckpoint.inspect(
        blocks, checkpointIdentitySha256);

// target implements PmemCheckpoint.RestoreTarget, described below.
PmemCheckpoint.Info restored = PmemCheckpoint.restore(
        blocks, checkpointIdentitySha256, transferBuffer, target);
```

The SHA-256 checkpoint identity should bind model/schema/application namespace. `nextGeneration` must be nonnegative and strictly newer than the latest committed generation for that identity. Each chunk must fit the borrowed transfer buffer and fixed payload slot; the complete manifest must also fit. maxChunks(blocks) exposes the manifest limit. commit preflights chunkCount+1 slots, but interrupted attempts can consume unreferenced chunk slots. ChunkSource must not append to the borrowed store or retain/mutate its borrowed transfer buffer after returning.

RestoreTarget has exactly four methods: begin(long generation, int chunks, long totalBytes), writeChunk(int index, byte[] transferBuffer, int length), commit(), and abort(); all may throw IOException. Stage bytes privately, consume/copy each borrowed transfer buffer before returning, publish the complete destination state only in commit, and discard staging in abort. The receiver owns destination rollback and any uncertain commit outcome. A coherent tiny retrieval snapshot can serialize the selected row IDs, scores, gathered vectors and corpus/model identity as application chunks; it must not be described as a complete training checkpoint.

Restore validates manifest and every chunk before begin. Later I/O or receiver failure still invokes abort. The underlying store refuses corrupt committed frames and certain torn/nonzero commit markers. Manifest-last publication therefore does NOT guarantee recovery of an older checkpoint after every possible corruption/power failure. Physical crash behavior remains separately qualified.

## Verification and deployment scope

The companion `PMEM_REUSE_EXECUTION.json` records the exact source revision,
executed recipe outputs and receiving commands for this continuation. Historical
receipts describe their own immutable snapshots.

Run the ordinary component lanes from the repository root with a full JDK 21,
Maven and a C17 compiler:

```sh
mvn -B -ntp -f m3/optane/pom.xml package
cmake -S make/m3/optane -B build/pmem-native
cmake --build build/pmem-native
mvn -B -ntp -Pm3-optane-native \
    -Dm3.optane.library.path="$PWD/build/pmem-native" \
    -f m3/optane/pom.xml test
```

The native profile includes the memo, tensor, native-open and vector regressions.
The durable/checkpoint and FP32 main contracts also require their explicit jtreg or
standalone execution; test compilation alone does not execute those contracts.
The four existing scale tests are opt-in through `pmem.source.reuse.scale`,
`pmem.gram.scale`, `pmem.corpus.scale` and `pmem.corpus.selected.scale`.

The historical `m3-optane-jmod` profile builds the `org.m3jdk.optane` Maven bridge.
The actual product packaging check uses a `jdk.m3.io` JMOD, all three JNI libraries,
and `PmemNativePackaging` without a development `java.library.path`.
A component image linked with stock Java 21 base modules has a different scope
from a complete source-built M3JDK21 and HotSpot image.

The store has fixed append-only capacity. Logical invalidation, replacement and
interrupted checkpoint attempts can consume capacity; no reclamation, automatic
resize or multi-process writer coordination is provided. Do not infer physical
Optane/DAX power-loss behavior, NUMA/GPU/RDMA performance or a production speedup
from ordinary-file tests. Measure workload benefit against the same computation,
complete identity and cache policy on representative storage.
