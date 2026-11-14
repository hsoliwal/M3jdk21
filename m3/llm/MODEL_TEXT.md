<!-- SPDX-License-Identifier: Apache-2.0 -->
# Exact text, typed questions and numerical codes

`jdk.m3.llm.model` supplies three separate component contracts. `M3ModelInput`
retains exact immutable input text and dependency identities.
`M3SystemOneSchema` and `M3SystemOneRequest` describe typed questions and prepare
that input. `M3NcmQuantizer` converts supplied numerical encoder latents into
`M3NcmCodes` using frozen codebooks. The optional JNI library accelerates the
nearest-code search inside that numerical operation.

These are reusable primitives. No Jev service, NCM transformer, trained
encoder/decoder, model weights or training pipeline is included. Preparing an
input does not run a model. Encoding numerical latents does not prove that an
encoder produced them from the attached text. The caller must establish that
relationship before attaching the result.

## Exact String input and cache identity

`M3ModelInput` retains the original `String` reference. On M3JDK21 that String
continues to use the ordinary String implementation; no second storage pool
or public internal M3String coordinate is introduced. On a stock JDK the same
API uses that JDK's ordinary String implementation.

Each input binds four lower-case, 64-character SHA-256 roots:

| Root | Caller responsibility |
| --- | --- |
| `modelRoot` | Exact immutable model/configuration version |
| `tokenizerRoot` | Exact tokenization and preprocessing version |
| `schemaRoot` | Complete question/output schema version |
| `contextRoot` | All other execution state that can affect evaluation, including relevant source/policy generations and position/attention context |

These roots are supplied by the caller. Syntax validation cannot establish
that a root describes a real artifact. A floating model name is insufficient
as a version root. Typed request preparation additionally binds the configured
model name and computes the exact schema root.

Text hashing streams framed raw UTF-16 code units, including NUL and unpaired
surrogates. It does not use a replacement-producing UTF-8 conversion. `sameText`
checks the text digest and then exact String equality. `matches` additionally
checks all namespace fields. Learned result equality is not permission to
substitute a result for its source.

```java
import jdk.m3.llm.model.M3ModelInput;

// These four values must identify the caller's actual immutable dependencies.
M3ModelInput input = M3ModelInput.of(sourceString,
    modelSha256, tokenizerSha256, schemaSha256, contextSha256);
boolean reusable = input.matches(previousInput);
```

## Typed System One request structure

