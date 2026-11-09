<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# Shared source deltas from the existing compiler event

`M3JavacParser.sourceProjection()` exposes one reusable `M3AST.Projection<M3File>`
identity. The existing `M3AST.Unit.project` memoizer owns its result only for the
current event. The operation retains no compiler state or source payload between
events, tasks, workers or builds. Equal operation names remain insufficient for
sharing any other projection.

The source factory accepts only the original `PARSED` event, obtains its existing
`M3AST.Source`, retains the immutable String through `M3File.of`, and verifies the
same exact UTF-16 SHA-256 seal as the existing tree projection. It reads no file,
does not invoke a parser and does not traverse the AST. The existing tree
projection now requests this source identity through the same event memoizer.
Separate tree-projection keys still produce separate node arrays and retain their
original node budgets, ranges and failure behavior.

`M3JavacParser.changesFrom(unit, previous)` requests that shared source and calls
the existing `M3File.changesFrom` operation. The result is an exact UTF-16 source
replacement window plus URI and optional byte-seal evidence. It does not classify
semantic changes or grant reuse of attributed facts. The caller supplies a real
detached previous snapshot; missing prior evidence is not replaced with an
invented empty file. Compiler-retained character input carries no original-file
byte seal, so its byte comparison remains `UNKNOWN`.

Existing Maven and Gradle `M3AST.Consumer` implementations can request these APIs
inside `parsed(Unit)`. The current `M3GradleASTProvider` already attaches consumers
to the original javac task; neither its SPI nor its consumer-discovery ownership
changes. Consumers needing only source deltas do not request a tree projection
or change syntax-retention policy. A callback view remains thread-bound and
expires when the event callbacks return. A non-PARSED request is refused before source capture, and the
existing memoizer retains that failure within the event.

An up-to-date or cache-restored build task emits no compiler callback. These APIs
do not change Maven/Gradle scheduling, perform class-ABI comparison, identify
generated-output ownership or establish cross-process semantic reuse. Actual
class-file fingerprints remain a separate post-compilation observation.

## Verification contract

`M3SharedSourceDeltaContract` runs actual javac compilations with and without the
consumer. It checks shared source identity across two source consumers and a tree
consumer, one source-only projection computation, unchanged source-read counts
during requests, exact edit ranges, URI changes, unknown byte seals, separate
task/event identities, expired and wrong-phase refusal, and baseline parity of
generated class bytes and diagnostic facts. Fixtures include multiple top-level
classes and a compiler error. It emits `M3_SHARED_SOURCE_DELTA_PASS` only after all
checks pass. Existing `M3JavacParserContract` remains the independent tree/range,
budget and lifetime oracle; its counters include the new nested source operation.

Use the existing parser builder and its source-built external compiler patch.
The dedicated contract accepts the existing `m3.parser.test.work` system property
for compiler outputs. Compiler-module patch qualification is distinct from a
source-built M3JDK image, a custom Gradle distribution or Maven reactor execution.
Record those gates separately with exact source and execution receipts.
