<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# CPU-tool plan, image and runtime reference

[Guide](README.md) · [Precompute package](m3-packages.md#precompute-api-contracts) · [Lexical and similarity batches](precompute-batch-reference.md) · [Coverage](coverage.md)

This reference covers the complete bodies of `M3CpuToolPlan`, `M3CpuToolImage`,
`M3CpuToolImageCompiler` and `M3CpuToolRuntime`, including their public nested
classes/record and methods. It also covers `M3CpuToolNativeSignals`, which is a
**package-private adapter**, not a fifth public API. All belong to the isolated
`m3/ports/precompute` module, package `com.m3.precompute`.

Source inspection pin: `13a70cdbbe7b0abc75df18c78e1ca044e0c74240`,
14 October 2026. The five complete source bodies, their CPU self-test/JUnit adapter,
lexical Java/JNI/C boundary, native parity/check sources and build files were read.
Examples, Java/native builds and tests, policy/recipe verifiers, platform and
performance checks are **NOT_RUN** in this documentation review. Descriptions of
work and allocations are source observations, not measurements.

The [existing CPU-tool overview][overview] and [module README][readme] remain
entry points. The [porting invariant][policy] and [name map][mapping] retain
canonical target ownership and the ordered String qualification gate. This guide
adds API detail; it does not admit another JDK backend, complete a donor catalogue,
or change licensing/receiving status. Only public M3JDK21 material informs it.

## Choose preparation, transport or execution

| Task | API | What happens at this boundary |
| --- | --- | --- |
| Declare one stage | `new M3CpuToolPlan.Step(id, operation, dependencies, constant)` | Validate identifiers/local bounds; copy dependency list |
| Prepare a complete graph | `M3CpuToolPlan.compile(steps, outputId)` | Admit operators/graph; sort definitions; compute execution order, literal bytes, KMP prefix arrays, XOR masks and plan digest |
| Export a prepared plan | `M3CpuToolImage.encode(plan)` | Allocate a deterministic declaration image plus SHA-256 checksum |
| Restore an image | `M3CpuToolImage.decode(image)` | Check bytes/version/bounds, rebuild a plan and require canonical re-encoding |
| Prepare TSV declarations | `M3CpuToolImageCompiler.compileTsv(source)` | Parse TSV/Base64URL, then perform ordinary plan compilation |
| Compile explicit files | `M3CpuToolImageCompiler.main(args)` | Read one TSV, encode its plan, create one new image file |
| Discover callable operators | `M3CpuToolRuntime.operations()` | Return an immutable set of 13 IDs; iteration order is not a schedule |
| Execute | `M3CpuToolRuntime.execute(plan, input)` | Snapshot subject bytes, run the serial prepared graph and return copied output with digest receipts |
| Inspect current admission size | `M3CpuToolRuntime.automaticByteBudget()` | Estimate a per-invocation byte ceiling from current heap residency |

These operators are typed binary transformations, not operating-system commands.
A plan carries no executable path, shell text, callback, reflection target or JNI
symbol. Unknown operations fail compilation. The wider candidate catalogue does
not add executable APIs. `main` is the image compiler, not a runtime command
launcher. The plan/runtime do not walk files, launch subprocesses, perform network
requests or alter `java.lang.String`/`java.util.regex.Pattern`.
[Sources: plan][plan], [image][image], [compiler][compiler], [runtime][runtime].

## Plan admission, ownership and identity

### Step construction is local validation

`Step(String id, String operation, List<String> dependencies, String constant)`
is a public record. Its generated `id()`, `operation()`, `dependencies()` and
`constant()` expose immutable strings and the copied, unmodifiable dependency
list. Generated `equals`/`hashCode` compare record components, including ordered
list contents; `toString` is the ordinary record projection.

- IDs, operation IDs and dependency IDs must match `[a-z][a-z0-9_.-]{0,63}`:
  1–64 ASCII characters, with a lowercase letter first. Null `id`/`operation`
  or invalid non-null IDs throw `IllegalArgumentException`.
- `dependencies` must be non-null; `List.copyOf` also rejects null elements.
  At most 16 dependencies are allowed; duplicates are rejected. Ordering is kept.
- `constant` must be non-null and at most 4,096 **UTF-16 code units** at this step.
  Null lists/constants/elements throw `NullPointerException`. Other local
  violations throw `IllegalArgumentException`.
- Construction alone does not verify that the operator exists, its arity is
  correct, dependencies exist, or the constant is usable UTF-8. For example, a
  nonempty unused constant can be constructed and then rejected by `compile`.

### compile validates the whole graph and prepares stable work

`compile(List<Step> steps, String outputId)` requires a non-null list and entries,
a valid output ID, and 1–128 nodes. It rejects duplicate node IDs, operations
outside `operations()`, more than 512 total dependency edges, missing output or
input IDs, cycles in the output's dependency traversal, and any unreachable node.
A disconnected cycle is also rejected as unreachable even if it is never traversed.
`bytes.concat` requires 2–16 dependencies; all other operators allow 0 or 1.
A zero-dependency stage will consume the invocation's original input.

The compiler stores source definitions in lexicographic node-ID order, using a
`TreeMap`. It traverses from the designated output in each node's declared
dependency order, appending each reachable stage after its dependencies. Shared
stages execute once. All nodes must feed the selected output. Reordering the
outer declaration list leaves the canonical definitions unchanged; reordering a
node's dependencies is a real change and determines concatenation order.

For `search.literal`, compilation strictly encodes the constant as UTF-8, rejects
an empty literal or malformed UTF-16, and prepares byte-wise KMP failure links.
For `bytes.xor`, the constant must be exactly two ASCII hexadecimal digits;
uppercase and lowercase are accepted. All other operations require an empty
constant. Strict UTF-8 encoding during preparation/fingerprinting also enforces
a 4,096-byte bound on each framed string. The constructor's code-unit limit
therefore does not imply that a non-ASCII constant will pass compilation.
[Source: Step, PreparedStep, compile, utf8Exact and fingerprint][plan].

### Compiled is plan-owned state, not a global cache

`Compiled` has no public constructor. Its four public methods are:

| Method | Returned contract |
| --- | --- |
| `definitions()` | Unmodifiable ID-sorted `List<Step>`; no prepared arrays |
| `outputId()` | The selected output stage ID |
| `sha256()` | Lowercase hexadecimal SHA-256 of the framed canonical declarations |
| `stages()` | Number of prepared stages, equal to admitted reachable node count |

The digest frames `M3_CPU_PRECOMPUTE_PLAN_V1`, output ID, node count, and each
node's ID, operator, constant, dependency count and ordered dependency IDs. The
node/dependency counts are decimal strings framed like other strings, unlike
the image format's binary integer counts. Each string is strict UTF-8 prefixed
by a big-endian 32-bit byte length. The digest describes
the plan declarations, not a JVM version, native-library identity, current heap
budget, subject bytes, source commit or signer. Hex spelling such as `ff` versus
`FF` remains a different declaration even where an operation behaves identically.
A digest match is not an exact equality proof or authorization to execute.

`Compiled` does not override `equals`/`hashCode`; distinct compilations/restorations
remain distinct Java objects. No plan interning, mapped/shared-memory ownership,
OS-wide string interning, string-backing registration or cross-process cache is
implemented here. Plans retain their immutable definitions and private arrays
until ordinary Java reachability permits collection; there is no `close` or
invalidation API. A changed declaration requires compiling a new plan. Prepared
state is not mutated by execution, whose maps/buffers are invocation-local.
Callers must keep the supplied collection stable during compilation; copying
is not an atomic snapshot of concurrent collection mutation.

## Image format and validation

`M3CpuToolImage.encode(Compiled plan)` rejects null, serializes canonical
declarations and returns a new mutable `byte[]`. It does **not** serialize
runtime input/output, callbacks, machine code, KMP tables, XOR fields or the
plan's digest string. It encodes:

1. Eight ASCII bytes `M3CPU001` as format/version magic.
2. Output ID as a length-prefixed strict UTF-8 string.
3. Node count as a big-endian signed 32-bit integer.
4. For each ID-sorted definition: ID, operation and constant strings, dependency
   count as a 32-bit integer, then ordered dependency strings.
5. 32 raw SHA-256 bytes computed over all preceding payload bytes.

All string lengths count bytes, not UTF-16 units. The whole image, checksum
included, must be no more than 1,048,576 bytes. Encoding constructs its
`ByteArrayOutputStream` payload before checking that total limit, then copies
into the final array and appends the checksum. The payload checksum and
`Compiled.sha256()` use different framing; they are not interchangeable receipts.

`decode(byte[] image)` rejects null and checks the following before returning:

- Coarse image size between 48 and 1,048,576 bytes, inclusive.
- Stored checksum equals a freshly computed payload checksum via
  `MessageDigest.isEqual`.
- Exact `M3CPU001` magic, 1–128 nodes, 0–16 dependencies per node, string lengths
  0–4,096 bytes, enough bytes for each string, and strict well-formed UTF-8.
- No trailing payload bytes, all `Step`/`compile` constraints, and byte-for-byte
  equality with a fresh `encode(restored)`.

The final equality check rejects alternative definition ordering even when an
attacker recomputes the checksum. Unknown version, malformed text, illegal graph,
truncation, checksum changes, noncanonical image and trailing data throw
`IllegalArgumentException`; caught stream `IOException` becomes an
`IllegalArgumentException` with a truncation cause. The checksum is unkeyed:
anyone can construct new valid bytes and their checksum. It authenticates no
signer, provenance, permission or semantic compatibility with another runtime.

Decoding reparses and **recompiles** declarations, allocating strings, lists,
prepared literals/prefix arrays and a new plan. Hash checking copies the payload
and checksum; canonical checking encodes another image. There is no zero-copy
view or retained mapped file. The input image is not defensively snapshotted as
a whole: hashing uses a copy but parsing and final comparison still read the
caller array. Keep it unchanged throughout `decode`; this API makes no atomic
concurrent-mutation guarantee. After return, the plan retains declarations and
prepared state, not the caller's image array. [Source: encode/decode][image].

## TSV and compiler file effects

`M3CpuToolImageCompiler.HEADER` is exactly the following six tab-separated
columns (the displayed `\t` means one actual tab):

```text
M3_CPU_TOOL_DAG_V1\toutput\tstep\toperation\tinputs\tconstantBase64Url
```

`compileTsv(String source)` splits only on LF (`\n`), preserving empty fields.
It requires that exact first line and at least one data row. Every row has
exactly six tab-separated fields: version marker, output ID, node ID,
operation ID, comma-separated dependency IDs, Base64URL-encoded constant bytes.
All rows must name the same output. Empty dependency text means zero inputs;
nonempty dependency text is split on commas without trimming. Constants use
Java's URL-safe Base64 decoder and strict UTF-8 decoding. Empty encoded text
means an empty constant; valid padded or unpadded Base64URL is accepted. Encoded
constant text is capped at 8,192 characters, with plan byte/unit limits applying
after decoding. Malformed Base64URL/UTF-8, wrong header/row width/marker, mixed
outputs and empty plans fail with `IllegalArgumentException`.

No CSV quoting, comments, blank-line skipping, BOM stripping, CRLF normalization
or whitespace trimming is performed. One final empty line from a trailing LF
is allowed; an additional blank row is invalid. A CRLF header retains `\r` and
fails the exact header comparison. Null `source` produces `NullPointerException`.
The direct `String` API does not enforce a total one-megabyte TSV limit and
splits/builds rows before graph admission, so it is not a bounded streaming
parser. [Source: HEADER, compileTsv and decoders][compiler].

`main(String[] args)` requires exactly two arguments, `<plan.tsv> <new-image-file>`.
It resolves those paths directly, reads the **whole** source with `Files.readAllBytes`,
then rejects input larger than 1,048,576 bytes. It strictly decodes UTF-8,
compiles, encodes and calls `Files.write` with `CREATE_NEW` and `WRITE`. An
existing destination is refused instead of overwritten. Successful output is
followed by a stdout line `planSha256<TAB>stages`. File `IOException` propagates,
as do path/validation failures. No directories are created, no path confinement
is imposed, and the write is not an atomic publication/rollback protocol: an I/O
failure after creation can leave an incomplete file. The read size check occurs
after allocation, not before it. Null `args`/path entries have ordinary Java
null failure behavior. [Source: main][compiler].

This example prepares and executes a Java literal plan; it is illustrative and
was not run in this review:

```java
var plan = M3CpuToolPlan.compile(List.of(
        new M3CpuToolPlan.Step("find", "search.literal", List.of(), "aba")), "find");
byte[] image = M3CpuToolImage.encode(plan);
var restored = M3CpuToolImage.decode(image);
var result = M3CpuToolRuntime.execute(restored,
        "ababa".getBytes(StandardCharsets.UTF_8));
ByteBuffer positions = ByteBuffer.wrap(result.bytes()); // big endian
// positions.getInt(), positions.getInt(): byte offsets 0 and 2
```

The corresponding TSV row is
`M3_CPU_TOOL_DAG_V1\tfind\tfind\tsearch.literal\t\tYWJh`, after the exact header,
with real tabs/LF and Base64URL `YWJh` for `aba`. The supported file invocation
is `java -cp m3/ports/precompute/target/classes com.m3.precompute.M3CpuToolImageCompiler plan.tsv plan.m3cpu`
after building the module. It creates a plan image, not a Java classfile.

## Binary operator contracts

All binary integers/doubles emitted by this runtime use Java `ByteBuffer`'s
big-endian order. There is no delimiter, MIME/type tag or textual decimal/hex
format in these operator outputs. Except `text.code-signals`, subject input
may be arbitrary bytes, including malformed UTF-8. Operations do not implicitly
decode one another's results. The caller chooses and interprets the graph.
[Source: operations and apply helpers][runtime].

| Operator ID | Constant and arity | Result and important boundaries |
| --- | --- | --- |
| `bytes.identity` | Empty; 0–1 dependencies | Same internal data buffer, with input/result copies at the public execution boundary |
| `bytes.concat` | Empty; 2–16 dependencies | New contiguous concatenation in dependency-list order; combined length must fit budget |
| `bytes.length` | Empty; 0–1 | Eight bytes: signed 64-bit nonnegative byte length, not character count |
| `bytes.xor` | Two hex digits; 0–1 | Clone of input with the compiled one-byte mask XORed into every byte |
| `hash.sha256` | Empty; 0–1 | 32 raw digest bytes, not 64 hex characters or an equality oracle |
| `search.literal` | Nonempty strict UTF-8 literal; 0–1 | Zero or more 32-bit signed nonnegative byte offsets, increasing and including overlaps; zero hits gives an empty array |
| `ordering.bytes` | Empty; 0–1 | New array in unsigned order 0–255, using 256 frequency bins; no locale/line/text collation |
| `set.unique.bytes` | Empty; 0–1 | Each observed unsigned byte once, in increasing order; does not preserve encounter order |
| `statistics.byte-histogram` | Empty; 0–1 | Exactly 1,024 bytes: 256 signed 32-bit counts, bin index equal to unsigned byte value |
| `compression.rle.encode` | Empty; 0–1 | Pairs `(unsigned count 1..255, byte value)`; equal runs longer than 255 split; zero-length input gives zero bytes |
| `compression.rle.decode` | Empty; 0–1 | Expand those pairs; reject odd-length input, zero count or expanded size above budget; adjacent equal pairs need not be canonical |
| `information.entropy` | Empty; 0–1 | Eight bytes: `double` Shannon entropy of unsigned-byte frequencies, in bits/byte; empty input returns `0.0`; floating-point, not exact rational arithmetic |
| `text.code-signals` | Empty; 0–1 | Strict UTF-8 to UTF-16 lexical scan; eight bytes containing one packed feature `long`; not parser/regex-match output |

Literal matching compares the prepared UTF-8 byte sequence, not Unicode code
points or UTF-16 positions. `é`/supplementary literals are supported through
strict encoding, but offsets still count input bytes. The input need not be
well-formed text, and no normalization, case folding, regex interpretation,
word boundary, region option or empty-pattern behavior is provided. KMP walks
the subject twice per execution: first to count matches and check `4 * hits`
against budget, then to fill the exact-size position buffer. The input scan and
output positions remain runtime work; compiling once does not precompute them.

RLE also makes count/size and fill passes, so random data can expand toward twice
its input size and be refused. Decode verifies total expansion before allocating
the output. Sorting, unique, histogram and entropy scan runtime frequencies in
256 bins. Entropy uses `StrictMath.log` and a precomputed inverse natural log of
two; a deterministic formula does not imply measured performance or accuracy
beyond the implemented floating-point calculation.

## Execution snapshots, receipts and budgets

`execute(Compiled plan, byte[] input)` rejects null arguments, obtains **one**
automatic budget for the call and rejects input above it before cloning. Each
root stage receives that stable cloned input; each one-input stage receives its
dependency's result, and concat first materializes its fan-in. Execution is
serial in the compiled dependency order. Remaining-consumer counts retire an
intermediate map entry after its last dependency use. This removes a reference;
it does not force collection or promise immediate buffer reclamation.

No operation mutates its input buffer in place: identity/concat return the
selected internal buffer; XOR clones before mutation; other transforms produce
their own results. The final `Execution` constructor clones output again and
hashes that clone. The original caller array is not retained. The initial clone
is not synchronization with a caller concurrently writing the array; keep input
stable while the snapshot is taken. Prepared plan reuse does not cache subjects,
intermediate results or whole execution outputs across calls. [Source: execute][runtime].

`Execution` has no public constructor or custom equality and exposes:

| Method | Result |
| --- | --- |
| `bytes()` | A fresh defensive clone on every call |
| `planSha256()` | The plan's declaration digest string |
| `inputSha256()` | Lowercase SHA-256 hex of the invocation's stable input clone |
| `outputSha256()` | Lowercase SHA-256 hex of the stored final output copy |
| `steps()` | Total prepared/executed stage count |
| `byteCount()` | Stored output byte length |

These are content receipts, not full byte-equality witnesses, native-provider
attestations, authorization tokens or proof of performance. The result owns its
copied output for its ordinary Java lifetime; no explicit release is necessary.

`automaticByteBudget()` computes `used = totalMemory - freeMemory`, then
`remaining = max(0, maxMemory - used)`, and clamps `remaining / 4` between
8,192 and 67,108,864 bytes. A later call can return a different value. There is
no caller-supplied budget overload; JVM maximum heap and current residency affect
admission. The 8 KiB minimum can exceed actually free memory.

Checks cover input size, fan-in totals, each stage output, literal-position
output, RLE encoded/expanded size, and a post-stage sum of stable input length
plus lengths of all live output-map entries, capped at twice the budget. The
resident sum counts entries, **not unique arrays**: identity/shared-buffer aliases
can be counted repeatedly. Conversely it excludes collection/object overhead,
prepared tables, some temporaries, the final result clone/digests, UTF-8 decoder
buffers, UTF-16 copies and JNI scratch. Some checks occur after allocations.
It is neither exact peak-heap accounting nor a reservation, total-process/native
memory cap, allocation-free guarantee or protection against `OutOfMemoryError`.

Budget and invalid input/format failures generally throw `IllegalArgumentException`.
Unresolved prepared dependencies, unknown admitted tools or Java/native lexical
mismatch throw `IllegalStateException`; ordinary JVM/native loading/allocation
errors can also propagate. No partial `Execution` is returned on failure.
There is no CPU-tool progress callback, cancellation token, interruption check,
timeout or parallel scheduling API. The internal lexical calls explicitly use
`M3Progress.none()`; the batch interfaces' cancellation facility is not exposed
through this runtime. Size limits do not establish bounded wall-clock time.

## Text and optional native boundary

`M3CpuToolNativeSignals.pack(byte[])` is package-private. It strictly decodes
UTF-8 with malformed/unmappable reporting, copies decoded UTF-16 into a new
`char[]`, and constructs one-row offset/length arrays `{0}` and `{characters.length}`.
Malformed bytes throw `IllegalArgumentException("invalid input UTF-8")`.

It **always** computes Java lexical facts first using
`M3CodeTextSignalBatchJava.INSTANCE.analyze` with `Limits.DEFAULT` and
`M3Progress.none()`. Defaults are 64 MiB transfer accounting, 16,000,000 UTF-16
units and 1,000,000 rows; the adapter uses one row, accounting for
`2 * utf16Units + 16` transfer bytes. Decoding/copying happens before those
provider checks, so a runtime-admitted byte input may still fail lexical limits.
No override of those defaults is exposed by the CPU-tool API.

Only when `Boolean.getBoolean("m3.cpu.tools.native.signals")` is true does it
also load and invoke `M3CodeTextSignalBatchNative` on the same UTF-16 row. Results
must be `Arrays.equals`; a mismatch throws. It then returns the **Java** packed
long encoded in an eight-byte big-endian array. This opt-in is additional parity
work, not a path that bypasses Java computation or proves acceleration.

The packed layout is low 16 bits of lexical flags, then six saturating unsigned
8-bit lanes at shifts 16, 24, 32, 40, 48 and 56 for semicolons, braces,
parentheses, backslashes, regex-meta characters and punctuation. Counts cap at
255. Flags/counts describe syntax-looking characters, not validity, semantics,
text identity or regex matches. The [batch reference](precompute-batch-reference.md)
documents that owner in detail.

The existing loader first checks `m3.precompute.native.path`, then
`m3.precompute.native.pathFile`, then `System.loadLibrary("m3_precompute")`.
Those properties belong to host configuration, not plan declarations. Loaded
state is process/class-loader state with no unload API here; a failed required
load propagates instead of silently falling back. `text.code-signals` with the
opt-in disabled uses Java regardless of native availability.

JNI reuses the existing `nativeAnalyzeRange` symbol and C11
`m3_precompute_code_text_signal` kernel. It validates geometry, allocates native
row metadata/result scratch and a largest-row UTF-16 scratch buffer, copies
with `GetIntArrayRegion`/`GetCharArrayRegion`, writes a Java `long[]`, and frees
its scratch on success/error paths. It does not retain a Java heap pointer
between calls. Java then copies each JNI batch result into its own result array.
For this adapter there is only one row, so row chunking gives no within-row
cancellation. Existing C whitespace predicates and Java `Character.isWhitespace`
remain separate implementations whose parity needs the applicable tests.
[Sources: adapter][native-adapter], [lexical interface][lexical-api],
[Java lexical][lexical-java], [native loader/provider][lexical-native],
[JNI][jni], [C kernel][c-kernel] and [header][c-header].

## Build routes, assertions and explicit gaps

The [POM][pom] selects Java release 21, compiler plugin 3.13.0,
`-Xlint:all -Werror -proc:none`, JUnit Jupiter 5.10.2 and Surefire 3.2.5 with
module-path use disabled. Its ordinary dependency list is test-only JUnit;
opt-in OpenRewrite profiles are authoring/proof tooling. The existing module
commands are:

```sh
mvn -B -ntp -f m3/ports/precompute/pom.xml test
mvn -B -ntp -f m3/ports/precompute/pom.xml -Pnative test
```

These are documented routes, not run receipts. `-Pnative` configures CMake,
builds and runs CTest before Java tests, then supplies
`m3.precompute.native.pathFile` to Surefire. [CMake][cmake] requires version 3.20,
JNI headers and a C11 compiler, and builds the existing `m3_precompute` shared
library. The profile does **not** set `m3.cpu.tools.native.signals=true`.

The [CPU JUnit adapter][junit] calls [M3CpuToolPrecomputeSelfTest.runAll()][selftest].
Its source includes these checks:

| Area | What the test actually checks |
| --- | --- |
| Literal search | Six literals including accented/supplementary characters, 2,000 seeded byte inputs each, comparison to naive byte positions, plus overlapping `aa` matches |
| Operators | SHA bytes, result stage/count accessors and defensive `bytes()` clone, byte length, histogram sample, unsigned sort/unique, XOR, two entropy examples, RLE roundtrips and runs over 255 bytes |
| Plans/images | Declaration-list reorder gives the same digest, fan-in followed by sorted unique result, encode/decode fixed point, plan digest preservation, identical execution output, corruption/truncation/appending refusal, unmodifiable definitions |
| Admission | Empty/unknown/duplicate/dangling/cyclic/unreachable graph, invalid ID/arity/constant/surrogate, odd/zero-count RLE and input larger than the then-reported budget |
| TSV/files | One literal row executes, text/image digest agrees, bad header/Base64URL/mixed output refused; CLI creates a readable image and refuses an existing destination |
| Lexical route | Eight-byte nonzero Java lexical result, malformed UTF-8 refusal, and an assertion that the CPU native opt-in property is false |

`m3.cpu.precompute.coreOnly=true` skips the lexical method in that self-test.
Consequently the CPU suite is not itself an opt-in native adapter qualification:
with the lexical method enabled it explicitly expects the native flag to be
false. [M3NativeParityTest][native-test] instead compares the existing lexical
and similarity batch providers over its finite examples; without explicit native
path/path-file configuration it is skipped by JUnit assumption. The [C check][c-test]
asserts selected lexical flags/counts, whitespace and null/empty-kernel behavior.
Those sources establish intended assertions, not their successful execution here.

Remaining verification includes correctly checksummed adversarial image
versions/strings/definition order, all graph/constant/image size boundaries,
image mutation races, full receipt validation, direct concat-order assertions,
repeated/concurrent execution,
intermediate residency/fan-in/position/RLE limit edges, constrained-heap and
native allocation failures, direct CPU opt-in mismatch/unavailability paths,
Unicode-wide Java/C parity, platform coverage and cold/warm CPU/heap/native
measurements. This is a test-gap inventory, not a claim these paths are defective.
No repository execution, new native ABI, runtime promotion or whole-package
acceptance is supplied by this documentation increment.

[plan]: ../../m3/ports/precompute/src/main/java/com/m3/precompute/M3CpuToolPlan.java
[image]: ../../m3/ports/precompute/src/main/java/com/m3/precompute/M3CpuToolImage.java
[compiler]: ../../m3/ports/precompute/src/main/java/com/m3/precompute/M3CpuToolImageCompiler.java
[runtime]: ../../m3/ports/precompute/src/main/java/com/m3/precompute/M3CpuToolRuntime.java
[native-adapter]: ../../m3/ports/precompute/src/main/java/com/m3/precompute/M3CpuToolNativeSignals.java
[lexical-api]: ../../m3/ports/precompute/src/main/java/com/m3/precompute/M3CodeTextSignalBatch.java
[lexical-java]: ../../m3/ports/precompute/src/main/java/com/m3/precompute/M3CodeTextSignalBatchJava.java
[lexical-native]: ../../m3/ports/precompute/src/main/java/com/m3/precompute/M3CodeTextSignalBatchNative.java
[jni]: ../../m3/ports/precompute/src/main/native/src/m3_precompute_signals_jni.c
[c-kernel]: ../../m3/ports/precompute/src/main/native/src/m3_precompute_signals.c
[c-header]: ../../m3/ports/precompute/src/main/native/include/m3_precompute_signals.h
[selftest]: ../../m3/ports/precompute/src/test/java/com/m3/precompute/M3CpuToolPrecomputeSelfTest.java
[junit]: ../../m3/ports/precompute/src/test/java/com/m3/precompute/M3CpuToolPrecomputeTest.java
[native-test]: ../../m3/ports/precompute/src/test/java/com/m3/precompute/M3NativeParityTest.java
[c-test]: ../../m3/ports/precompute/src/main/native/tests/m3_precompute_signals_checks.c
[pom]: ../../m3/ports/precompute/pom.xml
[cmake]: ../../m3/ports/precompute/src/main/native/CMakeLists.txt
[overview]: ../../m3/ports/precompute/docs/M3_CPU_TOOLS_PRECOMPUTE_20261014.md
[readme]: ../../m3/ports/precompute/README.md
[policy]: ../../m3/docs/m3jdk21-porting-invariant.md
[mapping]: ../../m3/docs/name-mapping.json