The question shapes follow the public Jev documentation:
[NOUL, Choice and Score](https://docs.typesafe.ai/concepts/system-one).
This is an independent request structure with no provider invocation or
built-in evaluation implementation.

| Question | Structural contract |
| --- | --- |
| `NoulQuestion` | One yes/no probability component, with optional true/false criteria; no invented separate confidence field |
| `ChoiceQuestion` | 2–255 unique named alternatives, each with an optional criterion; a complete answer requires a distribution over every alternative |
| `ScoreQuestion` | 1–255 ordered rubric descriptions; array position defines the numeric score and a complete answer requires a distribution over every level |

Question identifiers must be unique. Required text must be nonblank; question
text cannot contain NUL. Optional text preserves the distinction between null
and empty. Lists are copied and exposed immutably. Question order, option
order, rubric order, instructions and criteria all contribute to schema identity.
These checks validate request structure, not returned probability distributions.

```java
import java.util.List;
import jdk.m3.llm.model.M3SystemOneRequest;
import jdk.m3.llm.model.M3SystemOneSchema;

var schema = M3SystemOneSchema.compile(List.of(
    new M3SystemOneSchema.NoulQuestion(
        "supported", "Use the supplied evidence", "Supported", "Unsupported"),
    new M3SystemOneSchema.ChoiceQuestion("route", null, List.of(
        new M3SystemOneSchema.ChoiceOption("accept", "Enough evidence"),
        new M3SystemOneSchema.ChoiceOption("review", "More evidence required"))),
    new M3SystemOneSchema.ScoreQuestion(
        "coverage", null, List.of("None", "Partial", "Complete"))));
var request = new M3SystemOneRequest("caller-selected-model", sourceString, schema);
var prepared = request.prepare(modelSha256, tokenizerSha256, contextSha256);
```

## Residual vector quantization

The Java scalar adaptation follows the public multilevel
residual-codebook design reviewed in
[`HazyResearch/ncm`](https://github.com/HazyResearch/ncm/tree/c70274273f489abc73832b2b98584a8483b6b203).
See [the donor notice](src/main/native/NCM_DONOR_NOTICE.md) for the pinned
reference and license. There are no runtime donor JARs, Python dependencies or
automatic checkpoint downloads.

Input latent storage is position-major/channel-minor `float[]`. Each level has
an independent flattened codebook. Level lengths are positive, nondecreasing
and end at the full latent length. Codebook sizes are positive. Area pooling
uses floor/ceiling bins; linear upsampling uses half-pixel coordinates with edge
clamping. Nearest-code distances use ordered double multiplication/addition;
ties choose the lowest code index. Produced components round to float. Nonfinite
inputs and numerical overflow are refused.

```java
import jdk.m3.llm.model.M3NcmQuantizer;

// Small illustrative numerical fixture, not a trained model or a text encoder.
var quantizer = M3NcmQuantizer.of(
    2, 1, new int[] {1, 2}, new int[] {2, 2},
    new float[] {0, 2, 0, 1}, M3NcmQuantizer.Backend.JAVA);
var codes = quantizer.encode(prepared, new float[] {2, 3});
float[] reconstructedLatents = quantizer.reconstruct(codes); // [2, 3]
float[] firstLevel = quantizer.reconstruct(codes, 1);         // [2, 2]
```

`withResidualConvolution` accepts frozen per-level residual transforms. Weight
order is `[level][output channel][input channel][kernel]`; biases are
`[level][output channel]`. The kernel must be positive and odd, with zero padding
and a finite per-level ratio in `[0,1]`. The contribution is
`(1-ratio) * input + ratio * convolution`. The codebook root includes geometry,
floating-point bit patterns, residual parameters and the numerical semantics
version. Backend selection does not change that root.

Learned resampling and GELU convolution stacks are outside this API. Importing
a checkpoint that uses them requires a separately validated implementation;
silently dropping them would change the model. This component makes no
checkpoint-parity, prediction-quality, compression-ratio or speedup claim.

`M3NcmCodes` owns copied primitive code/offset/size arrays and retains the exact
input separately. Codes are indices in versioned numerical codebooks, not
canonical text IDs or lexicon concepts. Accessors return copies. Reconstruction
rejects a different codebook or level shape. It reconstructs numerical latents,
not exact source text.

`retainedBytes()` reports owned primitive array payloads, excluding object
headers, digest Strings, exact text and temporary work. The explicit code
admission budget uses that same narrow accounting. It is not a whole-process
or heap-memory limit. Array inputs must not be mutated concurrently while a
factory or operation is copying them.

## Bounded quantizer checkpoints and next-concept inputs

`checkpointBytes(maxBytes)` exports the complete supported quantizer state.
`fromCheckpoint(bytes, expectedCodebookRoot, maxBytes, backend)` validates and
restores it. The canonical big-endian format preserves the numerical semantics,
geometry, codebooks, residual ratios, weights and biases, including raw signed-zero
bits. A versioned envelope identifies the plain or residual-Conv1d architecture.
Backend choice remains outside model identity. Canonical Synexia and portable M3
owners intentionally share this wire format and codebook-root algorithm.

The decoder rejects unsupported versions/architectures, malformed lengths,
nonfinite state, inconsistent dimensions, trailing/truncated data and an unexpected
codebook root before allocating decoded model arrays. `maxBytes` bounds encoded
bytes, not the process heap. Restore additionally uses a bounded snapshot plus
decoded and immutable-owned arrays; its precise primitive accounting is in the API
Javadoc. The caller must not concurrently mutate the bytes during the initial copy.
Store the returned bytes through the application's existing artifact/checkpoint
owner; the quantizer adds no filesystem store or automatic model download.

```java
byte[] checkpoint = quantizer.checkpointBytes(4096);
var restored = M3NcmQuantizer.fromCheckpoint(checkpoint,
    quantizer.codebookRoot(), 4096, M3NcmQuantizer.Backend.JAVA);
float[] nextLevel = restored.nextLevelInput(codes, 1); // [2, 2] for the fixture above
float[] modelInputs = restored.nextConceptInputs(codes);
```

`nextLevelInput` accepts `1 <= completedLevels < levelCount`. It accumulates the
completed residual contributions and area-pools that reconstruction to the next
level. `nextConceptInputs` concatenates these arrays in level order with one
cumulative pass; a single-level quantizer returns an empty array. This is the
pinned NCM donor's next-concept input boundary. The transformer, tokenizer and
trained encoder/decoder remain separate requirements.

Nearest-code search now prunes a later candidate when its nonnegative partial
squared distance reaches the best distance. Ties still select the smallest code,
and a zero-distance winner can finish its row. Full finite-value validation runs
first, so an early exit cannot hide an invalid later component. Both Java and C++
are checked independently against the unpruned ordered-double algorithm. Worst-case
complexity remains O(rows * codebookSize * channels); no measured speedup is claimed.

## Optional JNI backend

Build with Java21 headers, a C++20 compiler and CMake 3.20 or newer. From the
repository root, with `JAVA_HOME` set to the selected Java21 installation:

```sh
cmake -S m3/llm/src/main/native -B m3/llm/target/ncm-native \
  -DCMAKE_BUILD_TYPE=Release -DM3_NCM_TESTS=ON
cmake --build m3/llm/target/ncm-native
ctest --test-dir m3/llm/target/ncm-native --output-on-failure
```

The shared library is `libm3_ncm.so` on Linux, `libm3_ncm.dylib` on macOS or
`m3_ncm.dll` on Windows, subject to the chosen generator's output directories.
This is the only NCM native library: the same target also carries the
`jdk.m3.llm.projection.M3NcmNativeOps` resize, Phi and tied-logit primitives, and
their nearest-code entry uses the kernel's FP32 expanded-norm entry
`m3_ncm_nearest_expanded_f32`, matching `M3NcmOps.nearest` bit for bit. `M3NcmNativeOps`
loads it at class initialization from `-Dm3.ncm.library` when set, otherwise with
`System.loadLibrary("m3_ncm")`; `-Dm3.llm.ncm.disableNative=true` keeps it Java-only.
There are no implied platform qualification results from this build recipe.
The strict compiler settings disable fast-math and floating-point contraction;
retain them for agreement with the Java oracle.

Loading is explicit and the ABI is checked:

```java
import java.nio.file.Path;

M3NcmQuantizer.loadNative(Path.of(absoluteLibraryPath));
var nativeQuantizer = M3NcmQuantizer.of(
    2, 1, new int[] {1, 2}, new int[] {2, 2},
    new float[] {0, 2, 0, 1}, M3NcmQuantizer.Backend.NATIVE);
```

`JAVA` always uses the Java oracle. `NATIVE` refuses use before a successful
explicit load. `AUTO` selects once at invocation entry and uses Java when a
library has not been loaded. Failures from an available native library propagate;
they are not converted to a successful fallback result. Native code receives
primitive numerical arrays only; no String internals cross JNI.

## Component qualification

```sh
mvn -f m3/llm/pom.xml verify
# After the explicit native build, provide the absolute path for JNI tests:
mvn -f m3/llm/pom.xml -Dm3.ncm.library=/absolute/path/libm3_ncm.so verify
# The same path also drives M3NcmNativeOps; require it and check JNI use:
mvn -f m3/llm/pom.xml "-DargLine=-Xcheck:jni -Dm3.ncm.library=/absolute/path/libm3_ncm.so -Dncm.test.native.required=true" verify
# Optional standalone C ABI sanitizers on a compatible compiler/runtime:
cmake -S m3/llm/src/main/native -B m3/llm/target/ncm-sanitized \
  -DM3_NCM_TESTS=ON -DM3_NCM_SANITIZERS=ON
cmake --build m3/llm/target/ncm-sanitized
ctest --test-dir m3/llm/target/ncm-sanitized --output-on-failure
```

The Java suite covers raw UTF-16 identity, namespace mismatches, immutable
storage, typed schema structure, numerical fixtures, codebook identity and
failure boundaries. The explicitly enabled JNI suite compares Java and native
nearest-code selection and complete quantizer output. The standalone C ABI
test checks bounded randomized cases and invalid shape/nonfinite inputs.
Default Java-only verification does not execute the JNI tests.

The existing `ModuleImageMain` additionally checks the exported package,
JDK-only module dependency closure and Java numerical/typed-input behavior
inside a linked image. The normal README contains the `jlink` commands.
Passing one absolute library path to `ModuleImageMain` also exercises JNI
selection through the named module; use `-Xcheck:jni` for this explicit run:

```sh
/path/to/fresh-image/bin/java -Xcheck:jni --add-modules jdk.m3.llm \
  -cp m3/llm/target/test-classes jdk.m3.llm.test.ModuleImageMain \
  /absolute/path/libm3_ncm.so
```
Component tests on a stock JDK do not qualify a source-built M3JDK distribution,
live Jev inference, a trained NCM checkpoint, a GPU or an unexecuted platform.
