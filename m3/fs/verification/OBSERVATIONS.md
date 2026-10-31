<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# File observation, completed capture, and delta evidence

`MFileObservation`, `MFileCapture`, `MFileDelta`, and the optional
`MFileChangeSource` provider contract are part of `com.m3.fs` in the existing
`jdk.m3fs` module. They use the existing `M3ContentId`, `M3ObjectRef`,
`M3FileManifest`, and `M3PrecomputeKey` representation. They do not introduce a
payload store, parser, or retained history map. These files are after templates
for the canonical source recipes.

## Observation contract

`MFile.observation()` is an explicit fresh provider operation implemented by the
facade. It returns a stable immutable value; provider refresh or later stream
closure does not mutate that value. The original traversal entry metadata remains
unchanged. Provider failures propagate and do not become absent observations.
The generic `MFileContent.observation()` default reports unsupported metadata by
checked `IOException`; a content implementation must supply its observation
contract before it can use metadata capture. Progress and shadow defaults do not
require metadata support.

The source key is the exact fully resolvable `MFile.sourceAddress()`. Two different
source strings cannot be compared, even if a caller believes they are aliases.
The implementation retains the supplied string; it does not globally intern file
identities. Source keys may contain credentials and should not be logged.

`metadata(sourceKey, flags, size, modifiedTimeMillis, stamp)` uses explicit known
bits for existence, type, size, and modification time. Unknown numeric values are
passed as zero and distinguished from a known zero by their known bits. The
observation representation accepts a known negative timestamp; an adapter may
conservatively leave a provider's negative sentinel unknown. `FILE` and `DIRECTORY`
are content and children capabilities and may coexist for a hybrid provider.
`unknown(sourceKey)` and `absent(sourceKey)` are distinct factories.

`withContent(completeRawReference)` requires an existing nonnegative `RAW_BYTES`
reference. The caller attests that it covers the complete selected file layer,
starting at offset zero and ending at a successful EOF. A completed monitored
wrapper cannot establish where an already-positioned underlying stream began;
EOF in a decoder also does not prove EOF in its transport. This attachment does
not claim that the source stayed unchanged while it was observed. A known metadata
size must match the reference size. Unknown metadata size is allowed. A different
content reference cannot overwrite a content attachment on the same observation.

`withStructure(manifest, name, key)` shares those existing immutable objects and
selects one manifest structure. It verifies that the manifest's raw reference and
the key's first input match the observation's full raw reference. The caller
attests that the selected output was computed with that key. The method does not
run a parser or inspect payloads to prove that attestation.

## Composed defaults and automatic read capture

`MFileContent.progressify(progress)`, `shadow(observer)`, and
`captureReads(consumer)` compose into one view with one stream observation state.
The view uses the existing read count and SHA-256 digest. It does not stack another
hasher or read the file again for each decorator. `shadow(layer, observer)` supplies
an explicit byte-domain label; the ordinary input label is `input`. A label does
not convert positions between transport, decoded text, or parser domains.

For a provider entry with metadata support, an ordinary complete transfer can
produce the capture through these defaults:

```java
MFileContent content = file.progressify(progress)
        .shadow(observer)
        .captureReads(capture -> acceptCapture(capture));
try (InputStream input = content.openInputStream()) {
    input.transferTo(destination);
}
```

Here `progress`, `observer`, and `destination` are caller-owned, and
`acceptCapture` is the caller's completion consumer. The view borrows progress and
shadows; closing the input does not close those observers or mark a child progress
monitor done. Source content must begin at offset zero of the selected file layer.
The `MFile` adapter opens that provider entry's input. An arbitrary implementation
which returns an already-positioned stream must not attest whole-file coverage.

`MFileShadow.onBytes(bytes, offset, length)` observes synchronous consumed bytes.
Its borrowed slice is read-only and valid only during the callback; retaining
bytes requires a caller-owned copy. `onComplete(completion)` receives a completed
byte observation. `andThen(next)` composes observers in order. The completion value contains the layer, byte count,
`M3ContentId`, and `CompletionKind`. `INPUT_EOF` means the input actually reached
EOF and its primary close succeeded. `OUTPUT_SEQUENCE` identifies confirmed
writes after successful output flush/close; it is not a full destination snapshot.

