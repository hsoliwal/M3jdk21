<!-- SPDX-License-Identifier: Apache-2.0 -->
# com.m3.optane — optional Java 21/JNI JMOD candidate

Copyright 2026 Hitesh Soliwal and contributors.

## What this delivers

An independent Apache-2.0 named module, modular JAR and (Linux JNI enabled when built)
JMOD. It is **opt-in**, not a patched HotSpot or an automatic optimization of every
String/collection. Source generation belongs to the Synexia recipe crate
`synexia-openrewrite-recipes/recipes/optane-list-handoff-20261015`.

- MemoryModeProfile: explicitly operator-declared DRAM/PMem capacities. Never adds
  DRAM cache to OS-visible capacity and never claims hardware/BIOS verification.
- ScanPlan: lazy long-offset range traversal, at most 64 MiB per window, O(1)
  planner state, ordered callbacks and progress/cancellation. Window budgets account
  for the caller's worker and temporary-copy counts. The caller owns payloads,
  scheduling, CPU binding and source/publication authority.
- MappedReadAdvice: optional Linux JNI NORMAL/SEQUENTIAL/RANDOM/WILL_NEED advice
  on existing read-only mapped buffers. Page alignment is inward, ranges are
  validated in Java AND native code, buffers/cursors remain unchanged, and no pointer
  survives a call. Errors/unavailable native code are explicit, not fake success.
- Diagnostics launcher: prints a declared profile and native ABI availability, not
  a hardware detection report or a performance claim.

No allocator, arena, new interner, corpus store, forced unmap, persistence protocol,
GC policy, CPU-affinity scheduler, JNI byte copy or automatic source hook is added.
There is no dependency on Synexia at runtime and no change to java.base.

## Memory Mode versus App Direct

On the current T7920 Memory Mode design, DRAM is a CPU-managed cache in front of
volatile PMem. Ordinary Java heap, native allocations and cached file pages already
use OS-visible memory. This module cannot reserve hidden DRAM, allocate an object
into it, make PMem persistent, or enlarge the hardware cache.

Locality and a bounded active set are the optimization hypothesis. File advice is
kernel page-cache/readahead policy, **not** an Intel DRAM-cache instruction. Repeated
index probes need a different hint from a one-pass scan; neither is universally faster.

App Direct/PMDK remains in the existing MIndexPmem2Region -> ShardStager -> Tier/Corpus
and LangDexPmemImageSet owners. No PMDK library is packaged by this JMOD. There is no
claim that DAX, App Direct, NUMA binding, power-loss recovery or direct GPU access to
PMem has been implemented/verified here. Do not change BIOS mode or reformat PMem
namespaces to run these tests.

## Build and use

```sh
# In an exact recipe-generated target checkout; output must be absent.
bash m3/optane/verify.sh
# Or, on an approved Maven-equipped runner:
mvn -B -ntp -f m3/optane/pom.xml clean verify

# Named JAR works on the ordinary module path; no JNI path means explicit no-op hints.
java --module-path m3/optane/target/com.m3.optane.jar \
  -m com.m3.optane 64 2048 2 40

# JMOD is a LINK-TIME artifact, not a runtime --module-path file.
# verify.sh already produces and tests this linked image:
m3/optane/target/runtime/bin/m3-optane 64 2048 2 40
```

`M3_JDK=/path/to/a/complete/matched/M3JDK21` selects the product JDK. Stock Java 21
software tests do not prove a source-built M3JDK image. `M3_OPTANE_NO_NATIVE=1`
builds Java-only software artifacts. `M3_OPTANE_BUILD` selects a fresh private output.
The shell verifier is Linux-oriented; Windows native packaging has not been added.

Example bridge at an existing owner's read loop (illustrative integration, not an
already wired M3String hook):

```java
var profile = new MemoryModeProfile(64L << 30, 2048L << 30, 2);
var plan = profile.plan(32L << 30, 40, 2, 64 << 20, null);
plan.forEach(0, corpusLength, (offset, count) -> {
    // Borrow exactly this range from the current MIndex/M3 owner.
    // Run its existing verified CPU/JNI kernel. Do not construct a new text pool.
    existingOwnerProcessRange(offset, count);
}, progressMonitor);
```

The budgets are hypotheses supplied by the operator, not a promise that bytes remain
in DRAM. Cache conflicts, other processes, NUMA locality and callback allocations are
not modeled. The socket count records profile intent; it does NOT bind workers.

## Existing owners retained; next integration gates

| Existing owner | Disposition |
|---|---|
| java.lang.M3StringOwner/Atom/Pool/Tuple, M3MappedStringBacking | No changes; opt-in call sites pending target lease/API proof |
| MIndexPmem2Region / mindex_pmem2_jni.c | Retain canonical native PMDK mapping/persistence ownership |
| MIndexPmem2Tier/Corpus | Retain verified SHA roots, fallback, shard and failure-domain decisions |
| LangDexCompiledImageSet / LangDexPmemImageSet | Retain immutable source images and verified lazy reopen |
| MIndexProgressMonitor | Adapt small dependency-free contract; no new global progress owner |
| M3 GPU precompute | Existing detached bounded batches and CPU parity remain required |

The proposed target mapping is recorded in PORT-MAPPING.pending.json. It is a
**PENDING optional-module admission**, not a new authority that supersedes the
canonical m3/docs/name-mapping.json or advances String -> arrays -> collections ->
compiler receiving stages. Full registry admission and automatic callers remain open.

## Qualification, measurements and limitations

The verifier compiles actual Java 21 and C17, checks normal/interpreter behavior,
Java-only/native paths and JNI under -Xcheck:jni, creates the JAR/JMOD, then runs the
same contracts from the linked image. The 4 TiB test enumerates logical ranges; it
**does not allocate 4 TiB**. Mapped-file tests use ordinary temporary files, not Optane.

Actual run logs and machine/tool versions belong in the delivery evidence. No stub
PMDK/JUnit/device library is used. Maven/OpenRewrite/JUnit, full reactor, modified
M3JDK image, real T7920 CPU/NUMA/cache-counter and performance qualification remain
separate gates. No speedup claim is made. The native artifact is machine/platform-
specific and should be rebuilt for the user's deployment distribution.

Hardware acceptance should compare the same corpus and query order, 1/2/4/8/... workers,
local versus remote NUMA where verified, 64/128/256/512 GiB **real** DRAM configurations,
and normal/random/sequential hints. Record cold preparation separately from repeated
queries, p50/p95 latency, throughput, GC, RSS/native/page-table usage and actual memory
controller counters. A process restart alone does not establish cold page/cache state.

## Primary references

- https://www.intel.com/content/www/us/en/support/articles/000055901/technologies/memory-and-storage/intel-optane-persistent-memory.html
- https://docs.oracle.com/en/java/javase/21/docs/specs/man/jmod.html
- https://docs.oracle.com/en/java/javase/21/docs/specs/man/jlink.html
- https://man7.org/linux/man-pages/man2/madvise.2.html
- https://github.com/hsoliwal/com.synexia/blob/develop/synexia-mindex/native-jni/PMDK_SOURCE_SDK.md
- https://github.com/hsoliwal/M3jdk21/blob/master/m3/docs/m3jdk21-porting-invariant.md
