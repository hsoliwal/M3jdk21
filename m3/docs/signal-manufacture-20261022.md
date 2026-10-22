<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Integrated compiler method facts and lexical JNI signals

M3JDK21 owns both capabilities inside its existing modules. `jdk.compiler`
manufactures detached method facts from the original javac analysis event;
`java.base/libjava` implements an explicitly selected native batch for the
existing lexical receiver. There is no Synexia runtime dependency.

Canonical receiving recipe: `com.synexia.m3.M3JdkSignalManufacture20261022`,
version `20261022`, in the Synexia convergence workspace. The exact Java and
text preimage/postimage manifests and real SDK replay evidence are maintained
there. This receiving document does not introduce a second recipe engine.

## Method facts

Call `M3AST.methodFacts(unit)` during an original live `ANALYZED` callback.
The existing event projection cache shares one immutable result for consumers
of that event. Capturing it performs no second parse, attribution or source
reread. Retain the detached `M3AST.MethodFacts`, never the live unit or compiler
objects. Existing thread, phase and close checks remain authoritative.

`M3AST_METHOD_FACTS_V1` exposes 24 stable column IDs. Declaration flags use a
long lane; the remaining coordinates use int lanes. A long presence mask per
method distinguishes unavailable observations from zero. Scalar access to an
unavailable value refuses; raw lane copies must be interpreted with the mask.

| Group | Columns |
| --- | --- |
| Declaration | Flags, parameter count, declared throws count, method ID, owner ID |
| Source | Method and body start/end, expressed in original UTF-16 offsets |
| Control syntax | If, conditional expression, switch and loop counts; maximum block depth |
| Body syntax | Invocation, new-class, new-array, throw and synchronized-statement counts |
| Relationships | Field-read, field-write, lambda and member-reference counts |
| Completeness | Unresolved or unrepresentable observed symbol flag |

Four compressed sparse row relations contain call, field-read, field-write and
member-reference occurrences. Offsets index contiguous primitive member IDs
and owner IDs. IDs refer to one sorted identity dictionary; member identities
include owner and erased resolved signature. Repeated sites remain repeated.
Member references are distinct from calls. Array-element mutation does not
become a write to the field holding the array reference.

Explicit methods and constructors in the analyzed declaration and its nested
classes receive rows. Compiler-generated methods and implicit constructor calls
do not become source sites. Counts for an enclosing method exclude nested class
and lambda bodies. Abstract/native methods have no body observations.

Source URI, original UTF-16 length/digest and analyzed binary name accompany
the arrays. An opaque compiler-session ID distinguishes separate tasks using
identical text. It is not a reproducible classpath/toolchain fingerprint or a
certificate of authenticity. Persistent reuse requires the consumer's complete
semantic-context and invalidation policy.

Projection budgets are explicit: 65,536 method rows, 1,048,576 relation sites,
131,072 unique identities and 8,388,608 identity UTF-16 units per event. Budget
exhaustion refuses the whole projection. Returned arrays are defensive copies;
the result retains no compiler tree, symbol, task, context or source text.

These are source observations. They do not establish complete dispatch graphs,
incoming callers, runtime frequencies, purity, aliasing, escape behavior,
allocation volume, retained footprint or optimization profitability.
`completeCallGraph()` remains false.

## Native lexical batch

The package-private `java.lang.M3CodeTextSignalBatch` retains its original
four-argument Java implementation. An additional internal overload accepts
`Backend.JAVA` or `Backend.NATIVE_REQUIRED`. Native selection uses one batch
entry in libjava and propagates `UnsatisfiedLinkError` when unavailable, including
for zero rows. There is no automatic threshold or silent native fallback.

Both paths validate row geometry and the existing limits. The native boundary
validates independently before allocation or scanning, and checks the copied
row blocks again. It uses fixed 128-row and 1024-character regions without
native heap allocation, retained heap pointers or application callbacks.
The caller keeps the three input arrays unchanged during a call; independent
batches may run concurrently. Overlapping or reordered rows are supported.

All sixteen lexical flag bits and six saturated byte counters preserve the
existing packed schema. UTF-16 units remain Java chars, including surrogates.
The two-unit arrow and method-reference checks span region seams within a row
and never cross row boundaries. Whitespace follows Java 21's char predicate.

This lexical word is a routing hint. It proves neither regex validity nor
program semantics. Java Pattern and the relevant language compiler remain
the exact authorities. Native work is proportional to rows plus total scanned
row length; overlapping rows may repeat work. Array transfer limits do not bound
that cumulative scan work. No native speedup or zero-copy claim is made.

## Qualification and policy

The added compiler tests live in `test/langtools/tools/javac/m3`; the native
test lives in `test/jdk/java/lang/String/M3CodeTextSignalBatchNativeTest.java`.
The latter has an `integrated` jtreg entry using libjava and a separate explicit
test-library mode for bounded component proof. The production class contains
no library-loading hook.

The receiving packet records source-built compiler-module checks, actual JNI
parity, strict compiler diagnostics, source/API identities, recipe fixed points,
refusal witnesses and independent review. A component or hybrid-image proof
does not qualify a full configured/built/installed M3JDK21, broader jtreg/JCK,
all OS/architectures, or comparative performance. Those gates remain separate.
The global receiving phase and all unrelated collection/regex owners retain
their existing admission status.
