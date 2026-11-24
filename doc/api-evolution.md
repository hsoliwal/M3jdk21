<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Compatible API evolution in M3JDK21

M3JDK21 may extend its existing API owners with useful interface defaults,
delegating overloads and optional skeletal abstract classes. Preserve the
supported Java 21 subset and previously accepted fork extensions. An addition
must preserve existing implementation obligations, subclass hooks and specified
observable behavior. New extension calls need not run on stock Java 21.

## Choose the extension mechanism

| Mechanism | Required design |
| --- | --- |
| Interface default | Derive a valid fallback from established operations; check inherited-method collisions. |
| Convenience overload | Supply documented default values and invoke the established virtual operation. |
| Existing abstract base | Add concrete behavior without imposing new abstract obligations on existing concrete subclasses. |
| Optional skeletal base | Keep adoption optional; preserve direct implementations and existing superclass choices. |
| Specialized implementation | Override the fallback only while preserving its declared contract. |
| Capability without a valid fallback | Specify an explicit opt-in extension boundary. |

The `default` modifier belongs to interface methods. Abstract classes provide
fallbacks using ordinary concrete methods. Overloads can express default choices
without changing the Java 21 language syntax.

## Preserve dispatch and behavior

A convenience overload normally delegates to the existing virtual method. Its
implementation specification must state the forwarded arguments, call count,
validation owner and failure behavior. Do not route around a provider override,
add lifecycle callbacks, change cancellation checks, clear interruption, or finish
a borrowed resource merely because a shorter entry point was introduced.

The progress SPI applies this rule directly:

```java
default void beginUnknownWork(String task) {
    begin(task, UNKNOWN_WORK);
}

default void workedOneUnit() {
    worked(1L);
}
```

The full-argument hooks retain task validation and progress semantics, including
the handling of a null task. The distinct defaults add no monitor state or
callbacks. Existing concrete class methods retain Java's normal precedence.

The existing whole-array monitored I/O overloads in `InputStream`, `OutputStream`,
`Reader` and `Writer` demonstrate the concrete-method delegation pattern. Their
delegating bodies and old-binary tests do not establish unchanged-source
compatibility: the overload groups have the method-reference limitation below.
Keep their established virtual hooks and regression tests. A new abstract base
is not required to obtain the delegation behavior.

An unsupported-operation fallback is valid only where the new API explicitly
defines that operation as optional. A no-op is valid only where it satisfies the
declared contract. Do not invent a successful result or silently discard options.

## Additive changes still require compatibility evidence

Java's binary-compatibility classification is narrower than complete application
compatibility. The acceptance record must distinguish:

1. Existing binaries linking and executing against the candidate API.
2. Existing source recompiling, with the intended overload and inherited-method
   selection.
3. Existing behavior, including exceptions, callback order, mutation, ownership,
   cancellation, threading and serialization where applicable.

For example, adding `read(char[])` beside `read(CharBuffer)` would make an existing
`read(null)` expression ambiguous. It is rejected for `Readable`; this change does
not add either array overload to that interface. Test null arguments, lambdas,
method references, boxing, widening, varargs, erasure and inherited methods when
they are relevant to the candidate overload set.

Adding an overload with a different argument count can also break previously
valid method references. For example, an existing `monitor::worked` expression
selects `Consumer<Long>` over `Function<Long,Object>` when the only declaration is
`worked(long)`. Adding `worked()` makes that method reference inexact for this
overload-resolution context and the selector call becomes ambiguous. The same
problem occurs for `monitor::begin` with the corresponding two-argument selector.
The proposed short overloads were rejected after this concrete counterexample.
The progress defaults therefore use distinct names, `beginUnknownWork(String)`
and `workedOneUnit()`, leaving both established method groups unchanged.

Test method references passed to overloaded functional-interface parameters as
well as method references assigned to a single known target type. Different
arity alone is not sufficient evidence that an overload preserves old source.

### Existing monitored-I/O source-selection limitation

The same different-arity counterexample applies to concrete methods in abstract
classes. A source-sealed comparison of the actual pre-overload and current
M3JDK21 owners reproduces unchanged-source ambiguity for these five method groups:

