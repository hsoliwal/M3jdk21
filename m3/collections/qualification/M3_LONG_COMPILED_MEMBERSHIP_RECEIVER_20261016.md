# M3JDK21 compile-time frozen long membership — additive candidate

Canonical Synexia recipe: `com.synexia.m3.Jdk21LongCompiledMembership20261016` (source, emitter, manifest, tests and donor catalogue in Synexia; target consumes exactly two Java postimages). Parent M3JDK21 review #978, which provides the existing LongPredicate/M3LongCollection filtered-view contract.

This receiving increment **does not change** java.util, java.lang.String, HotSpot, JNI/native symbol tables, M3 native canonical image layout or any public existing collection methods. The new `M3LongCompiledMembership` implements `java.util.function.LongPredicate` and plugs into existing M3 `filter` factories. It stores immutable arrays only; no per-element wrapper/Map.Entry, native pointer or canonical payload owner.

Compile-time emitter is authoring-only, not a second JDK compiler: it accepts explicit immutable signed decimal constants, precomputes a dense membership bitmap for contiguous-enough domains or a sorted long lookup for sparse domains, emits one class static initializer and rejects malformed source or output conflicts. It NEVER transforms arbitrary Java predicate callbacks, mutable captures, Java equality, comparator order or mutation contracts.

Executed local Java21 proof: 650,019 primitive checks, generated dense/sparse source `javac --release 21 -Xlint:all -Werror`, exact generated bitmap + sparse expected-membership oracle. Synthetic real-API compilation uses current #978 interfaces and source. Full module Maven/OpenRewrite SDK, matched JDK, native Java/JNI and JMH evidence remain NOT_RUN / HOLD.
