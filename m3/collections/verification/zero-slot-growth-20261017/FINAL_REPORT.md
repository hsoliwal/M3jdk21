# M3JDK21 — primitive zero-key capacity receiver

Three existing `com.m3.collections` classes are refined through source-sealed
Synexia recipe, with exact Git-blob preimages and byte-equal published postimages.
No new owner, array representation, Entry object, public method signature or JNI
symbol was introduced. Map/Set values, iteration order and fail-fast behavior
were checked against JDK oracles across 196,611 seeded operations. The direct
selected-source Java21 candidate test recorded 656,464 checks, six killed mutants,
ten source refusals and repeat-identical 12 class files.

The one intentional diagnostic behavior change is `capacity()` and corresponding
primitive `payloadBytes()`, which no longer grow prematurely when key zero occupies
an off-table scalar. At the eight-slot five-nonzero-plus-zero boundary, the set's
primitive payload changes 64->32 bytes, int-int map 128->64, and int-long map 192->96.
These figures exclude Java array headers, object headers, old-copy peak allocation,
GC/native costs, retained domain objects and CPU throughput.

Scope: `m3/collections` separately built Apache-2.0 module. No java.base, HotSpot or
`java.util` replacement is installed. String -> arrays -> collections migration
order remains locked. Maven JUnit, OpenRewrite SDK, full reactor, jtreg,
JNI/GC/platform and independent review are still NOT_RUN and block promotion.