The capture view samples metadata for each newly opened input and records the
sample time. Actual EOF alone does not publish a completion callback. After a
clean primary close, the raw shadow completion observers run first; after they
return successfully, the capture is validated and delivered. Early close,
cancellation, read failure, shadow failure, or close failure produces no completed
capture. Capture consumers run once in their configured order. A consumer failure
propagates without retry and prevents later consumers from running. An observation
already delivered to an earlier callback is not rolled back.

`MFileCapture.fromInput(sampledMetadata, capturedAtMillis, completion)` accepts an
`INPUT_EOF` completion and binds its exact count/hash as an existing `RAW_BYTES`
reference. The factory cannot independently inspect the stream's origin or prove
the caller's full-layer attestation. It returns immutable accessors:

| Accessor | Meaning |
| --- | --- |
| `observation()` | The sampled metadata with its validated complete raw reference. |
| `manifest()` | An existing-format `M3FileManifest` containing that raw reference. A raw capture starts without derived structures. |
| `capturedAtMillis()` | Epoch milliseconds when the metadata sample was taken. This is separate from the provider's modification timestamp. |

The raw-only manifest belongs to the capture. `observation().manifest()` remains
empty until a structural result is selected. Capture records content identity; it
does not publish raw bytes or the manifest into a filesystem store.

A known sampled size must equal the completed byte count. A mismatch throws
checked `IOException` and emits no `MFileCapture`; the existing `withContent`
validation is retained. Raw completion observers run before this check and can
still receive the exact byte evidence. Unknown metadata size is permitted; the
raw manifest nevertheless carries the exact observed count. This operation makes
no automatic second metadata query, changes no sampled timestamp, and makes no
atomic-snapshot or source-stability claim. Matching size and modification time
alone never prove that bytes stayed unchanged during a read.

## Existing word IDs and optional structural results

`capture.withStructure(name, reference, key)` returns a new capture using the
existing manifest and precompute-key machinery. It preserves other manifest
references and selects the supplied structure for observation comparisons. Its
key must bind the raw input as the first content identity; the producer remains
responsible for attesting that the output was computed using that key. No parser,
tokenizer, dictionary, payload cache, or storage publication is triggered.

The existing [cold-file image admission](../src/main/java/com/m3/fs/M3ColdFileImage.java)
and [file manifest store](../src/main/java/com/m3/fs/M3FileSystemStore.java) already
support token-file images and source facts. Reference those existing artifacts
when a producer has supplied them. The existing
[index image](../src/main/java/com/m3/fs/M3FsIndexImage.java) preserves supplied
token IDs; its text-only factory deliberately creates empty token spans instead
of inventing identities.

[M3WordRows](../../core/src/com/m3/text/M3WordRows.java) owns generation-qualified
word facts. Its packed word ID contains a language coordinate and a local word
coordinate; equal numeric IDs from different lexicon generations do not establish
equal words. Retain the dictionary identity/generation and relevant tokenizer,
language, encoding, and normalization context in the existing derivation key or
its additional input identities. The durable word-row owner rejects overlay IDs.
This bridge does not add global file interning or replace the current
[Path-based M3File](../../file/src/main/java/com/m3/file/M3File.java).

[M3ASTHierarchy](../../ast/src/main/java/com/m3/ast/M3ASTHierarchy.java) aggregates
already verified atom handles and masks. Reusing its existing immutable result
does not require parsing source again. Producing new atomization results remains
the existing atomizer/compiler owner's work. Its L0 hash includes the source
SHA-256 and atomization receipt; its root is therefore not a whitespace-invariant
structural hash. Comparisons which intentionally ignore trivia must use a producer
and derivation key that explicitly define that meaning.

The retained cost of a raw capture is a few references and primitive facts, plus
its raw manifest; it retains no per-byte, per-word, or per-AST-node object graph.
Adding a derived manifest entry can copy its immutable reference columns while
continuing to share the referenced payload identity.
Hashing still costs work proportional to the bytes read, and a shadow performs
its configured synchronous work. `M3WordRows` uses mapped 128-byte records with a
64-byte hot scalar portion and arithmetic row lookup. `M3ASTHierarchy` keeps
primitive handle/mask lanes and scope metadata, but building a new hierarchy
still copies and hashes occurrences and sorts file facts. These representation
properties do not establish negligible parse cost or a measured performance gain.

## Comparison semantics

`MFileDelta.between(before, after)` retains those two references plus primitive
statuses and a reason bitmask. `hasReason(mask)` tests whether any requested bit is
present. The overload with `Consumer<? super MFileDelta>` invokes the callback
exactly once after validation and returns that same delta. It propagates callback
failure and never retries it.