| Existing four-argument hook | Added whole-array overload |
| --- | --- |
| `InputStream.readMonitored(byte[], int, int, ProgressMonitor)` | `readMonitored(byte[], ProgressMonitor)` |
| `InputStream.readNBytesMonitored(byte[], int, int, ProgressMonitor)` | `readNBytesMonitored(byte[], ProgressMonitor)` |
| `OutputStream.writeMonitored(byte[], int, int, ProgressMonitor)` | `writeMonitored(byte[], ProgressMonitor)` |
| `Reader.readMonitored(char[], int, int, ProgressMonitor)` | `readMonitored(char[], ProgressMonitor)` |
| `Writer.writeMonitored(char[], int, int, ProgressMonitor)` | `writeMonitored(char[], ProgressMonitor)` |

An overloaded selector accepting either a primitive-int-returning or
boxed-Integer-returning four-argument function can reject `input::readMonitored`.
A selector accepting either a void-returning or Object-returning four-argument
function can similarly reject `output::writeMonitored`. The same caller source
compiles before the whole-array overloads are added; its frozen old binaries
continue to execute the established four-argument hooks afterward. This is a
source compatibility defect in these M3 extension groups, distinct from a
failure of a stock Java 21 API or the old binary linkage in the tested cases.

An explicit functional-interface target or a suitably typed lambda can
disambiguate a caller. Those are client source changes, not proof that unchanged
source remains compatible. A distinct name avoids this particular ambiguity
when choosing a new API before publication. Adding an alias after publication
does not make an already overloaded method group exact again.

The published declarations remain intact. Their unrestricted unchanged-source
compatibility qualification remains unresolved; removal or renaming could
break newer consumers and requires a separately reviewed compatibility decision.
The concrete-base cases in `MethodReferenceEvolution.java` retain both the
negative language example and distinct-name/explicit-target controls. Exact
actual-owner inputs, compiler diagnostics and old-binary hashes remain in the
canonical Synexia `m3-api-companions-20261029` recipe family.

Unrelated inherited defaults can conflict at compilation and can cause an
`IncompatibleClassChangeError` when an old binary invokes the method. A superclass
abstract declaration can also prevent an interface default from satisfying a
subclass's implementation obligation. Do not assume that a default repairs an
arbitrary abstract-class hierarchy. Retain functional-interface status where
existing lambdas rely on it.

Changing an established concrete method to abstract, making an extension hook
final, or changing static/instance status is outside this compatibility-preserving
addition policy. Preserve existing descriptors, constructors, accessibility and
the documented provider contract.

## Required proof for a candidate

Compile representative callers and providers against the exact baseline and
retain their class files. Run those unchanged binaries against the candidate,
then recompile their unchanged sources and compare behavior and dispatch.
Compile and run callers of the new operations. Include concrete and abstract
superclasses, direct providers and relevant negative collision fixtures.

Use API comparisons as one input; compilation success alone does not establish
behavioral equivalence. Exercise reflection, proxy dispatch, serialization,
service loading, module access or JNI when the changed contract affects them.
Identify the actual tested providers and platform scope. A finite corpus does
not establish compatibility with every unknown third-party implementation or
every program that enumerates all methods reflectively.

The reusable Maven/OpenRewrite recipe, frozen before/after sources, refusal and
fixed-point tests, category decisions and execution evidence remain in Synexia.
M3JDK21 receives exact emitted product sources, this product documentation and
target-owned regression tests. Independent target compiler, jtreg and runtime
checks remain required; recipe execution is not a replacement for those checks.

Focused target tests:

- `test/jdk/java/util/concurrent/ProgressMonitor/DefaultOverloads.java`
- `test/langtools/tools/javac/compatibility/DefaultApiEvolution.java`
- `test/langtools/tools/javac/compatibility/MethodReferenceEvolution.java`
- Existing `test/jdk/java/io/MonitoredIoArrayOverloads.java`
- Existing progress-provider and `ProgressJob` regressions

## Java 21 specification references

- [JLS 13.5.7: interface method evolution](https://docs.oracle.com/javase/specs/jls/se21/html/jls-13.html#jls-13.5.7)
- [JLS 13.4.23: overload evolution](https://docs.oracle.com/javase/specs/jls/se21/html/jls-13.html#jls-13.4.23)
- [JLS 8.4.8.4: inherited methods and abstract superclasses](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8.4)
- [JLS 9.8: functional interfaces](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.8)
- [JLS 15.13.1: exact and inexact method references](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.13.1)
- [JLS 15.12.2.5: most-specific overload selection](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.12.2.5)
