<!-- SPDX-License-Identifier: Apache-2.0 -->
# Repeated M3 regex effects — target receiving record

This stage adapts the existing Synexia relation-power capability into the existing M3JDK21
regex effect owner. It is not a complete regex/parser/compiler/image migration.
Source parent: `hsoliwal/com.synexia@470e81934ddbae6e8015986976afbc41acb79c4f`.
Target parent: `hsoliwal/M3jdk21@d2c7fba68f149f25d30fafc0eca0f4372ad75e89`.
Canonical recipe: `com.synexia.rewrite.M3Jdk21RegexPowerRecipe` in Synexia.
Read this with `name-mapping.json` and its existing regex-effects supplement.

## Runtime changes and mapping

`MIndexStateRelation.repeated` / its native repeat atom supply the reviewed algorithmic
reference; target `M3RegexDpPlan.Effect.repeated` remains the existing internal public signature.
The target-owned C power helper calls the existing generic/structured composition atoms rather
than creating another union engine. `Java_jdk_internal_mindex_M3RegexDpPlan_repeat0` is one
additive internal native export. Existing composition exports and public Java descriptors remain.
There is no Synexia service, package dependency, copied editorial solution, new String API,
new canonical spelling store, or additional retained cache. The target retains its GPLv2 with
Classpath exception terms; recipe/tooling expression remains Apache-2.0.

Automatic native power is limited to all-star plans with at least 16 states, a multistate row,
count at least 256 and a null checkpoint. The mixed-pattern candidate was slower in the measured
case and was rejected. The C entry remains exact for arbitrary valid relations and both composition
modes. An older library without repeat0 leaves Java powers available; a missing power entry does
not disable supported old composition entries. A non-null Runnable uses the original Java loop:
all callback counts, cancellation points and exception precedence are preserved. Negative count
and length overflow are checked before callbacks. Interrupt status is not consumed.

`M3StringRegexDp` factors an exact equal-child repetition spine into one base-fragment effect and
one power. Owner AND coordinate equality are required at every level. Hash or text similarity is
not proof of equality; different slices of one owner must not collapse. Only the already eligible
whole-tuple cohort is considered. Existing source/pattern limits, heap hysteresis, work budget,
recursion cap, scalar one-row DP and Pattern fallback remain. Skipped node visits and power work
are charged conservatively. Intermediate repetition effects need not occupy cache slots.

## Bounds and verification boundary

Power requires O(q^2 log(count+1)) worst-case work, q <= 64. The null-checkpoint JNI branch crosses
JNI once, snapshots one row array and returns one array; it never expands text. There are at most
63 exponent iterations for a nonnegative Java long. Declared native working arrays occupy at most
2560 bytes across the JNI/power/composition call chain, excluding call frames/compiler spills.
The C helper also handles UINT64_MAX in qualification. All lanes are validated before zero-count
identity or zero-output shortcuts, and invalid input leaves C output unchanged, including aliases.

Component tests compare before/after public powers and checkpoint traces, direct JNI matrices,
full and partial aliases, old/no-native fallback, all widths, long counts, and corrupted inputs.
Wrapper tests use explicitly TEST_ONLY owner/coordinate fixtures. Factoring a 1024-copy fixture
retained two effect slots instead of eleven, with two cold source reads and zero warm reads in
that run. This is not measured product cache residency or actual M3String/HotSpot integration.

The task retains adverse timings and tests. Diagnostic Java effect-power timing includes the JNI
call and returned result allocation but is not JMH or String.matches timing. Source-built JDK and
libjava linking, actual canonical-storage jtreg/CompactStrings, full Maven/OpenRewrite SDK/reactor,
Windows/MSVC and product heap/native-memory/latency qualification remain pending. No phase of the
ordered String -> arrays -> collections -> compiler migration is advanced by this component pass.

Persistent regex images, captures/find/restart semantics, compiler lowering, strict LRU/payload
spilling and the DOOP viewport work remain open, as do their original mapping records. The user’s
DOOP terminology is Deferred Object-Oriented Programming; Atomizer + Patternizer + M3 denotes
Machine-Oriented Programming. Those names describe the requested architecture, not a completed
viewport or homepage implementation in this stage.
