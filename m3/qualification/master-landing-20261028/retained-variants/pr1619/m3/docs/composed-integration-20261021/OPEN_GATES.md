<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Observed open gates and explicit boundaries

This packet is a finite receiving checkpoint, submitted as draft review work.
Executed passing tests do not close unrelated repository promotion gates.

## Two inherited core test failures

`M3StringCharArrayCompositionTest` requests private `M3String.admitArrays` and
`admitArrayRanges` methods that are absent in both the original receiving base
and this implementation. It fails at its first reflective binding, before its
content and ownership assertions. Repository recovery restored the historical
test but explicitly deferred the corresponding #952 M3String modifications.
The original test is preserved; no substitute private API or weakened assertion
is introduced to make the receipt appear successful. Ordinary String constructor,
concat, slice, owner lifetime and JNI composition behavior are exercised by the
other recorded runtime tests. The missing private feature remains unreceived.

`M3StringPoolBoundsTest` passes its first M3-disabled action but its flag-on action
asserts that a global native accounting counter is unchanged across work that
can create or reclaim incidental M3-backed Strings. A diagnostic run observed
65 additional bytes; a discarded fixture experiment also observed an eight-byte
decrease during another measurement. Neither observation is an exact per-call
allocation oracle. The test and runtime accounting are preserved. The new
deterministic native/tuple publication failure tests separately verify the
specific exceptional ownership transfers addressed by this PR. This distinction
does not convert the failing global test into a pass.

## Existing String phase gate

The unchanged `check_string_phase.py` raises `String successor chain length
drift`. The same failure was reproduced using the exact checker and metadata
from original receiving commit `a115db5d8d9a53c20bf6f6880286a967a80a8dda`, before
its target-source checks. The associated checker unit tests pass, and the
closure checker explicitly reports twelve open gates with promotion blocked.
This integration does not rewrite the historical successor chain or advance
the String/Arrays/Collections/AST family order.

## Runtime and packaging boundary

`UseM3StringStorage` remains experimental. Its current enabled configuration is
interpreter-only and rejects the existing incompatible JVMCI, CDS, JFR and String
deduplication combinations. Building normal CDS archives with M3 storage disabled
is a build result, not qualification of M3-enabled CDS. The enabled compiler and
GC/serviceability matrix, full jtreg, JCK and non-Linux platforms remain outside
the executed acceptance recorded here.

The current pool is VM-local. This work does not establish universal OS-wide
canonical sharing. Tuple/range metadata, transient traversal state, requested
mutable Java arrays and JNI contiguous compatibility buffers have real costs.
No claim of arbitrary Java byte/char arrays aliasing a native mapping is made.

The collection module is packaged into an image through the existing JMOD/jlink
route, with native libraries and legal resources checked. Source-built E1 full
suite results and E2 focused relink results have separate hashes and scopes.
They do not prove every available third-party provider or challenge corpus.

## View semantics and source rights

The public `java.util` identity owner has an explicit, tested classification
path. Generic `com.m3` filtered identity values/set hashing do not gain a guessed
classification from class names, contents or observed hash values. Those
proving-ground cases remain separate follow-up work. Filtered navigation also
retains the documented ambiguity of an `IllegalArgumentException` raised by a
comparator during range construction; the received correction specifically
preserves exceptions originating from cursor creation.

The existing core `CollectionViews` license/API promotion gate remains. Source
notices are retained; this packet does not infer permission to relicense a
first-party Apache source as OpenJDK code. Headerless or copyright-only historical
fixtures remain classified without an invented license declaration. Recipe
licensing and donor licensing are recorded separately in the canonical packet.

## Resource measurements

The finite radix experiment includes a native varying-key CPU ratio of 1.137
(candidate/base) at 65,536 rows. Repeated and narrow keys improved in the same
limited paired run. Java/native scratch allocation schedules and retained-index
payload sizes were unchanged. Process RSS is an operating-system diagnostic,
not an isolated retained-heap measurement. Packed-selection allocation results
apply to the recorded zero-survivor workload and runtime epochs. No universal
speedup, overall memory reduction or statistically general result is asserted.

## Failed attempts retained

The evidence keeps initial compiler/doclint and transport failures, the original
23/4/1 jtreg outcome, the discarded pool fixture adjustment, stale donor fixture
refusals, bounded resource regressions and the receiving image's existing-output
jlink refusal. Repairs and successful reruns are separate records. No build
warning, source-drift refusal or unrelated policy gate was disabled to obtain the
accepted results.
