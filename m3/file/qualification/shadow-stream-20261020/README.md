<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 provider-owned shadow streams and subprogress

This is an **in-place M3JDK21 file-module capability**, not an M3 + JDK
wrapper and not another filesystem. Its original `com.m3.file.M3File`
source, NIO provider, NOFOLLOW_LINKS, metadata/attributes API, module
descriptor and all public interfaces remain byte-identical.

## Streaming contract

`M3FileStreams.input(M3File, windowBytes, primaryMonitor, shadowMonitor)`
opens exactly **one original M3File input** and returns one owner with
`primary()` and `shadow()`. The observer is a real InputStream, not a
new file handle, seek, eager replica or second parse. It can only read
bytes that the primary already consumed. Observers can read concurrently
in a separate thread, or read after primary has made progress; reading
a still-empty observer on the same thread before primary starts blocks.

A single bounded byte[] ring stores only unconsumed observed bytes.
When the observer lags past the configured maximum (1..1,048,576 bytes),
the next primary read **throws before reading upstream bytes**. This is
deliberate fail-fast backpressure, not data loss, unlimited buffering or
an implicit daemon. Shadow cancellation, interruption, closing, EOF,
byte-at-a-time reading, bulk reading and `skip()` have independent
contracts; primary `skip()` really reads and forwards bytes so a nested
parser cannot silently miss skipped content. Mark/reset is refused:
there is no second source version available for replay. After the
observer closes, primary can continue without waiting or buffering.

This supports layered lexing/parsing by connecting the first stream's
shadow to a second stream owner and delegating analysis to the original
parser. **The parser/AST is still owned by the original javac/JDT/Jackson
or other frontend**. A shadow stream does not itself coalesce two
independent parser invocations into one AST, nor validate semantic
classpath changes.

## Output and progress

`M3FileStreams.output(primaryOutput, shadowOutput, primaryMonitor,
shadowMonitor)` emits each original byte exactly once into each supplied
OutputStream. There is no retained output payload. Cancellation is
checked before writes; progress is counted only after each successful
individual write. A failed shadow write cannot roll back bytes
already written to the primary output, and propagates an IOException
instead of claiming transactional output success.

`SubProgressMonitor` is a Java21 functional interface with default
`isCanceled()`, `done()`, and `subMonitor(parentTicks, childUnits)`.
The default child monitor uses deterministic integer work allocation,
synchronized advancement, exact final rounding, and strict 0..1,000,000
tick budgets; it never silently over-credits parent progress. No
parent callback/monitor is manufactured for each byte. No native
pointers or Java stream wrappers cross JNI.

Examples:

```java
var file = M3File.of(java.nio.file.Path.of("input.json"));
var progress = (M3FileStreams.SubProgressMonitor) units -> { };
try (var session = M3FileStreams.input(file, 65536,
        progress.subMonitor(60, 100), progress.subMonitor(40, 100))) {
    InputStream consumer = session.primary();
    InputStream shadow = session.shadow();
    // Advance consumer in its original parser; a separate worker may consume
    // the shadow as bytes become available. Neither reader reparses the file.
}
```

## Evidence and donor catalogue

The two product Java files were compiled with **actual, unchanged
M3File.java Git blob `c182a3b1526f9fd0ca20c631d97df3048076defd`**,
on stock Java 21.0.12.1, `javac --release 21 -proc:none -Xlint:all
-Werror`. Deterministic + randomized `M3FileStreamsContract` passed
**17,934 checks** both normal and `-Xint`. Tests cover observer-first
virtual-thread blocking, zero source read-ahead, original NIO read,
every ring boundary/partial chunk, fail-before-read behavior, byte
ordering, shadow skip, closure, interruption/cancellation, two
OutputStream sinks, a primary-success/shadow-failure boundary and
hierarchical default-method progress accounting.

Two **source-sealed Java OpenRewrite recipe** postimages have
`ABSENT` preimages and exact SHA-256 plus canonical engine custody
under `m3/tooling/shared-frontend/recipe-custody/datasets/
m3-file-shadow-20261020`. The existing Maven/OpenRewrite
`M3HashPinnedJavaSnapshotRecipe`, SDK JUnit refusal and fixed-point
gates remain authoritative; no new recipe engine is introduced.
See `CATEGORY_CATALOG.tsv` for problem-category donor decisions.
Challenge sites are classified, not copied.

The original `M3File` and `com.m3.file` module descriptors have no
change in this PR. Attribute/date/length metadata remain **advisory
freshness hints**. Content hashes, class ABI facts and MIndexAST
structure hashes must be joined by the original M3File/M3AST delta
pipeline before downstream consumers may skip work. Existing drafts
#1581/#1590 and native M3FS/indexed file deltas remain separate;
this shadow stream does not duplicate or overwrite those owners.

## Repeat and admission

```sh
root="$PWD/m3/tooling/shared-frontend/recipe-custody"
mvn -B -ntp -f "$root/pom.xml" -Pcontract \
  -Dm3.dataset=m3-file-shadow-20261020 \
  "-Drecipe.owner.root=$root/sdk-owner" verify
python3 "$root/receive.py" --dataset m3-file-shadow-20261020 \
  --recipe-owner "$root/sdk-owner" --target "$PWD" --check

mkdir -p /tmp/m3-file-shadow
javac --release 21 -proc:none -Xlint:all -Werror -d /tmp/m3-file-shadow \
  m3/file/src/main/java/com/m3/file/M3File.java \
  m3/file/src/main/java/com/m3/file/M3FileStreams.java \
  m3/file/src/test/java/com/m3/file/M3FileStreamsContract.java
java -cp /tmp/m3-file-shadow com.m3.file.M3FileStreamsContract
java -Xint -cp /tmp/m3-file-shadow com.m3.file.M3FileStreamsContract
```

The hosted Maven/OpenRewrite SDK, complete M3JDK21 build/HotSpot,
cross-platform JNI/GC/runtime integration and application-level
Maven/Gradle/JDT progress propagation remain admission gates.
**Keep draft until those product gates are observed.**
