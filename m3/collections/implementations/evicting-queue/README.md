# M3JDK21 evicting queue — canonical owner candidate

This is a first-party **M3JDK21** collection implementation, not a Guava adapter or a separate Java SDK.

- Canonical Java owner: `com.m3.collections.queue.M3EvictingQueue`.
- Canonical Maven candidate: `com.m3.collections:m3-evicting-queue`.
- Source of the original implementation: parent-owned dynamic `Object[]` ring with physical offsets, primitive state flags, and stable `long[]` removal-mask compaction.
- Java 21 only; no Guava runtime dependency and no JDK public `java.util` class rename.

The historical, public, final, Serializable
`com.m3.collections.guava.M3EvictingQueue` remains exactly where it was.
Its old bytecode name, reflective lookup and existing serialized stream
identity must continue to work. Adding a new source/class does **not**
silently reinterpret old serialized bytes as a new Java class.

This is an **additive, review-only namespace migration candidate**.
The source is mechanically reproduced from the sealed historical postimage
with the package/Javadoc adjustment; the temporary duplicate is intentional
to avoid a breaking superclass/delegation/serial-fields change to the legacy
class. It is not permission to maintain two independently diverging queues.
The Synexia Maven/OpenRewrite recipe seals both exact files and refuses
different old source bytes or an occupied new destination. Future
consolidation requires independent consumer-closure, ABI, wire and installed
JDK tests. It must not be mistaken for promotion into `java.base` or
`java.util` before the String/array/collection admission sequence.

## Proof gates

- `M3EvictingQueueCanonicalContractProbe` executes both implementations
  with a common behavior transcript, normalized public method signatures,
  reflection-final checks, zero/negative-capacity and null contracts,
  offset/iterator/mask operations and serialization round-trip.
- The historical class's prior exact-byte stream probe must still pass.
- For the same value, the two wire encodings differ **only** in the equally
  long class descriptor name; the probe verifies byte equality after
  substituting that descriptor.
- Java 21 Maven package/build and binary classpath checks are executed in
  the accompanying GitHub Actions workflow.

The root mapping `m3.collections.evicting-queue.owner-naming` remains a
proposal. No compatibility contract is declared fully qualified or merged
until its consumer inventory, rights/NOTICE and JDK image gates pass.