| Available evidence | Content result | Qualification |
| --- | --- | --- |
| Matching size and timestamp, or matching unknown fields | `UNKNOWN` | Metadata never proves unchanged bytes. |
| Equal authoritative tokens in the same source/scope/layer/mechanism/epoch | `SAME` | `AUTHORITATIVE_PROVIDER_TOKEN`; no content ID is fabricated. |
| Changed authoritative token | `UNKNOWN` | Revision changed; identical bytes may have been rewritten. |
| Matching or changed heuristic token | `UNKNOWN` | Heuristic reasons are retained separately. |
| Equal complete raw identity and length | `SAME` | `FULL_CONTENT_ID`, independent of timestamp changes. |
| Different complete raw identity or length | `DIFFERENT` | Complete observed evidence takes precedence over tokens. |
| Equal authoritative token but different complete raw identities | `DIFFERENT` | `CONFLICTING_EVIDENCE` is also set. |
| Equal authoritative token but contradictory known lengths, without two full references | `UNKNOWN` | `CONFLICTING_EVIDENCE` blocks a token-only unchanged claim. |

An identical digest with contradictory attested lengths is also marked conflicting;
the different lengths prevent byte equality. `CONTENT_SIZE_CHANGED` and digest
reasons remain distinct.

`presence()` reports known additions and removals separately. `metadata()` is
`SAME` only for equal known absence, or equal known presence/type/size/time; a
missing field otherwise yields `UNKNOWN` unless another known field differs.
None of these statuses claims atomic source sampling or live source stability.

Structural equality compares the selected structure's object identity and size
only when the structure name, object kind, key type, algorithm, version,
configuration, and all additional inputs agree. Each primary input remains bound
to its own observation's raw content, so two different raw revisions can have an
equal structural result. An incompatible derivation produces `UNKNOWN`, even
when the output hashes happen to match. Structural hash equality is not a proof
of arbitrary program semantic equivalence.

## Provider change stamps

Existing providers need no change. A provider file object may implement
`MFileChangeSource`; its default `changeStamp()` returns unavailable. The explicit
observation call requests a stamp when the object implements this contract.

`ChangeStamp.authoritative(sourceKey, scope, layerKey, mechanism, epoch, token)`
is a provider attestation that equal tokens in that exact domain identify equal
bytes for the selected source layer. Every field is explicit and nonempty. The
provider owns epoch rollover and the guarantee; M3FS does not infer it from an
ETag spelling or a timestamp. Weak tags and sampled metadata use `heuristic(...)`.
Capability support and stamp authority are separate questions.

## Executable verification

The standalone consumer covers source conflicts, unknown versus absent metadata,
authoritative/heuristic/domain-separated tokens, raw identity precedence and
conflicts, hybrid content/children capabilities, immutable attachments, input-bound
structural provenance, derivation changes, and callback delivery. The JUnit
wrapper runs the same consumer in the existing module test suite.
The default-content and capture consumers additionally cover composed decorators,
clean-close publication, raw completion ordering, strict known-size drift refusal,
capture timestamps, and existing structure binding. The independent named-image
consumer exercises composed progress/shadow/capture through local and selected
archive reads and verifies the exact raw manifest. It must run against a newly
materialized, source-locked product image before those additions are qualified.

For a bounded standalone check against an already compiled product baseline:

```sh
facts_jdk=/path/to/jdk21
facts_runtime=/path/to/compiled/product/classes
facts_output=/path/to/fresh/facts-test-classes
mkdir -p "$facts_output"
"$facts_jdk/bin/javac" --release 21 -Xlint:all -Werror \
  -cp "$facts_runtime" -d "$facts_output" \
  src/main/java/com/m3/fs/MFileObservation.java \
  src/main/java/com/m3/fs/MFileDelta.java \
  src/main/java/com/m3/fs/MFileChangeSource.java \
  src/test/java/com/m3/fs/MFileObservationSelfTest.java
"$facts_jdk/bin/java" -ea -cp "$facts_output:$facts_runtime" \
  com.m3.fs.MFileObservationSelfTest
```

This consumer is Java-only. It is not evidence of the full Maven/native gate,
the final generated linked image, or a source-built OpenJDK pass. Those remain
separate qualifications using the materialized source lock and existing packer.
