# M3 lazy long-priority frontier receiver

This is a tooling-side Apache-2.0 adaptation of Synexia's primitive heap algorithm. The canonical owner and the hash-pinned OpenRewrite recipe remain in `hsoliwal/com.synexia`; M3 does not acquire a Synexia runtime dependency.

## Contract

- `peek()` returns the current top without removing it.
- `poll()` removes one top and repairs one heap path.
- `pollTop(limit)` consumes only the requested prefix.
- Min and max order are explicit.
- Construction clones the source array; neither construction nor draining mutates caller input.
- The queue stores primitive `long` values in one array and does not retain one object per value.
- Empty reads and negative limits fail explicitly.

This is not a replacement for `java.util.PriorityQueue`, changes no `java.base` API, and has not been integrated into a JDK hot path. It is the receiver-side implementation and correctness fixture for the source-owned recipe packet.

## Provenance

Source recipe PR: [Synexia #10629](https://github.com/hsoliwal/com.synexia/pull/10629). Canonical donor owner: `synexia-common/src/main/java/com/synexia/common/collections/PackedLongHeap.java`, Apache-2.0, Git blob `30238e6df8a9b142412d2ce7f0b26cc5159d3cfe`. The target class is an independent adaptation, not a byte-identical copy. Eclipse Nebula's EPL-2.0 class is not copied.

The source-owned packet seals both receiver Java files with ABSENT preimages and exact postimage SHA-256 values. `SOURCE_PROVENANCE.tsv` records the mapping and licensing.

## Verify

```sh
sh m3/tooling/lazy-priority-frontier/verify.sh
```

The script compiles with Java 21, all lint warnings treated as errors, then runs four deterministic regression groups. The 4,096-value comparison establishes ordering correctness against `Arrays.sort`; it is not a performance benchmark. No speedup or JNI benefit is claimed.
